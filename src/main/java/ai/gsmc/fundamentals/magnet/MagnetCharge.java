package ai.gsmc.fundamentals.magnet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The magnets a motor, generator or stator was built with, and how much of their field is left. */
public record MagnetCharge(MagnetGrade grade, float field) {

    /** A machine built before magnets had grades: every NdFeB the mod made then carried dysprosium or terbium. */
    public static final MagnetCharge UNGRADED = new MagnetCharge(MagnetGrade.DY_NDFEB, 1);

    public static final Codec<MagnetCharge> CODEC = RecordCodecBuilder.create(i -> i.group(
            MagnetGrade.CODEC.fieldOf("grade").forGetter(MagnetCharge::grade),
            Codec.floatRange(0, 1).optionalFieldOf("field", 1F).forGetter(MagnetCharge::field))
            .apply(i, MagnetCharge::new));
    public static final StreamCodec<ByteBuf, MagnetCharge> STREAM_CODEC = StreamCodec.composite(
            MagnetGrade.STREAM_CODEC, MagnetCharge::grade, ByteBufCodecs.FLOAT, MagnetCharge::field, MagnetCharge::new);

    public MagnetCharge heatedTo(double celsius) {
        float left = Math.min(field, grade.survives(celsius));
        return left == field ? this : new MagnetCharge(grade, left);
    }

    /** The machine's output as a share of a cold, fresh NdFeB one, in twentieths so that the heat's flicker does not churn the network. */
    public float output(double celsius) {
        return Math.round(grade.strength * field * grade.sag(celsius) * 20) / 20F;
    }

    public boolean demagnetised() {
        return field <= 0;
    }
}
