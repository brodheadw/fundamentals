package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.power.Losses;
import com.drmangotea.tfmg.content.electricity.utilities.resistor.ResistorBlockEntity;
import com.drmangotea.tfmg.registry.TFMGBlocks;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class LossTests {

    @GameTest(template = "empty", timeoutTicks = 1200)
    public void aResistorTurnsAllItDrawsIntoHeatAndWarmsItsBlock(GameTestHelper helper) {
        BlockPos generator = new BlockPos(1, 1, 2), resistor = new BlockPos(2, 1, 2);
        helper.setBlock(generator, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfmg:creative_generator")));
        helper.setBlock(resistor, TFMGBlocks.RESISTOR.getDefaultState().setValue(BlockStateProperties.FACING, Direction.EAST));
        ResistorBlockEntity load = helper.getBlockEntity(resistor);
        load.resistance = 20;
        load.updateNextTick();
        double ambient = Heat.ambient(helper.getLevel(), helper.absolutePos(resistor));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(load.getPowerUsage() > 1000 && load.canWork(),
                        "the generator drives the resistor: " + load.getData().getVoltage() + " V, " + load.getPowerUsage() + " W"))
                .thenExecute(() -> helper.assertTrue(Losses.watts(load) == load.getPowerUsage(), "a resistor passes nothing on: all it draws is heat"))
                .thenWaitUntil(() -> helper.assertTrue(Heat.at(helper.getLevel(), helper.absolutePos(resistor)) - ambient > 20,
                        "the resistor warms: " + (Heat.at(helper.getLevel(), helper.absolutePos(resistor)) - ambient)))
                .thenSucceed();
    }
}
