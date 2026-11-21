package ai.gsmc.fundamentals.survey;

import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

/**
 * De Saussure's hair hygrometer of 1783: a human hair, degreased, lengthens by about two and a half percent from dry air to
 * saturated, and turns the needle over a pulley as it does. It reads relative humidity: the water in the air against what air
 * that warm could hold.
 */
public class HygrometerBlockEntity extends SurveyBlockEntity {

    /** The water in a biome's air: its downfall of 1 is air saturated at 25 °C, and even a desert holds a trace. */
    static final double REFERENCE = 25, TRACE = 0.03;
    /** Open water within this reach wets the air over it. */
    static final int DAMP_REACH = 2;
    static final double DAMP = 20, RAIN = 95;

    private double humidity = 50, celsius = 15;

    public HygrometerBlockEntity(BlockPos pos, BlockState state) {
        super(Survey.hygrometerEntity(), pos, state);
    }

    /** Magnus: the vapour pressure that saturates air, in hectopascals, doubling about every 10 °C. */
    static double saturation(double celsius) {
        return 6.112 * Math.exp(17.62 * celsius / (243.12 + celsius));
    }

    /** Relative humidity in percent for a biome's downfall at a temperature, beside water or under rain or snow. */
    public static double relativeHumidity(double downfall, double celsius, boolean water, boolean rain) {
        double rh = 100 * (TRACE + downfall) * saturation(REFERENCE) / saturation(celsius);
        if (water) rh += DAMP;
        if (rain) rh = Math.max(rh, RAIN);
        return Math.clamp(rh, 0, 100);
    }

    /** The temperature the air would have to cool to for dew to form. */
    static double dewPoint(double celsius, double humidity) {
        double g = Math.log(Math.max(humidity, 0.1) / 100) + 17.62 * celsius / (243.12 + celsius);
        return 243.12 * g / (17.62 - g);
    }

    @Override
    protected void read(ServerLevel level) {
        Biome biome = level.getBiome(worldPosition).value();
        celsius = Heat.at(level, worldPosition);
        boolean water = false;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-DAMP_REACH, -DAMP_REACH, -DAMP_REACH), worldPosition.offset(DAMP_REACH, DAMP_REACH, DAMP_REACH))) {
            if (level.getFluidState(pos).is(FluidTags.WATER)) { water = true; break; }
        }
        boolean rain = level.isRaining() && biome.getPrecipitationAt(worldPosition) != Biome.Precipitation.NONE;
        humidity = relativeHumidity(biome.getModifiedClimateSettings().downfall(), celsius, water, rain);
    }

    public double humidity() { return humidity; }

    @Override
    public double fraction() {
        return humidity / 100;
    }

    @Override
    protected void describe(List<Component> lines) {
        String key = "goggles.fundamentals.hygrometer.";
        String feel = humidity < 20 ? "parched" : humidity < 40 ? "dry" : humidity < 70 ? "fair" : humidity < 95 ? "damp" : "saturated";
        lines.add(Component.translatable(key + "reading", String.format(Locale.ROOT, "%.0f", humidity), Component.translatable(key + feel))
                .withStyle(humidity >= 95 ? ChatFormatting.AQUA : humidity < 20 ? ChatFormatting.GOLD : ChatFormatting.WHITE));
        lines.add(Component.translatable(key + "dew", String.format(Locale.ROOT, "%.0f", celsius), String.format(Locale.ROOT, "%.0f", dewPoint(celsius, humidity)))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void save(CompoundTag tag) {
        tag.putDouble("humidity", humidity);
        tag.putDouble("celsius", celsius);
    }

    @Override
    protected void load(CompoundTag tag) {
        humidity = tag.contains("humidity") ? tag.getDouble("humidity") : 50;
        celsius = tag.contains("celsius") ? tag.getDouble("celsius") : 15;
    }
}
