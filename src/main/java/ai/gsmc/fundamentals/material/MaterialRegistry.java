package ai.gsmc.fundamentals.material;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central, loader-agnostic registry of all {@link Material}s. Each commodity group registers its
 * own materials through its registrar (PLAN §4) during mod init; nothing here is tied to a
 * Minecraft registry yet.
 *
 * <p>Not thread-safe — all registration happens single-threaded during mod init.
 */
public final class MaterialRegistry {

    private static final Map<String, Material> BY_ID = new LinkedHashMap<>();

    private MaterialRegistry() {}

    /** Registers a material. Throws if the id is already taken (catches cross-group collisions). */
    public static Material register(Material material) {
        Material prev = BY_ID.putIfAbsent(material.id(), material);
        if (prev != null) {
            throw new IllegalStateException("Duplicate material id '" + material.id()
                    + "' (group '" + material.group() + "' vs existing group '" + prev.group()
                    + "'). Material ids must be unique across groups — see PLAN §4.");
        }
        return material;
    }

    /**
     * Builds and registers a material for {@code group}, with the standard form-derived tags
     * ({@link MaterialTags#standard}). A null {@code display} derives the name from the id.
     */
    public static Material define(String group, String id, String display, MaterialType type,
                                  String formula, MaterialForm[] forms,
                                  MaterialProperties.Builder props) {
        return register(Material.builder(id, type)
                .display(display)
                .formula(formula)
                .group(group)
                .properties(props)
                .forms(forms)
                .tags(MaterialTags.standard(id, forms))
                .build());
    }

    /** As {@link #define}, for an ore mineral tagged under the {@code commodity} it yields. */
    public static Material defineMineral(String group, String id, String display, String formula,
                                         String commodity, MaterialForm[] forms,
                                         MaterialProperties.Builder props) {
        return register(Material.builder(id, MaterialType.MINERAL)
                .display(display)
                .formula(formula)
                .group(group)
                .properties(props)
                .forms(forms)
                .tags(MaterialTags.mineral(id, commodity, forms))
                .build());
    }

    public static Material get(String id) {
        return BY_ID.get(id);
    }

    public static boolean contains(String id) {
        return BY_ID.containsKey(id);
    }

    public static Collection<Material> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    public static int size() {
        return BY_ID.size();
    }

    /** Count of materials registered under a given commodity group slug. */
    public static long countInGroup(String group) {
        return BY_ID.values().stream().filter(m -> m.group().equals(group)).count();
    }
}
