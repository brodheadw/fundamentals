#!/usr/bin/env python3
import gzip
import json
import math
import re
import shutil
import zipfile
from pathlib import Path

import common
from build_chemica_compat import fluid_ingredient as fluid
from common import ASSETS, DATA, JAVA, RESOURCES, drop_self, ns, read_lang, result, result_fluid, write, write_lang
from paint_materials import MATERIALS
from paint_separation import CREATE_JAR

REAGENTS_JAVA = JAVA / "separation/Reagents.java"
PARAMAGNETISM = JAVA / "separation/Paramagnetism.java"
RECIPES = DATA / "recipe"

VAT = {
    "wall": 1,
    "floor": 1,
    "rim_clearance": 5,
    "weir_below_rim": 3,
    "window": [4, 12],
}

CLEAR = 0xDCE6EC
LIQUORS = {
    "rare_earth_liquor": ("Rare Earth Liquor", 0xB9A8C8),
    "light_rare_earth_liquor": ("Light Rare Earth Liquor", 0xB4A6CF),
    "heavy_rare_earth_liquor": ("Heavy Rare Earth Liquor", 0xE2D6B0),
    "lanthanum_cerium_liquor": ("Lanthanum-Cerium Liquor", CLEAR),
    "praseodymium_neodymium_liquor": ("Didymium Liquor", 0x9A8CC4),
    "samarium_europium_gadolinium_liquor": ("Samarium-Europium-Gadolinium Liquor", 0xE8DFB4),
    "europium_gadolinium_liquor": ("Europium-Gadolinium Liquor", 0xE4DEDA),
    "terbium_to_lutetium_liquor": ("Terbium-to-Lutetium Liquor", 0xE6D4C0),
    "terbium_dysprosium_liquor": ("Terbium-Dysprosium Liquor", 0xE6DFC2),
    "yttrium_heavies_liquor": ("Yttrium and Late Heavies Liquor", 0xE4CFC6),
    "holmium_to_lutetium_liquor": ("Holmium-to-Lutetium Liquor", 0xE5C8C0),
    "holmium_erbium_liquor": ("Holmium-Erbium Liquor", 0xE6C2C0),
    "thulium_ytterbium_lutetium_liquor": ("Thulium-Ytterbium-Lutetium Liquor", 0xDCE4D4),
    "ytterbium_lutetium_liquor": ("Ytterbium-Lutetium Liquor", CLEAR),
    "lanthanum_liquor": ("Lanthanum Liquor", CLEAR),
    "cerium_liquor": ("Cerium Liquor", CLEAR),
    "praseodymium_liquor": ("Praseodymium Liquor", 0xA8D6A0),
    "neodymium_liquor": ("Neodymium Liquor", 0x9C86CC),
    "samarium_liquor": ("Samarium Liquor", 0xEFE6A8),
    "europium_liquor": ("Europium Liquor", 0xE8D8D8),
    "gadolinium_liquor": ("Gadolinium Liquor", CLEAR),
    "terbium_liquor": ("Terbium Liquor", CLEAR),
    "dysprosium_liquor": ("Dysprosium Liquor", 0xEEE8B8),
    "holmium_liquor": ("Holmium Liquor", 0xECE4B0),
    "erbium_liquor": ("Erbium Liquor", 0xECB4BC),
    "thulium_liquor": ("Thulium Liquor", 0xD4E8C8),
    "ytterbium_liquor": ("Ytterbium Liquor", CLEAR),
    "lutetium_liquor": ("Lutetium Liquor", CLEAR),
    "yttrium_liquor": ("Yttrium Liquor", CLEAR),
    "scandium_liquor": ("Scandium Liquor", CLEAR),
}
ORGANICS = {
    "p204": ("P204", 0xEAD88C),
    "p507": ("P507", 0xECE0A8),
    "naphthenic_acid": ("Naphthenic Acid", 0xA8843C),
}
ACIDS = {
    "hydrochloric_acid": ("Hydrochloric Acid", 0xE4EEF2),
    "nitric_acid": ("Nitric Acid", 0xF0EDC8),
    "phosphoric_acid": ("Phosphoric Acid", 0xE8ECE4),
    "hydrofluoric_acid": ("Hydrofluoric Acid", 0xE6F0EA),
    "aqua_regia": ("Aqua Regia", 0xE0662A),
    "bromine": ("Bromine", 0x7A1E0E),
}
WASTES = {
    "spent_liquor": ("Spent Liquor", 0x8E9A86),
    "calcium_chloride_liquor": ("Calcium Chloride Liquor", 0xDCE6E4),
}
SEA = {"seawater": ("Seawater", 0x3F76E4)}
SALINES = {"bittern": ("Bittern", 0xE6DEB8), "salt_brine": ("Salt Brine", 0xE2ECEE)}
CRUDES = {
    "crude_rare_earth_liquor": ("Crude Rare Earth Liquor", 0x8E7F86),
    "crude_heavy_rare_earth_liquor": ("Crude Heavy Rare Earth Liquor", 0x9E9A80),
}
FOULED = {
    "fouled_p204": ("Fouled P204", 0x6E5A38),
    "fouled_p507": ("Fouled P507", 0x72603E),
    "fouled_naphthenic_acid": ("Fouled Naphthenic Acid", 0x5A4424),
}
PRECURSORS = {
    "ethylhexanol": ("2-Ethylhexanol", 0xEEEEE6),
    "phosphorus_trichloride": ("Phosphorus Trichloride", 0xECF0EC),
    "d2ehpa": ("D2EHPA", 0xF0E4B0),
    "ehehpa": ("EHEHPA", 0xF2EAC4),
    "titanium_tetrachloride": ("Titanium Tetrachloride", 0xEEF0EA),
    "vinyl_chloride": ("Vinyl Chloride", 0xEEF0EE),
}
GASES = {
    "argon": ("Argon", 0xC8D8F0),
    "chlorine": ("Chlorine", 0xD2E496),
    "water_gas": ("Water Gas", 0xD8DCE0),
    "ammonia": ("Ammonia", 0xE4ECF0),
    "osmium_tetroxide": ("Osmium Tetroxide", 0xF2E8A0),
    "ruthenium_tetroxide": ("Ruthenium Tetroxide", 0xF0A830),
    "nickel_carbonyl": ("Nickel Carbonyl", 0xEEEEDC),
}
PLATINUM_LIQUORS = {
    "nickel_copper_sulfate": ("Nickel-Copper Sulfate Liquor", 0x58A890),
    "nickel_sulfate_liquor": ("Nickel Sulfate Liquor", 0x4CA060),
    "cobalt_chloride_liquor": ("Cobalt Chloride Liquor", 0xD25A78),
    "platinum_palladium_liquor": ("Platinum-Palladium Liquor", 0xC8701E),
    "palladium_liquor": ("Palladium Liquor", 0xA8542A),
    "palladium_tetrammine_liquor": ("Palladium Tetrammine Liquor", 0xE6E8E0),
    "iridium_rhodium_liquor": ("Iridium-Rhodium Liquor", 0x6A2A1E),
    "rhodium_liquor": ("Rhodium Liquor", 0xC85A6A),
}
SULFATE_LIQUORS = {"beryllium_sulfate_liquor": ("Beryllium Sulfate Liquor", CLEAR), "lithium_sulfate_liquor": ("Lithium Sulfate Liquor", CLEAR)}
CAUSTICS = {"caustic_soda": ("Caustic Soda", 0xEEF2F0), "sodium_aluminate_liquor": ("Sodium Aluminate Liquor", 0xB8864A),
            "sodium_tungstate_liquor": ("Sodium Tungstate Liquor", 0xE6ECE8)}
FLUIDS = {**{k: (*v, "LIQUOR") for k, v in {**LIQUORS, **PLATINUM_LIQUORS, **SULFATE_LIQUORS}.items()}, **{k: (*v, "ORGANIC") for k, v in ORGANICS.items()},
          **{k: (*v, "ACID") for k, v in ACIDS.items()}, **{k: (*v, "GAS") for k, v in GASES.items()},
          **{k: (*v, "WASTE") for k, v in WASTES.items()}, **{k: (*v, "SALINE") for k, v in SALINES.items()}, **{k: (*v, "WATER") for k, v in SEA.items()}, **{k: (*v, "CRUDE") for k, v in CRUDES.items()},
          **{k: (*v, "FOULED") for k, v in FOULED.items()}, **{k: (*v, "PRECURSOR") for k, v in PRECURSORS.items()},
          **{k: (*v, "CAUSTIC") for k, v in CAUSTICS.items()}}

ELECTROLYSIS = ("lanthanum", "cerium", "praseodymium", "neodymium", "didymium")
CALCIOTHERMIC = ("gadolinium", "terbium", "dysprosium", "yttrium", "scandium")
LANTHANOTHERMIC = ("samarium",)
OXALATE_FROM = {"didymium": "praseodymium_neodymium_liquor"}

CUTS = [
    ("rare_earth_liquor", "p507", 8, "light_rare_earth_liquor", "heavy_rare_earth_liquor"),
    ("light_rare_earth_liquor", "p507", 14, "lanthanum_cerium_liquor", "praseodymium_neodymium_liquor"),
    ("lanthanum_cerium_liquor", "p507", 12, "lanthanum_liquor", "cerium_liquor"),
    ("praseodymium_neodymium_liquor", "p507", 32, "praseodymium_liquor", "neodymium_liquor"),
    ("heavy_rare_earth_liquor", "p204", 10, "samarium_europium_gadolinium_liquor", "terbium_to_lutetium_liquor"),
    ("samarium_europium_gadolinium_liquor", "p507", 16, "samarium_liquor", "europium_gadolinium_liquor"),
    ("terbium_to_lutetium_liquor", "p204", 12, "terbium_dysprosium_liquor", "yttrium_heavies_liquor"),
    ("terbium_dysprosium_liquor", "p507", 20, "terbium_liquor", "dysprosium_liquor"),
    ("yttrium_heavies_liquor", "naphthenic_acid", 14, "yttrium_liquor", "holmium_to_lutetium_liquor"),
    ("holmium_to_lutetium_liquor", "p204", 10, "holmium_erbium_liquor", "thulium_ytterbium_lutetium_liquor"),
    ("holmium_erbium_liquor", "p507", 20, "holmium_liquor", "erbium_liquor"),
    ("thulium_ytterbium_lutetium_liquor", "p204", 12, "thulium_liquor", "ytterbium_lutetium_liquor"),
    ("ytterbium_lutetium_liquor", "p507", 20, "ytterbium_liquor", "lutetium_liquor"),
]
MONAZITE = {"La": 23.9, "Ce": 46.0, "Pr": 5.0, "Nd": 17.4, "Sm": 2.53, "Eu": 0.05, "Gd": 1.49, "Tb": 0.04, "Dy": 0.69,
            "Ho": 0.05, "Er": 0.21, "Tm": 0.01, "Yb": 0.12, "Lu": 0.04, "Y": 2.41}
ION_CLAY = {"Sm": 2.8, "Eu": 0.1, "Gd": 6.9, "Tb": 1.3, "Dy": 6.7, "Ho": 1.6, "Er": 4.9, "Tm": 0.7, "Yb": 2.5, "Lu": 0.4, "Y": 64.9}
CARRIES = {
    "rare_earth_liquor": tuple(MONAZITE),
    "light_rare_earth_liquor": ("La", "Ce", "Pr", "Nd"),
    "heavy_rare_earth_liquor": tuple(ION_CLAY),
    "lanthanum_cerium_liquor": ("La", "Ce"),
    "praseodymium_neodymium_liquor": ("Pr", "Nd"),
    "samarium_europium_gadolinium_liquor": ("Sm", "Eu", "Gd"),
    "europium_gadolinium_liquor": ("Eu", "Gd"),
    "terbium_to_lutetium_liquor": ("Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu", "Y"),
    "terbium_dysprosium_liquor": ("Tb", "Dy"),
    "yttrium_heavies_liquor": ("Y", "Ho", "Er", "Tm", "Yb", "Lu"),
    "holmium_to_lutetium_liquor": ("Ho", "Er", "Tm", "Yb", "Lu"),
    "holmium_erbium_liquor": ("Ho", "Er"),
    "thulium_ytterbium_lutetium_liquor": ("Tm", "Yb", "Lu"),
    "ytterbium_lutetium_liquor": ("Yb", "Lu"),
    **{f"{e}_liquor": (s,) for e, s in (("lanthanum", "La"), ("cerium", "Ce"), ("praseodymium", "Pr"), ("neodymium", "Nd"),
                                        ("samarium", "Sm"), ("europium", "Eu"), ("gadolinium", "Gd"), ("terbium", "Tb"),
                                        ("dysprosium", "Dy"), ("holmium", "Ho"), ("erbium", "Er"), ("thulium", "Tm"),
                                        ("ytterbium", "Yb"), ("lutetium", "Lu"), ("yttrium", "Y"))},
}
ZINC_REDUCTION = ("europium_gadolinium_liquor", "europium_sulfate", "europium_liquor", "gadolinium_liquor")
LIGHT_BRANCH = ("rare_earth_liquor", "light_rare_earth_liquor", "lanthanum_cerium_liquor", "praseodymium_neodymium_liquor")
LEAST = 0.05


def light_fraction(liquor, light, heavy):
    feed = MONAZITE if liquor in LIGHT_BRANCH else ION_CLAY
    assert set(CARRIES[light]) | set(CARRIES[heavy]) == set(CARRIES[liquor]), liquor
    share = sum(feed[e] for e in CARRIES[light]) / sum(feed[e] for e in CARRIES[liquor])
    return min(1 - LEAST, max(LEAST, round(share * 20) / 20))


MOMENTS = {"La": 0, "Ce": 2.5, "Pr": 3.6, "Nd": 3.6, "Sm": 1.5, "Eu": 3.4, "Gd": 7.9, "Tb": 9.7, "Dy": 10.6, "Ho": 10.6,
           "Er": 9.6, "Tm": 7.6, "Yb": 4.5, "Lu": 0, "Y": 0}
WEAK = 0.1
STRAY = 0.05
PASS_MOMENT = 16
CURIE = {e: float(v) for e, v in re.findall(r'"(\w+)", ([0-9.]+)', re.search(r"CURIE = Map\.of\(([^)]*)\)", PARAMAGNETISM.read_text(encoding="utf-8")).group(1))}


def magnetic_cut(liquor, stages, light, heavy):
    feed = MONAZITE if liquor in LIGHT_BRANCH else ION_CLAY

    def chi(side, curie=False):
        return sum(feed[e] * MOMENTS[e] ** 2 * (CURIE.get(e, 1.0) if curie else 1) for e in CARRIES[side]) / sum(feed[e] for e in CARRIES[side])

    (drawn, strong), (left, weak) = sorted(((light, chi(light)), (heavy, chi(heavy))), key=lambda kv: -kv[1])
    if strong == 0 or weak > WEAK * strong:
        return None
    middle = (strong + weak) / 2
    stray = sum(feed[e] for e in CARRIES[drawn] if MOMENTS[e] ** 2 < middle) + sum(feed[e] for e in CARRIES[left] if MOMENTS[e] ** 2 > middle)
    if stray > STRAY * sum(feed[e] for e in CARRIES[liquor]):
        return None
    passes = math.ceil(PASS_MOMENT / math.sqrt(strong - weak))
    assert passes <= 0.6 * stages, liquor
    return drawn, passes, round((chi(drawn, True) - chi(left, True)) / (strong - weak), 2)


MAGNETIC = {liquor: cut for liquor, _, stages, light, heavy in CUTS if (cut := magnetic_cut(liquor, stages, light, heavy))}


STRIP = "hydrochloric_acid"
PLANT_ITEMS = {"salt": "Salt", "oxalic_acid": "Oxalic Acid", "roasted_bastnasite": "Roasted Bastnäsite", "light_rare_earth_sulfate": "Light Rare Earth Sulfate",
               "heavy_rare_earth_sulfate": "Heavy Rare Earth Sulfate", "calcium_chloride": "Calcium Chloride", "calcium_ingot": "Calcium Ingot",
               "white_phosphorus": "White Phosphorus", "light_rare_earth_carbonate": "Light Rare Earth Carbonate",
               "heavy_rare_earth_carbonate": "Heavy Rare Earth Carbonate", "cerium_concentrate": "Cerium Concentrate", "europium_sulfate": "Europium Sulfate"}


def item(id, count=1):
    out = {"item": ns(id)}
    return out if count == 1 else [out] * count


def mixing(name, ingredients, results, heated=False):
    common.mixing(RECIPES / f"mixing/{name}.json", ingredients, results, "heated" if heated else None)


def java_table():
    lines = ["package ai.gsmc.fundamentals.separation;", "", "import java.util.List;", "",
             "// Written by tools/build_separation_data.py; edit the table there.",
             "public final class Reagents {", "",
             "    public enum Kind { LIQUOR, ORGANIC, ACID }", "",
             "    public record Reagent(String id, int tint, Kind kind) {}", "",
             "    public static final List<Reagent> ALL = List.of("]
    entries = [f'            new Reagent("{id}", 0x{tint:06X}, Kind.{kind})' for id, (_, tint, kind) in FLUIDS.items()]
    lines += [",\n".join(entries) + ");", "", "    private Reagents() {}", "}", ""]
    kinds = sorted({kind for _, _, kind in FLUIDS.values()}, key=["LIQUOR", "ORGANIC", "ACID", "GAS", "WASTE", "SALINE", "WATER", "CRUDE", "FOULED", "PRECURSOR", "CAUSTIC"].index)
    lines[7] = "    public enum Kind { " + ", ".join(kinds) + " }"
    REAGENTS_JAVA.write_text("\n".join(lines), encoding="utf-8")


DISSOLVES = {
    "hydrochloric_acid": ["minecraft:calcite", "minecraft:dripstone_block", "minecraft:pointed_dripstone", "minecraft:bone_block", "create:limestone"],
    "hydrofluoric_acid": ["#c:glass_blocks", "#c:glass_panes", "#minecraft:sand", "minecraft:sandstone", "minecraft:red_sandstone", "minecraft:quartz_block", "minecraft:smooth_quartz",
                          "minecraft:tuff"],
    "nitric_acid": ["#c:storage_blocks/copper", "#c:storage_blocks/iron", "minecraft:copper_block", "minecraft:iron_block", "minecraft:cut_copper"],
    "phosphoric_acid": [],
    "aqua_regia": ["#c:storage_blocks/gold", "minecraft:gold_block", "minecraft:raw_gold_block", "#c:storage_blocks/copper", "#c:storage_blocks/iron",
                   "minecraft:copper_block", "minecraft:iron_block", "minecraft:cut_copper"],
    "bromine": ["#c:storage_blocks/aluminum", "#c:storage_blocks/copper", "#c:storage_blocks/iron", "minecraft:copper_block", "minecraft:iron_block",
                "minecraft:cut_copper"],
}


ACID_PROOF = [{"tag": "fundamentals:plastic_sheets"}, {"tag": "c:plates/stainless_steel"}]


def acids():
    for acid, eats in DISSOLVES.items():
        write(ASSETS / f"blockstates/{acid}.json", {"variants": {"": {"model": f"fundamentals:block/{acid}"}}})
        write(ASSETS / f"models/block/{acid}.json", {"textures": {"particle": "fundamentals:block/fluid/liquor_still"}})
        write(ASSETS / f"models/item/{acid}_bucket.json", {"parent": "neoforge:item/bucket", "loader": "neoforge:fluid_container", "fluid": f"fundamentals:{acid}"})
        values = [{"id": v, "required": False} if ":" in v and not v.startswith("#") and not v.startswith("minecraft:") else v for v in eats]
        write(DATA / f"tags/block/dissolves/{acid}.json", {"replace": False, "values": values})
    write(ASSETS / "models/item/seawater_bucket.json", {"parent": "neoforge:item/bucket", "loader": "neoforge:fluid_container", "fluid": "fundamentals:seawater"})


def chemistry():
    mixing("salt", [fluid("seawater", 1000)], [result("salt", 2), result_fluid("bittern", 100), {"id": "minecraft:water", "amount": 900}], heated=True)
    mixing("hydrochloric_acid", item("salt", 2) + [fluid("tfmg:sulfuric_acid", 500)], [result_fluid("hydrochloric_acid", 500)], heated=True)
    mixing("nitric_acid", item("tfmg:nitrate_dust", 2) + [fluid("tfmg:sulfuric_acid", 500)], [result_fluid("nitric_acid", 500)], heated=True)
    mixing("oxalic_acid", item("minecraft:sugar", 2) + [fluid("nitric_acid", 250)], [result("oxalic_acid", 2)], heated=True)
    mixing("phosphoric_acid", item("minecraft:bone_meal", 2) + [fluid("tfmg:sulfuric_acid", 500)], [result_fluid("phosphoric_acid", 500)])
    mixing("p204", [fluid("d2ehpa", 250), fluid("tfmg:kerosene", 750), item("tfmg:limesand")], [result_fluid("p204", 1000)])
    mixing("p507", [fluid("ehehpa", 250), fluid("tfmg:kerosene", 750), item("tfmg:limesand")], [result_fluid("p507", 1000)])
    vat("naphthenic_acid", [fluid("tfmg:heavy_oil", 1000), fluid("caustic_soda", 250), fluid("tfmg:sulfuric_acid", 250)], [result_fluid("naphthenic_acid", 500)],
        ["tfmg:mixing"], folder="mixing")
    mixing("bastnasite_concentrate", item("bastnasite_dust", 2) + [fluid("minecraft:water", 250), fluid("naphthenic_acid", 100)],
           [result("bastnasite_concentrate"), {"id": "fundamentals:bastnasite_concentrate", "chance": 0.5}])
    for kind, time in (("campfire_cooking", 400), ("smoking", 200)):
        write(RECIPES / f"roasting/roasted_bastnasite_{kind}.json", {"type": f"minecraft:{kind}", "category": "misc", "ingredient": item("bastnasite_concentrate"),
                                                                    "result": {"id": "fundamentals:roasted_bastnasite"}, "experience": 0.1, "cookingtime": time})
    mixing("rare_earth_liquor_from_bastnasite", [item("roasted_bastnasite"), fluid(STRIP, 250)],
           [result_fluid("crude_rare_earth_liquor", 250), result("cerium_concentrate")], heated=True)
    write(RECIPES / "calcining/cerium_oxide_from_concentrate.json", {
        "type": "minecraft:blasting", "category": "misc", "ingredient": item("cerium_concentrate"),
        "result": {"id": "fundamentals:cerium_oxide"}, "experience": 0.3, "cookingtime": 100})
    for grade in ("light", "heavy"):
        mixing(f"{grade}_rare_earth_sulfate", [item(f"{grade}_rare_earth_concentrate"), fluid("tfmg:sulfuric_acid", 250)],
               [result(f"{grade}_rare_earth_sulfate"), result_fluid("phosphoric_acid", 125)], heated=True)
    residue = {"light": result("monazite_residue_dust"), "heavy": {"id": "fundamentals:monazite_residue_dust", "chance": 0.5}}
    for grade, crude, name in (("light", "crude_rare_earth_liquor", "rare_earth_liquor"), ("heavy", "crude_heavy_rare_earth_liquor", "heavy_rare_earth_liquor")):
        mixing(f"{grade}_rare_earth_carbonate", [item(f"{grade}_rare_earth_sulfate"), item("soda_ash"), fluid("minecraft:water", 500)],
               [result(f"{grade}_rare_earth_carbonate"), residue[grade]])
        mixing(name, [item(f"{grade}_rare_earth_carbonate"), fluid(STRIP, 500)], [result_fluid(crude, 500)])
    for crude, clean in (("crude_rare_earth_liquor", "rare_earth_liquor"), ("crude_heavy_rare_earth_liquor", "heavy_rare_earth_liquor")):
        mixing(f"clarify_{clean}", item("tfmg:limesand", 2) + [fluid(crude, 1000)], [result_fluid(clean, 1000), result("clarifier_sludge")])
    for organic in ORGANICS:
        mixing(f"scrub_{organic}", [item("tfmg:limesand"), fluid(f"fouled_{organic}", 1000)], [result_fluid(organic, 900)])
    mixing("calcium_chloride_liquor", item("tfmg:limesand", 2) + [fluid("spent_liquor", 1000)], [result_fluid("calcium_chloride_liquor", 1000)])
    mixing("calcium_chloride", [fluid("calcium_chloride_liquor", 1000)], [result("calcium_chloride", 3)], heated=True)
    mixing("scandium_liquor", item("thortveitite_dust", 2) + [item("tfmg:coal_coke"), fluid("chlorine", 500), fluid("minecraft:water", 500)],
           [result_fluid("scandium_liquor", 500)], heated=True)
    mixing("heavy_rare_earth_liquor_from_clay", item("raw_ion_adsorption_clay", 4) + [item("salt"), fluid("minecraft:water", 500)],
           [result_fluid("crude_heavy_rare_earth_liquor", 250), {"id": "minecraft:clay_ball", "count": 4}])
    mixing("heavy_rare_earth_liquor_from_clay_with_seawater", item("raw_ion_adsorption_clay", 4) + [fluid("seawater", 1000)],
           [result_fluid("crude_heavy_rare_earth_liquor", 250), {"id": "minecraft:clay_ball", "count": 4}])
    feed, salt, liquor, rest = ZINC_REDUCTION
    europium = light_fraction(feed, liquor, rest)
    mixing(salt, [fluid(feed, 1000), {"tag": "c:nuggets/zinc"}, fluid("tfmg:sulfuric_acid", 100)],
           [result_fluid(rest, round(1000 * (1 - europium))), {"id": f"fundamentals:{salt}", "chance": round(1000 * europium / 250, 2)}])
    mixing(liquor, [item(salt), fluid("nitric_acid", 250)], [result_fluid(liquor, 250)], heated=True)
    vat("bromine", [fluid("bittern", 1000), fluid("chlorine", 20)], [result_fluid("bromine", 20), result("magnesium_chloride", 2)],
        ["tfmg:mixing"], folder="salt")


def vat(name, ingredients, results, machines, heated=True, time=100, folder="reduction"):
    heat = "superheated" if heated == "superheated" else "heated" if heated else None
    common.vat(RECIPES / f"{folder}/{name}.json", ingredients, results, machines, heat, time)


def metals():
    mixing("hydrofluoric_acid", item("raw_fluorite", 2) + [fluid("tfmg:sulfuric_acid", 500)], [result_fluid("hydrofluoric_acid", 500)], heated=True)
    vat("argon", [fluid("tfmg:air", 1000)], [{"id": "fundamentals:argon", "amount": 9}], ["tfmg:centrifuge"], heated=False, time=10)
    vat("calcium_ingot", item("calcium_chloride", 2), [result("calcium_ingot", 2), result_fluid("chlorine", 500)], ["tfmg:electrode", "tfmg:electrode"])
    for element in ELECTROLYSIS + CALCIOTHERMIC:
        mixing(f"{element}_fluoride", [item(f"{element}_oxide"), fluid("hydrofluoric_acid", 500)], [result(f"{element}_fluoride")])
    for element in ELECTROLYSIS:
        vat(f"{element}_ingot", [item(f"{element}_fluoride")] + item(f"{element}_oxide", 2),
            [result(f"{element}_ingot", 2), {"id": f"fundamentals:{element}_fluoride", "chance": 0.9}],
            ["tfmg:electrode", "tfmg:electrode"], heated="superheated")
    for element in CALCIOTHERMIC:
        vat(f"{element}_ingot", item(f"{element}_fluoride", 2) + item("calcium_ingot", 2) + [fluid("argon", 250)],
            [result(f"{element}_ingot", 2), result("raw_fluorite", 2)], ["tfmg:mixing"], heated="superheated")
    for element in LANTHANOTHERMIC:
        vat(f"{element}_ingot", item(f"{element}_oxide", 2) + item("lanthanum_ingot", 2) + [fluid("argon", 250)],
            [result(f"{element}_ingot", 2), result("lanthanum_oxide", 2)], ["tfmg:mixing"], heated="superheated")
    write(DATA.parent / "c/tags/item/ingots/calcium.json", {"replace": False, "values": ["fundamentals:calcium_ingot"]})


def solvents():
    def chem(name, ingredients, results, machines=("tfmg:mixing",), heated=True):
        vat(name, ingredients, results, list(machines), heated=heated, folder="solvents")

    chem("water_gas", [item("tfmg:coal_coke"), fluid("minecraft:water", 500)], [result_fluid("water_gas", 1000)])
    chem("hydrogen", [fluid("water_gas", 1000), fluid("minecraft:water", 500)], [{"id": "tfmg:hydrogen", "amount": 1000}, {"id": "tfmg:carbon_dioxide", "amount": 500}])
    chem("ethylhexanol", [item("cobalt_ingot"), fluid("tfmg:propylene", 500), fluid("water_gas", 500), fluid("tfmg:hydrogen", 500)],
         [result_fluid("ethylhexanol", 250), {"id": "fundamentals:cobalt_ingot", "chance": 0.95}])
    chem("white_phosphorus", item("minecraft:bone_meal", 2) + [item("tfmg:coal_coke"), {"tag": "c:sands/colorless"}],
         [result("white_phosphorus"), {"id": "tfmg:slag"}], machines=("tfmg:electrode", "tfmg:electrode"), heated="superheated")
    mixing("chlorine", [item("raw_pyrolusite"), fluid(STRIP, 1000)], [result_fluid("chlorine", 250)], heated=True)
    mixing("phosphorus_trichloride", [item("white_phosphorus"), fluid("chlorine", 750)], [result_fluid("phosphorus_trichloride", 500)], heated=True)
    chem("d2ehpa", [fluid("ethylhexanol", 500), fluid("phosphorus_trichloride", 250), fluid("tfmg:air", 500), fluid("minecraft:water", 250)],
         [result_fluid("d2ehpa", 250), result_fluid(STRIP, 500)], heated=False)
    chem("ehehpa", [fluid("ethylhexanol", 500), fluid("phosphorus_trichloride", 250), fluid("minecraft:water", 250)],
         [result_fluid("ehehpa", 250), result_fluid(STRIP, 500)])


def cuts():
    for liquor, organic, stages, light, heavy in CUTS:
        write(RECIPES / f"separation/{liquor}.json", {
            "type": "fundamentals:separation", "liquor": f"fundamentals:{liquor}", "organic": f"fundamentals:{organic}",
            "strip": f"fundamentals:{STRIP}", "stages": stages,
            "light": f"fundamentals:{light}", "heavy": f"fundamentals:{heavy}", "light_fraction": light_fraction(liquor, light, heavy)})


def magnetic():
    for liquor, _, _, light, heavy in CUTS:
        if liquor in MAGNETIC:
            drawn, passes, curie = MAGNETIC[liquor]
            write(RECIPES / f"magnetic/{liquor}.json", {
                "type": "fundamentals:magnetic", "liquor": f"fundamentals:{liquor}", "light": f"fundamentals:{light}",
                "heavy": f"fundamentals:{heavy}", "light_fraction": light_fraction(liquor, light, heavy),
                "attracted": f"fundamentals:{drawn}", "passes": passes, "curie_share": curie})


def oxalates():
    for element, forms in MATERIALS.items():
        if "oxalate" not in forms:
            continue
        liquor = OXALATE_FROM.get(element, f"{element}_liquor")
        assert liquor in LIQUORS, element
        mixing(f"{element}_oxalate", [item("oxalic_acid"), fluid(liquor, 250)], [result(f"{element}_oxalate")])
        write(RECIPES / f"calcining/{element}_oxide.json", {
            "type": "minecraft:blasting", "category": "misc", "ingredient": item(f"{element}_oxalate"),
            "result": {"id": f"fundamentals:{element}_oxide"}, "experience": 0.3, "cookingtime": 100})


def mixer_settler():
    tex = {"side": "fundamentals:block/mixer_settler_side", "top": "fundamentals:block/mixer_settler_top",
           "window": "fundamentals:block/mixer_settler_window", "nozzle": "fundamentals:block/mixer_settler_nozzle",
           "particle": "fundamentals:block/mixer_settler_side"}
    full = [0, 0, 16, 16]

    def box(f, t, faces):
        return {"from": list(f), "to": list(t), "faces": {d: {"texture": tx, "uv": uv, **({"cullface": d} if cull else {})}
                                                           for d, (tx, uv, cull) in faces.items()}}

    def model(name, elements):
        write(ASSETS / f"models/block/mixer_settler/{name}.json",
              {"ambientocclusion": False, "render_type": "minecraft:cutout", "textures": tex, "elements": elements})
        return f"fundamentals:block/mixer_settler/{name}"

    W, F = VAT["wall"], VAT["floor"]
    WEIR_TOP = 16 - VAT["weir_below_rim"]
    WIN0, WIN1 = VAT["window"]
    SLAB = {"west": ((0, 0, 0), (W, 16, 16)), "east": ((16 - W, 0, 0), (16, 16, 16)),
            "north": ((0, 0, 0), (16, 16, W)), "south": ((0, 0, 16 - W), (16, 16, 16))}
    INNER = {"west": "east", "east": "west", "north": "south", "south": "north"}

    def panel(side):
        lo, hi = SLAB[side]
        along_x = side in ("north", "south")
        cap = ("#top", [0, 0, 16, W] if along_x else [0, 0, W, 16], False)
        return [box(lo, hi, {side: ("#side", full, True), INNER[side]: ("#top", full, False), "up": cap})]

    def window_wall(side, part):
        lo, hi = SLAB[side]
        along_x = side in ("north", "south")
        axis = 0 if along_x else 2
        cap = ("#top", [0, 0, 16, W] if along_x else [0, 0, W, 16], False)
        out = []
        ends = ("east", "west") if along_x else ("south", "north")
        for (p0, p1, uv), end in zip(((0, WIN0, [0, 0, WIN0, 16]), (WIN1, 16, [WIN1, 0, 16, 16])), ends):
            f, t = list(lo), list(hi)
            f[axis], t[axis] = p0, p1
            out.append(box(f, t, {side: ("#side", uv, True), INNER[side]: ("#top", uv, False), "up": cap, end: ("#top", [0, 0, W, 16], False)}))
        uv = {"single": [0, 0, 8, 16], "bottom": [0, 2, 8, 16], "top": [0, 0, 8, 14]}[part]
        f, t = list(lo), list(hi)
        f[axis], t[axis] = WIN0, WIN1
        faces = {side: ("#window", uv, True), INNER[side]: ("#window", uv, False)}
        if part != "bottom":
            faces["up"] = cap
        out.append(box(f, t, faces))
        return out

    def weir_model(name, y0, y1):
        return model(name, [box((0, y0, 0), (16, y1, W), {"north": ("#top", [0, 16 - y1, 16, 16 - y0], False),
                                                          "south": ("#top", [0, 16 - y1, 16, 16 - y0], False),
                                                          "up": ("#top", [0, 0, 16, W], False)})])

    sides = (("west", "left"), ("east", "right"), ("north", "front"), ("south", "back"))
    walls = {prop: model(f"wall_{prop}", panel(side)) for side, prop in sides}
    windows = {(prop, part): model(f"wall_{prop}_{part}", window_wall(side, part)) for side, prop in sides for part in ("single", "bottom", "top")}
    floor = model("floor", [box((0, 0, 0), (16, F, 16), {"down": ("#top", full, True), "up": ("#top", full, False)})])
    weir = weir_model("weir", F, 16)
    weir_low = weir_model("weir_low", F, WEIR_TOP)
    weir_lip = weir_model("weir_lip", 0, WEIR_TOP)

    port_ahead = model("port_ahead", [box((4, 10, 1), (12, 14, 2), {"south": ("#nozzle", [0, 0, 16, 16], False), "up": ("#nozzle", [0, 6, 16, 10], False),
                                                                   "east": ("#nozzle", [6, 4, 10, 12], False), "west": ("#nozzle", [6, 4, 10, 12], False)})])
    port_behind = model("port_behind", [box((4, 2, 14), (12, 6, 15), {"north": ("#nozzle", [0, 0, 16, 16], False), "up": ("#nozzle", [0, 6, 16, 10], False),
                                                                     "east": ("#nozzle", [6, 4, 10, 12], False), "west": ("#nozzle", [6, 4, 10, 12], False)})])
    parts = []
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        rot = {"y": y} if y else {}
        linked = {"front": "link_ahead", "back": "link_behind"}
        for prop, mdl in walls.items():
            plain = {"facing": facing, prop: "false", "window": "false"}
            glazed = {"facing": facing, prop: "false", "window": "true"}
            if prop in linked:
                parts.append({"when": {"facing": facing, prop: "false", "window": "true", linked[prop]: "true"}, "apply": {"model": mdl, **rot}})
                glazed[linked[prop]] = "false"
            parts.append({"when": plain, "apply": {"model": mdl, **rot}})
            parts.append({"when": {**glazed, "above": "false", "below": "false"}, "apply": {"model": windows[(prop, "single")], **rot}})
            parts.append({"when": {**glazed, "above": "true", "below": "false"}, "apply": {"model": windows[(prop, "bottom")], **rot}})
            parts.append({"when": {**glazed, "above": "false", "below": "true"}, "apply": {"model": windows[(prop, "top")], **rot}})
        parts.append({"when": {"facing": facing, "front": "false", "link_ahead": "true", "above": "false"}, "apply": {"model": port_ahead, **rot}})
        parts.append({"when": {"facing": facing, "back": "false", "link_behind": "true", "below": "false"}, "apply": {"model": port_behind, **rot}})
        parts.append({"when": {"facing": facing, "rows": "well", "below": "false", "above": "true"}, "apply": {"model": weir, **rot}})
        parts.append({"when": {"facing": facing, "rows": "well", "below": "false", "above": "false"}, "apply": {"model": weir_low, **rot}})
        parts.append({"when": {"facing": facing, "rows": "well", "below": "true"}, "apply": {"model": weir_lip, **rot}})
    parts.append({"when": {"below": "false"}, "apply": {"model": floor}})
    write(ASSETS / "blockstates/mixer_settler.json", {"multipart": parts})
    write(ASSETS / "models/item/mixer_settler.json", {"ambientocclusion": False, "textures": tex, "elements":
          panel("west") + panel("east") + panel("north") + panel("south")
          + [box((0, 0, 0), (16, F, 16), {"down": ("#top", full, False), "up": ("#top", full, False)}),
             box((0, F, 10), (16, WEIR_TOP, 10 + W), {"north": ("#top", [0, 16 - WEIR_TOP, 16, 16 - F], False), "south": ("#top", [0, 16 - WEIR_TOP, 16, 16 - F], False), "up": ("#top", [0, 10, 16, 10 + W], False)})],
          "display": {"gui": {"rotation": [30, 225, 0], "scale": [0.625, 0.625, 0.625]}, "ground": {"scale": [0.25, 0.25, 0.25]},
                      "fixed": {"scale": [0.5, 0.5, 0.5]}, "thirdperson_righthand": {"rotation": [75, 45, 0], "scale": [0.375, 0.375, 0.375], "translation": [0, 2.5, 0]},
                      "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]}}})
    write(RESOURCES / "fundamentals_vat.json", VAT)
    write(DATA / "loot_table/blocks/mixer_settler.json", {"type": "minecraft:block", "pools": [drop_self("mixer_settler")]})
    write(RECIPES / "mixer_settler.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P P", "PPP", "PFP"],
        "key": {"P": ACID_PROOF, "F": {"item": "create:fluid_pipe"}},
        "result": {"id": "fundamentals:mixer_settler", "count": 6}})
    for name in PLANT_ITEMS:
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})


def magnetomigration_cell():
    faces = {"north": "end", "south": "end", "east": "magnet", "west": "side", "up": "top", "down": "side"}
    tex = {face: f"fundamentals:block/magnetomigration_cell_{sheet}" for face, sheet in faces.items()}
    write(ASSETS / "models/block/magnetomigration_cell.json",
          {"parent": "minecraft:block/cube", "render_type": "minecraft:translucent",
           "textures": {**tex, "particle": "fundamentals:block/magnetomigration_cell_side"}})
    write(ASSETS / "blockstates/magnetomigration_cell.json", {"variants": {
        f"facing={facing}": {"model": "fundamentals:block/magnetomigration_cell", **({"y": y} if y else {})}
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write(ASSETS / "models/item/magnetomigration_cell.json", {"parent": "fundamentals:block/magnetomigration_cell"})
    write(DATA / "loot_table/blocks/magnetomigration_cell.json", {"type": "minecraft:block", "pools": [drop_self("magnetomigration_cell")]})
    write(RECIPES / "magnetomigration_cell.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PPP", "PTM", "PPP"],
        "key": {"P": ACID_PROOF, "T": [{"item": "fundamentals:plastic_fluid_tank"}, {"tag": "c:plates/stainless_steel"}], "M": {"tag": "fundamentals:magnets"}},
        "result": {"id": "fundamentals:magnetomigration_cell", "count": 1}})


def tank(name, render_type):
    sheets = {"0": "top", "1": "", "3": "window", "4": "inner", "5": "window_single", "particle": ""}
    tex = {k: f"fundamentals:block/{name}" + (f"_{v}" if v else "") for k, v in sheets.items()}
    variants = {}
    for top in (False, True):
        for bottom in (False, True):
            part = "single" if top and bottom else "top" if top else "bottom" if bottom else "middle"
            for shape in ("plain", "window", "window_ne", "window_nw", "window_se", "window_sw"):
                model = f"block_{part}" + ("" if shape == "plain" else f"_{shape}")
                write(ASSETS / f"models/block/{name}/{model}.json", {"parent": f"create:block/fluid_tank/{model}", "render_type": render_type, "textures": tex})
                variants[f"bottom={str(bottom).lower()},shape={shape},top={str(top).lower()}"] = {"model": f"fundamentals:block/{name}/{model}"}
    write(ASSETS / f"blockstates/{name}.json", {"variants": variants})
    write(ASSETS / f"models/item/{name}.json", {"parent": f"fundamentals:block/{name}/block_single_window"})
    write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [drop_self(name)]})


def plastic_tank():
    tank("plastic_fluid_tank", "minecraft:translucent")
    write(RECIPES / "plastic_fluid_tank.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P", "B", "P"],
        "key": {"P": {"tag": "fundamentals:plastic_sheets"}, "B": {"tag": "c:barrels/wooden"}},
        "result": {"id": "fundamentals:plastic_fluid_tank", "count": 1}})


def create_json(path):
    with zipfile.ZipFile(CREATE_JAR) as jar:
        return json.loads(jar.read(f"assets/create/{path}"))


TITANIUM_SHEETS = {"create:block/pipes": "titanium_pipes", "create:block/pipes_connected": "titanium_pipes_connected", "create:block/pump": "titanium_pump",
                   "create:block/fluid_valve": "titanium_fluid_valve", "create:block/valve_open": "titanium_valve_open",
                   "create:block/valve_closed": "titanium_valve_closed", "block/copper_block": "titanium_fluid_tank_top",
                   "create:block/copper_underside": "titanium_fluid_valve"}
TITANIUM_PIPEWORK = {"titanium_pipe": "Titanium Pipe", "titanium_mechanical_pump": "Titanium Mechanical Pump", "titanium_fluid_valve": "Titanium Fluid Valve",
                     "titanium_fluid_tank": "Titanium Fluid Tank"}


def titanium_copy(create_dir, ours, render_type=None):
    with zipfile.ZipFile(CREATE_JAR) as jar:
        paths = sorted(n for n in jar.namelist() if n.startswith(f"assets/create/models/block/{create_dir}/") and n.endswith(".json")
                       and Path(n).stem not in ("cog", "pointer", "casing", "window"))
    for path in paths:
        model = path.removeprefix(f"assets/create/models/block/{create_dir}/").removesuffix(".json")
        textures, parent = {}, f"create:block/{create_dir}/{model}"
        while parent and parent.startswith("create:block/"):
            data = create_json(f"models/{parent.removeprefix('create:')}.json")
            textures = {**data.get("textures", {}), **textures}
            parent = data.get("parent")
        swapped = {k: f"fundamentals:block/{TITANIUM_SHEETS[v]}" for k, v in textures.items() if v in TITANIUM_SHEETS}
        write(ASSETS / f"models/block/{ours}/{model}.json",
              {"parent": f"create:block/{create_dir}/{model}", **({"render_type": render_type} if render_type else {}), "textures": swapped})


def titanium_blockstate(create_name, ours):
    text = json.dumps(create_json(f"blockstates/{create_name}.json")).replace(f"create:block/{create_name}/", f"fundamentals:block/{ours}/")
    write(ASSETS / f"blockstates/{ours}.json", json.loads(text))


def titanium_pipework():
    titanium_copy("fluid_pipe", "titanium_pipe")
    titanium_blockstate("fluid_pipe", "titanium_pipe")
    titanium_copy("mechanical_pump", "titanium_mechanical_pump")
    titanium_blockstate("mechanical_pump", "titanium_mechanical_pump")
    titanium_copy("fluid_valve", "titanium_fluid_valve", "minecraft:cutout_mipped")
    titanium_blockstate("fluid_valve", "titanium_fluid_valve")
    tank("titanium_fluid_tank", "minecraft:cutout_mipped")
    for name, model in (("titanium_pipe", "item"), ("titanium_mechanical_pump", "item"), ("titanium_fluid_valve", "item")):
        write(ASSETS / f"models/item/{name}.json", {"parent": f"fundamentals:block/{name}/{model}"})
    for name in TITANIUM_PIPEWORK:
        if name != "titanium_fluid_tank":
            write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [drop_self(name)]})
    write(RECIPES / "titanium_pipe.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SIS"],
        "key": {"S": {"tag": "c:plates/titanium"}, "I": {"tag": "c:ingots/titanium"}}, "result": {"id": "fundamentals:titanium_pipe", "count": 4}})
    write(RECIPES / "titanium_mechanical_pump.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": "create:cogwheel"}, {"item": "fundamentals:titanium_pipe"}],
        "result": {"id": "fundamentals:titanium_mechanical_pump", "count": 1}})
    write(RECIPES / "titanium_fluid_valve.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"tag": "c:plates/titanium"}, {"item": "fundamentals:titanium_pipe"}],
        "result": {"id": "fundamentals:titanium_fluid_valve", "count": 1}})
    write(RECIPES / "titanium_fluid_tank.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P", "B", "P"],
        "key": {"P": {"tag": "c:plates/titanium"}, "B": {"tag": "c:barrels/wooden"}},
        "result": {"id": "fundamentals:titanium_fluid_tank", "count": 1}})


def template():
    empty = gzip.decompress((DATA / "structure/empty.nbt").read_bytes())
    i = empty.index(b"size") + len(b"size") + 1 + 4
    patched = empty[:i] + (27).to_bytes(4, "big") + (5).to_bytes(4, "big") + (5).to_bytes(4, "big") + empty[i + 12:]
    (DATA / "structure/battery.nbt").write_bytes(gzip.compress(patched, mtime=0))


def names():
    lang = read_lang()
    for id, (name, _, _) in FLUIDS.items():
        lang[f"fluid_type.fundamentals.{id}"] = name
    lang["block.fundamentals.mixer_settler"] = "Mixer-Settler Casing"
    lang["block.fundamentals.plastic_fluid_tank"] = "Plastic Fluid Tank"
    for name, display in TITANIUM_PIPEWORK.items():
        lang[f"block.fundamentals.{name}"] = display
    for acid in DISSOLVES:
        lang[f"block.fundamentals.{acid}"] = FLUIDS[acid][0]
        lang[f"item.fundamentals.{acid}_bucket"] = f"{FLUIDS[acid][0]} Bucket"
    lang["item.fundamentals.seawater_bucket"] = "Seawater Bucket"
    lang["goggles.fundamentals.boiler.fresh_water"] = "No water coming in: a boiler takes fresh water, never seawater"
    for name, display in PLANT_ITEMS.items():
        lang[f"item.fundamentals.{name}"] = display
    lang["goggles.fundamentals.mixer_settler.stages"] = "Battery of %s stages, %s mB a batch"
    lang["goggles.fundamentals.mixer_settler.stage"] = "Stage %s across, %s along, %s tall"
    lang["goggles.fundamentals.mixer_settler.idle"] = "Nothing in the feed"
    lang["goggles.fundamentals.mixer_settler.no_cut"] = "%s does not part"
    lang["goggles.fundamentals.mixer_settler.short"] = "%s parts in %s stages; this battery has %s"
    lang["goggles.fundamentals.mixer_settler.organic"] = "Every stage wants %s on top"
    lang["goggles.fundamentals.mixer_settler.mixer"] = "Every trough wants a Mechanical Mixer turning over it"
    lang["goggles.fundamentals.mixer_settler.lever"] = "Waiting for the lever"
    lang["goggles.fundamentals.mixer_settler.casing"] = "Casing: a stage is three across and three along"
    lang["goggles.fundamentals.mixer_settler.settling"] = "Coming to equilibrium: %s s"
    lang["goggles.fundamentals.mixer_settler.strip"] = "The far end wants %s"
    lang["goggles.fundamentals.mixer_settler.ready"] = "Parting %s into %s mB %s and %s mB %s a batch"
    lang["goggles.fundamentals.mixer_settler.progress"] = "Organic on %s of %s stages, liquor in %s"
    lang["goggles.fundamentals.mixer_settler.ends"] = "Feed %s, strip %s"
    lang["goggles.fundamentals.mixer_settler.products"] = "Out %s at the head, %s at the tail"
    lang["goggles.fundamentals.mixer_settler.sump"] = "Sump %s"
    lang["goggles.fundamentals.mixer_settler.waste"] = "The sump is full: pump the spent liquor out from below"
    lang["goggles.fundamentals.mixer_settler.crud"] = "Crud at the interface: a dirty feed has fouled the organic; drain it and scrub it with lime"
    lang["goggles.fundamentals.mixer_settler.emulsion"] = "The mixer is too fast: the phases emulsify and will not settle"
    lang["goggles.fundamentals.mixer_settler.nothing"] = "nothing"
    lang["block.fundamentals.magnetomigration_cell"] = "Magnetomigration Cell"
    cell = "goggles.fundamentals.magnetomigration_cell"
    lang[f"{cell}.line"] = "Line of %s cells, %s mB a batch"
    lang[f"{cell}.idle"] = "Nothing in the feed"
    lang[f"{cell}.no_cut"] = "%s has no magnetic cut: its products do not sort by moment"
    lang[f"{cell}.short"] = "%s parts in %s passes; this line has %s cells"
    lang[f"{cell}.full"] = "The outlets are full: pipe the products away from the last cell's sides"
    lang[f"{cell}.ready"] = "Parting %s: %s mB %s to the magnets, %s mB %s away a batch"
    lang[f"{cell}.passes"] = "%s passes wanted, %s cells in line"
    lang[f"{cell}.heat"] = "Liquor at %s °C, the cut %s%% as magnetic as at 20 °C: a batch every %s s"
    lang[f"{cell}.feed"] = "Feed %s"
    lang[f"{cell}.outlets"] = "Magnet side %s, far side %s"
    write_lang(lang)


def main():
    for folder in ("mixing", "separation", "magnetic", "calcining", "reduction", "solvents"):
        shutil.rmtree(RECIPES / folder, ignore_errors=True)
    shutil.rmtree(ASSETS / "models/block/mixer_settler", ignore_errors=True)
    for vessel in ("plastic_fluid_tank", *TITANIUM_PIPEWORK):
        shutil.rmtree(ASSETS / f"models/block/{vessel}", ignore_errors=True)
    for stale in (ASSETS / "models/block").glob("mixer_settler*.json"):
        stale.unlink()
    java_table()
    chemistry()
    cuts()
    magnetic()
    oxalates()
    acids()
    metals()
    solvents()
    mixer_settler()
    magnetomigration_cell()
    plastic_tank()
    titanium_pipework()
    template()
    names()
    print(f"{len(FLUIDS)} fluids, {len(CUTS)} cuts, {len(MAGNETIC)} with a magnetic route")


if __name__ == "__main__":
    main()
