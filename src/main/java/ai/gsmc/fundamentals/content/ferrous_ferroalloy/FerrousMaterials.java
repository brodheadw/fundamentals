package ai.gsmc.fundamentals.content.ferrous_ferroalloy;

import ai.gsmc.fundamentals.material.Material;
import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialType;

import java.util.ArrayList;
import java.util.List;

/**
 * Materials for the {@code ferrous_ferroalloy} commodity group (Laptop B, PLAN §3):
 * iron + the ferroalloy metals and their key intermediates/alloys.
 *
 * <p>Property stats lean into the mod's magnet theme where it's physically real: iron, cobalt
 * and nickel are the three ferromagnetic elements, and cobalt's high Curie point gives it the
 * heat resistance that SmCo magnets rely on — so even B's metals feed A's magnet work through
 * tags.
 *
 * <p>Only this group's materials are defined here; never touch another group's content (PLAN §4).
 */
public final class FerrousMaterials {

    public static final String GROUP = "ferrous_ferroalloy";

    private FerrousMaterials() {}

    public static void register() {
        // --- Iron & steel ---
        reg("iron", MaterialType.ELEMENT, "Fe",
                forms(MaterialForm.ORE, MaterialForm.RAW, MaterialForm.INGOT, MaterialForm.DUST,
                        MaterialForm.NUGGET, MaterialForm.PLATE, MaterialForm.BLOCK),
                MaterialProperties.builder().density(1.0).magnetStrength(0.40)
                        .heatResistance(0.45).conductivity(0.17).hardness(0.40));
        reg("steel", MaterialType.ALLOY, "",
                forms(MaterialForm.INGOT, MaterialForm.DUST, MaterialForm.NUGGET,
                        MaterialForm.PLATE, MaterialForm.BLOCK),
                MaterialProperties.builder().density(1.0).magnetStrength(0.35)
                        .heatResistance(0.60).hardness(0.70));

        // --- Ferroalloy elements ---
        reg("chromium", MaterialType.ELEMENT, "Cr",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().density(0.92).hardness(0.90).heatResistance(0.70));
        reg("manganese", MaterialType.ELEMENT, "Mn",
                forms(MaterialForm.ORE, MaterialForm.RAW, MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().density(0.95).hardness(0.75));
        reg("nickel", MaterialType.ELEMENT, "Ni",
                forms(MaterialForm.ORE, MaterialForm.RAW, MaterialForm.INGOT, MaterialForm.DUST,
                        MaterialForm.NUGGET),
                MaterialProperties.builder().density(1.13).magnetStrength(0.30)
                        .conductivity(0.25).hardness(0.50));
        reg("cobalt", MaterialType.ELEMENT, "Co",
                forms(MaterialForm.INGOT, MaterialForm.DUST, MaterialForm.NUGGET),
                MaterialProperties.builder().density(1.13).magnetStrength(0.45)
                        .heatResistance(0.85).hardness(0.55));
        reg("molybdenum", MaterialType.ELEMENT, "Mo",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().density(1.30).heatResistance(0.88).hardness(0.80));
        reg("tungsten", MaterialType.ELEMENT, "W",
                forms(MaterialForm.INGOT, MaterialForm.DUST, MaterialForm.PLATE),
                MaterialProperties.builder().density(1.90).heatResistance(1.00).hardness(0.95)
                        .conductivity(0.30));
        reg("vanadium", MaterialType.ELEMENT, "V",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().density(0.77).hardness(0.80).heatResistance(0.65));
        reg("titanium", MaterialType.ELEMENT, "Ti",
                forms(MaterialForm.INGOT, MaterialForm.DUST, MaterialForm.PLATE),
                MaterialProperties.builder().density(0.57).hardness(0.70).heatResistance(0.70)
                        .conductivity(0.03));

        // --- Oxide / salt intermediates ---
        reg("tungsten_trioxide", MaterialType.COMPOUND, "WO3",
                forms(MaterialForm.OXIDE, MaterialForm.DUST),
                MaterialProperties.builder());
        reg("ammonium_paratungstate", MaterialType.COMPOUND, "(NH4)10(H2W12O42)",
                forms(MaterialForm.DUST),
                MaterialProperties.builder());

        // --- Ferroalloys (smelter products) ---
        reg("ferrochrome", MaterialType.ALLOY, "",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().hardness(0.85).heatResistance(0.65));
        reg("ferromanganese", MaterialType.ALLOY, "",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().hardness(0.70));
        reg("ferronickel", MaterialType.ALLOY, "",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().magnetStrength(0.25).hardness(0.55));
        reg("ferromolybdenum", MaterialType.ALLOY, "",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().heatResistance(0.80).hardness(0.80));
        reg("ferrotungsten", MaterialType.ALLOY, "",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().heatResistance(0.90).hardness(0.90));
        reg("ferrovanadium", MaterialType.ALLOY, "",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().hardness(0.80));
    }

    private static MaterialForm[] forms(MaterialForm... f) {
        return f;
    }

    /** Registers one material, auto-deriving the standard c:/fundamentals tags from its forms. */
    private static void reg(String id, MaterialType type, String formula, MaterialForm[] forms,
                            MaterialProperties.Builder props) {
        MaterialRegistry.register(Material.builder(id, type)
                .formula(formula)
                .group(GROUP)
                .properties(props)
                .forms(forms)
                .tags(tagsFor(id, forms))
                .build());
    }

    private static String[] tagsFor(String id, MaterialForm[] forms) {
        List<String> tags = new ArrayList<>();
        for (MaterialForm form : forms) {
            switch (form) {
                case ORE -> tags.add("c:ores/" + id);
                case RAW -> tags.add("c:raw_materials/" + id);
                case INGOT -> tags.add("c:ingots/" + id);
                case DUST -> tags.add("c:dusts/" + id);
                case OXIDE -> tags.add("fundamentals:oxides/" + id);
                case CONCENTRATE -> tags.add("fundamentals:concentrates/" + id);
                default -> { /* nugget/plate/block: item tags added with their recipes later */ }
            }
        }
        return tags.toArray(new String[0]);
    }
}
