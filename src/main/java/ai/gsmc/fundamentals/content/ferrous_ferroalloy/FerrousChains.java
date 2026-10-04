package ai.gsmc.fundamentals.content.ferrous_ferroalloy;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.process.ProcessingChain;
import ai.gsmc.fundamentals.process.ProcessingChainRegistry;
import ai.gsmc.fundamentals.process.ProcessingStage;

/**
 * Processing chains for the {@code ferrous_ferroalloy} group (Laptop B). Encodes the
 * researched {@code data/ores.json} stage lists as validated {@link ProcessingChain}s, using
 * the materials registered by {@link FerrousMaterials}.
 *
 * <p>Simplified to commodity level for gameplay (one "iron ore" rather than hematite vs
 * magnetite); specific ore minerals become ore-block variants later.
 */
public final class FerrousChains {

    private FerrousChains() {}

    public static void register() {
        // Iron: ore -> raw -> dust -> ingot (comminution then carbothermic smelt).
        ProcessingChainRegistry.register(
                ProcessingChain.builder("iron", "iron", FerrousMaterials.GROUP)
                        .step(ProcessingStage.CRUSHING, "iron", MaterialForm.ORE, "iron", MaterialForm.RAW)
                        .step(ProcessingStage.GRINDING, "iron", MaterialForm.RAW, "iron", MaterialForm.DUST)
                        .step(ProcessingStage.CARBOTHERMIC_REDUCTION, "iron", MaterialForm.DUST, "iron", MaterialForm.INGOT)
                        .build());

        // Steel: iron ingot -> steel ingot (converting / decarburisation; carbon feed is a
        // tagged input supplied later, c:dusts/carbon from the industrial_minerals graphite line).
        ProcessingChainRegistry.register(
                ProcessingChain.builder("steel", "steel", FerrousMaterials.GROUP)
                        .step(ProcessingStage.CONVERTING, "iron", MaterialForm.INGOT, "steel", MaterialForm.INGOT)
                        .build());
    }
}
