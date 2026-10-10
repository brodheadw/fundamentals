package ai.gsmc.fundamentals.magnet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum MagnetGrade implements StringRepresentable {
    NDFEB("neodymium_iron_boron", 80, 120, 320, 1.0F),
    DY_NDFEB("dysprosium_neodymium_iron_boron", 180, 230, 340, 1.0F),
    SMCO("samarium_cobalt", 300, 400, 800, 0.8F),
    ALNICO("alnico", 525, 600, 860, 0.4F);

    public static final StringRepresentable.EnumCodec<MagnetGrade> CODEC = StringRepresentable.fromEnum(MagnetGrade::values);
    public static final StreamCodec<ByteBuf, MagnetGrade> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[i], MagnetGrade::ordinal);

    public final String material;
    public final int maxOperating, damage, curie;
    public final float strength;

    MagnetGrade(String material, int maxOperating, int damage, int curie, float strength) {
        this.material = material;
        this.maxOperating = maxOperating;
        this.damage = damage;
        this.curie = curie;
        this.strength = strength;
    }

    @Override
    public String getSerializedName() {
        return material;
    }

    public String magnet() {
        return material + "_magnet";
    }

    public float sag(double celsius) {
        if (celsius <= maxOperating) {
            return 1;
        }
        if (celsius <= damage) {
            return (float) (1 - 0.5 * (celsius - maxOperating) / (damage - maxOperating));
        }
        return (float) Math.max(0, 0.5 * (1 - (celsius - damage) / (curie - damage)));
    }

    public float survives(double celsius) {
        return celsius <= damage ? 1 : (float) Math.max(0, 1 - (celsius - damage) / (curie - damage));
    }
}
