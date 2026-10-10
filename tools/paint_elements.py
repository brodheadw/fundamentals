#!/usr/bin/env python3
import sys

from PIL import Image
from common import TEXTURES

TILES = TEXTURES / "item/element"
BACKGROUND = TEXTURES / "gui/advancements/backgrounds/periodic_table.png"

NAMES = """H Hydrogen,He Helium,Li Lithium,Be Beryllium,B Boron,C Carbon,N Nitrogen,O Oxygen,F Fluorine,Ne Neon,
Na Sodium,Mg Magnesium,Al Aluminium,Si Silicon,P Phosphorus,S Sulfur,Cl Chlorine,Ar Argon,K Potassium,Ca Calcium,
Sc Scandium,Ti Titanium,V Vanadium,Cr Chromium,Mn Manganese,Fe Iron,Co Cobalt,Ni Nickel,Cu Copper,Zn Zinc,
Ga Gallium,Ge Germanium,As Arsenic,Se Selenium,Br Bromine,Kr Krypton,Rb Rubidium,Sr Strontium,Y Yttrium,Zr Zirconium,
Nb Niobium,Mo Molybdenum,Tc Technetium,Ru Ruthenium,Rh Rhodium,Pd Palladium,Ag Silver,Cd Cadmium,In Indium,Sn Tin,
Sb Antimony,Te Tellurium,I Iodine,Xe Xenon,Cs Caesium,Ba Barium,La Lanthanum,Ce Cerium,Pr Praseodymium,Nd Neodymium,
Pm Promethium,Sm Samarium,Eu Europium,Gd Gadolinium,Tb Terbium,Dy Dysprosium,Ho Holmium,Er Erbium,Tm Thulium,Yb Ytterbium,
Lu Lutetium,Hf Hafnium,Ta Tantalum,W Tungsten,Re Rhenium,Os Osmium,Ir Iridium,Pt Platinum,Au Gold,Hg Mercury,
Tl Thallium,Pb Lead,Bi Bismuth,Po Polonium,At Astatine,Rn Radon,Fr Francium,Ra Radium,Ac Actinium,Th Thorium,
Pa Protactinium,U Uranium,Np Neptunium,Pu Plutonium,Am Americium,Cm Curium,Bk Berkelium,Cf Californium,Es Einsteinium,Fm Fermium,
Md Mendelevium,No Nobelium,Lr Lawrencium,Rf Rutherfordium,Db Dubnium,Sg Seaborgium,Bh Bohrium,Hs Hassium,Mt Meitnerium,Ds Darmstadtium,
Rg Roentgenium,Cn Copernicium,Nh Nihonium,Fl Flerovium,Mc Moscovium,Lv Livermorium,Ts Tennessine,Og Oganesson"""

FAMILIES = {
    "alkali_metal": "Li Na K Rb Cs Fr",
    "alkaline_earth": "Be Mg Ca Sr Ba Ra",
    "post_transition": "Al Ga In Sn Tl Pb Bi Po Nh Fl Mc Lv",
    "metalloid": "B Si Ge As Sb Te",
    "nonmetal": "H C N O P S Se",
    "halogen": "F Cl Br I At Ts",
    "noble_gas": "He Ne Ar Kr Xe Rn Og",
}

COLOURS = {
    "alkali_metal": ((178, 54, 52), (112, 30, 30)),
    "alkaline_earth": ((196, 114, 34), (124, 68, 18)),
    "transition": ((54, 100, 158), (30, 58, 98)),
    "post_transition": ((70, 126, 112), (40, 76, 66)),
    "metalloid": ((112, 120, 52), (66, 72, 28)),
    "nonmetal": ((52, 138, 62), (28, 84, 34)),
    "halogen": ((30, 136, 150), (16, 82, 92)),
    "noble_gas": ((118, 70, 160), (70, 40, 98)),
    "lanthanide": ((176, 62, 120), (108, 34, 72)),
    "actinide": ((128, 52, 78), (76, 28, 44)),
}


def family(z, symbol):
    if 57 <= z <= 71:
        return "lanthanide"
    if 89 <= z <= 103:
        return "actinide"
    return next((name for name, members in FAMILIES.items() if symbol in members.split()), "transition")


ELEMENTS = {z: (symbol, name, family(z, symbol))
            for z, (symbol, name) in enumerate((entry.split() for entry in NAMES.replace("\n", "").split(",")), start=1)}
assert len(ELEMENTS) == 118


def cell(z):
    starts = (1, 3, 11, 19, 37, 55, 87, 119)
    period = next(p for p in range(7) if z < starts[p + 1])
    i = z - starts[period]
    if period == 0:
        return (0 if z == 1 else 17), 0
    if period in (1, 2):
        return (i if i < 2 else i + 10), period
    if period in (3, 4):
        return i, period
    if 2 <= i <= 16:
        return i, period + 2.5
    return (i if i < 2 else i - 14), period


GLYPHS = {
    "A": ".#.|#.#|###|#.#|#.#", "B": "##.|#.#|##.|#.#|##.", "C": ".##|#..|#..|#..|.##", "D": "##.|#.#|#.#|#.#|##.",
    "E": "###|#..|##.|#..|###", "F": "###|#..|##.|#..|#..", "G": ".##|#..|#.#|#.#|.##", "H": "#.#|#.#|###|#.#|#.#",
    "I": "###|.#.|.#.|.#.|###", "K": "#.#|#.#|##.|#.#|#.#", "L": "#..|#..|#..|#..|###", "M": "###|###|#.#|#.#|#.#",
    "N": "##.|#.#|#.#|#.#|#.#", "O": ".#.|#.#|#.#|#.#|.#.", "P": "##.|#.#|##.|#..|#..", "R": "##.|#.#|##.|#.#|#.#",
    "S": ".##|#..|.#.|..#|##.", "T": "###|.#.|.#.|.#.|.#.", "U": "#.#|#.#|#.#|#.#|###", "V": "#.#|#.#|#.#|#.#|.#.",
    "W": "#.#|#.#|#.#|###|###", "X": "#.#|#.#|.#.|#.#|#.#", "Y": "#.#|#.#|.#.|.#.|.#.", "Z": "###|..#|.#.|#..|###",
    "a": "...|##.|.##|#.#|.##", "b": "#..|#..|##.|#.#|##.", "c": "...|.##|#..|#..|.##", "d": "..#|..#|.##|#.#|.##",
    "e": "...|.#.|###|#..|.##", "f": ".##|#..|##.|#..|#..", "g": ".##|#.#|.##|..#|##.", "h": "#..|#..|##.|#.#|#.#",
    "i": ".#.|...|.#.|.#.|.#.", "k": "#..|#.#|##.|#.#|#.#", "l": "##.|.#.|.#.|.#.|.##", "m": "...|##.|###|#.#|#.#",
    "n": "...|##.|#.#|#.#|#.#", "o": "...|.#.|#.#|#.#|.#.", "p": "...|##.|#.#|##.|#..", "r": "...|#.#|##.|#..|#..",
    "s": "...|.##|##.|..#|##.", "t": ".#.|###|.#.|.#.|..#", "u": "...|#.#|#.#|#.#|.##", "v": "...|#.#|#.#|#.#|.#.",
    "y": "#.#|#.#|.##|..#|##.",
}

INK = (246, 246, 242, 255)


def tile(symbol, fill, rim):
    img = Image.new("RGBA", (16, 16), rim + (255,))
    for x in range(1, 15):
        for y in range(1, 15):
            img.putpixel((x, y), fill + (255,))
    light = tuple(min(255, c + 34) for c in fill) + (255,)
    for i in range(1, 15):
        img.putpixel((i, 1), light)
        img.putpixel((1, i), light)
    width = 6 * len(symbol) + (len(symbol) - 1)
    left = (16 - width + 1) // 2
    for n, letter in enumerate(symbol):
        for row, line in enumerate(GLYPHS[letter].split("|")):
            for col, on in enumerate(line):
                if on == "#":
                    for dx in (0, 1):
                        for dy in (0, 1):
                            img.putpixel((left + n * 7 + col * 2 + dx, 3 + row * 2 + dy), INK)
    return img


def table_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for z, (_, _, fam) in ELEMENTS.items():
        col, row = cell(z)
        if row > 7:
            x, y = int(col) - 1, int(row) + 4
            if not 1 <= x <= 14:
                continue
        else:
            x = col if col < 2 else col - 2 if col >= 12 else 2 + (col - 2) * 8 // 10
            y = row + 3
        img.putpixel((x, y), COLOURS[fam][0] + (255,))
    return img


def background():
    img = Image.new("RGBA", (16, 16), (22, 24, 30, 255))
    for i in range(16):
        img.putpixel((i, 0), (30, 33, 41, 255))
        img.putpixel((0, i), (30, 33, 41, 255))
    img.putpixel((0, 0), (38, 42, 52, 255))
    return img


def main():
    TILES.mkdir(parents=True, exist_ok=True)
    for stale in TILES.glob("*.png"):
        stale.unlink()
    for z, (symbol, _, fam) in ELEMENTS.items():
        tile(symbol, *COLOURS[fam]).save(TILES / f"{symbol.lower()}.png")
    table_icon().save(TEXTURES / "item/element.png")
    BACKGROUND.parent.mkdir(parents=True, exist_ok=True)
    background().save(BACKGROUND)
    if len(sys.argv) > 1:
        sheet = Image.new("RGBA", (18 * 17, 10 * 17 + 8), (22, 24, 30, 255))
        for z, (symbol, _, fam) in ELEMENTS.items():
            col, row = cell(z)
            sheet.paste(tile(symbol, *COLOURS[fam]), (int(col * 17), int(row * 17)))
        icon = table_icon().resize((16, 16))
        sheet.paste(icon, (6 * 17, 17), icon)
        sheet.resize((sheet.width * 4, sheet.height * 4), Image.NEAREST).save(sys.argv[1])
    print(f"painted {len(ELEMENTS)} element tiles")


if __name__ == "__main__":
    main()
