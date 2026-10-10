package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.worldgen.DepositFeature;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
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
    public void theMagnetAlloysAreMeltedUnderArgon(GameTestHelper helper) {
        Map.of("fundamentals:uses/neodymium_iron_boron", "create:mixing", "fundamentals:uses/neodymium_iron_boron_with_gadolinium", "create:mixing",
                "fundamentals:uses/dysprosium_neodymium_iron_boron", "create:mixing", "fundamentals:uses/samarium_cobalt", "create:mixing")
                .forEach((id, type) -> helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.getKey(recipe(helper, id).getType()).toString().equals(type),
                        id + " should be a " + type + " recipe"));
        helper.assertTrue(!takes(recipe(helper, "fundamentals:uses/neodymium_iron_boron"), "fundamentals:dysprosium_ingot")
                && takes(recipe(helper, "fundamentals:uses/dysprosium_neodymium_iron_boron"), "fundamentals:terbium_ingot"), "only the heat grade wants dysprosium or terbium");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/neodymium_iron_boron"), "fundamentals:ferroboron")
                && takes(recipe(helper, "fundamentals:uses/ferroboron"), "fundamentals:raw_borax"), "NdFeB wants boron as ferroboron, from borax");
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
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/fluid_catalytic_cracking"), "fundamentals:lanthanum_oxide"), "cracking heavy oil should spend lanthanum oxide");
        helper.assertFalse(takes(recipe(helper, "tfmg:vat_machine_recipe/naphtha"), "fundamentals:lanthanum_oxide"), "steam cracking naphtha wants no catalyst");
        for (String vessel : new String[] {"fundamentals:mixer_settler", "fundamentals:magnetomigration_cell"}) {
            helper.assertTrue(takes(recipe(helper, vessel), "fundamentals:stainless_steel_plate"), vessel + " should not need plastic, which needs the olefins");
        }
        helper.assertTrue(recipe(helper, "fundamentals:uses/phosphor").getResultItem(registries).is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("fundamentals:phosphor")))
                && takes(recipe(helper, "fundamentals:uses/phosphor"), "fundamentals:europium_oxide")
                && takes(recipe(helper, "fundamentals:uses/phosphor"), "fundamentals:lanthanum_oxide"), "Y2O3:Eu and LaPO4:Ce,Tb should make phosphor");
        for (String lamp : new String[] {"tfmg:crafting/materials/aluminum_lamp", "tfmg:crafting/materials/circular_light"}) {
            helper.assertTrue(takes(recipe(helper, lamp), "fundamentals:phosphor"), lamp + " should take phosphor");
        }
        helper.assertTrue(takes(recipe(helper, "tfmg:crafting/materials/fireproof_chemical_vat"), "fundamentals:yttrium_oxide"), "the fireproof vat should take yttria");
        helper.assertTrue(takes(recipe(helper, "create:crafting/kinetics/goggles"), "fundamentals:didymium_glass"), "goggles should be didymium glass");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/didymium_glass"), "fundamentals:didymium_oxide"), "didymium glass wants didymium oxide");
        helper.assertTrue(recipe(helper, "fundamentals:uses/rose_glass").getResultItem(registries).is(net.minecraft.world.item.Items.PINK_STAINED_GLASS)
                && takes(recipe(helper, "fundamentals:uses/rose_glass"), "fundamentals:erbium_oxide"), "erbium should make pink glass");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/aluminium_scandium"), "fundamentals:scandium_nugget")
                && recipe(helper, "fundamentals:uses/panel_rack_from_scandium").getResultItem(registries).getCount() == 2, "scandium should lighten the rack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void theLeftoversHaveSinksAndTheOrphansAreGone(GameTestHelper helper) {
        ItemStack slag = ((ai.gsmc.fundamentals.ironworking.BloomeryRecipe) recipe(helper, "fundamentals:bloomery/iron_bloom")).byproduct();
        helper.assertTrue(recipe(helper, "tfmg:mixing/concrete_mixture_from_slag").getIngredients().stream().anyMatch(i -> i.test(slag)), "the bloomery's slag should go into concrete");
        helper.assertTrue(takes(recipe(helper, "tfmg:crafting/materials/gas_lamp"), "fundamentals:gas_mantle"), "the gas lamp should burn a mantle");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/exhaust_three_way"), "fundamentals:rhodium_nugget"), "the three-way converter wants rhodium");
        for (String gone : new String[] {"fundamentals:slag", "fundamentals:europium_ingot", "fundamentals:lutetium_ingot", "fundamentals:neodymium_dust",
                "fundamentals:aluminium_scandium_dust", "fundamentals:cobalt_dust", "fundamentals:molybdenum_dust", "fundamentals:rhenium_dust",
                "fundamentals:tungsten_dust", "fundamentals:monazite_dust", "fundamentals:neodymium_iron_boron_dust",
                "fundamentals:dysprosium_neodymium_iron_boron_dust", "fundamentals:samarium_cobalt_dust"}) {
            helper.assertFalse(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(gone)), gone + " should be gone");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void scandiumOxideAndMetalAreMadeSomewhere(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        for (String id : new String[] {"fundamentals:scandium_oxide", "fundamentals:scandium_ingot"}) {
            helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream().anyMatch(r -> r.value().getResultItem(registries).is(stack(id).getItem())),
                    id + " should have a recipe that makes it");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void titaniumIsWonFromIlmeniteAndRutileAndSpent(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/titania_slag"), "fundamentals:raw_ilmenite")
                && takes(recipe(helper, "fundamentals:uses/titanium_tetrachloride_from_titania_slag"), "fundamentals:titania_slag")
                && takes(recipe(helper, "fundamentals:uses/titanium_tetrachloride_from_rutile"), "fundamentals:raw_rutile"), "ilmenite and rutile should both chlorinate");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/titanium_ingot"), "fundamentals:titanium_sponge")
                && recipe(helper, "fundamentals:uses/titanium_ingot").getResultItem(registries).is(stack("fundamentals:titanium_ingot").getItem()), "the sponge should remelt to ingot");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/turbine_engine_from_titanium"), "fundamentals:titanium_plate"), "titanium should build the turbine engine");
        helper.succeed();
    }

    private static boolean gives(GameTestHelper helper, String recipe, String id) {
        Recipe<?> found = recipe(helper, recipe);
        return found.getResultItem(helper.getLevel().registryAccess()).is(stack(id).getItem())
                || found instanceof ProcessingRecipe<?, ?> processing && processing.getRollableResults().stream().anyMatch(r -> r.getStack().is(stack(id).getItem()));
    }

    private static boolean carries(GameTestHelper helper, String deposit, String ore) {
        var feature = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, deposit));
        if (feature != null && feature.config() instanceof DepositFeature.Config config) {
            return config.ores().stream().anyMatch(o -> BuiltInRegistries.BLOCK.getKey(o.state().getBlock()).getPath().equals(ore));
        }
        return feature != null && feature.config() instanceof OreConfiguration config
                && config.targetStates.stream().anyMatch(t -> BuiltInRegistries.BLOCK.getKey(t.state.getBlock()).getPath().equals(ore));
    }

    @GameTest(template = "empty")
    public void zirconAndHafniumTakeTheKrollRoadFromTheSands(GameTestHelper helper) {
        helper.assertTrue(carries(helper, "placer_zircon", "zircon_ore") && carries(helper, "syenite_massif", "zircon_ore"), "zircon should lie in the sands and the syenite");
        helper.assertTrue(gives(helper, "fundamentals:washing/raw_zircon", "fundamentals:zircon_concentrate")
                && takes(recipe(helper, "fundamentals:uses/crude_zirconium_tetrachloride"), "fundamentals:zircon_concentrate")
                && gives(helper, "fundamentals:uses/zirconium_tetrachloride", "fundamentals:hafnium_tetrachloride"), "zircon should wash, chlorinate and give up its hafnium");
        for (String metal : new String[] {"zirconium", "hafnium"}) {
            helper.assertTrue(takes(recipe(helper, "fundamentals:uses/" + metal + "_sponge"), "fundamentals:" + metal + "_tetrachloride")
                    && takes(recipe(helper, "fundamentals:uses/" + metal + "_sponge"), "fundamentals:magnesium_ingot")
                    && gives(helper, "fundamentals:uses/" + metal + "_ingot", "fundamentals:" + metal + "_ingot"), metal + " should be reduced by magnesium and arc-melted");
        }
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/yttria_stabilised_zirconia"), "fundamentals:zirconium_oxide")
                && takes(recipe(helper, "fundamentals:uses/yttria_stabilised_zirconia"), "fundamentals:yttrium_oxide")
                && takes(recipe(helper, "tfmg:turbine_blade"), "fundamentals:yttria_stabilised_zirconia"), "zirconia and yttria should coat the turbine blade");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void berylliumIsWonFromBerylAndBertrandite(GameTestHelper helper) {
        helper.assertTrue(carries(helper, "pegmatite_dyke", "beryl_ore") && carries(helper, "beryllium_tuff", "bertrandite_ore"),
                "beryl should be in the pegmatite and bertrandite in the tuff");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/beryl_frit"), "fundamentals:raw_beryl")
                && takes(recipe(helper, "fundamentals:uses/beryllium_sulfate_liquor"), "fundamentals:beryl_frit")
                && takes(recipe(helper, "fundamentals:uses/beryllium_sulfate_liquor_from_bertrandite"), "fundamentals:raw_bertrandite"), "beryl frit and bertrandite should leach");
        helper.assertTrue(gives(helper, "fundamentals:uses/beryllium_hydroxide", "fundamentals:beryllium_hydroxide")
                && takes(recipe(helper, "fundamentals:uses/ammonium_fluoroberyllate"), "fundamentals:beryllium_hydroxide")
                && takes(recipe(helper, "fundamentals:uses/beryllium_fluoride"), "fundamentals:ammonium_fluoroberyllate")
                && takes(recipe(helper, "fundamentals:uses/beryllium_pebbles"), "fundamentals:beryllium_fluoride")
                && takes(recipe(helper, "fundamentals:uses/beryllium_pebbles"), "fundamentals:magnesium_ingot")
                && gives(helper, "fundamentals:uses/beryllium_ingot", "fundamentals:beryllium_ingot"), "the liquor should reach the metal through the fluoride");
        helper.assertTrue(gives(helper, "fundamentals:uses/beryllium_copper_from_oxide", "fundamentals:beryllium_copper_block")
                && takes(recipe(helper, "fundamentals:uses/cable_connector_from_beryllium_copper"), "fundamentals:beryllium_copper_ingot"), "beryllium should go into copper and the connector");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void cobaltCopperMolybdenumAndRheniumHaveTheirChains(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        for (String item : new String[] {"cobalt_ingot", "molybdenum_oxide", "molybdenum_ingot", "rhenium_ingot", "superalloy_plate", "molybdenum_steel_plate", "roasted_cobaltite", "roasted_chalcopyrite", "rhenium_flue_dust",
                "copper_matte_dust", "blister_copper_ingot"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, item)), item + " is not an item");
        }
        Map.of("fundamentals:roasting/roasted_cobaltite_campfire_cooking", "minecraft:campfire_cooking", "fundamentals:uses/cobalt_ingot", "tfmg:vat_machine_recipe",
                "fundamentals:bloomery/copper_matte_from_roasted_chalcopyrite", "fundamentals:bloomery", "fundamentals:uses/blister_copper", "create:mixing",
                "fundamentals:uses/copper_ingot_from_blister_copper", "minecraft:blasting", "fundamentals:uses/molybdenum_oxide", "create:mixing",
                "fundamentals:uses/molybdenum_ingot", "tfmg:vat_machine_recipe", "fundamentals:uses/rhenium_ingot", "tfmg:vat_machine_recipe",
                "fundamentals:uses/superalloy", "create:mixing", "fundamentals:uses/molybdenum_steel", "create:mixing")
                .forEach((id, type) -> helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.getKey(recipe(helper, id).getType()).toString().equals(type), id + " should be a " + type));
        helper.assertTrue(recipe(helper, "fundamentals:bloomery/copper_matte_from_roasted_chalcopyrite").getResultItem(registries).is(stack("fundamentals:copper_matte_dust").getItem()),
                "roasted chalcopyrite should smelt to matte, not copper");
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

    @GameTest(template = "empty")
    public void chromiteGivesFerrochromeAndTheLongWayChromium(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        Map.of("fundamentals:washing/chromite_dust", "chromite_concentrate", "fundamentals:uses/ferrochrome", "ferrochrome_ingot",
                "fundamentals:uses/stainless_steel", "stainless_steel_ingot", "fundamentals:uses/soda_ash", "soda_ash",
                "fundamentals:uses/sodium_chromate", "sodium_chromate", "fundamentals:uses/sodium_dichromate", "sodium_dichromate",
                "fundamentals:uses/chromium_oxide", "chromium_oxide", "fundamentals:uses/chromium_ingot", "chromium_ingot")
                .forEach((id, out) -> helper.assertTrue(recipe(helper, id).getResultItem(registries).is(stack("fundamentals:" + out).getItem()), id + " should make " + out));
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/sodium_chromate"), "fundamentals:chromite_concentrate"), "the soda roast wants chromite");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/chromium_ingot"), "fundamentals:aluminium_powder"), "chromium is reduced by aluminium");
        helper.assertTrue(takes(recipe(helper, "tfmg:crafting/materials/flarestack"), "fundamentals:stainless_steel_ingot"), "the flarestack should be stainless");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/superalloy"), "fundamentals:chromium_ingot"), "the superalloy wants chromium");
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("fundamentals:trona_ore")), "trona should be an ore");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void bauxiteGoesThroughBayerToAlumina(GameTestHelper helper) {
        ProcessingRecipe<?, ?> digest = (ProcessingRecipe<?, ?>) recipe(helper, "fundamentals:uses/sodium_aluminate_liquor");
        helper.assertTrue(takes(digest, "tfmg:bauxite_powder") && digest.getRollableResults().stream().anyMatch(r -> r.getStack().is(stack("fundamentals:red_mud").getItem())),
                "bauxite should digest to aluminate liquor and leave red mud");
        ProcessingRecipe<?, ?> precipitate = (ProcessingRecipe<?, ?>) recipe(helper, "fundamentals:uses/aluminium_hydroxide");
        helper.assertTrue(precipitate.getRollableResults().stream().anyMatch(r -> r.getStack().is(stack("fundamentals:aluminium_hydroxide").getItem())),
                "the liquor should throw down aluminium hydroxide");
        Recipe<?> calcine = recipe(helper, "fundamentals:uses/alumina");
        helper.assertTrue(takes(calcine, "fundamentals:aluminium_hydroxide") && calcine.getResultItem(helper.getLevel().registryAccess()).is(stack("fundamentals:alumina").getItem()),
                "the hydroxide should calcine to alumina");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void hallHeroultMakesTheFactorysAluminium(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("tfmg:vat_machine_recipe/aluminum")).isEmpty(),
                "bauxite powder should no longer electrolyse straight to aluminium");
        ProcessingRecipe<?, ?> pot = (ProcessingRecipe<?, ?>) recipe(helper, "fundamentals:uses/aluminium_ingot");
        helper.assertTrue(takes(pot, "fundamentals:alumina") && takes(pot, "fundamentals:cryolite") && takes(pot, "tfmg:coal_coke"),
                "the pot takes alumina in cryolite and burns a carbon anode");
        helper.assertTrue(pot.getRollableResults().stream().anyMatch(r -> r.getStack().is(stack("tfmg:aluminum_ingot").getItem())), "the pot should give the Factory's aluminium");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void aSparkPlugIsAnAluminaInsulator(GameTestHelper helper) {
        for (String id : new String[] {"tfmg:mechanical_crafting/spark_plug", "fundamentals:uses/iridium_spark_plug", "fundamentals:uses/platinum_spark_plug"}) {
            helper.assertTrue(takes(recipe(helper, id), "fundamentals:alumina") && !takes(recipe(helper, id), "tfmg:aluminum_ingot"), id + " should insulate with alumina, not aluminium");
        }
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/platinum_spark_plug"), "fundamentals:platinum_nugget"), "a plug can be platinum-tipped");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void tinIsWonFromCassiteriteAndSpentOnBronzeAndSolder(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        Map.of("fundamentals:uses/tin_concentrate", "tin_concentrate", "fundamentals:roasting/roasted_tin_concentrate_smoking", "roasted_tin_concentrate",
                "fundamentals:bloomery/crude_tin_from_roasted_tin_concentrate", "crude_tin_ingot", "fundamentals:uses/tin_ingot", "tin_ingot")
                .forEach((id, out) -> helper.assertTrue(recipe(helper, id).getResultItem(registries).is(stack("fundamentals:" + out).getItem()), id + " should make " + out));
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/tin_concentrate"), "fundamentals:raw_cassiterite")
                && takes(recipe(helper, "fundamentals:uses/tin_ingot"), "fundamentals:crude_tin_ingot"), "tin should be won from cassiterite");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/bronze_ingot"), "fundamentals:tin_ingot")
                && takes(recipe(helper, "fundamentals:uses/solder"), "fundamentals:tin_ingot"), "tin should go into bronze and solder");
        helper.assertTrue(takes(recipe(helper, "create:crafting/curiosities/peculiar_bell"), "fundamentals:bronze_plate"), "the peculiar bell should be bronze");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void silverComesOutOfLeadAndGoesIntoContacts(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        for (String id : new String[] {"fundamentals:uses/cupellation_of_crust", "fundamentals:uses/cupellation_of_argentite"}) {
            helper.assertTrue(recipe(helper, id).getResultItem(registries).is(stack("fundamentals:silver_nugget").getItem())
                    || recipe(helper, id).getResultItem(registries).is(stack("fundamentals:silver_ingot").getItem()), id + " should give silver");
        }
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/parkes_desilvering"), "fundamentals:lead_bullion_ingot"), "zinc should desilver lead bullion");
        helper.assertTrue(takes(recipe(helper, "tfmg:crafting/materials/electrical_switch"), "fundamentals:silver_plate"), "the Factory's switch should take silver contacts");
        helper.succeed();
    }
}
