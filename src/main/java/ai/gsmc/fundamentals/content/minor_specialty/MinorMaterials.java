package ai.gsmc.fundamentals.content.minor_specialty;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;

import static ai.gsmc.fundamentals.material.MaterialForm.*;

public final class MinorMaterials {

    public static final String GROUP = "minor_specialty";

    private MinorMaterials() {}

    public static void register() {
        MaterialRegistry.defineMineral(GROUP, "stibnite", null, "Sb2S3", "antimony", new MaterialForm[] {ORE, RAW},
                MaterialProperties.builder().density(0.47).hardness(0.20).toxicity(0.30));
        MaterialRegistry.defineMineral(GROUP, "pyrochlore", null, "(Na,Ca)2Nb2O6(OH,F)", "niobium", new MaterialForm[] {ORE, RAW, CONCENTRATE},
                MaterialProperties.builder().density(0.47).hardness(0.55));
        MaterialRegistry.defineMineral(GROUP, "columbite", "Columbite-Tantalite", "(Fe,Mn)(Nb,Ta)2O6", "tantalum", new MaterialForm[] {ORE, RAW, DUST},
                MaterialProperties.builder().density(0.69).hardness(0.65));

        MaterialRegistry.define(GROUP, "antimony", null, MaterialType.ELEMENT, "Sb", new MaterialForm[] {OXIDE, INGOT, NUGGET},
                MaterialProperties.builder().density(0.85).hardness(0.30).conductivity(0.04).toxicity(0.40));
        MaterialRegistry.define(GROUP, "antimonial_lead", null, MaterialType.ALLOY, "Pb-Sb", new MaterialForm[] {INGOT, PLATE},
                MaterialProperties.builder().density(1.40).hardness(0.20).conductivity(0.07).toxicity(0.60));
        MaterialRegistry.define(GROUP, "niobium", null, MaterialType.ELEMENT, "Nb", new MaterialForm[] {OXIDE},
                MaterialProperties.builder().density(1.09).hardness(0.60).heatResistance(0.85).conductivity(0.12));
        MaterialRegistry.define(GROUP, "ferroniobium", null, MaterialType.ALLOY, "Fe-Nb", new MaterialForm[] {INGOT, NUGGET},
                MaterialProperties.builder().density(1.03).hardness(0.80));
        MaterialRegistry.define(GROUP, "tantalum", null, MaterialType.ELEMENT, "Ta", new MaterialForm[] {INGOT, NUGGET},
                MaterialProperties.builder().density(2.11).hardness(0.70).heatResistance(0.95).conductivity(0.13));
    }
}
