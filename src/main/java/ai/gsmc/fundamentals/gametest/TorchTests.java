package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.torch.Torches;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class TorchTests {

    @GameTest(template = "empty")
    public void aTorchNoLongerBurnsDownAndAStubLeftInTheWorldRelightsFacingTheSameWay(GameTestHelper helper) {
        helper.assertTrue(!Blocks.TORCH.defaultBlockState().isRandomlyTicking(), "a torch should not take random ticks");
        BlockPos wall = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.setBlock(new BlockPos(1, 2, 2), Blocks.STONE);
        var level = helper.getLevel();
        level.setBlockAndUpdate(wall, Torches.burntWall().defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH));
        BlockState stub = level.getBlockState(wall);
        helper.assertTrue(stub.isRandomlyTicking(), "a stub should take random ticks");
        stub.randomTick(level, wall, level.getRandom());
        BlockState lit = level.getBlockState(wall);
        helper.assertTrue(lit.is(Blocks.WALL_TORCH) && lit.getValue(WallTorchBlock.FACING) == Direction.NORTH,
                "a stub should relight as a wall torch facing north, not " + lit);
        helper.succeed();
    }
}
