#!/usr/bin/env python3
"""Paints the solar panel and its rack. Edit and re-run; don't hand-edit the PNGs.

    python3 tools/paint_power.py
"""
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/fundamentals/textures/block"

ALUMINIUM = [(86, 90, 98), (138, 144, 152), (186, 192, 200), (228, 232, 238)]
STEEL = [(44, 46, 52), (72, 76, 84), (104, 108, 118), (150, 154, 164)]


def metal(tones, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            tone = 3 if y == 0 or x == 0 else 0 if y == 15 or x == 15 else 2 if rng.random() < 0.75 else 1
            img.putpixel((x, y), tones[tone] + (255,))
    return img


def cells():
    """Nine dark cells in an aluminium frame. Their clipped corners leave a white diamond where four meet."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            gap_x, gap_y = (x - 1) % 5 == 4, (y - 1) % 5 == 4
            if x in (0, 15) or y in (0, 15):
                colour = ALUMINIUM[3] if y == 0 or x == 0 else ALUMINIUM[0]
            elif gap_x and gap_y:
                colour = (206, 212, 222)
            elif gap_x or gap_y:
                colour = (46, 54, 74)
            elif (x + y) % 23 in (9, 10):
                colour = (26, 34, 62)  # the sky in the glass
            else:
                colour = (14, 18, 36) if (y - 1) % 5 in (1, 3) else (18, 24, 44)
            img.putpixel((x, y), colour + (255,))
    return img


if __name__ == "__main__":
    cells().save(OUT / "solar_panel_top.png")
    metal(ALUMINIUM, "solar-frame").save(OUT / "solar_panel_frame.png")
    metal(STEEL, "panel-rack").save(OUT / "panel_rack.png")
    print("wrote the solar panel and rack textures")
