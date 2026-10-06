package ai.gsmc.fundamentals.material;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MaterialRegistry {

    private static final Map<String, Material> BY_ID = new LinkedHashMap<>();

    private MaterialRegistry() {}

    public static Material register(Material material) {
        Material prev = BY_ID.putIfAbsent(material.id(), material);
        if (prev != null) {
            throw new IllegalStateException("Duplicate material id '" + material.id()
                    + "' in groups '" + prev.group() + "' and '" + material.group() + "'");
        }
        return material;
    }

    public static Material define(String group, String id, String display, MaterialType type,
                                  String formula, MaterialForm[] forms,
                                  MaterialProperties.Builder props) {
        return define(group, id, display, type, formula, forms, props, MaterialTags.standard(id, forms));
    }

    public static Material defineMineral(String group, String id, String display, String formula,
                                         String commodity, MaterialForm[] forms,
                                         MaterialProperties.Builder props) {
        return define(group, id, display, MaterialType.MINERAL, formula, forms, props,
                MaterialTags.mineral(id, commodity, forms));
    }

    private static Material define(String group, String id, String display, MaterialType type,
                                   String formula, MaterialForm[] forms,
                                   MaterialProperties.Builder props, List<String> tags) {
        if (display == null) {
            display = Arrays.stream(id.split("_")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
        }
        return register(new Material(id, display, type, formula, group, props.build(),
                Sets.immutableEnumSet(Arrays.asList(forms)), tags));
    }

    public static Material get(String id) {
        return BY_ID.get(id);
    }

    public static Collection<Material> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}
