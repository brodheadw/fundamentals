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
        MaterialRegistry.define(GROUP, "lithium", null, MaterialType.ELEMENT, "Li", new MaterialForm[] {INGOT, NUGGET, BLOCK},
                MaterialProperties.builder().density(0.07).conductivity(0.18).hardness(0.05));
        MaterialRegistry.define(GROUP, "magnesium", null, MaterialType.ELEMENT, "Mg", new MaterialForm[] {INGOT},
                MaterialProperties.builder().density(0.22).conductivity(0.38).hardness(0.10));

        MaterialRegistry.defineMineral(GROUP, "zircon", null, "ZrSiO4", "zirconium", new MaterialForm[] {ORE, RAW, CONCENTRATE},
                MaterialProperties.builder().density(0.59).hardness(0.75));
        MaterialRegistry.defineMineral(GROUP, "beryl", null, "Be3Al2Si6O18", "beryllium", new MaterialForm[] {ORE, RAW},
                MaterialProperties.builder().density(0.34).hardness(0.80).toxicity(0.10));
        MaterialRegistry.defineMineral(GROUP, "bertrandite", null, "Be4Si2O7(OH)2", "beryllium", new MaterialForm[] {ORE, RAW},
                MaterialProperties.builder().density(0.33).hardness(0.65).toxicity(0.10));
        MaterialRegistry.define(GROUP, "zirconium", null, MaterialType.ELEMENT, "Zr", new MaterialForm[] {OXIDE, SPONGE, INGOT, NUGGET, PLATE},
                MaterialProperties.builder().density(0.83).hardness(0.55).heatResistance(0.80).conductivity(0.04));
        MaterialRegistry.define(GROUP, "hafnium", null, MaterialType.ELEMENT, "Hf", new MaterialForm[] {SPONGE, INGOT, NUGGET},
                MaterialProperties.builder().density(1.69).hardness(0.60).heatResistance(0.90).conductivity(0.05));
        MaterialRegistry.define(GROUP, "beryllium", null, MaterialType.ELEMENT, "Be", new MaterialForm[] {OXIDE, FLUORIDE, INGOT, NUGGET},
                MaterialProperties.builder().density(0.24).hardness(0.60).conductivity(0.43).toxicity(0.80));
        MaterialRegistry.define(GROUP, "beryllium_copper", null, MaterialType.ALLOY, "Cu-Be", new MaterialForm[] {INGOT, BLOCK},
                MaterialProperties.builder().density(1.05).hardness(0.70).conductivity(0.25));
    }
}
