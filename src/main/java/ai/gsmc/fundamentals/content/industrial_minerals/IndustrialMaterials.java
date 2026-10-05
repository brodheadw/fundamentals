package ai.gsmc.fundamentals.content.industrial_minerals;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;

/**
 * Materials for the {@code industrial_minerals} commodity group (Laptop B, PLAN §3): the
 * non-metal / bulk chemical-feedstock minerals and their solid products.
 *
 * <p><b>First cut — solids only.</b> The group's fluid-heavy chemistry (phosphoric/sulfuric acid,
 * chlor-alkali Cl₂/NaOH liquor, the Solvay loop) waits on a shared {@code FLUID} material form,
 * which A earmarked for the rare-earth solvent-extraction design — so it's added once, there.
 * Until then this covers the minerals and the dry products (silicon, soda ash, potash, phosphorus).
 */
public final class IndustrialMaterials {

    public static final String GROUP = "industrial_minerals";

    private static final MaterialForm[] MINERAL_FORMS = {MaterialForm.ORE, MaterialForm.RAW};

    private IndustrialMaterials() {}

    public static void register() {
        // --- Minerals (§2.4: tagged under the commodity they yield) ---
        mineral("quartz", "Quartz", "SiO2", "silica",
                MaterialProperties.builder().density(0.34).hardness(0.60));
        mineral("apatite", "Apatite", "Ca5(PO4)3F", "phosphate",
                MaterialProperties.builder().density(0.41));
        mineral("halite", "Halite", "NaCl", "salt",
                MaterialProperties.builder().density(0.28));
        mineral("sylvite", "Sylvite", "KCl", "potash",
                MaterialProperties.builder().density(0.25));
        mineral("gypsum", "Gypsum", "CaSO4·2H2O", "gypsum",
                MaterialProperties.builder().density(0.29));
        mineral("trona", "Trona", "Na3H(CO3)2·2H2O", "soda_ash",
                MaterialProperties.builder().density(0.27));
        mineral("graphite", "Graphite", "C", "graphite",
                MaterialProperties.builder().density(0.28).conductivity(0.10));

        // --- Dry products ---
        // Silicon: quartz -> carbothermic MG-Si -> Siemens polysilicon (the tech backbone).
        reg("silicon", MaterialType.ELEMENT, "Si",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().density(0.30).hardness(0.60).conductivity(0.01));
        reg("soda_ash", MaterialType.COMPOUND, "Na2CO3",
                forms(MaterialForm.DUST),
                MaterialProperties.builder().density(0.32));
        reg("potash", MaterialType.COMPOUND, "KCl",
                forms(MaterialForm.DUST),
                MaterialProperties.builder().density(0.25));
        reg("quicklime", MaterialType.COMPOUND, "CaO",
                forms(MaterialForm.DUST),
                MaterialProperties.builder().density(0.42));
        reg("phosphorus", MaterialType.ELEMENT, "P",
                forms(MaterialForm.DUST),
                MaterialProperties.builder().density(0.23).toxicity(0.30)); // white P is nasty
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
