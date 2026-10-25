package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.cold.Cold;
import ai.gsmc.fundamentals.cold.HeatTempModifier;
import ai.gsmc.fundamentals.heat.Thermometers;
import com.momosoftworks.coldsweat.api.temperature.modifier.BiomeTempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.BlockTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.temperature.PlayerTempCap;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The Cold Sweat half of ColdTests, apart so nothing loads its classes when it is not installed. Returns a failure, or null. */
final class ColdSweatChecks {

    private ColdSweatChecks() {}

    static String check(Player player) {
        for (double celsius : new double[] {-40, 0, 15, 37, 1150}) {
            double theirs = Temperature.convert(celsius, Temperature.Units.C, Temperature.Units.MC, true);
            if (Math.abs(Cold.toColdSweat(celsius) - theirs) > 1e-9) {
                return celsius + " °C: ours " + Cold.toColdSweat(celsius) + ", Cold Sweat's " + theirs;
            }
        }
        var world = EntityTempManager.gatherTempModifiers(player).get(Temperature.Trait.WORLD);
        if (world.stream().noneMatch(mod -> mod instanceof HeatTempModifier)
                || world.stream().anyMatch(mod -> mod instanceof BiomeTempModifier || mod instanceof BlockTempModifier)) {
            return "the world temperature should be ours, not Cold Sweat's biomes and blocks: " + world;
        }
        double units = Temperature.apply(0, player, Temperature.Trait.WORLD, new HeatTempModifier());
        double celsius = Heat.at(player.level(), player.blockPosition());
        if (Math.abs(units * Cold.CELSIUS_PER_UNIT - celsius) > 1e-6) {
            return "Cold Sweat should read " + celsius + " °C, not " + units * Cold.CELSIUS_PER_UNIT;
        }
        PlayerTempCap cap = new PlayerTempCap();
        player.getInventory().items.set(0, new ItemStack(Thermometers.items().getFirst()));
        cap.calculateHudVisibility(player);
        return cap.showWorldTemp ? null : "one of our thermometers in the hotbar should show Cold Sweat's world gauge";
    }
}
