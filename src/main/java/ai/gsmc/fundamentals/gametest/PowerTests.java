package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.power.Electricity;
import ai.gsmc.fundamentals.power.SolarPanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class PowerTests {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty", timeoutTicks = 200)
    public void aSolarPanelMakesPowerUnderTheSunAndNoneInShade(GameTestHelper helper) {
        helper.setDayTime(6000);
        helper.setBlock(POS, Electricity.solarPanel());
        SolarPanelBlockEntity panel = helper.getBlockEntity(POS);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(panel.voltageGeneration() == SolarPanelBlockEntity.VOLTS && panel.powerGeneration() > 0,
                    "at noon under open sky the panel should generate, got " + panel.voltageGeneration() + " V, " + panel.powerGeneration() + " W");
            helper.assertTrue(panel.getNetworkPowerGeneration() >= panel.powerGeneration(),
                    "its network should carry what it makes, got " + panel.getNetworkPowerGeneration() + " W");
            helper.setBlock(POS.above(), Blocks.STONE);
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(panel.voltageGeneration() == 0 && panel.powerGeneration() == 0, "a shaded panel should make nothing");
                helper.succeed();
            });
        });
    }
}
