#!/usr/bin/env python3
"""Paints the solar panel. Edit and re-run; don't hand-edit the PNGs.

    python3 tools/paint_power.py
"""
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/fundamentals/textures/block"

FRAME = [(92, 96, 104), (140, 146, 154), (184, 190, 198), (224, 228, 234)]
CELL = [(14, 20, 48), (22, 34, 82), (32, 50, 112), (60, 92, 168)]
WIRE = (112, 124, 156)


def top():
    """Nine cells in an aluminium frame, each crossed by a silver collector line."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                colour = FRAME[3] if y == 0 or x == 0 else FRAME[0]
            elif x in (5, 10) or y in (5, 10):
                colour = CELL[0]
            elif (x - 1) % 5 == 1:
                colour = WIRE
            else:
                colour = CELL[3] if x + y in (8, 9) else CELL[2] if x + y < 8 else CELL[1]  # the sun on the glass
            img.putpixel((x, y), colour + (255,))
    img.putpixel((0, 15), FRAME[1] + (255,))
    img.putpixel((15, 0), FRAME[1] + (255,))
    return img


def side():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            tone = 3 if y % 4 == 0 else 0 if y % 4 == 3 else 2 if x % 8 else 1
            img.putpixel((x, y), FRAME[tone] + (255,))
    return img


if __name__ == "__main__":
    top().save(OUT / "solar_panel_top.png")
    side().save(OUT / "solar_panel_side.png")
    print("wrote the solar panel textures")
