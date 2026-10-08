package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.separation.Hazards;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Every tick of a Create pipe's fluid transport, the acid it carries gets its chance to eat it. */
@Mixin(value = FluidTransportBehaviour.class, remap = false)
public abstract class PipeCorrosionMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void fundamentals$corrode(CallbackInfo ci) {
        FluidTransportBehaviour self = (FluidTransportBehaviour) (Object) this;
        BlockEntityBehaviour behaviour = (BlockEntityBehaviour) (Object) this;
        if (self.interfaces == null || behaviour.blockEntity == null || behaviour.blockEntity.getLevel() == null) {
            return;
        }
        List<FluidStack> carried = self.interfaces.values().stream().map(PipeConnection::getProvidedFluid).toList();
        Hazards.corrode(behaviour.blockEntity.getLevel(), behaviour.blockEntity.getBlockPos(), behaviour.blockEntity.getBlockState(), carried);
    }
}
