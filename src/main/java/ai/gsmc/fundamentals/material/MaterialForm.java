package ai.gsmc.fundamentals.material;

import java.util.Locale;

public enum MaterialForm {
    ORE,
    RAW,
    CONCENTRATE,
    OXALATE,
    FLUORIDE,
    OXIDE,
    DUST,
    SPONGE,
    INGOT,
    NUGGET,
    PLATE,
    BLOCK;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
