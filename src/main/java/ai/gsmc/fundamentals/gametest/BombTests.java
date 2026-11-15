package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.bombs.Bombs;
import ai.gsmc.fundamentals.bombs.Charge;
import ai.gsmc.fundamentals.bombs.Detonation;
import ai.gsmc.fundamentals.bombs.ExplosiveBlock;
import ai.gsmc.fundamentals.bombs.Fallout;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class BombTests {

    private static List<Charge> lit(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(Charge.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(6));
    }

    private static void snuff(GameTestHelper helper) {
        lit(helper).forEach(Charge::discard);
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static Recipe<?> recipe(GameTestHelper helper, String id) {
        return helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow(() -> new AssertionError("no recipe " + id)).value();
    }

    @GameTest(template = "empty", batch = "bombs")
    public void powderTakesAFlameButCastTntOnlyASignal(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos keg = new BlockPos(0, 1, 0), tnt = new BlockPos(2, 1, 2);
        helper.setBlock(keg, Bombs.block(ExplosiveBlock.Kind.POWDER_KEG));
        helper.setBlock(tnt, Bombs.block(ExplosiveBlock.Kind.TNT_CHARGE));
        level.getBlockState(helper.absolutePos(keg)).onCaughtFire(level, helper.absolutePos(keg), Direction.UP, null);
        level.getBlockState(helper.absolutePos(tnt)).onCaughtFire(level, helper.absolutePos(tnt), Direction.UP, null);
        helper.assertTrue(lit(helper).size() == 1 && lit(helper).get(0).getBlockState().is(Bombs.block(ExplosiveBlock.Kind.POWDER_KEG)),
                "fire should light the keg and only the keg");
        helper.assertBlockPresent(Bombs.block(ExplosiveBlock.Kind.TNT_CHARGE), tnt);
        helper.setBlock(tnt.above(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(lit(helper).size() == 2, "a signal should fire the cast charge's cap");
        snuff(helper);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "bombs")
    public void anfoNeedsABoosterAndFailsWet(GameTestHelper helper) {
        var level = helper.getLevel();
        ExplosiveBlock anfo = Bombs.block(ExplosiveBlock.Kind.ANFO_CHARGE);
        BlockPos dry = new BlockPos(0, 1, 0), wet = new BlockPos(2, 1, 2);
        helper.setBlock(dry, anfo);
        helper.setBlock(dry.above(), Blocks.REDSTONE_BLOCK);
        helper.assertBlockPresent(anfo, dry);
        helper.assertTrue(lit(helper).isEmpty(), "ANFO took a signal with no booster");
        anfo.wasExploded(level, helper.absolutePos(dry), null);
        helper.assertTrue(lit(helper).size() == 1, "a booster's blast should set off dry ANFO");
        helper.setBlock(wet.north(), Blocks.WATER);
        anfo.wasExploded(level, helper.absolutePos(wet), null);
        helper.assertTrue(lit(helper).size() == 1, "wet ANFO went off");
        snuff(helper);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "bombs_fission", timeoutTicks = 400)
    public void aFissionBombHollowsItsCraterAndLeavesFallout(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int x = 0; x < 3; x++) for (int y = 0; y < 3; y++) for (int z = 0; z < 3; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.SAND);
        BlockPos centre = new BlockPos(1, 1, 1);
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 3, 1));
        Detonation.start(level, helper.absolutePos(centre), 2);
        helper.assertTrue(!pig.isAlive(), "the fireball should kill what stands in it");
        helper.assertTrue(Fallout.get(level).hot(helper.absolutePos(centre)), "the ground should be left hot");
        helper.succeedWhen(() -> {
            helper.assertTrue(!Detonation.active(), "still burning");
            helper.assertBlockPresent(Blocks.AIR, centre);
            helper.assertBlockPresent(Blocks.AIR, centre.above());
        });
    }

    @GameTest(template = "empty", batch = "bombs")
    public void theFissileMetalsComeFromTheFuelCycle(GameTestHelper helper) {
        var purex = (ProcessingRecipe<?, ?>) recipe(helper, "fundamentals:uses/purex");
        helper.assertTrue(purex.getRollableResults().stream().anyMatch(r -> r.getStack().is(stack("fundamentals:plutonium_dioxide").getItem())),
                "reprocessing should give the plutonium the fuel bred");
        var cascade = (ProcessingRecipe<?, ?>) recipe(helper, "fundamentals:bombs/highly_enriched_uranium_hexafluoride");
        helper.assertTrue(cascade.getFluidIngredients().get(0).test(new FluidStack(BuiltInRegistries.FLUID.get(Fundamentals.id("enriched_uranium_hexafluoride")), 1000)),
                "weapons uranium should come from reactor-grade uranium centrifuged again");
        var implosion = recipe(helper, "fundamentals:bombs/implosion_bomb");
        helper.assertTrue(implosion.getIngredients().stream().anyMatch(i -> i.test(stack("fundamentals:plutonium_pit")))
                        && implosion.getIngredients().stream().anyMatch(i -> i.test(stack("fundamentals:explosive_lens"))),
                "the implosion bomb should be a pit inside lenses");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "bombs")
    public void bismuthOnTheReactorTurnsToPolonium(GameTestHelper helper) {
        BlockPos controller = new BlockPos(1, 1, 1);
        helper.setBlock(controller.above(), Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(controller.above());
        chest.setItem(0, stack("fundamentals:bismuth_ingot").copyWithCount(2));
        ai.gsmc.fundamentals.content.nuclear.SpentFuel.discharge(helper.getLevel(), helper.absolutePos(controller));
        int irradiated = 0, bismuth = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).is(stack("fundamentals:irradiated_bismuth").getItem())) irradiated += chest.getItem(i).getCount();
            if (chest.getItem(i).is(stack("fundamentals:bismuth_ingot").getItem())) bismuth += chest.getItem(i).getCount();
        }
        helper.assertTrue(irradiated == 1 && bismuth == 1, "one slug a rod burnt: " + irradiated + " irradiated, " + bismuth + " left");
        helper.succeed();
    }
}
