package ai.gsmc.fundamentals.content.precious_pgm;

import ai.gsmc.fundamentals.process.MaterialRef;
import ai.gsmc.fundamentals.process.ProcessingChain;
import ai.gsmc.fundamentals.process.ProcessingChainRegistry;

import static ai.gsmc.fundamentals.material.MaterialForm.*;
import static ai.gsmc.fundamentals.process.ProcessingStage.*;

public final class PreciousChains {

    private PreciousChains() {}

    public static void register() {
        ProcessingChainRegistry.register(ProcessingChain.builder("gold", "gold", PreciousMaterials.GROUP)
                .step(PANNING, "native_gold", RAW, "gold", INGOT)
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("silver", "silver", PreciousMaterials.GROUP)
                .step(SMELTING, "argentite", RAW, "silver", INGOT)
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("platinum", "platinum", PreciousMaterials.GROUP)
                .step(FROTH_FLOTATION, "sperrylite", RAW, "platinum_group_concentrate", CONCENTRATE)
                .step(SOLVENT_EXTRACTION, MaterialRef.of("platinum_group_concentrate", CONCENTRATE),
                        MaterialRef.of("platinum", INGOT), MaterialRef.of("palladium", INGOT), MaterialRef.of("rhodium", INGOT))
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("mercury", "mercury", PreciousMaterials.GROUP)
                .step(RETORTING, "cinnabar", RAW, "mercury", DUST)
                .build());
    }
}
