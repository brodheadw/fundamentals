package ai.gsmc.fundamentals.separation;

import java.util.Map;

/**
 * How a lanthanide ion's paramagnetic susceptibility goes with temperature. Most Ln3+ ions follow Curie's law, χ ∝ 1/T (their
 * Weiss constants are a few kelvin, nothing beside room temperature), so a cold liquor is more magnetic than a warm one. Sm3+ and
 * Eu3+ do not: their first excited J levels lie close enough above the ground level to be mixed in by the field, a second-order
 * Van Vleck term that does not depend on temperature. Eu3+'s ground level is 7F0, J = 0, with no moment at all: what it has is
 * that mixing and the 7F1 level warmth reaches, which together hold it nearly flat. Sm3+'s 6H5/2 ground level is worth 0.85 μB of its 1.5 at room temperature, so a third of its
 * susceptibility (0.85² / 1.5²) follows Curie and the rest is flat. Van Vleck, The Theory of Electric and Magnetic Susceptibilities
 * (1932); Cotton, Lanthanide and Actinide Chemistry (2006).
 */
public final class Paramagnetism {

    /** Where the moments are tabled, 20 °C. */
    public static final double ROOM_KELVIN = 293;
    /** The share of an ion's room-temperature susceptibility that follows Curie's law; every Ln3+ not named is all Curie.
     * tools/build_separation_data.py reads this table. */
    public static final Map<String, Double> CURIE = Map.of("Sm", 0.32, "Eu", 0.0);

    private Paramagnetism() {}

    /** χ(T) / χ(20 °C) for a susceptibility of which {@code curieShare} follows Curie's law and the rest is flat. */
    public static double relative(double curieShare, double celsius) {
        double kelvin = Math.max(20, celsius + 273.15);
        return curieShare * ROOM_KELVIN / kelvin + 1 - curieShare;
    }

    public static double relative(String element, double celsius) {
        return relative(CURIE.getOrDefault(element, 1.0), celsius);
    }
}
