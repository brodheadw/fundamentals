package ai.gsmc.fundamentals.uses;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * The few items the rare earths are spent on that are not a form of a material: the phosphor the lamps
 * take (europium red and terbium green on a yttria host) and the didymium glass that welders' goggles are
 * made of. The recipes are written by tools/build_uses_data.py.
 */
public final class Uses {

    private static Item phosphor;
    private static Item didymiumGlass;

    private Uses() {}

    public static List<Item> items() {
        return List.of(phosphor, didymiumGlass);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(id("phosphor"), phosphor = new Item(new Item.Properties()));
        registry.accept(id("didymium_glass"), didymiumGlass = new Item(new Item.Properties()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, path);
    }
}
