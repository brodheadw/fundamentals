package ai.gsmc.fundamentals.process;

/**
 * Progression tiers for processing machines. A {@link ProcessingStage} is unlocked at its
 * tier. See {@code docs/PLAN.md} §2.3.
 *
 * <p>This is append-only shared schema — coordinate changes via PR (PLAN §4).
 */
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
