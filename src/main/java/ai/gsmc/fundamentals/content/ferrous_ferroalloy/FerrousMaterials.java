package ai.gsmc.fundamentals.content.ferrous_ferroalloy;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;

import static ai.gsmc.fundamentals.material.MaterialForm.*;

public final class FerrousMaterials {

    public static final String GROUP = "ferrous_ferroalloy";

    private static final MaterialForm[] MINERAL_FORMS = {ORE, RAW};
    private static final MaterialForm[] CONCENTRATED = {ORE, RAW, DUST, CONCENTRATE};

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
        mineral("chromite", "Chromite", "FeCr2O4", "chromium", CONCENTRATED,
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
        reg("stainless_steel", MaterialType.ALLOY, "Fe-Cr-Ni", forms(INGOT, PLATE),
                MaterialProperties.builder().density(1.0).heatResistance(0.70).hardness(0.70));
        reg("chromium", MaterialType.ELEMENT, "Cr", forms(OXIDE, INGOT),
                MaterialProperties.builder().density(0.92).hardness(0.90).heatResistance(0.70));
        reg("manganese", MaterialType.ELEMENT, "Mn", forms(INGOT, DUST),
                MaterialProperties.builder().density(0.95).hardness(0.75));
        reg("nickel", MaterialType.ELEMENT, "Ni", forms(INGOT, DUST, NUGGET),
                MaterialProperties.builder().density(1.13).magnetStrength(0.30).conductivity(0.25).hardness(0.50));
        reg("cobalt", MaterialType.ELEMENT, "Co", forms(INGOT, NUGGET),
                MaterialProperties.builder().density(1.13).magnetStrength(0.45).heatResistance(0.85).hardness(0.55));
        reg("molybdenum", MaterialType.ELEMENT, "Mo", forms(OXIDE, INGOT),
                MaterialProperties.builder().density(1.30).heatResistance(0.88).hardness(0.80));
        // Rhenium rides in molybdenite at parts per million and leaves the roaster as flue dust; the superalloy is why anyone bothers.
        reg("rhenium", MaterialType.ELEMENT, "Re", forms(INGOT, NUGGET),
                MaterialProperties.builder().density(2.10).heatResistance(1.00).hardness(0.75));
        reg("superalloy", MaterialType.ALLOY, "Ni-Co-Cr-W-Al-Re", forms(INGOT, PLATE),
                MaterialProperties.builder().density(0.85).heatResistance(0.98).hardness(0.80));
        reg("molybdenum_steel", MaterialType.ALLOY, "Fe-Mo", forms(INGOT, PLATE),
                MaterialProperties.builder().density(0.80).heatResistance(0.70).hardness(0.85));
        // Hadfield's steel, 12 to 14 per cent manganese and about 1 carbon: austenitic, it work-hardens where it is struck
        reg("manganese_steel", MaterialType.ALLOY, "Fe-Mn-C", forms(INGOT),
                MaterialProperties.builder().density(1.0).hardness(0.85));
        // the two legs of a type K thermocouple: chromel is nickel with a tenth of chromium, alumel nickel with a few per cent of aluminium
        reg("chromel", MaterialType.ALLOY, "Ni-Cr", forms(INGOT),
                MaterialProperties.builder().density(1.10).heatResistance(0.80).conductivity(0.03));
        reg("alumel", MaterialType.ALLOY, "Ni-Al", forms(INGOT),
                MaterialProperties.builder().density(1.10).heatResistance(0.75).conductivity(0.05));
        // the cast magnet before the rare earths: iron with aluminium, nickel, cobalt and a little copper, weak but good past 500 °C
        reg("alnico", MaterialType.ALLOY, "Fe-Al-Ni-Co-Cu", forms(INGOT),
                MaterialProperties.builder().density(0.95).magnetStrength(0.40).heatResistance(0.90).hardness(0.70));
        reg("tungsten", MaterialType.ELEMENT, "W", forms(OXIDE, INGOT),
                MaterialProperties.builder().density(1.90).heatResistance(1.00).hardness(0.95).conductivity(0.30));
        reg("vanadium", MaterialType.ELEMENT, "V", forms(INGOT, DUST),
                MaterialProperties.builder().density(0.77).hardness(0.80).heatResistance(0.65));
        reg("titanium", MaterialType.ELEMENT, "Ti", forms(OXIDE, SPONGE, INGOT, PLATE),
                MaterialProperties.builder().density(0.57).hardness(0.70).heatResistance(0.70).conductivity(0.03));

        reg("ammonium_paratungstate", MaterialType.COMPOUND, "(NH4)10(H2W12O42)", forms(DUST),
                MaterialProperties.builder());

        reg("ferrochrome", MaterialType.ALLOY, "Fe-Cr-C", forms(INGOT),
                MaterialProperties.builder().hardness(0.85).heatResistance(0.65));
        reg("ferromanganese", MaterialType.ALLOY, "", forms(INGOT), MaterialProperties.builder().hardness(0.70));
        reg("ferronickel", MaterialType.ALLOY, "Fe-Ni", forms(INGOT),
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
        mineral(id, display, formula, commodity, MINERAL_FORMS, props);
    }

    private static void mineral(String id, String display, String formula, String commodity, MaterialForm[] forms,
                                MaterialProperties.Builder props) {
        MaterialRegistry.defineMineral(GROUP, id, display, formula, commodity, forms, props);
    }

    private static void reg(String id, MaterialType type, String formula, MaterialForm[] forms,
                            MaterialProperties.Builder props) {
        MaterialRegistry.define(GROUP, id, null, type, formula, forms, props);
    }
}
