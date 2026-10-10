package ai.gsmc.fundamentals.heat;

import net.minecraft.util.Mth;

public enum Thermometer {
    MERCURY("mercury_thermometer", -39, 357, true),
    SPIRIT("spirit_thermometer", -60, 150, true),
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

    public double fraction(double celsius) {
        return Mth.clamp((celsius - min) / (max - min), 0, 1);
    }

    public int signal(double celsius) {
        return (int) Math.round(15 * fraction(celsius));
    }
}
