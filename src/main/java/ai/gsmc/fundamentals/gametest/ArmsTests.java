package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class ArmsTests {

    private static ProcessingRecipe<?, ?> processing(GameTestHelper helper, String id) {
        return (ProcessingRecipe<?, ?>) helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id))
                .orElseThrow(() -> new AssertionError("no recipe " + id)).value();
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static Fluid fluid(String id) {
        return BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
    }

    private static void step(GameTestHelper helper, String id, String from, String to) {
        ProcessingRecipe<?, ?> recipe = processing(helper, id);
        helper.assertTrue(recipe.getIngredients().stream().anyMatch(i -> i.test(stack(from)))
                && recipe.getRollableResults().stream().anyMatch(r -> r.getStack().is(stack(to).getItem())), id + " should take " + from + " to " + to);
    }

    @GameTest(template = "empty")
    public void blackPowderIsCornedFromSaltpetreCharcoalAndSulfur(GameTestHelper helper) {
        step(helper, "fundamentals:arms/black_powder_mill_cake", "tfmg:nitrate_dust", "fundamentals:mill_cake");
        step(helper, "fundamentals:arms/black_powder_mill_cake", "minecraft:charcoal", "fundamentals:mill_cake");
        step(helper, "fundamentals:arms/black_powder_mill_cake", "tfmg:sulfur_dust", "fundamentals:mill_cake");
        step(helper, "fundamentals:arms/black_powder_press_cake", "fundamentals:mill_cake", "fundamentals:press_cake");
        step(helper, "fundamentals:arms/black_powder", "fundamentals:press_cake", "minecraft:gunpowder");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("tfmg:mixing/gunpowder")).isEmpty(),
                "The Factory's dry-mixed gunpowder should be gone");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void guncottonIsPaperInMixedAcidAndGelatinisesToSmokelessPowder(GameTestHelper helper) {
        ProcessingRecipe<?, ?> nitration = processing(helper, "fundamentals:arms/nitrocellulose");
        step(helper, "fundamentals:arms/nitrocellulose", "minecraft:paper", "fundamentals:nitrocellulose");
        helper.assertTrue(nitration.getFluidIngredients().stream().anyMatch(i -> i.test(new FluidStack(fluid("fundamentals:nitric_acid"), 1000)))
                && nitration.getFluidIngredients().stream().anyMatch(i -> i.test(new FluidStack(fluid("tfmg:sulfuric_acid"), 1000))),
                "guncotton is nitrated in nitric and sulfuric acid together");
        String powder = ModList.get().isLoaded("chemica") ? "fundamentals:arms/smokeless_powder_poudre_b" : "fundamentals:arms/smokeless_powder_schultze";
        step(helper, powder, "fundamentals:nitrocellulose", "fundamentals:smokeless_powder");
        helper.succeed();
    }
}
