#!/usr/bin/env python3
import math
import random

from PIL import Image
from common import TEXTURES

OUT = TEXTURES / "block"

SWEEP = 240
CENTRE = 16.0
FACE = (236, 230, 212)
INK = (40, 38, 36)
ANDESITE = [(62, 66, 60), (98, 104, 96), (140, 146, 134), (178, 184, 170), (206, 210, 198)]
WHITE_LEG = (250, 250, 248)
BURST = (200, 40, 32)
SPIRIT = (232, 24, 24)

GAUGES = {
    "mercury_thermometer": ((150, 158, 174), "column"),
    "spirit_thermometer": ((180, 200, 206), "spirit"),
    "bimetallic_thermometer": ((214, 168, 70), "coil"),
    "type_k_thermocouple": ((40, 150, 70), "legs"),
    "type_s_thermocouple": ((236, 122, 26), "legs"),
}


def shade(colour, t):
    return tuple(max(0, min(255, int(c * t))) for c in colour)


def polar(x, y):
    dx, dy = x + 0.5 - CENTRE, y + 0.5 - CENTRE
    return math.hypot(dx, dy), math.degrees(math.atan2(dx, -dy))


def dial(name):
    band, mark = GAUGES[name]
    img = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            r, a = polar(x, y)
            if r > 10.2:
                colour = ANDESITE[0]
            elif r > 9.3:
                colour = shade(FACE, 0.72)
            elif r > 7.3 and abs(a) <= SWEEP / 2 + 2:
                colour = BURST if mark in ("column", "spirit") and a > SWEEP / 2 - 22 else shade(band, 1.1 if r > 8.3 else 0.9)
            else:
                colour = FACE
            img.putpixel((x, y), colour + (255,))
    for step in range(9):
        a = math.radians(-SWEEP / 2 + SWEEP * step / 8)
        for r in ((5.0, 5.8, 6.6) if step % 2 == 0 else (6.6,)):
            img.putpixel((int(CENTRE + r * math.sin(a)), int(CENTRE - r * math.cos(a))), INK + (255,))
    {"column": column, "spirit": spirit, "coil": coil, "legs": legs}[mark](img, band)
    return img


def tube(img, dark, light, bulb):
    glass = (96, 116, 128)
    for y in range(18, 24):
        img.putpixel((14, y), glass + (255,))
        img.putpixel((17, y), glass + (255,))
        img.putpixel((15, y), (dark if y > 19 else (214, 230, 236)) + (255,))
        img.putpixel((16, y), (light if y > 19 else (214, 230, 236)) + (255,))
    for x in (14, 15, 16, 17):
        img.putpixel((x, 24), (bulb if x in (15, 16) else glass) + (255,))


def column(img, colour):
    tube(img, shade(colour, 0.55), shade(colour, 0.85), shade(colour, 0.6))


def spirit(img, colour):
    tube(img, shade(SPIRIT, 0.8), SPIRIT, shade(SPIRIT, 0.8))


def coil(img, colour):
    steel = (96, 100, 108)
    spiral = [(16, 21), (17, 21), (17, 22), (16, 23), (15, 23), (14, 22), (14, 21), (15, 19), (16, 19), (17, 19), (18, 20),
              (19, 21), (19, 22), (18, 24), (17, 24), (16, 24), (15, 24), (14, 24), (13, 23)]
    for i, (x, y) in enumerate(spiral):
        img.putpixel((x, y), (shade(colour, 0.8) if i % 2 else steel) + (255,))


def legs(img, colour):
    for i in range(6):
        for dx in (0, 1):
            img.putpixel((12 + dx + i // 2, 24 - i), shade(colour, 0.85 if dx else 1.0) + (255,))
            img.putpixel((18 + dx - i // 2, 24 - i), shade(WHITE_LEG, 0.8 if dx else 1.0) + (255,))
    for x in (15, 16):
        img.putpixel((x, 18), INK + (255,))


def casing():
    rng = random.Random("thermometer-casing")
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if y == 0 or x == 0:
                tone = 4
            elif y == 15 or x == 15:
                tone = 0
            elif y in (1, 14) or x in (1, 14):
                tone = 3 if y == 1 or x == 1 else 1
            else:
                tone = 2 if rng.random() < 0.8 else 1
            img.putpixel((x, y), ANDESITE[tone] + (255,))
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        img.putpixel((x, y), ANDESITE[4] + (255,))
        img.putpixel((x + 1, y + 1), ANDESITE[0] + (255,))
    return img


def needle():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), ((196, 34, 28) if x < 8 else (36, 34, 34)) + (255,))
    return img


def main():
    casing().save(OUT / "thermometer_casing.png")
    needle().save(OUT / "thermometer_needle.png")
    for name in GAUGES:
        dial(name).save(OUT / f"{name}_dial.png")
    print(f"wrote {len(GAUGES)} thermometer dials")


if __name__ == "__main__":
    main()
