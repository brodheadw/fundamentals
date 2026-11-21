package ai.gsmc.fundamentals.survey;

import ai.gsmc.fundamentals.heat.ThermometerBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** A surveying instrument facing the way it was placed: the seismometer on a wall or floor, the radio dish at the sky. */
public class SurveyBlock extends BaseEntityBlock {

    private final Supplier<BlockEntityType<? extends SurveyBlockEntity>> type;
    private final BiFunction<BlockPos, BlockState, SurveyBlockEntity> factory;
    private final boolean gauge;

    public SurveyBlock(Properties properties, Supplier<BlockEntityType<? extends SurveyBlockEntity>> type,
                       BiFunction<BlockPos, BlockState, SurveyBlockEntity> factory, boolean gauge) {
        super(properties);
        this.type = type;
        this.factory = factory;
        this.gauge = gauge;
        registerDefaultState(stateDefinition.any().setValue(ThermometerBlock.FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ThermometerBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ThermometerBlock.FACING, gauge ? context.getClickedFace() : aim(context));
    }

    /** A dish is aimed up, or toward the horizon the placer faces, tilted halfway up the sky. */
    private static Direction aim(BlockPlaceContext context) {
        return context.getPlayer() != null && context.getPlayer().getXRot() < -30 || context.getClickedFace() == Direction.DOWN
                ? Direction.UP : context.getHorizontalDirection();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return gauge ? ThermometerBlock.shape(state.getValue(ThermometerBlock.FACING)) : Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SurveyBlockEntity instrument ? instrument.signal() : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        BlockEntityTicker<SurveyBlockEntity> ticker = level.isClientSide ? SurveyBlockEntity::clientTick : SurveyBlockEntity::serverTick;
        return blockEntityType == type.get() ? (BlockEntityTicker<T>) ticker : null;
    }
}
