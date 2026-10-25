package ai.gsmc.fundamentals.registry;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.content.rare_earths.RareEarthMaterials;
import ai.gsmc.fundamentals.material.Material;
import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;
import ai.gsmc.fundamentals.oxidation.Weathering;
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
import java.util.Set;
import java.util.function.BiConsumer;

public final class MaterialItems {

    private static final Set<String> GROUPS = Set.of(RareEarthMaterials.GROUP);
    private static final Set<String> ITEM_IDS = Set.of("cobalt", "molybdenum", "rhenium", "superalloy", "molybdenum_steel", "chromel", "alumel", "alnico", "tungsten",
            "copper_matte", "blister_copper", "chromite", "wolframite", "scheelite", "chromium", "ferrochrome", "ferronickel", "stainless_steel", "ferromanganese", "manganese_steel",
            "nickel_matte", "converter_matte", "platinum_group_concentrate", "platinum", "palladium", "rhodium", "ruthenium", "iridium", "osmium",
            "tin_concentrate", "crude_tin", "tin", "bronze",
            "titanium", "magnesium", "zircon", "zirconium", "hafnium", "beryllium", "beryllium_copper",
            "lead_bullion", "silver", "uraninite", "zircaloy",
            "graphite", "sodium", "electrum", "hsla_steel", "pyrochlore", "columbite", "antimony", "antimonial_lead", "niobium", "ferroniobium", "tantalum",
            "germanium", "gallium", "indium", "cadmium", "garnet", "emery", "bismuth");

    private static final Map<ResourceLocation, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Item> ITEMS = new LinkedHashMap<>();

    private MaterialItems() {}

    public static ResourceLocation id(Material material, MaterialForm form) {
        boolean named = form == MaterialForm.CONCENTRATE && material.type() == MaterialType.CONCENTRATE;
        return Fundamentals.id(named ? material.id() : material.id() + "_" + form.id());
    }

    public static List<Material> materials() {
        return MaterialRegistry.all().stream().filter(m -> GROUPS.contains(m.group()) || ITEM_IDS.contains(m.id())).toList();
    }

    /**
     * How hard a block of the metal is to mine and to blow apart, against iron's 5 and 6: soft tin and silver give
     * sooner, beryllium copper (the strongest copper alloy) holds out, and the sintered magnets are hard but brittle.
     */
    private static final Map<String, float[]> TOUGHNESS = Map.ofEntries(
            Map.entry("tin", new float[]{3.0F, 4.0F}),
            Map.entry("silver", new float[]{4.0F, 6.0F}),
            Map.entry("aluminium_scandium", new float[]{4.0F, 6.0F}),
            Map.entry("bronze", new float[]{5.0F, 8.0F}),
            Map.entry("beryllium_copper", new float[]{8.0F, 14.0F}),
            Map.entry("praseodymium", new float[]{4.0F, 5.0F}),
            Map.entry("neodymium", new float[]{4.0F, 5.0F}),
            Map.entry("samarium", new float[]{4.0F, 5.0F}),
            Map.entry("terbium", new float[]{4.0F, 5.0F}),
            Map.entry("dysprosium", new float[]{4.0F, 5.0F}),
            Map.entry("neodymium_iron_boron", new float[]{7.0F, 4.0F}),
            Map.entry("dysprosium_neodymium_iron_boron", new float[]{7.0F, 4.0F}),
            Map.entry("samarium_cobalt", new float[]{7.0F, 4.0F}));

    public static BlockBehaviour.Properties metalBlock(String metal) {
        float[] toughness = TOUGHNESS.getOrDefault(metal, new float[]{5.0F, 6.0F});
        return BlockBehaviour.Properties.of().strength(toughness[0], toughness[1]);
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        for (Material material : materials()) {
            if (material.has(MaterialForm.BLOCK)) {
                ResourceLocation id = id(material, MaterialForm.BLOCK);
                Block block = Weathering.storageBlock(material.id());
                if (block == null) {
                    block = new Block(metalBlock(material.id()).mapColor(MapColor.METAL).requiresCorrectToolForDrops().sound(SoundType.METAL));
                }
                BLOCKS.put(id, block);
                registry.accept(id, block);
            }
        }
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        for (Material material : materials()) {
            for (MaterialForm form : material.forms()) {
                if (form == MaterialForm.ORE || form == MaterialForm.RAW) {
                    continue;
                }
                ResourceLocation id = id(material, form);
                Item item = form == MaterialForm.BLOCK ? new BlockItem(BLOCKS.get(id), new Item.Properties()) : new Item(new Item.Properties());
                ITEMS.put(id, item);
                registry.accept(id, item);
            }
        }
    }

    public static Map<ResourceLocation, Item> items() {
        return ITEMS;
    }
}
