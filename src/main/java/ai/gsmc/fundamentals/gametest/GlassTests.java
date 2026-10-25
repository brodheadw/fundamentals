package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.glass.BlastDoorBlock;
import ai.gsmc.fundamentals.glass.BlastDoorBlockEntity;
import ai.gsmc.fundamentals.glass.Glass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class GlassTests {

    private static final List<String> NEEDS_CHEMICA = List.of("glass/phosgene", "glass/bisphenol_a");

    @GameTest(template = "empty")
    public void everyGlassRecipeLoads(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        List<String> missing = new ArrayList<>();
        server.getResourceManager().listResources("recipe/glass", path -> path.getPath().endsWith(".json")).keySet().stream()
                .filter(file -> file.getNamespace().equals(Fundamentals.MOD_ID))
                .map(file -> file.withPath(file.getPath().substring("recipe/".length(), file.getPath().length() - 5)))
                .filter(id -> !NEEDS_CHEMICA.contains(id.getPath()) || ModList.get().isLoaded("chemica"))
                .filter(id -> server.getRecipeManager().byKey(id).isEmpty())
                .forEach(id -> missing.add(id.toString()));
        helper.assertTrue(missing.isEmpty(), "glass recipes that failed to load: " + missing);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void windowsStandBlastsInOrderOfStrength(GameTestHelper helper) {
        List<Block> order = List.of(Blocks.GLASS, Glass.block("borosilicate_glass"), Glass.block("tempered_glass"), Glass.block("sapphire_window"),
                Glass.block("laminated_glass"), Glass.blastDoor());
        for (int i = 1; i < order.size(); i++) {
            helper.assertTrue(order.get(i).getExplosionResistance() > order.get(i - 1).getExplosionResistance(),
                    order.get(i) + " should stand a blast better than " + order.get(i - 1));
        }
        helper.assertTrue(Glass.block("tempered_glass_pane").getExplosionResistance() == Glass.block("tempered_glass").getExplosionResistance()
                && Glass.block("laminated_glass_pane").getExplosionResistance() == Glass.block("laminated_glass").getExplosionResistance(), "a pane is as strong as its glass");
        helper.assertTrue(Glass.blastDoor().getExplosionResistance() >= Blocks.OBSIDIAN.getExplosionResistance(), "the blast door stands what obsidian does");
        helper.assertTrue(Glass.blastDoor().defaultBlockState().is(BlockTags.NEEDS_DIAMOND_TOOL) && Glass.blastDoor().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                "the blast door wants a diamond pickaxe");
        helper.succeed();
    }

    private static BlockPos part(BlockPos controller, Direction facing, int part) {
        return BlastDoorBlock.partPos(controller, facing, part);
    }

    /** Places a door from its item in the middle of the 3x3x3 test box, as a player facing south would: its controller there, the rest round it. */
    private static Direction placeDoor(GameTestHelper helper, BlockPos controller) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        // the item has to be in hand: placing reads it from the player. placeAt clicks the air at the controller, so places there.
        ItemStack door = new ItemStack(Glass.blastDoor().asItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, door);
        helper.placeAt(player, door, controller.below(), Direction.UP);
        var state = helper.getBlockState(controller);
        helper.assertTrue(state.is(Glass.blastDoor()) && state.getValue(BlastDoorBlock.PART) == BlastDoorBlock.CONTROLLER, "the item places the door's controller where it was aimed");
        Direction facing = state.getValue(BlastDoorBlock.FACING);
        float hardness = state.getDestroySpeed(helper.getLevel(), helper.absolutePos(controller));
        for (int i = 0; i < 9; i++) {
            var at = helper.getBlockState(part(controller, facing, i));
            helper.assertTrue(at.is(Glass.blastDoor()) && at.getValue(BlastDoorBlock.PART) == i, "one item stands the whole 3x3 door, part " + i);
            helper.assertTrue(at.getDestroySpeed(helper.getLevel(), helper.absolutePos(part(controller, facing, i))) == hardness, "every part is as hard to dig");
        }
        helper.assertTrue(hardness >= 5 * Blocks.OBSIDIAN.defaultDestroyTime(), "the door is five times as hard to dig as obsidian: " + hardness);
        return facing;
    }

    private static boolean shut(GameTestHelper helper, BlockPos controller, Direction facing) {
        for (int i = 0; i < 9; i++) {
            BlockPos at = part(controller, facing, i);
            if (helper.getBlockState(at).getCollisionShape(helper.getLevel(), helper.absolutePos(at), CollisionContext.empty()).isEmpty()) return false;
        }
        return true;
    }

    private static boolean clear(GameTestHelper helper, BlockPos controller, Direction facing) {
        for (int i = 0; i < 9; i++) {
            BlockPos at = part(controller, facing, i);
            if (!helper.getBlockState(at).getCollisionShape(helper.getLevel(), helper.absolutePos(at), CollisionContext.empty()).isEmpty()) return false;
        }
        return true;
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public void theGridOpensTheBlastDoorAndCuttingItShutsIt(GameTestHelper helper) {
        BlockPos controller = new BlockPos(1, 1, 1);
        Direction facing = placeDoor(helper, controller);
        BlockPos generator = controller.relative(facing);
        Block creative = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfmg:creative_generator"));
        helper.assertTrue(shut(helper, controller, facing), "a new door stands shut");
        helper.setBlock(generator, creative);
        int[] running = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    BlastDoorBlockEntity door = helper.getBlockEntity(controller);
                    helper.assertTrue(door.opening() && door.moving() && door.getPowerUsage() > 0, "fed by the grid the door starts to open and draws power: "
                            + door.getData().getVoltage() + " V, " + door.getPowerUsage() + " W");
                    running[0] = door.getPowerUsage();
                })
                .thenWaitUntil(() -> helper.assertTrue(clear(helper, controller, facing), "open, the doorway is clear through all nine blocks"))
                .thenWaitUntil(() -> {
                    BlastDoorBlockEntity door = helper.getBlockEntity(controller);
                    helper.assertTrue(door.getPowerUsage() > 0 && door.getPowerUsage() < running[0], "held open it draws less than moving: " + door.getPowerUsage() + " < " + running[0]);
                })
                .thenExecute(() -> helper.setBlock(generator, Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(shut(helper, controller, facing), "with the grid cut the door shuts"))
                .thenWaitUntil(() -> helper.assertFalse(((BlastDoorBlockEntity) helper.getBlockEntity(controller)).moving(), "and its leaves close fully"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public void redstoneDoesNotOpenAnUnpoweredBlastDoor(GameTestHelper helper) {
        BlockPos controller = new BlockPos(1, 1, 1);
        Direction facing = placeDoor(helper, controller);
        helper.setBlock(controller.relative(facing), Blocks.REDSTONE_BLOCK);
        helper.setBlock(part(controller, facing, 4).relative(facing.getOpposite()), Blocks.REDSTONE_BLOCK);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.useBlock(part(controller, facing, 0), player);
        helper.runAfterDelay(BlastDoorBlockEntity.TRAVEL + 20, () -> {
            BlastDoorBlockEntity door = helper.getBlockEntity(controller);
            helper.assertTrue(shut(helper, controller, facing) && !door.opening() && !door.moving(), "redstone and a hand leave an unpowered door shut");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public void theRiotShieldBlocksAHitFromTheFront(GameTestHelper helper) {
        ItemStack shield = new ItemStack(Glass.riotShield());
        helper.assertTrue(shield.canPerformAction(ItemAbilities.SHIELD_BLOCK), "the riot shield is a shield");
        helper.assertTrue(shield.getMaxDamage() > new ItemStack(Items.SHIELD).getMaxDamage(), "and outlasts oak");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)), 0, 0);
        player.setItemInHand(InteractionHand.OFF_HAND, shield);
        player.startUsingItem(InteractionHand.OFF_HAND);
        for (int i = 0; i < 6; i++) player.tick();
        helper.assertTrue(player.isBlocking(), "raised, it blocks");
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 4));
        float health = player.getHealth();
        player.hurt(helper.getLevel().damageSources().mobAttack(zombie), 4);
        helper.assertTrue(player.getHealth() == health, "a blow from in front does no harm: " + player.getHealth() + " of " + health);
        helper.assertTrue(player.getOffhandItem().getDamageValue() > 0, "the shield takes the wear");
        helper.succeed();
    }
}
