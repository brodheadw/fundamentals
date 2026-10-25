package ai.gsmc.fundamentals.cold;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class Parkas {

    public static final List<String> MATERIALS = List.of("deer_hide", "down", "waxed_canvas", "terephthalic_acid", "polyester_fibre",
            "nylon_ripstop", "eptfe_laminate", "sodium_silicate", "silica_hydrogel", "aerogel_blanket");
    private static final List<ArmorItem.Type> PIECES = List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS);
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private static final Map<Item, Parka> TIERS = new LinkedHashMap<>();

    private Parkas() {}

    public static List<Item> items() {
        return List.copyOf(ITEMS.values());
    }

    public static Parka tier(Item item) {
        return TIERS.get(item);
    }

    public static void registerArmorMaterials(BiConsumer<ResourceLocation, ArmorMaterial> registry) {
        for (Parka parka : Parka.values()) {
            // as light as leather: the warmth is insulation, not armour
            Map<ArmorItem.Type, Integer> defense = new EnumMap<>(Map.of(ArmorItem.Type.HELMET, 1, ArmorItem.Type.CHESTPLATE, 3,
                    ArmorItem.Type.LEGGINGS, 2, ArmorItem.Type.BOOTS, 1, ArmorItem.Type.BODY, 3));
            registry.accept(Fundamentals.id(parka.id), new ArmorMaterial(defense, 15, SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(ITEMS.get(parka.shell)), List.of(new ArmorMaterial.Layer(Fundamentals.id(parka.id))), 0, 0));
        }
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        for (String id : MATERIALS) {
            register(registry, id, new Item(new Item.Properties()));
        }
        for (Parka parka : Parka.values()) {
            Holder<ArmorMaterial> material = DeferredHolder.create(Registries.ARMOR_MATERIAL, Fundamentals.id(parka.id));
            for (ArmorItem.Type type : PIECES) {
                Item item = new ArmorItem(material, type, new Item.Properties().durability(type.getDurability(parka.durability)));
                TIERS.put(item, parka);
                register(registry, parka.piece(type), item);
            }
        }
    }

    private static void register(BiConsumer<ResourceLocation, Item> registry, String id, Item item) {
        ITEMS.put(id, item);
        registry.accept(Fundamentals.id(id), item);
    }
}
