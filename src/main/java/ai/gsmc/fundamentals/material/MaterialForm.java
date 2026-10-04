package ai.gsmc.fundamentals.material;

import java.util.Locale;

/**
 * The physical forms a {@link Material} can take as items/blocks. Each form becomes a tagged
 * item (see PLAN §4 tag conventions, e.g. {@code c:ingots/<metal>}, {@code c:dusts/<material>}).
 * Append-only shared schema.
 */
public enum MaterialForm {
    ORE,            // in-world ore block
    RAW,            // raw chunk mined from ore (pre-processing)
    CONCENTRATE,    // beneficiation product
    OXIDE,          // calcined oxide intermediate (e.g. WO3, Nd2O3)
    DUST,           // ground powder
    INGOT,          // smelted/refined metal unit
    NUGGET,         // 1/9 ingot
    PLATE,          // pressed metal
    BLOCK;          // storage block of the metal

    /** snake_case id used for item ids and tag paths. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
