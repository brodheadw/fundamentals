package ai.gsmc.fundamentals.power;

import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A resonant transformer on The Factory's grid: fed, it charges its top-load and arcs into the nearest enemy in reach. */
public class TeslaCoilBlock extends Block implements IBE<TeslaCoilBlockEntity> {
    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 16, 13);

    public TeslaCoilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
    public Class<TeslaCoilBlockEntity> getBlockEntityClass() {
        return TeslaCoilBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TeslaCoilBlockEntity> getBlockEntityType() {
        return Electricity.teslaCoilEntity();
    }
}
