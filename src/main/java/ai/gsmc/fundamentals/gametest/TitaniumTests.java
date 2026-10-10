package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.heat.Heat;
import ai.gsmc.fundamentals.separation.Acids;
import ai.gsmc.fundamentals.separation.Hazards;
import ai.gsmc.fundamentals.separation.Separation;
import ai.gsmc.fundamentals.titanium.Titanium;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class TitaniumTests {

    private static void tank(GameTestHelper helper, Block block, BlockPos at, Fluid fluid) {
        helper.setBlock(at, block.defaultBlockState());
        ((FluidTankBlockEntity) helper.getBlockEntity(at)).getTankInventory().fill(new FluidStack(fluid, 1000), IFluidHandler.FluidAction.EXECUTE);
    }

    @GameTest(template = "battery", timeoutTicks = 300)
    public void titaniumHoldsHydrochloricButNotHydrofluoricAndPlasticSoftensHot(GameTestHelper helper) {
        FluidStack hcl = new FluidStack(Acids.all().get("hydrochloric_acid").source, 1);
        helper.assertTrue(Hazards.wall(Titanium.pipe().defaultBlockState()) == Hazards.Wall.TITANIUM
                && Hazards.chance(Titanium.pipe().defaultBlockState(), hcl, 20) == 0 && Hazards.chance(Titanium.pipe().defaultBlockState(), hcl, 90) > 0,
                "a titanium pipe should hold cold hydrochloric acid and not hot");
        BlockPos chloride = new BlockPos(2, 1, 2), fluoride = new BlockPos(8, 1, 2), hot = new BlockPos(14, 1, 2), cool = new BlockPos(20, 1, 2);
        Block plastic = Separation.plasticTank();
        tank(helper, Titanium.tank(), chloride, Acids.all().get("hydrochloric_acid").source);
        tank(helper, Titanium.tank(), fluoride, Acids.all().get("hydrofluoric_acid").source);
        Heat.boost(helper.getLevel(), helper.absolutePos(hot), 200, 1, 300);
        tank(helper, plastic, hot, Fluids.WATER);
        tank(helper, plastic, cool, Fluids.WATER);
        double acid = Hazards.corrosionChance, fluorine = Hazards.fluorideChance, soft = Hazards.softeningChance;
        Hazards.corrosionChance = Hazards.fluorideChance = Hazards.softeningChance = 1.0;
        helper.runAfterDelay(100, () -> {
            Hazards.corrosionChance = acid;
            Hazards.fluorideChance = fluorine;
            Hazards.softeningChance = soft;
            helper.assertBlockPresent(Titanium.tank(), chloride);
            helper.assertBlockNotPresent(Titanium.tank(), fluoride);
            helper.assertBlockNotPresent(plastic, hot);
            helper.assertBlockPresent(plastic, cool);
            helper.succeed();
        });
    }
}
