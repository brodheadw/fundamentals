#!/usr/bin/env python3
"""The burnt-out torch: vanilla's two-pixel stick, its head charred black under a cap of grey ash."""
from PIL import Image

from paint_separation import TEXTURES

# the torch template draws the stick from columns 7-8, rows 6-15; its top two rows are the head
WOOD = {10: ((0x6b, 0x4a, 0x2a), (0x52, 0x37, 0x1e))}
HEAD = {6: ((0x5a, 0x55, 0x50), (0x2a, 0x24, 0x20)), 7: ((0x22, 0x1c, 0x18), (0x1a, 0x16, 0x13)),
        8: ((0x3a, 0x2a, 0x1e), (0x2e, 0x22, 0x18)), 9: ((0x4a, 0x33, 0x20), (0x3b, 0x29, 0x1a))}


def burnt_torch():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rows = {**HEAD, **{y: WOOD[10] for y in range(10, 16)}}
    for y, (lit, shaded) in rows.items():
        img.putpixel((7, y), (*lit, 255))
        img.putpixel((8, y), (*shaded, 255))
    return img


def main():
    burnt_torch().save(TEXTURES / "block/burnt_torch.png")


if __name__ == "__main__":
    main()
