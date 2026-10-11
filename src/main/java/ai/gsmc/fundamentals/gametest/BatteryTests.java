package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class BatteryTests {

    private static Optional<Recipe<?>> recipe(GameTestHelper helper, String id) {
        return helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).map(holder -> holder.value());
    }

    private static ProcessingRecipe<?, ?> processing(GameTestHelper helper, String id) {
        return (ProcessingRecipe<?, ?>) recipe(helper, id).orElseThrow(() -> new AssertionError("no recipe " + id));
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static Fluid fluid(String id) {
        return BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
    }

    private static boolean takes(Recipe<?> recipe, String item) {
        return recipe.getIngredients().stream().anyMatch(i -> i.test(stack(item)));
    }

    private static boolean takesFluid(ProcessingRecipe<?, ?> recipe, String id) {
        return recipe.getFluidIngredients().stream().anyMatch(i -> i.test(new FluidStack(fluid(id), 1000)));
    }

    private static boolean gives(ProcessingRecipe<?, ?> recipe, String item) {
        return recipe.getRollableResults().stream().anyMatch(r -> r.getStack().is(stack(item).getItem()));
    }

    private static void step(GameTestHelper helper, String id, String from, String to) {
        var recipe = processing(helper, "fundamentals:uses/" + id);
        helper.assertTrue(takes(recipe, from) && gives(recipe, to), id + " should take " + from + " and give " + to);
    }

    private static void accumulator(GameTestHelper helper, String id, String cell) {
        Recipe<?> recipe = recipe(helper, id).orElseThrow(() -> new AssertionError("no recipe " + id));
        helper.assertTrue(takes(recipe, cell) && recipe.getResultItem(helper.getLevel().registryAccess()).is(stack("tfmg:accumulator").getItem()),
                id + " should build an accumulator from " + cell);
    }

    @GameTest(template = "empty")
    public void leadAcidCellsComeFromFormedPlates(GameTestHelper helper) {
        step(helper, "lead_grid", "fundamentals:antimonial_lead_plate", "fundamentals:lead_grid");
        step(helper, "litharge_from_lead", "tfmg:lead_ingot", "fundamentals:litharge");
        step(helper, "lead_paste", "fundamentals:litharge", "fundamentals:lead_paste");
        step(helper, "pasted_lead_plate", "fundamentals:lead_grid", "fundamentals:pasted_lead_plate");
        var formation = processing(helper, "fundamentals:uses/lead_plate_formation");
        helper.assertTrue(takes(formation, "fundamentals:pasted_lead_plate") && takesFluid(formation, "tfmg:sulfuric_acid")
                && gives(formation, "fundamentals:lead_dioxide_plate") && gives(formation, "fundamentals:sponge_lead_plate"),
                "formation should charge pasted plates to a lead dioxide and a sponge lead plate");
        var cell = processing(helper, "fundamentals:uses/lead_acid_cell");
        helper.assertTrue(takes(cell, "fundamentals:lead_dioxide_plate") && takes(cell, "fundamentals:sponge_lead_plate") && takes(cell, "tfmg:plastic_sheet")
                && takesFluid(cell, "tfmg:sulfuric_acid") && gives(cell, "fundamentals:lead_acid_cell"), "the plates in acid should make a lead-acid cell");
        accumulator(helper, "tfmg:crafting/materials/accumulator", "fundamentals:lead_acid_cell");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void nmcCellsNeedNickelManganeseCobaltAndLithium(GameTestHelper helper) {
        step(helper, "manganese_sulfate", "fundamentals:raw_pyrolusite", "fundamentals:manganese_sulfate");
        step(helper, "cobalt_sulfate", "fundamentals:cobalt_ingot", "fundamentals:cobalt_sulfate");
        var precursor = processing(helper, "fundamentals:uses/nmc_hydroxide");
        helper.assertTrue(takesFluid(precursor, "fundamentals:nickel_sulfate_liquor") && takes(precursor, "fundamentals:manganese_sulfate")
                && takes(precursor, "fundamentals:cobalt_sulfate") && gives(precursor, "fundamentals:nmc_hydroxide"),
                "the precursor should co-precipitate nickel, manganese and cobalt");
        var cathode = processing(helper, "fundamentals:uses/nmc_cathode");
        helper.assertTrue(takes(cathode, "fundamentals:nmc_hydroxide") && takes(cathode, "fundamentals:lithium_carbonate") && gives(cathode, "fundamentals:nmc_cathode"),
                "the precursor should calcine with lithium carbonate to NMC");
        var pentafluoride = processing(helper, "fundamentals:uses/phosphorus_pentafluoride");
        helper.assertTrue(takes(pentafluoride, "fundamentals:white_phosphorus") && takesFluid(pentafluoride, "fundamentals:fluorine")
                && pentafluoride.getFluidResults().stream().anyMatch(f -> f.getFluid() == fluid("fundamentals:phosphorus_pentafluoride")),
                "white phosphorus burnt in fluorine should give phosphorus pentafluoride");
        step(helper, "lithium_hexafluorophosphate", "fundamentals:lithium_fluoride", "fundamentals:lithium_hexafluorophosphate");
        var cell = processing(helper, "fundamentals:uses/lithium_charge_from_nmc_cathode");
        helper.assertTrue(takes(cell, "fundamentals:nmc_cathode") && takes(cell, "fundamentals:graphite") && takes(cell, "create:copper_sheet")
                && takes(cell, "tfmg:aluminum_sheet") && takesFluid(cell, "fundamentals:lithium_ion_electrolyte") && gives(cell, "tfmg:lithium_charge"),
                "an NMC cathode, a graphite anode, the foils and the electrolyte should make the lithium charge");
        helper.assertTrue(recipe(helper, "tfmg:crafting/materials/lithium_charge").isEmpty(), "lithium metal is not a lithium-ion cell");
        accumulator(helper, "tfmg:crafting/materials/accumulatorfrom_lithium", "tfmg:lithium_charge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void lfpCellsNeedIronPhosphorusAndLithium(GameTestHelper helper) {
        var phosphate = processing(helper, "fundamentals:uses/iron_phosphate");
        helper.assertTrue(takes(phosphate, "minecraft:iron_ingot") && takesFluid(phosphate, "fundamentals:phosphoric_acid") && gives(phosphate, "fundamentals:iron_phosphate"),
                "iron in phosphoric acid should give iron phosphate");
        var cathode = processing(helper, "fundamentals:uses/lfp_cathode");
        helper.assertTrue(takes(cathode, "fundamentals:iron_phosphate") && takes(cathode, "fundamentals:lithium_carbonate") && gives(cathode, "fundamentals:lfp_cathode"),
                "iron phosphate should calcine with lithium carbonate to LFP");
        step(helper, "lithium_charge_from_lfp_cathode", "fundamentals:lfp_cathode", "tfmg:lithium_charge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void nickelMetalHydrideCellsTakeTheLightRareEarths(GameTestHelper helper) {
        var alloy = processing(helper, "fundamentals:uses/hydride_alloy");
        helper.assertTrue(takes(alloy, "fundamentals:lanthanum_ingot") && takes(alloy, "fundamentals:cerium_ingot") && takes(alloy, "tfmg:nickel_ingot")
                && gives(alloy, "fundamentals:hydride_alloy"), "the hydride alloy should be lanthanum or cerium with nickel");
        var cell = processing(helper, "fundamentals:uses/nickel_metal_hydride_cell");
        helper.assertTrue(takes(cell, "fundamentals:hydride_alloy") && takes(cell, "fundamentals:nickel_hydroxide") && gives(cell, "fundamentals:nickel_metal_hydride_cell"),
                "a hydride anode and a nickel hydroxide cathode should make the cell");
        accumulator(helper, "fundamentals:uses/accumulator_from_nickel_metal_hydride", "fundamentals:nickel_metal_hydride_cell");
        helper.succeed();
    }
}
