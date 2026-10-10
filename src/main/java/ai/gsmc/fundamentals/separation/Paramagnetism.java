package ai.gsmc.fundamentals.separation;

import java.util.Map;

public final class Paramagnetism {

    public static final double ROOM_KELVIN = 293;
    // tools/build_separation_data.py reads this table.
    public static final Map<String, Double> CURIE = Map.of("Sm", 0.32, "Eu", 0.0);

    private Paramagnetism() {}

    public static double relative(double curieShare, double celsius) {
        double kelvin = Math.max(20, celsius + 273.15);
        return curieShare * ROOM_KELVIN / kelvin + 1 - curieShare;
    }

    public static double relative(String element, double celsius) {
        return relative(CURIE.getOrDefault(element, 1.0), celsius);
    }
}
