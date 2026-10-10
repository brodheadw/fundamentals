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
            registry.accept(Fundamentals.id(kind.id), block);
        }
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        entity = BlockEntityType.Builder.of(ThermometerBlockEntity::new, BLOCKS.values().toArray(Block[]::new)).build(null);
        registry.accept(Fundamentals.id("thermometer"), entity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        for (Thermometer kind : Thermometer.values()) {
            Item item = new BlockItem(BLOCKS.get(kind), new Item.Properties());
            ITEMS.put(kind, item);
            registry.accept(Fundamentals.id(kind.id), item);
        }
    }

}
