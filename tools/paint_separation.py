#!/usr/bin/env python3
import random
import zipfile
from pathlib import Path

from PIL import Image
from common import TEXTURES

CREATE_JAR = next(Path.home().glob(".gradle/caches/modules-2/files-2.1/maven.modrinth/create/*/*/create-*.jar"))

STEEL = [(30, 33, 38), (40, 44, 50), (48, 52, 59), (56, 61, 68), (66, 72, 80), (84, 91, 100), (104, 112, 122), (128, 136, 146)]
PLASTIC = [(95, 97, 115), (108, 114, 127), (136, 142, 155), (152, 156, 168), (167, 169, 180), (180, 182, 193), (196, 201, 207), (216, 221, 225), (234, 236, 238)]
COPPER = [(120, 62, 44), (172, 96, 66), (212, 136, 98), (244, 190, 156)]
HDPE = ((210, 207, 198), (232, 230, 223))
SEE_THROUGH = 0.8


def create_texture(name):
    with zipfile.ZipFile(CREATE_JAR) as jar:
        with jar.open(f"assets/create/textures/block/{name}.png") as f:
            return Image.open(f).convert("RGBA").copy()


def steel(img, ramp=STEEL, floor=0.25, span=0.6):
    out = Image.new("RGBA", img.size)
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a < 255:
                out.putpixel((x, y), (r, g, b, a))
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
            t = min(1.0, max(0.0, (lum - floor) / span))
            i = t * (len(ramp) - 1)
            lo, hi = ramp[int(i)], ramp[min(len(ramp) - 1, int(i) + 1)]
            k = i - int(i)
            out.putpixel((x, y), tuple(int(lo[c] + (hi[c] - lo[c]) * k) for c in range(3)) + (255,))
    return out


def moulded(img, palette=None, boxes=()):
    out = img.copy()
    px = out.load()

    def lum(x, y):
        r, g, b, _ = px[x, y]
        return (0.299 * r + 0.587 * g + 0.114 * b) / 255

    def ours(x, y):
        r, g, b, a = px[x, y]
        return a == 255 and (palette is None or (r, g, b) in palette) and lum(x, y) > 0.06

    def region(x, y):
        return next((box for box in boxes if box[0] <= x < box[2] and box[1] <= y < box[3]), (0, 0, img.width, img.height))

    means = {}
    for y in range(img.height):
        for x in range(img.width):
            if ours(x, y):
                means.setdefault(region(x, y), []).append(lum(x, y))
    means = {box: max(values) for box, values in means.items()}
    paint = {}
    for y in range(img.height):
        for x in range(img.width):
            if ours(x, y):
                lift = max(-48, min(12, round((lum(x, y) - means[region(x, y)]) * 170) + 8))
                paint[x, y] = hdpe(x, y, "moulded", lift, 196 if lift > -8 else 220 if lift > -24 else 244)
    for xy, texel in paint.items():
        px[xy] = texel
    return out


def liquor(seed, flow=False):
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
    from paint_materials import SHAPES, ramp
    tones = ramp(pal)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(SHAPES["ingot"]):
        for x, tone in enumerate(row):
            if tone != ".":
                img.putpixel((x, y), tones[int(tone)] + (255,))
    return img


def nozzle():
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


def hdpe(x, y, seed, lift=0, alpha=196):
    rng = random.Random(f"{seed}:{x // 2},{y // 3}")
    fine = random.Random(f"{seed}:{x},{y}").random()
    t = 0.2 + 0.35 * rng.random() + 0.45 * fine
    base = tuple(round(lo + (hi - lo) * t) for lo, hi in zip(HDPE[0], HDPE[1]))
    return tuple(max(0, min(255, c + lift)) for c in base) + (solid(alpha),)


def solid(alpha):
    return round(255 - (255 - alpha) * SEE_THROUGH)


def wall_texel(x, y, seed):
    row = y % 8
    if row == 6:
        return hdpe(x, y, seed, 12, 232)
    if row == 7:
        return hdpe(x, y, seed, -28, 244)
    if row == 5:
        return hdpe(x, y, seed, -6, 210)
    return hdpe(x, y, seed)


def tank_wall(left, right, top, bottom, seed):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            r, g, b, a = wall_texel(x, y, seed)
            lift, alpha = 0, a
            if top and y == 0:
                lift, alpha = 8, solid(248)
            elif top and y == 1:
                lift, alpha = -4, solid(236)
            elif bottom and y == 14:
                lift, alpha = -10, solid(236)
            elif bottom and y == 15:
                lift, alpha = -24, solid(250)
            if left and x == 0 or right and x == 15:
                lift, alpha = min(lift, 0) - 16, max(alpha, solid(240))
            elif left and x == 1 or right and x == 14:
                lift, alpha = lift - 6, max(alpha, solid(218))
            img.putpixel((x, y), tuple(max(0, min(255, c + lift)) for c in (r, g, b)) + (alpha,))
    return img


def tank_lid(left, right, top, bottom, seed, cap=None, vent=None):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = min([16] + [x for f in (left,) if f] + [15 - x for f in (right,) if f] + [y for f in (top,) if f] + [15 - y for f in (bottom,) if f])
            lift = 6 if d >= 4 else (-18, -8, -1, 3)[d]
            if left and right and top and bottom:
                lift += round(5 * (1 - ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 / 10.6))
            img.putpixel((x, y), hdpe(x, y, seed, lift, 244 if d >= 2 else 250))
    mid = tuple((lo + hi) // 2 for lo, hi in zip(*HDPE))
    for centre, outer, inner in ((cap, 5, 3.6), (vent, 2.2, 1.2)):
        if centre is None:
            continue
        cx, cy = centre
        for y in range(16):
            for x in range(16):
                dx, dy = x - cx, y - cy
                r = (dx * dx + dy * dy) ** 0.5
                lit = dx + dy < 0
                if r < inner:
                    notch = inner > 2 and r > inner - 1.1 and (x + y) % 2 == 0
                    lift = -52 if notch else -34 if lit else -42
                elif r < outer:
                    lift = 18 if lit else -14
                elif r < outer + 1 and not lit:
                    lift = -22
                else:
                    continue
                img.putpixel((x, y), tuple(c + lift for c in mid) + (255,))
    return img


def ct_sheet(tile):
    sheet = Image.new("RGBA", (64, 64))
    for cy in range(4):
        for cx in range(4):
            sheet.paste(tile(cx in (0, 1), cx in (0, 3), cy in (0, 3), cy in (2, 3), f"{cx}{cy}"), (cx * 16, cy * 16))
    return sheet


def plastic_tank_sheets():
    def lid(left, right, top, bottom, seed):
        alone = left and right and top and bottom
        corner = left and top and not right and not bottom
        return tank_lid(left, right, top, bottom, seed, cap=(7.5, 7.5) if alone else (9.5, 9.5) if corner else None,
                        vent=(10, 10) if right and bottom and not left and not top else None)

    def inner(left, right, top, bottom, seed):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y), hdpe(x, y, "inner" + seed, -14, 200))
        return img

    window = Image.new("RGBA", (16, 16))
    single = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            window.putpixel((x, y), wall_texel(x, y + 4 if x < 8 else y, "window"))
            single.putpixel((x, y), wall_texel(x, y + 4, "single"))
    return {
        "": tank_wall(True, True, True, True, "03"),
        "_connected": ct_sheet(tank_wall),
        "_top": lid(True, True, True, True, "03"),
        "_top_connected": ct_sheet(lid),
        "_inner": inner(True, True, True, True, "03"),
        "_inner_connected": ct_sheet(inner),
        "_window": window,
        "_window_single": single,
    }


def cell_sheets():
    from paint_materials import METAL
    ndfeb = METAL["neodymium_iron_boron"]
    body = moulded(create_texture("fluid_tank"))
    sheets = {"side": body}
    magnet = body.copy()
    for y in range(3, 13):
        for x in range(3, 13):
            edge = ndfeb[1] if x == 3 or y == 3 else ndfeb[0] if x == 12 or y == 12 else ndfeb[1] if (x * 3 + y * 5) % 11 == 0 else ndfeb[0]
            magnet.putpixel((x, y), edge + (255,))
    sheets["magnet"] = magnet
    top = body.copy()
    for y in range(1, 15):
        for x in range(4, 12):
            if x in (4, 11):
                top.putpixel((x, y), PLASTIC[1] + (255,))
                continue
            cloud = (x - 5) * 9
            v = 222 - cloud - (8 if (x + 2 * y) % 7 == 0 else 0)
            top.putpixel((x, y), (v, v + 4, v + 8, 255))
    sheets["top"] = top
    end = body.copy()
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if r < 2.2:
                end.putpixel((x, y), (40, 42, 48, 255))
            elif r < 4.5:
                end.putpixel((x, y), PLASTIC[6 if y < 8 else 4] + (255,))
            elif r < 5.5:
                end.putpixel((x, y), PLASTIC[1] + (255,))
    sheets["end"] = end
    return sheets


def heap(seed, highlight, body, shadow):
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
    steel(create_texture("fluid_tank_top")).save(TEXTURES / "block/mixer_settler_top.png")
    steel(create_texture("fluid_tank_top_connected")).save(TEXTURES / "block/mixer_settler_top_connected.png")
    for sheet, img in plastic_tank_sheets().items():
        img.save(TEXTURES / f"block/plastic_fluid_tank{sheet}.png")
    for sheet, img in cell_sheets().items():
        img.save(TEXTURES / f"block/magnetomigration_cell_{sheet}.png")
    liquor("still").save(TEXTURES / "block/fluid/liquor_still.png")
    liquor("flow", flow=True).save(TEXTURES / "block/fluid/liquor_flow.png")
    heap("salt", (255, 255, 255), (232, 234, 236), (176, 180, 186)).save(TEXTURES / "item/salt.png")
    heap("oxalic", (255, 255, 252), (238, 236, 224), (184, 180, 160)).save(TEXTURES / "item/oxalic_acid.png")
    heap("roasted_bastnasite", (240, 222, 178), (210, 184, 132), (150, 124, 82)).save(TEXTURES / "item/roasted_bastnasite.png")
    heap("light_sulfate", (250, 242, 242), (226, 214, 216), (170, 158, 162)).save(TEXTURES / "item/light_rare_earth_sulfate.png")
    heap("heavy_sulfate", (250, 248, 238), (228, 224, 208), (172, 168, 150)).save(TEXTURES / "item/heavy_rare_earth_sulfate.png")
    heap("light_carbonate", (252, 246, 246), (232, 222, 224), (178, 166, 170)).save(TEXTURES / "item/light_rare_earth_carbonate.png")
    heap("heavy_carbonate", (254, 252, 244), (234, 230, 216), (180, 176, 160)).save(TEXTURES / "item/heavy_rare_earth_carbonate.png")
    heap("cerium_concentrate", (246, 236, 196), (222, 204, 150), (164, 146, 98)).save(TEXTURES / "item/cerium_concentrate.png")
    heap("europium_sulfate", (255, 255, 255), (236, 236, 238), (184, 184, 190)).save(TEXTURES / "item/europium_sulfate.png")
    heap("calcium_chloride", (255, 255, 255), (240, 240, 236), (190, 190, 184)).save(TEXTURES / "item/calcium_chloride.png")
    sticks((252, 250, 232), (238, 232, 196), (196, 186, 136)).save(TEXTURES / "item/white_phosphorus.png")
    ingot(((88, 90, 94), (138, 141, 146), (180, 184, 190), (222, 226, 232))).save(TEXTURES / "item/calcium_ingot.png")
    nozzle().save(TEXTURES / "block/mixer_settler_nozzle.png")
    print("separation textures written")


if __name__ == "__main__":
    main()
