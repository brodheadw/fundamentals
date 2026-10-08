#!/usr/bin/env python3
"""The heat sources: what each block adds to the temperature around it, °C above ambient at the block itself and
just outside it, falling to nothing `reach` blocks away. The inside is the real flame or chamber; the outside is
what the walls let through, so standing by a furnace is hot, not a kiln. A data map, so another mod can add its
blocks with a file. Re-run after any edit.

    python3 tools/build_heat_data.py
"""
import json

from build_ore_data import ASSETS, DATA, write

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


def main():
    write(DATA / "data_maps/block/heat_source.json", {"replace": False, "values": {
        block: {"celsius": c, "reach": r, **({"outside": o[0]} if o else {})} for block, (c, r, *o) in SOURCES.items()}})
    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    lang["heat.fundamentals.readout"] = "%s °C (%s °F)"
    lang["goggles.fundamentals.heat"] = "Here %s °C"
    write(path, lang)
    print(f"{len(SOURCES)} heat sources")


if __name__ == "__main__":
    main()
