package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.compute.Compute;
import ai.gsmc.fundamentals.compute.Power;
import ai.gsmc.fundamentals.compute.PowerSupplyBlockEntity;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.wildspell.fundamental.api.heat.Heat;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class FabTests {

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    @GameTest(template = "empty")
    public void everyRecipeLoadsAndComputersNeedChips(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        List<String> missing = new ArrayList<>();
        server.getResourceManager().listResources("recipe", path -> path.getPath().endsWith(".json")).keySet().stream()
                .filter(file -> file.getNamespace().equals(Fundamentals.MOD_ID) && file.getPath().startsWith("recipe/compute/"))
                .map(file -> file.withPath(file.getPath().substring("recipe/".length(), file.getPath().length() - 5)))
                // novolac is condensed from Chemica's phenol, so it only exists alongside Chemica
                .filter(id -> !id.getPath().equals("compute/novolac_resin") || net.neoforged.fml.ModList.get().isLoaded("chemica"))
                .filter(id -> server.getRecipeManager().byKey(id).isEmpty())
                .forEach(id -> missing.add(id.toString()));
        helper.assertTrue(missing.isEmpty(), "recipes that failed to load (unknown item or fluid?): " + missing);
        for (String computer : List.of("computer_normal", "computer_advanced", "monitor_normal", "wired_modem", "disk_drive", "pocket_computer_normal")) {
            Recipe<?> recipe = server.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("computercraft", computer)).orElseThrow().value();
            helper.assertTrue(recipe.getIngredients().stream().anyMatch(i -> Compute.CHIP.values().stream().anyMatch(chip -> i.test(new ItemStack(chip)))),
                    computer + " must be built from a fabbed chip");
            helper.assertFalse(recipe.getIngredients().stream().anyMatch(i -> i.test(new ItemStack(net.minecraft.world.item.Items.STONE))), computer + " is no longer stone");
        }
        helper.assertTrue(server.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("computercraft", "disk_1")).isEmpty(),
                "the paper-and-redstone disk recipes are gone");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void theMaskInTheDeployerPicksTheDesign(GameTestHelper helper) {
        for (String chip : Compute.CHIPS) {
            String design = chip.replace("_chip", "");
            ItemStack wafer = stack(chip.equals("microcontroller") ? "tfmg:n_semiconductor" : chip.equals("memory_chip") ? "tfmg:p_semiconductor" : "fundamentals:epitaxial_wafer");
            RecipeWrapper input = new RecipeWrapper(new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, wafer, new ItemStack(Compute.MASK.get(chip)))));
            var step = SequencedAssemblyRecipe.getRecipe(helper.getLevel(), input, AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class);
            helper.assertTrue(step.isPresent() && step.get().id().getPath().startsWith("compute/" + design + "_wafer"),
                    "a " + design + " mask on " + wafer + " should start the " + design + " wafer, got " + step.map(s -> s.id()).orElse(null));
        }
        helper.succeed();
    }

    private static AbstractComputerBlockEntity computer(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos);
    }

    @GameTest(template = "empty", timeoutTicks = 2400)
    public void computersRunOnlyOnALiveSupplyAndCutOutWhenHot(GameTestHelper helper) {
        BlockPos computerPos = new BlockPos(2, 1, 2), supplyPos = new BlockPos(3, 1, 2), generatorPos = new BlockPos(4, 1, 2);
        Block generator = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfmg:creative_generator"));
        helper.setBlock(computerPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("computercraft:computer_normal")));
        helper.setBlock(supplyPos, Compute.powerSupply());
        helper.setBlock(generatorPos, generator);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    PowerSupplyBlockEntity supply = helper.getBlockEntity(supplyPos);
                    helper.assertTrue(supply.live(), "the generator should bring the supply up: " + supply.getData().getVoltage() + " V, works " + supply.canWork());
                })
                .thenExecute(() -> computer(helper, computerPos).createServerComputer().turnOn())
                .thenWaitUntil(() -> helper.assertTrue(computer(helper, computerPos).getServerComputer().isOn(), "on a live supply the computer boots"))
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(computer(helper, computerPos).getServerComputer().isOn(), "on a live supply the computer keeps running");
                    PowerSupplyBlockEntity supply = helper.getBlockEntity(supplyPos);
                    helper.assertTrue(supply.getPowerUsage() > 0, "the supply draws from the grid: " + supply.getPowerUsage() + " W");
                    helper.setBlock(generatorPos, net.minecraft.world.level.block.Blocks.AIR);
                })
                .thenWaitUntil(() -> helper.assertFalse(computer(helper, computerPos).getServerComputer().isOn(), "pulling the generator cuts the computer off"))
                .thenExecute(() -> helper.setBlock(generatorPos, generator))
                .thenWaitUntil(() -> helper.assertTrue(((PowerSupplyBlockEntity) helper.getBlockEntity(supplyPos)).live(), "the supply comes back"))
                .thenExecute(() -> computer(helper, computerPos).createServerComputer().turnOn())
                .thenWaitUntil(() -> helper.assertTrue(computer(helper, computerPos).getServerComputer().isOn(), "it boots again"))
                .thenExecute(() -> {
                    helper.assertTrue(Heat.at(helper.getLevel(), helper.absolutePos(computerPos)) < Power.CUTOUT_CELSIUS, "a running computer only warms its room");
                    Heat.boost(helper.getLevel(), helper.absolutePos(computerPos), 150, 1, 200);
                })
                .thenWaitUntil(() -> helper.assertFalse(computer(helper, computerPos).getServerComputer().isOn(),
                        "past its junction limit the computer trips its thermal cutout"))
                .thenSucceed();
    }
}
