package ai.gsmc.fundamentals.content.nuclear;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;

import static ai.gsmc.fundamentals.material.MaterialForm.*;

public final class NuclearMaterials {

    public static final String GROUP = "nuclear";

    private NuclearMaterials() {}

    public static void register() {
        MaterialRegistry.defineMineral(GROUP, "uraninite", null, "UO2", "uranium", new MaterialForm[] {ORE, RAW, DUST},
                MaterialProperties.builder().density(1.10).hardness(0.55).radioactivity(0.80).toxicity(0.30));
        MaterialRegistry.define(GROUP, "zircaloy", null, MaterialType.ALLOY, "Zr-Sn", new MaterialForm[] {INGOT, PLATE},
                MaterialProperties.builder().density(0.83).hardness(0.60).heatResistance(0.80).conductivity(0.03));
    }
}
