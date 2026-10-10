package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.magnet.MagnetBehaviour;
import com.drmangotea.tfmg.content.electricity.generators.GeneratorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GeneratorBlockEntity.class, remap = false)
public abstract class GeneratorMagnetMixin {

    @Inject(method = "generation", at = @At("RETURN"), cancellable = true)
    private void fundamentals$magnet(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((int) (cir.getReturnValueI() * MagnetBehaviour.output((BlockEntity) (Object) this)));
    }
}
