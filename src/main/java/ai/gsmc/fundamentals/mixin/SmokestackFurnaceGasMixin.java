package ai.gsmc.fundamentals.mixin;

import com.drmangotea.tfmg.registry.TFMGFluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// the coke ovens' off-gas is furnace gas, which a stack flares off; the column passes it on burnt, as carbon dioxide
@Mixin(targets = "com.drmangotea.tfmg.content.machinery.misc.smokestack.SmokestackBlockEntity$1", remap = false)
public abstract class SmokestackFurnaceGasMixin {

    @Inject(method = "isFluidValid", at = @At("RETURN"), cancellable = true)
    private void fundamentals$flare(FluidStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getFluid().isSame(TFMGFluids.FURNACE_GAS.getSource())) {
            cir.setReturnValue(true);
        }
    }
}
