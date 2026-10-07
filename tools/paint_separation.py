#!/usr/bin/env python3
"""Paints the mixer-settler casing, the one fluid texture every reagent is tinted from, the mixer's whisk,
and the salt and oxalic acid. Edit and re-run; don't hand-edit the PNGs.

The casing is Create's fluid tank in dark steel: its riveted panels, connected-texture sheet, inner wall
and window are Create's own (MIT) textures recoloured by luminance, so the vat reads exactly like a
Create tank and its connected textures line up with Create's sheet layout.

    python3 tools/paint_separation.py
"""
import random
import zipfile
from pathlib import Path

from PIL import Image

TEXTURES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/fundamentals/textures"
CREATE_JAR = next(Path.home().glob(".gradle/caches/modules-2/files-2.1/maven.modrinth/create/*/*/create-*.jar"))

# Dark steel, from the shadow in a seam to the glint on a rivet.
STEEL = [(22, 24, 30), (34, 37, 44), (46, 50, 58), (58, 63, 72), (74, 80, 90), (96, 103, 114), (126, 134, 146), (160, 168, 180)]
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


def rim():
    img = Image.new("RGBA", (16, 16))
    rng = random.Random("rim")
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), STEEL[5 if rng.random() < 0.7 else 4] + (255,))
    return img


def motor():
    """The drive on the mixer box: a dark housing with a copper band and a lighter cap."""
    img = Image.new("RGBA", (16, 16))
    rng = random.Random("motor")
    for y in range(16):
        for x in range(16):
            tone = STEEL[3 if rng.random() < 0.75 else 2]
            if y < 2:
                tone = STEEL[6]
            elif 6 <= y <= 7:
                tone = COPPER[2 if y == 6 else 1]
            elif y == 15 or x in (0, 15):
                tone = STEEL[1]
            img.putpixel((x, y), tone + (255,))
    return img


def whisk():
    """A cross of flat blades on a shaft, drawn as the game's mixer head is: a 16x16 with the blade at left."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(11):
            if y >= 2 and (x in (0, 10) or y in (2, 15) or (x + y) % 4 == 0):
                img.putpixel((x, y), STEEL[6 if (x + y) % 4 == 0 else 4] + (255,))
            elif y >= 2:
                img.putpixel((x, y), STEEL[3] + (255,))
        for x in range(12, 14):
            img.putpixel((x, y), STEEL[5 if x == 12 else 3] + (255,))
    return img


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


def main():
    (TEXTURES / "block/fluid").mkdir(parents=True, exist_ok=True)
    (TEXTURES / "item").mkdir(parents=True, exist_ok=True)
    steel(create_texture("fluid_tank")).save(TEXTURES / "block/mixer_settler_side.png")
    steel(create_texture("fluid_tank_connected")).save(TEXTURES / "block/mixer_settler_side_connected.png")
    steel(create_texture("fluid_tank_inner")).save(TEXTURES / "block/mixer_settler_inside.png")
    steel(create_texture("fluid_tank_window")).save(TEXTURES / "block/mixer_settler_window.png")
    rim().save(TEXTURES / "block/mixer_settler_rim.png")
    motor().save(TEXTURES / "block/mixer_settler_motor.png")
    whisk().save(TEXTURES / "block/mixer_settler_whisk.png")
    liquor("still").save(TEXTURES / "block/fluid/liquor_still.png")
    liquor("flow", flow=True).save(TEXTURES / "block/fluid/liquor_flow.png")
    heap("salt", (255, 255, 255), (232, 234, 236), (176, 180, 186)).save(TEXTURES / "item/salt.png")
    heap("oxalic", (255, 255, 252), (238, 236, 224), (184, 180, 160)).save(TEXTURES / "item/oxalic_acid.png")
    print("separation textures written")


if __name__ == "__main__":
    main()
