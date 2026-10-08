#!/usr/bin/env python3
"""The heat sources: what each block adds to the temperature around it, °C above ambient at the block itself,
falling to nothing `reach` blocks away. A data map, so another mod can add its blocks with a file. Re-run after
any edit.

    python3 tools/build_heat_data.py
"""
import json

from build_ore_data import ASSETS, DATA, write

SOURCES = {
    "minecraft:lava": (1100, 4), "minecraft:lava_cauldron": (600, 3), "minecraft:magma_block": (200, 2),
    "minecraft:fire": (600, 3), "minecraft:soul_fire": (500, 3), "minecraft:campfire": (300, 3), "minecraft:soul_campfire": (250, 3),
    "minecraft:furnace": (400, 3), "minecraft:blast_furnace": (600, 3), "minecraft:smoker": (300, 3), "minecraft:torch": (40, 1), "minecraft:wall_torch": (40, 1),
    "minecraft:ice": (-10, 2), "minecraft:packed_ice": (-15, 2), "minecraft:blue_ice": (-20, 3), "minecraft:powder_snow": (-15, 2), "minecraft:snow_block": (-5, 1),
    # Create's burner at its kindled level; the Java scales it by state, smouldering a quarter, seething double
    "create:blaze_burner": (600, 4),
    "fundamentals:bloomery": (500, 3),
}


def main():
    write(DATA / "data_maps/block/heat_source.json", {"replace": False, "values": {
        block: {"celsius": c, "reach": r} for block, (c, r) in SOURCES.items()}})
    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    lang["heat.fundamentals.readout"] = "%s °C (%s °F)"
    lang["goggles.fundamentals.heat"] = "Here %s °C"
    write(path, lang)
    print(f"{len(SOURCES)} heat sources")


if __name__ == "__main__":
    main()
