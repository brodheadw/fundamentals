package ai.gsmc.fundamentals.material;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A material in the Fundamentals system: an element, compound, alloy, mineral or concentrate,
 * with {@link MaterialProperties stats}, the {@link MaterialForm forms} it exists as, and the
 * commodity {@code group} that owns it (see PLAN §3). Immutable; build with {@link #builder}.
 *
 * <p>Materials are loader-agnostic data. Turning a material's forms into registered
 * blocks/items is done later by the per-loader registration layer; this class carries only the
 * definition.
 */
public final class Material {

    private final String id;           // e.g. "iron", "ferrochrome", "tungsten_trioxide"
    private final String display;      // e.g. "Iron"
    private final MaterialType type;
    private final String formula;      // e.g. "Fe"; may be "" for alloys
    private final String group;        // commodity group slug, PLAN §3 (e.g. "ferrous_ferroalloy")
    private final MaterialProperties properties;
    private final Set<MaterialForm> forms;
    private final List<String> tags;

    private Material(Builder b) {
        this.id = b.id;
        this.display = b.display != null ? b.display : defaultDisplay(b.id);
        this.type = b.type;
        this.formula = b.formula;
        this.group = b.group;
        this.properties = b.properties;
        this.forms = Collections.unmodifiableSet(b.forms.isEmpty()
                ? EnumSet.noneOf(MaterialForm.class) : EnumSet.copyOf(b.forms));
        this.tags = List.copyOf(b.tags);
    }

    public String id() { return id; }
    public String display() { return display; }
    public MaterialType type() { return type; }
    public String formula() { return formula; }
    public String group() { return group; }
    public MaterialProperties properties() { return properties; }
    public Set<MaterialForm> forms() { return forms; }
    public List<String> tags() { return tags; }

    public boolean has(MaterialForm form) {
        return forms.contains(form);
    }

    private static String defaultDisplay(String id) {
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    public static Builder builder(String id, MaterialType type) {
        return new Builder(id, type);
    }

    @Override
    public String toString() {
        return "Material[" + id + " (" + type + ", " + group + "), forms=" + forms + "]";
    }

    public static final class Builder {
        private final String id;
        private final MaterialType type;
        private String display;
        private String formula = "";
        private String group = "uncategorized";
        private MaterialProperties properties = MaterialProperties.NONE;
        private final Set<MaterialForm> forms = EnumSet.noneOf(MaterialForm.class);
        private final java.util.List<String> tags = new java.util.ArrayList<>();

        private Builder(String id, MaterialType type) {
            this.id = id;
            this.type = type;
        }

        public Builder display(String v) { this.display = v; return this; }
        public Builder formula(String v) { this.formula = v; return this; }
        public Builder group(String v) { this.group = v; return this; }
        public Builder properties(MaterialProperties v) { this.properties = v; return this; }
        public Builder properties(MaterialProperties.Builder v) { this.properties = v.build(); return this; }

        public Builder forms(MaterialForm... v) {
            this.forms.addAll(Arrays.asList(v));
            return this;
        }

        public Builder tags(String... v) {
            this.tags.addAll(Arrays.asList(v));
            return this;
        }

        public Material build() {
            return new Material(this);
        }
    }
}
