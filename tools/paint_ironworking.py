#!/usr/bin/env python3
"""Paints the bloomery block, the iron-working items and the hand tools, in vanilla's idiom:
its stick and stone-tool colours, a dark rim, light from the top left. Edit and re-run; don't
hand-edit the PNGs.

    python3 tools/paint_ironworking.py
"""
import random
from pathlib import Path

from PIL import Image

from paint_minerals import paint_raw

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/fundamentals/textures"
CLEAR = (0, 0, 0, 0)

# Colours vanilla uses for a stick and for stone tools, dark to light.
STICK = [(40, 30, 11), (73, 54, 21), (104, 78, 30), (137, 103, 39)]
STONE = [(24, 24, 24), (73, 73, 73), (104, 104, 104), (127, 127, 127), (137, 137, 137), (154, 154, 154)]
CLAY = [(104, 62, 44), (124, 76, 54), (140, 88, 62), (154, 98, 70), (168, 110, 80)]
SOOT = [(28, 22, 20), (44, 34, 30), (62, 48, 42)]
FIRE = [(196, 72, 16), (236, 124, 24), (252, 184, 48), (255, 236, 132)]


def put(img, x, y, colour):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), tuple(colour) + (255,))


def stick(img, x, y, length):
    """A vanilla-style stick running up and to the right from its lower-left end at (x, y)."""
    for i in range(length):
        put(img, x + i, y - i, STICK[1])
        put(img, x + i + 1, y - i, STICK[3] if i % 2 == 0 else STICK[2])
        put(img, x + i + 2, y - i, STICK[0])
    put(img, x, y + 1, STICK[0])
    put(img, x + 1, y + 1, STICK[0])


def hammer():
    img = Image.new("RGBA", (16, 16), CLEAR)
    stick(img, 2, 13, 8)
    # The head: a squared stone block set across the top of the handle.
    cx, cy = 10.5, 4.5
    cells = set()
    for x in range(16):
        for y in range(16):
            along = ((x + 0.5 - cx) - (y + 0.5 - cy)) / 1.4142
            across = ((x + 0.5 - cx) + (y + 0.5 - cy)) / 1.4142
            if abs(along) <= 2.2 and abs(across) <= 3.9:
                cells.add((x, y))
    for x, y in cells:
        has = lambda dx, dy: (x + dx, y + dy) in cells
        if not has(0, 1) or not has(1, 0):
            tone = 0
        elif not has(0, -1) or not has(-1, 0):
            tone = 5
        else:
            tone = 4 if (x + y) < 15 else 3 if (x + y) < 17 else 2
        put(img, x, y, STONE[tone])
    for x, y in ((8, 7), (9, 6)):  # the lashing that holds the head on
        put(img, x, y, (92, 66, 40))
    return img


def mortar():
    img = Image.new("RGBA", (16, 16), CLEAR)
    rows = {6: (2, 13), 7: (2, 13), 8: (2, 13), 9: (3, 12), 10: (3, 12), 11: (4, 11), 12: (5, 10), 13: (4, 11), 14: (4, 11)}
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            if x in (x0, x1) or y == 14 or (y == 12 and x in (5, 10)):
                tone = 0
            elif y == 6:
                tone = 5                        # the lip, catching the light
            elif y == 7 and x0 + 2 <= x <= x1 - 2:
                tone = 1                        # the hollow
            elif x <= x0 + 2:
                tone = 4
            elif x >= x1 - 2 or y >= 13:
                tone = 2
            else:
                tone = 3
            put(img, x, y, STONE[tone])
    put(img, 2, 6, STONE[0])
    put(img, 13, 6, STONE[0])
    stick(img, 7, 7, 6)                         # the pestle, standing in the bowl
    for x, y in ((13, 1), (14, 1), (14, 2)):    # its rounded stone head
        put(img, x, y, STONE[4])
    put(img, 15, 1, STONE[0])
    put(img, 13, 0, STONE[0])
    put(img, 14, 0, STONE[0])
    return img


def daub(rng, soot_rows=0):
    """A clay wall raised in coils: soft mottling, a faint seam every few rows."""
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
                    colour = SOOT[rng.randrange(1, 3)]  # smoke-blackened lip
                put(img, x + dx, y, colour)
    for x in range(16):  # a darker course top and bottom, as vanilla's furnace has
        put(img, x, 15, CLAY[0])
    return img


def front(rng, lit):
    img = daub(rng, soot_rows=3)
    arch = {y: (5, 10) for y in range(10, 15)}
    arch[9] = (6, 9)
    for y, (x0, x1) in arch.items():   # the tapping arch at the foot
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
    block, item = ROOT / "block", ROOT / "item"
    item.mkdir(parents=True, exist_ok=True)
    daub(random.Random("side")).save(block / "bloomery_side.png")
    front(random.Random("front"), False).save(block / "bloomery_front.png")
    front(random.Random("front"), True).save(block / "bloomery_front_lit.png")
    top(random.Random("top"), False).save(block / "bloomery_top.png")
    top(random.Random("top"), True).save(block / "bloomery_top_lit.png")
    # A bloom is spongy iron shot through with slag: dark and rusty. Slag is black and glassy.
    paint_raw("iron_bloom", ((48, 34, 30), (100, 70, 56), (150, 110, 88), (224, 206, 190))).save(item / "iron_bloom.png")
    paint_raw("slag", ((18, 18, 22), (40, 40, 48), (70, 72, 86), (150, 156, 176))).save(item / "slag.png")
    hammer().save(item / "smithing_hammer.png")
    mortar().save(item / "mortar_and_pestle.png")
    print("wrote bloomery, iron-working and hand-tool textures")
