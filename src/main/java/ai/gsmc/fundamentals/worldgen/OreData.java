package ai.gsmc.fundamentals.worldgen;

import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public record OreData(List<BlockDef> blocks, Map<String, String> hosts) {

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
