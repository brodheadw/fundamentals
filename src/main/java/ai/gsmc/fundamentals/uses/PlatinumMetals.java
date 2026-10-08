package ai.gsmc.fundamentals.uses;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The precious-metal refinery's items that are not a form of a material: the ammonium chloride it precipitates with, the
 * residues the aqua regia leaves, the yellow, red and black salts each metal is parted as, and what the metals are spent on.
 * The recipes and names are written by tools/build_uses_data.py, which lists the same ids.
 */
public final class PlatinumMetals {

    public static final List<String> IDS = List.of("ammonium_chloride", "insoluble_residue", "iridium_rhodium_residue",
            "ammonium_chloroplatinate", "dichlorodiammine_palladium", "ammonium_chlororuthenate", "ammonium_chloroiridate",
            "ammonium_chlororhodate", "reforming_catalyst", "platinum_rhodium_gauze", "osmium_filament");

    private static final List<Item> ITEMS = new ArrayList<>();

    private PlatinumMetals() {}

    public static List<Item> items() {
        return ITEMS;
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        for (String id : IDS) {
            Item item = new Item(new Item.Properties());
            ITEMS.add(item);
            registry.accept(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, id), item);
        }
    }
}
