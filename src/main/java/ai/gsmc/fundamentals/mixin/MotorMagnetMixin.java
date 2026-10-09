package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.magnet.MagnetBehaviour;
import com.drmangotea.tfmg.content.electricity.utilities.electric_motor.ElectricMotorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A motor turns as fast as its magnets let it: its speed, and with it the stress it can carry, scaled by what the heat has left. */
@Mixin(value = ElectricMotorBlockEntity.class, remap = false)
public abstract class MotorMagnetMixin {

    @Inject(method = "getGeneratedSpeed", at = @At("RETURN"), cancellable = true)
    private void fundamentals$magnet(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValueF() * MagnetBehaviour.output((BlockEntity) (Object) this));
    }
}
