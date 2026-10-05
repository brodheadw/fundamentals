package ai.gsmc.fundamentals.registry;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Hand tools for working materials before any machine exists. The mortar and pestle grinds by
 * hand what Create's millstone grinds by power: grain to flour, and coloured minerals to
 * pigment, the way painters' workshops did (malachite green, azurite blue, vermilion from
 * cinnabar, the ochres from iron minerals). Its recipes are plain shapeless ones in
 * {@code data/fundamentals/recipe/grinding/}.
 */
public final class HandTools {

    private static Item mortarAndPestle;

    private HandTools() {}

    public static Item mortarAndPestle() { return mortarAndPestle; }

    public static List<Item> items() {
        return List.of(mortarAndPestle);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "mortar_and_pestle"),
                mortarAndPestle = new HandToolItem(new Item.Properties().durability(128)));
    }
}
