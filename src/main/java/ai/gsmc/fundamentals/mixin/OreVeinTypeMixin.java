package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.worldgen.MagnetiteVeins;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets {@link MagnetiteVeins} change what the hardcoded iron vein is made of. */
@Mixin(targets = "net.minecraft.world.level.levelgen.OreVeinifier$VeinType")
public abstract class OreVeinTypeMixin implements MagnetiteVeins.Vein {

    @Shadow @Final @Mutable BlockState ore;
    @Shadow @Final @Mutable BlockState rawOreBlock;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fundamentals$remember(CallbackInfo ci) {
        if ("IRON".equals(((Enum<?>) (Object) this).name())) {
            MagnetiteVeins.iron = this;
        }
    }

    @Override
    public void fundamentals$setBlocks(BlockState ore, BlockState rich) {
        this.ore = ore;
        this.rawOreBlock = rich;
    }
}
