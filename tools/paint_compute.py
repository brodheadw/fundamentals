#!/usr/bin/env python3
"""Paints the fab's parts and the power supply."""
import math
from pathlib import Path

from PIL import Image

from common import TEXTURES as TEX
OUTLINE = (24, 24, 30)


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def mix(a, b, t):
    return clamp(a[i] + (b[i] - a[i]) * t for i in range(3))


def shade(c, t):
    return clamp(v * t for v in c)


def canvas():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), tuple(c[:3]) + (255,))


def outline(img):
    src = img.copy()
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3]:
                continue
            if any(0 <= x + dx < 16 and 0 <= y + dy < 16 and src.getpixel((x + dx, y + dy))[3]
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                put(img, x, y, OUTLINE)
    return img


# --- wafers ---------------------------------------------------------------
CX, CY, R = 7.5, 7.3, 6.9


def in_wafer(x, y):
    # A round wafer with its orientation flat along the bottom; a pixel is in if most of it is.
    inside = sum(math.hypot(x + (i + 0.5) / 4 - 0.5 - CX, y + (j + 0.5) / 4 - 0.5 - CY) <= R for i in range(4) for j in range(4))
    return inside >= 9 and y <= 13


def silicon(x, y, tint=(0, 0, 0)):
    # Polished silicon: blue-grey mirror with a diagonal sheen.
    sheen = max(0.0, 1 - abs((x - y) + 3) / 3.5)
    base = mix((92, 100, 120), (196, 206, 224), 0.25 + 0.6 * sheen)
    return clamp(base[i] + tint[i] for i in range(3))


def wafer(pattern=None, tint=(0, 0, 0), coat=None):
    img = canvas()
    for y in range(16):
        for x in range(16):
            if not in_wafer(x, y):
                continue
            c = silicon(x, y, tint)
            if pattern:
                c = pattern(x, y, c)
            if coat:
                c = mix(c, coat, 0.55)
            edge = any(not in_wafer(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            put(img, x, y, shade(c, 0.78) if edge else c)
    return outline(img)


def dies(pitch, colours, street=(70, 74, 86)):
    # A grid of dies separated by saw streets; each die alternates through thin-film interference colours.
    def pattern(x, y, c):
        if (x - 1) % pitch == 0 or (y - 1) % pitch == 0:
            return mix(c, street, 0.6)
        k = ((x - 1) // pitch + (y - 1) // pitch) % len(colours)
        sheen = max(0.0, 1 - abs((x - y) + 3) / 3.5)
        return mix(colours[k], (255, 255, 255), 0.35 * sheen)
    return pattern


DESIGNS = {
    # 10 um PMOS: big dies, amber and green oxide.
    "microcontroller": (4, [(176, 132, 52), (98, 150, 92)]),
    # DRAM: fine, regular, cold blue-violet arrays.
    "memory": (3, [(92, 84, 172), (64, 112, 182)]),
    # CMOS with aluminium: golden and rose dies.
    "microprocessor": (3, [(204, 166, 70), (178, 104, 120)]),
    # 90 nm copper: the whole wafer runs rainbow under the light.
    "advanced_processor": (2, [(200, 80, 150), (70, 160, 200), (120, 200, 90), (220, 170, 60)]),
}


# --- dies and packages ----------------------------------------------------
def die(design):
    pitch, colours = DESIGNS[design]
    img = canvas()
    for y in range(4, 12):
        for x in range(4, 12):
            ring = x in (4, 11) or y in (4, 11)
            pad = ring and (x + y) % 2 == 0
            if pad:
                c = (214, 214, 220)
            elif ring:
                c = (110, 116, 130)
            else:
                k = ((x - 5) // 2 + (y - 5) // 2) % len(colours)
                c = mix(colours[k], (255, 255, 255), 0.25 if (x + y) % 3 == 0 else 0)
            put(img, x, y, c)
    return outline(img)


def dip(length, notch=True):
    # Black epoxy dual in-line package, silver legs down both sides.
    img = canvas()
    top, bottom = 8 - length // 2, 8 + (length + 1) // 2 - 1
    for y in range(top, bottom + 1):
        for x in range(5, 11):
            c = (46, 46, 52) if x not in (5, 10) else (34, 34, 38)
            if y == top and x in (7, 8) and notch:
                c = (20, 20, 22)
            put(img, x, y, c)
        if (y - top) % 2 == 1 and top < y < bottom:
            for x in (3, 4, 11, 12):
                put(img, x, y, (206, 210, 216) if x in (4, 11) else (150, 154, 162))
    put(img, 6, bottom - 1, (120, 120, 128))
    return outline(img)


def pga():
    # Ceramic pin-grid package: tan ceramic, gold lid over the die.
    img = canvas()
    for y in range(2, 14):
        for x in range(2, 14):
            ceramic = (216, 200, 168) if (x + y) % 5 else (198, 182, 150)
            if 5 <= x <= 10 and 5 <= y <= 10:
                lid = mix((236, 196, 92), (255, 236, 160), 0.6 if x - y > 0 else 0.1)
                put(img, x, y, shade(lid, 0.8) if x in (5, 10) or y in (5, 10) else lid)
            elif (x in (3, 12) or y in (3, 12)) and (x + y) % 2 == 0:
                put(img, x, y, (190, 156, 70))
            else:
                put(img, x, y, ceramic)
    put(img, 2, 2, (150, 136, 108))
    return outline(img)


def flip_chip():
    # Organic green substrate with a nickel-plated heat spreader, the way 2004 processors shipped.
    img = canvas()
    for y in range(1, 15):
        for x in range(1, 15):
            if 4 <= x <= 11 and 4 <= y <= 11:
                sheen = max(0.0, 1 - abs((x - y)) / 4)
                c = mix((150, 156, 166), (232, 236, 242), sheen)
                if x in (4, 11) or y in (4, 11):
                    c = shade(c, 0.75)
                put(img, x, y, c)
            else:
                c = (40, 108, 64) if (x * 3 + y) % 7 else (54, 132, 80)
                if (x in (2, 13) or y in (2, 13)) and (x + y) % 2 == 0:
                    c = (204, 170, 72)
                put(img, x, y, c)
    put(img, 2, 2, (220, 186, 80))
    return outline(img)


# --- masks ----------------------------------------------------------------
def plate(glass, chrome, pattern=None, coat=None):
    img = canvas()
    for y in range(1, 15):
        for x in range(1, 15):
            edge = x in (1, 14) or y in (1, 14)
            if edge:
                c = shade(glass, 0.8)
            elif 3 <= x <= 12 and 3 <= y <= 12:
                c = chrome if pattern is None else (pattern(x, y) or glass)
            else:
                c = glass
            sheen = max(0.0, 1 - abs((x - y) - 4) / 2.5)
            c = mix(c, (255, 255, 255), 0.3 * sheen)
            if coat and not edge:
                c = mix(c, coat, 0.5)
            put(img, x, y, c)
    return outline(img)


CHROME = (66, 70, 78)


def mask_pattern(design):
    def p(x, y):
        if design == "microcontroller":
            # Fat 10 um traces: a bus across the middle and gates dropping from it.
            return CHROME if y in (7, 8) or (x in (4, 7, 10) and y >= 4 and y not in (5,)) or (y == 11 and x in range(3, 13)) else None
        if design == "memory":
            return CHROME if x % 2 == 1 and y % 3 != 0 else None
        if design == "microprocessor":
            return CHROME if (x + y) % 4 in (0, 1) and not (6 <= x <= 9 and 6 <= y <= 9) else None
        return CHROME if (x % 2 == 0) ^ (y % 2 == 0) else None
    return p


SODA_GLASS = (150, 196, 178)
QUARTZ = (206, 226, 236)


# --- displays -------------------------------------------------------------
def lcd(colour):
    img = canvas()
    for y in range(3, 13):
        for x in range(1, 15):
            if x in (1, 14) or y in (3, 12):
                c = (58, 62, 68)
            elif colour:
                c = [(196, 64, 64), (72, 180, 84), (70, 104, 210)][x % 3]
                c = mix(c, (20, 20, 24), 0.25 if y % 3 == 0 else 0)
            else:
                c = (146, 164, 120) if (x + y) % 6 else (128, 146, 104)
                if y in (6, 7) and 3 <= x <= 12 and x % 3 != 2:
                    c = (52, 62, 46)
            put(img, x, y, c)
    return outline(img)


def chunk(colour, light):
    img = canvas()
    shape = ["................", "................", "................", "......XXXX......", "....XXXXXXXX....",
             "...XXXXXXXXXX...", "...XXXXXXXXXXX..", "..XXXXXXXXXXXX..", "..XXXXXXXXXXXX..", "..XXXXXXXXXXXX..",
             "...XXXXXXXXXX...", "....XXXXXXXXX...", ".....XXXXXX.....", "................", "................", "................"]
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch == "X":
                t = 0.5 + 0.5 * max(0.0, 1 - math.hypot(x - 6, y - 6) / 6)
                put(img, x, y, mix(colour, light, t * 0.8))
    return outline(img)


def fused_quartz():
    img = canvas()
    for y in range(4, 12):
        for x in range(2, 14):
            sheen = max(0.0, 1 - abs((x - y) - 1) / 2.5)
            c = mix((196, 214, 226), (250, 252, 255), sheen)
            if y in (4, 11) or x in (2, 13):
                c = shade(c, 0.82)
            put(img, x, y, c)
    return outline(img)


def lead_frame():
    img = die("microcontroller")
    for y in range(3, 13):
        for x in (2, 3, 12, 13):
            if y % 2 == 1:
                put(img, x, y, (196, 120, 76))
    return outline(img)


# --- power supply ---------------------------------------------------------
def steel(x, y):
    c = (128, 132, 138) if (x * 7 + y * 3) % 11 else (116, 120, 126)
    if x in (0, 15) or y in (0, 15):
        c = (84, 88, 94)
    return c


def psu_side():
    img = canvas()
    for y in range(16):
        for x in range(16):
            c = steel(x, y)
            if 3 <= y <= 12 and x in (3, 5, 7, 9, 11) and 2 <= x <= 12:
                c = (40, 42, 46)  # vent slots
            put(img, x, y, c)
    return img


def psu_front(on):
    img = canvas()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 6.5)
            c = steel(x, y)
            if r <= 5.8:
                # Seven swept blades behind a wire guard of concentric rings.
                angle = math.degrees(math.atan2(y - 6.5, x - 7.5)) + r * 24
                c = (64, 66, 72) if (angle % (360 / 7)) < 24 else (26, 28, 32)
                if r < 1.6:
                    c = (92, 96, 104)
                if abs(r - 5.3) < 0.45 or abs(r - 3.2) < 0.4:
                    c = (172, 176, 182)
            put(img, x, y, c)
    for x in range(2, 7):
        for y in range(13, 15):
            put(img, x, y, (24, 24, 26))  # mains socket
    put(img, 3, 13, (180, 180, 186)), put(img, 5, 13, (180, 180, 186))
    led = (90, 240, 110) if on else (40, 70, 46)
    put(img, 12, 13, led), put(img, 13, 13, shade(led, 0.8))
    for x in (11, 12, 13, 14):
        put(img, x, 14, (204, 160, 52) if x % 2 else (60, 60, 64))  # rating plate
    return img


def psu_top():
    img = canvas()
    for y in range(16):
        for x in range(16):
            c = steel(x, y)
            if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
                c = (176, 180, 186)  # screws
            elif y in (1, 14) and 1 <= x <= 14:
                c = (100, 104, 110)  # folded seam
            put(img, x, y, c)
    return img


def save(img, name):
    path = TEX / name
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def main():
    save(wafer(), "item/compute/processing_wafer.png")
    save(wafer(tint=(-8, 14, -6)), "item/compute/epitaxial_wafer.png")
    for design, (pitch, colours) in DESIGNS.items():
        save(wafer(dies(pitch, colours)), f"item/compute/{design}_wafer.png")
        save(die(design), f"item/compute/{design}_die.png")
        save(plate(SODA_GLASS if design == "microcontroller" else QUARTZ, CHROME, mask_pattern(design)), f"item/compute/{design}_mask.png")
    save(dip(10), "item/compute/microcontroller.png")
    save(dip(8), "item/compute/memory_chip.png")
    save(pga(), "item/compute/microprocessor.png")
    save(flip_chip(), "item/compute/advanced_processor.png")
    save(plate(SODA_GLASS, CHROME), "item/compute/chrome_mask_blank.png")
    save(plate(QUARTZ, CHROME), "item/compute/quartz_mask_blank.png")
    save(plate(QUARTZ, CHROME, mask_pattern("memory"), coat=(214, 160, 40)), "item/compute/processing_mask.png")
    save(lead_frame(), "item/compute/packaging_chip.png")
    save(lcd(False), "item/compute/lcd_panel.png")
    save(lcd(True), "item/compute/colour_lcd_panel.png")
    save(chunk((150, 84, 24), (238, 186, 90)), "item/compute/novolac_resin.png")
    save(fused_quartz(), "item/compute/fused_quartz.png")
    save(psu_side(), "block/compute/power_supply_side.png")
    save(psu_top(), "block/compute/power_supply_top.png")
    save(psu_front(False), "block/compute/power_supply_front.png")
    save(psu_front(True), "block/compute/power_supply_front_on.png")


if __name__ == "__main__":
    main()
