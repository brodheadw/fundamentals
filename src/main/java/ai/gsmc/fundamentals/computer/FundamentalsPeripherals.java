package ai.gsmc.fundamentals.computer;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.air.BarometerBlockEntity;
import ai.gsmc.fundamentals.air.FieldMillBlockEntity;
import ai.gsmc.fundamentals.heat.ThermometerBlockEntity;
import ai.gsmc.fundamentals.ironworking.BloomeryBlockEntity;
import ai.gsmc.fundamentals.oxidation.InertDrumBlockEntity;
import ai.gsmc.fundamentals.separation.MagnetomigrationCellBlockEntity;
import ai.gsmc.fundamentals.separation.MixerSettlerBlockEntity;
import com.drmangotea.tfmg.content.electricity.base.IElectric;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Map;

/** What a CC:Tweaked computer reads from the blocks beside it. Only loaded when CC:Tweaked is installed. */
public final class FundamentalsPeripherals implements GenericPeripheral {

    public static void register() {
        ComputerCraftAPI.registerGenericSource(new FundamentalsPeripherals());
    }

    @Override
    public String id() {
        return Fundamentals.MOD_ID + ":peripherals";
    }

    private static Map<String, Object> fluid(FluidStack stack) {
        return Map.of("name", BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString(), "amount", stack.getAmount());
    }

    @LuaFunction(mainThread = true)
    public final double getTemperature(ThermometerBlockEntity thermometer) {
        return thermometer.celsius();
    }

    @LuaFunction(mainThread = true)
    public final String getKind(ThermometerBlockEntity thermometer) {
        return thermometer.kind().name().toLowerCase();
    }

    @LuaFunction(mainThread = true)
    public final int getSignal(ThermometerBlockEntity thermometer) {
        return thermometer.signal();
    }

    @LuaFunction(mainThread = true)
    public final double getPressure(BarometerBlockEntity barometer) {
        return barometer.hectopascals();
    }

    /** The air's electric field in volts per metre. */
    @LuaFunction(mainThread = true)
    public final double getField(FieldMillBlockEntity mill) {
        return mill.voltsPerMetre();
    }

    @LuaFunction(mainThread = true)
    public final int getSignal(FieldMillBlockEntity mill) {
        return mill.signal();
    }

    /** Hectopascals per in-game hour over the readings the barometer holds; falling ahead of rain. */
    @LuaFunction(mainThread = true)
    public final double getTendency(BarometerBlockEntity barometer) {
        return barometer.perHour();
    }

    @LuaFunction(mainThread = true)
    public final String getKind(BarometerBlockEntity barometer) {
        return barometer.kind().name().toLowerCase();
    }

    @LuaFunction(mainThread = true)
    public final int getSignal(BarometerBlockEntity barometer) {
        return barometer.signal();
    }

    @LuaFunction(mainThread = true)
    public final double getTemperature(MagnetomigrationCellBlockEntity cell) {
        return cell.celsius();
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getOrganic(MixerSettlerBlockEntity settler) {
        return fluid(settler.stage().organic());
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getAqueous(MixerSettlerBlockEntity settler) {
        return fluid(settler.stage().aqueous());
    }

    @LuaFunction(mainThread = true)
    public final int getPhaseCapacity(MixerSettlerBlockEntity settler) {
        return settler.stage().phaseCapacity();
    }

    @LuaFunction(mainThread = true)
    public final boolean isStirred(MixerSettlerBlockEntity settler) {
        return settler.isStirred();
    }

    @LuaFunction(mainThread = true)
    public final boolean isOverStirred(MixerSettlerBlockEntity settler) {
        return settler.isOverStirred();
    }

    @LuaFunction(mainThread = true)
    public final boolean isStage(MixerSettlerBlockEntity settler) {
        return settler.isStage();
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getGas(InertDrumBlockEntity drum) {
        return fluid(drum.gas());
    }

    @LuaFunction(mainThread = true)
    public final int getOre(BloomeryBlockEntity bloomery) {
        return bloomery.oreCount();
    }

    @LuaFunction(mainThread = true)
    public final int getCharcoal(BloomeryBlockEntity bloomery) {
        return bloomery.charcoalCount();
    }

    @LuaFunction(mainThread = true)
    public final boolean hasBloom(BloomeryBlockEntity bloomery) {
        return bloomery.hasProducts();
    }

    // Every TFMG electrical block, the solar panel among them: the grid readings a multimeter gives.
    @LuaFunction(mainThread = true)
    public final int getVoltage(IElectric electric) {
        return electric.getData().getVoltage();
    }

    @LuaFunction(mainThread = true)
    public final double getCurrent(IElectric electric) {
        return electric.getCurrent();
    }

    @LuaFunction(mainThread = true)
    public final int getPowerUsage(IElectric electric) {
        return electric.getPowerUsage();
    }

    @LuaFunction(mainThread = true)
    public final int getPowerGeneration(IElectric electric) {
        return electric.powerGeneration();
    }

    @LuaFunction(mainThread = true)
    public final int getNetworkPowerUsage(IElectric electric) {
        return electric.getNetworkPowerUsage();
    }

    @LuaFunction(mainThread = true)
    public final int getNetworkPowerGeneration(IElectric electric) {
        return electric.getNetworkPowerGeneration();
    }

    @LuaFunction(mainThread = true)
    public final boolean isPowered(IElectric electric) {
        return electric.canWork() && electric.getData().getVoltage() > 0;
    }
}
