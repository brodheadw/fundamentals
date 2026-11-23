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
    private static Block fluorescentLight, fluorescentTroffer, sodiumLamp;
    private static BlockEntityType<LampBlockEntity> lampEntity;
    private static Item fluorescentLightItem, fluorescentTrofferItem, sodiumLampItem, fluorescentTube, ballast, sodiumArcTube;

    private Electricity() {}

    public static Block panelRack() { return panelRack; }
    public static Block solarPanel() { return solarPanel; }
    public static BlockEntityType<SolarPanelBlockEntity> solarPanelEntity() { return solarPanelEntity; }
    public static Item photovoltaicPanel() { return photovoltaicPanel; }
    public static Block teslaCoil() { return teslaCoil; }
    public static BlockEntityType<TeslaCoilBlockEntity> teslaCoilEntity() { return teslaCoilEntity; }
    public static Block fluorescentLight() { return fluorescentLight; }
    public static Block fluorescentTroffer() { return fluorescentTroffer; }
    public static Block sodiumLamp() { return sodiumLamp; }
    public static BlockEntityType<LampBlockEntity> lampEntity() { return lampEntity; }

    public static List<Item> items() {
        return List.of(panelRackItem, photovoltaicPanel, teslaCoilItem, fluorescentTube, ballast, fluorescentLightItem, fluorescentTrofferItem, sodiumArcTube, sodiumLampItem);
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        BlockBehaviour.Properties rack = BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                .strength(2.0F, 4.0F).sound(SoundType.METAL).noOcclusion();
        registry.accept(Fundamentals.id("panel_rack"), panelRack = new PanelRackBlock(rack));
        registry.accept(Fundamentals.id("solar_panel"), solarPanel = new SolarPanelBlock(rack));
        registry.accept(Fundamentals.id("tesla_coil"), teslaCoil = new TeslaCoilBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                .requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()));
        BlockBehaviour.Properties lamp = BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                .strength(1.0F, 3.0F).sound(SoundType.LANTERN).noOcclusion();
        registry.accept(Fundamentals.id("fluorescent_light"), fluorescentLight = new LampBlock(lamp, LampBlock.Kind.BATTEN));
        registry.accept(Fundamentals.id("fluorescent_troffer"), fluorescentTroffer = new LampBlock(lamp, LampBlock.Kind.TROFFER));
        registry.accept(Fundamentals.id("sodium_lamp"), sodiumLamp = new LampBlock(lamp, LampBlock.Kind.SODIUM));
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        solarPanelEntity = BlockEntityType.Builder.of(SolarPanelBlockEntity::new, solarPanel).build(null);
        registry.accept(Fundamentals.id("solar_panel"), solarPanelEntity);
        teslaCoilEntity = BlockEntityType.Builder.of(TeslaCoilBlockEntity::new, teslaCoil).build(null);
        registry.accept(Fundamentals.id("tesla_coil"), teslaCoilEntity);
        lampEntity = BlockEntityType.Builder.of(LampBlockEntity::new, fluorescentLight, fluorescentTroffer, sodiumLamp).build(null);
        registry.accept(Fundamentals.id("lamp"), lampEntity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(Fundamentals.id("panel_rack"), panelRackItem = new BlockItem(panelRack, new Item.Properties()));
        registry.accept(Fundamentals.id("photovoltaic_panel"), photovoltaicPanel = new Item(new Item.Properties()));
        registry.accept(Fundamentals.id("tesla_coil"), teslaCoilItem = new BlockItem(teslaCoil, new Item.Properties()));
        registry.accept(Fundamentals.id("fluorescent_tube"), fluorescentTube = new Item(new Item.Properties()));
        registry.accept(Fundamentals.id("ballast"), ballast = new Item(new Item.Properties()));
        registry.accept(Fundamentals.id("sodium_arc_tube"), sodiumArcTube = new Item(new Item.Properties()));
        registry.accept(Fundamentals.id("fluorescent_light"), fluorescentLightItem = new BlockItem(fluorescentLight, new Item.Properties()));
        registry.accept(Fundamentals.id("fluorescent_troffer"), fluorescentTrofferItem = new BlockItem(fluorescentTroffer, new Item.Properties()));
        registry.accept(Fundamentals.id("sodium_lamp"), sodiumLampItem = new BlockItem(sodiumLamp, new Item.Properties()));
    }

}
