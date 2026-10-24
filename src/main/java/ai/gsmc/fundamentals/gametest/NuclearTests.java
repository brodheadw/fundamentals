package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.content.nuclear.Radiation;
import ai.gsmc.fundamentals.worldgen.DepositFeature;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class NuclearTests {

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
    public void uraniniteLiesInTheRollFront(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceKey<PlacedFeature> placed = ResourceKey.create(Registries.PLACED_FEATURE, Fundamentals.id("roll_front"));
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.DESERT).getGenerationSettings().features()
                .stream().anyMatch(step -> step.stream().anyMatch(feature -> feature.is(placed))), "the roll-front should generate under desert");
        DepositFeature.Config shipped = (DepositFeature.Config) level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(Fundamentals.id("roll_front")).config();
        DepositFeature.Config small = new DepositFeature.Config(shipped.shape(), shipped.host(), shipped.ores(), new DepositFeature.Range(6, 6),
                shipped.thickness(), shipped.height(), shipped.replaceable());
        BlockPos centre = helper.absolutePos(new BlockPos(1, 8, 1));
        Iterable<BlockPos> box = BlockPos.betweenClosed(centre.offset(-6, -5, -6), centre.offset(6, 5, 6));
        box.forEach(pos -> level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2));
        DepositFeature.INSTANCE.place(small, level, level.getChunkSource().getGenerator(), level.getRandom(), centre);
        int uraninite = 0;
        for (BlockPos pos : box) {
            uraninite += BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath().equals("uraninite_ore") ? 1 : 0;
        }
        box.forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2));
        helper.assertTrue(uraninite > 0, "a roll-front placed in stone should carry uraninite");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void uraniniteRunsToAFuelPellet(GameTestHelper helper) {
        helper.assertTrue(takes(processing(helper, "fundamentals:uses/uranyl_sulfate_liquor"), "fundamentals:uraninite_dust")
                && givesFluid(processing(helper, "fundamentals:uses/uranyl_sulfate_liquor"), "fundamentals:uranyl_sulfate_liquor"), "uraninite dust should leach to uranyl sulfate");
        var yellowcake = processing(helper, "fundamentals:uses/yellowcake");
        helper.assertTrue(takesFluid(yellowcake, "fundamentals:uranyl_sulfate_liquor") && gives(yellowcake, "fundamentals:yellowcake"), "the liquor should give yellowcake");
        var greenSalt = processing(helper, "fundamentals:uses/uranium_tetrafluoride");
        helper.assertTrue(takes(greenSalt, "fundamentals:yellowcake") && takesFluid(greenSalt, "fundamentals:hydrofluoric_acid")
                && gives(greenSalt, "fundamentals:uranium_tetrafluoride"), "yellowcake should hydrofluorinate to green salt");
        var hexafluoride = processing(helper, "fundamentals:uses/uranium_hexafluoride");
        helper.assertTrue(takes(hexafluoride, "fundamentals:uranium_tetrafluoride") && takesFluid(hexafluoride, "fundamentals:fluorine")
                && givesFluid(hexafluoride, "fundamentals:uranium_hexafluoride"), "green salt and fluorine should give uranium hexafluoride");
        var centrifuge = processing(helper, "fundamentals:uses/uranium_enrichment");
        helper.assertTrue(takesFluid(centrifuge, "fundamentals:uranium_hexafluoride") && givesFluid(centrifuge, "fundamentals:enriched_uranium_hexafluoride")
                && givesFluid(centrifuge, "fundamentals:depleted_uranium_hexafluoride"), "the centrifuge should split enriched from depleted");
        var oxide = processing(helper, "fundamentals:uses/uranium_dioxide");
        helper.assertTrue(takesFluid(oxide, "fundamentals:enriched_uranium_hexafluoride") && gives(oxide, "fundamentals:uranium_dioxide"),
                "enriched hexafluoride should convert to the dioxide");
        var pellet = processing(helper, "fundamentals:uses/uranium_fuel_pellet");
        helper.assertTrue(takes(pellet, "fundamentals:uranium_dioxide") && gives(pellet, "fundamentals:uranium_fuel_pellet"), "the dioxide should sinter to a pellet");
        helper.assertTrue(takes(processing(helper, "fundamentals:uses/fluorine"), "fundamentals:potassium_bifluoride"), "fluorine comes from a bifluoride cell");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void createNuclearsRodsTakeOurMetals(GameTestHelper helper) {
        // Passes without Create Nuclear; run it with ORG_GRADLE_PROJECT_createnuclear=1 tools/gametest.sh.
        if (!ModList.get().isLoaded("createnuclear")) {
            helper.succeed();
            return;
        }
        Recipe<?> control = recipe(helper, "createnuclear:mechanical_crafting/graphite_rod").orElseThrow();
        helper.assertTrue(takes(control, "fundamentals:hafnium_ingot") && !takes(control, "createnuclear:graphene"), "the control rod should be hafnium");
        helper.assertTrue(takes(recipe(helper, "fundamentals:createnuclear/control_rod_from_boron_carbide").orElseThrow(), "fundamentals:boron_carbide"),
                "a control rod should also come from boron carbide");
        Recipe<?> fuel = recipe(helper, "createnuclear:mechanical_crafting/uranium_rod").orElseThrow();
        helper.assertTrue(takes(fuel, "fundamentals:uranium_fuel_pellet") && takes(fuel, "fundamentals:zircaloy_tube"),
                "a fuel rod should be our pellets in a Zircaloy tube; if not, data/createnuclear loaded before Create Nuclear's own");
        helper.assertTrue(takes(recipe(helper, "createnuclear:mechanical_crafting/reactor_core").orElseThrow(), "fundamentals:beryllium_ingot"),
                "the reactor core should take a beryllium reflector");
        helper.assertTrue(recipe(helper, "createnuclear:crushing/granite").isEmpty() && recipe(helper, "createnuclear:mixing/steel").isEmpty(),
                "granite is not uranium ore and steel is not stirred cold");
        var plains = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS).getGenerationSettings().features();
        helper.assertTrue(plains.stream().flatMap(HolderSet::stream).noneMatch(f -> f.unwrapKey().orElseThrow().location().toString().endsWith("_ore")
                        && f.unwrapKey().orElseThrow().location().getNamespace().equals("createnuclear")),
                "Create Nuclear's generic ores should not generate");
        helper.assertTrue(Radiation.dose(stack("fundamentals:yellowcake")) > 0, "our yellowcake should be radioactive to Create Nuclear");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void spentFuelCoolsOnlyUnderWater(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.WATER);
        var wet = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), 0, 0, 0, stack("fundamentals:spent_fuel_rod"));
        var dry = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), 0, 0, 0, stack("fundamentals:spent_fuel_rod"));
        var water = helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1.2, 1.5));
        var air = helper.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1.2, 3.5));
        wet.setPos(water);
        dry.setPos(air);
        helper.getLevel().addFreshEntity(wet);
        helper.getLevel().addFreshEntity(dry);
        helper.runAfterDelay(2, () -> {
            ai.gsmc.fundamentals.content.nuclear.SpentFuel.pool(wet, ai.gsmc.fundamentals.content.nuclear.SpentFuel.COOL_TICKS);
            ai.gsmc.fundamentals.content.nuclear.SpentFuel.pool(dry, ai.gsmc.fundamentals.content.nuclear.SpentFuel.COOL_TICKS);
            helper.assertTrue(wet.getItem().is(stack("fundamentals:cooled_spent_fuel").getItem()), "a day under water cools spent fuel");
            helper.assertTrue(dry.getItem().is(stack("fundamentals:spent_fuel_rod").getItem()), "spent fuel out of water stays hot");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public void wasteRestsOnlyInDeepRock(GameTestHelper helper) {
        BlockPos canister = new BlockPos(2, 2, 2);
        helper.setBlock(canister, BuiltInRegistries.BLOCK.get(Fundamentals.id("waste_canister")));
        var entombed = (java.util.function.Supplier<Boolean>) () ->
                ai.gsmc.fundamentals.content.nuclear.WasteCanisterBlock.entombed(helper.getLevel(), helper.absolutePos(canister));
        helper.assertFalse(entombed.get(), "a canister in the open is not entombed");
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) helper.setBlock(canister.relative(direction), Blocks.DEEPSLATE);
        helper.assertTrue(entombed.get() == helper.absolutePos(canister).getY() < 0, "deepslate all round entombs it, below y 0 only");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void spentFuelRunsToAWasteCanister(GameTestHelper helper) {
        helper.assertTrue(takes(processing(helper, "fundamentals:uses/purex"), "fundamentals:cooled_spent_fuel"), "PUREX takes cooled fuel");
        helper.assertTrue(takes(processing(helper, "fundamentals:uses/vitrified_waste"), "fundamentals:boric_acid"), "vitrification takes boric acid");
        helper.assertTrue(recipe(helper, "fundamentals:uses/waste_canister_vitrified").isPresent()
                && recipe(helper, "fundamentals:uses/waste_canister_spent_fuel").isPresent(), "both wastes can be canistered");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void theReactorHandsBackItsSpentFuel(GameTestHelper helper) throws ClassNotFoundException {
        if (!net.neoforged.fml.ModList.get().isLoaded("createnuclear")) {
            helper.succeed();
            return;
        }
        Class<?> consumable = Class.forName("net.nuclearteam.createnuclear.content.multiblock.controller.consumable.ItemConsumable");
        helper.assertTrue(java.util.Arrays.stream(consumable.getDeclaredMethods()).anyMatch(m -> m.getName().contains("fundamentals$spentFuel")),
                "the reactor's fuel consumption carries the spent-fuel hook");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void spentFuelGoesIntoTheChestOnTheController(GameTestHelper helper) {
        BlockPos caught = new BlockPos(1, 1, 1), loose = new BlockPos(4, 1, 4);
        helper.setBlock(caught.above(), Blocks.CHEST);
        ai.gsmc.fundamentals.content.nuclear.SpentFuel.discharge(helper.getLevel(), helper.absolutePos(caught));
        ai.gsmc.fundamentals.content.nuclear.SpentFuel.discharge(helper.getLevel(), helper.absolutePos(loose));
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getBlockEntity(caught.above());
        helper.assertTrue(chest.getItem(0).is(stack("fundamentals:spent_fuel_rod").getItem()), "a chest on the controller catches the spent rod");
        helper.assertItemEntityPresent(stack("fundamentals:spent_fuel_rod").getItem(), loose.above(), 1.5);
        helper.succeed();
    }
}
