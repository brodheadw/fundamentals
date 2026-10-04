package ai.gsmc.fundamentals.material;

/**
 * Physical/chemical stats a {@link Material} carries. These are the data that machines and
 * (later) crafted tools/magnets read — e.g. {@link #magnetStrength} distinguishes NdFeB from
 * SmCo so magnets built from different materials behave differently (PLAN §2.1).
 *
 * <p>All values are normalised to roughly {@code 0.0 .. 1.0} for gameplay comparison rather
 * than being true SI units (density is relative, 1.0 ~= iron). This is append-only shared
 * schema — add fields with sensible defaults, never reorder (PLAN §4).
 *
 * @param density        relative density (iron ≈ 1.0)
 * @param magnetStrength ferromagnetic/permanent-magnet strength (0 = non-magnetic)
 * @param heatResistance resistance to demagnetisation / thermal failure
 * @param conductivity   electrical conductivity (copper ≈ 1.0)
 * @param hardness       mechanical hardness
 * @param radioactivity  0 = stable; &gt;0 = emits (hazard mechanics)
 * @param toxicity       0 = safe; &gt;0 = toxic (hazard mechanics)
 */
public record MaterialProperties(
        double density,
        double magnetStrength,
        double heatResistance,
        double conductivity,
        double hardness,
        double radioactivity,
        double toxicity
) {
    public static final MaterialProperties NONE = new MaterialProperties(0, 0, 0, 0, 0, 0, 0);

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder so material definitions read cleanly (all fields default to 0). */
    public static final class Builder {
        private double density, magnetStrength, heatResistance, conductivity, hardness,
                radioactivity, toxicity;

        public Builder density(double v) { this.density = v; return this; }
        public Builder magnetStrength(double v) { this.magnetStrength = v; return this; }
        public Builder heatResistance(double v) { this.heatResistance = v; return this; }
        public Builder conductivity(double v) { this.conductivity = v; return this; }
        public Builder hardness(double v) { this.hardness = v; return this; }
        public Builder radioactivity(double v) { this.radioactivity = v; return this; }
        public Builder toxicity(double v) { this.toxicity = v; return this; }

        public MaterialProperties build() {
            return new MaterialProperties(density, magnetStrength, heatResistance,
                    conductivity, hardness, radioactivity, toxicity);
        }
    }
}
