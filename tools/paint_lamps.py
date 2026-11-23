#!/usr/bin/env python3
"""The electric lamps: fluorescent batten and troffer, sodium street lamp, and their parts. Textures, models, blockstates,
loot, recipes. Run from the repo root: python3 tools/paint_lamps.py"""
import random
from pathlib import Path

from PIL import Image
from common import ASSETS, DATA, LANG, TEXTURES, read_lang, write

A, D = str(ASSETS) + "/", str(DATA) + "/"
rng = random.Random(11)


def tex(name, fn):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), tuple(max(0, min(255, int(c))) for c in fn(x, y)) + (255,))
    img.save(TEXTURES / "block" / f"{name}.png")


def jitter(c, k=6):
    d = rng.randint(-k, k)
    return tuple(v + d for v in c)


# white stove-enamelled steel, a seam down each edge
tex("lamp_housing", lambda x, y: jitter((214, 214, 208) if 0 < x < 15 and 0 < y < 15 else (176, 176, 170), 4))
# the tube: lengthwise it is all one, across it the phosphor is brightest down the middle
# the batten's prismatic diffuser over the tube: white plastic, so whatever it is dyed is the light's colour
tex("fluorescent_tube_off", lambda x, y: jitter((206, 210, 210) if 4 <= x <= 11 else (168, 172, 172), 3) if y % 3 else jitter((186, 190, 190), 3))
tex("fluorescent_tube_on", lambda x, y: ((255, 255, 255) if 4 <= x <= 11 else (236, 240, 244)) if y % 3 else (244, 246, 248))
# the troffer face: a prismatic plastic panel, a fine grid of little pyramids catching the tubes behind it


def prisms(lit):
    def fn(x, y):
        if x in (0, 15) or y in (0, 15):
            return (226, 226, 222)
        facet = (x + y) % 2
        return ((255, 255, 255) if facet else (238, 240, 244)) if lit else ((188, 190, 194) if facet else (164, 166, 172))
    return fn


tex("troffer_off", prisms(False))
tex("troffer_on", prisms(True))
# the cobra head: cast aluminium, grey and grainy
tex("sodium_lamp_housing", lambda x, y: jitter((132, 136, 140) if 0 < x < 15 and 0 < y < 15 else (100, 104, 108), 8))
# the refractor bowl: dark glass cold, the pink of the first glow while the sodium boils, the sodium's orange once it has
tex("sodium_lens_off", lambda x, y: jitter((70, 74, 78) if (x + y) % 4 else (96, 100, 104), 4))
tex("sodium_lens_warm", lambda x, y: jitter((214, 104, 128) if (x + y) % 4 else (236, 140, 160), 4))
tex("sodium_lens_on", lambda x, y: jitter((255, 168, 52) if (x + y) % 4 else (255, 204, 120), 4))
# the blacklight's tube is Wood's glass, near black cold and a deep violet lit
tex("blacklight_tube_off", lambda x, y: jitter((44, 34, 74) if 4 <= x <= 11 else (30, 24, 52), 3))
tex("blacklight_tube_on", lambda x, y: (196, 120, 255) if 6 <= x <= 9 else (140, 70, 230) if 4 <= x <= 11 else (90, 40, 170))
# mercury vapour: an older grey-green head; the bowl faint blue warming, then a cold blue-white
tex("mercury_lamp_housing", lambda x, y: jitter((112, 126, 116) if 0 < x < 15 and 0 < y < 15 else (82, 94, 86), 8))
tex("mercury_lens_off", lambda x, y: jitter((74, 80, 84) if (x + y) % 4 else (100, 106, 110), 4))
tex("mercury_lens_warm", lambda x, y: jitter((78, 140, 168) if (x + y) % 4 else (110, 170, 196), 4))
tex("mercury_lens_on", lambda x, y: jitter((214, 230, 255) if (x + y) % 4 else (240, 248, 255), 4))
# low-pressure sodium: a long flat lantern; through its clear cover, the U-tube red with neon at first, then the one yellow
tex("low_sodium_housing", lambda x, y: jitter((70, 72, 76) if 0 < x < 15 and 0 < y < 15 else (50, 52, 56), 6))
tex("low_sodium_lens_off", lambda x, y: jitter((150, 140, 110) if x in (4, 5, 10, 11) and 2 < y < 14 or 4 <= x <= 11 and y in (12, 13) else (96, 98, 100), 4))
tex("low_sodium_lens_warm", lambda x, y: jitter((255, 70, 40) if x in (4, 5, 10, 11) and 2 < y < 14 or 4 <= x <= 11 and y in (12, 13) else (130, 60, 50), 4))
tex("low_sodium_lens_on", lambda x, y: jitter((255, 200, 30) if x in (4, 5, 10, 11) and 2 < y < 14 or 4 <= x <= 11 and y in (12, 13) else (210, 150, 30), 4))
# the bulb: clear glass round a coiled filament, a brass screw cap, a black flex
tex("bulb_glass_off", lambda x, y: (150, 120, 90) if 6 <= x <= 9 and y == 8 or x in (6, 9) and 8 < y < 12 else jitter((214, 220, 222), 3))
tex("bulb_glass_on", lambda x, y: (255, 255, 230) if 6 <= x <= 9 and 7 <= y <= 12 else (255, 228, 170))
tex("bulb_cap", lambda x, y: jitter((176, 146, 70) if y % 3 else (130, 104, 46), 4))
tex("bulb_flex", lambda x, y: jitter((28, 28, 30), 3))


def item_icon(name, fn):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            c = fn(x, y)
            if c:
                img.putpixel((x, y), c + (255,))
    img.save(TEXTURES / "item" / f"{name}.png")


# a straight tube corner to corner, metal pins at each end
item_icon("fluorescent_tube", lambda x, y: (150, 150, 150) if abs(x - (15 - y)) <= 1 and (x < 2 or x > 13)
          else (236, 238, 238) if abs(x - (15 - y)) <= 1 else None)
# the ballast: a steel can with leads out of one end
item_icon("ballast", lambda x, y: (60, 62, 70) if 3 <= x <= 12 and 5 <= y <= 12 and (x in (3, 12) or y in (5, 12))
          else (96, 98, 108) if 3 <= x <= 12 and 5 <= y <= 12 else (190, 40, 30) if x == 6 and y < 5 and y > 1
          else (230, 230, 230) if x == 9 and y < 5 and y > 1 else None)
# the arc tube: a translucent white alumina tube, niobium end caps
item_icon("sodium_arc_tube", lambda x, y: (120, 130, 140) if 6 <= x <= 9 and (y < 3 or y > 12)
          else (240, 236, 224) if 6 <= x <= 9 else None)
item_icon("blacklight_tube", lambda x, y: (150, 150, 150) if abs(x - (15 - y)) <= 1 and (x < 2 or x > 13)
          else (58, 40, 104) if abs(x - (15 - y)) <= 1 else None)
# quartz, short and fat, a bead of mercury in it
item_icon("mercury_arc_tube", lambda x, y: (120, 130, 140) if 5 <= x <= 10 and (y < 4 or y > 11)
          else (200, 204, 210) if 6 <= x <= 9 and y == 9 else (232, 240, 244) if 5 <= x <= 10 else None)
# the U of borate glass, with a bead of sodium in each dimple
item_icon("low_pressure_sodium_tube", lambda x, y: (120, 130, 140) if y < 2 and x in (4, 5, 10, 11)
          else (236, 226, 190) if (x in (4, 5, 10, 11) and 2 <= y <= 13) or (4 <= x <= 11 and y in (12, 13)) else None)


def uv(d, frm, to):
    """The uv the game would take from the element's bounds, written out."""
    (x0, y0, z0), (x1, y1, z1) = frm, to
    return {"up": [x0, z0, x1, z1], "down": [x0, z0, x1, z1], "north": [x0, 16 - y1, x1, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
            "east": [z0, 16 - y1, z1, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0]}[d]


def el(frm, to, faces):
    return {"from": frm, "to": to, "faces": {d: {"uv": uv(d, frm, to), "texture": t} for d, t in faces.items()}}


ALL = ("north", "south", "east", "west", "up", "down")


def box(frm, to, texture, top=None):
    return el(frm, to, {d: (top if d == "up" and top else texture) for d in ALL})


def model(name, elements, textures):
    write(Path(A + f"models/block/{name}.json"), {"parent": "block/block", "textures": dict(textures, particle=textures["housing"]), "elements": elements})


def tinted(element):
    """The diffuser's faces take the plastic's colour."""
    for face in element["faces"].values():
        face["tintindex"] = 0
    return element


for lit in ("off", "on"):
    tube = {"housing": "fundamentals:block/lamp_housing", "tube": f"fundamentals:block/fluorescent_tube_{lit}"}
    # the batten on a floor, tube running north to south; a second model runs it east to west, for a wall
    for name, tube, mark in ((f"fluorescent_light", f"fluorescent_tube_{lit}", tinted), (f"blacklight", f"blacklight_tube_{lit}", lambda e: e)):
        t = {"housing": "fundamentals:block/lamp_housing", "tube": f"fundamentals:block/{tube}"}
        model(f"{name}_{lit}", [box([6, 0, 0], [10, 2, 16], "#housing"), mark(box([6.5, 2, 1], [9.5, 3, 15], "#tube")),
                                box([6, 0, 0], [10, 3.5, 1], "#housing"), box([6, 0, 15], [10, 3.5, 16], "#housing")], t)
        model(f"{name}_wall_{lit}", [box([0, 0, 6], [16, 2, 10], "#housing"), mark(box([1, 2, 6.5], [15, 3, 9.5], "#tube")),
                                     box([0, 0, 6], [1, 3.5, 10], "#housing"), box([15, 0, 6], [16, 3.5, 10], "#housing")], t)
    model(f"fluorescent_troffer_{lit}", [box([0, 0, 0], [16, 2.5, 16], "#housing"), tinted(box([1, 2.5, 1], [15, 3, 15], "#face"))],
          {"housing": "fundamentals:block/lamp_housing", "face": f"fundamentals:block/troffer_{lit}"})
    model(f"light_bulb_{lit}", [box([7.5, 0, 7.5], [8.5, 6, 8.5], "#flex"), box([6.5, 6, 6.5], [9.5, 8, 9.5], "#cap"),
                                box([5.5, 8, 5.5], [10.5, 12, 10.5], "#housing"), box([6.5, 12, 6.5], [9.5, 13, 9.5], "#housing")],
          {"housing": f"fundamentals:block/bulb_glass_{lit}", "cap": "fundamentals:block/bulb_cap", "flex": "fundamentals:block/bulb_flex"})
for lens in ("off", "warm", "on"):
    for name, metal in (("sodium_lamp", "sodium"), ("mercury_vapour_lamp", "mercury")):
        model(f"{name}_{lens}", [box([3, 0, 2], [13, 4, 14], "#housing"), box([4, 4, 3], [12, 5, 13], "#lens"), box([7, 0, 14], [9, 2, 16], "#housing")],
              {"housing": f"fundamentals:block/{metal}_lamp_housing", "lens": f"fundamentals:block/{metal}_lens_{lens}"})
    model(f"low_pressure_sodium_lamp_{lens}", [box([2, 0, 1], [14, 3, 15], "#housing"), box([3, 3, 2], [13, 4, 14], "#lens"), box([7, 0, 15], [9, 2, 16], "#housing")],
          {"housing": "fundamentals:block/low_sodium_housing", "lens": f"fundamentals:block/low_sodium_lens_{lens}"})

ROT = {("ceiling", "east"): (180, 270), ("ceiling", "north"): (180, 180), ("ceiling", "south"): (180, 0), ("ceiling", "west"): (180, 90),
       ("floor", "east"): (0, 90), ("floor", "north"): (0, 0), ("floor", "south"): (0, 180), ("floor", "west"): (0, 270),
       ("wall", "east"): (90, 90), ("wall", "north"): (90, 0), ("wall", "south"): (90, 180), ("wall", "west"): (90, 270)}


def blockstate(name, pick):
    variants = {}
    for (face, facing), (x, y) in ROT.items():
        for level in range(16):
            v = {"model": f"fundamentals:block/{pick(face, level)}"}
            if x:
                v["x"] = x
            if y:
                v["y"] = y
            variants[f"face={face},facing={facing},level={level}"] = v
    write(Path(A + f"blockstates/{name}.json"), {"variants": variants})


blockstate("fluorescent_light", lambda face, lv: f"fluorescent_light{'_wall' if face == 'wall' else ''}_{'on' if lv else 'off'}")
blockstate("fluorescent_troffer", lambda face, lv: f"fluorescent_troffer_{'on' if lv else 'off'}")
blockstate("sodium_lamp", lambda face, lv: f"sodium_lamp_{'off' if lv == 0 else 'warm' if lv <= 8 else 'on'}")
blockstate("blacklight", lambda face, lv: f"blacklight{'_wall' if face == 'wall' else ''}_{'on' if lv else 'off'}")
blockstate("mercury_vapour_lamp", lambda face, lv: f"mercury_vapour_lamp_{'off' if lv == 0 else 'warm' if lv <= 7 else 'on'}")
blockstate("low_pressure_sodium_lamp", lambda face, lv: f"low_pressure_sodium_lamp_{'off' if lv == 0 else 'warm' if lv <= 7 else 'on'}")
blockstate("light_bulb", lambda face, lv: f"light_bulb_{'on' if lv else 'off'}")

LAMPS = ("fluorescent_light", "fluorescent_troffer", "sodium_lamp", "blacklight", "mercury_vapour_lamp", "low_pressure_sodium_lamp", "light_bulb")
for name in LAMPS:
    block = name + "_on"
    write(Path(A + f"models/item/{name}.json"), {"parent": f"fundamentals:block/{block}"})
    write(Path(D + f"loot_table/blocks/{name}.json"), {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
          "conditions": [{"condition": "minecraft:survives_explosion"}], "entries": [{"type": "minecraft:item", "name": f"fundamentals:{name}"}]}],
          "random_sequence": f"fundamentals:blocks/{name}"})
for name in ("fluorescent_tube", "ballast", "sodium_arc_tube", "blacklight_tube", "mercury_arc_tube", "low_pressure_sodium_tube"):
    write(Path(A + f"models/item/{name}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})


def shaped(name, pattern, key, result, count=1):
    write(Path(D + f"recipe/{name}.json"), {"type": "minecraft:crafting_shaped", "category": "redstone", "pattern": pattern,
          "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in key.items()}, "result": {"id": f"fundamentals:{result}", "count": count}})


# a glass tube coated inside with the triphosphor, a coiled tungsten electrode sealed in each end, a drop of mercury, and argon
shaped("fluorescent_tube", ["GPG", "TMT", " A "], {"G": "#c:glass_panes/colorless", "P": "fundamentals:phosphor", "T": "fundamentals:tungsten_filament",
                                                    "M": "fundamentals:mercury", "A": "fundamentals:argon_canister"}, "fluorescent_tube", 2)
# a magnetic ballast: a copper winding on a laminated iron core, the choke that limits the arc and kicks it alight
shaped("ballast", ["WIW", "WIW"], {"W": "#c:wires/copper", "I": "#c:plates/iron"}, "ballast")
# high-pressure sodium: a translucent alumina tube, sodium amalgamated with mercury, a tungsten electrode in each end
shaped("sodium_arc_tube", ["TAT", "SMS"], {"T": "fundamentals:tungsten_filament", "A": "fundamentals:alumina", "S": "fundamentals:sodium_ingot",
                                            "M": "fundamentals:mercury"}, "sodium_arc_tube")
shaped("fluorescent_light", ["IBI", " F "], {"I": "#c:plates/iron", "B": "fundamentals:ballast", "F": "fundamentals:fluorescent_tube"}, "fluorescent_light")
shaped("fluorescent_troffer", ["IBI", "FPF"], {"I": "#c:plates/iron", "B": "fundamentals:ballast", "F": "fundamentals:fluorescent_tube",
                                               "P": "tfmg:plastic_sheet"}, "fluorescent_troffer")
shaped("sodium_lamp", ["AAA", "BTG"], {"A": "#c:plates/aluminum", "B": "fundamentals:ballast", "T": "fundamentals:sodium_arc_tube",
                                         "G": "#c:glass_panes/colorless"}, "sodium_lamp")
# Wood's glass (nickel oxide in the melt) passes the near-ultraviolet and little else; a europium phosphor makes it
shaped("blacklight_tube", ["GEG", "TMT", "NAN"], {"G": "#c:glass_panes/colorless", "E": "fundamentals:europium_oxide", "T": "fundamentals:tungsten_filament",
                                                   "M": "fundamentals:mercury", "N": "fundamentals:nickel_oxide", "A": "fundamentals:argon_canister"}, "blacklight_tube", 2)
shaped("blacklight", ["IBI", " U "], {"I": "#c:plates/iron", "B": "fundamentals:ballast", "U": "fundamentals:blacklight_tube"}, "blacklight")
# mercury vapour: a fused quartz arc tube, mercury and a starting gas of argon
shaped("mercury_arc_tube", ["TQT", "MAM"], {"T": "fundamentals:tungsten_filament", "Q": "minecraft:quartz", "M": "fundamentals:mercury",
                                             "A": "fundamentals:argon_canister"}, "mercury_arc_tube")
shaped("mercury_vapour_lamp", ["AAA", "BTG"], {"A": "#c:plates/aluminum", "B": "fundamentals:ballast", "T": "fundamentals:mercury_arc_tube",
                                                 "G": "#c:glass_panes/colorless"}, "mercury_vapour_lamp")
# low-pressure sodium: a U of sodium-proof glass with sodium in it and a Penning gas to strike, in an evacuated jacket
shaped("low_pressure_sodium_tube", ["GSG", "TAT"], {"G": "#c:glass_panes/colorless", "S": "fundamentals:sodium_ingot", "T": "fundamentals:tungsten_filament",
                                                     "A": "fundamentals:argon_canister"}, "low_pressure_sodium_tube")
shaped("low_pressure_sodium_lamp", ["AAA", "BTB", " G "], {"A": "#c:plates/aluminum", "B": "fundamentals:ballast", "T": "fundamentals:low_pressure_sodium_tube",
                                                           "G": "#c:glass_panes/colorless"}, "low_pressure_sodium_lamp")
# a filament bulb: the coiled tungsten in argon under a glass envelope, on a copper lead
shaped("light_bulb", [" G ", "ATW"], {"G": "#c:glass_panes/colorless", "A": "fundamentals:argon_canister", "T": "fundamentals:tungsten_filament",
                                       "W": "#c:wires/copper"}, "light_bulb", 2)

# a fluorescent's diffuser moulded in coloured plastic: its light comes out that colour
DYES = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")
for lamp in ("fluorescent_light", "fluorescent_troffer"):
    for dye in DYES:
        write(Path(D + f"recipe/{dye}_{lamp}.json"), {"type": "minecraft:crafting_shapeless", "category": "redstone",
              "ingredients": [{"item": f"fundamentals:{lamp}"}, {"item": f"fundamentals:{dye}_plastic_block"}],
              "result": {"id": f"fundamentals:{lamp}", "count": 1, "components": {"minecraft:block_state": {"color": dye}}}})
    write(Path(D + f"recipe/clear_{lamp}.json"), {"type": "minecraft:crafting_shapeless", "category": "redstone",
          "ingredients": [{"item": f"fundamentals:{lamp}"}, {"item": "tfmg:plastic_sheet"}],
          "result": {"id": f"fundamentals:{lamp}", "count": 1, "components": {"minecraft:block_state": {"color": "none"}}}})

# Gleam's coloured light, where it is installed: the diffuser's colour, the arc's colour as it warms, the blacklight's UV
LIGHT = {"none": "#EEF4FF", "white": "#FFF6EA", "orange": "#FF8C1A", "magenta": "#FF33DD", "light_blue": "#5CB8FF", "yellow": "#FFE233",
         "lime": "#9CFF2E", "pink": "#FF8FC8", "gray": "#9A9A9A", "light_gray": "#D8D8D8", "cyan": "#22E6E6", "purple": "#9933FF",
         "blue": "#2E4DFF", "brown": "#A0622D", "green": "#2ECC40", "red": "#FF2020", "black": "#3A2020"}


def glow(file, emitters, colour, radius=8.0):
    props = {"encoding": "blacklight"} if colour == "blacklight" else {"encoding": "hex", "hex": colour}
    write(Path(A + f"gleam/light_providers/{file}.json"), {"emitters": emitters, "light_properties": dict(props, radius=radius, intensity=1.0)})


for lamp in ("fluorescent_light", "fluorescent_troffer"):
    for colour, hexa in LIGHT.items():
        glow(f"{lamp}_{colour}", [f"fundamentals:{lamp}[color={colour}]"], hexa)
for lamp, first, last, warm, lit in (("sodium_lamp", 2, 8, "#FF7FA0", "#FF9A2E"), ("mercury_vapour_lamp", 3, 7, "#70B8C8", "#C8DCFF"),
                                      ("low_pressure_sodium_lamp", 1, 7, "#FF3A20", "#FFB000")):
    glow(f"{lamp}_warming", [f"fundamentals:{lamp}[level={lv}]" for lv in range(first, last + 1)], warm, 5.0)
    glow(f"{lamp}_lit", [f"fundamentals:{lamp}[level={lv}]" for lv in range(last + 1, 16)], lit, 10.0)
glow("blacklight", ["fundamentals:blacklight"], "blacklight", 6.0)
glow("light_bulb", ["fundamentals:light_bulb"], "#FFC77A", 7.0)

lang = read_lang()
lang.update({
    "block.fundamentals.fluorescent_light": "Fluorescent Light",
    "block.fundamentals.fluorescent_troffer": "Fluorescent Troffer",
    "block.fundamentals.sodium_lamp": "Sodium Street Lamp",
    "item.fundamentals.fluorescent_tube": "Fluorescent Tube",
    "item.fundamentals.ballast": "Ballast",
    "item.fundamentals.sodium_arc_tube": "Sodium Arc Tube",
    "block.fundamentals.blacklight": "Blacklight",
    "block.fundamentals.mercury_vapour_lamp": "Mercury Vapour Lamp",
    "block.fundamentals.low_pressure_sodium_lamp": "Low-Pressure Sodium Lamp",
    "block.fundamentals.light_bulb": "Light Bulb",
    "item.fundamentals.blacklight_tube": "Blacklight Tube",
    "item.fundamentals.mercury_arc_tube": "Mercury Arc Tube",
    "item.fundamentals.low_pressure_sodium_tube": "Low-Pressure Sodium Tube",
    "goggles.fundamentals.readout.status.striking": "striking",
    "goggles.fundamentals.readout.status.warming": "warming up",
    "goggles.fundamentals.readout.status.lit": "lit",
    "goggles.fundamentals.readout.status.cooling": "cooling before it can restrike",
})
write(LANG, lang)
print("lamps painted")
