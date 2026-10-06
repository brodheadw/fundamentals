package ai.gsmc.fundamentals.registry;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.BiConsumer;

public final class HandTools {

    private static Item mortarAndPestle;

    private HandTools() {}

    public static Item mortarAndPestle() { return mortarAndPestle; }

    public static List<Item> items() {
        return List.of(mortarAndPestle);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "mortar_and_pestle"),
                mortarAndPestle = new MortarItem(new Item.Properties().durability(128)));
    }
}
