package ai.gsmc.fundamentals.content.precious_pgm;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;

import static ai.gsmc.fundamentals.material.MaterialForm.*;

public final class PreciousMaterials {

    public static final String GROUP = "precious_pgm";

    private static final MaterialForm[] MINERAL_FORMS = {ORE, RAW};

    private PreciousMaterials() {}

    public static void register() {
        // native_gold is vanilla's gold ore; it is defined so chains have something to refer to.
        mineral("native_gold", "Native Gold", "Au", "gold", MaterialProperties.builder().density(2.45));
        mineral("native_silver", "Native Silver", "Ag", "silver", MaterialProperties.builder().density(1.33));
        mineral("argentite", "Argentite", "Ag2S", "silver", MaterialProperties.builder().density(0.92));
        mineral("sperrylite", "Sperrylite", "PtAs2", "platinum",
                MaterialProperties.builder().density(1.40).toxicity(0.20)); // As-bearing
        mineral("cooperite", "Cooperite", "PtS", "platinum", MaterialProperties.builder().density(1.26));
        mineral("braggite", "Braggite", "(Pt,Pd,Ni)S", "platinum", MaterialProperties.builder().density(1.26));
        mineral("cinnabar", "Cinnabar", "HgS", "mercury", MaterialProperties.builder().density(1.03).toxicity(0.60));

        reg("platinum_group_concentrate", MaterialType.CONCENTRATE, "", forms(CONCENTRATE),
                MaterialProperties.builder());

        reg("gold", MaterialType.ELEMENT, "Au", forms(INGOT, NUGGET, BLOCK, DUST, PLATE),
                MaterialProperties.builder().density(2.45).conductivity(0.76).hardness(0.25).heatResistance(0.30));
        reg("silver", MaterialType.ELEMENT, "Ag", forms(INGOT, NUGGET, BLOCK, DUST, PLATE),
                MaterialProperties.builder().density(1.33).conductivity(1.05).hardness(0.25));

        reg("platinum", MaterialType.ELEMENT, "Pt", forms(INGOT, NUGGET, DUST),
                MaterialProperties.builder().density(2.73).hardness(0.35).heatResistance(0.80).conductivity(0.16));
        reg("palladium", MaterialType.ELEMENT, "Pd", forms(INGOT, DUST),
                MaterialProperties.builder().density(1.52).hardness(0.40).heatResistance(0.60));
        reg("rhodium", MaterialType.ELEMENT, "Rh", forms(INGOT, DUST),
                MaterialProperties.builder().density(1.58).hardness(0.60).heatResistance(0.85).conductivity(0.38));
        reg("ruthenium", MaterialType.ELEMENT, "Ru", forms(INGOT, DUST),
                MaterialProperties.builder().density(1.56).hardness(0.70).heatResistance(0.80));
        reg("iridium", MaterialType.ELEMENT, "Ir", forms(INGOT, DUST),
                MaterialProperties.builder().density(2.86).hardness(0.70).heatResistance(0.90));
        reg("osmium", MaterialType.ELEMENT, "Os", forms(INGOT, DUST),
                MaterialProperties.builder().density(2.87).hardness(0.80).heatResistance(0.88)
                        .toxicity(0.30)); // OsO4 is toxic

        // Mercury is DUST until there is a fluid form.
        reg("mercury", MaterialType.ELEMENT, "Hg", forms(DUST),
                MaterialProperties.builder().density(1.72).toxicity(0.90));

        reg("electrum", MaterialType.ALLOY, "(Au,Ag)", forms(INGOT, NUGGET),
                MaterialProperties.builder().density(1.90).conductivity(0.80).hardness(0.30));
    }

    private static MaterialForm[] forms(MaterialForm... f) {
        return f;
    }

    private static void mineral(String id, String display, String formula, String commodity,
                                MaterialProperties.Builder props) {
        MaterialRegistry.defineMineral(GROUP, id, display, formula, commodity, MINERAL_FORMS, props);
    }

    private static void reg(String id, MaterialType type, String formula, MaterialForm[] forms,
                            MaterialProperties.Builder props) {
        MaterialRegistry.define(GROUP, id, null, type, formula, forms, props);
    }
}
