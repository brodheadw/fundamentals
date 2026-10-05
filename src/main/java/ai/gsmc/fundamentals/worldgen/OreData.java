package ai.gsmc.fundamentals.worldgen;

import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * The block list written by {@code tools/build_ore_data.py}: which ore and host-rock blocks
 * exist and which rocks an ore can sit in. The same script writes their models, loot and spawn
 * rules, so a block exists exactly when those do.
 */
public record OreData(List<BlockDef> blocks, Map<String, String> hosts) {

    /**
     * @param soft    dug with a shovel (clays, laterites, mineral sands) rather than a pickaxe
     * @param mineral the mineral material this is the ore of; null for a plain host rock
     */
    public record BlockDef(String name, boolean soft, String mineral) {}

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
