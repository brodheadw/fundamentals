#!/usr/bin/env python3
"""Paints the 16x16 ore texture for every mineral in the mod.

Almost every ore is an overlay: transparent except for the mineral itself. The block model draws
it over the texture of whatever rock the ore formed in (stone, deepslate, granite, a Create stone,
one of our own rocks...), so the same ore sits naturally in any of them and follows the player's
resource pack. The mineral is spread evenly across the tile in muted colours, the way vanilla
and the long-standing ore mods do it, so a body of ore reads as one continuous speckled mass.

A few "ores" are whole rocks rather than a mineral in something else (bauxite, the laterites,
the REE clay, bog iron); those are painted as full tiles. See WHOLE.

Each mineral keeps its real habit (cubes, prisms, plates, smears, banded lumps), comes in three
grades of richness, and has several variants per grade so large bodies do not tile.

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

# Our own host rocks: (tones dark->light, blur_x, blur_y, share of each tone).
HOSTS = {
    "carbonatite": ([(170, 150, 118), (192, 176, 144), (210, 197, 168), (226, 216, 190)], 1, 1, (1, 3, 4, 2)),
    "gabbro": ([(50, 56, 52), (64, 70, 64), (80, 86, 78), (98, 104, 94)], 1, 1, (2, 4, 3, 1)),
    "syenite": ([(144, 150, 144), (164, 170, 162), (182, 187, 178), (198, 202, 194)], 1, 1, (1, 3, 4, 2)),
    "laterite": ([(118, 54, 34), (140, 68, 42), (158, 82, 50), (172, 98, 60)], 1, 1, (2, 4, 3, 1)),
}
ROCK_BLOCKS = list(HOSTS)

# Ores that are a whole rock, painted as a full tile rather than an overlay.
WHOLE = ["ion_adsorption_clay", "bauxite", "goethite", "nickel_laterite"]

CLEAR = (255, 0, 255)  # painted as transparent

# Each ore gets several textures and the game picks one per block position, so a wall of one
# ore does not repeat the same tile.
VARIANTS = 3

# An ore body is graded: rich at its core, ordinary around that, a trace where it peters out.
# The value is how much mineral the texture carries relative to the ordinary ("edge") one.
GRADES = {"core": 1.6, "edge": 1.0, "trace": 0.4}


def seed(name, variant, grade="edge"):
    return name + (f"#{variant}" if variant else "") + ("" if grade == "edge" else f"@{grade}")


def scaled(element, r):
    """One recipe element with its amount of mineral scaled by `r` (None drops it)."""
    count = lambda n: int(n * r + 0.5)
    size = lambda n: max(2, int(n * min(1.25, max(0.7, r)) + 0.5))
    kind = element[0]
    if kind in ("banded", "smear", "blob"):
        return (kind, count(element[1]), size(element[2]), size(element[3])) if count(element[1]) else None
    if kind in ("speck", "companion", "laminae"):
        n = count(element[-1])
        return element[:-1] + (n,) if n else None
    if kind == "crystals":
        groups = count(element[2])
        return (kind, element[1], groups, element[3]) if groups else None
    return element


ORTHO = [(1, 0), (-1, 0), (0, 1), (0, -1)]
DIAG = ORTHO + [(1, 1), (-1, 1), (1, -1), (-1, -1)]

# Palettes are (shadow, base, light, glint). Kept muted: an ore should sit in the rock, not
# shout over it; the glint is the one bright pixel that says "metal".
P = {
    "hematite": ((66, 24, 22), (112, 42, 36), (150, 66, 52), (198, 124, 104)),
    "magnetite": ((16, 16, 22), (38, 38, 48), (68, 70, 84), (150, 154, 172)),
    "pyrolusite": ((14, 14, 16), (34, 34, 38), (58, 58, 66), (112, 114, 126)),
    "pentlandite": ((92, 70, 36), (140, 110, 58), (178, 148, 86), (226, 204, 140)),
    "pyrrhotite": ((70, 50, 36), (110, 82, 60), (140, 110, 82), (180, 152, 118)),
    "chromite": ((10, 10, 12), (26, 26, 30), (48, 48, 54), (96, 96, 106)),
    "wolframite": ((18, 14, 12), (46, 36, 32), (80, 66, 58), (156, 144, 132)),
    "scheelite": ((140, 116, 76), (188, 168, 120), (216, 202, 160), (244, 238, 214)),
    "molybdenite": ((58, 64, 82), (96, 106, 130), (136, 148, 172), (204, 212, 230)),
    "cobaltite": ((98, 90, 98), (150, 142, 150), (192, 184, 190), (240, 234, 238)),
    "erythrite": ((130, 34, 82), (186, 62, 118), (214, 110, 154), (240, 176, 204)),
    "ilmenite": ((12, 12, 14), (32, 32, 38), (60, 60, 70), (118, 120, 134)),
    "rutile": ((76, 22, 12), (128, 44, 22), (176, 84, 40), (224, 156, 96)),
    "chalcopyrite": ((104, 82, 26), (160, 132, 44), (200, 172, 70), (244, 228, 150)),
    "chalcocite": ((22, 24, 32), (50, 56, 68), (88, 98, 116), (160, 172, 192)),
    "covellite": ((22, 26, 78), (42, 52, 134), (74, 92, 184), (156, 176, 240)),
    "malachite": ((14, 66, 44), (28, 110, 72), (62, 156, 106), (140, 214, 170)),
    "azurite": ((16, 28, 92), (30, 54, 148), (60, 94, 196), (140, 172, 240)),
    "cuprite": ((66, 12, 18), (122, 24, 32), (168, 46, 50), (228, 122, 112)),
    "native_copper": ((112, 56, 32), (170, 96, 60), (208, 136, 94), (244, 196, 160)),
    "galena": ((34, 38, 52), (66, 72, 94), (104, 112, 138), (206, 214, 232)),
    "sphalerite": ((44, 26, 12), (88, 54, 24), (132, 88, 38), (204, 158, 80)),
    "smithsonite": ((70, 122, 118), (108, 164, 158), (150, 198, 190), (212, 238, 232)),
    "hemimorphite": ((106, 138, 168), (150, 180, 206), (190, 212, 230), (240, 246, 252)),
    "cassiterite": ((24, 16, 12), (56, 38, 28), (94, 68, 48), (176, 152, 122)),
    "bastnasite": ((112, 66, 26), (164, 110, 44), (200, 152, 76), (240, 212, 144)),
    "monazite": ((92, 40, 22), (146, 70, 36), (186, 106, 56), (230, 162, 108)),
    "xenotime": ((82, 72, 36), (126, 116, 60), (164, 154, 90), (216, 210, 154)),
    "loparite": ((14, 14, 18), (36, 36, 44), (72, 74, 88), (160, 164, 180)),
    "euxenite": ((20, 14, 8), (52, 38, 20), (96, 78, 38), (176, 158, 92)),
    "thortveitite": ((22, 32, 26), (52, 70, 58), (96, 118, 100), (162, 180, 162)),
    "native_silver": ((120, 126, 136), (176, 182, 192), (214, 220, 228), (250, 252, 255)),
    "argentite": ((18, 20, 26), (44, 48, 58), (78, 84, 98), (144, 152, 168)),
    "sperrylite": ((126, 132, 140), (182, 188, 196), (222, 226, 232), (255, 255, 255)),
    "cooperite": ((62, 66, 74), (104, 110, 120), (146, 152, 162), (204, 210, 218)),
    "braggite": ((88, 88, 82), (138, 138, 128), (182, 182, 170), (228, 228, 218)),
    "cinnabar": ((104, 16, 20), (160, 30, 30), (202, 58, 46), (240, 132, 112)),
    # ore spodumene is white to grey-green; kunzite's pink and hiddenite's green are gem rarities
    "spodumene": ((98, 106, 94), (162, 170, 156), (210, 216, 202), (244, 246, 238)),
    "fluorite": ((58, 32, 104), (112, 72, 168), (164, 128, 212), (224, 204, 244)),
    "borax": ((146, 142, 134), (198, 196, 190), (232, 232, 228), (252, 252, 250)),
    "trona": ((150, 140, 116), (204, 196, 174), (230, 224, 206), (250, 246, 234)),
    # colourless to the pale pink and orange that haloarchaea and a trace of iron give rock salt
    "halite": ((168, 128, 116), (216, 184, 172), (238, 218, 208), (255, 250, 246)),
    # zircon grains are honey-brown to reddish-brown, the colour radiation damage from their own uranium gives them
    "zircon": ((74, 40, 16), (132, 80, 36), (180, 126, 66), (232, 196, 140)),
    # beryl is the pale blue-green of aquamarine; bertrandite colourless to white, in a pale tuff
    "beryl": ((54, 96, 82), (96, 148, 128), (148, 196, 174), (218, 242, 230)),
    "bertrandite": ((148, 146, 138), (196, 196, 188), (226, 226, 220), (252, 252, 250)),
}

# Bornite tarnishes iridescent ("peacock ore"): patches of several hues on one lump.
BORNITE = [((60, 34, 92), (104, 62, 150), (150, 104, 190), (200, 164, 226)),
           ((26, 54, 112), (46, 92, 166), (90, 142, 206), (164, 200, 238)),
           ((104, 62, 34), (160, 104, 60), (200, 148, 94), (236, 196, 146)),
           ((24, 90, 96), (44, 132, 132), (96, 180, 170), (170, 224, 214))]

# name: [elements in paint order]. Every recipe spreads five to eight clusters across the whole
# tile; what differs is the habit. See the element painters in build().
#   ("blob", count, min, max)        rounded lumps of that many pixels
#   ("smear", count, min, max)       ragged, branching patches (sooty or wiry minerals)
#   ("banded", count, min, max)      lumps with concentric colour bands
#   ("crystals", kind, groups, n)    groups of n intergrown cubes / prisms / plates / knobs
#   ("speck", count)                 single grains
#   ("companion", mineral, count)    grains of a mineral that always comes with this one
#   ("laminae", count)               thin dark layers (heavy-mineral sands)
RECIPES = {
    # --- iron, ferroalloys ---
    "hematite": [("blob", 6, 4, 8), ("speck", 5)],
    "magnetite": [("crystals", "cube", 5, 1), ("blob", 2, 3, 5), ("speck", 4)],
    "pyrolusite": [("smear", 4, 6, 10), ("speck", 6)],
    "pentlandite": [("blob", 5, 4, 7), ("companion", "pyrrhotite", 4), ("speck", 3)],
    "chromite": [("blob", 5, 4, 8), ("speck", 7)],
    "wolframite": [("crystals", "plate", 5, 1), ("speck", 3)],
    "scheelite": [("blob", 6, 4, 7), ("speck", 4)],
    "molybdenite": [("crystals", "plate", 5, 1), ("speck", 4)],
    # Tin-white cubes with pink "cobalt bloom" (erythrite) wherever they weather.
    "cobaltite": [("crystals", "cube", 4, 1), ("companion", "erythrite", 5), ("speck", 2)],
    # Mineral sands: the heavy minerals are the dark layers in a beach.
    "ilmenite": [("laminae", 4), ("blob", 4, 3, 5), ("speck", 6)],
    "rutile": [("laminae", 1), ("blob", 5, 3, 5), ("speck", 8)],
    # --- copper ---
    "chalcopyrite": [("blob", 6, 4, 7), ("speck", 5)],
    "bornite": [("blob", 6, 4, 8), ("speck", 4)],
    "chalcocite": [("smear", 4, 6, 9), ("speck", 5)],
    "covellite": [("crystals", "plate", 5, 1), ("speck", 3)],
    "malachite": [("banded", 5, 6, 10), ("speck", 3)],
    # Azurite alters to malachite, so the two are found together.
    "azurite": [("blob", 5, 4, 8), ("companion", "malachite", 4), ("speck", 2)],
    # Cuprite forms on native copper in the oxidised zone.
    "cuprite": [("blob", 5, 4, 7), ("companion", "native_copper", 3), ("speck", 3)],
    # --- lead, zinc, tin ---
    "galena": [("crystals", "cube", 5, 1), ("blob", 2, 3, 5), ("speck", 4)],
    "sphalerite": [("blob", 6, 4, 8), ("speck", 4)],
    "smithsonite": [("crystals", "knob", 5, 1), ("speck", 4)],
    "hemimorphite": [("crystals", "knob", 4, 1), ("blob", 2, 3, 4), ("speck", 3)],
    "cassiterite": [("crystals", "prism", 5, 1), ("speck", 4)],
    # --- rare earths ---
    "bastnasite": [("blob", 6, 4, 8), ("speck", 5)],
    "monazite": [("laminae", 2), ("blob", 4, 3, 5), ("speck", 8)],
    "xenotime": [("crystals", "prism", 4, 1), ("speck", 4)],
    "loparite": [("crystals", "cube", 5, 1), ("speck", 4)],
    "euxenite": [("blob", 5, 4, 7), ("speck", 4)],
    # Greyish-green to greenish-black prisms, a few in a whole pegmatite.
    "thortveitite": [("crystals", "prism", 3, 1), ("speck", 3)],
    # --- precious ---
    "native_silver": [("smear", 5, 5, 8), ("speck", 5)],
    "argentite": [("blob", 5, 4, 7), ("companion", "native_silver", 3), ("speck", 3)],
    # PGM minerals are tiny bright grains, never masses: even the core is mostly rock.
    "sperrylite": [("crystals", "cube", 2, 1), ("speck", 6)],
    "cooperite": [("blob", 2, 2, 4), ("speck", 7)],
    "braggite": [("blob", 2, 2, 4), ("companion", "pentlandite", 3), ("speck", 5)],
    "cinnabar": [("blob", 6, 4, 7), ("speck", 5)],
    # --- lithium ---
    # Pale laths, sometimes a foot long, in the coarse granite of a pegmatite.
    "spodumene": [("crystals", "prism", 5, 1), ("speck", 3)],
    "fluorite": [("crystals", "prism", 4, 1), ("speck", 2)],
    "borax": [("crystals", "prism", 3, 1), ("speck", 3)],
    # Fibrous, columnar beds a few feet thick under the Green River basin.
    "trona": [("crystals", "prism", 4, 1), ("speck", 2)],
    # Glassy cubes, the habit every child grows from a salt solution.
    "halite": [("crystals", "cube", 5, 1), ("speck", 3)],
    # --- zirconium, beryllium ---
    # Stubby square prisms, washed out of granite into the sands as rounded grains.
    "zircon": [("crystals", "prism", 4, 1), ("speck", 7)],
    # Six-sided prisms, sometimes a metre long, in the coarse quartz and feldspar of a pegmatite.
    "beryl": [("crystals", "prism", 4, 1), ("speck", 3)],
    # Tiny tablets in the fluorite nodules of a rhyolite tuff, as at Spor Mountain.
    "bertrandite": [("crystals", "plate", 3, 1), ("speck", 6)],
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
    if kind == "carbonatite":
        for _ in range(4):
            img.putpixel((rng.randrange(SIZE), rng.randrange(SIZE)), (132, 96, 62))
    return img


class Canvas:
    def __init__(self, name, variant=0, grade="edge"):
        self.rng = random.Random(seed(name, variant, grade))
        self.img = Image.new("RGB", (SIZE, SIZE), CLEAR)
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
        """Key out everything that is not mineral."""
        rgba = self.img.convert("RGBA")
        for x in range(SIZE):
            for y in range(SIZE):
                if self.img.getpixel((x, y)) == CLEAR:
                    rgba.putpixel((x, y), (0, 0, 0, 0))
        return rgba


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
        s = rng.choice((2, 2, 3, 3))
        return rect(x0, y0, s, s)
    if kind == "prism":
        return rect(x0, y0, 2, rng.randint(3, 5)) if rng.random() < 0.7 else rect(x0, y0, rng.randint(3, 4), 2)
    if kind == "plate":
        return rect(x0, y0, rng.randint(3, 4), rng.choice((1, 2)))
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
    """A wandering line across the tile that meets itself at the edges, so it tiles."""
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


def build(name, variant=0, grade="edge"):
    c = Canvas(name, variant, grade)
    rng, pal = c.rng, P[name] if name != "bornite" else None
    kept = [scaled(e, GRADES[grade]) for e in RECIPES[name]]
    if not any(e and e[0] in ("banded", "smear", "blob", "crystals") for e in kept):
        kept.insert(0, ("blob", 1, 2, 3))  # even a trace shows some mineral
    for element in filter(None, kept):
        kind = element[0]
        if kind == "laminae":
            for y in rng.sample(range(SIZE), element[1]):
                for x in range(SIZE):
                    if rng.random() < 0.7:
                        p = wrap(x, y + (rng.random() < 0.2))
                        c.img.putpixel(p, P["ilmenite"][rng.choice((1, 1, 2))])
                        c.ore.add(p)
        elif kind == "banded":
            for _ in range(element[1]):
                size = rng.randint(element[2], element[3])
                paint_banded(c, c.place(lambda: c.grow(size, compact=1.5), 1), pal)
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
                cells = c.place(lambda: c.grow(size), 1)
                tone = rng.choice(BORNITE)[rng.choice((1, 2))] if name == "bornite" else pal[rng.choice((1, 2))]
                for p in cells:
                    c.img.putpixel(p, tone)
                c.ore |= cells
        elif kind == "companion":
            other = P[element[1]]
            for _ in range(element[2]):
                size = rng.choice((1, 2, 2, 3))
                c.paint(c.place(lambda: c.grow(size), 1), other, glint=False, grain=0)
        elif kind == "crystals":
            for _ in range(element[2]):
                for part in aggregate(c, element[1], element[3]):
                    c.paint(part, pal, grain=0.1)  # each crystal shaded on its own, so faces read
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


def paint_clay(variant=0):
    """Ion-adsorption clay: weathered granite regolith — no ore grains, the REEs sit on the clay."""
    rng = random.Random(seed("ion_adsorption_clay", variant))
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


def paint_bauxite(variant=0):
    """Bauxite is a rock, not a grain in stone: red-brown laterite studded with pisoliths."""
    rng = random.Random(seed("bauxite", variant))
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


def paint_goethite(variant=0):
    """Earthy ochre iron hydroxide with dark glossy botryoidal crusts (bog iron, gossan caps)."""
    rng = random.Random(seed("goethite", variant))
    tones = [(110, 74, 22), (146, 102, 30), (178, 132, 44), (204, 162, 64)]
    img = Image.new("RGB", (SIZE, SIZE))
    for p, v in field(rng, 1, 1).items():
        img.putpixel(p, tones[min(3, int(v * 4))])
    c = Canvas("goethite", variant)
    c.rng, c.img = rng, img
    crust = ((20, 12, 8), (52, 32, 16), (92, 60, 26), (150, 110, 60))
    for part in aggregate(c, "knob", 4) + aggregate(c, "knob", 2):
        c.paint(part, crust, grain=0.1)
    return c.finish()


def paint_nickel_laterite(variant=0):
    """Tropical weathering profile: rusty limonite cut by apple-green garnierite veinlets."""
    rng = random.Random(seed("nickel_laterite", variant))
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
    """name -> grade -> [texture per variant]."""
    whole = {"ion_adsorption_clay": paint_clay, "bauxite": paint_bauxite, "goethite": paint_goethite,
             "nickel_laterite": paint_nickel_laterite}
    assert set(whole) == set(WHOLE)
    out = {name: {g: [build(name, v, g) for v in range(VARIANTS)] for g in GRADES} for name in RECIPES}
    # Whole rocks have no "amount of mineral" to vary; their grades share one look.
    out.update({name: {g: [paint(v).convert("RGBA") for v in range(VARIANTS)] for g in GRADES}
                for name, paint in whole.items()})
    return out


# Raw chunk colours for the whole-rock ores, which have no entry in P.
RAW_WHOLE = {
    "bauxite": ((96, 44, 28), (150, 78, 48), (196, 132, 88), (236, 208, 168)),
    "goethite": ((84, 56, 18), (140, 98, 30), (186, 140, 50), (226, 190, 96)),
    "nickel_laterite": ((92, 62, 28), (150, 108, 46), (136, 176, 92), (190, 222, 140)),
    "ion_adsorption_clay": ((120, 66, 42), (168, 102, 64), (202, 142, 96), (236, 214, 188)),
}


def ramp(pal):
    """Eight tones, dark outline to highlight, from a four-tone palette."""
    shadow, base, light, glint = pal
    mix = lambda a, b, t: tuple(int(x + (y - x) * t) for x, y in zip(a, b))
    return [mix((0, 0, 0), shadow, 0.5), mix((0, 0, 0), shadow, 0.78), shadow, mix(shadow, base, 0.5), base,
            mix(base, light, 0.5), light, mix(light, glint, 0.6)]


def paint_raw(name, palette=None):
    """A raw chunk in the idiom of vanilla's raw ores: one big lump and a smaller one in front,
    lit from the top left, in broad facets with a dark rim along the bottom."""
    rng = random.Random("raw-" + name)
    tones = ramp(palette or RAW_WHOLE.get(name) or (P[name] if name != "bornite" else BORNITE[rng.randrange(4)]))
    # Two overlapping rounded lumps; the sizes and the overlap vary by mineral.
    cx, cy, rx, ry = 7.0 + rng.uniform(-0.6, 0.6), 7.0 + rng.uniform(-0.5, 0.5), rng.uniform(5.2, 6.0), rng.uniform(4.2, 5.0)
    sx, sy, sr = cx + rng.uniform(2.6, 3.6), cy + rng.uniform(2.6, 3.4), rng.uniform(2.6, 3.2)
    def inside(x, y):
        return ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1 or (x - sx) ** 2 + (y - sy) ** 2 <= sr ** 2
    cells = {(x, y) for x in range(16) for y in range(16) if inside(x + 0.5 + rng.uniform(-0.25, 0.25), y + 0.5)}
    cells = {c for c in cells if 1 <= c[0] <= 14 and 1 <= c[1] <= 14}
    facet = {(fx, fy): rng.uniform(-0.9, 0.9) for fx in range(8) for fy in range(8)}  # 2x2 planes
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for x, y in cells:
        has = lambda dx, dy: (x + dx, y + dy) in cells
        small = (x - sx) ** 2 + (y - sy) ** 2 <= sr ** 2
        ox, oy, r = (sx, sy, sr) if small else (cx, cy, max(rx, ry))
        light = 4.2 - ((x - ox) + (y - oy)) / r * 2.3 + facet[(x // 2, y // 2)]
        tone = max(2, min(7, int(round(light))))
        if not has(0, 1) or not has(1, 0):
            tone = 0 if not has(0, 1) and not has(1, 0) else 1   # rim in shadow
        elif not has(0, -1) or not has(-1, 0):
            tone = min(tone, 5) if rng.random() < 0.5 else max(tone - 1, 3)  # lit rim, not blown out
        elif small and not ((x - 1 - sx) ** 2 + (y - 1 - sy) ** 2 <= sr ** 2):
            tone = 2  # the crease where the small lump sits in front of the big one
        img.putpixel((x, y), tones[tone] + (255,))
    return img


def paint_rocks():
    return {name: host_rock(random.Random("host-" + name), name) for name in ROCK_BLOCKS}


SHEET_ORDER = [
    "bastnasite", "monazite", "xenotime", "ion_adsorption_clay", "loparite", "euxenite", "thortveitite",
    "chalcopyrite", "bornite", "chalcocite", "covellite", "malachite", "azurite", "cuprite",
    "bauxite", "galena", "sphalerite", "smithsonite", "hemimorphite", "cassiterite",
    "hematite", "magnetite", "goethite", "pyrolusite", "pentlandite", "nickel_laterite",
    "chromite", "wolframite", "scheelite", "molybdenite", "cobaltite", "ilmenite", "rutile",
    "native_silver", "argentite", "sperrylite", "cooperite", "braggite", "cinnabar", "spodumene", "fluorite", "borax", "trona", "halite",
    "zircon", "beryl", "bertrandite",
]


def vanilla(block):
    """The game's own texture, for previews only (never written into the mod)."""
    import glob, io, zipfile
    jars = glob.glob(str(Path.home() / ".gradle/caches/fabric-loom/*/minecraft-client.jar"))
    if not jars:
        return Image.new("RGBA", (SIZE, SIZE), {"deepslate": (72, 72, 76, 255)}.get(block, (126, 126, 126, 255)))
    with zipfile.ZipFile(jars[0]) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/block/{block}.png"))).convert("RGBA")


def contact_sheet(textures, path, cols=12, scale=8):
    """Every ore as core / edge / trace, previewed over vanilla stone."""
    stone = vanilla("stone")
    tiles = [(name, img.convert("RGBA")) for name, img in paint_rocks().items()]
    while len(tiles) % cols:
        tiles.append(None)
    for name in SHEET_ORDER:
        for grade in GRADES:
            img = textures[name][grade][0]
            tiles.append((f"{name} {grade}" if grade == "core" else grade,
                          img if name in WHOLE else Image.alpha_composite(stone, img)))
    tile, pad, label = SIZE * scale, 10, 22
    rows = -(-len(tiles) // cols)
    sheet = Image.new("RGB", (cols * (tile + pad) + pad, rows * (tile + pad + label) + pad), (32, 34, 38))
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default(size=12)
    for i, entry in enumerate(tiles):
        if not entry:
            continue
        x = pad + (i % cols) * (tile + pad)
        y = pad + (i // cols) * (tile + pad + label)
        sheet.paste(entry[1].convert("RGB").resize((tile, tile), Image.NEAREST), (x, y))
        draw.text((x, y + tile + 3), entry[0].replace("_", " "), fill=(226, 228, 232), font=font)
    sheet.save(path)


if __name__ == "__main__":
    textures = paint_all()
    OUT.mkdir(parents=True, exist_ok=True)
    for stale in OUT.glob("*_ore_*.png"):  # only our own: other painters write here too
        stale.unlink()
    for name, grades in textures.items():
        for grade, variants in grades.items():
            for v, img in enumerate(variants):
                img.save(OUT / f"{name}_ore_{grade}_{v}.png")
    rocks = paint_rocks()
    for name, img in rocks.items():
        img.save(OUT / f"{name}.png")
    (OUT.parent / "item").mkdir(exist_ok=True)
    for name in textures:
        paint_raw(name).save(OUT.parent / "item" / f"raw_{name}.png")
    print(f"wrote {len(textures)} ores x {len(GRADES)} grades x {VARIANTS} variants and {len(rocks)} rocks to {OUT}")
    if len(sys.argv) > 1:
        contact_sheet(textures, sys.argv[1])
        print(f"wrote contact sheet {sys.argv[1]}")
