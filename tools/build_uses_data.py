#!/usr/bin/env python3
"""What the rare earths are for: the recipes that spend the metals and oxides, and the recipes of
Create and The Factory Must Grow we take over so that they need them. Re-run after any edit.

    python3 tools/paint_uses.py && python3 tools/build_uses_data.py

Everything of ours lands in recipe/uses/; a takeover is written at the other mod's own recipe path
under data/<mod>/, which replaces theirs.
"""
import json
import shutil

from build_ore_data import ASSETS, DATA, write

USES = DATA / "recipe/uses"
TFMG = DATA.parent / "tfmg/recipe"
CREATE = DATA.parent / "create/recipe"

# the items of ours that are not a form of a material: name -> display
ITEMS = {"phosphor": "Phosphor", "didymium_glass": "Didymium Glass"}


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


def shaped(path, pattern, key, result):
    write(path, {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key, "result": result})


def magnets():
    """Nd2Fe14B is sintered from neodymium (or didymium, as the industry does), iron and boron, with
    dysprosium to hold its field when hot; SmCo5 from samarium and cobalt. Both are then polarized into
    The Factory Must Grow's magnet, which its motors and generators are already built from, so a
    rare earth plant is what a motor needs."""
    for name, rare in (("neodymium_iron_boron", "neodymium_ingot"), ("neodymium_iron_boron_from_didymium", "didymium_ingot")):
        mixing(name, item(rare, 2) + item("dysprosium_ingot") + tag("c:ingots/iron", 4) + item("raw_borax"),
               [result("neodymium_iron_boron_ingot", 4)], "superheated")
    # there is no cobalt metal yet: cobaltite goes straight in, roasted of its arsenic and sulfur by the heat
    mixing("samarium_cobalt", item("samarium_ingot") + item("raw_cobaltite", 4), [result("samarium_cobalt_ingot", 2)], "superheated")
    write(TFMG / "polarizing/magnet.json", {"type": "tfmg:polarizing", "ingredients": tag("c:ingots/neodymium_iron_boron"),
                                            "results": [{"id": "tfmg:magnet"}]})
    write(USES / "magnet_from_samarium_cobalt.json", {"type": "tfmg:polarizing", "ingredients": tag("c:ingots/samarium_cobalt"),
                                                      "results": [{"id": "tfmg:magnet"}]})


def cerium():
    """Ferrocerium, the lighter flint: cerium alloyed with iron sparks when scraped and never wears out."""
    write(USES / "ferrocerium_striker.json", {
        "type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": item("cerium_ingot") + tag("c:ingots/iron"),
        "result": {"id": "minecraft:flint_and_steel", "components": {
            "minecraft:unbreakable": {}, "minecraft:custom_name": json.dumps({"text": "Ferrocerium Striker", "italic": False})}}})


def lanthanum():
    """Lanthanum oxide is the stabiliser of the FCC catalyst that cracks heavy oil; the Factory's cracking
    of naphtha into ethylene and propylene now spends a little of it each run."""
    write(TFMG / "vat_machine_recipe/naphtha.json", {
        "type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"],
        "heat_requirement": "heated", "machines": ["tfmg:mixing"], "min_size": 1,
        "ingredients": [{"type": "neoforge:single", "amount": 500, "fluid": "tfmg:naphtha"}] + item("lanthanum_oxide"),
        "results": [{"amount": 250, "id": "tfmg:ethylene"}, {"amount": 250, "id": "tfmg:propylene"}]})


def phosphors():
    """Europium gives the red and terbium the green, both on a yttria host: the phosphor of every
    fluorescent tube and screen. The Factory's lamps take it."""
    mixing("phosphor", item("yttrium_oxide", 2) + item("europium_oxide") + item("terbium_oxide"), [result("phosphor", 4)], "heated")
    shaped(TFMG / "crafting/materials/aluminum_lamp.json", ["P ", "BF", "S "],
           {"B": {"item": "tfmg:light_bulb"}, "P": {"item": "create:framed_glass_pane"}, "S": {"tag": "c:plates/aluminum"}, "F": {"item": "fundamentals:phosphor"}},
           {"count": 1, "id": "tfmg:aluminum_lamp"})
    shaped(TFMG / "crafting/materials/circular_light.json", ["P ", "BF", "S "],
           {"B": {"item": "tfmg:light_bulb"}, "P": {"item": "create:framed_glass"}, "S": {"tag": "c:nuggets/steel"}, "F": {"item": "fundamentals:phosphor"}},
           {"count": 1, "id": "tfmg:circular_light"})


def yttrium():
    """Yttria-stabilised zirconia is the thermal-barrier ceramic; the Factory's fireproof vat takes
    yttrium oxide in its lining."""
    shaped(TFMG / "crafting/materials/fireproof_chemical_vat.json", ["PRP", "NTN", "YHY"],
           {"H": {"item": "tfmg:heavy_machinery_casing"}, "N": {"item": "tfmg:circuit_board"}, "P": {"item": "tfmg:fireproof_bricks"},
            "R": {"item": "tfmg:rubber_sheet"}, "T": {"item": "tfmg:steel_chemical_vat"}, "Y": {"item": "fundamentals:yttrium_oxide"}},
           {"count": 1, "id": "tfmg:fireproof_chemical_vat"})


def glass():
    """Didymium glass cuts the sodium flare for glassblowers and welders: Create's goggles are made of it.
    Erbium makes glass pink."""
    write(USES / "didymium_glass.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": item("didymium_oxide") + tag("c:glass_blocks/colorless", 2), "result": result("didymium_glass", 2)})
    shaped(CREATE / "crafting/kinetics/goggles.json", [" S ", "GPG"],
           {"G": {"item": "fundamentals:didymium_glass"}, "P": {"tag": "c:plates/gold"}, "S": {"tag": "c:strings"}},
           {"count": 1, "id": "create:goggles"})
    shaped(USES / "rose_glass.json", ["GGG", "GEG", "GGG"],
           {"G": {"tag": "c:glass_blocks/colorless"}, "E": {"item": "fundamentals:erbium_oxide"}},
           {"count": 8, "id": "minecraft:pink_stained_glass"})


def scandium():
    """Al-Sc: a little scandium makes aluminium light and weldable enough for airframes; here it makes a
    rack go twice as far."""
    mixing("aluminium_scandium", item("scandium_ingot") + tag("c:ingots/aluminum", 7), [result("aluminium_scandium_ingot", 8)], "superheated")
    shaped(USES / "panel_rack_from_scandium.json", ["S S", "SSS"], {"S": {"tag": "c:plates/aluminium_scandium"}},
           {"count": 2, "id": "fundamentals:panel_rack"})


def names():
    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    for name, display in ITEMS.items():
        lang[f"item.fundamentals.{name}"] = display
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})
    write(path, lang)


def main():
    shutil.rmtree(USES, ignore_errors=True)
    shutil.rmtree(TFMG, ignore_errors=True)
    shutil.rmtree(CREATE, ignore_errors=True)
    magnets()
    cerium()
    lanthanum()
    phosphors()
    yttrium()
    glass()
    scandium()
    names()
    print("uses written")


if __name__ == "__main__":
    main()
