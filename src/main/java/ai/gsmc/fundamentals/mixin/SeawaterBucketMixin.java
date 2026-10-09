package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.separation.Seawater;
import ai.gsmc.fundamentals.separation.Separation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A bucket, or a dispenser's, filled at a water source in the sea comes up with seawater. */
@Mixin(LiquidBlock.class)
public abstract class SeawaterBucketMixin {

    @Inject(method = "pickupBlock", at = @At("RETURN"), cancellable = true)
    private void fundamentals$seawater(Player player, LevelAccessor level, BlockPos pos, BlockState state, CallbackInfoReturnable<ItemStack> cir) {
        if (cir.getReturnValue().is(Items.WATER_BUCKET) && Seawater.seaSource(level, pos, state)) {
            cir.setReturnValue(new ItemStack(Separation.seawaterBucket()));
        }
    }
}
