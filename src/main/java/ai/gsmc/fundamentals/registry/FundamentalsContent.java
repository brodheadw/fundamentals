package ai.gsmc.fundamentals.registry;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.content.ferrous_ferroalloy.FerrousMaterials;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.process.ProcessingStage;

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
        // IndustrialMinerals.register();   // TODO (B)
        // LightBatteryTech.register();     // TODO (B)
        // MinorSpecialty.register();       // TODO (B)

        // --- Laptop A groups ---
        // RareEarthMaterials.register();   // TODO (A)
        // BaseMetalMaterials.register();   // TODO (A)
        // PreciousPgmMaterials.register(); // TODO (A)

        Fundamentals.LOGGER.info("Registered {} materials across groups; {} processing stages available.",
                MaterialRegistry.size(), ProcessingStage.values().length);
        Fundamentals.LOGGER.info("  ferrous_ferroalloy: {} materials",
                MaterialRegistry.countInGroup(FerrousMaterials.GROUP));
    }
}
