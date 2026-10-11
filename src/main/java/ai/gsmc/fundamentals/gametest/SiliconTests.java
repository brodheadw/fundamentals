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
public class SiliconTests {

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

    private static boolean givesFluid(ProcessingRecipe<?, ?> recipe, String id) {
        return recipe.getFluidResults().stream().anyMatch(f -> f.getFluid() == fluid(id));
    }

    @GameTest(template = "empty")
    public void quartzAndCarbonGiveMetallurgicalSilicon(GameTestHelper helper) {
        var arc = processing(helper, "fundamentals:uses/metallurgical_silicon");
        helper.assertTrue(takes(arc, "minecraft:quartz") && takes(arc, "tfmg:coal_coke") && givesFluid(arc, "tfmg:liquid_silicon"),
                "quartz and carbon in the arc vat should tap liquid silicon");
        helper.assertTrue(recipe(helper, "tfmg:industrial_blasting/silicon").isEmpty(), "a blast furnace does not reduce quartz to silicon");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void siemensDepositsPolysilicon(GameTestHelper helper) {
        helper.assertTrue(takes(processing(helper, "fundamentals:uses/crude_trichlorosilane"), "tfmg:silicon_ingot"),
                "metallurgical silicon should chlorinate to trichlorosilane");
        var siemens = processing(helper, "fundamentals:uses/polysilicon");
        helper.assertTrue(takesFluid(siemens, "fundamentals:trichlorosilane") && takesFluid(siemens, "tfmg:hydrogen") && gives(siemens, "fundamentals:polysilicon")
                && givesFluid(siemens, "fundamentals:silicon_tetrachloride"), "trichlorosilane and hydrogen should deposit polysilicon and give off the tetrachloride");
        helper.assertTrue(takesFluid(processing(helper, "fundamentals:uses/crude_trichlorosilane_from_tetrachloride"), "fundamentals:silicon_tetrachloride"),
                "the tetrachloride should go back to trichlorosilane");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void dopantsTakeWafersNotIngots(GameTestHelper helper) {
        for (String dopant : new String[] {"tfmg:mixing/n_semiconductor", "tfmg:mixing/p_semiconductor"}) {
            Recipe<?> mixing = recipe(helper, dopant).orElseThrow();
            helper.assertTrue(takes(mixing, "fundamentals:silicon_wafer") && !takes(mixing, "tfmg:silicon_ingot"), dopant + " should dope a wafer, not metallurgical silicon");
        }
        helper.assertTrue(gives(processing(helper, "fundamentals:uses/silicon_wafer"), "fundamentals:silicon_wafer"), "a boule should saw to wafers");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void galliumComesOutOfBayerLiquor(GameTestHelper helper) {
        helper.assertTrue(givesFluid(processing(helper, "fundamentals:uses/aluminium_hydroxide"), "fundamentals:spent_bayer_liquor"),
                "the precipitators should leave spent Bayer liquor");
        var cathode = processing(helper, "fundamentals:uses/sodium_gallate_liquor");
        helper.assertTrue(takesFluid(cathode, "fundamentals:spent_bayer_liquor") && givesFluid(cathode, "fundamentals:sodium_gallate_liquor")
                && givesFluid(cathode, "fundamentals:caustic_soda"), "the mercury cathode should take gallium out of the spent liquor and pass the caustic on");
        helper.assertTrue(gives(processing(helper, "fundamentals:uses/gallium"), "fundamentals:gallium_nugget"), "sodium gallate should electrolyse to gallium");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void indiumIsExtractedWithP204(GameTestHelper helper) {
        helper.assertTrue(gives(processing(helper, "fundamentals:uses/zinc_leach_liquor"), "fundamentals:zinc_leach_residue"), "the zinc leach should leave a residue");
        var extraction = processing(helper, "fundamentals:uses/indium_extraction");
        helper.assertTrue(takesFluid(extraction, "fundamentals:indium_sulfate_liquor") && takesFluid(extraction, "fundamentals:p204")
                && givesFluid(extraction, "fundamentals:indium_chloride_liquor"), "P204 should take indium out of the residue's leach");
        helper.assertTrue(gives(processing(helper, "fundamentals:uses/indium_sponge"), "fundamentals:indium_sponge"), "zinc should cement the indium");
        helper.succeed();
    }
}
