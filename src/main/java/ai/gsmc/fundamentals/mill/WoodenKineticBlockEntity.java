package ai.gsmc.fundamentals.mill;

import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** An oak shaft or cog: it turns as Create's do, and snaps to sticks when spun too fast or loaded too hard. */
public class WoodenKineticBlockEntity extends BracketedKineticBlockEntity {

    public WoodenKineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide && overloaded(speed, stress)) {
            snap();
        }
    }

    static boolean overloaded(float speed, float stress) {
        return Math.abs(speed) > WoodenMill.MOST_RPM || stress > WoodenMill.MOST_STRESS;
    }

    private void snap() {
        BlockPos pos = getBlockPos();
        int sticks = getBlockState().is(WoodenMill.largeCogwheel()) ? 3 : 2;
        level.levelEvent(2001, pos, Block.getId(getBlockState()));
        level.playSound(null, pos, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 0.6F, 1.2F);
        level.removeBlock(pos, false);
        Block.popResource(level, pos, new ItemStack(Items.STICK, sticks));
    }
}
