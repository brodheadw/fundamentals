package ai.gsmc.fundamentals.material;

import java.util.List;
import java.util.Set;

public record Material(String id, String display, MaterialType type, String formula, String group,
                       MaterialProperties properties, Set<MaterialForm> forms, List<String> tags) {

    public boolean has(MaterialForm form) {
        return forms.contains(form);
    }
}
