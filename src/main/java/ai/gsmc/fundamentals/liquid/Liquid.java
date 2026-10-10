package ai.gsmc.fundamentals.liquid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * What a fluid really is: density in kg/m³ (a gas's at 20 °C and 1 atm), viscosity in mPa·s, freezing and boiling point,
 * flash point and autoignition temperature in °C at 1 atm, and the temperature a hot stream leaves its process at. Water-based
 * fluids freeze in the pipe; the hazard flags are what the game makes of it. Written by tools/build_heat_data.py into the data map
 * {@code fundamentals:liquid_properties}; any mod can describe its own fluids with a data file.
 */
public record Liquid(double density, Optional<Double> viscosity, Optional<Double> freezes, Optional<Double> boils, Optional<Double> flashPoint,
                     Optional<Double> autoignition, Optional<Double> celsius, boolean aqueous, boolean toxic, boolean corrosive, boolean fuming) {

    public static final Codec<Liquid> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("density").forGetter(Liquid::density),
            Codec.DOUBLE.optionalFieldOf("viscosity").forGetter(Liquid::viscosity),
            Codec.DOUBLE.optionalFieldOf("freezes").forGetter(Liquid::freezes),
            Codec.DOUBLE.optionalFieldOf("boils").forGetter(Liquid::boils),
            Codec.DOUBLE.optionalFieldOf("flash_point").forGetter(Liquid::flashPoint),
            Codec.DOUBLE.optionalFieldOf("autoignition").forGetter(Liquid::autoignition),
            Codec.DOUBLE.optionalFieldOf("celsius").forGetter(Liquid::celsius),
            Codec.BOOL.optionalFieldOf("aqueous", false).forGetter(Liquid::aqueous),
            Codec.BOOL.optionalFieldOf("toxic", false).forGetter(Liquid::toxic),
            Codec.BOOL.optionalFieldOf("corrosive", false).forGetter(Liquid::corrosive),
            Codec.BOOL.optionalFieldOf("fuming", false).forGetter(Liquid::fuming))
            .apply(i, Liquid::new));

    public boolean flammable() {
        return flashPoint.isPresent();
    }
}
