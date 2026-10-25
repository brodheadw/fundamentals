package ai.gsmc.fundamentals.glass;

import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A 3x3 sliding blast door, placed whole from one item. Each of the nine blocks is a part; the bottom middle one is the
 * controller, which holds the motor's block entity and draws from The Factory's grid. Live grid: the leaves slide apart.
 * Grid cut: its accumulator shuts them. Redstone and hands do nothing.
 */
public class BlastDoorBlock extends Block implements IBE<BlastDoorBlockEntity> {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** Row times three plus column: column 1 is the middle, row 0 the bottom. */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final int CONTROLLER = 1;

    public BlastDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, CONTROLLER).setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OPEN);
    }

    /** Where part {@code part} of a door whose controller is at {@code controller} stands. */
    public static BlockPos partPos(BlockPos controller, Direction facing, int part) {
        return controller.relative(facing.getClockWise(), part % 3 - 1).above(part / 3);
    }

    public static BlockPos controller(BlockPos pos, BlockState state) {
        int part = state.getValue(PART);
        return pos.relative(state.getValue(FACING).getClockWise(), 1 - part % 3).below(part / 3);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (int part = 0; part < 9; part++) {
            BlockPos at = partPos(context.getClickedPos(), facing, part);
            if (!context.getLevel().getBlockState(at).canBeReplaced(context) || !context.getLevel().isInWorldBounds(at)) return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        place(level, pos, state.getValue(FACING));
    }

    /** Stands the eight other parts round a placed controller. */
    public static void place(Level level, BlockPos controller, Direction facing) {
        BlockState base = Glass.blastDoor().defaultBlockState().setValue(FACING, facing);
        for (int part = 0; part < 9; part++) {
            if (part != CONTROLLER) level.setBlock(partPos(controller, facing, part), base.setValue(PART, part), 3);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (state.getValue(PART) == CONTROLLER && !oldState.is(this)) withBlockEntityDo(level, pos, IElectric::onPlaced);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (newState.is(this)) return;
        IBE.onRemove(state, level, pos, newState);
        // One part gone takes the whole door: only the part broken drops the door.
        BlockPos controller = controller(pos, state);
        for (int part = 0; part < 9; part++) {
            BlockPos at = partPos(controller, state.getValue(FACING), part);
            BlockState there = level.getBlockState(at);
            if (!at.equals(pos) && there.is(this) && there.getValue(FACING) == state.getValue(FACING) && there.getValue(PART) == part) {
                level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(OPEN);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == CONTROLLER ? IBE.super.newBlockEntity(pos, state) : null;
    }

    @Override
    public Class<BlastDoorBlockEntity> getBlockEntityClass() {
        return BlastDoorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BlastDoorBlockEntity> getBlockEntityType() {
        return Glass.blastDoorEntity();
    }
}
