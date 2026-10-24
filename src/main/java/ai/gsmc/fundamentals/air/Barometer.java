package ai.gsmc.fundamentals.air;

import net.minecraft.util.Mth;

/** Two barometers: one reads the pressure, the other records it and warns of a falling glass. */
public enum Barometer {
    MERCURY("mercury_barometer", false),
    BAROGRAPH("barograph", true);

    /** The dial's span in hectopascals, from deep storm to a high, dry spell or a valley floor. */
    public static final double MIN = 940, MAX = 1060;
    /** A fall this fast fills the barograph's warning: roughly the glass dropping ahead of thunder. */
    public static final double ALARM = 4;

    public final String id;
    /** Whether the comparator reports the fall rather than the pressure. */
    public final boolean warns;

    Barometer(String id, boolean warns) {
        this.id = id;
        this.warns = warns;
    }

    public static double fraction(double hectopascals) {
        return Mth.clamp((hectopascals - MIN) / (MAX - MIN), 0, 1);
    }

    /** The comparator signal for a reading and its change over the last hour. */
    public int signal(double hectopascals, double perHour) {
        return warns ? (int) Math.round(15 * Mth.clamp(-perHour / ALARM, 0, 1)) : (int) Math.round(15 * fraction(hectopascals));
    }
}
