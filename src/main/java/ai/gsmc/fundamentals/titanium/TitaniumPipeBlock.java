package ai.gsmc.fundamentals.titanium;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Create's fluid pipe in titanium: the same pipe, with its own block entity type and model. */
public class TitaniumPipeBlock extends FluidPipeBlock {

    public TitaniumPipeBlock(Properties properties) {
        super(properties);
    }

    // Create's wrench turns a straight pipe into its copper glass pipe; there is no titanium one.
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return tryRemoveBracket(context) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
        return Titanium.pipeEntity();
    }
}
