#!/usr/bin/env python3
"""Sheet-metal doors and trapdoors, recoloured from iron's in each metal's own palette, and metal roofing (standing-seam tinplate,
corrugated galvanized iron) with its stairs and slabs: textures, models, blockstates, loot, recipes, tags and names."""
import json
import random
import zipfile
from pathlib import Path

from PIL import Image

from common import ASSETS, DATA, LANG, RESOURCES, TEXTURES, read_lang, tag, write

CLIENT = next(Path.home().glob(".gradle/caches/fabric-loom/1.21.1/neoforge/*/client-extra.jar"))
METALS = ["brass", "bronze", "tin", "silver", "gold", "steel", "stainless_steel", "galvanized_steel", "titanium"]
DISPLAY = {"tin": "Tinplate", "galvanized_steel": "Galvanized Steel", "stainless_steel": "Stainless Steel"}
# The stock a door is cut from: sheet where the pack rolls it, otherwise the ingot, as vanilla's iron door is.
STOCK = {"brass": {"tag": "c:plates/brass"}, "bronze": {"tag": "c:plates/bronze"}, "tin": {"tag": "c:ingots/tin"},
         "silver": {"tag": "c:plates/silver"}, "gold": {"tag": "c:plates/gold"}, "steel": {"tag": "c:plates/steel"},
         "stainless_steel": {"tag": "c:plates/stainless_steel"}, "galvanized_steel": {"item": "fundamentals:galvanized_steel_plate"},
         "titanium": {"tag": "c:plates/titanium"}}
ROOFING = {"tin_roofing": "Tin Roofing", "corrugated_iron": "Corrugated Iron"}


def vanilla(path):
    with zipfile.ZipFile(CLIENT) as jar:
        return jar.read(f"assets/minecraft/{path}")


def palette(metal):
    """The metal's colours, darkest to lightest, from its bars (the fixture metals) or its plate."""
    source = TEXTURES / f"block/{metal}_bars.png"
    if not source.exists():
        source = TEXTURES / f"item/{metal}_plate.png"
    img = Image.open(source).convert("RGBA")
    colours = [p[:3] for p in img.getdata() if p[3] > 200]
    return sorted(colours, key=lambda c: 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2])


def luma(c):
    return 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]


def recolour(png, ramp):
    """Iron's shading carried over, quantile for quantile, onto the metal's ramp."""
    img = Image.open(__import__("io").BytesIO(png)).convert("RGBA")
    lit = sorted({luma(p) for p in img.getdata() if p[3]})
    out = img.copy()
    for xy in ((x, y) for y in range(img.height) for x in range(img.width)):
        p = img.getpixel(xy)
        if p[3]:
            q = lit.index(luma(p)) / max(1, len(lit) - 1)
            out.putpixel(xy, ramp[min(len(ramp) - 1, int(q * len(ramp)))] + (p[3],))
    return out


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c) + (255,)


def tin_roofing():
    """Standing-seam tinplate: flat pans, a raised seam every eight pixels catching the light along one side."""
    base = (196, 200, 204)
    rng = random.Random(7)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            col = x % 8
            k = 1.18 if col == 0 else 0.72 if col == 1 else 1.0 - 0.03 * rng.random() + 0.02 * ((x + y) % 3 == 0)
            img.putpixel((x, y), shade(base, k))
    return img


def corrugated_iron():
    """Corrugated galvanized sheet: a ridge every four pixels, lit on its crest, with the spangle of the zinc coat."""
    base = (168, 176, 178)
    rng = random.Random(11)
    profile = [1.2, 1.05, 0.82, 0.92]
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade(base, profile[x % 4] + (0.06 if rng.random() < 0.12 else 0)))
    return img


def door_files(metal):
    ramp = palette(metal)
    for part in ("top", "bottom"):
        recolour(vanilla(f"textures/block/iron_door_{part}.png"), ramp).save(TEXTURES / f"block/{metal}_door_{part}.png")
    recolour(vanilla("textures/item/iron_door.png"), ramp).save(TEXTURES / f"item/{metal}_door.png")
    recolour(vanilla("textures/block/iron_trapdoor.png"), ramp).save(TEXTURES / f"block/{metal}_trapdoor.png")
    for kind in ("door", "trapdoor"):
        state = vanilla(f"blockstates/iron_{kind}.json").decode().replace(f"minecraft:block/iron_{kind}", f"fundamentals:block/{metal}_{kind}")
        (ASSETS / f"blockstates/{metal}_{kind}.json").write_text(state)
    with zipfile.ZipFile(CLIENT) as jar:
        for name in jar.namelist():
            for kind in ("door", "trapdoor"):
                prefix = f"assets/minecraft/models/block/iron_{kind}_"
                if name.startswith(prefix) and (kind == "trapdoor" or "trapdoor" not in name):
                    model = json.loads(jar.read(name).decode().replace(f"minecraft:block/iron_{kind}", f"fundamentals:block/{metal}_{kind}"))
                    model["render_type"] = "minecraft:cutout"
                    write(ASSETS / f"models/block/{metal}_{kind}_{name[len(prefix):]}", model)
    write(ASSETS / f"models/item/{metal}_door.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{metal}_door"}})
    write(ASSETS / f"models/item/{metal}_trapdoor.json", {"parent": f"fundamentals:block/{metal}_trapdoor_bottom"})


def roofing_files(roof):
    texture = f"fundamentals:block/{roof}"
    write(ASSETS / f"blockstates/{roof}.json", {"variants": {"": {"model": texture}}})
    write(ASSETS / f"models/block/{roof}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": texture}})
    write(ASSETS / f"models/item/{roof}.json", {"parent": texture})
    sides = {"bottom": texture, "side": texture, "top": texture}
    for model in ("stairs", "inner_stairs", "outer_stairs", "slab", "slab_top"):
        name = {"stairs": "stairs", "inner_stairs": "stairs_inner", "outer_stairs": "stairs_outer", "slab": "slab", "slab_top": "slab_top"}[model]
        write(ASSETS / f"models/block/{roof}_{name}.json", {"parent": f"minecraft:block/{model}", "textures": sides})
    for kind in ("stairs", "slab"):
        state = vanilla(f"blockstates/cut_copper_{kind}.json").decode().replace("minecraft:block/cut_copper", f"fundamentals:block/{roof}")
        (ASSETS / f"blockstates/{roof}_{kind}.json").write_text(state)
        write(ASSETS / f"models/item/{roof}_{kind}.json", {"parent": f"fundamentals:block/{roof}_{kind}"})


def loot(block, door=False):
    entry = {"type": "minecraft:item", "name": f"fundamentals:{block}"}
    pool = {"rolls": 1, "bonus_rolls": 0, "entries": [entry], "conditions": [{"condition": "minecraft:survives_explosion"}]}
    if door:
        pool["conditions"].insert(0, {"condition": "minecraft:block_state_property", "block": f"fundamentals:{block}", "properties": {"half": "lower"}})
    if block.endswith("_slab"):
        entry["functions"] = [{"function": "minecraft:set_count", "count": 2, "add": False,
                               "conditions": [{"condition": "minecraft:block_state_property", "block": f"fundamentals:{block}", "properties": {"type": "double"}}]},
                              {"function": "minecraft:explosion_decay"}]
    write(DATA / f"loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [pool]})


def shaped(name, pattern, key, result, count):
    write(DATA / f"recipe/building/{name}.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": pattern, "key": key,
                                                  "result": {"id": f"fundamentals:{result}", "count": count}})


def cut(name, ingredient, result, count):
    write(DATA / f"recipe/building/{name}.json", {"type": "minecraft:stonecutting", "ingredient": {"item": f"fundamentals:{ingredient}"},
                                                  "result": {"id": f"fundamentals:{result}", "count": count}})


BLOCKS = [f"fundamentals:{m}_{k}" for m in METALS for k in ("door", "trapdoor")] + [f"fundamentals:{r}{s}" for r in ROOFING for s in ("", "_stairs", "_slab")]


def main():
    lang = read_lang()
    for metal in METALS:
        door_files(metal)
        loot(f"{metal}_door", door=True)
        loot(f"{metal}_trapdoor")
        name = DISPLAY.get(metal, metal.replace("_", " ").title())
        lang[f"block.fundamentals.{metal}_door"] = f"{name} Door"
        lang[f"block.fundamentals.{metal}_trapdoor"] = f"{name} Trapdoor"
        shaped(f"{metal}_door", ["##", "##", "##"], {"#": STOCK[metal]}, f"{metal}_door", 3)
        shaped(f"{metal}_trapdoor", ["##", "##"], {"#": STOCK[metal]}, f"{metal}_trapdoor", 1)
    tin_roofing().save(TEXTURES / "block/tin_roofing.png")
    corrugated_iron().save(TEXTURES / "block/corrugated_iron.png")
    # tinplate is mild steel sheet dipped in tin; corrugated iron is galvanized sheet run through corrugating rolls
    shaped("tin_roofing", ["TTT", "SSS"], {"T": {"tag": "c:ingots/tin"}, "S": {"tag": "c:plates/steel"}}, "tin_roofing", 6)
    shaped("corrugated_iron", ["###", "###"], {"#": {"item": "fundamentals:galvanized_steel_plate"}}, "corrugated_iron", 6)
    for roof, display in ROOFING.items():
        roofing_files(roof)
        for block in (roof, f"{roof}_stairs", f"{roof}_slab"):
            loot(block)
        lang[f"block.fundamentals.{roof}"] = display
        lang[f"block.fundamentals.{roof}_stairs"] = f"{display} Stairs"
        lang[f"block.fundamentals.{roof}_slab"] = f"{display} Slab"
        item = {"item": f"fundamentals:{roof}"}
        shaped(f"{roof}_stairs", ["#  ", "## ", "###"], {"#": item}, f"{roof}_stairs", 4)
        shaped(f"{roof}_slab", ["###"], {"#": item}, f"{roof}_slab", 6)
        cut(f"{roof}_stairs_from_cutting", roof, f"{roof}_stairs", 1)
        cut(f"{roof}_slab_from_cutting", roof, f"{roof}_slab", 2)
    write(LANG, lang)  # keeps the file's order, which other tools left unsorted
    mc = RESOURCES / "data/minecraft/tags"
    for kind in ("block", "item"):
        tag(mc / f"{kind}/doors.json", [f"fundamentals:{m}_door" for m in METALS])
        tag(mc / f"{kind}/trapdoors.json", [f"fundamentals:{m}_trapdoor" for m in METALS])
        tag(mc / f"{kind}/stairs.json", [f"fundamentals:{r}_stairs" for r in ROOFING])
        tag(mc / f"{kind}/slabs.json", [f"fundamentals:{r}_slab" for r in ROOFING])
    pickaxe = mc / "block/mineable/pickaxe.json"
    values = json.loads(pickaxe.read_text())["values"]
    write(pickaxe, {"replace": False, "values": values + [b for b in BLOCKS if b not in values]})


if __name__ == "__main__":
    main()
