package ai.gsmc.fundamentals.cold;

import ai.gsmc.fundamentals.heat.Thermometers;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

public final class Cold {

    // Cold Sweat's world unit: its Temperature.convert takes C to MC as celsius / 25, zero at zero
    public static final double CELSIUS_PER_UNIT = 25;

    private Cold() {}

    public static double toColdSweat(double celsius) {
        return celsius / CELSIUS_PER_UNIT;
    }

    public static boolean holdsThermometer(Player player) {
        return Thermometers.items().stream().anyMatch(item -> player.getOffhandItem().is(item)
                || player.getInventory().items.stream().limit(9).anyMatch(stack -> stack.is(item)));
    }

    public static void register() {
        if (ModList.get().isLoaded("cold_sweat")) {
            ColdSweatCompat.register();
        }
    }
}
