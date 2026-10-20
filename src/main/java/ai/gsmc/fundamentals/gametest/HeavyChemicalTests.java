package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.uses.Fertiliser;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class HeavyChemicalTests {

    private static Optional<Recipe<?>> recipe(GameTestHelper helper, String id) {
        return helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).map(holder -> holder.value());
    }

    private static ProcessingRecipe<?, ?> processing(GameTestHelper helper, String id) {
        return (ProcessingRecipe<?, ?>) recipe(helper, id).orElseThrow(() -> new AssertionError("no recipe " + id));
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static Fluid fluid(String id) {
        return BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
    }

    private static boolean takes(Recipe<?> recipe, String item) {
        return recipe.getIngredients().stream().anyMatch(i -> i.test(stack(item)));
    }

    private static boolean takesFluid(ProcessingRecipe<?, ?> recipe, String id) {
        return recipe.getFluidIngredients().stream().anyMatch(i -> i.test(new FluidStack(fluid(id), 1000)));
    }

    private static boolean gives(ProcessingRecipe<?, ?> recipe, String item) {
        return recipe.getRollableResults().stream().anyMatch(r -> r.getStack().is(stack(item).getItem()));
    }

    private static boolean givesFluid(ProcessingRecipe<?, ?> recipe, String id) {
        return recipe.getFluidResults().stream().anyMatch(f -> f.getFluid() == fluid(id));
    }

    @GameTest(template = "empty")
    public void theContactProcessRunsOverVanadiumPentoxide(GameTestHelper helper) {
        helper.assertTrue(takes(processing(helper, "fundamentals:uses/sulfur_dioxide"), "tfmg:sulfur_dust"), "sulfur burns to sulfur dioxide");
        ProcessingRecipe<?, ?> converter = processing(helper, "fundamentals:uses/sulfur_trioxide");
        helper.assertTrue(takes(converter, "fundamentals:vanadium_pentoxide") && takesFluid(converter, "fundamentals:sulfur_dioxide")
                && givesFluid(converter, "fundamentals:sulfur_trioxide"), "the converter takes sulfur dioxide over vanadium pentoxide to the trioxide");
        helper.assertTrue(takesFluid(processing(helper, "fundamentals:uses/oleum"), "fundamentals:sulfur_trioxide"), "the trioxide is absorbed to oleum");
        helper.assertTrue(givesFluid(processing(helper, "fundamentals:uses/sulfuric_acid_from_oleum"), "tfmg:sulfuric_acid"), "oleum lets down to The Factory's sulfuric acid");
        helper.assertTrue(takes(processing(helper, "fundamentals:uses/sodium_vanadate_liquor"), "fundamentals:raw_magnetite")
                && takes(processing(helper, "fundamentals:uses/vanadium_pentoxide"), "fundamentals:ammonium_chloride"),
                "vanadium pentoxide comes from magnetite without Chemica");
        helper.assertTrue(givesFluid(processing(helper, "fundamentals:uses/zinc_oxide_from_sphalerite_with_sulfur_dioxide"), "fundamentals:sulfur_dioxide"),
                "a sulfide roasted in a basin gives its sulfur dioxide up");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void zincIsWonWithItsByproducts(GameTestHelper helper) {
        for (String id : new String[] {"create:smelting/zinc_ingot_from_raw_ore", "create:blasting/zinc_ingot_from_crushed", "create:splashing/crushed_raw_zinc",
                "fundamentals:roasting/zinc_oxide_from_sphalerite_smoking"}) {
            helper.assertTrue(recipe(helper, id).isEmpty(), id + " skips the zinc circuit");
        }
        helper.assertTrue(gives(processing(helper, "fundamentals:uses/zinc_oxide_from_sphalerite_with_sulfur_dioxide"), "fundamentals:calomel"),
                "the roaster gas gives up the ore's mercury as calomel");
        ProcessingRecipe<?, ?> purification = processing(helper, "fundamentals:uses/zinc_sulfate_liquor");
        for (String cake : new String[] {"fundamentals:copper_cement", "fundamentals:cobalt_cake", "fundamentals:cadmium_sponge"}) {
            helper.assertTrue(gives(purification, cake), "zinc dust cements out " + cake);
        }
        helper.assertTrue(gives(processing(helper, "fundamentals:uses/zinc_ingot"), "fundamentals:cadmium_sponge"), "a zinc retort distils cadmium first");
        Recipe<?> jarosite = recipe(helper, "fundamentals:uses/zinc_leach_liquor_from_residue").orElseThrow(() -> new AssertionError("no hot acid leach"));
        helper.assertTrue(takes(jarosite, "fundamentals:zinc_leach_residue"), "the hot acid leach takes the ferrite residue");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void clinkerAndGypsumMakeTheFactorysCement(GameTestHelper helper) {
        ProcessingRecipe<?, ?> cement = processing(helper, "fundamentals:uses/portland_cement");
        helper.assertTrue(takes(cement, "fundamentals:cement_clinker") && takes(cement, "fundamentals:gypsum") && gives(cement, "tfmg:cement"),
                "clinker and gypsum grind to The Factory's cement");
        helper.assertTrue(takesFluid(processing(helper, "fundamentals:uses/gypsum"), "fundamentals:sulfur_dioxide"), "a lime scrubber makes the gypsum");
        helper.assertTrue(recipe(helper, "tfmg:mixing/cement").isEmpty(), "clay and limesand stirred together are not cement");
        Recipe<?> concrete = recipe(helper, "tfmg:vat_machine_recipe/concrete").orElseThrow(() -> new AssertionError("no liquid concrete"));
        helper.assertTrue(takes(concrete, "tfmg:cement") && !takes(concrete, "tfmg:limesand"), "The Factory's liquid concrete takes cement, not raw lime");
        helper.assertTrue(takes(processing(helper, "tfmg:mixing/concrete_mixture"), "tfmg:cement"), "The Factory's concrete mixture still takes cement");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void ammoniaAndNitricAcidMakeAmmoniumNitrate(GameTestHelper helper) {
        ProcessingRecipe<?, ?> nitrate = processing(helper, "fundamentals:uses/ammonium_nitrate");
        helper.assertTrue(takesFluid(nitrate, "fundamentals:ammonia") && takesFluid(nitrate, "fundamentals:nitric_acid")
                && gives(nitrate, "fundamentals:ammonium_nitrate"), "ammonia neutralised with nitric acid is ammonium nitrate");
        helper.assertTrue(takes(processing(helper, "fundamentals:uses/npk_fertiliser"), "tfmg:nitrate_dust"), "NPK takes saltpetre for its potash");
        helper.succeed();
    }

    @GameTest(template = "battery", timeoutTicks = 100)
    public void npkFeedsAPatch(GameTestHelper helper) {
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 0, 2), Blocks.FARMLAND);
            helper.setBlock(new BlockPos(x, 1, 2), Blocks.WHEAT);
        }
        Fertiliser npk = (Fertiliser) stack("fundamentals:npk_fertiliser").getItem();
        helper.assertTrue(npk.feed(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), null), "NPK should feed wheat");
        for (int x = 1; x <= 3; x++) {
            int age = helper.getBlockState(new BlockPos(x, 1, 2)).getValue(CropBlock.AGE);
            helper.assertTrue(age > 0, "every crop in the patch should grow, the one at " + x + " is at " + age);
        }
        helper.succeed();
    }

    @GameTest(template = "battery", timeoutTicks = 100)
    public void burningAmmoniumNitrateDetonatesOnlyInAMass(GameTestHelper helper) {
        BlockPos mass = new BlockPos(4, 1, 2), few = new BlockPos(20, 1, 2);
        for (BlockPos pos : new BlockPos[] {mass, few}) {
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos.north(), Blocks.DIRT);
            ItemEntity item = helper.spawnItem(stack("fundamentals:ammonium_nitrate").getItem(), pos.getX() + 0.5F, pos.getY(), pos.getZ() + 0.5F);
            item.getItem().setCount(pos == mass ? Fertiliser.DETONATES : 4);
            item.setRemainingFireTicks(100);
        }
        helper.runAfterDelay(10, () -> {
            helper.assertBlockPresent(Blocks.AIR, mass.north());
            helper.assertBlockPresent(Blocks.DIRT, few.north());
            helper.succeed();
        });
    }
}
