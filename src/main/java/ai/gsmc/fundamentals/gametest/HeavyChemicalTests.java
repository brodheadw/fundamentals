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
    public void sulfurComesFromOilPyriteAndSpentAcid(GameTestHelper helper) {
        ProcessingRecipe<?, ?> hydrotreater = processing(helper, "fundamentals:uses/hydrotreated_diesel");
        helper.assertTrue(takes(hydrotreater, "fundamentals:hydrotreating_catalyst") && takesFluid(hydrotreater, "tfmg:heavy_oil")
                && givesFluid(hydrotreater, "fundamentals:hydrogen_sulfide"),
                "hydrotreating takes heavy oil's sulfur off as hydrogen sulfide");
        ProcessingRecipe<?, ?> claus = processing(helper, "fundamentals:uses/claus_sulfur");
        helper.assertTrue(takesFluid(claus, "fundamentals:hydrogen_sulfide") && gives(claus, "tfmg:sulfur_dust"), "the Claus unit drops sulfur");
        ProcessingRecipe<?, ?> pyrite = processing(helper, "fundamentals:uses/raw_hematite_from_pyrite_with_sulfur_dioxide");
        helper.assertTrue(takes(pyrite, "fundamentals:raw_pyrite") && gives(pyrite, "fundamentals:raw_hematite") && givesFluid(pyrite, "fundamentals:sulfur_dioxide"),
                "pyrite roasts to cinder and sulfur dioxide");
        helper.assertTrue(takes(processing(helper, "fundamentals:uses/sulfur_dust_from_native_sulfur_milling"), "fundamentals:raw_native_sulfur"),
                "the Nether's fumarole crust grinds to sulfur dust");
        helper.assertTrue(givesFluid(processing(helper, "fundamentals:uses/alkylate"), "fundamentals:spent_acid"), "alkylation leaves spent acid");
        helper.assertTrue(givesFluid(processing(helper, "fundamentals:uses/sulfur_dioxide_from_spent_acid"), "fundamentals:sulfur_dioxide"),
                "spent acid regenerates to sulfur dioxide");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void deepslateMeltsAndCastsAsBlackstone(GameTestHelper helper) throws Exception {
        var melt = processing(helper, "fundamentals:rock/lava_from_cobbled_deepslate");
        helper.assertTrue(takes(melt, "minecraft:cobbled_deepslate") && givesFluid(melt, "minecraft:lava"), "cobbled deepslate should melt to lava");
        var cast = helper.getLevel().getRecipeManager().byKey(ai.gsmc.fundamentals.Fundamentals.id("rock/blackstone_from_lava_casting")).orElseThrow().value();
        helper.assertTrue(cast.getResultItem(helper.getLevel().registryAccess()).is(net.minecraft.world.item.Items.BLACKSTONE), "lava should cast as blackstone");
        // a mixer also runs shapeless crafting, so check a real superheated mixer picks the melt
        var basinPos = new net.minecraft.core.BlockPos(1, 2, 1);
        helper.setBlock(basinPos.below(), com.simibubi.create.AllBlocks.BLAZE_BURNER.getDefaultState()
                .setValue(com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL, com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.SEETHING));
        helper.setBlock(basinPos, com.simibubi.create.AllBlocks.BASIN.getDefaultState());
        helper.setBlock(basinPos.above(2), com.simibubi.create.AllBlocks.MECHANICAL_MIXER.getDefaultState());
        com.simibubi.create.content.processing.basin.BasinBlockEntity basin = helper.getBlockEntity(basinPos);
        basin.getInputInventory().insertItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLED_DEEPSLATE, 64), false);
        com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity mixer = helper.getBlockEntity(basinPos.above(2));
        var find = com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity.class.getDeclaredMethod("getMatchingRecipes");
        find.setAccessible(true);
        @SuppressWarnings("unchecked")
        var recipes = (java.util.List<net.minecraft.world.item.crafting.Recipe<?>>) find.invoke(mixer);
        helper.assertTrue(!recipes.isEmpty() && recipes.get(0) == melt, "a superheated mixer over cobbled deepslate should melt it first, got " + recipes);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public void aCastingBasinSetsLavaAsBlackstone(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 2, 1);
        var basin = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("tfmg:casting_basin"));
        helper.setBlock(pos, basin.defaultBlockState());
        var fluids = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), net.minecraft.core.Direction.UP);
        helper.assertTrue(fluids != null && fluids.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 144),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 144, "the casting basin should take a block's worth of lava");
        helper.succeedWhen(() -> {
            var items = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), net.minecraft.core.Direction.DOWN);
            boolean cast = false;
            for (int i = 0; items != null && i < items.getSlots(); i++) cast |= items.getStackInSlot(i).is(net.minecraft.world.item.Items.BLACKSTONE);
            helper.assertTrue(cast, "the basin should have cast blackstone");
        });
    }

    @GameTest(template = "empty")
    public void aSuperheatedMixerRoastsGalenaInAir(GameTestHelper helper) throws Exception {
        var basinPos = new net.minecraft.core.BlockPos(1, 2, 1);
        helper.setBlock(basinPos.below(), com.simibubi.create.AllBlocks.BLAZE_BURNER.getDefaultState()
                .setValue(com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL, com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.SEETHING));
        helper.setBlock(basinPos, com.simibubi.create.AllBlocks.BASIN.getDefaultState());
        helper.setBlock(basinPos.above(2), com.simibubi.create.AllBlocks.MECHANICAL_MIXER.getDefaultState());
        com.simibubi.create.content.processing.basin.BasinBlockEntity basin = helper.getBlockEntity(basinPos);
        basin.getInputInventory().insertItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse("fundamentals:galena_concentrate")), 64), false);
        com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity mixer = helper.getBlockEntity(basinPos.above(2));
        var find = com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity.class.getDeclaredMethod("getMatchingRecipes");
        find.setAccessible(true);
        var roast = processing(helper, "fundamentals:uses/roasted_galena_from_galena_with_sulfur_dioxide");
        helper.assertTrue(!((java.util.List<?>) find.invoke(mixer)).contains(roast), "galena with no air should not roast");
        var air = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse("tfmg:air"));
        var tanks = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, helper.absolutePos(basinPos), net.minecraft.core.Direction.UP);
        helper.assertTrue(tanks.fill(new net.neoforged.neoforge.fluids.FluidStack(air, 1000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 1000, "the basin should take air");
        @SuppressWarnings("unchecked")
        var recipes = (java.util.List<net.minecraft.world.item.crafting.Recipe<?>>) find.invoke(mixer);
        helper.assertTrue(!recipes.isEmpty() && recipes.get(0) == roast, "galena in air over a superheated burner should roast, got " + recipes);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void zincIsWonWithItsByproducts(GameTestHelper helper) {
        for (String id : new String[] {"create:smelting/zinc_ingot_from_raw_ore", "create:blasting/zinc_ingot_from_crushed", "create:splashing/crushed_raw_zinc",
                "fundamentals:roasting/zinc_oxide_from_sphalerite_smoking", "fundamentals:roasting/roasted_chalcopyrite_smoking",
                "fundamentals:roasting/roasted_pentlandite_campfire_cooking", "fundamentals:roasting/roasted_galena_smoking"}) {
            helper.assertTrue(recipe(helper, id).isEmpty(), id + " skips the zinc circuit");
        }
        helper.assertTrue(gives(processing(helper, "fundamentals:uses/zinc_oxide_from_sphalerite_with_sulfur_dioxide"), "fundamentals:calomel"),
                "the roaster gas gives up the ore's mercury as calomel");
        ProcessingRecipe<?, ?> purification = processing(helper, "fundamentals:uses/zinc_sulfate_liquor");
        for (String cake : new String[] {"fundamentals:copper_cement", "fundamentals:cobalt_cake", "fundamentals:cadmium_sponge"}) {
            helper.assertTrue(gives(purification, cake), "zinc dust cements out " + cake);
        }
        helper.assertTrue(gives(processing(helper, "fundamentals:uses/zinc_ingot"), "fundamentals:cadmium_sponge"), "a zinc retort distils cadmium first");
        var blast = processing(helper, "fundamentals:uses/lead_bullion_from_blast_smelting");
        helper.assertTrue(takes(blast, "fundamentals:roasted_galena") && takes(blast, "tfmg:coal_coke") && gives(blast, "fundamentals:lead_bullion_ingot"),
                "roasted galena should smelt to bullion with coke, not only in the bloomery");
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

    // alone in its batch: the blast reached a neighbouring test's crops
    @GameTest(template = "battery", timeoutTicks = 100, batch = "detonation")
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
