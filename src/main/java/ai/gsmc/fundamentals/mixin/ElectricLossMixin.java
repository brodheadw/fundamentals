package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.power.Losses;
import com.drmangotea.tfmg.content.electricity.base.ElectricBlockEntity;
import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.drmangotea.tfmg.content.electricity.base.KineticElectricBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {ElectricBlockEntity.class, KineticElectricBlockEntity.class}, remap = false)
public abstract class ElectricLossMixin {

    @Inject(method = "lazyTick", at = @At("TAIL"))
    private void fundamentals$losses(CallbackInfo ci) {
        Losses.report((IElectric) this);
    }
}
