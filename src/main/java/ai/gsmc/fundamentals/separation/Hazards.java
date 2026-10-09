package ai.gsmc.fundamentals.separation;

import ai.gsmc.fundamentals.Fundamentals;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
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

import java.util.List;

/**
 * What the plant does to people and pipes. The fuming acids (hydrofluoric, nitric, aqua regia) hurt anyone within reach of them
 * in the open: as blocks in the world, or in a basin they are being used in. Create's diving helmet on a filled
 * backtank is the gas mask, and breathes its air. And the acids eat copper: Create's pipes, pumps and valves carrying one
 * corrode and eventually burst, spilling it. The liquors are rare earth chlorides in dilute acid and the spent liquor and brine
 * are chloride too, so they eat it as well, more slowly. Metal tanks go the same way, ten times slower for the thicker wall.
 * Plastic pipes, pumps, valves and tanks, and glass pipes, do not corrode.
 */
public final class Hazards {

    /** How far a fuming source reaches. */
    private static final int REACH = 2;
    /** Per tick, for a pipe carrying an acid: on average a pipe lasts two minutes. */
    public static double corrosionChance = 1.0 / 2400;
    /** Per tick, for a pipe carrying a liquor, crude liquor, spent liquor or brine: on average eight minutes. */
    public static double liquorCorrosionChance = 1.0 / 9600;
    /** How many times longer a tank's wall lasts than a pipe's, for the same fluid: twenty minutes under acid, eighty under a liquor. */
    private static final int TANK_WALL = 10;

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

    /** Once a second, each player's surroundings are searched for a basin with a fuming acid in it. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos at = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-REACH - 1, -REACH, -REACH - 1), at.offset(REACH + 1, REACH, REACH + 1))) {
            if (!(level.getBlockState(pos).getBlock() instanceof BasinBlock)) {
                continue;
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

    /** Called every tick for every pipe by the mixin on Create's fluid transport: an acid in a corrodible pipe may burst it. */
    public static void corrode(Level level, BlockPos pos, BlockState state, List<FluidStack> carried) {
        if (level.isClientSide || !corrodible(state)) {
            return;
        }
        FluidStack eating = null;
        for (FluidStack stack : carried) {
            if (stack.isEmpty()) {
                continue;
            }
            Reagents.Kind kind = Separation.kind(stack.getFluid());
            if (kind == Reagents.Kind.ACID) {
                eating = stack;
                break;
            }
            if (eating == null && corrodes(kind)) {
                eating = stack;
            }
        }
        if (eating == null || level.random.nextDouble() >= chance(eating)) {
            return;
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
    }

    /** Called every tick for every Create fluid tank by its mixin, and acts once per tank on the controller: when the wall goes,
     * one wetted block of it fails, the tank loses that block's share of what it holds, and an acid spills where it stood. */
    public static void corrodeTank(FluidTankBlockEntity tank) {
        Level level = tank.getLevel();
        if (level == null || level.isClientSide || !tank.isController()) {
            return;
        }
        FluidStack held = tank.getTankInventory().getFluid();
        if (held.isEmpty() || !corrodes(Separation.kind(held.getFluid())) || !corrodible(tank.getBlockState())
                || level.random.nextDouble() >= chance(held) / TANK_WALL) {
            return;
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
    }

    public static boolean corrodes(Reagents.Kind kind) {
        return kind == Reagents.Kind.ACID || kind == Reagents.Kind.LIQUOR || kind == Reagents.Kind.CRUDE || kind == Reagents.Kind.WASTE;
    }

    public static double chance(FluidStack carried) {
        Reagents.Kind kind = Separation.kind(carried.getFluid());
        return kind == Reagents.Kind.ACID ? corrosionChance : corrodes(kind) ? liquorCorrosionChance : 0;
    }

    /** Create's pipes and tanks are copper and TFMG's metal pipes and tanks are metal; only plastic and glass stand up to acid,
     * and a creative tank to anything. TFMG's pipes and our dyed ones are Create's pipe classes underneath, so plastic is told apart by name. */
    public static boolean corrodible(BlockState state) {
        Block block = state.getBlock();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        boolean plastic = (id.getNamespace().equals("tfmg") || id.getNamespace().equals(Fundamentals.MOD_ID)) && id.getPath().contains("plastic");
        if (plastic || block instanceof GlassFluidPipeBlock || block instanceof PlasticTankBlock
                || AllBlocks.CREATIVE_FLUID_TANK.has(state)) {
            return false;
        }
        if (block instanceof FluidTankBlock || id.getNamespace().equals("tfmg") && id.getPath().endsWith("fluid_tank")) {
            return true;
        }
        if (block instanceof FluidPipeBlock || block instanceof AxisPipeBlock || block instanceof EncasedPipeBlock || block instanceof SmartFluidPipeBlock
                || block instanceof PumpBlock || block instanceof FluidValveBlock) {
            return true;
        }
        return id.getNamespace().equals("tfmg") && id.getPath().contains("pipe") && !id.getPath().contains("glass");
    }
}
