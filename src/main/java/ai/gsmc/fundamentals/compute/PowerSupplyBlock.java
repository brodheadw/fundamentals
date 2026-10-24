package ai.gsmc.fundamentals.compute;

import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class PowerSupplyBlock extends Block implements IBE<PowerSupplyBlockEntity> {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public PowerSupplyBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!oldState.is(this)) withBlockEntityDo(level, pos, IElectric::onPlaced);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<PowerSupplyBlockEntity> getBlockEntityClass() {
        return PowerSupplyBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PowerSupplyBlockEntity> getBlockEntityType() {
        return Compute.powerSupplyEntity();
    }
}
