#!/usr/bin/env python3
"""Paints the items the rare earths are spent on: the phosphor and the didymium glass. Edit and
re-run; don't hand-edit the PNGs.

    python3 tools/paint_uses.py
"""
from PIL import Image

from paint_minerals import paint_raw
from paint_separation import TEXTURES, heap


def lens():
    """A square of didymium glass: the grey-violet pane of a welder's goggle, with a highlight."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    body, edge, light = (124, 110, 150, 210), (74, 62, 96, 255), (214, 204, 232, 230)
    for x in range(2, 14):
        for y in range(2, 14):
            on_edge = x in (2, 13) or y in (2, 13)
            img.putpixel((x, y), edge if on_edge else body)
    for x, y in ((4, 4), (5, 4), (4, 5), (6, 4), (4, 6)):
        img.putpixel((x, y), light)
    return img


def filament():
    """A coil of tungsten wire: a zigzag of bright grey across the sprite."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wire, light = (150, 154, 162, 255), (214, 218, 226, 255)
    for x in range(2, 14):
        y = 8 + (2 if (x // 2) % 2 == 0 else -2)
        img.putpixel((x, y), light if x % 4 == 0 else wire)
        img.putpixel((x, y + 1), wire)
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
    # tenorite, CuO, is black; zinc oxide is white; nickel oxide is green, dulled here by the pentlandite's iron oxide
    paint_raw("copper_calcine", ((22, 20, 20), (48, 44, 42), (78, 72, 68), (122, 114, 108))).save(TEXTURES / "item/copper_calcine.png")
    heap("zinc_oxide", (252, 252, 248), (222, 222, 214), (160, 160, 152)).save(TEXTURES / "item/zinc_oxide.png")
    paint_raw("roasted_pentlandite", ((40, 46, 34), (72, 84, 60), (108, 122, 90), (156, 168, 132))).save(TEXTURES / "item/roasted_pentlandite.png")
    # lithium chloride is white; ferroboron a grey metal lump
    heap("lithium_chloride", (255, 255, 255), (236, 238, 238), (182, 186, 188)).save(TEXTURES / "item/lithium_chloride.png")
    paint_raw("ferroboron", ((58, 60, 64), (104, 108, 114), (150, 154, 160), (200, 204, 210))).save(TEXTURES / "item/ferroboron.png")
    # soda ash is white; sodium chromate lemon yellow and the dichromate orange-red, as chromate salts are; aluminium powder dull silver
    heap("soda_ash", (254, 254, 252), (232, 232, 228), (178, 178, 172)).save(TEXTURES / "item/soda_ash.png")
    heap("sodium_chromate", (252, 240, 110), (234, 206, 34), (168, 138, 18)).save(TEXTURES / "item/sodium_chromate.png")
    heap("sodium_dichromate", (255, 160, 80), (226, 98, 28), (150, 52, 16)).save(TEXTURES / "item/sodium_dichromate.png")
    heap("aluminium_powder", (224, 226, 230), (172, 176, 184), (110, 114, 122)).save(TEXTURES / "item/aluminium_powder.png")
    print("uses textures written")


if __name__ == "__main__":
    main()
