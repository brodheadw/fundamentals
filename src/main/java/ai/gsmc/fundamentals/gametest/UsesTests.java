package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/** The recipes that spend the rare earths, and the other mods' recipes taken over so that they need them. */
@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class UsesTests {

    private static Recipe<?> recipe(GameTestHelper helper, String id) {
        var found = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id));
        helper.assertTrue(found.isPresent(), id + " did not load");
        return found.get().value();
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static boolean takes(Recipe<?> recipe, String id) {
        return recipe.getIngredients().stream().anyMatch(i -> i.test(stack(id)));
    }

    @GameTest(template = "empty")
    public void theMagnetAlloysAreSinteredAndPolarizedIntoTheFactorysMagnet(GameTestHelper helper) {
        Map.of("fundamentals:uses/neodymium_iron_boron", "create:mixing", "fundamentals:uses/neodymium_iron_boron_from_didymium", "create:mixing",
                "fundamentals:uses/samarium_cobalt", "create:mixing", "fundamentals:uses/magnet_from_samarium_cobalt", "tfmg:polarizing")
                .forEach((id, type) -> helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.getKey(recipe(helper, id).getType()).toString().equals(type),
                        id + " should be a " + type + " recipe"));
        Recipe<?> magnet = recipe(helper, "tfmg:polarizing/magnet");
        helper.assertTrue(takes(magnet, "fundamentals:neodymium_iron_boron_ingot"), "the Factory's magnet should be polarized from NdFeB");
        helper.assertTrue(!takes(magnet, "tfmg:magnetic_alloy_ingot"), "the Factory's magnetic alloy should no longer make a magnet on its own");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/neodymium_iron_boron"), "fundamentals:raw_borax"), "NdFeB wants boron, from borax");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/samarium_cobalt"), "fundamentals:cobalt_ingot"), "SmCo wants cobalt metal");
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("fundamentals:borax_ore")), "borax should be an ore");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void theOtherRareEarthsAreSpentWhereTheyReallyAre(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        ItemStack striker = recipe(helper, "fundamentals:uses/ferrocerium_striker").getResultItem(registries);
        helper.assertTrue(striker.is(net.minecraft.world.item.Items.FLINT_AND_STEEL) && striker.has(DataComponents.UNBREAKABLE),
                "cerium and iron should make an unbreakable flint and steel");
        helper.assertTrue(takes(recipe(helper, "tfmg:vat_machine_recipe/naphtha"), "fundamentals:lanthanum_oxide"), "cracking naphtha should spend lanthanum oxide");
        helper.assertTrue(recipe(helper, "fundamentals:uses/phosphor").getResultItem(registries).is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("fundamentals:phosphor")))
                && takes(recipe(helper, "fundamentals:uses/phosphor"), "fundamentals:europium_oxide"), "europium, terbium and yttria should make phosphor");
        for (String lamp : new String[] {"tfmg:crafting/materials/aluminum_lamp", "tfmg:crafting/materials/circular_light"}) {
            helper.assertTrue(takes(recipe(helper, lamp), "fundamentals:phosphor"), lamp + " should take phosphor");
        }
        helper.assertTrue(takes(recipe(helper, "tfmg:crafting/materials/fireproof_chemical_vat"), "fundamentals:yttrium_oxide"), "the fireproof vat should take yttria");
        helper.assertTrue(takes(recipe(helper, "create:crafting/kinetics/goggles"), "fundamentals:didymium_glass"), "goggles should be didymium glass");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/didymium_glass"), "fundamentals:didymium_oxide"), "didymium glass wants didymium oxide");
        helper.assertTrue(recipe(helper, "fundamentals:uses/rose_glass").getResultItem(registries).is(net.minecraft.world.item.Items.PINK_STAINED_GLASS)
                && takes(recipe(helper, "fundamentals:uses/rose_glass"), "fundamentals:erbium_oxide"), "erbium should make pink glass");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/aluminium_scandium"), "fundamentals:scandium_ingot")
                && recipe(helper, "fundamentals:uses/panel_rack_from_scandium").getResultItem(registries).getCount() == 2, "scandium should lighten the rack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void cobaltCopperMolybdenumAndRheniumHaveTheirChains(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        for (String item : new String[] {"cobalt_ingot", "molybdenum_oxide", "molybdenum_ingot", "rhenium_ingot", "superalloy_plate", "molybdenum_steel_plate", "roasted_cobaltite", "roasted_chalcopyrite", "rhenium_flue_dust"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, item)), item + " is not an item");
        }
        Map.of("fundamentals:roasting/roasted_cobaltite_campfire_cooking", "minecraft:campfire_cooking", "fundamentals:uses/cobalt_ingot", "minecraft:blasting",
                "fundamentals:bloomery/copper_from_roasted_chalcopyrite", "fundamentals:bloomery", "fundamentals:uses/molybdenum_oxide", "create:mixing",
                "fundamentals:uses/molybdenum_ingot", "tfmg:vat_machine_recipe", "fundamentals:uses/rhenium_ingot", "tfmg:vat_machine_recipe",
                "fundamentals:uses/superalloy", "create:mixing", "fundamentals:uses/molybdenum_steel", "create:mixing")
                .forEach((id, type) -> helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.getKey(recipe(helper, id).getType()).toString().equals(type), id + " should be a " + type));
        helper.assertTrue(recipe(helper, "fundamentals:bloomery/copper_from_roasted_chalcopyrite").getResultItem(registries).is(net.minecraft.world.item.Items.COPPER_INGOT), "roasted chalcopyrite should bloom to copper");
        helper.assertTrue(takes(recipe(helper, "tfmg:turbine_blade"), "fundamentals:superalloy_plate"), "the turbine blade should take superalloy plates");
        helper.assertTrue(takes(recipe(helper, "tfmg:item_application/heavy_machinery_casing"), "fundamentals:molybdenum_steel_plate"), "the heavy casing should take molybdenum steel");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/superalloy"), "fundamentals:rhenium_ingot"), "the superalloy wants rhenium");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void tungstenAndTheLastSinks(GameTestHelper helper) {
        for (String item : new String[] {"tungsten_oxide", "tungsten_ingot", "tungsten_plate", "tungsten_carbide", "tungsten_filament"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, item)), item + " is not an item");
        }
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/tungsten_oxide_from_scheelite"), "fundamentals:raw_scheelite"), "scheelite should give the trioxide");
        helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.getKey(recipe(helper, "fundamentals:uses/tungsten_ingot").getType()).toString().equals("tfmg:vat_machine_recipe"), "tungsten is reduced in a vat");
        helper.assertTrue(takes(recipe(helper, "tfmg:crafting/materials/light_bulb"), "fundamentals:tungsten_filament"), "the light bulb should burn a tungsten filament");
        helper.assertTrue(takes(recipe(helper, "create:crafting/kinetics/mechanical_drill"), "fundamentals:tungsten_carbide"), "the drill should bite with carbide");
        helper.assertTrue(takes(recipe(helper, "tfmg:crafting/materials/exhaust"), "fundamentals:cerium_oxide"), "the exhaust should take ceria");
        helper.assertTrue(takes(recipe(helper, "tfmg:crafting/materials/lithium_charge"), "fundamentals:cobalt_ingot"), "the lithium charge should take cobalt");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/neodymium_glass"), "fundamentals:neodymium_oxide") && takes(recipe(helper, "fundamentals:uses/holmium_glass"), "fundamentals:holmium_oxide"),
                "neodymium and holmium should colour glass");
        helper.succeed();
    }
}
