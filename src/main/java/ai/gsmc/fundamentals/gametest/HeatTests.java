package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.heat.Thermometer;
import ai.gsmc.fundamentals.heat.ThermometerBlock;
import ai.gsmc.fundamentals.heat.ThermometerBlockEntity;
import ai.gsmc.fundamentals.heat.Thermometers;
import ai.gsmc.fundamentals.uses.Uses;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
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
        helper.assertTrue(Math.abs(Heat.climate(0.8) - 15) < 1e-9 && Math.abs(Heat.climate(0.95) - 26) < 1e-9 && Heat.climate(2) < 35,
                "plains should be temperate, a jungle tropical, a desert hot but not past any real annual mean");
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

    @GameTest(template = "battery", timeoutTicks = 200)
    public void aThermometerOnALitBlastFurnaceReadsItAndMercuryBursts(GameTestHelper helper) {
        BlockPos furnace = new BlockPos(5, 1, 2), typeS = furnace.east(), mercury = furnace.west(), comparator = typeS.east();
        for (int x = 2; x <= 8; x++) {
            helper.setBlock(new BlockPos(x, 0, 2), Blocks.STONE);
        }
        helper.setBlock(furnace, Blocks.BLAST_FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.LIT, true));
        helper.setBlock(typeS, Thermometers.block(Thermometer.TYPE_S).defaultBlockState().setValue(ThermometerBlock.FACING, Direction.EAST));
        helper.setBlock(mercury, Thermometers.block(Thermometer.MERCURY).defaultBlockState().setValue(ThermometerBlock.FACING, Direction.WEST));
        helper.setBlock(comparator, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST));
        helper.startSequence()
                .thenWaitUntil(() -> {
                    ThermometerBlockEntity gauge = helper.getBlockEntity(typeS);
                    helper.assertTrue(gauge.celsius() > 1400, "a type S on a lit blast furnace should read past 1,400 °C, got " + gauge.celsius());
                    int signal = ((ComparatorBlockEntity) helper.getBlockEntity(comparator)).getOutputSignal();
                    helper.assertTrue(signal == Thermometer.TYPE_S.signal(gauge.celsius()) && signal >= 13, "a comparator should read it high on the scale, got " + signal);
                })
                .thenExecute(() -> {
                    helper.assertBlockNotPresent(Thermometers.block(Thermometer.MERCURY), mercury);
                    helper.assertItemEntityPresent(Uses.mercury(), mercury, 1.5);
                })
                .thenSucceed();
    }

}
