package ai.gsmc.fundamentals.content.base_metals;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;

import static ai.gsmc.fundamentals.material.MaterialForm.*;

public final class BaseMetalMaterials {

    public static final String GROUP = "base_metals";

    private static final MaterialForm[] MINERAL_FORMS = {ORE, RAW, DUST};
    private static final MaterialForm[] CONCENTRATE_FORMS = {CONCENTRATE};
    private static final MaterialForm[] METAL_FORMS = {INGOT, DUST, NUGGET, PLATE, BLOCK};
    private static final MaterialForm[] METAL_WITH_OXIDE_FORMS = {OXIDE, INGOT, DUST, NUGGET, PLATE, BLOCK};
    private static final MaterialForm[] TIN_FORMS = {INGOT, NUGGET, BLOCK};
    private static final MaterialForm[] BRONZE_FORMS = {INGOT, NUGGET, PLATE, BLOCK};

    private BaseMetalMaterials() {}

    public static void register() {
        // copper sulfides: float, then smelt
        mineral("chalcopyrite", "copper", "CuFeS2", 0.53, 0);
        mineral("bornite", "copper", "Cu5FeS4", 0.64, 0);
        mineral("chalcocite", "copper", "Cu2S", 0.71, 0);
        mineral("covellite", "copper", "CuS", 0.60, 0);

        // copper oxides and carbonates: smelt directly, or leach and electrowin
        mineral("malachite", "copper", "Cu2CO3(OH)2", 0.50, 0);
        mineral("azurite", "copper", "Cu3(CO3)2(OH)2", 0.48, 0);
        mineral("cuprite", "copper", "Cu2O", 0.78, 0);
        mineral("native_copper", "copper", "Cu", 1.13, 0);

        mineral("bauxite", "aluminum", "Al(OH)3 + AlO(OH)", 0.32, 0);
        mineral("galena", "lead", "PbS", 0.95, 0.40);
        mineral("sphalerite", "zinc", "(Zn,Fe)S", 0.51, 0);
        mineral("smithsonite", "zinc", "ZnCO3", 0.56, 0);
        mineral("hemimorphite", "zinc", "Zn4Si2O7(OH)2·H2O", 0.44, 0);
        mineral("cassiterite", "tin", "SnO2", 0.88, 0);

        reg("copper_concentrate", null, MaterialType.CONCENTRATE, "", CONCENTRATE_FORMS, MaterialProperties.builder());
        reg("lead_concentrate", null, MaterialType.CONCENTRATE, "", CONCENTRATE_FORMS,
                MaterialProperties.builder().toxicity(0.40));
        reg("zinc_concentrate", null, MaterialType.CONCENTRATE, "", CONCENTRATE_FORMS, MaterialProperties.builder());
        reg("tin_concentrate", null, MaterialType.CONCENTRATE, "", CONCENTRATE_FORMS, MaterialProperties.builder());

        reg("copper_matte", null, MaterialType.COMPOUND, "Cu2S·FeS",
                new MaterialForm[] {DUST}, MaterialProperties.builder());
        reg("nickel_matte", null, MaterialType.COMPOUND, "Ni3S2·Cu2S·FeS",
                new MaterialForm[] {DUST}, MaterialProperties.builder());
        reg("converter_matte", null, MaterialType.COMPOUND, "Ni3S2·Cu2S",
                new MaterialForm[] {DUST}, MaterialProperties.builder());
        reg("blister_copper", null, MaterialType.ALLOY, "", new MaterialForm[] {INGOT},
                MaterialProperties.builder().density(1.12).conductivity(0.60));
        reg("crude_tin", null, MaterialType.ALLOY, "", new MaterialForm[] {INGOT},
                MaterialProperties.builder().density(0.95).conductivity(0.13));

        reg("copper", null, MaterialType.ELEMENT, "Cu", METAL_FORMS,
                MaterialProperties.builder().density(1.14).conductivity(1.00).hardness(0.30));
        reg("aluminum", "Aluminium", MaterialType.ELEMENT, "Al", METAL_WITH_OXIDE_FORMS,
                MaterialProperties.builder().density(0.34).conductivity(0.61).hardness(0.25));
        reg("lead", null, MaterialType.ELEMENT, "Pb", METAL_WITH_OXIDE_FORMS,
                MaterialProperties.builder().density(1.44).conductivity(0.08).hardness(0.10).toxicity(0.60));
        reg("zinc", null, MaterialType.ELEMENT, "Zn", METAL_WITH_OXIDE_FORMS,
                MaterialProperties.builder().density(0.91).conductivity(0.28).hardness(0.25));
        reg("tin", null, MaterialType.ELEMENT, "Sn", TIN_FORMS,
                MaterialProperties.builder().density(0.93).conductivity(0.15).hardness(0.15));

        reg("bronze", null, MaterialType.ALLOY, "", BRONZE_FORMS,
                MaterialProperties.builder().density(1.12).conductivity(0.12).hardness(0.45));
        reg("brass", null, MaterialType.ALLOY, "", METAL_FORMS,
                MaterialProperties.builder().density(1.08).conductivity(0.28).hardness(0.40));
    }

    private static void mineral(String id, String commodity, String formula, double density, double toxicity) {
        MaterialRegistry.defineMineral(GROUP, id, null, formula, commodity, MINERAL_FORMS,
                MaterialProperties.builder().density(density).toxicity(toxicity));
    }

    private static void reg(String id, String display, MaterialType type, String formula,
                            MaterialForm[] forms, MaterialProperties.Builder props) {
        MaterialRegistry.define(GROUP, id, display, type, formula, forms, props);
    }
}
