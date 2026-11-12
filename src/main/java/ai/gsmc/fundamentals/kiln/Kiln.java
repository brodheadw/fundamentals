package ai.gsmc.fundamentals.kiln;

import ai.gsmc.fundamentals.Fundamentals;
import com.simibubi.create.api.stress.BlockStressValues;
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

/** The rotary kiln: drum lengths in a row, turned by a shaft and fired by a blaze burner under one end. */
public final class Kiln {

    private static Block drum;
    private static BlockEntityType<KilnDrumBlockEntity> drumEntity;
    private static Item drumItem, rawMeal;

    private Kiln() {}

    public static Block drum() { return drum; }
    public static BlockEntityType<KilnDrumBlockEntity> drumEntity() { return drumEntity; }
    public static List<Item> items() { return List.of(drumItem, rawMeal); }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        drum = new KilnDrumBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                .strength(5.0F, 6.0F).sound(SoundType.NETHERITE_BLOCK).noOcclusion());
        registry.accept(Fundamentals.id("kiln_drum"), drum);
    }

    public static void commonSetup() {
        // a drum full of rock is heavy to turn
        BlockStressValues.IMPACTS.register(drum, () -> 8.0);
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        drumEntity = BlockEntityType.Builder.of((pos, state) -> new KilnDrumBlockEntity(drumEntity, pos, state), drum).build(null);
        registry.accept(Fundamentals.id("kiln_drum"), drumEntity);
    }

    public static void registerRecipeTypes(BiConsumer<ResourceLocation, RecipeType<?>> registry) {
        registry.accept(Fundamentals.id("kiln"), KilnRecipe.TYPE);
    }

    public static void registerRecipeSerializers(BiConsumer<ResourceLocation, RecipeSerializer<?>> registry) {
        registry.accept(Fundamentals.id("kiln"), KilnRecipe.SERIALIZER);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(Fundamentals.id("kiln_drum"), drumItem = new BlockItem(drum, new Item.Properties()));
        registry.accept(Fundamentals.id("cement_raw_meal"), rawMeal = new Item(new Item.Properties()));
    }
}
