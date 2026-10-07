#!/usr/bin/env python3
"""The rare earth separation line: the reagent fluids, the solvent-extraction cuts, and the chemistry
around them. Writes the Java reagent table, the cut recipes, the Create mixing recipes that make the
reagents and liquors, the oxalate route out, the mixer-settler's blockstate, model, loot and tags, the
names, and the gametest template. Re-run after any edit; build_ore_data.py last for the tool tags.

    python3 tools/paint_separation.py && python3 tools/build_separation_data.py && python3 tools/build_ore_data.py
"""
import gzip
import json
import shutil
from pathlib import Path

from build_ore_data import ASSETS, DATA, ROOT, drop_self, write
from paint_materials import MATERIALS

JAVA = Path(__file__).resolve().parent.parent / "src/main/java/ai/gsmc/fundamentals/separation/Reagents.java"
RECIPES = DATA / "recipe"

# id: (name, tint, kind). Tints are what the chloride solutions look like: Nd lilac, Pr green, Er pink,
# Sm and Dy straw, Ho and Tm faintly yellow and green, the rest colourless, so a mixed liquor takes the
# colour of whatever in it is coloured.
CLEAR = 0xDCE6EC
LIQUORS = {
    "rare_earth_liquor": ("Rare Earth Liquor", 0xB9A8C8),
    "light_rare_earth_liquor": ("Light Rare Earth Liquor", 0xB4A6CF),
    "heavy_rare_earth_liquor": ("Heavy Rare Earth Liquor", 0xE2D6B0),
    "lanthanum_cerium_liquor": ("Lanthanum-Cerium Liquor", CLEAR),
    "praseodymium_neodymium_liquor": ("Didymium Liquor", 0x9A8CC4),
    "samarium_europium_gadolinium_liquor": ("Samarium-Europium-Gadolinium Liquor", 0xE8DFB4),
    "europium_gadolinium_liquor": ("Europium-Gadolinium Liquor", 0xE4DEDA),
    "terbium_to_lutetium_liquor": ("Terbium-to-Lutetium Liquor", 0xE6D4C0),
    "terbium_dysprosium_liquor": ("Terbium-Dysprosium Liquor", 0xE6DFC2),
    "yttrium_heavies_liquor": ("Yttrium and Late Heavies Liquor", 0xE4CFC6),
    "holmium_to_lutetium_liquor": ("Holmium-to-Lutetium Liquor", 0xE5C8C0),
    "holmium_erbium_liquor": ("Holmium-Erbium Liquor", 0xE6C2C0),
    "thulium_ytterbium_lutetium_liquor": ("Thulium-Ytterbium-Lutetium Liquor", 0xDCE4D4),
    "ytterbium_lutetium_liquor": ("Ytterbium-Lutetium Liquor", CLEAR),
    "lanthanum_liquor": ("Lanthanum Liquor", CLEAR),
    "cerium_liquor": ("Cerium Liquor", CLEAR),
    "praseodymium_liquor": ("Praseodymium Liquor", 0xA8D6A0),
    "neodymium_liquor": ("Neodymium Liquor", 0x9C86CC),
    "samarium_liquor": ("Samarium Liquor", 0xEFE6A8),
    "europium_liquor": ("Europium Liquor", 0xE8D8D8),
    "gadolinium_liquor": ("Gadolinium Liquor", CLEAR),
    "terbium_liquor": ("Terbium Liquor", CLEAR),
    "dysprosium_liquor": ("Dysprosium Liquor", 0xEEE8B8),
    "holmium_liquor": ("Holmium Liquor", 0xECE4B0),
    "erbium_liquor": ("Erbium Liquor", 0xECB4BC),
    "thulium_liquor": ("Thulium Liquor", 0xD4E8C8),
    "ytterbium_liquor": ("Ytterbium Liquor", CLEAR),
    "lutetium_liquor": ("Lutetium Liquor", CLEAR),
    "yttrium_liquor": ("Yttrium Liquor", CLEAR),
}
ORGANICS = {
    "p204": ("P204", 0xD8B060),
    "p507": ("P507", 0xC89440),
    "naphthenic_acid": ("Naphthenic Acid", 0x8A6A2E),
}
ACIDS = {
    "hydrochloric_acid": ("Hydrochloric Acid", 0xE4EEF2),
    "nitric_acid": ("Nitric Acid", 0xF0EDC8),
    "phosphoric_acid": ("Phosphoric Acid", 0xE8ECE4),
}
FLUIDS = {**{k: (*v, "LIQUOR") for k, v in LIQUORS.items()}, **{k: (*v, "ORGANIC") for k, v in ORGANICS.items()},
          **{k: (*v, "ACID") for k, v in ACIDS.items()}}

# One cut per mixer-settler battery: (liquor in, organic, stages, light out, heavy out). The stage count
# follows the separation factor of the pair being parted: Sm/Nd is about 10 and parts in a few stages,
# Nd/Pr is about 1.4 and takes a battery thirty-two long. Everything strips with hydrochloric acid.
CUTS = [
    ("rare_earth_liquor", "p507", 8, "light_rare_earth_liquor", "heavy_rare_earth_liquor"),
    ("light_rare_earth_liquor", "p507", 14, "lanthanum_cerium_liquor", "praseodymium_neodymium_liquor"),
    ("lanthanum_cerium_liquor", "p507", 12, "lanthanum_liquor", "cerium_liquor"),
    ("praseodymium_neodymium_liquor", "p507", 32, "praseodymium_liquor", "neodymium_liquor"),
    ("heavy_rare_earth_liquor", "p204", 10, "samarium_europium_gadolinium_liquor", "terbium_to_lutetium_liquor"),
    ("samarium_europium_gadolinium_liquor", "p507", 16, "samarium_liquor", "europium_gadolinium_liquor"),
    ("europium_gadolinium_liquor", "p507", 18, "europium_liquor", "gadolinium_liquor"),
    ("terbium_to_lutetium_liquor", "p204", 12, "terbium_dysprosium_liquor", "yttrium_heavies_liquor"),
    ("terbium_dysprosium_liquor", "p507", 20, "terbium_liquor", "dysprosium_liquor"),
    ("yttrium_heavies_liquor", "naphthenic_acid", 14, "yttrium_liquor", "holmium_to_lutetium_liquor"),
    ("holmium_to_lutetium_liquor", "p204", 10, "holmium_erbium_liquor", "thulium_ytterbium_lutetium_liquor"),
    ("holmium_erbium_liquor", "p507", 20, "holmium_liquor", "erbium_liquor"),
    ("thulium_ytterbium_lutetium_liquor", "p204", 12, "thulium_liquor", "ytterbium_lutetium_liquor"),
    ("ytterbium_lutetium_liquor", "p507", 20, "ytterbium_liquor", "lutetium_liquor"),
]
STRIP = "hydrochloric_acid"


def fluid(id, amount):
    return {"type": "neoforge:single", "amount": amount, "fluid": id if ":" in id else f"fundamentals:{id}"}


def item(id, count=1):
    out = {"item": id if ":" in id else f"fundamentals:{id}"}
    return out if count == 1 else [out] * count


def mixing(name, ingredients, results, heated=False):
    recipe = {"type": "create:mixing", "ingredients": ingredients, "results": results}
    if heated:
        recipe["heat_requirement"] = "heated"
    write(RECIPES / f"mixing/{name}.json", recipe)


def result_fluid(id, amount):
    return {"id": f"fundamentals:{id}", "amount": amount}


def result_item(id, count=1):
    out = {"id": f"fundamentals:{id}"}
    return out if count == 1 else {**out, "count": count}


def java_table():
    lines = ["package ai.gsmc.fundamentals.separation;", "", "import java.util.List;", "",
             "// Written by tools/build_separation_data.py; edit the table there.",
             "public final class Reagents {", "",
             "    public enum Kind { LIQUOR, ORGANIC, ACID }", "",
             "    public record Reagent(String id, int tint, Kind kind) {}", "",
             "    public static final List<Reagent> ALL = List.of("]
    entries = [f'            new Reagent("{id}", 0x{tint:06X}, Kind.{kind})' for id, (_, tint, kind) in FLUIDS.items()]
    lines += [",\n".join(entries) + ");", "", "    private Reagents() {}", "}", ""]
    JAVA.write_text("\n".join(lines), encoding="utf-8")


def chemistry():
    # Salt by boiling off water; the Mannheim process for the acid.
    mixing("salt", [fluid("minecraft:water", 1000)], [result_item("salt", 2)], heated=True)
    mixing("hydrochloric_acid", item("salt", 2) + [fluid("tfmg:sulfuric_acid", 500)], [result_fluid("hydrochloric_acid", 500)], heated=True)
    mixing("nitric_acid", item("tfmg:nitrate_dust", 2) + [fluid("tfmg:sulfuric_acid", 500)], [result_fluid("nitric_acid", 500)])
    # Carbohydrate oxidised by nitric acid: the classical oxalic acid route.
    mixing("oxalic_acid", item("minecraft:sugar", 2) + [fluid("nitric_acid", 250)], [result_item("oxalic_acid", 2)], heated=True)
    # Wet-process phosphoric acid from a phosphate rock, which bone meal stands in for.
    mixing("phosphoric_acid", item("minecraft:bone_meal", 2) + [fluid("tfmg:sulfuric_acid", 500)], [result_fluid("phosphoric_acid", 500)])
    # The organophosphorus extractants: P204 and P507 are the same family of 2-ethylhexyl esters, so one
    # recipe cold and one hot stands for the two syntheses. Naphthenic acid is petroleum's own.
    mixing("p204", [fluid("phosphoric_acid", 250), fluid("tfmg:kerosene", 750)], [result_fluid("p204", 1000)])
    mixing("p507", [fluid("phosphoric_acid", 250), fluid("tfmg:kerosene", 750)], [result_fluid("p507", 1000)], heated=True)
    mixing("naphthenic_acid", [fluid("tfmg:heavy_oil", 1000), fluid("tfmg:sulfuric_acid", 250)], [result_fluid("naphthenic_acid", 500)], heated=True)
    # Leaching the concentrates into chloride liquor.
    mixing("rare_earth_liquor", [item("light_rare_earth_concentrate"), fluid(STRIP, 500)], [result_fluid("rare_earth_liquor", 500)], heated=True)
    mixing("heavy_rare_earth_liquor", [item("heavy_rare_earth_concentrate"), fluid(STRIP, 500)], [result_fluid("heavy_rare_earth_liquor", 500)], heated=True)


def cuts():
    for liquor, organic, stages, light, heavy in CUTS:
        write(RECIPES / f"separation/{liquor}.json", {
            "type": "fundamentals:separation", "liquor": f"fundamentals:{liquor}", "organic": f"fundamentals:{organic}",
            "strip": f"fundamentals:{STRIP}", "stages": stages,
            "light": f"fundamentals:{light}", "heavy": f"fundamentals:{heavy}"})


def oxalates():
    """A single-element liquor precipitates with oxalic acid, and the oxalate calcines to the oxide."""
    for element, forms in MATERIALS.items():
        if "oxalate" not in forms:
            continue
        assert f"{element}_liquor" in LIQUORS, element
        mixing(f"{element}_oxalate", [item("oxalic_acid"), fluid(f"{element}_liquor", 250)], [result_item(f"{element}_oxalate")])
        write(RECIPES / f"calcining/{element}_oxide.json", {
            "type": "minecraft:smelting", "category": "misc", "ingredient": item(f"{element}_oxalate"),
            "result": {"id": f"fundamentals:{element}_oxide"}, "experience": 0.3, "cookingtime": 200})


def mixer_settler():
    """The casing. Casings facing the same way merge into one stage as they are placed, any box up to three
    across, three along and two tall, so the model is a multipart: a wall wherever a face is not shared with
    the same stage (with a rim on the top layer and a window strip on the bay's upper walls), a floor under
    the bottom layer, the weir between the mixing trough (the back row) and the settling bay, and the mixer
    drive on the trough's top centre casing. A one-row stage carries its well at the back of the row. Create's
    connected textures tie the exterior walls into one tank; the fluids inside are drawn by the renderer."""
    tex = {"side": "fundamentals:block/mixer_settler_side", "inside": "fundamentals:block/mixer_settler_inside",
           "rim": "fundamentals:block/mixer_settler_rim", "window": "fundamentals:block/mixer_settler_window",
           "motor": "fundamentals:block/mixer_settler_motor", "particle": "fundamentals:block/mixer_settler_side"}
    full = [0, 0, 16, 16]

    def box(f, t, faces):
        return {"from": list(f), "to": list(t), "faces": {d: {"texture": tx, "uv": uv, **({"cullface": d} if cull else {})}
                                                           for d, (tx, uv, cull) in faces.items()}}

    def model(name, elements):
        write(ASSETS / f"models/block/mixer_settler/{name}.json",
              {"ambientocclusion": False, "render_type": "minecraft:cutout", "textures": tex, "elements": elements})
        return f"fundamentals:block/mixer_settler/{name}"

    # Walls are drawn for facing=north: left is west, back is south. Each comes plain, capped (top layer), and
    # windowed-and-capped (the bay's top layer). Walls run the full block; neighbouring walls overlap at the
    # corner by a pixel, which the cull faces hide.
    def wall(side, cap, window):
        outer = side
        inner = {"west": "east", "east": "west", "north": "south", "south": "north"}[side]
        if side == "west":
            lo, hi = (0, 0, 0), (1, 16, 16)
        elif side == "east":
            lo, hi = (15, 0, 0), (16, 16, 16)
        elif side == "north":
            lo, hi = (0, 0, 0), (16, 16, 1)
        else:
            lo, hi = (0, 0, 15), (16, 16, 16)
        capf = {"up": ("#rim", [0, 0, 16, 1], False)} if cap else {}
        if not window:
            return [box(lo, hi, {outer: ("#side", full, True), inner: ("#inside", full, False), **capf})]
        out = []
        along_x = side in ("north", "south")
        for p0, p1, uv in ((0, 4, [0, 0, 4, 16]), (12, 16, [12, 0, 16, 16])):
            f, t = list(lo), list(hi)
            f[0 if along_x else 2], t[0 if along_x else 2] = p0, p1
            out.append(box(f, t, {outer: ("#side", uv, True), inner: ("#inside", uv, False), **capf}))
        f, t = list(lo), list(hi)
        f[0 if along_x else 2], t[0 if along_x else 2] = 4, 12
        out.append(box(f, t, {outer: ("#window", [4, 0, 12, 16], True), inner: ("#window", [4, 0, 12, 16], False),
                              **({"up": ("#rim", [4, 0, 12, 1], False)} if cap else {})}))
        return out

    pieces = {}
    for side, prop in (("west", "left"), ("east", "right"), ("north", "front"), ("south", "back")):
        pieces[(prop, "plain")] = model(f"wall_{prop}", wall(side, False, False))
        pieces[(prop, "cap")] = model(f"wall_{prop}_top", wall(side, True, False))
        pieces[(prop, "window")] = model(f"wall_{prop}_window", wall(side, True, True))
    floor = model("floor", [box((0, 0, 0), (16, 1, 16), {"down": ("#side", full, True), "up": ("#inside", full, False)})])
    # the weir: a trough cell's whole forward face on the bottom layer, a lip on the layer above
    weir = model("weir", [box((0, 1, 0), (16, 16, 1), {"north": ("#inside", full, False), "south": ("#inside", full, False)})])
    weir_lip = model("weir_lip", [box((0, 0, 0), (16, 2, 1), {"north": ("#inside", [0, 14, 16, 16], False), "south": ("#inside", [0, 14, 16, 16], False),
                                                           "up": ("#rim", [0, 0, 16, 1], False)})])
    # a one-row stage keeps a small well at the back of the row, behind a low weir
    well = model("well", [box((0, 1, 10), (16, 12, 11), {"north": ("#inside", [0, 4, 16, 15], False), "south": ("#inside", [0, 4, 16, 15], False),
                                                      "up": ("#rim", [0, 10, 16, 11], False)})])
    motor = model("motor", [
        box((0, 14, 6), (16, 16, 10), {d: ("#motor", [0, 8, 16, 10], False) for d in ("north", "south", "up")}),
        box((5, 16, 5), (11, 26, 11), {**{d: ("#motor", [5, 0, 11, 10], False) for d in ("north", "south", "east", "west")},
                                        "up": ("#motor", [5, 0, 11, 6], False)}),
    ])
    motor_single = model("motor_single", [
        box((0, 12, 11), (16, 14, 15), {d: ("#motor", [0, 8, 16, 10], False) for d in ("north", "south", "up")}),
        box((6, 14, 11), (10, 22, 15), {**{d: ("#motor", [6, 0, 10, 8], False) for d in ("north", "south", "east", "west")},
                                         "up": ("#motor", [6, 0, 10, 4], False)}),
    ])

    parts = []
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        def case(when, mdl):
            parts.append({"when": {"facing": facing, **when}, "apply": {"model": mdl, **({"y": y} if y else {})}})
        case({"below": "false"}, floor)
        for prop in ("left", "right", "front", "back"):
            case({prop: "false", "above": "true"}, pieces[(prop, "plain")])
            case({prop: "false", "above": "false", "rows": "well"}, pieces[(prop, "cap")])
            case({prop: "false", "above": "false", "rows": "bay|single"}, pieces[(prop, "window")])
        case({"rows": "well", "below": "false"}, weir)
        case({"rows": "well", "below": "true"}, weir_lip)
        case({"rows": "single", "below": "false"}, well)
        case({"motor": "true", "rows": "well"}, motor)
        case({"motor": "true", "rows": "single"}, motor_single)
    write(ASSETS / "blockstates/mixer_settler.json", {"multipart": parts})
    # the item shows a lone casing: every wall, the floor, the small well and its motor
    write(ASSETS / "models/item/mixer_settler.json", {"ambientocclusion": False, "textures": tex, "elements":
          [box((0, 0, 0), (16, 1, 16), {"down": ("#side", full, True), "up": ("#inside", full, False)})]
          + wall("west", True, True) + wall("east", True, True) + wall("north", True, True) + wall("south", True, False)
          + [box((0, 1, 10), (16, 12, 11), {"north": ("#inside", [0, 4, 16, 15], False), "south": ("#inside", [0, 4, 16, 15], False), "up": ("#rim", [0, 10, 16, 11], False)}),
             box((0, 12, 11), (16, 14, 15), {d: ("#motor", [0, 8, 16, 10], False) for d in ("north", "south", "up")}),
             box((6, 14, 11), (10, 22, 15), {**{d: ("#motor", [6, 0, 10, 8], False) for d in ("north", "south", "east", "west")}, "up": ("#motor", [6, 0, 10, 4], False)})],
          "display": {"gui": {"rotation": [30, 225, 0], "scale": [0.55, 0.55, 0.55], "translation": [0, -1, 0]},
                      "ground": {"scale": [0.25, 0.25, 0.25]}, "fixed": {"scale": [0.5, 0.5, 0.5]},
                      "thirdperson_righthand": {"rotation": [75, 45, 0], "scale": [0.375, 0.375, 0.375], "translation": [0, 2.5, 0]},
                      "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]}}})
    write(DATA / "loot_table/blocks/mixer_settler.json", {"type": "minecraft:block", "pools": [drop_self("mixer_settler")]})
    write(RECIPES / "mixer_settler.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["C C", "SSS", "SPS"],
        "key": {"C": {"tag": "c:ingots/copper"}, "S": {"tag": "c:plates/steel"}, "P": {"item": "create:fluid_pipe"}},
        "result": {"id": "fundamentals:mixer_settler", "count": 6}})
    for name in ("salt", "oxalic_acid"):
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})


def template():
    """A 27x3x5 gametest floor, patched from the 3x3x3 empty one: room for eight stages end to end."""
    empty = gzip.decompress((DATA / "structure/empty.nbt").read_bytes())
    i = empty.index(b"size") + len(b"size") + 1 + 4
    patched = empty[:i] + (27).to_bytes(4, "big") + (3).to_bytes(4, "big") + (5).to_bytes(4, "big") + empty[i + 12:]
    (DATA / "structure/battery.nbt").write_bytes(gzip.compress(patched, mtime=0))


def names():
    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    for id, (name, _, _) in FLUIDS.items():
        lang[f"fluid_type.fundamentals.{id}"] = name
    lang["block.fundamentals.mixer_settler"] = "Mixer-Settler Casing"
    lang["item.fundamentals.salt"] = "Salt"
    lang["item.fundamentals.oxalic_acid"] = "Oxalic Acid"
    lang["goggles.fundamentals.mixer_settler.stages"] = "Battery of %s stages, %s mB a batch"
    lang["goggles.fundamentals.mixer_settler.stage"] = "Stage %s across, %s along, %s tall"
    lang["goggles.fundamentals.mixer_settler.idle"] = "Nothing in the feed"
    lang["goggles.fundamentals.mixer_settler.no_cut"] = "%s does not part"
    lang["goggles.fundamentals.mixer_settler.short"] = "%s parts in %s stages; this battery has %s"
    lang["goggles.fundamentals.mixer_settler.organic"] = "Every stage wants %s on top"
    lang["goggles.fundamentals.mixer_settler.strip"] = "The far end wants %s"
    lang["goggles.fundamentals.mixer_settler.ready"] = "Parting %s into %s and %s"
    write(path, lang)


def main():
    for folder in ("mixing", "separation", "calcining"):
        shutil.rmtree(RECIPES / folder, ignore_errors=True)
    shutil.rmtree(ASSETS / "models/block/mixer_settler", ignore_errors=True)
    for stale in (ASSETS / "models/block").glob("mixer_settler*.json"):
        stale.unlink()
    java_table()
    chemistry()
    cuts()
    oxalates()
    mixer_settler()
    template()
    names()
    print(f"{len(FLUIDS)} fluids, {len(CUTS)} cuts")


if __name__ == "__main__":
    main()
