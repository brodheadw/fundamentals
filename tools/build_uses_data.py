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
ITEMS = {"phosphor": "Phosphor", "didymium_glass": "Didymium Glass", "roasted_cobaltite": "Roasted Cobaltite",
         "roasted_chalcopyrite": "Roasted Chalcopyrite", "rhenium_flue_dust": "Rhenium Flue Dust",
         "tungsten_carbide": "Tungsten Carbide", "tungsten_filament": "Tungsten Filament", "clarifier_sludge": "Clarifier Sludge"}
ROASTING = DATA / "recipe/roasting"


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
    mixing("samarium_cobalt", item("samarium_ingot") + item("cobalt_ingot", 4), [result("samarium_cobalt_ingot", 2)], "superheated")
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
    mixing("phosphor", item("yttrium_oxide", 2) + item("europium_oxide") + item("terbium_oxide"), [result("phosphor", 4)], "superheated")
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


def roast(ore, roasted):
    """A sulfide ore roasted on a campfire or in a smoker, as galena is, to drive off its sulfur (and arsenic)."""
    for kind, time in (("campfire_cooking", 400), ("smoking", 200)):
        write(ROASTING / f"{roasted}_{kind}.json", {"type": f"minecraft:{kind}", "category": "misc", "ingredient": {"item": f"fundamentals:raw_{ore}"},
                                                   "result": {"id": f"fundamentals:{roasted}"}, "experience": 0.1, "cookingtime": time})


def vat(name, items, fluid_id, amount, results, machines=("tfmg:mixing",), heat="heated"):
    write(USES / f"{name}.json", {"type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"],
                                  "heat_requirement": heat, "machines": list(machines), "min_size": 1, "processing_time": 100,
                                  "ingredients": items + [{"type": "neoforge:single", "amount": amount, "fluid": fluid_id}], "results": results})


def cobalt():
    """Cobaltite roasted of its arsenic and sulfur, then blasted to the metal. Cobalt blue is the oxide calcined with alumina."""
    roast("cobaltite", "roasted_cobaltite")
    write(USES / "cobalt_ingot.json", {"type": "minecraft:blasting", "category": "misc", "ingredient": {"item": "fundamentals:roasted_cobaltite"},
                                       "result": {"id": "fundamentals:cobalt_ingot"}, "experience": 0.7, "cookingtime": 200})
    mixing("cobalt_blue", item("roasted_cobaltite") + item("tfmg:bauxite_powder", 2), [result("minecraft:blue_dye", 4)], "superheated")


def copper_molybdenum_rhenium():
    """The porphyry chain. Chalcopyrite roasts to a copper oxide the bloomery smelts, its iron going to slag. Molybdenite
    roasts to molybdenum trioxide, and the rhenium in it leaves up the flue: the roaster's dust is where every gram of
    rhenium on earth comes from. Both oxides are reduced under hydrogen, as the industry does, in a heated vat."""
    roast("chalcopyrite", "roasted_chalcopyrite")
    write(DATA / "recipe/bloomery/copper_from_roasted_chalcopyrite.json", {"type": "fundamentals:bloomery", "ingredient": {"item": "fundamentals:roasted_chalcopyrite"},
                                                                          "result": {"id": "minecraft:copper_ingot", "count": 1}, "byproduct": {"id": "fundamentals:slag", "count": 1}})
    mixing("molybdenum_oxide", item("raw_molybdenite", 2), [result("molybdenum_oxide", 2), {"id": "fundamentals:rhenium_flue_dust", "chance": 0.5}], "heated")
    vat("molybdenum_ingot", item("molybdenum_oxide", 2), "tfmg:hydrogen", 500, [result("molybdenum_ingot", 2)])
    vat("rhenium_ingot", item("rhenium_flue_dust", 2), "tfmg:hydrogen", 250, [result("rhenium_ingot")])


def alloys():
    """Where cobalt, molybdenum and rhenium go: the nickel superalloy of turbine blades, and molybdenum steel for the heavy casings."""
    mixing("superalloy", item("tfmg:nickel_ingot", 4) + item("cobalt_ingot", 2) + item("rhenium_ingot"), [result("superalloy_ingot", 4)], "superheated")
    mixing("molybdenum_steel", item("molybdenum_ingot") + tag("c:ingots/steel", 4), [result("molybdenum_steel_ingot", 4)], "superheated")
    shaped(TFMG / "turbine_blade.json", ["III", "ISI", "III"], {"S": {"item": "create:shaft"}, "I": {"tag": "c:plates/superalloy"}},
           {"count": 1, "id": "tfmg:turbine_blade", "components": {"tfmg:fuel_tags": {"kerosene": "c:kerosene"}, "tfmg:fuels": {"kerosene": "Kerosene"}}})
    write(TFMG / "item_application/heavy_machinery_casing.json", {"type": "create:item_application",
          "ingredients": [{"item": "tfmg:steel_casing"}, {"tag": "c:plates/molybdenum_steel"}], "results": [{"id": "tfmg:heavy_machinery_casing"}]})


def tungsten():
    """Scheelite and wolframite decompose in hot hydrochloric acid to tungstic acid, which the heat takes to the trioxide; hydrogen
    reduces the trioxide to the metal. The metal is drawn to the filament every light bulb burns, and carburised to the carbide
    every drill bites with."""
    for ore in ("scheelite", "wolframite"):
        mixing(f"tungsten_oxide_from_{ore}", item(f"raw_{ore}", 2) + [{"type": "neoforge:single", "amount": 500, "fluid": "fundamentals:hydrochloric_acid"}],
               [result("tungsten_oxide", 2)], "heated")
    vat("tungsten_ingot", item("tungsten_oxide", 2), "tfmg:hydrogen", 500, [result("tungsten_ingot", 2)])
    mixing("tungsten_carbide", item("tungsten_ingot") + tag("minecraft:coals", 2), [result("tungsten_carbide", 2)], "superheated")
    write(USES / "tungsten_filament.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": item("tungsten_ingot"), "result": result("tungsten_filament", 4)})
    shaped(TFMG / "crafting/materials/light_bulb.json", ["CWC", "CGC", "NNN"],
           {"C": {"tag": "c:nuggets/copper"}, "G": {"item": "create:framed_glass"}, "N": {"tag": "c:nuggets/steel"}, "W": {"item": "fundamentals:tungsten_filament"}},
           {"count": 2, "id": "tfmg:light_bulb"})
    shaped(CREATE / "crafting/kinetics/mechanical_drill.json", [" A ", "AIA", " C "],
           {"A": {"item": "create:andesite_alloy"}, "C": {"item": "create:andesite_casing"}, "I": {"item": "fundamentals:tungsten_carbide"}},
           {"count": 1, "id": "create:mechanical_drill"})


def more_sinks():
    """Cerium oxide is the oxygen store of every catalytic converter: the Factory's exhaust takes two. Lithium cobalt oxide is the cathode
    the first lithium batteries ran on: the lithium charge takes a cobalt. Neodymium and holmium colour glass, as erbium does."""
    shaped(TFMG / "crafting/materials/exhaust.json", ["BPB", "EPE", "CPC"],
           {"B": {"item": "minecraft:iron_bars"}, "C": {"tag": "c:ingots/cast_iron"}, "P": {"item": "tfmg:cast_iron_pipe"}, "E": {"item": "fundamentals:cerium_oxide"}},
           {"count": 1, "id": "tfmg:exhaust"})
    shaped(TFMG / "crafting/materials/lithium_charge.json", [" P ", "LKL", " A "],
           {"A": {"tag": "c:plates/aluminum"}, "L": {"tag": "c:ingots/lithium"}, "P": {"item": "tfmg:plastic_sheet"}, "K": {"item": "fundamentals:cobalt_ingot"}},
           {"count": 1, "id": "tfmg:lithium_charge"})
    for oxide, glass in (("neodymium", "purple"), ("holmium", "yellow")):
        shaped(USES / f"{oxide}_glass.json", ["GGG", "GEG", "GGG"], {"G": {"tag": "c:glass_blocks/colorless"}, "E": {"item": f"fundamentals:{oxide}_oxide"}},
               {"count": 8, "id": f"minecraft:{glass}_stained_glass"})


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
    for stale in ROASTING.glob("roasted_c*"):
        stale.unlink()
    magnets()
    cerium()
    lanthanum()
    phosphors()
    yttrium()
    glass()
    scandium()
    cobalt()
    copper_molybdenum_rhenium()
    alloys()
    tungsten()
    more_sinks()
    names()
    print("uses written")


if __name__ == "__main__":
    main()
