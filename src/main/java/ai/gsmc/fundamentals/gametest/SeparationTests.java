package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.separation.Battery;
import ai.gsmc.fundamentals.separation.Stall;
import ai.gsmc.fundamentals.separation.MagneticRecipe;
import ai.gsmc.fundamentals.separation.MagnetomigrationCellBlock;
import ai.gsmc.fundamentals.separation.MagnetomigrationCellBlockEntity;
import ai.gsmc.fundamentals.separation.MixerSettlerBlock;
import ai.gsmc.fundamentals.separation.MixerSettlerBlockEntity;
import ai.gsmc.fundamentals.separation.Paramagnetism;
import ai.gsmc.fundamentals.separation.Separation;
import ai.gsmc.fundamentals.separation.SeparationRecipe;
import ai.gsmc.fundamentals.separation.VatGeometry;
import ai.gsmc.fundamentals.worldgen.DepositFeature;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class SeparationTests {

    private static final int SPIN_UP = 10;

    private static int settle(int stages) {
        return stages * MixerSettlerBlockEntity.EQUILIBRATION_PER_STAGE + 2 * MixerSettlerBlockEntity.PERIOD + 20;
    }
    private static final int PLANT = 18 * MixerSettlerBlockEntity.CAPACITY_PER_CASING / 2;
    private static final BlockState CASING = Separation.mixerSettler().defaultBlockState().setValue(MixerSettlerBlock.FACING, Direction.EAST);

    private static void mixer(GameTestHelper helper, int x, int y, int z) {
        mixer(helper, x, y, z, 64);
    }

    private static void mixer(GameTestHelper helper, int x, int y, int z, int rpm) {
        Block mixer = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:mechanical_mixer"));
        Block cog = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:cogwheel"));
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:creative_motor"));
        helper.setBlock(new BlockPos(x, y, z), mixer.defaultBlockState());
        helper.setBlock(new BlockPos(x, y, z + 1), cog.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        helper.setBlock(new BlockPos(x, y + 1, z + 1), motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN));
        var motorEntity = helper.getBlockEntity(new BlockPos(x, y + 1, z + 1));
        var tag = motorEntity.saveWithoutMetadata(helper.getLevel().registryAccess());
        tag.putInt("ScrollValue", rpm);
        motorEntity.loadWithComponents(tag, helper.getLevel().registryAccess());
    }

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
        helper.setBlock(new BlockPos(1, 1, 0), Blocks.REDSTONE_BLOCK);
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

    private static IFluidHandler port(GameTestHelper helper, int x, Direction side) {
        return port(helper, x, 2, side);
    }

    private static IFluidHandler port(GameTestHelper helper, int x, int y, Direction side) {
        IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(new BlockPos(x, y, 1)), side);
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

    @GameTest(template = "battery", timeoutTicks = 800)
    public void anEightStagePlantBatteryPartsTheLiquor(GameTestHelper helper) {
        int batch = 18 * MixerSettlerBlockEntity.BATCH_PER_CASING;
        SeparationRecipe cut = SeparationRecipe.forLiquor(helper.getLevel(), Separation.fluid("rare_earth_liquor")).orElseThrow();
        int light = cut.lightOf(batch), heavy = cut.heavyOf(batch);
        helper.assertTrue(light + heavy == batch && light > 4 * heavy, "mixed liquor is mostly lights, got " + light + " light to " + heavy + " heavy a batch");
        plantBattery(helper, 8, "p507", () -> {
            helper.assertTrue(fill(helper, 1, Direction.WEST, "rare_earth_liquor", 1000) == 1000, "the head's back face should take the feed");
            helper.assertTrue(fill(helper, 24, Direction.EAST, "hydrochloric_acid", 1000) == 1000, "the tail's front face should take the acid");
            helper.runAfterDelay(settle(8), () -> {
                FluidStack raffinate = held(helper, 1, Direction.NORTH);
                FluidStack strip = held(helper, 24, Direction.NORTH);
                int cuts = raffinate.getAmount() / light;
                helper.assertTrue(raffinate.is(Separation.fluid("light_rare_earth_liquor")) && cuts >= 1 && raffinate.getAmount() == cuts * light,
                        "the head should hold whole cuts of light raffinate, got " + raffinate);
                helper.assertTrue(strip.is(Separation.fluid("heavy_rare_earth_liquor")) && strip.getAmount() == cuts * heavy,
                        "the tail should hold the heavy share of the same cuts, got " + strip);
                helper.assertTrue(held(helper, 1, Direction.WEST).getAmount() == 1000 - cuts * batch && held(helper, 24, Direction.EAST).getAmount() == 1000 - cuts * batch,
                        "each cut spends a batch of feed and of acid");
                int organic = 0;
                for (int i = 0; i < 8; i++) {
                    organic += held(helper, 1 + 3 * i, Direction.UP).getAmount();
                }
                helper.assertTrue(8 * PLANT - organic == cuts * (batch / 50), "the organic is a loop, down only by what each cut entrains, got " + organic);
                FluidStack sump = port(helper, 1, 1, Direction.DOWN).getFluidInTank(0);
                helper.assertTrue(sump.is(Separation.fluid("spent_liquor")) && sump.getAmount() == cuts * (batch / 5),
                        "every cut should leave a fifth of a batch of spent liquor in the sump under the head, got " + sump);
                helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(new BlockPos(10, 1, 1)), Direction.DOWN) == null,
                        "only the head has a sump");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "battery", timeoutTicks = 800)
    public void casingsMergeAsTheyArePlacedAndBreakApartWhenOneGoes(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), CASING);
        helper.setBlock(new BlockPos(1, 1, 2), CASING);
        helper.runAfterDelay(SPIN_UP, () -> {
            MixerSettlerBlockEntity pair = casing(helper, 1, 1, 1);
            helper.assertTrue(pair.isController() && pair.across() == 2 && pair.along() == 1 && pair.tall() == 1 && !pair.isStage(), "two casings side by side merge but are not yet a stage");
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

    @GameTest(template = "battery", timeoutTicks = 800)
    public void aStageWithNoMixerTurningStallsTheBattery(GameTestHelper helper) {
        plantBattery(helper, 8, "p507", () -> {
            fill(helper, 1, Direction.WEST, "rare_earth_liquor", 1000);
            fill(helper, 24, Direction.EAST, "hydrochloric_acid", 1000);
            helper.setBlock(new BlockPos(10, 4, 3), Blocks.AIR);
            helper.runAfterDelay(settle(8), () -> {
                helper.assertTrue(held(helper, 1, Direction.NORTH).isEmpty(), "with one motor gone the battery should stall");
                helper.assertTrue(casing(helper, 1, 1, 1).battery().stall().map(Stall::key).orElse("").equals("mixer"), "the goggles should blame the mixer");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "battery", timeoutTicks = 800)
    public void theBatteryWaitsForItsLever(GameTestHelper helper) {
        plantBattery(helper, 8, "p507", () -> {
            helper.setBlock(new BlockPos(1, 1, 0), Blocks.AIR);
            fill(helper, 1, Direction.WEST, "rare_earth_liquor", 1000);
            fill(helper, 24, Direction.EAST, "hydrochloric_acid", 1000);
            helper.runAfterDelay(settle(8), () -> {
                helper.assertTrue(held(helper, 1, Direction.NORTH).isEmpty(), "no lever, no cut");
                helper.assertTrue(casing(helper, 1, 1, 1).battery().stall().map(Stall::key).orElse("").equals("lever"), "the goggles should ask for the lever");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "battery", timeoutTicks = 100)
    public void thePortsOnlyTakeWhatBelongsInThem(GameTestHelper helper) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 1; dz <= 3; dz++) {
                helper.setBlock(new BlockPos(1 + dx, 2, dz), CASING);
            }
        }
        helper.runAfterDelay(SPIN_UP, () -> {
            helper.assertTrue(port(helper, 1, Direction.NORTH).fill(new FluidStack(Separation.fluid("rare_earth_liquor"), 100), IFluidHandler.FluidAction.EXECUTE) == 0, "the sides only give");
            helper.assertTrue(fill(helper, 1, Direction.UP, "rare_earth_liquor", 100) == 0, "the top is for organics");
            helper.assertTrue(fill(helper, 1, Direction.WEST, "p507", 100) == 0, "the back is for liquor");
            helper.assertTrue(port(helper, 1, Direction.WEST).fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "water is not a reagent");
            helper.assertTrue(fill(helper, 1, Direction.UP, "p507", 100) == 100 && fill(helper, 1, Direction.WEST, "rare_earth_liquor", 100) == 100, "the right fluids go in");
            helper.succeed();
        });
    }

    @GameTest(template = "battery", timeoutTicks = 800)
    public void organicChargedAtTheHeadRunsDownTheBattery(GameTestHelper helper) {
        for (int i = 0; i < 9; i++) {
            for (int dz = 1; dz <= 3; dz++) {
                helper.setBlock(new BlockPos(1 + i, 2, dz), CASING);
            }
        }
        helper.runAfterDelay(SPIN_UP, () -> {
            fill(helper, 1, Direction.UP, "p507", 1125);
            helper.runAfterDelay(400, () -> {
                int head = held(helper, 1, Direction.UP).getAmount(), tail = held(helper, 7, Direction.UP).getAmount();
                helper.assertTrue(tail > 0 && head + held(helper, 4, Direction.UP).getAmount() + tail == 1125,
                        "the organic should spread forward and be conserved, got " + head + " / " + tail);
                helper.succeed();
            });
        });
    }

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
            helper.assertTrue(organic.is(Separation.fluid("p507")) && organic.getAmount() == corner.phaseCapacity(), "the corner's organic should survive the merges up to the phase's half of the vat, got " + organic);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public void theModelsAreCutToTheVatGeometry(GameTestHelper helper) {
        VatGeometry vat = VatGeometry.get();
        helper.assertTrue(vat.brim(1) < vat.weir(1) && vat.weir(1) < 1 && vat.brim(2) < vat.weir(2) && vat.weir(2) < 2,
                "the settled phases stop under the weir and the weir under the rim at every height");
        helper.assertTrue(box(element("floor", 0), "to")[1] == vat.floor(), "the floor model is the geometry's floor");
        helper.assertTrue(box(element("weir_low", 0), "to")[1] == 16 - vat.weirBelowRim() && box(element("weir_lip", 0), "to")[1] == 16 - vat.weirBelowRim(),
                "the weir models stop where the geometry's weir does");
        helper.assertTrue(box(element("weir_low", 0), "to")[2] == vat.wall() && box(element("wall_front", 0), "to")[2] == vat.wall(),
                "the weir and the walls are the geometry's thickness");
        JsonObject glass = element("wall_left_single", 2);
        helper.assertTrue(box(glass, "from")[2] == vat.window()[0] && box(glass, "to")[2] == vat.window()[1], "the window glass spans the geometry's window");
        helper.succeed();
    }

    private static JsonObject element(String model, int index) {
        try (Reader in = new InputStreamReader(SeparationTests.class.getResourceAsStream("/assets/fundamentals/models/block/mixer_settler/" + model + ".json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(in).getAsJsonObject().getAsJsonArray("elements").get(index).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException(model, e);
        }
    }

    private static int[] box(JsonObject element, String corner) {
        JsonArray a = element.getAsJsonArray(corner);
        return new int[] {a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()};
    }

    @GameTest(template = "battery", timeoutTicks = 1200)
    public void aCrudeFeedFoulsTheOrganicAndLimeScrubsIt(GameTestHelper helper) {
        plantBattery(helper, 8, "p507", () -> {
            helper.assertTrue(fill(helper, 1, Direction.WEST, "crude_rare_earth_liquor", 2000) == 2000, "the head should take a crude feed");
            fill(helper, 24, Direction.EAST, "hydrochloric_acid", 1000);
            helper.runAfterDelay(settle(8) + 2 * MixerSettlerBlockEntity.PERIOD, () -> {
                int light = SeparationRecipe.forLiquor(helper.getLevel(), Separation.fluid("rare_earth_liquor")).orElseThrow()
                        .lightOf(18 * MixerSettlerBlockEntity.BATCH_PER_CASING);
                helper.assertTrue(held(helper, 1, Direction.NORTH).getAmount() >= 3 * light, "three cuts should run on crude feed first");
                helper.assertTrue(held(helper, 4, Direction.UP).is(Separation.fluid("fouled_p507")), "the organic should be fouled after three crude cuts");
                helper.assertTrue(casing(helper, 1, 1, 1).battery().stall().map(Stall::key).orElse("").equals("crud"), "the goggles should blame the crud");
                var recipes = helper.getLevel().getRecipeManager();
                for (String id : List.of("mixing/scrub_p507", "mixing/clarify_rare_earth_liquor", "mixing/clarify_heavy_rare_earth_liquor")) {
                    helper.assertTrue(recipes.byKey(Fundamentals.id(id)).isPresent(), id + " is missing");
                }
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public void everyLiquorIsCutDownToSingleElements(GameTestHelper helper) {
        List<SeparationRecipe> cuts = helper.getLevel().getRecipeManager().getAllRecipesFor(SeparationRecipe.TYPE)
                .stream().map(RecipeHolder::value).toList();
        helper.assertTrue(cuts.size() == 13, "expected 13 cuts, found " + cuts.size());
        Set<Fluid> parted = cuts.stream().map(SeparationRecipe::liquor).collect(Collectors.toSet());
        Set<Fluid> made = cuts.stream().flatMap(c -> Stream.of(c.light(), c.heavy())).collect(Collectors.toSet());
        Fluid reduced = Separation.fluid("europium_gadolinium_liquor");
        helper.assertTrue(made.contains(reduced) && !parted.contains(reduced), "europium should leave gadolinium by zinc reduction, not a cut");
        var zinc = (com.simibubi.create.content.processing.recipe.ProcessingRecipe<?, ?>) helper.getLevel().getRecipeManager()
                .byKey(Fundamentals.id("mixing/europium_sulfate")).orElseThrow().value();
        helper.assertTrue(zinc.getIngredients().stream().anyMatch(i -> i.test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:zinc_nugget")))))
                && zinc.getFluidResults().stream().anyMatch(s -> s.getFluid() == Separation.fluid("gadolinium_liquor")), "zinc should drop europium and leave gadolinium liquor");
        for (Fluid liquor : made) {
            if (!parted.contains(liquor) && liquor != reduced) {
                String id = BuiltInRegistries.FLUID.getKey(liquor).getPath();
                helper.assertTrue(id.chars().filter(ch -> ch == '_').count() == 1, id + " is a mixed liquor nothing parts");
                var oxalate = helper.getLevel().getRecipeManager().byKey(Fundamentals.id("mixing/" + id.replace("_liquor", "_oxalate")));
                helper.assertTrue(oxalate.isPresent(), id + " has no oxalate recipe");
            }
        }
        helper.succeed();
    }

    private static final BlockState CELL = Separation.magnetomigrationCell().defaultBlockState().setValue(MagnetomigrationCellBlock.FACING, Direction.EAST);

    private static MagnetomigrationCellBlockEntity line(GameTestHelper helper, int x0, int z, int cells, String liquor) {
        for (int i = 0; i < cells; i++) {
            helper.setBlock(new BlockPos(x0 + i, 1, z), CELL);
        }
        IFluidHandler feed = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(new BlockPos(x0, 1, z)), Direction.WEST);
        helper.assertTrue(feed != null && feed.fill(new FluidStack(Separation.fluid(liquor), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                "the head cell's back should take " + liquor);
        return (MagnetomigrationCellBlockEntity) helper.getBlockEntity(new BlockPos(x0, 1, z));
    }

    private static FluidStack outlet(GameTestHelper helper, int x, int z, Direction side) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(new BlockPos(x, 1, z)), side).getFluidInTank(0);
    }

    @GameTest(template = "battery", timeoutTicks = 400)
    public void aMagnetomigrationLinePartsYttriumFromTheHeavies(GameTestHelper helper) {
        Fluid liquor = Separation.fluid("yttrium_heavies_liquor");
        MagneticRecipe cut = MagneticRecipe.forLiquor(helper.getLevel(), liquor).orElseThrow();
        SeparationRecipe battery = SeparationRecipe.forLiquor(helper.getLevel(), liquor).orElseThrow();
        helper.assertTrue(cut.light() == battery.light() && cut.heavy() == battery.heavy() && cut.lightFraction() == battery.lightFraction()
                && cut.passes() < battery.stages(), "the magnetic cut should give the battery's products in its proportion, in fewer passes than its stages");
        helper.assertTrue(MagneticRecipe.forLiquor(helper.getLevel(), Separation.fluid("praseodymium_neodymium_liquor")).isEmpty(),
                "praseodymium and neodymium are alike in moment and should have no magnetic cut");
        int n = cut.passes(), batch = MagnetomigrationCellBlockEntity.BATCH;
        line(helper, 1, 1, n, "yttrium_heavies_liquor");
        MagnetomigrationCellBlockEntity shortHead = line(helper, 1, 3, n - 1, "yttrium_heavies_liquor");
        MagnetomigrationCellBlockEntity didymium = line(helper, 12, 1, n, "praseodymium_neodymium_liquor");
        helper.runAfterDelay(2 * MagnetomigrationCellBlockEntity.PERIOD + 20, () -> {
            FluidStack drawn = outlet(helper, n, 1, Direction.SOUTH), rest = outlet(helper, n, 1, Direction.NORTH);
            int cuts = rest.getAmount() / cut.lightOf(batch);
            helper.assertTrue(rest.is(Separation.fluid("yttrium_liquor")) && cuts >= 1 && rest.getAmount() == cuts * cut.lightOf(batch),
                    "the tail's far side should hold whole batches of yttrium, got " + rest);
            helper.assertTrue(drawn.is(Separation.fluid("holmium_to_lutetium_liquor")) && drawn.getAmount() == cuts * cut.heavyOf(batch),
                    "the tail's magnet side should hold the heavies of the same batches, got " + drawn);
            helper.assertTrue(shortHead.line().stall().map(s -> s.key().equals("short")).orElse(false)
                    && outlet(helper, n - 1, 3, Direction.NORTH).isEmpty(), "a line short of its passes should stall and part nothing");
            helper.assertTrue(didymium.line().stall().map(s -> s.key().equals("no_cut")).orElse(false), "didymium liquor should stall a line: no magnetic cut");
            helper.succeed();
        });
    }

    @GameTest(template = "battery", timeoutTicks = 600)
    public void aColdLinePartsFasterThanAHotOneAndSamariumBarelyCares(GameTestHelper helper) {
        double nd = Paramagnetism.relative("Nd", -20) / Paramagnetism.relative("Nd", 150), sm = Paramagnetism.relative("Sm", -20) / Paramagnetism.relative("Sm", 150);
        helper.assertTrue(nd > 1.5 && sm < 1.25 && Paramagnetism.relative("Eu", 150) == Paramagnetism.relative("Eu", -20),
                "neodymium should follow Curie's law and samarium and europium mostly not: Nd " + nd + ", Sm " + sm);
        BlockPos hot = new BlockPos(4, 1, 2), cold = new BlockPos(20, 1, 2);
        Heat.boost(helper.getLevel(), helper.absolutePos(hot), 200, 2, 600);
        Heat.boost(helper.getLevel(), helper.absolutePos(cold), -60, 2, 600);
        int n = MagneticRecipe.forLiquor(helper.getLevel(), Separation.fluid("yttrium_heavies_liquor")).orElseThrow().passes();
        MagnetomigrationCellBlockEntity hotHead = line(helper, hot.getX(), hot.getZ(), n, "yttrium_heavies_liquor");
        MagnetomigrationCellBlockEntity coldHead = line(helper, cold.getX(), cold.getZ(), n, "yttrium_heavies_liquor");
        helper.runAfterDelay(500, () -> {
            int hotDrawn = outlet(helper, hot.getX() + n - 1, hot.getZ(), Direction.SOUTH).getAmount();
            int coldDrawn = outlet(helper, cold.getX() + n - 1, cold.getZ(), Direction.SOUTH).getAmount();
            helper.assertTrue(hotHead.celsius() > 150 && coldHead.celsius() < 0, "the heads should read their heat: " + hotHead.celsius() + " / " + coldHead.celsius());
            helper.assertTrue(coldDrawn > hotDrawn && hotDrawn > 0, "the cold line should part more in the same time: cold " + coldDrawn + " mB, hot " + hotDrawn);
            helper.succeed();
        });
    }
}
