#!/usr/bin/env python3
"""Writes the advancements: a handful of milestones, not a checklist, and the periodic table, a tab with an
advancement per element. Re-run after adding a mineral, an item, a grinding recipe or a rare earth.

    python3 tools/paint_elements.py && python3 tools/build_advancements.py

An element lights up the first time the player holds anything made of it: each has an item tag,
fundamentals:elements/<symbol>, of every item that contains it. Ours are read off the formulas in
content/*/*Materials.java, a form adding what it adds (an oxide its oxygen); the items that are not a form of a
material, and vanilla's, Create's and TFMG's, are listed by hand below. An element nothing contains stays a dim
tile. PeriodicTable.java pins the tab to the table, by the atomic number each icon carries.
"""
import json
import re
import shutil
from pathlib import Path

from build_ore_data import ASSETS, DATA, DISPLAY, ORES, tag, write
from build_heat_data import THERMOMETERS
from build_separation_data import DISSOLVES, PLANT_ITEMS
from build_uses_data import BLOCKS as USES_BLOCKS, ITEMS as USES_ITEMS, MAGNETS, PGM_ITEMS, PLASTIC_BLOCKS, PLASTIC_ITEMS
from paint_elements import ELEMENTS
from paint_materials import MATERIALS, items as material_items
from paint_oxidation import BLOCKS as OXIDATION_BLOCKS, FAMILIES as WEATHERING

OUT = DATA / "advancement"
ELEMENT_TAGS = DATA / "tags/item/elements"
JAVA = Path(__file__).resolve().parent.parent / "src/main/java/ai/gsmc/fundamentals/content"
RAW = [f"fundamentals:raw_{mineral}" for mineral in ORES]
GRINDING = sorted(f"fundamentals:grinding/{p.stem}" for p in (DATA / "recipe/grinding").glob("*.json"))
SYMBOLS = {symbol: z for z, (symbol, _, _) in ELEMENTS.items()}
NAME = {symbol: name for symbol, name, _ in ELEMENTS.values()}
RARE_EARTHS = [NAME[s].lower() for s in ("La", "Ce", "Pr", "Nd", "Sm", "Eu", "Gd", "Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu", "Y", "Sc")]

# What a form adds to its material.
FORM_ADDS = {"oxide": "O", "fluoride": "F", "oxalate": "C2O4"}
# Materials registered without a formula.
MATERIAL_FORMULAS = {
    "steel": "Fe-C", "bronze": "Cu-Sn", "brass": "Cu-Zn", "blister_copper": "Cu", "crude_tin": "Sn-Fe", "lead_bullion": "Pb-Ag",
    "ferromanganese": "Fe-Mn", "ferronickel": "Fe-Ni", "ferromolybdenum": "Fe-Mo", "ferrotungsten": "Fe-W", "ferrovanadium": "Fe-V",
    "didymium": "(Pr,Nd)", "tin_concentrate": "SnO2", "copper_concentrate": "CuFeS2", "lead_concentrate": "PbS", "zinc_concentrate": "ZnS",
    "platinum_group_concentrate": "Pt,Pd,Rh,Ru,Ir,Os",
    "bastnasite_concentrate": "(Ce,La,Nd)CO3F",
    "light_rare_earth_concentrate": "(La,Ce,Pr,Nd,Sm,Th)PO4",
    "heavy_rare_earth_concentrate": "(Y,Gd,Tb,Dy,Ho,Er,Tm,Yb,Lu)PO4",
    # kaolinite holding rare earth ions, heavy ones above all
    "ion_adsorption_clay": "Al2Si2O5(OH)4,(Y,La,Nd,Dy)",
}
# Our items that are not a form of a material.
ITEM_FORMULAS = {
    "phosphor": "Y2O3,Eu,LaPO4,Ce,Tb", "didymium_glass": "SiO2,(Pr,Nd)", "roasted_cobaltite": "Co3O4", "roasted_chalcopyrite": "CuO,Fe2O3",
    "rhenium_flue_dust": "Re2O7", "tungsten_carbide": "WC", "tungsten_filament": "W", "clarifier_sludge": "Fe(OH)3,Al(OH)3,Th(OH)4",
    "copper_calcine": "CuO,Fe2O3", "zinc_oxide": "ZnO", "roasted_pentlandite": "NiO,Fe2O3", "lithium_chloride": "LiCl",
    "ferroboron": "FeB", "soda_ash": "Na2CO3", "sodium_chromate": "Na2CrO4", "sodium_dichromate": "Na2Cr2O7", "aluminium_powder": "Al",
    "roasted_tin_concentrate": "SnO2,Fe2O3", "solder": "Sn-Pb", "titania_slag": "TiO2,FeO", "magnesium_chloride": "MgCl2",
    "silver_zinc_crust": "Ag-Zn", "litharge": "PbO", "thorium_nitrate": "Th(NO3)4", "gas_mantle": "ThO2,CeO2",
    "ammonium_chloride": "NH4Cl", "insoluble_residue": "Ru,Rh,Ir,Os", "iridium_rhodium_residue": "Ir,Rh",
    "ammonium_chloroplatinate": "(NH4)2PtCl6", "dichlorodiammine_palladium": "Pd(NH3)2Cl2", "ammonium_chlororuthenate": "(NH4)2RuCl6",
    "ammonium_chloroiridate": "(NH4)2IrCl6", "ammonium_chlororhodate": "(NH4)3RhCl6", "reforming_catalyst": "Pt,Re,Al2O3",
    "platinum_rhodium_gauze": "Pt-Rh", "osmium_filament": "Os",
    "salt": "NaCl", "oxalic_acid": "H2C2O4", "roasted_bastnasite": "(Ce,La,Nd)OF", "light_rare_earth_sulfate": "(La,Ce,Pr,Nd,Sm)2(SO4)3",
    "heavy_rare_earth_sulfate": "(Y,Gd,Tb,Dy,Ho,Er,Tm,Yb,Lu)2(SO4)3", "calcium_chloride": "CaCl2", "calcium_ingot": "Ca", "white_phosphorus": "P4",
    "hydrochloric_acid_bucket": "HCl", "nitric_acid_bucket": "HNO3", "phosphoric_acid_bucket": "H3PO4", "hydrofluoric_acid_bucket": "HF",
    "aqua_regia_bucket": "HNO3,HCl", "bromine_bucket": "Br2", "seawater_bucket": "H2O,NaCl,MgCl2",
    # a bloom is iron holding its slag, fayalite
    "iron_bloom": "Fe,Fe2SiO4", "roasted_galena": "PbO", "calcined_spodumene": "LiAlSi2O6", "photovoltaic_panel": "Si",
    "clarifier_sludge_block": "Fe(OH)3,Al(OH)3,Th(OH)4", "mercury": "Hg",
    # the crude chloride still holds the hafnium zircon carries
    "crude_zirconium_tetrachloride": "ZrCl4,HfCl4", "zirconium_tetrachloride": "ZrCl4", "hafnium_tetrachloride": "HfCl4",
    "yttria_stabilised_zirconia": "ZrO2,Y2O3", "beryl_frit": "Be3Al2Si6O18", "beryllium_hydroxide": "Be(OH)2",
    "ammonium_fluoroberyllate": "(NH4)2BeF4", "beryllium_pebbles": "Be",
    # sintered NdFeB is sold nickel-plated against rust; SmCo and cast alnico go bare
    "neodymium_iron_boron_magnet": "Nd2Fe14B,Ni", "dysprosium_neodymium_iron_boron_magnet": "(Nd,Dy)2Fe14B,Ni", "samarium_cobalt_magnet": "SmCo5",
    "alnico_magnet": "Fe-Al-Ni-Co-Cu",
    "mercury_thermometer": "Hg,SiO2", "spirit_thermometer": "C12H26,SiO2", "bimetallic_thermometer": "Cu-Zn,Fe", "type_k_thermocouple": "Ni-Cr,Ni-Al", "type_s_thermocouple": "Pt-Rh,Pt",
    # Natta's catalyst, TiCl3 with the AlCl3 the aluminium leaves in it; polyvinyl chloride, and the polyethylene of the dyed blocks
    "ziegler_natta_catalyst": "TiCl3,AlCl3", "pvc_resin": "C2H3Cl", "pvc_sheet": "C2H3Cl",
    **{name: "C2H4" for name in PLASTIC_BLOCKS},
    # rust is hydrated iron(III) oxide; the canister and the drum are steel, the canister holding its argon
    "rusty_iron_ingot": "Fe,Fe2O3", "rusty_steel_ingot": "Fe-C,Fe2O3", "canister": "Fe", "argon_canister": "Fe,Ar", "inert_storage_drum": "Fe",
    # a weathered block is its metal under its patina (basic copper carbonate), its tarnish (silver sulfide) or its oxide
    **{name: {"bronze": "Cu-Sn,Cu2CO3(OH)2", "silver": "Ag,Ag2S"}.get(metal) or next(f"{sym},O" for sym, n in NAME.items() if n.lower() == metal)
       for metal in WEATHERING for name in OXIDATION_BLOCKS if name.endswith(f"_{metal}_block")},
}
# Vanilla's, Create's and The Factory Must Grow's.
OTHER_FORMULAS = {
    "minecraft": {
        "water_bucket": "H2O", "ice": "H2O", "snowball": "H2O",
        "coal": "C", "charcoal": "C", "coal_block": "C", "diamond": "C", "diamond_block": "C",
        "raw_iron": "Fe", "iron_ingot": "Fe", "iron_nugget": "Fe", "iron_block": "Fe", "raw_iron_block": "Fe",
        "raw_copper": "Cu", "copper_ingot": "Cu", "copper_block": "Cu", "raw_copper_block": "Cu", "copper_ore": "Cu", "deepslate_copper_ore": "Cu",
        "raw_gold": "Au", "gold_ingot": "Au", "gold_nugget": "Au", "gold_block": "Au", "raw_gold_block": "Au",
        "gold_ore": "Au", "deepslate_gold_ore": "Au", "nether_gold_ore": "Au",
        "quartz": "SiO2", "quartz_block": "SiO2", "nether_quartz_ore": "SiO2", "amethyst_shard": "SiO2", "flint": "SiO2", "sand": "SiO2",
        "emerald": "Be3Al2Si6O18", "emerald_block": "Be3Al2Si6O18", "emerald_ore": "Be3Al2Si6O18", "deepslate_emerald_ore": "Be3Al2Si6O18",
        "lapis_lazuli": "(Na,Ca)8(AlSiO4)6(SO4,S,Cl)2", "lapis_block": "(Na,Ca)8(AlSiO4)6(SO4,S,Cl)2",
        "calcite": "CaCO3", "dripstone_block": "CaCO3", "pointed_dripstone": "CaCO3",
        "bone": "Ca5(PO4)3OH", "bone_meal": "Ca5(PO4)3OH", "bone_block": "Ca5(PO4)3OH",
        "gunpowder": "KNO3,C,S", "clay_ball": "Al2Si2O5(OH)4", "clay": "Al2Si2O5(OH)4",
    },
    "create": {
        "zinc_ingot": "Zn", "zinc_nugget": "Zn", "zinc_block": "Zn", "raw_zinc": "Zn", "raw_zinc_block": "Zn", "crushed_raw_zinc": "Zn",
        "zinc_ore": "Zn", "deepslate_zinc_ore": "Zn",
        "brass_ingot": "Cu-Zn", "brass_nugget": "Cu-Zn", "brass_sheet": "Cu-Zn", "brass_block": "Cu-Zn",
        "copper_sheet": "Cu", "copper_nugget": "Cu", "iron_sheet": "Fe", "golden_sheet": "Au",
        "crushed_raw_iron": "Fe", "crushed_raw_gold": "Au", "crushed_raw_copper": "Cu",
        "crushed_raw_aluminum": "Al", "crushed_raw_lead": "Pb", "crushed_raw_nickel": "Ni",
        "limestone": "CaCO3",
    },
    "tfmg": {
        "aluminum_ingot": "Al", "aluminum_nugget": "Al", "aluminum_sheet": "Al", "aluminum_block": "Al",
        "bauxite_powder": "Al(OH)3", "bauxite": "Al(OH)3",
        "lead_ingot": "Pb", "lead_nugget": "Pb", "lead_sheet": "Pb", "lead_block": "Pb", "raw_lead": "Pb", "galena": "PbS",
        "nickel_ingot": "Ni", "nickel_nugget": "Ni", "nickel_sheet": "Ni", "nickel_block": "Ni", "raw_nickel": "Ni",
        "lithium_ingot": "Li", "lithium_nugget": "Li", "lithium_block": "Li", "raw_lithium": "Li", "crushed_raw_lithium": "Li",
        "constantan_ingot": "Cu-Ni", "constantan_nugget": "Cu-Ni", "constantan_block": "Cu-Ni",
        "cast_iron_ingot": "Fe-C", "cast_iron_nugget": "Fe-C", "cast_iron_sheet": "Fe-C", "cast_iron_block": "Fe-C",
        "steel_ingot": "Fe-C", "steel_nugget": "Fe-C", "steel_block": "Fe-C",
        "magnetic_alloy_ingot": "Fe-Ni-Si-C", "magnetic_alloy_sheet": "Fe-Ni-Si-C",
        "silicon_ingot": "Si", "sulfur_dust": "S", "sulfur": "S", "sulfuric_acid_bucket": "H2SO4", "nitrate_dust": "KNO3",
        "copper_sulfate": "CuSO4", "coal_coke": "C", "coal_coke_dust": "C", "coal_coke_block": "C", "graphite_electrode": "C",
        "thermite_powder": "Al,Fe2O3", "slag": "Fe2SiO4",
        "plastic_sheet": "C2H4", "plastic_block": "C2H4",
        "hydrogen_bucket": "H2", "neon_bucket": "Ne", "carbon_dioxide_bucket": "CO2", "air_bucket": "N2,O2,Ar",
    },
}

# Where to look, when the minerals alone would say it badly.
WHERE = {
    "H": "In water, and in every acid",
    "Be": "In beryl, bertrandite and emerald, which is beryl",
    "Hf": "Parted from zircon, which carries a fiftieth as much",
    "B": "In borax, from dry lake beds",
    "C": "In coal, charcoal, coke and diamond",
    "N": "In nitric acid, saltpetre and air",
    "O": "In water, air and most ores",
    "F": "In fluorite, and the acid made from it",
    "Ne": "Spun out of air in a centrifuge",
    "Na": "In salt, halite, seawater, trona and borax",
    "Mg": "In seawater, and the bittern it leaves",
    "Si": "In quartz, sand and flint",
    "P": "In bone, monazite and xenotime",
    "Cl": "In salt, seawater and hydrochloric acid",
    "Ar": "In a tank of air",
    "Br": "In bittern, which chlorine frees it from",
    "K": "In saltpetre and gunpowder",
    "Ca": "In calcite, limestone, bone and fluorite",
    "Au": "Native gold, as it always was",
    "Re": "In the flue dust of a molybdenite roaster",
    **{s: "Parted from platinum group concentrate" for s in ("Ru", "Rh", "Ir", "Os")},
    **{s: "Parted from monazite and bastnäsite in the mixer-settlers" for s in ("La", "Ce", "Pr", "Nd", "Sm", "Eu")},
    **{s: "Parted from xenotime, euxenite and ionic clay in the mixer-settlers" for s in ("Gd", "Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu")},
    "Th": "In monazite and euxenite, and the residue they leave",
}
SYNTHETIC = {"Tc", "Pm"} | {symbol for symbol, z in SYMBOLS.items() if z >= 93}


# A rare earth still mixed with another hasn't been found yet, only the ore or liquor it sits in.
RARE_EARTH_SYMBOLS = {"Sc", "Y", "La", "Ce", "Pr", "Nd", "Pm", "Sm", "Eu", "Gd", "Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu"}


def elements(formula):
    found = set(re.findall(r"[A-Z][a-z]?", formula))
    leftover = re.sub(r"[A-Z][a-z]?|[0-9()·,+\- ]", "", formula)
    if not formula or leftover or not found <= set(SYMBOLS):
        return None
    return found


def separated(formula):
    mixed = {s for group in re.findall(r"\(([^()]*)\)", formula) if "," in group for s in re.findall(r"[A-Z][a-z]?", group)}
    found = elements(formula)
    return None if found is None else found - (mixed & RARE_EARTH_SYMBOLS)


def material_formulas():
    """Material id -> formula, from the registry's Java."""
    formulas = {}
    call = re.compile(r'\b(?:mineral|reg|element|defineMineral|define)\(\s*(?:GROUP\s*,\s*)?"([a-z_]+)"((?:\s*,\s*(?:"[^"]*"|null|[A-Za-z_.]+))*)')
    for java in sorted(JAVA.glob("*/*Materials.java")):
        for material, args in call.findall(java.read_text(encoding="utf-8")):
            formulas[material] = next((s for s in re.findall(r'"([^"]*)"', args) if elements(s)), "")
    return {**formulas, **MATERIAL_FORMULAS}


def our_items():
    names = [name for _, _, name in material_items()]
    names += [f"raw_{mineral}" for mineral in ORES] + [f"{mineral}_ore" for mineral in ORES]
    names += list(USES_ITEMS) + list(PGM_ITEMS) + list(MAGNETS) + list(PLANT_ITEMS) + list(USES_BLOCKS) + list(PLASTIC_ITEMS) + list(PLASTIC_BLOCKS) + [f"{acid}_bucket" for acid in DISSOLVES] + ["seawater_bucket"]
    names += ["iron_bloom", "roasted_galena", "calcined_spodumene", "photovoltaic_panel"] + list(THERMOMETERS)
    names += OXIDATION_BLOCKS + ["canister", "argon_canister", "rusty_iron_ingot", "rusty_steel_ingot"]
    return names


def contents(name, formulas):
    if name in ITEM_FORMULAS:
        return separated(ITEM_FORMULAS[name])
    material, form = name, None
    if name.startswith("raw_"):
        material = name[4:]
    elif name not in formulas:
        material, _, form = name.rpartition("_")
    found = elements(formulas.get(material, "")) and separated(formulas[material])
    if found is None:
        raise SystemExit(f"no formula for {name}: give its material one, or list it in ITEM_FORMULAS")
    return found | (elements(FORM_ADDS[form]) if form in FORM_ADDS else set())


def element_tags():
    """symbol -> the item ids that contain it."""
    formulas = material_formulas()
    holders = {symbol: [] for symbol in SYMBOLS}
    for name in dict.fromkeys(our_items()):
        for symbol in contents(name, formulas):
            holders[symbol].append(f"fundamentals:{name}")
    for namespace, entries in OTHER_FORMULAS.items():
        for name, formula in entries.items():
            for symbol in separated(formula):
                holders[symbol].append(f"{namespace}:{name}")
    return holders


def where(symbol, holders, lang):
    if symbol in WHERE:
        return WHERE[symbol]
    minerals = [m for m in ORES if f"fundamentals:raw_{m}" in holders]
    if minerals:
        names = [DISPLAY.get(m, m.replace("_", " ").title()).lower() for m in minerals[:3]]
        return "In " + (names[0] if len(names) == 1 else ", ".join(names[:-1]) + " and " + names[-1])
    ours = [h.split(":")[1] for h in holders if h.startswith("fundamentals:")]
    if ours:
        return "In " + lang.get(f"item.fundamentals.{ours[0]}", ours[0]).lower()
    raise SystemExit(f"say where {symbol} comes from in WHERE")


def have(*items):
    return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": list(items)}]}}


def holds(symbol):
    return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"#fundamentals:elements/{symbol.lower()}"}]}}


def tile(z):
    return {"id": "fundamentals:element", "components": {"minecraft:custom_model_data": z}}


# name: (parent, icon, frame, title, description, criteria, True if any one criterion will do)
ADVANCEMENTS = {
    "root": (None, "raw_hematite", "task", "Fundamentals", "Dig a mineral out of the ground",
             {"raw": have(*RAW)}, False),
    "bloom": ("root", "iron_bloom", "task", "Three Thousand Years", "Fire a bloomery and rake out a bloom of iron",
              {"bloom": have("fundamentals:iron_bloom")}, False),
    "mortar": ("root", "mortar_and_pestle", "task", "Ground by Hand", "Grind something in a mortar and pestle",
               {recipe: {"trigger": "minecraft:recipe_crafted", "conditions": {"recipe_id": recipe}} for recipe in GRINDING}, True),
    "collection": ("root", "raw_malachite", "challenge", "Rock Collection",
                   f"Hold a raw chunk of every one of the {len(RAW)} minerals",
                   {item: have(item) for item in RAW}, False),
    "concentrate": ("root", "heavy_rare_earth_concentrate", "task", "Heavy Sand",
                    "Wash a rare earth mineral down to a mixed concentrate",
                    {"concentrate": have("fundamentals:light_rare_earth_concentrate", "fundamentals:heavy_rare_earth_concentrate")}, False),
    "rare_earths": ("concentrate", "neodymium_oxide", "challenge", "Sixteen of Seventeen",
                    "Hold the oxide of every rare earth, the form they trade in. Promethium doesn't count: there isn't any",
                    {name: have(f"fundamentals:{name}_oxide") for name in RARE_EARTHS}, False),
    "magnet": ("concentrate", "neodymium_iron_boron_ingot", "goal", "Permanent",
               "Alloy a rare earth magnet: neodymium-iron-boron or samarium-cobalt",
               {"magnet": have("fundamentals:neodymium_iron_boron_ingot", "fundamentals:dysprosium_neodymium_iron_boron_ingot",
                               "fundamentals:samarium_cobalt_ingot")}, False),
}


def periodic_table(lang):
    shutil.rmtree(ELEMENT_TAGS, ignore_errors=True)
    shutil.rmtree(ASSETS / "models/item/element", ignore_errors=True)
    holders = element_tags()
    found = [symbol for symbol in SYMBOLS if holders[symbol]]
    lang["item.fundamentals.element"] = "Periodic Table"
    lang["advancement.fundamentals.elements.root.title"] = "Periodic Table"
    lang["advancement.fundamentals.elements.root.description"] = "Hold anything made of an element. Each one you find lights up"
    write(OUT / "elements/root.json", {
        "display": {"icon": {"id": "fundamentals:element"}, "title": {"translate": "advancement.fundamentals.elements.root.title"},
                    "description": {"translate": "advancement.fundamentals.elements.root.description"}, "frame": "task",
                    "background": "fundamentals:textures/gui/advancements/backgrounds/periodic_table.png",
                    "show_toast": False, "announce_to_chat": False},
        "criteria": {symbol.lower(): holds(symbol) for symbol in found},
        "requirements": [[symbol.lower() for symbol in found]],
    })
    overrides = []
    for z, (symbol, name, family) in ELEMENTS.items():
        key, id = f"advancement.fundamentals.elements.{symbol.lower()}", symbol.lower()
        if holders[symbol]:
            tag(ELEMENT_TAGS / f"{id}.json", holders[symbol])
            source = where(symbol, holders[symbol], lang)
        else:
            source = "Made only in reactors and accelerators" if symbol in SYNTHETIC else "Not found in this world"
        lang[f"{key}.title"], lang[f"{key}.description"] = name, f"{z} · {source}"
        rare_earth = family == "lanthanide" or symbol in ("Sc", "Y")
        write(OUT / f"elements/{id}.json", {
            "parent": "fundamentals:elements/root",
            "display": {"icon": tile(z), "title": {"translate": f"{key}.title"}, "description": {"translate": f"{key}.description"},
                        "frame": "goal" if rare_earth else "task", "show_toast": bool(holders[symbol]), "announce_to_chat": False},
            "criteria": {id: holds(symbol) if holders[symbol] else {"trigger": "minecraft:impossible"}},
        })
        write(ASSETS / f"models/item/element/{id}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/element/{id}"}})
        overrides.append({"predicate": {"custom_model_data": z}, "model": f"fundamentals:item/element/{id}"})
    write(ASSETS / "models/item/element.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": "fundamentals:item/element"}, "overrides": overrides})
    return found


def main():
    oxides = {f"{name}_oxide" for name in RARE_EARTHS}
    assert oxides <= {name for _, _, name in material_items()}, f"rare earth oxides missing: {sorted(oxides - {n for _, _, n in material_items()})}"
    shutil.rmtree(OUT, ignore_errors=True)
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    for name, (parent, icon, frame, title, description, criteria, any_one) in ADVANCEMENTS.items():
        key = f"advancement.fundamentals.{name}"
        lang[f"{key}.title"], lang[f"{key}.description"] = title, description
        advancement = {
            "display": {"icon": {"id": f"fundamentals:{icon}"}, "title": {"translate": f"{key}.title"},
                        "description": {"translate": f"{key}.description"}, "frame": frame},
            "criteria": criteria,
        }
        if parent:
            advancement["parent"] = f"fundamentals:{parent}"
        else:
            advancement["display"].update(background="minecraft:textures/gui/advancements/backgrounds/stone.png",
                                          show_toast=False, announce_to_chat=False)
        if any_one:
            advancement["requirements"] = [list(criteria)]
        write(OUT / f"{name}.json", advancement)
    found = periodic_table(lang)
    write(lang_path, dict(sorted(lang.items())))
    print(f"wrote {len(ADVANCEMENTS)} advancements and a periodic table of {len(ELEMENTS)}, {len(found)} of them to be found: {' '.join(found)}")


if __name__ == "__main__":
    main()
