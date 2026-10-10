package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.liquid.Liquid;
import ai.gsmc.fundamentals.liquid.Liquids;
import ai.gsmc.fundamentals.separation.Acids;
import ai.gsmc.fundamentals.separation.Hazards;
import ai.gsmc.fundamentals.separation.Reagents;
import ai.gsmc.fundamentals.separation.Separation;
import ai.gsmc.fundamentals.titanium.Titanium;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class LiquidTests {

    private static Fluid fluid(String id) {
        return BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
    }

    private static void hold(GameTestHelper helper, BlockPos at, double celsius) {
        BlockPos pos = helper.absolutePos(at);
        Heat.boost(helper.getLevel(), pos, celsius - Heat.at(helper.getLevel(), pos), 0, 400);
    }

    @GameTest(template = "battery", timeoutTicks = 100)
    public void everyFluidHasItsPropertiesAndTheHazardsAgree(GameTestHelper helper) {
        int foreign = 30;
        helper.assertTrue(BuiltInRegistries.FLUID.getDataMap(Liquids.PROPERTIES).size() == Reagents.ALL.size() + foreign,
                "every reagent and the " + foreign + " fluids of vanilla and the Factory should have properties, got " + BuiltInRegistries.FLUID.getDataMap(Liquids.PROPERTIES).size());
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        for (Reagents.Reagent reagent : Reagents.ALL) {
            Fluid fluid = Separation.fluid(reagent.id());
            Liquid liquid = Liquids.of(fluid);
            helper.assertTrue(liquid != null && Liquids.describe(helper.getLevel(), pos, fluid, new ArrayList<>(), true), reagent.id() + " should have properties and goggles");
            FluidStack stack = new FluidStack(fluid, 1);
            boolean eats = Hazards.chance(stack) > 0 || Hazards.chance(Titanium.pipe().defaultBlockState(), stack, 20) > 0;
            helper.assertTrue(liquid.corrosive() == eats, reagent.id() + " should be corrosive exactly when Hazards lets it eat a pipe");
            Acids.Acid acid = Acids.all().get(reagent.id());
            helper.assertTrue(liquid.fuming() == (acid != null && acid.fumes) && (acid == null || liquid.toxic() == acid.poisons),
                    reagent.id() + " should fume and poison as Acids has it");
        }
        helper.assertTrue(Liquids.of(Acids.all().get("hydrochloric_acid").flowing) != null, "a flowing acid should read as its source");
        helper.assertTrue(Liquids.pumping(Fluids.WATER) == 1 && Liquids.pumping(fluid("tfmg:heavy_oil")) < 0.6 && Liquids.pumping(fluid("tfmg:molten_plastic")) == 0.25,
                "water should pump freely, heavy oil at about half, and a polymer melt at the floor");
        helper.succeed();
    }

    @GameTest(template = "battery", timeoutTicks = 100)
    public void waterFreezesBelowZeroAndSeawaterOnlyBelowItsOwnPoint(GameTestHelper helper) {
        BlockPos cold = new BlockPos(2, 1, 2), colder = new BlockPos(8, 1, 2), mild = new BlockPos(14, 1, 2);
        hold(helper, cold, -1);
        hold(helper, colder, -3);
        hold(helper, mild, 5);
        var level = helper.getLevel();
        Fluid seawater = Separation.fluid("seawater"), acid = Acids.all().get("hydrochloric_acid").source;
        helper.assertTrue(Liquids.frozen(level, helper.absolutePos(cold), Fluids.WATER), "water should freeze at -1 °C");
        helper.assertFalse(Liquids.frozen(level, helper.absolutePos(cold), seawater), "seawater should still flow at -1 °C");
        helper.assertTrue(Liquids.frozen(level, helper.absolutePos(colder), seawater), "seawater should freeze at -3 °C");
        helper.assertFalse(Liquids.frozen(level, helper.absolutePos(colder), acid), "concentrated hydrochloric acid should flow far below that");
        helper.assertFalse(Liquids.frozen(level, helper.absolutePos(mild), Fluids.WATER), "water should flow at 5 °C");
        helper.succeed();
    }

    @GameTest(template = "battery", timeoutTicks = 100)
    public void keroseneBesideLavaCatchesFire(GameTestHelper helper) {
        for (int x = 1; x <= 21; x++) {
            helper.setBlock(new BlockPos(x, 0, 2), Blocks.STONE);
        }
        BlockPos lava = new BlockPos(2, 1, 2), beside = lava.east(), far = new BlockPos(20, 1, 2);
        helper.setBlock(lava, Blocks.LAVA);
        Fluid kerosene = fluid("tfmg:kerosene");
        helper.runAfterDelay(25, () -> {
            var level = helper.getLevel();
            helper.assertFalse(Liquids.ignite(level, helper.absolutePos(beside), Fluids.WATER), "water should not burn");
            helper.assertFalse(Liquids.ignite(level, helper.absolutePos(far), kerosene), "kerosene in the cool should not light");
            helper.assertTrue(Liquids.ignite(level, helper.absolutePos(beside), kerosene), "kerosene let out beside lava should catch");
            helper.assertTrue(helper.getBlockState(beside).getBlock() instanceof BaseFireBlock, "and burn there");
            Block spilt = kerosene.defaultFluidState().createLegacyBlock().getBlock();
            helper.assertTrue(spilt.defaultBlockState().isFlammable(level, helper.absolutePos(far), Direction.UP), "a pool of kerosene should burn as fire's fuel");
            helper.succeed();
        });
    }

    @GameTest(template = "battery", timeoutTicks = 200)
    public void anOpenBasinOfHotHydrochloricAcidBoilsOffAndACoveredOneDoesNot(GameTestHelper helper) {
        Block basin = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:basin"));
        BlockPos open = new BlockPos(2, 1, 2), covered = new BlockPos(8, 1, 2);
        Fluid acid = Acids.all().get("hydrochloric_acid").source;
        for (BlockPos at : new BlockPos[] {open, covered}) {
            helper.setBlock(at, basin.defaultBlockState());
            IFluidHandler tanks = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(at), null);
            tanks.fill(new FluidStack(acid, 1000), IFluidHandler.FluidAction.EXECUTE);
            hold(helper, at, 90);
        }
        helper.setBlock(covered.above(2), Blocks.STONE);
        helper.runAfterDelay(100, () -> {
            int left = amount(helper, open), kept = amount(helper, covered);
            helper.assertTrue(left < 1000 && left >= 1000 - 3 * Liquids.VENT, "an open basin of 36% hydrochloric acid at 90 °C should boil off, got " + left);
            helper.assertTrue(kept == 1000, "a covered one should keep it, got " + kept);
            helper.succeed();
        });
    }

    private static int amount(GameTestHelper helper, BlockPos at) {
        IFluidHandler tanks = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(at), null);
        int total = 0;
        for (int i = 0; i < tanks.getTanks(); i++) {
            total += tanks.getFluidInTank(i).getAmount();
        }
        return total;
    }
}
