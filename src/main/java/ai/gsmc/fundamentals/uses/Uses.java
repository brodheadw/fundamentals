package ai.gsmc.fundamentals.uses;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * The few items of the chains that are not a form of a material: the phosphor the lamps take, the didymium
 * glass that welders' goggles are made of, the roasted sulfide ores on their way to cobalt and copper, and the
 * flue dust the molybdenite roaster gives up its rhenium in. The recipes are written by tools/build_uses_data.py.
 */
public final class Uses {

    private static Item phosphor;
    private static Item didymiumGlass;
    private static Item roastedCobaltite;
    private static Item roastedChalcopyrite;
    private static Item rheniumFlueDust;
    private static Item tungstenCarbide;
    private static Item tungstenFilament;

    private Uses() {}

    public static List<Item> items() {
        return List.of(phosphor, didymiumGlass, roastedCobaltite, roastedChalcopyrite, rheniumFlueDust, tungstenCarbide, tungstenFilament);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(id("phosphor"), phosphor = new Item(new Item.Properties()));
        registry.accept(id("didymium_glass"), didymiumGlass = new Item(new Item.Properties()));
        registry.accept(id("roasted_cobaltite"), roastedCobaltite = new Item(new Item.Properties()));
        registry.accept(id("roasted_chalcopyrite"), roastedChalcopyrite = new Item(new Item.Properties()));
        registry.accept(id("rhenium_flue_dust"), rheniumFlueDust = new Item(new Item.Properties()));
        registry.accept(id("tungsten_carbide"), tungstenCarbide = new Item(new Item.Properties()));
        registry.accept(id("tungsten_filament"), tungstenFilament = new Item(new Item.Properties()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, path);
    }
}
