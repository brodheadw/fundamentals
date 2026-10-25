package ai.gsmc.fundamentals.cold;

import ai.gsmc.fundamentals.heat.Thermometers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.List;
import java.util.function.BiConsumer;

public final class Cold {

    // Cold Sweat's world unit: its Temperature.convert takes C to MC as celsius / 25, zero at zero
    public static final double CELSIUS_PER_UNIT = 25;

    // the vacuum flask is Cold Sweat's waterskin insulated, so it exists only with Cold Sweat
    static Item flask, filledFlask, unfinishedFlask;

    private Cold() {}

    public static List<Item> flasks() {
        return flask == null ? List.of() : List.of(flask, filledFlask);
    }

    public static Item flask() {
        return flask;
    }

    public static boolean isFilledFlask(ItemStack stack) {
        return filledFlask != null && stack.is(filledFlask);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        if (ModList.get().isLoaded("cold_sweat")) {
            ColdSweatCompat.registerFlasks(registry);
        }
    }

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
