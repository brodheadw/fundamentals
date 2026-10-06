package ai.gsmc.fundamentals.content.ferrous_ferroalloy;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;

import static ai.gsmc.fundamentals.material.MaterialForm.*;

public final class FerrousMaterials {

    public static final String GROUP = "ferrous_ferroalloy";

    private static final MaterialForm[] MINERAL_FORMS = {ORE, RAW};

    private FerrousMaterials() {}

    public static void register() {
        mineral("hematite", "Hematite", "Fe2O3", "iron",
                MaterialProperties.builder().density(0.66).magnetStrength(0.05));
        mineral("magnetite", "Magnetite", "Fe3O4", "iron",
                MaterialProperties.builder().density(0.66).magnetStrength(0.20)); // lodestone mineral
        mineral("goethite", "Goethite", "FeO(OH)", "iron", MaterialProperties.builder().density(0.48));
        mineral("pyrolusite", "Pyrolusite", "MnO2", "manganese", MaterialProperties.builder().density(0.63));
        mineral("pentlandite", "Pentlandite", "(Fe,Ni)9S8", "nickel", MaterialProperties.builder().density(0.61));
        mineral("nickel_laterite", "Nickel Laterite", "(Fe,Ni)O(OH)", "nickel",
                MaterialProperties.builder().density(0.40));
        mineral("chromite", "Chromite", "FeCr2O4", "chromium",
                MaterialProperties.builder().density(0.61).magnetStrength(0.05));
        mineral("wolframite", "Wolframite", "(Fe,Mn)WO4", "tungsten", MaterialProperties.builder().density(0.93));
        mineral("scheelite", "Scheelite", "CaWO4", "tungsten", MaterialProperties.builder().density(0.76));
        mineral("molybdenite", "Molybdenite", "MoS2", "molybdenum", MaterialProperties.builder().density(0.60));
        mineral("cobaltite", "Cobaltite", "CoAsS", "cobalt",
                MaterialProperties.builder().density(0.80).toxicity(0.20)); // As-bearing
        mineral("ilmenite", "Ilmenite", "FeTiO3", "titanium", MaterialProperties.builder().density(0.60));
        mineral("rutile", "Rutile", "TiO2", "titanium", MaterialProperties.builder().density(0.53));

        reg("iron", MaterialType.ELEMENT, "Fe", forms(INGOT, DUST, NUGGET, PLATE, BLOCK),
                MaterialProperties.builder().density(1.0).magnetStrength(0.40)
                        .heatResistance(0.45).conductivity(0.17).hardness(0.40));
        reg("steel", MaterialType.ALLOY, "", forms(INGOT, DUST, NUGGET, PLATE, BLOCK),
                MaterialProperties.builder().density(1.0).magnetStrength(0.35).heatResistance(0.60).hardness(0.70));
        reg("chromium", MaterialType.ELEMENT, "Cr", forms(INGOT, DUST),
                MaterialProperties.builder().density(0.92).hardness(0.90).heatResistance(0.70));
        reg("manganese", MaterialType.ELEMENT, "Mn", forms(INGOT, DUST),
                MaterialProperties.builder().density(0.95).hardness(0.75));
        reg("nickel", MaterialType.ELEMENT, "Ni", forms(INGOT, DUST, NUGGET),
                MaterialProperties.builder().density(1.13).magnetStrength(0.30).conductivity(0.25).hardness(0.50));
        reg("cobalt", MaterialType.ELEMENT, "Co", forms(INGOT, DUST, NUGGET),
                MaterialProperties.builder().density(1.13).magnetStrength(0.45).heatResistance(0.85).hardness(0.55));
        reg("molybdenum", MaterialType.ELEMENT, "Mo", forms(INGOT, DUST),
                MaterialProperties.builder().density(1.30).heatResistance(0.88).hardness(0.80));
        reg("tungsten", MaterialType.ELEMENT, "W", forms(INGOT, DUST, PLATE),
                MaterialProperties.builder().density(1.90).heatResistance(1.00).hardness(0.95).conductivity(0.30));
        reg("vanadium", MaterialType.ELEMENT, "V", forms(INGOT, DUST),
                MaterialProperties.builder().density(0.77).hardness(0.80).heatResistance(0.65));
        reg("titanium", MaterialType.ELEMENT, "Ti", forms(INGOT, DUST, PLATE),
                MaterialProperties.builder().density(0.57).hardness(0.70).heatResistance(0.70).conductivity(0.03));

        reg("tungsten_trioxide", MaterialType.COMPOUND, "WO3", forms(OXIDE, DUST), MaterialProperties.builder());
        reg("ammonium_paratungstate", MaterialType.COMPOUND, "(NH4)10(H2W12O42)", forms(DUST),
                MaterialProperties.builder());

        reg("ferrochrome", MaterialType.ALLOY, "", forms(INGOT, DUST),
                MaterialProperties.builder().hardness(0.85).heatResistance(0.65));
        reg("ferromanganese", MaterialType.ALLOY, "", forms(INGOT, DUST), MaterialProperties.builder().hardness(0.70));
        reg("ferronickel", MaterialType.ALLOY, "", forms(INGOT, DUST),
                MaterialProperties.builder().magnetStrength(0.25).hardness(0.55));
        reg("ferromolybdenum", MaterialType.ALLOY, "", forms(INGOT, DUST),
                MaterialProperties.builder().heatResistance(0.80).hardness(0.80));
        reg("ferrotungsten", MaterialType.ALLOY, "", forms(INGOT, DUST),
                MaterialProperties.builder().heatResistance(0.90).hardness(0.90));
        reg("ferrovanadium", MaterialType.ALLOY, "", forms(INGOT, DUST), MaterialProperties.builder().hardness(0.80));
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
