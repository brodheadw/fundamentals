package ai.gsmc.fundamentals.separation;

import java.util.List;

// Written by tools/build_separation_data.py; edit the table there.
public final class Reagents {

    public enum Kind { LIQUOR, ORGANIC, ACID, GAS }

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
            new Reagent("p204", 0xD8B060, Kind.ORGANIC),
            new Reagent("p507", 0xC89440, Kind.ORGANIC),
            new Reagent("naphthenic_acid", 0x8A6A2E, Kind.ORGANIC),
            new Reagent("hydrochloric_acid", 0xE4EEF2, Kind.ACID),
            new Reagent("nitric_acid", 0xF0EDC8, Kind.ACID),
            new Reagent("phosphoric_acid", 0xE8ECE4, Kind.ACID),
            new Reagent("hydrofluoric_acid", 0xE6F0EA, Kind.ACID),
            new Reagent("argon", 0xC8D8F0, Kind.GAS));

    private Reagents() {}
}
