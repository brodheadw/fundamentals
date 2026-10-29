package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.liquid.Liquid;
import ai.gsmc.fundamentals.liquid.Liquids;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// a melt poured out creeps as its viscosity lets it and sets what it touches alight, as lava does
@Mixin(BaseFlowingFluid.class)
public abstract class MoltenFlowMixin extends FlowingFluid {

    @Inject(method = "getTickDelay", at = @At("HEAD"), cancellable = true)
    private void fundamentals$creep(LevelReader level, CallbackInfoReturnable<Integer> cir) {
        Liquid molten = Liquids.molten(this);
        if (molten != null) {
            cir.setReturnValue(Liquids.flowDelay(molten));
        }
    }

    @Inject(method = "getDropOff", at = @At("HEAD"), cancellable = true)
    private void fundamentals$reach(LevelReader level, CallbackInfoReturnable<Integer> cir) {
        Liquid molten = Liquids.molten(this);
        if (molten != null && Liquids.viscous(molten)) {
            cir.setReturnValue(2);
        }
    }

    @Inject(method = "getSlopeFindDistance", at = @At("HEAD"), cancellable = true)
    private void fundamentals$slope(LevelReader level, CallbackInfoReturnable<Integer> cir) {
        Liquid molten = Liquids.molten(this);
        if (molten != null && Liquids.viscous(molten)) {
            cir.setReturnValue(2);
        }
    }

    @Override
    protected boolean isRandomlyTicking() {
        return Liquids.molten(this) != null;
    }

    @Override
    protected void randomTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        if (Liquids.molten(this) != null) {
            ((FluidInvoker) Fluids.LAVA).fundamentals$randomTick(level, pos, state, random);
        }
    }
}
