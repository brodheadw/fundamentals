package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.uses.PlatinumMetals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/** The platinum metals: out of the nickel matte, through the precious-metal refinery, into the catalysts and the hard little parts. */
@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class PlatinumTests {

    private static final String[] METALS = {"platinum", "palladium", "rhodium", "ruthenium", "iridium", "osmium"};

    private static Recipe<?> recipe(GameTestHelper helper, String id) {
        var found = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id));
        helper.assertTrue(found.isPresent(), id + " did not load");
        return found.get().value();
    }

    private static boolean takes(Recipe<?> recipe, String id) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
        return recipe.getIngredients().stream().anyMatch(i -> i.test(stack));
    }

    private static String type(Recipe<?> recipe) {
        return BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()).toString();
    }

    @GameTest(template = "empty")
    public void theMatteCarriesThePlatinumMetalsToTheRefinery(GameTestHelper helper) {
        Map.of("fundamentals:bloomery/nickel_matte_from_pentlandite", "fundamentals:bloomery", "fundamentals:platinum/converter_matte_with_copper", "create:mixing",
                "fundamentals:platinum/matte_leach", "create:mixing", "fundamentals:platinum/nickel_electrowinning", "tfmg:vat_machine_recipe",
                "fundamentals:platinum/platinum_palladium_liquor", "create:mixing", "fundamentals:platinum/tetroxides", "tfmg:vat_machine_recipe",
                "fundamentals:platinum/platinum_sponge", "minecraft:blasting", "fundamentals:platinum/osmium_sponge", "tfmg:vat_machine_recipe")
                .forEach((id, type) -> helper.assertTrue(type(recipe(helper, id)).equals(type), id + " should be a " + type));
        helper.assertTrue(takes(recipe(helper, "fundamentals:platinum/converter_matte_with_copper"), "fundamentals:copper_matte_dust"), "copper matte should feed the converter");
        helper.assertTrue(takes(recipe(helper, "fundamentals:platinum/matte_leach_with_platinum_minerals"), "fundamentals:raw_sperrylite"), "the reef's minerals should go into the leach");
        helper.assertTrue(takes(recipe(helper, "fundamentals:platinum/ammonium_chloroplatinate"), "fundamentals:ammonium_chloride"), "platinum drops with ammonium chloride");
        for (String metal : METALS) {
            Recipe<?> sinter = recipe(helper, "fundamentals:platinum/" + metal + "_ingot");
            helper.assertTrue(type(sinter).equals("create:compacting") && takes(sinter, "fundamentals:" + metal + "_sponge"), metal + " sponge should be pressed to the ingot");
        }
        for (String id : PlatinumMetals.IDS) {
            helper.assertTrue(Fundamentals.class.getResource("/assets/fundamentals/textures/item/" + id + ".png") != null, id + " has no texture");
            helper.assertTrue(Fundamentals.class.getResource("/assets/fundamentals/models/item/" + id + ".json") != null, id + " has no model");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void everyPlatinumMetalHasASink(GameTestHelper helper) {
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/reforming_catalyst"), "fundamentals:platinum_nugget"), "the reforming catalyst wants platinum");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/reforming"), "fundamentals:reforming_catalyst"), "naphtha should reform over the catalyst");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/platinum_rhodium_gauze"), "fundamentals:rhodium_nugget"), "the gauze wants rhodium");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/nitric_acid_from_ammonia"), "fundamentals:platinum_rhodium_gauze"), "ammonia burns over the gauze");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/exhaust_three_way"), "fundamentals:palladium_nugget"), "palladium makes the exhaust go further");
        helper.assertFalse(takes(recipe(helper, "tfmg:crafting/materials/exhaust"), "fundamentals:palladium_nugget"), "an engine shouldn't wait on palladium");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/superalloy_with_ruthenium"), "fundamentals:ruthenium_nugget"), "ruthenium goes in the superalloy");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/iridium_spark_plug"), "fundamentals:iridium_nugget"), "the spark plug takes an iridium tip");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/light_bulb_from_osmium"), "fundamentals:osmium_filament"), "a bulb can burn an osmium filament");
        helper.succeed();
    }
}
