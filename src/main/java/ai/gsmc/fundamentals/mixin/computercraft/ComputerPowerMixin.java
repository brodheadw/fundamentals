package ai.gsmc.fundamentals.mixin.computercraft;

import ai.gsmc.fundamentals.compute.Power;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractComputerBlockEntity.class)
public abstract class ComputerPowerMixin {
    @Inject(method = "serverTick", at = @At("RETURN"))
    private void fundamentals$needsPower(CallbackInfo ci) {
        Power.tick((AbstractComputerBlockEntity) (Object) this);
    }
}
