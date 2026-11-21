package ai.gsmc.fundamentals.survey;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

/**
 * A coil hung on a spring round a magnet: the ground moves, the magnet with it, the coil lags, and the current it makes is the
 * ground's motion. One station cannot tell which way a quake came from, but the lag between its P and S waves gives how far, and
 * with that and the amplitude, Richter's magnitude.
 */
public class SeismometerBlockEntity extends SurveyBlockEntity {

    /** Shaking lasts this long at the station. */
    static final long SHAKING = 40;
    /** The smallest ground motion the coil makes out above the noise, as log10 of micrometres. */
    static final double NOISE = -1, FULL_SCALE = 5;

    private double shaking = NOISE, magnitude, distance;
    private long heard = -1;

    public SeismometerBlockEntity(BlockPos pos, BlockState state) {
        super(Survey.seismometerEntity(), pos, state);
    }

    /** Richter's local scale, with the amplitude falling as the square of distance past a hundred blocks. */
    static double amplitude(double magnitude, double distance) {
        return magnitude - 2 * Math.log10(Math.max(distance, 10) / 100);
    }

    @Override
    protected void read(ServerLevel level) {
        shaking = NOISE;
        for (Disturbances.Event quake : Disturbances.since(level, Disturbances.Kind.QUAKE, Disturbances.KEEP)) {
            double d = Math.sqrt(quake.pos().distSqr(worldPosition)), a = amplitude(quake.size(), d);
            if (a < NOISE) continue;
            if (level.getGameTime() - quake.time() <= SHAKING) shaking = Math.max(shaking, a);
            if (quake.time() >= heard) {
                heard = quake.time();
                magnitude = quake.size();
                distance = d;
            }
        }
    }

    public double shaking() { return shaking; }
    public double magnitude() { return magnitude; }
    public double distance() { return distance; }
    public long heard() { return heard; }

    @Override
    public double fraction() {
        return Math.clamp((shaking - NOISE) / (FULL_SCALE - NOISE), 0, 1);
    }

    @Override
    protected void describe(List<Component> lines) {
        String key = "goggles.fundamentals.seismometer.";
        lines.add(shaking > NOISE ? Component.translatable(key + "shaking", String.format(Locale.ROOT, "%,.1f", Math.pow(10, shaking))).withStyle(ChatFormatting.GOLD)
                : Component.translatable(key + "quiet").withStyle(ChatFormatting.DARK_GRAY));
        if (heard >= 0 && level != null && level.getGameTime() - heard <= Disturbances.KEEP) {
            lines.add(Component.translatable(key + "last", String.format(Locale.ROOT, "%.1f", magnitude),
                    String.format(Locale.ROOT, "%,.0f", distance), (level.getGameTime() - heard) / 20));
        }
    }

    @Override
    protected void save(CompoundTag tag) {
        tag.putDouble("shaking", shaking);
        tag.putDouble("magnitude", magnitude);
        tag.putDouble("distance", distance);
        tag.putLong("heard", heard);
    }

    @Override
    protected void load(CompoundTag tag) {
        shaking = tag.contains("shaking") ? tag.getDouble("shaking") : NOISE;
        magnitude = tag.getDouble("magnitude");
        distance = tag.getDouble("distance");
        heard = tag.contains("heard") ? tag.getLong("heard") : -1;
    }
}
