package ai.gsmc.fundamentals.registry;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.content.rare_earths.RareEarthMaterials;
import ai.gsmc.fundamentals.material.Material;
import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public final class MaterialItems {

    // Groups whose forms are items so far. The others wait on mapping forms to the items vanilla, Create and TFMG already have.
    private static final Set<String> GROUPS = Set.of(RareEarthMaterials.GROUP);

    private static final Map<ResourceLocation, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Item> ITEMS = new LinkedHashMap<>();

    private MaterialItems() {}

    public static ResourceLocation id(Material material, MaterialForm form) {
        boolean named = form == MaterialForm.CONCENTRATE && material.type() == MaterialType.CONCENTRATE;
        return ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, named ? material.id() : material.id() + "_" + form.id());
    }

    public static List<Material> materials() {
        return MaterialRegistry.all().stream().filter(m -> GROUPS.contains(m.group())).toList();
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        for (Material material : materials()) {
            if (material.has(MaterialForm.BLOCK)) {
                ResourceLocation id = id(material, MaterialForm.BLOCK);
                Block block = new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                        .strength(5.0F, 6.0F).sound(SoundType.METAL));
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
