package ai.gsmc.fundamentals.ironworking;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.registry.HandToolItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.List;
import java.util.function.BiConsumer;

public final class IronWorking {

    private static Block bloomery;
    private static BlockEntityType<BloomeryBlockEntity> bloomeryEntity;
    private static Item bloomeryItem;
    private static Item ironBloom;
    private static Item roastedGalena;
    private static Item calcinedSpodumene;
    private static Item smithingHammer;

    private IronWorking() {}

    public static Block bloomery() { return bloomery; }
    public static BlockEntityType<BloomeryBlockEntity> bloomeryEntity() { return bloomeryEntity; }
    public static Item ironBloom() { return ironBloom; }
    public static Item roastedGalena() { return roastedGalena; }
    public static Item smithingHammer() { return smithingHammer; }

    public static List<Item> items() {
        return List.of(bloomeryItem, smithingHammer, ironBloom, roastedGalena, calcinedSpodumene);
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        bloomery = new BloomeryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE)
                .requiresCorrectToolForDrops().strength(2.0F, 4.0F).sound(SoundType.STONE)
                .lightLevel(state -> state.getValue(BloomeryBlock.LIT) ? 13 : 0));
        registry.accept(Fundamentals.id("bloomery"), bloomery);
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        bloomeryEntity = BlockEntityType.Builder.of(BloomeryBlockEntity::new, bloomery).build(null);
        registry.accept(Fundamentals.id("bloomery"), bloomeryEntity);
    }

    public static void registerRecipeTypes(BiConsumer<ResourceLocation, RecipeType<?>> registry) {
        registry.accept(Fundamentals.id("bloomery"), BloomeryRecipe.TYPE);
    }

    public static void registerRecipeSerializers(BiConsumer<ResourceLocation, RecipeSerializer<?>> registry) {
        registry.accept(Fundamentals.id("bloomery"), BloomeryRecipe.SERIALIZER);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(Fundamentals.id("bloomery"), bloomeryItem = new BlockItem(bloomery, new Item.Properties()));
        registry.accept(Fundamentals.id("smithing_hammer"), smithingHammer = new HandToolItem(new Item.Properties().durability(96)));
        registry.accept(Fundamentals.id("iron_bloom"), ironBloom = new Item(new Item.Properties()));
        registry.accept(Fundamentals.id("roasted_galena"), roastedGalena = new Item(new Item.Properties()));
        registry.accept(Fundamentals.id("calcined_spodumene"), calcinedSpodumene = new Item(new Item.Properties()));
    }

}
