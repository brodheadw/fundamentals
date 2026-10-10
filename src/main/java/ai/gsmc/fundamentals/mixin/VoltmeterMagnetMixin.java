package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.magnet.MagnetBehaviour;
import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.drmangotea.tfmg.content.electricity.measurement.VoltMeterBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = VoltMeterBlockEntity.class, remap = false)
public abstract class VoltmeterMagnetMixin {

    @Inject(method = "getUnit", at = @At("RETURN"), cancellable = true)
    private void fundamentals$magnet(IElectric electric, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValueF() * MagnetBehaviour.field((BlockEntity) (Object) this));
    }
}
