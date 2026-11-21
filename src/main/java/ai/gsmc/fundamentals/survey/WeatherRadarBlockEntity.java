package ai.gsmc.fundamentals.survey;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

/**
 * A turning dish that sends out microwave pulses and times their echoes off raindrops and snowflakes, as weather radars have since
 * the war: how far off the precipitation is, which way, and how much of the sky around carries it. It sweeps sixteen bearings
 * out to 256 blocks; a thunderstorm returns the strongest echo.
 */
public class WeatherRadarBlockEntity extends SurveyBlockEntity {

    public static final int RANGE = 256, STEP = 32, BEARINGS = 16;

    private String echo = "clear", dryBiome = "";
    private double distance, dryDistance;
    private int bearing, dryBearing, cover;

    public WeatherRadarBlockEntity(BlockPos pos, BlockState state) {
        super(Survey.weatherRadarEntity(), pos, state);
    }

    @Override
    protected void read(ServerLevel level) {
        echo = "clear";
        dryBiome = "";
        cover = 0;
        distance = dryDistance = Double.MAX_VALUE;
        if (!level.isRaining()) return;
        // Minecraft's weather is one switch per dimension: an echo is any biome in range that rains or snows while it is on
        ping(level, 0, 0);
        for (int b = 0; b < BEARINGS; b++) {
            boolean wet = false;
            for (int r = STEP; r <= RANGE; r += STEP) wet |= ping(level, b, r);
            if (wet) cover++;
        }
    }

    /** One pulse out along a bearing: whether it came back off rain or snow at that range. */
    private boolean ping(ServerLevel level, int b, int r) {
        double angle = 2 * Math.PI * b / BEARINGS;
        BlockPos at = worldPosition.offset((int) Math.round(Math.sin(angle) * r), 0, (int) Math.round(-Math.cos(angle) * r));
        Holder<Biome> biome = level.getBiome(at);
        Biome.Precipitation falling = biome.value().getPrecipitationAt(at);
        if (falling == Biome.Precipitation.NONE) {
            if (r < dryDistance) {
                dryDistance = r;
                dryBearing = b;
                dryBiome = biome.unwrapKey().map(key -> key.location().toString()).orElse("");
            }
            return false;
        }
        if (r < distance) {
            distance = r;
            bearing = b;
            echo = falling == Biome.Precipitation.SNOW ? "snow" : level.isThundering() ? "storm" : "rain";
        }
        return true;
    }

    public String echo() { return echo; }
    public double distance() { return distance; }
    public int cover() { return cover; }

    /** Full for precipitation overhead, falling off to the edge of the sweep; nothing for clear skies. */
    @Override
    public double fraction() {
        return echo.equals("clear") ? 0 : Math.clamp(1 - distance / (RANGE + STEP), 1.0 / 15, 1);
    }

    private static String point(int b) {
        double angle = 2 * Math.PI * b / BEARINGS;
        return Survey.bearing(Math.sin(angle), -Math.cos(angle));
    }

    @Override
    protected void describe(List<Component> lines) {
        String key = "goggles.fundamentals.weather_radar.";
        if (echo.equals("clear")) {
            lines.add(Component.translatable(key + "clear", RANGE).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        Component kind = Component.translatable(key + echo);
        lines.add((distance == 0 ? Component.translatable(key + "overhead", kind)
                : Component.translatable(key + "echo", kind, String.format(Locale.ROOT, "%.0f", distance), point(bearing)))
                .withStyle(echo.equals("storm") ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        lines.add(Component.translatable(key + "cover", cover, BEARINGS).withStyle(ChatFormatting.GRAY));
        if (!dryBiome.isEmpty()) {
            lines.add(Component.translatable(key + "dry", Component.translatable(Util.makeDescriptionId("biome", ResourceLocation.parse(dryBiome))),
                    String.format(Locale.ROOT, "%.0f", dryDistance), point(dryBearing)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    protected void save(CompoundTag tag) {
        tag.putString("echo", echo);
        tag.putDouble("distance", distance);
        tag.putInt("bearing", bearing);
        tag.putInt("cover", cover);
        tag.putString("dry_biome", dryBiome);
        tag.putDouble("dry_distance", dryDistance);
        tag.putInt("dry_bearing", dryBearing);
    }

    @Override
    protected void load(CompoundTag tag) {
        echo = tag.contains("echo") ? tag.getString("echo") : "clear";
        distance = tag.getDouble("distance");
        bearing = tag.getInt("bearing");
        cover = tag.getInt("cover");
        dryBiome = tag.getString("dry_biome");
        dryDistance = tag.getDouble("dry_distance");
        dryBearing = tag.getInt("dry_bearing");
    }
}
