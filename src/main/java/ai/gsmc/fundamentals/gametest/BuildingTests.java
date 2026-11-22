package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.building.Building;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
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
            helper.setBlock(at, door.defaultBlockState());
            helper.setBlock(at.above(), door.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            helper.useBlock(at);
            helper.assertTrue(helper.getBlockState(at).getValue(DoorBlock.OPEN), metal + " door opens by hand");
            var trapdoor = Building.block(metal + "_trapdoor");
            BlockPos hatch = new BlockPos(2, 1, 1);
            helper.setBlock(hatch, trapdoor.defaultBlockState());
            helper.useBlock(hatch);
            helper.assertTrue(helper.getBlockState(hatch).getValue(TrapDoorBlock.OPEN), metal + " trapdoor opens by hand");
        }
        helper.succeed();
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
