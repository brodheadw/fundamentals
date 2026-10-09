package ai.gsmc.fundamentals.heat;

import net.minecraft.util.Mth;

/**
 * What a dial gauge reads with, and over what range: mercury in glass from its freezing point to its boiling point, a brass
 * and steel bimetal strip, and the two thermocouples, type K (chromel against alumel) and type S (platinum with a tenth of
 * rhodium against platinum). Past the top the mercury boils and bursts its glass; the rest peg against the stop.
 */
public enum Thermometer {
    MERCURY("mercury_thermometer", -39, 357, true),
    BIMETALLIC("bimetallic_thermometer", -50, 500, false),
    TYPE_K("type_k_thermocouple", -200, 1260, false),
    TYPE_S("type_s_thermocouple", -50, 1600, false);

    public final String id;
    public final double min, max;
    public final boolean bursts;

    Thermometer(String id, double min, double max, boolean bursts) {
        this.id = id;
        this.min = min;
        this.max = max;
        this.bursts = bursts;
    }

    /** Where the needle stands, 0 at the bottom of the scale and 1 at the top. */
    public double fraction(double celsius) {
        return Mth.clamp((celsius - min) / (max - min), 0, 1);
    }

    /** The comparator signal, 0 to 15 across the scale. */
    public int signal(double celsius) {
        return (int) Math.round(15 * fraction(celsius));
    }
}
