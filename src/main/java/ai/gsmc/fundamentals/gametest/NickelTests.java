package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.separation.Hazards;
import ai.gsmc.fundamentals.separation.Separation;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Nickel: ferronickel from carbon smelting, class 1 nickel by Mond's carbonyl, the cobalt in the refinery's liquor, and the carbonyl's poison. */
@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class NickelTests {

    private static ProcessingRecipe<?, ?> recipe(GameTestHelper helper, String id) {
        var found = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id));
        helper.assertTrue(found.isPresent(), id + " did not load");
        return (ProcessingRecipe<?, ?>) found.get().value();
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static boolean takes(Recipe<?> recipe, String id) {
        return recipe.getIngredients().stream().anyMatch(i -> i.test(stack(id)));
    }

    private static boolean gives(ProcessingRecipe<?, ?> recipe, String id) {
        return recipe.getRollableResults().stream().anyMatch(r -> r.getStack().is(stack(id).getItem()));
    }

    private static boolean takesFluid(ProcessingRecipe<?, ?> recipe, String id) {
        return recipe.getFluidIngredients().stream().anyMatch(i -> i.ingredient().test(new FluidStack(Separation.fluid(id), 1)));
    }

    private static boolean givesFluid(ProcessingRecipe<?, ?> recipe, String id) {
        return recipe.getFluidResults().stream().anyMatch(f -> f.is(Separation.fluid(id)));
    }

    @GameTest(template = "empty")
    public void carbonSmeltsNickelOreToFerronickel(GameTestHelper helper) {
        var laterite = recipe(helper, "fundamentals:uses/ferronickel_from_laterite");
        helper.assertTrue(takes(laterite, "fundamentals:raw_nickel_laterite") && gives(laterite, "fundamentals:ferronickel_ingot")
                && !gives(laterite, "tfmg:nickel_ingot"), "laterite smelted with charcoal should give ferronickel, not nickel");
        helper.assertTrue(gives(recipe(helper, "fundamentals:uses/ferronickel"), "fundamentals:ferronickel_ingot"), "roasted pentlandite with charcoal should give ferronickel");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/stainless_steel_from_ferronickel"), "fundamentals:ferronickel_ingot"), "stainless should take ferronickel");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void theMondProcessCarriesNickelOffAsCarbonyl(GameTestHelper helper) {
        var roast = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("fundamentals:roasting/nickel_oxide_campfire_cooking"));
        helper.assertTrue(roast.isPresent() && takes(roast.get().value(), "fundamentals:converter_matte_dust"), "converter matte should roast to nickel oxide");
        var volatiliser = recipe(helper, "fundamentals:uses/nickel_carbonyl");
        helper.assertTrue(takes(volatiliser, "fundamentals:nickel_oxide") && takesFluid(volatiliser, "water_gas") && givesFluid(volatiliser, "nickel_carbonyl"),
                "water gas should carry the nickel off the oxide as carbonyl");
        var decomposer = recipe(helper, "fundamentals:uses/carbonyl_decomposition");
        helper.assertTrue(takesFluid(decomposer, "nickel_carbonyl") && gives(decomposer, "fundamentals:nickel_pellets"), "the carbonyl should decompose to pellets");
        helper.assertTrue(takes(recipe(helper, "fundamentals:uses/nickel_ingot_from_pellets"), "fundamentals:nickel_pellets")
                && gives(recipe(helper, "fundamentals:uses/nickel_ingot_from_pellets"), "tfmg:nickel_ingot"), "the pellets should press to nickel");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void theNickelRefineryGivesCobalt(GameTestHelper helper) {
        var extraction = recipe(helper, "fundamentals:uses/cobalt_extraction");
        helper.assertTrue(takesFluid(extraction, "nickel_copper_sulfate") && takesFluid(extraction, "p507")
                && givesFluid(extraction, "cobalt_chloride_liquor") && givesFluid(extraction, "nickel_sulfate_liquor"), "P507 should part the cobalt from the nickel liquor");
        helper.assertTrue(takesFluid(recipe(helper, "fundamentals:uses/cobalt_electrowinning"), "cobalt_chloride_liquor")
                && gives(recipe(helper, "fundamentals:uses/cobalt_electrowinning"), "fundamentals:cobalt_ingot"), "the cobalt liquor should electrowin to cobalt");
        helper.assertTrue(gives(recipe(helper, "fundamentals:uses/nickel_electrowinning_from_raffinate"), "tfmg:nickel_ingot"), "the raffinate should electrowin to nickel");
        helper.succeed();
    }

    @GameTest(template = "battery", timeoutTicks = Hazards.CARBONYL_ONSET + 200)
    public void nickelCarbonylPoisonsTheUnmaskedNowAndLater(GameTestHelper helper) {
        helper.assertTrue(Hazards.chance(new FluidStack(Separation.fluid("nickel_carbonyl"), 1)) == 0, "nickel carbonyl should not eat its pipe");
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 2));
        float health = pig.getHealth();
        Hazards.leak(helper.getLevel(), helper.absolutePos(new BlockPos(4, 1, 2)));
        helper.runAfterDelay(30, () -> helper.assertTrue(pig.getHealth() < health && pig.hasEffect(MobEffects.CONFUSION), "an unmasked pig should be hurt at once"));
        helper.runAfterDelay(Hazards.CARBONYL_ONSET + 60, () -> {
            helper.assertTrue(pig.hasEffect(MobEffects.WITHER) || pig.isDeadOrDying(), "half a minute on, the pig's lungs should fill");
            helper.succeed();
        });
    }

    @GameTest(template = "battery", timeoutTicks = 200)
    public void theMaskKeepsNickelCarbonylOut(GameTestHelper helper) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 2));
        ItemStack tank = AllItems.COPPER_BACKTANK.asStack();
        tank.set(AllDataComponents.BACKTANK_AIR, BacktankUtil.maxAirWithoutEnchants());
        pig.setItemSlot(EquipmentSlot.HEAD, AllItems.COPPER_DIVING_HELMET.asStack());
        pig.setItemSlot(EquipmentSlot.CHEST, tank);
        float health = pig.getHealth();
        Hazards.leak(helper.getLevel(), helper.absolutePos(new BlockPos(4, 1, 2)));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(pig.getHealth() == health && !pig.hasEffect(MobEffects.CONFUSION), "a masked pig should breathe its tank");
            helper.assertTrue(BacktankUtil.getAir(pig.getItemBySlot(EquipmentSlot.CHEST)) < BacktankUtil.maxAirWithoutEnchants(), "the mask should spend air");
            helper.succeed();
        });
    }
}
