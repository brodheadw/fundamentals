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
                .step(BLOOMERY, "galena", RAW, "lead_bullion", INGOT)
                .step(CUPELLATION, MaterialRef.of("lead_bullion", INGOT), MaterialRef.of("silver", INGOT), MaterialRef.of("lead", OXIDE))
                .step(CUPELLATION, MaterialRef.of("argentite", RAW), MaterialRef.of("silver", INGOT), MaterialRef.of("lead", OXIDE))
                .step(CUPELLATION, MaterialRef.of("native_silver", RAW), MaterialRef.of("silver", INGOT), MaterialRef.of("lead", OXIDE))
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("platinum", "platinum", PreciousMaterials.GROUP)
                .step(SMELTING, "pentlandite", RAW, "nickel_matte", DUST)
                .step(CONVERTING, "nickel_matte", DUST, "converter_matte", DUST)
                .step(LEACHING, "converter_matte", DUST, "platinum_group_concentrate", CONCENTRATE)
                .step(DISSOLUTION, MaterialRef.of("platinum_group_concentrate", CONCENTRATE), MaterialRef.of("platinum", SPONGE),
                        MaterialRef.of("palladium", SPONGE), MaterialRef.of("rhodium", SPONGE), MaterialRef.of("ruthenium", SPONGE),
                        MaterialRef.of("iridium", SPONGE), MaterialRef.of("osmium", SPONGE))
                .step(SINTERING, "platinum", SPONGE, "platinum", INGOT)
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("mercury", "mercury", PreciousMaterials.GROUP)
                .step(RETORTING, "cinnabar", RAW, "mercury", DUST)
                .build());
    }
}
