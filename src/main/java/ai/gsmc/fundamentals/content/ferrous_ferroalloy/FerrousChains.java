package ai.gsmc.fundamentals.content.ferrous_ferroalloy;

import ai.gsmc.fundamentals.process.MaterialRef;
import ai.gsmc.fundamentals.process.ProcessingChain;
import ai.gsmc.fundamentals.process.ProcessingChainRegistry;

import static ai.gsmc.fundamentals.material.MaterialForm.*;
import static ai.gsmc.fundamentals.process.ProcessingStage.*;

public final class FerrousChains {

    private FerrousChains() {}

    public static void register() {
        ProcessingChainRegistry.register(ProcessingChain.builder("iron", "iron", FerrousMaterials.GROUP)
                .step(BLOOMERY, "hematite", RAW, "iron", INGOT)
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("steel", "steel", FerrousMaterials.GROUP)
                .step(CONVERTING, "iron", INGOT, "steel", INGOT)
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("ferrochrome", "ferrochrome", FerrousMaterials.GROUP)
                .step(SMELTING, "chromite", RAW, "ferrochrome", INGOT)
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("ferromanganese", "ferromanganese", FerrousMaterials.GROUP)
                .step(CARBOTHERMIC_REDUCTION, "pyrolusite", RAW, "ferromanganese", INGOT)
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("tungsten", "tungsten", FerrousMaterials.GROUP)
                .step(LEACHING, "wolframite", RAW, "ammonium_paratungstate", DUST)
                .step(CALCINATION, "ammonium_paratungstate", DUST, "tungsten_trioxide", OXIDE)
                .step(HYDROGEN_REDUCTION, "tungsten_trioxide", OXIDE, "tungsten", DUST)
                .build());

        // MoS2 -> MoO3 -> metal; the oxide step is skipped for now.
        ProcessingChainRegistry.register(ProcessingChain.builder("molybdenum", "molybdenum", FerrousMaterials.GROUP)
                .step(ROASTING, "molybdenite", RAW, "molybdenum", INGOT)
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("nickel", "nickel", FerrousMaterials.GROUP)
                .step(SMELTING, MaterialRef.of("pentlandite", RAW), MaterialRef.of("nickel", INGOT),
                        MaterialRef.of("cobalt", INGOT))
                .build());

        ProcessingChainRegistry.register(ProcessingChain.builder("ferronickel", "ferronickel", FerrousMaterials.GROUP)
                .step(SMELTING, "nickel_laterite", RAW, "ferronickel", INGOT)
                .build());

        // The Kroll route runs through TiCl4, skipped until there is a fluid form.
        ProcessingChainRegistry.register(ProcessingChain.builder("titanium", "titanium", FerrousMaterials.GROUP)
                .step(KROLL_PROCESS, "rutile", RAW, "titanium", INGOT)
                .build());
    }
}
