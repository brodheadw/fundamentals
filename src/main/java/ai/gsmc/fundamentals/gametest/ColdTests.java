package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.cold.Cold;
import ai.gsmc.fundamentals.cold.Parka;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class ColdTests {

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static Recipe<?> recipe(GameTestHelper helper, String id) {
        return helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow(() -> new AssertionError("no recipe " + id)).value();
    }

    private static boolean takes(Recipe<?> recipe, String item) {
        return recipe.getIngredients().stream().anyMatch(i -> i.test(stack(item)));
    }

    private static boolean takesFluid(Recipe<?> recipe, String id) {
        return ((ProcessingRecipe<?, ?>) recipe).getFluidIngredients().stream()
                .anyMatch(i -> i.test(new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse(id)), 1000)));
    }

    private static boolean gives(GameTestHelper helper, Recipe<?> recipe, String item) {
        return recipe.getResultItem(helper.getLevel().registryAccess()).is(stack(item).getItem());
    }

    @GameTest(template = "empty")
    public void coldSweatReadsCelsiusInUnitsOfTwentyFiveDegrees(GameTestHelper helper) {
        helper.assertTrue(Cold.toColdSweat(0) == 0 && Cold.toColdSweat(25) == 1 && Cold.toColdSweat(-37.5) == -1.5,
                "0, 25 and -37.5 °C should be 0, 1 and -1.5 of Cold Sweat's units");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void polyesterIsSpunFromNaphthaAndEthylene(GameTestHelper helper) {
        var acid = recipe(helper, "fundamentals:cold/terephthalic_acid");
        helper.assertTrue(takesFluid(acid, "tfmg:naphtha") && takesFluid(acid, "tfmg:air") && takes(acid, "fundamentals:cobalt_sulfate")
                && gives(helper, acid, "fundamentals:terephthalic_acid"), "naphtha's xylene oxidised by air over cobalt should give terephthalic acid");
        var fibre = recipe(helper, "fundamentals:cold/polyester_fibre");
        helper.assertTrue(takes(fibre, "fundamentals:terephthalic_acid") && takesFluid(fibre, "tfmg:ethylene") && takesFluid(fibre, "minecraft:water")
                && gives(helper, fibre, "fundamentals:polyester_fibre"), "terephthalic acid and ethylene's glycol should condense to polyester fibre");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void aerogelStartsAsWaterGlassSetInABatt(GameTestHelper helper) {
        var glass = recipe(helper, "fundamentals:cold/sodium_silicate");
        helper.assertTrue(takes(glass, "minecraft:sand") && takes(glass, "fundamentals:soda_ash") && gives(helper, glass, "fundamentals:sodium_silicate"),
                "sand fused with soda ash should give sodium silicate");
        var gel = recipe(helper, "fundamentals:cold/silica_hydrogel");
        helper.assertTrue(takes(gel, "fundamentals:sodium_silicate") && takes(gel, "fundamentals:polyester_fibre") && takesFluid(gel, "tfmg:sulfuric_acid")
                && gives(helper, gel, "fundamentals:silica_hydrogel"), "acidified water glass should set to a gel in a polyester batt");
        boolean chemica = ModList.get().isLoaded("chemica");
        for (String step : new String[] {"aerogel_blanket", "eptfe_laminate", "nylon_ripstop"}) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(Fundamentals.id("cold/" + step)).isPresent() == chemica,
                    step + " needs Chemica's ethanol, PTFE or nylon, so it should exist exactly when Chemica does");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void everyParkaIsSewnFromItsShellAndFill(GameTestHelper helper) {
        String[][] tiers = {{"fur", "fundamentals:deer_hide"}, {"down", "fundamentals:waxed_canvas"},
                {"synthetic", "fundamentals:nylon_ripstop"}, {"expedition", "fundamentals:eptfe_laminate"}};
        String[] fills = {null, "fundamentals:down", "fundamentals:polyester_fibre", "fundamentals:aerogel_blanket"};
        helper.assertTrue(tiers.length == Parka.values().length, "every parka should be tested");
        for (int t = 0; t < tiers.length; t++) {
            for (String piece : new String[] {"hood", "parka", "trousers", "boots"}) {
                String id = tiers[t][0] + "_" + piece;
                var sewn = recipe(helper, "fundamentals:cold/" + id);
                helper.assertTrue(takes(sewn, tiers[t][1]) && (fills[t] == null || takes(sewn, fills[t])) && gives(helper, sewn, "fundamentals:" + id),
                        id + " should be sewn from " + tiers[t][1] + (fills[t] == null ? "" : " and " + fills[t]));
            }
        }
        var down = recipe(helper, "fundamentals:cold/down");
        helper.assertTrue(takes(down, "minecraft:feather") && ((ProcessingRecipe<?, ?>) down).getRollableResults().getFirst().getChance() < 1,
                "down should be washed out of feathers, a little from each");
        helper.succeed();
    }

    // only with Cold Sweat installed (-Pcoldsweat): its world temperature is ours, and our thermometer shows its gauge
    @GameTest(template = "empty")
    public void coldSweatsWorldTemperatureIsTheHeatGrids(GameTestHelper helper) {
        if (!ModList.get().isLoaded("cold_sweat")) {
            helper.succeed();
            return;
        }
        String failure = ColdSweatChecks.check(helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertTrue(failure == null, failure);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void theVacuumFlaskIsSilveredGlassPumpedOutAndCased(GameTestHelper helper) {
        var found = helper.getLevel().getRecipeManager().byKey(Fundamentals.id("cold/vacuum_flask"));
        if (!ModList.get().isLoaded("cold_sweat")) {
            helper.assertTrue(found.isEmpty(), "the vacuum flask is Cold Sweat's waterskin, so it needs Cold Sweat");
            helper.succeed();
            return;
        }
        var flask = (SequencedAssemblyRecipe) found.orElseThrow().value();
        var steps = flask.getSequence().stream().map(step -> step.getRecipe()).toList();
        helper.assertTrue(flask.getIngredient().test(stack("fundamentals:borosilicate_glass")) && gives(helper, flask, "fundamentals:vacuum_flask"),
                "a vacuum flask should be made from a borosilicate glass block");
        for (String part : new String[] {"fundamentals:silver_nugget", "minecraft:sugar", "create:mechanical_pump", "tfmg:rubber_sheet"}) {
            helper.assertTrue(steps.stream().anyMatch(step -> takes(step, part)), "the flask should take a " + part);
        }
        var steelPlate = TagKey.create(Registries.ITEM, ResourceLocation.parse("c:plates/steel"));
        helper.assertTrue(steps.stream().anyMatch(step -> step.getIngredients().stream().anyMatch(i -> i.getItems().length > 0
                && Arrays.stream(i.getItems()).allMatch(plate -> plate.is(steelPlate)))), "the flask should be cased in a steel plate");
        for (String fluid : new String[] {"fundamentals:nitric_acid", "fundamentals:ammonia"}) {
            helper.assertTrue(steps.stream().anyMatch(step -> takesFluid(step, fluid)), "the silvering should take " + fluid);
        }
        helper.succeed();
    }

    // only with Cold Sweat installed (-Pcoldsweat): the flask fills, pours and loses its heat as its waterskin does, ten times slower
    @GameTest(template = "empty")
    public void aVacuumFlaskKeepsItsHeatTenTimesLongerThanAWaterskin(GameTestHelper helper) {
        if (!ModList.get().isLoaded("cold_sweat")) {
            helper.succeed();
            return;
        }
        String failure = ColdSweatChecks.flask(helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertTrue(failure == null, failure);
        helper.succeed();
    }
}
