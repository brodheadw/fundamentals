package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.liquid.Liquids;
import ai.gsmc.fundamentals.separation.Hazards;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Once a second of a Create pipe's fluid transport, what it carries gets its chance to eat it, and the heat to soften a plastic one;
 * a burst pipe ticks no further, and nor does one whose water has frozen, until it thaws. */
@Mixin(value = FluidTransportBehaviour.class, remap = false)
public abstract class PipeCorrosionMixin {

    @Unique
    private boolean fundamentals$frozen;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void fundamentals$corrode(CallbackInfo ci) {
        SmartBlockEntity entity = ((BlockEntityBehaviour) (Object) this).blockEntity;
        Level level = entity == null ? null : entity.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        FluidTransportBehaviour self = (FluidTransportBehaviour) (Object) this;
        if (Math.floorMod(level.getGameTime() + entity.getBlockPos().asLong(), Hazards.PIPE_INTERVAL) == 0) {
            boolean carrying = self.interfaces != null && !self.interfaces.isEmpty();
            if (carrying && Hazards.corrode(level, entity.getBlockPos(), entity.getBlockState(), self.interfaces.values())) {
                ci.cancel();
                return;
            }
            fundamentals$frozen = carrying && Liquids.frozen(level, entity.getBlockPos(), self.interfaces.values());
        }
        if (fundamentals$frozen) {
            ci.cancel();
        }
    }
}
