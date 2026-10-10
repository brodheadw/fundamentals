package ai.gsmc.fundamentals.oxidation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record Rate(String kind, float days, int stages, Optional<Item> product, int per, float dry, float wet) {

    public static final Codec<Rate> CODEC = RecordCodecBuilder.create(in -> in.group(
            Codec.STRING.fieldOf("kind").forGetter(Rate::kind),
            Codec.FLOAT.fieldOf("days").forGetter(Rate::days),
            Codec.INT.optionalFieldOf("stages", 3).forGetter(Rate::stages),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("product").forGetter(Rate::product),
            Codec.INT.optionalFieldOf("per", 1).forGetter(Rate::per),
            Codec.FLOAT.optionalFieldOf("dry", 0.25F).forGetter(Rate::dry),
            Codec.FLOAT.optionalFieldOf("wet", 4F).forGetter(Rate::wet)
    ).apply(in, Rate::new));

    public int steps() {
        return stages + (product.isPresent() ? 1 : 0);
    }

    public double factor(Moisture moisture) {
        return switch (moisture) {
            case DRY -> dry;
            case AIR -> 1;
            case WET -> wet;
            case SALT -> wet * 2;
        };
    }
}
