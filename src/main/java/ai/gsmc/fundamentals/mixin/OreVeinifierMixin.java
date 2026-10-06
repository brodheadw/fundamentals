package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.worldgen.MagnetiteVeins;
import net.minecraft.world.level.levelgen.OreVeinifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OreVeinifier.class)
public abstract class OreVeinifierMixin {

    @Inject(method = "create", at = @At("HEAD"))
    private static void fundamentals$magnetiteVeins(CallbackInfoReturnable<?> cir) {
        MagnetiteVeins.apply();
    }
}
