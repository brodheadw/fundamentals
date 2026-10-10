package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.liquid.Liquids;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.fluids.FluidNetwork;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A pump's network moves nothing while what it draws has frozen at either end of its first pipe, looked at once a second, and moves
 * a viscous fluid slower. */
@Mixin(value = FluidNetwork.class, remap = false)
public abstract class FluidNetworkMixin {

    @Shadow
    Level world;
    @Shadow
    BlockFace start;
    @Shadow
    FluidStack fluid;
    @Unique
    private long fundamentals$looked = Long.MIN_VALUE;
    @Unique
    private boolean fundamentals$frozen;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void fundamentals$frozen(CallbackInfo ci) {
        if (world == null || world.isClientSide || fluid.isEmpty()) {
            return;
        }
        long now = world.getGameTime();
        if (now - fundamentals$looked >= Liquids.INTERVAL) {
            fundamentals$looked = now;
            fundamentals$frozen = Liquids.frozen(world, start.getPos(), fluid.getFluid()) || Liquids.frozen(world, start.getConnectedPos(), fluid.getFluid());
        }
        if (fundamentals$frozen) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/fluids/FluidNetwork;transferSpeed:I", opcode = Opcodes.GETFIELD))
    private int fundamentals$viscous(int speed) {
        return fluid.isEmpty() ? speed : Math.max(1, (int) Math.round(speed * Liquids.pumping(fluid.getFluid())));
    }
}
