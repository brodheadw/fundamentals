package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.building.Building;
import com.simibubi.create.content.decoration.TrainTrapdoorBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class BuildingTests {

    @GameTest(template = "empty", batch = "building")
    public void metalDoorsAndTrapdoorsOpenByHand(GameTestHelper helper) {
        for (String metal : Building.METALS) {
            var door = Building.block(metal + "_door");
            BlockPos at = new BlockPos(1, 1, 1);
            // Create's door drops a lower half whose upper half is another block, so clear the last metal's door first
            helper.setBlock(at.above(), Blocks.AIR);
            helper.setBlock(at, door.defaultBlockState());
            helper.setBlock(at.above(), door.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            helper.useBlock(at);
            helper.assertTrue(helper.getBlockState(at).getValue(DoorBlock.OPEN), metal + " door opens by hand");
            var trapdoor = Building.block(metal + "_trapdoor");
            BlockPos hatch = new BlockPos(2, 1, 1);
            helper.setBlock(hatch, trapdoor.defaultBlockState());
            helper.useBlock(hatch);
            helper.assertTrue(helper.getBlockState(hatch).getValue(TrapDoorBlock.OPEN), metal + " trapdoor opens by hand");
            helper.assertTrue(door instanceof SlidingDoorBlock && trapdoor instanceof TrainTrapdoorBlock, metal + " door slides and its trapdoor is a framed hatch");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "building")
    public void brassDoubleDoorSlidesOpenOnRedstone(GameTestHelper helper) {
        var door = Building.block("brass_door");
        BlockPos left = new BlockPos(1, 1, 1), right = left.east(), power = left.south();
        BlockState lower = door.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        helper.setBlock(left, lower.setValue(DoorBlock.HINGE, DoorHingeSide.LEFT));
        helper.setBlock(left.above(), lower.setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.setBlock(right, lower.setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT));
        helper.setBlock(right.above(), lower.setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.assertTrue(helper.getBlockEntity(left) instanceof SlidingDoorBlockEntity, "Create's sliding door entity sits on the brass door");
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> helper.setBlock(power, Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> {
                    helper.assertTrue(helper.getBlockState(left).getValue(DoorBlock.OPEN), "redstone opens the powered leaf");
                    helper.assertTrue(helper.getBlockState(right).getValue(DoorBlock.OPEN), "and its pair opens with it");
                    helper.assertFalse(helper.getBlockState(left).getValue(SlidingDoorBlock.VISIBLE), "the leaf is drawn sliding, not swung");
                })
                .thenIdle(20)
                .thenExecute(() -> helper.setBlock(power, Blocks.AIR))
                .thenWaitUntil(() -> {
                    helper.assertFalse(helper.getBlockState(left).getValue(DoorBlock.OPEN) || helper.getBlockState(right).getValue(DoorBlock.OPEN), "cutting the power shuts both");
                    helper.assertTrue(helper.getBlockState(left).getValue(SlidingDoorBlock.VISIBLE) && helper.getBlockState(right).getValue(SlidingDoorBlock.VISIBLE),
                            "once slid home the leaves stand as blocks again");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "building")
    public void aDoorFromBeforeTheSlidingDoorsStillShuts(GameTestHelper helper) {
        var door = Building.block("tin_door");
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, door.defaultBlockState());
        helper.setBlock(at.above(), door.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // a door placed by 0.32 has no block entity in the saved chunk
        helper.getLevel().removeBlockEntity(helper.absolutePos(at));
        helper.startSequence()
                .thenExecute(() -> helper.useBlock(at))
                .thenIdle(20)
                .thenExecute(() -> helper.useBlock(at))
                .thenWaitUntil(() -> helper.assertTrue(!helper.getBlockState(at).getValue(DoorBlock.OPEN) && helper.getBlockState(at).getValue(SlidingDoorBlock.VISIBLE),
                        "it slides shut and stands as a block again"))
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "building")
    public void aTinRoofBakesAtNoonAndBurnsBareFeet(GameTestHelper helper) {
        var level = helper.getLevel();
        long was = level.getDayTime();
        BlockPos roof = new BlockPos(1, 1, 1);
        helper.setBlock(roof, Building.block("tin_roofing"));
        BlockPos abs = helper.absolutePos(roof);
        level.setDayTime(18000);
        helper.assertTrue(Building.sun(level, abs) == 0, "no sun on a roof at midnight");
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        level.setRainLevel(0);
        level.setThunderLevel(0);
        helper.assertTrue(Building.sun(level, abs) > 0.9, "clear noon on an open roof is full sun");
        double before = Heat.at(level, abs.above());
        Building.bake(level, abs);
        double after = Heat.at(level, abs.above());
        var zombie = helper.spawn(EntityType.ZOMBIE, roof.above());
        float health = zombie.getHealth();
        Building.scorch(level, abs, zombie);
        level.setDayTime(was);
        helper.assertTrue(after > before + 20, "a noon roof warms the air over it: " + before + " -> " + after);
        helper.assertTrue(zombie.getHealth() < health, "bare feet burn on it at noon");
        zombie.discard();
        helper.succeed();
    }
}
