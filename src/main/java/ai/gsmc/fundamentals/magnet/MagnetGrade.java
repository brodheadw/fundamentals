package ai.gsmc.fundamentals.magnet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * A permanent magnet by what it is made of, with the three temperatures that matter to a machine built round it, in °C:
 * the most it is rated to run at, past which its field sags; the point past which the sag no longer comes back as it cools,
 * a share of the field lost for good; and its Curie point, where the field is gone. And how strong it is beside NdFeB.
 * NdFeB's coercivity collapses when hot, which is why dysprosium or terbium goes into the grain boundaries of the motor
 * grades; SmCo runs at 300 °C, alnico past 500 but is weak. tools/build_book.py reads the numbers from here.
 */
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

    /** What the heat takes off the field while it lasts: nothing up to the rating, half by the damage point, all of it at Curie. */
    public float sag(double celsius) {
        if (celsius <= maxOperating) {
            return 1;
        }
        if (celsius <= damage) {
            return (float) (1 - 0.5 * (celsius - maxOperating) / (damage - maxOperating));
        }
        return (float) Math.max(0, 0.5 * (1 - (celsius - damage) / (curie - damage)));
    }

    /** The most of its field a magnet can keep once it has been this hot. */
    public float survives(double celsius) {
        return celsius <= damage ? 1 : (float) Math.max(0, 1 - (celsius - damage) / (curie - damage));
    }
}
