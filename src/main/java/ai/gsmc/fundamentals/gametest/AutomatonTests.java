package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.automaton.Automata;
import ai.gsmc.fundamentals.automaton.Automaton;
import ai.gsmc.fundamentals.automaton.Boilers;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class AutomatonTests {

    private static Automaton automaton(GameTestHelper helper, BlockPos at) {
        Automaton automaton = helper.spawn(Automata.entity(), at);
        automaton.setSteam(Automaton.MAX_STEAM);
        return automaton;
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public void anAutomatonClimbsAScaffoldingTower(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        for (int y = 1; y <= 7; y++) helper.setBlock(new BlockPos(1, y, 1), Blocks.SCAFFOLDING);
        Automaton automaton = automaton(helper, new BlockPos(1, 1, 1));
        BlockPos top = helper.absolutePos(new BlockPos(1, 8, 1));
        double start = automaton.getY();
        automaton.aim(top);
        helper.onEachTick(() -> automaton.getNavigation().moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 1.0));
        helper.succeedWhen(() -> helper.assertTrue(automaton.getY() - start >= 3, "climbed only " + (automaton.getY() - start)));
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public void anAutomatonBlowsSteamAtAHostile(GameTestHelper helper) {
        Automaton automaton = automaton(helper, new BlockPos(0, 1, 0));
        var husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 1, 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "the husk wasn't scalded");
            helper.assertTrue(automaton.steam() < Automaton.MAX_STEAM - Automaton.BLAST_COST + 1, "the blast cost no steam");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public void anAutomatonPicksUpWhatIsDropped(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        Automaton automaton = automaton(helper, new BlockPos(0, 1, 0));
        var apple = new ItemEntity(helper.getLevel(), automaton.getX() + 2, automaton.getY(), automaton.getZ() + 2, new ItemStack(Items.APPLE));
        apple.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        helper.getLevel().addFreshEntity(apple);
        helper.succeedWhen(() -> helper.assertTrue(automaton.cargo().countItem(Items.APPLE) == 1, "the apple wasn't fetched"));
    }

    @GameTest(template = "empty")
    public void aLitBoilerIsFoundAndRefills(GameTestHelper helper) {
        BlockPos tankPos = new BlockPos(2, 1, 2);
        helper.setBlock(tankPos, AllBlocks.FLUID_TANK.get());
        BlockPos abs = helper.absolutePos(tankPos);
        helper.assertFalse(Boilers.steaming(helper.getLevel(), abs), "a cold tank raises steam");
        var boiler = ((FluidTankBlockEntity) helper.getLevel().getBlockEntity(abs)).getControllerBE().boiler;
        boiler.waterSupply = 1;
        boiler.activeHeat = 1;
        helper.assertTrue(Boilers.steaming(helper.getLevel(), abs), "a heated, watered boiler raises no steam");
        helper.assertTrue(abs.equals(Boilers.nearest(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), Automaton.BOILER_SEARCH)), "the boiler wasn't found");
        Automaton automaton = automaton(helper, new BlockPos(0, 1, 0));
        automaton.setSteam(100);
        automaton.refill();
        helper.assertTrue(automaton.steam() == 100 + Automaton.REFILL_PER_TICK, "standing at the boiler didn't refill it");
        helper.succeed();
    }
}
