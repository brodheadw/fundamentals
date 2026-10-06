package ai.gsmc.fundamentals.material;

import java.util.ArrayList;
import java.util.List;

public final class MaterialTags {

    private MaterialTags() {}

    public static List<String> standard(String id, MaterialForm... forms) {
        return mineral(id, id, forms);
    }

    public static List<String> mineral(String id, String commodity, MaterialForm... forms) {
        List<String> tags = new ArrayList<>();
        for (MaterialForm form : forms) {
            switch (form) {
                case ORE -> tags.add("c:ores/" + commodity);
                case RAW -> tags.add("c:raw_materials/" + commodity);
                case INGOT -> tags.add("c:ingots/" + id);
                case DUST -> tags.add("c:dusts/" + id);
                case OXIDE -> tags.add("fundamentals:oxides/" + id);
                case CONCENTRATE -> tags.add("fundamentals:concentrates/" + id);
                default -> {}
            }
        }
        return List.copyOf(tags);
    }
}
