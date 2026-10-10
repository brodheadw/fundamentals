package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.liquid.Liquids;
import ai.gsmc.fundamentals.separation.Reagents;
import ai.gsmc.fundamentals.separation.Separation;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runs only with Chemica installed: ./gradlew -Pchemica runServer, or ORG_GRADLE_PROJECT_chemica=1 tools/gametest.sh. */
@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class ChemicaTests {

    private static ProcessingRecipe<?, ?> recipe(GameTestHelper helper, String id) {
        return (ProcessingRecipe<?, ?>) helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow().value();
    }

    @GameTest(template = "empty")
    public void chemicasReagentsAreOurs(GameTestHelper helper) {
        if (!ModList.get().isLoaded("chemica")) {
            helper.succeed();
            return;
        }
        Fluid theirs = BuiltInRegistries.FLUID.get(ResourceLocation.parse("chemica:hydrochloric_acid"));
        helper.assertTrue(Separation.reagent(theirs) == Separation.fluid("hydrochloric_acid") && Separation.kind(theirs) == Reagents.Kind.ACID,
                "Chemica's hydrochloric acid should stand for ours, in a mixer-settler and in a pipe");
        helper.assertTrue(Liquids.of(theirs) == Liquids.of(Separation.fluid("hydrochloric_acid")), "Chemica's hydrochloric acid should read our liquid properties");
        helper.assertTrue(recipe(helper, "fundamentals:mixing/chlorine").getFluidIngredients().getFirst().test(new FluidStack(theirs, 1000)),
                "our chlorine should come from Chemica's hydrochloric acid");
        var monomer = recipe(helper, "chemica:vat_machine_recipe/mixing/vinyl_chloride_monomer");
        helper.assertTrue(monomer.getFluidIngredients().stream().anyMatch(i -> i.test(new FluidStack(Separation.fluid("chlorine"), 1000)))
                        && monomer.getFluidResults().getFirst().getFluid() == Separation.fluid("vinyl_chloride"),
                "Chemica's vinyl chloride should take our chlorine and give ours; if not, data/chemica loaded before Chemica's own");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("chemica:mixing/salt")).isEmpty(),
                "salt is not boiled out of fresh water");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("tfmg:mixing/p_semiconductor")).orElseThrow().value()
                        .getIngredients().stream().anyMatch(i -> i.test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("fundamentals:boric_acid"))))),
                "boron dopes p-type silicon, not Chemica's iridium");
        helper.assertTrue(recipe(helper, "chemica:crushing/platinum_ingot").getIngredients().getFirst().test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("fundamentals:platinum_ingot")))),
                "Chemica's platinum should come from ours");
        var plains = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS).getGenerationSettings().features();
        helper.assertTrue(plains.stream().flatMap(HolderSet::stream).noneMatch(f -> f.unwrapKey().orElseThrow().location().getNamespace().equals("chemica")),
                "Chemica's generic ores should not generate");
        helper.succeed();
    }
}
