#!/usr/bin/env python3
"""The heat sources this mod adds (vanilla's are Fundamentals: Principles' own): what each block adds to the temperature around it, °C above ambient at the block itself and
just outside it, falling to nothing `reach` blocks away. The inside is the real flame or chamber; the outside is
what the walls let through, so standing by a furnace is hot, not a kiln. A data map, so another mod can add its
blocks with a file. And the dial thermometers that read it: their models, blockstates, loot, recipes and names.
And the liquids: every fluid the mod makes or touches, with its real density, viscosity, freezing and boiling points,
flash point and the rest, as a synced data map (fundamentals:liquid_properties) the game reads for freezing pipes,
boiling basins, fires and slow pumps, and the book prints. Re-run after any edit.

    python3 tools/paint_thermometers.py && python3 tools/build_heat_data.py
"""
import json
import shutil

from build_ore_data import ASSETS, DATA, ROOT, drop_self, write
from build_separation_data import FLUIDS, LIQUORS

SOURCES = {
    # Create's burner kindled, what a heated recipe gets; the Java scales it by state, seething 1.6 times for superheated
    "create:blaze_burner": (1000, 4, 150),
    "fundamentals:bloomery": (1200, 3, 100),
    "tfmg:molten_steel": (1550, 4, 300), "tfmg:molten_slag": (1450, 4, 300),
    "tfmg:blue_fire": (800, 3, 400), "tfmg:green_fire": (800, 3, 400), "tfmg:lithium_fire": (800, 3, 400), "tfmg:lithium_torch": (40, 1),
}

# The references every figure below comes from, cited by key.
REFERENCES = {
    "CRC": "CRC Handbook of Chemistry and Physics, 97th ed. (2016): constants, and concentrative properties of aqueous solutions",
    "Perry": "Perry's Chemical Engineers' Handbook, 9th ed. (2019), section 2",
    "NIST": "NIST Chemistry WebBook, SRD 69: thermophysical properties of fluids",
    "SDS": "Supplier safety data sheets (Sigma-Aldrich, Fisher, OxyChem, Daihachi, ExxonMobil): grades, flash points, autoignition",
    "ASTM": "ASTM D1655, D975, D396 and D4814: jet fuel, diesel, fuel oil and gasoline",
    "USGS": "USGS Volcano Hazards Program: basaltic lava",
    "HI": "ANSI/HI 9.6.7 (Hydraulic Institute, 2015): viscosity and pump performance",
}
# Every fluid: density kg/m³ (gases at 20 °C and 1 atm), viscosity mPa·s at 20 °C (a hot stream at its own temperature),
# freezing and boiling point °C at 1 atm (None: it has none, decomposes, or no figure is published), flash point and
# autoignition temperature °C (None: not flammable), the stream's own temperature where it leaves its process hot, flags,
# source and what the figures stand for. "aq" marks a water-based fluid, the ones that freeze in the pipe; "toxic" one that
# poisons whoever breathes it. Corrosive and fuming are what separation.Hazards and Acids make them, below.
LIQUID = "aq"
RARE_EARTH_LIQUOR = (1300, 2.4, -15, 104, None, None, None, {LIQUID}, "CRC", "1.5 M rare earth chloride at pH 1, estimated from the chlorides' concentrative tables")
CRUDE_LIQUOR = (1350, 3.0, -15, 104, None, None, None, {LIQUID}, "CRC", "the leach liquor with its iron, aluminium, thorium and fines")
PGM_LIQUOR = (1150, 1.5, -55, 108, None, None, None, {LIQUID}, "Perry", "platinum metal chlorides in 6 M (20%) hydrochloric acid")
KEROSENE = (800, 1.6, -40, 150, 38, 210)
EXTRACTANT = (*KEROSENE[2:], None, set(), "SDS", "30% extractant in kerosene, flash point the diluent's")
OURS = {
    **{id: RARE_EARTH_LIQUOR for id in LIQUORS},
    "nickel_copper_sulfate": (1250, 1.9, -4, 102, None, None, None, {LIQUID}, "CRC", "sulfate leach of about 80 g/L nickel with copper"),
    "nickel_sulfate_liquor": (1220, 1.8, -4, 102, None, None, None, {LIQUID}, "CRC", "nickel sulfate, about 80 g/L nickel"),
    "cobalt_chloride_liquor": (1200, 1.8, -12, 104, None, None, None, {LIQUID}, "CRC", "1.5 M cobalt chloride"),
    "platinum_palladium_liquor": PGM_LIQUOR, "palladium_liquor": PGM_LIQUOR, "iridium_rhodium_liquor": PGM_LIQUOR, "rhodium_liquor": PGM_LIQUOR,
    "palladium_tetrammine_liquor": (1040, 1.1, -6, 100, None, None, None, {LIQUID}, "CRC", "palladium tetrammine chloride in dilute ammonia"),
    "beryllium_sulfate_liquor": (1280, 2.6, -8, 103, None, None, None, {LIQUID, "toxic"}, "SDS", "beryllium sulfate in sulfuric acid; its mist is fatal to breathe"),
    "lithium_sulfate_liquor": (1180, 1.6, -8, 102, None, None, None, {LIQUID}, "CRC", "lithium sulfate leach, about 25 g/L lithium"),
    "p204": (850, 3.5, *EXTRACTANT[:-1], "30% D2EHPA (P204) in kerosene, flash point the diluent's"),
    "p507": (845, 3.2, *EXTRACTANT),
    "naphthenic_acid": (850, 4.0, *EXTRACTANT[:-1], "20% naphthenic acid and 15% isooctanol in kerosene"),
    "fouled_p204": (860, 7.0, *EXTRACTANT[:-1], "P204 thickened by crud at the interface"),
    "fouled_p507": (855, 6.5, *EXTRACTANT[:-1], "P507 thickened by crud at the interface"),
    "fouled_naphthenic_acid": (860, 8.0, *EXTRACTANT[:-1], "naphthenic acid thickened by crud at the interface"),
    "hydrochloric_acid": (1179, 1.9, -30, 61, None, None, None, {LIQUID}, "Perry", "36%, concentrated and fuming: past 61 °C its hydrogen chloride boils out"),
    "nitric_acid": (1410, 2.0, -41, 121, None, None, None, {LIQUID}, "CRC", "68%, the azeotrope"),
    "phosphoric_acid": (1579, 14, -17, 135, None, None, None, {LIQUID}, "Perry", "75% (54% P2O5), wet-process merchant grade; 85% freezes at 21 °C"),
    "hydrofluoric_acid": (1150, 1.0, -35, 108, None, None, None, {LIQUID, "toxic"}, "SDS", "48%"),
    "aqua_regia": (1210, 1.8, -42, 108, None, None, None, {LIQUID}, "SDS", "three of 36% hydrochloric to one of 68% nitric"),
    "bromine": (3103, 0.94, -7.2, 58.8, None, None, None, {"toxic"}, "CRC", "the element, a liquid"),
    "argon": (1.66, 0.0223, -189.3, -185.8, None, None, None, set(), "NIST", "gas"),
    "chlorine": (2.99, 0.0134, -101.5, -34.0, None, None, None, {"toxic"}, "NIST", "gas, two and a half times as heavy as air"),
    "water_gas": (0.66, 0.0145, -205, -191.5, -191.5, 500, None, {"toxic"}, "NIST", "carbon monoxide and hydrogen, half and half; flammable gas"),
    "ammonia": (0.72, 0.0099, -77.7, -33.3, None, None, None, {"toxic"}, "NIST", "gas"),
    "osmium_tetroxide": (5100, None, 40.6, 129.7, None, None, None, {"toxic"}, "CRC", "a solid at room temperature that sublimes; the vapour blinds"),
    "ruthenium_tetroxide": (3290, None, 25.4, 40, None, None, None, {"toxic"}, "CRC", "melts at 25 °C, boils at 40, explodes past 108"),
    "nickel_carbonyl": (1319, None, -19.3, 43, -20, 60, None, {"toxic"}, "CRC", "a liquid boiling at 43 °C; NIOSH: flash point -20 °C, explodes at 60"),
    "spent_liquor": (1060, 1.1, -5, 101, None, None, None, {LIQUID}, "CRC", "raffinate: dilute hydrochloric acid with ammonium and calcium chlorides"),
    "calcium_chloride_liquor": (1180, 2.0, -19, 105, None, None, None, {LIQUID}, "CRC", "20% calcium chloride"),
    "bittern": (1260, 3.0, -30, 110, None, None, None, {LIQUID}, "Perry", "bittern at 30 °Bé, mostly magnesium chloride"),
    "salt_brine": (1197, 1.9, -21.1, 108.8, None, None, None, {LIQUID}, "CRC", "saturated sodium chloride, 26%; the eutectic is -21.1 °C"),
    "seawater": (1025, 1.08, -1.9, 100.6, None, None, None, {LIQUID}, "CRC", "35 g/kg salinity"),
    "crude_rare_earth_liquor": CRUDE_LIQUOR, "crude_heavy_rare_earth_liquor": CRUDE_LIQUOR,
    "ethylhexanol": (833, 9.8, -76, 184.6, 73, 290, None, set(), "SDS", "neat 2-ethylhexanol"),
    "phosphorus_trichloride": (1574, 0.65, -93.6, 76.1, None, None, None, {"toxic"}, "CRC", "neat; fatal to breathe, fumes in moist air"),
    "d2ehpa": (975, 35, -60, None, 206, None, None, set(), "SDS", "neat D2EHPA; decomposes before it boils, the freezing point its pour point"),
    "ehehpa": (950, 36, -60, None, 190, None, None, set(), "SDS", "neat EHEHPA (PC-88A); decomposes before it boils"),
    "titanium_tetrachloride": (1726, 0.83, -24.1, 136.4, None, None, None, set(), "CRC", "neat; fumes to hydrogen chloride in moist air"),
    "vinyl_chloride": (2.6, 0.0108, -153.8, -13.4, -78, 472, None, set(), "NIST", "gas, liquefied under pressure; flammable"),
    "caustic_soda": (1274, 4.4, -17, 112, None, None, None, {LIQUID}, "SDS", "25% sodium hydroxide; 50% freezes at 12 °C"),
    "sodium_tungstate_liquor": (1200, 2.0, -10, 104, None, None, None, {LIQUID}, "Perry", "sodium tungstate digest, about 150 g/L WO3 in caustic"),
    "sodium_aluminate_liquor": (1320, 12, -15, 112, None, None, 145, {LIQUID}, "Perry",
                                "Bayer pregnant liquor, about 150 g/L Na2O, leaving the digester at 145 °C under pressure"),
}
# What else the plant handles: vanilla's, and The Factory Must Grow's oils, fuels, gases and melts. Name, then as above.
GAS = "gas at 20 °C and 1 atm"
FOREIGN = {
    "minecraft:water": ("Water", 998.2, 1.002, 0, 100, None, None, None, {LIQUID}, "CRC", "fresh water"),
    "minecraft:lava": ("Lava", 2700, 100000, 1000, None, None, None, 1150, set(), "USGS", "basalt melt, about 100 Pa·s; it sets near 1,000 °C"),
    "tfmg:crude_oil": ("Crude Oil", 870, 10, -20, 35, -20, None, None, set(), "SDS", "medium crude; the freezing point its pour point, the boiling point where it starts"),
    "tfmg:heavy_oil": ("Heavy Oil", 990, 3000, 15, 350, 66, 407, None, set(), "ASTM", "residual fuel oil, No. 6; the freezing point its pour point"),
    "tfmg:lubrication_oil": ("Lubrication Oil", 870, 150, -15, None, 220, 370, None, set(), "SDS", "ISO VG 68 mineral oil"),
    "tfmg:creosote": ("Creosote", 1080, 10, None, 200, 74, 335, None, set(), "SDS", "coal-tar creosote"),
    "tfmg:naphtha": ("Naphtha", 720, 0.6, None, 30, -22, 290, None, set(), "SDS", "light naphtha"),
    "tfmg:gasoline": ("Gasoline", 740, 0.6, None, 35, -43, 280, None, set(), "ASTM", "the boiling point where it starts"),
    "tfmg:kerosene": ("Kerosene", *KEROSENE, None, set(), "ASTM", "Jet A; the boiling point where it starts"),
    "tfmg:diesel": ("Diesel", 840, 4.0, -15, 180, 52, 210, None, set(), "ASTM", "No. 2 diesel; the freezing point its cloud point"),
    "tfmg:napalm": ("Napalm", 800, 20000, None, 35, -43, 280, None, set(), "SDS", "gasoline thickened with polystyrene, napalm-B"),
    "tfmg:cooling_fluid": ("Cooling Fluid", 1070, 3.8, -37, 107, None, None, None, {LIQUID}, "CRC", "50% ethylene glycol in water"),
    "tfmg:sulfuric_acid": ("Sulfuric Acid", 1830, 23, -32, 280, None, None, None, {LIQUID, "corrosive"}, "CRC", "93% (66 °Bé); 98% freezes at 3 °C"),
    "tfmg:liquid_concrete": ("Liquid Concrete", 2400, 50000, -2, 100, None, None, None, {LIQUID}, "Perry", "fresh concrete, its pore water freezing first"),
    "tfmg:liquid_asphalt": ("Liquid Asphalt", 1020, 300, 50, None, 230, 485, 160, set(), "SDS", "bitumen laid at 160 °C; sets at its softening point"),
    "tfmg:molten_plastic": ("Molten Plastic", 760, 1000000, 130, None, 340, 350, 230, set(), "Perry", "polyethylene melt at 230 °C, about 1,000 Pa·s"),
    "tfmg:molten_steel": ("Molten Steel", 7000, 6, 1500, 2860, None, None, 1600, set(), "CRC", "tapped at 1,600 °C"),
    "tfmg:molten_slag": ("Molten Slag", 2800, 500, 1350, None, None, None, 1500, set(), "Perry", "blast furnace slag at 1,500 °C"),
    "tfmg:liquid_silicon": ("Liquid Silicon", 2570, 0.88, 1414, 3265, None, None, 1450, set(), "CRC", "just past its melting point"),
    "tfmg:air": ("Air", 1.204, 0.0181, None, -194.3, None, None, None, set(), "NIST", GAS),
    "tfmg:hot_air": ("Hot Air", 0.277, 0.049, None, -194.3, None, None, 1000, set(), "NIST", "hot blast at 1,000 °C"),
    "tfmg:carbon_dioxide": ("Carbon Dioxide", 1.84, 0.0147, None, -78.5, None, None, None, set(), "NIST", "gas; sublimes at -78.5 °C"),
    "tfmg:neon": ("Neon", 0.84, 0.0313, -248.6, -246.1, None, None, None, set(), "NIST", GAS),
    "tfmg:hydrogen": ("Hydrogen", 0.0838, 0.0088, -259.2, -252.9, -252.9, 500, None, set(), "NIST", "flammable gas"),
    "tfmg:furnace_gas": ("Furnace Gas", 1.25, 0.017, None, -191, -191, 630, None, {"toxic"}, "NIST", "blast furnace gas, a fifth carbon monoxide; flammable"),
    "tfmg:ethylene": ("Ethylene", 1.17, 0.0101, -169.2, -103.7, -136, 450, None, set(), "NIST", "flammable gas"),
    "tfmg:propylene": ("Propylene", 1.75, 0.0083, -185.2, -47.6, -108, 455, None, set(), "NIST", "flammable gas"),
    "tfmg:propane": ("Propane", 1.83, 0.0080, -187.7, -42.1, -104, 470, None, set(), "NIST", "flammable gas"),
    "tfmg:butane": ("Butane", 2.48, 0.0074, -138.3, -0.5, -60, 405, None, set(), "NIST", "flammable gas"),
    "tfmg:lpg": ("LPG", 2.0, 0.0078, -188, -42, -104, 450, None, set(), "NIST", "propane and butane"),
}
# What Hazards and Acids make corrosive and fuming: the kinds that eat copper (Hazards.corrodes), caustic that eats aluminium,
# chlorine that eats titanium, and the acids Acids.all() fumes. LiquidTests holds the two to each other.
CORRODES = ("ACID", "LIQUOR", "CRUDE", "WASTE", "SALINE", "WATER", "CAUSTIC")
FUMES = ("hydrochloric_acid", "hydrofluoric_acid", "nitric_acid", "aqua_regia", "bromine")


def liquids():
    """The table: ours from the reagent table, then everything else, as the data map's values keyed by fluid."""
    missing = set(FLUIDS) ^ set(OURS)
    if missing:
        raise SystemExit(f"reagents without liquid properties, or properties without a reagent: {sorted(missing)}")
    rows = {}
    for id, (density, viscosity, freezes, boils, flash, auto, celsius, flags, source, basis) in OURS.items():
        kind = FLUIDS[id][2]
        hazards = set(flags) | ({"corrosive"} if kind in CORRODES or id == "chlorine" else set()) | ({"fuming"} if id in FUMES else set())
        rows[f"fundamentals:{id}"] = (FLUIDS[id][0], density, viscosity, freezes, boils, flash, auto, celsius, hazards, source, basis)
    for id, row in FOREIGN.items():
        rows[id] = row
    return rows


def liquid_value(density, viscosity, freezes, boils, flash, auto, celsius, flags):
    value = {"density": density, **({"viscosity": viscosity} if viscosity is not None else {})}
    for key, v in (("freezes", freezes), ("boils", boils), ("flash_point", flash), ("autoignition", auto), ("celsius", celsius)):
        if v is not None:
            value[key] = v
    for flag, key in ((LIQUID, "aqueous"), ("toxic", "toxic"), ("corrosive", "corrosive"), ("fuming", "fuming")):
        if flag in flags:
            value[key] = True
    return value


def liquid_lang(lang):
    g = "goggles.fundamentals.liquid"
    lang[f"{g}.body"] = "%s kg/m³, %s mPa·s"
    lang[f"{g}.dense"] = "%s kg/m³"
    lang[f"{g}.range"] = "Freezes %s °C, boils %s °C"
    lang[f"{g}.freezes"] = "Freezes %s °C"
    lang[f"{g}.boils"] = "Boils %s °C"
    lang[f"{g}.flammable"] = "flammable from %s °C"
    lang[f"{g}.toxic"] = "toxic"
    lang[f"{g}.fuming"] = "fuming"
    lang[f"{g}.corrosive"] = "corrosive"
    lang[f"{g}.frozen"] = "Frozen at %s °C: warm it past %s °C and it flows"
    lang[f"{g}.boiling"] = "Past its boiling point at %s °C: an open basin boils it off"
    lang[f"{g}.hot"] = "Running at %s °C"
    lang[f"{g}.viscous"] = "Viscous: a pump moves it at %s%%"


THERMOMETER_RECIPES = DATA / "recipe/thermometers"
# The gauges, as heat.Thermometer lists them: name, and what stands over the andesite casing in the recipe, top to bottom,
# where Create's speedometer has its compass, and anything beside it. Mercury in a glass tube; kerosene in a glass tube,
# red dye beside it; a strip of brass on steel; chromel and alumel ingots drawn to the two legs of a type K thermocouple; a
# rhodium nugget alloyed into platinum for the positive leg of type S, a platinum nugget for the negative.
THERMOMETERS = {
    "mercury_thermometer": ("Mercury Thermometer", {"item": "minecraft:glass_pane"}, {"item": "fundamentals:mercury"}),
    "spirit_thermometer": ("Spirit Thermometer", {"item": "minecraft:glass_pane"}, {"tag": "c:buckets/kerosene"}, {"tag": "c:dyes/red"}),
    "bimetallic_thermometer": ("Bimetallic Thermometer", {"tag": "c:plates/brass"}, {"tag": "c:plates/iron"}),
    "type_k_thermocouple": ("Type K Thermocouple", {"tag": "c:ingots/chromel"}, {"tag": "c:ingots/alumel"}),
    "type_s_thermocouple": ("Type S Thermocouple", {"tag": "c:nuggets/rhodium"}, {"tag": "c:nuggets/platinum"}),
}
# Cut to paint_thermometers.py's numbers: the dial's centre is the block's, the bezel a pixel wide round a 10-pixel face.
BODY = ([2, 2, 12], [14, 14, 16])
BEZEL = [([2, 13, 11], [14, 14, 12]), ([2, 2, 11], [14, 3, 12]), ([13, 3, 11], [14, 13, 12]), ([2, 3, 11], [3, 13, 12])]
NEEDLE = [([7.625, 6.75, 11.4], [8.375, 12.5, 11.8], [0, 0, 2, 8]), ([7.25, 7.25, 11.2], [8.75, 8.75, 11.9], [10, 0, 12, 2])]
FACES = ("north", "south", "east", "west", "up", "down")


def casing_elements():
    body = {"from": BODY[0], "to": BODY[1], "faces": {
        face: {"texture": "#dial", "uv": [2, 2, 14, 14]} if face == "north" else
        {"texture": "#casing", **({"cullface": "south"} if face == "south" else {})} for face in FACES}}
    bezel = [{"from": a, "to": b, "faces": {face: {"texture": "#casing"} for face in FACES if face != "south"}} for a, b in BEZEL]
    return [body] + bezel


def needle_elements(angle=0):
    rotation = {"rotation": {"angle": angle, "axis": "z", "origin": [8, 8, 11.5]}} if angle else {}
    return [{"from": a, "to": b, **rotation, "faces": {face: {"texture": "#needle", "uv": uv} for face in FACES}} for a, b, uv in NEEDLE]


def thermometers(lang):
    """A small andesite gauge mounted on a block's face, facing out from it, reading the block behind it; its needle is drawn
    by ThermometerRenderer from the needle model, and the item carries the needle fixed a third of the way up the scale."""
    shutil.rmtree(THERMOMETER_RECIPES, ignore_errors=True)
    needle = {"needle": "fundamentals:block/thermometer_needle"}
    casing = {"casing": "fundamentals:block/thermometer_casing", "particle": "fundamentals:block/thermometer_casing"}
    write(ASSETS / "models/block/thermometer_needle.json", {"textures": needle, "elements": needle_elements()})
    write(ASSETS / "models/block/thermometer.json", {"parent": "minecraft:block/block", "textures": casing, "elements": casing_elements()})
    write(ASSETS / "models/item/thermometer.json", {"parent": "minecraft:block/block", "textures": {**casing, **needle},
                                                    "elements": casing_elements() + needle_elements(-45)})
    for name, (display, upper, lower, *beside) in THERMOMETERS.items():
        dial = {"dial": f"fundamentals:block/{name}_dial"}
        write(ASSETS / f"models/block/{name}.json", {"parent": "fundamentals:block/thermometer", "textures": dial})
        write(ASSETS / f"models/item/{name}.json", {"parent": "fundamentals:item/thermometer", "textures": dial})
        write(ASSETS / f"blockstates/{name}.json", {"variants": {
            f"facing={facing}": {"model": f"fundamentals:block/{name}", **rotation}
            for facing, rotation in (("north", {}), ("east", {"y": 90}), ("south", {"y": 180}), ("west", {"y": 270}),
                                     ("up", {"x": 270}), ("down", {"x": 90}))}})
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [drop_self(name)]})
        write(THERMOMETER_RECIPES / f"{name}.json", {
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["U ", "LB", "A "] if beside else ["U", "L", "A"],
            "key": {"U": upper, "L": lower, **({"B": beside[0]} if beside else {}), "A": {"item": "create:andesite_casing"}},
            "result": {"id": f"fundamentals:{name}", "count": 1}})
        lang[f"block.fundamentals.{name}"] = display
    lang["goggles.fundamentals.thermometer.over"] = "Off the scale, past %s °C"
    lang["goggles.fundamentals.thermometer.under"] = "Off the scale, under %s °C"
    lang["goggles.fundamentals.thermometer.range"] = "Reads %s to %s °C"


def main():
    (DATA / "data_maps/block/heat_source.json").unlink(missing_ok=True)
    write(ROOT / "data/fundamentalmagic/data_maps/block/heat_source.json", {"replace": False, "values": {
        block: {"celsius": c, "reach": r, **({"outside": o[0]} if o else {})} for block, (c, r, *o) in SOURCES.items()}})
    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    lang["heat.fundamentals.readout"] = "%s °C (%s °F)"
    lang["goggles.fundamentals.heat"] = "Here %s °C"
    thermometers(lang)
    rows = liquids()
    write(DATA / "data_maps/fluid/liquid_properties.json", {"replace": False, "values": {
        id: liquid_value(*row[1:9]) for id, row in rows.items()}})
    liquid_lang(lang)
    write(path, lang)
    print(f"{len(SOURCES)} heat sources, {len(THERMOMETERS)} thermometers, {len(rows)} liquids")


if __name__ == "__main__":
    main()
