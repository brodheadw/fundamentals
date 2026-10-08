#!/usr/bin/env python3
"""What the rare earths are for: the recipes that spend the metals and oxides, and the recipes of
Create and The Factory Must Grow we take over so that they need them. Re-run after any edit.

    python3 tools/build_uses_data.py

Everything of ours lands in recipe/uses/; a takeover is written at the other mod's own recipe path
under data/<mod>/, which replaces theirs.
"""
import shutil

from build_ore_data import DATA, write

USES = DATA / "recipe/uses"
TFMG = DATA.parent / "tfmg/recipe"
CREATE = DATA.parent / "create/recipe"


def item(id, count=1):
    return [{"item": id if ":" in id else f"fundamentals:{id}"}] * count


def tag(name, count=1):
    return [{"tag": name}] * count


def result(id, count=1):
    return {"id": id if ":" in id else f"fundamentals:{id}", **({"count": count} if count > 1 else {})}


def mixing(name, ingredients, results, heat=None):
    recipe = {"type": "create:mixing", "ingredients": ingredients, "results": results}
    if heat:
        recipe["heat_requirement"] = heat
    write(USES / f"{name}.json", recipe)


def magnets():
    """Nd2Fe14B is sintered from neodymium (or didymium, as the industry does), iron and boron, with
    dysprosium to hold its field when hot; SmCo5 from samarium and cobalt. Both are then polarized into
    The Factory Must Grow's magnet, which its motors and generators are already built from, so a
    rare earth plant is what a motor needs."""
    for name, rare in (("neodymium_iron_boron", "neodymium_ingot"), ("neodymium_iron_boron_from_didymium", "didymium_ingot")):
        mixing(name, item(rare, 2) + item("dysprosium_ingot") + tag("c:ingots/iron", 4) + item("raw_borax"),
               [result("neodymium_iron_boron_ingot", 4)], "superheated")
    mixing("samarium_cobalt", item("samarium_ingot") + tag("c:ingots/cobalt", 4), [result("samarium_cobalt_ingot", 2)], "superheated")
    write(TFMG / "polarizing/magnet.json", {"type": "tfmg:polarizing", "ingredients": tag("c:ingots/neodymium_iron_boron"),
                                            "results": [{"id": "tfmg:magnet"}]})
    write(USES / "magnet_from_samarium_cobalt.json", {"type": "tfmg:polarizing", "ingredients": tag("c:ingots/samarium_cobalt"),
                                                      "results": [{"id": "tfmg:magnet"}]})


def main():
    shutil.rmtree(USES, ignore_errors=True)
    shutil.rmtree(TFMG, ignore_errors=True)
    shutil.rmtree(CREATE, ignore_errors=True)
    magnets()
    print("uses written")


if __name__ == "__main__":
    main()
