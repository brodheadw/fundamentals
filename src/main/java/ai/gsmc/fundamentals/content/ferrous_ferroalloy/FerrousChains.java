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
 * <p>Per §2.4, chains start from an ore <b>mineral</b> (hematite), not from "iron ore", and
 * iron's first route is the primitive <b>bloomery</b> (T0, no machines).
 */
public final class FerrousChains {

    private FerrousChains() {}

    public static void register() {
        // Iron, primitive route: raw hematite + charcoal -> bloomery -> wrought iron ingot.
        // (T0 early-game route; a later blast-furnace chain will give molten pig iron.)
        ProcessingChainRegistry.register(
                ProcessingChain.builder("iron", "iron", FerrousMaterials.GROUP)
                        .step(ProcessingStage.BLOOMERY, "hematite", MaterialForm.RAW, "iron", MaterialForm.INGOT)
                        .build());

        // Steel: iron ingot -> steel ingot (converting / decarburisation; carbon feed is a
        // tagged input supplied later, c:dusts/carbon from the industrial_minerals graphite line).
        ProcessingChainRegistry.register(
                ProcessingChain.builder("steel", "steel", FerrousMaterials.GROUP)
                        .step(ProcessingStage.CONVERTING, "iron", MaterialForm.INGOT, "steel", MaterialForm.INGOT)
                        .build());
    }
}
