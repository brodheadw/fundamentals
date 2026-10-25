#!/usr/bin/env python3
"""Parkas against Cold Sweat's cold: the fibres and shells they are sewn from, their recipes and insulation, and their textures.
Cold Sweat is optional; without it the insulation files are never read and the parkas are light armour."""
import json
import random
import shutil

from PIL import Image

from common import ASSETS, DATA, RESOURCES, TEXTURES, fluid, mixing, read_lang, result, result_fluid, vat, write, write_lang
from paint_separation import heap

RECIPES = DATA / "recipe/cold"
INSULATORS = DATA / "cold_sweat/item/insulator"
ITEM_TEMPS = DATA / "cold_sweat/item/item_temp"
COLD_SWEAT = [{"type": "neoforge:mod_loaded", "modid": "cold_sweat"}]
# cold.FilledFlask lets one in ten of Cold Sweat's waterskin cooling steps through. Held in the hotbar a waterskin warms (or chills)
# its holder 0.025 a tick while its water goes neutral; the flask, giving up its heat ten times slower, gives a tenth of that ten times longer.
FLASK_SLOWER = 10
WATERSKIN_HOTBAR = 0.025
CHEMICA = [{"type": "neoforge:mod_loaded", "modid": "chemica"}]
PIECES = {"hood": ("helmet", ["SSS", "SIS"]), "parka": ("chestplate", ["S S", "SIS", "SIS"]),
          "trousers": ("leggings", ["SIS", "S S", "S S"]), "boots": ("boots", ["I I", "S S"])}
# Cold Sweat spreads its goat fur set 10/14/12/10 over hood, body, legs and feet; ours follow the same shares.
SHARE = {"hood": 10 / 46, "parka": 14 / 46, "trousers": 12 / 46, "boots": 10 / 46}
# Whole-set cold insulation in Cold Sweat's units, where its leather set is 23 and its goat fur set 46. 0.1^(n/40) is how much
# slower the body then drifts toward the world's temperature. About 13 units to the clo; see build_book.cold for the reasoning.
TIERS = {
    "fur": {"names": ("Fur Hood", "Fur Parka", "Fur Trousers", "Fur Kamiks"), "cold": 60, "shell": {"item": "fundamentals:deer_hide"},
            "fill": {"tag": "c:furs/goat"}},
    "down": {"names": ("Down Hood", "Down Parka", "Down Trousers", "Down Booties"), "cold": 76, "shell": {"item": "fundamentals:waxed_canvas"},
             "fill": {"item": "fundamentals:down"}},
    "synthetic": {"names": ("Synthetic Hood", "Synthetic Parka", "Synthetic Trousers", "Synthetic Booties"), "cold": 72,
                  "shell": {"item": "fundamentals:nylon_ripstop"}, "fill": {"item": "fundamentals:polyester_fibre"}},
    "expedition": {"names": ("Expedition Hood", "Expedition Parka", "Expedition Salopettes", "Expedition Boots"), "cold": 100,
                   "shell": {"item": "fundamentals:eptfe_laminate"}, "fill": {"item": "fundamentals:aerogel_blanket"}},
}
LEATHER, GOAT = 23, 46
# warmer each step, down and synthetic within a tenth of each other, the membrane-and-aerogel suit warmest
assert LEATHER < GOAT < TIERS["fur"]["cold"] < min(TIERS["down"]["cold"], TIERS["synthetic"]["cold"]) < TIERS["expedition"]["cold"]
assert abs(TIERS["down"]["cold"] - TIERS["synthetic"]["cold"]) <= TIERS["down"]["cold"] / 10
# Polyester keeps its loft wet, so its wearer chills half as much from soaking; a membrane keeps the whole body dry, but only all of it.
WET_IMMUNITY = {"synthetic": 0.5, "expedition": 1.0}
MATERIALS = {"deer_hide": "Deer Hide", "down": "Down", "waxed_canvas": "Waxed Canvas", "terephthalic_acid": "Terephthalic Acid",
             "polyester_fibre": "Polyester Fibre", "nylon_ripstop": "Nylon Ripstop", "eptfe_laminate": "ePTFE Laminate",
             "sodium_silicate": "Sodium Silicate", "silica_hydrogel": "Silica Hydrogel", "aerogel_blanket": "Aerogel Blanket"}


def piece_cold(tier, piece):
    return round(TIERS[tier]["cold"] * SHARE[piece], 1)


def recipes():
    shutil.rmtree(RECIPES, ignore_errors=True)
    for tier, spec in TIERS.items():
        for piece, (_, pattern) in PIECES.items():
            write(RECIPES / f"{tier}_{piece}.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern,
                                                     "key": {"S": spec["shell"], "I": spec["fill"]}, "result": result(f"{tier}_{piece}")})
    # down is the fluff under a bird's feathers, washed and blown out of them; a feather is mostly quill and vane
    write(RECIPES / "down.json", {"type": "create:splashing", "ingredients": [{"item": "minecraft:feather"}],
                                  "results": [{"id": "fundamentals:down", "chance": 0.35}]})
    write(RECIPES / "waxed_canvas.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                          "ingredients": [{"item": "minecraft:string"}] * 3 + [{"item": "minecraft:honeycomb"}],
                                          "result": result("waxed_canvas", 2)})
    # Amoco's process: p-xylene (here the naphtha's reformate) oxidised by air over cobalt and manganese, back nine times in ten
    vat(RECIPES / "terephthalic_acid.json", [fluid("tfmg:naphtha", 250), fluid("tfmg:air", 1000), {"item": "fundamentals:cobalt_sulfate"},
                                            {"item": "fundamentals:manganese_sulfate"}],
        [result("terephthalic_acid", 2), {"id": "fundamentals:cobalt_sulfate", "chance": 0.9}, {"id": "fundamentals:manganese_sulfate", "chance": 0.9}])
    # silver oxidises ethylene to its oxide, water opens that to glycol, glycol and the acid condense to PET, spun from the melt
    vat(RECIPES / "polyester_fibre.json", [{"item": "fundamentals:terephthalic_acid"}] * 2 + [
        fluid("tfmg:ethylene", 250), fluid("tfmg:air", 250), fluid("minecraft:water", 250), {"item": "fundamentals:silver_nugget"}],
        [result("polyester_fibre", 4), {"id": "fundamentals:silver_nugget", "chance": 0.9}], heat="superheated")
    # Na2CO3 + SiO2 -> Na2SiO3 + CO2, a glass melt at 1,400 °C
    mixing(RECIPES / "sodium_silicate.json", [{"item": "minecraft:sand"}, {"item": "fundamentals:soda_ash"}],
           [result("sodium_silicate"), result_fluid("tfmg:carbon_dioxide", 250)], "superheated")
    # acid turns water glass to silicic acid, which sets as a gel through the batt it was poured into
    mixing(RECIPES / "silica_hydrogel.json", [{"item": "fundamentals:sodium_silicate"}, {"item": "fundamentals:polyester_fibre"},
                                              fluid("minecraft:water", 250), fluid("tfmg:sulfuric_acid", 100)], [result("silica_hydrogel")])
    write(RECIPES / "nylon_ripstop.json", {"neoforge:conditions": CHEMICA, "type": "minecraft:crafting_shapeless", "category": "misc",
                                           "ingredients": [{"item": "chemica:nylon_ingot"}], "result": result("nylon_ripstop", 2)})
    # Gore's 1969 discovery: PTFE stretched fast and hot opens into a web of nodes and fibrils, then is bonded to a face fabric
    write(RECIPES / "eptfe_laminate.json", {"neoforge:conditions": CHEMICA, "type": "create:compacting",
                                            "ingredients": [{"item": "chemica:polytetrafluoroethylene_sheet"}, {"item": "fundamentals:nylon_ripstop"}],
                                            "results": [result("eptfe_laminate", 2)], "heat_requirement": "heated"})
    # water out for ethanol, ethanol out for liquid CO2, then past CO2's critical point (31 °C, 74 bar) so no meniscus crushes the pores
    aerogel = RECIPES / "aerogel_blanket.json"
    vat(aerogel, [{"item": "fundamentals:silica_hydrogel"}, fluid("chemica:ethanol", 250), fluid("tfmg:carbon_dioxide", 500)],
        [result("aerogel_blanket"), result_fluid("chemica:ethanol", 200)], heat=None)
    recipe = json.loads(aerogel.read_text())
    write(aerogel, {"neoforge:conditions": CHEMICA, **recipe})
    # ours read the same heat model and more; Cold Sweat's gauge follows them instead
    write(RESOURCES / "data/cold_sweat/recipe/thermometer.json", {"neoforge:conditions": [{"type": "neoforge:false"}]})
    write(DATA / "tags/item/hidden/cold_sweat.json", {"replace": False, "values": [{"id": "cold_sweat:thermometer", "required": False}]})


def deer_hide():
    modifiers = DATA / "loot_modifiers"
    write(modifiers / "deer_hide.json", {"type": "fundamentals:swap_drop", "from": "minecraft:leather", "to": "fundamentals:deer_hide",
                                         "conditions": [{"condition": "neoforge:loot_table_id", "loot_table_id": "naturalist:entities/deer"}]})
    write(DATA.parent / "neoforge/loot_modifiers/global_loot_modifiers.json", {
        "replace": False, "entries": [f"fundamentals:{path.stem}" for path in sorted(modifiers.glob("*.json"))]})


def insulators():
    shutil.rmtree(INSULATORS, ignore_errors=True)
    for tier in TIERS:
        for piece in PIECES:
            write(INSULATORS / f"{tier}_{piece}.json", {"type": "armor", "item": {"items": [f"fundamentals:{tier}_{piece}"]},
                                                        "insulation": {"cold": piece_cold(tier, piece), "heat": 0.0}})
    write(INSULATORS / "synthetic_wet.json", {"type": "armor", "item": {"items": ["fundamentals:synthetic_parka"]},
                                              "immune_temp_modifiers": {"cold_sweat:water": WET_IMMUNITY["synthetic"]}})
    write(INSULATORS / "expedition_membrane.json", {
        "type": "armor", "item": {"items": ["fundamentals:expedition_parka"]},
        "entity": {"entities": ["*"], "equipment": {slot: {"items": [f"fundamentals:expedition_{piece}"]}
                                                    for slot, piece in (("head", "hood"), ("legs", "trousers"), ("feet", "boots"))}},
        "immune_temp_modifiers": {"cold_sweat:water": WET_IMMUNITY["expedition"]}})


def flask():
    """The vacuum flask: Cold Sweat's waterskin in a silvered, evacuated double wall of borosilicate glass, cased in steel."""
    def step(kind, extra, keep=False):
        s = {"type": kind, "ingredients": [{"item": "fundamentals:unfinished_vacuum_flask"}] + extra,
             "results": [{"id": "fundamentals:unfinished_vacuum_flask"}]}
        return {**s, "keep_held_item": True} if keep else s
    # Brashear's silvering: silver in nitric acid is silver nitrate, ammonia makes it Tollens' reagent, and sugar reduces it to a mirror
    # on the glass. A deployer's held mechanical pump then draws the gap down, and the steel case and a rubber stopper finish it.
    write(RECIPES / "vacuum_flask.json", {
        "neoforge:conditions": COLD_SWEAT, "type": "create:sequenced_assembly", "ingredient": {"item": "fundamentals:borosilicate_glass"}, "loops": 1,
        "results": [{"id": "fundamentals:vacuum_flask"}], "transitional_item": {"id": "fundamentals:unfinished_vacuum_flask"},
        "sequence": [step("create:deploying", [{"item": "fundamentals:silver_nugget"}]),
                     step("create:filling", [fluid("nitric_acid", 50)]),
                     step("create:filling", [fluid("ammonia", 50)]),
                     step("create:deploying", [{"item": "minecraft:sugar"}]),
                     step("create:deploying", [{"item": "create:mechanical_pump"}], keep=True),
                     step("create:deploying", [{"tag": "c:plates/steel"}]),
                     step("create:deploying", [{"item": "tfmg:rubber_sheet"}])]})
    # as Cold Sweat fills its waterskin from a spout
    write(RECIPES / "filled_vacuum_flask.json", {
        "neoforge:conditions": COLD_SWEAT, "type": "create:filling",
        "ingredients": [{"item": "fundamentals:vacuum_flask"}, fluid("minecraft:water", 250)], "results": [{"id": "fundamentals:filled_vacuum_flask"}]})
    for name, sign, bound in (("hot", 1, "0.1:"), ("cold", -1, ":-0.1")):
        write(ITEM_TEMPS / f"filled_vacuum_flask_{name}.json", {
            "item": {"items": ["fundamentals:filled_vacuum_flask"], "components": {"cold_sweat:temperature": bound}},
            "slots": ["hand", "hotbar"], "temperature": sign * WATERSKIN_HOTBAR / FLASK_SLOWER, "trait": "core", "max_effect": 999, "hide_if_unmet": True})
    for hearth in ("boiler_valid", "icebox_valid"):
        write(RESOURCES / f"data/cold_sweat/tags/item/{hearth}.json", {"replace": False, "values": [
            {"id": f"fundamentals:{item}", "required": False} for item in ("vacuum_flask", "filled_vacuum_flask")]})


def models_and_lang():
    lang = read_lang()
    for name, display in {"vacuum_flask": "Vacuum Flask", "filled_vacuum_flask": "Vacuum Flask", "unfinished_vacuum_flask": "Unfinished Vacuum Flask"}.items():
        lang[f"item.fundamentals.{name}"] = display
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})
    for name, display in MATERIALS.items():
        lang[f"item.fundamentals.{name}"] = display
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})
    for tier, spec in TIERS.items():
        for piece, display in zip(PIECES, spec["names"]):
            lang[f"item.fundamentals.{tier}_{piece}"] = display
            write(ASSETS / f"models/item/{tier}_{piece}.json",
                  {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{tier}_{piece}"}})
    write_lang(lang)


# --- textures: each parka is its cloth over the whole of the armour model, ruffed where a real one is ---

COLOURS = {"fur": (150, 112, 74), "down": (92, 96, 58), "synthetic": (44, 82, 168), "expedition": (226, 96, 34)}
RUFF = (226, 214, 188)
YOKE = (54, 56, 62)


def cloth(tier, x, y, lum, rng, ruff=False, yoke=False):
    """One pixel of a tier's cloth: fur's hair, down's deep baffles, synthetic's flatter quilting, the membrane's smooth shell."""
    colour = RUFF if ruff else YOKE if yoke else COLOURS[tier]
    t = 0.55 + 0.75 * lum
    if tier == "fur":
        t *= 0.85 + 0.3 * rng.random() if not ruff else 0.9 + 0.2 * rng.random()
    elif tier == "down":
        t *= (0.7, 1.12, 1.0)[y % 3]
    elif tier == "synthetic":
        t *= (0.72, 1.1, 1.04, 1.0)[y % 4] * (0.95 if x % 3 == 0 else 1)
    elif tier == "expedition" and not yoke:
        t *= 1.06 if (x + y) % 5 else 0.96
    return tuple(max(0, min(255, int(c * t))) for c in colour) + (255,)


# The faces of the humanoid armour model on its 64x32 sheet (x0, y0, x1, y1), filled whole: leather leaves most of them bare.
HEAD = [(8, 0, 24, 8), (0, 8, 32, 16)]
BODY = [(20, 16, 36, 20), (16, 20, 40, 32)]
ARM = [(44, 16, 52, 20), (40, 20, 56, 32)]
FOOT = [(8, 16, 12, 20), (0, 26, 16, 32)]
LEG = [(4, 16, 12, 20), (0, 20, 16, 32)]
WAIST = [(16, 26, 40, 32)]


def armour_layer(tier, faces, seed, ruff=lambda x, y: False, yoke=lambda x, y: False):
    rng = random.Random(seed)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for x0, y0, x1, y1 in faces:
        for y in range(y0, y1):
            for x in range(x0, x1):
                lum = 0.2 if y == y1 - 1 else 0.62 if y == y0 else 0.5
                img.putpixel((x, y), cloth(tier, x, y, lum, rng, ruff(x, y), yoke(x, y)))
    return img


def armour_layers():
    out = TEXTURES / "models/armor"
    for tier in TIERS:
        # a fur ruff round the hood's face and the cuffs, wolverine on a real parka; the expedition shell has a dark yoke
        trimmed = tier != "synthetic"
        ruff = lambda x, y: trimmed and ((8 <= x < 16 and 8 <= y < 16 and (x in (8, 15) or y in (8, 15))) or (40 <= x < 56 and y == 31)
                                         or (tier == "fur" and x < 16 and y == 26))
        yoke = lambda x, y: tier == "expedition" and 20 <= y < 23 and 16 <= x < 56
        outer = armour_layer(tier, HEAD + BODY + ARM + FOOT, f"{tier}-1", ruff, yoke)
        # the hood frames the face; it does not cover it
        outer.paste((0, 0, 0, 0), (9, 9, 15, 15))
        outer.save(out / f"{tier}_layer_1.png")
        armour_layer(tier, LEG + WAIST, f"{tier}-2").save(out / f"{tier}_layer_2.png")

# 0 seam, 1 shade, 2 cloth, 3 lit; r is the ruff round a face or a cuff
ICONS = {
    "hood": ["................", ".....000000.....", "....02222220....", "...0233333220...", "..023rrrrrr320..", "..02r000000r20..",
             "..02r0....0r20..", "..02r0....0r20..", "..02r0....0r20..", "..022r0000r220..", "..0222rrrr2220..", "...0222222220...",
             "...0122222210...", "....01111110....", ".....000000.....", "................"],
    "parka": ["................", "...000r..r000...", "..02223rr32220..", ".02222322322220.", ".02222222222220.", ".02210222201220.",
              ".02210222201220.", ".02210222201220.", ".02210222201220.", ".02210222201220.", ".0rr10222201rr0.", ".0rr10222201rr0.",
              "..0001222210000.", "....01222210....", "....00000000....", "................"],
    "trousers": ["................", "...0000000000...", "...0333333330...", "...0222222220...", "...0222222220...", "...0222002220...",
                 "...0222002220...", "...0222002220...", "...0222002220...", "...0222002220...", "...0222002220...", "...0122002210...",
                 "...0122002210...", "...0111001110...", "...0000000000...", "................"],
    "boots": ["................", "................", "................", "...rrrr..rrrr...", "...0220..0220...", "...0230..0230...",
              "...0220..0220...", "...0220..0220...", "...0220..0220...", "...02200.02200..", "..022220022220..", "..0222220222220.",
              "..0111110111110.", "..000000000000..", "................", "................"],
}


def armour_icons():
    for tier in TIERS:
        rng = random.Random(f"icon-{tier}")
        for piece, rows in ICONS.items():
            img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
            for y, row in enumerate(rows):
                for x, c in enumerate(row):
                    if c == ".":
                        continue
                    ruff = c == "r" and tier != "synthetic"
                    lum = {"0": 0.0, "1": 0.3, "2": 0.5, "3": 0.8, "r": 0.6}[c]
                    img.putpixel((x, y), cloth(tier, x, y, lum, rng, ruff))
            img.save(TEXTURES / f"item/{tier}_{piece}.png")


def tuft(seed, light, body, shadow, alpha=255):
    """A loose fluffy cluster: down's plumules or a staple of fibre."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for _ in range(170):
        x, y = rng.gauss(8, 2.6), rng.gauss(8.5, 2.3)
        if 1 <= x < 15 and 2 <= y < 15:
            colour = light if y < 7.5 else shadow if y > 11 else body
            img.putpixel((int(x), int(y)), colour + (alpha,))
    return img


def fabric(base, seed, weave=None, edge_colour=None, face=None):
    """A folded square of cloth, the fold's underside darker; weave marks a ripstop grid, face a second ply showing at the edge."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    edge_colour = edge_colour or tuple(c * 3 // 5 for c in base)
    for y in range(3, 14):
        for x in range(2, 14):
            fold = x + y > 22
            t = (0.78 if fold else 1.0) * (0.94 + 0.12 * rng.random())
            colour = tuple(min(255, int(c * t)) for c in base)
            if weave and (x % 4 == 0 or y % 4 == 0):
                colour = tuple(min(255, int(c * 0.85)) for c in colour)
            if x in (2, 13) or y in (3, 13) or x + y == 22:
                colour = edge_colour
            if face and y == 13 and 3 <= x <= 12:
                colour = face
            img.putpixel((x, y), colour + (255,))
    return img


def hide():
    """A stretched pelt: four legs and a neck, hair side up, scraped pale at the rim."""
    rng = random.Random("deer")
    rows = ["................", "......####......", ".....######.....", "..##.######.##..", ".############...", "..############..",
            "...##########...", "...##########...", "...##########...", "...##########...", "..############..", ".##.########.##.",
            ".#...######...#.", "......####......", ".......##.......", "................"]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c == "#":
                inner = 0 < x < 15 and rows[y][x - 1] == "#" and rows[y][x + 1] == "#" and rows[y - 1][x] == "#" and rows[y + 1][x] == "#"
                base = (138, 98, 62) if inner else (206, 182, 146)
                t = 0.85 + 0.3 * rng.random()
                img.putpixel((x, y), tuple(min(255, int(c * t)) for c in base) + (255,))
    return img


def slab(colour, alpha, seed):
    """A translucent block: hydrogel's wet sheen, aerogel's frozen smoke."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(4, 14):
        for x in range(2, 14):
            top = y < 6 and x >= 2 + (6 - y)
            side = x >= 12
            t = 1.15 if top else 0.8 if side else 0.95 + 0.1 * rng.random()
            a = min(255, alpha + (60 if x in (2, 13) or y in (4, 13) else 0))
            img.putpixel((x, y), tuple(min(255, int(c * t)) for c in colour) + (a,))
    return img


def material_icons():
    item = TEXTURES / "item"
    hide().save(item / "deer_hide.png")
    tuft("down", (250, 248, 240), (232, 226, 212), (196, 188, 172)).save(item / "down.png")
    tuft("polyester", (246, 248, 252), (222, 228, 238), (186, 194, 210)).save(item / "polyester_fibre.png")
    fabric((112, 104, 66), "canvas").save(item / "waxed_canvas.png")
    fabric((50, 88, 176), "ripstop", weave=True).save(item / "nylon_ripstop.png")
    fabric((238, 238, 234), "eptfe", face=(50, 88, 176)).save(item / "eptfe_laminate.png")
    heap("terephthalic", (252, 252, 250), (232, 232, 228), (200, 200, 196)).save(item / "terephthalic_acid.png")
    heap("silicate", (208, 226, 214), (164, 192, 176), (122, 148, 134)).save(item / "sodium_silicate.png")
    slab((176, 206, 222), 150, "hydrogel").save(item / "silica_hydrogel.png")
    # aerogel scatters blue the way the sky does, and is mostly air
    slab((186, 212, 246), 96, "aerogel").save(item / "aerogel_blanket.png")


STEEL = (168, 174, 182)
MIRROR = (226, 230, 236)
RUBBER = (46, 44, 48)


def cylinder(img, x0, x1, y0, y1, colour, sheen=False, base=True):
    """An upright cylinder lit from the left: bright a pixel in from the left edge, falling off to the right, outlined dark."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            u = (x - x0) / (x1 - x0)
            t = 1.25 if u < 0.25 and x != x0 else 1.0 - 0.45 * u
            if sheen and x == x0 + 2:
                t = 1.4
            if x in (x0, x1) or (base and y == y1):
                t *= 0.55
            img.putpixel((x, y), tuple(max(0, min(255, int(c * t))) for c in colour) + (255,))


def flask_icons():
    """A steel-cased flask with its rubber stopper in (filled) or out (empty, showing the silvered mouth); unfinished, the bare silvered glass."""
    item = TEXTURES / "item"
    for name, stoppered in (("filled_vacuum_flask", True), ("vacuum_flask", False)):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        cylinder(img, 4, 11, 4, 14, STEEL)
        cylinder(img, 5, 10, 3, 3, STEEL, base=False)
        for x in range(5, 11):
            for y in (5, 12):
                r, g, b, a = img.getpixel((x, y))
                img.putpixel((x, y), (r * 3 // 4, g * 3 // 4, b * 3 // 4, a))
        if stoppered:
            cylinder(img, 6, 9, 1, 2, RUBBER, base=False)
            img.putpixel((7, 1), (92, 90, 96, 255))
        else:
            # the open mouth: the silvered lip of the inner wall round the dark bore
            for x in range(5, 11):
                img.putpixel((x, 3), MIRROR + (255,) if x in (5, 6, 9, 10) else (24, 24, 28, 255))
        img.save(item / f"{name}.png")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cylinder(img, 5, 10, 5, 14, MIRROR, sheen=True)
    cylinder(img, 6, 9, 2, 4, MIRROR, base=False)
    for x in range(6, 10):
        img.putpixel((x, 2), (200, 224, 228, 150))
    img.save(item / "unfinished_vacuum_flask.png")


def main():
    recipes()
    flask()
    deer_hide()
    insulators()
    models_and_lang()
    (TEXTURES / "models/armor").mkdir(parents=True, exist_ok=True)
    armour_layers()
    armour_icons()
    material_icons()
    flask_icons()
    print(f"{len(TIERS)} parkas, {len(MATERIALS)} materials; set insulation " + ", ".join(f"{t} {s['cold']}" for t, s in TIERS.items()))


if __name__ == "__main__":
    main()
