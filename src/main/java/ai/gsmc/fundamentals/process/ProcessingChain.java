package ai.gsmc.fundamentals.process;

import ai.gsmc.fundamentals.material.Material;
import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public record ProcessingChain(String id, String commodity, String group, List<ProcessingStep> steps) {

    public ProcessingChain {
        steps = List.copyOf(steps);
    }

    public List<String> validate() {
        List<MaterialRef> refs = new ArrayList<>();
        for (ProcessingStep step : steps) {
            refs.add(step.input());
            refs.add(step.output());
            refs.addAll(step.byproducts());
        }
        List<String> errors = new ArrayList<>();
        for (MaterialRef ref : refs) {
            Material m = MaterialRegistry.get(ref.material());
            if (m == null) {
                errors.add(id + ": references unknown material '" + ref.material() + "'");
            } else if (!m.has(ref.form())) {
                errors.add(id + ": material '" + ref.material() + "' has no form " + ref.form());
            }
        }
        return errors;
    }

    public static Builder builder(String id, String commodity, String group) {
        return new Builder(id, commodity, group);
    }

    public static final class Builder {
        private final String id;
        private final String commodity;
        private final String group;
        private final List<ProcessingStep> steps = new ArrayList<>();

        private Builder(String id, String commodity, String group) {
            this.id = id;
            this.commodity = commodity;
            this.group = group;
        }

        public Builder step(ProcessingStage stage, MaterialRef in, MaterialRef out,
                            MaterialRef... byproducts) {
            steps.add(new ProcessingStep(stage, in, out, Arrays.asList(byproducts)));
            return this;
        }

        public Builder step(ProcessingStage stage, String inId, MaterialForm inForm,
                            String outId, MaterialForm outForm) {
            return step(stage, MaterialRef.of(inId, inForm), MaterialRef.of(outId, outForm));
        }

        public ProcessingChain build() {
            return new ProcessingChain(id, commodity, group, steps);
        }
    }
}
