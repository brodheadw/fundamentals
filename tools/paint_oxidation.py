#!/usr/bin/env python3
import random

from PIL import Image

from paint_materials import METAL, OXIDE, mix, paint_block, paint_item
from common import TEXTURES


PATINA = ("", "exposed_", "weathered_", "oxidized_")
TARNISH = ("", "dulled_", "tarnished_", "blackened_")
FLAKING = ("", "tarnished_", "corroded_", "crumbled_")

VERDIGRIS = ((34, 82, 70), (62, 128, 106), (96, 166, 138), (146, 204, 178))
ACANTHITE = ((18, 14, 18), (40, 32, 40), (68, 56, 66), (106, 92, 104))
RUST = ((74, 30, 14), (128, 58, 26), (170, 88, 42), (206, 128, 72))

# Mirrors oxidation/Weathering.java.
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
    prefixes, _, waxable, _ = FAMILIES[metal]
    return [f"{p}{metal}_block" for p in prefixes[1:]] + ([f"waxed_{p}{metal}_block" for p in prefixes] if waxable else [])


BLOCKS = [name for metal in FAMILIES for name in family_blocks(metal)] + ["inert_storage_drum"]

DULL = ((40, 40, 44), (78, 78, 84), (110, 110, 116), (150, 150, 156))
COVER = (0.0, 0.3, 0.6, 0.85)
SPOTS = (0.0, 0.12, 0.3, 0.55)

IRON = ((72, 72, 72), (150, 150, 150), (204, 204, 204), (236, 236, 236))
STEEL = ((44, 48, 56), (88, 94, 106), (132, 138, 150), (184, 190, 202))
RUSTY = {"rusty_iron_ingot": IRON, "rusty_steel_ingot": STEEL}

DRUM_STEEL = ((38, 42, 50), (66, 72, 82), (98, 104, 116), (140, 146, 158))
ARGON_GREEN = (40, 104, 52)


CRUST = ((128, 128, 124), (178, 178, 172), (212, 212, 206), (236, 236, 230))
LANTHANIDES = ("lanthanum", "cerium", "praseodymium", "didymium", "neodymium", "samarium", "gadolinium", "terbium", "dysprosium", "yttrium")
NUGGETS = ("praseodymium", "neodymium", "samarium", "terbium", "dysprosium")
STAGED = {
    "minecraft:copper_ingot": ("copper", 3),
    **{f"fundamentals:{m}_{form}": (m, 3) for m in ("bronze", "silver") for form in ("ingot", "nugget", "plate")},
    "fundamentals:calcium_ingot": ("calcium", 3), "fundamentals:magnesium_ingot": ("magnesium", 3),
    **{f"fundamentals:{m}_ingot": (m, 2) for m in LANTHANIDES},
    **{f"fundamentals:{m}_nugget": (m, 2) for m in NUGGETS},
}
COPPER = ((96, 44, 28), (164, 82, 54), (214, 126, 88), (246, 176, 136))


def ages_to(metal):
    if metal in ("copper", "bronze"):
        return "patina", VERDIGRIS, VERDIGRIS
    if metal == "silver":
        return "tarnish", ACANTHITE, ACANTHITE
    if metal in ("calcium", "magnesium"):
        return "crust", CRUST, CRUST
    return "oxide", mix(OXIDE[metal], DULL, 0.55), mix(OXIDE[metal], ((236, 236, 232),) * 4, 0.35)


def fresh(item):
    namespace, name = item.split(":")
    return paint_item("ingot", COPPER) if namespace == "minecraft" else Image.open(TEXTURES / "item" / f"{name}.png").convert("RGBA")


def outline(px, body):
    return [p for p in body if any(px[x, y][3] == 0 if 0 <= x < 16 and 0 <= y < 16 else True
                                   for x, y in ((p[0] + 1, p[1]), (p[0] - 1, p[1]), (p[0], p[1] + 1), (p[0], p[1] - 1)))]


def aged(item, stage):
    metal, stages = STAGED[item]
    kind, surface, blotch = ages_to(metal)
    t = stage / stages
    img = fresh(item)
    px = img.load()
    rng = random.Random(f"aged-{item}-{stage}")
    body = [(x, y) for y in range(16) for x in range(16) if px[x, y][3]]
    rim = set(outline(px, body))
    lum = {p: sum(px[p][:3]) for p in body}
    lo, hi = min(lum.values()), max(lum.values())
    cover = 0.2 + 0.75 * t if kind == "patina" else 0.45 + 0.5 * t
    for p in body:
        k = (lum[p] - lo) / max(1, hi - lo) * 3
        if kind in ("oxide", "crust"):
            k = 0.8 + k * 0.45
        tone = tuple(int(a + (b - a) * (k - int(k))) for a, b in zip(surface[int(k)], surface[min(3, int(k) + 1)]))
        px[p] = tuple(int(c + (d - c) * cover) for c, d in zip(px[p][:3], tone)) + (255,)
    inner = [p for p in body if p not in rim]
    flaws = rng.sample(inner, min(len(inner), 1 + 2 * stage))
    spread = 1.2 + 1.4 * t
    for p in inner:
        near = min(abs(p[0] - fx) + abs(p[1] - fy) for fx, fy in flaws)
        if near <= spread and rng.random() < 0.9 - 0.15 * near:
            if kind == "tarnish":
                px[p] = blotch[0 if near < spread - 0.8 else 3 if rng.random() < 0.5 else 1] + (255,)
            elif kind == "patina":
                px[p] = blotch[2 if (p[0] + p[1]) % 3 else 3] + (255,)
            else:
                px[p] = blotch[3 if rng.random() < 0.6 else 2] + (255,)
    for p in rng.sample(inner, min(len(inner), (len(inner) * stage) // 14)):
        px[p] = (surface[0] if kind != "tarnish" else (12, 10, 12)) + (255,)
    if kind == "patina":
        tops = {}
        for x, y in inner:
            tops.setdefault(x, y)
        for x in rng.sample(sorted(tops), min(len(tops), 2 * stage)):
            for y in range(tops[x], tops[x] + rng.randrange(2, 3 + 2 * stage)):
                if (x, y) in inner:
                    px[x, y] = VERDIGRIS[3] + (255,)
    if kind == "oxide" and stage == stages:
        bottom = max(y for _, y in body)
        gone = [p for p in rim if p[1] < bottom - 1 and rng.random() < 0.3]
        for p in gone:
            px[p] = (0, 0, 0, 0)
        left = [p for p in body if p not in gone]
        for p in left:
            if not any(px[x, y][3] for x, y in ((p[0] + 1, p[1]), (p[0] - 1, p[1]), (p[0], p[1] + 1), (p[0], p[1] - 1))
                       if 0 <= x < 16 and 0 <= y < 16):
                px[p] = (0, 0, 0, 0)
        left = [p for p in left if px[p][3]]
        for p in outline(px, left):
            px[p] = surface[0] + (255,)
        under = sorted({x for x, y in left if y == max(yy for xx, yy in left if xx == x)})
        for x in rng.sample(under, min(len(under), 3)):
            y = max(yy for xx, yy in left if xx == x) + 1
            if y < 16:
                px[x, y] = blotch[rng.randrange(1, 4)] + (255,)
    return img


def aged_name(item, stage):
    return f"{item.split(':')[1]}_{stage}"


def stage_blocks():
    out = {}
    for metal, (prefixes, colour, _, crumbles) in FAMILIES.items():
        for stage in range(1, 4):
            name = f"{prefixes[stage]}{metal}_block"
            if crumbles and stage == 3:
                out[name] = crumbled(name, colour)
                continue
            surface = mix(colour, DULL, 0.55) if crumbles else colour
            img = paint_block(f"{metal}-{stage}", mix(METAL[metal], surface, COVER[stage]))
            spotted(img, colour, SPOTS[stage], name)
            out[name] = img
    return out


def spotted(img, colour, share, seed):
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


def body_mask():
    return {(x, z) for x in range(2, 14) for z in range(2, 14) if 3 <= x < 13 or 3 <= z < 13}


def drum_side():
    rng = random.Random("drum-side")
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            curve = DRUM_STEEL[3] if x in (4, 5) else DRUM_STEEL[2] if x < 9 else DRUM_STEEL[1] if x < 12 or rng.random() < 0.5 else DRUM_STEEL[0]
            if y == 2:
                colour = DRUM_STEEL[3]
            elif y == 15:
                colour = DRUM_STEEL[0]
            elif 3 <= y <= 4:
                shade = 1.15 if x in (4, 5) else 1.0 if x < 9 else 0.8
                colour = tuple(min(255, int(c * shade * (1 if y == 3 else 0.82))) for c in ARGON_GREEN)
            elif x in (3, 12) and y % 3 == 0:
                colour = DRUM_STEEL[3] if x == 3 else DRUM_STEEL[2]
            elif x in (2, 13):
                colour = DRUM_STEEL[0]
            else:
                colour = curve
            img.putpixel((x, y), colour + (255,))
    return img


def drum_lid(bung):
    mask = body_mask()
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x, z in mask:
        edge = sum((x + dx, z + dz) not in mask for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        inner = not edge and any((x + dx, z + dz) not in mask for dx in (-1, 0, 1) for dz in (-1, 0, 1))
        colour = DRUM_STEEL[3] if edge and (x < 8 or z < 8) else DRUM_STEEL[1] if edge else DRUM_STEEL[0] if inner \
            else DRUM_STEEL[2] if (x * 7 + z * 3) % 11 else DRUM_STEEL[1]
        img.putpixel((x, z), colour + (255,))
    if bung:
        for x, z in ((5, 10), (6, 10), (5, 11), (6, 11)):
            img.putpixel((x, z), DRUM_STEEL[0 if (x, z) == (6, 11) else 3] + (255,))
    return img


BRASS = ((96, 64, 24), (166, 120, 48), (214, 172, 82), (246, 218, 140))
HANDWHEEL = ((92, 22, 20), (156, 38, 32), (204, 64, 52))
DIAL = (236, 232, 218)


def drum_fittings():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    put = lambda x, y, c: img.putpixel((x, y), c + (255,))
    for x in range(16):
        put(x, 0, DRUM_STEEL[3] if x < 6 else DRUM_STEEL[2])
        put(x, 1, DRUM_STEEL[0])
    for y in range(4, 10):
        for x in range(6):
            rim = x in (0, 5) or y in (4, 9)
            put(x, y, (BRASS[2] if x + y < 10 else BRASS[1]) if rim else DIAL)
    for x, y in ((1, 6), (1, 5), (2, 5)):
        put(x, y, (110, 106, 98))
    for x, y in ((3, 6), (4, 5)):
        put(x, y, (190, 30, 28))
    put(2, 7, (30, 28, 26))
    for y in range(4, 6):
        for x in range(6, 12):
            put(x, y, BRASS[2] if y == 4 else BRASS[1])
    for y in range(8, 12):
        for x in range(8, 12):
            put(x, y, BRASS[3] if x == 8 else BRASS[2] if x < 10 else BRASS[1] if x < 11 else BRASS[0])
    for y in range(10, 14):
        for x in range(12, 16):
            hub = (x, y) in ((13, 11), (14, 11), (13, 12), (14, 12))
            put(x, y, BRASS[3] if (x, y) == (13, 11) else BRASS[1] if hub else HANDWHEEL[2] if x + y < 25 else HANDWHEEL[1])
    for x in range(12, 16):
        put(x, 14, HANDWHEEL[1])
    return img


def canister(shoulder):
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
        img.save(TEXTURES / "block" / f"{name}.png")
    for name, pal in RUSTY.items():
        rusty(name, pal).save(TEXTURES / "item" / f"{name}.png")
    drum_side().save(TEXTURES / "block/inert_storage_drum_side.png")
    drum_lid(True).save(TEXTURES / "block/inert_storage_drum_top.png")
    drum_lid(False).save(TEXTURES / "block/inert_storage_drum_bottom.png")
    drum_fittings().save(TEXTURES / "block/inert_storage_drum_fittings.png")
    (TEXTURES / "item/aged").mkdir(exist_ok=True)
    for item, (_, stages) in STAGED.items():
        for stage in range(1, stages + 1):
            aged(item, stage).save(TEXTURES / "item/aged" / f"{aged_name(item, stage)}.png")
    canister(None).save(TEXTURES / "item/canister.png")
    canister(ARGON_GREEN).save(TEXTURES / "item/argon_canister.png")
    print(f"wrote {len(stage_blocks())} weathered blocks, the rusty ingots, the drum and the canisters")


if __name__ == "__main__":
    main()
