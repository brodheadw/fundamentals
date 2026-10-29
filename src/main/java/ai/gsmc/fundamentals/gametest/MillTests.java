package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.mill.WoodenKineticBlockEntity;
import ai.gsmc.fundamentals.mill.WoodenMill;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class MillTests {

    private static void motor(GameTestHelper helper, BlockPos pos, int speed) {
        helper.setBlock(pos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.SOUTH));
        CreativeMotorBlockEntity motor = helper.getBlockEntity(pos);
        motor.generatedSpeed.setValue(speed);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public void anOakShaftTurnsAtAMillsPaceAndSnapsToSticksSpunTooFast(GameTestHelper helper) {
        BlockPos slow = new BlockPos(1, 2, 1), fast = new BlockPos(3, 2, 1);
        for (BlockPos motor : new BlockPos[] {slow, fast}) {
            helper.setBlock(motor.south(), WoodenMill.shaft().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
            helper.setBlock(motor.south(2), WoodenMill.cogwheel().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        }
        motor(helper, slow, 32);
        motor(helper, fast, 128);
        helper.succeedWhen(() -> {
            WoodenKineticBlockEntity turning = helper.getBlockEntity(slow.south(2));
            helper.assertTrue(turning.getSpeed() == 32, "an oak cog behind an oak shaft should turn at 32 RPM, not " + turning.getSpeed());
            helper.assertBlockNotPresent(WoodenMill.shaft(), fast.south());
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(fast.south())).inflate(2),
                    item -> item.getItem().is(Items.STICK)).isEmpty(), "an oak shaft spun at 128 RPM should snap to sticks");
        });
    }
}
