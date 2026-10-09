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
        // a cold test world eats slowly: up to ten bites of twenty ticks
        helper.runAfterDelay(240, () -> {
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 1, 2));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 1, 2));
            helper.assertTrue(!helper.getBlockState(new BlockPos(3, 1, 2)).is(Acids.all().get("hydrochloric_acid").block), "the acid that ate the calcite should be spent");
            helper.succeed();
        });
    }

    @GameTest(template = "battery", timeoutTicks = 400)
    public void hydrofluoricAcidEatsGlassAndHurts(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.GLASS);
        pour(helper, "hydrofluoric_acid", new BlockPos(2, 2, 2));
        var pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG, new BlockPos(5, 1, 2));
        pour(helper, "hydrofluoric_acid", new BlockPos(5, 1, 2));
        float health = pig.getHealth();
        helper.runAfterDelay(240, () -> {
            // the glass is gone; what is left of the acid runs into the hole, as it should
            helper.assertTrue(!helper.getBlockState(new BlockPos(2, 1, 2)).is(Blocks.GLASS), "hydrofluoric acid should eat glass");
            helper.assertTrue(pig.getHealth() < health, "a pig standing in hydrofluoric acid should be hurt");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public void everyAcidHasABucketAndABlock(GameTestHelper helper) {
        for (String acid : new String[] {"hydrochloric_acid", "hydrofluoric_acid", "nitric_acid", "phosphoric_acid", "aqua_regia"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, acid + "_bucket")), acid + " has no bucket");
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, acid)), acid + " has no block");
            helper.assertTrue(BuiltInRegistries.FLUID.containsKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, acid + "_flowing")), acid + " has no flowing form");
        }
        helper.succeed();
    }

    @GameTest(template = "battery", timeoutTicks = 200)
    public void hydrofluoricFumesHurtAtADistance(GameTestHelper helper) {
        var pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG, new BlockPos(5, 1, 2));
        pour(helper, "hydrofluoric_acid", new BlockPos(2, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.STONE);
        float health = pig.getHealth();
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(pig.getHealth() < health, "a pig two blocks from hydrofluoric acid, not touching it, should be hurt by the fumes");
            helper.succeed();
        });
    }

    @GameTest(template = "battery", timeoutTicks = 300)
    public void copperPipesCorrodeUnderAcid(GameTestHelper helper) {
        Block tank = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "plastic_fluid_tank"));
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

    @GameTest(template = "empty")
    public void liquorsEatCopperSlowlyAndPlasticHolds(GameTestHelper helper) {
        Block plastic = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("tfmg", "plastic_pipe"));
        Block copper = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "fluid_pipe"));
        helper.assertFalse(Hazards.corrodible(plastic.defaultBlockState()), "a plastic pipe should not corrode");
        helper.assertTrue(Hazards.corrodible(copper.defaultBlockState()), "a copper pipe should");
        helper.assertTrue(Hazards.corrodible(BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_pump")).defaultBlockState()), "so should a copper pump");
        helper.assertFalse(Hazards.corrodible(BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("tfmg", "plastic_mechanical_pump")).defaultBlockState()), "but not a plastic one");
        double liquor = Hazards.chance(new net.neoforged.neoforge.fluids.FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "rare_earth_liquor")), 1));
        double acid = Hazards.chance(new net.neoforged.neoforge.fluids.FluidStack(Acids.all().get("hydrochloric_acid").source, 1));
        helper.assertTrue(liquor > 0 && liquor < acid, "a liquor should eat copper, more slowly than acid: " + liquor + " vs " + acid);
        helper.succeed();
    }

    @GameTest(template = "battery", timeoutTicks = 300)
    public void copperTanksCorrodeAndPlasticAndCreativeDoNot(GameTestHelper helper) {
        Block copper = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:fluid_tank"));
        Block plastic = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "plastic_fluid_tank"));
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
