package ai.gsmc.fundamentals.content.nuclear;

import java.util.Map;

/** How hot each of our radioactive things is, on Create Nuclear's dose scale; kept apart so it reads without that mod. */
public final class Doses {

    // spent fuel is hotter than the rod it was: the fission products, not the uranium, are what burn
    public static final Map<String, Double> DOSE = Map.ofEntries(
            Map.entry("uraninite_ore", 3.0), Map.entry("raw_uraninite", 3.0), Map.entry("uraninite_dust", 2.0), Map.entry("yellowcake", 4.0),
            Map.entry("uranium_tetrafluoride", 2.0), Map.entry("uranium_dioxide", 2.0), Map.entry("uranium_fuel_pellet", 2.0),
            Map.entry("depleted_uranium_oxide", 1.0), Map.entry("thorium_dioxide", 1.0), Map.entry("monazite_residue_dust", 1.0),
            Map.entry("spent_fuel_rod", 200.0), Map.entry("cooled_spent_fuel", 40.0), Map.entry("vitrified_waste", 20.0), Map.entry("waste_canister", 60.0),
            Map.entry("highly_enriched_uranium", 3.0), Map.entry("plutonium_dioxide", 6.0), Map.entry("plutonium", 8.0),
            Map.entry("plutonium_pit", 10.0), Map.entry("irradiated_bismuth", 30.0), Map.entry("polonium", 30.0),
            Map.entry("urchin_initiator", 10.0), Map.entry("gun_type_bomb", 4.0), Map.entry("implosion_bomb", 4.0));

    private Doses() {}
}
