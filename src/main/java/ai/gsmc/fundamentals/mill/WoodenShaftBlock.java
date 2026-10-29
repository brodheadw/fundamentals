package ai.gsmc.fundamentals.mill;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class WoodenShaftBlock extends ShaftBlock {

    public WoodenShaftBlock(Properties properties) {
        super(properties);
    }

    // Create turns a shaft placed into a steam engine into its iron crankshaft; an oak one stays oak
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null || state.is(this)) {
            return state;
        }
        return defaultBlockState().setValue(AXIS, state.getValue(AXIS))
                .setValue(BlockStateProperties.WATERLOGGED, state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false));
    }

    @Override
    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return WoodenMill.entity();
    }
}
