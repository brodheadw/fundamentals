#!/usr/bin/env python3
"""Paints the titanium pipe, pump, valve and tank: Create's own textures (MIT), their copper redrawn as titanium plate, a cool
grey whose shadows go faintly blue and highlights faintly straw, the thin oxide that heat and anodising leave on it. The iron,
the glass, the bore and the valve's dial are left as they are. Edit and re-run; don't hand-edit the PNGs.

    python3 tools/paint_titanium.py
"""
from paint_separation import TEXTURES, create_texture

# Create's copper, every tone its pipes, pump, valve and tank use, darkest (the inside of a bore) first; and titanium tone for tone,
# blue-grey in the seams, straw on the glints.
COPPER = [(61, 22, 30), (91, 41, 36), (121, 59, 43), (144, 73, 49), (154, 80, 56), (167, 90, 64), (178, 98, 71), (194, 107, 76),
          (200, 116, 86), (214, 123, 91), (227, 130, 108)]
TITANIUM = [(38, 41, 53), (56, 60, 74), (76, 80, 95), (97, 101, 114), (112, 115, 126), (128, 131, 138), (143, 145, 149), (158, 159, 160),
            (171, 171, 169), (186, 185, 179), (204, 201, 189)]
SHEETS = {
    "pipes": "titanium_pipes", "pipes_connected": "titanium_pipes_connected", "pump": "titanium_pump", "fluid_valve": "titanium_fluid_valve",
    "valve_open": "titanium_valve_open", "valve_closed": "titanium_valve_closed",
    **{f"fluid_tank{sheet}": f"titanium_fluid_tank{sheet}" for sheet in ("", "_connected", "_top", "_top_connected", "_inner", "_inner_connected",
                                                                        "_window", "_window_single")},
}


def titanium(img):
    swap = dict(zip(COPPER, TITANIUM))
    out = img.copy()
    out.putdata([swap.get((r, g, b), (r, g, b)) + (a,) if a == 255 else (r, g, b, a) for r, g, b, a in img.getdata()])
    return out


def main():
    for source, name in SHEETS.items():
        titanium(create_texture(source)).save(TEXTURES / f"block/{name}.png")
    print(f"wrote {len(SHEETS)} titanium textures")


if __name__ == "__main__":
    main()
