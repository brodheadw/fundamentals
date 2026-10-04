package ai.gsmc.fundamentals.content.precious_pgm;

import ai.gsmc.fundamentals.material.Material;
import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialTags;
import ai.gsmc.fundamentals.material.MaterialType;

/**
 * Materials for the {@code precious_pgm} commodity group (Laptop B, PLAN §3 after the A→B flip):
 * gold, silver, the six platinum-group metals and mercury, with their ore minerals.
 *
 * <p>Follows §2.4: minerals carry {@code ORE}/{@code RAW} + the commodity tag; elements are
 * products only. <b>Gold is the exception</b> — native gold is a real mineral, so {@code
 * native_gold} and the {@code gold} metal's ingot/nugget/block map to the <b>vanilla</b> gold
 * ore and items (the registration layer maps the forms to {@code minecraft:*}; nothing duplicate
 * is registered, and vanilla gold ore keeps generating).
 *
 * <p>Silver and the PGMs are heavily byproduct-driven in reality (silver from Pb/Zn/Cu, PGMs from
 * Ni-Cu sulfide smelting): those cross-group byproducts arrive as tags from their owners (PLAN §3).
 */
public final class PreciousMaterials {

    public static final String GROUP = "precious_pgm";

    private PreciousMaterials() {}

    public static void register() {
        // --- Ore minerals ---
        // native_gold maps to VANILLA gold ore / raw gold (stays, §2.4) — defined so chains and
        // c:ores/gold have a referent; the registration layer resolves it to minecraft:*.
        mineral("native_gold", "Native Gold", "Au", "gold",
                MaterialProperties.builder().density(2.45));
        mineral("native_silver", "Native Silver", "Ag", "silver",
                MaterialProperties.builder().density(1.33));
        mineral("argentite", "Argentite", "Ag2S", "silver",
                MaterialProperties.builder().density(0.92));
        mineral("sperrylite", "Sperrylite", "PtAs2", "platinum",
                MaterialProperties.builder().density(1.40).toxicity(0.20)); // As-bearing
        mineral("cooperite", "Cooperite", "PtS", "platinum",
                MaterialProperties.builder().density(1.26));
        mineral("braggite", "Braggite", "(Pt,Pd,Ni)S", "platinum",
                MaterialProperties.builder().density(1.26));
        mineral("cinnabar", "Cinnabar", "HgS", "mercury",
                MaterialProperties.builder().density(1.03).toxicity(0.60));

        // --- Beneficiation intermediate (PGM ores concentrate together) ---
        reg("platinum_group_concentrate", MaterialType.CONCENTRATE, "",
                forms(MaterialForm.CONCENTRATE),
                MaterialProperties.builder());

        // --- Coinage metals (gold maps to vanilla items) ---
        reg("gold", MaterialType.ELEMENT, "Au",
                forms(MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.BLOCK,
                        MaterialForm.DUST, MaterialForm.PLATE),
                MaterialProperties.builder().density(2.45).conductivity(0.76).hardness(0.25)
                        .heatResistance(0.30));
        reg("silver", MaterialType.ELEMENT, "Ag",
                forms(MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.BLOCK,
                        MaterialForm.DUST, MaterialForm.PLATE),
                MaterialProperties.builder().density(1.33).conductivity(1.05).hardness(0.25));

        // --- Platinum-group metals (refined individually from the concentrate) ---
        reg("platinum", MaterialType.ELEMENT, "Pt",
                forms(MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.DUST),
                MaterialProperties.builder().density(2.73).hardness(0.35).heatResistance(0.80)
                        .conductivity(0.16));
        reg("palladium", MaterialType.ELEMENT, "Pd",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().density(1.52).hardness(0.40).heatResistance(0.60));
        reg("rhodium", MaterialType.ELEMENT, "Rh",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().density(1.58).hardness(0.60).heatResistance(0.85)
                        .conductivity(0.38));
        reg("ruthenium", MaterialType.ELEMENT, "Ru",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().density(1.56).hardness(0.70).heatResistance(0.80));
        reg("iridium", MaterialType.ELEMENT, "Ir",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().density(2.86).hardness(0.70).heatResistance(0.90));
        reg("osmium", MaterialType.ELEMENT, "Os",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().density(2.87).hardness(0.80).heatResistance(0.88)
                        .toxicity(0.30)); // OsO4 is toxic

        // --- Mercury: liquid metal (a proper FLUID form is pending in the shared schema; DUST
        // stands in for now). Highly toxic. ---
        reg("mercury", MaterialType.ELEMENT, "Hg",
                forms(MaterialForm.DUST),
                MaterialProperties.builder().density(1.72).toxicity(0.90));

        // --- Natural alloy ---
        reg("electrum", MaterialType.ALLOY, "(Au,Ag)",
                forms(MaterialForm.INGOT, MaterialForm.NUGGET),
                MaterialProperties.builder().density(1.90).conductivity(0.80).hardness(0.30));
    }

    private static MaterialForm[] forms(MaterialForm... f) {
        return f;
    }

    /** An ore mineral: MINERAL type, ORE+RAW forms, tagged under its {@code commodity} (§2.4). */
    private static void mineral(String id, String display, String formula, String commodity,
                                MaterialProperties.Builder props) {
        MaterialRegistry.register(Material.builder(id, MaterialType.MINERAL)
                .display(display)
                .formula(formula)
                .group(GROUP)
                .properties(props)
                .forms(MaterialForm.ORE, MaterialForm.RAW)
                .tags("c:ores/" + commodity, "c:raw_materials/" + commodity)
                .build());
    }

    private static void reg(String id, MaterialType type, String formula, MaterialForm[] forms,
                            MaterialProperties.Builder props) {
        MaterialRegistry.register(Material.builder(id, type)
                .formula(formula)
                .group(GROUP)
                .properties(props)
                .forms(forms)
                .tags(MaterialTags.standard(id, forms))
                .build());
    }
}
