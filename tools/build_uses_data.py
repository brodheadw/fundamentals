#!/usr/bin/env python3
"""What the rare earths are for: the recipes that spend the metals and oxides, and the recipes of
Create and The Factory Must Grow we take over so that they need them; and the roads the base metals take
instead of Create's crushing-and-furnace shortcuts. Re-run after any edit.

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
         "tungsten_carbide": "Tungsten Carbide", "tungsten_filament": "Tungsten Filament", "clarifier_sludge": "Clarifier Sludge",
         "copper_calcine": "Copper Calcine", "zinc_oxide": "Zinc Oxide", "roasted_pentlandite": "Roasted Pentlandite",
         "lithium_chloride": "Lithium Chloride", "ferroboron": "Ferroboron",
         "soda_ash": "Soda Ash", "sodium_chromate": "Sodium Chromate", "sodium_dichromate": "Sodium Dichromate", "aluminium_powder": "Aluminium Powder",
         "roasted_tin_concentrate": "Roasted Tin Concentrate", "solder": "Solder",
         "titania_slag": "Titania Slag", "magnesium_chloride": "Magnesium Chloride",
         "silver_zinc_crust": "Silver-Zinc Crust", "litharge": "Litharge"}
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


def argon(amount=100):
    return [{"type": "neoforge:single", "amount": amount, "fluid": "fundamentals:argon"}]


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
    """Nd2Fe14B is melted from neodymium (or didymium, as the industry does), iron and boron, with
    dysprosium to hold its field when hot; SmCo5 from samarium and cobalt. Rare earth metal burns in air when
    molten, so both are melted under argon. The boron goes in as ferroboron, which borax, iron and charcoal give
    in the heat of an arc. Both are then polarized into The Factory Must Grow's magnet, which its motors and
    generators are already built from, so a rare earth plant is what a motor needs."""
    mixing("ferroboron", item("raw_borax") + tag("c:ingots/iron") + item("minecraft:charcoal", 2), [result("ferroboron")], "superheated")
    for name, rare in (("neodymium_iron_boron", "neodymium_ingot"), ("neodymium_iron_boron_from_didymium", "didymium_ingot")):
        mixing(name, item(rare, 2) + item("dysprosium_ingot") + tag("c:ingots/iron", 3) + item("ferroboron") + argon(),
               [result("neodymium_iron_boron_ingot", 4)], "superheated")
    mixing("samarium_cobalt", item("samarium_ingot") + item("cobalt_ingot", 4) + argon(), [result("samarium_cobalt_ingot", 2)], "superheated")
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
    rack go twice as far. The master alloy is mostly made without scandium metal at all: the fluoride stirred into molten
    aluminium, which takes the fluorine and gives the scandium to the melt, the aluminium fluoride skimmed off as dross."""
    mixing("aluminium_scandium", item("scandium_ingot") + tag("c:ingots/aluminum", 7), [result("aluminium_scandium_ingot", 8)], "superheated")
    mixing("aluminium_scandium_from_fluoride", item("scandium_fluoride") + tag("c:ingots/aluminum", 7), [result("aluminium_scandium_ingot", 7), result("slag")], "superheated")
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
                                  "ingredients": items + [{"type": "neoforge:single", "amount": amount, "fluid": fluid_id}], "results": results})


def cobalt():
    """Cobaltite roasted of its arsenic and sulfur to the oxide, which hydrogen reduces in a heated vat: cobalt melts at
    1,495 C, past any furnace of the day. Cobalt blue is the oxide calcined with alumina."""
    roast("cobaltite", "roasted_cobaltite")
    vat("cobalt_ingot", item("roasted_cobaltite", 2), "tfmg:hydrogen", 500, [result("cobalt_ingot", 2)])
    mixing("cobalt_blue", item("roasted_cobaltite") + item("tfmg:bauxite_powder", 2), [result("minecraft:blue_dye", 4)], "superheated")


def copper_molybdenum_rhenium():
    """The porphyry chain. Chalcopyrite is roasted of part of its sulfur and smelted to matte. Molybdenite
    roasts to molybdenum trioxide, and the rhenium in it leaves up the flue: the roaster's dust is where every gram of
    rhenium on earth comes from. Both oxides are reduced under hydrogen, as the industry does, in a heated vat."""
    roast("chalcopyrite", "roasted_chalcopyrite")
    mixing("molybdenum_oxide", item("raw_molybdenite", 2), [result("molybdenum_oxide", 2), {"id": "fundamentals:rhenium_flue_dust", "chance": 0.5}], "heated")
    vat("molybdenum_ingot", item("molybdenum_oxide", 2), "tfmg:hydrogen", 500, [result("molybdenum_ingot", 2)])
    vat("rhenium_ingot", item("rhenium_flue_dust", 2), "tfmg:hydrogen", 250, [result("rhenium_ingot")])


def copper_sulfides():
    """A copper sulfide is not smelted to copper but to matte. Roasted chalcopyrite, and the calcine bornite, chalcocite and
    covellite roast to, melt in the bloomery to matte, the copper and iron sulfides, the rest going to slag. Air blown
    through the molten matte burns the iron to an oxide that sand fluxes off and the sulfur to SO2, leaving blister copper,
    pocked where the gas broke out; the blast furnace fire-refines it to copper."""
    for ore in ("bornite", "chalcocite", "covellite"):
        roast(ore, "copper_calcine", f"copper_calcine_from_{ore}")
    for feed in ("roasted_chalcopyrite", "copper_calcine"):
        write(DATA / f"recipe/bloomery/copper_matte_from_{feed}.json", {"type": "fundamentals:bloomery", "ingredient": {"item": f"fundamentals:{feed}"},
                                                                       "result": {"id": "fundamentals:copper_matte_dust", "count": 1}, "byproduct": {"id": "fundamentals:slag", "count": 1}})
    mixing("blister_copper", item("copper_matte_dust", 2) + tag("c:sands/colorless"), [result("blister_copper_ingot", 2), result("slag")], "superheated")
    write(USES / "copper_ingot_from_blister_copper.json", {"type": "minecraft:blasting", "category": "misc", "ingredient": {"item": "fundamentals:blister_copper_ingot"},
                                                           "result": {"id": "minecraft:copper_ingot"}, "experience": 0.3, "cookingtime": 100})


def lithium():
    """Spodumene calcined white in the blast furnace opens to hot hydrochloric acid, which takes its lithium as the
    chloride; boiled dry, that is the salt lithium is won from. Lithium cannot be won from water, so the dry chloride
    is electrolysed molten, at about 450 C, and gives off its chlorine at the anode."""
    write(LITHIUM / "calcined_spodumene.json", {"type": "minecraft:blasting", "category": "misc", "ingredient": {"item": "fundamentals:raw_spodumene"},
                                                "result": {"id": "fundamentals:calcined_spodumene"}, "experience": 0.2, "cookingtime": 100})
    write(LITHIUM / "lithium_chloride.json", {"type": "create:mixing", "heat_requirement": "heated", "ingredients": item("calcined_spodumene", 2)
                                              + [{"type": "neoforge:single", "amount": 500, "fluid": "fundamentals:hydrochloric_acid"}],
                                              "results": [result("lithium_chloride", 2)]})
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
    """Pentlandite roasts to a nickel oxide that charcoal reduces at a blaze cake's heat, its iron going to slag. Laterite is
    too lean to roast and is smelted whole with charcoal, as it is in the electric furnaces of Indonesia."""
    roast("pentlandite", "roasted_pentlandite")
    mixing("nickel_ingot", item("roasted_pentlandite") + item("minecraft:charcoal"), [result("tfmg:nickel_ingot"), result("slag")], "superheated")
    mixing("nickel_ingot_from_laterite", item("raw_nickel_laterite", 4) + item("minecraft:charcoal", 2),
           [result("tfmg:nickel_ingot"), result("slag", 2)], "superheated")


def tin():
    """Cassiterite is heavy (7 to the water's 1) and inert, so a wash leaves it behind as concentrate, hard-rock or placer alike.
    Roasting drives off the sulfur and arsenic of the pyrite and arsenopyrite that ride with it. Charcoal reduces the oxide at 1,200
    to 1,300 C, as the blowing house's shaft furnace did and the bloomery does; a superheated basin with coke stands in for the
    reverberatory. Crude tin carries iron; tin melts at 232 C, so on a gentle heat it runs off the iron-tin hardhead, and green wood
    stirred through the melt (poling) brings the last dross up. Bronze is a quarter tin, and bell metal; tin-lead solder joins circuit boards."""
    write(USES / "tin_concentrate.json", {"type": "create:splashing", "ingredients": item("raw_cassiterite"), "results": [result("tin_concentrate")]})
    roast("tin_concentrate", "roasted_tin_concentrate", raw=False)
    write(DATA / "recipe/bloomery/crude_tin_from_roasted_tin_concentrate.json", {"type": "fundamentals:bloomery", "ingredient": {"item": "fundamentals:roasted_tin_concentrate"},
                                                                                "result": {"id": "fundamentals:crude_tin_ingot", "count": 1}, "byproduct": {"id": "fundamentals:slag", "count": 1}})
    mixing("crude_tin", item("roasted_tin_concentrate", 2) + item("tfmg:coal_coke"), [result("crude_tin_ingot", 2), result("slag")], "superheated")
    mixing("tin_ingot", item("crude_tin_ingot", 2) + tag("c:rods/wooden"), [result("tin_ingot", 2), {"id": "fundamentals:slag", "chance": 0.25}], "heated")
    mixing("bronze_ingot", tag("c:ingots/copper", 3) + tag("c:ingots/tin"), [result("bronze_ingot", 4)], "heated")
    shaped(CREATE / "crafting/curiosities/peculiar_bell.json", ["I", "P"], {"I": {"tag": "c:storage_blocks/bronze"}, "P": {"tag": "c:plates/bronze"}},
           {"count": 1, "id": "create:peculiar_bell"})
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
                                           "underwater_ruin_small", "underwater_ruin_big") + ["bettermineshafts:chests/", "betterdungeons:chests/"], 0.12,
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


def alloys():
    """Where cobalt, chromium, molybdenum and rhenium go: the nickel superalloy of turbine blades, melted under argon as the
    rare earth magnets are, its chromium what keeps it from scaling in the hot gas, and molybdenum steel for the heavy casings."""
    mixing("superalloy", item("tfmg:nickel_ingot", 4) + item("chromium_ingot") + item("cobalt_ingot", 2) + item("rhenium_ingot") + argon(), [result("superalloy_ingot", 4)], "superheated")
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
    """Cerium oxide is the oxygen store of every catalytic converter: the Factory's exhaust takes two. Palladium on the ceria burns what the
    engine left, so a converter made with palladium nuggets lasts as two. Lithium cobalt oxide is the cathode
    the first lithium batteries ran on: the lithium charge takes a cobalt. Neodymium and holmium colour glass, as erbium does."""
    shaped(TFMG / "crafting/materials/exhaust.json", ["BPB", "EPE", "CPC"],
           {"B": {"item": "minecraft:iron_bars"}, "C": {"tag": "c:ingots/cast_iron"}, "P": {"item": "tfmg:cast_iron_pipe"}, "E": {"item": "fundamentals:cerium_oxide"}},
           {"count": 1, "id": "tfmg:exhaust"})
    shaped(USES / "exhaust_with_palladium.json", ["KPK", "EPE", "CPC"],
           {"K": {"tag": "c:nuggets/palladium"}, "C": {"tag": "c:ingots/cast_iron"}, "P": {"item": "tfmg:cast_iron_pipe"}, "E": {"item": "fundamentals:cerium_oxide"}},
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
    mixing("ferrochrome", item("chromite_concentrate", 2) + item("tfmg:coal_coke") + tag("tfmg:flux"), [result("ferrochrome_ingot"), result("slag")], "superheated")
    mixing("stainless_steel", item("ferrochrome_ingot", 3) + item("tfmg:nickel_ingot") + tag("c:ingots/steel", 6), [result("stainless_steel_ingot", 10)], "superheated")
    shaped(TFMG / "crafting/materials/flarestack.json", ["SPS", "BPB", "CPC"],
           {"B": {"item": "minecraft:iron_bars"}, "C": {"tag": "c:ingots/stainless_steel"}, "P": {"item": "tfmg:cast_iron_pipe"}, "S": {"item": "minecraft:flint_and_steel"}},
           {"count": 1, "id": "tfmg:flarestack"})
    mixing("sodium_chromate", item("chromite_concentrate") + item("soda_ash", 2), [result("sodium_chromate", 2)], "heated")
    mixing("sodium_dichromate", item("sodium_chromate", 2) + [{"type": "neoforge:single", "amount": 250, "fluid": "tfmg:sulfuric_acid"}], [result("sodium_dichromate")])
    mixing("chromium_oxide", item("sodium_dichromate") + tag("minecraft:coals"), [result("chromium_oxide"), result("soda_ash")], "heated")
    write(USES / "aluminium_powder.json", {"type": "create:milling", "ingredients": tag("c:ingots/aluminum"), "processing_time": 200,
                                           "results": [result("aluminium_powder", 2)]})
    mixing("chromium_ingot", item("chromium_oxide") + item("aluminium_powder"), [result("chromium_ingot"), result("slag")], "superheated")
    write(USES / "chrome_oxide_green.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": item("chromium_oxide"),
                                             "result": result("minecraft:green_dye", 2)})


def titanium():
    """Ilmenite is smelted with coke in an electric furnace at about 1,650 °C, as at Sorel and Richards Bay: the iron runs off as
    pig iron and the titanium stays in a slag of 80 to 90 per cent TiO2. Rutile is 95 per cent TiO2 already. Either is chlorinated
    with coke at about 1,000 °C to titanium tetrachloride, which boils at 136 °C and is distilled off. Molten magnesium under argon
    reduces it in a steel retort at 800 to 850 °C to a sponge of titanium and magnesium chloride (Kroll), and the chloride is
    electrolysed back to magnesium and chlorine for the next batch. The first magnesium comes the Dow way, from sea water, lime and
    hydrochloric acid. Titanium melts at 1,668 °C and takes oxygen and nitrogen from the air hot, so the sponge is arc-melted under
    vacuum, argon here. Nine tenths of the world's titanium never becomes metal but white pigment: the tetrachloride burnt in oxygen gives
    pure TiO2 and its chlorine back (the chloride process). The metal's place is where strength for its weight counts, the compressor
    of a gas turbine; the Factory's turbine engine built on titanium plate goes twice as far."""
    electrodes = ("tfmg:electrode", "tfmg:electrode")
    mixing("titania_slag", item("raw_ilmenite", 4) + item("tfmg:coal_coke"), [result("titania_slag", 2), result("tfmg:cast_iron_ingot")], "superheated")
    for feed in ("raw_rutile", "titania_slag"):
        mixing(f"titanium_tetrachloride_from_{feed.removeprefix('raw_')}", item(feed, 2) + item("tfmg:coal_coke") + [fluid("chlorine", 1000)],
               [out_fluid("titanium_tetrachloride", 500)], "heated")
    mixing("magnesium_chloride", [fluid("minecraft:water", 1000), fluid("hydrochloric_acid", 250)] + item("tfmg:limesand"), [result("magnesium_chloride")], "heated")
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


def fluid(id, amount):
    return {"type": "neoforge:single", "amount": amount, "fluid": id if ":" in id else f"fundamentals:{id}"}


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
                                                                       "result": {"id": "fundamentals:nickel_matte_dust", "count": 1}, "byproduct": {"id": "fundamentals:slag", "count": 1}})
    write(DATA / "tags/item/platinum_minerals.json", {"replace": False, "values": [f"fundamentals:raw_{m}" for m in ("sperrylite", "cooperite", "braggite")]})
    sand = tag("c:sands/colorless")
    pgm_mixing("converter_matte", item("nickel_matte_dust", 2) + sand, [result("converter_matte_dust", 2), result("slag")], "superheated")
    pgm_mixing("converter_matte_with_copper", item("nickel_matte_dust") + item("copper_matte_dust") + sand, [result("converter_matte_dust", 2), result("slag")], "superheated")
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
    burns ammonia to the nitric oxide nitric acid is made from (Ostwald). Palladium's place is the exhaust (more_sinks).
    Ruthenium lets a single-crystal superalloy carry more of everything else. An iridium-tipped spark plug outlasts four, and osmium,
    pasted and sintered, was the filament of the first metal-filament lamp."""
    mixing("reforming_catalyst", item("platinum_nugget", 2) + item("rhenium_ingot") + item("tfmg:bauxite_powder", 4), [result("reforming_catalyst", 4)], "heated")
    pgm_vat("reforming", item("reforming_catalyst") + [fluid("tfmg:naphtha", 500)],
            [out_fluid("tfmg:gasoline", 400), out_fluid("tfmg:hydrogen", 100), {"id": "fundamentals:reforming_catalyst", "chance": 0.95}], folder=USES)
    shaped(USES / "platinum_rhodium_gauze.json", ["PPP", "PRP", "PPP"], {"P": {"tag": "c:nuggets/platinum"}, "R": {"tag": "c:nuggets/rhodium"}},
           {"count": 1, "id": "fundamentals:platinum_rhodium_gauze"})
    mixing("nitric_acid_from_ammonia", [fluid("ammonia", 250), fluid("tfmg:air", 1000)] + item("platinum_rhodium_gauze"),
           [out_fluid("nitric_acid", 250), {"id": "fundamentals:platinum_rhodium_gauze", "chance": 0.98}], "heated")
    mixing("superalloy_with_ruthenium", item("tfmg:nickel_ingot", 4) + item("chromium_ingot") + item("cobalt_ingot", 2) + item("rhenium_ingot")
           + item("ruthenium_nugget") + argon(), [result("superalloy_ingot", 6)], "superheated")
    shaped(USES / "iridium_spark_plug.json", ["I", "F", "A"], {"I": {"tag": "c:nuggets/iridium"}, "F": {"item": "minecraft:flint"}, "A": {"tag": "c:ingots/aluminum"}},
           {"count": 4, "id": "tfmg:spark_plug"})
    write(USES / "osmium_filament.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": item("osmium_sponge"), "result": result("osmium_filament", 4)})
    shaped(USES / "light_bulb_from_osmium.json", ["CWC", "CGC", "NNN"],
           {"C": {"tag": "c:nuggets/copper"}, "G": {"item": "create:framed_glass"}, "N": {"tag": "c:nuggets/steel"}, "W": {"item": "fundamentals:osmium_filament"}},
           {"count": 2, "id": "tfmg:light_bulb"})


def silver():
    """Most silver has always come out of lead. Galena carries a little, and roasted galena smelts in the bloomery not to lead but to
    lead bullion, which holds it; a furnace remelts the bullion to plain lead with the silver lost in it. The Parkes process stirs zinc
    into molten bullion at 450 to 500 C: the two do not mix, silver is some three thousand times more soluble in zinc than in lead, and the zinc rises with it as
    a crust that is skimmed off, the lead underneath desilvered. Cupellation is the older art: the silvery lead is blown with air on a
    hearth of bone ash at about 1,000 C, the lead burns to litharge and soaks into the cupel or runs off, and the silver stays bright.
    Cupelled straight, every bullion goes to litharge; cupelled as crust, the zinc burns to zinc oxide for the retort and only one lead
    in four has to come back from litharge, which charcoal reduces. Rich silver ores, argentite and native silver, were soaked into a
    lead bath on the cupel and cupelled with it."""
    write(DATA / "recipe/bloomery/lead_from_roasted_galena.json", {"type": "fundamentals:bloomery", "ingredient": {"item": "fundamentals:roasted_galena"},
                                                                   "result": {"id": "fundamentals:lead_bullion_ingot", "count": 1}, "byproduct": {"id": "fundamentals:slag", "count": 1}})
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


def names():
    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    for name, display in {**ITEMS, **PGM_ITEMS}.items():
        lang[f"item.fundamentals.{name}"] = display
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})
    write(path, lang)


def main():
    shutil.rmtree(USES, ignore_errors=True)
    shutil.rmtree(TFMG, ignore_errors=True)
    shutil.rmtree(CREATE, ignore_errors=True)
    shutil.rmtree(LITHIUM, ignore_errors=True)
    shutil.rmtree(PLATINUM, ignore_errors=True)
    for pattern in ("roasted_c*", "roasted_pentlandite_*", "copper_calcine_*", "zinc_oxide_*", "roasted_tin_*"):
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
    tungsten()
    more_sinks()
    chromium()
    titanium()
    platinum_feeds()
    platinum_refinery()
    platinum_sinks()
    silver()
    silver_sinks()
    names()
    print("uses written")


if __name__ == "__main__":
    main()
