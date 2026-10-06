package ai.gsmc.fundamentals.process;

import java.util.List;

public record ProcessingStep(
        ProcessingStage stage,
        MaterialRef input,
        MaterialRef output,
        List<MaterialRef> byproducts
) {
    public ProcessingStep {
        byproducts = List.copyOf(byproducts);
    }

    public Tier tier() {
        return stage.tier();
    }
}
