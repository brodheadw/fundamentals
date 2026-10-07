package ai.gsmc.fundamentals.separation;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Solvent extraction: the reagent fluids and the mixer-settler they run through. */
public final class Separation {

    private static final Map<String, FluidType> FLUID_TYPES = new LinkedHashMap<>();
    private static final Map<String, Fluid> FLUIDS = new LinkedHashMap<>();
    private static Block mixerSettler;
    private static BlockEntityType<MixerSettlerBlockEntity> mixerSettlerEntity;
    private static Item mixerSettlerItem;
    private static Item salt;
    private static Item oxalicAcid;

    private Separation() {}

    public static Block mixerSettler() { return mixerSettler; }
    public static BlockEntityType<MixerSettlerBlockEntity> mixerSettlerEntity() { return mixerSettlerEntity; }
    public static Map<String, FluidType> fluidTypes() { return FLUID_TYPES; }

    public static Fluid fluid(String id) {
        Fluid fluid = FLUIDS.get(id);
        if (fluid == null) {
            throw new IllegalArgumentException("no reagent " + id);
        }
        return fluid;
    }

    public static List<Item> items() {
        return List.of(mixerSettlerItem, salt, oxalicAcid);
    }

    public static void registerFluidTypes(BiConsumer<ResourceLocation, FluidType> registry) {
        for (Reagents.Reagent reagent : Reagents.ALL) {
            // The organics float on the aqueous phase, which is the whole trick of the mixer-settler.
            FluidType type = new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type." + Fundamentals.MOD_ID + "." + reagent.id())
                    .density(reagent.kind() == Reagents.Kind.ORGANIC ? 800 : 1100)
                    .viscosity(reagent.kind() == Reagents.Kind.ORGANIC ? 1500 : 1000));
            FLUID_TYPES.put(reagent.id(), type);
            registry.accept(id(reagent.id()), type);
        }
    }

    public static void registerFluids(BiConsumer<ResourceLocation, Fluid> registry) {
        for (Reagents.Reagent reagent : Reagents.ALL) {
            // Never placed in the world, so the source stands in for its own flowing form.
            Fluid[] self = new Fluid[1];
            self[0] = new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(
                    () -> FLUID_TYPES.get(reagent.id()), () -> self[0], () -> self[0]));
            FLUIDS.put(reagent.id(), self[0]);
            registry.accept(id(reagent.id()), self[0]);
        }
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        mixerSettler = new MixerSettlerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                .requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion());
        registry.accept(id("mixer_settler"), mixerSettler);
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        mixerSettlerEntity = BlockEntityType.Builder.of(MixerSettlerBlockEntity::new, mixerSettler).build(null);
        registry.accept(id("mixer_settler"), mixerSettlerEntity);
    }

    public static void registerRecipeTypes(BiConsumer<ResourceLocation, RecipeType<?>> registry) {
        registry.accept(id("separation"), SeparationRecipe.TYPE);
    }

    public static void registerRecipeSerializers(BiConsumer<ResourceLocation, RecipeSerializer<?>> registry) {
        registry.accept(id("separation"), SeparationRecipe.SERIALIZER);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(id("mixer_settler"), mixerSettlerItem = new BlockItem(mixerSettler, new Item.Properties()));
        registry.accept(id("salt"), salt = new Item(new Item.Properties()));
        registry.accept(id("oxalic_acid"), oxalicAcid = new Item(new Item.Properties()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, path);
    }
}
