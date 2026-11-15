package ai.gsmc.fundamentals.bombs;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A charge, primed as TNT is into a burning entity that carries this block's look. What sets each one off is what sets the real
 * thing off: black powder and dynamite take a flame; cast TNT, C-4 and the bombs only burn in a fire and need their cap or
 * detonator fired by a signal; ANFO takes no cap at all and goes only when a booster's blast reaches it, and not at all wet.
 */
public class ExplosiveBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    /**
     * power is vanilla's explosion power (its TNT is 4); brisance scales how hard the blast cuts: under one it shatters what a
     * heaving powder only lifts. radius, for the fission bombs, is the fireball's in blocks.
     */
    public enum Kind {
        POWDER_KEG(3F, 80, true, true, 1.6F, 0),
        DYNAMITE(5F, 60, true, true, 1.0F, 0),
        TNT_CHARGE(6F, 80, false, true, 0.8F, 0),
        ANFO_CHARGE(8F, 10, false, false, 1.0F, 0),
        C4_CHARGE(5F, 40, false, true, 0.4F, 0),
        GUN_TYPE_BOMB(0F, 200, false, true, 1.0F, 24),
        IMPLOSION_BOMB(0F, 200, false, true, 1.0F, 32);

        final float power, brisance;
        final int fuse, radius;
        final boolean flame, signal;

        Kind(float power, int fuse, boolean flame, boolean signal, float brisance, int radius) {
            this.power = power;
            this.fuse = fuse;
            this.flame = flame;
            this.signal = signal;
            this.brisance = brisance;
            this.radius = radius;
        }

        public boolean nuclear() {
            return radius > 0;
        }
    }

    private static final Map<Direction, VoxelShape> PLATE = new EnumMap<>(Map.of(
            Direction.UP, box(3, 0, 3, 13, 3, 13), Direction.DOWN, box(3, 13, 3, 13, 16, 13),
            Direction.NORTH, box(3, 3, 13, 13, 13, 16), Direction.SOUTH, box(3, 3, 0, 13, 13, 3),
            Direction.EAST, box(0, 3, 3, 3, 13, 13), Direction.WEST, box(13, 3, 3, 16, 13, 13)));

    private final Kind kind;

    public ExplosiveBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(properties -> new ExplosiveBlock(kind, properties));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = kind == Kind.C4_CHARGE ? context.getClickedFace()
                : kind.nuclear() ? context.getHorizontalDirection() : Direction.UP;
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind == Kind.C4_CHARGE ? PLATE.get(state.getValue(FACING)) : Shapes.block();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!old.is(state.getBlock()) && kind.signal && level.hasNeighborSignal(pos)) prime(level, pos, state, null, kind.fuse);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        if (kind.signal && level.hasNeighborSignal(pos)) prime(level, pos, state, null, kind.fuse);
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, Direction face, LivingEntity igniter) {
        if (kind.flame) prime(level, pos, state, igniter, kind.fuse);
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return !kind.nuclear();
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return kind.nuclear() ? 0 : kind.flame ? 15 : 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return kind.nuclear() ? 0 : kind.flame ? 100 : 5;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (!kind.flame) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.fundamentals.bomb.needs_signal"), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        prime(level, pos, state, player, kind.fuse);
        if (stack.is(Items.FLINT_AND_STEEL)) stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        else stack.consume(1, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (kind.flame && !level.isClientSide && projectile.isOnFire() && projectile.mayInteract(level, hit.getBlockPos())) {
            prime(level, hit.getBlockPos(), state, projectile.getOwner() instanceof LivingEntity owner ? owner : null, kind.fuse);
        }
    }

    /** A blast that reaches a charge sets it off sympathetically; a bomb's pit or core is only broken and scattered. */
    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!(level instanceof ServerLevel server)) return;
        if (kind.nuclear()) {
            Fallout.get(server).add(pos, 8, Fallout.DIRTY_TICKS);
            return;
        }
        if (kind == Kind.ANFO_CHARGE && wet(level, pos)) return;
        int fuse = Math.max(1, kind.fuse / 8 + level.random.nextInt(Math.max(1, kind.fuse / 4)));
        spawn(server, pos, defaultBlockState(), null, fuse);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    static boolean wet(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            FluidState fluid = level.getFluidState(pos.relative(side));
            if (fluid.is(FluidTags.WATER)) return true;
        }
        return level.isRainingAt(pos.above());
    }

    void prime(Level level, BlockPos pos, BlockState state, LivingEntity igniter, int fuse) {
        if (!(level instanceof ServerLevel server)) return;
        level.removeBlock(pos, false);
        spawn(server, pos, state, igniter, fuse);
    }

    static void spawn(ServerLevel level, BlockPos pos, BlockState state, LivingEntity igniter, int fuse) {
        Charge charge = new Charge(Bombs.charge(), level);
        charge.arm(pos, state, fuse, igniter);
        level.addFreshEntity(charge);
        level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1F, 1F);
    }

    /** What the charge does when its fuse is out. */
    void detonate(ServerLevel level, Vec3 at, Charge source) {
        if (kind.nuclear()) {
            Detonation.start(level, BlockPos.containing(at), kind.radius);
            return;
        }
        float brisance = kind.brisance;
        ExplosionDamageCalculator cutting = new ExplosionDamageCalculator() {
            @Override
            public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter getter, BlockPos pos, BlockState block, FluidState fluid) {
                return super.getBlockExplosionResistance(explosion, getter, pos, block, fluid).map(r -> r * brisance);
            }
        };
        level.explode(source, null, cutting, at.x, at.y, at.z, kind.power, false, Level.ExplosionInteraction.TNT);
        if (kind == Kind.POWDER_KEG) level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y, at.z, 40, 1.5, 1.0, 1.5, 0.02);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fundamentals.bomb." + kind.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
    }
}
