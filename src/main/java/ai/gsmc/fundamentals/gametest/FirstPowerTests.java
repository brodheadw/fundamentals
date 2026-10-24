package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.magnet.Magnets;
import com.drmangotea.tfmg.content.electricity.generators.GeneratorBlockEntity;
import com.drmangotea.tfmg.content.electricity.network.transformer.small.TransformerBlockEntity;
import com.drmangotea.tfmg.content.machinery.misc.winding_machine.WindingMachineBlockEntity;
import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import com.drmangotea.tfmg.content.machinery.vat.electrode_holder.ElectrodeHolderBlockEntity;
import com.drmangotea.tfmg.registry.TFMGBlocks;
import com.drmangotea.tfmg.registry.TFMGDataComponents;
import com.drmangotea.tfmg.registry.TFMGItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class FirstPowerTests {

    private static final BlockPos VAT = new BlockPos(16, 1, 1);

    private static ItemStack recipeResult(GameTestHelper helper, String id) {
        var recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id));
        helper.assertTrue(recipe.isPresent(), id + " did not load");
        return recipe.get().value().getResultItem(helper.getLevel().registryAccess()).copy();
    }

    private static CreativeMotorBlockEntity motor(GameTestHelper helper, BlockPos pos, Direction facing, int speed) {
        helper.setBlock(pos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, facing));
        CreativeMotorBlockEntity motor = helper.getBlockEntity(pos);
        motor.generatedSpeed.setValue(speed);
        return motor;
    }

    private static void hub(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, TFMGBlocks.COPPER_CABLE_HUB.getDefaultState());
    }

    private static TransformerBlockEntity transformer(GameTestHelper helper, BlockPos pos, ItemStack item) {
        helper.setBlock(pos, TFMGBlocks.TRANSFORMER.getDefaultState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        TransformerBlockEntity transformer = helper.getBlockEntity(pos);
        transformer.applyComponentsFromItemStack(item);
        return transformer;
    }

    private static VatBlockEntity vat(GameTestHelper helper) {
        VatBlockEntity part = helper.getBlockEntity(VAT);
        return part.getControllerBE();
    }

    private static int output(VatBlockEntity vat, String id) {
        var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
        var tanks = vat.outputTank.getCapability();
        int amount = 0;
        for (int i = 0; i < tanks.getTanks(); i++) {
            var contents = tanks.getFluidInTank(i);
            if (contents.getFluid().isSame(fluid)) {
                amount += contents.getAmount();
            }
        }
        return amount;
    }

    private static String readings(List<GeneratorBlockEntity> generators, List<ElectrodeHolderBlockEntity> holders) {
        return "generators " + generators.stream().map(g -> g.voltageGeneration() + " V/" + g.powerGeneration() + " W").toList()
                + ", source network " + generators.getFirst().getNetworkPowerGeneration() + " W"
                + ", electrodes " + holders.stream().map(h -> h.getData().getVoltage() + " V/" + h.getCurrent()
                + " A/" + h.getPowerUsage() + " W, powered=" + h.canWork()).toList();
    }

    @GameTest(template = "battery", timeoutTicks = 2400)
    public void steelGeneratorsAndIronCoreTransformersMakeTheFirstSilicon(GameTestHelper helper) {
        ItemStack generatorItem = recipeResult(helper, "fundamentals:uses/generator_with_steel_magnet");
        var charge = generatorItem.get(Magnets.CHARGE);
        helper.assertTrue(generatorItem.is(TFMGBlocks.GENERATOR.asItem()) && charge != null
                        && charge.grade().material.equals("steel"), "the starter generator recipe must preserve its steel magnet");
        ItemStack transformerItem = recipeResult(helper, "fundamentals:uses/transformer_with_iron_core");
        helper.assertTrue(transformerItem.is(TFMGBlocks.TRANSFORMER.asItem()), "the iron-core recipe must make a working transformer");
        ItemStack unfinishedCoil = recipeResult(helper, "fundamentals:uses/electromagnetic_coil_with_iron_core");
        helper.assertTrue(unfinishedCoil.is(TFMGItems.UNFINISHED_ELECTROMAGNETIC_COIL.get()), "iron cores must still need winding");
        ItemStack spool = recipeResult(helper, "tfmg:copper_spool");

        List<CreativeMotorBlockEntity> motors = new ArrayList<>();
        List<GeneratorBlockEntity> generators = new ArrayList<>();
        for (int x = 1; x <= 8; x++) {
            motors.add(motor(helper, new BlockPos(x, 2, 1), Direction.UP, 0));
            BlockPos pos = new BlockPos(x, 3, 1);
            helper.setBlock(pos, TFMGBlocks.GENERATOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
            GeneratorBlockEntity generator = helper.getBlockEntity(pos);
            generator.applyComponentsFromItemStack(generatorItem.copy());
            generators.add(generator);
        }
        for (int x = 9; x <= 12; x++) {
            hub(helper, new BlockPos(x, 3, 1));
        }
        for (int z = 2; z <= 3; z++) {
            for (int x = 12; x <= 11 + z; x++) {
                hub(helper, new BlockPos(x, 3, z));
            }
        }

        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(VAT.offset(x, 0, z), TFMGBlocks.FIREPROOF_CHEMICAL_VAT.getDefaultState());
            }
        }
        List<ElectrodeHolderBlockEntity> holders = new ArrayList<>();
        List<TransformerBlockEntity> firstStage = new ArrayList<>();
        List<TransformerBlockEntity> secondStage = new ArrayList<>();
        List<WindingMachineBlockEntity> winders = new ArrayList<>();
        for (int branch = 0; branch < 3; branch++) {
            BlockPos holderPos = VAT.offset(branch, 1, branch);
            helper.setBlock(holderPos, TFMGBlocks.ELECTRODE_HOLDER.getDefaultState());
            ElectrodeHolderBlockEntity holder = helper.getBlockEntity(holderPos);
            holder.setElectrode(TFMGItems.GRAPHITE_ELECTRODE.asStack(), false);
            holders.add(holder);
            BlockPos outputPos = holderPos.above();
            hub(helper, outputPos);
            secondStage.add(transformer(helper, outputPos.west(), transformerItem));
            hub(helper, outputPos.west(2));
            firstStage.add(transformer(helper, outputPos.west(3), transformerItem));

            BlockPos winderPos = new BlockPos(22 + branch * 2, 1, 2);
            motor(helper, winderPos.north(), Direction.SOUTH, 256);
            helper.setBlock(winderPos, TFMGBlocks.WINDING_MACHINE.getDefaultState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
            winders.add(helper.getBlockEntity(winderPos));
        }

        var sequence = helper.startSequence()
                .thenWaitUntil(() -> {
                    VatBlockEntity vat = vat(helper);
                    helper.assertTrue(vat != null && vat.getWidth() == 3 && vat.getHeight() == 1
                                    && vat.machineMap.size() == 3 && !vat.areMachinesValid,
                            "the vat must form naturally and find three unpowered electrodes");
                })
                .thenExecute(() -> {
                    VatBlockEntity vat = vat(helper);
                    helper.assertTrue(vat.inputInventory.insertItem(0, new ItemStack(Items.QUARTZ, 3), false).isEmpty(), "vat rejected quartz");
                    helper.assertTrue(vat.inputInventory.insertItem(1, new ItemStack(Items.CHARCOAL), false).isEmpty(), "vat rejected charcoal");
                });

        // Each branch gets real wound coils: 100:400 turns, then 100:500 turns.
        int[] turns = {100, 400, 100, 500};
        for (int job = 0; job < turns.length; job++) {
            int index = job;
            int target = turns[job];
            sequence.thenExecute(() -> {
                for (WindingMachineBlockEntity winder : winders) {
                    BlockEntityBehaviour.get(winder, ScrollValueBehaviour.TYPE).setValue(target / 10);
                    winder.spool = spool.copy();
                    helper.assertTrue(winder.inventory.insertItem(0, unfinishedCoil.copyWithCount(1), false).isEmpty(), "winder rejected an iron core");
                }
            }).thenWaitUntil(() -> {
                for (WindingMachineBlockEntity winder : winders) {
                    ItemStack coil = winder.inventory.getStackInSlot(0);
                    helper.assertTrue(coil.is(TFMGItems.ELECTROMAGNETIC_COIL.get())
                                    && coil.getOrDefault(TFMGDataComponents.COIL_TURNS, 0) == target,
                            "the kinetic winder should produce " + target + " turns, got "
                                    + coil.getOrDefault(TFMGDataComponents.COIL_TURNS, 0) + ", speed " + winder.getSpeed());
                }
            }).thenExecute(() -> {
                for (int branch = 0; branch < 3; branch++) {
                    ItemStack coil = winders.get(branch).inventory.extractItem(0, 1, false);
                    TransformerBlockEntity transformer = (index < 2 ? firstStage : secondStage).get(branch);
                    if (index % 2 == 0) {
                        transformer.primaryCoil = coil;
                    } else {
                        transformer.secondaryCoil = coil;
                    }
                    transformer.updateCoils();
                }
            });
        }

        sequence.thenExecute(() -> {
                    VatBlockEntity vat = vat(helper);
                    helper.assertTrue(output(vat, "tfmg:liquid_silicon") == 0 && vat.inputInventory.getStackInSlot(0).getCount() == 3
                                    && vat.inputInventory.getStackInSlot(1).getCount() == 1,
                            "winding coils without electrical power must not process quartz");
                    motors.forEach(motor -> motor.generatedSpeed.setValue(256));
                })
                .thenWaitUntil(() -> {
                    VatBlockEntity vat = vat(helper);
                    helper.assertTrue(generators.stream().allMatch(g -> g.powerGeneration() > 0)
                                    && holders.stream().allMatch(h -> h.getCurrent() >= 5 && h.canWork() && h.canOperate(vat)),
                            "the steel generators must power all three real electrode circuits: " + readings(generators, holders));
                })
                .thenWaitUntil(() -> helper.assertTrue(output(vat(helper), "tfmg:liquid_silicon") == 144
                                && output(vat(helper), "tfmg:carbon_dioxide") == 250,
                        "the powered vat must finish one silicon batch: " + readings(generators, holders)))
                .thenExecute(() -> {
                    VatBlockEntity vat = vat(helper);
                    helper.assertTrue(vat.inputInventory.getStackInSlot(0).isEmpty() && vat.inputInventory.getStackInSlot(1).isEmpty(),
                            "making silicon must consume three quartz and one charcoal");
                    helper.assertTrue(holders.stream().allMatch(h -> h.getCurrent() >= 5 && h.canWork()),
                            "power must remain sufficient through completion: " + readings(generators, holders));
                    Fundamentals.LOGGER.info("First power verified: 144 mB silicon, 250 mB carbon dioxide; {}", readings(generators, holders));
                })
                .thenSucceed();
    }
}
