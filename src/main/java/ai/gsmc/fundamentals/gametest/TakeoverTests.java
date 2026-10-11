package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class TakeoverTests {

    @GameTest(template = "empty")
    public void theFactorysMetalOresNoLongerGenerateButItsOilDoes(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        Registry<PlacedFeature> features = registries.registryOrThrow(Registries.PLACED_FEATURE);
        var plains = registries.registryOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS).getGenerationSettings().features();
        for (String ore : new String[] {"tfmg:lead_ore", "tfmg:lithium_ore", "tfmg:nickel_ore", "tfmg:oil_deposit", "tfmg:tfmg_striated_ores_overworld"}) {
            ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.parse(ore));
            helper.assertTrue(features.containsKey(key), ore + " is no longer a feature of The Factory Must Grow; our override names the wrong thing");
            boolean placed = plains.stream().anyMatch(step -> step.stream().anyMatch(feature -> feature.is(key)));
            helper.assertTrue(placed == !ore.endsWith("_ore"), ore + (placed ? " still generates" : " no longer generates") + " in the plains");
        }
        String layers = ConfiguredFeature.DIRECT_CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries),
                registries.registryOrThrow(Registries.CONFIGURED_FEATURE).get(ResourceLocation.parse("tfmg:tfmg_striated_ores_overworld"))).getOrThrow().toString();
        helper.assertTrue(layers.contains("tfmg:lignite") && !layers.contains("tfmg:galena") && !layers.contains("tfmg:bauxite"),
                "The Factory's striated layers should keep lignite and lose galena and bauxite");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void theFactorysGalenaMinesAsOurs(GameTestHelper helper) {
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        for (String block : new String[] {"tfmg:lead_ore", "tfmg:galena"}) {
            List<ItemStack> drops = Block.getDrops(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block)).defaultBlockState(), helper.getLevel(),
                    helper.absolutePos(BlockPos.ZERO), null, null, pickaxe);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(BuiltInRegistries.ITEM.get(Fundamentals.id("raw_galena"))), block + " dropped " + drops);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void createCannotCrushOrSmeltOurOresStraightToMetal(GameTestHelper helper) {
        Map.of("raw_hematite", "c:raw_materials/iron", "raw_chalcopyrite", "c:raw_materials/copper", "raw_galena", "c:raw_materials/lead",
                "raw_sphalerite", "c:raw_materials/zinc", "raw_pentlandite", "c:raw_materials/nickel", "sphalerite_ore", "c:ores/zinc", "galena_ore", "c:ores/lead")
                .forEach((item, tag) -> helper.assertFalse(new ItemStack(BuiltInRegistries.ITEM.get(Fundamentals.id(item)))
                        .is(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag))), item + " is in #" + tag + ", which Create crushes or smelts straight to metal"));
        var recipes = helper.getLevel().getRecipeManager();
        helper.assertTrue(recipes.byKey(ResourceLocation.parse("create:smelting/iron_ingot_from_crushed")).isEmpty(), "a furnace still reduces crushed iron ore");
        for (String id : new String[] {"fundamentals:uses/zinc_ingot", "fundamentals:platinum/nickel_electrowinning"}) {
            helper.assertTrue(recipes.byKey(ResourceLocation.parse(id)).isPresent(), id + " did not load");
        }
        helper.succeed();
    }

    private static List<ItemStack> roll(ServerLevel level, ResourceKey<LootTable> key, int times, boolean raw) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        List<ItemStack> loot = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
            if (raw) {
                table.getRandomItemsRaw(params, loot::add);
            } else {
                loot.addAll(table.getRandomItems(params));
            }
        }
        return loot;
    }

    @GameTest(template = "empty")
    public void chestsAreThinnedOfIron(GameTestHelper helper) {
        int iron = 0, gold = 0;
        for (ItemStack stack : roll(helper.getLevel(), BuiltInLootTables.ABANDONED_MINESHAFT, 400, false)) {
            iron += stack.is(Items.IRON_INGOT) ? stack.getCount() : 0;
            gold += stack.is(Items.GOLD_INGOT) ? stack.getCount() : 0;
        }
        helper.assertTrue(iron * 2 < gold * 3, "mineshaft chests gave " + iron + " iron ingots to " + gold + " gold; unthinned it is three to one");
        Set<net.minecraft.world.item.Item> tools = Set.of(Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_HOE);
        for (var smith : List.of(BuiltInLootTables.VILLAGE_WEAPONSMITH, BuiltInLootTables.VILLAGE_TOOLSMITH)) {
            long raw = roll(helper.getLevel(), smith, 400, true).stream().filter(s -> tools.contains(s.getItem())).count();
            long kept = roll(helper.getLevel(), smith, 400, false).stream().filter(s -> tools.contains(s.getItem())).count();
            helper.assertTrue(raw > 100 && kept * 5 < raw * 2, smith.location() + " gave " + kept + " iron tools in 400 chests; unthinned it gave " + raw);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void rareEarthsAreFoundOnlyInEndAndAncientCities(GameTestHelper helper) {
        Set<String> common = Set.of("fundamentals:tin_nugget", "fundamentals:tin_ingot", "fundamentals:bronze_nugget", "fundamentals:bronze_ingot");
        List<String> village = roll(helper.getLevel(), BuiltInLootTables.VILLAGE_TOOLSMITH, 400, false).stream()
                .map(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()).filter(id -> id.startsWith("fundamentals:")).toList();
        helper.assertTrue(village.stream().anyMatch(common::contains), "village chests should turn up a little tin or bronze");
        helper.assertTrue(village.stream().allMatch(common::contains), "village chests gave " + village.stream().filter(id -> !common.contains(id)).distinct().toList());
        long rare = roll(helper.getLevel(), BuiltInLootTables.END_CITY_TREASURE, 200, false).stream()
                .filter(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace().equals(Fundamentals.MOD_ID)).count();
        helper.assertTrue(rare > 0, "200 end city chests gave no rare earth or platinum metal");
        helper.succeed();
    }
}
