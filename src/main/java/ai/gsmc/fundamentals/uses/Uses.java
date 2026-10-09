package ai.gsmc.fundamentals.uses;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * The few items of the chains that are not a form of a material: the phosphor the lamps take, the didymium
 * glass that welders' goggles are made of, the roasted ores on their way to cobalt, copper, zinc and nickel, the
 * flue dust the molybdenite roaster gives up its rhenium in, the lithium chloride lithium is won from, the ferroboron
 * the magnets take their boron as, the soda ash, sodium salts and aluminium powder of the road to chromium, the roasted tin
 * concentrate the bloomery smelts, the solder circuit boards are joined with, the titania slag ilmenite smelts to, and the
 * magnesium chloride magnesium is won from and the Kroll process gives back. The recipes
 * are written by tools/build_uses_data.py.
 */
public final class Uses {

    private static Item phosphor;
    private static Item didymiumGlass;
    private static Item roastedCobaltite;
    private static Item roastedChalcopyrite;
    private static Item rheniumFlueDust;
    private static Item tungstenCarbide;
    private static Item tungstenFilament;
    private static Item clarifierSludge;
    private static Item copperCalcine;
    private static Item zincOxide;
    private static Item roastedPentlandite;
    private static Item lithiumChloride;
    private static Item ferroboron;
    private static Item sodaAsh;
    private static Item sodiumChromate;
    private static Item sodiumDichromate;
    private static Item aluminiumPowder;
    private static Item roastedTinConcentrate;
    private static Item solder;
    private static Item titaniaSlag;
    private static Item magnesiumChloride;

    private Uses() {}

    public static List<Item> items() {
        return List.of(phosphor, didymiumGlass, roastedCobaltite, roastedChalcopyrite, rheniumFlueDust, tungstenCarbide, tungstenFilament, clarifierSludge,
                copperCalcine, zincOxide, roastedPentlandite, lithiumChloride, ferroboron, sodaAsh, sodiumChromate, sodiumDichromate,
                aluminiumPowder, roastedTinConcentrate, solder, titaniaSlag, magnesiumChloride);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(id("phosphor"), phosphor = new Item(new Item.Properties()));
        registry.accept(id("didymium_glass"), didymiumGlass = new Item(new Item.Properties()));
        registry.accept(id("roasted_cobaltite"), roastedCobaltite = new Item(new Item.Properties()));
        registry.accept(id("roasted_chalcopyrite"), roastedChalcopyrite = new Item(new Item.Properties()));
        registry.accept(id("rhenium_flue_dust"), rheniumFlueDust = new Item(new Item.Properties()));
        registry.accept(id("tungsten_carbide"), tungstenCarbide = new Item(new Item.Properties()));
        registry.accept(id("tungsten_filament"), tungstenFilament = new Item(new Item.Properties()));
        registry.accept(id("clarifier_sludge"), clarifierSludge = new Item(new Item.Properties()));
        registry.accept(id("copper_calcine"), copperCalcine = new Item(new Item.Properties()));
        registry.accept(id("zinc_oxide"), zincOxide = new Item(new Item.Properties()));
        registry.accept(id("roasted_pentlandite"), roastedPentlandite = new Item(new Item.Properties()));
        registry.accept(id("lithium_chloride"), lithiumChloride = new Item(new Item.Properties()));
        registry.accept(id("ferroboron"), ferroboron = new Item(new Item.Properties()));
        registry.accept(id("soda_ash"), sodaAsh = new Item(new Item.Properties()));
        registry.accept(id("sodium_chromate"), sodiumChromate = new Item(new Item.Properties()));
        registry.accept(id("sodium_dichromate"), sodiumDichromate = new Item(new Item.Properties()));
        registry.accept(id("aluminium_powder"), aluminiumPowder = new Item(new Item.Properties()));
        registry.accept(id("roasted_tin_concentrate"), roastedTinConcentrate = new Item(new Item.Properties()));
        registry.accept(id("solder"), solder = new Item(new Item.Properties()));
        registry.accept(id("titania_slag"), titaniaSlag = new Item(new Item.Properties()));
        registry.accept(id("magnesium_chloride"), magnesiumChloride = new Item(new Item.Properties()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, path);
    }
}
