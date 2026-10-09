package ai.gsmc.fundamentals.content.light_battery_tech;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;

import static ai.gsmc.fundamentals.material.MaterialForm.*;

public final class LightBatteryMaterials {

    public static final String GROUP = "light_battery_tech";

    private LightBatteryMaterials() {}

    public static void register() {
        MaterialRegistry.defineMineral(GROUP, "spodumene", null, "LiAlSi2O6", "lithium", new MaterialForm[] {ORE, RAW},
                MaterialProperties.builder().density(0.40).hardness(0.65));
        // The ingot, nugget and block are The Factory Must Grow's.
        MaterialRegistry.define(GROUP, "lithium", null, MaterialType.ELEMENT, "Li", new MaterialForm[] {INGOT, NUGGET, BLOCK},
                MaterialProperties.builder().density(0.07).conductivity(0.18).hardness(0.05));
        MaterialRegistry.define(GROUP, "magnesium", null, MaterialType.ELEMENT, "Mg", new MaterialForm[] {INGOT},
                MaterialProperties.builder().density(0.22).conductivity(0.38).hardness(0.10));
    }
}
