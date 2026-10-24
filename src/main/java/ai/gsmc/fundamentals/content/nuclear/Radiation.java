package ai.gsmc.fundamentals.content.nuclear;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.nuclearteam.createnuclear.api.radiation.RadiationRegistry;

import java.util.Map;

// Create Nuclear's dose per item held, on its own scale: its raw uranium is 3, its yellowcake 4, its fuel rod 100.
public final class Radiation {

    // spent fuel is hotter than the rod it was: the fission products, not the uranium, are what burn
    private static final Map<String, Double> DOSE = Map.ofEntries(
            Map.entry("uraninite_ore", 3.0), Map.entry("raw_uraninite", 3.0), Map.entry("uraninite_dust", 2.0), Map.entry("yellowcake", 4.0),
            Map.entry("uranium_tetrafluoride", 2.0), Map.entry("uranium_dioxide", 2.0), Map.entry("uranium_fuel_pellet", 2.0),
            Map.entry("depleted_uranium_oxide", 1.0), Map.entry("thorium_dioxide", 1.0), Map.entry("monazite_residue_dust", 1.0),
            Map.entry("spent_fuel_rod", 200.0), Map.entry("cooled_spent_fuel", 40.0), Map.entry("vitrified_waste", 20.0), Map.entry("waste_canister", 60.0));

    private Radiation() {}

    public static void register() {
        DOSE.forEach((id, dose) -> RadiationRegistry.register().item(BuiltInRegistries.ITEM.get(Fundamentals.id(id))).value(dose).build());
    }

    public static double dose(ItemStack stack) {
        return RadiationRegistry.get(stack);
    }
}
