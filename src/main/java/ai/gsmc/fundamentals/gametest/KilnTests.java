package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.kiln.Kiln;
import ai.gsmc.fundamentals.kiln.KilnDrumBlockEntity;
import com.drmangotea.tfmg.registry.TFMGBlocks;
import com.drmangotea.tfmg.registry.TFMGFluids;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class KilnTests {

    /** Four drum lengths along +x at row z, turned from the west by a motor, a burner under the east (hot) end, a chest past it. */
    private static BlockPos kiln(GameTestHelper helper, int z, int rpm, BlazeBurnerBlock.HeatLevel heat) {
        for (int x = 2; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 2, z), Kiln.drum().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        }
        BlockPos burner = new BlockPos(5, 1, z);
        helper.setBlock(burner, AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, heat));
        ((BlazeBurnerBlockEntity) helper.getBlockEntity(burner)).isCreative = true;
        BlockPos motor = new BlockPos(1, 2, z);
        helper.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.EAST));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(motor)).generatedSpeed.setValue(rpm);
        helper.setBlock(new BlockPos(6, 2, z), Blocks.CHEST);
        return new BlockPos(2, 2, z);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(Fundamentals.id(id));
    }

    private static void feed(GameTestHelper helper, BlockPos feed, Item item) {
        // the motor's speed reaches the drum a tick after it is set
        helper.runAfterDelay(2, () -> {
            KilnDrumBlockEntity drum = helper.getBlockEntity(feed);
            helper.assertTrue(drum.insert(new ItemStack(item, 4), false).isEmpty(), "the feed end takes four");
            helper.assertTrue(((KilnDrumBlockEntity) helper.getBlockEntity(feed.east())).insert(new ItemStack(item), true).getCount() == 1,
                    "only the feed end takes a charge");
        });
    }

    private static int inChest(GameTestHelper helper, int z, Item item) {
        ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(6, 2, z));
        int count = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).is(item)) count += chest.getItem(i).getCount();
        }
        return count;
    }

    @GameTest(template = "empty", timeoutTicks = 500)
    public void aSlowKilnRoastsBastnasiteOnItsWayToTheHotEnd(GameTestHelper helper) {
        feed(helper, kiln(helper, 1, 32, BlazeBurnerBlock.HeatLevel.KINDLED), item("bastnasite_concentrate"));
        helper.succeedWhen(() -> helper.assertTrue(inChest(helper, 1, item("roasted_bastnasite")) == 4,
                "four roasted bastnasite out of the hot end, not " + inChest(helper, 1, item("roasted_bastnasite"))));
    }

    @GameTest(template = "empty", timeoutTicks = 500)
    public void tooFastOrTooCoolTheChargePassesThroughRaw(GameTestHelper helper) {
        // at 256 RPM bastnasite lies 30 ticks above 600 °C, not the 100 it needs; a kindled kiln never reaches clinker's 1,450
        feed(helper, kiln(helper, 1, 256, BlazeBurnerBlock.HeatLevel.KINDLED), item("bastnasite_concentrate"));
        feed(helper, kiln(helper, 4, 32, BlazeBurnerBlock.HeatLevel.KINDLED), item("cement_raw_meal"));
        helper.succeedWhen(() -> {
            helper.assertTrue(inChest(helper, 1, item("bastnasite_concentrate")) == 4, "the fast kiln passes the bastnasite raw");
            helper.assertTrue(inChest(helper, 4, item("cement_raw_meal")) == 4, "the kindled kiln passes the raw meal unburnt");
            helper.assertTrue(inChest(helper, 4, item("cement_clinker")) == 0, "and makes no clinker");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 500)
    public void aSeethingKilnBurnsClinker(GameTestHelper helper) {
        feed(helper, kiln(helper, 1, 32, BlazeBurnerBlock.HeatLevel.SEETHING), item("cement_raw_meal"));
        helper.succeedWhen(() -> helper.assertTrue(inChest(helper, 1, item("cement_clinker")) == 4, "four clinker out of a seething kiln"));
    }

    @GameTest(template = "empty", timeoutTicks = 500)
    public void aFireboxBurningPropaneBurnsClinker(GameTestHelper helper) {
        BlockPos feed = kiln(helper, 1, 32, BlazeBurnerBlock.HeatLevel.SEETHING);
        BlockPos firebox = new BlockPos(5, 1, 1);
        helper.setBlock(firebox, TFMGBlocks.FIREBOX.getDefaultState());
        IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(firebox), null);
        helper.assertTrue(tank != null && tank.fill(new FluidStack(TFMGFluids.PROPANE.get(), 8000), IFluidHandler.FluidAction.EXECUTE) > 0, "the firebox takes propane");
        feed(helper, feed, item("cement_raw_meal"));
        helper.succeedWhen(() -> helper.assertTrue(inChest(helper, 1, item("cement_clinker")) == 4,
                "four clinker out of a propane-fired kiln, not " + inChest(helper, 1, item("cement_clinker"))));
    }
}
