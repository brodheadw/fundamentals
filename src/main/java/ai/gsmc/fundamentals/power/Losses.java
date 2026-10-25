package ai.gsmc.fundamentals.power;

import ai.gsmc.fundamentals.compute.PowerSupplyBlockEntity;
import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.drmangotea.tfmg.content.electricity.base.KineticElectricBlockEntity;
import com.drmangotea.tfmg.content.electricity.base.VoltageAlteringBlockEntity;
import com.drmangotea.tfmg.content.electricity.generators.creative_generator.CreativeGeneratorBlockEntity;
import com.drmangotea.tfmg.content.electricity.lights.LightBulbBlockEntity;
import com.drmangotea.tfmg.content.electricity.lights.neon_tube.NeonTubeBlockEntity;
import com.drmangotea.tfmg.content.electricity.network.large_switch.LargeSwitchBlockEntity;
import com.drmangotea.tfmg.content.electricity.network.transformer.large.LargeTransformerBlockEntity;
import com.drmangotea.tfmg.content.electricity.storage.AccumulatorBlockEntity;
import com.drmangotea.tfmg.content.electricity.utilities.converter.ConverterBlockEntity;
import com.drmangotea.tfmg.content.electricity.utilities.electric_motor.ElectricMotorBlockEntity;
import com.drmangotea.tfmg.content.electricity.utilities.electric_pump.ElectricPumpBlockEntity;
import com.drmangotea.tfmg.content.machinery.vat.electrode_holder.ElectrodeHolderBlockEntity;
import com.wildspell.fundamental.api.energy.Energy;
import net.minecraft.server.level.ServerLevel;

/** Where electricity stops being electricity: what a block draws and does not pass on as work, light or charge is heat in it. */
public final class Losses {
    /** Seconds between reports: TFMG's blocks report on Create's lazy tick, every ten ticks. */
    public static final double PERIOD = 0.5;
    /** A shaft-driven generator turns this much of its shaft into current; the rest warms its windings. */
    public static final double GENERATOR = 0.9;

    private Losses() {}

    /** The share of the power a consumer draws that leaves it as something other than heat. */
    public static double passedOn(IElectric block) {
        return block instanceof ElectricMotorBlockEntity ? 0.9
                : block instanceof VoltageAlteringBlockEntity || block instanceof LargeTransformerBlockEntity ? 0.98
                : block instanceof LargeSwitchBlockEntity ? 1
                : block instanceof ConverterBlockEntity || block instanceof AccumulatorBlockEntity ? 0.95
                : block instanceof PowerSupplyBlockEntity ? 0.85
                : block instanceof ElectricPumpBlockEntity ? 0.7
                : block instanceof ElectrodeHolderBlockEntity ? 0.6
                : block instanceof NeonTubeBlockEntity ? 0.25
                : block instanceof LightBulbBlockEntity ? 0.05
                : 0;
    }

    /** Watts this block is losing as heat right now. */
    public static double watts(IElectric block) {
        if (block.powerGeneration() > 0) return !(block instanceof KineticElectricBlockEntity) || block instanceof CreativeGeneratorBlockEntity ? 0 : block.getGeneratorLoad() * (1 / GENERATOR - 1);
        if (block.resistance() == 0 || !block.canWork()) return 0;
        return block.getPowerUsage() * (1 - passedOn(block));
    }

    public static void report(IElectric block) {
        if (block.getLevelAccessor() instanceof ServerLevel level) {
            Energy.dissipate(level, block.getBlockPos(), watts(block) * PERIOD);
        }
    }
}
