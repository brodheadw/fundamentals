package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.oxidation.CanisterItem;
import ai.gsmc.fundamentals.oxidation.InertDrumBlockEntity;
import ai.gsmc.fundamentals.oxidation.Oxidation;
import ai.gsmc.fundamentals.separation.Separation;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.DataMapHooks;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

    private static String key(Component line) {
        return line.getSiblings().get(0).getContents() instanceof TranslatableContents t ? t.getKey() : line.getString();
    }

    @GameTest(template = "empty")
    public void drumGogglesReadTheGasTheContentsAndWhetherTheyKeep(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Oxidation.drum());
        InertDrumBlockEntity drum = (InertDrumBlockEntity) helper.getBlockEntity(pos);
        drum.handler(null).fill(new FluidStack(Separation.fluid("argon"), 1000), IFluidHandler.FluidAction.EXECUTE);
        IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(items != null && items.insertItem(0, new ItemStack(item("fundamentals:cerium_ingot"), 4), false).isEmpty(),
                "a funnel or hopper should be able to put metal into the drum");
        drum.setItem(1, new ItemStack(Items.COPPER_INGOT, 60));
        List<Component> lines = new ArrayList<>();
        drum.addToGoggleTooltip(lines, false);
        String prefix = "goggles.fundamentals.inert_storage_drum.";
        helper.assertTrue(key(lines.get(1)).equals(prefix + "argon") && lines.get(1).getString().contains("1000 mB")
                && lines.get(1).getString().contains("10 mB a day"), "the goggles should read the argon and its leak: " + lines.get(1).getString());
        helper.assertTrue(lines.get(2).getString().contains("2 of 27") && lines.get(2).getString().contains("64"), "two stacks, 64 items: " + lines.get(2).getString());
        helper.assertTrue(key(lines.get(3)).equals(prefix + "kept"), "charged, nothing inside ages");
        drum.backdate(1001L * InertDrumBlockEntity.TICKS_PER_MB);
        lines.clear();
        drum.addToGoggleTooltip(lines, false);
        helper.assertTrue(key(lines.get(1)).equals(prefix + "no_gas") && key(lines.get(3)).equals(prefix + "ageing"),
                "once the argon has leaked away the goggles should say so: " + lines.stream().map(Component::getString).toList());
        helper.succeed();
    }

    /** The model an item's override list picks for a stage, as the client picks it: the last override the value satisfies. */
    private static String modelFor(String item, float stage) throws IOException {
        String[] id = item.split(":");
        JsonObject model = json("/assets/" + id[0] + "/models/item/" + id[1] + ".json");
        JsonArray overrides = model.has("overrides") ? model.getAsJsonArray("overrides") : new JsonArray();
        for (int i = overrides.size() - 1; i >= 0; i--) {
            JsonObject override = overrides.get(i).getAsJsonObject();
            if (override.getAsJsonObject("predicate").get(Oxidation.STAGE_PROPERTY.toString()).getAsFloat() <= stage) {
                return override.get("model").getAsString();
            }
        }
        return id[0] + ":item/" + id[1];
    }

    private static JsonObject json(String path) throws IOException {
        try (InputStream in = OxidationTests.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("no " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @GameTest(template = "empty")
    public void anAgedIngotLooksItsStage(GameTestHelper helper) throws IOException {
        ItemStack cerium = new ItemStack(item("fundamentals:cerium_ingot"));
        cerium.set(Oxidation.STAGE, 2);
        String model = modelFor("fundamentals:cerium_ingot", Oxidation.stageProperty(cerium));
        helper.assertTrue(model.equals("fundamentals:item/aged/cerium_ingot_2"), "a stage 2 cerium ingot should draw its stage 2 model, not " + model);
        String texture = json("/assets/fundamentals/models/item/aged/cerium_ingot_2.json").getAsJsonObject("textures").get("layer0").getAsString();
        helper.assertTrue(OxidationTests.class.getResource("/assets/fundamentals/textures/" + texture.split(":")[1] + ".png") != null, "no texture " + texture);
        helper.assertTrue(modelFor("minecraft:copper_ingot", 3).equals("fundamentals:item/aged/copper_ingot_3"), "green copper should look green");
        helper.assertTrue(modelFor("fundamentals:cerium_ingot", Oxidation.stageProperty(new ItemStack(item("fundamentals:cerium_ingot"))))
                .equals("fundamentals:item/cerium_ingot"), "a fresh ingot keeps its own model");
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

    @GameTest(template = "empty", timeoutTicks = 100)
    public void anAgedDropPickedUpWithRoomForSomeIsNotDuplicated(GameTestHelper helper) {
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        Inventory inventory = player.getInventory();
        inventory.clearContent();
        inventory.setItem(0, new ItemStack(item("fundamentals:cerium_ingot"), 60));
        for (int i = 1; i < inventory.items.size(); i++) {
            inventory.setItem(i, new ItemStack(Items.DIRT, 64));
        }
        inventory.offhand.set(0, new ItemStack(Items.DIRT, 64));
        Vec3 at = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        ItemEntity drop = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(item("fundamentals:cerium_ingot"), 64));
        drop.setNoPickUpDelay();
        drop.setData(Oxidation.CLOCK, -HUNDRED_DAYS);
        helper.getLevel().addFreshEntity(drop);
        helper.runAfterDelay(5, () -> {
            drop.playerTouch(player);
            drop.playerTouch(player);
            int held = 0;
            for (ItemStack stack : inventory.items) {
                held += stack.is(Items.DIRT) ? 0 : stack.getCount();
            }
            int total = held + (drop.isAlive() ? drop.getItem().getCount() : 0);
            inventory.clearContent();
            helper.assertTrue(drop.getItem().has(Oxidation.STAGE) || !drop.getItem().is(item("fundamentals:cerium_ingot")), "a hundred days on the ground should have aged it");
            helper.assertTrue(total == 124, "sixty in the pack and sixty-four on the ground should stay 124, not " + total);
            drop.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public void aCanisterEmptiedIntoASmallSlotKeepsTheRestSealed(GameTestHelper helper) {
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        SimpleContainer one = new SimpleContainer(1) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
        Slot slot = new Slot(one, 0, 0, 0);
        ItemStack canister = new ItemStack(Oxidation.argonCanister());
        CanisterItem.seal(canister, new ItemStack(item("fundamentals:cerium_ingot"), 64));
        canister.getItem().overrideStackedOnOther(canister, slot, ClickAction.SECONDARY, player);
        helper.assertTrue(one.getItem(0).getCount() == 1 && CanisterItem.contents(canister).getCount() == 63,
                "one should go in and 63 stay sealed, got " + one.getItem(0) + " and " + CanisterItem.contents(canister));
        ItemStack empty = new ItemStack(Oxidation.argonCanister());
        helper.assertFalse(empty.getItem().overrideOtherStackedOnMe(empty, canister.copy(), slot, ClickAction.SECONDARY, player, SlotAccess.NULL),
                "a canister should not seal another canister");
        helper.succeed();
    }
}
