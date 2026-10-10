package ai.gsmc.fundamentals.liquid;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.heat.Heat;
import ai.gsmc.fundamentals.separation.Hazards;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * What a fluid's real properties do in play. Water-based fluids freeze in the pipe: below their freezing point a pipe stops ticking
 * and a pump's network stops moving them until the heat comes back. A fluid past its boiling point in an open basin boils off, and
 * what it gives off is breathed. A flammable fluid let out by a burst pipe beside a flame, past its flash point, or anywhere past its
 * autoignition temperature, catches fire, and spilt ones in the world burn as vanilla's fire burns wood. Viscous fluids pump slower,
 * after the Hydraulic Institute's viscosity correction. Goggles on a pipe, pump, valve, tank or basin give the figures. The figures are
 * {@link Liquid}s in the data map {@link #PROPERTIES}.
 */
public final class Liquids {

    public static final DataMapType<Fluid, Liquid> PROPERTIES = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "liquid_properties"), Registries.FLUID, Liquid.CODEC)
            .synced(Liquid.CODEC, false).build();

    /** Ticks between a pump network's looks at whether what it moves has frozen. */
    public static final int INTERVAL = 20;
    /** Ticks between an open basin's boil-offs, and the millibuckets each takes. */
    public static final int VENT_INTERVAL = 40, VENT = 25;
    /** How far below the climate the blocks round a pipe can cool it: blue ice is -20 °C and a few of them add up. */
    private static final double COLDEST = 40;
    /** °C of heat from the blocks round it that means a flame or a hot surface close by, which lights a flammable past its flash point. */
    private static final double SPARK = 100;
    /** mPa·s past which a pump slows, the slowest it gets, and how fast it falls off per decade of viscosity. */
    private static final double VISCOUS = 100, SLOWEST = 0.25, PER_DECADE = 0.35;

    private Liquids() {}

    public static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(PROPERTIES);
    }

    @Nullable
    public static Liquid of(Fluid fluid) {
        if (fluid == Fluids.EMPTY) {
            return null;
        }
        Fluid still = fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
        return still.builtInRegistryHolder().getData(PROPERTIES);
    }

    /** A fluid's own temperature, °C: a hot stream's from the table, else whatever its fluid type says. */
    public static double own(FluidStack stack) {
        Liquid liquid = of(stack.getFluid());
        return liquid != null && liquid.celsius().isPresent() ? liquid.celsius().get() : stack.getFluid().getFluidType().getTemperature(stack) - 273.15;
    }

    /** What {@code fluid} at {@code pos} stands at: the heat round it, or its own if it runs hotter. */
    public static double celsius(Level level, BlockPos pos, Fluid fluid) {
        Liquid liquid = of(fluid);
        double here = Heat.at(level, pos);
        return liquid == null || liquid.celsius().isEmpty() ? here : Math.max(here, liquid.celsius().get());
    }

    public static boolean frozen(Level level, BlockPos pos, Fluid fluid) {
        Liquid liquid = of(fluid);
        if (liquid == null || !liquid.aqueous() || liquid.freezes().isEmpty() || liquid.celsius().isPresent()) {
            return false;
        }
        double point = liquid.freezes().get();
        return Heat.ambient(level, pos) - COLDEST < point && Heat.at(level, pos) < point;
    }

    public static boolean frozen(Level level, BlockPos pos, Collection<PipeConnection> connections) {
        for (PipeConnection connection : connections) {
            FluidStack stack = connection.getProvidedFluid();
            if (!stack.isEmpty() && frozen(level, pos, stack.getFluid())) {
                return true;
            }
        }
        return false;
    }

    /** Past its boiling point, and a liquid at room temperature: a gas in a pipe is no news. */
    public static boolean boiling(Level level, BlockPos pos, Fluid fluid) {
        Liquid liquid = of(fluid);
        return liquid != null && liquid.boils().filter(b -> b > 20).isPresent() && celsius(level, pos, fluid) > liquid.boils().get();
    }

    /** The share of its usual flow a pump moves {@code fluid} at. */
    public static double pumping(Fluid fluid) {
        Liquid liquid = of(fluid);
        double viscosity = liquid == null ? 0 : liquid.viscosity().orElse(0.0);
        return viscosity <= VISCOUS ? 1 : Math.max(SLOWEST, 1 - PER_DECADE * Math.log10(viscosity / VISCOUS));
    }

    /** Called every {@link #VENT_INTERVAL} ticks for every basin: open to the sky above (no mixer or press over it), whatever in it
     * is past its boiling point boils off a little, and is breathed. */
    public static void vent(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos.above()).isAir() || !level.getBlockState(pos.above(2)).isAir()) {
            return;
        }
        IFluidHandler tanks = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (tanks == null) {
            return;
        }
        for (int i = 0; i < tanks.getTanks(); i++) {
            FluidStack stack = tanks.getFluidInTank(i);
            Liquid liquid = stack.isEmpty() ? null : of(stack.getFluid());
            if (liquid == null || liquid.boils().isEmpty() || celsius(level, pos, stack.getFluid()) <= liquid.boils().get()) {
                continue;
            }
            tanks.drain(stack.copyWithAmount(VENT), IFluidHandler.FluidAction.EXECUTE);
            Hazards.vapour(level, pos, stack.getFluid(), liquid.toxic());
        }
    }

    /** {@code fluid} let out at {@code pos} catches fire if it can: past its autoignition temperature, or past its flash point with a
     * flame or hot surface close by. True if it did. */
    public static boolean ignite(Level level, BlockPos pos, Fluid fluid) {
        Liquid liquid = of(fluid);
        if (liquid == null || !liquid.flammable()) {
            return false;
        }
        double here = Heat.at(level, pos);
        boolean lit = liquid.autoignition().filter(a -> here >= a).isPresent()
                || here >= liquid.flashPoint().get() && here - Heat.ambient(level, pos) >= SPARK;
        BlockState fire = BaseFireBlock.getState(level, pos);
        if (!lit || !level.getBlockState(pos).canBeReplaced() || !fire.canSurvive(level, pos)) {
            return false;
        }
        level.setBlockAndUpdate(pos, fire);
        return true;
    }

    /** Spilt in the world, a flammable fluid burns as vanilla's fire burns its fuels: the lower the flash point, the readier. */
    public static void onDataMapsUpdated(DataMapsUpdatedEvent event) {
        event.ifRegistry(Registries.FLUID, registry -> registry.getDataMap(PROPERTIES).forEach((key, liquid) -> {
            Block block = registry.getOrThrow(key).defaultFluidState().createLegacyBlock().getBlock();
            if (liquid.flammable() && block instanceof LiquidBlock) {
                double flash = liquid.flashPoint().get();
                ((FireBlock) Blocks.FIRE).setFlammable(block, flash < 23 ? 60 : flash < 60 ? 30 : 5, flash < 23 ? 100 : flash < 60 ? 60 : 20);
            }
        }));
    }

    @Nullable
    public static Fluid carried(@Nullable FluidTransportBehaviour pipe) {
        if (pipe == null || pipe.interfaces == null) {
            return null;
        }
        return pipe.interfaces.values().stream().map(PipeConnection::getProvidedFluid).filter(s -> !s.isEmpty()).map(FluidStack::getFluid)
                .findFirst().orElse(null);
    }

    /** The goggles' lines for {@code fluid} at {@code pos}: what it is, and what it is doing there. False if there is nothing to say;
     * {@code named} heads them with its name, for a pipe, which Create gives none. */
    public static boolean describe(@Nullable Level level, BlockPos pos, @Nullable Fluid fluid, List<Component> tooltip, boolean named) {
        Liquid liquid = fluid == null || level == null ? null : of(fluid);
        if (liquid == null) {
            return false;
        }
        String g = "goggles.fundamentals.liquid.";
        if (named) {
            tooltip.add(fluid.getFluidType().getDescription().copy());
        }
        tooltip.add(indent(liquid.viscosity().isPresent() ? Component.translatable(g + "body", figure(liquid.density()), figure(liquid.viscosity().get()))
                : Component.translatable(g + "dense", figure(liquid.density()))));
        if (liquid.freezes().isPresent() && liquid.boils().isPresent()) {
            tooltip.add(indent(Component.translatable(g + "range", figure(liquid.freezes().get()), figure(liquid.boils().get()))));
        } else {
            liquid.freezes().ifPresent(f -> tooltip.add(indent(Component.translatable(g + "freezes", figure(f)))));
            liquid.boils().ifPresent(b -> tooltip.add(indent(Component.translatable(g + "boils", figure(b)))));
        }
        List<Component> hazards = new ArrayList<>();
        liquid.flashPoint().ifPresent(f -> hazards.add(Component.translatable(g + "flammable", figure(f))));
        for (String hazard : new String[] {"toxic", "fuming", "corrosive"}) {
            if (hazard.equals("toxic") ? liquid.toxic() : hazard.equals("fuming") ? liquid.fuming() : liquid.corrosive()) {
                hazards.add(Component.translatable(g + hazard));
            }
        }
        if (!hazards.isEmpty()) {
            MutableComponent line = Component.empty();
            for (int i = 0; i < hazards.size(); i++) {
                line.append(i == 0 ? Component.empty() : Component.literal(", ")).append(hazards.get(i));
            }
            tooltip.add(indent(line));
        }
        double celsius = celsius(level, pos, fluid);
        if (frozen(level, pos, fluid)) {
            tooltip.add(indent(Component.translatable(g + "frozen", figure(celsius), figure(liquid.freezes().get()))));
        } else if (boiling(level, pos, fluid)) {
            tooltip.add(indent(Component.translatable(g + "boiling", figure(celsius))));
        }
        liquid.celsius().ifPresent(c -> tooltip.add(indent(Component.translatable(g + "hot", figure(c)))));
        double pumping = pumping(fluid);
        if (pumping < 1) {
            tooltip.add(indent(Component.translatable(g + "viscous", Math.round(100 * pumping))));
        }
        return true;
    }

    /** Three significant figures, or whole and grouped from a hundred up. */
    public static String figure(double value) {
        return Math.abs(value) >= 100 ? String.format("%,.0f", value)
                : new BigDecimal(value).round(new MathContext(3)).stripTrailingZeros().toPlainString();
    }

    private static Component indent(Component text) {
        return Component.literal("    ").append(text);
    }
}
