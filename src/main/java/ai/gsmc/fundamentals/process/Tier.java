package ai.gsmc.fundamentals.process;

public enum Tier {
    T0("Hand"),
    T1("Mechanical"),
    T2("Thermal"),
    T3("Hydrometallurgy"),
    T4("Electrometallurgy"),
    T5("Advanced Separation");

    private final String display;

    Tier(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
