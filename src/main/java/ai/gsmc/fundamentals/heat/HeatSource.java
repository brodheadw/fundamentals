package ai.gsmc.fundamentals.heat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What a block adds to the temperature around it: its own temperature in °C above ambient at the block itself,
 * falling to nothing {@code reach} blocks away. Written by data maps ({@code fundamentals:heat_source}); any mod can
 * add its own blocks with a data file.
 */
public record HeatSource(double celsius, int reach) {

    public static final Codec<HeatSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("celsius").forGetter(HeatSource::celsius),
            Codec.INT.optionalFieldOf("reach", 3).forGetter(HeatSource::reach)).apply(i, HeatSource::new));
}
