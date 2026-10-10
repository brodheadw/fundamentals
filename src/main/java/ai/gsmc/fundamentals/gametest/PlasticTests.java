package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.plastics.DyedPlasticPipeBlock;
import ai.gsmc.fundamentals.plastics.Pigment;
import ai.gsmc.fundamentals.plastics.Plastics;
import ai.gsmc.fundamentals.separation.Hazards;
import ai.gsmc.fundamentals.separation.PlasticTankBlock;
import ai.gsmc.fundamentals.separation.Separation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class PlasticTests {

    @GameTest(template = "empty", timeoutTicks = 100)
    public void aDyedTankKeepsItsColourAndNeitherItNorADyedPipeCorrodes(GameTestHelper helper) {
        BlockPos low = new BlockPos(1, 1, 1);
        helper.setBlock(low, Separation.plasticTank().defaultBlockState());
        helper.assertTrue(Plastics.dye(helper.getLevel(), helper.absolutePos(low), DyeColor.ORANGE), "dyeing a natural tank should change it");
        helper.setBlock(low.above(), Separation.plasticTank().defaultBlockState());
        BlockPos pipe = new BlockPos(3, 1, 1);
        helper.setBlock(pipe, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfmg:plastic_pipe")).defaultBlockState());
        Plastics.dye(helper.getLevel(), helper.absolutePos(pipe), DyeColor.YELLOW);
        helper.runAfterDelay(20, () -> {
            BlockState lower = helper.getBlockState(low);
            helper.assertFalse(lower.getValue(PlasticTankBlock.TOP), "the two tanks should have joined, which rewrites the lower one's state");
            helper.assertTrue(lower.getValue(PlasticTankBlock.COLOR) == Pigment.ORANGE, "the tank lost its colour when it joined: " + lower);
            helper.assertFalse(Hazards.corrodible(lower), "a dyed tank is still plastic");
            BlockState dyed = helper.getBlockState(pipe);
            helper.assertTrue(dyed.getBlock() instanceof DyedPlasticPipeBlock && dyed.getValue(DyedPlasticPipeBlock.COLOR) == DyeColor.YELLOW, "dyeing a plastic pipe should give a dyed one: " + dyed);
            helper.assertFalse(Hazards.corrodible(dyed), "a dyed pipe is still plastic");
            helper.succeed();
        });
    }

}
