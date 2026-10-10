package ai.gsmc.fundamentals.separation;

import ai.gsmc.fundamentals.Fundamentals;
import com.simibubi.create.content.fluids.tank.FluidTankItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class Separation {

    private static final Map<String, FluidType> FLUID_TYPES = new LinkedHashMap<>();
    private static final Map<String, Fluid> FLUIDS = new LinkedHashMap<>();
    private static final Map<Fluid, Reagents.Kind> KINDS = new LinkedHashMap<>();
    private static final Map<Fluid, Integer> TINTS = new LinkedHashMap<>();
    private static final List<String> ITEM_IDS = List.of("salt", "calcium_ingot", "oxalic_acid", "roasted_bastnasite", "light_rare_earth_sulfate", "heavy_rare_earth_sulfate",
            "light_rare_earth_carbonate", "heavy_rare_earth_carbonate", "cerium_concentrate", "europium_sulfate", "calcium_chloride", "white_phosphorus");
    private static final List<Item> ITEMS = new ArrayList<>();
    private static Block mixerSettler;
    private static BlockEntityType<MixerSettlerBlockEntity> mixerSettlerEntity;
    private static Item mixerSettlerItem;
    private static Block magnetomigrationCell;
    private static BlockEntityType<MagnetomigrationCellBlockEntity> magnetomigrationCellEntity;
    private static Item magnetomigrationCellItem;
    private static Block plasticTank;
    private static BlockEntityType<PlasticTankBlockEntity> plasticTankEntity;
    private static Item plasticTankItem;
    private static Item seawaterBucket;

    private Separation() {}

    public static Block mixerSettler() { return mixerSettler; }
    public static BlockEntityType<MixerSettlerBlockEntity> mixerSettlerEntity() { return mixerSettlerEntity; }
    public static Block magnetomigrationCell() { return magnetomigrationCell; }
    public static BlockEntityType<MagnetomigrationCellBlockEntity> magnetomigrationCellEntity() { return magnetomigrationCellEntity; }
    public static Block plasticTank() { return plasticTank; }
    public static BlockEntityType<PlasticTankBlockEntity> plasticTankEntity() { return plasticTankEntity; }
    public static Map<String, FluidType> fluidTypes() { return FLUID_TYPES; }
    public static Item seawaterBucket() { return seawaterBucket; }

    public static Fluid fluid(String id) {
        Fluid fluid = FLUIDS.get(id);
        if (fluid == null) {
            throw new IllegalArgumentException("no reagent " + id);
        }
        return fluid;
    }

    @Nullable
    public static Reagents.Kind kind(Fluid fluid) {
        return KINDS.get(reagent(fluid));
    }

    @SuppressWarnings("deprecation")
    public static Fluid reagent(Fluid fluid) {
        if (KINDS.containsKey(fluid)) {
            return fluid;
        }
        return fluid.builtInRegistryHolder().tags().map(TagKey::location).filter(id -> id.getNamespace().equals(Fundamentals.MOD_ID))
                .map(id -> FLUIDS.get(id.getPath())).filter(Objects::nonNull).findFirst().orElse(fluid);
    }

    public static Fluid clarified(Fluid fluid) {
        String id = idOf(fluid);
        return id.startsWith("crude_") ? FLUIDS.getOrDefault(id.substring(6), fluid) : fluid;
    }

    public static Fluid fouled(Fluid organic) {
        return FLUIDS.getOrDefault("fouled_" + idOf(organic), organic);
    }

    private static String idOf(Fluid fluid) {
        return FLUIDS.entrySet().stream().filter(e -> e.getValue() == fluid).map(Map.Entry::getKey).findFirst().orElse("");
    }

    public static int tint(Fluid fluid) {
        return TINTS.getOrDefault(fluid, 0xFFFFFF);
    }

    public static List<Item> items() {
        List<Item> items = new ArrayList<>(List.of(mixerSettlerItem, magnetomigrationCellItem, plasticTankItem));
        items.addAll(ITEMS);
        Acids.all().values().forEach(acid -> items.add(acid.bucket));
        items.add(seawaterBucket);
        return items;
    }

    public static void registerFluidTypes(BiConsumer<ResourceLocation, FluidType> registry) {
        for (Reagents.Reagent reagent : Reagents.ALL) {
            String description = "fluid_type." + Fundamentals.MOD_ID + "." + reagent.id();
            FluidType type = reagent.kind() == Reagents.Kind.WATER ? Seawater.type(description) : new FluidType(FluidType.Properties.create()
                    .descriptionId(description)
                    .density(reagent.kind() == Reagents.Kind.ORGANIC ? 800 : 1100)
                    .viscosity(reagent.kind() == Reagents.Kind.ORGANIC ? 1500 : 1000));
            FLUID_TYPES.put(reagent.id(), type);
            registry.accept(Fundamentals.id(reagent.id()), type);
        }
    }

    public static void registerFluids(BiConsumer<ResourceLocation, Fluid> registry) {
        for (Reagents.Reagent reagent : Reagents.ALL) {
            if (reagent.kind() == Reagents.Kind.ACID) {
                Acids.Acid acid = Acids.all().get(reagent.id());
                FLUIDS.put(reagent.id(), acid.source);
                for (Fluid fluid : new Fluid[] {acid.source, acid.flowing}) {
                    KINDS.put(fluid, reagent.kind());
                    TINTS.put(fluid, reagent.tint());
                }
                registry.accept(Fundamentals.id(reagent.id()), acid.source);
                registry.accept(Fundamentals.id(reagent.id() + "_flowing"), acid.flowing);
                continue;
            }
            Fluid[] self = new Fluid[1];
            BaseFlowingFluid.Properties properties = new BaseFlowingFluid.Properties(() -> FLUID_TYPES.get(reagent.id()), () -> self[0], () -> self[0]);
            if (reagent.kind() == Reagents.Kind.WATER) {
                properties.bucket(() -> seawaterBucket);
            }
            self[0] = new BaseFlowingFluid.Source(properties);
            FLUIDS.put(reagent.id(), self[0]);
            KINDS.put(self[0], reagent.kind());
            TINTS.put(self[0], reagent.tint());
            registry.accept(Fundamentals.id(reagent.id()), self[0]);
        }
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        mixerSettler = new MixerSettlerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                .requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion());
        registry.accept(Fundamentals.id("mixer_settler"), mixerSettler);
        magnetomigrationCell = new MagnetomigrationCellBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
                .requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.STONE).noOcclusion());
        registry.accept(Fundamentals.id("magnetomigration_cell"), magnetomigrationCell);
        plasticTank = new PlasticTankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
                .requiresCorrectToolForDrops().strength(0.8F).sound(SoundType.STONE).noOcclusion()
                .isRedstoneConductor((state, level, pos) -> true));
        registry.accept(Fundamentals.id("plastic_fluid_tank"), plasticTank);
        for (Acids.Acid acid : Acids.all().values()) {
            registry.accept(Fundamentals.id(acid.id), acid.block);
        }
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        mixerSettlerEntity = BlockEntityType.Builder.of(MixerSettlerBlockEntity::new, mixerSettler).build(null);
        registry.accept(Fundamentals.id("mixer_settler"), mixerSettlerEntity);
        magnetomigrationCellEntity = BlockEntityType.Builder.of(MagnetomigrationCellBlockEntity::new, magnetomigrationCell).build(null);
        registry.accept(Fundamentals.id("magnetomigration_cell"), magnetomigrationCellEntity);
        plasticTankEntity = BlockEntityType.Builder.of(PlasticTankBlockEntity::new, plasticTank).build(null);
        registry.accept(Fundamentals.id("plastic_fluid_tank"), plasticTankEntity);
    }

    public static void registerRecipeTypes(BiConsumer<ResourceLocation, RecipeType<?>> registry) {
        registry.accept(Fundamentals.id("separation"), SeparationRecipe.TYPE);
        registry.accept(Fundamentals.id("magnetic"), MagneticRecipe.TYPE);
    }

    public static void registerRecipeSerializers(BiConsumer<ResourceLocation, RecipeSerializer<?>> registry) {
        registry.accept(Fundamentals.id("separation"), SeparationRecipe.SERIALIZER);
        registry.accept(Fundamentals.id("magnetic"), MagneticRecipe.SERIALIZER);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(Fundamentals.id("mixer_settler"), mixerSettlerItem = new BlockItem(mixerSettler, new Item.Properties()));
        registry.accept(Fundamentals.id("magnetomigration_cell"), magnetomigrationCellItem = new BlockItem(magnetomigrationCell, new Item.Properties()));
        registry.accept(Fundamentals.id("plastic_fluid_tank"), plasticTankItem = new FluidTankItem(plasticTank, new Item.Properties()));
        for (String id : ITEM_IDS) {
            Item item = new Item(new Item.Properties());
            ITEMS.add(item);
            registry.accept(Fundamentals.id(id), item);
        }
        for (Acids.Acid acid : Acids.all().values()) {
            registry.accept(Fundamentals.id(acid.id + "_bucket"), acid.bucket);
        }
        registry.accept(Fundamentals.id("seawater_bucket"), seawaterBucket = new Seawater.BucketOfSeawater(fluid("seawater"),
                new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    }

}
