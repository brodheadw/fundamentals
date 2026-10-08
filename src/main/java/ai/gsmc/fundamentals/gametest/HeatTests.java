package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.heat.Heat;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class HeatTests {

    @GameTest(template = "battery", timeoutTicks = 200)
    public void theTemperatureIsClimatePlusWhatIsBurningNearby(GameTestHelper helper) {
        BlockPos here = helper.absolutePos(new BlockPos(13, 1, 2));
        double ambient = Heat.at(helper.getLevel(), here);
        helper.assertTrue(ambient > -30 && ambient < 60, "a plausible climate, got " + ambient);
        helper.assertTrue(Math.abs(Heat.fahrenheit(100) - 212) < 1e-9 && Math.abs(Heat.fahrenheit(-40) + 40) < 1e-9, "Fahrenheit should convert");
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.LAVA);
        helper.setBlock(new BlockPos(24, 1, 2), Blocks.BLUE_ICE);
        helper.runAfterDelay(25, () -> {
            double inLava = Heat.at(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)));
            double byLava = Heat.at(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 2)));
            double byIce = Heat.at(helper.getLevel(), helper.absolutePos(new BlockPos(23, 1, 2)));
            double far = Heat.at(helper.getLevel(), here);
            helper.assertTrue(inLava > far + 1100, "lava itself should be past 1,100 degrees, got " + inLava + " vs " + far);
            helper.assertTrue(byLava > far + 200 && byLava < far + 300, "a block from lava should be a couple of hundred degrees hotter, got " + byLava + " vs " + far);
            helper.assertTrue(byIce < far, "next to blue ice should be colder, got " + byIce + " vs " + far);
            helper.assertTrue(Math.abs(far - ambient) < 1, "thirteen blocks from both, the climate should stand, got " + far + " vs " + ambient);
            helper.succeed();
        });
    }
}
