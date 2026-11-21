package ai.gsmc.fundamentals.survey;

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

/** Instruments for what happens out of sight: the Geiger counter, the seismometer and the radio telescope. */
public final class Survey {

    private static Block seismometer, radioTelescope;
    private static BlockEntityType<SeismometerBlockEntity> seismometerEntity;
    private static BlockEntityType<RadioTelescopeBlockEntity> radioTelescopeEntity;
    private static Item geigerCounter, seismometerItem, radioTelescopeItem;

    private Survey() {}

    public static Block seismometer() { return seismometer; }
    public static Block radioTelescope() { return radioTelescope; }
    public static BlockEntityType<SeismometerBlockEntity> seismometerEntity() { return seismometerEntity; }
    public static BlockEntityType<RadioTelescopeBlockEntity> radioTelescopeEntity() { return radioTelescopeEntity; }
    public static List<Item> items() { return List.of(geigerCounter, seismometerItem, radioTelescopeItem); }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        seismometer = new SurveyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.COPPER).noOcclusion(),
                Survey::seismometerEntity, SeismometerBlockEntity::new, true);
        radioTelescope = new SurveyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion(),
                Survey::radioTelescopeEntity, RadioTelescopeBlockEntity::new, false);
        registry.accept(Fundamentals.id("seismometer"), seismometer);
        registry.accept(Fundamentals.id("radio_telescope"), radioTelescope);
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        seismometerEntity = BlockEntityType.Builder.of(SeismometerBlockEntity::new, seismometer).build(null);
        radioTelescopeEntity = BlockEntityType.Builder.of(RadioTelescopeBlockEntity::new, radioTelescope).build(null);
        registry.accept(Fundamentals.id("seismometer"), seismometerEntity);
        registry.accept(Fundamentals.id("radio_telescope"), radioTelescopeEntity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(Fundamentals.id("geiger_counter"), geigerCounter = new GeigerCounterItem(new Item.Properties().stacksTo(1)));
        registry.accept(Fundamentals.id("seismometer"), seismometerItem = new BlockItem(seismometer, new Item.Properties()));
        registry.accept(Fundamentals.id("radio_telescope"), radioTelescopeItem = new BlockItem(radioTelescope, new Item.Properties()));
    }
}
