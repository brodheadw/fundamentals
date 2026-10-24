package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.air.Barometer;
import ai.gsmc.fundamentals.air.BarometerBlockEntity;
import ai.gsmc.fundamentals.air.Barometers;
import ai.gsmc.fundamentals.computer.FundamentalsPeripherals;
import ai.gsmc.fundamentals.heat.Thermometer;
import ai.gsmc.fundamentals.heat.ThermometerBlock;
import ai.gsmc.fundamentals.heat.ThermometerBlockEntity;
import ai.gsmc.fundamentals.heat.Thermometers;
import ai.gsmc.fundamentals.power.Electricity;
import ai.gsmc.fundamentals.power.PanelRackBlock;
import ai.gsmc.fundamentals.power.SolarPanelBlockEntity;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.impl.Peripherals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class ComputerTests {

    private static List<String> methods(GameTestHelper helper, BlockPos pos) {
        IPeripheral peripheral = Peripherals.getGenericPeripheral(helper.getLevel(), helper.absolutePos(pos), Direction.UP,
                helper.getLevel().getBlockEntity(helper.absolutePos(pos)));
        helper.assertTrue(peripheral instanceof IDynamicPeripheral, "a computer should find a peripheral at " + pos);
        return List.of(((IDynamicPeripheral) peripheral).getMethodNames());
    }

    @GameTest(template = "empty")
    public void aComputerReadsTheGaugesAndTheGrid(GameTestHelper helper) {
        BlockPos thermometer = new BlockPos(1, 1, 1), barometer = new BlockPos(3, 1, 1), panel = new BlockPos(5, 1, 1);
        helper.setBlock(thermometer, Thermometers.block(Thermometer.MERCURY).defaultBlockState().setValue(ThermometerBlock.FACING, Direction.UP));
        helper.setBlock(barometer, Barometers.block(Barometer.MERCURY).defaultBlockState().setValue(ThermometerBlock.FACING, Direction.UP));
        helper.setBlock(panel, Electricity.solarPanel().defaultBlockState().setValue(PanelRackBlock.FACING, Direction.EAST));
        FundamentalsPeripherals source = new FundamentalsPeripherals();

        helper.assertTrue(methods(helper, thermometer).containsAll(List.of("getTemperature", "getKind", "getSignal")), "thermometer methods: " + methods(helper, thermometer));
        ThermometerBlockEntity glass = helper.getBlockEntity(thermometer);
        helper.assertTrue(source.getTemperature(glass) == glass.celsius() && source.getKind(glass).equals("mercury"), "the thermometer reads through");

        helper.assertTrue(methods(helper, barometer).containsAll(List.of("getPressure", "getTendency")), "barometer methods: " + methods(helper, barometer));
        BarometerBlockEntity gauge = helper.getBlockEntity(barometer);
        gauge.sample(helper.getLevel());
        helper.assertTrue(source.getPressure(gauge) > 900 && source.getPressure(gauge) < 1100, "a computer reads the air pressure: " + source.getPressure(gauge));

        helper.assertTrue(methods(helper, panel).containsAll(List.of("getVoltage", "getPowerGeneration", "getNetworkPowerUsage", "isPowered")),
                "a TFMG electrical block reads like a multimeter: " + methods(helper, panel));
        SolarPanelBlockEntity solar = helper.getBlockEntity(panel);
        helper.assertTrue(source.getPowerGeneration(solar) == solar.powerGeneration(), "the panel's output reads through");
        helper.succeed();
    }
}
