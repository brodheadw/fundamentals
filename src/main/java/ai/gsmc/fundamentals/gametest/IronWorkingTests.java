package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.ironworking.BloomeryBlock;
import ai.gsmc.fundamentals.ironworking.BloomeryBlockEntity;
import ai.gsmc.fundamentals.ironworking.IronWorking;
import ai.gsmc.fundamentals.registry.HandTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
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

    private static Item hematite() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "raw_hematite"));
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
        use(helper, player, new ItemStack(Items.TORCH));  // no iron needed to light your first firing
        helper.assertBlockProperty(POS, BloomeryBlock.LIT, true);
        helper.runAfterDelay(BloomeryBlockEntity.BURN_TICKS + 5, () -> {
            helper.assertBlockProperty(POS, BloomeryBlock.LIT, false);
            use(helper, player, ItemStack.EMPTY);
            helper.assertItemEntityPresent(IronWorking.ironBloom(), POS, 3);
            helper.assertItemEntityPresent(IronWorking.slag(), POS, 3);
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
    public void hammeringABloomGivesWroughtIronAndWearsTheHammer(GameTestHelper helper) {
        CraftingInput grid = CraftingInput.of(2, 1, List.of(
                new ItemStack(IronWorking.ironBloom()), new ItemStack(IronWorking.smithingHammer())));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "bloom + hammer should be a recipe");
        ItemStack result = recipe.get().value().assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(Items.IRON_INGOT), "hammering a bloom should give an iron ingot, got " + result);
        ItemStack hammer = recipe.get().value().getRemainingItems(grid).get(1);
        helper.assertTrue(hammer.is(IronWorking.smithingHammer()) && hammer.getDamageValue() == 1,
                "the hammer should stay in the grid, one use worn");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void grindingInTheHandsUsesTheOtherHandsMaterial(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Item malachite = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "raw_malachite"));
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
    public void mortarGrindsPigmentAndFlour(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        Item malachite = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "raw_malachite"));
        Item flour = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wheat_flour"));
        helper.assertTrue(flour != Items.AIR, "Create's wheat flour should exist");

        CraftingInput pigment = CraftingInput.of(2, 1, List.of(new ItemStack(malachite), new ItemStack(HandTools.mortarAndPestle())));
        var grindPigment = recipes.getRecipeFor(RecipeType.CRAFTING, pigment, helper.getLevel());
        helper.assertTrue(grindPigment.isPresent(), "malachite + mortar should be a recipe");
        ItemStack dye = grindPigment.get().value().assemble(pigment, helper.getLevel().registryAccess());
        helper.assertTrue(dye.is(Items.GREEN_DYE) && dye.getCount() == 2, "malachite should grind to 2 green dye, got " + dye);
        ItemStack mortar = grindPigment.get().value().getRemainingItems(pigment).get(1);
        helper.assertTrue(mortar.is(HandTools.mortarAndPestle()) && mortar.getDamageValue() == 1,
                "the mortar should stay in the grid, one use worn");

        CraftingInput grain = CraftingInput.of(2, 1, List.of(new ItemStack(Items.WHEAT), new ItemStack(HandTools.mortarAndPestle())));
        var grindGrain = recipes.getRecipeFor(RecipeType.CRAFTING, grain, helper.getLevel());
        helper.assertTrue(grindGrain.isPresent()
                        && grindGrain.get().value().assemble(grain, helper.getLevel().registryAccess()).is(flour),
                "wheat + mortar should give Create's wheat flour");
        helper.succeed();
    }
}
