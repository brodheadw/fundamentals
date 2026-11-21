package ai.gsmc.fundamentals.mixin;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Create Propulsion's Stirling engine only knew blaze burners; here it runs on whatever the heat field puts under it, lava first
@Pseudo
@Mixin(targets = "dev.propulsionteam.propulsionsimulated.content.heat.engine.StirlingEngineBlockEntity", remap = false)
public abstract class StirlingHeatMixin extends GeneratingKineticBlockEntity {

    // a fading burner, the coolest one the engine takes, reads 500 °C on the heat field
    private static final double FADING_CELSIUS = 500;

    @Shadow private int activeTicks;

    @Shadow public abstract boolean isEngineActive();

    private StirlingHeatMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "tickBlazeBurnerHeat", at = @At("HEAD"), cancellable = true)
    private void fundamentals$heatFromBelow(CallbackInfo ci) {
        BlockPos below = worldPosition.below();
        if (!isEngineActive() || level.getBlockState(below).getBlock() instanceof BlazeBurnerBlock || Heat.at(level, below) < FADING_CELSIUS) {
            return;
        }
        boolean starting = activeTicks == 0;
        activeTicks = 3;
        if (starting) {
            updateGeneratedRotation();
        }
        ci.cancel();
    }
}
