package ai.gsmc.fundamentals.registry;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.material.Material;
import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;
import ai.gsmc.fundamentals.worldgen.OreData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * The in-world ore blocks ({@link OreBlock}) and the few host rocks that are blocks of their own,
 * as listed in {@link OreData}.
 *
 * <p>Loader-agnostic: the loader hands in a registration callback (see {@code Fundamentals}).
 */
public final class OreBlocks {

    /** Minerals whose ore is an existing vanilla block (PLAN §2.4); nothing of ours is registered. */
    public static final Map<String, String> VANILLA = Map.of(
            "native_gold", "minecraft:gold_ore",
            "native_copper", "minecraft:copper_ore");

    private static final Map<ResourceLocation, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Item> ITEMS = new LinkedHashMap<>();
    private static final List<Block> ORES = new ArrayList<>();

    private OreBlocks() {}

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        for (OreData.BlockDef def : OreData.get().blocks()) {
            BlockBehaviour.Properties props = def.soft()
                    ? BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.6F).sound(SoundType.GRAVEL)
                    : BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops()
                            .strength(def.mineral() == null ? 1.5F : 3.0F, 3.0F);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, def.name());
            Block block = def.mineral() == null ? new Block(props) : new OreBlock(props);
            BLOCKS.put(id, block);
            if (block instanceof OreBlock) {
                ORES.add(block);
            }
            registry.accept(id, block);
        }
        reportMismatches();
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        BLOCKS.forEach((id, block) -> {
            Item item = new BlockItem(block, new Item.Properties());
            ITEMS.put(id, item);
            registry.accept(id, item);
        });
    }

    public static Collection<Item> items() {
        return ITEMS.values();
    }

    /** The ore blocks: their models layer a transparent mineral texture over a rock, so they need cutout rendering. */
    public static List<Block> ores() {
        return ORES;
    }

    /** Ore blocks and mineral materials are defined in different places; say so when they disagree. */
    private static void reportMismatches() {
        Set<String> blocks = OreData.get().blocks().stream().map(OreData.BlockDef::mineral)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> minerals = MaterialRegistry.all().stream()
                .filter(m -> m.type() == MaterialType.MINERAL && m.has(MaterialForm.ORE))
                .map(Material::id).collect(Collectors.toSet());
        for (String mineral : minerals) {
            if (!blocks.contains(mineral) && !VANILLA.containsKey(mineral)) {
                Fundamentals.LOGGER.warn("Mineral '{}' has no ore block: add it to tools/build_ore_data.py", mineral);
            }
        }
        for (String block : blocks) {
            if (!minerals.contains(block)) {
                Fundamentals.LOGGER.warn("Ore block '{}_ore' has no mineral material yet", block);
            }
        }
        Map<String, String> hosts = OreData.get().hosts();
        for (OreBlock.Host host : OreBlock.Host.values()) {
            if (!host.block().equals(hosts.get(host.getSerializedName()))) {
                Fundamentals.LOGGER.error("Ore host '{}' differs between OreBlock.Host and tools/build_ore_data.py",
                        host.getSerializedName());
            }
        }
        if (hosts.size() != OreBlock.Host.values().length) {
            Fundamentals.LOGGER.error("Ore host lists differ between OreBlock.Host and tools/build_ore_data.py");
        }
    }
}
