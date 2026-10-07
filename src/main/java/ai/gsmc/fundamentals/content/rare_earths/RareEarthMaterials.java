package ai.gsmc.fundamentals.content.rare_earths;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;

import static ai.gsmc.fundamentals.material.MaterialForm.*;

public final class RareEarthMaterials {

    public static final String GROUP = "rare_earths";

    private static final MaterialForm[] MINERAL_FORMS = {ORE, RAW, DUST};
    private static final MaterialForm[] ELEMENT_FORMS = {OXALATE, OXIDE, INGOT, DUST};
    private static final MaterialForm[] MAGNET_ELEMENT_FORMS = {OXALATE, OXIDE, INGOT, DUST, NUGGET, BLOCK};
    // Scandium is not in the chloride liquors, so it has no oxalate.
    private static final MaterialForm[] SCANDIUM_FORMS = {OXIDE, INGOT, DUST};
    private static final MaterialForm[] ALLOY_FORMS = {INGOT, DUST, NUGGET, PLATE, BLOCK};

    private RareEarthMaterials() {}

    public static void register() {
        mineral("bastnasite", "Bastnäsite", "(Ce,La,Nd)CO3F", MINERAL_FORMS, 0.63, 0);
        mineral("monazite", null, "(Ce,La,Nd,Th)PO4", MINERAL_FORMS, 0.65, 0.30);
        mineral("xenotime", null, "YPO4", MINERAL_FORMS, 0.60, 0.10);
        mineral("ion_adsorption_clay", "Ion-Adsorption Clay", "", new MaterialForm[] {ORE, RAW}, 0.33, 0);
        mineral("loparite", null, "(Na,Ca,Ce)(Ti,Nb,Ta)O3", MINERAL_FORMS, 0.61, 0.15);
        mineral("euxenite", null, "(Y,Ca,Ce,U,Th)(Nb,Ta,Ti)2O6", MINERAL_FORMS, 0.64, 0.35);

        reg("light_rare_earth_concentrate", null, MaterialType.CONCENTRATE, "",
                new MaterialForm[] {CONCENTRATE}, MaterialProperties.builder());
        reg("heavy_rare_earth_concentrate", null, MaterialType.CONCENTRATE, "",
                new MaterialForm[] {CONCENTRATE}, MaterialProperties.builder());

        // light
        element("lanthanum", "La", ELEMENT_FORMS, 0.78);
        element("cerium", "Ce", ELEMENT_FORMS, 0.86);
        element("praseodymium", "Pr", MAGNET_ELEMENT_FORMS, 0.86);
        element("neodymium", "Nd", MAGNET_ELEMENT_FORMS, 0.89);
        element("samarium", "Sm", MAGNET_ELEMENT_FORMS, 0.96);
        element("europium", "Eu", ELEMENT_FORMS, 0.67);

        // heavy, plus Y and Sc, which separate with them
        element("gadolinium", "Gd", ELEMENT_FORMS, 1.00);
        element("terbium", "Tb", MAGNET_ELEMENT_FORMS, 1.05);
        element("dysprosium", "Dy", MAGNET_ELEMENT_FORMS, 1.09);
        element("holmium", "Ho", ELEMENT_FORMS, 1.12);
        element("erbium", "Er", ELEMENT_FORMS, 1.15);
        element("thulium", "Tm", ELEMENT_FORMS, 1.18);
        element("ytterbium", "Yb", ELEMENT_FORMS, 0.88);
        element("lutetium", "Lu", ELEMENT_FORMS, 1.25);
        element("yttrium", "Y", ELEMENT_FORMS, 0.57);
        element("scandium", "Sc", SCANDIUM_FORMS, 0.38);

        reg("didymium", null, MaterialType.ALLOY, "", new MaterialForm[] {OXIDE, INGOT, DUST},
                MaterialProperties.builder().density(0.88));
        // Strongest magnet, but loses coercivity when hot; SmCo trades strength for heat.
        reg("neodymium_iron_boron", "NdFeB", MaterialType.ALLOY, "Nd2Fe14B", ALLOY_FORMS,
                MaterialProperties.builder().density(0.95).magnetStrength(1.00).heatResistance(0.30).hardness(0.60));
        reg("samarium_cobalt", "SmCo", MaterialType.ALLOY, "SmCo5", ALLOY_FORMS,
                MaterialProperties.builder().density(1.06).magnetStrength(0.70).heatResistance(0.80).hardness(0.55));
    }

    private static void mineral(String id, String display, String formula, MaterialForm[] forms,
                                double density, double radioactivity) {
        MaterialRegistry.defineMineral(GROUP, id, display, formula, "rare_earth", forms,
                MaterialProperties.builder().density(density).radioactivity(radioactivity));
    }

    private static void element(String id, String symbol, MaterialForm[] forms, double density) {
        reg(id, null, MaterialType.ELEMENT, symbol, forms, MaterialProperties.builder().density(density));
    }

    private static void reg(String id, String display, MaterialType type, String formula,
                            MaterialForm[] forms, MaterialProperties.Builder props) {
        MaterialRegistry.define(GROUP, id, display, type, formula, forms, props);
    }
}
