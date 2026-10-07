package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.separation.MixerSettlerBlock;
import ai.gsmc.fundamentals.separation.MixerSettlerBlockEntity;
import ai.gsmc.fundamentals.separation.Separation;
import ai.gsmc.fundamentals.separation.SeparationRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class SeparationTests {

    private static final int SETTLE = 2 * MixerSettlerBlockEntity.PERIOD + 15;
    private static final int SPIN_UP = 10;
    private static final int PLANT = 18 * MixerSettlerBlockEntity.CAPACITY_PER_CASING;
    private static final BlockState CASING = Separation.mixerSettler().defaultBlockState().setValue(MixerSettlerBlock.FACING, Direction.EAST);

    /** Create's Mechanical Mixer over the trough at (x, z), driven by a cogwheel beside it under a Creative Motor. */
    private static void mixer(GameTestHelper helper, int x, int y, int z) {
        Block mixer = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:mechanical_mixer"));
        Block cog = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:cogwheel"));
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:creative_motor"));
        helper.setBlock(new BlockPos(x, y, z), mixer.defaultBlockState());
        helper.setBlock(new BlockPos(x, y, z + 1), cog.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        helper.setBlock(new BlockPos(x, y + 1, z + 1), motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN));
    }

    /**
     * A battery of plant stages along x, facing east, charged with the organic on every stage. Each stage is
     * three casings across (z 1..3), three along (x) and two tall (y 1..2); stage {@code i} starts at x = 1 + 3i.
     * Casings merge a tick after placement, so the rest of the test runs in {@code then}.
     */
    private static void plantBattery(GameTestHelper helper, int stages, String organic, Runnable then) {
        for (int i = 0; i < stages; i++) {
            for (int dx = 0; dx < 3; dx++) {
                for (int dz = 0; dz < 3; dz++) {
                    for (int dy = 0; dy < 2; dy++) {
                        helper.setBlock(new BlockPos(1 + 3 * i + dx, 1 + dy, 1 + dz), CASING);
                    }
                }
            }
        }
        for (int i = 0; i < stages; i++) {
            mixer(helper, 1 + 3 * i, 3, 2);
        }
        helper.runAfterDelay(SPIN_UP, () -> {
            for (int i = 0; i < stages; i++) {
                MixerSettlerBlockEntity corner = casing(helper, 1 + 3 * i, 1, 1);
                helper.assertTrue(corner.isController() && corner.volume() == 18, "stage " + i + " should be one 3x3x2 vat, got " + corner.volume());
                helper.assertTrue(corner.isStirred(), "stage " + i + " should feel its mixer turning");
                port(helper, 1 + 3 * i, Direction.UP).fill(new FluidStack(Separation.fluid(organic), PLANT), IFluidHandler.FluidAction.EXECUTE);
            }
            then.run();
        });
    }

    private static MixerSettlerBlockEntity casing(GameTestHelper helper, int x, int y, int z) {
        return (MixerSettlerBlockEntity) helper.getBlockEntity(new BlockPos(x, y, z));
    }

    /** The port on {@code side} of the casing at x on the top-left edge (y 2, z 1), where every end face is exterior. */
    private static IFluidHandler port(GameTestHelper helper, int x, Direction side) {
        IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(new BlockPos(x, 2, 1)), side);
        if (handler == null) {
            helper.fail("casing " + x + " has no port on its " + side + " face: " + helper.getBlockState(new BlockPos(x, 2, 1)));
        }
        return handler;
    }

    private static int fill(GameTestHelper helper, int x, Direction side, String fluid, int amount) {
        return port(helper, x, side).fill(new FluidStack(Separation.fluid(fluid), amount), IFluidHandler.FluidAction.EXECUTE);
    }

    private static FluidStack held(GameTestHelper helper, int x, Direction side) {
        return port(helper, x, side).getFluidInTank(0);
    }

    @GameTest(template = "battery", timeoutTicks = 200)
    public void anEightStagePlantBatteryPartsTheLiquor(GameTestHelper helper) {
        int batch = 18 * MixerSettlerBlockEntity.BATCH_PER_CASING;
        plantBattery(helper, 8, "p507", () -> {
            helper.assertTrue(fill(helper, 1, Direction.WEST, "rare_earth_liquor", 1000) == 1000, "the head's back face should take the feed");
            helper.assertTrue(fill(helper, 24, Direction.EAST, "hydrochloric_acid", 1000) == 1000, "the tail's front face should take the acid");
            helper.runAfterDelay(SETTLE, () -> {
                FluidStack raffinate = held(helper, 1, Direction.NORTH);
                FluidStack strip = held(helper, 24, Direction.NORTH);
                int cuts = raffinate.getAmount() / batch;
                helper.assertTrue(raffinate.is(Separation.fluid("light_rare_earth_liquor")) && cuts >= 1 && raffinate.getAmount() == cuts * batch,
                        "the head should hold whole batches of light raffinate, got " + raffinate);
                helper.assertTrue(strip.is(Separation.fluid("heavy_rare_earth_liquor")) && strip.getAmount() == cuts * batch,
                        "the tail should hold the same batches of loaded strip, got " + strip);
                helper.assertTrue(held(helper, 1, Direction.WEST).getAmount() == 1000 - cuts * batch && held(helper, 24, Direction.EAST).getAmount() == 1000 - cuts * batch,
                        "each cut spends a batch of feed and of acid");
                helper.assertTrue(held(helper, 10, Direction.UP).getAmount() == PLANT, "the organic is a loop, not a consumable");
                helper.succeed();
            });
        });
    }

    /** A lab line: single-wide casings merge three at a time along the axis, so a row of 24 is eight small stages. */
    @GameTest(template = "battery", timeoutTicks = 200)
    public void aRowOfSingleCasingsIsABatteryOfSmallStages(GameTestHelper helper) {
        for (int i = 0; i < 24; i++) {
            helper.setBlock(new BlockPos(1 + i, 2, 1), CASING);
        }
        for (int i = 0; i < 8; i++) {
            mixer(helper, 1 + 3 * i, 3, 1);
        }
        helper.runAfterDelay(SPIN_UP, () -> {
            MixerSettlerBlockEntity head = casing(helper, 1, 2, 1);
            helper.assertTrue(head.isController() && head.along() == 3 && head.volume() == 3, "casings along the axis merge three at a time, got " + head.volume());
            helper.assertTrue(head.battery().size() == 8, "twenty-four casings should be eight stages, got " + head.battery().size());
            for (int i = 0; i < 8; i++) {
                port(helper, 1 + 3 * i, Direction.UP).fill(new FluidStack(Separation.fluid("p507"), 750), IFluidHandler.FluidAction.EXECUTE);
            }
            helper.assertTrue(fill(helper, 1, Direction.WEST, "rare_earth_liquor", 1000) == 3 * MixerSettlerBlockEntity.CAPACITY_PER_CASING,
                    "a three-casing stage holds three casings' worth");
            fill(helper, 24, Direction.EAST, "hydrochloric_acid", 750);
            helper.runAfterDelay(SETTLE, () -> {
                FluidStack raffinate = held(helper, 1, Direction.NORTH);
                int small = 3 * MixerSettlerBlockEntity.BATCH_PER_CASING;
                helper.assertTrue(raffinate.is(Separation.fluid("light_rare_earth_liquor")) && raffinate.getAmount() >= small && raffinate.getAmount() % small == 0,
                        "the head should hold small batches of raffinate, got " + raffinate);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "battery", timeoutTicks = 200)
    public void casingsMergeAsTheyArePlacedAndBreakApartWhenOneGoes(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), CASING);
        helper.setBlock(new BlockPos(1, 1, 2), CASING);
        helper.runAfterDelay(SPIN_UP, () -> {
            MixerSettlerBlockEntity pair = casing(helper, 1, 1, 1);
            helper.assertTrue(pair.isController() && pair.across() == 2 && pair.along() == 1 && pair.tall() == 1, "two casings side by side should be one 2x1x1 stage");
            helper.assertBlockProperty(new BlockPos(1, 1, 1), MixerSettlerBlock.RIGHT, true);
            helper.assertBlockProperty(new BlockPos(1, 1, 2), MixerSettlerBlock.LEFT, true);
            helper.setBlock(new BlockPos(2, 1, 1), CASING);
            helper.setBlock(new BlockPos(2, 1, 2), CASING);
            helper.runAfterDelay(SPIN_UP, () -> {
                MixerSettlerBlockEntity quad = casing(helper, 1, 1, 1);
                helper.assertTrue(quad.isController() && quad.volume() == 4 && quad.along() == 2, "the second pair should merge into a 2x2x1 stage, got " + quad.volume());
                helper.assertBlockProperty(new BlockPos(1, 1, 1), MixerSettlerBlock.ROWS, MixerSettlerBlock.Rows.WELL);
                helper.assertBlockProperty(new BlockPos(2, 1, 1), MixerSettlerBlock.ROWS, MixerSettlerBlock.Rows.BAY);
                helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(new BlockPos(1, 1, 1)), Direction.EAST) == null,
                        "a face shared inside the stage has no port");
                helper.destroyBlock(new BlockPos(2, 1, 2));
                helper.runAfterDelay(SPIN_UP, () -> {
                    MixerSettlerBlockEntity left = casing(helper, 1, 1, 1);
                    helper.assertTrue(left.volume() <= 2, "losing a casing should break the stage into what is left, got " + left.volume());
                    helper.assertBlockProperty(new BlockPos(2, 1, 1), MixerSettlerBlock.LEFT, false);
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "battery", timeoutTicks = 200)
    public void aBatteryTooShortForItsCutDoesNothing(GameTestHelper helper) {
        plantBattery(helper, 7, "p507", () -> {
            fill(helper, 1, Direction.WEST, "rare_earth_liquor", 1000);
            fill(helper, 21, Direction.EAST, "hydrochloric_acid", 1000);
            helper.runAfterDelay(SETTLE, () -> {
                helper.assertTrue(held(helper, 1, Direction.NORTH).isEmpty() && held(helper, 1, Direction.WEST).getAmount() == 1000,
                        "seven stages cannot make an eight-stage cut");
                helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(new BlockPos(11, 2, 1)), Direction.NORTH) == null,
                        "a middle stage has no side port");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "battery", timeoutTicks = 200)
    public void aStageWithNoMixerTurningStallsTheBattery(GameTestHelper helper) {
        plantBattery(helper, 8, "p507", () -> {
            fill(helper, 1, Direction.WEST, "rare_earth_liquor", 1000);
            fill(helper, 24, Direction.EAST, "hydrochloric_acid", 1000);
            helper.setBlock(new BlockPos(10, 4, 3), Blocks.AIR);
            helper.runAfterDelay(SETTLE, () -> {
                helper.assertTrue(held(helper, 1, Direction.NORTH).isEmpty(), "with one motor gone the battery should stall");
                helper.assertTrue(casing(helper, 1, 1, 1).stall().map(s -> s[0]).orElse("").equals("mixer"), "the goggles should blame the mixer");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "battery", timeoutTicks = 100)
    public void thePortsOnlyTakeWhatBelongsInThem(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 1), CASING);
        helper.runAfterDelay(SPIN_UP, () -> {
            helper.assertTrue(fill(helper, 1, Direction.UP, "rare_earth_liquor", 100) == 0, "the top is for organics");
            helper.assertTrue(fill(helper, 1, Direction.WEST, "p507", 100) == 0, "the back is for liquor");
            helper.assertTrue(port(helper, 1, Direction.WEST).fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "water is not a reagent");
            helper.assertTrue(fill(helper, 1, Direction.UP, "p507", 100) == 100 && fill(helper, 1, Direction.WEST, "rare_earth_liquor", 100) == 100, "the right fluids go in");
            helper.succeed();
        });
    }

    @GameTest(template = "battery", timeoutTicks = 200)
    public void theWrongOrganicStallsTheCut(GameTestHelper helper) {
        plantBattery(helper, 8, "p204", () -> {
            fill(helper, 1, Direction.WEST, "rare_earth_liquor", 1000);
            fill(helper, 24, Direction.EAST, "hydrochloric_acid", 1000);
            helper.runAfterDelay(SETTLE, () -> {
                helper.assertTrue(held(helper, 1, Direction.NORTH).isEmpty(), "the first cut wants P507, not P204");
                helper.succeed();
            });
        });
    }

    /** The way a datapack builds one: commands, with tank contents on the corner casing. */
    @GameTest(template = "battery", timeoutTicks = 100)
    public void aStagePlacedByCommandsFormsAndKeepsItsTanks(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4);
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                for (int dy = 0; dy < 2; dy++) {
                    BlockPos at = helper.absolutePos(new BlockPos(1 + dx, 1 + dy, 1 + dz));
                    String nbt = dx == 0 && dz == 0 && dy == 0 ? "{organic:{id:\"fundamentals:p507\",amount:4000}}" : "";
                    server.getCommands().performPrefixedCommand(source, "setblock %d %d %d fundamentals:mixer_settler[facing=east]%s".formatted(at.getX(), at.getY(), at.getZ(), nbt));
                }
            }
        }
        helper.runAfterDelay(3, () -> {
            MixerSettlerBlockEntity corner = casing(helper, 1, 1, 1);
            helper.assertTrue(corner.isController() && corner.volume() == 18, "the commands should leave one 3x3x2 vat, got " + corner.volume());
            FluidStack organic = held(helper, 1, Direction.UP);
            helper.assertTrue(organic.is(Separation.fluid("p507")) && organic.getAmount() == 4000, "the corner's organic should survive the merges, got " + organic);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public void everyRareEarthMetalHasARouteFromItsOxide(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String element : List.of("lanthanum", "cerium", "praseodymium", "neodymium", "didymium", "gadolinium", "terbium", "dysprosium",
                "holmium", "erbium", "lutetium", "yttrium", "samarium", "europium", "thulium", "ytterbium")) {
            helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "reduction/" + element + "_ingot")).isPresent(),
                    element + " has no reduction to metal");
            helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "calcining/" + element + "_oxide")).isPresent(),
                    element + " has no calcining to oxide");
        }
        for (String reagent : List.of("mixing/hydrofluoric_acid", "reduction/argon", "reduction/calcium_ingot", "mixing/neodymium_fluoride")) {
            helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, reagent)).isPresent(), reagent + " is missing");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void everyLiquorIsCutDownToSingleElements(GameTestHelper helper) {
        List<SeparationRecipe> cuts = helper.getLevel().getRecipeManager().getAllRecipesFor(SeparationRecipe.TYPE)
                .stream().map(RecipeHolder::value).toList();
        helper.assertTrue(cuts.size() == 14, "expected 14 cuts, found " + cuts.size());
        Set<Fluid> parted = cuts.stream().map(SeparationRecipe::liquor).collect(Collectors.toSet());
        Set<Fluid> made = cuts.stream().flatMap(c -> Stream.of(c.light(), c.heavy())).collect(Collectors.toSet());
        for (Fluid liquor : made) {
            if (!parted.contains(liquor)) {
                String id = BuiltInRegistries.FLUID.getKey(liquor).getPath();
                helper.assertTrue(id.chars().filter(ch -> ch == '_').count() == 1, id + " is a mixed liquor nothing parts");
                var oxalate = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID,
                        "mixing/" + id.replace("_liquor", "_oxalate")));
                helper.assertTrue(oxalate.isPresent(), id + " has no oxalate recipe");
            }
        }
        helper.succeed();
    }
}
