package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.air.FieldMill;
import ai.gsmc.fundamentals.air.FieldMillBlockEntity;
import ai.gsmc.fundamentals.heat.ThermometerBlock;
import com.wildspell.fundamental.api.air.AirField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class FieldMillTests {
    private static BlockPos storm;

    static {
        AirField.addSource((level, pos) -> pos.equals(storm) ? 30000 : 0);
    }

    @GameTest(template = "empty", batch = "field_mill")
    public void aFieldMillReadsAChargedDustStormOnALogScale(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, FieldMill.block().defaultBlockState().setValue(ThermometerBlock.FACING, Direction.UP));
        FieldMillBlockEntity mill = helper.getBlockEntity(at);
        var finding = level.getServer().getAdvancements().get(Fundamentals.id("triboelectric"));
        helper.assertTrue(finding != null, "the finding is loaded");
        try {
            mill.sample(level);
            helper.assertTrue(mill.voltsPerMetre() == AirField.at(level, helper.absolutePos(at)) && mill.signal() <= 1,
                    "with no storm it reads the weather's field and the comparator stays low: " + mill.voltsPerMetre() + " V/m, " + mill.signal());
            storm = helper.absolutePos(at);
            mill.sample(level);
            helper.assertTrue(mill.voltsPerMetre() >= 30000 && mill.signal() >= 12 && mill.signal() < 15,
                    "a dust storm's field reads tens of kilovolts, high on the log scale: " + mill.voltsPerMetre() + " V/m, " + mill.signal());
            helper.assertTrue(level.getBlockState(helper.absolutePos(at)).getAnalogOutputSignal(level, helper.absolutePos(at)) == mill.signal(), "a comparator reads it");
            helper.assertTrue(FieldMill.signal(AirField.FAIR) == 0 && FieldMill.signal(FieldMill.MAX) == 15, "the scale runs fair weather to fifty kilovolts");
        } finally {
            storm = null;
        }
        helper.succeed();
    }
}
