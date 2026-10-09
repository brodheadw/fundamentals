package ai.gsmc.fundamentals.oxidation;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The metal storage blocks that weather in place: bronze to a green patina and silver to black, both of which honeycomb waxes
 * (the lacquer on a bronze statue or a silver tray); and the rare earth metal blocks, which tarnish, corrode and crumble to
 * their oxide, and which no wax saves. Mirrored in tools/paint_oxidation.py, which paints the stages and writes their data.
 */
public final class Weathering {

    /** A weathering metal: the prefix of each stage's block, how fast it goes relative to copper, its response to dry and damp air. */
    public record Family(String metal, List<String> prefixes, double rate, double dry, double wet, boolean waxable, boolean crumbles) {}

    private static final List<String> PATINA = List.of("", "exposed_", "weathered_", "oxidized_");
    private static final List<String> TARNISH = List.of("", "dulled_", "tarnished_", "blackened_");
    private static final List<String> FLAKING = List.of("", "tarnished_", "corroded_", "crumbled_");

    public static final Map<String, Family> FAMILIES = Stream.of(
            new Family("bronze", PATINA, 0.5, 0.25, 2, true, false),
            new Family("silver", TARNISH, 0.4, 0.5, 1.5, true, false),
            new Family("praseodymium", FLAKING, 4, 0.25, 4, false, true),
            new Family("neodymium", FLAKING, 3, 0.25, 4, false, true),
            new Family("samarium", FLAKING, 1, 0.25, 4, false, true),
            new Family("terbium", FLAKING, 0.5, 0.25, 4, false, true),
            new Family("dysprosium", FLAKING, 0.5, 0.25, 4, false, true)
    ).collect(Collectors.toMap(Family::metal, f -> f, (a, b) -> a, java.util.LinkedHashMap::new));

    private static final List<Block> BLOCKS = new ArrayList<>();
    private static final List<Item> ITEMS = new ArrayList<>();

    private Weathering() {}

    public static List<Item> items() {
        return ITEMS;
    }

    private static BlockBehaviour.Properties properties(boolean crumbled) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                .strength(crumbled ? 2.0F : 5.0F, 6.0F).sound(crumbled ? SoundType.TUFF : SoundType.METAL);
    }

    /** The unweathered block of a metal that weathers, for MaterialItems to register under the storage block's own id, or null. */
    @Nullable
    public static Block storageBlock(String metal) {
        Family family = FAMILIES.get(metal);
        return family == null ? null : new WeatheringMetalBlock(WeatherState.UNAFFECTED, family, properties(false));
    }

    private static List<String> names(Family family) {
        List<String> names = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            names.add(family.prefixes().get(i) + family.metal() + "_block");
        }
        if (family.waxable()) {
            for (String prefix : family.prefixes()) {
                names.add("waxed_" + prefix + family.metal() + "_block");
            }
        }
        return names;
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        for (Family family : FAMILIES.values()) {
            List<String> names = names(family);
            for (int i = 0; i < names.size(); i++) {
                WeatherState age = i < 3 ? WeatherState.values()[i + 1] : null;
                Block block = age == null ? new Block(properties(false))
                        : new WeatheringMetalBlock(age, family, properties(family.crumbles() && age == WeatherState.OXIDIZED));
                BLOCKS.add(block);
                registry.accept(id(names.get(i)), block);
            }
        }
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        int i = 0;
        for (Family family : FAMILIES.values()) {
            for (String name : names(family)) {
                Item item = new BlockItem(BLOCKS.get(i++), new Item.Properties());
                ITEMS.add(item);
                registry.accept(id(name), item);
            }
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, path);
    }
}
