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
tex("fluorescent_tube_off", lambda x, y: jitter((206, 210, 210) if 4 <= x <= 11 else (168, 172, 172), 3))
tex("fluorescent_tube_on", lambda x, y: (255, 255, 250) if 4 <= x <= 11 else (232, 240, 246))
# the troffer face: a parabolic aluminium louvre in a 4 by 4 grid over the tubes


def louvre(lit):
    def fn(x, y):
        if x in (0, 15) or y in (0, 15):
            return (214, 214, 208)
        if x % 5 == 0 or y % 5 == 0:
            return (196, 200, 206) if lit else (150, 154, 160)
        depth = min(x % 5, 5 - x % 5, y % 5, 5 - y % 5)
        return (255, 255, 248) if lit else (92 + 18 * depth, 96 + 18 * depth, 100 + 18 * depth)
    return fn


tex("troffer_off", louvre(False))
tex("troffer_on", louvre(True))
# the cobra head: cast aluminium, grey and grainy
tex("sodium_lamp_housing", lambda x, y: jitter((132, 136, 140) if 0 < x < 15 and 0 < y < 15 else (100, 104, 108), 8))
# the refractor bowl: dark glass cold, the pink of the first glow while the sodium boils, the sodium's orange once it has
tex("sodium_lens_off", lambda x, y: jitter((70, 74, 78) if (x + y) % 4 else (96, 100, 104), 4))
tex("sodium_lens_warm", lambda x, y: jitter((214, 104, 128) if (x + y) % 4 else (236, 140, 160), 4))
tex("sodium_lens_on", lambda x, y: jitter((255, 168, 52) if (x + y) % 4 else (255, 204, 120), 4))


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


for lit in ("off", "on"):
    tube = {"housing": "fundamentals:block/lamp_housing", "tube": f"fundamentals:block/fluorescent_tube_{lit}"}
    # the batten on a floor, tube running north to south; a second model runs it east to west, for a wall
    model(f"fluorescent_light_{lit}", [box([6, 0, 0], [10, 2, 16], "#housing"), box([6.5, 2, 1], [9.5, 3, 15], "#tube"),
                                         box([6, 0, 0], [10, 3.5, 1], "#housing"), box([6, 0, 15], [10, 3.5, 16], "#housing")], tube)
    model(f"fluorescent_light_wall_{lit}", [box([0, 0, 6], [16, 2, 10], "#housing"), box([1, 2, 6.5], [15, 3, 9.5], "#tube"),
                                              box([0, 0, 6], [1, 3.5, 10], "#housing"), box([15, 0, 6], [16, 3.5, 10], "#housing")], tube)
    model(f"fluorescent_troffer_{lit}", [box([0, 0, 0], [16, 3, 16], "#housing", "#face")],
          {"housing": "fundamentals:block/lamp_housing", "face": f"fundamentals:block/troffer_{lit}"})
for lens in ("off", "warm", "on"):
    model(f"sodium_lamp_{lens}", [box([3, 0, 2], [13, 4, 14], "#housing"), box([4, 4, 3], [12, 5, 13], "#lens"), box([7, 0, 14], [9, 2, 16], "#housing")],
          {"housing": "fundamentals:block/sodium_lamp_housing", "lens": f"fundamentals:block/sodium_lens_{lens}"})

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

for name, block in (("fluorescent_light", "fluorescent_light_on"), ("fluorescent_troffer", "fluorescent_troffer_on"), ("sodium_lamp", "sodium_lamp_on")):
    write(Path(A + f"models/item/{name}.json"), {"parent": f"fundamentals:block/{block}"})
    write(Path(D + f"loot_table/blocks/{name}.json"), {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
          "conditions": [{"condition": "minecraft:survives_explosion"}], "entries": [{"type": "minecraft:item", "name": f"fundamentals:{name}"}]}],
          "random_sequence": f"fundamentals:blocks/{name}"})
for name in ("fluorescent_tube", "ballast", "sodium_arc_tube"):
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
lang = read_lang()
lang.update({
    "block.fundamentals.fluorescent_light": "Fluorescent Light",
    "block.fundamentals.fluorescent_troffer": "Fluorescent Troffer",
    "block.fundamentals.sodium_lamp": "Sodium Street Lamp",
    "item.fundamentals.fluorescent_tube": "Fluorescent Tube",
    "item.fundamentals.ballast": "Ballast",
    "item.fundamentals.sodium_arc_tube": "Sodium Arc Tube",
    "goggles.fundamentals.readout.status.striking": "striking",
    "goggles.fundamentals.readout.status.warming": "warming up",
    "goggles.fundamentals.readout.status.lit": "lit",
    "goggles.fundamentals.readout.status.cooling": "cooling before it can restrike",
})
write(LANG, lang)
print("lamps painted")
