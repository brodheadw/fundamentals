#!/usr/bin/env python3
"""Paints an item for every form a material comes in: concentrate, oxide, dust, ingot, nugget,
plate and storage block. One silhouette per form, in vanilla's idiom (a dark rim, light from the
top left), recoloured per material. Edit and re-run; don't hand-edit the PNGs.

    python3 tools/paint_materials.py            # write textures
    python3 tools/paint_materials.py sheet.png  # also write a labelled contact sheet

Only the rare earths so far. Run tools/build_material_data.py afterwards.
"""
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from paint_minerals import P, ramp

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/fundamentals/textures"

# These mirror the form lists in content/rare_earths/RareEarthMaterials.java.
MINERAL = ("dust",)
CONCENTRATE = ("concentrate",)
ELEMENT = ("oxalate", "fluoride", "oxide", "dust", "ingot")
MAGNET_ELEMENT = ("oxalate", "fluoride", "oxide", "dust", "ingot", "nugget", "block")
# Sm, Eu, Tm and Yb are reduced from the oxide by lanthanum, so they never pass through a fluoride.
VOLATILE = ("oxalate", "oxide", "dust", "ingot")
VOLATILE_MAGNET = ("oxalate", "oxide", "dust", "ingot", "nugget", "block")
DIDYMIUM = ("oxalate", "fluoride", "oxide", "dust", "ingot")
# Scandium and didymium are not in the chloride liquors, so they have no oxalate.
NO_LIQUOR = ("oxide", "dust", "ingot")
ALLOY = ("dust", "ingot", "nugget", "plate", "block")

WHITE = ((150, 150, 148), (206, 206, 204), (236, 236, 234), (255, 255, 255))


def mix(a, b, t):
    return tuple(tuple(int(x + (y - x) * t) for x, y in zip(ca, cb)) for ca, cb in zip(a, b))


SILVER = ((84, 86, 92), (146, 150, 158), (198, 202, 210), (240, 242, 248))


def tinted(hue, strength=0.38):
    """Silver leaning toward `hue`, tone for tone."""
    out = []
    for tone in SILVER:
        scale = sum(tone) / sum(hue)
        out.append(tuple(min(255, int(t + (h * scale - t) * strength)) for t, h in zip(tone, hue)))
    return tuple(out)


# The metals are all silver. Each leans toward the colour its salts or phosphors are known by
# (praseodymium green, neodymium lilac, erbium rose, terbium's green glow), so sixteen ingots can
# be told apart in a chest.
METAL = {
    "lanthanum": tinted((206, 198, 176)),
    "cerium": tinted((214, 186, 118)),
    "praseodymium": tinted((118, 190, 128)),
    "neodymium": tinted((160, 138, 222)),
    "samarium": tinted((222, 200, 108)),
    "europium": tinted((228, 138, 160)),
    "gadolinium": SILVER,
    "terbium": tinted((98, 200, 178)),
    "dysprosium": tinted((178, 200, 98)),
    "holmium": tinted((232, 168, 118)),
    "erbium": tinted((232, 128, 170)),
    "thulium": tinted((118, 190, 212)),
    "ytterbium": tinted((202, 196, 148)),
    "lutetium": tinted((128, 158, 222)),
    "yttrium": ((68, 70, 78), (118, 122, 132), (168, 172, 182), (220, 224, 230)),
    "scandium": ((104, 106, 112), (176, 180, 188), (220, 224, 230), (252, 253, 255)),
    "didymium": tinted((140, 164, 176)),
    "neodymium_iron_boron": ((40, 42, 50), (82, 86, 98), (130, 134, 148), (196, 200, 214)),
    "samarium_cobalt": ((70, 64, 60), (124, 116, 108), (170, 162, 152), (222, 214, 204)),
}

# The oxides are painted the colours they really are; the white ones borrow a little of their
# metal's tint.
OXIDE = {
    "cerium": ((156, 144, 96), (214, 202, 146), (238, 230, 186), (254, 250, 226)),
    "praseodymium": ((30, 24, 20), (62, 50, 42), (98, 82, 70), (150, 132, 116)),
    "neodymium": ((112, 120, 160), (166, 176, 214), (204, 212, 238), (236, 240, 252)),
    "samarium": ((158, 148, 108), (216, 206, 160), (240, 232, 196), (255, 250, 230)),
    "europium": ((164, 132, 138), (222, 190, 196), (242, 218, 222), (255, 242, 244)),
    "terbium": ((44, 28, 18), (84, 56, 36), (124, 88, 60), (178, 140, 104)),
    "dysprosium": ((176, 180, 160), (222, 226, 206), (242, 244, 230), (254, 255, 246)),
    "holmium": ((160, 150, 100), (220, 210, 150), (242, 234, 190), (255, 252, 228)),
    "erbium": ((160, 96, 124), (214, 146, 174), (238, 188, 208), (254, 228, 238)),
    "thulium": ((126, 152, 128), (182, 208, 184), (216, 234, 216), (244, 252, 244)),
    "didymium": ((62, 54, 56), (104, 92, 94), (146, 132, 134), (196, 184, 186)),
}
for name in ("lanthanum", "gadolinium", "ytterbium", "lutetium", "yttrium", "scandium"):
    OXIDE[name] = mix(WHITE, METAL[name], 0.3)

# The oxalates and fluorides are salts of the trivalent ion, so they take the ion's colour, not the
# oxide's: praseodymium's are green although Pr6O11 is black, terbium's white although Tb4O7 is brown.
# Each entry is the ion's colour at full strength; the salts are mixed toward white from it.
ION = {
    "praseodymium": ((88, 150, 96), (136, 196, 140), (180, 226, 180), (222, 246, 220)),
    "neodymium": ((120, 104, 168), (168, 152, 212), (206, 194, 238), (236, 230, 252)),
    "didymium": ((108, 102, 136), (152, 146, 180), (194, 190, 216), (230, 228, 242)),
    "samarium": ((170, 158, 100), (222, 210, 150), (242, 234, 192), (255, 250, 228)),
    "europium": ((176, 150, 154), (226, 204, 208), (244, 230, 232), (255, 246, 247)),
    "dysprosium": ((160, 166, 112), (212, 218, 160), (236, 240, 198), (252, 254, 232)),
    "holmium": ((176, 156, 104), (228, 210, 150), (246, 234, 190), (255, 250, 228)),
    "erbium": ((176, 104, 134), (224, 156, 184), (242, 196, 214), (254, 232, 240)),
    "thulium": ((128, 162, 132), (184, 214, 186), (218, 238, 218), (244, 252, 244)),
}
for name in ("lanthanum", "cerium", "gadolinium", "terbium", "ytterbium", "lutetium", "yttrium"):
    ION[name] = mix(WHITE, METAL[name], 0.2)

OTHER = {
    "light_rare_earth_concentrate": ((96, 62, 34), (150, 104, 58), (190, 146, 90), (228, 196, 140)),
    "heavy_rare_earth_concentrate": ((84, 76, 48), (132, 122, 78), (172, 162, 110), (216, 208, 160)),
}

# material: forms. Same order as the Java registry, which is the order of the creative tab.
MATERIALS = {
    "bastnasite": MINERAL, "monazite": MINERAL, "xenotime": MINERAL, "loparite": MINERAL, "euxenite": MINERAL,
    "light_rare_earth_concentrate": CONCENTRATE, "heavy_rare_earth_concentrate": CONCENTRATE,
    "lanthanum": ELEMENT, "cerium": ELEMENT, "praseodymium": MAGNET_ELEMENT, "neodymium": MAGNET_ELEMENT,
    "samarium": VOLATILE_MAGNET, "europium": VOLATILE,
    "gadolinium": ELEMENT, "terbium": MAGNET_ELEMENT, "dysprosium": MAGNET_ELEMENT, "holmium": ELEMENT,
    "erbium": ELEMENT, "thulium": VOLATILE, "ytterbium": VOLATILE, "lutetium": ELEMENT, "yttrium": ELEMENT,
    "scandium": NO_LIQUOR,
    "didymium": DIDYMIUM, "neodymium_iron_boron": ALLOY, "samarium_cobalt": ALLOY,
}

DISPLAY = {"bastnasite": "Bastnäsite", "neodymium_iron_boron": "NdFeB", "samarium_cobalt": "SmCo"}


def item_name(material, form):
    return material if form == "concentrate" else f"{material}_{form}"


def items():
    """(material, form, item name) for everything painted here."""
    return [(material, form, item_name(material, form)) for material, forms in MATERIALS.items() for form in forms]


def palette(material, form):
    if form == "oxide":
        return OXIDE[material]
    if form == "oxalate":
        # the hydrated oxalate is a pale powder with the ion's cast
        return mix(WHITE, ION[material], 0.5)
    if form == "fluoride":
        # the anhydrous fluoride shows the ion more strongly
        return mix(WHITE, ION[material], 0.8)
    if material in P:
        return P[material]
    if material in OTHER:
        return OTHER[material]
    # A metal ground to powder loses its shine.
    return mix(METAL[material], ((0, 0, 0),) * 4, 0.22) if form == "dust" else METAL[material]


# Digits are tones of ramp(): 0 the darkest rim, 7 the highlight.
SHAPES = {
    "ingot": [
        "................",
        "................",
        "................",
        "..........111...",
        ".......11166621.",
        "....111666666621",
        ".111666666677620",
        "1577666677764310",
        "1557777776443310",
        "155557644333310.",
        ".15555443333100.",
        "..145443331000..",
        "...144331000....",
        "....111000......",
        "................",
        "................",
    ],
    "nugget": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "......1111......",
        ".....16766210...",
        "....167765420...",
        "....156654320...",
        "....015443200...",
        ".....0000000....",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "plate": [
        "................",
        "................",
        "................",
        "................",
        "................",
        ".........1111...",
        "......11156651..",
        "...111566666651.",
        ".116666666666510",
        ".157666666654320",
        "..1576666543200.",
        "....1576543000..",
        "......153000....",
        ".......000......",
        "................",
        "................",
    ],
    "dust": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".......33.......",
        ".....235642.....",
        "....245654531...",
        "..2356454652310.",
        ".235546356435210",
        ".124354254324110",
        "..0112213211100.",
        "....00000000....",
        "................",
        "................",
    ],
    "oxide": [
        "................",
        "................",
        "................",
        "................",
        ".......66.......",
        "......5676......",
        ".....456765.....",
        "....456676543...",
        "...345667665432.",
        "..23455666554321",
        ".123445565544321",
        ".012334454433210",
        "..0012233322100.",
        "....000000000...",
        "................",
        "................",
    ],
    "fluoride": [
        "................",
        "................",
        "................",
        "................",
        "........7.......",
        ".......676......",
        "......56765.....",
        ".....4567654....",
        "....345676543...",
        "...34566665432..",
        "..2345565654321.",
        "..1234454433210.",
        "..01123332211000",
        "...000000000000.",
        "................",
        "................",
    ],
    "oxalate": [
        "................",
        "................",
        "................",
        "................",
        "................",
        ".......7........",
        "......676.6.....",
        ".....56765765...",
        "....4566545654..",
        "...345654565432.",
        "..23456545654321",
        ".12345654543321.",
        ".01233443322100.",
        "..00000000000...",
        "................",
        "................",
    ],
    "concentrate": [
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....22..22.....",
        "....256212552...",
        "...24563146531..",
        "..2134526415320.",
        ".246521356314510",
        ".135641245213420",
        "..1243113410210.",
        "...01100110000..",
        "................",
        "................",
        "................",
    ],
}
assert all(len(rows) == 16 and all(len(row) == 16 for row in rows) for rows in SHAPES.values())


def paint_item(form, pal):
    tones = ramp(pal)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(SHAPES[form]):
        for x, tone in enumerate(row):
            if tone != ".":
                img.putpixel((x, y), tones[int(tone)] + (255,))
    return img


def paint_block(name, pal):
    """A storage block: a rimmed slab in three cast courses, as vanilla's metal blocks are."""
    tones, rng = ramp(pal), random.Random("block-" + name)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if y == 0 or x == 0:
                tone = 6
            elif y == 15 or x == 15:
                tone = 2
            else:
                course = (y - 1) % 5
                tone = 7 if course == 0 else 3 if course == 4 or y == 14 else 5 if rng.random() < 0.8 else 4
            img.putpixel((x, y), tones[tone] + (255,))
    img.putpixel((0, 15), tones[3] + (255,))
    img.putpixel((15, 0), tones[3] + (255,))
    return img


def paint(material, form):
    pal = palette(material, form)
    return paint_block(material, pal) if form == "block" else paint_item(form, pal)


def contact_sheet(path, scale=6):
    """Every item, one material to a row, on an inventory slot's grey."""
    forms = ["concentrate", "oxide", "dust", "ingot", "nugget", "plate", "block"]
    tile, pad, label = 16 * scale, 8, 170
    sheet = Image.new("RGB", (label + len(forms) * (tile + pad) + pad, len(MATERIALS) * (tile + pad) + pad + 20), (40, 42, 46))
    draw, font = ImageDraw.Draw(sheet), ImageFont.load_default(size=12)
    for i, form in enumerate(forms):
        draw.text((label + pad + i * (tile + pad), 4), form, fill=(226, 228, 232), font=font)
    for row, (material, have) in enumerate(MATERIALS.items()):
        y = 20 + pad + row * (tile + pad)
        draw.text((6, y + tile // 2 - 6), material.replace("_", " "), fill=(226, 228, 232), font=font)
        for i, form in enumerate(forms):
            if form in have:
                slot = Image.new("RGBA", (16, 16), (139, 139, 139, 255))
                slot.alpha_composite(paint(material, form))
                sheet.paste(slot.convert("RGB").resize((tile, tile), Image.NEAREST), (label + pad + i * (tile + pad), y))
    sheet.save(path)


if __name__ == "__main__":
    for material, form, name in items():
        paint(material, form).save(OUT / ("block" if form == "block" else "item") / f"{name}.png")
    print(f"wrote {len(items())} material textures")
    if len(sys.argv) > 1:
        contact_sheet(sys.argv[1])
        print(f"wrote contact sheet {sys.argv[1]}")
