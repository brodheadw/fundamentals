package ai.gsmc.fundamentals.registry;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.content.ferrous_ferroalloy.FerrousChains;
import ai.gsmc.fundamentals.content.ferrous_ferroalloy.FerrousMaterials;
import ai.gsmc.fundamentals.content.rare_earths.RareEarthMaterials;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.process.ProcessingChainRegistry;
import ai.gsmc.fundamentals.process.ProcessingStage;

import java.util.List;

/**
 * Central content bootstrap. Each commodity group registers through its own registrar so the
 * two machines edit different files (PLAN §4); this class only lists the registrars to call.
 *
 * <p>When Laptop A adds a group, append one line here (e.g. {@code RareEarthMaterials.register()})
 * and note it in PLAN §6 — the only shared edit point, and only ever one line per group.
 */
public final class FundamentalsContent {

    private FundamentalsContent() {}

    public static void registerAll() {
        // --- Laptop B groups ---
        FerrousMaterials.register();
        FerrousChains.register();
        // IndustrialMinerals.register();   // TODO (B)
        // LightBatteryTech.register();     // TODO (B)
        // MinorSpecialty.register();       // TODO (B)
        // PreciousPgmMaterials.register(); // TODO (B)

        // --- Laptop A groups ---
        RareEarthMaterials.register();
        // BaseMetalMaterials.register();   // TODO (A)

        Fundamentals.LOGGER.info("Registered {} materials, {} processing chains; {} stages available.",
                MaterialRegistry.size(), ProcessingChainRegistry.size(), ProcessingStage.values().length);
        Fundamentals.LOGGER.info("  ferrous_ferroalloy: {} materials",
                MaterialRegistry.countInGroup(FerrousMaterials.GROUP));
        Fundamentals.LOGGER.info("  rare_earths: {} materials",
                MaterialRegistry.countInGroup(RareEarthMaterials.GROUP));

        // Fail loud in dev if a chain references a material/form that doesn't exist.
        List<String> problems = ProcessingChainRegistry.validateAll();
        if (!problems.isEmpty()) {
            problems.forEach(p -> Fundamentals.LOGGER.error("  invalid chain: {}", p));
            throw new IllegalStateException("Fundamentals: " + problems.size()
                    + " processing-chain validation error(s) — see log.");
        }
    }
}
