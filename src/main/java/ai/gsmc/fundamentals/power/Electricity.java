package ai.gsmc.fundamentals.power;

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

public final class Electricity {

    private static final ResourceLocation SOLAR_PANEL = ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "solar_panel");

    private static Block solarPanel;
    private static BlockEntityType<SolarPanelBlockEntity> solarPanelEntity;
    private static Item solarPanelItem;

    private Electricity() {}

    public static Block solarPanel() { return solarPanel; }
    public static BlockEntityType<SolarPanelBlockEntity> solarPanelEntity() { return solarPanelEntity; }

    public static List<Item> items() {
        return List.of(solarPanelItem);
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        solarPanel = new SolarPanelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.LAPIS).requiresCorrectToolForDrops()
                .strength(1.5F, 3.0F).sound(SoundType.GLASS).noOcclusion());
        registry.accept(SOLAR_PANEL, solarPanel);
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        solarPanelEntity = BlockEntityType.Builder.of(SolarPanelBlockEntity::new, solarPanel).build(null);
        registry.accept(SOLAR_PANEL, solarPanelEntity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(SOLAR_PANEL, solarPanelItem = new BlockItem(solarPanel, new Item.Properties()));
    }
}
