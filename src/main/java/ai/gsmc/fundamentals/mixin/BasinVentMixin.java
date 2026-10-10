package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.liquid.Liquids;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every couple of seconds an open basin boils off whatever in it is past its boiling point. */
@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class BasinVentMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void fundamentals$vent(CallbackInfo ci) {
        BasinBlockEntity self = (BasinBlockEntity) (Object) this;
        if (self.getLevel() instanceof ServerLevel level && Math.floorMod(level.getGameTime() + self.getBlockPos().asLong(), Liquids.VENT_INTERVAL) == 0) {
            Liquids.vent(level, self.getBlockPos());
        }
    }
}
