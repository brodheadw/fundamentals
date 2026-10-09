package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.separation.Seawater;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A pump drawing through an open pipe end from a water source in the sea draws seawater: Create's pumps and The Factory Must Grow's, which are Create's underneath. */
@Mixin(value = OpenEndedPipe.class, remap = false)
public abstract class SeawaterPipeMixin {

    @Shadow
    private Level world;
    @Shadow
    private BlockPos outputPos;
    @Unique
    private boolean fundamentals$sea;

    // The draw may leave the block flowing, so whether it was a sea source is read before it.
    @Inject(method = "removeFluidFromSpace", at = @At("HEAD"))
    private void fundamentals$look(boolean simulate, CallbackInfoReturnable<FluidStack> cir) {
        fundamentals$sea = world != null && world.isLoaded(outputPos) && Seawater.seaSource(world, outputPos, world.getBlockState(outputPos));
    }

    @Inject(method = "removeFluidFromSpace", at = @At("RETURN"), cancellable = true)
    private void fundamentals$seawater(boolean simulate, CallbackInfoReturnable<FluidStack> cir) {
        if (fundamentals$sea) {
            cir.setReturnValue(Seawater.drawn(cir.getReturnValue()));
        }
    }
}
