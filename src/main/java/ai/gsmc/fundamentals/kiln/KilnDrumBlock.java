package ai.gsmc.fundamentals.kiln;

import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** One length of a rotary kiln's drum: a steel tube lined with firebrick, turning on its long axis and passing the turn along. */
public class KilnDrumBlock extends HorizontalAxisKineticBlock implements IBE<KilnDrumBlockEntity> {

    public KilnDrumBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(HORIZONTAL_AXIS);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    // a charge can be shovelled in by hand at the feed end, as well as by funnel or chute
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return onBlockEntityUseItemOn(level, pos, drum -> {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            ItemStack left = drum.insert(stack, false);
            if (left.getCount() == stack.getCount()) return ItemInteractionResult.FAIL;
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, left);
            return ItemInteractionResult.SUCCESS;
        });
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<KilnDrumBlockEntity> getBlockEntityClass() {
        return KilnDrumBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends KilnDrumBlockEntity> getBlockEntityType() {
        return Kiln.drumEntity();
    }
}
