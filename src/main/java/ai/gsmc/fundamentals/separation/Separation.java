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

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Solvent extraction: the reagent fluids and the mixer-settler they run through. */
public final class Separation {

    private static final Map<String, FluidType> FLUID_TYPES = new LinkedHashMap<>();
    private static final Map<String, Fluid> FLUIDS = new LinkedHashMap<>();
    private static final Map<Fluid, Reagents.Kind> KINDS = new LinkedHashMap<>();
    private static final Map<Fluid, Integer> TINTS = new LinkedHashMap<>();
    private static Block mixerSettler;
    private static BlockEntityType<MixerSettlerBlockEntity> mixerSettlerEntity;
    private static Item mixerSettlerItem;
    private static Item salt;
    private static Item calciumIngot;
    private static Item oxalicAcid;
    private static Item roastedBastnasite;
    private static Item lightRareEarthSulfate;
    private static Item heavyRareEarthSulfate;
    private static Item calciumChloride;
    private static Item whitePhosphorus;

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

    /** What a fluid is to the separation line, or null for anything that is not a reagent. */
    @Nullable
    public static Reagents.Kind kind(Fluid fluid) {
        return KINDS.get(fluid);
    }

    /** The clean liquor a crude one clarifies to, or the fluid itself if it is not crude. */
    public static Fluid clarified(Fluid fluid) {
        String id = FLUIDS.entrySet().stream().filter(e -> e.getValue() == fluid).map(Map.Entry::getKey).findFirst().orElse("");
        return id.startsWith("crude_") ? FLUIDS.getOrDefault(id.substring(6), fluid) : fluid;
    }

    /** The fouled form of an organic. */
    public static Fluid fouled(Fluid organic) {
        String id = FLUIDS.entrySet().stream().filter(e -> e.getValue() == organic).map(Map.Entry::getKey).findFirst().orElse("");
        return FLUIDS.getOrDefault("fouled_" + id, organic);
    }

    /** The reagent's colour as the table gives it, or Create's white for anything else. */
    public static int tint(Fluid fluid) {
        return TINTS.getOrDefault(fluid, 0xFFFFFF);
    }

    public static List<Item> items() {
        List<Item> items = new java.util.ArrayList<>(List.of(mixerSettlerItem, salt, oxalicAcid, roastedBastnasite,
                lightRareEarthSulfate, heavyRareEarthSulfate, calciumChloride, calciumIngot, whitePhosphorus));
        Acids.all().values().forEach(acid -> items.add(acid.bucket));
        return items;
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
            if (reagent.kind() == Reagents.Kind.ACID) {
                // the acids live in the world too: source, flowing form, block and bucket, built in Acids
                Acids.Acid acid = Acids.all().get(reagent.id());
                FLUIDS.put(reagent.id(), acid.source);
                for (Fluid fluid : new Fluid[] {acid.source, acid.flowing}) {
                    KINDS.put(fluid, reagent.kind());
                    TINTS.put(fluid, reagent.tint());
                }
                registry.accept(id(reagent.id()), acid.source);
                registry.accept(id(reagent.id() + "_flowing"), acid.flowing);
                continue;
            }
            // Never placed in the world, so the source stands in for its own flowing form.
            Fluid[] self = new Fluid[1];
            self[0] = new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(
                    () -> FLUID_TYPES.get(reagent.id()), () -> self[0], () -> self[0]));
            FLUIDS.put(reagent.id(), self[0]);
            KINDS.put(self[0], reagent.kind());
            TINTS.put(self[0], reagent.tint());
            registry.accept(id(reagent.id()), self[0]);
        }
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        mixerSettler = new MixerSettlerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                .requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion());
        registry.accept(id("mixer_settler"), mixerSettler);
        for (Acids.Acid acid : Acids.all().values()) {
            registry.accept(id(acid.id), acid.block);
        }
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
        registry.accept(id("calcium_ingot"), calciumIngot = new Item(new Item.Properties()));
        registry.accept(id("oxalic_acid"), oxalicAcid = new Item(new Item.Properties()));
        registry.accept(id("roasted_bastnasite"), roastedBastnasite = new Item(new Item.Properties()));
        registry.accept(id("light_rare_earth_sulfate"), lightRareEarthSulfate = new Item(new Item.Properties()));
        registry.accept(id("heavy_rare_earth_sulfate"), heavyRareEarthSulfate = new Item(new Item.Properties()));
        registry.accept(id("calcium_chloride"), calciumChloride = new Item(new Item.Properties()));
        registry.accept(id("white_phosphorus"), whitePhosphorus = new Item(new Item.Properties()));
        for (Acids.Acid acid : Acids.all().values()) {
            registry.accept(id(acid.id + "_bucket"), acid.bucket);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, path);
    }
}
