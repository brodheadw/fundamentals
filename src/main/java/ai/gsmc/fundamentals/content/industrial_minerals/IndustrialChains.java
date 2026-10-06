package ai.gsmc.fundamentals.content.industrial_minerals;

import ai.gsmc.fundamentals.process.ProcessingChain;
import ai.gsmc.fundamentals.process.ProcessingChainRegistry;

import static ai.gsmc.fundamentals.material.MaterialForm.*;
import static ai.gsmc.fundamentals.process.ProcessingStage.*;

public final class IndustrialChains {

    private IndustrialChains() {}

    public static void register() {
        ProcessingChainRegistry.register(ProcessingChain.builder("silicon", "silicon", IndustrialMaterials.GROUP)
                .step(CARBOTHERMIC_REDUCTION, "quartz", RAW, "silicon", INGOT)
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("soda_ash", "soda_ash", IndustrialMaterials.GROUP)
                .step(CALCINATION, "trona", RAW, "soda_ash", DUST)
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("potash", "potash", IndustrialMaterials.GROUP)
                .step(FROTH_FLOTATION, "sylvite", RAW, "potash", DUST)
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("phosphorus", "phosphorus", IndustrialMaterials.GROUP)
                .step(CARBOTHERMIC_REDUCTION, "apatite", RAW, "phosphorus", DUST)
                .build());
    }
}
