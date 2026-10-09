package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.oxidation.InertDrumBlockEntity;
import ai.gsmc.fundamentals.oxidation.Oxidation;
import ai.gsmc.fundamentals.separation.Separation;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.DataMapHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class OxidationTests {

    private static final long HUNDRED_DAYS = 100 * 24000L;

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }

    /** What opening it does, without a mock player in the world: those break the Factory's electric blocks in other tests. */
    private static void open(GameTestHelper helper, Container container) {
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        NeoForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, ChestMenu.threeRows(0, player.getInventory(), container)));
    }

    @GameTest(template = "empty")
    public void ceriumLeftInAChestIsOxideWhenTheChestIsOpenedAndGoldIsStillGold(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(pos);
        chest.setItem(0, new ItemStack(item("fundamentals:cerium_ingot"), 4));
        chest.setItem(1, new ItemStack(Items.GOLD_INGOT, 4));
        chest.setItem(2, new ItemStack(Items.COPPER_BLOCK));
        chest.setData(Oxidation.CLOCK, helper.getLevel().getGameTime() - HUNDRED_DAYS);
        open(helper, chest);
        helper.assertTrue(chest.getItem(0).is(item("fundamentals:cerium_oxide")) && chest.getItem(0).getCount() == 4,
                "a hundred days in a chest should have turned four cerium ingots to four oxide, not " + chest.getItem(0));
        helper.assertTrue(chest.getItem(1).is(Items.GOLD_INGOT) && !chest.getItem(1).has(Oxidation.STAGE), "gold never ages: " + chest.getItem(1));
        helper.assertFalse(chest.getItem(2).is(Items.COPPER_BLOCK), "a copper block should have weathered in the chest");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void ceriumInAChargedArgonDrumStaysAnIngot(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Oxidation.drum());
        InertDrumBlockEntity drum = (InertDrumBlockEntity) helper.getBlockEntity(pos);
        IFluidHandler tank = drum.handler(null);
        helper.assertTrue(tank.fill(new FluidStack(Separation.fluid("argon"), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "the drum should take 1,000 mB of argon");
        drum.setItem(0, new ItemStack(item("fundamentals:cerium_ingot"), 4));
        drum.backdate(HUNDRED_DAYS / 2);
        open(helper, drum);
        ItemStack inside = drum.getItem(0);
        helper.assertTrue(inside.is(item("fundamentals:cerium_ingot")) && !inside.has(Oxidation.STAGE), "cerium under argon should not age: " + inside);
        int left = drum.gas().getAmount();
        helper.assertTrue(left == 1000 - 500 - InertDrumBlockEntity.VENT, "fifty days and an opening should leave 475 mB, left " + left);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void aBronzeBlockWeathersAndWaxesLikeCopper(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        Block bronze = block("fundamentals:bronze_block");
        helper.setBlock(pos, bronze);
        BlockPos at = helper.absolutePos(pos);
        for (int i = 0; i < 20000 && helper.getBlockState(pos).is(bronze); i++) {
            BlockState state = helper.getBlockState(pos);
            state.randomTick(helper.getLevel(), at, helper.getLevel().random);
        }
        helper.assertBlockPresent(block("fundamentals:exposed_bronze_block"), pos);
        helper.assertTrue(DataMapHooks.getBlockWaxed(bronze) == block("fundamentals:waxed_bronze_block"), "honeycomb should wax a bronze block");
        ItemStack green = new ItemStack(Items.COPPER_INGOT);
        green.set(Oxidation.STAGE, 3);
        ItemStack polished = SandPaperPolishingRecipe.applyPolish(helper.getLevel(), Vec3.ZERO, green, ItemStack.EMPTY);
        helper.assertTrue(polished.is(Items.COPPER_INGOT) && !polished.has(Oxidation.STAGE), "sand paper should take the verdigris off, gave " + polished);
        helper.assertFalse(SandPaperPolishingRecipe.canPolish(helper.getLevel(), new ItemStack(Items.COPPER_INGOT)), "a bright ingot has nothing to polish");
        helper.succeed();
    }
}
