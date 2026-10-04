package ai.gsmc.fundamentals.process;

import ai.gsmc.fundamentals.material.MaterialForm;

/**
 * A reference to a specific {@link ai.gsmc.fundamentals.material.Material} in a specific
 * {@link MaterialForm} — e.g. {@code iron:dust}. Used as the input/output of a
 * {@link ProcessingStep}. Append-only shared schema (PLAN §4).
 */
public record MaterialRef(String material, MaterialForm form) {

    public static MaterialRef of(String material, MaterialForm form) {
        return new MaterialRef(material, form);
    }

    @Override
    public String toString() {
        return material + ":" + form.id();
    }
}
