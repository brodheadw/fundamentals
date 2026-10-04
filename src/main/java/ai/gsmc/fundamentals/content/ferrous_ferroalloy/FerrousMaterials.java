package ai.gsmc.fundamentals.content.ferrous_ferroalloy;

import ai.gsmc.fundamentals.material.Material;
import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialProperties;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.material.MaterialTags;
import ai.gsmc.fundamentals.material.MaterialType;

/**
 * Materials for the {@code ferrous_ferroalloy} commodity group (Laptop B, PLAN §3):
 * iron + the ferroalloy metals, their key intermediates/alloys, and the ore minerals they come
 * from.
 *
 * <p>Follows the §2.4 ruling: <b>only minerals exist in the ground</b> ({@link MaterialType#MINERAL}
 * carrying {@code ORE}/{@code RAW} and the commodity tag, e.g. hematite → {@code c:ores/iron});
 * <b>elements have no {@code ORE}/{@code RAW}</b>, only products. Vanilla iron items stay canonical
 * — the registration layer maps iron's INGOT/NUGGET/BLOCK to {@code minecraft:*} rather than
 * registering duplicates.
 *
 * <p>Property stats lean into the magnet theme where it's physically real: iron, cobalt and nickel
 * are the three ferromagnetic elements, and cobalt's high Curie point gives it the heat resistance
 * SmCo relies on — so even B's metals feed A's magnet work through tags.
 */
public final class FerrousMaterials {

    public static final String GROUP = "ferrous_ferroalloy";

    private FerrousMaterials() {}

    public static void register() {
        // --- Ore minerals (the only things generated in-world; carry the commodity tag) ---
        mineral("hematite", "Hematite", "Fe2O3", "iron",
                MaterialProperties.builder().density(0.66).magnetStrength(0.05));
        mineral("magnetite", "Magnetite", "Fe3O4", "iron",
                MaterialProperties.builder().density(0.66).magnetStrength(0.20)); // lodestone mineral
        mineral("goethite", "Goethite", "FeO(OH)", "iron",
                MaterialProperties.builder().density(0.48));
        mineral("pyrolusite", "Pyrolusite", "MnO2", "manganese",
                MaterialProperties.builder().density(0.63));
        mineral("pentlandite", "Pentlandite", "(Fe,Ni)9S8", "nickel",
                MaterialProperties.builder().density(0.61));
        mineral("nickel_laterite", "Nickel Laterite", "(Fe,Ni)O(OH)", "nickel",
                MaterialProperties.builder().density(0.40));

        // --- Metals (products only — no ORE/RAW per §2.4). Vanilla iron is canonical. ---
        reg("iron", MaterialType.ELEMENT, "Fe",
                forms(MaterialForm.INGOT, MaterialForm.DUST, MaterialForm.NUGGET,
                        MaterialForm.PLATE, MaterialForm.BLOCK),
                MaterialProperties.builder().density(1.0).magnetStrength(0.40)
                        .heatResistance(0.45).conductivity(0.17).hardness(0.40));
        reg("steel", MaterialType.ALLOY, "",
                forms(MaterialForm.INGOT, MaterialForm.DUST, MaterialForm.NUGGET,
                        MaterialForm.PLATE, MaterialForm.BLOCK),
                MaterialProperties.builder().density(1.0).magnetStrength(0.35)
                        .heatResistance(0.60).hardness(0.70));
        reg("chromium", MaterialType.ELEMENT, "Cr",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().density(0.92).hardness(0.90).heatResistance(0.70));
        reg("manganese", MaterialType.ELEMENT, "Mn",
                forms(MaterialForm.INGOT, MaterialForm.DUST),
                MaterialProperties.builder().density(0.95).hardness(0.75));
        reg("nickel", MaterialType.ELEMENT, "Ni",
                forms(MaterialForm.INGOT, MaterialForm.DUST, MaterialForm.NUGGET),
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

    /** A product material; standard form-derived tags via the shared {@link MaterialTags}. */
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
