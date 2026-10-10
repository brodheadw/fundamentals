package ai.gsmc.fundamentals.material;

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
