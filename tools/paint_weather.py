#!/usr/bin/env python3
"""The weather and survey instruments of 0.28: item sprites painted pixel by pixel, and the weather radar's models."""
import json
import math

from PIL import Image
from common import TEXTURES, ASSETS

ITEMS = TEXTURES / "item"

PALETTE = {
    ".": None,
    "o": (44, 36, 30), "k": (30, 30, 34),
    "B": (238, 200, 104), "b": (202, 156, 66), "d": (142, 102, 42),
    "W": (242, 236, 214), "w": (206, 198, 176), "r": (204, 44, 34),
    "s": (210, 216, 222), "S": (150, 156, 166), "t": (92, 96, 106),
    "c": (226, 140, 84), "C": (168, 90, 50),
    "K": (70, 72, 78), "m": (50, 52, 58), "M": (104, 106, 114), "h": (140, 142, 150),
    "g": (120, 168, 196), "G": (196, 230, 244), "n": (60, 92, 116),
    "u": (150, 98, 56), "U": (106, 66, 38),
    "L": (236, 226, 204), "H": (255, 253, 244), "l": (200, 184, 152), "e": (160, 138, 104), "q": (110, 90, 66),
    "x": (220, 64, 40), "y": (180, 180, 176),
}

SPRITES = {
    # a fluxgate magnetometer: a brass-cased meter, its needle off the zero, and the sensor tube with its two wound cores
    "magnetometer": [
        "...........ooo..",
        "..........osSto.",
        "..........ocCCo.",
        "..........oCCCo.",
        "..........osSto.",
        "..........ocCCo.",
        "oooooooooooCCCo.",
        "oBBBBBBBBbosSto.",
        "oBoooooobdosSto.",
        "oBoWwWwWodosSto.",
        "oBoWWWrWodosSto.",
        "oBoWWrWWodosSto.",
        "oBowkwwwodoosto.",
        "oBbooooobdoBbo..",
        "obdddddddddddo..",
        "oooooooooooooo..",
    ],
    # a K-24 aerial camera: the film magazine on top, the body between its two grips, the lens looking down
    "aerial_camera": [
        "................",
        "....oooooooo....",
        "...ohMMMMMMMmo..",
        "...oMKKKKKKKmo..",
        "..ooooooooooooo.",
        "ouoKhMMMMMMMKmou",
        "ouoMKKKKKKKKKmoU",
        "ouoMKBBBBBBbKmoU",
        "ouoMKKKKKKKKKmoU",
        "oUoKmmmmmmmmmmoU",
        "ooooooBBBBboooo.",
        ".....ombbbdo....",
        ".....oGgggno....",
        ".....oggggno....",
        "......onnno.....",
        ".......ooo......",
    ],
    # a weather balloon: pale latex filled with hydrogen, tied off, and the radiosonde on its line
    "weather_balloon": [
        ".....oooooo.....",
        "...ooHHLLLLoo...",
        "..oHHHLLLLLLlo..",
        ".oLHHLLLLLLLLlo.",
        ".oLHLLLLLLLLLlo.",
        ".oLLLLLLLLLLLlo.",
        ".oLLLLLLLLLLleo.",
        "..oLLLLLLLLleo..",
        "...oLLLLLlleo...",
        "....oelllleo....",
        "......oeqo......",
        "........y.......",
        "........y.......",
        "......ooooo.....",
        "......oWWxo.....",
        "......ooooo.....",
    ],
}


def paint(rows):
    img = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            colour = PALETTE[ch]
            if colour:
                img.putpixel((x, y), colour + (255,))
    return img


def face(texture, uv=None):
    return {"texture": texture, **({"uv": uv} if uv else {})}


def box(lo, hi, texture, rotation=None):
    faces = {side: face(texture) for side in ("north", "south", "east", "west", "up", "down")}
    return {"from": lo, "to": hi, **({"rotation": rotation} if rotation else {}), "faces": faces}


# the dish's axis lifted 22.5° above the horizon, about its trunnions
TILT = {"angle": 22.5, "axis": "x", "origin": [8, 12, 10]}


def ring(outer, inner, z, texture="#dish"):
    """A round ring in the dish's plane about its axis at (8, 12), built a row of pixels at a time; inner 0 is a disc."""
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
        spans = [(-o, o)] if i == 0 else [(-o, -i), (i, o)]
        for x0, x1 in spans:
            parts.append(box([8 + x0, 12 + y0, z], [8 + x1, 12 + y1, z + 1], texture, TILT))
    return parts


def dish():
    """What turns: the turntable, the yoke, and a round dish facing north, stepping forward from its centre to the rim, with
    the feed horn held out at its focus."""
    return ([box([5, 5, 5], [11, 6, 11], "#feed"), box([1, 6, 8.5], [15, 7, 11.5], "#mount"),
             box([1, 7, 9], [2, 13, 11], "#mount"), box([14, 7, 9], [15, 13, 11], "#mount")]
            + ring(2, 0, 12) + ring(4, 2, 11) + ring(6, 4, 10) + ring(1, 0, 13, "#feed")
            + [box([7.5, 11.5, 5], [8.5, 12.5, 12], "#feed", TILT), box([7, 11, 3.5], [9, 13, 5], "#feed", TILT)])


def base():
    """What stays: a casing plinth and the pedestal the dish turns on."""
    return [box([2, 0, 2], [14, 2, 14], "#base"), box([6, 2, 6], [10, 5, 10], "#mount")]


TEXTURES_RADAR = {"dish": "minecraft:block/white_concrete", "mount": "minecraft:block/light_gray_concrete",
                  "feed": "minecraft:block/iron_block", "base": "create:block/andesite_casing", "particle": "minecraft:block/white_concrete"}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def main():
    for name, rows in SPRITES.items():
        paint(rows).save(ITEMS / f"{name}.png")
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})
    models = ASSETS / "models/block"
    write(models / "weather_radar.json", {"parent": "minecraft:block/block", "textures": TEXTURES_RADAR, "elements": base()})
    write(models / "weather_radar_dish.json", {"textures": TEXTURES_RADAR, "elements": dish()})
    write(ASSETS / "models/item/weather_radar.json", {"parent": "minecraft:block/block", "textures": TEXTURES_RADAR, "elements": base() + dish()})
    write(ASSETS / "blockstates/weather_radar.json", {"variants": {
        f"facing={facing}": {"model": "fundamentals:block/weather_radar"} for facing in ("north", "east", "south", "west", "up", "down")}})
    print(f"painted {len(SPRITES)} instruments and the weather radar's models")


if __name__ == "__main__":
    main()
