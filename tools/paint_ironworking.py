#!/usr/bin/env python3
"""Paints the bloomery block, the iron-working items and the hand tools. Edit and re-run; don't
hand-edit the PNGs.

    python3 tools/paint_ironworking.py
"""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/fundamentals/textures"
CLAY = [(128, 76, 56), (146, 90, 66), (162, 104, 76), (176, 118, 88)]
SOOT = [(34, 26, 24), (52, 40, 36), (70, 54, 48)]
CLEAR = (0, 0, 0, 0)


def daub(rng, soot_rows=0):
    """Hand-built clay wall: mottled, with the horizontal seams of the coils it was raised in."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        seam = y % 5 == 4
        for x in range(16):
            tone = CLAY[rng.choice((0, 1, 1, 2, 2, 3))]
            if seam and rng.random() < 0.8:
                tone = CLAY[0]
            if y < soot_rows and rng.random() < 0.6 - y * 0.12:
                tone = SOOT[rng.randrange(3)]  # smoke-blackened lip
            img.putpixel((x, y), tone + (255,))
    return img


def front(rng, lit):
    img = daub(rng, soot_rows=3)
    # The arched tapping hole at the base, where air is blown in and the slag runs out.
    for y in range(9, 16):
        for x in range(5, 11):
            if y == 9 and x in (5, 10):
                continue
            if lit:
                heat = (y - 9) / 6
                colour = (255, int(120 + 110 * heat), int(30 + 90 * heat)) if 6 <= x <= 9 else (214, 86, 24)
            else:
                colour = SOOT[0] if 6 <= x <= 9 else SOOT[1]
            img.putpixel((x, y), colour + (255,))
    return img


def top(rng, lit):
    img = daub(rng)
    for y in range(4, 12):
        for x in range(4, 12):
            if (x in (4, 11)) and (y in (4, 11)):
                continue
            edge = x in (4, 11) or y in (4, 11)
            colour = (SOOT[2] if edge else SOOT[0]) if not lit else ((190, 70, 20) if edge else (250, 170, 60))
            img.putpixel((x, y), colour + (255,))
    return img


def lump(rng, tones, glints, cells):
    """An irregular lump for an item icon: darker toward the bottom right."""
    img = Image.new("RGBA", (16, 16), CLEAR)
    for x, y in cells:
        right = (x + 1, y) not in cells
        below = (x, y + 1) not in cells
        above = (x, y - 1) not in cells
        tone = tones[0] if right or below else tones[2] if above else tones[1]
        img.putpixel((x, y), tone + (255,))
    inner = [c for c in cells if all((c[0] + dx, c[1] + dy) in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
    for x, y in rng.sample(inner, min(len(inner), glints[1])):
        img.putpixel((x, y), glints[0] + (255,))
    return img


def blob(rng, size):
    cells = {(8, 8)}
    while len(cells) < size:
        x, y = rng.choice(sorted(cells))
        dx, dy = rng.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))
        if 2 <= x + dx <= 13 and 3 <= y + dy <= 13:
            cells.add((x + dx, y + dy))
    return cells


def hammer():
    img = Image.new("RGBA", (16, 16), CLEAR)
    wood = [(92, 66, 36), (122, 90, 50), (150, 114, 66)]
    stone = [(84, 84, 84), (118, 118, 118), (148, 148, 148), (176, 176, 176)]
    for i in range(9):  # handle, bottom-left to the head
        x, y = 2 + i, 13 - i
        img.putpixel((x, y), wood[1] + (255,))
        img.putpixel((x + 1, y), wood[0] + (255,))
        img.putpixel((x, y - 1), wood[2] + (255,))
    for x in range(7, 15):  # head: a squared stone block lashed across the top
        for y in range(1, 7):
            if (x, y) in ((7, 1), (14, 6)):
                continue
            tone = stone[3] if y == 1 or x == 7 else stone[0] if y == 6 or x == 14 else stone[1 + (x * 7 + y * 3) % 2]
            img.putpixel((x, y), tone + (255,))
    return img


def mortar():
    img = Image.new("RGBA", (16, 16), CLEAR)
    stone = [(74, 74, 78), (104, 104, 108), (134, 134, 138), (166, 166, 170)]
    wood = [(92, 66, 36), (122, 90, 50), (150, 114, 66)]
    # Bowl: wide at the lip, narrowing to a foot.
    rows = {8: (2, 13), 9: (2, 13), 10: (3, 12), 11: (3, 12), 12: (4, 11), 13: (5, 10), 14: (4, 11)}
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            tone = stone[3] if y == 8 else stone[0] if x == x1 or y == 14 else stone[2] if x == x0 else stone[1]
            img.putpixel((x, y), tone + (255,))
    for x in range(4, 12):  # the hollow, seen over the lip
        img.putpixel((x, 8), (52, 52, 56, 255))
    for i in range(7):  # pestle leaning out of the bowl to the upper right
        x, y = 7 + i, 8 - i
        img.putpixel((x, y), wood[1] + (255,))
        img.putpixel((x + 1, y), wood[0] + (255,))
    img.putpixel((14, 1), wood[2] + (255,))
    img.putpixel((13, 1), wood[2] + (255,))
    return img


if __name__ == "__main__":
    rng = random.Random("ironworking")
    block, item = ROOT / "block", ROOT / "item"
    item.mkdir(parents=True, exist_ok=True)
    daub(rng).save(block / "bloomery_side.png")
    front(random.Random("front"), False).save(block / "bloomery_front.png")
    front(random.Random("front"), True).save(block / "bloomery_front_lit.png")
    top(random.Random("top"), False).save(block / "bloomery_top.png")
    top(random.Random("top"), True).save(block / "bloomery_top_lit.png")
    # A bloom is spongy iron shot through with slag: dark and rusty, with a few bright metal spots.
    lump(rng, [(52, 40, 38), (96, 70, 60), (140, 104, 86)], ((206, 206, 214), 5), blob(rng, 58)).save(item / "iron_bloom.png")
    # Slag is glassy and black, with a dull sheen.
    lump(rng, [(22, 22, 26), (44, 44, 52), (72, 74, 86)], ((120, 124, 140), 3), blob(rng, 44)).save(item / "slag.png")
    hammer().save(item / "smithing_hammer.png")
    mortar().save(item / "mortar_and_pestle.png")
    print("wrote bloomery, iron-working and hand-tool textures")
