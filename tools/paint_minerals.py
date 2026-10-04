#!/usr/bin/env python3
"""Paints the 16x16 ore-block textures for the rare_earths and base_metals minerals.

Each mineral is composed from the way it actually occurs (one dominant mass plus stragglers,
crystal aggregates, veinlets in quartz, companion minerals) rather than from one shared
template. Edit the specs here and re-run; don't hand-edit the PNGs.

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
QUARTZ = [(176, 176, 172), (204, 204, 198), (228, 228, 222)]
ORTHO = [(1, 0), (-1, 0), (0, 1), (0, -1)]
DIAG = ORTHO + [(1, 1), (-1, 1), (1, -1), (-1, -1)]

# Palettes are (shadow, base, light, glint).
P = {
    "bastnasite": ((120, 66, 22), (186, 120, 44), (226, 174, 84), (250, 224, 150)),
    "monazite": ((96, 38, 20), (160, 72, 34), (204, 112, 56), (240, 170, 110)),
    "xenotime": ((84, 74, 34), (138, 126, 62), (182, 172, 98), (226, 220, 160)),
    "loparite": ((14, 14, 18), (38, 38, 46), (84, 86, 100), (176, 180, 196)),
    "euxenite": ((20, 14, 8), (58, 42, 20), (112, 90, 40), (196, 176, 96)),
    "chalcopyrite": ((112, 86, 20), (190, 156, 44), (232, 204, 84), (255, 244, 170)),
    "chalcocite": ((20, 22, 30), (50, 56, 70), (104, 116, 138), (178, 190, 212)),
    "covellite": ((18, 22, 78), (40, 52, 150), (78, 100, 208), (170, 190, 255)),
    "malachite": ((10, 70, 44), (24, 128, 78), (70, 190, 124), (160, 236, 186)),
    "azurite": ((12, 24, 96), (26, 54, 170), (60, 102, 224), (150, 184, 255)),
    "cuprite": ((70, 8, 16), (140, 20, 32), (198, 46, 54), (250, 130, 120)),
    "native_copper": ((120, 56, 30), (194, 104, 62), (236, 150, 100), (255, 208, 170)),
    "galena": ((58, 62, 72), (118, 124, 138), (176, 182, 196), (236, 240, 248)),
    "sphalerite": ((40, 22, 10), (92, 54, 22), (150, 96, 38), (222, 170, 80)),
    "smithsonite": ((64, 128, 124), (112, 184, 176), (164, 220, 210), (226, 248, 242)),
    "hemimorphite": ((112, 150, 184), (166, 200, 226), (210, 230, 244), (250, 252, 255)),
    "cassiterite": ((22, 14, 10), (58, 38, 26), (104, 74, 50), (190, 164, 130)),
}

# Bornite tarnishes iridescent ("peacock ore"): patches of several hues on one mass.
BORNITE = [((60, 30, 96), (118, 62, 170), (176, 110, 214), (214, 170, 240)),
           ((22, 52, 120), (44, 100, 190), (92, 160, 232), (170, 210, 250)),
           ((112, 62, 30), (184, 112, 58), (226, 164, 96), (250, 210, 150)),
           ((20, 96, 104), (40, 150, 150), (104, 208, 196), (180, 240, 230))]

# What each texture is built from, in paint order. See the element painters below.
RECIPES = {
    # Tabular honey-brown masses in carbonatite.
    "bastnasite": [("mass", 14, 19), ("blob", 2, 4, 7), ("speck", 4)],
    # An accessory mineral: scattered small resinous grains, never a big mass.
    "monazite": [("blob", 4, 5, 8), ("speck", 4)],
    "xenotime": [("crystals", "prism", 2, 3), ("crystals", "prism", 1, 1), ("speck", 3)],
    "loparite": [("crystals", "cube", 2, 3), ("crystals", "cube", 2, 1), ("speck", 3)],
    "euxenite": [("mass", 16, 20), ("blob", 3, 4, 7), ("speck", 3)],
    # Porphyry copper: sulfide riding quartz veinlets, with disseminated grains off the vein.
    "chalcopyrite": [("vein", "diag", 3, 5, 9), ("blob", 2, 3, 5), ("speck", 5)],
    "bornite": [("mass", 20, 26), ("blob", 2, 5, 8), ("speck", 3)],
    # Sooty secondary sulfide: irregular smeared patches.
    "chalcocite": [("smear", 2, 12, 16), ("blob", 2, 3, 5), ("speck", 4)],
    "covellite": [("crystals", "plate", 2, 3), ("crystals", "plate", 2, 1), ("speck", 2)],
    "malachite": [("banded", 2, 18, 24), ("blob", 2, 3, 5)],
    # Azurite alters to malachite, so the two are found together.
    "azurite": [("mass", 13, 17), ("blob", 2, 4, 6), ("companion", "malachite", 5)],
    # Cuprite forms on native copper in the oxidised zone.
    "cuprite": [("blob", 4, 5, 9), ("speck", 3), ("companion", "native_copper", 4)],
    "native_copper": [("smear", 2, 14, 20), ("speck", 4)],
    "galena": [("crystals", "cube", 2, 3), ("crystals", "cube", 2, 1), ("speck", 2)],
    "sphalerite": [("mass", 13, 17), ("blob", 3, 4, 6), ("speck", 3)],
    "smithsonite": [("crystals", "knob", 2, 3), ("crystals", "knob", 2, 1)],
    "hemimorphite": [("crystals", "knob", 1, 4), ("crystals", "knob", 2, 1), ("speck", 3)],
    # Greisen tin: stubby dark prisms along a quartz vein.
    "cassiterite": [("vein", "wavy", 3, 4, 7), ("crystals", "prism", 1, 1), ("speck", 3)],
}


def wrap(x, y):
    return x % SIZE, y % SIZE


def stone(rng):
    raw = [[rng.random() for _ in range(SIZE)] for _ in range(SIZE)]
    img = Image.new("RGB", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            # Blur along x only: stone reads as short horizontal streaks.
            v = (raw[y][x - 1] + 2 * raw[y][x] + raw[y][(x + 1) % SIZE] + raw[y][(x + 2) % SIZE]) / 5
            img.putpixel((x, y), STONE[min(4, max(0, int((v - 0.22) * 9)))])
    return img


class Canvas:
    def __init__(self, name):
        self.rng = random.Random(name)
        self.img = stone(self.rng)
        self.ore = set()

    def free(self, cells, margin):
        r = range(-margin, margin + 1)
        return not {wrap(x + dx, y + dy) for x, y in cells for dx in r for dy in r} & self.ore

    def grow(self, size, steps=ORTHO, compact=3.0, seed=None):
        cells = {seed or (self.rng.randrange(SIZE), self.rng.randrange(SIZE))}
        while len(cells) < size:
            frontier = {}
            for x, y in sorted(cells):
                for dx, dy in steps:
                    p = wrap(x + dx, y + dy)
                    if p not in cells:
                        frontier[p] = frontier.get(p, 0) + 1
            # Higher `compact` favours cells touching several cluster cells: lumps, not squiggles.
            cells.add(self.rng.choices(list(frontier), weights=[w ** compact for w in frontier.values()])[0])
        return cells

    def place(self, make, margin=1):
        for _ in range(300):
            cells = make()
            if self.free(cells, margin):
                return cells
        return set()

    def paint(self, cells, pal, glint=True, grain=0.25):
        shadow, base, light, hi = pal
        mid = tuple((a + b) // 2 for a, b in zip(shadow, base))
        for x, y in sorted(cells):
            s = shade(cells, x, y)
            colour = (shadow, base, light)[s + 1]
            if s == 0 and self.rng.random() < grain:
                colour = mid  # break up flat interiors
            self.img.putpixel((x, y), colour)
        if glint and len(cells) > 2:
            lit = [p for p in sorted(cells) if shade(cells, *p) > 0]
            if lit:
                self.img.putpixel(self.rng.choice(lit), hi)
        self.ore |= cells

    def finish(self):
        """Darken the stone just below/right of the ore so it sits in the rock."""
        for x, y in self.ore:
            for dx, dy in ((0, 1), (1, 0)):
                p = wrap(x + dx, y + dy)
                if p not in self.ore:
                    self.img.putpixel(p, tuple(max(0, c - 26) for c in self.img.getpixel(p)))
        return self.img


def shade(cells, x, y):
    """+1 on a top/left edge (lit), -1 on a bottom/right edge (shadowed), 0 inside."""
    has = lambda dx, dy: wrap(x + dx, y + dy) in cells
    lit = (not has(0, -1)) + (not has(-1, 0))
    dark = (not has(0, 1)) + (not has(1, 0))
    return (lit > dark) - (dark > lit)


def rect(x0, y0, w, h):
    return {wrap(x0 + i, y0 + j) for i in range(w) for j in range(h)}


def crystal(rng, kind, x0, y0):
    if kind == "cube":
        s = rng.choice((2, 3, 3, 4))
        return rect(x0, y0, s, s)
    if kind == "prism":
        return rect(x0, y0, 2, rng.randint(3, 6)) if rng.random() < 0.7 else rect(x0, y0, rng.randint(3, 5), 2)
    if kind == "plate":
        return rect(x0, y0, rng.randint(3, 5), rng.choice((1, 2)))
    cells = rect(x0, y0, 3, 3)  # knob: a 3x3 with the corners knocked off
    return cells - {wrap(x0 + i, y0 + j) for i in (0, 2) for j in (0, 2) if rng.random() < 0.8}


def aggregate(c, kind, count):
    """An intergrown group of crystals: each new one overlaps or abuts the group."""
    rng = c.rng

    def make():
        x0, y0 = rng.randrange(SIZE), rng.randrange(SIZE)
        parts = [crystal(rng, kind, x0, y0)]
        while len(parts) < count:
            ax, ay = rng.choice(sorted(set().union(*parts)))
            parts.append(crystal(rng, kind, ax + rng.randint(-2, 1), ay + rng.randint(-2, 1)))
        return parts

    for _ in range(300):
        parts = make()
        if c.free(set().union(*parts), 1):
            return parts
    return []


def vein_path(rng, style):
    """A quartz veinlet crossing the tile and meeting itself at the edges, so it tiles."""
    for _ in range(500):
        y = y0 = rng.randrange(SIZE)
        path = []
        for x in range(SIZE):
            path.append((x, y))
            if style == "diag":
                y += 1 if rng.random() < 0.85 else rng.choice((0, 2))
            else:
                y += rng.choice((-1, 0, 0, 1))
        if (y - y0) % SIZE == 0:
            return [wrap(*p) for p in path]
    return [(x, 8) for x in range(SIZE)]


def build(name):
    c = Canvas(name)
    rng, pal = c.rng, P.get(name)
    for element in RECIPES[name]:
        kind = element[0]
        if kind == "mass":
            size = rng.randint(element[1], element[2])
            cells = c.place(lambda: c.grow(size), 1)
            if name == "bornite":
                paint_bornite(c, cells)
            else:
                c.paint(cells, pal)
        elif kind == "banded":
            for _ in range(element[1]):
                size = rng.randint(element[2], element[3])
                cells = c.place(lambda: c.grow(size, compact=1.5), 1)
                paint_banded(c, cells, pal)
        elif kind == "smear":
            for _ in range(element[1]):
                size = rng.randint(element[2], element[3])
                c.paint(c.place(lambda: c.grow(size, DIAG, compact=0.4), 1), pal)
        elif kind == "blob":
            for _ in range(element[1]):
                size = rng.randint(element[2], element[3])
                cells = c.place(lambda: c.grow(size), 1)
                paint_bornite(c, cells) if name == "bornite" else c.paint(cells, pal)
        elif kind == "speck":
            for _ in range(element[1]):
                size = rng.choice((1, 1, 2))
                cells = c.place(lambda: c.grow(size), 0)
                tone = rng.choice(BORNITE)[rng.choice((1, 2))] if name == "bornite" else pal[rng.choice((1, 2))]
                for p in cells:
                    c.img.putpixel(p, tone)
                c.ore |= cells
        elif kind == "companion":
            other = P[element[1]]
            for _ in range(element[2]):
                size = rng.choice((1, 2, 2, 3))
                c.paint(c.place(lambda: c.grow(size), 0), other, glint=False, grain=0)
        elif kind == "crystals":
            for _ in range(element[2]):
                for part in aggregate(c, element[1], element[3]):
                    c.paint(part, pal, grain=0.1)  # each crystal shaded on its own, so faces read
        elif kind == "vein":
            path = vein_path(rng, element[1])
            quartz = set(path) | {wrap(x, y + 1) for x, y in path if rng.random() < 0.45}
            for p in quartz:
                c.img.putpixel(p, rng.choice(QUARTZ))
            for _ in range(element[2]):  # ore sits on the vein
                seed, size = rng.choice(path), rng.randint(element[3], element[4])
                cells = c.place(lambda: c.grow(size, seed=seed), 0)
                c.paint(cells, pal)
    return c.finish()


def paint_bornite(c, cells):
    hues = {}
    for x, y in sorted(cells):
        near = [hues[p] for p in (wrap(x - 1, y), wrap(x, y - 1)) if p in hues]
        hues[(x, y)] = c.rng.choice(near) if near and c.rng.random() < 0.6 else c.rng.randrange(len(BORNITE))
        c.img.putpixel((x, y), BORNITE[hues[(x, y)]][shade(cells, x, y) + 1])
    c.ore |= cells


def paint_banded(c, cells, pal):
    """Concentric colour bands, as in a cut malachite nodule."""
    if not cells:
        return
    # Wrapped clusters straddle the tile edge; measure rings from a member cell, not the mean.
    core = max(sorted(cells), key=lambda p: sum(wrap(p[0] + dx, p[1] + dy) in cells for dx, dy in DIAG))
    for x, y in sorted(cells):
        dx = min((x - core[0]) % SIZE, (core[0] - x) % SIZE)
        dy = min((y - core[1]) % SIZE, (core[1] - y) % SIZE)
        c.img.putpixel((x, y), (pal[2], pal[1], pal[0], pal[1])[(dx + dy) % 4])
    c.ore |= cells


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
            img.putpixel((x, y), rng.choice(matrix[:3] if (x * 7 + y * 3) % 5 else matrix))
    taken = set()
    for _ in range(40):
        if len(taken) > 70:
            break
        x0, y0, big = rng.randrange(SIZE), rng.randrange(SIZE), rng.random() < 0.35
        cells = rect(x0, y0, 3, 3) - {wrap(x0 + i, y0 + j) for i in (0, 2) for j in (0, 2)} if big \
            else rect(x0, y0, rng.choice((1, 2, 2)), rng.choice((1, 2)))
        if {wrap(x + dx, y + dy) for x, y in cells for dx in (-1, 0, 1) for dy in (-1, 0, 1)} & taken:
            continue
        taken |= cells
        # Pisoliths: pea-sized concretions, pale shell around a darker iron-rich core.
        shell, core = rng.choice((((224, 180, 120), (172, 104, 62)), ((206, 150, 92), (140, 70, 42)),
                                  ((236, 214, 180), (196, 140, 90))))
        for x, y in sorted(cells):
            centre = big and (x, y) == wrap(x0 + 1, y0 + 1)
            lit = shade(cells, x, y) > 0
            img.putpixel((x, y), core if centre else (shell if lit or big else tuple(int(v * 0.86) for v in shell)))
    return img


def paint_all():
    out = {name: build(name) for name in RECIPES}
    out["ion_adsorption_clay"] = paint_clay()
    out["bauxite"] = paint_bauxite()
    return out


def contact_sheet(textures, path, cols=5, scale=20):
    order = ["bastnasite", "monazite", "xenotime", "ion_adsorption_clay", "loparite", "euxenite",
             "chalcopyrite", "bornite", "chalcocite", "covellite", "malachite", "azurite", "cuprite",
             "native_copper", "bauxite", "galena", "sphalerite", "smithsonite", "hemimorphite",
             "cassiterite"]
    tile, pad, label = SIZE * scale, 24, 30
    rows = -(-len(order) // cols)
    sheet = Image.new("RGB", (cols * (tile + pad) + pad, rows * (tile + pad + label) + pad), (32, 34, 38))
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default(size=20)
    for i, name in enumerate(order):
        x = pad + (i % cols) * (tile + pad)
        y = pad + (i // cols) * (tile + pad + label)
        big = textures[name].resize((SIZE * scale, SIZE * scale), Image.NEAREST)
        sheet.paste(big, (x, y))
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
