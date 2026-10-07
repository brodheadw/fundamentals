package ai.gsmc.fundamentals.separation;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Casing for a solvent-extraction stage. Casings facing the same way merge into one stage as they are
 * placed, any box up to three across, three along and two tall: one is a lab box, eighteen a plant vat.
 * The back row is the mixing trough, stirred by a Mechanical Mixer standing over it; the rows ahead are the
 * settling bay. Stages standing end to end are one battery; the liquor goes in at the back of the first and leaves it as raffinate, the strip acid goes
 * in at the front of the last and leaves loaded with what the organic carried forward.
 */
public class MixerSettlerBlock extends BaseEntityBlock {

    public static final MapCodec<MixerSettlerBlock> CODEC = simpleCodec(MixerSettlerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // Which faces are shared with the rest of the stage, relative to the facing: no wall is drawn there.
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty FRONT = BooleanProperty.create("front");
    public static final BooleanProperty BACK = BooleanProperty.create("back");
    public static final BooleanProperty ABOVE = BooleanProperty.create("above");
    public static final BooleanProperty BELOW = BooleanProperty.create("below");
    public static final EnumProperty<Rows> ROWS = EnumProperty.create("rows", Rows.class);
    /** The hatch: the trough's top-centre casing has no lid, and the Mechanical Mixer stands over it. */
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    /** Ports in the shared wall to the stage ahead (organic overflows forward) and behind (aqueous drains back). */
    /** The middle casing of an outside wall carries the stage's window, as Create's tanks do. */
    public static final BooleanProperty WINDOW = BooleanProperty.create("window");
    public static final BooleanProperty LINK_AHEAD = BooleanProperty.create("link_ahead");
    public static final BooleanProperty LINK_BEHIND = BooleanProperty.create("link_behind");
    public static final int MAX_ACROSS = 3;
    public static final int MAX_ALONG = 3;
    public static final int MAX_TALL = 2;

    /** Whether a casing is in the trough row, the bay, or a stage only one row long that holds both. */
    public enum Rows implements StringRepresentable {
        SINGLE, WELL, BAY;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public MixerSettlerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(LEFT, false).setValue(RIGHT, false).setValue(FRONT, false).setValue(BACK, false)
                .setValue(ABOVE, false).setValue(BELOW, false).setValue(ROWS, Rows.SINGLE).setValue(OPEN, true)
                .setValue(LINK_AHEAD, false).setValue(LINK_BEHIND, false).setValue(WINDOW, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT, FRONT, BACK, ABOVE, BELOW, ROWS, OPEN, LINK_AHEAD, LINK_BEHIND, WINDOW);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** The vat is open: its floor and whatever walls it has, so a player can climb in and stand in the liquor. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.computeIfAbsent(state, MixerSettlerBlock::shapeOf);
    }

    private static final Map<BlockState, VoxelShape> SHAPES = new HashMap<>();

    private static VoxelShape shapeOf(BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction right = facing.getClockWise();
        VoxelShape shape = state.getValue(BELOW) ? Shapes.empty() : Block.box(0, 0, 1, 16, 1, 16).move(0, 0, -1 / 16.0);
        shape = Shapes.or(shape, state.getValue(BELOW) ? Shapes.empty() : Block.box(0, 0, 0, 16, 1, 16));
        if (!state.getValue(LEFT)) shape = Shapes.or(shape, wall(right.getOpposite()));
        if (!state.getValue(RIGHT)) shape = Shapes.or(shape, wall(right));
        if (!state.getValue(FRONT)) shape = Shapes.or(shape, wall(facing));
        if (!state.getValue(BACK)) shape = Shapes.or(shape, wall(facing.getOpposite()));
        if (state.getValue(ROWS) == Rows.WELL && !state.getValue(BELOW)) shape = Shapes.or(shape, wall(facing));
        return shape;
    }

    private static VoxelShape wall(Direction side) {
        return switch (side) {
            case NORTH -> Block.box(0, 0, 0, 16, 16, 1);
            case SOUTH -> Block.box(0, 0, 15, 16, 16, 16);
            case WEST -> Block.box(0, 0, 0, 1, 16, 16);
            case EAST -> Block.box(15, 0, 0, 16, 16, 16);
            default -> Shapes.empty();
        };
    }

    // Merging during placement would re-enter setBlock on the block being placed, so it waits a tick.
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof MixerSettlerBlockEntity casing) {
            casing.merge();
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !newState.is(this) && level.getBlockEntity(pos) instanceof MixerSettlerBlockEntity casing) {
            casing.dissolve();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Anything in the vat below the liquid's surface is buoyed up to it and slowed, as in water, and anything
     * alive is poisoned by the acid liquor and sickened by the kerosene the extractant rides in.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level.getBlockEntity(pos) instanceof MixerSettlerBlockEntity casing)) {
            return;
        }
        double surface = casing.floorY() + casing.surface();
        if (entity.getY() >= surface) {
            return;
        }
        entity.resetFallDistance();
        Vec3 v = entity.getDeltaMovement();
        double depthBelow = surface - entity.getY();
        // ankle-deep only drags; past the knee the liquid carries you up
        if (depthBelow < 0.5) {
            entity.setDeltaMovement(v.x * 0.85, v.y, v.z * 0.85);
        } else {
            double lift = Math.min(0.08, (depthBelow - 0.5) * 0.08);
            entity.setDeltaMovement(v.x * 0.8, Math.min(v.y + lift, 0.1), v.z * 0.8);
        }
        if (entity instanceof LivingEntity living && !level.isClientSide) {
            if (!casing.stage().aqueous().isEmpty()) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            }
            if (!casing.stage().organic().isEmpty()) {
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            }
            if (level.random.nextInt(20) == 0 && level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.BUBBLE_POP, living.getX(), surface, living.getZ(), 2, 0.2, 0.0, 0.2, 0.0);
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MixerSettlerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Separation.mixerSettlerEntity(), MixerSettlerBlockEntity::serverTick);
    }
}
