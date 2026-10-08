#!/usr/bin/env python3
"""Paints the mixer-settler casing, the one fluid texture every reagent is tinted from, and the plant's
salts. Edit and re-run; don't hand-edit the PNGs.

The casing is a welded polypropylene tank, as the real ones are: Create's fluid-tank panel and connected-texture
sheet (MIT) recoloured by luminance onto a dark flat PP grey, so the frame ribs land on the exterior edges of a stage
the way Create's connected textures place them.

    python3 tools/paint_separation.py
"""
import random
import zipfile
from pathlib import Path

from PIL import Image

TEXTURES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/fundamentals/textures"
CREATE_JAR = next(Path.home().glob(".gradle/caches/modules-2/files-2.1/maven.modrinth/create/*/*/create-*.jar"))

# Dark steel, from the shadow in a seam to the glint on a rivet.
# Welded polypropylene sheet, as the real tanks are: dark, flat, a little blue, the frame ribs a shade lighter.
STEEL = [(30, 33, 38), (40, 44, 50), (48, 52, 59), (56, 61, 68), (66, 72, 80), (84, 91, 100), (104, 112, 122), (128, 136, 146)]
COPPER = [(120, 62, 44), (172, 96, 66), (212, 136, 98), (244, 190, 156)]


def create_texture(name):
    with zipfile.ZipFile(CREATE_JAR) as jar:
        with jar.open(f"assets/create/textures/block/{name}.png") as f:
            return Image.open(f).convert("RGBA").copy()


def steel(img):
    """Recolour by luminance onto the steel ramp; transparent pixels (the window's glass) stay as they are."""
    out = Image.new("RGBA", img.size)
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a < 255:
                out.putpixel((x, y), (r, g, b, a))
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
            t = min(1.0, max(0.0, (lum - 0.25) / 0.6))
            i = t * (len(STEEL) - 1)
            lo, hi = STEEL[int(i)], STEEL[min(len(STEEL) - 1, int(i) + 1)]
            k = i - int(i)
            out.putpixel((x, y), tuple(int(lo[c] + (hi[c] - lo[c]) * k) for c in range(3)) + (255,))
    return out


def liquor(seed, flow=False):
    """A still liquid: pale ripples on white, tinted per fluid by the game. The flow texture is the same
    at twice the height so pipes can scroll it."""
    rng = random.Random(seed)
    h = 32 if flow else 16
    img = Image.new("RGBA", (16, h))
    for y in range(h):
        for x in range(16):
            ripple = (x + y * 2 + rng.randint(0, 1)) % 7 in (0, 1)
            v = 196 if ripple else 232 if rng.random() < 0.8 else 214
            img.putpixel((x, y), (v, v, v, 230))
    return img


def ingot(pal):
    """The calcium ingot, drawn with the materials painter's ingot shape in a dull grey."""
    from paint_materials import SHAPES, ramp
    tones = ramp(pal)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(SHAPES["ingot"]):
        for x, tone in enumerate(row):
            if tone != ".":
                img.putpixel((x, y), tones[int(tone)] + (255,))
    return img


def nozzle():
    """A copper port flange: a ring with a dark bore, on a transparent ground."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if r < 2.2:
                img.putpixel((x, y), (24, 20, 18, 255))
            elif r < 4.5:
                img.putpixel((x, y), COPPER[2 if y < 8 else 1] + (255,))
            elif r < 5.5:
                img.putpixel((x, y), COPPER[0] + (255,))
    return img


def heap(seed, highlight, body, shadow):
    """A small heap of crystals, as salt and oxalic acid both are."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    profile = [0, 0, 0, 0, 0, 1, 2, 3, 4, 4, 3, 2, 1, 0, 0, 0]
    for x in range(16):
        top = 11 - profile[x] - (1 if 3 <= x <= 12 else 0) - (1 if 5 <= x <= 10 else 0)
        if not 2 <= x <= 13:
            continue
        for y in range(top, 13):
            glint = rng.random() < 0.18 and y > top
            colour = highlight if y == top or glint else shadow if y >= 12 or (x in (2, 13)) else body
            img.putpixel((x, y), colour + (255,))
    return img


def sticks(highlight, body, shadow):
    """Two cast sticks lying across each other, as white phosphorus is sold (and kept under water)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x0, y0, length in ((1, 11, 11), (5, 14, 10)):
        for i in range(length):
            for w, colour in ((-1, highlight), (0, body), (1, shadow)):
                x, y = x0 + i, y0 - i + w
                if 0 <= x < 16 and 0 <= y < 16:
                    img.putpixel((x, y), colour + (255,))
        img.putpixel((x0, y0 + 1), shadow + (255,))
    return img


def main():
    (TEXTURES / "block/fluid").mkdir(parents=True, exist_ok=True)
    (TEXTURES / "item").mkdir(parents=True, exist_ok=True)
    steel(create_texture("fluid_tank")).save(TEXTURES / "block/mixer_settler_side.png")
    steel(create_texture("fluid_tank_connected")).save(TEXTURES / "block/mixer_settler_side_connected.png")
    steel(create_texture("fluid_tank_window")).save(TEXTURES / "block/mixer_settler_window.png")
    steel(create_texture("fluid_tank_window")).save(TEXTURES / "block/mixer_settler_window.png")
    steel(create_texture("fluid_tank_top")).save(TEXTURES / "block/mixer_settler_top.png")
    steel(create_texture("fluid_tank_top_connected")).save(TEXTURES / "block/mixer_settler_top_connected.png")
    liquor("still").save(TEXTURES / "block/fluid/liquor_still.png")
    liquor("flow", flow=True).save(TEXTURES / "block/fluid/liquor_flow.png")
    heap("salt", (255, 255, 255), (232, 234, 236), (176, 180, 186)).save(TEXTURES / "item/salt.png")
    heap("oxalic", (255, 255, 252), (238, 236, 224), (184, 180, 160)).save(TEXTURES / "item/oxalic_acid.png")
    # roasting oxidises the cerium to CeO2, which turns the concentrate buff; the sulfates are white, the light one pinked by
    # its neodymium; calcium chloride is white
    heap("roasted_bastnasite", (240, 222, 178), (210, 184, 132), (150, 124, 82)).save(TEXTURES / "item/roasted_bastnasite.png")
    heap("light_sulfate", (250, 242, 242), (226, 214, 216), (170, 158, 162)).save(TEXTURES / "item/light_rare_earth_sulfate.png")
    heap("heavy_sulfate", (250, 248, 238), (228, 224, 208), (172, 168, 150)).save(TEXTURES / "item/heavy_rare_earth_sulfate.png")
    heap("calcium_chloride", (255, 255, 255), (240, 240, 236), (190, 190, 184)).save(TEXTURES / "item/calcium_chloride.png")
    sticks((252, 250, 232), (238, 232, 196), (196, 186, 136)).save(TEXTURES / "item/white_phosphorus.png")
    ingot(((88, 90, 94), (138, 141, 146), (180, 184, 190), (222, 226, 232))).save(TEXTURES / "item/calcium_ingot.png")
    nozzle().save(TEXTURES / "block/mixer_settler_nozzle.png")
    print("separation textures written")


if __name__ == "__main__":
    main()
