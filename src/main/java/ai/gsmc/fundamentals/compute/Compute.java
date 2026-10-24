package ai.gsmc.fundamentals.compute;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** The fab: wafers to packaged chips on Create's machines, and the power supply CC: Tweaked's computers run from. */
public final class Compute {

    /** The four designs the fab makes, oldest process first. */
    public static final List<String> CHIPS = List.of("microcontroller", "memory_chip", "microprocessor", "advanced_processor");
    /** Fab chemistry, with the tint each fluid shows in pipes and tanks. */
    public static final Map<String, Integer> FLUID_TINTS = Map.of("negative_photoresist", 0xB8732E, "positive_photoresist", 0xD9A321, "chrome_etchant", 0xE8771A);
    private static final List<String> PARTS = List.of("fused_quartz", "novolac_resin", "epitaxial_wafer", "processing_wafer", "chrome_mask_blank",
            "quartz_mask_blank", "processing_mask", "packaging_chip", "lcd_panel", "colour_lcd_panel");
    /** One per item in production: things the fab carries between steps don't stack. */
    private static final List<String> SINGLES = List.of("processing_wafer", "processing_mask", "packaging_chip");

    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final Map<String, Item> MASK = new LinkedHashMap<>(), WAFER = new LinkedHashMap<>(), DIE = new LinkedHashMap<>(), CHIP = new LinkedHashMap<>();
    public static final Map<String, FluidType> FLUID_TYPE = new LinkedHashMap<>();
    public static final Map<String, Fluid> FLUID = new LinkedHashMap<>();

    private static PowerSupplyBlock powerSupply;
    private static BlockEntityType<PowerSupplyBlockEntity> powerSupplyEntity;
    private static Item powerSupplyItem;

    private Compute() {}

    public static PowerSupplyBlock powerSupply() { return powerSupply; }
    public static BlockEntityType<PowerSupplyBlockEntity> powerSupplyEntity() { return powerSupplyEntity; }

    public static List<Item> items() {
        List<Item> items = new ArrayList<>(List.of(powerSupplyItem));
        items.addAll(ITEMS.values());
        return items;
    }

    public static void registerFluidTypes(BiConsumer<ResourceLocation, FluidType> registry) {
        FLUID_TINTS.keySet().forEach(fluid -> {
            FluidType type = new FluidType(FluidType.Properties.create().descriptionId("fluid_type." + Fundamentals.MOD_ID + "." + fluid)
                    .density(1100).viscosity(fluid.endsWith("photoresist") ? 4000 : 1000));
            FLUID_TYPE.put(fluid, type);
            registry.accept(Fundamentals.id(fluid), type);
        });
    }

    public static void registerFluids(BiConsumer<ResourceLocation, Fluid> registry) {
        FLUID_TINTS.keySet().forEach(fluid -> {
            Fluid[] self = new Fluid[1];
            self[0] = new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(() -> FLUID_TYPE.get(fluid), () -> self[0], () -> self[0]));
            FLUID.put(fluid, self[0]);
            registry.accept(Fundamentals.id(fluid), self[0]);
        });
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        powerSupply = new PowerSupplyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                .strength(3.0F, 6.0F).sound(SoundType.NETHERITE_BLOCK));
        registry.accept(Fundamentals.id("power_supply"), powerSupply);
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        powerSupplyEntity = BlockEntityType.Builder.of(PowerSupplyBlockEntity::new, powerSupply).build(null);
        registry.accept(Fundamentals.id("power_supply"), powerSupplyEntity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(Fundamentals.id("power_supply"), powerSupplyItem = new BlockItem(powerSupply, new Item.Properties()));
        PARTS.forEach(part -> item(registry, part));
        for (String chip : CHIPS) {
            String design = chip.replace("_chip", "");
            MASK.put(chip, item(registry, design + "_mask"));
            WAFER.put(chip, item(registry, design + "_wafer"));
            DIE.put(chip, item(registry, design + "_die"));
            CHIP.put(chip, item(registry, chip));
        }
    }

    private static Item item(BiConsumer<ResourceLocation, Item> registry, String name) {
        int stack = name.endsWith("_mask") || SINGLES.contains(name) ? 1 : name.endsWith("_wafer") || name.endsWith("_blank") ? 16 : 64;
        Item item = new Part(new Item.Properties().stacksTo(stack));
        ITEMS.put(name, item);
        registry.accept(Fundamentals.id(name), item);
        return item;
    }
}
