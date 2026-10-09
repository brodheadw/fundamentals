package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.magnet.MagnetBehaviour;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** The Factory's motor and generator inherit their goggles from Create's generating block entity; a magnet adds its lines there. */
@Mixin(value = GeneratingKineticBlockEntity.class, remap = false)
public abstract class MagnetGogglesMixin {

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
    private void fundamentals$magnet(List<Component> tooltip, boolean isPlayerSneaking, CallbackInfoReturnable<Boolean> cir) {
        MagnetBehaviour magnet = BlockEntityBehaviour.get((BlockEntity) (Object) this, MagnetBehaviour.TYPE);
        if (magnet != null) {
            magnet.addToGoggleTooltip(tooltip);
            cir.setReturnValue(true);
        }
    }
}
