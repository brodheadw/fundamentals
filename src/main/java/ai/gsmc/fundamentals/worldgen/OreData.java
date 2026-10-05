package ai.gsmc.fundamentals.worldgen;

import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The block and spawn list written by {@code tools/build_ore_data.py}: which ore and host-rock
 * blocks exist and which deposits generate where. One file drives block registration and
 * spawning, so the two cannot drift apart.
 */
public record OreData(List<BlockDef> blocks, List<Spawn> add, List<String> remove) {

    /**
     * @param soft    dug with a shovel (clays, laterites, mineral sands) rather than a pickaxe
     * @param overlay drawn as a transparent layer over a vanilla block's texture
     * @param mineral the mineral material this is the ore of; null for a plain host rock
     */
    public record BlockDef(String name, boolean soft, boolean overlay, String mineral) {}

    /** @param biomes biome tag id; @param features placed-feature ids added to those biomes */
    public record Spawn(String biomes, List<String> features) {}

    private static OreData loaded;

    public static OreData get() {
        if (loaded == null) {
            try (Reader in = new InputStreamReader(
                    OreData.class.getResourceAsStream("/fundamentals_ores.json"), StandardCharsets.UTF_8)) {
                loaded = new Gson().fromJson(in, OreData.class);
            } catch (Exception e) {
                throw new IllegalStateException("Fundamentals: cannot read fundamentals_ores.json", e);
            }
        }
        return loaded;
    }
}
