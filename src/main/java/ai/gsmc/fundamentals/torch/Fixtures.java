package ai.gsmc.fundamentals.torch;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Iron's lantern, soul lantern, chain and bars in the other metals they were made of: copper, brass, bronze, tinplate, silver, gold and steel. */
public final class Fixtures {

    public static final List<String> METALS = List.of("copper", "brass", "bronze", "tin", "silver", "gold", "steel");
    private static final Map<String, Function<BlockBehaviour.Properties, Block>> KINDS = Map.of(
            "lantern", LanternBlock::new, "soul_lantern", LanternBlock::new, "chain", ChainBlock::new, "bars", IronBarsBlock::new);
    private static final Map<String, Block> LIKE = Map.of(
            "lantern", Blocks.LANTERN, "soul_lantern", Blocks.SOUL_LANTERN, "chain", Blocks.CHAIN, "bars", Blocks.IRON_BARS);
    public static final List<String> KIND_ORDER = List.of("lantern", "soul_lantern", "chain", "bars");

    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final List<Item> ITEMS = new ArrayList<>();

    private Fixtures() {}

    public static Block block(String id) { return BLOCKS.get(id); }
    public static List<Item> items() { return ITEMS; }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        for (String metal : METALS) {
            for (String kind : KIND_ORDER) {
                Block block = KINDS.get(kind).apply(BlockBehaviour.Properties.ofFullCopy(LIKE.get(kind)));
                BLOCKS.put(metal + "_" + kind, block);
                registry.accept(Fundamentals.id(metal + "_" + kind), block);
            }
        }
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        BLOCKS.forEach((id, block) -> {
            Item item = new BlockItem(block, new Item.Properties());
            ITEMS.add(item);
            registry.accept(Fundamentals.id(id), item);
        });
    }
}
