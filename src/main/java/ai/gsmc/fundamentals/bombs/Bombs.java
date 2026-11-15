package ai.gsmc.fundamentals.bombs;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.world.level.block.Block;

/**
 * Explosives from black powder to the fission bomb: the charges and bombs as blocks, and what they are made of as items. The
 * recipes are hand-written under data/fundamentals/recipe/bombs.
 */
public final class Bombs {

    public static final List<String> IDS = List.of(
            "dynamite_stick", "blasting_cap", "trinitrotoluene", "rdx", "composition_b", "plastic_explosive", "explosive_lens",
            "bridgewire_detonator", "highly_enriched_uranium", "plutonium_dioxide", "plutonium", "plutonium_pit",
            "irradiated_bismuth", "polonium", "urchin_initiator");

    private static final Map<ExplosiveBlock.Kind, ExplosiveBlock> BLOCKS = new EnumMap<>(ExplosiveBlock.Kind.class);
    private static final List<Item> ITEMS = new ArrayList<>();
    private static EntityType<Charge> charge;

    private Bombs() {}

    public static EntityType<Charge> charge() { return charge; }
    public static ExplosiveBlock block(ExplosiveBlock.Kind kind) { return BLOCKS.get(kind); }
    public static List<Item> items() { return List.copyOf(ITEMS); }

    static String id(ExplosiveBlock.Kind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        for (ExplosiveBlock.Kind kind : ExplosiveBlock.Kind.values()) {
            BlockBehaviour.Properties properties = kind.nuclear()
                    ? BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5F, 6F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops()
                    : BlockBehaviour.Properties.of().mapColor(kind == ExplosiveBlock.Kind.POWDER_KEG ? MapColor.WOOD : MapColor.SAND)
                            .instabreak().sound(kind == ExplosiveBlock.Kind.POWDER_KEG ? SoundType.WOOD : SoundType.GRASS).ignitedByLava();
            if (kind == ExplosiveBlock.Kind.C4_CHARGE) properties = properties.noOcclusion().noCollission();
            if (kind.nuclear()) properties = properties.noOcclusion();
            ExplosiveBlock block = new ExplosiveBlock(kind, properties);
            BLOCKS.put(kind, block);
            registry.accept(Fundamentals.id(id(kind)), block);
        }
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        for (String id : IDS) {
            Item item = new Item(new Item.Properties());
            ITEMS.add(item);
            registry.accept(Fundamentals.id(id), item);
        }
        BLOCKS.forEach((kind, block) -> {
            Item item = new BlockItem(block, new Item.Properties().stacksTo(kind.nuclear() ? 1 : 64).rarity(kind.nuclear() ? Rarity.EPIC : Rarity.COMMON));
            ITEMS.add(item);
            registry.accept(Fundamentals.id(id(kind)), item);
        });
    }

    public static void registerEntities(BiConsumer<ResourceLocation, EntityType<?>> registry) {
        charge = EntityType.Builder.<Charge>of(Charge::new, MobCategory.MISC).fireImmune().sized(0.98F, 0.98F).eyeHeight(0.15F)
                .clientTrackingRange(10).updateInterval(10).build(Fundamentals.id("charge").toString());
        registry.accept(Fundamentals.id("charge"), charge);
    }
}
