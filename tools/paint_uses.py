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
    print("uses textures written")


if __name__ == "__main__":
    main()
