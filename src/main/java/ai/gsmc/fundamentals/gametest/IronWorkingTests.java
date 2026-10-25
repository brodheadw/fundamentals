package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.ironworking.BloomeryBlock;
import ai.gsmc.fundamentals.ironworking.BloomeryBlockEntity;
import ai.gsmc.fundamentals.ironworking.IronWorking;
import ai.gsmc.fundamentals.registry.HandTools;
import ai.gsmc.fundamentals.separation.Separation;
import ai.gsmc.fundamentals.worldgen.DepositFeature;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class IronWorkingTests {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static void use(GameTestHelper helper, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.useBlock(POS, player);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static Item hematite() {
        return item("fundamentals:raw_hematite");
    }

    @GameTest(template = "empty", timeoutTicks = BloomeryBlockEntity.BURN_TICKS + 100)
    public void bloomeryTurnsOreAndCharcoalIntoBloomsAndSlag(GameTestHelper helper) {
        helper.setBlock(POS, IronWorking.bloomery());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        use(helper, player, new ItemStack(hematite(), 2));
        use(helper, player, new ItemStack(hematite(), 1));
        use(helper, player, new ItemStack(Items.CHARCOAL, 2));
        use(helper, player, new ItemStack(Items.CHARCOAL, 1));
        BloomeryBlockEntity bloomery = helper.getBlockEntity(POS);
        helper.assertTrue(bloomery.oreCount() == 2 && bloomery.charcoalCount() == 2, "ore and charcoal should load one at a time");
        use(helper, player, new ItemStack(Items.TORCH));
        helper.assertBlockProperty(POS, BloomeryBlock.LIT, true);
        helper.runAfterDelay(BloomeryBlockEntity.BURN_TICKS + 5, () -> {
            helper.assertBlockProperty(POS, BloomeryBlock.LIT, false);
            use(helper, player, ItemStack.EMPTY);
            helper.assertItemEntityPresent(IronWorking.ironBloom(), POS, 3);
            helper.assertItemEntityPresent(item("tfmg:slag"), POS, 3);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = BloomeryBlockEntity.BURN_TICKS + 100)
    public void anOverchargedBloomeryRunsHotAndCastsPigIron(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("tfmg:compacting/cast_iron")).isEmpty(),
                "iron and coal pressed hot is not cast iron");
        helper.setBlock(POS, IronWorking.bloomery());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        use(helper, player, new ItemStack(hematite(), 1));
        use(helper, player, new ItemStack(Items.CHARCOAL, 1));
        use(helper, player, new ItemStack(Items.CHARCOAL, 1));
        use(helper, player, new ItemStack(Items.TORCH));
        helper.runAfterDelay(BloomeryBlockEntity.BURN_TICKS + 5, () -> {
            use(helper, player, ItemStack.EMPTY);
            helper.assertItemEntityPresent(item("tfmg:cast_iron_ingot"), POS, 3);
            helper.assertItemEntityNotPresent(IronWorking.ironBloom(), POS, 3);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public void bloomeryRefusesMineralCoalAndWontLightShortOfCharcoal(GameTestHelper helper) {
        helper.setBlock(POS, IronWorking.bloomery());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        use(helper, player, new ItemStack(hematite(), 2));
        use(helper, player, new ItemStack(hematite(), 2));
        use(helper, player, new ItemStack(Items.COAL, 4));
        use(helper, player, new ItemStack(Items.CHARCOAL, 1));
        BloomeryBlockEntity bloomery = helper.getBlockEntity(POS);
        helper.assertTrue(bloomery.charcoalCount() == 1, "coal must not count as charcoal");
        use(helper, player, new ItemStack(Items.FLINT_AND_STEEL));
        helper.assertBlockProperty(POS, BloomeryBlock.LIT, false);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void grindingInTheHandsUsesTheOtherHandsMaterial(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Item malachite = item("fundamentals:raw_malachite");
        ItemStack mortar = new ItemStack(HandTools.mortarAndPestle());
        player.setItemInHand(InteractionHand.MAIN_HAND, mortar);

        helper.assertTrue(!mortar.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                "with nothing to grind the mortar should do nothing");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(malachite, 3));
        helper.assertTrue(mortar.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction()
                        && player.isUsingItem(), "with malachite in the other hand it should start grinding");

        mortar.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.getOffhandItem().getCount() == 2, "one malachite should be used up");
        helper.assertTrue(player.getInventory().countItem(Items.GREEN_DYE) == 2, "two green dye should land in the inventory");
        helper.assertTrue(mortar.getDamageValue() == 1, "the mortar should wear by one use");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void bloomeryTakesSideriteOnlyRoasted(GameTestHelper helper) {
        helper.setBlock(POS, IronWorking.bloomery());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BloomeryBlockEntity bloomery = helper.getBlockEntity(POS);
        use(helper, player, new ItemStack(item("fundamentals:raw_siderite")));
        helper.assertTrue(bloomery.oreCount() == 0, "raw siderite is a carbonate and must be roasted before the bloomery takes it");
        use(helper, player, new ItemStack(item("fundamentals:roasted_siderite")));
        helper.assertTrue(bloomery.oreCount() == 1, "the bloomery should take roasted siderite");
        helper.succeed();
    }

    private static Fluid fluid(String id) {
        return BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
    }

    @GameTest(template = "empty")
    public void theBlastFurnaceMakesPigIronNotSteel(GameTestHelper helper) {
        RecipeType<?> blasting = BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.parse("tfmg:industrial_blasting"));
        ItemStack ore = new ItemStack(item("create:crushed_raw_iron"));
        List<ProcessingRecipe<?, ?>> smelts = helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value().getType() == blasting).<ProcessingRecipe<?, ?>>map(holder -> (ProcessingRecipe<?, ?>) holder.value())
                .filter(recipe -> recipe.getIngredients().stream().anyMatch(i -> i.test(ore))).toList();
        helper.assertTrue(smelts.size() == 1, "crushed iron ore should have one blast furnace recipe, not " + smelts.size());
        List<FluidStack> tapped = smelts.getFirst().getFluidResults();
        helper.assertTrue(tapped.getFirst().is(Separation.fluid("molten_pig_iron")), "the blast furnace should tap pig iron first, not " + tapped.getFirst().getFluid());
        helper.assertTrue(tapped.stream().noneMatch(f -> f.is(fluid("tfmg:molten_steel"))), "the blast furnace should not make steel");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void puddlingPigIronWithRustGivesWroughtIron(GameTestHelper helper) {
        BlockPos basinPos = new BlockPos(1, 2, 1);
        helper.setBlock(basinPos.below(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SEETHING));
        helper.setBlock(basinPos, AllBlocks.BASIN.getDefaultState());
        BasinBlockEntity basin = helper.getBlockEntity(basinPos);
        basin.getInputInventory().insertItem(0, new ItemStack(item("tfmg:cast_iron_ingot"), 2), false);
        basin.getInputInventory().insertItem(1, new ItemStack(item("fundamentals:rusty_iron_ingot")), false);
        var puddling = helper.getLevel().getRecipeManager().byKey(Fundamentals.id("uses/puddling")).orElseThrow().value();
        helper.assertTrue(BasinRecipe.match(basin, puddling), "two cast iron and a rusty iron ingot over a superheated burner should puddle");
        helper.assertTrue(BasinRecipe.apply(basin, puddling), "the puddle ball should come out into the basin");
        ItemStack ball = basin.getOutputInventory().getStackInSlot(0);
        helper.assertTrue(ball.is(item("fundamentals:puddle_ball")), "the basin should hold a puddle ball, not " + ball);
        ItemStack hammer = new ItemStack(IronWorking.smithingHammer());
        var shingled = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 1, List.of(ball, hammer)), helper.getLevel())
                .orElseThrow().value().assemble(CraftingInput.of(2, 1, List.of(ball, hammer)), helper.getLevel().registryAccess());
        helper.assertTrue(shingled.is(Items.IRON_INGOT) && shingled.getCount() == 3, "a shingled puddle ball should give three iron ingots, not " + shingled);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public void theConverterBlowsPigIronToSteel(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfmg:cast_iron_chemical_vat")));
        helper.runAfterDelay(5, () -> {
            IFluidHandler tanks = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
            helper.assertTrue(tanks.fill(new FluidStack(Separation.fluid("molten_pig_iron"), 288), IFluidHandler.FluidAction.EXECUTE) == 288
                    && tanks.fill(new FluidStack(fluid("tfmg:air"), 500), IFluidHandler.FluidAction.EXECUTE) == 500, "the vat should take pig iron and air");
            var items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
            helper.assertTrue(items.insertItem(0, new ItemStack(item("tfmg:limesand")), false).isEmpty(), "the vat should take a limesand");
            helper.succeedWhen(() -> {
                int steel = 0;
                for (int i = 0; i < tanks.getTanks(); i++) {
                    steel += tanks.getFluidInTank(i).is(fluid("tfmg:molten_steel")) ? tanks.getFluidInTank(i).getAmount() : 0;
                }
                helper.assertTrue(steel == 252, "the converter should have blown 252 mB of steel, not " + steel);
            });
        });
    }

    @GameTest(template = "empty")
    public void sideriteLiesInTheCoalMeasures(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceKey<PlacedFeature> placed = ResourceKey.create(Registries.PLACED_FEATURE, Fundamentals.id("coal_measures"));
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.FOREST).getGenerationSettings().features()
                .stream().anyMatch(step -> step.stream().anyMatch(feature -> feature.is(placed))), "the coal measures should generate under forest");
        DepositFeature.Config shipped = (DepositFeature.Config) level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(Fundamentals.id("coal_measures")).config();
        DepositFeature.Config small = new DepositFeature.Config(shipped.shape(), shipped.host(), shipped.ores(), new DepositFeature.Range(6, 6),
                shipped.thickness(), shipped.height(), shipped.replaceable());
        BlockPos centre = helper.absolutePos(new BlockPos(1, 8, 1));
        Iterable<BlockPos> box = BlockPos.betweenClosed(centre.offset(-6, -5, -6), centre.offset(6, 5, 6));
        int siderite = 0, underclay = 0;
        // one small bed can put its only coal seam on the bottom layer, so sample several
        for (int attempt = 0; attempt < 8 && (siderite == 0 || underclay == 0); attempt++) {
            box.forEach(pos -> level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2));
            DepositFeature.INSTANCE.place(small, level, level.getChunkSource().getGenerator(), level.getRandom(), centre.offset(attempt % 3 - 1, 0, attempt / 3 - 1));
            for (BlockPos pos : box) {
                siderite += BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath().equals("siderite_ore") ? 1 : 0;
                underclay += level.getBlockState(pos).is(Blocks.COAL_ORE)
                        && BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.below()).getBlock()).getPath().equals("fireclay") ? 1 : 0;
            }
        }
        box.forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2));
        helper.assertTrue(siderite > 0, "a coal measures bed placed in stone should carry siderite");
        helper.assertTrue(underclay > 0, "fireclay should lie under the coal seams");
        helper.succeed();
    }
}
