package ai.gsmc.fundamentals.worldgen;

import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The ore list written by {@code tools/build_ore_data.py}: which ore blocks exist and where they
 * generate. One file drives block registration and spawning, so the two cannot drift apart.
 */
public record OreData(List<Ore> ores, List<Spawn> add, List<String> remove) {

    /** @param soft dug with a shovel (clays, laterites, mineral sands) rather than a pickaxe */
    public record Ore(String mineral, boolean soft) {}

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
