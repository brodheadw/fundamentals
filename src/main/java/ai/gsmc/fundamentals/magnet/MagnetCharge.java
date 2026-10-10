package ai.gsmc.fundamentals.magnet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MagnetCharge(MagnetGrade grade, float field) {

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

    public float output(double celsius) {
        return Math.round(grade.strength * field * grade.sag(celsius) * 20) / 20F;
    }

    public boolean demagnetised() {
        return field <= 0;
    }
}
