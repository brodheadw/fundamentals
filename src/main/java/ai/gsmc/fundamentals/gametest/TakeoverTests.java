package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class TakeoverTests {

    @GameTest(template = "empty")
    public void spodumeneAndBauxiteFeedTheFactorysMachines(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("fundamentals:spodumene_ore"))
                && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("fundamentals:raw_spodumene")), "spodumene should be an ore with a raw chunk");
        Map<String, String> machines = Map.of("lithium/calcined_spodumene", "minecraft:blasting", "lithium/lithium_ingot", "tfmg:vat_machine_recipe",
                "aluminium/bauxite_powder", "create:milling", "aluminium/bauxite_powder_from_crushed", "create:milling");
        machines.forEach((id, machine) -> {
            var recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, id));
            helper.assertTrue(recipe.isPresent(), id + " did not load");
            helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.get().value().getType()).toString().equals(machine), id + " should be a " + machine + " recipe");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void theFactorysLeadLithiumAndNickelOresNoLongerGenerate(GameTestHelper helper) {
        Registry<PlacedFeature> features = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        var plains = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS).getGenerationSettings().features();
        for (String ore : new String[] {"tfmg:lead_ore", "tfmg:lithium_ore", "tfmg:nickel_ore"}) {
            ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.parse(ore));
            helper.assertTrue(features.containsKey(key), ore + " is no longer a feature of The Factory Must Grow; our switch-off names the wrong thing");
            helper.assertTrue(plains.stream().noneMatch(step -> step.stream().anyMatch(feature -> feature.is(key))), ore + " still generates in the plains");
        }
        helper.succeed();
    }
}
