package ai.gsmc.fundamentals.heat;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** The four dial thermometers, one block each, sharing one block entity. Their models, recipes and names are written by tools/build_heat_data.py. */
public final class Thermometers {

    private static final Map<Thermometer, Block> BLOCKS = new EnumMap<>(Thermometer.class);
    private static final Map<Thermometer, Item> ITEMS = new EnumMap<>(Thermometer.class);
    private static BlockEntityType<ThermometerBlockEntity> entity;

    private Thermometers() {}

    public static Block block(Thermometer kind) { return BLOCKS.get(kind); }
    public static BlockEntityType<ThermometerBlockEntity> entity() { return entity; }

    public static List<Item> items() {
        return List.copyOf(ITEMS.values());
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        for (Thermometer kind : Thermometer.values()) {
            Block block = new ThermometerBlock(kind, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.0F).sound(SoundType.COPPER).noOcclusion());
            BLOCKS.put(kind, block);
            registry.accept(id(kind.id), block);
        }
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        entity = BlockEntityType.Builder.of(ThermometerBlockEntity::new, BLOCKS.values().toArray(Block[]::new)).build(null);
        registry.accept(id("thermometer"), entity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        for (Thermometer kind : Thermometer.values()) {
            Item item = new BlockItem(BLOCKS.get(kind), new Item.Properties());
            ITEMS.put(kind, item);
            registry.accept(id(kind.id), item);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, path);
    }
}
