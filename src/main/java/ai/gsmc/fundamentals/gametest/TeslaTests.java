package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.power.Electricity;
import ai.gsmc.fundamentals.power.TeslaCoilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class TeslaTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public void aFedCoilArcsIntoAnEnemy(GameTestHelper helper) {
        BlockPos coil = new BlockPos(1, 1, 1);
        helper.setBlock(coil, Electricity.teslaCoil());
        helper.setBlock(coil.east(), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfmg:creative_generator")));
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        zombie.setNoAi(true);
        float full = zombie.getHealth();
        helper.succeedWhen(() -> {
            TeslaCoilBlockEntity entity = helper.getBlockEntity(coil);
            helper.assertTrue(entity.live(), "the grid feeds the coil: " + entity.getData().getVoltage() + " V");
            helper.assertTrue(zombie.getHealth() < full, "and it arcs into the zombie");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public void machinesReadOutLikeABoiler(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, Electricity.teslaCoil());
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        TeslaCoilBlockEntity coil = helper.getBlockEntity(at);
        helper.assertTrue(coil.addToGoggleTooltip(lines, false), "the coil reads out through goggles");
        String text = lines.stream().map(net.minecraft.network.chat.Component::getString).reduce("", (a, b) -> a + "\n" + b);
        helper.assertTrue(text.contains("|".repeat(ai.gsmc.fundamentals.goggles.Readout.SEGMENTS)) && text.contains("unpowered") && text.contains("Needs"),
                "a status, a bar per factor and what it needs: " + text);
        helper.succeed();
    }
}
