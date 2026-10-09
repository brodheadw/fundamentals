package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.separation.Hazards;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every tick of a Create fluid tank, and of TFMG's, which are Create's tank underneath, what it holds gets its chance to eat it;
 * a tank whose wall has just gone ticks no further. */
@Mixin(value = FluidTankBlockEntity.class, remap = false)
public abstract class TankCorrosionMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void fundamentals$corrode(CallbackInfo ci) {
        if (Hazards.corrodeTank((FluidTankBlockEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
