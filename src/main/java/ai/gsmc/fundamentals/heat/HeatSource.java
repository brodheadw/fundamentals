package ai.gsmc.fundamentals.heat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * What a block adds to the temperature around it: its own temperature in °C above ambient at the block itself, and
 * {@code outside} it, falling to nothing {@code reach} blocks away. A furnace's walls hold its fire in, so outside is
 * far cooler than in; left out, it is the same. Written by data maps ({@code fundamentals:heat_source}); any mod can
 * add its own blocks with a data file.
 */
public record HeatSource(double celsius, int reach, double outside) {

    public static final Codec<HeatSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("celsius").forGetter(HeatSource::celsius),
            Codec.INT.optionalFieldOf("reach", 3).forGetter(HeatSource::reach),
            Codec.DOUBLE.optionalFieldOf("outside").forGetter(s -> Optional.of(s.outside())))
            .apply(i, (celsius, reach, outside) -> new HeatSource(celsius, reach, outside.orElse(celsius))));
}
