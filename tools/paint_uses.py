#!/usr/bin/env python3
import random

from PIL import Image

from paint_materials import paint_block, paint_item
from paint_minerals import paint_raw
from paint_separation import TEXTURES, heap


def prills(seed, colours):
    """A heap of round granules, each a 2x2 bead lit from the top left; several colours make a blend."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for row, y in enumerate(range(12, 2, -2)):
        half = 6 - row - (1 if row == 4 else 0)
        for x in range(8 - half - row % 2, 8 + half - row % 2, 2):
            highlight, body, shadow = rng.choice(colours)
            for (dx, dy), colour in (((0, 0), highlight), ((1, 0), body), ((0, 1), body), ((1, 1), shadow)):
                img.putpixel((x + dx, y + dy), colour + (255,))
    return img


def lens():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    body, edge, light = (124, 110, 150, 210), (74, 62, 96, 255), (214, 204, 232, 230)
    for x in range(2, 14):
        for y in range(2, 14):
            on_edge = x in (2, 13) or y in (2, 13)
            img.putpixel((x, y), edge if on_edge else body)
    for x, y in ((4, 4), (5, 4), (4, 5), (6, 4), (4, 6)):
        img.putpixel((x, y), light)
    return img


def filament(wire=(150, 154, 162, 255), light=(214, 218, 226, 255)):
    # a coiled filament between two straight leads: each turn's front lit and slanted, its back in shadow
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    back = tuple(c * 3 // 5 for c in wire[:3]) + (255,)
    for x in (1, 2, 13, 14):
        img.putpixel((x, 8), wire)
    for x0 in range(3, 12, 2):
        for y in range(5, 12):
            img.putpixel((x0 + 1, y), back)
        for y, dx in ((11, 0), (10, 0), (9, 1), (8, 1), (7, 1), (6, 2), (5, 2)):
            if x0 + dx < 13:
                img.putpixel((x0 + dx, y), light if y in (7, 8) else wire)
    return img


def spool(wire, light, flange):
    # solder wound on a reel, the reel's two flanges either side
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    edge = tuple(c * 3 // 5 for c in flange[:3]) + (255,)
    shade = tuple(c * 3 // 4 for c in wire[:3]) + (255,)
    for y in range(2, 14):
        for x in (2, 3, 12, 13):
            img.putpixel((x, y), edge if x in (2, 13) or y in (2, 13) else flange)
    for y in range(4, 12):
        for x in range(4, 12):
            img.putpixel((x, y), light if y % 2 == 0 and x < 9 else wire if y % 2 == 0 else shade)
    return img


def gauze():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wire, light, rim = (200, 202, 208, 255), (246, 247, 250, 255), (120, 122, 130, 255)
    for x in range(2, 14):
        for y in range(2, 14):
            if x in (2, 13) or y in (2, 13):
                img.putpixel((x, y), rim)
            elif x % 2 == 0 or y % 2 == 0:
                img.putpixel((x, y), light if (x + y) % 4 == 0 else wire)
    return img


def spangled():
    img = paint_item("plate", ((92, 98, 106), (164, 170, 178), (208, 214, 220), (240, 244, 248)))
    rng = random.Random("spangle")
    seeds = [((rng.randrange(16), rng.randrange(16)), rng.choice((-14, -6, 6, 12))) for _ in range(7)]
    for y in range(16):
        for x in range(16):
            r, g, b, a = img.getpixel((x, y))
            if a and r > 120:
                shift = min(seeds, key=lambda s: (s[0][0] - x) ** 2 + (s[0][1] - y) ** 2)[1]
                img.putpixel((x, y), tuple(max(0, min(255, c + shift)) for c in (r, g, b)) + (a,))
    return img


def mantle():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    mesh, light, shade, ring = (232, 230, 222, 255), (255, 255, 250, 255), (176, 172, 160, 255), (120, 116, 108, 255)
    for x in range(6, 10):
        img.putpixel((x, 2), ring)
        img.putpixel((x, 3), ring)
    for y in range(4, 14):
        half = min(4, 1 + (y - 4) // 2) if y < 12 else 4 - (y - 11)
        for x in range(8 - half - 1, 8 + half + 1):
            edge = x in (8 - half - 1, 8 + half)
            colour = shade if edge else light if (x + y) % 3 == 0 and x < 8 else mesh if (x + y) % 2 else shade
            img.putpixel((x, y), colour)
    return img


def anode():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    bar, bar_light = (150, 150, 156, 255), (204, 204, 210, 255)
    strand, glint, rim = (66, 68, 80, 255), (122, 126, 142, 255), (36, 36, 44, 255)
    for x in range(2, 14):
        img.putpixel((x, 2), bar_light)
        img.putpixel((x, 3), bar)
    for x in (4, 11):
        img.putpixel((x, 4), bar)
    for x in range(3, 13):
        for y in range(5, 14):
            if x in (3, 12) or y in (5, 13):
                img.putpixel((x, y), rim)
            elif (x + y) % 4 == 0 or (x - y) % 4 == 0:
                img.putpixel((x, y), glint if (x + 2 * y) % 7 == 0 else strand)
    return img


def bar(tones, band=None):
    dark, mid, light, shine = tones
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(6, 12):
        for x in range(2, 12):
            img.putpixel((x, y), dark if y == 11 or x == 2 else mid)
    for y in (4, 5):
        for x in range(2 + 6 - y, 12 + 6 - y):
            img.putpixel((x, y), shine if (x, y) in ((5, 4), (6, 4), (4, 5)) else light)
    for x in (12, 13):
        for y in range(6 - (x - 11), 12 - (x - 11)):
            img.putpixel((x, y), dark)
    if band:
        for y in range(6, 12):
            img.putpixel((6, y), band[0])
            img.putpixel((7, y), band[0] if y == 11 else band[1])
        for x, y in ((7, 5), (8, 5), (8, 4), (9, 4)):
            img.putpixel((x, y), band[1])
    return img


def horseshoe(paint, pole):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(2, 14):
        for x in range(2, 14):
            dx, dy = x - 7.5, y - 7.5
            r = (dx * dx + dy * dy) ** 0.5
            arc = y <= 7.5 and 2.6 <= r <= 5.9
            leg = y > 7.5 and (2 <= x <= 4 or 11 <= x <= 13)
            if not (arc or leg):
                continue
            tones = pole if y >= 11 else paint
            edge = (arc and (r > 5.2 or r < 3.2)) or (leg and x in (2, 4, 11, 13))
            lit = x < 7 and not edge
            img.putpixel((x, y), tones[0] if edge else tones[2] if lit else tones[1])
    return img


def pellets():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    body, light, shade, end = (58, 56, 54, 255), (110, 106, 100, 255), (30, 28, 28, 255), (84, 80, 76, 255)
    for x0, y0 in ((2, 8), (6, 4), (9, 9)):
        for x in range(x0, x0 + 5):
            for y in range(y0, y0 + 4):
                edge = x == x0 + 4
                img.putpixel((x, y), end if edge else light if y == y0 else shade if y == y0 + 3 else body)
    return img


def tube(tones, bore):
    img = fuel_rod(tones)
    for x, y in ((12, 2), (13, 3)):
        img.putpixel((x, y), bore)
    return img


def grid():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wire, light, lug = (96, 100, 112, 255), (150, 154, 166, 255), (70, 74, 84, 255)
    for x in range(2, 14):
        for y in range(3, 15):
            if x in (2, 13) or y in (3, 14) or x % 3 == 2 or y % 3 == 2:
                img.putpixel((x, y), light if y == 3 or x == 2 else wire)
    for x in range(3, 6):
        for y in range(1, 3):
            img.putpixel((x, y), lug)
    return img


def cell(case, rim, terminals):
    dark, mid, light = case
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(3, 13):
        for y in range(5, 14):
            if x in (3, 12) or y in (5, 13):
                tone = rim
            elif x == 4 or y == 6:
                tone = light
            elif x == 11 or y == 12:
                tone = dark
            else:
                tone = mid
            img.putpixel((x, y), tone)
    for x0, colour in zip((4, 10), terminals):
        for x in range(x0 - 1, x0 + 3):
            for y in range(2, 5):
                edge = x in (x0 - 1, x0 + 2) or y == 2
                img.putpixel((x, y), rim if edge else colour)
    return img


def cylinder(wrap, band, cap, rim):
    dark, mid, light = wrap
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(4, 12):
        for y in range(2, 15):
            if x in (4, 11) or y in (2, 14):
                tone = rim
            elif y in (3, 4):
                tone = cap if y == 3 else band
            elif x == 5:
                tone = light
            elif x == 10 or y == 13:
                tone = dark
            else:
                tone = mid
            img.putpixel((x, y), tone)
    for x in range(6, 10):
        img.putpixel((x, 1), rim if x in (6, 9) else cap)
        img.putpixel((x, 0), rim if x in (7, 8) else (0, 0, 0, 0))
    return img


def batteries():
    lead = lambda *tones: paint_item("plate", tones)
    grid().save(TEXTURES / "item/lead_grid.png")
    heap("lead_paste", (214, 214, 206), (176, 176, 168), (122, 122, 116)).save(TEXTURES / "item/lead_paste.png")
    lead((92, 92, 88), (150, 150, 142), (196, 196, 186), (226, 226, 218)).save(TEXTURES / "item/pasted_lead_plate.png")
    lead((30, 20, 16), (62, 40, 30), (92, 62, 46), (124, 90, 70)).save(TEXTURES / "item/lead_dioxide_plate.png")
    lead((54, 58, 70), (92, 98, 112), (128, 134, 148), (166, 172, 186)).save(TEXTURES / "item/sponge_lead_plate.png")
    cell(((38, 40, 46, 255), (62, 64, 72, 255), (104, 106, 116, 255)), (16, 16, 20, 255), ((204, 54, 44, 255), (118, 120, 128, 255))).save(TEXTURES / "item/lead_acid_cell.png")
    heap("manganese_sulfate", (255, 236, 238), (240, 206, 212), (196, 160, 168)).save(TEXTURES / "item/manganese_sulfate.png")
    heap("cobalt_sulfate", (238, 128, 146), (200, 70, 96), (140, 36, 60)).save(TEXTURES / "item/cobalt_sulfate.png")
    heap("nmc_hydroxide", (150, 140, 96), (112, 102, 66), (70, 62, 38)).save(TEXTURES / "item/nmc_hydroxide.png")
    heap("nmc_cathode", (70, 70, 74), (40, 40, 44), (18, 18, 20)).save(TEXTURES / "item/nmc_cathode.png")
    heap("iron_phosphate", (255, 250, 236), (240, 228, 206), (196, 180, 156)).save(TEXTURES / "item/iron_phosphate.png")
    heap("lfp_cathode", (96, 98, 102), (58, 60, 64), (28, 30, 32)).save(TEXTURES / "item/lfp_cathode.png")
    heap("lithium_fluoride", (255, 255, 255), (238, 240, 242), (188, 192, 196)).save(TEXTURES / "item/lithium_fluoride.png")
    heap("lithium_hexafluorophosphate", (255, 255, 255), (242, 242, 244), (194, 196, 200)).save(TEXTURES / "item/lithium_hexafluorophosphate.png")
    heap("ethylene_carbonate", (255, 255, 252), (236, 236, 230), (186, 186, 178)).save(TEXTURES / "item/ethylene_carbonate.png")
    heap("nickel_hydroxide", (190, 230, 160), (138, 196, 110), (86, 140, 66)).save(TEXTURES / "item/nickel_hydroxide.png")
    paint_raw("hydride_alloy", ((70, 72, 78), (122, 126, 134), (168, 172, 180), (214, 218, 226))).save(TEXTURES / "item/hydride_alloy.png")
    cylinder(((26, 78, 46, 255), (46, 134, 80, 255), (88, 176, 118, 255)), (150, 154, 162, 255), (206, 210, 216, 255), (12, 34, 22, 255)).save(
        TEXTURES / "item/nickel_metal_hydride_cell.png")


def boule():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    shades = {4: (52, 56, 66), 5: (96, 102, 116), 6: (146, 154, 170), 7: (204, 212, 228), 8: (160, 168, 184), 9: (122, 128, 142), 10: (96, 102, 116), 11: (52, 56, 66)}
    for y in range(1, 15):
        half = 1 if y < 3 else 2 if y < 4 else 4
        for x in range(8 - half, 8 + half):
            tone = shades[x] if half == 4 else shades[min(x, 9)]
            img.putpixel((x, y), tone + (255,) if y < 14 else (52, 56, 66, 255))
    return img


def wafer():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    sheen = ((88, 96, 132), (118, 128, 168), (150, 140, 186), (128, 160, 190))
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if r <= 6.6 and not (y == 14 and 6 <= x <= 9):
                tone = sheen[(x + 2 * y) // 5 % 4]
                img.putpixel((x, y), (60, 64, 84, 255) if r > 5.8 else tone + (255,))
    for x, y in ((5, 4), (6, 4), (4, 5)):
        img.putpixel((x, y), (220, 226, 246, 255))
    return img


def chip():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    frame, die, glow, leg = (226, 228, 232, 255), (70, 90, 190, 255), (232, 214, 96, 255), (176, 140, 72, 255)
    for x in range(4, 12):
        for y in range(4, 12):
            img.putpixel((x, y), frame if x in (4, 11) or y in (4, 11) else glow if 6 <= x <= 9 and 6 <= y <= 9 else die)
    for x in (5, 10):
        for y in range(12, 15):
            img.putpixel((x, y), leg)
    return img


def magnets():
    nickel = ((92, 96, 104, 255), (170, 174, 182, 255), (214, 218, 226, 255), (250, 252, 255, 255))
    bar(nickel).save(TEXTURES / "item/neodymium_iron_boron_magnet.png")
    bar(nickel, ((98, 104, 46, 255), (156, 166, 78, 255))).save(TEXTURES / "item/dysprosium_neodymium_iron_boron_magnet.png")
    bar(((44, 40, 38, 255), (84, 78, 74, 255), (120, 114, 108, 255), (156, 150, 142, 255))).save(TEXTURES / "item/samarium_cobalt_magnet.png")
    horseshoe(((112, 20, 18, 255), (184, 40, 34, 255), (226, 84, 70, 255)),
              ((96, 98, 104, 255), (176, 180, 188, 255), (226, 230, 236, 255))).save(TEXTURES / "item/alnico_magnet.png")



def fuel_rod(tones, glow=None):
    # a clad rod lying corner to corner, lit from the upper left; a spent one still glows Cherenkov blue in its seam
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    outline, shade, body, light, shine = tones
    for y in range(16):
        for x in range(16):
            u, v = x - y, x + y - 15  # along the rod, and across it
            if abs(u) > 11 or abs(v) > 2:
                continue
            tone = (shine if u % 5 else light) if v == -2 else body if v in (-1, 0) else shade if v == 1 else outline
            if glow and v in (-1, 0) and u % 3 == 0:
                tone = glow
            if abs(u) >= 10:
                tone = outline if abs(u) == 11 else shade
            img.putpixel((x, y), tone)
    return img


def glass_log(tones):
    # borosilicate cast in a standing log: black glass, glossy, its waste glinting amber inside
    dark, body, rim, glint, shine = tones
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(2, 15):
        for x in range(4, 12):
            if y in (2, 14) and x in (4, 11):
                continue
            top = y <= 3
            tone = rim if top else dark if x in (4, 11) or y == 14 else body
            if top and y == 3 and x in (4, 11):
                tone = dark
            img.putpixel((x, y), tone)
    for x, y in ((5, 5), (5, 6), (5, 7), (5, 8), (6, 5), (5, 10), (5, 11)):
        img.putpixel((x, y), shine)
    for x, y in ((8, 7), (9, 10), (7, 12), (9, 5), (8, 9)):
        img.putpixel((x, y), glint)
    for x in range(6, 10):
        img.putpixel((x, 2), shine)
    return img


def canister_side(steel, label):
    # a welded stainless drum: lit down its left, seams top and bottom, a band of hazard stripes
    light, mid, dark, seam = steel
    yellow, black = label
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            tone = light if x < 4 else mid if x < 12 else dark
            if y in (0, 15):
                tone = seam
            elif y in (1, 14):
                tone = dark if x % 4 == 3 else light
            elif 6 <= y <= 9:
                tone = black if (x + y) % 4 < 2 else yellow
            elif y in (5, 10):
                tone = seam
            img.putpixel((x, y), tone)
    return img


def canister_top(steel):
    # the welded lid, a ring and a lifting boss in its middle
    light, mid, dark, seam = steel
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            tone = seam if x in (0, 15) or y in (0, 15) else mid
            if 5.2 < r < 6.4:
                tone = dark if x + y > 15 else light
            if r < 2.2:
                tone = light if x + y < 15 else dark
            img.putpixel((x, y), tone)
    return img


def main():
    heap("phosphor", (255, 250, 252), (240, 226, 236), (196, 170, 190)).save(TEXTURES / "item/phosphor.png")
    lens().save(TEXTURES / "item/didymium_glass.png")
    paint_raw("roasted_cobaltite", ((70, 60, 66), (120, 108, 112), (166, 154, 156), (214, 206, 206))).save(TEXTURES / "item/roasted_cobaltite.png")
    paint_raw("roasted_chalcopyrite", ((80, 50, 36), (138, 90, 62), (184, 132, 96), (230, 190, 150))).save(TEXTURES / "item/roasted_chalcopyrite.png")
    heap("rhenium_flue_dust", (250, 250, 246), (214, 214, 208), (150, 150, 146)).save(TEXTURES / "item/rhenium_flue_dust.png")
    heap("tungsten_carbide", (120, 122, 128), (74, 76, 82), (40, 42, 46)).save(TEXTURES / "item/tungsten_carbide.png")
    filament().save(TEXTURES / "item/tungsten_filament.png")
    heap("clarifier_sludge", (150, 128, 96), (112, 92, 64), (70, 56, 38)).save(TEXTURES / "item/clarifier_sludge.png")
    paint_block("clarifier_sludge_block", ((62, 48, 32), (104, 84, 58), (140, 118, 86), (176, 152, 116))).save(TEXTURES / "block/clarifier_sludge_block.png")
    paint_block("red_mud_block", ((84, 34, 22), (128, 54, 36), (164, 78, 52), (196, 108, 76))).save(TEXTURES / "block/red_mud_block.png")
    heap("thorium_nitrate", (255, 255, 255), (238, 238, 234), (186, 186, 180)).save(TEXTURES / "item/thorium_nitrate.png")
    mantle().save(TEXTURES / "item/gas_mantle.png")
    paint_raw("copper_calcine", ((22, 20, 20), (48, 44, 42), (78, 72, 68), (122, 114, 108))).save(TEXTURES / "item/copper_calcine.png")
    heap("zinc_oxide", (252, 252, 248), (222, 222, 214), (160, 160, 152)).save(TEXTURES / "item/zinc_oxide.png")
    paint_raw("roasted_pentlandite", ((40, 46, 34), (72, 84, 60), (108, 122, 90), (156, 168, 132))).save(TEXTURES / "item/roasted_pentlandite.png")
    heap("lithium_chloride", (255, 255, 255), (236, 238, 238), (182, 186, 188)).save(TEXTURES / "item/lithium_chloride.png")
    paint_raw("ferroboron", ((58, 60, 64), (104, 108, 114), (150, 154, 160), (200, 204, 210))).save(TEXTURES / "item/ferroboron.png")
    heap("soda_ash", (254, 254, 252), (232, 232, 228), (178, 178, 172)).save(TEXTURES / "item/soda_ash.png")
    heap("sodium_chromate", (252, 240, 110), (234, 206, 34), (168, 138, 18)).save(TEXTURES / "item/sodium_chromate.png")
    heap("sodium_dichromate", (255, 160, 80), (226, 98, 28), (150, 52, 16)).save(TEXTURES / "item/sodium_dichromate.png")
    heap("aluminium_powder", (224, 226, 230), (172, 176, 184), (110, 114, 122)).save(TEXTURES / "item/aluminium_powder.png")
    paint_raw("titania_slag", ((18, 16, 16), (42, 38, 36), (72, 64, 60), (118, 106, 98))).save(TEXTURES / "item/titania_slag.png")
    heap("magnesium_chloride", (255, 255, 255), (234, 236, 238), (180, 184, 188)).save(TEXTURES / "item/magnesium_chloride.png")
    heap("ammonium_chloride", (255, 255, 255), (238, 240, 240), (186, 190, 192)).save(TEXTURES / "item/ammonium_chloride.png")
    heap("insoluble_residue", (88, 88, 92), (52, 52, 56), (24, 24, 28)).save(TEXTURES / "item/insoluble_residue.png")
    heap("iridium_rhodium_residue", (104, 92, 90), (66, 56, 54), (34, 28, 28)).save(TEXTURES / "item/iridium_rhodium_residue.png")
    heap("ammonium_chloroplatinate", (255, 244, 120), (246, 214, 40), (190, 150, 16)).save(TEXTURES / "item/ammonium_chloroplatinate.png")
    heap("dichlorodiammine_palladium", (250, 230, 140), (226, 192, 78), (162, 128, 40)).save(TEXTURES / "item/dichlorodiammine_palladium.png")
    heap("ammonium_chlororuthenate", (156, 72, 52), (112, 40, 28), (64, 20, 14)).save(TEXTURES / "item/ammonium_chlororuthenate.png")
    heap("ammonium_chloroiridate", (96, 40, 32), (58, 24, 20), (28, 12, 10)).save(TEXTURES / "item/ammonium_chloroiridate.png")
    heap("ammonium_chlororhodate", (236, 132, 150), (204, 80, 104), (140, 42, 64)).save(TEXTURES / "item/ammonium_chlororhodate.png")
    heap("reforming_catalyst", (246, 246, 242), (206, 206, 204), (136, 138, 142)).save(TEXTURES / "item/reforming_catalyst.png")
    heap("hydrotreating_catalyst", (150, 172, 196), (94, 118, 150), (50, 64, 92)).save(TEXTURES / "item/hydrotreating_catalyst.png")
    steel = ((36, 42, 50, 255), (84, 94, 108, 255), (136, 150, 168, 255), (192, 204, 220, 255))
    bar(steel, ((130, 36, 30, 255), (200, 70, 54, 255))).save(TEXTURES / "item/steel_magnet.png")
    paint_item("ingot", tuple(t[:3] for t in steel)).save(TEXTURES / "item/hardened_steel.png")
    paint_item("ingot", ((112, 30, 12), (196, 74, 22), (246, 146, 48), (255, 224, 142))).save(TEXTURES / "item/hot_steel.png")
    cell(((84, 124, 140, 200), (124, 172, 180, 170), (188, 222, 224, 210)), (52, 84, 96, 255),
         ((192, 114, 66, 255), (166, 174, 188, 255))).save(TEXTURES / "item/galvanic_cell.png")
    clad = ((22, 26, 30, 255), (52, 58, 60, 255), (82, 88, 86, 255), (120, 126, 120, 255), (160, 166, 156, 255))
    fuel_rod(clad, (110, 210, 255, 255)).save(TEXTURES / "item/spent_fuel_rod.png")
    fuel_rod(clad).save(TEXTURES / "item/cooled_spent_fuel.png")
    glass_log(((10, 10, 12, 255), (30, 28, 30, 255), (86, 80, 76, 255), (196, 128, 48, 255), (214, 222, 228, 255))).save(TEXTURES / "item/vitrified_waste.png")
    stainless = ((196, 200, 206, 255), (150, 156, 164, 255), (104, 110, 120, 255), (64, 68, 76, 255))
    canister_side(stainless, ((236, 194, 36, 255), (28, 26, 24, 255))).save(TEXTURES / "block/waste_canister.png")
    canister_top(stainless).save(TEXTURES / "block/waste_canister_top.png")
    gauze().save(TEXTURES / "item/platinum_rhodium_gauze.png")
    filament((112, 126, 150, 255), (178, 192, 216, 255)).save(TEXTURES / "item/osmium_filament.png")
    heap("roasted_tin_concentrate", (130, 84, 60), (82, 50, 34), (38, 24, 18)).save(TEXTURES / "item/roasted_tin_concentrate.png")
    spool((150, 152, 156, 255), (206, 208, 212, 255), (60, 90, 150, 255)).save(TEXTURES / "item/solder.png")
    paint_raw("silver_zinc_crust", ((70, 72, 76), (118, 120, 124), (164, 166, 170), (212, 214, 216))).save(TEXTURES / "item/silver_zinc_crust.png")
    heap("litharge", (252, 206, 100), (230, 150, 46), (164, 88, 22)).save(TEXTURES / "item/litharge.png")
    heap("crude_zirconium_tetrachloride", (252, 248, 224), (228, 222, 186), (170, 162, 122)).save(TEXTURES / "item/crude_zirconium_tetrachloride.png")
    heap("zirconium_tetrachloride", (255, 255, 255), (236, 238, 238), (182, 186, 188)).save(TEXTURES / "item/zirconium_tetrachloride.png")
    heap("hafnium_tetrachloride", (255, 255, 255), (230, 232, 236), (172, 176, 184)).save(TEXTURES / "item/hafnium_tetrachloride.png")
    heap("yttria_stabilised_zirconia", (254, 252, 244), (232, 228, 214), (176, 170, 152)).save(TEXTURES / "item/yttria_stabilised_zirconia.png")
    paint_raw("beryl_frit", ((58, 94, 82), (108, 148, 130), (162, 198, 180), (226, 244, 236))).save(TEXTURES / "item/beryl_frit.png")
    heap("beryllium_hydroxide", (255, 255, 255), (240, 240, 238), (190, 190, 186)).save(TEXTURES / "item/beryllium_hydroxide.png")
    heap("ammonium_fluoroberyllate", (255, 255, 255), (236, 240, 240), (184, 190, 192)).save(TEXTURES / "item/ammonium_fluoroberyllate.png")
    paint_raw("beryllium_pebbles", ((70, 76, 84), (124, 132, 142), (170, 178, 188), (216, 222, 230))).save(TEXTURES / "item/beryllium_pebbles.png")
    anode().save(TEXTURES / "item/dimensionally_stable_anode.png")
    heap("red_mud", (178, 84, 58), (140, 58, 38), (92, 36, 24)).save(TEXTURES / "item/red_mud.png")
    heap("aluminium_hydroxide", (255, 255, 255), (240, 240, 236), (192, 192, 186)).save(TEXTURES / "item/aluminium_hydroxide.png")
    heap("alumina", (255, 255, 255), (244, 244, 244), (200, 202, 206)).save(TEXTURES / "item/alumina.png")
    heap("cryolite", (255, 255, 255), (232, 236, 238), (178, 186, 190)).save(TEXTURES / "item/cryolite.png")
    paint_raw("nickel_oxide", ((50, 72, 44), (90, 120, 72), (130, 160, 102), (178, 204, 148))).save(TEXTURES / "item/nickel_oxide.png")
    paint_raw("nickel_pellets", ((96, 96, 90), (160, 160, 152), (206, 206, 198), (244, 244, 238))).save(TEXTURES / "item/nickel_pellets.png")
    heap("tungstic_acid", (250, 238, 120), (226, 204, 60), (160, 140, 30)).save(TEXTURES / "item/tungstic_acid.png")
    heap("ammonium_paratungstate", (255, 255, 255), (240, 242, 244), (192, 196, 202)).save(TEXTURES / "item/ammonium_paratungstate.png")
    heap("ammonium_perrhenate", (255, 255, 255), (236, 238, 242), (184, 188, 196)).save(TEXTURES / "item/ammonium_perrhenate.png")
    heap("lithium_carbonate", (255, 255, 255), (242, 242, 240), (196, 196, 192)).save(TEXTURES / "item/lithium_carbonate.png")
    heap("mcraly_powder", (196, 198, 204), (140, 144, 152), (84, 88, 96)).save(TEXTURES / "item/mcraly_powder.png")
    heap("boric_acid", (255, 255, 255), (242, 242, 238), (196, 196, 190)).save(TEXTURES / "item/boric_acid.png")
    spangled().save(TEXTURES / "item/galvanized_steel_plate.png")
    paint_raw("roasted_siderite", ((38, 20, 18), (72, 38, 32), (106, 60, 48), (152, 104, 88))).save(TEXTURES / "item/roasted_siderite.png")
    heap("yellowcake", (252, 232, 96), (232, 196, 44), (168, 128, 22)).save(TEXTURES / "item/yellowcake.png")
    heap("uranium_tetrafluoride", (190, 214, 112), (140, 172, 70), (86, 112, 40)).save(TEXTURES / "item/uranium_tetrafluoride.png")
    heap("uranium_dioxide", (112, 90, 72), (70, 54, 42), (34, 26, 20)).save(TEXTURES / "item/uranium_dioxide.png")
    heap("depleted_uranium_oxide", (92, 92, 70), (56, 56, 42), (26, 26, 20)).save(TEXTURES / "item/depleted_uranium_oxide.png")
    heap("thorium_dioxide", (255, 255, 255), (238, 238, 234), (188, 188, 182)).save(TEXTURES / "item/thorium_dioxide.png")
    heap("potassium_bifluoride", (255, 255, 255), (240, 242, 240), (194, 198, 194)).save(TEXTURES / "item/potassium_bifluoride.png")
    paint_raw("boron_carbide", ((14, 14, 16), (34, 34, 40), (62, 62, 72), (130, 132, 146))).save(TEXTURES / "item/boron_carbide.png")
    pellets().save(TEXTURES / "item/uranium_fuel_pellet.png")
    tube(((40, 42, 46, 255), (88, 90, 96, 255), (150, 152, 158, 255), (190, 192, 198, 255), (222, 224, 230, 255)), (24, 24, 28, 255)).save(TEXTURES / "item/zircaloy_tube.png")
    paint_raw("puddle_ball", ((112, 34, 14), (196, 84, 28), (240, 150, 56), (255, 226, 150))).save(TEXTURES / "item/puddle_ball.png")
    heap("graphite", (150, 152, 160), (70, 72, 78), (28, 28, 32)).save(TEXTURES / "item/graphite.png")
    heap("potash", (250, 236, 230), (226, 190, 178), (170, 120, 104)).save(TEXTURES / "item/potash.png")
    paint_raw("gold_amalgam", ((96, 96, 92), (164, 162, 150), (208, 204, 186), (246, 242, 222))).save(TEXTURES / "item/gold_amalgam.png")
    heap("potassium_fluorotantalate", (255, 255, 255), (238, 240, 242), (186, 190, 196)).save(TEXTURES / "item/potassium_fluorotantalate.png")
    paint_raw("polysilicon", ((70, 74, 84), (122, 128, 140), (168, 174, 188), (214, 220, 232))).save(TEXTURES / "item/polysilicon.png")
    boule().save(TEXTURES / "item/silicon_boule.png")
    wafer().save(TEXTURES / "item/silicon_wafer.png")
    heap("zinc_leach_residue", (150, 92, 64), (110, 62, 40), (64, 34, 22)).save(TEXTURES / "item/zinc_leach_residue.png")
    chip().save(TEXTURES / "item/led_chip.png")
    heap("indium_tin_oxide", (214, 220, 150), (180, 190, 110), (122, 130, 66)).save(TEXTURES / "item/indium_tin_oxide.png")
    heap("vanadium_pentoxide", (240, 168, 60), (214, 128, 26), (150, 82, 14)).save(TEXTURES / "item/vanadium_pentoxide.png")
    paint_raw("cement_clinker", ((40, 42, 44), (70, 72, 74), (104, 106, 106), (146, 146, 142))).save(TEXTURES / "item/cement_clinker.png")
    heap("gypsum", (255, 255, 255), (242, 240, 234), (196, 192, 184)).save(TEXTURES / "item/gypsum.png")
    heap("phosphogypsum", (236, 232, 222), (204, 198, 184), (150, 144, 130)).save(TEXTURES / "item/phosphogypsum.png")
    heap("calomel", (255, 255, 255), (238, 236, 230), (190, 186, 178)).save(TEXTURES / "item/calomel.png")
    paint_raw("copper_cement", ((92, 40, 24), (150, 72, 44), (196, 104, 66), (228, 140, 96))).save(TEXTURES / "item/copper_cement.png")
    heap("cobalt_cake", (150, 146, 150), (104, 100, 108), (60, 58, 66)).save(TEXTURES / "item/cobalt_cake.png")
    heap("anode_mud", (70, 62, 56), (44, 38, 34), (20, 18, 16)).save(TEXTURES / "item/anode_mud.png")
    heap("jarosite", (214, 170, 72), (176, 130, 44), (120, 84, 26)).save(TEXTURES / "item/jarosite.png")
    paint_block("jarosite_block", ((120, 84, 26), (160, 116, 40), (190, 146, 60), (220, 178, 84))).save(TEXTURES / "block/jarosite_block.png")
    heap("lead_silver_residue", (150, 148, 140), (112, 110, 104), (70, 68, 64)).save(TEXTURES / "item/lead_silver_residue.png")
    paint_block("phosphogypsum_block", ((150, 144, 130), (190, 184, 170), (214, 208, 194), (234, 230, 220))).save(TEXTURES / "block/phosphogypsum_block.png")
    prills("ammonium_nitrate", [((255, 255, 255), (238, 238, 234), (190, 190, 184))]).save(TEXTURES / "item/ammonium_nitrate.png")
    prills("urea", [((255, 255, 255), (246, 246, 244), (206, 206, 202))]).save(TEXTURES / "item/urea.png")
    prills("superphosphate", [((214, 208, 196), (172, 164, 150), (120, 112, 100))]).save(TEXTURES / "item/superphosphate.png")
    prills("ammonium_phosphate", [((232, 228, 218), (196, 190, 178), (140, 134, 122))]).save(TEXTURES / "item/ammonium_phosphate.png")
    prills("npk_fertiliser", [((255, 255, 255), (238, 238, 234), (190, 190, 184)), ((206, 200, 188), (164, 156, 142), (112, 104, 92)), ((236, 170, 160), (204, 122, 112), (146, 76, 70))]).save(TEXTURES / "item/npk_fertiliser.png")
    magnets()
    batteries()
    print("uses textures written")


if __name__ == "__main__":
    main()
