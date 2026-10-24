package ai.gsmc.fundamentals.mixin.createnuclear;

import ai.gsmc.fundamentals.content.nuclear.SpentFuel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.nuclearteam.createnuclear.api.multiblock.rods.RodType;
import net.nuclearteam.createnuclear.content.multiblock.controller.consumable.ItemConsumable;
import net.nuclearteam.createnuclear.content.multiblock.controller.manager.ReactorInputManagerI;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// a fuel rod the reactor burns comes back out as spent fuel
@Mixin(value = ItemConsumable.class, remap = false)
public abstract class ReactorSpentFuelMixin {

    @Shadow @Final private RodType rodType;

    @Inject(method = "consume", at = @At("RETURN"))
    private void fundamentals$spentFuel(ReactorInputManagerI inputs, Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !level.isClientSide && rodType != null && rodType.type() == RodType.TypeRod.FUEL) {
            SpentFuel.discharge(inputs.getItemHandlers(level, pos), level, pos);
        }
    }
}
