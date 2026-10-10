package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.power.Electricity;
import ai.gsmc.fundamentals.power.PanelRackBlock;
import ai.gsmc.fundamentals.power.SolarPanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class PowerTests {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty", timeoutTicks = 400)
    public void aPanelMountedOnARackMakesPowerUnderTheSunAndNoneInShade(GameTestHelper helper) {
        helper.setDayTime(6000);
        helper.setBlock(POS, Electricity.panelRack().defaultBlockState().setValue(PanelRackBlock.FACING, Direction.EAST));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Electricity.photovoltaicPanel()));
        helper.useBlock(POS, player);
        helper.assertBlockState(POS, state -> state.is(Electricity.solarPanel()) && state.getValue(PanelRackBlock.FACING) == Direction.EAST,
                () -> "a panel put on a rack should make a solar panel facing the way the rack did");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "mounting should use up the panel");
        SolarPanelBlockEntity panel = helper.getBlockEntity(POS);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(panel.voltageGeneration() == SolarPanelBlockEntity.VOLTS && panel.powerGeneration() > 0,
                        "at noon under open sky the panel should generate, got " + panel.voltageGeneration() + " V, " + panel.powerGeneration() + " W"))
                .thenExecute(() -> helper.assertTrue(panel.getNetworkPowerGeneration() >= panel.powerGeneration(),
                        "its network should carry what it makes, got " + panel.getNetworkPowerGeneration() + " W"))
                .thenExecute(() -> helper.setBlock(POS.above(), Blocks.STONE))
                .thenWaitUntil(() -> helper.assertTrue(panel.voltageGeneration() == 0 && panel.powerGeneration() == 0, "a shaded panel should make nothing"))
                .thenSucceed();
    }
}
