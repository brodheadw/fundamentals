package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.air.Barometer;
import ai.gsmc.fundamentals.air.BarometerBlockEntity;
import ai.gsmc.fundamentals.air.Barometers;
import ai.gsmc.fundamentals.heat.ThermometerBlock;
import com.wildspell.fundamental.api.air.Pressure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class BarometerTests {

    // alone in its batch: it sets the world's weather
    @GameTest(template = "battery", timeoutTicks = 40, batch = "barometer")
    public void theBarographWarnsAsTheGlassFallsAheadOfRain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevelData data = (ServerLevelData) level.getLevelData();
        int clear = data.getClearWeatherTime(), rain = data.getRainTime(), thunder = data.getThunderTime();
        boolean raining = data.isRaining(), thundering = data.isThundering();
        BlockPos graph = new BlockPos(2, 1, 2), glass = new BlockPos(4, 1, 2);
        helper.setBlock(graph, Barometers.block(Barometer.BAROGRAPH).defaultBlockState().setValue(ThermometerBlock.FACING, Direction.UP));
        helper.setBlock(glass, Barometers.block(Barometer.MERCURY).defaultBlockState().setValue(ThermometerBlock.FACING, Direction.UP));
        BarometerBlockEntity barograph = helper.getBlockEntity(graph);
        BarometerBlockEntity mercury = helper.getBlockEntity(glass);
        try {
            level.setWeatherParameters(0, Pressure.LEAD * 3, false, false);
            barograph.sample(level);
            barograph.sample(level);
            helper.assertTrue(barograph.signal() == 0, "steady fair weather gives no warning");
            mercury.sample(level);
            int fair = mercury.signal();
            // the glass falls over the last quarter day before the rain: an hour's worth of readings across it
            for (int ticks = Pressure.LEAD - 1; ticks >= Pressure.LEAD - Pressure.HOUR; ticks -= BarometerBlockEntity.SAMPLE) {
                level.setWeatherParameters(0, ticks, false, false);
                barograph.sample(level);
            }
            helper.assertTrue(barograph.perHour() < -BarometerBlockEntity.STORM && barograph.signal() > 8,
                    "an hour of falling glass ahead of rain is a storm warning: " + barograph.perHour() + " hPa/h, signal " + barograph.signal());
            level.setWeatherParameters(0, 0, true, true);
            mercury.sample(level);
            helper.assertTrue(mercury.signal() < fair, "the mercury barometer reads lower in the storm");
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(graph)).getAnalogOutputSignal(level, helper.absolutePos(graph)) == barograph.signal(),
                    "a comparator reads the warning");
        } finally {
            level.setWeatherParameters(clear, rain, raining, thundering);
            data.setThunderTime(thunder);
        }
        helper.succeed();
    }
}
