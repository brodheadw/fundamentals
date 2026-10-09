#!/usr/bin/env python3
"""Writes the advancements: a handful of milestones, not a checklist. Re-run after adding a
mineral, a grinding recipe or a rare earth.

    python3 tools/build_advancements.py
"""
import json
import shutil

from build_ore_data import ASSETS, DATA, ORES, write
from paint_materials import MATERIALS

OUT = DATA / "advancement"
RAW = [f"fundamentals:raw_{mineral}" for mineral in ORES]
RARE_EARTHS = [name for name, forms in MATERIALS.items() if "oxalate" in forms and name != "didymium"]
GRINDING = sorted(f"fundamentals:grinding/{p.stem}" for p in (DATA / "recipe/grinding").glob("*.json"))


def have(*items):
    return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": list(items)}]}}


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
               {"magnet": have("fundamentals:neodymium_iron_boron_ingot", "fundamentals:samarium_cobalt_ingot")}, False),
}


def main():
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
    write(lang_path, dict(sorted(lang.items())))
    print(f"wrote {len(ADVANCEMENTS)} advancements")


if __name__ == "__main__":
    main()
