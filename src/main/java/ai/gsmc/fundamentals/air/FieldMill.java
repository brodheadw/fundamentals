package ai.gsmc.fundamentals.air;

import ai.gsmc.fundamentals.Fundamentals;
import com.wildspell.fundamental.api.air.AirField;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.BiConsumer;

/** The field mill and its scale: logarithmic, since the air's field runs from 130 V/m on a fair day to tens of kilovolts in a storm. */
public final class FieldMill {

    /** The dial's span in volts per metre: fair weather at the bottom, a violent dust storm at the top. */
    public static final double MIN = AirField.FAIR, MAX = 50000;
    /** A field this strong without thunder overhead can only be charged dust. */
    public static final double DUST_STORM = 20000;

    private static Block block;
    private static Item item;
    private static BlockEntityType<FieldMillBlockEntity> entity;

    private FieldMill() {}

    public static Block block() { return block; }
    public static Item item() { return item; }
    public static BlockEntityType<FieldMillBlockEntity> entity() { return entity; }

    public static double fraction(double voltsPerMetre) {
        return voltsPerMetre <= MIN ? 0 : Mth.clamp(Math.log(voltsPerMetre / MIN) / Math.log(MAX / MIN), 0, 1);
    }

    public static int signal(double voltsPerMetre) {
        return (int) Math.round(15 * fraction(voltsPerMetre));
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        block = new FieldMillBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.COPPER).noOcclusion());
        registry.accept(Fundamentals.id("field_mill"), block);
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        entity = BlockEntityType.Builder.of(FieldMillBlockEntity::new, block).build(null);
        registry.accept(Fundamentals.id("field_mill"), entity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(Fundamentals.id("field_mill"), item = new BlockItem(block, new Item.Properties()));
    }
}
