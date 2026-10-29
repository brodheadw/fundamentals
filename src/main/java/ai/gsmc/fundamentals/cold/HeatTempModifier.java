package ai.gsmc.fundamentals.cold;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

/**
 * The world temperature at the feet as the body feels it, in Cold Sweat's units: the same reading as /heat up to a warm day, then
 * levelling off toward {@link #HOTTEST}, so that living beside lava or a caldera is warm but never burning. Touching it still burns.
 */
public class HeatTempModifier extends TempModifier {
    /** Where the felt heat starts to level off, and the most it ever reaches, in °C. */
    public static final double WARM = 30, HOTTEST = 45;

    public static double felt(double celsius) {
        return celsius <= WARM ? celsius : WARM + (HOTTEST - WARM) * Math.tanh((celsius - WARM) / (HOTTEST - WARM));
    }

    @Override
    protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait) {
        double units = Cold.toColdSweat(felt(Heat.at(entity.level(), entity.blockPosition())));
        return temp -> temp + units;
    }
}
