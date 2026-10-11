package ai.gsmc.fundamentals.content.nuclear;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.nuclearteam.createnuclear.api.radiation.RadiationRegistry;

import java.util.Map;

// Create Nuclear's dose per item held, on its own scale: its raw uranium is 3, its yellowcake 4, its fuel rod 100.
public final class Radiation {

    private static final Map<String, Double> DOSE = Map.of(
            "uraninite_ore", 3.0, "raw_uraninite", 3.0, "uraninite_dust", 2.0, "yellowcake", 4.0, "uranium_tetrafluoride", 2.0,
            "uranium_dioxide", 2.0, "uranium_fuel_pellet", 2.0, "depleted_uranium_oxide", 1.0, "thorium_dioxide", 1.0, "monazite_residue_dust", 1.0);

    private Radiation() {}

    public static void register() {
        DOSE.forEach((id, dose) -> RadiationRegistry.register().item(BuiltInRegistries.ITEM.get(Fundamentals.id(id))).value(dose).build());
    }

    public static double dose(ItemStack stack) {
        return RadiationRegistry.get(stack);
    }
}
