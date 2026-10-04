package ai.gsmc.fundamentals.process;

import ai.gsmc.fundamentals.material.Material;
import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.material.MaterialRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An ordered sequence of {@link ProcessingStep}s taking a commodity from its ore to a final
 * product — the in-code form of a chain's {@code stages[]} in {@code data/ores.json}.
 *
 * <p>Build with {@link #builder}. Call {@link #validate} after registration to catch references
 * to materials/forms that don't exist (the chain model is only as correct as the materials it
 * points at).
 */
public final class ProcessingChain {

    private final String id;          // e.g. "iron", "steel"
    private final String commodity;   // material id of the final product
    private final String group;       // owning commodity group slug (PLAN §3)
    private final List<ProcessingStep> steps;

    private ProcessingChain(Builder b) {
        this.id = b.id;
        this.commodity = b.commodity;
        this.group = b.group;
        this.steps = List.copyOf(b.steps);
    }

    public String id() { return id; }
    public String commodity() { return commodity; }
    public String group() { return group; }
    public List<ProcessingStep> steps() { return steps; }

    /** Highest tier any step in this chain requires (its gating tier). */
    public Tier maxTier() {
        Tier max = Tier.T0;
        for (ProcessingStep s : steps) {
            if (s.tier().ordinal() > max.ordinal()) max = s.tier();
        }
        return max;
    }

    /** Returns a list of human-readable problems; empty = valid. */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        List<MaterialRef> refs = new ArrayList<>();
        for (ProcessingStep step : steps) {
            if (step.stage() == null) errors.add(id + ": step has null stage");
            refs.add(step.input());
            refs.add(step.output());
            refs.addAll(step.byproducts());
        }
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

        /** Convenience: a step whose input/output are forms of materials by id. */
        public Builder step(ProcessingStage stage, String inId, MaterialForm inForm,
                            String outId, MaterialForm outForm) {
            return step(stage, MaterialRef.of(inId, inForm), MaterialRef.of(outId, outForm));
        }

        public ProcessingChain build() {
            return new ProcessingChain(this);
        }
    }
}
