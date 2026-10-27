package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.torch.Torches;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class TorchTests {

    @GameTest(template = "empty")
    public void aTorchFirstNoticedIsGivenAboutAWeekAndOnePastItsDayGoesOutFacingTheSameWay(GameTestHelper helper) {
        BlockPos wall = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.setBlock(new BlockPos(1, 2, 2), Blocks.STONE);
        var level = helper.getLevel();
        level.setBlockAndUpdate(wall, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH));
        BlockState torch = level.getBlockState(wall);
        helper.assertTrue(torch.isRandomlyTicking(), "a torch should take random ticks");

        torch.randomTick(level, wall, level.getRandom());
        Long out = Torches.outAt(level, wall);
        long now = level.getDayTime();
        helper.assertTrue(out != null && out - now >= 6 * 24000 && out - now <= 10 * 24000,
                "a torch first noticed should burn six to ten days, not " + (out == null ? "unrecorded" : (out - now) / 24000.0 + " days"));
        helper.assertTrue(level.getBlockState(wall).is(Blocks.WALL_TORCH), "a fresh torch should still be lit");

        // its day come, without moving the shared clock other tests read
        level.getChunkAt(wall).getData(Torches.OUT_AT).put(Long.toString(wall.asLong()), now);
        torch.randomTick(level, wall, level.getRandom());
        BlockState stub = level.getBlockState(wall);
        helper.assertTrue(stub.is(Torches.burntWall()) && stub.getValue(WallTorchBlock.FACING) == Direction.NORTH,
                "a torch past its day should be a burnt stub facing north, not " + stub);
        helper.assertTrue(Torches.outAt(level, wall) == null, "a burnt stub should leave no record behind");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void flintAndSteelRelightsAStubForAnotherWeek(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        var level = helper.getLevel();
        level.setBlockAndUpdate(pos, Torches.burnt().defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, flint);
        level.getBlockState(pos).useItemOn(flint, level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        helper.assertTrue(level.getBlockState(pos).is(Blocks.TORCH), "flint and steel should relight the stub, not leave " + level.getBlockState(pos));
        Long out = Torches.outAt(level, pos);
        helper.assertTrue(out != null && out - level.getDayTime() >= 6 * 24000, "a relit torch should have a fresh week, not " + out);
        helper.assertTrue(flint.getDamageValue() == 1, "relighting should wear the flint and steel");
        helper.succeed();
    }
}
