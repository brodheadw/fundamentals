package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.magnet.MagnetBehaviour;
import ai.gsmc.fundamentals.magnet.MagnetCharge;
import ai.gsmc.fundamentals.magnet.MagnetGrade;
import ai.gsmc.fundamentals.magnet.Magnets;
import com.drmangotea.tfmg.content.electricity.generators.GeneratorBlockEntity;
import com.drmangotea.tfmg.registry.TFMGBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class MagnetTests {

    private static Recipe<?> recipe(GameTestHelper helper, String id) {
        var found = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id));
        helper.assertTrue(found.isPresent(), id + " did not load");
        return found.get().value();
    }

    private static boolean takes(Recipe<?> recipe, String id) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
        return recipe.getIngredients().stream().anyMatch(i -> i.test(stack));
    }

    @GameTest(template = "empty")
    public void eachAlloyPolarizesToItsOwnMagnetAndTheMachinesCarryTheGrade(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        for (MagnetGrade grade : MagnetGrade.values()) {
            Recipe<?> polarize = recipe(helper, "fundamentals:uses/" + grade.magnet() + "_from_ingot");
            helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.getKey(polarize.getType()).toString().equals("tfmg:polarizing")
                    && takes(polarize, "fundamentals:" + grade.material + "_ingot") && polarize.getResultItem(registries).is(Magnets.magnet(grade)),
                    grade.material + " should polarize to its own magnet");
            for (String machine : new String[] {"electric_motor", "generator", "stator", "electric_pump", "voltmeter"}) {
                Recipe<?> built = recipe(helper, "fundamentals:uses/" + machine + "_with_" + grade.magnet());
                MagnetCharge charge = built.getResultItem(registries).get(Magnets.CHARGE);
                helper.assertTrue(charge != null && charge.grade() == grade && charge.field() == 1, machine + " built with " + grade + " should carry it, got " + charge);
            }
        }
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("tfmg:polarizing/magnet")).isEmpty(), "the Factory's own magnet should no longer be made");
        helper.assertTrue(Magnets.grade(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfmg:magnet")))) == MagnetGrade.DY_NDFEB,
                "a Factory magnet already in a world should count as Dy-NdFeB");
        helper.succeed();
    }

    private static GeneratorBlockEntity generator(GameTestHelper helper, int x, MagnetGrade grade, boolean furnaces) {
        BlockPos motor = new BlockPos(x, 1, 1), at = new BlockPos(x, 1, 2);
        helper.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.SOUTH));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(motor)).generatedSpeed.setValue(256);
        helper.setBlock(at, TFMGBlocks.GENERATOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.NORTH));
        ItemStack item = new ItemStack(TFMGBlocks.GENERATOR.get());
        item.set(Magnets.CHARGE, new MagnetCharge(grade, 1));
        GeneratorBlockEntity generator = helper.getBlockEntity(at);
        generator.applyComponentsFromItemStack(item);
        if (furnaces) {
            for (BlockPos furnace : List.of(at.west(), at.east(), at.above())) {
                helper.setBlock(furnace, Blocks.BLAST_FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.LIT, true));
            }
        }
        return generator;
    }

    @GameTest(template = "battery", timeoutTicks = 400)
    public void anNdFeBGeneratorAmongBlastFurnacesDeratesAndAnSmCoOneDoesNot(GameTestHelper helper) {
        GeneratorBlockEntity hot = generator(helper, 3, MagnetGrade.NDFEB, true);
        GeneratorBlockEntity smco = generator(helper, 11, MagnetGrade.SMCO, true);
        GeneratorBlockEntity cold = generator(helper, 22, MagnetGrade.NDFEB, false);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(cold.generation() > 0, "the cold generator should be turning and generating"))
                .thenExecute(() -> List.of(hot, smco, cold).forEach(g -> BlockEntityBehaviour.get(g, MagnetBehaviour.TYPE).check()))
                .thenWaitUntil(() -> {
                    helper.assertTrue(hot.generation() < cold.generation(), "NdFeB between blast furnaces should derate, got " + hot.generation() + " vs " + cold.generation());
                    MagnetCharge cooked = hot.components().get(Magnets.CHARGE);
                    helper.assertTrue(cooked != null && cooked.field() < 1, "past 120 °C the NdFeB should lose field for good, got " + cooked);
                    helper.assertTrue(MagnetBehaviour.output(smco) == MagnetGrade.SMCO.strength && smco.generation() == (int) (cold.generation() * MagnetGrade.SMCO.strength),
                            "SmCo should shrug off the furnaces, got " + MagnetBehaviour.output(smco) + ", " + smco.generation() + " vs " + cold.generation());
                    List<ItemStack> drops = Block.getDrops(hot.getBlockState(), helper.getLevel(), hot.getBlockPos(), hot);
                    helper.assertTrue(drops.size() == 1 && cooked.equals(drops.getFirst().get(Magnets.CHARGE)), "the broken generator should drop with its cooked magnet, got " + drops);
                })
                .thenSucceed();
    }

}
