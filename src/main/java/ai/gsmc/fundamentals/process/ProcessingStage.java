package ai.gsmc.fundamentals.process;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum ProcessingStage {
    BLOOMERY(Tier.T0),
    PANNING(Tier.T0),

    CRUSHING(Tier.T1),
    GRINDING(Tier.T1),
    WASHING(Tier.T1),
    SCREENING(Tier.T1),
    GRAVITY_SEPARATION(Tier.T1),
    MAGNETIC_SEPARATION(Tier.T1),
    ELECTROSTATIC_SEPARATION(Tier.T1),
    FROTH_FLOTATION(Tier.T1),

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

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String display() {
        return Arrays.stream(id().split("_")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }

    // "Leaching(Cyanidation)" is LEACHING; an unknown token is null.
    public static ProcessingStage fromCatalog(String token) {
        if (token == null) return null;
        String base = token.split("\\(", 2)[0].trim();
        String snake = base.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        try {
            return valueOf(snake);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
