package ai.gsmc.fundamentals.separation;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
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

import java.util.Locale;

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
                .setValue(ABOVE, false).setValue(BELOW, false).setValue(ROWS, Rows.SINGLE).setValue(OPEN, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT, FRONT, BACK, ABOVE, BELOW, ROWS, OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MixerSettlerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Separation.mixerSettlerEntity(), MixerSettlerBlockEntity::serverTick);
    }
}
