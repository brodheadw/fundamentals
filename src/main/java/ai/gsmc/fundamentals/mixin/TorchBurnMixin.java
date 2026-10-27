package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.torch.Torches;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Torches take random ticks, so a torch can burn down; see {@link Torches}. */
@Mixin(BlockBehaviour.class)
abstract class TorchBurnMixin {

    // by class, since the state cache asks before Blocks.TORCH is assigned; soul torches tick too but nothing comes of it
    @Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true)
    private void fundamentals$torchesTick(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        Class<?> type = state.getBlock().getClass();
        if (type == TorchBlock.class || type == WallTorchBlock.class) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"))
    private void fundamentals$burnDown(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (Torches.burns(state)) {
            Torches.tick(state, level, pos);
        }
    }
}
