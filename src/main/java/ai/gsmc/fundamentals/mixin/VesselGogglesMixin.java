package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.liquid.Liquids;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Create's tanks, basins, pumps and valves, and everything built on them, add the properties of what they hold to their goggles. */
@Mixin(value = {FluidTankBlockEntity.class, BasinBlockEntity.class, KineticBlockEntity.class}, remap = false)
public abstract class VesselGogglesMixin {

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
    private void fundamentals$liquid(List<Component> tooltip, boolean isPlayerSneaking, CallbackInfoReturnable<Boolean> cir) {
        BlockEntity self = (BlockEntity) (Object) this;
        Set<Fluid> held = new LinkedHashSet<>();
        if (self instanceof FluidTankBlockEntity tank) {
            FluidTankBlockEntity controller = tank.getControllerBE();
            FluidStack stack = controller == null ? FluidStack.EMPTY : controller.getTankInventory().getFluid();
            if (!stack.isEmpty()) {
                held.add(stack.getFluid());
            }
        } else if (self instanceof BasinBlockEntity basin) {
            basin.getTanks().forEach(tanks -> tanks.forEach(segment -> {
                if (!segment.getRenderedFluid().isEmpty()) {
                    held.add(segment.getRenderedFluid().getFluid());
                }
            }));
        } else {
            Fluid carried = Liquids.carried(BlockEntityBehaviour.get(self, FluidTransportBehaviour.TYPE));
            if (carried != null) {
                held.add(carried);
            }
        }
        boolean said = false;
        for (Fluid fluid : held) {
            said |= Liquids.describe(self.getLevel(), self.getBlockPos(), fluid, tooltip, self instanceof KineticBlockEntity);
        }
        if (said) {
            cir.setReturnValue(true);
        }
    }
}
