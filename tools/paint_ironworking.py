#!/usr/bin/env python3
import random

from PIL import Image

from paint_minerals import paint_raw
from common import TEXTURES

CLEAR = (0, 0, 0, 0)

CLAY = [(104, 62, 44), (124, 76, 54), (140, 88, 62), (154, 98, 70), (168, 110, 80)]
SOOT = [(28, 22, 20), (44, 34, 30), (62, 48, 42)]
FIRE = [(196, 72, 16), (236, 124, 24), (252, 184, 48), (255, 236, 132)]


S_DARK, S_LEFT, S_MID, S_LIGHT = (40, 30, 11), (73, 54, 21), (104, 78, 30), (137, 103, 39)
HARD, SOFT, DEEP, MID, FILL, HIGH = (24, 24, 24), (73, 73, 73), (108, 108, 108), (127, 127, 127), (137, 137, 137), (154, 154, 154)

KEY = {
    "o": HARD, "s": SOFT, "e": DEEP, "m": MID, "l": FILL, "h": HIGH,
    "0": S_DARK, "1": S_LEFT, "2": S_MID, "3": S_LIGHT,
}


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), tuple(c) + (255,))


def rows(img, lines, x0=0, y0=0):
    for dy, line in enumerate(lines):
        for dx, ch in enumerate(line):
            if ch != ".":
                put(img, x0 + dx, y0 + dy, KEY[ch])


def shovel_handle(img, top_row, light_rows=(13, 12, 10, 8, 7)):
    put(img, 3, 14, S_DARK)
    put(img, 4, 14, S_DARK)
    put(img, 2, 12, S_LEFT)
    for y in range(13, top_row - 1, -1):
        x = 15 - y
        put(img, x, y, S_LEFT)
        if y >= 7:
            mid = S_LIGHT if y in light_rows else S_MID
        else:
            mid = S_LIGHT if (7 - y) % 2 == 0 else S_MID
        put(img, x + 1, y, mid)
        put(img, x + 2, y, S_DARK)


def smithing_hammer():
    img = Image.new("RGBA", (16, 16), CLEAR)
    shovel_handle(img, top_row=8)
    rows(img, [
        ".sssssss.",
        "shhlllmmo",
        "shlllmmmo",
        "sllmmmmmo",
        "olmmmmmeo",
        ".ooo..oo.",
    ], 4, 2)
    rows(img, ["13"], 8, 7)
    return img


def mortar_and_pestle():
    img = Image.new("RGBA", (16, 16), CLEAR)
    rows(img, [
        "................",
        "................",
        "................",
        "................",
        ".....ssssss.....",
        "...slhhhhhhlo...",
        "..slooooooolmo..",
        "..slloooooolmo..",
        "..shlllllllmmo..",
        "..shllllllmmmo..",
        "...shlllllmmo...",
        "....osllmmso....",
        ".....oooooo.....",
        "................",
        "................",
        "................",
    ])
    rows(img, ["13320"], 5, 7)
    for i, y in enumerate(range(6, 0, -1)):
        x = 13 - y
        put(img, x, y, S_LEFT)
        put(img, x + 1, y, S_LIGHT if i % 2 == 0 else S_MID)
        if y > 1:
            put(img, x + 2, y, S_DARK)
    return img


def pestle_sprite():
    img = Image.new("RGBA", (16, 16), CLEAR)
    rows(img, ["13320", ".1320"], 2, 12)
    for i, y in enumerate(range(11, 2, -1)):
        x = 14 - y
        put(img, x, y, S_LEFT)
        put(img, x + 1, y, S_LIGHT if i % 2 == 0 else S_MID)
        if y > 3:
            put(img, x + 2, y, S_DARK)
    return img


def daub(rng, soot_rows=0):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(0, 16, 2):
            tone = rng.choice((1, 2, 2, 3, 3, 4))
            if y % 5 == 4:
                tone = max(0, tone - 2)
            for dx in (0, 1):
                t = tone if rng.random() < 0.8 else max(0, tone - 1)
                colour = CLAY[t]
                if y < soot_rows and rng.random() < 0.55 - y * 0.15:
                    colour = SOOT[rng.randrange(1, 3)]
                put(img, x + dx, y, colour)
    for x in range(16):
        put(img, x, 15, CLAY[0])
    return img


def front(rng, lit):
    img = daub(rng, soot_rows=3)
    arch = {y: (5, 10) for y in range(10, 15)}
    arch[9] = (6, 9)
    for y, (x0, x1) in arch.items():
        put(img, x0 - 1, y, CLAY[0])
        put(img, x1 + 1, y, CLAY[0])
        for x in range(x0, x1 + 1):
            if lit:
                heat = (y - 9) + (2 - abs(x - 7.5)) * 0.8 + rng.uniform(-0.8, 0.8)
                put(img, x, y, FIRE[max(0, min(3, int(heat / 1.8)))])
            else:
                put(img, x, y, SOOT[0] if y > 10 else SOOT[1])
    for x in range(6, 10):
        put(img, x, 8, CLAY[0])
    return img


def top(rng, lit):
    img = daub(rng)
    for y in range(4, 12):
        for x in range(4, 12):
            if x in (4, 11) and y in (4, 11):
                continue
            rim = x in (4, 11) or y in (4, 11)
            if lit:
                put(img, x, y, FIRE[0] if rim else FIRE[rng.choice((1, 2, 2, 3))])
            else:
                put(img, x, y, SOOT[2] if rim else SOOT[rng.choice((0, 0, 1))])
    return img


if __name__ == "__main__":
    block, item = TEXTURES / "block", TEXTURES / "item"
    item.mkdir(parents=True, exist_ok=True)
    daub(random.Random("side")).save(block / "bloomery_side.png")
    front(random.Random("front"), False).save(block / "bloomery_front.png")
    front(random.Random("front"), True).save(block / "bloomery_front_lit.png")
    top(random.Random("top"), False).save(block / "bloomery_top.png")
    top(random.Random("top"), True).save(block / "bloomery_top_lit.png")
    paint_raw("iron_bloom", ((48, 34, 30), (100, 70, 56), (150, 110, 88), (224, 206, 190))).save(item / "iron_bloom.png")
    paint_raw("roasted_galena", ((82, 76, 56), (134, 126, 92), (178, 170, 128), (226, 220, 184))).save(item / "roasted_galena.png")
    paint_raw("calcined_spodumene", ((134, 130, 128), (190, 186, 182), (224, 222, 218), (250, 249, 246))).save(item / "calcined_spodumene.png")
    smithing_hammer().save(item / "smithing_hammer.png")
    mortar_and_pestle().save(item / "mortar_and_pestle.png")
    pestle_sprite().save(item / "pestle.png")
    print("wrote bloomery, iron-working and hand-tool textures")
