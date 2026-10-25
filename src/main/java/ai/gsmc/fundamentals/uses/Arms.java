package ai.gsmc.fundamentals.uses;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Propellants, primers, cartridge parts and gun parts; their recipes are written by tools/build_arms_data.py. */
public final class Arms {

    public static final List<String> IDS = List.of(
            "mill_cake", "press_cake", "nitrocellulose", "smokeless_powder", "mercury_fulminate", "primer",
            "cartridge_case", "jacketed_bullet", "barrel_blank", "gun_barrel", "lens");

    private static final List<Item> ITEMS = new ArrayList<>();

    private Arms() {}

    public static List<Item> items() {
        return List.copyOf(ITEMS);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        for (String id : IDS) {
            Item item = new Item(new Item.Properties());
            ITEMS.add(item);
            registry.accept(Fundamentals.id(id), item);
        }
    }
}
