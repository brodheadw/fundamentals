#!/usr/bin/env python3
"""Paints the 16x16 ore-block textures for the rare_earths and base_metals minerals.

Edit the specs here and re-run; don't hand-edit the PNGs.

    python3 tools/paint_minerals.py            # write textures
    python3 tools/paint_minerals.py sheet.png  # also write a labelled contact sheet
"""
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

SIZE = 16
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/fundamentals/textures/block"

STONE = [(100, 100, 100), (113, 113, 113), (125, 125, 125), (134, 134, 134), (143, 143, 143)]

# id: (habit, count, (shadow, base, light, glint))
HOSTED = {
    # rare earths
    "bastnasite": ("blobs", 4, ((120, 66, 22), (186, 120, 44), (226, 174, 84), (250, 224, 150))),
    "monazite": ("blobs", 4, ((96, 38, 20), (160, 72, 34), (204, 112, 56), (240, 170, 110))),
    "xenotime": ("prisms", 5, ((84, 74, 34), (138, 126, 62), (182, 172, 98), (226, 220, 160))),
    "loparite": ("cubes", 6, ((14, 14, 18), (38, 38, 46), (84, 86, 100), (176, 180, 196))),
    "euxenite": ("blobs", 5, ((20, 14, 8), (58, 42, 20), (112, 90, 40), (196, 176, 96))),
    # copper
    "chalcopyrite": ("blobs", 5, ((112, 86, 20), (190, 156, 44), (232, 204, 84), (255, 244, 170))),
    "chalcocite": ("blobs", 5, ((24, 26, 32), (56, 60, 70), (92, 98, 110), (150, 158, 172))),
    "covellite": ("plates", 5, ((18, 22, 78), (40, 52, 150), (78, 100, 208), (170, 190, 255))),
    "malachite": ("bands", 4, ((10, 70, 44), (24, 128, 78), (70, 190, 124), (160, 236, 186))),
    "azurite": ("blobs", 5, ((12, 24, 96), (26, 54, 170), (60, 102, 224), (150, 184, 255))),
    "cuprite": ("blobs", 5, ((70, 8, 16), (140, 20, 32), (198, 46, 54), (250, 130, 120))),
    "native_copper": ("dendrite", 3, ((120, 56, 30), (194, 104, 62), (236, 150, 100), (255, 208, 170))),
    # lead, zinc, tin
    "galena": ("cubes", 6, ((58, 62, 72), (118, 124, 138), (176, 182, 196), (236, 240, 248))),
    "sphalerite": ("blobs", 5, ((40, 22, 10), (92, 54, 22), (150, 96, 38), (222, 170, 80))),
    "smithsonite": ("botryoidal", 5, ((64, 128, 124), (112, 184, 176), (164, 220, 210), (226, 248, 242))),
    "hemimorphite": ("botryoidal", 5, ((112, 150, 184), (166, 200, 226), (210, 230, 244), (250, 252, 255))),
    "cassiterite": ("prisms", 5, ((22, 14, 10), (58, 38, 26), (104, 74, 50), (190, 164, 130))),
}

# Bornite tarnishes iridescent ("peacock ore"): each pixel picks from several hues.
BORNITE = [((60, 30, 96), (118, 62, 170), (176, 110, 214)),
           ((22, 52, 120), (44, 100, 190), (92, 160, 232)),
           ((112, 62, 30), (184, 112, 58), (226, 164, 96)),
           ((20, 96, 104), (40, 150, 150), (104, 208, 196))]


def stone(rng):
    raw = [[rng.random() for _ in range(SIZE)] for _ in range(SIZE)]
    img = Image.new("RGB", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            # Blur along x only: stone reads as short horizontal streaks.
            v = (raw[y][x - 1] + 2 * raw[y][x] + raw[y][(x + 1) % SIZE] + raw[y][(x + 2) % SIZE]) / 5
            img.putpixel((x, y), STONE[min(4, max(0, int((v - 0.22) * 9)))])
    return img


def grow(rng, taken, size, steps, compact=3):
    """Random-walk cluster of `size` cells, clear of `taken` (wrapping, so the texture tiles)."""
    for _ in range(200):
        cells = {(rng.randrange(SIZE), rng.randrange(SIZE))}
        while len(cells) < size:
            frontier = {}
            for x, y in sorted(cells):
                for dx, dy in steps:
                    p = ((x + dx) % SIZE, (y + dy) % SIZE)
                    if p not in cells:
                        frontier[p] = frontier.get(p, 0) + 1
            # Favour cells touching several cluster cells: lumps, not squiggles.
            cells.add(rng.choices(list(frontier), weights=[w ** compact for w in frontier.values()])[0])
        halo = {((x + dx) % SIZE, (y + dy) % SIZE) for x, y in cells for dx in (-1, 0, 1) for dy in (-1, 0, 1)}
        if not halo & taken:
            return cells
    return set()


def rect(rng, taken, w, h):
    for _ in range(200):
        x0, y0 = rng.randrange(SIZE), rng.randrange(SIZE)
        cells = {((x0 + i) % SIZE, (y0 + j) % SIZE) for i in range(w) for j in range(h)}
        halo = {((x + dx) % SIZE, (y + dy) % SIZE) for x, y in cells for dx in (-1, 0, 1) for dy in (-1, 0, 1)}
        if not halo & taken:
            return cells
    return set()


def clusters(rng, habit, count):
    taken, out = set(), []
    for _ in range(count):
        if habit == "cubes":
            s = rng.choice((2, 3, 3))
            c = rect(rng, taken, s, s)
        elif habit == "prisms":
            c = rect(rng, taken, *rng.choice(((2, 3), (2, 4), (2, 5), (4, 2), (3, 2))))
        elif habit == "plates":
            c = rect(rng, taken, *rng.choice(((3, 1), (4, 1), (4, 2), (3, 2))))
        elif habit == "botryoidal":
            c = rect(rng, taken, 3, 3)
            if c:
                xs, ys = sorted({x for x, _ in c}), sorted({y for _, y in c})
                wrapped = lambda v: [v[0], v[-1]] if len(v) == 3 and v[1] - v[0] == 1 and v[2] - v[1] == 1 else None
                ex, ey = wrapped(xs), wrapped(ys)
                if ex and ey:  # knock the corners off: a 3x3 becomes a rounded knob
                    c -= {(x, y) for x in ex for y in ey if rng.random() < 0.75}
        elif habit == "dendrite":
            c = grow(rng, taken, rng.randint(10, 14), [(1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, 1)], compact=0.5)
        elif habit == "bands":
            c = grow(rng, taken, rng.randint(11, 15), [(1, 0), (-1, 0), (0, 1), (0, -1)])
        else:
            c = grow(rng, taken, rng.randint(6, 10), [(1, 0), (-1, 0), (0, 1), (0, -1)])
        if c:
            taken |= c
            out.append(c)
    return out


def shade(cells, x, y):
    """+1 on a top/left edge (lit), -1 on a bottom/right edge (shadowed), 0 inside."""
    has = lambda dx, dy: ((x + dx) % SIZE, (y + dy) % SIZE) in cells
    lit = (not has(0, -1)) + (not has(-1, 0))
    dark = (not has(0, 1)) + (not has(1, 0))
    return (lit > dark) - (dark > lit)


def paint_hosted(name, habit, count, pal):
    rng = random.Random(name)
    img = stone(rng)
    shadow, base, light, glint = pal
    ore = set()
    for cells in clusters(rng, habit, count):
        ore |= cells
        ordered = sorted(cells)
        cx = sum(x for x, _ in ordered) / len(ordered)
        cy = sum(y for _, y in ordered) / len(ordered)
        for x, y in ordered:
            if habit == "bands":
                ring = int(abs(x - cx) + abs(y - cy) + 0.5) % 3
                colour = (light, base, shadow)[ring]
            else:
                colour = (shadow, base, light)[shade(cells, x, y) + 1]
            img.putpixel((x, y), colour)
        lit = [p for p in ordered if shade(cells, *p) > 0] or ordered
        img.putpixel(rng.choice(lit), glint)
    under_shadow(img, ore)
    return img


def paint_bornite():
    rng = random.Random("bornite")
    img = stone(rng)
    ore = set()
    for cells in clusters(rng, "blobs", 5):
        ore |= cells
        hue = rng.randrange(len(BORNITE))
        for x, y in sorted(cells):
            if rng.random() < 0.45:
                hue = rng.randrange(len(BORNITE))
            img.putpixel((x, y), BORNITE[hue][shade(cells, x, y) + 1])
    under_shadow(img, ore)
    return img


def under_shadow(img, ore):
    """Darken the stone just below/right of each ore cluster so it sits in the rock."""
    for x, y in ore:
        for dx, dy in ((0, 1), (1, 0)):
            p = ((x + dx) % SIZE, (y + dy) % SIZE)
            if p not in ore:
                r, g, b = img.getpixel(p)
                img.putpixel(p, (max(0, r - 26), max(0, g - 26), max(0, b - 26)))


def paint_clay():
    """Ion-adsorption clay: weathered granite regolith — no ore grains, the REEs sit on the clay."""
    rng = random.Random("ion_adsorption_clay")
    tones = [(150, 84, 52), (172, 102, 62), (190, 122, 76), (204, 142, 92)]
    kaolin = [(222, 196, 164), (236, 220, 196)]
    raw = [[rng.random() for _ in range(SIZE)] for _ in range(SIZE)]
    img = Image.new("RGB", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            # Wide soft blotches, stretched sideways: weathering fronts, not planks.
            v = sum(raw[(y + dy) % SIZE][(x + dx) % SIZE] for dx in (-2, -1, 0, 1, 2) for dy in (-1, 0, 1)) / 15
            img.putpixel((x, y), tones[min(3, max(0, int((v - 0.32) * 11)))])
    for _ in range(8):
        x, y = rng.randrange(SIZE), rng.randrange(SIZE)
        img.putpixel((x, y), rng.choice(kaolin))
        if rng.random() < 0.5:
            img.putpixel(((x + 1) % SIZE, y), kaolin[0])
    for _ in range(5):  # relict quartz/feldspar grit from the parent granite
        img.putpixel((rng.randrange(SIZE), rng.randrange(SIZE)), (112, 60, 40))
    return img


def paint_bauxite():
    """Bauxite is a rock, not a grain in stone: red-brown laterite studded with pisoliths."""
    rng = random.Random("bauxite")
    matrix = [(118, 54, 34), (140, 68, 42), (158, 82, 50), (172, 98, 60)]
    img = Image.new("RGB", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            img.putpixel((x, y), rng.choice(matrix))
    taken = set()
    for _ in range(9):
        c = rect(rng, taken, 3, 3) if rng.random() < 0.6 else rect(rng, taken, 2, 2)
        if not c:
            continue
        taken |= c
        rim, core = rng.choice((((224, 180, 120), (150, 70, 40)), ((206, 150, 92), (120, 52, 32)),
                                ((236, 214, 180), (176, 104, 62))))
        for x, y in sorted(c):
            sides = sum(((x + dx) % SIZE, (y + dy) % SIZE) in c for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if len(c) == 9 and sides == 2:
                continue  # corner: leave matrix showing so the 3x3 reads round
            img.putpixel((x, y), core if sides == 4 else rim)
    return img


def paint_all():
    out = {name: paint_hosted(name, *spec) for name, spec in HOSTED.items()}
    out["bornite"] = paint_bornite()
    out["ion_adsorption_clay"] = paint_clay()
    out["bauxite"] = paint_bauxite()
    return out


def contact_sheet(textures, path, cols=5, scale=10):
    order = ["bastnasite", "monazite", "xenotime", "ion_adsorption_clay", "loparite", "euxenite",
             "chalcopyrite", "bornite", "chalcocite", "covellite", "malachite", "azurite", "cuprite",
             "native_copper", "bauxite", "galena", "sphalerite", "smithsonite", "hemimorphite",
             "cassiterite"]
    tile, pad, label = SIZE * scale * 2, 24, 30
    rows = -(-len(order) // cols)
    sheet = Image.new("RGB", (cols * (tile + pad) + pad, rows * (tile + pad + label) + pad), (32, 34, 38))
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default(size=20)
    for i, name in enumerate(order):
        x = pad + (i % cols) * (tile + pad)
        y = pad + (i // cols) * (tile + pad + label)
        big = textures[name].resize((SIZE * scale, SIZE * scale), Image.NEAREST)
        for dx in (0, 1):  # 2x2 so tiling seams show
            for dy in (0, 1):
                sheet.paste(big, (x + dx * SIZE * scale, y + dy * SIZE * scale))
        draw.text((x, y + tile + 4), name.replace("_", " "), fill=(226, 228, 232), font=font)
    sheet.save(path)


if __name__ == "__main__":
    textures = paint_all()
    OUT.mkdir(parents=True, exist_ok=True)
    for name, img in textures.items():
        img.save(OUT / f"{name}_ore.png")
    print(f"wrote {len(textures)} textures to {OUT}")
    if len(sys.argv) > 1:
        contact_sheet(textures, sys.argv[1])
        print(f"wrote contact sheet {sys.argv[1]}")
