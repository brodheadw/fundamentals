#!/usr/bin/env python3
import gzip
import json
import struct

from common import ASSETS, JAVA, read_lang, write_lang

SCENE = "mixer_settler"
PONDER_TEXT = JAVA / "client/ponder/MixerSettlerPonderText.java"

HEADER = "Parting the rare earths in a mixer-settler battery"
TEXTS = [
    "Casings placed facing the same way merge into one vat: three across, three along, one or two tall. Nine casings are a lab vat, eighteen a plant vat.",
    "The back row is the mixing box. A Mechanical Mixer standing over its hatch, driven by a cogwheel beside it, beats the two liquids together; the rows ahead are the settling bay, where they part again.",
    "Charge the extractant, P507 here, through the top. It floats on the liquor and is never used up: it only ferries the heavier rare earths forward, stage by stage.",
    "The liquor goes in at the back of the first stage and the strip acid at the front of the last. The raffinate leaves the first stage by its sides, the loaded strip the last stage by its sides.",
    "A lever on the head stage starts the battery. Stages standing end to end are one battery, and each cut needs a set number: eight to part the lights from the heavies, thirty-two to part neodymium from praseodymium.",
    "Every cut leaves a fifth of a batch of spent liquor in a sump under the head stage. Pump it out from below and neutralise it with lime, or the battery stops when the sump is full.",
    "Goggles on any casing tell you what the battery holds, what it is waiting for, and how far the organic and the liquor have got down the line.",
]


def tag(t, value):
    if t == 1: return struct.pack(">b", value)
    if t == 3: return struct.pack(">i", value)
    if t == 8:
        b = value.encode("utf-8"); return struct.pack(">H", len(b)) + b
    if t == 9:
        et, items = value
        return struct.pack(">bi", et, len(items)) + b"".join(tag(et, i) for i in items)
    if t == 10:
        out = b""
        for name, (vt, v) in value.items():
            out += struct.pack(">b", vt) + tag(8, name) + tag(vt, v)
        return out + b"\x00"
    if t == 11:
        return struct.pack(">i", len(value)) + b"".join(struct.pack(">i", v) for v in value)
    raise ValueError(t)


def compound(d):
    return (10, d)


def string(s):
    return (8, s)


def integer(i):
    return (3, i)


def int_array(v):
    return (11, list(v))


def int_list(v):
    return (9, (3, list(v)))


def fluid(id, amount):
    return compound({"Fluid": compound({"id": string(f"fundamentals:{id}"), "amount": integer(amount)})})


def structure(size, blocks):
    palette, index = [], {}
    entries = []
    for x, y, z, name, props, nbt in blocks:
        key = (name, tuple(sorted(props.items())))
        if key not in index:
            index[key] = len(palette)
            entry = {"Name": string(name)}
            if props:
                entry["Properties"] = compound({k: string(v) for k, v in props.items()})
            palette.append(compound(entry))
        block = {"pos": int_list((x, y, z)), "state": integer(index[key])}
        if nbt is not None:
            block["nbt"] = nbt
        entries.append(compound(block))
    root = compound({"size": int_list(size), "entities": (9, (10, [])), "blocks": (9, (10, [e[1] for e in entries])),
                     "palette": (9, (10, [p[1] for p in palette])), "DataVersion": integer(3955)})
    return gzip.compress(b"\x0a" + tag(8, "") + tag(10, root[1]), mtime=0)


def scene_blocks():
    blocks = []
    for x in range(9):
        for z in range(7):
            blocks.append((x, 0, z, "minecraft:white_concrete", {}, None))
    origin = (3, 1, 2)
    w, l, h = 3, 3, 2
    for a in range(w):
        for l0 in range(l):
            for u in range(h):
                x, y, z = origin[0] + l0, origin[1] + u, origin[2] + a
                props = {"facing": "east", "left": str(a > 0).lower(), "right": str(a < w - 1).lower(), "back": str(l0 > 0).lower(),
                         "front": str(l0 < l - 1).lower(), "below": str(u > 0).lower(), "above": str(u < h - 1).lower(),
                         "rows": "well" if l0 == 0 else "bay", "open": str(l0 == 0 and u == h - 1 and a == w // 2).lower(),
                         "window": str(a == w // 2 or l0 == l // 2).lower(), "link_ahead": "false", "link_behind": "false"}
                nbt = {"controller": int_array(origin), "size": int_array((w, l, h))}
                if (x, y, z) == origin:
                    nbt["organic"] = compound({"id": string("fundamentals:p507"), "amount": integer(2250)})
                    nbt["aqueous"] = compound({"id": string("fundamentals:light_rare_earth_liquor"), "amount": integer(1800)})
                blocks.append((x, y, z, "fundamentals:mixer_settler", props, compound(nbt)))
    hatch = (origin[0], origin[1] + h - 1, origin[2] + w // 2)
    blocks.append((hatch[0], hatch[1] + 1, hatch[2], "create:mechanical_mixer", {}, None))
    blocks.append((hatch[0], hatch[1] + 1, hatch[2] + 1, "create:cogwheel", {"axis": "y"}, None))
    blocks += [(2, 1, 3, "create:mechanical_pump", {"facing": "east"}, None), (1, 1, 3, "create:fluid_tank", {}, fluid("light_rare_earth_liquor", 4000)),
               (6, 1, 3, "create:mechanical_pump", {"facing": "west"}, None), (7, 1, 3, "create:fluid_tank", {}, fluid("hydrochloric_acid", 4000)),
               (3, 1, 1, "create:mechanical_pump", {"facing": "north"}, None), (3, 1, 0, "create:fluid_tank", {}, None),
               (5, 1, 5, "create:mechanical_pump", {"facing": "south"}, None), (5, 1, 6, "create:fluid_tank", {}, None),
               (4, 3, 2, "create:mechanical_pump", {"facing": "down"}, None), (4, 4, 2, "create:fluid_tank", {}, fluid("p507", 4000)),
               (2, 2, 2, "minecraft:lever", {"face": "wall", "facing": "west", "powered": "false"}, None)]
    return blocks


def main():
    out = ASSETS / f"ponder/{SCENE}.nbt"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(structure((9, 5, 7), scene_blocks()))
    lang = read_lang()
    lang[f"fundamentals.ponder.{SCENE}.header"] = HEADER
    for i, text in enumerate(TEXTS, 1):
        lang[f"fundamentals.ponder.{SCENE}.text_{i}"] = text
    write_lang(lang)
    PONDER_TEXT.parent.mkdir(parents=True, exist_ok=True)
    PONDER_TEXT.write_text("package ai.gsmc.fundamentals.client.ponder;\n\n// Written by tools/build_ponder.py; edit the words there.\n"
                    "public final class MixerSettlerPonderText {\n\n    public static final String HEADER = " + json.dumps(HEADER) + ";\n"
                    "    public static final String[] TEXTS = {\n" + "".join(f"            {json.dumps(t)},\n" for t in TEXTS)
                    + "    };\n\n    private MixerSettlerPonderText() {}\n}\n", encoding="utf-8")
    print(f"ponder scene written: {len(TEXTS)} texts")


if __name__ == "__main__":
    main()
