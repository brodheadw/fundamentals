package ai.gsmc.fundamentals.process;

import ai.gsmc.fundamentals.material.MaterialForm;

public record MaterialRef(String material, MaterialForm form) {

    public static MaterialRef of(String material, MaterialForm form) {
        return new MaterialRef(material, form);
    }

    @Override
    public String toString() {
        return material + ":" + form.id();
    }
}
