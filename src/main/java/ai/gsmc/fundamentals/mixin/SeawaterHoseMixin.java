package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.separation.Seawater;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A hose pulley lowered into the sea draws seawater, read where the hose ends. */
@Mixin(value = FluidDrainingBehaviour.class, remap = false)
public abstract class SeawaterHoseMixin {

    @Inject(method = "getDrainableFluid", at = @At("RETURN"), cancellable = true)
    private void fundamentals$seawater(BlockPos rootPos, CallbackInfoReturnable<FluidStack> cir) {
        Level level = ((BlockEntityBehaviour) (Object) this).getWorld();
        if (level != null && cir.getReturnValue().is(Fluids.WATER) && level.getFluidState(rootPos).is(FluidTags.WATER) && Seawater.sea(level.getBiome(rootPos))) {
            cir.setReturnValue(Seawater.drawn(cir.getReturnValue()));
        }
    }
}
