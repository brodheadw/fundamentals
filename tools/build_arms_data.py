#!/usr/bin/env python3
"""Black powder, guncotton, smokeless powder, primers, cartridges and gun parts, and the takeover of Vic's Point Blank's
weapon printer recipes and Create Big Cannons' alloys, propellants and fuzes. Both mods are optional: what is written for
them carries their mod_loaded condition, and is checked here against their jars, since the dev runtime has neither."""
import json
import math
import re
import shutil
import zipfile
from pathlib import Path

from PIL import Image

import common
from build_chemica_compat import fluid_ingredient as fluid
from common import ASSETS, DATA, RESOURCES, TEXTURES, TFMG_JAR, read_lang, write, write_lang
from paint_minerals import paint_raw
from paint_separation import CREATE_JAR, heap
from paint_uses import fuel_rod, tube

MODS = Path.home() / "Library/Application Support/ModrinthApp/profiles/Wildspell/mods"
POINTBLANK_JAR = next(MODS.glob("pointblank-neoforge-*.jar"))
CBC_JAR = next(MODS.glob("createbigcannons-*.jar"))
CHEMICA_JAR = next(Path.home().glob(".gradle/caches/modules-2/files-2.1/maven.modrinth/chemica/*/*/chemica-*.jar"))
VANILLA_JAR = Path.home() / ".gradle/caches/fabric-loom/1.21.1/minecraft-merged.jar"
ARMS = DATA / "recipe/arms"
PB = RESOURCES / "data/pointblank/recipe"
CBC = RESOURCES / "data/createbigcannons"
TFMG_GUNPOWDER = RESOURCES / "data/tfmg/recipe/mixing/gunpowder.json"

CHEMICA = {"type": "neoforge:mod_loaded", "modid": "chemica"}
NO_CHEMICA = {"type": "neoforge:not", "value": CHEMICA}
POINTBLANK = [{"type": "neoforge:mod_loaded", "modid": "pointblank"}]
BIG_CANNONS = [{"type": "neoforge:mod_loaded", "modid": "createbigcannons"}]
OFF = {"neoforge:conditions": [{"type": "neoforge:false"}]}

ITEMS = {"mill_cake": "Black Powder Mill Cake", "press_cake": "Black Powder Press Cake", "nitrocellulose": "Nitrocellulose",
         "smokeless_powder": "Smokeless Powder", "mercury_fulminate": "Mercury Fulminate", "primer": "Primer",
         "cartridge_case": "Drawn Brass Case", "jacketed_bullet": "Jacketed Bullet", "barrel_blank": "Barrel Blank",
         "gun_barrel": "Gun Barrel", "lens": "Ground Lens"}

# Six saltpetre to a charcoal and a sulfur, near the 75:15:10 of English government powder.
SALTPETRE, CHARCOAL, SULFUR = 6, 1, 1
MILL_CAKES, GRAINS = 2, 3
PAPER_NITRATED, NITRIC, SULFURIC = 2, 250, 500
POUDRE_B = (4, 250, 200)  # nitrocellulose, ethanol in, ethanol recovered
CORDITE = (2, 250, 100, 4)  # nitrocellulose, nitroglycerin, acetone, powder out
SCHULTZE = 3
FULMINATE = 8
PRIMERS, CASES, BULLETS = 16, 4, 8
NETHERSTEEL = (4, 5)  # steel in, nethersteel out, with one netherite ingot

STEEL = "#c:ingots/hsla_steel"
ALUMINIUM = "#c:ingots/aluminum"
STAINLESS = "#c:ingots/stainless_steel"
PLASTIC = "#c:plates/polyethylene"
SHEET = "tfmg:aluminum_sheet"
SCREW = "tfmg:screw"
INTERNALS = "pointblank:guninternals"
PROCESSOR = "pointblank:processor"
BARREL, LENS, PRIMER, CASE, BULLET, POWDER, GUNCOTTON = (f"fundamentals:{i}" for i in (
    "gun_barrel", "lens", "primer", "cartridge_case", "jacketed_bullet", "smokeless_powder", "nitrocellulose"))
LED, CELL = "fundamentals:led_chip", "fundamentals:galvanic_cell"

# rounds made: (rounds, cases, bullets, smokeless powder); a primer to every round. A case or a bullet is a pistol's worth of brass or lead.
AMMO = {"ammo9mm": (8, 8, 8, 1), "ammo45acp": (8, 8, 12, 1), "ammo357": (8, 8, 8, 2), "ammo50ae": (8, 16, 16, 3),
        "ammo46": (8, 8, 4, 1), "ammo57": (8, 8, 4, 1), "ammo545": (8, 16, 8, 3), "ammo556": (8, 16, 8, 3),
        "ammo762": (8, 16, 12, 3), "ammo68": (8, 24, 12, 5), "ammo762x51": (8, 24, 16, 5), "ammo338lapua": (4, 20, 16, 6),
        "ammo50bmg": (2, 24, 24, 7)}
SHELLS = 8

FRAMES = {**dict.fromkeys(("glock17", "glock18", "p30l", "usp45", "g36c", "g36k", "aug", "aughbar", "p90", "mp7", "ump45",
                           "vector", "sl8", "xm29"), "polymer"),
          **dict.fromkeys(("m4a1", "m4a1mod1", "m4sopmodii", "m16a1", "hk416", "star15", "ar57", "ro635", "uar10", "xm7", "scarl",
                           "m9", "mk14ebr", "l96a1", "ballista", "c14"), "aluminium")}
MACHINE_GUNS = {"lamg", "m249", "mk48"}
LAUNCHERS = {
    "at4": [(SHEET, 6), (PLASTIC, 4), (INTERNALS, 2), (LENS, 1), (SCREW, 4)],
    "smaw": [(SHEET, 6), (PLASTIC, 4), (INTERNALS, 3), (BARREL, 1), (LENS, 2), (SCREW, 4)],
    "javelin": [(SHEET, 6), (PLASTIC, 4), (INTERNALS, 2), (PROCESSOR, 2), (LENS, 4), ("#c:ingots/germanium", 2),
                ("fundamentals:nickel_metal_hydride_cell", 2), (SCREW, 4)],
    "m32mgl": [(BARREL, 1), (STEEL, 6), (INTERNALS, 4), ("fundamentals:hardened_steel", 2), (PLASTIC, 2), (SCREW, 4)],
    "gp25": [(BARREL, 1), (ALUMINIUM, 2), (INTERNALS, 1), (SCREW, 2)],
    "m203launcher": [(BARREL, 1), (ALUMINIUM, 2), (INTERNALS, 1), (SCREW, 2)],
}
REFLEX = [(LENS, 1), (LED, 1), (CELL, 1), (ALUMINIUM, 1)]
PRISM = [(LENS, 3), (LED, 1), (CELL, 1), (ALUMINIUM, 2)]
SCOPE = [(LENS, 5), (ALUMINIUM, 2), (SCREW, 2)]
SUPPRESSOR = [(STAINLESS, 2), (STEEL, 1)]
PARTS = {
    **dict.fromkeys(("aimpoint", "aimpoint_t2", "delta", "holographic", "holographic558", "holographic_em", "moa", "moa_hg",
                     "operatorreflex", "rspec", "srs"), REFLEX),
    **dict.fromkeys(("acog", "hamr", "specter", "hi_red", "hi_red_zoom", "spear", "spearblack"), PRISM),
    **dict.fromkeys(("drake_scope", "eaglescope", "hawk_scope", "precision_scope", "wolf_scope"), SCOPE),
    **dict.fromkeys(("ak_suppressor", "ar_suppressor", "ar_suppressor_tan", "hp_suppressor", "rf_suppressor", "sg_suppressor",
                     "smg_suppressor", "xm7_suppressor"), SUPPRESSOR),
    **dict.fromkeys(("ar_muzzlebrake", "smg_muzzlebrake"), [(STEEL, 1)]),
    **dict.fromkeys(("foregrip", "foregrip_tan", "heragrip", "shortgrip", "stubbygriptan"), [(PLASTIC, 1)]),
    "ak_romgrip": [("#minecraft:planks", 1)],
    **dict.fromkeys(("glockstock", "m9_stock"), [(PLASTIC, 1), (ALUMINIUM, 1)]),
    "cantedrail": [(ALUMINIUM, 1)],
    "scarl_iss": SUPPRESSOR + [(ALUMINIUM, 1)],
    **dict.fromkeys(("m16a3kit", "star15mod3receiver"), [(ALUMINIUM, 2), (INTERNALS, 1), (SCREW, 2)]),
    "m870modshotgun": [(BARREL, 1), (STEEL, 2), (INTERNALS, 2), (PLASTIC, 1), (SCREW, 2)],
    "ulg99cannon": [(ALUMINIUM, 2), (INTERNALS, 2), (LED, 4), ("tfmg:capacitor_item", 4), ("tfmg:copper_wire", 4), (SCREW, 2)],
}
WARHEADS = {
    "grenade40mm": (2, [(SHEET, 1), (GUNCOTTON, 1), (POWDER, 1), (PRIMER, 2)]),
    "grenade20mm": (4, [(SHEET, 1), (GUNCOTTON, 1), (POWDER, 1), (PRIMER, 4)]),
    "at4_rocket": (1, [(SHEET, 2), (GUNCOTTON, 2), (POWDER, 2), ("#c:plates/copper", 1), (PRIMER, 1)]),
    "smaw_rocket": (1, [(SHEET, 2), (GUNCOTTON, 2), (POWDER, 2), ("#c:plates/copper", 1), (PRIMER, 1)]),
    "javelin_rocket": (1, [(SHEET, 3), (GUNCOTTON, 3), (POWDER, 3), ("#c:plates/copper", 1), (PRIMER, 1), (PROCESSOR, 1),
                           ("#c:ingots/germanium", 1), ("fundamentals:nickel_metal_hydride_cell", 1)]),
    "ammolasercharge": (2, [("fundamentals:nickel_metal_hydride_cell", 1)]),
}
GUNMETAL = ("gunmetal_mesh", "gunmetal_ingot_from_smelting_gunmetal_mesh", "gunmetal_ingot_from_blasting_gunmetal_mesh",
            "gunmetal_nugget", "gunmetal_ingot_from_nuggets")

CBC_OFF = {
    "mixing/alloy_steel": "iron and coal stirred cold do not make steel",
    "mixing/alloy_bronze_tin": "half tin is speculum metal, not bronze; ours is seven copper to one tin",
    "mixing/alloy_bronze_brass": "brass and cinder flour do not make bronze",
    "mixing/alloy_bronze_tinless": "copper, zinc and cinder flour make brass, not bronze",
    "mixing/alloy_nethersteel_cast_iron": "nethersteel is a steel; cast iron is not one",
    "compacting/iron_to_cast_iron_ingot": "cast iron is smelted, not pressed out of iron and coal",
    "compacting/iron_to_cast_iron_block": "cast iron is smelted, not pressed out of iron and coal",
    "mixing/guncotton": "guncotton is cellulose in nitric and sulfuric acid, not paper, gunpowder and redstone; ours stands in",
    "mixing/congealed_nitro": "gunpowder, blaze powder and magma cream make no propellant",
    "mixing/congealed_nitro_no_nether": "slime does not gelatinise guncotton; our smokeless powder stands in",
    "hardened_nitro": "its gelatinised dough is no longer made",
    "milling/nitropowder": "its gelatinised dough is no longer made; our smokeless powder stands in",
}
FUZES = ("impact_fuze", "inertia_fuze", "proximity_fuze", "timed_fuze", "wired_fuze")
SMOKELESS_ROUNDS = ("machine_gun_round", "sequenced_assembly/assembling_machine_gun_round", "sequenced_assembly/filling_autocannon_cartridge")


def ing(id):
    return {"tag": id[1:]} if id.startswith("#") else {"item": id}


def items(id, n=1):
    return [ing(id)] * n


def recipe(name, body, conditions=()):
    write(ARMS / f"{name}.json", {**({"neoforge:conditions": list(conditions)} if conditions else {}), **body})


def out(id, count=1):
    return {"id": id, **({"count": count} if count > 1 else {})}


def propellants():
    recipe("black_powder_mill_cake", {"type": "create:mixing", "ingredients": items("#c:dusts/saltpeter", SALTPETRE) + items("minecraft:charcoal", CHARCOAL)
                                      + items("tfmg:sulfur_dust", SULFUR) + [fluid("minecraft:water", 100)],
                                      "results": [out("fundamentals:mill_cake", MILL_CAKES)]})
    recipe("black_powder_press_cake", {"type": "create:pressing", "ingredients": items("fundamentals:mill_cake"), "results": [out("fundamentals:press_cake")]})
    recipe("black_powder", {"type": "create:milling", "processing_time": 100, "ingredients": items("fundamentals:press_cake"),
                            "results": [out("minecraft:gunpowder", GRAINS)]})
    write(TFMG_GUNPOWDER, OFF)
    recipe("nitrocellulose", {"type": "create:mixing", "ingredients": items("minecraft:paper", PAPER_NITRATED)
                              + [fluid("fundamentals:nitric_acid", NITRIC), fluid("tfmg:sulfuric_acid", SULFURIC)],
                              "results": [out(GUNCOTTON, PAPER_NITRATED), {"id": "fundamentals:spent_acid", "amount": SULFURIC}]})
    cellulose, ethanol, recovered = POUDRE_B
    recipe("smokeless_powder_poudre_b", {"type": "create:mixing", "heat_requirement": "heated", "ingredients": items(GUNCOTTON, cellulose)
                                         + [{"type": "neoforge:tag", "amount": ethanol, "tag": "c:ethanol"}],
                                         "results": [out(POWDER, cellulose), {"id": "chemica:ethanol", "amount": recovered}]}, [CHEMICA])
    cellulose, glycerin, acetone, made = CORDITE
    recipe("smokeless_powder_cordite", {"type": "create:mixing", "ingredients": items(GUNCOTTON, cellulose)
                                        + [fluid("chemica:nitroglycerin", glycerin), fluid("chemica:acetone", acetone)],
                                        "results": [out(POWDER, made)]}, [CHEMICA])
    recipe("smokeless_powder_schultze", {"type": "create:mixing", "ingredients": items(GUNCOTTON, SCHULTZE) + items("#c:dusts/saltpeter")
                                         + [fluid("minecraft:water", 100)], "results": [out(POWDER, SCHULTZE)]}, [NO_CHEMICA])
    recipe("mercury_fulminate", {"type": "create:mixing", "ingredients": items("fundamentals:mercury") + [fluid("fundamentals:nitric_acid", 500),
                                 {"type": "neoforge:tag", "amount": 500, "tag": "c:ethanol"}],
                                 "results": [out("fundamentals:mercury_fulminate", FULMINATE)]}, [CHEMICA])


def parts():
    recipe("primer", {"type": "create:deploying", "ingredients": items("#c:plates/brass") + items("fundamentals:mercury_fulminate"),
                      "results": [out(PRIMER, PRIMERS)]})
    recipe("cartridge_case", {"type": "create:pressing", "ingredients": items("#c:plates/brass"), "results": [out(CASE, CASES)]})
    recipe("jacketed_bullet", {"type": "create:deploying", "ingredients": items("#c:ingots/antimonial_lead") + items("#c:plates/copper"),
                               "results": [out(BULLET, BULLETS)]})
    blank = "fundamentals:barrel_blank"
    recipe("gun_barrel", {"type": "create:sequenced_assembly", "ingredient": ing("#c:ingots/steel"), "loops": 1,
                          "transitional_item": out(blank), "results": [out(BARREL)], "sequence": [
                              {"type": "create:filling", "ingredients": items(blank) + [fluid("tfmg:lubrication_oil", 50)], "results": [out(blank)]},
                              {"type": "create:cutting", "ingredients": items(blank), "results": [out(blank)]},
                              {"type": "create:cutting", "ingredients": items(blank), "results": [out(blank)]},
                              {"type": "create:pressing", "ingredients": items(blank), "results": [out(blank)]}]})
    recipe("lens", {"type": "create:sandpaper_polishing", "ingredients": items("#c:glass_panes"), "results": [out(LENS)]})


def printer(name, ingredients, count=1):
    assert 0 < len(ingredients) <= 10 and all(0 < n < 100 for _, n in ingredients), f"the weapon printer takes one to ten ingredients of 1 to 99: {name}"
    write(PB / f"{name}.json", {"neoforge:conditions": POINTBLANK, "type": "pointblank:default",
                                "ingredients": [{"count": n, **ing(id)} for id, n in ingredients],
                                "result": {"item": f"pointblank:{name}", **({"count": count} if count > 1 else {})}})


def gun(name, original):
    took = {(i.get("item") or "#" + i["tag"]): i["count"] for i in original["ingredients"]}
    metal, frame = math.ceil(took["pointblank:gunmetal_ingot"] / 4), FRAMES.get(name, "steel")
    ingredients = [(BARREL, 6 if name == "m134minigun" else 2 if name in MACHINE_GUNS | {"xm29"} else 1),
                   (INTERNALS, math.ceil(took["pointblank:guninternals"] / 4))]
    if frame == "polymer":
        ingredients += [(STEEL, math.ceil(metal / 2)), (PLASTIC, math.ceil(metal / 2))]
    else:
        ingredients += [(ALUMINIUM if frame == "aluminium" else STEEL, metal),
                        ("#minecraft:planks", took["#minecraft:planks"]) if "#minecraft:planks" in took else (PLASTIC, 1 if metal <= 6 else 2)]
    if name in MACHINE_GUNS or name == "m134minigun":
        ingredients.append(("tfmg:steel_mechanism", 2))
    if name == "m134minigun":
        ingredients.append(("tfmg:electric_motor", 1))
    if name == "xm29":
        ingredients += [(PROCESSOR, 1), (LENS, 2)]
    printer(name, ingredients + [(SCREW, 2)])


def point_blank():
    shutil.rmtree(PB.parent, ignore_errors=True)
    with zipfile.ZipFile(POINTBLANK_JAR) as jar:
        originals = {Path(e).stem: json.loads(jar.read(e)) for e in jar.namelist()
                     if e.startswith("data/pointblank/recipe/") and e.endswith(".json")}
    counts = {"rewritten": 0, "off": 0}
    for name, original in originals.items():
        if original["type"] != "pointblank:default":
            continue
        counts["rewritten"] += 1
        if name in AMMO:
            rounds, cases, bullets, powder = AMMO[name]
            printer(name, [(CASE, cases), (BULLET, bullets), (PRIMER, rounds), (POWDER, powder)], rounds)
        elif name == "ammo12gauge":
            printer(name, [(CASE, SHELLS), (PLASTIC, 2), ("#c:ingots/antimonial_lead", 2), (PRIMER, SHELLS), (POWDER, 2)], SHELLS)
        elif name in WARHEADS:
            printer(name, WARHEADS[name][1], WARHEADS[name][0])
        elif name in LAUNCHERS:
            printer(name, LAUNCHERS[name])
        elif name in PARTS:
            printer(name, PARTS[name])
        else:
            gun(name, original)
    for name in GUNMETAL:
        write(PB / f"{name}.json", OFF)
        counts["off"] += 1
    write(PB / "guninternals.json", {"neoforge:conditions": POINTBLANK, "type": "minecraft:crafting_shapeless", "category": "misc",
                                     "ingredients": items("fundamentals:hardened_steel", 2) + items("tfmg:steel_mechanism") + items(SCREW),
                                     "result": {"id": INTERNALS, "count": 2}})
    write(PB / "processor.json", {"neoforge:conditions": POINTBLANK, "type": "minecraft:crafting_shapeless", "category": "misc",
                                  "ingredients": items("fundamentals:microcontroller") + items("tfmg:circuit_board"), "result": {"id": PROCESSOR}})
    write(PB / "printer.json", {"neoforge:conditions": POINTBLANK, "type": "minecraft:crafting_shaped", "category": "misc",
                                "key": {"I": ing("#c:ingots/steel"), "P": ing("minecraft:glass_pane"), "X": ing(PROCESSOR)},
                                "pattern": ["III", "PXP", "III"], "result": {"id": "pointblank:printer"}})
    counts["rewritten"] += 3
    return counts, set(originals)


def swap(node, mapping):
    if isinstance(node, list):
        return [swap(each, mapping) for each in node]
    if isinstance(node, dict):
        return mapping.get(json.dumps(node, sort_keys=True)) or {key: swap(value, mapping) for key, value in node.items()}
    return node


def big_cannons():
    shutil.rmtree(CBC, ignore_errors=True)
    for path in CBC_OFF:
        write(CBC / f"recipe/{path}.json", OFF)
    steel, made = NETHERSTEEL
    write(CBC / "recipe/mixing/alloy_nethersteel_steel.json", {"neoforge:conditions": BIG_CANNONS, "type": "create:mixing", "heat_requirement": "superheated",
                                                                "ingredients": items("minecraft:netherite_ingot") + items("#c:ingots/steel", steel),
                                                                "results": [out("createbigcannons:nethersteel_ingot", made)]})
    key = lambda node: json.dumps(node, sort_keys=True)
    to_primer = {key({"tag": "createbigcannons:dusts_redstone"}): ing(PRIMER)}
    to_smokeless = {key({"tag": t}): {"tag": "createbigcannons:nitropowder"} for t in ("createbigcannons:gunpowder", "createbigcannons:gunpowder_pinch")}
    to_smokeless[key({"tag": "c:nuggets/copper"})] = ing(BULLET)
    with zipfile.ZipFile(CBC_JAR) as jar:
        for names, mapping in ((FUZES, to_primer), (SMOKELESS_ROUNDS, to_smokeless)):
            for path in names:
                original = json.loads(jar.read(f"data/createbigcannons/recipe/{path}.json"))
                rewritten = swap(original, mapping)
                assert rewritten != original, f"nothing to rewrite in {path}"
                write(CBC / f"recipe/{path}.json", {"neoforge:conditions": BIG_CANNONS, **rewritten})
    common.tag(CBC / "tags/item/guncotton.json", [GUNCOTTON])
    common.tag(CBC / "tags/item/nitropowder.json", [POWDER])
    return {"rewritten": 1 + len(FUZES) + len(SMOKELESS_ROUNDS), "off": len(CBC_OFF)}


def known_items():
    known = {f"fundamentals:{k.split('.')[2]}" for k in json.loads(common.LANG.read_text()) if k.startswith(("item.fundamentals.", "block.fundamentals."))}
    known |= {f"fundamentals:{i}" for i in ITEMS}
    for jar_path, space in ((TFMG_JAR, "tfmg"), (CREATE_JAR, "create"), (POINTBLANK_JAR, "pointblank"), (CBC_JAR, "createbigcannons"), (CHEMICA_JAR, "chemica"), (VANILLA_JAR, "minecraft")):
        with zipfile.ZipFile(jar_path) as jar:
            known |= {f"{space}:{Path(e).stem}" for e in jar.namelist() if e.startswith((f"assets/{space}/models/item/", f"assets/{space}/blockstates/"))}
    return known


def check(pointblank_names):
    """Every item a recipe of ours names exists, and every weapon printer recipe of Point Blank's is taken over."""
    known, unknown = known_items(), set()
    for path in [*ARMS.glob("**/*.json"), *PB.glob("**/*.json"), *CBC.glob("recipe/**/*.json"), TFMG_GUNPOWDER]:
        text = json.dumps(json.loads(path.read_text()))
        for found in re.findall(r'"(?:item|id)": "([a-z0-9_]+:[a-z0-9_/]+)"', text):
            if found not in known and not found.startswith(("fundamentals:spent_acid", "chemica:ethanol")):
                unknown.add(f"{path.name}: {found}")
    missing = {n for n in pointblank_names if not (PB / f"{n}.json").exists()} - {"workstation"}
    assert not unknown, f"recipes name items that do not exist: {sorted(unknown)}"
    assert not missing, f"Point Blank recipes not taken over: {sorted(missing)}"


def paint():
    paint_raw("mill_cake", ((28, 28, 30), (52, 52, 56), (78, 78, 82), (104, 104, 108))).save(TEXTURES / "item/mill_cake.png")
    paint_raw("press_cake", ((18, 18, 22), (38, 38, 44), (60, 60, 68), (92, 92, 104))).save(TEXTURES / "item/press_cake.png")
    heap("nitrocellulose", (255, 255, 252), (238, 236, 228), (190, 186, 176)).save(TEXTURES / "item/nitrocellulose.png")
    heap("smokeless_powder", (122, 130, 118), (70, 78, 70), (36, 40, 38)).save(TEXTURES / "item/smokeless_powder.png")
    heap("mercury_fulminate", (236, 232, 222), (196, 190, 180), (140, 134, 126)).save(TEXTURES / "item/mercury_fulminate.png")
    fuel_rod(((44, 46, 50, 255), (92, 94, 100, 255), (140, 142, 150, 255), (178, 180, 188, 255), (210, 212, 220, 255))).save(TEXTURES / "item/barrel_blank.png")
    tube(((22, 24, 30, 255), (44, 48, 58, 255), (70, 76, 90, 255), (104, 110, 126, 255), (150, 156, 172, 255)), (8, 8, 10, 255)).save(TEXTURES / "item/gun_barrel.png")
    brass, copper = ((112, 82, 26), (190, 150, 60), (232, 200, 108), (252, 236, 170)), ((104, 44, 24), (176, 88, 52), (220, 132, 88), (246, 190, 150))
    for name, shape, tones in (("cartridge_case", "case", brass), ("jacketed_bullet", "bullet", copper), ("primer", "cup", brass)):
        rounds(shape, tones).save(TEXTURES / f"item/{name}.png")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            d = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if d <= 36:
                img.putpixel((x, y), (70, 96, 120, 255) if d > 25 else (188, 220, 236, 150) if (x - 5) ** 2 + (y - 5) ** 2 > 3 else (246, 252, 255, 230))
    img.save(TEXTURES / "item/lens.png")


def rounds(shape, tones):
    """Three cases, bullets or primer cups standing side by side, lit from the left."""
    dark, mid, light, shine = (t + (255,) for t in tones)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for left in (1, 6, 11):
        top = {"case": 2, "bullet": 6, "cup": 10}[shape] + (left == 6) * 2
        for y in range(top, 15):
            width = 4
            if shape == "bullet" and y < top + 3:
                width = 2 if y == top else 4 if y > top + 1 else 3
            if shape == "case" and y in (top + 3, top + 4):
                width = 3 if y == top + 3 else 4
            for dx in range(width):
                x = left + (4 - width) // 2 + dx
                img.putpixel((x, y), dark if y == 14 or dx == width - 1 else shine if dx == 0 and y < top + 4 else light if dx == 0 else mid)
    return img


def names():
    lang = read_lang()
    for name, display in ITEMS.items():
        lang[f"item.fundamentals.{name}"] = display
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})
    write_lang(lang)
    # the pack's gunpowder is black powder, corned from saltpetre, charcoal and sulfur
    write(ASSETS.parent / "minecraft/lang/en_us.json", {"item.minecraft.gunpowder": "Black Powder"})


def main():
    shutil.rmtree(ARMS, ignore_errors=True)
    propellants()
    parts()
    pointblank, pointblank_names = point_blank()
    cannons = big_cannons()
    paint()
    names()
    check(pointblank_names)
    with zipfile.ZipFile(POINTBLANK_JAR) as jar:
        hidden = common.hide_dead(jar, "pointblank", lambda i: i.split(":")[1].startswith("gunmetal_") or i == "pointblank:motor")
    with zipfile.ZipFile(CBC_JAR) as jar:
        hidden += common.hide_dead(jar, "createbigcannons", lambda i: i.split(":")[1] in ("guncotton", "congealed_nitro", "hardened_nitro", "nitropowder"))
    print(f"arms: {len(list(ARMS.glob('*.json')))} recipes of ours; Point Blank {pointblank['rewritten']} rewritten, {pointblank['off']} switched off; "
          f"Create Big Cannons {cannons['rewritten']} rewritten, {cannons['off']} switched off; The Factory's gunpowder switched off; {hidden} dead items hidden")


if __name__ == "__main__":
    main()
