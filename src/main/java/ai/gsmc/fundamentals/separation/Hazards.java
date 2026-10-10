package ai.gsmc.fundamentals.separation;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.uses.Uses;
import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * What the plant does to people and pipes. The fuming acids (hydrofluoric, nitric, aqua regia) and bromine hurt anyone within reach of them
 * in the open: as blocks in the world, or in a basin they are being used in. Create's diving helmet on a filled
 * backtank is the gas mask, and breathes its air. Beryllium's hydroxide, oxide and salts are a dust that scars the lungs: held in the hand or
 * stirred in a basin, they are breathed the same way. And the acids eat copper: Create's pipes, pumps and valves carrying one
 * corrode and eventually burst, spilling it. The liquors are rare earth chlorides in dilute acid, and the spent liquor, the calcium chloride
 * liquor and bittern are chloride too, so they eat it as well, more slowly, and seawater, a tenth as salt as bittern, slower still.
 * Metal tanks go the same way, ten times slower for the thicker wall.
 * Plastic pipes, pumps, valves and tanks do not corrode. A glass pipe is a copper pipe with a window, and corrodes as one.
 * Caustic soda and the sodium aluminate liquor are the other way about: they leave copper and steel alone and eat aluminium, so only
 * The Factory Must Grow's aluminium pipes, pumps, valves and tanks corrode under them, at a liquor's pace. And a Hall-Héroult pot gives off
 * hydrogen fluoride: a vat with cryolite in it fumes as hydrofluoric acid does.
 */
public final class Hazards {

    private static final TagKey<Item> BERYLLIUM_DUSTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "beryllium_dusts"));

    /** How far a fuming source reaches. */
    private static final int REACH = 2;
    /** Per tick, for a pipe carrying an acid: on average a pipe lasts two minutes. */
    public static double corrosionChance = 1.0 / 2400;
    /** Per tick, for a pipe carrying a liquor, crude liquor, spent liquor or any other chloride solution: on average eight minutes. */
    public static double liquorCorrosionChance = 1.0 / 9600;
    /** Per tick, for a pipe carrying seawater: on average half an hour. */
    public static double seawaterCorrosionChance = 1.0 / 36000;
    /** How many times longer a tank's wall lasts than a pipe's, for the same fluid: twenty minutes under acid, eighty under a liquor. */
    private static final int TANK_WALL = 10;
    /** How many ticks apart a pipe is checked, each check standing for all of them. */
    public static final int PIPE_INTERVAL = 20;
    private static final Map<Block, Boolean> CORRODIBLE = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> ALUMINIUM = new ConcurrentHashMap<>();

    private Hazards() {}

    // ---- fumes ----

    /** Everyone within reach of {@code source}, a fuming acid in the open: masked, they breathe their tank; bare, it burns. */
    public static void fume(ServerLevel level, BlockPos source, Acids.Acid acid) {
        breathe(level, source, acid.poisons);
        level.sendParticles(ParticleTypes.WHITE_SMOKE, source.getX() + 0.5, source.getY() + 1.1, source.getZ() + 0.5, 3, 0.3, 0.2, 0.3, 0.01);
    }

    /** The vapour off spilt mercury, once: it poisons whoever is within reach and unmasked. */
    public static void mercuryVapour(ServerLevel level, BlockPos source) {
        breathe(level, source, true);
        level.sendParticles(ParticleTypes.WHITE_SMOKE, source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.01);
    }

    /** Hydrogen fluoride off the cryolite bath of a Hall-Héroult pot: it burns and poisons whoever is within reach and unmasked. */
    public static void fluorideFume(ServerLevel level, BlockPos source) {
        breathe(level, source, true);
        level.sendParticles(ParticleTypes.WHITE_SMOKE, source.getX() + 0.5, source.getY() + 1.1, source.getZ() + 0.5, 3, 0.3, 0.2, 0.3, 0.01);
    }

    /** Beryllium dust raised where it is handled in the open: whoever is within reach and unmasked breathes it. */
    public static void berylliumDust(ServerLevel level, BlockPos source) {
        breathe(level, source, false);
        level.sendParticles(ParticleTypes.WHITE_ASH, source.getX() + 0.5, source.getY() + 1.0, source.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.01);
    }

    private static void breathe(ServerLevel level, BlockPos source, boolean poisons) {
        AABB box = new AABB(source).inflate(REACH);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
            ItemStack tank = DivingHelmetItem.isWornBy(living) ? BacktankUtil.getAllWithAir(living).stream().findFirst().orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
            if (!tank.isEmpty()) {
                BacktankUtil.consumeAir(living, tank, 1);
                continue;
            }
            living.hurt(level.damageSources().magic(), 1.0F);
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            if (poisons) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            }
        }
    }

    /** Once a second, each player's hands are checked for beryllium dust, and their surroundings searched for a basin with a fuming acid
     * or beryllium dust in it, and for a vat with cryolite in it. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos at = player.blockPosition();
        if (player.getMainHandItem().is(BERYLLIUM_DUSTS) || player.getOffhandItem().is(BERYLLIUM_DUSTS)) {
            berylliumDust(level, at);
        }
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-REACH - 1, -REACH, -REACH - 1), at.offset(REACH + 1, REACH, REACH + 1))) {
            if (level.getBlockEntity(pos) instanceof VatBlockEntity vat && vat.isController()
                    && IntStream.range(0, vat.inputInventory.getSlots()).anyMatch(i -> vat.inputInventory.getStackInSlot(i).is(Uses.cryolite()))) {
                fluorideFume(level, pos.immutable());
                continue;
            }
            if (!(level.getBlockState(pos).getBlock() instanceof BasinBlock)) {
                continue;
            }
            IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
            if (items != null && IntStream.range(0, items.getSlots()).anyMatch(i -> items.getStackInSlot(i).is(BERYLLIUM_DUSTS))) {
                berylliumDust(level, pos.immutable());
            }
            IFluidHandler tanks = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
            if (tanks == null) {
                continue;
            }
            for (int i = 0; i < tanks.getTanks(); i++) {
                Acids.Acid acid = fuming(tanks.getFluidInTank(i).getFluid());
                if (acid != null) {
                    fume(level, pos.immutable(), acid);
                    break;
                }
            }
        }
    }

    private static Acids.Acid fuming(Fluid fluid) {
        for (Acids.Acid acid : Acids.all().values()) {
            if (acid.fumes && (acid.source == fluid || acid.flowing == fluid)) {
                return acid;
            }
        }
        return null;
    }

    // ---- corrosion ----

    /** Called every {@link #PIPE_INTERVAL} ticks for every pipe by the mixin on Create's fluid transport: an acid in a corrodible
     * pipe may burst it. True if it did. */
    public static boolean corrode(Level level, BlockPos pos, BlockState state, Collection<PipeConnection> connections) {
        if (level.isClientSide) {
            return false;
        }
        FluidStack eating = null;
        for (PipeConnection connection : connections) {
            FluidStack stack = connection.getProvidedFluid();
            if (stack.isEmpty()) {
                continue;
            }
            Reagents.Kind kind = Separation.kind(stack.getFluid());
            if (!eats(kind, state)) {
                continue;
            }
            if (kind == Reagents.Kind.ACID) {
                eating = stack;
                break;
            }
            if (eating == null) {
                eating = stack;
            }
        }
        if (eating == null || level.random.nextDouble() >= 1 - Math.pow(1 - chance(eating), PIPE_INTERVAL)) {
            return false;
        }
        FluidStack spilled = eating;
        Acids.Acid acid = Acids.all().values().stream().filter(a -> a.source == spilled.getFluid() || a.flowing == spilled.getFluid()).findFirst().orElse(null);
        level.destroyBlock(pos, false);
        if (acid != null) {
            level.setBlock(pos, acid.block.defaultBlockState().setValue(LiquidBlock.LEVEL, 6), Block.UPDATE_ALL);
        }
        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.9F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        }
        return true;
    }

    /** Called every tick for every Create fluid tank by its mixin, and acts once per tank on the controller: when the wall goes,
     * one wetted block of it fails, the tank loses that block's share of what it holds, and an acid spills where it stood. True if it did. */
    public static boolean corrodeTank(FluidTankBlockEntity tank) {
        Level level = tank.getLevel();
        if (level == null || level.isClientSide || !tank.isController()) {
            return false;
        }
        FluidStack held = tank.getTankInventory().getFluid();
        if (held.isEmpty() || !eats(Separation.kind(held.getFluid()), tank.getBlockState())
                || level.random.nextDouble() >= chance(held) / TANK_WALL) {
            return false;
        }
        int width = tank.getWidth();
        int wetted = Math.max(1, (int) Math.ceil(tank.getFillState() * tank.getHeight()));
        BlockPos pos = tank.getBlockPos().offset(level.random.nextInt(width), level.random.nextInt(wetted), level.random.nextInt(width));
        Fluid fluid = held.getFluid();
        tank.getTankInventory().drain(held.getAmount() / (width * width * tank.getHeight()), IFluidHandler.FluidAction.EXECUTE);
        Acids.Acid acid = Acids.all().values().stream().filter(a -> a.source == fluid || a.flowing == fluid).findFirst().orElse(null);
        level.destroyBlock(pos, false);
        if (acid != null) {
            level.setBlock(pos, acid.block.defaultBlockState().setValue(LiquidBlock.LEVEL, 6), Block.UPDATE_ALL);
        }
        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.9F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        }
        return true;
    }

    public static boolean corrodes(Reagents.Kind kind) {
        return kind == Reagents.Kind.ACID || kind == Reagents.Kind.LIQUOR || kind == Reagents.Kind.CRUDE || kind == Reagents.Kind.WASTE
                || kind == Reagents.Kind.SALINE || kind == Reagents.Kind.WATER;
    }

    public static double chance(FluidStack carried) {
        Reagents.Kind kind = Separation.kind(carried.getFluid());
        return kind == Reagents.Kind.ACID ? corrosionChance : kind == Reagents.Kind.WATER ? seawaterCorrosionChance
                : corrodes(kind) || kind == Reagents.Kind.CAUSTIC ? liquorCorrosionChance : 0;
    }

    /** Whether a fluid of this kind eats a pipe, pump, valve or tank of this block: caustic only aluminium, the rest what corrodes. */
    public static boolean eats(@Nullable Reagents.Kind kind, BlockState state) {
        return kind == Reagents.Kind.CAUSTIC ? ALUMINIUM.computeIfAbsent(state.getBlock(), Hazards::aluminium) : corrodes(kind) && corrodible(state);
    }

    private static boolean aluminium(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return corrodible(block) && id.getNamespace().equals("tfmg") && id.getPath().contains("aluminum");
    }

    /** Create's pipes and tanks are copper and TFMG's metal pipes and tanks are metal, windowed or not; only plastic stands up to
     * acid, and a creative tank to anything. TFMG's pipes and our dyed ones are Create's pipe classes underneath, so plastic is told
     * apart by name. */
    public static boolean corrodible(BlockState state) {
        return CORRODIBLE.computeIfAbsent(state.getBlock(), Hazards::corrodible);
    }

    private static boolean corrodible(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        boolean plastic = (id.getNamespace().equals("tfmg") || id.getNamespace().equals(Fundamentals.MOD_ID)) && id.getPath().contains("plastic");
        if (plastic || block instanceof PlasticTankBlock || block == AllBlocks.CREATIVE_FLUID_TANK.get()) {
            return false;
        }
        if (block instanceof FluidTankBlock || id.getNamespace().equals("tfmg") && id.getPath().endsWith("fluid_tank")) {
            return true;
        }
        if (block instanceof FluidPipeBlock || block instanceof AxisPipeBlock || block instanceof EncasedPipeBlock || block instanceof SmartFluidPipeBlock
                || block instanceof PumpBlock || block instanceof FluidValveBlock) {
            return true;
        }
        return id.getNamespace().equals("tfmg") && id.getPath().contains("pipe");
    }
}
