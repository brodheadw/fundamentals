#!/usr/bin/env python3
"""The satellite and ground station of 0.29: the satellite's sprite, the station's textures and its model."""
import json
import math

from PIL import Image
from common import TEXTURES, ASSETS
from paint_weather import box, face, paint, write
import paint_weather

PALETTE = {
    ".": None,
    "o": (28, 28, 34),
    # solar cells: deep blue silicon, a lighter sheen, silver bus lines and frame
    "n": (24, 42, 96), "N": (38, 66, 140), "g": (88, 124, 196), "s": (196, 202, 210), "S": (132, 138, 150),
    # gold multilayer insulation, crinkled
    "B": (246, 212, 112), "b": (214, 164, 58), "d": (150, 104, 34), "Y": (255, 240, 176),
    # white-painted dish and boom, the lens looking down
    "W": (244, 244, 238), "w": (200, 202, 200), "k": (82, 86, 94),
    "G": (186, 226, 242), "L": (96, 150, 186), "l": (40, 70, 98),
}
paint_weather.PALETTE.update(PALETTE)

SATELLITE = [
    # a Landsat-like Earth-observation satellite seen from the side: two solar wings on struts, the body wrapped in gold foil,
    # the high-gain dish on its mast and the camera's lens looking down
    "....oo....oo....",
    ".....oWWWwo.....",
    "oooo..owwo..oooo",
    "ogNo..oSko..oNno",
    "oNno.oooooo.oNgo",
    "osso.oYBBbo.osso",
    "oNgo.oBdBbo.oNno",
    "oNnoSoBYbdoSogNo",
    "osso.obBYbo.osso",
    "ogNo.oBbBdo.oNno",
    "oNno.obdbdo.oNgo",
    "osso.okSSko.osso",
    "oooo.oooooo.oooo",
    "......oGLo......",
    "......oLlo......",
    ".......oo.......",
]


# the console as seen from in front, each element's face taking the part of it where it stands: the monitor's screen above, the
# desk's lamps, switches and meter below
CONSOLE = [
    "................",
    "................",
    "................",
    "...oooooooooo...",
    "...oKnnfgggKo...",
    "...oKnGgGfgKo...",
    "...oKnnfgggKo...",
    "...oKnGfGggKo...",
    "...oKnnnffgKo...",
    "...oooooooooo...",
    ".hhhhhhhhhhhhhh.",
    ".pRpApEpRpApEpp.",
    ".pvpvpvpvpqqqpp.",
    ".pxpxpxpxpqrqpp.",
    ".tttttttttttttt.",
    "................",
]

# the cabinet's enamelled steel: a seam round the panel, a louvred vent and four screws
SIDE = [
    "mPPPPPPPPPPPPPPm",
    "PcPPPPPPPPPPPPcP",
    "PPPPPPPPPPPPPPPP",
    "PPPPPPPPPPPPPPPP",
    "PPPmmmmmmmmmmPPP",
    "PPPzzzzzzzzzzPPP",
    "PPPmmmmmmmmmmPPP",
    "PPPzzzzzzzzzzPPP",
    "PPPmmmmmmmmmmPPP",
    "PPPzzzzzzzzzzPPP",
    "PPPPPPPPPPPPPPPP",
    "PPPPPPPPPPPPPPPP",
    "PPPPPPPPPPPPPPPP",
    "PPPPPPPPPPPPPPPP",
    "PcPPPPPPPPPPPPcP",
    "mPPPPPPPPPPPPPPm",
]

PALETTE.update({
    "K": (52, 56, 60), "n": (10, 34, 20), "f": (96, 200, 110), "g": (44, 124, 60), "G": (170, 255, 170),
    "h": (196, 200, 192), "p": (150, 160, 158), "t": (86, 92, 92),
    "R": (226, 58, 46), "A": (240, 170, 44), "E": (92, 214, 96), "v": (226, 226, 222), "x": (70, 74, 78),
    "q": (40, 42, 46), "r": (236, 232, 214),
    "P": (118, 132, 140), "m": (84, 96, 104), "z": (46, 54, 60), "c": (168, 178, 184),
})
paint_weather.PALETTE.update(PALETTE)


def dish_texture():
    """The dish face on, centred where the model's dish is: white aluminium panels, a seam round each ring and radial seams."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - (16 - DISH_Y)
            r = math.hypot(dx, dy)
            shade = 238 - int(r * 3)
            seam = abs(r - 2) < 0.45 or abs(r - 4) < 0.45 or (r > 2 and (abs(dx) < 0.5 or abs(dy) < 0.5))
            v = shade - 34 if seam else shade - (6 if (x + y) % 3 == 0 else 0)
            img.putpixel((x, y), (v, v + 2, v - 4, 255))
    return img


DISH_Y = 10
# the dish's axis lifted 45° above the horizon behind the console, about its mount
TILT = {"angle": -45, "axis": "x", "origin": [8, DISH_Y, 12]}


def plate(lo, hi):
    """One piece of the dish: its face white, its back grey steel."""
    faces = {side: face("#dish") for side in ("south", "east", "west", "up", "down")}
    faces["north"] = face("#side")
    return {"from": lo, "to": hi, "rotation": TILT, "faces": faces}


def ring(outer, inner, z):
    """A ring in the dish's plane round its axis, built a row of pixels at a time; inner 0 is a disc."""
    def half(r, dy):
        return math.floor(math.sqrt(max(0.0, r * r - dy * dy)) + 0.25)
    rows = []
    for y in range(-outer, outer):
        dy = y + 0.5
        o, i = half(outer, dy), half(inner, dy) if abs(dy) < inner else 0
        if rows and rows[-1][1:] == (o, i):
            rows[-1][0][1] = y + 1
        else:
            rows.append(([y, y + 1], o, i))
    parts = []
    for (y0, y1), o, i in rows:
        for x0, x1 in ([(-o, o)] if i == 0 else [(-o, -i), (i, o)]):
            parts.append(plate([8 + x0, DISH_Y + y0, z], [8 + x1, DISH_Y + y1, z + 1]))
    return parts


def station():
    """A console with its monitor facing north, and behind it a dish on a pedestal looking up and away to the south."""
    def part(lo, hi, front="#side"):
        return {"from": lo, "to": hi, "faces": {s: face(front if s == "north" else "#side") for s in ("north", "south", "east", "west", "up", "down")}}
    tilted = lambda lo, hi: {**box(lo, hi, "#feed"), "rotation": TILT}
    return ([box([0, 0, 0], [16, 1, 16], "#base"),
             part([1, 1, 1], [15, 6, 6], "#console"), part([3, 6, 3], [13, 13, 6], "#console"),
             part([6, 1, 10], [10, 5, 14]), part([7, 5, 11], [9, 9, 13])]
            + ring(2, 0, 11) + ring(4, 2, 12) + ring(6, 4, 13)
            + [tilted([7.5, DISH_Y - 0.5, 12], [8.5, DISH_Y + 0.5, 16.5]), tilted([7, DISH_Y - 1, 16.5], [9, DISH_Y + 1, 18])])


TEXTURES_STATION = {"console": "fundamentals:block/ground_station_console", "side": "fundamentals:block/ground_station_side",
                    "dish": "fundamentals:block/ground_station_dish", "feed": "minecraft:block/iron_block",
                    "base": "create:block/andesite_casing", "particle": "fundamentals:block/ground_station_side"}


def main():
    paint(SATELLITE).save(TEXTURES / "item/satellite.png")
    write(ASSETS / "models/item/satellite.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "fundamentals:item/satellite"}})
    blocks = TEXTURES / "block"
    paint(CONSOLE).save(blocks / "ground_station_console.png")
    paint(SIDE).save(blocks / "ground_station_side.png")
    dish_texture().save(blocks / "ground_station_dish.png")
    model = {"parent": "minecraft:block/block", "textures": TEXTURES_STATION, "elements": station()}
    write(ASSETS / "models/block/ground_station.json", model)
    write(ASSETS / "models/item/ground_station.json", model)
    write(ASSETS / "blockstates/ground_station.json", {"variants": {
        f"facing={facing}": {"model": "fundamentals:block/ground_station", **({"y": y} if y else {})}
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270), ("up", 0), ("down", 0))}})
    print("painted the satellite and the ground station")


if __name__ == "__main__":
    main()
