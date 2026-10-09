package ai.gsmc.fundamentals.mixin;

import com.simibubi.create.content.fluids.tank.BoilerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Create's boiler already takes only fresh water (it asks for exactly minecraft:water); a starved boiler's goggles say so, since seawater looks the same in a pipe. */
@Mixin(value = BoilerData.class, remap = false)
public abstract class BoilerFreshWaterMixin {

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"))
    private void fundamentals$freshWater(List<Component> tooltip, boolean isPlayerSneaking, int boilerSize, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && ((BoilerData) (Object) this).waterSupply <= 0) {
            tooltip.add(Component.literal("    ").append(Component.translatable("goggles.fundamentals.boiler.fresh_water").withStyle(ChatFormatting.GOLD)));
        }
    }
}
