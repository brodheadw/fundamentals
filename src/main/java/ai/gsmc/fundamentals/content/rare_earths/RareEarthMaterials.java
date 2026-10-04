package ai.gsmc.fundamentals.content.rare_earths;

import ai.gsmc.fundamentals.material.Material;
import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialTags;
import ai.gsmc.fundamentals.material.MaterialType;

/**
 * Materials for the {@code rare_earths} commodity group (Laptop A, PLAN §3): the REE ore
 * minerals, the mixed concentrates they beneficiate to, the sixteen separable elements, and the
 * magnet alloys.
 *
 * <p>Every ore yields a <em>mixed</em> concentrate; individual elements only exist after the
 * solvent-extraction cascade. An element's {@link MaterialForm#OXIDE} form is its separated
 * oxide (Nd2O3, Pr6O11, Tb4O7, CeO2, …), the product the cascade delivers before reduction.
 *
 * <p>Thorium, uranium, boron, cobalt, iron and Ti/Nb/Ta belong to Laptop B's groups — the ores
 * here only emit those as byproduct tags, never define them (PLAN §3 byproduct rule).
 */
public final class RareEarthMaterials {

    public static final String GROUP = "rare_earths";

    private static final MaterialForm[] MINERAL_FORMS =
            {MaterialForm.ORE, MaterialForm.RAW, MaterialForm.DUST};
    private static final MaterialForm[] ELEMENT_FORMS =
            {MaterialForm.OXIDE, MaterialForm.INGOT, MaterialForm.DUST};
    private static final MaterialForm[] MAGNET_ELEMENT_FORMS =
            {MaterialForm.OXIDE, MaterialForm.INGOT, MaterialForm.DUST, MaterialForm.NUGGET,
                    MaterialForm.BLOCK};
    private static final MaterialForm[] ALLOY_FORMS =
            {MaterialForm.INGOT, MaterialForm.DUST, MaterialForm.NUGGET, MaterialForm.PLATE,
                    MaterialForm.BLOCK};

    private RareEarthMaterials() {}

    public static void register() {
        // --- Ore minerals ---
        reg("bastnasite", "Bastnäsite", MaterialType.MINERAL, "(Ce,La,Nd)CO3F", MINERAL_FORMS,
                MaterialProperties.builder().density(0.63));
        reg("monazite", null, MaterialType.MINERAL, "(Ce,La,Nd,Th)PO4", MINERAL_FORMS,
                MaterialProperties.builder().density(0.65).radioactivity(0.30));
        reg("xenotime", null, MaterialType.MINERAL, "YPO4", MINERAL_FORMS,
                MaterialProperties.builder().density(0.60).radioactivity(0.10));
        reg("ion_adsorption_clay", "Ion-Adsorption Clay", MaterialType.MINERAL, "",
                new MaterialForm[] {MaterialForm.ORE, MaterialForm.RAW},
                MaterialProperties.builder().density(0.33));
        reg("loparite", null, MaterialType.MINERAL, "(Na,Ca,Ce)(Ti,Nb,Ta)O3", MINERAL_FORMS,
                MaterialProperties.builder().density(0.61).radioactivity(0.15));
        reg("euxenite", null, MaterialType.MINERAL, "(Y,Ca,Ce,U,Th)(Nb,Ta,Ti)2O6", MINERAL_FORMS,
                MaterialProperties.builder().density(0.64).radioactivity(0.35));

        // --- Mixed concentrates (pre-separation) ---
        reg("light_rare_earth_concentrate", null, MaterialType.CONCENTRATE, "",
                new MaterialForm[] {MaterialForm.CONCENTRATE}, MaterialProperties.builder());
        reg("heavy_rare_earth_concentrate", null, MaterialType.CONCENTRATE, "",
                new MaterialForm[] {MaterialForm.CONCENTRATE}, MaterialProperties.builder());

        // --- Light REEs ---
        element("lanthanum", "La", ELEMENT_FORMS, 0.78);
        element("cerium", "Ce", ELEMENT_FORMS, 0.86);
        element("praseodymium", "Pr", MAGNET_ELEMENT_FORMS, 0.86);
        element("neodymium", "Nd", MAGNET_ELEMENT_FORMS, 0.89);
        element("samarium", "Sm", MAGNET_ELEMENT_FORMS, 0.96);
        element("europium", "Eu", ELEMENT_FORMS, 0.67);

        // --- Heavy REEs (plus Y and Sc, which separate with them) ---
        element("gadolinium", "Gd", ELEMENT_FORMS, 1.00);
        element("terbium", "Tb", MAGNET_ELEMENT_FORMS, 1.05);
        element("dysprosium", "Dy", MAGNET_ELEMENT_FORMS, 1.09);
        element("holmium", "Ho", ELEMENT_FORMS, 1.12);
        element("erbium", "Er", ELEMENT_FORMS, 1.15);
        element("thulium", "Tm", ELEMENT_FORMS, 1.18);
        element("ytterbium", "Yb", ELEMENT_FORMS, 0.88);
        element("lutetium", "Lu", ELEMENT_FORMS, 1.25);
        element("yttrium", "Y", ELEMENT_FORMS, 0.57);
        element("scandium", "Sc", ELEMENT_FORMS, 0.38);

        // --- Alloys ---
        reg("didymium", null, MaterialType.ALLOY, "", new MaterialForm[] {MaterialForm.OXIDE,
                        MaterialForm.INGOT, MaterialForm.DUST},
                MaterialProperties.builder().density(0.88));
        // Strongest magnet, but loses coercivity when hot; SmCo trades strength for heat.
        reg("neodymium_iron_boron", "NdFeB", MaterialType.ALLOY, "Nd2Fe14B", ALLOY_FORMS,
                MaterialProperties.builder().density(0.95).magnetStrength(1.00)
                        .heatResistance(0.30).hardness(0.60));
        reg("samarium_cobalt", "SmCo", MaterialType.ALLOY, "SmCo5", ALLOY_FORMS,
                MaterialProperties.builder().density(1.06).magnetStrength(0.70)
                        .heatResistance(0.80).hardness(0.55));
    }

    private static void element(String id, String symbol, MaterialForm[] forms, double density) {
        reg(id, null, MaterialType.ELEMENT, symbol, forms,
                MaterialProperties.builder().density(density));
    }

    private static void reg(String id, String display, MaterialType type, String formula,
                            MaterialForm[] forms, MaterialProperties.Builder props) {
        MaterialRegistry.register(Material.builder(id, type)
                .display(display)
                .formula(formula)
                .group(GROUP)
                .properties(props)
                .forms(forms)
                .tags(MaterialTags.standard(id, forms))
                .build());
    }
}
