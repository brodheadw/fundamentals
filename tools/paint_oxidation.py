#!/usr/bin/env python3
"""Paints what metal turns into in air, and what keeps it from it: each weathering storage block at each stage (bronze going
to a verdigris green, silver blackening with sulfide, the rare earth metals tarnishing, corroding and crumbling to their
oxide's colour), the rusty iron and steel ingots, the inert storage drum and the argon canister. Edit and re-run; don't
hand-edit the PNGs.

    python3 tools/paint_oxidation.py && python3 tools/build_oxidation_data.py

FAMILIES mirrors oxidation/Weathering.java.
"""
import random
from pathlib import Path

from PIL import Image

from paint_materials import METAL, OXIDE, mix, paint_block, paint_item

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/fundamentals/textures"

PATINA = ("", "exposed_", "weathered_", "oxidized_")
TARNISH = ("", "dulled_", "tarnished_", "blackened_")
FLAKING = ("", "tarnished_", "corroded_", "crumbled_")

# Basic copper carbonate and sulfate, the verdigris of a bronze statue: the blue-green of vanilla's oxidised copper, a shade duller.
VERDIGRIS = ((34, 82, 70), (62, 128, 106), (96, 166, 138), (146, 204, 178))
# Silver sulfide, which is what tarnish is: brown-black with a bloom of purple at the edges.
ACANTHITE = ((18, 14, 18), (40, 32, 40), (68, 56, 66), (106, 92, 104))
# Hydrated iron(III) oxide: the orange-brown of rust.
RUST = ((74, 30, 14), (128, 58, 26), (170, 88, 42), (206, 128, 72))

# metal: (stage prefixes, the colour it goes to, honeycomb waxes it, its last stage has crumbled)
FAMILIES = {
    "bronze": (PATINA, VERDIGRIS, True, False),
    "silver": (TARNISH, ACANTHITE, True, False),
    "praseodymium": (FLAKING, OXIDE["praseodymium"], False, True),
    "neodymium": (FLAKING, OXIDE["neodymium"], False, True),
    "samarium": (FLAKING, OXIDE["samarium"], False, True),
    "terbium": (FLAKING, OXIDE["terbium"], False, True),
    "dysprosium": (FLAKING, OXIDE["dysprosium"], False, True),
}


def family_blocks(metal):
    """The blocks a weathering metal adds to its storage block, in Weathering.java's order: the later stages, then the waxed four."""
    prefixes, _, waxable, _ = FAMILIES[metal]
    return [f"{p}{metal}_block" for p in prefixes[1:]] + ([f"waxed_{p}{metal}_block" for p in prefixes] if waxable else [])


BLOCKS = [name for metal in FAMILIES for name in family_blocks(metal)] + ["inert_storage_drum"]

# How far each stage's surface has gone over, and how much of it is spots of the full colour.
DULL = ((40, 40, 44), (78, 78, 84), (110, 110, 116), (150, 150, 156))
COVER = (0.0, 0.3, 0.6, 0.85)
SPOTS = (0.0, 0.12, 0.3, 0.55)

IRON = ((72, 72, 72), (150, 150, 150), (204, 204, 204), (236, 236, 236))
STEEL = ((44, 48, 56), (88, 94, 106), (132, 138, 150), (184, 190, 202))
RUSTY = {"rusty_iron_ingot": IRON, "rusty_steel_ingot": STEEL}

# The drum: The Factory Must Grow's steel, and the dark green that marks an argon cylinder (EN 1089-3, RAL 6001).
DRUM_STEEL = ((38, 42, 50), (66, 72, 82), (98, 104, 116), (140, 146, 158))
ARGON_GREEN = (40, 104, 52)


def stage_blocks():
    """name -> palette-faithful texture, for every stage after the first of every family."""
    out = {}
    for metal, (prefixes, colour, _, crumbles) in FAMILIES.items():
        for stage in range(1, 4):
            name = f"{prefixes[stage]}{metal}_block"
            if crumbles and stage == 3:
                out[name] = crumbled(name, colour)
                continue
            # a rare earth metal dulls to grey before its oxide shows; patina and tarnish are their own colour from the start
            surface = mix(colour, DULL, 0.55) if crumbles else colour
            img = paint_block(f"{metal}-{stage}", mix(METAL[metal], surface, COVER[stage]))
            spotted(img, colour, SPOTS[stage], name)
            out[name] = img
    return out


def spotted(img, colour, share, seed):
    """Blooms of the full colour, clumped as corrosion starts at a flaw and spreads."""
    rng = random.Random("spots-" + seed)
    tones = (colour[0], colour[1], colour[2], colour[3])
    seeds = [(rng.randrange(1, 15), rng.randrange(1, 15)) for _ in range(int(share * 14) + 1)]
    for y in range(1, 15):
        for x in range(1, 15):
            near = min(abs(x - sx) + abs(y - sy) for sx, sy in seeds)
            if rng.random() < share * (1.4 - 0.35 * near):
                img.putpixel((x, y), tones[1 if (x + y) % 3 == 0 else 2] + (255,))
    return img


def crumbled(name, colour):
    """Metal gone to its oxide: a powdery block of the oxide's colour, cracked through, with a last fleck of metal here and there."""
    rng = random.Random("crumbled-" + name)
    img = Image.new("RGBA", (16, 16))
    cracks = set()
    for _ in range(3):
        x, y = rng.randrange(16), 0
        while y < 16:
            cracks.add((x, y))
            x = max(0, min(15, x + rng.choice((-1, 0, 0, 1))))
            y += 1
    for y in range(16):
        for x in range(16):
            tone = colour[0] if (x, y) in cracks else colour[3 if rng.random() < 0.15 else 2 if rng.random() < 0.6 else 1]
            img.putpixel((x, y), tone + (255,))
    return img


def rusty(name, pal):
    img = paint_item("ingot", mix(pal, RUST, 0.55))
    rng = random.Random(name)
    body = [(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3] and 0 < x < 15]
    for x, y in rng.sample(body, len(body) // 3):
        img.putpixel((x, y), RUST[rng.randrange(1, 4)] + (255,))
    return img


def drum_side():
    rng = random.Random("drum-side")
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if y in (0, 15):
                colour = DRUM_STEEL[0]
            elif y in (3, 12):
                colour = DRUM_STEEL[3]
            elif y in (4, 13):
                colour = DRUM_STEEL[0]
            elif 7 <= y <= 8:
                colour = ARGON_GREEN if y == 7 else tuple(int(c * 0.75) for c in ARGON_GREEN)
            else:
                # light from the left, round the curve of the drum
                colour = DRUM_STEEL[2] if x < 5 else DRUM_STEEL[1] if x < 12 or rng.random() < 0.3 else DRUM_STEEL[0]
            img.putpixel((x, y), colour + (255,))
    return img


def drum_top(bung):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            rim = x in (0, 15) or y in (0, 15)
            ring = x in (1, 14) or y in (1, 14)
            colour = DRUM_STEEL[0] if rim else DRUM_STEEL[3] if ring else DRUM_STEEL[2] if (x * 7 + y * 3) % 11 else DRUM_STEEL[1]
            img.putpixel((x, y), colour + (255,))
    if bung:
        for x, y in ((10, 10), (11, 10), (10, 11), (11, 11)):
            img.putpixel((x, y), (ARGON_GREEN if (x, y) == (10, 10) else DRUM_STEEL[0]) + (255,))
        for x, y in ((4, 4), (5, 4), (4, 5), (5, 5)):
            img.putpixel((x, y), DRUM_STEEL[0 if (x, y) == (5, 5) else 3] + (255,))
    return img


def canister(shoulder):
    """A little gas cylinder standing upright: a steel body, its shoulder painted for the gas, a brass valve on top."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    brass = ((120, 90, 30), (190, 150, 60), (230, 200, 110))
    for y in range(1, 4):
        for x in range(7, 9 + (1 if y == 2 else 0)):
            img.putpixel((x, y), brass[2 if x == 7 else 1 if y < 3 else 0] + (255,))
    for y in range(4, 15):
        width = 2 if y == 4 else 3 if y == 5 else 4
        for x in range(8 - width, 8 + width):
            edge = x in (8 - width, 8 + width - 1) or y == 14
            if y <= 6 and shoulder:
                colour = tuple(int(c * (0.6 if edge else 1.2 if x < 7 else 1.0)) for c in shoulder)
            else:
                colour = DRUM_STEEL[0] if edge else DRUM_STEEL[3] if x < 6 else DRUM_STEEL[2] if x < 9 else DRUM_STEEL[1]
            img.putpixel((x, y), tuple(min(255, c) for c in colour) + (255,))
    return img


def main():
    for name, img in stage_blocks().items():
        img.save(OUT / "block" / f"{name}.png")
    for name, pal in RUSTY.items():
        rusty(name, pal).save(OUT / "item" / f"{name}.png")
    drum_side().save(OUT / "block/inert_storage_drum_side.png")
    drum_top(True).save(OUT / "block/inert_storage_drum_top.png")
    drum_top(False).save(OUT / "block/inert_storage_drum_bottom.png")
    canister(None).save(OUT / "item/canister.png")
    canister(ARGON_GREEN).save(OUT / "item/argon_canister.png")
    print(f"wrote {len(stage_blocks())} weathered blocks, the rusty ingots, the drum and the canisters")


if __name__ == "__main__":
    main()
