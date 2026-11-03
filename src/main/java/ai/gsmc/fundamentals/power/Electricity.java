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

    private static Block panelRack;
    private static Block solarPanel;
    private static BlockEntityType<SolarPanelBlockEntity> solarPanelEntity;
    private static Item panelRackItem;
    private static Item photovoltaicPanel;
    private static Block teslaCoil;
    private static BlockEntityType<TeslaCoilBlockEntity> teslaCoilEntity;
    private static Item teslaCoilItem;

    private Electricity() {}

    public static Block panelRack() { return panelRack; }
    public static Block solarPanel() { return solarPanel; }
    public static BlockEntityType<SolarPanelBlockEntity> solarPanelEntity() { return solarPanelEntity; }
    public static Item photovoltaicPanel() { return photovoltaicPanel; }
    public static Block teslaCoil() { return teslaCoil; }
    public static BlockEntityType<TeslaCoilBlockEntity> teslaCoilEntity() { return teslaCoilEntity; }

    public static List<Item> items() {
        return List.of(panelRackItem, photovoltaicPanel, teslaCoilItem);
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        BlockBehaviour.Properties rack = BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                .strength(2.0F, 4.0F).sound(SoundType.METAL).noOcclusion();
        registry.accept(Fundamentals.id("panel_rack"), panelRack = new PanelRackBlock(rack));
        registry.accept(Fundamentals.id("solar_panel"), solarPanel = new SolarPanelBlock(rack));
        registry.accept(Fundamentals.id("tesla_coil"), teslaCoil = new TeslaCoilBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                .requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()));
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        solarPanelEntity = BlockEntityType.Builder.of(SolarPanelBlockEntity::new, solarPanel).build(null);
        registry.accept(Fundamentals.id("solar_panel"), solarPanelEntity);
        teslaCoilEntity = BlockEntityType.Builder.of(TeslaCoilBlockEntity::new, teslaCoil).build(null);
        registry.accept(Fundamentals.id("tesla_coil"), teslaCoilEntity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(Fundamentals.id("panel_rack"), panelRackItem = new BlockItem(panelRack, new Item.Properties()));
        registry.accept(Fundamentals.id("photovoltaic_panel"), photovoltaicPanel = new Item(new Item.Properties()));
        registry.accept(Fundamentals.id("tesla_coil"), teslaCoilItem = new BlockItem(teslaCoil, new Item.Properties()));
    }

}
