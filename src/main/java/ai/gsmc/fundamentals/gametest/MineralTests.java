package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.worldgen.DepositFeature;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class MineralTests {

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

    private static void step(GameTestHelper helper, String id, String from, String to) {
        var recipe = processing(helper, id);
        helper.assertTrue(takes(recipe, from) && gives(recipe, to), id + " should take " + from + " to " + to);
    }

    private record Case(String deposit, String mineral, ResourceKey<Biome> biome) {}

    private static final List<Case> DEPOSITS = List.of(
            new Case("graphite_schist", "graphite", Biomes.PLAINS),
            new Case("antimony_vein", "stibnite", Biomes.JAGGED_PEAKS),
            new Case("gold_quartz_vein", "native_gold", Biomes.JAGGED_PEAKS),
            new Case("carbonatite_plug", "pyrochlore", Biomes.BADLANDS),
            new Case("spodumene_pegmatite", "columbite", Biomes.BADLANDS),
            new Case("evaporite_bed", "sylvite", Biomes.DESERT));

    private static boolean generates(ServerLevel level, ResourceKey<Biome> biome, String feature) {
        ResourceKey<PlacedFeature> placed = ResourceKey.create(Registries.PLACED_FEATURE, Fundamentals.id(feature));
        return level.registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(biome).getGenerationSettings().features()
                .stream().anyMatch(step -> step.stream().anyMatch(f -> f.is(placed)));
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public void eachNewMineralFormsInItsDeposit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(1, 8, 1));
        Iterable<BlockPos> box = BlockPos.betweenClosed(centre.offset(-7, -6, -7), centre.offset(7, 6, 7));
        for (Case each : DEPOSITS) {
            helper.assertTrue(generates(level, each.biome(), each.deposit()), each.deposit() + " should generate in " + each.biome().location());
            DepositFeature.Config shipped = (DepositFeature.Config) level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(Fundamentals.id(each.deposit())).config();
            DepositFeature.Config small = new DepositFeature.Config(shipped.shape(), shipped.host(), shipped.ores(), new DepositFeature.Range(6, 6),
                    shipped.thickness(), shipped.height().equals(DepositFeature.Range.NONE) ? shipped.height() : new DepositFeature.Range(10, 10),
                    shipped.replaceable());
            Block ore = BuiltInRegistries.BLOCK.get(Fundamentals.id(each.mineral() + "_ore"));
            int found = 0;
            for (int attempt = 0; attempt < 27 && found == 0; attempt++) {
                box.forEach(pos -> level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2));
                // The body's noise is seeded by where it stands, so each attempt stands a little elsewhere.
                BlockPos at = centre.offset(attempt % 3 - 1, attempt / 3 % 3 - 1, attempt / 9 % 3 - 1);
                DepositFeature.INSTANCE.place(small, level, level.getChunkSource().getGenerator(), level.getRandom(), at);
                for (BlockPos pos : box) {
                    found += level.getBlockState(pos).is(ore) ? 1 : 0;
                }
            }
            box.forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2));
            helper.assertTrue(found > 0, each.deposit() + " placed in stone should carry " + each.mineral());
        }
        helper.assertTrue(generates(level, Biomes.RIVER, "placer_native_gold") && generates(level, Biomes.BEACH, "placer_native_gold"),
                "gold should wash into river and beach sand");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void vanillaGoldOreMinesAsNativeGold(GameTestHelper helper) {
        for (Block block : new Block[] {Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE}) {
            List<ItemStack> drops = Block.getDrops(block.defaultBlockState(), helper.getLevel(), helper.absolutePos(BlockPos.ZERO), null, null,
                    new ItemStack(Items.IRON_PICKAXE));
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(stack("fundamentals:raw_native_gold").getItem()), block + " dropped " + drops);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void nativeGoldRunsToGold(GameTestHelper helper) {
        step(helper, "fundamentals:uses/gold_from_panning", "fundamentals:raw_native_gold", "minecraft:gold_nugget");
        step(helper, "fundamentals:uses/gold_amalgam", "fundamentals:raw_native_gold", "fundamentals:gold_amalgam");
        helper.assertTrue(takes(processing(helper, "fundamentals:uses/gold_amalgam"), "fundamentals:mercury"), "amalgamation takes mercury");
        step(helper, "fundamentals:uses/electrum_from_amalgam", "fundamentals:gold_amalgam", "fundamentals:electrum_ingot");
        var miller = processing(helper, "fundamentals:uses/gold_from_electrum");
        helper.assertTrue(takes(miller, "fundamentals:electrum_ingot") && takesFluid(miller, "fundamentals:chlorine") && gives(miller, "minecraft:gold_ingot")
                && gives(miller, "fundamentals:silver_nugget"), "chlorine should part electrum to gold and silver");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void graphiteRunsToAnElectrode(GameTestHelper helper) {
        step(helper, "fundamentals:uses/graphite_concentrate", "fundamentals:raw_graphite", "fundamentals:graphite_concentrate");
        step(helper, "fundamentals:uses/graphite", "fundamentals:graphite_concentrate", "fundamentals:graphite");
        step(helper, "fundamentals:uses/graphite_thermal", "fundamentals:graphite_concentrate", "fundamentals:graphite");
        step(helper, "fundamentals:uses/graphite_acheson", "tfmg:coal_coke", "fundamentals:graphite");
        step(helper, "fundamentals:uses/graphite_electrode", "fundamentals:graphite", "tfmg:graphite_electrode");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void stibniteRunsToTheBatteryGrid(GameTestHelper helper) {
        helper.assertTrue(takes(recipe(helper, "fundamentals:roasting/antimony_oxide_campfire_cooking").orElseThrow(), "fundamentals:raw_stibnite"),
                "stibnite should roast to the oxide");
        step(helper, "fundamentals:uses/antimony_ingot", "fundamentals:antimony_oxide", "fundamentals:antimony_ingot");
        step(helper, "fundamentals:uses/antimonial_lead", "fundamentals:antimony_nugget", "fundamentals:antimonial_lead_ingot");
        Recipe<?> accumulator = recipe(helper, "tfmg:crafting/materials/accumulator").orElseThrow();
        helper.assertTrue(takes(accumulator, "fundamentals:antimonial_lead_plate"), "the lead-acid accumulator's grids should be antimonial lead");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void niobiumAndTantalumReachTheirMetals(GameTestHelper helper) {
        step(helper, "fundamentals:uses/ferroniobium_from_pyrochlore_concentrate", "fundamentals:pyrochlore_concentrate", "fundamentals:ferroniobium_ingot");
        step(helper, "fundamentals:uses/hsla_steel", "fundamentals:ferroniobium_nugget", "fundamentals:hsla_steel_ingot");
        var digest = processing(helper, "fundamentals:uses/potassium_fluorotantalate");
        helper.assertTrue(takes(digest, "fundamentals:columbite_dust") && takes(digest, "fundamentals:potash") && gives(digest, "fundamentals:potassium_fluorotantalate")
                && gives(digest, "fundamentals:niobium_oxide"), "columbite should digest to K2TaF7 and niobium oxide");
        step(helper, "fundamentals:uses/niobium_oxide_from_euxenite", "fundamentals:euxenite_dust", "fundamentals:niobium_oxide");
        step(helper, "fundamentals:uses/ferroniobium_from_niobium_oxide", "fundamentals:niobium_oxide", "fundamentals:ferroniobium_ingot");
        step(helper, "fundamentals:uses/sodium_ingot", "fundamentals:salt", "fundamentals:sodium_ingot");
        var reduction = processing(helper, "fundamentals:uses/tantalum_ingot");
        helper.assertTrue(takes(reduction, "fundamentals:sodium_ingot") && gives(reduction, "fundamentals:tantalum_ingot"), "sodium should reduce K2TaF7 to tantalum");
        helper.assertTrue(recipe(helper, "fundamentals:uses/tantalum_capacitor").isPresent(), "tantalum should make capacitors");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void sylviteRunsToPotash(GameTestHelper helper) {
        step(helper, "fundamentals:uses/potash", "fundamentals:raw_sylvite", "fundamentals:potash");
        helper.assertTrue(stack("fundamentals:potash").is(net.minecraft.tags.TagKey.create(Registries.ITEM, ResourceLocation.parse("c:dusts/potassium_chloride"))),
                "potash should be #c:dusts/potassium_chloride");
        helper.assertTrue(takes(processing(helper, "fundamentals:lithium/lithium_ingot"), "fundamentals:potash"), "lithium is electrolysed from LiCl-KCl");
        helper.succeed();
    }
}
