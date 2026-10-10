package ai.gsmc.fundamentals.separation;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.liquid.Liquids;
import ai.gsmc.fundamentals.uses.Uses;
import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
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
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;
import javax.annotation.Nullable;

public final class Hazards {

    private static final TagKey<Item> BERYLLIUM_DUSTS = TagKey.create(Registries.ITEM, Fundamentals.id("beryllium_dusts"));
    private static final TagKey<Item> NICKEL_DUSTS = TagKey.create(Registries.ITEM, Fundamentals.id("nickel_dusts"));
    private static final TagKey<Item> SULFIDES = TagKey.create(Registries.ITEM, Fundamentals.id("sulfides"));

    private static final int REACH = 2;
    public static double corrosionChance = 1.0 / 2400;
    public static double liquorCorrosionChance = 1.0 / 9600;
    public static double seawaterCorrosionChance = 1.0 / 72000;
    public static double fluorideChance = 1.0 / 600;
    public static double softeningChance = 1.0 / 100;
    public static final double PLASTIC_SOFTENS = 110;
    public static final double TITANIUM_HCL = 60;
    private static final int TANK_WALL = 10;
    public static final int PIPE_INTERVAL = 20;
    private static final Map<Block, Wall> WALLS = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> ALUMINIUM = new ConcurrentHashMap<>();
    private static final int CARBONYL_REACH = 3;
    private static final float CARBONYL_DAMAGE = 3.0F;
    public static final int CARBONYL_ONSET = 600;
    private static final int WITHER_PER_DOSE = 80, WITHER_MOST = 1200;
    private static final int LEAK_SECONDS = 3;
    private static final Map<UUID, long[]> CARBONYL_DOSES = new ConcurrentHashMap<>();
    private static final Queue<Leak> LEAKS = new ConcurrentLinkedQueue<>();

    private record Leak(ServerLevel level, BlockPos pos, int[] left) {}

    public enum Wall { METAL, TITANIUM, PLASTIC, PROOF }

    private Hazards() {}

    public static void fume(ServerLevel level, BlockPos source, Acids.Acid acid) {
        breathe(level, source, acid.poisons);
        level.sendParticles(ParticleTypes.WHITE_SMOKE, source.getX() + 0.5, source.getY() + 1.1, source.getZ() + 0.5, 3, 0.3, 0.2, 0.3, 0.01);
    }

    public static void mercuryVapour(ServerLevel level, BlockPos source) {
        breathe(level, source, true);
        level.sendParticles(ParticleTypes.WHITE_SMOKE, source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.01);
    }

    public static void fluorideFume(ServerLevel level, BlockPos source) {
        breathe(level, source, true);
        level.sendParticles(ParticleTypes.WHITE_SMOKE, source.getX() + 0.5, source.getY() + 1.1, source.getZ() + 0.5, 3, 0.3, 0.2, 0.3, 0.01);
    }

    public static void lungDust(ServerLevel level, BlockPos source) {
        breathe(level, source, false);
        level.sendParticles(ParticleTypes.WHITE_ASH, source.getX() + 0.5, source.getY() + 1.0, source.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.01);
    }

    public static void sulfurDioxide(ServerLevel level, BlockPos source) {
        breathe(level, source, false);
        level.sendParticles(ParticleTypes.SMOKE, source.getX() + 0.5, source.getY() + 1.0, source.getZ() + 0.5, 4, 0.2, 0.3, 0.2, 0.01);
    }

    public static void vapour(ServerLevel level, BlockPos source, Fluid fluid, boolean toxic) {
        Acids.Acid acid = Acids.of(fluid);
        if (isCarbonyl(fluid)) {
            nickelCarbonyl(level, source);
        } else if (acid != null && acid.fumes) {
            fume(level, source, acid);
        } else if (toxic) {
            breathe(level, source, true);
        }
        level.sendParticles(ParticleTypes.CLOUD, source.getX() + 0.5, source.getY() + 1.0, source.getZ() + 0.5, 4, 0.25, 0.1, 0.25, 0.01);
    }

    public static void nickelCarbonyl(ServerLevel level, BlockPos source) {
        long onset = level.getGameTime() + CARBONYL_ONSET;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(source).inflate(CARBONYL_REACH))) {
            if (masked(living, 2)) {
                continue;
            }
            living.hurt(level.damageSources().magic(), CARBONYL_DAMAGE);
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0));
            CARBONYL_DOSES.merge(living.getUUID(), new long[] {onset, 1}, (was, more) -> new long[] {was[0], was[1] + 1});
        }
        level.sendParticles(ParticleTypes.WHITE_ASH, source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5, 2, 0.4, 0.4, 0.4, 0.01);
    }

    public static void leak(ServerLevel level, BlockPos pos) {
        LEAKS.add(new Leak(level, pos.immutable(), new int[] {LEAK_SECONDS}));
    }

    public static boolean isCarbonyl(Fluid fluid) {
        return fluid == Separation.fluid("nickel_carbonyl");
    }

    public static void registerPipeEffects() {
        OpenPipeEffectHandler.REGISTRY.register(Separation.fluid("nickel_carbonyl"), (level, area, fluid) -> {
            if (level instanceof ServerLevel server && server.getGameTime() % 20 == 0) {
                nickelCarbonyl(server, BlockPos.containing(area.getCenter()));
            }
        });
    }

    private static void breathe(ServerLevel level, BlockPos source, boolean poisons) {
        AABB box = new AABB(source).inflate(REACH);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (masked(living, 1)) {
                continue;
            }
            living.hurt(level.damageSources().magic(), 1.0F);
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            if (poisons) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            }
        }
    }

    private static boolean masked(LivingEntity living, int air) {
        ItemStack tank = DivingHelmetItem.isWornBy(living) ? BacktankUtil.getAllWithAir(living).stream().findFirst().orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        if (tank.isEmpty()) {
            return false;
        }
        BacktankUtil.consumeAir(living, tank, air);
        return true;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos at = player.blockPosition();
        if (dust(player.getMainHandItem()) || dust(player.getOffhandItem())) {
            lungDust(level, at);
        }
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-REACH - 1, -REACH, -REACH - 1), at.offset(REACH + 1, REACH, REACH + 1))) {
            if (level.getBlockEntity(pos) instanceof VatBlockEntity vat && vat.isController()
                    && IntStream.range(0, vat.inputInventory.getSlots()).anyMatch(i -> vat.inputInventory.getStackInSlot(i).is(Uses.cryolite()))) {
                fluorideFume(level, pos.immutable());
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CampfireBlock || state.getBlock() instanceof AbstractFurnaceBlock) {
                if (roastingSulfide(level, pos, state)) {
                    sulfurDioxide(level, pos.immutable());
                }
                continue;
            }
            if (!(state.getBlock() instanceof BasinBlock)) {
                continue;
            }
            IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
            if (items != null && IntStream.range(0, items.getSlots()).anyMatch(i -> dust(items.getStackInSlot(i)))) {
                lungDust(level, pos.immutable());
            }
            IFluidHandler tanks = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
            if (tanks == null) {
                continue;
            }
            for (int i = 0; i < tanks.getTanks(); i++) {
                Fluid fluid = tanks.getFluidInTank(i).getFluid();
                if (isCarbonyl(fluid)) {
                    nickelCarbonyl(level, pos.immutable());
                    break;
                }
                Acids.Acid acid = Acids.of(fluid);
                if (acid != null && acid.fumes) {
                    fume(level, pos.immutable(), acid);
                    break;
                }
            }
        }
    }

    private static boolean dust(ItemStack stack) {
        return stack.is(BERYLLIUM_DUSTS) || stack.is(NICKEL_DUSTS);
    }

    private static boolean roastingSulfide(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(BlockStateProperties.LIT)) {
            return false;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof CampfireBlockEntity campfire) {
            return campfire.getItems().stream().anyMatch(stack -> stack.is(SULFIDES));
        }
        return entity instanceof AbstractFurnaceBlockEntity furnace && furnace.getItem(0).is(SULFIDES);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        LEAKS.removeIf(leak -> {
            if (Liquids.ignite(leak.level(), leak.pos(), Separation.fluid("nickel_carbonyl"))) {
                return true;
            }
            nickelCarbonyl(leak.level(), leak.pos());
            return --leak.left()[0] <= 0;
        });
        long now = server.overworld().getGameTime();
        CARBONYL_DOSES.entrySet().removeIf(dose -> {
            if (dose.getValue()[0] > now) {
                return false;
            }
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getEntity(dose.getKey()) instanceof LivingEntity living && living.isAlive()) {
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) Math.min(WITHER_PER_DOSE * dose.getValue()[1], WITHER_MOST), 1));
                    break;
                }
            }
            return true;
        });
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && holdsCarbonyl(level, event.getPos())) {
            leak(level, event.getPos());
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            event.getAffectedBlocks().stream().filter(pos -> holdsCarbonyl(level, pos)).forEach(pos -> leak(level, pos));
        }
    }

    private static boolean holdsCarbonyl(Level level, BlockPos pos) {
        FluidTransportBehaviour pipe = BlockEntityBehaviour.get(level, pos, FluidTransportBehaviour.TYPE);
        if (pipe != null && pipe.interfaces != null && pipe.interfaces.values().stream().anyMatch(c -> isCarbonyl(c.getProvidedFluid().getFluid()))) {
            return true;
        }
        IFluidHandler tanks = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        return tanks != null && IntStream.range(0, tanks.getTanks()).anyMatch(i -> isCarbonyl(tanks.getFluidInTank(i).getFluid()));
    }

    public static boolean corrode(Level level, BlockPos pos, BlockState state, Collection<PipeConnection> connections) {
        Wall wall = wall(state);
        if (level.isClientSide || wall == Wall.PROOF) {
            return false;
        }
        FluidStack eating = null;
        double worst = 0, heat = Double.NaN;
        for (PipeConnection connection : connections) {
            FluidStack stack = connection.getProvidedFluid();
            if (stack.isEmpty()) {
                continue;
            }
            if (wall != Wall.METAL && Double.isNaN(heat)) {
                heat = Heat.at(level, pos);
            }
            double chance = chance(state, stack, wall == Wall.METAL ? celsius(stack) : Math.max(heat, celsius(stack)));
            if (chance > worst) {
                worst = chance;
                eating = stack;
            }
        }
        if (eating == null || level.random.nextDouble() >= 1 - Math.pow(1 - worst, PIPE_INTERVAL)) {
            return false;
        }
        burst(level, pos, eating.getFluid());
        return true;
    }

    public static boolean corrodeTank(FluidTankBlockEntity tank) {
        Level level = tank.getLevel();
        if (level == null || level.isClientSide || !tank.isController()) {
            return false;
        }
        FluidStack held = tank.getTankInventory().getFluid();
        BlockState state = tank.getBlockState();
        if (held.isEmpty() || wall(state) == Wall.PROOF) {
            return false;
        }
        double celsius = wall(state) == Wall.METAL ? celsius(held) : Math.max(Heat.at(level, tank.getBlockPos()), celsius(held));
        if (level.random.nextDouble() >= chance(state, held, celsius) / TANK_WALL) {
            return false;
        }
        int width = tank.getWidth();
        int wetted = Math.max(1, (int) Math.ceil(tank.getFillState() * tank.getHeight()));
        BlockPos pos = tank.getBlockPos().offset(level.random.nextInt(width), level.random.nextInt(wetted), level.random.nextInt(width));
        tank.getTankInventory().drain(held.getAmount() / (width * width * tank.getHeight()), IFluidHandler.FluidAction.EXECUTE);
        burst(level, pos, held.getFluid());
        return true;
    }

    private static void burst(Level level, BlockPos pos, Fluid fluid) {
        Acids.Acid acid = Acids.of(fluid);
        level.destroyBlock(pos, false);
        if (acid != null) {
            level.setBlock(pos, acid.block.defaultBlockState().setValue(LiquidBlock.LEVEL, 6), Block.UPDATE_ALL);
        }
        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.9F);
        Liquids.ignite(level, pos, fluid);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        }
    }

    private static double celsius(FluidStack stack) {
        return Liquids.own(stack);
    }

    public static double chance(BlockState state, FluidStack carried, double celsius) {
        return switch (wall(state)) {
            case METAL -> eats(Separation.kind(carried.getFluid()), state) && !(is(carried, "nitric_acid") && aluminium(state)) ? chance(carried) : 0;
            case PLASTIC -> celsius > PLASTIC_SOFTENS ? softeningChance : is(carried, "bromine") ? corrosionChance : 0;
            case TITANIUM -> is(carried, "hydrofluoric_acid") ? fluorideChance
                    : is(carried, "chlorine") || is(carried, "hydrochloric_acid") && celsius > TITANIUM_HCL ? corrosionChance : 0;
            case PROOF -> 0;
        };
    }

    private static boolean is(FluidStack stack, String reagent) {
        Acids.Acid acid = Acids.all().get(reagent);
        Fluid fluid = Separation.reagent(stack.getFluid());
        return acid == null ? fluid == Separation.fluid(reagent) : fluid == acid.source || fluid == acid.flowing;
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

    public static boolean eats(@Nullable Reagents.Kind kind, BlockState state) {
        return kind == Reagents.Kind.CAUSTIC ? aluminium(state) : corrodes(kind) && corrodible(state);
    }

    private static boolean aluminium(BlockState state) {
        return ALUMINIUM.computeIfAbsent(state.getBlock(), Hazards::aluminium);
    }

    private static boolean aluminium(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return WALLS.computeIfAbsent(block, Hazards::wall) == Wall.METAL && id.getNamespace().equals("tfmg") && id.getPath().contains("aluminum");
    }

    public static Wall wall(BlockState state) {
        return WALLS.computeIfAbsent(state.getBlock(), Hazards::wall);
    }

    public static boolean corrodible(BlockState state) {
        return wall(state) == Wall.METAL;
    }

    private static Wall wall(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        boolean ours = id.getNamespace().equals(Fundamentals.MOD_ID), tfmg = id.getNamespace().equals("tfmg");
        if ((tfmg || ours) && id.getPath().contains("plastic") || block instanceof PlasticTankBlock) {
            return Wall.PLASTIC;
        }
        if (ours && id.getPath().startsWith("titanium_")) {
            return Wall.TITANIUM;
        }
        if (block == AllBlocks.CREATIVE_FLUID_TANK.get()) {
            return Wall.PROOF;
        }
        if (block instanceof FluidTankBlock || tfmg && id.getPath().endsWith("fluid_tank")) {
            return Wall.METAL;
        }
        if (block instanceof FluidPipeBlock || block instanceof AxisPipeBlock || block instanceof EncasedPipeBlock || block instanceof SmartFluidPipeBlock
                || block instanceof PumpBlock || block instanceof FluidValveBlock) {
            return Wall.METAL;
        }
        return tfmg && id.getPath().contains("pipe") ? Wall.METAL : Wall.PROOF;
    }
}
