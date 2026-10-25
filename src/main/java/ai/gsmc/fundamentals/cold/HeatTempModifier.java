package ai.gsmc.fundamentals.cold;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

/** The world temperature at the feet, the same reading as /heat: climate plus the heat grid, in Cold Sweat's units. */
public class HeatTempModifier extends TempModifier {
    @Override
    protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait) {
        double units = Cold.toColdSweat(Heat.at(entity.level(), entity.blockPosition()));
        return temp -> temp + units;
    }
}
