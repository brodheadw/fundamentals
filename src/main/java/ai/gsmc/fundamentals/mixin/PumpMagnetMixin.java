package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.magnet.MagnetBehaviour;
import com.drmangotea.tfmg.content.electricity.utilities.electric_pump.ElectricPumpBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** The electric pump has a motor in it: the pressure it puts on its pipes is scaled by what the heat has left of its magnets. */
@Mixin(value = ElectricPumpBlockEntity.class, remap = false)
public abstract class PumpMagnetMixin {

    @ModifyArg(method = "distributePressureTo", index = 2, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/fluids/FluidTransportBehaviour;addPressure(Lnet/minecraft/core/Direction;ZF)V"))
    private float fundamentals$magnet(float pressure) {
        return pressure * MagnetBehaviour.output((BlockEntity) (Object) this);
    }
}
