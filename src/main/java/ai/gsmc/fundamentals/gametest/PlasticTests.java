package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.plastics.DyedPlasticPipeBlock;
import ai.gsmc.fundamentals.plastics.Pigment;
import ai.gsmc.fundamentals.plastics.Plastics;
import ai.gsmc.fundamentals.separation.Hazards;
import ai.gsmc.fundamentals.separation.PlasticTankBlock;
import ai.gsmc.fundamentals.separation.Separation;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class PlasticTests {

    @GameTest(template = "empty")
    public void theFactorysPlasticVatsNeedTheCatalystAndPvcMakesPipe(GameTestHelper helper) {
        ItemStack catalyst = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "ziegler_natta_catalyst")));
        for (String olefin : new String[] {"ethylene", "propylene"}) {
            var recipe = (ProcessingRecipe<?, ?>) helper.getLevel().getRecipeManager()
                    .byKey(ResourceLocation.parse("tfmg:vat_machine_recipe/plastic_from_" + olefin)).orElseThrow().value();
            helper.assertTrue(recipe.getIngredients().stream().anyMatch(i -> i.test(catalyst)), "plastic from " + olefin + " should take the catalyst");
            helper.assertTrue(recipe.getRollableResults().stream().anyMatch(r -> r.getStack().is(catalyst.getItem()) && r.getChance() < 1),
                    "the catalyst should come back most of the time, not always");
        }
        ItemStack pvc = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "pvc_sheet")));
        var pipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("tfmg:crafting/materials/plastic_pipe")).orElseThrow().value();
        helper.assertTrue(pipe.getIngredients().stream().anyMatch(i -> i.test(pvc)), "a PVC sheet should make the Factory's plastic pipe");
        helper.succeed();
    }

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

    @GameTest(template = "empty")
    public void eightPlasticBlocksAndADyeMakeEightColouredOnes(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "plastics/red_plastic_block")).orElseThrow().value();
        ItemStack block = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfmg:plastic_block")));
        helper.assertTrue(recipe.getIngredients().stream().filter(i -> i.test(block)).count() == 8, "eight of TFMG's plastic blocks should go in");
        helper.assertTrue(recipe.getIngredients().stream().anyMatch(i -> i.test(new ItemStack(Items.RED_DYE))), "with a red dye");
        ItemStack out = recipe.getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(out.getCount() == 8 && BuiltInRegistries.ITEM.getKey(out.getItem()).getPath().equals("red_plastic_block"), "should make eight red plastic blocks, made " + out);
        helper.succeed();
    }
}
