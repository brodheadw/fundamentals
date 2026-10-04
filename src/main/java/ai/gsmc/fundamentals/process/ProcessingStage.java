package ai.gsmc.fundamentals.process;

import java.util.Locale;

/**
 * The canonical processing-stage vocabulary (see {@code data/ores.json.stageVocabulary} and
 * {@code docs/PLAN.md} §2.2). Every ore chain in {@code data/ores.json} is expressed as an
 * ordered list of these stages, so each stage maps to exactly one machine / recipe type.
 *
 * <p>Ordered roughly by position in a real flowsheet. Each stage carries the {@link Tier} at
 * which its machine becomes available. This enum is append-only shared schema — add new stages
 * at the end of their tier grouping via PR, never renumber (PLAN §4).
 */
public enum ProcessingStage {
    // --- T0 Hand: primitive, no-machine routes that set the early game (PLAN §2.4) ---
    BLOOMERY(Tier.T0),   // mineral + charcoal -> solid bloom -> wrought iron; not in the real-world
                         // 49-stage catalog, it's the historical early-game iron route
    PANNING(Tier.T0),    // gravity washing of placer/native material (e.g. native gold)

    // --- T1 Mechanical: comminution & physical concentration ---
    CRUSHING(Tier.T1),
    GRINDING(Tier.T1),
    WASHING(Tier.T1),
    SCREENING(Tier.T1),
    GRAVITY_SEPARATION(Tier.T1),
    MAGNETIC_SEPARATION(Tier.T1),
    ELECTROSTATIC_SEPARATION(Tier.T1),
    FROTH_FLOTATION(Tier.T1),

    // --- T2 Thermal: roasting, smelting, carbothermic reduction ---
    DECREPITATION(Tier.T2),
    ROASTING(Tier.T2),
    VOLATILIZATION(Tier.T2),
    CALCINATION(Tier.T2),
    SINTERING(Tier.T2),
    CHLORINATION(Tier.T2),
    CRACKING(Tier.T2),
    SMELTING(Tier.T2),
    CONVERTING(Tier.T2),
    CARBOTHERMIC_REDUCTION(Tier.T2),
    REDUCTION(Tier.T2),
    RETORTING(Tier.T2),
    REFINING(Tier.T2),

    // --- T3 Hydrometallurgy: leaching, precipitation ---
    LEACHING(Tier.T3),
    PRESSURE_OXIDATION(Tier.T3),
    BIO_OXIDATION(Tier.T3),
    DISSOLUTION(Tier.T3),
    PRECIPITATION(Tier.T3),
    EVAPORATION(Tier.T3),
    CRYSTALLIZATION(Tier.T3),
    CARBON_ADSORPTION(Tier.T3),
    HYDROGEN_REDUCTION(Tier.T3),
    PURIFICATION(Tier.T3),

    // --- T4 Electrometallurgy & named industrial processes ---
    ELECTROWINNING(Tier.T4),
    ELECTROLYSIS(Tier.T4),
    HALL_HEROULT(Tier.T4),
    ELECTROREFINING(Tier.T4),
    CUPELLATION(Tier.T4),
    ALUMINOTHERMIC_REDUCTION(Tier.T4),
    SILICOTHERMIC_REDUCTION(Tier.T4),
    BAYER_PROCESS(Tier.T4),
    SOLVAY_PROCESS(Tier.T4),
    CONTACT_PROCESS(Tier.T4),
    CLAUS_PROCESS(Tier.T4),
    FRASCH_PROCESS(Tier.T4),
    SIEMENS_PROCESS(Tier.T4),

    // --- T5 Advanced separation (endgame) ---
    SOLVENT_EXTRACTION(Tier.T5),
    ION_EXCHANGE(Tier.T5),
    MOLTEN_SALT_ELECTROLYSIS(Tier.T5),
    METALLOTHERMIC_REDUCTION(Tier.T5),
    KROLL_PROCESS(Tier.T5),
    ALLOYING(Tier.T5);

    private final Tier tier;

    ProcessingStage(Tier tier) {
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    /** snake_case id used for machine ids, recipe types and tags (e.g. {@code froth_flotation}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Human-readable display, e.g. {@code Froth Flotation}. */
    public String display() {
        StringBuilder sb = new StringBuilder();
        for (String word : name().split("_")) {
            sb.append(Character.toUpperCase(word.charAt(0)))
              .append(word.substring(1).toLowerCase(Locale.ROOT))
              .append(' ');
        }
        return sb.toString().trim();
    }

    /**
     * Matches a canonical stage token from {@code data/ores.json} (e.g. {@code "FrothFlotation"}
     * or {@code "Leaching(Cyanidation)"}) to this enum, ignoring any parenthetical qualifier.
     * Returns null if unknown.
     */
    public static ProcessingStage fromCatalog(String token) {
        if (token == null) return null;
        String base = token.split("\\(", 2)[0].trim();
        // CamelCase -> UPPER_SNAKE
        String snake = base.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        try {
            return valueOf(snake);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
