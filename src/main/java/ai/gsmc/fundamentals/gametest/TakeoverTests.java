package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
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
        Map<String, String> machines = Map.of("lithium/calcined_spodumene", "minecraft:blasting", "lithium/lithium_chloride", "create:mixing", "lithium/lithium_ingot", "tfmg:vat_machine_recipe",
                "aluminium/bauxite_powder", "create:milling", "aluminium/bauxite_powder_from_crushed", "create:milling");
        machines.forEach((id, machine) -> {
            var recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, id));
            helper.assertTrue(recipe.isPresent(), id + " did not load");
            helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.get().value().getType()).toString().equals(machine), id + " should be a " + machine + " recipe");
        });
        var lithium = (com.simibubi.create.content.processing.recipe.ProcessingRecipe<?, ?>) helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "lithium/lithium_ingot")).orElseThrow().value();
        helper.assertTrue(lithium.getFluidIngredients().isEmpty(), "lithium cannot be won from water: the vat should electrolyse the dry chloride");
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

    @GameTest(template = "empty")
    public void createCannotCrushOrSmeltOurOresStraightToMetal(GameTestHelper helper) {
        Map.of("raw_hematite", "c:raw_materials/iron", "raw_chalcopyrite", "c:raw_materials/copper", "raw_galena", "c:raw_materials/lead",
                "raw_sphalerite", "c:raw_materials/zinc", "raw_pentlandite", "c:raw_materials/nickel", "sphalerite_ore", "c:ores/zinc", "galena_ore", "c:ores/lead")
                .forEach((item, tag) -> helper.assertFalse(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, item)))
                        .is(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag))), item + " is in #" + tag + ", which Create crushes or smelts straight to metal"));
        var recipes = helper.getLevel().getRecipeManager();
        helper.assertTrue(recipes.byKey(ResourceLocation.parse("create:smelting/iron_ingot_from_crushed")).isEmpty(), "a furnace still reduces crushed iron ore");
        for (String id : new String[] {"fundamentals:uses/zinc_ingot", "fundamentals:uses/nickel_ingot"}) {
            helper.assertTrue(recipes.byKey(ResourceLocation.parse(id)).isPresent(), id + " did not load");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void chestsKeepAQuarterOfTheirIron(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable mineshaft = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.ABANDONED_MINESHAFT);
        int iron = 0, gold = 0;
        for (int i = 0; i < 400; i++) {
            for (ItemStack stack : mineshaft.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST))) {
                iron += stack.is(Items.IRON_INGOT) ? stack.getCount() : 0;
                gold += stack.is(Items.GOLD_INGOT) ? stack.getCount() : 0;
            }
        }
        helper.assertTrue(iron * 2 < gold * 3, "mineshaft chests gave " + iron + " iron ingots to " + gold + " gold; unthinned it is three to one");
        helper.succeed();
    }
}
