package ai.gsmc.fundamentals.process;

import java.util.List;

/**
 * One step of a {@link ProcessingChain}: a {@link ProcessingStage} machine transforming an
 * input into a primary output, optionally emitting byproducts (which may belong to another
 * commodity group — those are consumed via tags by their owner, PLAN §3).
 *
 * @param stage      the machine/stage that performs this step (determines required {@link Tier})
 * @param input      what goes in
 * @param output     the primary product
 * @param byproducts additional recovered materials (may be empty)
 */
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
