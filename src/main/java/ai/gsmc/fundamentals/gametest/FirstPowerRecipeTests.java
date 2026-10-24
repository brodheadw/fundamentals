package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import com.drmangotea.tfmg.recipes.VatMachineRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class FirstPowerRecipeTests {

    private static Recipe<?> recipe(GameTestHelper helper, String id) {
        return helper.getLevel().getRecipeManager().byKey(Fundamentals.id("uses/" + id))
                .orElseThrow(() -> new AssertionError("no recipe " + id)).value();
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static boolean takes(Recipe<?> recipe, String id) {
        return recipe.getIngredients().stream().anyMatch(i -> i.test(stack(id)));
    }

    private static boolean takesFluid(ProcessingRecipe<?, ?> recipe, String id) {
        var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
        return recipe.getFluidIngredients().stream().anyMatch(i -> i.test(new FluidStack(fluid, 1000)));
    }

    private static boolean givesFluid(ProcessingRecipe<?, ?> recipe, String id) {
        var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
        return recipe.getFluidResults().stream().anyMatch(f -> f.getFluid() == fluid);
    }

    @GameTest(template = "empty")
    public void chemicalCellMagnetizesQuenchedSteelWithoutElectricMachines(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        Recipe<?> hot = recipe(helper, "hot_steel");
        helper.assertTrue(hot.getType() == RecipeType.SMELTING && takes(hot, "tfmg:steel_ingot")
                && hot.getResultItem(registries).is(stack("fundamentals:hot_steel").getItem()), "steel should heat in a furnace");
        var quench = (ProcessingRecipe<?, ?>) recipe(helper, "hardened_steel");
        helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.getKey(quench.getType()).toString().equals("create:filling")
                && takes(quench, "fundamentals:hot_steel") && takesFluid(quench, "minecraft:water")
                && quench.getResultItem(registries).is(stack("fundamentals:hardened_steel").getItem()), "hot steel should quench in water");
        var cell = (VatMachineRecipe) recipe(helper, "galvanic_cell");
        helper.assertTrue(cell.machines.isEmpty() && cell.getRequiredHeat() == HeatCondition.NONE
                && takes(cell, "minecraft:copper_ingot") && takes(cell, "create:zinc_ingot") && takes(cell, "minecraft:glass_bottle")
                && takesFluid(cell, "tfmg:sulfuric_acid") && takesFluid(cell, "minecraft:water")
                && cell.getResultItem(registries).is(stack("fundamentals:galvanic_cell").getItem()),
                "the starter cell should assemble copper, zinc and dilute acid without electric machines");
        var magnet = (ShapelessRecipe) recipe(helper, "steel_magnet");
        var input = CraftingInput.of(3, 1, List.of(stack("fundamentals:hardened_steel"), stack("fundamentals:galvanic_cell"), stack("tfmg:copper_wire")));
        helper.assertTrue(magnet.matches(input, helper.getLevel()) && magnet.assemble(input, registries).is(stack("fundamentals:steel_magnet").getItem()),
                "the chemical cell and coil should magnetize hardened steel on the crafting table");
        helper.assertTrue(magnet.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "magnetization should consume its chemical cell and winding");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void ironCoresMakeTheFirstTransformerWithoutSilicon(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        for (String id : new String[] {"transformer_with_iron_core", "electromagnetic_coil_with_iron_core"}) {
            Recipe<?> built = recipe(helper, id);
            String core = id.startsWith("transformer") ? "create:iron_sheet" : "minecraft:iron_ingot";
            helper.assertTrue(takes(built, core) && !takes(built, "tfmg:magnetic_alloy_ingot") && !takes(built, "tfmg:magnetic_alloy_sheet"),
                    id + " should use an iron core without silicon-bearing magnetic alloy");
            String output = id.startsWith("transformer") ? "tfmg:transformer" : "tfmg:unfinished_electromagnetic_coil";
            helper.assertTrue(built.getResultItem(registries).is(stack(output).getItem()), id + " should make " + output);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void nickelCarbonylRefiningDoesNotNeedANickelPoweredMixer(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var formation = (VatMachineRecipe) recipe(helper, "nickel_carbonyl");
        var decomposition = (VatMachineRecipe) recipe(helper, "carbonyl_decomposition");
        for (var vat : List.of(formation, decomposition)) {
            helper.assertTrue(vat.machines.isEmpty() && vat.getRequiredHeat() == HeatCondition.HEATED,
                    "carbonyl gas processing should retain its heat requirement without a powered mixer");
        }
        helper.assertTrue(takes(formation, "fundamentals:nickel_oxide") && takesFluid(formation, "fundamentals:water_gas")
                && givesFluid(formation, "fundamentals:nickel_carbonyl")
                && formation.getRollableResults().stream().anyMatch(r -> r.getStack().is(stack("fundamentals:platinum_group_concentrate").getItem())),
                "carbonyl formation should retain nickel feed, gas and the platinum-group byproduct");
        helper.assertTrue(takesFluid(decomposition, "fundamentals:nickel_carbonyl") && givesFluid(decomposition, "fundamentals:water_gas")
                && decomposition.getResultItem(registries).is(stack("fundamentals:nickel_pellets").getItem()),
                "carbonyl decomposition should retain nickel pellets and recovered gas");
        helper.succeed();
    }
}
