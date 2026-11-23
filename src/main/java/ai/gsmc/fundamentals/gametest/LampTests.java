package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.power.Electricity;
import ai.gsmc.fundamentals.power.LampBlock;
import ai.gsmc.fundamentals.power.LampBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class LampTests {
    static int light(GameTestHelper helper, BlockPos at) {
        return helper.getBlockState(at).getValue(LampBlock.LIGHT);
    }

    static void feed(GameTestHelper helper, BlockPos lamp, Block block) {
        helper.setBlock(lamp, block);
        helper.setBlock(lamp.east(), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfmg:creative_generator")));
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public void aFedTubeFlickersThenHolds(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        feed(helper, at, Electricity.fluorescentLight());
        helper.succeedWhen(() -> {
            LampBlockEntity lamp = helper.getBlockEntity(at);
            helper.assertTrue(lamp.live(), "the grid feeds the tube: " + lamp.getData().getVoltage() + " V");
            helper.assertTrue(lamp.steady() && light(helper, at) == 15, "and once struck it holds at full light");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public void anUnfedLampIsDark(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, Electricity.sodiumLamp());
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(light(helper, at) == 0, "no grid, no light");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public void sodiumGlowsDimThenWarmsToFull(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        feed(helper, at, Electricity.sodiumLamp());
        int[] dimSeen = {0};
        helper.onEachTick(() -> {
            int l = light(helper, at);
            if (l > 0 && l < 10) dimSeen[0]++;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(light(helper, at) == 15, "warm, it is fully bright: " + light(helper, at));
            helper.assertTrue(dimSeen[0] > 40, "but it glowed dim a good while first: " + dimSeen[0] + " ticks");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public void aRedDiffuserPassesLessLight(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        feed(helper, at, Electricity.fluorescentTroffer());
        helper.setBlock(at, helper.getBlockState(at).setValue(LampBlock.COLOR, ai.gsmc.fundamentals.plastics.Pigment.RED));
        helper.succeedWhen(() -> {
            LampBlockEntity lamp = helper.getBlockEntity(at);
            helper.assertTrue(lamp.steady() && light(helper, at) == LampBlock.passes(ai.gsmc.fundamentals.plastics.Pigment.RED) && light(helper, at) < 15,
                    "struck behind red plastic, it gives the red's share of the light: " + light(helper, at));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public void aBulbIsLitTheMomentItIsFedAndABlacklightIsDim(GameTestHelper helper) {
        BlockPos bulb = new BlockPos(1, 1, 1), uv = new BlockPos(1, 1, 3);
        feed(helper, bulb, Electricity.lightBulb());
        feed(helper, uv, Electricity.blacklight());
        helper.runAfterDelay(5, () -> helper.assertTrue(light(helper, bulb) > 0, "no starter, no warming: the filament is lit at once"));
        helper.succeedWhen(() -> {
            helper.assertTrue(light(helper, bulb) == LampBlock.Kind.BULB.full || helper.<LampBlockEntity>getBlockEntity(bulb).getData().getVoltage() < LampBlockEntity.RATED_VOLTS,
                    "at its rated volts it is at full: " + light(helper, bulb));
            helper.assertTrue(helper.<LampBlockEntity>getBlockEntity(uv).steady() && light(helper, uv) == LampBlock.Kind.BLACKLIGHT.full, "the UV tube struck shows only a dim violet");
        });
    }
}
