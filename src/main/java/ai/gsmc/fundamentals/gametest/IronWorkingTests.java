package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.ironworking.BloomeryBlock;
import ai.gsmc.fundamentals.ironworking.BloomeryBlockEntity;
import ai.gsmc.fundamentals.ironworking.IronWorking;
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
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "hematite_ore"));
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
}
