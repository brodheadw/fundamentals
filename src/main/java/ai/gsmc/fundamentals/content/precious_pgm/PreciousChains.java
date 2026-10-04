package ai.gsmc.fundamentals.content.precious_pgm;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.process.MaterialRef;
import ai.gsmc.fundamentals.process.ProcessingChain;
import ai.gsmc.fundamentals.process.ProcessingChainRegistry;
import ai.gsmc.fundamentals.process.ProcessingStage;

/**
 * Processing chains for {@code precious_pgm}. Spans the full difficulty range: gold's trivial
 * T0 panning route up to the deep PGM refining chain (flotation → solvent-extraction split into
 * individual platinum-group metals).
 */
public final class PreciousChains {

    private PreciousChains() {}

    public static void register() {
        // Gold, primitive route: pan native gold, melt to ingot (no chemistry; §2.4 T0).
        ProcessingChainRegistry.register(
                ProcessingChain.builder("gold", "gold", PreciousMaterials.GROUP)
                        .step(ProcessingStage.PANNING, "native_gold", MaterialForm.RAW, "gold", MaterialForm.INGOT)
                        .build());

        // Silver from argentite: roast-smelt to metal (cupellation refines Ag from Pb byproduct
        // streams, handled where galena is processed).
        ProcessingChainRegistry.register(
                ProcessingChain.builder("silver", "silver", PreciousMaterials.GROUP)
                        .step(ProcessingStage.SMELTING, "argentite", MaterialForm.RAW, "silver", MaterialForm.INGOT)
                        .build());

        // PGMs: flotation to a combined concentrate, then the solvent-extraction cascade splits
        // out the individual metals (endgame T5). Pd and Rh fall out as byproducts of the same run.
        ProcessingChainRegistry.register(
                ProcessingChain.builder("platinum", "platinum", PreciousMaterials.GROUP)
                        .step(ProcessingStage.FROTH_FLOTATION,
                                MaterialRef.of("sperrylite", MaterialForm.RAW),
                                MaterialRef.of("platinum_group_concentrate", MaterialForm.CONCENTRATE))
                        .step(ProcessingStage.SOLVENT_EXTRACTION,
                                MaterialRef.of("platinum_group_concentrate", MaterialForm.CONCENTRATE),
                                MaterialRef.of("platinum", MaterialForm.INGOT),
                                MaterialRef.of("palladium", MaterialForm.INGOT),
                                MaterialRef.of("rhodium", MaterialForm.INGOT))
                        .build());

        // Mercury: retort-distil cinnabar; the vapour condenses to liquid metal.
        ProcessingChainRegistry.register(
                ProcessingChain.builder("mercury", "mercury", PreciousMaterials.GROUP)
                        .step(ProcessingStage.RETORTING, "cinnabar", MaterialForm.RAW, "mercury", MaterialForm.DUST)
                        .build());
    }
}
