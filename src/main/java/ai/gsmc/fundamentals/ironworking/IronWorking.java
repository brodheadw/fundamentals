package ai.gsmc.fundamentals.ironworking;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * The medieval route to iron (PLAN §2.4): ore and charcoal in a {@link BloomeryBlock} give an
 * iron bloom and slag; the bloom is hammered into wrought iron, which is the vanilla iron ingot.
 */
public final class IronWorking {

    private static Block bloomery;
    private static BlockEntityType<BloomeryBlockEntity> bloomeryEntity;
    private static Item bloomeryItem;
    private static Item ironBloom;
    private static Item slag;
    private static Item smithingHammer;

    private IronWorking() {}

    public static Block bloomery() { return bloomery; }
    public static BlockEntityType<BloomeryBlockEntity> bloomeryEntity() { return bloomeryEntity; }
    public static Item ironBloom() { return ironBloom; }
    public static Item slag() { return slag; }
    public static Item smithingHammer() { return smithingHammer; }

    public static List<Item> items() {
        return List.of(bloomeryItem, smithingHammer, ironBloom, slag);
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        bloomery = new BloomeryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE)
                .requiresCorrectToolForDrops().strength(2.0F, 4.0F).sound(SoundType.STONE)
                .lightLevel(state -> state.getValue(BloomeryBlock.LIT) ? 13 : 0));
        registry.accept(id("bloomery"), bloomery);
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        bloomeryEntity = BlockEntityType.Builder.of(BloomeryBlockEntity::new, bloomery).build(null);
        registry.accept(id("bloomery"), bloomeryEntity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(id("bloomery"), bloomeryItem = new BlockItem(bloomery, new Item.Properties()));
        registry.accept(id("smithing_hammer"), smithingHammer = new HammerItem(new Item.Properties().durability(96)));
        registry.accept(id("iron_bloom"), ironBloom = new Item(new Item.Properties()));
        registry.accept(id("slag"), slag = new Item(new Item.Properties()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, path);
    }
}
