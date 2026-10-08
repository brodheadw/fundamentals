#!/usr/bin/env python3
"""Paints the items the rare earths are spent on: the phosphor and the didymium glass. Edit and
re-run; don't hand-edit the PNGs.

    python3 tools/paint_uses.py
"""
from PIL import Image

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


def main():
    heap("phosphor", (255, 250, 252), (240, 226, 236), (196, 170, 190)).save(TEXTURES / "item/phosphor.png")
    lens().save(TEXTURES / "item/didymium_glass.png")
    print("uses textures written")


if __name__ == "__main__":
    main()
