package ai.gsmc.fundamentals.content.ferrous_ferroalloy;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.process.MaterialRef;
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

        // Ferrochrome: carbothermic smelt of chromite (the dominant chromium product).
        ProcessingChainRegistry.register(
                ProcessingChain.builder("ferrochrome", "ferrochrome", FerrousMaterials.GROUP)
                        .step(ProcessingStage.SMELTING, "chromite", MaterialForm.RAW, "ferrochrome", MaterialForm.INGOT)
                        .build());

        // Ferromanganese: carbothermic reduction of pyrolusite.
        ProcessingChainRegistry.register(
                ProcessingChain.builder("ferromanganese", "ferromanganese", FerrousMaterials.GROUP)
                        .step(ProcessingStage.CARBOTHERMIC_REDUCTION, "pyrolusite", MaterialForm.RAW, "ferromanganese", MaterialForm.INGOT)
                        .build());

        // Tungsten: wolframite → APT → WO3 → W powder. Scheelite converges on the same APT step.
        ProcessingChainRegistry.register(
                ProcessingChain.builder("tungsten", "tungsten", FerrousMaterials.GROUP)
                        .step(ProcessingStage.LEACHING, "wolframite", MaterialForm.RAW, "ammonium_paratungstate", MaterialForm.DUST)
                        .step(ProcessingStage.CALCINATION, "ammonium_paratungstate", MaterialForm.DUST, "tungsten_trioxide", MaterialForm.OXIDE)
                        .step(ProcessingStage.HYDROGEN_REDUCTION, "tungsten_trioxide", MaterialForm.OXIDE, "tungsten", MaterialForm.DUST)
                        .build());

        // Molybdenum: roast molybdenite (MoS2 → MoO3 → metal; oxide step abstracted for now).
        ProcessingChainRegistry.register(
                ProcessingChain.builder("molybdenum", "molybdenum", FerrousMaterials.GROUP)
                        .step(ProcessingStage.ROASTING, "molybdenite", MaterialForm.RAW, "molybdenum", MaterialForm.INGOT)
                        .build());

        // Nickel: smelt pentlandite; cobalt is the classic byproduct of the same sulfide ore.
        ProcessingChainRegistry.register(
                ProcessingChain.builder("nickel", "nickel", FerrousMaterials.GROUP)
                        .step(ProcessingStage.SMELTING,
                                MaterialRef.of("pentlandite", MaterialForm.RAW),
                                MaterialRef.of("nickel", MaterialForm.INGOT),
                                MaterialRef.of("cobalt", MaterialForm.INGOT))
                        .build());

        // Ferronickel: RKEF smelt of saprolitic nickel laterite.
        ProcessingChainRegistry.register(
                ProcessingChain.builder("ferronickel", "ferronickel", FerrousMaterials.GROUP)
                        .step(ProcessingStage.SMELTING, "nickel_laterite", MaterialForm.RAW, "ferronickel", MaterialForm.INGOT)
                        .build());

        // Titanium: Kroll route (rutile → TiCl4 → Mg reduction → sponge). TiCl4 intermediate
        // abstracted until a compound/fluid material exists.
        ProcessingChainRegistry.register(
                ProcessingChain.builder("titanium", "titanium", FerrousMaterials.GROUP)
                        .step(ProcessingStage.KROLL_PROCESS, "rutile", MaterialForm.RAW, "titanium", MaterialForm.INGOT)
                        .build());
    }
}
