package ai.gsmc.fundamentals.separation;

import java.util.List;

// Written by tools/build_separation_data.py; edit the table there.
public final class Reagents {

    public enum Kind { LIQUOR, ORGANIC, ACID, GAS, WASTE, CRUDE, FOULED, PRECURSOR }

    public record Reagent(String id, int tint, Kind kind) {}

    public static final List<Reagent> ALL = List.of(
            new Reagent("rare_earth_liquor", 0xB9A8C8, Kind.LIQUOR),
            new Reagent("light_rare_earth_liquor", 0xB4A6CF, Kind.LIQUOR),
            new Reagent("heavy_rare_earth_liquor", 0xE2D6B0, Kind.LIQUOR),
            new Reagent("lanthanum_cerium_liquor", 0xDCE6EC, Kind.LIQUOR),
            new Reagent("praseodymium_neodymium_liquor", 0x9A8CC4, Kind.LIQUOR),
            new Reagent("samarium_europium_gadolinium_liquor", 0xE8DFB4, Kind.LIQUOR),
            new Reagent("europium_gadolinium_liquor", 0xE4DEDA, Kind.LIQUOR),
            new Reagent("terbium_to_lutetium_liquor", 0xE6D4C0, Kind.LIQUOR),
            new Reagent("terbium_dysprosium_liquor", 0xE6DFC2, Kind.LIQUOR),
            new Reagent("yttrium_heavies_liquor", 0xE4CFC6, Kind.LIQUOR),
            new Reagent("holmium_to_lutetium_liquor", 0xE5C8C0, Kind.LIQUOR),
            new Reagent("holmium_erbium_liquor", 0xE6C2C0, Kind.LIQUOR),
            new Reagent("thulium_ytterbium_lutetium_liquor", 0xDCE4D4, Kind.LIQUOR),
            new Reagent("ytterbium_lutetium_liquor", 0xDCE6EC, Kind.LIQUOR),
            new Reagent("lanthanum_liquor", 0xDCE6EC, Kind.LIQUOR),
            new Reagent("cerium_liquor", 0xDCE6EC, Kind.LIQUOR),
            new Reagent("praseodymium_liquor", 0xA8D6A0, Kind.LIQUOR),
            new Reagent("neodymium_liquor", 0x9C86CC, Kind.LIQUOR),
            new Reagent("samarium_liquor", 0xEFE6A8, Kind.LIQUOR),
            new Reagent("europium_liquor", 0xE8D8D8, Kind.LIQUOR),
            new Reagent("gadolinium_liquor", 0xDCE6EC, Kind.LIQUOR),
            new Reagent("terbium_liquor", 0xDCE6EC, Kind.LIQUOR),
            new Reagent("dysprosium_liquor", 0xEEE8B8, Kind.LIQUOR),
            new Reagent("holmium_liquor", 0xECE4B0, Kind.LIQUOR),
            new Reagent("erbium_liquor", 0xECB4BC, Kind.LIQUOR),
            new Reagent("thulium_liquor", 0xD4E8C8, Kind.LIQUOR),
            new Reagent("ytterbium_liquor", 0xDCE6EC, Kind.LIQUOR),
            new Reagent("lutetium_liquor", 0xDCE6EC, Kind.LIQUOR),
            new Reagent("yttrium_liquor", 0xDCE6EC, Kind.LIQUOR),
            new Reagent("scandium_liquor", 0xDCE6EC, Kind.LIQUOR),
            new Reagent("nickel_copper_sulfate", 0x58A890, Kind.LIQUOR),
            new Reagent("platinum_palladium_liquor", 0xC8701E, Kind.LIQUOR),
            new Reagent("palladium_liquor", 0xA8542A, Kind.LIQUOR),
            new Reagent("palladium_tetrammine_liquor", 0xE6E8E0, Kind.LIQUOR),
            new Reagent("iridium_rhodium_liquor", 0x6A2A1E, Kind.LIQUOR),
            new Reagent("rhodium_liquor", 0xC85A6A, Kind.LIQUOR),
            new Reagent("p204", 0xEAD88C, Kind.ORGANIC),
            new Reagent("p507", 0xECE0A8, Kind.ORGANIC),
            new Reagent("naphthenic_acid", 0xA8843C, Kind.ORGANIC),
            new Reagent("hydrochloric_acid", 0xE4EEF2, Kind.ACID),
            new Reagent("nitric_acid", 0xF0EDC8, Kind.ACID),
            new Reagent("phosphoric_acid", 0xE8ECE4, Kind.ACID),
            new Reagent("hydrofluoric_acid", 0xE6F0EA, Kind.ACID),
            new Reagent("aqua_regia", 0xE0662A, Kind.ACID),
            new Reagent("argon", 0xC8D8F0, Kind.GAS),
            new Reagent("chlorine", 0xD2E496, Kind.GAS),
            new Reagent("water_gas", 0xD8DCE0, Kind.GAS),
            new Reagent("ammonia", 0xE4ECF0, Kind.GAS),
            new Reagent("osmium_tetroxide", 0xF2E8A0, Kind.GAS),
            new Reagent("ruthenium_tetroxide", 0xF0A830, Kind.GAS),
            new Reagent("spent_liquor", 0x8E9A86, Kind.WASTE),
            new Reagent("brine", 0xDCE6E4, Kind.WASTE),
            new Reagent("crude_rare_earth_liquor", 0x8E7F86, Kind.CRUDE),
            new Reagent("crude_heavy_rare_earth_liquor", 0x9E9A80, Kind.CRUDE),
            new Reagent("fouled_p204", 0x6E5A38, Kind.FOULED),
            new Reagent("fouled_p507", 0x72603E, Kind.FOULED),
            new Reagent("fouled_naphthenic_acid", 0x5A4424, Kind.FOULED),
            new Reagent("ethylhexanol", 0xEEEEE6, Kind.PRECURSOR),
            new Reagent("phosphorus_trichloride", 0xECF0EC, Kind.PRECURSOR),
            new Reagent("d2ehpa", 0xF0E4B0, Kind.PRECURSOR),
            new Reagent("ehehpa", 0xF2EAC4, Kind.PRECURSOR),
            new Reagent("titanium_tetrachloride", 0xEEF0EA, Kind.PRECURSOR));

    private Reagents() {}
}
