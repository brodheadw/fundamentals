#!/usr/bin/env python3
"""Safety glass, polycarbonate and the blast door: recipes, models, blockstates, loot, tags and lang."""
import shutil

from build_chemica_compat import LOADED as CHEMICA_LOADED, fluid_ingredient as fluid
from common import ASSETS, DATA, cube, ns, read_lang, result, result_fluid, write, write_lang

GLASS = DATA / "recipe/glass"
ITEMS = {"hot_glass": "Hot Glass", "hot_glass_pane": "Hot Glass Pane", "bisphenol_a": "Bisphenol A", "polycarbonate_resin": "Polycarbonate Resin",
         "polycarbonate_sheet": "Polycarbonate Sheet"}
# Shatter like glass: only silk touch lifts them whole.
FRAGILE = {"borosilicate_glass": "Borosilicate Glass", "tempered_glass": "Tempered Glass"}
# Hold together when broken: the interlayer keeps a laminate in one piece, and a sapphire window comes out in its bezel.
TOUGH = {"laminated_glass": "Laminated Armour Glass", "sapphire_window": "Sapphire Window"}
PANES = {f"{name}_pane": f"{display} Pane" for name, display in {**FRAGILE, "laminated_glass": "Laminated Armour Glass"}.items()}
SILK_TOUCH = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
# Copied from vanilla's shield: the polycarbonate shield is held the same way, its plate and grip where the oak one's are.
SHIELD_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [10, 6, -4], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [10, 6, 12], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, 180, 5], "translation": [-10, 2, -10], "scale": [1.25, 1.25, 1.25]},
    "firstperson_lefthand": {"rotation": [0, 180, 5], "translation": [10, 0, -10], "scale": [1.25, 1.25, 1.25]},
    "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]},
    "fixed": {"rotation": [0, 180, 0], "translation": [-4.5, 4.5, -5], "scale": [0.55, 0.55, 0.55]},
    "ground": {"rotation": [0, 0, 0], "translation": [2, 4, 2], "scale": [0.25, 0.25, 0.25]}}
SHIELD_BLOCKING = {
    "thirdperson_righthand": {"rotation": [45, 155, 0], "translation": [-3.49, 11, -2], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [45, 155, 0], "translation": [11.51, 7, 2.5], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, 180, -5], "translation": [-15, 5, -11], "scale": [1.25, 1.25, 1.25]},
    "firstperson_lefthand": {"rotation": [0, 180, -5], "translation": [5, 5, -11], "scale": [1.25, 1.25, 1.25]},
    "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]}}


def item(id, count=1):
    return [{"item": ns(id)}] * count


def tag(name, count=1):
    return [{"tag": name}] * count


def recipe(name, obj):
    write(GLASS / f"{name}.json", obj)


def shaped(name, pattern, key, out):
    recipe(name, {"type": "minecraft:crafting_shaped", "category": "building", "pattern": pattern, "key": key, "result": out})


def recipes():
    # Tempering: glass cut to size, heated to about 620 °C, then blown on both faces by jets of air. The skin sets first and the
    # core, shrinking as it cools, pulls it into compression; a crack has to overcome that before it can run, so the glass bends
    # four or five times as far before it breaks. Tempered glass cannot be cut afterwards: it is tempered as a pane, and a pane from a pane.
    for name, source in (("hot_glass", "minecraft:glass"), ("hot_glass_pane", "minecraft:glass_pane")):
        for kind, time in (("smelting", 200), ("blasting", 100)):
            recipe(f"{name}_{kind}", {"type": f"minecraft:{kind}", "category": "misc", "ingredient": {"item": source},
                                      "result": result(name), "experience": 0, "cookingtime": time})
    recipe("tempered_glass", {"type": "create:filling", "ingredients": item("hot_glass") + [fluid("tfmg:air", 250)], "results": [result("tempered_glass")]})
    recipe("tempered_glass_pane", {"type": "create:filling", "ingredients": item("hot_glass_pane") + [fluid("tfmg:air", 100)],
                                   "results": [result("tempered_glass_pane")]})
    # Borosilicate: four fifths silica and an eighth boric oxide, with a little soda. Borax is sodium borate, so it brings both;
    # the glass expands a third as much as soda-lime when heated, and so survives being heated unevenly. It melts hotter, too.
    recipe("borosilicate_glass", {"type": "create:mixing", "heat_requirement": "superheated",
                                  "ingredients": tag("c:sands/colorless", 3) + item("raw_borax"), "results": [result("borosilicate_glass", 3)]})
    for name in ("borosilicate_glass", "laminated_glass"):
        shaped(f"{name}_pane", ["GGG", "GGG"], {"G": {"item": f"fundamentals:{name}"}}, result(f"{name}_pane", 16))
    # Polycarbonate, the interfacial way: phosgene from carbon monoxide and chlorine over charcoal; bisphenol A from two phenol
    # and one acetone, the cumene process's two products, over an acid; the two meet in caustic soda, which takes the hydrogen chloride as salt.
    recipe("phosgene", {"neoforge:conditions": CHEMICA_LOADED, "type": "create:mixing",
                        "ingredients": [fluid("chemica:carbon_monoxide", 250), fluid("chlorine", 250)] + item("minecraft:charcoal"),
                        "results": [result_fluid("phosgene", 250), {"id": "minecraft:charcoal", "chance": 0.95}]})
    recipe("bisphenol_a", {"neoforge:conditions": CHEMICA_LOADED, "type": "create:mixing", "heat_requirement": "heated",
                           # a basin holds two fluids: the acid is a catalyst, not used up, so it is left out of the inputs
                           "ingredients": [fluid("chemica:phenol", 500), fluid("chemica:acetone", 250)],
                           "results": [result("bisphenol_a", 2)]})
    recipe("polycarbonate_resin", {"type": "create:mixing", "ingredients": item("bisphenol_a") + [fluid("phosgene", 250), fluid("caustic_soda", 500)],
                                   "results": [result("polycarbonate_resin"), result_fluid("salt_brine", 500)]})
    recipe("polycarbonate_sheet", {"type": "create:compacting", "heat_requirement": "heated", "ingredients": item("polycarbonate_resin"),
                                   "results": [result("polycarbonate_sheet")]})
    shaped("riot_shield", ["PAP", "PPP", " P "], {"P": {"item": "fundamentals:polycarbonate_sheet"}, "A": {"tag": "c:ingots/aluminum"}}, result("riot_shield"))
    # Glass-clad polycarbonate: tempered glass outside, polycarbonate behind, bonded in an autoclave under heat and pressure.
    # The glass blunts and breaks up a bullet; the polycarbonate catches the pieces and the spall.
    recipe("laminated_glass", {"type": "create:compacting", "heat_requirement": "heated",
                               "ingredients": item("tempered_glass", 2) + item("polycarbonate_sheet"), "results": [result("laminated_glass")]})
    recipe("laminated_glass_pane_from_tempered", {"type": "create:compacting", "heat_requirement": "heated",
                                                  "ingredients": item("tempered_glass_pane", 2) + item("polycarbonate_sheet"),
                                                  "results": [result("laminated_glass_pane", 2)]})
    shaped("sapphire_window", [" P ", "PBP", " P "], {"P": {"tag": "c:plates/steel"}, "B": {"item": "fundamentals:sapphire_boule"}}, result("sapphire_window"))
    shaped("blast_door", ["HYH", "HLH", "HMH"], {"H": {"item": "tfmg:heavy_plate"}, "Y": {"tag": "c:dyes/yellow"},
                                                 "L": {"item": "fundamentals:laminated_glass"}, "M": {"item": "tfmg:electric_motor"}}, result("blast_door"))


def glass_model(texture):
    return {**cube(texture), "render_type": "minecraft:translucent"}


def assets(lang):
    for name, display in {**FRAGILE, **TOUGH}.items():
        lang[f"block.fundamentals.{name}"] = display
        write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"fundamentals:block/{name}"}}})
        write(ASSETS / f"models/block/{name}.json", glass_model(f"fundamentals:block/{name}"))
        write(ASSETS / f"models/item/{name}.json", {"parent": f"fundamentals:block/{name}"})
    for name, display in PANES.items():
        glass = name.removesuffix("_pane")
        lang[f"block.fundamentals.{name}"] = display
        parts = {}
        for part in ("post", "side", "side_alt", "noside", "noside_alt"):
            parts[part] = f"fundamentals:block/{name}_{part}"
            write(ASSETS / f"models/block/{name}_{part}.json", {"parent": f"minecraft:block/template_glass_pane_{part}", "render_type": "minecraft:translucent",
                                                                "textures": {"pane": f"fundamentals:block/{glass}", "edge": f"fundamentals:block/{name}_top"}})
        sides = {"north": ("side", 0), "east": ("side", 90), "south": ("side_alt", 0), "west": ("side_alt", 90)}
        nosides = {"north": ("noside", 0), "east": ("noside_alt", 0), "south": ("noside_alt", 90), "west": ("noside", 270)}
        multipart = [{"apply": {"model": parts["post"]}}]
        multipart += [{"apply": {"model": parts[m], **({"y": y} if y else {})}, "when": {d: "true"}} for d, (m, y) in sides.items()]
        multipart += [{"apply": {"model": parts[m], **({"y": y} if y else {})}, "when": {d: "false"}} for d, (m, y) in nosides.items()]
        write(ASSETS / f"blockstates/{name}.json", {"multipart": multipart})
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "render_type": "minecraft:translucent",
                                                    "textures": {"layer0": f"fundamentals:block/{glass}"}})
    for name, display in ITEMS.items():
        lang[f"item.fundamentals.{name}"] = display
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})
    write(ASSETS / "models/item/polycarbonate_sheet.json", {"parent": "minecraft:item/generated", "render_type": "minecraft:translucent",
                                                           "textures": {"layer0": "fundamentals:item/polycarbonate_sheet"}})
    # the blast door draws itself from its block entity; its blockstate only gives the break particles
    lang["block.fundamentals.blast_door"] = "Blast Door"
    write(ASSETS / "blockstates/blast_door.json", {"variants": {"": {"model": "fundamentals:block/blast_door"}}})
    write(ASSETS / "models/block/blast_door.json", {"textures": {"particle": "fundamentals:block/blast_door_edge"}})
    write(ASSETS / "models/item/blast_door.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "fundamentals:item/blast_door"}})
    lang["item.fundamentals.riot_shield"] = "Riot Shield"
    plate = {"north": [0, 0, 6, 11], "south": [6, 0, 12, 11], "east": [12, 0, 12.5, 11], "west": [12, 0, 12.5, 11], "up": [12, 0, 12.5, 6], "down": [12, 0, 12.5, 6]}
    grip = {face: [13, 0, 16, 4] for face in ("north", "south", "east", "west", "up", "down")}
    elements = [{"from": [-6, -11, 1], "to": [6, 11, 2], "faces": {f: {"uv": uv, "texture": "#shield"} for f, uv in plate.items()}},
                {"from": [-1, -3, -5], "to": [1, 3, 1], "faces": {f: {"uv": uv, "texture": "#shield"} for f, uv in grip.items()}}]
    shield = {"render_type": "minecraft:translucent", "gui_light": "front",
              "textures": {"shield": "fundamentals:item/riot_shield", "particle": "fundamentals:item/riot_shield"}, "elements": elements}
    write(ASSETS / "models/item/riot_shield.json", {**shield, "display": SHIELD_DISPLAY,
                                                     "overrides": [{"predicate": {"blocking": 1}, "model": "fundamentals:item/riot_shield_blocking"}]})
    write(ASSETS / "models/item/riot_shield_blocking.json", {**shield, "display": SHIELD_BLOCKING})


def loot():
    for name in [*FRAGILE, *(f"{n}_pane" for n in FRAGILE)]:
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"fundamentals:{name}"}], "conditions": [SILK_TOUCH]}]})
    for name in [*TOUGH, "laminated_glass_pane", "blast_door"]:
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"fundamentals:{name}"}],
             "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def tags():
    c = DATA.parent / "c/tags"
    ours = lambda *names: {"replace": False, "values": [f"fundamentals:{n}" for n in names]}
    for kind in ("block", "item"):
        write(c / f"{kind}/glass_blocks.json", ours("borosilicate_glass", "tempered_glass", "laminated_glass"))
        write(c / f"{kind}/glass_blocks/colorless.json", ours("borosilicate_glass", "tempered_glass"))
        write(c / f"{kind}/glass_panes.json", ours(*PANES))
        write(c / f"{kind}/glass_panes/colorless.json", ours("borosilicate_glass_pane", "tempered_glass_pane"))
    write(DATA.parent / "minecraft/tags/block/impermeable.json", ours("borosilicate_glass", "tempered_glass", "laminated_glass", "sapphire_window"))
    write(c / "item/tools/shield.json", ours("riot_shield"))
    write(DATA.parent / "minecraft/tags/item/enchantable/durability.json", ours("riot_shield"))


def main():
    shutil.rmtree(GLASS, ignore_errors=True)
    recipes()
    lang = read_lang()
    assets(lang)
    write_lang(lang)
    loot()
    tags()
    print("glass written")


if __name__ == "__main__":
    main()
