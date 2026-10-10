package ai.gsmc.fundamentals.uses;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class Uses {

    public static final List<String> IDS = List.of(
            "phosphor", "didymium_glass", "roasted_cobaltite", "roasted_chalcopyrite", "rhenium_flue_dust", "tungsten_carbide",
            "tungsten_filament", "clarifier_sludge", "copper_calcine", "zinc_oxide", "roasted_pentlandite", "lithium_chloride",
            "ferroboron", "soda_ash", "sodium_chromate", "sodium_dichromate", "aluminium_powder", "roasted_tin_concentrate",
            "solder", "titania_slag", "magnesium_chloride", "silver_zinc_crust", "litharge", "thorium_nitrate", "gas_mantle",
            "mercury", "crude_zirconium_tetrachloride", "zirconium_tetrachloride", "hafnium_tetrachloride",
            "yttria_stabilised_zirconia", "beryl_frit", "beryllium_hydroxide", "ammonium_fluoroberyllate", "beryllium_pebbles",
            "dimensionally_stable_anode", "red_mud", "aluminium_hydroxide", "alumina", "cryolite", "nickel_oxide", "nickel_pellets",
            "tungstic_acid", "ammonium_paratungstate", "ammonium_perrhenate", "lithium_carbonate", "mcraly_powder", "boric_acid",
            "galvanized_steel_plate", "roasted_siderite");

    public static final List<String> PLATINUM_IDS = List.of(
            "ammonium_chloride", "insoluble_residue", "iridium_rhodium_residue", "ammonium_chloroplatinate",
            "dichlorodiammine_palladium", "ammonium_chlororuthenate", "ammonium_chloroiridate", "ammonium_chlororhodate",
            "reforming_catalyst", "platinum_rhodium_gauze", "osmium_filament");

    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private static Block sludgeBlock;

    private Uses() {}

    public static Item mercury() { return ITEMS.get("mercury"); }
    public static Item cryolite() { return ITEMS.get("cryolite"); }

    public static List<Item> items() {
        return List.copyOf(ITEMS.values());
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        registry.accept(Fundamentals.id("clarifier_sludge_block"), sludgeBlock = new Block(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT)
                .strength(0.8F).sound(SoundType.MUD)));
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        for (String id : IDS) {
            register(registry, id, new Item(new Item.Properties()));
        }
        register(registry, "clarifier_sludge_block", new BlockItem(sludgeBlock, new Item.Properties()));
        for (String id : PLATINUM_IDS) {
            register(registry, id, new Item(new Item.Properties()));
        }
    }

    private static void register(BiConsumer<ResourceLocation, Item> registry, String id, Item item) {
        ITEMS.put(id, item);
        registry.accept(Fundamentals.id(id), item);
    }
}
