#!/usr/bin/env python3
"""The millwright's oak gearing: its textures, painted over Create's own layouts, and its models, blockstates, loot, recipes and names.

The shaft is a squared oak axle, its ends showing the growth rings. The cogwheels are compass-arm wheels: an oak rim and hub
with teeth of pale hornbeam pegged into it, the hard wood millwrights kept for teeth.
"""
import io
import random
import zipfile

from PIL import Image

from common import ASSETS, DATA, read_lang, write, write_lang
from paint_separation import CREATE_JAR

TEXTURES = ASSETS / "textures/block"
OAK = [(70, 50, 28), (98, 72, 42), (128, 96, 58), (156, 120, 74), (182, 144, 92)]
HORNBEAM = [(132, 118, 92), (168, 154, 122), (198, 186, 152), (224, 214, 182)]
PARTS = ("wooden_shaft", "wooden_cogwheel", "wooden_large_cogwheel")
# where each cogwheel's teeth lie on its 32 px sheet, read off Create's models (their UVs, doubled)
TEETH = {"cogwheel": [(10, 16, 13, 19), (14, 12, 32, 19)], "large_cogwheel": [(0, 23, 30, 29), (20, 19, 23, 22)]}


def create_texture(name):
    with zipfile.ZipFile(CREATE_JAR) as jar:
        return Image.open(io.BytesIO(jar.read(f"assets/create/textures/block/{name}.png"))).convert("RGBA")


def luma(rgb):
    return 0.3 * rgb[0] + 0.59 * rgb[1] + 0.11 * rgb[2]


def in_any(x, y, boxes):
    return any(x0 <= x < x1 and y0 <= y < y1 for x0, y0, x1, y1 in boxes)


def oak_over(name, grain_seed, teeth=()):
    """Create's texture in oak, its own shading kept: each pixel's brightness, within the sheet's own range, picks the shade.
    Long streaks of grain run down the oak; the tooth regions are hornbeam, close-grained and pale."""
    src = create_texture(name)
    rng = random.Random(grain_seed)
    img = Image.new("RGBA", src.size, (0, 0, 0, 0))
    pixels = [(x, y) for y in range(src.height) for x in range(src.width) if src.getpixel((x, y))[3]]
    def scale(points):
        values = [luma(src.getpixel(p)) for p in points] or [0, 1]
        return min(values), max(max(values), min(values) + 1)
    oak_lo, oak_hi = scale([p for p in pixels if not in_any(*p, teeth)])
    tooth_lo, tooth_hi = scale([p for p in pixels if in_any(*p, teeth)])
    streak = {x: rng.choice((-1, 0, 0, 0, 1)) for x in range(src.width)}
    for x, y in pixels:
        rgb, a = src.getpixel((x, y))[:3], src.getpixel((x, y))[3]
        if in_any(x, y, teeth):
            shade = round((luma(rgb) - tooth_lo) / (tooth_hi - tooth_lo) * (len(HORNBEAM) - 1))
            img.putpixel((x, y), HORNBEAM[shade] + (a,))
        else:
            shade = round((luma(rgb) - oak_lo) / (oak_hi - oak_lo) * (len(OAK) - 1)) + streak[x]
            img.putpixel((x, y), OAK[max(0, min(len(OAK) - 1, shade))] + (a,))
    return img


def end_grain():
    """The shaft's end: the pith dark at the middle of a 4 px square, a ring of late wood round it."""
    img = create_texture("axis_top")
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    for y in range(img.height):
        for x in range(img.width):
            if img.getpixel((x, y))[3]:
                ring = max(abs(x - 7.5), abs(y - 7.5))
                out.putpixel((x, y), OAK[1] + (255,) if ring < 1 else OAK[3] + (255,) if ring < 1.6 else OAK[2] + (255,))
    return out


def paint():
    oak_over("axis", "wooden_axis").save(TEXTURES / "wooden_axis.png")
    oak_over("cogwheel_axis", "wooden_cogwheel_axis").save(TEXTURES / "wooden_cogwheel_axis.png")
    end_grain().save(TEXTURES / "wooden_axis_top.png")
    for cog in ("cogwheel", "large_cogwheel"):
        oak_over(cog, f"wooden_{cog}", TEETH[cog]).save(TEXTURES / f"wooden_{cog}.png")


def models():
    write(ASSETS / "models/block/wooden_shaft.json", {"parent": "create:block/shaft", "textures": {
        "particle": "fundamentals:block/wooden_axis", "0": "fundamentals:block/wooden_axis", "1": "fundamentals:block/wooden_axis_top"}})
    write(ASSETS / "models/block/wooden_cogwheel.json", {"parent": "create:block/cogwheel", "textures": {
        "particle": "minecraft:block/stripped_oak_log", "0": "fundamentals:block/wooden_cogwheel_axis", "3": "fundamentals:block/wooden_axis_top",
        "1_2": "fundamentals:block/wooden_cogwheel"}})
    write(ASSETS / "models/block/wooden_large_cogwheel.json", {"parent": "create:block/large_cogwheel", "textures": {
        "particle": "minecraft:block/stripped_oak_log", "0": "fundamentals:block/wooden_cogwheel_axis", "3": "fundamentals:block/wooden_axis_top",
        "4": "fundamentals:block/wooden_large_cogwheel"}})
    for part in PARTS:
        write(ASSETS / f"blockstates/{part}.json", {"variants": {
            "axis=x": {"model": f"fundamentals:block/{part}", "x": 90, "y": 90}, "axis=y": {"model": f"fundamentals:block/{part}"},
            "axis=z": {"model": f"fundamentals:block/{part}", "x": 90, "y": 180}}})
        write(ASSETS / f"models/item/{part}.json", {"parent": f"fundamentals:block/{part}"})
        write(DATA / f"loot_table/blocks/{part}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"fundamentals:{part}"}],
             "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(DATA.parent / "minecraft/tags/block/mineable/axe.json", {"replace": False, "values": [f"fundamentals:{part}" for part in PARTS]})


def recipes():
    recipe = DATA / "recipe/mill"
    # a mill shaft is hewn from a whole trunk, squared with the adze
    write(recipe / "wooden_shaft.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["L", "L"],
                                         "key": {"L": {"tag": "c:stripped_logs"}}, "result": {"id": "fundamentals:wooden_shaft", "count": 6}})
    write(recipe / "wooden_cogwheel.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                            "ingredients": [{"item": "fundamentals:wooden_shaft"}, {"tag": "minecraft:planks"}],
                                            "result": {"id": "fundamentals:wooden_cogwheel"}})
    write(recipe / "wooden_large_cogwheel.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                  "ingredients": [{"item": "fundamentals:wooden_shaft"}, {"tag": "minecraft:planks"}, {"tag": "minecraft:planks"}],
                                                  "result": {"id": "fundamentals:wooden_large_cogwheel"}})
    # water wheels and windmills turned on oak shafts long before iron ones
    write(recipe / "water_wheel_on_wooden_shaft.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SSS", "SCS", "SSS"],
                                                        "key": {"C": {"item": "fundamentals:wooden_shaft"}, "S": {"tag": "minecraft:planks"}},
                                                        "result": {"id": "create:water_wheel"}})
    write(recipe / "windmill_bearing_on_wooden_shaft.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["B", "C", "I"],
                                                             "key": {"B": {"tag": "minecraft:wooden_slabs"}, "C": {"tag": "c:stones"},
                                                                     "I": {"item": "fundamentals:wooden_shaft"}},
                                                             "result": {"id": "create:windmill_bearing"}})


def names():
    lang = read_lang()
    lang["block.fundamentals.wooden_shaft"] = "Oak Shaft"
    lang["block.fundamentals.wooden_cogwheel"] = "Oak Cogwheel"
    lang["block.fundamentals.wooden_large_cogwheel"] = "Large Oak Cogwheel"
    lang["tooltip.fundamentals.wooden_mill"] = "Oak gives way: snaps above %s RPM or under more than %s su of load"
    write_lang(lang)


if __name__ == "__main__":
    paint()
    models()
    recipes()
    names()
    print("mill written")
