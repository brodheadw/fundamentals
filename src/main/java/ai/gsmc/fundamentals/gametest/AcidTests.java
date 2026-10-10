package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.separation.Acids;
import ai.gsmc.fundamentals.separation.Hazards;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class AcidTests {

    private static void pour(GameTestHelper helper, String acid, BlockPos at) {
        helper.setBlock(at, Acids.all().get(acid).block.defaultBlockState());
    }

    @GameTest(template = "battery", timeoutTicks = 400)
    public void hydrochloricAcidEatsCalciteButNotStoneAndIsSpent(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.CALCITE);
        helper.setBlock(new BlockPos(6, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.STONE);
        pour(helper, "hydrochloric_acid", new BlockPos(3, 1, 2));
        pour(helper, "hydrochloric_acid", new BlockPos(5, 1, 2));
        helper.runAfterDelay(240, () -> {
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 1, 2));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 1, 2));
            helper.assertTrue(!helper.getBlockState(new BlockPos(3, 1, 2)).is(Acids.all().get("hydrochloric_acid").block), "the acid that ate the calcite should be spent");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public void whatEatsWhichWall(GameTestHelper helper) {
        helper.assertTrue(Acids.all().get("hydrochloric_acid").fumes && !Acids.all().get("phosphoric_acid").fumes, "concentrated hydrochloric acid fumes; phosphoric does not");
        var plastic = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfmg:plastic_pipe")).defaultBlockState();
        var aluminium = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfmg:aluminum_pipe")).defaultBlockState();
        var copper = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:fluid_pipe")).defaultBlockState();
        var bromine = new net.neoforged.neoforge.fluids.FluidStack(Acids.all().get("bromine").source, 1);
        var nitric = new net.neoforged.neoforge.fluids.FluidStack(Acids.all().get("nitric_acid").source, 1);
        var hydrochloric = new net.neoforged.neoforge.fluids.FluidStack(Acids.all().get("hydrochloric_acid").source, 1);
        helper.assertTrue(Hazards.chance(plastic, bromine, 20) > 0 && Hazards.chance(plastic, hydrochloric, 20) == 0, "bromine should eat plastic, and the acids not");
        helper.assertTrue(Hazards.chance(aluminium, nitric, 20) == 0 && Hazards.chance(aluminium, hydrochloric, 20) > 0 && Hazards.chance(copper, nitric, 20) > 0,
                "nitric acid should leave aluminium alone and still eat copper");
        helper.succeed();
    }

    @GameTest(template = "battery", timeoutTicks = 300)
    public void copperPipesCorrodeUnderAcid(GameTestHelper helper) {
        Block tank = BuiltInRegistries.BLOCK.get(Fundamentals.id("plastic_fluid_tank"));
        Block pump = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfmg:plastic_mechanical_pump"));
        Block pipe = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:fluid_pipe"));
        Block cog = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:cogwheel"));
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:creative_motor"));
        helper.setBlock(new BlockPos(2, 1, 2), tank.defaultBlockState());
        helper.setBlock(new BlockPos(3, 1, 2), pump.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(4, 1, 2), pipe.defaultBlockState());
        helper.setBlock(new BlockPos(5, 1, 2), pipe.defaultBlockState());
        helper.setBlock(new BlockPos(6, 1, 2), tank.defaultBlockState());
        helper.setBlock(new BlockPos(3, 2, 2), cog.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X));
        helper.setBlock(new BlockPos(2, 2, 2), motor.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.EAST));
        var acid = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, helper.absolutePos(new BlockPos(2, 1, 2)), Direction.UP);
        acid.fill(new net.neoforged.neoforge.fluids.FluidStack(Acids.all().get("hydrochloric_acid").source, 4000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        double was = Hazards.corrosionChance;
        Hazards.corrosionChance = 1.0;
        helper.runAfterDelay(120, () -> {
            Hazards.corrosionChance = was;
            boolean burst = !helper.getBlockState(new BlockPos(4, 1, 2)).is(pipe) || !helper.getBlockState(new BlockPos(5, 1, 2)).is(pipe);
            helper.assertTrue(burst, "a copper pipe carrying hydrochloric acid should have burst, got " + helper.getBlockState(new BlockPos(4, 1, 2)) + " / " + helper.getBlockState(new BlockPos(5, 1, 2)));
            helper.succeed();
        });
    }

    @GameTest(template = "battery", timeoutTicks = 300)
    public void copperTanksCorrodeAndPlasticAndCreativeDoNot(GameTestHelper helper) {
        Block copper = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:fluid_tank"));
        Block plastic = BuiltInRegistries.BLOCK.get(Fundamentals.id("plastic_fluid_tank"));
        Block creative = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:creative_fluid_tank"));
        helper.assertTrue(Hazards.corrodible(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfmg:steel_fluid_tank")).defaultBlockState()), "a steel tank should corrode");
        var acid = new net.neoforged.neoforge.fluids.FluidStack(Acids.all().get("hydrochloric_acid").source, 1000);
        BlockPos[] at = {new BlockPos(2, 1, 2), new BlockPos(10, 1, 2), new BlockPos(18, 1, 2)};
        Block[] tanks = {copper, plastic, creative};
        for (int i = 0; i < 3; i++) {
            helper.setBlock(at[i], tanks[i].defaultBlockState());
            var tank = (com.simibubi.create.content.fluids.tank.FluidTankBlockEntity) helper.getBlockEntity(at[i]);
            if (tank.getTankInventory() instanceof com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity.CreativeSmartFluidTank endless) {
                endless.setContainedFluid(acid);
            } else {
                tank.getTankInventory().fill(acid.copy(), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            }
        }
        double was = Hazards.corrosionChance;
        Hazards.corrosionChance = 1.0;
        helper.runAfterDelay(100, () -> {
            Hazards.corrosionChance = was;
            helper.assertBlockNotPresent(copper, at[0]);
            helper.assertBlockPresent(plastic, at[1]);
            helper.assertBlockPresent(creative, at[2]);
            helper.succeed();
        });
    }
}
