#!/usr/bin/env python3
"""The heat sources: what each block adds to the temperature around it, °C above ambient at the block itself and
just outside it, falling to nothing `reach` blocks away. The inside is the real flame or chamber; the outside is
what the walls let through, so standing by a furnace is hot, not a kiln. A data map, so another mod can add its
blocks with a file. And the dial thermometers that read it: their models, blockstates, loot, recipes and names.
Re-run after any edit.

    python3 tools/paint_thermometers.py && python3 tools/build_heat_data.py
"""
import json
import shutil

from build_ore_data import ASSETS, DATA, drop_self, write

SOURCES = {
    "minecraft:lava": (1150, 4, 300), "minecraft:lava_cauldron": (1150, 3, 200), "minecraft:magma_block": (700, 2, 100),
    "minecraft:fire": (800, 3, 400), "minecraft:soul_fire": (700, 3, 350), "minecraft:campfire": (800, 3, 100), "minecraft:soul_campfire": (700, 3, 80),
    "minecraft:furnace": (750, 3, 60), "minecraft:blast_furnace": (1500, 3, 100), "minecraft:smoker": (600, 3, 50),
    "minecraft:torch": (40, 1), "minecraft:wall_torch": (40, 1),
    "minecraft:ice": (-10, 2), "minecraft:packed_ice": (-15, 2), "minecraft:blue_ice": (-20, 3), "minecraft:powder_snow": (-15, 2), "minecraft:snow_block": (-5, 1),
    # Create's burner kindled, what a heated recipe gets; the Java scales it by state, seething 1.6 times for superheated
    "create:blaze_burner": (1000, 4, 150),
    "fundamentals:bloomery": (1200, 3, 100),
    "tfmg:molten_steel": (1550, 4, 300), "tfmg:molten_slag": (1450, 4, 300),
    "tfmg:blue_fire": (800, 3, 400), "tfmg:green_fire": (800, 3, 400), "tfmg:lithium_fire": (800, 3, 400), "tfmg:lithium_torch": (40, 1),
}

THERMOMETER_RECIPES = DATA / "recipe/thermometers"
# The four gauges, as heat.Thermometer lists them: name, and what stands over the andesite casing in the recipe, top to bottom,
# where Create's speedometer has its compass. Mercury in a glass tube; a strip of brass on steel; chromel and alumel ingots
# drawn to the two legs of a type K thermocouple; a rhodium nugget alloyed into platinum for the positive leg of type S, a
# platinum nugget for the negative.
THERMOMETERS = {
    "mercury_thermometer": ("Mercury Thermometer", {"item": "minecraft:glass_pane"}, {"item": "fundamentals:mercury"}),
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
    for name, (display, upper, lower) in THERMOMETERS.items():
        dial = {"dial": f"fundamentals:block/{name}_dial"}
        write(ASSETS / f"models/block/{name}.json", {"parent": "fundamentals:block/thermometer", "textures": dial})
        write(ASSETS / f"models/item/{name}.json", {"parent": "fundamentals:item/thermometer", "textures": dial})
        write(ASSETS / f"blockstates/{name}.json", {"variants": {
            f"facing={facing}": {"model": f"fundamentals:block/{name}", **rotation}
            for facing, rotation in (("north", {}), ("east", {"y": 90}), ("south", {"y": 180}), ("west", {"y": 270}),
                                     ("up", {"x": 270}), ("down", {"x": 90}))}})
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [drop_self(name)]})
        write(THERMOMETER_RECIPES / f"{name}.json", {
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["U", "L", "A"],
            "key": {"U": upper, "L": lower, "A": {"item": "create:andesite_casing"}},
            "result": {"id": f"fundamentals:{name}", "count": 1}})
        lang[f"block.fundamentals.{name}"] = display
    lang["goggles.fundamentals.thermometer.over"] = "Off the scale, past %s °C"
    lang["goggles.fundamentals.thermometer.under"] = "Off the scale, under %s °C"
    lang["goggles.fundamentals.thermometer.range"] = "Reads %s to %s °C"


def main():
    write(DATA / "data_maps/block/heat_source.json", {"replace": False, "values": {
        block: {"celsius": c, "reach": r, **({"outside": o[0]} if o else {})} for block, (c, r, *o) in SOURCES.items()}})
    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    lang["heat.fundamentals.readout"] = "%s °C (%s °F)"
    lang["goggles.fundamentals.heat"] = "Here %s °C"
    thermometers(lang)
    write(path, lang)
    print(f"{len(SOURCES)} heat sources, {len(THERMOMETERS)} thermometers")


if __name__ == "__main__":
    main()
