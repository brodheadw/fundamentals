package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.magnet.MagnetBehaviour;
import com.drmangotea.tfmg.content.electricity.generators.large_generator.RotorBlockEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = RotorBlockEntity.class, remap = false)
public abstract class RotorMagnetMixin {

    @Shadow
    List<BlockPos> stators;

    @Inject(method = "generation", at = @At("RETURN"), cancellable = true)
    private void fundamentals$magnet(CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() == 0 || stators.isEmpty()) {
            return;
        }
        RotorBlockEntity rotor = (RotorBlockEntity) (Object) this;
        float sum = 0;
        for (BlockPos stator : stators) {
            sum += MagnetBehaviour.output(rotor.getLevel().getBlockEntity(stator));
        }
        cir.setReturnValue((int) (cir.getReturnValueI() * sum / stators.size()));
    }
}
