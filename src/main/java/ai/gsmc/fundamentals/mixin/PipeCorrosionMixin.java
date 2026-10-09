package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.separation.Hazards;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Once a second of a Create pipe's fluid transport, the acid it carries gets its chance to eat it; a burst pipe ticks no further. */
@Mixin(value = FluidTransportBehaviour.class, remap = false)
public abstract class PipeCorrosionMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void fundamentals$corrode(CallbackInfo ci) {
        SmartBlockEntity entity = ((BlockEntityBehaviour) (Object) this).blockEntity;
        Level level = entity == null ? null : entity.getLevel();
        if (level == null || level.isClientSide || Math.floorMod(level.getGameTime() + entity.getBlockPos().asLong(), Hazards.PIPE_INTERVAL) != 0) {
            return;
        }
        FluidTransportBehaviour self = (FluidTransportBehaviour) (Object) this;
        if (self.interfaces != null && !self.interfaces.isEmpty()
                && Hazards.corrode(level, entity.getBlockPos(), entity.getBlockState(), self.interfaces.values())) {
            ci.cancel();
        }
    }
}
