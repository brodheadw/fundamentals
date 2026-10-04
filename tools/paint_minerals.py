#!/usr/bin/env python3
"""Paints the 16x16 ore-block texture for every mineral in the mod.

Each mineral is painted in the rock it really occurs in (carbonatite, granite, limestone,
gossan, basalt, ...) and composed from its real habit (one dominant mass plus stragglers,
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
# Host rocks: (tones dark->light, blur_x, blur_y, share of each tone).
HOSTS = {
    "stone": ([(100, 100, 100), (113, 113, 113), (125, 125, 125), (134, 134, 134), (143, 143, 143)], 2, 0, (1, 3, 4, 3, 1)),
    "carbonatite": ([(170, 150, 118), (192, 176, 144), (210, 197, 168), (226, 216, 190)], 1, 1, (1, 3, 4, 2)),
    "granite": ([(150, 110, 96), (184, 138, 120), (204, 164, 146), (216, 206, 196)], 1, 1, (2, 4, 3, 2)),
    "greisen": ([(138, 138, 134), (160, 160, 154), (180, 180, 172), (198, 198, 190)], 1, 1, (1, 3, 4, 2)),
    "porphyry": ([(118, 106, 104), (132, 120, 116), (144, 132, 126), (154, 142, 136)], 1, 0, (1, 3, 4, 2)),
    "gossan": ([(96, 54, 26), (126, 76, 34), (154, 98, 42), (178, 122, 54)], 1, 1, (2, 4, 3, 1)),
    "limestone": ([(160, 156, 142), (176, 172, 158), (188, 184, 170), (198, 195, 182)], 3, 0, (1, 3, 4, 2)),
    "basalt": ([(44, 46, 50), (56, 58, 62), (68, 70, 74), (80, 82, 86)], 1, 0, (2, 4, 3, 1)),
    "mafic": ([(50, 56, 52), (64, 70, 64), (80, 86, 78), (98, 104, 94)], 1, 1, (2, 4, 3, 1)),
    "syenite": ([(144, 150, 144), (164, 170, 162), (182, 187, 178), (198, 202, 194)], 1, 1, (1, 3, 4, 2)),
    "sand": ([(184, 164, 120), (200, 182, 138), (214, 198, 154), (226, 212, 170)], 2, 0, (1, 3, 4, 2)),
}

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
    "pyrolusite": ((8, 8, 10), (26, 26, 30), (52, 52, 60), (104, 106, 120)),
    "pentlandite": ((96, 72, 34), (160, 126, 62), (204, 172, 96), (240, 220, 150)),
    "pyrrhotite": ((70, 50, 36), (116, 86, 62), (150, 116, 86), (190, 160, 124)),
    "native_silver": ((96, 100, 110), (164, 170, 180), (214, 218, 226), (250, 252, 255)),
    "argentite": ((16, 18, 22), (44, 48, 56), (84, 90, 102), (150, 158, 172)),
    "sperrylite": ((120, 126, 134), (186, 192, 200), (228, 232, 238), (255, 255, 255)),
    "cooperite": ((60, 64, 72), (110, 116, 126), (156, 162, 172), (210, 216, 224)),
    "braggite": ((90, 90, 84), (148, 148, 138), (196, 196, 184), (236, 236, 226)),
    "chromite": ((6, 6, 8), (20, 20, 24), (40, 40, 46), (80, 80, 90)),
    "cinnabar": ((110, 12, 18), (184, 26, 28), (228, 58, 46), (255, 142, 120)),
}

# Bornite tarnishes iridescent ("peacock ore"): patches of several hues on one mass.
BORNITE = [((60, 30, 96), (118, 62, 170), (176, 110, 214), (214, 170, 240)),
           ((22, 52, 120), (44, 100, 190), (92, 160, 232), (170, 210, 250)),
           ((112, 62, 30), (184, 112, 58), (226, 164, 96), (250, 210, 150)),
           ((20, 96, 104), (40, 150, 150), (104, 208, 196), (180, 240, 230))]

# name: (host rock, [elements in paint order]). See the element painters in build().
RECIPES = {
    # --- rare earths ---
    # Tabular honey-brown masses in a carbonatite plug (Mountain Pass, Bayan Obo).
    "bastnasite": ("carbonatite", [("mass", 14, 19), ("blob", 2, 4, 7), ("speck", 4), ("companion", "pyrolusite", 3)]),
    # A placer mineral: heavy resinous grains concentrated in black-sand laminae on beaches.
    "monazite": ("sand", [("laminae", 3), ("blob", 3, 3, 5), ("speck", 8)]),
    "xenotime": ("granite", [("crystals", "prism", 2, 2), ("crystals", "prism", 1, 1), ("speck", 3)]),
    "loparite": ("syenite", [("laths", 5), ("crystals", "cube", 2, 3), ("crystals", "cube", 2, 1), ("speck", 3)]),
    "euxenite": ("granite", [("mass", 14, 18), ("blob", 2, 4, 7), ("speck", 3)]),
    # --- copper ---
    # Porphyry copper: sulfide riding quartz veinlets, with disseminated grains off the vein.
    "chalcopyrite": ("porphyry", [("phenocrysts", 6), ("vein", "diag", 3, 5, 9), ("blob", 2, 3, 5), ("speck", 5)]),
    "bornite": ("porphyry", [("phenocrysts", 5), ("mass", 20, 26), ("blob", 2, 5, 8), ("speck", 3)]),
    # Sooty secondary sulfide: irregular smeared patches under the leached cap.
    "chalcocite": ("porphyry", [("phenocrysts", 4), ("smear", 2, 12, 16), ("blob", 2, 3, 5), ("speck", 4)]),
    "covellite": ("stone", [("crystals", "plate", 2, 3), ("crystals", "plate", 2, 1), ("speck", 2)]),
    "malachite": ("gossan", [("voids", 5), ("banded", 2, 18, 24), ("blob", 2, 3, 5)]),
    # Azurite alters to malachite, so the two are found together; classic in limestone.
    "azurite": ("limestone", [("mass", 13, 17), ("blob", 2, 4, 6), ("companion", "malachite", 5)]),
    # Cuprite forms on native copper in the oxidised zone.
    "cuprite": ("gossan", [("voids", 4), ("blob", 4, 5, 9), ("speck", 3), ("companion", "native_copper", 4)]),
    # --- lead, zinc, tin ---
    "galena": ("limestone", [("crystals", "cube", 2, 3), ("crystals", "cube", 2, 1), ("speck", 2)]),
    "sphalerite": ("limestone", [("mass", 13, 17), ("blob", 3, 4, 6), ("speck", 3), ("companion", "galena", 3)]),
    "smithsonite": ("limestone", [("crystals", "knob", 2, 3), ("crystals", "knob", 2, 1)]),
    "hemimorphite": ("gossan", [("voids", 4), ("crystals", "knob", 1, 4), ("crystals", "knob", 2, 1), ("speck", 3)]),
    # Greisen tin: stubby dark prisms along a quartz vein.
    "cassiterite": ("greisen", [("vein", "wavy", 3, 4, 7), ("crystals", "prism", 1, 1), ("speck", 3)]),
    # --- ferrous (Laptop B's minerals) ---
    # Manganese oxide: sooty black dendrites creeping across limestone.
    "pyrolusite": ("limestone", [("smear", 3, 12, 18), ("speck", 6)]),
    # Sudbury/Norilsk: bronze sulfide blebs in dark mafic rock, always with pyrrhotite.
    "pentlandite": ("mafic", [("mass", 10, 14), ("blob", 2, 3, 6), ("companion", "pyrrhotite", 5), ("speck", 3)]),
    # --- precious and PGM (Laptop B's minerals) ---
    "native_silver": ("stone", [("vein", "wavy", 0, 0, 0), ("smear", 2, 9, 13), ("speck", 3), ("companion", "argentite", 3)]),
    "argentite": ("stone", [("vein", "diag", 3, 4, 8), ("blob", 2, 3, 5), ("speck", 3)]),
    # PGM minerals are tiny bright grains in a dark layered intrusion, not masses.
    "sperrylite": ("mafic", [("crystals", "cube", 3, 1), ("speck", 6)]),
    "cooperite": ("mafic", [("band", "chromite"), ("blob", 2, 2, 4), ("speck", 6)]),
    "braggite": ("mafic", [("companion", "pentlandite", 5), ("blob", 2, 2, 4), ("speck", 5)]),
    "cinnabar": ("limestone", [("vein", "wavy", 3, 4, 8), ("blob", 2, 4, 7), ("speck", 5)]),
}


def wrap(x, y):
    return x % SIZE, y % SIZE


def field(rng, bx, by):
    """A wrapped random field blurred `bx`/`by` pixels each way, ranked to 0..1."""
    raw = [[rng.random() for _ in range(SIZE)] for _ in range(SIZE)]
    v = {(x, y): sum(raw[(y + dy) % SIZE][(x + dx) % SIZE] for dx in range(-bx, bx + 1) for dy in range(-by, by + 1))
         for x in range(SIZE) for y in range(SIZE)}
    order = sorted(v, key=v.get)
    return {p: i / len(order) for i, p in enumerate(order)}


def host_rock(rng, kind):
    tones, bx, by, share = HOSTS[kind]
    cuts, total = [], 0
    for w in share:
        total += w
        cuts.append(total / sum(share))
    img = Image.new("RGB", (SIZE, SIZE))
    for p, v in field(rng, bx, by).items():
        img.putpixel(p, tones[next(i for i, c in enumerate(cuts) if v < c or i == len(cuts) - 1)])
    if kind == "granite":  # coarse: dark mica books and glassy quartz eyes
        for _ in range(9):
            img.putpixel((rng.randrange(SIZE), rng.randrange(SIZE)), (52, 46, 44))
        for _ in range(5):
            x, y = rng.randrange(SIZE), rng.randrange(SIZE)
            for p in rect(x, y, 2, rng.choice((1, 2))):
                img.putpixel(p, (224, 220, 214))
    elif kind == "carbonatite":
        for _ in range(4):
            img.putpixel((rng.randrange(SIZE), rng.randrange(SIZE)), (132, 96, 62))
    elif kind == "limestone":  # bedding planes
        for y in (rng.randrange(0, 5), rng.randrange(6, 11), rng.randrange(12, 16)):
            for x in range(SIZE):
                if rng.random() < 0.7:
                    img.putpixel((x, y), (146, 142, 128))
    elif kind == "greisen":
        for _ in range(8):
            img.putpixel((rng.randrange(SIZE), rng.randrange(SIZE)), (220, 220, 210))
    return img


class Canvas:
    def __init__(self, name, host):
        self.rng = random.Random(name)
        self.img = host_rock(self.rng, host)
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
    host, elements = RECIPES[name]
    c = Canvas(name, host)
    rng, pal = c.rng, P.get(name)
    for element in elements:
        kind = element[0]
        if kind == "phenocrysts":  # pale feldspar crystals floating in the porphyry groundmass
            for _ in range(element[1]):
                for p in rect(rng.randrange(SIZE), rng.randrange(SIZE), rng.choice((1, 2)), rng.choice((1, 2))):
                    c.img.putpixel(p, rng.choice(((196, 188, 178), (210, 204, 194))))
        elif kind == "voids":  # leached pits (gossan boxwork) or gas bubbles (basalt)
            for _ in range(element[1]):
                dark = tuple(int(v * 0.55) for v in HOSTS[host][0][0])
                for p in rect(rng.randrange(SIZE), rng.randrange(SIZE), rng.choice((1, 1, 2)), 1):
                    c.img.putpixel(p, dark)
        elif kind == "laths":  # dark aegirine needles in the syenite
            for _ in range(element[1]):
                w, h = rng.choice(((1, 3), (3, 1), (1, 2)))
                for p in rect(rng.randrange(SIZE), rng.randrange(SIZE), w, h):
                    c.img.putpixel(p, (44, 58, 48))
        elif kind == "laminae":  # black-sand layers of heavy minerals
            for y in rng.sample(range(SIZE), element[1]):
                for x in range(SIZE):
                    if rng.random() < 0.75:
                        c.img.putpixel(wrap(x, y + (rng.random() < 0.2)), rng.choice(((58, 52, 48), (84, 74, 64))))
        elif kind == "band":  # a chromitite seam through the intrusion
            y0, other = rng.randrange(SIZE), P[element[1]]
            for x in range(SIZE):
                for dy in range(rng.choice((1, 2, 2))):
                    c.img.putpixel(wrap(x, y0 + dy), other[rng.choice((0, 1, 1, 2))])
        elif kind == "mass":
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


def paint_banded_iron(name, bands):
    """Banded iron formation: thin wavy layers of iron oxide and chert/jasper, laid on a sea floor."""
    rng = random.Random(name)
    rows = [tones for tones, thick in bands for _ in range(thick)]
    wobble, y = [], 0
    for x in range(SIZE):
        wobble.append(y)
        y += rng.choice((-1, 0, 0, 0, 0, 0, 0, 1))
    wobble = [w - round(wobble[-1] * x / (SIZE - 1)) for x, w in enumerate(wobble)]  # close the loop
    img = Image.new("RGB", (SIZE, SIZE))
    for x in range(SIZE):
        for y in range(SIZE):
            tones = rows[(y + wobble[x]) % SIZE]
            img.putpixel((x, y), tones[min(len(tones) - 1, int(rng.random() ** 0.6 * len(tones)))] if rng.random() < 0.9 else tones[0])
    return img


JASPER = [(112, 30, 26), (140, 38, 30), (160, 52, 36)]
SPECULAR = [(64, 62, 70), (88, 86, 96), (112, 110, 122), (150, 150, 164)]
CHERT = [(120, 114, 108), (136, 130, 122), (150, 144, 136)]
LODESTONE = [(18, 18, 22), (30, 30, 36), (44, 44, 52), (70, 72, 84)]


def paint_goethite():
    """Earthy ochre iron hydroxide with dark glossy botryoidal crusts (bog iron, gossan caps)."""
    rng = random.Random("goethite")
    tones = [(110, 74, 22), (146, 102, 30), (178, 132, 44), (204, 162, 64)]
    img = Image.new("RGB", (SIZE, SIZE))
    for p, v in field(rng, 1, 1).items():
        img.putpixel(p, tones[min(3, int(v * 4))])
    c = Canvas.__new__(Canvas)
    c.rng, c.img, c.ore = rng, img, set()
    crust = ((20, 12, 8), (52, 32, 16), (92, 60, 26), (150, 110, 60))
    for part in aggregate(c, "knob", 4) + aggregate(c, "knob", 2):
        c.paint(part, crust, grain=0.1)
    return c.finish()


def paint_nickel_laterite():
    """Tropical weathering profile: rusty limonite cut by apple-green garnierite veinlets."""
    rng = random.Random("nickel_laterite")
    tones = [(122, 78, 34), (150, 102, 44), (174, 128, 58), (196, 152, 76)]
    green = [(74, 124, 62), (106, 160, 82), (144, 192, 106)]
    img = Image.new("RGB", (SIZE, SIZE))
    for p, v in field(rng, 2, 1).items():
        img.putpixel(p, tones[min(3, int(v * 4))])
    for style in ("wavy", "diag"):
        for x, y in vein_path(rng, style):
            img.putpixel((x, y), rng.choice(green))
            if rng.random() < 0.35:
                img.putpixel(wrap(x, y + 1), green[0])
    return img


def paint_all():
    out = {name: build(name) for name in RECIPES}
    out["ion_adsorption_clay"] = paint_clay()
    out["bauxite"] = paint_bauxite()
    out["hematite"] = paint_banded_iron("hematite", [(JASPER, 2), (SPECULAR, 1), (JASPER, 1), (SPECULAR, 2),
                                                     (JASPER, 3), (SPECULAR, 1), (JASPER, 1), (SPECULAR, 2),
                                                     (JASPER, 2), (SPECULAR, 1)])
    out["magnetite"] = paint_banded_iron("magnetite", [(LODESTONE, 3), (CHERT, 1), (LODESTONE, 2), (CHERT, 2),
                                                       (LODESTONE, 1), (CHERT, 1), (LODESTONE, 3), (CHERT, 1),
                                                       (LODESTONE, 1), (CHERT, 1)])
    out["goethite"] = paint_goethite()
    out["nickel_laterite"] = paint_nickel_laterite()
    return out


SHEET_ORDER = [
    "bastnasite", "monazite", "xenotime", "ion_adsorption_clay", "loparite", "euxenite",
    "chalcopyrite", "bornite", "chalcocite", "covellite", "malachite", "azurite", "cuprite",
    "bauxite", "galena", "sphalerite", "smithsonite", "hemimorphite", "cassiterite",
    "hematite", "magnetite", "goethite", "pyrolusite", "pentlandite", "nickel_laterite",
    "native_silver", "argentite", "sperrylite", "cooperite", "braggite", "cinnabar",
]


def contact_sheet(textures, path, cols=7, scale=14):
    tile, pad, label = SIZE * scale, 20, 28
    rows = -(-len(SHEET_ORDER) // cols)
    sheet = Image.new("RGB", (cols * (tile + pad) + pad, rows * (tile + pad + label) + pad), (32, 34, 38))
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default(size=18)
    for i, name in enumerate(SHEET_ORDER):
        x = pad + (i % cols) * (tile + pad)
        y = pad + (i // cols) * (tile + pad + label)
        sheet.paste(textures[name].resize((tile, tile), Image.NEAREST), (x, y))
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
