package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.anchor.RealityAnchorItem;
import com.wildspell.fundamental.api.field.Anchors;
import com.wildspell.fundamental.api.field.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class AnchorTests {

    @GameTest(template = "empty", timeoutTicks = 60)
    public void aRunningAnchorHoldsItsPlaceAndSpendsItsCell(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        ItemStack anchor = new ItemStack(BuiltInRegistries.ITEM.get(Fundamentals.id("reality_anchor")));
        player.setItemInHand(InteractionHand.MAIN_HAND, anchor);
        anchor.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(RealityAnchorItem.on(anchor), "used, it switches on");
        helper.runAfterDelay(25, () -> {
            RealityAnchorItem.hold(level, player, anchor);
            helper.assertTrue(Anchors.at(level, at) && Field.sample(level, at).intensity() == 0.0F, "on, its place reads still and empty");
            helper.assertTrue(anchor.getDamageValue() >= 1, "and it spends its cell: " + anchor.getDamageValue());
            anchor.getItem().use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(!RealityAnchorItem.on(anchor), "used again, it lets go");
            helper.assertTrue(anchor.getItem().isValidRepairItem(anchor, new ItemStack(BuiltInRegistries.ITEM.get(Fundamentals.id("lead_acid_cell")))),
                    "a fresh lead-acid cell recharges it");
            Anchors.clear();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public void everyStepToTheAnchorHasARecipe(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String step : new String[] {"boron_oxide", "crude_boron", "amorphous_boron", "magnesium_diboride_precursor", "niobium_ingot", "niobium_sheet",
                "magnesium_diboride_billet", "drawn_magnesium_diboride_wire", "magnesium_diboride_wire", "coil_former", "superconducting_coil", "cryostat",
                "quartz_resonator", "reality_anchor"}) {
            helper.assertTrue(recipes.byKey(Fundamentals.id("anchor/" + step)).isPresent(), "a recipe makes " + step);
        }
        helper.succeed();
    }
}
