package ai.gsmc.fundamentals.mixin.coldsweat;

import ai.gsmc.fundamentals.cold.Cold;
import com.momosoftworks.coldsweat.common.capability.temperature.AbstractTempCap;
import com.momosoftworks.coldsweat.common.capability.temperature.PlayerTempCap;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Cold Sweat's own thermometer is hidden; one of ours in the hotbar shows its world gauge instead
@Mixin(PlayerTempCap.class)
public abstract class ThermometerHudMixin {
    @Inject(method = "calculateHudVisibility", at = @At("TAIL"))
    private void fundamentals$ourThermometers(Player player, CallbackInfo ci) {
        if (Cold.holdsThermometer(player)) {
            ((AbstractTempCap) (Object) this).showWorldTemp = true;
        }
    }
}
