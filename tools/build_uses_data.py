#!/usr/bin/env python3
import json
import shutil
import zipfile
from pathlib import Path

import common
from build_chemica_compat import CHEMICA_SHEETS, fluid_ingredient as fluid, optional
from common import ASSETS, DATA, cube, drop_self, ns, read_lang, result, result_fluid, tag as tag_file, write, write_lang

USES = DATA / "recipe/uses"
TFMG = DATA.parent / "tfmg/recipe"
TFMG_LOOT = DATA.parent / "tfmg/loot_table/blocks"
CREATE = DATA.parent / "create/recipe"

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
# In the order of magnet.MagnetGrade.
MAGNET_ALLOYS = {"neodymium_iron_boron": ("ingot", "plate"), "dysprosium_neodymium_iron_boron": ("ingot", "plate"),
                 "samarium_cobalt": ("ingot", "plate"), "alnico": ("ingot",)}
MAGNET_GRADES = ("NdFeB", "Dy-NdFeB", "SmCo", "Alnico")
MAGNETS = {f"{alloy}_magnet": f"{grade} Magnet" for alloy, grade in zip(MAGNET_ALLOYS, MAGNET_GRADES)}
MAGNET_MACHINES = {"electric_motor": "sequenced_assembly/motor", "generator": "sequenced_assembly/generator", "stator": "mechanical_crafting/stator",
                   "electric_pump": "crafting/materials/electric_pump", "voltmeter": "crafting/materials/voltmeter"}
BLOCKS = {"clarifier_sludge_block": "Clarifier Tailings"}
# Registered by uses.Uses in this order.
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
PLASTIC_ITEMS = {"ziegler_natta_catalyst": "Ziegler-Natta Catalyst", "pvc_resin": "PVC Resin", "pvc_sheet": "PVC Sheet"}
PLASTIC_BLOCKS = {f"{dye}_plastic_block": f"{dye.replace('_', ' ').title()} Plastic Block" for dye in DYES}
TFMG_PLASTIC_MODELS = (["plastic_pipe/core_x", "plastic_pipe/core_y", "plastic_pipe/core_z", "plastic_pipe/casing", "plastic_pipe/item",
                        "plastic_pipe/window", "plastic_mechanical_pump/block", "plastic_mechanical_pump/item", "plastic_smart_fluid_pipe/block",
                        "plastic_smart_fluid_pipe/item", "plastic_fluid_valve/item", "plastic_fluid_valve/pointer"]
                       + [f"plastic_fluid_valve/block_{axis}_{state}" for axis in ("horizontal", "vertical") for state in ("open", "closed")]
                       + [f"plastic_pipe/{part}/{side}" for part in ("connection", "drain", "rim", "rim_connector")
                          for side in ("up", "down", "north", "south", "east", "west")])


def argon(amount=100):
    return [fluid("argon", amount)]


def item(id, count=1):
    return [{"item": ns(id)}] * count


def tag(name, count=1):
    return [{"tag": name}] * count


def mixing(name, ingredients, results, heat=None):
    common.mixing(USES / f"{name}.json", ingredients, results, heat)


def shaped(path, pattern, key, result):
    write(path, {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key, "result": result})


def magnets():
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
    write(USES / "ferrocerium_striker.json", {
        "type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": item("cerium_ingot") + tag("c:ingots/iron"),
        "result": {"id": "minecraft:flint_and_steel", "components": {
            "minecraft:unbreakable": {}, "minecraft:custom_name": json.dumps({"text": "Ferrocerium Striker", "italic": False})}}})


def lanthanum():
    write(USES / "fluid_catalytic_cracking.json", {
        "type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"],
        "heat_requirement": "heated", "machines": ["tfmg:mixing"], "min_size": 1, "processing_time": 100,
        "ingredients": [fluid("tfmg:heavy_oil", 500)] + item("lanthanum_oxide"),
        "results": [result_fluid("tfmg:gasoline", 250), result_fluid("tfmg:propylene", 150), {"id": "fundamentals:lanthanum_oxide", "chance": 0.95},
                    {"id": "tfmg:coal_coke_dust", "chance": 0.25}]})


def phosphors():
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
    shaped(TFMG / "crafting/materials/fireproof_chemical_vat.json", ["PRP", "NTN", "YHY"],
           {"H": {"item": "tfmg:heavy_machinery_casing"}, "N": {"item": "tfmg:circuit_board"}, "P": {"item": "tfmg:fireproof_bricks"},
            "R": {"item": "tfmg:rubber_sheet"}, "T": {"item": "tfmg:steel_chemical_vat"}, "Y": {"item": "fundamentals:yttrium_oxide"}},
           {"count": 1, "id": "tfmg:fireproof_chemical_vat"})


def glass():
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
    mixing("aluminium_scandium", item("scandium_nugget") + tag("c:ingots/aluminum", 8), [result("aluminium_scandium_ingot", 8)], "superheated")
    mixing("aluminium_scandium_from_fluoride", item("scandium_fluoride") + tag("c:storage_blocks/aluminum", 4),
           [result("aluminium_scandium_block", 4), result("tfmg:slag")], "superheated")
    shaped(USES / "panel_rack_from_scandium.json", ["S S", "SSS"], {"S": {"tag": "c:plates/aluminium_scandium"}},
           {"count": 2, "id": "fundamentals:panel_rack"})


def roast(ore, roasted, name=None, raw=True):
    for kind, time in (("campfire_cooking", 400), ("smoking", 200)):
        write(ROASTING / f"{name or roasted}_{kind}.json", {"type": f"minecraft:{kind}", "category": "misc", "ingredient": {"item": f"fundamentals:{'raw_' if raw else ''}{ore}"},
                                                   "result": {"id": f"fundamentals:{roasted}"}, "experience": 0.1, "cookingtime": time})


def vat(name, items, fluid_id, amount, results, machines=("tfmg:mixing",), heat="heated"):
    common.vat(USES / f"{name}.json", items + [fluid(fluid_id, amount)], results, machines, heat)


def cobalt():
    roast("cobaltite", "roasted_cobaltite")
    vat("cobalt_ingot", item("roasted_cobaltite", 2), "tfmg:hydrogen", 500, [result("cobalt_ingot", 2)])
    mixing("cobalt_blue", item("roasted_cobaltite") + item("tfmg:bauxite_powder", 2), [result("minecraft:blue_dye", 4)], "superheated")


def copper_molybdenum_rhenium():
    roast("chalcopyrite", "roasted_chalcopyrite")
    mixing("molybdenum_oxide", item("raw_molybdenite", 2), [result("molybdenum_oxide", 2), {"id": "fundamentals:rhenium_flue_dust", "chance": 0.1}], "heated")
    vat("molybdenum_ingot", item("molybdenum_oxide", 2), "tfmg:hydrogen", 500, [result("molybdenum_ingot", 2)])
    mixing("ammonium_perrhenate", item("rhenium_flue_dust") + [fluid("minecraft:water", 250), fluid("ammonia", 50)], [result("ammonium_perrhenate")])
    vat("rhenium_nugget", item("ammonium_perrhenate"), "tfmg:hydrogen", 250, [result("rhenium_nugget", 2)])


def copper_sulfides():
    for ore in ("bornite", "chalcocite", "covellite"):
        roast(ore, "copper_calcine", f"copper_calcine_from_{ore}")
    for feed in ("roasted_chalcopyrite", "copper_calcine"):
        write(DATA / f"recipe/bloomery/copper_matte_from_{feed}.json", {"type": "fundamentals:bloomery", "ingredient": {"item": f"fundamentals:{feed}"},
                                                                       "result": {"id": "fundamentals:copper_matte_dust", "count": 1}, "byproduct": {"id": "tfmg:slag", "count": 1}})
    mixing("blister_copper", item("copper_matte_dust", 2) + tag("c:sands/colorless"), [result("blister_copper_ingot", 2), result("tfmg:slag")], "superheated")
    write(USES / "copper_ingot_from_blister_copper.json", {"type": "minecraft:blasting", "category": "misc", "ingredient": {"item": "fundamentals:blister_copper_ingot"},
                                                           "result": {"id": "minecraft:copper_ingot"}, "experience": 0.3, "cookingtime": 100})


def lithium():
    write(LITHIUM / "calcined_spodumene.json", {"type": "minecraft:blasting", "category": "misc", "ingredient": {"item": "fundamentals:raw_spodumene"},
                                                "result": {"id": "fundamentals:calcined_spodumene"}, "experience": 0.2, "cookingtime": 100})
    write(LITHIUM / "lithium_sulfate_liquor.json", {"type": "create:mixing", "heat_requirement": "heated", "ingredients": item("calcined_spodumene", 2)
                                                    + [fluid("tfmg:sulfuric_acid", 250), fluid("minecraft:water", 500)],
                                                    "results": [result_fluid("lithium_sulfate_liquor", 500)]})
    write(LITHIUM / "lithium_carbonate.json", {"type": "create:mixing", "heat_requirement": "heated", "ingredients": item("soda_ash", 2)
                                               + [fluid("lithium_sulfate_liquor", 500)], "results": [result("lithium_carbonate", 2)]})
    write(LITHIUM / "lithium_chloride.json", {"type": "create:mixing", "heat_requirement": "heated", "ingredients": item("lithium_carbonate", 2)
                                              + [fluid("hydrochloric_acid", 500)],
                                              "results": [result("lithium_chloride", 2), result_fluid("tfmg:carbon_dioxide", 250)]})
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
    for ore in ("hematite", "magnetite", "goethite"):
        write(USES / f"crushed_iron_from_{ore}.json", {"type": "create:crushing", "ingredients": item(f"raw_{ore}"), "processing_time": 400,
                                                       "results": [{"id": "create:crushed_raw_iron"}, {"id": "create:experience_nugget", "chance": 0.75}]})
    for path in ("smelting/iron_ingot_from_crushed", "blasting/iron_ingot_from_crushed", "splashing/crushed_raw_iron"):
        disabled(CREATE / f"{path}.json")


def zinc():
    for ore in ("sphalerite", "smithsonite", "hemimorphite"):
        roast(ore, "zinc_oxide", f"zinc_oxide_from_{ore}")
    mixing("zinc_ingot", item("zinc_oxide") + item("minecraft:charcoal"), [result("create:zinc_ingot")], "superheated")


def nickel():
    roast("pentlandite", "roasted_pentlandite")
    roast("converter_matte_dust", "nickel_oxide", raw=False)
    mixing("ferronickel", item("roasted_pentlandite") + item("minecraft:charcoal"), [result("ferronickel_ingot"), result("tfmg:slag")], "superheated")
    mixing("ferronickel_from_laterite", item("raw_nickel_laterite", 4) + item("minecraft:charcoal", 2),
           [result("ferronickel_ingot"), result("tfmg:slag", 2)], "superheated")
    pgm_vat("nickel_carbonyl", item("nickel_oxide", 2) + [fluid("water_gas", 1000)],
            [result_fluid("nickel_carbonyl", 500), {"id": "fundamentals:platinum_group_concentrate", "chance": 0.1}], folder=USES)
    pgm_vat("carbonyl_decomposition", [fluid("nickel_carbonyl", 500)], [result("nickel_pellets", 2), result_fluid("water_gas", 500)], folder=USES)
    write(USES / "nickel_ingot_from_pellets.json", {"type": "create:compacting", "heat_requirement": "heated", "ingredients": item("nickel_pellets"),
                                                    "results": [result("tfmg:nickel_ingot")]})
    pgm_vat("cobalt_extraction", [fluid("nickel_copper_sulfate", 1000), fluid("p507", 250), fluid("hydrochloric_acid", 50)],
            [result_fluid("nickel_sulfate_liquor", 950), result_fluid("cobalt_chloride_liquor", 50), result_fluid("p507", 245)], heat=None, folder=USES)
    pgm_vat("nickel_electrowinning_from_raffinate", [fluid("nickel_sulfate_liquor", 500)],
            [result("tfmg:nickel_ingot"), {"id": "minecraft:copper_ingot", "chance": 0.5}, result_fluid("tfmg:sulfuric_acid", 250)],
            machines=("tfmg:electrode", "tfmg:electrode"), heat=None, folder=USES)
    pgm_vat("cobalt_electrowinning", [fluid("cobalt_chloride_liquor", 500)], [result("cobalt_ingot"), result_fluid("chlorine", 250)],
            machines=("tfmg:electrode", "tfmg:electrode"), heat=None, folder=USES)
    tag_file(DATA / "tags/item/nickel_dusts.json", [f"fundamentals:{name}" for name in ("nickel_matte_dust", "converter_matte_dust", "roasted_pentlandite", "nickel_oxide")])
    tag_file(DATA / "tags/item/sulfides.json", [f"fundamentals:{name}" for name in ("raw_pentlandite", "raw_cobaltite", "converter_matte_dust")])


def tin():
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
    modifiers = DATA / "loot_modifiers"
    shutil.rmtree(modifiers, ignore_errors=True)
    common = [{"id": f"#c:{name}", "required": False} for name in ("ingots/iron", "nuggets/iron", "storage_blocks/iron", "raw_materials/iron", "storage_blocks/raw_iron")]
    write(DATA / "tags/item/scarce_in_chests.json", {"replace": False, "values": common + [
        "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots", "minecraft:iron_horse_armor",
        "minecraft:iron_sword", "minecraft:iron_axe", "minecraft:iron_pickaxe", "minecraft:iron_shovel", "minecraft:iron_hoe", "minecraft:shears"]})
    write(modifiers / "scarce_iron_in_chests.json", {"type": "fundamentals:scarce_in_chests", "conditions": [],
                                                    "items": "#fundamentals:scarce_in_chests", "keep": 0.25})
    chests = lambda *names: [f"minecraft:chests/{name}" for name in names]
    pool = lambda *entries: [{"item": ns(id), "weight": weight, "min": low, "max": high} for id, weight, low, high in entries]
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
    return (item("tfmg:nickel_ingot", 6) + item("cobalt_ingot") + item("chromium_ingot") + item("tungsten_ingot") + tag("c:nuggets/aluminum", 5)
            + item("rhenium_nugget", 2) + list(extra) + argon()), [result("superalloy_ingot", count)]


def alloys():
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
    mixing("mercury", item("raw_cinnabar") + [fluid("tfmg:air", 250)], [result("mercury")], "heated")
    mixing("chromel", item("tfmg:nickel_ingot", 9) + item("chromium_ingot"), [result("chromel_ingot", 10)], "superheated")
    mixing("alumel", item("tfmg:nickel_ingot", 9) + tag("c:nuggets/aluminum", 4), [result("alumel_ingot", 9)], "superheated")


def semiconductors():
    common.mixing(TFMG / "mixing/n_semiconductor.json", item("tfmg:silicon_ingot", 4) + item("white_phosphorus"), [result("tfmg:n_semiconductor", 4)], "heated")
    common.mixing(TFMG / "mixing/p_semiconductor.json", item("tfmg:silicon_ingot", 4) + item("boric_acid"), [result("tfmg:p_semiconductor", 4)], "heated")


def manganese():
    mixing("ferromanganese", item("raw_pyrolusite", 2) + item("tfmg:coal_coke") + tag("c:nuggets/iron") + tag("tfmg:flux"),
           [result("ferromanganese_ingot"), result("tfmg:slag")], "superheated")
    mixing("manganese_steel", item("ferromanganese_ingot") + tag("c:ingots/steel", 6), [result("manganese_steel_ingot", 7)], "superheated")
    with zipfile.ZipFile(TFMG_JAR) as jar:
        plate = json.loads(jar.read("data/tfmg/recipe/sequenced_assembly/heavy_plate.json"))
    write(USES / "heavy_plate_from_manganese_steel.json", {**plate, "ingredient": {"tag": "c:ingots/manganese_steel"}, "results": [{"id": "tfmg:heavy_plate", "count": 2}]})


def galvanizing():
    mixing("galvanized_steel_plate", item("tfmg:heavy_plate", 4) + tag("c:nuggets/zinc"), [result("galvanized_steel_plate", 4)], "heated")
    write(DATA.parent / "c/tags/item/plates/steel.json", {"replace": False, "values": ["fundamentals:galvanized_steel_plate"]})


def tungsten():
    pgm_vat("sodium_tungstate_liquor", item("raw_wolframite", 2) + [fluid("caustic_soda", 500)], [result_fluid("sodium_tungstate_liquor", 500)], folder=USES)
    mixing("sodium_tungstate_liquor_from_soda_ash", item("raw_wolframite", 2) + item("soda_ash", 2) + [fluid("minecraft:water", 500)],
           [result_fluid("sodium_tungstate_liquor", 500)], "superheated")
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
    write(USES / "salt_from_halite_milling.json", {"type": "create:milling", "ingredients": item("raw_halite"), "processing_time": 200,
                                                   "results": [result("salt", 2)]})
    write(USES / "salt_from_halite_crushing.json", {"type": "create:crushing", "ingredients": item("raw_halite"), "processing_time": 250,
                                                    "results": [result("salt", 2), {"id": "fundamentals:salt", "chance": 0.5}]})


def titanium():
    electrodes = ("tfmg:electrode", "tfmg:electrode")
    mixing("titania_slag", item("raw_ilmenite", 4) + item("tfmg:coal_coke"), [result("titania_slag", 2), result("tfmg:cast_iron_ingot")], "superheated")
    for feed in ("raw_rutile", "titania_slag"):
        mixing(f"titanium_tetrachloride_from_{feed.removeprefix('raw_')}", item(feed, 2) + item("tfmg:coal_coke") + [fluid("chlorine", 1000)],
               [result_fluid("titanium_tetrachloride", 500)], "heated")
    mixing("magnesium_chloride", [fluid("seawater", 1000), fluid("hydrochloric_acid", 250)] + item("tfmg:limesand"), [result("magnesium_chloride")], "heated")
    mixing("magnesium_chloride_from_bittern", [fluid("bittern", 500)], [result("magnesium_chloride")], "heated")
    pgm_vat("magnesium_ingot", item("magnesium_chloride", 2), [result("magnesium_ingot", 2), result_fluid("chlorine", 500)], machines=electrodes, folder=USES)
    pgm_vat("titanium_sponge", item("magnesium_ingot", 4) + [fluid("titanium_tetrachloride", 500)] + argon(),
            [result("titanium_sponge", 2), result("magnesium_chloride", 4)], folder=USES)
    pgm_vat("titanium_ingot", item("titanium_sponge", 2) + argon(), [result("titanium_ingot", 2)], machines=electrodes, heat="superheated", folder=USES)
    mixing("titanium_oxide", [fluid("titanium_tetrachloride", 250), fluid("tfmg:air", 1000)], [result("titanium_oxide"), result_fluid("chlorine", 500)], "heated")
    write(USES / "titanium_white.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": item("titanium_oxide"),
                                         "result": result("minecraft:white_dye", 4)})
    shaped(USES / "turbine_engine_from_titanium.json", ["OOO", "PHP", "OOO"],
           {"H": {"item": "tfmg:heavy_machinery_casing"}, "O": {"tag": "c:plates/titanium"}, "P": {"item": "tfmg:aluminum_pipe"}},
           {"count": 4, "id": "tfmg:turbine_engine"})


def zirconium():
    electrodes = ("tfmg:electrode", "tfmg:electrode")
    mixing("crude_zirconium_tetrachloride", item("zircon_concentrate", 2) + item("tfmg:coal_coke") + [fluid("chlorine", 1000)],
           [result("crude_zirconium_tetrachloride", 2)], "heated")
    mixing("zirconium_oxide", item("crude_zirconium_tetrachloride") + [fluid("minecraft:water", 500)], [result("zirconium_oxide"), result_fluid("hydrochloric_acid", 250)], "heated")
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
    mixing("beryl_frit", item("raw_beryl", 2) + [fluid("minecraft:water", 250)], [result("beryl_frit", 2)], "superheated")
    mixing("beryl_frit_from_emerald", item("minecraft:emerald") + [fluid("minecraft:water", 250)], [{"id": "fundamentals:beryl_frit", "chance": 0.5}], "superheated")
    mixing("beryllium_sulfate_liquor", item("beryl_frit", 2) + [fluid("tfmg:sulfuric_acid", 500)], [result_fluid("beryllium_sulfate_liquor", 500)], "heated")
    mixing("beryllium_sulfate_liquor_from_bertrandite", item("raw_bertrandite", 4) + [fluid("tfmg:sulfuric_acid", 500)],
           [result_fluid("beryllium_sulfate_liquor", 250)], "heated")
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


def pgm_mixing(name, ingredients, results, heat=None):
    common.mixing(PLATINUM / f"{name}.json", ingredients, results, heat)


def pgm_vat(name, ingredients, results, machines=("tfmg:mixing",), heat="heated", folder=PLATINUM):
    common.vat(folder / f"{name}.json", ingredients, results, machines, heat)


def platinum_feeds():
    write(DATA / "recipe/bloomery/nickel_matte_from_pentlandite.json", {"type": "fundamentals:bloomery", "ingredient": {"item": "fundamentals:raw_pentlandite"},
                                                                       "result": {"id": "fundamentals:nickel_matte_dust", "count": 1}, "byproduct": {"id": "tfmg:slag", "count": 1}})
    write(DATA / "tags/item/platinum_minerals.json", {"replace": False, "values": [f"fundamentals:raw_{m}" for m in ("sperrylite", "cooperite", "braggite")]})
    sand = tag("c:sands/colorless")
    pgm_mixing("converter_matte", item("nickel_matte_dust", 2) + sand, [result("converter_matte_dust", 2), result("tfmg:slag")], "superheated")
    pgm_mixing("converter_matte_with_copper", item("nickel_matte_dust") + item("copper_matte_dust") + sand, [result("converter_matte_dust", 2), result("tfmg:slag")], "superheated")
    acid = fluid("tfmg:sulfuric_acid", 500)
    pgm_mixing("matte_leach", item("converter_matte_dust", 2) + [acid],
               [result_fluid("nickel_copper_sulfate", 500), {"id": "fundamentals:platinum_group_concentrate", "chance": 0.1}], "heated")
    pgm_mixing("matte_leach_with_platinum_minerals", item("converter_matte_dust", 2) + tag("fundamentals:platinum_minerals") + [acid],
               [result_fluid("nickel_copper_sulfate", 500), result("platinum_group_concentrate")], "heated")
    pgm_vat("nickel_electrowinning", [fluid("nickel_copper_sulfate", 500)],
            [result("tfmg:nickel_ingot"), {"id": "minecraft:copper_ingot", "chance": 0.5}, result_fluid("tfmg:sulfuric_acid", 250)],
            machines=("tfmg:electrode", "tfmg:electrode"), heat=None)


def platinum_refinery():
    pgm_mixing("ammonia", [fluid("tfmg:hydrogen", 750), fluid("tfmg:air", 250)] + item("raw_magnetite"),
               [result_fluid("ammonia", 500), {"id": "fundamentals:raw_magnetite", "chance": 0.9}], "heated")
    pgm_mixing("ammonium_chloride", [fluid("ammonia", 250), fluid("hydrochloric_acid", 250)], [result("ammonium_chloride", 2)])
    pgm_mixing("aqua_regia", [fluid("hydrochloric_acid", 375), fluid("nitric_acid", 125)], [result_fluid("aqua_regia", 500)])
    pgm_mixing("platinum_palladium_liquor", item("platinum_group_concentrate") + [fluid("aqua_regia", 500)],
               [result_fluid("platinum_palladium_liquor", 500), {"id": "fundamentals:insoluble_residue", "chance": 0.5}], "heated")
    pgm_mixing("platinum_palladium_liquor_from_chlorine", item("platinum_group_concentrate") + [fluid("hydrochloric_acid", 500), fluid("chlorine", 250)],
               [result_fluid("platinum_palladium_liquor", 500), {"id": "fundamentals:insoluble_residue", "chance": 0.5}], "heated")
    pgm_mixing("ammonium_chloroplatinate", [fluid("platinum_palladium_liquor", 500)] + item("ammonium_chloride", 2),
               [result("ammonium_chloroplatinate"), result_fluid("palladium_liquor", 250)])
    pgm_mixing("palladium_tetrammine_liquor", [fluid("palladium_liquor", 250), fluid("ammonia", 250)], [result_fluid("palladium_tetrammine_liquor", 250)])
    pgm_mixing("dichlorodiammine_palladium", [fluid("palladium_tetrammine_liquor", 250), fluid("hydrochloric_acid", 250)],
               [result("dichlorodiammine_palladium"), result_fluid("spent_liquor", 250)])
    pgm_vat("tetroxides", item("insoluble_residue") + item("tfmg:limesand") + [fluid("chlorine", 250), fluid("minecraft:water", 500)],
            [result_fluid("osmium_tetroxide", 50), result_fluid("ruthenium_tetroxide", 100), {"id": "fundamentals:iridium_rhodium_residue", "chance": 0.5}])
    pgm_mixing("ammonium_chlororuthenate", [fluid("ruthenium_tetroxide", 100), fluid("hydrochloric_acid", 250)] + item("ammonium_chloride"),
               [result("ammonium_chlororuthenate")])
    pgm_mixing("iridium_rhodium_liquor", item("iridium_rhodium_residue") + item("salt", 2) + [fluid("chlorine", 250), fluid("hydrochloric_acid", 250)],
               [result_fluid("iridium_rhodium_liquor", 250)], "heated")
    pgm_mixing("ammonium_chloroiridate", [fluid("iridium_rhodium_liquor", 250)] + item("ammonium_chloride", 2),
               [result("ammonium_chloroiridate"), result_fluid("rhodium_liquor", 250)])
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
    mixing("reforming_catalyst", item("platinum_nugget", 2) + item("rhenium_nugget", 2) + item("tfmg:bauxite_powder", 4), [result("reforming_catalyst", 4)], "heated")
    pgm_vat("reforming", item("reforming_catalyst") + [fluid("tfmg:naphtha", 500)],
            [result_fluid("tfmg:gasoline", 400), result_fluid("tfmg:hydrogen", 100), {"id": "fundamentals:reforming_catalyst", "chance": 0.95}], folder=USES)
    shaped(USES / "platinum_rhodium_gauze.json", ["PPP", "PRP", "PPP"], {"P": {"tag": "c:nuggets/platinum"}, "R": {"tag": "c:nuggets/rhodium"}},
           {"count": 1, "id": "fundamentals:platinum_rhodium_gauze"})
    mixing("nitric_acid_from_ammonia", [fluid("ammonia", 250), fluid("tfmg:air", 1000)] + item("platinum_rhodium_gauze"),
           [result_fluid("nitric_acid", 250), {"id": "fundamentals:platinum_rhodium_gauze", "chance": 0.98}], "heated")
    mixing("superalloy_with_ruthenium", *superalloy(item("ruthenium_nugget"), 9), "superheated")
    for tip, count in (("iridium", 4), ("platinum", 3)):
        shaped(USES / f"{tip}_spark_plug.json", ["T", "A", "N"], {"T": {"tag": f"c:nuggets/{tip}"}, "A": {"item": "fundamentals:alumina"}, "N": {"tag": "c:nuggets/nickel"}},
               {"count": count, "id": "tfmg:spark_plug"})
    write(USES / "osmium_filament.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": item("osmium_sponge"), "result": result("osmium_filament", 4)})
    shaped(USES / "light_bulb_from_osmium.json", ["CWC", "CGC", "NNN"],
           {"C": {"tag": "c:nuggets/copper"}, "G": {"item": "create:framed_glass"}, "N": {"tag": "c:nuggets/steel"}, "W": {"item": "fundamentals:osmium_filament"}},
           {"count": 2, "id": "tfmg:light_bulb"})


def chlor_alkali():
    mixing("salt_brine", item("salt", 2) + [fluid("minecraft:water", 1000)], [result_fluid("salt_brine", 1000)])
    mixing("dimensionally_stable_anode", tag("c:plates/titanium") + tag("c:nuggets/ruthenium") + tag("c:nuggets/iridium") + [fluid("hydrochloric_acid", 100)],
           [result("dimensionally_stable_anode")], "heated")
    pgm_vat("chlor_alkali", item("dimensionally_stable_anode") + [fluid("salt_brine", 1000)],
            [result_fluid("chlorine", 500), result_fluid("caustic_soda", 500), result_fluid("tfmg:hydrogen", 500),
             {"id": "fundamentals:dimensionally_stable_anode", "chance": 0.99}], machines=("tfmg:electrode", "tfmg:electrode"), heat=None, folder=USES)
    mixing("caustic_soda_from_soda_ash", item("soda_ash") + item("tfmg:limesand") + [fluid("minecraft:water", 500)], [result_fluid("caustic_soda", 250)], "heated")


def aluminium():
    pgm_vat("sodium_aluminate_liquor", item("tfmg:bauxite_powder", 2) + [fluid("caustic_soda", 500)],
            [result_fluid("sodium_aluminate_liquor", 500), result("red_mud")], folder=USES)
    mixing("aluminium_hydroxide", [fluid("sodium_aluminate_liquor", 500)], [result("aluminium_hydroxide"), result_fluid("caustic_soda", 400)])
    write(USES / "alumina.json", {"type": "minecraft:blasting", "category": "misc", "ingredient": {"item": "fundamentals:aluminium_hydroxide"},
                                  "result": {"id": "fundamentals:alumina"}, "experience": 0.2, "cookingtime": 100})
    mixing("cryolite", item("aluminium_hydroxide") + [fluid("hydrofluoric_acid", 500), fluid("caustic_soda", 250)], [result("cryolite", 2)])
    mixing("cryolite_from_soda_ash", item("aluminium_hydroxide") + item("soda_ash") + [fluid("hydrofluoric_acid", 500)], [result("cryolite", 2)])
    disabled(TFMG / "vat_machine_recipe/aluminum.json")
    pgm_vat("aluminium_ingot", item("alumina", 2) + item("cryolite") + item("tfmg:coal_coke"),
            [result("tfmg:aluminum_ingot"), {"id": "fundamentals:cryolite", "chance": 0.95}, result_fluid("tfmg:carbon_dioxide", 250)],
            machines=("tfmg:graphite_electrode", "tfmg:graphite_electrode"), folder=USES)
    write(TFMG / "mechanical_crafting/spark_plug.json", {"type": "create:mechanical_crafting", "accept_mirrored": False, "category": "misc",
                                                         "pattern": ["N", "A", "S"], "key": {"N": {"tag": "c:nuggets/nickel"}, "A": {"item": "fundamentals:alumina"},
                                                                                             "S": {"tag": "c:nuggets/steel"}},
                                                         "result": {"count": 1, "id": "tfmg:spark_plug"}, "show_notification": False})


def silver():
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
    mixing("thorium_nitrate", item("monazite_residue_dust") + [fluid("nitric_acid", 250)], [result("thorium_nitrate")], "heated")
    mixing("gas_mantle", item("thorium_nitrate", 4) + [fluid("cerium_liquor", 10)] + tag("c:strings", 4), [result("gas_mantle", 4)], "heated")
    shaped(TFMG / "crafting/materials/gas_lamp.json", [" C ", "BGB", "MP "],
           {"B": {"item": "tfmg:cast_iron_bars"}, "C": {"tag": "c:plates/cast_iron"}, "G": {"item": "create:framed_glass"}, "P": {"item": "tfmg:industrial_pipe"},
            "M": {"item": "fundamentals:gas_mantle"}},
           {"count": 1, "id": "tfmg:gas_lamp"})
    shaped(USES / "clarifier_sludge_block.json", ["###", "###", "###"], {"#": {"item": "fundamentals:clarifier_sludge"}},
           {"count": 1, "id": "fundamentals:clarifier_sludge_block"})


def lead_and_zinc_sinks():
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
    mixing("ziegler_natta_catalyst", [fluid("titanium_tetrachloride", 250)] + argon() + item("aluminium_powder"), [result("ziegler_natta_catalyst", 4)], "heated")
    for olefin in ("ethylene", "propylene"):
        write(TFMG / f"vat_machine_recipe/plastic_from_{olefin}.json", {
            "type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"],
            "heat_requirement": "heated", "machines": ["tfmg:mixing"], "min_size": 1,
            "ingredients": [fluid(f"tfmg:{olefin}", 500)] + item("ziegler_natta_catalyst"),
            "results": [result_fluid("tfmg:molten_plastic", 500), {"id": "fundamentals:ziegler_natta_catalyst", "chance": 0.9}]})
    write(PLASTICS / "vinyl_chloride.json", {"type": "create:mixing", "heat_requirement": "heated",
                                             "ingredients": [fluid("tfmg:ethylene", 500), fluid("chlorine", 500)],
                                             "results": [result_fluid("vinyl_chloride", 500), result_fluid("hydrochloric_acid", 500)]})
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
    lang = read_lang()
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
    write_lang(lang)


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
