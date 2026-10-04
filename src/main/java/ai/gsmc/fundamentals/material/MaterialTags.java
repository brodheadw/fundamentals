package ai.gsmc.fundamentals.material;

import java.util.ArrayList;
import java.util.List;

/**
 * Derives the standard {@code c:} / {@code fundamentals:} tags (PLAN §4) from a material's forms,
 * so every group registrar tags the same way.
 */
public final class MaterialTags {

    private MaterialTags() {}

    public static String[] standard(String id, MaterialForm... forms) {
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
