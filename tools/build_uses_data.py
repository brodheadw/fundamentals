#!/usr/bin/env python3
"""What the rare earths are for: the recipes that spend the metals and oxides, and the recipes of
Create and The Factory Must Grow we take over so that they need them; and the roads the base metals take
instead of Create's crushing-and-furnace shortcuts. Re-run after any edit.

    python3 tools/paint_uses.py && python3 tools/paint_plastics.py && python3 tools/build_uses_data.py

Everything of ours lands in recipe/uses/; a takeover is written at the other mod's own recipe path
under data/<mod>/, which replaces theirs.
"""
import json
import shutil
import zipfile
from pathlib import Path

from build_chemica_compat import CHEMICA_SHEETS, fluid_ingredient, optional
from build_ore_data import ASSETS, DATA, cube, drop_self, tag as tag_file, write

USES = DATA / "recipe/uses"
TFMG = DATA.parent / "tfmg/recipe"
TFMG_LOOT = DATA.parent / "tfmg/loot_table/blocks"
CREATE = DATA.parent / "create/recipe"

# the items of ours that are not a form of a material: name -> display
ITEMS = {"phosphor": "Phosphor", "didymium_glass": "Didymium Glass", "roasted_cobaltite": "Roasted Cobaltite",
         "roasted_chalcopyrite": "Roasted Chalcopyrite", "rhenium_flue_dust": "Rhenium Flue Dust",
         "tungsten_carbide": "Tungsten Carbide", "tungsten_filament": "Tungsten Filament", "clarifier_sludge": "Clarifier Sludge",
         "copper_calcine": "Copper Calcine", "zinc_oxide": "Zinc Oxide", "roasted_pentlandite": "Roasted Pentlandite",
         "lithium_chloride": "Lithium Chloride", "ferroboron": "Ferroboron",
         "soda_ash": "Soda Ash", "sodium_chromate": "Sodium Chromate", "sodium_dichromate": "Sodium Dichromate", "aluminium_powder": "Aluminium Powder",
         "roasted_tin_concentrate": "Roasted Tin Concentrate", "solder": "Solder",
         "titania_slag": "Titania Slag", "magnesium_chloride": "Magnesium Chloride",
         "silver_zinc_crust": "Silver-Zinc Crust", "litharge": "Litharge",
         "thorium_nitrate": "Thorium Nitrate", "gas_mantle": "Gas Mantle", "mercury": "Mercury",
         "crude_zirconium_tetrachloride": "Crude Zirconium Tetrachloride", "zirconium_tetrachloride": "Zirconium Tetrachloride",
         "hafnium_tetrachloride": "Hafnium Tetrachloride", "yttria_stabilised_zirconia": "Yttria-Stabilised Zirconia",
         "beryl_frit": "Beryl Frit", "beryllium_hydroxide": "Beryllium Hydroxide", "ammonium_fluoroberyllate": "Ammonium Fluoroberyllate",
         "beryllium_pebbles": "Beryllium Pebbles", "dimensionally_stable_anode": "Dimensionally Stable Anode", "red_mud": "Red Mud",
         "aluminium_hydroxide": "Aluminium Hydroxide", "alumina": "Alumina", "cryolite": "Cryolite",
         "nickel_oxide": "Nickel Oxide", "nickel_pellets": "Nickel Pellets", "tungstic_acid": "Tungstic Acid",
         "ammonium_paratungstate": "Ammonium Paratungstate", "ammonium_perrhenate": "Ammonium Perrhenate", "lithium_carbonate": "Lithium Carbonate",
         "mcraly_powder": "MCrAlY Powder", "boric_acid": "Boric Acid",
         "galvanized_steel_plate": "Galvanized Steel Plate"}
# The magnet alloys and the forms each polarizes from, in the order of magnet.MagnetGrade; the magnet is <alloy>_magnet.
MAGNET_ALLOYS = {"neodymium_iron_boron": ("ingot", "plate"), "dysprosium_neodymium_iron_boron": ("ingot", "plate"),
                 "samarium_cobalt": ("ingot", "plate"), "alnico": ("ingot",)}
MAGNET_GRADES = ("NdFeB", "Dy-NdFeB", "SmCo", "Alnico")
MAGNETS = {f"{alloy}_magnet": f"{grade} Magnet" for alloy, grade in zip(MAGNET_ALLOYS, MAGNET_GRADES)}
# The Factory's machines built round magnets that take a grade, and their recipes there.
MAGNET_MACHINES = {"electric_motor": "sequenced_assembly/motor", "generator": "sequenced_assembly/generator", "stator": "mechanical_crafting/stator",
                   "electric_pump": "crafting/materials/electric_pump", "voltmeter": "crafting/materials/voltmeter"}
# the blocks of ours that are not a form of a material: name -> display
BLOCKS = {"clarifier_sludge_block": "Clarifier Tailings"}
# The platinum refinery's items, registered by uses.PlatinumMetals in this order.
PGM_ITEMS = {"ammonium_chloride": "Ammonium Chloride", "insoluble_residue": "Insoluble Residue", "iridium_rhodium_residue": "Iridium-Rhodium Residue",
             "ammonium_chloroplatinate": "Ammonium Chloroplatinate", "dichlorodiammine_palladium": "Dichlorodiammine Palladium",
             "ammonium_chlororuthenate": "Ammonium Chlororuthenate", "ammonium_chloroiridate": "Ammonium Chloroiridate",
             "ammonium_chlororhodate": "Ammonium Chlororhodate", "reforming_catalyst": "Platinum-Rhenium Catalyst",
             "platinum_rhodium_gauze": "Platinum-Rhodium Gauze", "osmium_filament": "Osmium Filament"}
PLATINUM = DATA / "recipe/platinum"
PGMS = ("platinum", "palladium", "rhodium", "ruthenium", "iridium", "osmium")
ROASTING = DATA / "recipe/roasting"
LITHIUM = DATA / "recipe/lithium"
PLASTICS = DATA / "recipe/plastics"
TFMG_ASSETS = ASSETS.parent / "tfmg"
TFMG_JAR = next(Path.home().glob(".gradle/caches/modules-2/files-2.1/maven.modrinth/create-tfmg/*/*/create-tfmg-*.jar"))
DYES = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown",
        "green", "red", "black")
# registered by plastics.Plastics
PLASTIC_ITEMS = {"ziegler_natta_catalyst": "Ziegler-Natta Catalyst", "pvc_resin": "PVC Resin", "pvc_sheet": "PVC Sheet"}
PLASTIC_BLOCKS = {f"{dye}_plastic_block": f"{dye.replace('_', ' ').title()} Plastic Block" for dye in DYES}
# The Factory Must Grow's plastic pipe family: the models every other one of theirs inherits its render type from.
TFMG_PLASTIC_MODELS = (["plastic_pipe/core_x", "plastic_pipe/core_y", "plastic_pipe/core_z", "plastic_pipe/casing", "plastic_pipe/item",
                        "plastic_pipe/window", "plastic_mechanical_pump/block", "plastic_mechanical_pump/item", "plastic_smart_fluid_pipe/block",
                        "plastic_smart_fluid_pipe/item", "plastic_fluid_valve/item", "plastic_fluid_valve/pointer"]
                       + [f"plastic_fluid_valve/block_{axis}_{state}" for axis in ("horizontal", "vertical") for state in ("open", "closed")]
                       + [f"plastic_pipe/{part}/{side}" for part in ("connection", "drain", "rim", "rim_connector")
                          for side in ("up", "down", "north", "south", "east", "west")])


def argon(amount=100):
    return [fluid_ingredient("argon", amount)]


def item(id, count=1):
    return [{"item": id if ":" in id else f"fundamentals:{id}"}] * count


def tag(name, count=1):
    return [{"tag": name}] * count


def result(id, count=1):
    return {"id": id if ":" in id else f"fundamentals:{id}", **({"count": count} if count > 1 else {})}


def mixing(name, ingredients, results, heat=None):
    mixing_at(USES / f"{name}.json", ingredients, results, heat)


def mixing_at(path, ingredients, results, heat=None):
    recipe = {"type": "create:mixing", "ingredients": ingredients, "results": results}
    if heat:
        recipe["heat_requirement"] = heat
    write(path, recipe)


def shaped(path, pattern, key, result):
    write(path, {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key, "result": result})


def magnets():
    """Nd2Fe14B is melted from neodymium, iron and boron. Praseodymium sits in the same lattice, so the industry melts didymium
    (PrNd) as it comes off the plant, and gadolinium stands in for some of the neodymium in cheaper grades at a cost in
    strength. Plain NdFeB loses its coercivity past about 80 C; dysprosium or terbium in the melt (or diffused into the grain
    boundaries, terbium's main use now) holds it to 150-230 C, which is what every motor grade carries: the EH and AH grades
    are 8 to 10 per cent heavy rare earth, and three neodymium, a dysprosium, six iron and two ferroboron is 8. The high-temperature
    SmCo is Sm2(Co,Fe,Cu,Zr)17, per Arnold Magnetics 23 to 28 per cent samarium, 14 to 20 iron, 4 to 6 copper and 1.5 to 3
    zirconium in cobalt: two samarium, four cobalt, an iron and four copper and two zirconium nuggets is 26, 13, 6 and 3 in 52
    cobalt, seven ingots of the 7.7 melted. Rare earth metal burns in air when molten, so all are melted under argon. The boron goes in as ferroboron, which
    boric acid, iron and charcoal give in the heat of an arc: not borax, whose sodium would ride into the melt, but the boric acid sulfuric acid
    frees from it, Na2B4O7 + H2SO4 + 5 H2O -> 4 H3BO3 + Na2SO4, its oxide being what the carbon reduces. Alnico, the magnet before the rare earths, is iron with 8-12 per cent
    aluminium, 15-26 nickel, 5-24 cobalt and a few of copper, cast and heat-treated in a field: five iron, an aluminium, two
    nickel, two cobalt and three copper nuggets is 49 per cent iron, 10 aluminium, 19 nickel, 19 cobalt and 3 copper.
    Each alloy polarizes into its own magnet, and the Factory's motors, generators, stators, electric pumps and voltmeters are built from one grade
    and carry it (magnet.Magnets): their own recipes are taken over per grade, magnet step first so the deployer can tell
    which grade a part is being built to. The Factory's magnet is no longer made; what a world already holds still works,
    as the dysprosium grade it was."""
    mixing("boric_acid", item("raw_borax") + [fluid("tfmg:sulfuric_acid", 250)], [result("boric_acid", 2)], "heated")
    mixing("ferroboron", item("boric_acid") + tag("c:ingots/iron") + item("minecraft:charcoal", 2), [result("ferroboron")], "superheated")
    write(DATA / "tags/item/magnet_light_rare_earths.json", {"replace": False, "values": [f"fundamentals:{e}_ingot" for e in ("neodymium", "praseodymium", "didymium")]})
    write(DATA / "tags/item/magnet_heavy_rare_earths.json", {"replace": False, "values": [f"fundamentals:{e}_ingot" for e in ("dysprosium", "terbium")]})
    light, heavy = tag("fundamentals:magnet_light_rare_earths"), tag("fundamentals:magnet_heavy_rare_earths")
    boride = tag("c:ingots/iron", 3) + item("ferroboron") + argon()
    mixing("neodymium_iron_boron", light * 2 + boride, [result("neodymium_iron_boron_ingot", 4)], "superheated")
    mixing("neodymium_iron_boron_with_gadolinium", light + item("gadolinium_ingot") + boride, [result("neodymium_iron_boron_ingot", 3)], "superheated")
    double = tag("c:ingots/iron", 6) + item("ferroboron", 2) + argon(200)
    mixing("dysprosium_neodymium_iron_boron", light * 3 + heavy + double, [result("dysprosium_neodymium_iron_boron_ingot", 8)], "superheated")
    mixing("dysprosium_neodymium_iron_boron_with_gadolinium", light * 2 + item("gadolinium_ingot") + heavy + double,
           [result("dysprosium_neodymium_iron_boron_ingot", 6)], "superheated")
    mixing("samarium_cobalt", item("samarium_ingot", 2) + item("cobalt_ingot", 4) + tag("c:ingots/iron") + tag("c:nuggets/copper", 4) + tag("c:nuggets/zirconium", 2)
           + argon(200), [result("samarium_cobalt_ingot", 7)], "superheated")
    mixing("alnico", tag("c:ingots/iron", 5) + tag("c:ingots/aluminum") + tag("c:ingots/nickel", 2) + tag("c:ingots/cobalt", 2) + tag("c:nuggets/copper", 3),
           [result("alnico_ingot", 10)], "superheated")
    for alloy, forms in MAGNET_ALLOYS.items():
        for form in forms:
            write(USES / f"{alloy}_magnet_from_{form}.json", {"type": "tfmg:polarizing", "ingredients": tag(f"c:{form}s/{alloy}"),
                                                              "results": [{"id": f"fundamentals:{alloy}_magnet"}]})
    disabled(TFMG / "polarizing/magnet.json")
    tag_file(DATA / "tags/item/magnets.json", [f"fundamentals:{alloy}_magnet" for alloy in MAGNET_ALLOYS] + ["tfmg:magnet"])
    tag_file(DATA / "tags/item/magnet_machines.json", [f"tfmg:{machine}" for machine in MAGNET_MACHINES])
    write(USES / "magnet_rebuild.json", {"type": "fundamentals:magnet_rebuild", "category": "misc"})
    with zipfile.ZipFile(TFMG_JAR) as jar:
        def original(path):
            return json.loads(jar.read(f"data/tfmg/recipe/{path}.json"))

        for machine, path in MAGNET_MACHINES.items():
            recipe = original(path)
            disabled(TFMG / f"{path}.json")
            for alloy in MAGNET_ALLOYS:
                magnet = {"item": f"fundamentals:{alloy}_magnet"}
                if alloy == "dysprosium_neodymium_iron_boron":
                    magnet = [magnet, {"item": "tfmg:magnet"}]
                charge = {"fundamentals:magnet": {"grade": alloy}}
                graded = json.loads(json.dumps(recipe))
                if graded["type"] == "create:sequenced_assembly":
                    steps = graded["sequence"]
                    step = next(s for s in steps if {"item": "tfmg:magnet"} in s["ingredients"])
                    step["ingredients"] = [magnet if i == {"item": "tfmg:magnet"} else i for i in step["ingredients"]]
                    graded["sequence"] = [step] + [s for s in steps if s is not step]
                    graded["results"][0]["components"] = charge
                else:
                    graded["key"] = {k: magnet if v == {"item": "tfmg:magnet"} else v for k, v in graded["key"].items()}
                    graded["result"]["components"] = charge
                write(USES / f"{machine}_with_{alloy}_magnet.json", graded)
            table = json.loads(jar.read(f"data/tfmg/loot_table/blocks/{machine}.json"))
            for pool in table["pools"]:
                for entry in pool["entries"]:
                    entry["functions"] = [{"function": "minecraft:copy_components", "source": "block_entity", "include": ["fundamentals:magnet"]}]
            write(TFMG_LOOT / f"{machine}.json", table)


def cerium():
    """Ferrocerium, the lighter flint: cerium alloyed with iron sparks when scraped and never wears out."""
    write(USES / "ferrocerium_striker.json", {
        "type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": item("cerium_ingot") + tag("c:ingots/iron"),
        "result": {"id": "minecraft:flint_and_steel", "components": {
            "minecraft:unbreakable": {}, "minecraft:custom_name": json.dumps({"text": "Ferrocerium Striker", "italic": False})}}})


def lanthanum():
    """Fluid catalytic cracking: heavy oil over a hot zeolite catalyst, its framework held together by lanthanum, breaks to gasoline
    and propylene and lays coke on the catalyst, which the regenerator burns off; a little catalyst is lost as fines each pass.
    The Factory's steam cracking of naphtha to ethylene and propylene needs no catalyst and is left as it is."""
    write(USES / "fluid_catalytic_cracking.json", {
        "type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"],
        "heat_requirement": "heated", "machines": ["tfmg:mixing"], "min_size": 1, "processing_time": 100,
        "ingredients": [fluid("tfmg:heavy_oil", 500)] + item("lanthanum_oxide"),
        "results": [out_fluid("tfmg:gasoline", 250), out_fluid("tfmg:propylene", 150), {"id": "fundamentals:lanthanum_oxide", "chance": 0.95},
                    {"id": "tfmg:coal_coke_dust", "chance": 0.25}]})


def phosphors():
    """The tri-band phosphor of every fluorescent tube: europium's red on a yttria host, and terbium's green in lanthanum phosphate
    with cerium to take up the ultraviolet, LAP, its phosphate from phosphoric acid. The Factory's lamps take it. The ultraviolet is
    mercury's: a fluorescent tube is a few milligrams of mercury vapour in argon, the arc through it shining at 254 nm on the phosphor
    (the EU's RoHS directive allows 3 to 5 mg a tube), so the lamps take mercury too, a flask doing for two. The rest of the
    acid the crackers give off goes the way most phosphoric acid goes, neutralised with ammonia to ammonium phosphate fertiliser."""
    mixing("phosphor", item("yttrium_oxide", 2) + item("europium_oxide") + item("lanthanum_oxide") + item("cerium_oxide") + item("terbium_oxide")
           + [fluid("phosphoric_acid", 250)], [result("phosphor", 6)], "superheated")
    mixing("ammonium_phosphate", [fluid("phosphoric_acid", 250), fluid("ammonia", 250)], [result("minecraft:bone_meal")])
    shaped(TFMG / "crafting/materials/aluminum_lamp.json", ["PMP", "BFB", "SFS"],
           {"B": {"item": "tfmg:light_bulb"}, "P": {"item": "create:framed_glass_pane"}, "S": {"tag": "c:plates/aluminum"}, "F": {"item": "fundamentals:phosphor"},
            "M": {"item": "fundamentals:mercury"}},
           {"count": 2, "id": "tfmg:aluminum_lamp"})
    shaped(TFMG / "crafting/materials/circular_light.json", ["PMP", "BFB", "SFS"],
           {"B": {"item": "tfmg:light_bulb"}, "P": {"item": "create:framed_glass"}, "S": {"tag": "c:nuggets/steel"}, "F": {"item": "fundamentals:phosphor"},
            "M": {"item": "fundamentals:mercury"}},
           {"count": 2, "id": "tfmg:circular_light"})


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
    rack go twice as far. The master alloy is two per cent scandium, and mostly made without scandium metal at all: the fluoride
    stirred into molten aluminium, which takes the fluorine and gives the scandium to the melt, the aluminium fluoride skimmed off as dross."""
    mixing("aluminium_scandium", item("scandium_nugget") + tag("c:ingots/aluminum", 8), [result("aluminium_scandium_ingot", 8)], "superheated")
    mixing("aluminium_scandium_from_fluoride", item("scandium_fluoride") + tag("c:storage_blocks/aluminum", 4),
           [result("aluminium_scandium_block", 4), result("tfmg:slag")], "superheated")
    shaped(USES / "panel_rack_from_scandium.json", ["S S", "SSS"], {"S": {"tag": "c:plates/aluminium_scandium"}},
           {"count": 2, "id": "fundamentals:panel_rack"})


def roast(ore, roasted, name=None, raw=True):
    """A sulfide ore roasted on a campfire or in a smoker, as galena is, to drive off its sulfur (and arsenic)."""
    for kind, time in (("campfire_cooking", 400), ("smoking", 200)):
        write(ROASTING / f"{name or roasted}_{kind}.json", {"type": f"minecraft:{kind}", "category": "misc", "ingredient": {"item": f"fundamentals:{'raw_' if raw else ''}{ore}"},
                                                   "result": {"id": f"fundamentals:{roasted}"}, "experience": 0.1, "cookingtime": time})


def vat(name, items, fluid_id, amount, results, machines=("tfmg:mixing",), heat="heated"):
    write(USES / f"{name}.json", {"type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"],
                                  "heat_requirement": heat, "machines": list(machines), "min_size": 1, "processing_time": 100,
                                  "ingredients": items + [fluid_ingredient(fluid_id, amount)], "results": results})


def cobalt():
    """Cobaltite roasted of its arsenic and sulfur to the oxide, which hydrogen reduces in a heated vat: cobalt melts at
    1,495 C, past any furnace of the day. Cobalt blue is the oxide calcined with alumina."""
    roast("cobaltite", "roasted_cobaltite")
    vat("cobalt_ingot", item("roasted_cobaltite", 2), "tfmg:hydrogen", 500, [result("cobalt_ingot", 2)])
    mixing("cobalt_blue", item("roasted_cobaltite") + item("tfmg:bauxite_powder", 2), [result("minecraft:blue_dye", 4)], "superheated")


def copper_molybdenum_rhenium():
    """The porphyry chain. Chalcopyrite is roasted of part of its sulfur and smelted to matte. Molybdenite
    roasts to molybdenum trioxide, and the rhenium in it leaves up the flue as the heptoxide: the roaster's scrubbers are where
    all mined rhenium comes from, though about a quarter of supply is now recycled superalloy and catalyst (USGS, Mineral
    Commodity Summaries 2025). The scrub liquor's perrhenate is taken up on an ion exchanger, stripped with ammonia and
    crystallised as ammonium perrhenate, which hydrogen reduces to the metal powder. Real molybdenite concentrate carries
    100 to 3,000 ppm rhenium; here a flue dust one roast in ten, two nuggets to the dust, is a nugget per ten molybdenite,
    a few times the richest real concentrate, so that a superalloy melt costs a vein and not a mountain. The trioxide is reduced
    under hydrogen, as the industry does, in a heated vat."""
    roast("chalcopyrite", "roasted_chalcopyrite")
    mixing("molybdenum_oxide", item("raw_molybdenite", 2), [result("molybdenum_oxide", 2), {"id": "fundamentals:rhenium_flue_dust", "chance": 0.1}], "heated")
    vat("molybdenum_ingot", item("molybdenum_oxide", 2), "tfmg:hydrogen", 500, [result("molybdenum_ingot", 2)])
    mixing("ammonium_perrhenate", item("rhenium_flue_dust") + [fluid("minecraft:water", 250), fluid("ammonia", 50)], [result("ammonium_perrhenate")])
    vat("rhenium_nugget", item("ammonium_perrhenate"), "tfmg:hydrogen", 250, [result("rhenium_nugget", 2)])


def copper_sulfides():
    """A copper sulfide is not smelted to copper but to matte. Roasted chalcopyrite, and the calcine bornite, chalcocite and
    covellite roast to, melt in the bloomery to matte, the copper and iron sulfides, the rest going to slag. Air blown
    through the molten matte burns the iron to an oxide that sand fluxes off and the sulfur to SO2, leaving blister copper,
    pocked where the gas broke out; the blast furnace fire-refines it to copper."""
    for ore in ("bornite", "chalcocite", "covellite"):
        roast(ore, "copper_calcine", f"copper_calcine_from_{ore}")
    for feed in ("roasted_chalcopyrite", "copper_calcine"):
        write(DATA / f"recipe/bloomery/copper_matte_from_{feed}.json", {"type": "fundamentals:bloomery", "ingredient": {"item": f"fundamentals:{feed}"},
                                                                       "result": {"id": "fundamentals:copper_matte_dust", "count": 1}, "byproduct": {"id": "tfmg:slag", "count": 1}})
    mixing("blister_copper", item("copper_matte_dust", 2) + tag("c:sands/colorless"), [result("blister_copper_ingot", 2), result("tfmg:slag")], "superheated")
    write(USES / "copper_ingot_from_blister_copper.json", {"type": "minecraft:blasting", "category": "misc", "ingredient": {"item": "fundamentals:blister_copper_ingot"},
                                                           "result": {"id": "minecraft:copper_ingot"}, "experience": 0.3, "cookingtime": 100})


def lithium():
    """Spodumene is calcined at about 1,050 C from alpha to the open beta form, roasted with sulfuric acid at about 250 C and
    leached in water to lithium sulfate; soda ash throws down lithium carbonate from the hot liquor, the sodium sulfate staying in
    solution (Ullmann's, Lithium and Lithium Compounds; the Talison-Tianqi flowsheet). Hydrochloric acid turns the carbonate to
    the chloride, giving off carbon dioxide; boiled dry, that is the salt lithium is won from. Lithium cannot be won from water,
    so the dry chloride is electrolysed molten, with potassium chloride in the real bath, at about 450 C, and gives off its
    chlorine at the anode."""
    write(LITHIUM / "calcined_spodumene.json", {"type": "minecraft:blasting", "category": "misc", "ingredient": {"item": "fundamentals:raw_spodumene"},
                                                "result": {"id": "fundamentals:calcined_spodumene"}, "experience": 0.2, "cookingtime": 100})
    write(LITHIUM / "lithium_sulfate_liquor.json", {"type": "create:mixing", "heat_requirement": "heated", "ingredients": item("calcined_spodumene", 2)
                                                    + [fluid("tfmg:sulfuric_acid", 250), fluid("minecraft:water", 500)],
                                                    "results": [out_fluid("lithium_sulfate_liquor", 500)]})
    write(LITHIUM / "lithium_carbonate.json", {"type": "create:mixing", "heat_requirement": "heated", "ingredients": item("soda_ash", 2)
                                               + [fluid("lithium_sulfate_liquor", 500)], "results": [result("lithium_carbonate", 2)]})
    write(LITHIUM / "lithium_chloride.json", {"type": "create:mixing", "heat_requirement": "heated", "ingredients": item("lithium_carbonate", 2)
                                              + [fluid("hydrochloric_acid", 500)],
                                              "results": [result("lithium_chloride", 2), out_fluid("tfmg:carbon_dioxide", 250)]})
    write(LITHIUM / "lithium_ingot.json", {"type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"],
                                           "heat_requirement": "heated", "machines": ["tfmg:electrode", "tfmg:electrode"], "min_size": 1, "processing_time": 100,
                                           "ingredients": item("lithium_chloride", 2),
                                           "results": [{"id": "tfmg:lithium_ingot"}, {"chance": 0.5, "count": 3, "id": "tfmg:lithium_nugget"},
                                                       {"amount": 250, "id": "fundamentals:chlorine"}]})


def disabled(path):
    write(path, {"neoforge:conditions": [{"type": "neoforge:false"}]})


def crushing(name, ingredient, results, time=250):
    write(CREATE / f"crushing/{name}.json", {"type": "create:crushing", "ingredients": [ingredient], "processing_time": time, "results": results})


def iron():
    """Hematite, magnetite and goethite crush to Create's crushed iron ore, and crushed iron ore goes only to The Factory's
    blast furnace: iron oxide wants coke and some 1,500 °C, which neither a furnace nor a water fan has. Before the blast
    furnace, iron is the bloomery's."""
    for ore in ("hematite", "magnetite", "goethite"):
        write(USES / f"crushed_iron_from_{ore}.json", {"type": "create:crushing", "ingredients": item(f"raw_{ore}"), "processing_time": 400,
                                                       "results": [{"id": "create:crushed_raw_iron"}, {"id": "create:experience_nugget", "chance": 0.75}]})
    for path in ("smelting/iron_ingot_from_crushed", "blasting/iron_ingot_from_crushed", "splashing/crushed_raw_iron"):
        disabled(CREATE / f"{path}.json")


def zinc():
    """Sphalerite roasts to zinc oxide; smithsonite and hemimorphite, the old calamine, calcine to it. Zinc boils at 907 °C, below
    the heat that reduces it, so it was distilled out of a sealed retort packed with charcoal: a superheated basin stands in."""
    for ore in ("sphalerite", "smithsonite", "hemimorphite"):
        roast(ore, "zinc_oxide", f"zinc_oxide_from_{ore}")
    mixing("zinc_ingot", item("zinc_oxide") + item("minecraft:charcoal"), [result("create:zinc_ingot")], "superheated")


def nickel():
    """Carbon reduces iron with nickel, so a nickel ore smelted with carbon gives not nickel but ferronickel: roasted pentlandite
    or laterite smelted whole, as the rotary kiln-electric furnaces of New Caledonia and Indonesia smelt it, to an alloy of 20 to 40
    per cent nickel (Crundwell et al., Extractive Metallurgy of Nickel, Cobalt and Platinum Group Metals, 2011), taken here as a third.
    It goes into stainless steel, where two thirds of the world's nickel goes; class 1 nickel comes off the matte, by the sulfate leach
    and electrowinning or by the Mond process. Matte roasted dead gives off its sulfur and leaves nickel oxide; water gas reduces it
    at 400 C and its carbon monoxide carries the nickel off at 50 C as the carbonyl, Ni + 4 CO -> Ni(CO)4, which decomposes on hot
    pellets at about 230 C to nickel 99.97 per cent pure and gives the monoxide back (Mond, Langer and Quincke, 1890; Clydach from
    1902). What does not volatilise is the residue the Acton refinery took the platinum metals from. The leach liquor carries the
    cobalt: an organophosphorus extractant (Cyanex 272 in the West, P507 in China) takes cobalt over nickel at about pH 5 and
    hydrochloric acid strips it to cobalt chloride, which electrowins to cobalt and chlorine, as at Nikkelverk."""
    roast("pentlandite", "roasted_pentlandite")
    roast("converter_matte_dust", "nickel_oxide", raw=False)
    mixing("ferronickel", item("roasted_pentlandite") + item("minecraft:charcoal"), [result("ferronickel_ingot"), result("tfmg:slag")], "superheated")
    mixing("ferronickel_from_laterite", item("raw_nickel_laterite", 4) + item("minecraft:charcoal", 2),
           [result("ferronickel_ingot"), result("tfmg:slag", 2)], "superheated")
    pgm_vat("nickel_carbonyl", item("nickel_oxide", 2) + [fluid("water_gas", 1000)],
            [out_fluid("nickel_carbonyl", 500), {"id": "fundamentals:platinum_group_concentrate", "chance": 0.1}], folder=USES)
    pgm_vat("carbonyl_decomposition", [fluid("nickel_carbonyl", 500)], [result("nickel_pellets", 2), out_fluid("water_gas", 500)], folder=USES)
    write(USES / "nickel_ingot_from_pellets.json", {"type": "create:compacting", "heat_requirement": "heated", "ingredients": item("nickel_pellets"),
                                                    "results": [result("tfmg:nickel_ingot")]})
    pgm_vat("cobalt_extraction", [fluid("nickel_copper_sulfate", 1000), fluid("p507", 250), fluid("hydrochloric_acid", 50)],
            [out_fluid("nickel_sulfate_liquor", 950), out_fluid("cobalt_chloride_liquor", 50), out_fluid("p507", 245)], heat=None, folder=USES)
    pgm_vat("nickel_electrowinning_from_raffinate", [fluid("nickel_sulfate_liquor", 500)],
            [result("tfmg:nickel_ingot"), {"id": "minecraft:copper_ingot", "chance": 0.5}, out_fluid("tfmg:sulfuric_acid", 250)],
            machines=("tfmg:electrode", "tfmg:electrode"), heat=None, folder=USES)
    pgm_vat("cobalt_electrowinning", [fluid("cobalt_chloride_liquor", 500)], [result("cobalt_ingot"), out_fluid("chlorine", 250)],
            machines=("tfmg:electrode", "tfmg:electrode"), heat=None, folder=USES)
    tag_file(DATA / "tags/item/nickel_dusts.json", [f"fundamentals:{name}" for name in ("nickel_matte_dust", "converter_matte_dust", "roasted_pentlandite", "nickel_oxide")])
    tag_file(DATA / "tags/item/sulfides.json", [f"fundamentals:{name}" for name in ("raw_pentlandite", "raw_cobaltite", "converter_matte_dust")])


def tin():
    """Cassiterite is heavy (7 to the water's 1) and inert, so a wash leaves it behind as concentrate, hard-rock or placer alike.
    Roasting drives off the sulfur and arsenic of the pyrite and arsenopyrite that ride with it. Charcoal reduces the oxide at 1,200
    to 1,300 C, as the blowing house's shaft furnace did and the bloomery does; a superheated basin with coke stands in for the
    reverberatory. Crude tin carries iron; tin melts at 232 C, so on a gentle heat it runs off the iron-tin hardhead, and green wood
    stirred through the melt (poling) brings the last dross up. Arsenical copper came before it; tin bronze is copper with about an eighth
    of tin, seven to one here (bell metal is a harder fifth to a quarter tin), and is bells and the plain bearing a shaft turns in.
    Tin-lead solder joins circuit boards."""
    write(USES / "tin_concentrate.json", {"type": "create:splashing", "ingredients": item("raw_cassiterite"), "results": [result("tin_concentrate")]})
    roast("tin_concentrate", "roasted_tin_concentrate", raw=False)
    write(DATA / "recipe/bloomery/crude_tin_from_roasted_tin_concentrate.json", {"type": "fundamentals:bloomery", "ingredient": {"item": "fundamentals:roasted_tin_concentrate"},
                                                                                "result": {"id": "fundamentals:crude_tin_ingot", "count": 1}, "byproduct": {"id": "tfmg:slag", "count": 1}})
    mixing("crude_tin", item("roasted_tin_concentrate", 2) + item("tfmg:coal_coke"), [result("crude_tin_ingot", 2), result("tfmg:slag")], "superheated")
    mixing("tin_ingot", item("crude_tin_ingot", 2) + tag("c:rods/wooden"), [result("tin_ingot", 2), {"id": "tfmg:slag", "chance": 0.25}], "heated")
    mixing("bronze_ingot", tag("c:ingots/copper", 7) + tag("c:ingots/tin"), [result("bronze_ingot", 8)], "heated")
    shaped(CREATE / "crafting/curiosities/peculiar_bell.json", ["I", "P"], {"I": {"tag": "c:storage_blocks/bronze"}, "P": {"tag": "c:plates/bronze"}},
           {"count": 1, "id": "create:peculiar_bell"})
    shaped(CREATE / "crafting/kinetics/mechanical_bearing.json", [" B ", "PCP", " I "],
           {"B": {"tag": "minecraft:wooden_slabs"}, "C": {"item": "create:andesite_casing"}, "I": {"item": "create:shaft"}, "P": {"tag": "c:plates/bronze"}},
           {"count": 1, "id": "create:mechanical_bearing"})
    shaped(USES / "bell.json", [" S ", "BBB", "B B"], {"S": {"tag": "c:rods/wooden"}, "B": {"tag": "c:ingots/bronze"}}, {"count": 1, "id": "minecraft:bell"})
    mixing("solder", tag("c:ingots/tin") + tag("c:ingots/lead"), [result("solder", 8)], "heated")
    board = {"item": "tfmg:unfinished_circuit_board"}
    write(TFMG / "sequenced_assembly/unfinished_circuit_board.json", {
        "type": "create:sequenced_assembly", "ingredient": {"item": "tfmg:etched_circuit_board"}, "loops": 4, "results": [{"id": "tfmg:circuit_board"}],
        "sequence": [{"type": "create:deploying", "ingredients": [board, {"item": part}], "results": [{"id": "tfmg:unfinished_circuit_board"}]}
                     for part in ("tfmg:capacitor_item", "tfmg:resistor", "tfmg:transistor_item", "tfmg:resistor", "fundamentals:solder")],
        "transitional_item": {"id": "tfmg:unfinished_circuit_board"}})


def rocks():
    """Create's stones give their metal up when crushed. Here crimsite, the iron stone, gives hematite and asurine, the zinc stone,
    smithsonite, which then go the long way; tuff gives only flint, and The Factory's galena rock gives galena. Washed gravel
    leaves a little magnetite black sand rather than iron nuggets."""
    for rock, ore, chance in (("crimsite", "hematite", 0.4), ("asurine", "smithsonite", 0.3)):
        crushing(rock, {"item": f"create:{rock}"}, [{"id": f"fundamentals:raw_{ore}", "chance": chance}])
        crushing(f"{rock}_recycling", {"tag": f"create:stone_types/{rock}"}, [{"id": f"fundamentals:raw_{ore}", "chance": chance}])
    for name, ingredient in (("tuff", {"item": "minecraft:tuff"}), ("tuff_recycling", {"tag": "create:stone_types/tuff"})):
        crushing(name, ingredient, [{"id": "minecraft:flint", "chance": 0.25}], 350)
    write(CREATE / "crushing/galena.json", {"type": "create:crushing", "ingredients": [{"item": "tfmg:galena"}],
                                            "results": [{"id": "fundamentals:raw_galena", "chance": 0.4}]})
    write(CREATE / "splashing/gravel.json", {"type": "create:splashing", "ingredients": [{"item": "minecraft:gravel"}],
                                             "results": [{"id": "minecraft:flint", "chance": 0.25}, {"id": "fundamentals:raw_magnetite", "chance": 0.02}]})


def loot():
    """Iron is won from ore, so chests (every mod's) keep one iron ingot, nugget, block, tool or armour piece in four; iron golems
    drop nuggets, scrap rather than bar; and a drowned's copper ingot has gone green to malachite. Structure chests carry a smidge
    of the pack's metals, rarer the harder the place: tin and lead in a village, silver and tungsten in a stronghold, rare earth
    oxides and the platinum metals only in end and ancient cities. YUNG's rebuilt structures count as the vanilla ones they replace."""
    modifiers = DATA / "loot_modifiers"
    shutil.rmtree(modifiers, ignore_errors=True)
    common = [{"id": f"#c:{name}", "required": False} for name in ("ingots/iron", "nuggets/iron", "storage_blocks/iron", "raw_materials/iron", "storage_blocks/raw_iron")]
    write(DATA / "tags/item/scarce_in_chests.json", {"replace": False, "values": common + [
        "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots", "minecraft:iron_horse_armor",
        "minecraft:iron_sword", "minecraft:iron_axe", "minecraft:iron_pickaxe", "minecraft:iron_shovel", "minecraft:iron_hoe", "minecraft:shears"]})
    write(modifiers / "scarce_iron_in_chests.json", {"type": "fundamentals:scarce_in_chests", "conditions": [],
                                                    "items": "#fundamentals:scarce_in_chests", "keep": 0.25})
    chests = lambda *names: [f"minecraft:chests/{name}" for name in names]
    pool = lambda *entries: [{"item": id if ":" in id else f"fundamentals:{id}", "weight": weight, "min": low, "max": high} for id, weight, low, high in entries]
    tiers = {
        "common_metals_in_chests": (chests("village/", "abandoned_mineshaft", "simple_dungeon", "shipwreck_supply", "shipwreck_treasure", "ruined_portal",
                                           "underwater_ruin_small", "underwater_ruin_big") + [f"betterdungeons:{d}/chests/" for d in ("skeleton_dungeon", "small_dungeon", "small_nether_dungeon", "spider_dungeon", "zombie_dungeon")], 0.12,
                                    pool(("tin_nugget", 4, 1, 4), ("tfmg:lead_nugget", 3, 1, 4), ("create:zinc_nugget", 3, 1, 4), ("tfmg:nickel_nugget", 2, 1, 3),
                                         ("bronze_nugget", 2, 1, 4), ("tin_ingot", 1, 1, 1), ("bronze_ingot", 1, 1, 1))),
        "hard_metals_in_chests": (chests("stronghold_corridor", "stronghold_crossing", "stronghold_library", "desert_pyramid", "jungle_temple",
                                         "bastion_bridge", "bastion_hoglin_stable", "bastion_other", "bastion_treasure", "nether_bridge", "trial_chambers/",
                                         "woodland_mansion", "pillager_outpost", "buried_treasure")
                                  + ["betterstrongholds:chests/", "betterdeserttemples:chests/", "betterjungletemples:chests/", "betterfortresses:chests/"], 0.10,
                                  pool(("silver_nugget", 4, 1, 4), ("cobalt_nugget", 3, 1, 3), ("silver_ingot", 1, 1, 2), ("cobalt_ingot", 1, 1, 1),
                                       ("tungsten_ingot", 1, 1, 1), ("molybdenum_ingot", 1, 1, 1), ("platinum_nugget", 1, 1, 1))),
        "rare_metals_in_chests": (chests("end_city_treasure", "ancient_city"), 0.20,
                                  pool(*[(f"{ree}_oxide", 2, 1, 2) for ree in ("lanthanum", "cerium", "praseodymium", "neodymium", "samarium", "europium", "gadolinium",
                                                                                "terbium", "dysprosium", "holmium", "erbium", "thulium", "ytterbium", "lutetium", "yttrium", "scandium")],
                                       *[(f"{ree}_ingot", 1, 1, 1) for ree in ("lanthanum", "cerium", "praseodymium", "neodymium", "samarium", "gadolinium",
                                                                                "terbium", "dysprosium", "yttrium", "scandium", "didymium")],
                                       *[(f"{pgm}_nugget", 2, 1, 3) for pgm in PGMS], ("rhenium_ingot", 1, 1, 1))),
    }
    for name, (tables, chance, entries) in tiers.items():
        write(modifiers / f"{name}.json", {"type": "fundamentals:add_to_chests", "conditions": [], "tables": tables, "chance": chance, "pool": entries})
    swaps = {"iron_golem_scrap": ("minecraft:entities/iron_golem", "minecraft:iron_ingot", "minecraft:iron_nugget"),
             "drowned_malachite": ("minecraft:entities/drowned", "minecraft:copper_ingot", "fundamentals:raw_malachite")}
    for name, (table, old, new) in swaps.items():
        write(modifiers / f"{name}.json", {"type": "fundamentals:swap_drop", "from": old, "to": new,
                                           "conditions": [{"condition": "neoforge:loot_table_id", "loot_table_id": table}]})
    write(DATA.parent / "neoforge/loot_modifiers/global_loot_modifiers.json", {
        "replace": False, "entries": [f"fundamentals:{path.stem}" for path in sorted(modifiers.glob("*.json"))]})


def superalloy(extra=(), count=7):
    """A second-generation single-crystal nickel superalloy after CMSX-4, Ni-6.5Cr-9Co-6W-6.5Ta-5.6Al-3Re-1Ti (Reed, The Superalloys,
    2006): six nickel, a cobalt, a chromium and a tungsten ingot, five aluminium and two rhenium nuggets are 61 per cent nickel, 10 each
    of cobalt, chromium and tungsten, 5.7 aluminium and 2.3 rhenium, the tungsten standing in for the tantalum too, which is not in the pack."""
    return (item("tfmg:nickel_ingot", 6) + item("cobalt_ingot") + item("chromium_ingot") + item("tungsten_ingot") + tag("c:nuggets/aluminum", 5)
            + item("rhenium_nugget", 2) + list(extra) + argon()), [result("superalloy_ingot", count)]


def alloys():
    """Where cobalt, chromium, molybdenum, tungsten and rhenium go: the nickel superalloy of turbine blades, melted under argon as the
    rare earth magnets are. Its aluminium is what makes it a superalloy: it precipitates as the ordered Ni3Al gamma-prime that holds
    the strength to near the melting point, and with the chromium it grows the alumina scale that keeps it from burning in the hot gas;
    tungsten and rhenium stiffen the matrix against creep. The blade is sprayed with an MCrAlY bond coat, NiCoCrAlY gas-atomised under
    argon (Amdry 365 is Ni-23Co-17Cr-12.5Al-0.5Y: four nickel, two cobalt, a chromium, an aluminium and a yttrium nugget is
    49-25-12-12-1.4), whose yttrium pins the alumina scale to it, and over that the yttria-stabilised zirconia, the ceramic thermal
    barrier that lets it run in gas hotter than it melts. Molybdenum goes into steel as the roasted trioxide, not as metal: a trioxide
    in six blocks of steel is about one per cent, the molybdenum of the creep-resistant chromium-molybdenum boiler and pressure-vessel
    steels (ASTM A387 grade 22 is 0.9 to 1.1), their chromium left out."""
    mixing("superalloy", *superalloy(), "superheated")
    mixing("molybdenum_steel", item("molybdenum_oxide") + tag("c:storage_blocks/steel", 6), [result("molybdenum_steel_ingot", 54)], "superheated")
    mixing("mcraly_powder", item("tfmg:nickel_ingot", 4) + item("cobalt_ingot", 2) + item("chromium_ingot") + tag("c:ingots/aluminum") + item("yttrium_nugget")
           + argon(), [result("mcraly_powder", 8)], "superheated")
    shaped(TFMG / "turbine_blade.json", ["IYI", "ISI", "IBI"],
           {"S": {"item": "create:shaft"}, "I": {"tag": "c:plates/superalloy"}, "Y": {"item": "fundamentals:yttria_stabilised_zirconia"},
            "B": {"item": "fundamentals:mcraly_powder"}},
           {"count": 1, "id": "tfmg:turbine_blade", "components": {"tfmg:fuel_tags": {"kerosene": "c:kerosene"}, "tfmg:fuels": {"kerosene": "Kerosene"}}})
    write(TFMG / "item_application/heavy_machinery_casing.json", {"type": "create:item_application",
          "ingredients": [{"item": "tfmg:steel_casing"}, {"tag": "c:plates/molybdenum_steel"}], "results": [{"id": "tfmg:heavy_machinery_casing"}]})


def thermometry():
    """Mercury: cinnabar roasted in air at about 600 C in a retort, HgS + O2 -> Hg + SO2, the vapour condensed and run off into a
    flask, as at Almaden since Roman times. And the type K thermocouple's legs: chromel, nickel with a tenth of chromium, and alumel,
    nickel with 2 per cent each of aluminium and manganese and 1 of silicon (ASTM E230), here four aluminium nuggets in nine nickel
    with the manganese and silicon folded into the aluminium; both melted past nickel's 1,455 C."""
    mixing("mercury", item("raw_cinnabar") + [fluid("tfmg:air", 250)], [result("mercury")], "heated")
    mixing("chromel", item("tfmg:nickel_ingot", 9) + item("chromium_ingot"), [result("chromel_ingot", 10)], "superheated")
    mixing("alumel", item("tfmg:nickel_ingot", 9) + tag("c:nuggets/aluminum", 4), [result("alumel_ingot", 9)], "superheated")


def semiconductors():
    """Silicon is made n-type by a donor from group 15, phosphorus or arsenic, and p-type by an acceptor from group 13, boron, each
    diffused into the hot wafer at 900 to 1,100 C (Sze, Physics of Semiconductor Devices): phosphorus from its chloride or the white
    element, boron from boric acid. The Factory dopes with sulfur, which is no donor in silicon, and aluminium, an acceptor no fab uses;
    both are taken over. A dopant is parts per million of the wafer, so one does for four."""
    mixing_at(TFMG / "mixing/n_semiconductor.json", item("tfmg:silicon_ingot", 4) + item("white_phosphorus"), [result("tfmg:n_semiconductor", 4)], "heated")
    mixing_at(TFMG / "mixing/p_semiconductor.json", item("tfmg:silicon_ingot", 4) + item("boric_acid"), [result("tfmg:p_semiconductor", 4)], "heated")


def manganese():
    """Nine tenths of the world's manganese goes into steel (USGS, Mineral Commodity Summaries, Manganese), as ferromanganese or
    silicomanganese smelted from the ore with coke in a submerged-arc or blast furnace at about 1,500 C: the manganese and the iron the
    ore carries (an iron nugget here, pyrolusite being near pure MnO2) reduce together to an alloy about four fifths manganese and a few
    per cent carbon. A little in every steel takes up its
    sulfur and oxygen; a lot is Hadfield's steel (1882), 12 to 14 per cent manganese and 1 of carbon, which stays austenitic and hardens
    where it is struck, the steel of crusher jaws, railway crossings and the Brodie helmet. One ferromanganese in six steel is 11 per cent
    manganese. As wear plate, a thinner plate of it lasts as a thick one of mild steel, so an ingot presses to two of the Factory's heavy plates."""
    mixing("ferromanganese", item("raw_pyrolusite", 2) + item("tfmg:coal_coke") + tag("c:nuggets/iron") + tag("tfmg:flux"),
           [result("ferromanganese_ingot"), result("tfmg:slag")], "superheated")
    mixing("manganese_steel", item("ferromanganese_ingot") + tag("c:ingots/steel", 6), [result("manganese_steel_ingot", 7)], "superheated")
    with zipfile.ZipFile(TFMG_JAR) as jar:
        plate = json.loads(jar.read("data/tfmg/recipe/sequenced_assembly/heavy_plate.json"))
    write(USES / "heavy_plate_from_manganese_steel.json", {**plate, "ingredient": {"tag": "c:ingots/manganese_steel"}, "results": [{"id": "tfmg:heavy_plate", "count": 2}]})


def galvanizing():
    """Half the world's zinc goes on steel (International Zinc Association): dipped in molten zinc at about 450 C, the steel takes a
    coat that rusts in its place and, scratched through, still corrodes before the steel does. A coat is a few per cent of a plate's
    weight, a zinc nugget to four. A galvanized plate does what a steel plate does and never rusts."""
    mixing("galvanized_steel_plate", item("tfmg:heavy_plate", 4) + tag("c:nuggets/zinc"), [result("galvanized_steel_plate", 4)], "heated")
    write(DATA.parent / "c/tags/item/plates/steel.json", {"replace": False, "values": ["fundamentals:galvanized_steel_plate"]})


def tungsten():
    """Wolframite does not open to acid; it is digested in caustic soda under pressure at 150 to 200 C, or fused with soda ash and
    leached in water, to sodium tungstate, the iron and manganese left as hydroxides. Scheelite opens to hot hydrochloric acid, which
    throws down yellow tungstic acid. Either way the tungsten is taken up as ammonium tungstate, the liquor by an amine extractant or
    ion exchanger stripped with ammonia (folded into one step here), the acid by dissolving it in ammonia, and evaporated to
    crystals of ammonium paratungstate, APT, the form tungsten is traded in. Calcined, APT gives off its ammonia and water and leaves
    the yellow trioxide, and hydrogen reduces that to the metal powder (Lassner and Schubert, Tungsten, 1999). The metal is drawn
    to the filament every light bulb burns, and carburised to the carbide every drill bites with. In a vacuum the hot filament
    boils away and blackens the glass; Langmuir (1913) filled the bulb with an inert gas that holds the tungsten back, and lamps
    have been argon-filled since, so an argon bulb lasts half again as long. A halogen lamp seals its quartz
    envelope round molybdenum foil, the one metal whose expansion lets the seal hold, and stands the filament on molybdenum
    supports (Ullmann's, Molybdenum); a whiff of bromine carries the tungsten that boils off the filament back onto it, so it runs
    hotter and lasts twice as long. Bromine's one use here; its big real ones, flame retardants, are not modelled."""
    pgm_vat("sodium_tungstate_liquor", item("raw_wolframite", 2) + [fluid("caustic_soda", 500)], [out_fluid("sodium_tungstate_liquor", 500)], folder=USES)
    mixing("sodium_tungstate_liquor_from_soda_ash", item("raw_wolframite", 2) + item("soda_ash", 2) + [fluid("minecraft:water", 500)],
           [out_fluid("sodium_tungstate_liquor", 500)], "superheated")
    mixing("tungstic_acid", item("raw_scheelite", 2) + [fluid("hydrochloric_acid", 500)], [result("tungstic_acid", 2)], "heated")
    mixing("ammonium_paratungstate", [fluid("sodium_tungstate_liquor", 500), fluid("ammonia", 250)], [result("ammonium_paratungstate", 2)], "heated")
    mixing("ammonium_paratungstate_from_tungstic_acid", item("tungstic_acid", 2) + [fluid("ammonia", 250)], [result("ammonium_paratungstate", 2)], "heated")
    write(USES / "tungsten_oxide.json", {"type": "minecraft:blasting", "category": "misc", "ingredient": {"item": "fundamentals:ammonium_paratungstate"},
                                         "result": {"id": "fundamentals:tungsten_oxide"}, "experience": 0.2, "cookingtime": 100})
    vat("tungsten_ingot", item("tungsten_oxide", 2), "tfmg:hydrogen", 500, [result("tungsten_ingot", 2)])
    mixing("tungsten_carbide", item("tungsten_ingot") + tag("minecraft:coals", 2), [result("tungsten_carbide", 2)], "superheated")
    write(USES / "tungsten_filament.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": item("tungsten_ingot"), "result": result("tungsten_filament", 4)})
    shaped(TFMG / "crafting/materials/light_bulb.json", ["CWC", "CGC", "NNN"],
           {"C": {"tag": "c:nuggets/copper"}, "G": {"item": "create:framed_glass"}, "N": {"tag": "c:nuggets/steel"}, "W": {"item": "fundamentals:tungsten_filament"}},
           {"count": 2, "id": "tfmg:light_bulb"})
    mixing("light_bulb_argon", item("tungsten_filament") + item("create:framed_glass") + tag("c:nuggets/copper", 4) + tag("c:nuggets/steel", 3) + argon(50),
           [result("tfmg:light_bulb", 3)])
    mixing("light_bulb_halogen", item("tungsten_filament") + tag("c:gems/quartz") + item("molybdenum_ingot") + tag("c:nuggets/steel", 3)
           + [fluid("bromine", 10)], [result("tfmg:light_bulb", 4)])
    shaped(CREATE / "crafting/kinetics/mechanical_drill.json", [" A ", "AIA", " C "],
           {"A": {"item": "create:andesite_alloy"}, "C": {"item": "create:andesite_casing"}, "I": {"item": "fundamentals:tungsten_carbide"}},
           {"count": 1, "id": "create:mechanical_drill"})


def more_sinks():
    """Cerium oxide is the oxygen store of every catalytic converter: the Factory's exhaust takes two. The three-way converter puts platinum and
    palladium on the ceria to burn what the engine left and rhodium to break down its nitrogen oxides (four fifths of all rhodium goes there),
    so one made with the three lasts as two. Lithium cobalt oxide is the cathode
    the first lithium batteries ran on: the lithium charge takes a cobalt. Neodymium and holmium colour glass, as erbium does."""
    shaped(TFMG / "crafting/materials/exhaust.json", ["BPB", "EPE", "CPC"],
           {"B": {"item": "minecraft:iron_bars"}, "C": {"tag": "c:ingots/cast_iron"}, "P": {"item": "tfmg:cast_iron_pipe"}, "E": {"item": "fundamentals:cerium_oxide"}},
           {"count": 1, "id": "tfmg:exhaust"})
    shaped(USES / "exhaust_three_way.json", ["TKR", "EPE", "CPC"],
           {"T": {"tag": "c:nuggets/platinum"}, "K": {"tag": "c:nuggets/palladium"}, "R": {"tag": "c:nuggets/rhodium"},
            "C": {"tag": "c:ingots/cast_iron"}, "P": {"item": "tfmg:cast_iron_pipe"}, "E": {"item": "fundamentals:cerium_oxide"}},
           {"count": 2, "id": "tfmg:exhaust"})
    shaped(TFMG / "crafting/materials/lithium_charge.json", [" P ", "LKL", " A "],
           {"A": {"tag": "c:plates/aluminum"}, "L": {"tag": "c:ingots/lithium"}, "P": {"item": "tfmg:plastic_sheet"}, "K": {"item": "fundamentals:cobalt_ingot"}},
           {"count": 1, "id": "tfmg:lithium_charge"})
    for oxide, glass in (("neodymium", "purple"), ("holmium", "yellow")):
        shaped(USES / f"{oxide}_glass.json", ["GGG", "GEG", "GGG"], {"G": {"tag": "c:glass_blocks/colorless"}, "E": {"item": f"fundamentals:{oxide}_oxide"}},
               {"count": 8, "id": f"minecraft:{glass}_stained_glass"})


def chromium():
    """Chromite ground and washed to a concentrate, then smelted with coke and a flux, as a submerged-arc furnace does at 1,600 to 1,700 °C:
    the iron in chromite reduces with the chromium, so what comes out is ferrochrome, and ferrochrome with steel and nickel is stainless.
    Chromium metal goes the long way: soda roasted in air at about 1,100 °C to sodium chromate, leached and acidified to the dichromate,
    reduced by carbon to the green oxide (giving the soda ash back), and the oxide reduced by aluminium powder, which once lit burns on by itself.
    Soda ash is calcined trona, from the dry lakes where borax lies."""
    write(USES / "soda_ash.json", {"type": "minecraft:smelting", "category": "misc", "ingredient": {"item": "fundamentals:raw_trona"},
                                   "result": {"id": "fundamentals:soda_ash"}, "experience": 0.1, "cookingtime": 200})
    mixing("ferrochrome", item("chromite_concentrate", 2) + item("tfmg:coal_coke") + tag("tfmg:flux"), [result("ferrochrome_ingot"), result("tfmg:slag")], "superheated")
    mixing("stainless_steel", item("ferrochrome_ingot", 3) + item("tfmg:nickel_ingot") + tag("c:ingots/steel", 6), [result("stainless_steel_ingot", 10)], "superheated")
    mixing("stainless_steel_from_ferronickel", item("ferrochrome_ingot", 3) + item("ferronickel_ingot", 3) + tag("c:ingots/steel", 4),
           [result("stainless_steel_ingot", 10)], "superheated")
    shaped(TFMG / "crafting/materials/steel_chemical_vat.json", ["PPP", "NTN", "PPP"],
           {"N": {"tag": "c:plates/stainless_steel"}, "P": {"item": "tfmg:heavy_plate"}, "T": {"item": "tfmg:steel_fluid_tank"}},
           {"count": 2, "id": "tfmg:steel_chemical_vat"})
    shaped(TFMG / "crafting/materials/flarestack.json", ["SPS", "BPB", "CPC"],
           {"B": {"item": "minecraft:iron_bars"}, "C": {"tag": "c:ingots/stainless_steel"}, "P": {"item": "tfmg:cast_iron_pipe"}, "S": {"item": "minecraft:flint_and_steel"}},
           {"count": 1, "id": "tfmg:flarestack"})
    mixing("sodium_chromate", item("chromite_concentrate") + item("soda_ash", 2), [result("sodium_chromate", 2)], "heated")
    mixing("sodium_dichromate", item("sodium_chromate", 2) + [{"type": "neoforge:single", "amount": 250, "fluid": "tfmg:sulfuric_acid"}], [result("sodium_dichromate")])
    mixing("chromium_oxide", item("sodium_dichromate") + tag("minecraft:coals"), [result("chromium_oxide"), result("soda_ash")], "heated")
    write(USES / "aluminium_powder.json", {"type": "create:milling", "ingredients": tag("c:ingots/aluminum"), "processing_time": 200,
                                           "results": [result("aluminium_powder", 2)]})
    mixing("chromium_ingot", item("chromium_oxide") + item("aluminium_powder"), [result("chromium_ingot"), result("tfmg:slag")], "superheated")
    write(USES / "chrome_oxide_green.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": item("chromium_oxide"),
                                             "result": result("minecraft:green_dye", 2)})


def rock_salt():
    """Halite, rock salt, is mined and ground, not boiled: a millstone or crushing wheels make salt of it."""
    write(USES / "salt_from_halite_milling.json", {"type": "create:milling", "ingredients": item("raw_halite"), "processing_time": 200,
                                                   "results": [result("salt", 2)]})
    write(USES / "salt_from_halite_crushing.json", {"type": "create:crushing", "ingredients": item("raw_halite"), "processing_time": 250,
                                                    "results": [result("salt", 2), {"id": "fundamentals:salt", "chance": 0.5}]})


def titanium():
    """Ilmenite is smelted with coke in an electric furnace at about 1,650 °C, as at Sorel and Richards Bay: the iron runs off as
    pig iron and the titanium stays in a slag of 80 to 90 per cent TiO2. Rutile is 95 per cent TiO2 already. Either is chlorinated
    with coke at about 1,000 °C to titanium tetrachloride, which boils at 136 °C and is distilled off. Molten magnesium under argon
    reduces it in a steel retort at 800 to 850 °C to a sponge of titanium and magnesium chloride (Kroll), and the chloride is
    electrolysed back to magnesium and chlorine for the next batch. The first magnesium comes from the sea: the Dow way, seawater, lime and
    hydrochloric acid; or from the bittern salt-making leaves, boiled down to the chloride. Titanium melts at 1,668 °C and takes oxygen and nitrogen from the air hot, so the sponge is arc-melted under
    vacuum, argon here. Nine tenths of the world's titanium never becomes metal but white pigment: the tetrachloride burnt in oxygen gives
    pure TiO2 and its chlorine back (the chloride process). The metal's place is where strength for its weight counts, the compressor
    of a gas turbine; the Factory's turbine engine built on titanium plate goes twice as far."""
    electrodes = ("tfmg:electrode", "tfmg:electrode")
    mixing("titania_slag", item("raw_ilmenite", 4) + item("tfmg:coal_coke"), [result("titania_slag", 2), result("tfmg:cast_iron_ingot")], "superheated")
    for feed in ("raw_rutile", "titania_slag"):
        mixing(f"titanium_tetrachloride_from_{feed.removeprefix('raw_')}", item(feed, 2) + item("tfmg:coal_coke") + [fluid("chlorine", 1000)],
               [out_fluid("titanium_tetrachloride", 500)], "heated")
    mixing("magnesium_chloride", [fluid("seawater", 1000), fluid("hydrochloric_acid", 250)] + item("tfmg:limesand"), [result("magnesium_chloride")], "heated")
    mixing("magnesium_chloride_from_bittern", [fluid("bittern", 500)], [result("magnesium_chloride")], "heated")
    pgm_vat("magnesium_ingot", item("magnesium_chloride", 2), [result("magnesium_ingot", 2), out_fluid("chlorine", 500)], machines=electrodes, folder=USES)
    pgm_vat("titanium_sponge", item("magnesium_ingot", 4) + [fluid("titanium_tetrachloride", 500)] + argon(),
            [result("titanium_sponge", 2), result("magnesium_chloride", 4)], folder=USES)
    pgm_vat("titanium_ingot", item("titanium_sponge", 2) + argon(), [result("titanium_ingot", 2)], machines=electrodes, heat="superheated", folder=USES)
    mixing("titanium_oxide", [fluid("titanium_tetrachloride", 250), fluid("tfmg:air", 1000)], [result("titanium_oxide"), out_fluid("chlorine", 500)], "heated")
    write(USES / "titanium_white.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": item("titanium_oxide"),
                                         "result": result("minecraft:white_dye", 4)})
    shaped(USES / "turbine_engine_from_titanium.json", ["OOO", "PHP", "OOO"],
           {"H": {"item": "tfmg:heavy_machinery_casing"}, "O": {"tag": "c:plates/titanium"}, "P": {"item": "tfmg:aluminum_pipe"}},
           {"count": 4, "id": "tfmg:turbine_engine"})


def zirconium():
    """Zircon is the third mineral of the heavy sands, after ilmenite and rutile, and is washed out with them by its weight. Most of it
    never becomes metal: as sand it faces the moulds steel is cast in, standing 2,000 C without being wetted, and milled it is the white
    of every tile glaze. For the metal it takes the titanium road. Chlorinated with coke at about 1,000 C it gives zirconium tetrachloride,
    a white solid that sublimes at 331 C, the silica leaving as silicon tetrachloride; the crude chloride hydrolysed in water and calcined
    is zirconia, the white oxide. Zircon carries a fiftieth as much hafnium, its chemical twin, and the chloride keeps it. The two are
    parted by extractive distillation through a molten chloride (potassium chloroaluminate at Jarrie, salt here), hafnium tetrachloride
    being the more volatile and going overhead. Magnesium under argon reduces either chloride to sponge (Kroll), and the sponge is
    arc-melted as titanium's is. Zirconia with 7 to 8 per cent yttria (7YSZ) is yttria-stabilised zirconia, the thermal-barrier coat of a
    turbine blade. Zirconium shrugs off hot hydrochloric and sulfuric acid that stainless and titanium cannot, so the chemical industry lines
    its vessels with it, and a per cent or two of hafnium in the superalloy keeps its grain boundaries from cracking."""
    electrodes = ("tfmg:electrode", "tfmg:electrode")
    mixing("crude_zirconium_tetrachloride", item("zircon_concentrate", 2) + item("tfmg:coal_coke") + [fluid("chlorine", 1000)],
           [result("crude_zirconium_tetrachloride", 2)], "heated")
    mixing("zirconium_oxide", item("crude_zirconium_tetrachloride") + [fluid("minecraft:water", 500)], [result("zirconium_oxide"), out_fluid("hydrochloric_acid", 250)], "heated")
    mixing("zirconium_tetrachloride", item("crude_zirconium_tetrachloride", 4) + item("salt"),
           [result("zirconium_tetrachloride", 4), {"id": "fundamentals:hafnium_tetrachloride", "chance": 0.1}, {"id": "fundamentals:salt", "chance": 0.9}], "heated")
    for metal in ("zirconium", "hafnium"):
        pgm_vat(f"{metal}_sponge", item(f"{metal}_tetrachloride") + item("magnesium_ingot", 2) + argon(),
                [result(f"{metal}_sponge"), result("magnesium_chloride", 2)], folder=USES)
        pgm_vat(f"{metal}_ingot", item(f"{metal}_sponge", 2) + argon(), [result(f"{metal}_ingot", 2)], machines=electrodes, heat="superheated", folder=USES)
    mixing("yttria_stabilised_zirconia", item("zirconium_oxide", 12) + item("yttrium_oxide"), [result("yttria_stabilised_zirconia", 13)], "superheated")
    shaped(TFMG / "crafting/materials/casting_basin.json", ["BPB", "CZC", "CCC"],
           {"B": {"item": "tfmg:fireproof_brick"}, "C": {"tag": "c:ingots/cast_iron"}, "P": {"item": "tfmg:cast_iron_pipe"}, "Z": {"item": "fundamentals:zircon_concentrate"}},
           {"count": 1, "id": "tfmg:casting_basin"})
    shaped(USES / "white_terracotta_from_zircon.json", ["TTT", "TZT", "TTT"], {"T": {"item": "minecraft:terracotta"}, "Z": {"item": "fundamentals:zircon_concentrate"}},
           {"count": 8, "id": "minecraft:white_terracotta"})
    shaped(USES / "steel_chemical_vat_from_zirconium.json", ["PPP", "NTN", "PPP"],
           {"N": {"tag": "c:plates/zirconium"}, "P": {"item": "tfmg:heavy_plate"}, "T": {"item": "tfmg:steel_fluid_tank"}},
           {"count": 4, "id": "tfmg:steel_chemical_vat"})
    mixing("superalloy_with_hafnium", *superalloy(item("hafnium_nugget"), 9), "superheated")


def beryllium():
    """Beryl will not open to acid as it is. The Kjellgren-Sawyer process melts it in an arc furnace at about 1,650 C, past a blaze
    cake's 1,600, so the superheated basin stands in; the melt is quenched in water to a glass, the frit, which hot sulfuric acid opens,
    taking the beryllium out as sulfate with the beryl's aluminium. Bertrandite, the Spor Mountain ore, leaches as it is, but is poorer:
    a quarter as rich here, and the real ore is under one per cent beryllium oxide to sorted beryl's ten. Ammonia throws down beryllium
    hydroxide once the aluminium has crystallised out as alum.
    Hydrofluoric acid and ammonia take the hydroxide to ammonium fluoroberyllate, crystallised clean, which at about 1,000 C gives up its
    ammonium fluoride and leaves beryllium fluoride. Magnesium under argon reduces the fluoride at about 1,300 C to pebbles of beryllium in a
    slag of magnesium fluoride, and the pebbles are melted down. Calcined, the hydroxide is beryllia. Most beryllium goes into copper: two per
    cent makes beryllium copper, as strong as steel, springy, and sparkless when struck, the metal of connector springs and of the tools used
    where gas may lie. It is mostly made without the metal, from the oxide reduced by carbon under molten copper in an arc furnace. Emerald is
    beryl, green with a trace of chromium. The hydroxide, the oxide, the salts and the metal's own dust scar the lungs (berylliosis)."""
    mixing("beryl_frit", item("raw_beryl", 2) + [fluid("minecraft:water", 250)], [result("beryl_frit", 2)], "superheated")
    mixing("beryl_frit_from_emerald", item("minecraft:emerald") + [fluid("minecraft:water", 250)], [{"id": "fundamentals:beryl_frit", "chance": 0.5}], "superheated")
    mixing("beryllium_sulfate_liquor", item("beryl_frit", 2) + [fluid("tfmg:sulfuric_acid", 500)], [out_fluid("beryllium_sulfate_liquor", 500)], "heated")
    mixing("beryllium_sulfate_liquor_from_bertrandite", item("raw_bertrandite", 4) + [fluid("tfmg:sulfuric_acid", 500)],
           [out_fluid("beryllium_sulfate_liquor", 250)], "heated")
    mixing("beryllium_hydroxide", [fluid("beryllium_sulfate_liquor", 500), fluid("ammonia", 250)], [result("beryllium_hydroxide", 2)])
    mixing("ammonium_fluoroberyllate", item("beryllium_hydroxide", 2) + [fluid("hydrofluoric_acid", 500), fluid("ammonia", 250)], [result("ammonium_fluoroberyllate", 2)])
    for feed, made in (("ammonium_fluoroberyllate", "beryllium_fluoride"), ("beryllium_hydroxide", "beryllium_oxide")):
        write(USES / f"{made}.json", {"type": "minecraft:blasting", "category": "misc", "ingredient": {"item": f"fundamentals:{feed}"},
                                      "result": {"id": f"fundamentals:{made}"}, "experience": 0.3, "cookingtime": 100})
    pgm_vat("beryllium_pebbles", item("beryllium_fluoride", 2) + item("magnesium_ingot", 2) + argon(), [result("beryllium_pebbles", 2), result("tfmg:slag")],
            heat="superheated", folder=USES)
    mixing("beryllium_ingot", item("beryllium_pebbles", 2) + argon(), [result("beryllium_ingot", 2)], "superheated")
    mixing("beryllium_copper", item("beryllium_nugget") + tag("c:ingots/copper", 5), [result("beryllium_copper_ingot", 5)], "heated")
    mixing("beryllium_copper_from_oxide", item("beryllium_oxide") + tag("c:storage_blocks/copper", 4) + item("tfmg:coal_coke"),
           [result("beryllium_copper_block", 4)], "superheated")
    shaped(USES / "cable_connector_from_beryllium_copper.json", ["OOO", " C ", " N "],
           {"C": {"item": "tfmg:unfinished_insulator"}, "N": {"tag": "c:ingots/beryllium_copper"}, "O": {"tag": "c:nuggets/steel"}},
           {"count": 3, "id": "tfmg:cable_connector"})
    tag_file(DATA / "tags/item/beryllium_dusts.json", [f"fundamentals:{name}" for name in
                                                       ("beryllium_hydroxide", "beryllium_oxide", "beryllium_fluoride", "ammonium_fluoroberyllate", "beryllium_pebbles")])


def fluid(id, amount):
    return fluid_ingredient(id, amount)


def out_fluid(id, amount):
    return {"id": id if ":" in id else f"fundamentals:{id}", "amount": amount}


def pgm_mixing(name, ingredients, results, heat=None):
    recipe = {"type": "create:mixing", "ingredients": ingredients, "results": results}
    if heat:
        recipe["heat_requirement"] = heat
    write(PLATINUM / f"{name}.json", recipe)


def pgm_vat(name, ingredients, results, machines=("tfmg:mixing",), heat="heated", folder=PLATINUM):
    recipe = {"type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"],
              "machines": list(machines), "min_size": 1, "processing_time": 100, "ingredients": ingredients, "results": results}
    if heat:
        recipe["heat_requirement"] = heat
    write(folder / f"{name}.json", recipe)


def platinum_feeds():
    """The platinum metals ride in nickel-copper sulfide and are never won alone. Pentlandite smelts unroasted in the bloomery to
    nickel matte, as flash furnaces smelt Norilsk and Sudbury concentrate, and the matte collects the platinum metals. The converter
    (two matte and a sand, superheated) blows out the iron to converter matte; copper matte from the porphyry chain goes in alongside.
    The base-metal refinery leaches converter matte in hot sulfuric acid: nickel and copper go into solution, and what does not
    dissolve is the platinum group concentrate, one time in ten from a plain nickel matte. The layered intrusion's platinum minerals
    (sperrylite, cooperite, braggite) go into the same leach with the matte and come out as concentrate every time: they are what makes
    a platinum reef worth more than a nickel mine. The leach liquor electrowins to nickel, a little copper, and its acid back."""
    write(DATA / "recipe/bloomery/nickel_matte_from_pentlandite.json", {"type": "fundamentals:bloomery", "ingredient": {"item": "fundamentals:raw_pentlandite"},
                                                                       "result": {"id": "fundamentals:nickel_matte_dust", "count": 1}, "byproduct": {"id": "tfmg:slag", "count": 1}})
    write(DATA / "tags/item/platinum_minerals.json", {"replace": False, "values": [f"fundamentals:raw_{m}" for m in ("sperrylite", "cooperite", "braggite")]})
    sand = tag("c:sands/colorless")
    pgm_mixing("converter_matte", item("nickel_matte_dust", 2) + sand, [result("converter_matte_dust", 2), result("tfmg:slag")], "superheated")
    pgm_mixing("converter_matte_with_copper", item("nickel_matte_dust") + item("copper_matte_dust") + sand, [result("converter_matte_dust", 2), result("tfmg:slag")], "superheated")
    acid = fluid("tfmg:sulfuric_acid", 500)
    pgm_mixing("matte_leach", item("converter_matte_dust", 2) + [acid],
               [out_fluid("nickel_copper_sulfate", 500), {"id": "fundamentals:platinum_group_concentrate", "chance": 0.1}], "heated")
    pgm_mixing("matte_leach_with_platinum_minerals", item("converter_matte_dust", 2) + tag("fundamentals:platinum_minerals") + [acid],
               [out_fluid("nickel_copper_sulfate", 500), result("platinum_group_concentrate")], "heated")
    pgm_vat("nickel_electrowinning", [fluid("nickel_copper_sulfate", 500)],
            [result("tfmg:nickel_ingot"), {"id": "minecraft:copper_ingot", "chance": 0.5}, out_fluid("tfmg:sulfuric_acid", 250)],
            machines=("tfmg:electrode", "tfmg:electrode"), heat=None)


def platinum_refinery():
    """The classical precious-metal refinery, every step a batch in a basin under a mixer or a vat. Aqua regia (or hydrochloric acid
    with chlorine bubbled through it, as newer refineries leach) takes platinum and palladium and leaves rhodium, iridium, ruthenium and
    osmium undissolved. Ammonium chloride drops platinum as the yellow chloroplatinate; ammonia then acid drop palladium as yellow
    dichlorodiammine palladium. The insolubles are slurried with lime and chlorinated, the hypochlorite oxidising osmium and ruthenium to
    their tetroxides, which boil off; hydrochloric acid catches the ruthenium, the osmium passes on and hydrogen reduces it. What stays
    is iridium and rhodium: chlorinated hot with salt they turn to soluble chloro salts, ammonium chloride drops iridium as the black
    chloroiridate, and rhodium comes last. The chloroplatinate ignites straight to sponge; the other
    salts are reduced under hydrogen. None of the six melts at 1,600 C but palladium, so each sponge is pressed and sintered, as Wollaston
    made platinum malleable."""
    pgm_mixing("ammonia", [fluid("tfmg:hydrogen", 750), fluid("tfmg:air", 250)] + item("raw_magnetite"),
               [out_fluid("ammonia", 500), {"id": "fundamentals:raw_magnetite", "chance": 0.9}], "heated")
    pgm_mixing("ammonium_chloride", [fluid("ammonia", 250), fluid("hydrochloric_acid", 250)], [result("ammonium_chloride", 2)])
    pgm_mixing("aqua_regia", [fluid("hydrochloric_acid", 375), fluid("nitric_acid", 125)], [out_fluid("aqua_regia", 500)])
    pgm_mixing("platinum_palladium_liquor", item("platinum_group_concentrate") + [fluid("aqua_regia", 500)],
               [out_fluid("platinum_palladium_liquor", 500), {"id": "fundamentals:insoluble_residue", "chance": 0.5}], "heated")
    pgm_mixing("platinum_palladium_liquor_from_chlorine", item("platinum_group_concentrate") + [fluid("hydrochloric_acid", 500), fluid("chlorine", 250)],
               [out_fluid("platinum_palladium_liquor", 500), {"id": "fundamentals:insoluble_residue", "chance": 0.5}], "heated")
    pgm_mixing("ammonium_chloroplatinate", [fluid("platinum_palladium_liquor", 500)] + item("ammonium_chloride", 2),
               [result("ammonium_chloroplatinate"), out_fluid("palladium_liquor", 250)])
    pgm_mixing("palladium_tetrammine_liquor", [fluid("palladium_liquor", 250), fluid("ammonia", 250)], [out_fluid("palladium_tetrammine_liquor", 250)])
    pgm_mixing("dichlorodiammine_palladium", [fluid("palladium_tetrammine_liquor", 250), fluid("hydrochloric_acid", 250)],
               [result("dichlorodiammine_palladium"), out_fluid("spent_liquor", 250)])
    pgm_vat("tetroxides", item("insoluble_residue") + item("tfmg:limesand") + [fluid("chlorine", 250), fluid("minecraft:water", 500)],
            [out_fluid("osmium_tetroxide", 50), out_fluid("ruthenium_tetroxide", 100), {"id": "fundamentals:iridium_rhodium_residue", "chance": 0.5}])
    pgm_mixing("ammonium_chlororuthenate", [fluid("ruthenium_tetroxide", 100), fluid("hydrochloric_acid", 250)] + item("ammonium_chloride"),
               [result("ammonium_chlororuthenate")])
    pgm_mixing("iridium_rhodium_liquor", item("iridium_rhodium_residue") + item("salt", 2) + [fluid("chlorine", 250), fluid("hydrochloric_acid", 250)],
               [out_fluid("iridium_rhodium_liquor", 250)], "heated")
    pgm_mixing("ammonium_chloroiridate", [fluid("iridium_rhodium_liquor", 250)] + item("ammonium_chloride", 2),
               [result("ammonium_chloroiridate"), out_fluid("rhodium_liquor", 250)])
    pgm_mixing("ammonium_chlororhodate", [fluid("rhodium_liquor", 250)] + item("ammonium_chloride", 2), [result("ammonium_chlororhodate")], "heated")
    write(PLATINUM / "platinum_sponge.json", {"type": "minecraft:blasting", "category": "misc", "ingredient": {"item": "fundamentals:ammonium_chloroplatinate"},
                                              "result": {"id": "fundamentals:platinum_sponge"}, "experience": 0.5, "cookingtime": 100})
    for salt, metal in (("dichlorodiammine_palladium", "palladium"), ("ammonium_chlororuthenate", "ruthenium"),
                        ("ammonium_chloroiridate", "iridium"), ("ammonium_chlororhodate", "rhodium")):
        pgm_vat(f"{metal}_sponge", item(salt, 2) + [fluid("tfmg:hydrogen", 250)], [result(f"{metal}_sponge", 2)])
    pgm_vat("osmium_sponge", [fluid("osmium_tetroxide", 100), fluid("tfmg:hydrogen", 250)], [result("osmium_sponge")])
    for metal in PGMS:
        write(PLATINUM / f"{metal}_ingot.json", {"type": "create:compacting", "heat_requirement": "superheated",
                                                 "ingredients": item(f"{metal}_sponge"), "results": [result(f"{metal}_ingot")]})


def platinum_sinks():
    """Platinum and rhenium on alumina reform naphtha to gasoline, giving off hydrogen; platinum with a tenth of rhodium, woven to gauze,
    burns ammonia to the nitric oxide nitric acid is made from (Ostwald). Palladium's place, and most rhodium's, is the exhaust (more_sinks).
    Ruthenium lets a single-crystal superalloy carry more of everything else. A spark plug is an alumina insulator round a nickel-alloy
    centre electrode, and an iridium tip outlasts four plain plugs and a platinum one three (NGK, Denso); osmium,
    pasted and sintered, was the filament of the first metal-filament lamp."""
    mixing("reforming_catalyst", item("platinum_nugget", 2) + item("rhenium_nugget", 2) + item("tfmg:bauxite_powder", 4), [result("reforming_catalyst", 4)], "heated")
    pgm_vat("reforming", item("reforming_catalyst") + [fluid("tfmg:naphtha", 500)],
            [out_fluid("tfmg:gasoline", 400), out_fluid("tfmg:hydrogen", 100), {"id": "fundamentals:reforming_catalyst", "chance": 0.95}], folder=USES)
    shaped(USES / "platinum_rhodium_gauze.json", ["PPP", "PRP", "PPP"], {"P": {"tag": "c:nuggets/platinum"}, "R": {"tag": "c:nuggets/rhodium"}},
           {"count": 1, "id": "fundamentals:platinum_rhodium_gauze"})
    mixing("nitric_acid_from_ammonia", [fluid("ammonia", 250), fluid("tfmg:air", 1000)] + item("platinum_rhodium_gauze"),
           [out_fluid("nitric_acid", 250), {"id": "fundamentals:platinum_rhodium_gauze", "chance": 0.98}], "heated")
    mixing("superalloy_with_ruthenium", *superalloy(item("ruthenium_nugget"), 9), "superheated")
    for tip, count in (("iridium", 4), ("platinum", 3)):
        shaped(USES / f"{tip}_spark_plug.json", ["T", "A", "N"], {"T": {"tag": f"c:nuggets/{tip}"}, "A": {"item": "fundamentals:alumina"}, "N": {"tag": "c:nuggets/nickel"}},
               {"count": count, "id": "tfmg:spark_plug"})
    write(USES / "osmium_filament.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": item("osmium_sponge"), "result": result("osmium_filament", 4)})
    shaped(USES / "light_bulb_from_osmium.json", ["CWC", "CGC", "NNN"],
           {"C": {"tag": "c:nuggets/copper"}, "G": {"item": "create:framed_glass"}, "N": {"tag": "c:nuggets/steel"}, "W": {"item": "fundamentals:osmium_filament"}},
           {"count": 2, "id": "tfmg:light_bulb"})


def chlor_alkali():
    """Nearly all chlorine and all caustic soda come from one cell: brine electrolysed across a cation membrane, 2 NaCl + 2 H2O ->
    Cl2 + H2 + 2 NaOH, chlorine at the anode, hydrogen and caustic at the cathode (O'Brien, Bommaraju and Hine, Handbook of
    Chlor-Alkali Technology, 2005). The anode is De Nora's dimensionally stable anode, titanium coated with ruthenium and iridium oxides
    baked on from their chlorides, which outlasts the graphite it replaced by years (Trasatti, Electrochimica Acta 45, 2000). Before the
    cell, caustic soda was soda ash boiled with lime, Na2CO3 + Ca(OH)2 -> 2 NaOH + CaCO3, which is what Bayer's first plants ran on."""
    mixing("salt_brine", item("salt", 2) + [fluid("minecraft:water", 1000)], [out_fluid("salt_brine", 1000)])
    mixing("dimensionally_stable_anode", tag("c:plates/titanium") + tag("c:nuggets/ruthenium") + tag("c:nuggets/iridium") + [fluid("hydrochloric_acid", 100)],
           [result("dimensionally_stable_anode")], "heated")
    pgm_vat("chlor_alkali", item("dimensionally_stable_anode") + [fluid("salt_brine", 1000)],
            [out_fluid("chlorine", 500), out_fluid("caustic_soda", 500), out_fluid("tfmg:hydrogen", 500),
             {"id": "fundamentals:dimensionally_stable_anode", "chance": 0.99}], machines=("tfmg:electrode", "tfmg:electrode"), heat=None, folder=USES)
    mixing("caustic_soda_from_soda_ash", item("soda_ash") + item("tfmg:limesand") + [fluid("minecraft:water", 500)], [out_fluid("caustic_soda", 250)], "heated")


def aluminium():
    """Bayer (1888): bauxite digested in hot caustic soda under pressure at 150 to 250 C goes into solution as sodium aluminate, and
    what will not dissolve, iron oxide with the titania, silica and the trace scandium and rare earths, settles out as red mud. The
    cooled liquor, seeded, throws down aluminium hydroxide and the caustic goes back to the digesters; calcined at about 1,100 C the
    hydroxide is alumina. Hall-Heroult (1886): alumina dissolved in molten cryolite at about 960 C is electrolysed between a carbon
    cathode and carbon anodes that burn to carbon dioxide as they go, some 400 kg of carbon to the tonne; the cryolite is the bath and
    is all but never spent. Cryolite is made, the Greenland mine being worked out: aluminium hydroxide with hydrofluoric acid and caustic
    soda or soda ash (Grjotheim and Kvande, Introduction to Aluminium Electrolysis, 1993; Ullmann's, Aluminum Oxide)."""
    pgm_vat("sodium_aluminate_liquor", item("tfmg:bauxite_powder", 2) + [fluid("caustic_soda", 500)],
            [out_fluid("sodium_aluminate_liquor", 500), result("red_mud")], folder=USES)
    mixing("aluminium_hydroxide", [fluid("sodium_aluminate_liquor", 500)], [result("aluminium_hydroxide"), out_fluid("caustic_soda", 400)])
    write(USES / "alumina.json", {"type": "minecraft:blasting", "category": "misc", "ingredient": {"item": "fundamentals:aluminium_hydroxide"},
                                  "result": {"id": "fundamentals:alumina"}, "experience": 0.2, "cookingtime": 100})
    mixing("cryolite", item("aluminium_hydroxide") + [fluid("hydrofluoric_acid", 500), fluid("caustic_soda", 250)], [result("cryolite", 2)])
    mixing("cryolite_from_soda_ash", item("aluminium_hydroxide") + item("soda_ash") + [fluid("hydrofluoric_acid", 500)], [result("cryolite", 2)])
    disabled(TFMG / "vat_machine_recipe/aluminum.json")
    pgm_vat("aluminium_ingot", item("alumina", 2) + item("cryolite") + item("tfmg:coal_coke"),
            [result("tfmg:aluminum_ingot"), {"id": "fundamentals:cryolite", "chance": 0.95}, out_fluid("tfmg:carbon_dioxide", 250)],
            machines=("tfmg:graphite_electrode", "tfmg:graphite_electrode"), folder=USES)
    write(TFMG / "mechanical_crafting/spark_plug.json", {"type": "create:mechanical_crafting", "accept_mirrored": False, "category": "misc",
                                                         "pattern": ["N", "A", "S"], "key": {"N": {"tag": "c:nuggets/nickel"}, "A": {"item": "fundamentals:alumina"},
                                                                                             "S": {"tag": "c:nuggets/steel"}},
                                                         "result": {"count": 1, "id": "tfmg:spark_plug"}, "show_notification": False})


def silver():
    """Most silver has always come out of lead. Galena carries a little, and roasted galena smelts in the bloomery not to lead but to
    lead bullion, which holds it; a furnace remelts the bullion to plain lead with the silver lost in it. The Parkes process stirs zinc
    into molten bullion at 450 to 500 C: the two do not mix, silver is some three thousand times more soluble in zinc than in lead, and the zinc rises with it as
    a crust that is skimmed off, the lead underneath desilvered. Cupellation is the older art: the silvery lead is blown with air on a
    hearth of bone ash at about 1,000 C, the lead burns to litharge and soaks into the cupel or runs off, and the silver stays bright.
    Cupelled straight, every bullion goes to litharge; cupelled as crust, the zinc burns to zinc oxide for the retort and only one lead
    in four has to come back from litharge, which charcoal reduces. Rich silver ores, acanthite and native silver, were soaked into a
    lead bath on the cupel and cupelled with it."""
    write(DATA / "recipe/bloomery/lead_from_roasted_galena.json", {"type": "fundamentals:bloomery", "ingredient": {"item": "fundamentals:roasted_galena"},
                                                                   "result": {"id": "fundamentals:lead_bullion_ingot", "count": 1}, "byproduct": {"id": "tfmg:slag", "count": 1}})
    write(USES / "lead_from_bullion.json", {"type": "minecraft:smelting", "category": "misc", "ingredient": {"item": "fundamentals:lead_bullion_ingot"},
                                            "result": {"id": "tfmg:lead_ingot"}, "experience": 0.1, "cookingtime": 200})
    air = [fluid("tfmg:air", 250)]
    cupel = item("minecraft:bone_meal") + air
    mixing("parkes_desilvering", item("lead_bullion_ingot", 4) + tag("c:ingots/zinc"), [result("tfmg:lead_ingot", 3), result("silver_zinc_crust")], "heated")
    mixing("cupellation_of_crust", item("silver_zinc_crust") + cupel, [result("silver_nugget", 4), result("litharge"), result("zinc_oxide")], "heated")
    mixing("cupellation_of_bullion", item("lead_bullion_ingot") + cupel, [result("silver_nugget"), result("litharge")], "heated")
    for ore in ("argentite", "native_silver"):
        mixing(f"cupellation_of_{ore}", item(f"raw_{ore}") + tag("c:ingots/lead") + cupel, [result("silver_ingot"), result("litharge")], "heated")
    mixing("lead_from_litharge", item("litharge") + item("minecraft:charcoal"), [result("tfmg:lead_ingot")], "heated")


def thorium():
    """Monazite's thorium was the rare earth industry's first product: Welsbach's gas mantle, a cotton stocking soaked in the nitrates of
    thorium and a hundredth part of cerium, burnt out to a skeleton of thoria that glows white in a gas flame. The residue dissolves in nitric
    acid to the nitrate. A cerium liquor's 250 mB is an oxide's worth, so 10 mB to four nitrate is the hundredth part. The Factory's gas lamp burns a mantle. The clarifier's sludge, the iron, aluminium and thorium hydroxides the lime
    throws down, is packed into blocks for the tailings dam as the residue is."""
    mixing("thorium_nitrate", item("monazite_residue_dust") + [fluid("nitric_acid", 250)], [result("thorium_nitrate")], "heated")
    mixing("gas_mantle", item("thorium_nitrate", 4) + [fluid("cerium_liquor", 10)] + tag("c:strings", 4), [result("gas_mantle", 4)], "heated")
    shaped(TFMG / "crafting/materials/gas_lamp.json", [" C ", "BGB", "MP "],
           {"B": {"item": "tfmg:cast_iron_bars"}, "C": {"tag": "c:plates/cast_iron"}, "G": {"item": "create:framed_glass"}, "P": {"item": "tfmg:industrial_pipe"},
            "M": {"item": "fundamentals:gas_mantle"}},
           {"count": 1, "id": "tfmg:gas_lamp"})
    shaped(USES / "clarifier_sludge_block.json", ["###", "###", "###"], {"#": {"item": "fundamentals:clarifier_sludge"}},
           {"count": 1, "id": "fundamentals:clarifier_sludge_block"})


def lead_and_zinc_sinks():
    """A lead-acid plate is a lead grid pasted with litharge in sulfuric acid: the Factory's accumulator takes a litharge for its paste where
    it took a block of lead. Zinc oxide is the activator every sulfur cure of rubber needs, so the Factory's rubber takes one."""
    shaped(TFMG / "crafting/materials/accumulator.json", ["LWL", "SXS", "LCL"],
           {"X": {"item": "fundamentals:litharge"}, "C": {"item": "tfmg:industrial_aluminum_casing"}, "L": {"tag": "c:plates/lead"},
            "S": {"item": "tfmg:sulfuric_acid_bucket"}, "W": {"tag": "c:wires/copper"}},
           {"count": 1, "id": "tfmg:accumulator"})
    write(TFMG / "vat_machine_recipe/rubber.json", {
        "type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"],
        "heat_requirement": "heated", "machines": ["tfmg:mixing"], "min_size": 1,
        "ingredients": [{"item": "tfmg:sulfur_dust"}] + item("zinc_oxide") + [fluid("tfmg:heavy_oil", 250)],
        "results": [{"id": "tfmg:rubber_sheet"}]})


def silver_sinks():
    """Silver conducts better than any metal and its oxide conducts too, so a contact that arcs as it makes and breaks stays sound: switchgear
    contacts are silver. The Factory's switches take silver plates where they had lead and brass. A circuit board can be finished in silver
    as well as gold (immersion silver), and the silver-zinc cell, densest of the old batteries, makes an accumulator."""
    shaped(TFMG / "crafting/materials/electrical_switch.json", ["RPR", "SCS", "RPR"],
           {"C": {"item": "tfmg:heavy_machinery_casing"}, "S": {"tag": "c:plates/silver"}, "P": {"item": "tfmg:electric_post"}, "R": {"tag": "c:dusts/redstone"}},
           {"count": 1, "id": "tfmg:electrical_switch"})
    shaped(TFMG / "crafting/materials/large_switch.json", ["WRS", "HMP", "III"],
           {"S": {"tag": "c:plates/silver"}, "I": {"item": "tfmg:cable_connector"}, "M": {"item": "tfmg:steel_mechanism"}, "P": {"item": "tfmg:electric_post"},
            "R": {"item": "tfmg:rebar"}, "H": {"item": "tfmg:steel_cable_hub"}, "W": {"tag": "c:wires/copper"}},
           {"count": 1, "id": "tfmg:large_switch"})
    write(USES / "coated_circuit_board_with_silver.json", {"type": "create:deploying", "ingredients": [{"item": "tfmg:empty_circuit_board"}, {"tag": "c:plates/silver"}],
                                                           "results": [{"id": "tfmg:coated_circuit_board"}]})
    shaped(USES / "accumulator_from_silver_zinc.json", ["SWS", "ZPZ", "SCS"],
           {"S": {"tag": "c:plates/silver"}, "W": {"tag": "c:wires/copper"}, "Z": {"tag": "c:ingots/zinc"}, "P": {"item": "tfmg:plastic_sheet"},
            "C": {"item": "tfmg:industrial_aluminum_casing"}},
           {"count": 1, "id": "tfmg:accumulator"})


def plastics():
    """Polyethylene and polypropylene are made over a Ziegler-Natta catalyst. The first, Natta's, was titanium tetrachloride reduced
    by aluminium powder to violet TiCl3 with AlCl3 in it, and the olefin polymerises on it at a few atmospheres and under 100 °C. So the
    Factory's two plastic vats take a catalyst, which comes back nine times in ten, and still pour its molten plastic. PVC is the
    chemical plant's pipe: ethylene chlorinated and cracked to vinyl chloride, giving off hydrogen chloride, and the monomer polymerised
    as droplets in water to a white resin that is pressed hot into sheet. A sheet of either makes the Factory's plastic pipe, our tank,
    cells and casing. Dyed plastic is opaque: eight plastic blocks and a dye make eight of that colour, and one goes back to nine sheets."""
    mixing("ziegler_natta_catalyst", [fluid("titanium_tetrachloride", 250)] + argon() + item("aluminium_powder"), [result("ziegler_natta_catalyst", 4)], "heated")
    for olefin in ("ethylene", "propylene"):
        write(TFMG / f"vat_machine_recipe/plastic_from_{olefin}.json", {
            "type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"],
            "heat_requirement": "heated", "machines": ["tfmg:mixing"], "min_size": 1,
            "ingredients": [fluid(f"tfmg:{olefin}", 500)] + item("ziegler_natta_catalyst"),
            "results": [out_fluid("tfmg:molten_plastic", 500), {"id": "fundamentals:ziegler_natta_catalyst", "chance": 0.9}]})
    write(PLASTICS / "vinyl_chloride.json", {"type": "create:mixing", "heat_requirement": "heated",
                                             "ingredients": [fluid("tfmg:ethylene", 500), fluid("chlorine", 500)],
                                             "results": [out_fluid("vinyl_chloride", 500), out_fluid("hydrochloric_acid", 500)]})
    write(PLASTICS / "pvc_resin.json", {"type": "create:mixing", "heat_requirement": "heated",
                                        "ingredients": [fluid("vinyl_chloride", 250), fluid("minecraft:water", 250)], "results": [result("pvc_resin")]})
    write(PLASTICS / "pvc_sheet.json", {"type": "create:compacting", "heat_requirement": "heated", "ingredients": item("pvc_resin"),
                                        "results": [result("pvc_sheet")]})
    write(DATA / "tags/item/plastic_sheets.json", {"replace": False, "values": ["fundamentals:pvc_sheet", "tfmg:plastic_sheet"] + optional(CHEMICA_SHEETS)})
    tag_file(DATA / "tags/item/plastic_blocks.json", ["tfmg:plastic_block"] + [f"fundamentals:{name}" for name in PLASTIC_BLOCKS])
    sheets = {"I": {"tag": "fundamentals:plastic_sheets"}}
    shaped(TFMG / "crafting/materials/plastic_pipe.json", ["   ", "III", "   "], sheets, {"count": 4, "id": "tfmg:plastic_pipe"})
    shaped(TFMG / "crafting/materials/plastic_pipe_vertical.json", ["I", "I", "I"], sheets, {"count": 4, "id": "tfmg:plastic_pipe"})
    for dye in DYES:
        shaped(PLASTICS / f"{dye}_plastic_block.json", ["BBB", "BDB", "BBB"], {"B": {"tag": "fundamentals:plastic_blocks"}, "D": {"tag": f"c:dyes/{dye}"}},
               {"count": 8, "id": f"fundamentals:{dye}_plastic_block"})
        write(PLASTICS / f"plastic_sheet_from_{dye}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                            "ingredients": item(f"{dye}_plastic_block"), "result": result("tfmg:plastic_sheet", 9)})
    # the dyed pipe stands on The Factory Must Grow's pipe models, which natural plastic draws translucent
    with zipfile.ZipFile(TFMG_JAR) as jar:
        pipe = json.loads(jar.read("assets/tfmg/blockstates/plastic_pipe.json"))
        for name in TFMG_PLASTIC_MODELS:
            model = json.loads(jar.read(f"assets/tfmg/models/block/{name}.json"))
            write(TFMG_ASSETS / f"models/block/{name}.json", {**model, "render_type": "minecraft:translucent"})
    write(ASSETS / "blockstates/dyed_plastic_pipe.json", pipe)
    write(DATA / "loot_table/blocks/dyed_plastic_pipe.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "tfmg:plastic_pipe"}],
         "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def names():
    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    for name, display in {**ITEMS, **PGM_ITEMS, **MAGNETS}.items():
        lang[f"item.fundamentals.{name}"] = display
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})
    for name, display in PLASTIC_ITEMS.items():
        lang[f"item.fundamentals.{name}"] = display
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})
    lang["block.fundamentals.dyed_plastic_pipe"] = "Dyed Plastic Pipe"
    lang["tooltip.fundamentals.magnet.grade"] = "%s, rated to %s °C"
    lang["tooltip.fundamentals.magnet.field"] = "Overheated: %s%% of its field left"
    lang["tooltip.fundamentals.magnet.demagnetised"] = "Demagnetised: rebuild it with a magnet"
    lang["goggles.fundamentals.magnet.grade"] = "%s, rated to %s °C"
    lang["goggles.fundamentals.magnet.output"] = "At %s °C: %s%% of full output"
    lang["goggles.fundamentals.magnet.field"] = "%s%% of its field left: it has been past %s °C"
    lang["goggles.fundamentals.magnet.demagnetised"] = "Demagnetised past its Curie point, %s °C: rebuild it with a magnet"
    for name, display in {**BLOCKS, **PLASTIC_BLOCKS}.items():
        lang[f"block.fundamentals.{name}"] = display
        write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"fundamentals:block/{name}"}}})
        write(ASSETS / f"models/block/{name}.json", cube(f"fundamentals:block/{name}"))
        write(ASSETS / f"models/item/{name}.json", {"parent": f"fundamentals:block/{name}"})
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [drop_self(name)]})
    write(path, lang)


def main():
    shutil.rmtree(USES, ignore_errors=True)
    shutil.rmtree(TFMG, ignore_errors=True)
    shutil.rmtree(TFMG_LOOT.parent, ignore_errors=True)
    shutil.rmtree(CREATE, ignore_errors=True)
    shutil.rmtree(LITHIUM, ignore_errors=True)
    shutil.rmtree(PLATINUM, ignore_errors=True)
    shutil.rmtree(PLASTICS, ignore_errors=True)
    shutil.rmtree(TFMG_ASSETS / "models", ignore_errors=True)
    for pattern in ("roasted_c*", "roasted_pentlandite_*", "nickel_oxide_*", "copper_calcine_*", "zinc_oxide_*", "roasted_tin_*"):
        for stale in ROASTING.glob(pattern):
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
    copper_sulfides()
    lithium()
    iron()
    zinc()
    nickel()
    tin()
    rocks()
    loot()
    alloys()
    thermometry()
    semiconductors()
    manganese()
    galvanizing()
    tungsten()
    more_sinks()
    chromium()
    rock_salt()
    titanium()
    zirconium()
    beryllium()
    platinum_feeds()
    platinum_refinery()
    platinum_sinks()
    chlor_alkali()
    aluminium()
    silver()
    silver_sinks()
    thorium()
    lead_and_zinc_sinks()
    plastics()
    names()
    print("uses written")


if __name__ == "__main__":
    main()
