package ai.gsmc.fundamentals.content.industrial_minerals;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;

import static ai.gsmc.fundamentals.material.MaterialForm.*;

public final class IndustrialMaterials {

    public static final String GROUP = "industrial_minerals";

    private static final MaterialForm[] MINERAL_FORMS = {ORE, RAW};

    private IndustrialMaterials() {}

    public static void register() {
        mineral("quartz", "Quartz", "SiO2", "silica", MaterialProperties.builder().density(0.34).hardness(0.60));
        mineral("apatite", "Apatite", "Ca5(PO4)3F", "phosphate", MaterialProperties.builder().density(0.41));
        mineral("halite", "Halite", "NaCl", "salt", MaterialProperties.builder().density(0.28));
        mineral("fluorite", "Fluorite", "CaF2", "fluorspar", MaterialProperties.builder().density(0.32));
        mineral("borax", "Borax", "Na2B4O7·10H2O", "boron", MaterialProperties.builder().density(0.17));
        mineral("sylvite", "Sylvite", "KCl", "potash", MaterialProperties.builder().density(0.25));
        mineral("gypsum", "Gypsum", "CaSO4·2H2O", "gypsum", MaterialProperties.builder().density(0.29));
        mineral("trona", "Trona", "Na3H(CO3)2·2H2O", "soda_ash", MaterialProperties.builder().density(0.27));
        mineral("graphite", "Graphite", "C", "graphite", MaterialProperties.builder().density(0.28).conductivity(0.10));

        reg("silicon", MaterialType.ELEMENT, "Si", forms(INGOT, DUST),
                MaterialProperties.builder().density(0.30).hardness(0.60).conductivity(0.01));
        reg("soda_ash", MaterialType.COMPOUND, "Na2CO3", forms(DUST), MaterialProperties.builder().density(0.32));
        reg("potash", MaterialType.COMPOUND, "KCl", forms(DUST), MaterialProperties.builder().density(0.25));
        reg("quicklime", MaterialType.COMPOUND, "CaO", forms(DUST), MaterialProperties.builder().density(0.42));
        reg("phosphorus", MaterialType.ELEMENT, "P", forms(DUST),
                MaterialProperties.builder().density(0.23).toxicity(0.30));
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
