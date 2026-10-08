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

# The vat's proportions in sixteenths of a block. Written to fundamentals_vat.json for VatGeometry.java, so the
# models here, the fluid renderer and the buoyancy agree on where the floor, the walls, the weir and the window are.
VAT = {
    "wall": 1,             # thickness of every wall and the weir
    "floor": 1,            # thickness of the floor under the bottom layer
    "rim_clearance": 5,    # the settled phases stop this far under the rim
    "weir_below_rim": 3,   # the weir's lip, between the trough and the bay, stands this far under the rim
    "window": [4, 12],     # the glass spans these pixels of a window casing's wall, between two posts
}

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
# P507 and P204 are colourless to pale yellow and ride in kerosene, so they are straw; naphthenic acid is the dark one.
ORGANICS = {
    "p204": ("P204", 0xEAD88C),
    "p507": ("P507", 0xECE0A8),
    "naphthenic_acid": ("Naphthenic Acid", 0xA8843C),
}
ACIDS = {
    "hydrochloric_acid": ("Hydrochloric Acid", 0xE4EEF2),
    "nitric_acid": ("Nitric Acid", 0xF0EDC8),
    "phosphoric_acid": ("Phosphoric Acid", 0xE8ECE4),
    "hydrofluoric_acid": ("Hydrofluoric Acid", 0xE6F0EA),
}
# What the plant cannot use: the spent chloride liquor every cut leaves behind, and the brine it becomes
# once lime has neutralised it. The brine boils down to salt, which the clay leach takes back.
WASTES = {
    "spent_liquor": ("Spent Liquor", 0x8E9A86),
    "brine": ("Brine", 0xDCE6E4),
}
GASES = {
    "argon": ("Argon", 0xC8D8F0),
}
FLUIDS = {**{k: (*v, "LIQUOR") for k, v in LIQUORS.items()}, **{k: (*v, "ORGANIC") for k, v in ORGANICS.items()},
          **{k: (*v, "ACID") for k, v in ACIDS.items()}, **{k: (*v, "GAS") for k, v in GASES.items()},
          **{k: (*v, "WASTE") for k, v in WASTES.items()}}

# Oxide to metal. The lights and the heavies go through their fluoride: the lights by molten-salt
# electrolysis on TFMG's electrodes, the heavies by calciothermic reduction under argon, which gives the
# fluorite back as slag. The volatile four are reduced straight from the oxide by lanthanum metal under
# argon and distil off, leaving lanthanum oxide to go round again.
ELECTROLYSIS = ("lanthanum", "cerium", "praseodymium", "neodymium", "didymium")
CALCIOTHERMIC = ("gadolinium", "terbium", "dysprosium", "holmium", "erbium", "lutetium", "yttrium")
LANTHANOTHERMIC = ("samarium", "europium", "thulium", "ytterbium")
# which liquor precipitates each element's oxalate; everything else is its own name
OXALATE_FROM = {"didymium": "praseodymium_neodymium_liquor"}

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
    kinds = sorted({kind for _, _, kind in FLUIDS.values()}, key=["LIQUOR", "ORGANIC", "ACID", "GAS", "WASTE"].index)
    lines[7] = "    public enum Kind { " + ", ".join(kinds) + " }"
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
    # the thorium in the monazite stays behind when the light concentrate dissolves: a residue that has to be put somewhere
    mixing("rare_earth_liquor", [item("light_rare_earth_concentrate"), fluid(STRIP, 500)],
           [result_fluid("rare_earth_liquor", 500), result_item("monazite_residue_dust")], heated=True)
    # Waste: lime neutralises the spent liquor to brine, and brine boils down to salt for the clay leach.
    mixing("brine", item("tfmg:limesand", 2) + [fluid("spent_liquor", 1000)], [result_fluid("brine", 1000)])
    mixing("salt_from_brine", [fluid("brine", 1000)], [result_item("salt", 3)], heated=True)
    mixing("heavy_rare_earth_liquor", [item("heavy_rare_earth_concentrate"), fluid(STRIP, 500)], [result_fluid("heavy_rare_earth_liquor", 500)], heated=True)
    # The clay is not ground or roasted: its rare earths sit on the clay as ions and a salt solution lifts
    # them off, which is why the Chinese heaps are leached in place.
    mixing("heavy_rare_earth_liquor_from_clay", item("raw_ion_adsorption_clay", 4) + [item("salt"), fluid("minecraft:water", 500)],
           [result_fluid("heavy_rare_earth_liquor", 250)])


def vat(name, ingredients, results, machines, heated=True, time=100):
    recipe = {"type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"],
              "ingredients": ingredients, "machines": machines, "min_size": 1, "processing_time": time, "results": results}
    if heated:
        recipe["heat_requirement"] = "heated"
    write(RECIPES / f"reduction/{name}.json", recipe)


def metals():
    # Hydrofluoric acid from fluorspar and sulfuric acid; argon spun out of air as TFMG spins out neon;
    # calcium by electrolysing the chloride that lime and hydrochloric acid make.
    mixing("hydrofluoric_acid", item("raw_fluorite", 2) + [fluid("tfmg:sulfuric_acid", 500)], [result_fluid("hydrofluoric_acid", 500)], heated=True)
    vat("argon", [fluid("tfmg:air", 1000)], [{"id": "fundamentals:argon", "amount": 9}], ["tfmg:centrifuge"], heated=False, time=10)
    vat("calcium_ingot", item("tfmg:limesand", 2) + [fluid(STRIP, 500)], [result_item("calcium_ingot")], ["tfmg:electrode", "tfmg:electrode"])
    for element in ELECTROLYSIS + CALCIOTHERMIC:
        mixing(f"{element}_fluoride", [item(f"{element}_oxide"), fluid("hydrofluoric_acid", 500)], [result_item(f"{element}_fluoride")])
    for element in ELECTROLYSIS:
        vat(f"{element}_ingot", [item(f"{element}_fluoride")] + item(f"{element}_oxide", 2), [result_item(f"{element}_ingot", 2)],
            ["tfmg:electrode", "tfmg:electrode"])
    for element in CALCIOTHERMIC:
        # TFMG vats take four item inputs at most
        vat(f"{element}_ingot", item(f"{element}_fluoride", 2) + item("calcium_ingot", 2) + [fluid("argon", 250)],
            [result_item(f"{element}_ingot", 2), result_item("raw_fluorite", 2)], ["tfmg:mixing"])
    for element in LANTHANOTHERMIC:
        vat(f"{element}_ingot", item(f"{element}_oxide", 2) + item("lanthanum_ingot", 2) + [fluid("argon", 250)],
            [result_item(f"{element}_ingot", 2), result_item("lanthanum_oxide", 2)], ["tfmg:mixing"])
    write(DATA.parent / "c/tags/item/ingots/calcium.json", {"replace": False, "values": ["fundamentals:calcium_ingot"]})


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
        liquor = OXALATE_FROM.get(element, f"{element}_liquor")
        assert liquor in LIQUORS, element
        mixing(f"{element}_oxalate", [item("oxalic_acid"), fluid(liquor, 250)], [result_item(f"{element}_oxalate")])
        write(RECIPES / f"calcining/{element}_oxide.json", {
            "type": "minecraft:smelting", "category": "misc", "ingredient": item(f"{element}_oxalate"),
            "result": {"id": f"fundamentals:{element}_oxide"}, "experience": 0.3, "cookingtime": 200})


def mixer_settler():
    """The casing: a cell of an open-topped welded tank, as a Chinese separation hall is built. A floor under
    the bottom layer, a full panel on every face not shared with the rest of its stage, no lid, a window in
    the middle casing of each outside wall, and a weir on the seam between the mixing trough (the back row)
    and the settling bay, full height on the bottom layer of a two-tall stage and a lip on the one above.
    Every size comes from VAT. Create's connected textures put the frame ribs on the exterior edges; the
    fluids inside are drawn by the renderer."""
    tex = {"side": "fundamentals:block/mixer_settler_side", "top": "fundamentals:block/mixer_settler_top",
           "window": "create:block/fluid_tank_window", "nozzle": "fundamentals:block/mixer_settler_nozzle",
           "particle": "fundamentals:block/mixer_settler_side"}
    full = [0, 0, 16, 16]

    def box(f, t, faces):
        return {"from": list(f), "to": list(t), "faces": {d: {"texture": tx, "uv": uv, **({"cullface": d} if cull else {})}
                                                           for d, (tx, uv, cull) in faces.items()}}

    def model(name, elements):
        write(ASSETS / f"models/block/mixer_settler/{name}.json",
              {"ambientocclusion": False, "render_type": "minecraft:cutout", "textures": tex, "elements": elements})
        return f"fundamentals:block/mixer_settler/{name}"

    W, F = VAT["wall"], VAT["floor"]
    WEIR_TOP = 16 - VAT["weir_below_rim"]
    WIN0, WIN1 = VAT["window"]
    SLAB = {"west": ((0, 0, 0), (W, 16, 16)), "east": ((16 - W, 0, 0), (16, 16, 16)),
            "north": ((0, 0, 0), (16, 16, W)), "south": ((0, 0, 16 - W), (16, 16, 16))}
    INNER = {"west": "east", "east": "west", "north": "south", "south": "north"}

    def panel(side):
        """A plain wall: one panel, our dark sheet outside, the lid colour inside, capped."""
        lo, hi = SLAB[side]
        along_x = side in ("north", "south")
        cap = ("#top", [0, 0, 16, W] if along_x else [0, 0, W, 16], False)
        return [box(lo, hi, {side: ("#side", full, True), INNER[side]: ("#top", full, False), "up": cap})]

    def window_wall(side, part):
        """Create's tank window: two posts and, between them, glass filling the wall's thickness so the rim
        stays whole from above. Create cuts its window strip so the bolts sit only at the window's outer ends;
        `part` is single, bottom or top of a two-tall window."""
        lo, hi = SLAB[side]
        along_x = side in ("north", "south")
        axis = 0 if along_x else 2
        cap = ("#top", [0, 0, 16, W] if along_x else [0, 0, W, 16], False)
        out = []
        # each post also closes its end toward the glass, or the fluid shows through it from an angle
        ends = ("east", "west") if along_x else ("south", "north")
        for (p0, p1, uv), end in zip(((0, WIN0, [0, 0, WIN0, 16]), (WIN1, 16, [WIN1, 0, 16, 16])), ends):
            f, t = list(lo), list(hi)
            f[axis], t[axis] = p0, p1
            out.append(box(f, t, {side: ("#side", uv, True), INNER[side]: ("#top", uv, False), "up": cap, end: ("#top", [0, 0, W, 16], False)}))
        uv = {"single": [0, 0, 8, 16], "bottom": [0, 2, 8, 16], "top": [0, 0, 8, 14]}[part]
        f, t = list(lo), list(hi)
        f[axis], t[axis] = WIN0, WIN1
        faces = {side: ("#window", uv, True), INNER[side]: ("#window", uv, False)}
        if part != "bottom":
            faces["up"] = ("#window", [8, 0, 16, W] if along_x else [8, 0, 8 + W, 8], False)
        out.append(box(f, t, faces))
        return out

    def weir_model(name, y0, y1):
        """The weir on the trough's front seam, from y0 to y1 of the casing, both faces the lid colour."""
        return model(name, [box((0, y0, 0), (16, y1, W), {"north": ("#top", [0, 16 - y1, 16, 16 - y0], False),
                                                          "south": ("#top", [0, 16 - y1, 16, 16 - y0], False),
                                                          "up": ("#top", [0, 0, 16, W], False)})])

    sides = (("west", "left"), ("east", "right"), ("north", "front"), ("south", "back"))
    walls = {prop: model(f"wall_{prop}", panel(side)) for side, prop in sides}
    windows = {(prop, part): model(f"wall_{prop}_{part}", window_wall(side, part)) for side, prop in sides for part in ("single", "bottom", "top")}
    floor = model("floor", [box((0, 0, 0), (16, F, 16), {"down": ("#top", full, True), "up": ("#top", full, False)})])
    weir = weir_model("weir", F, 16)                 # bottom layer of a two-tall stage: runs on into the lip above
    weir_low = weir_model("weir_low", F, WEIR_TOP)   # the only layer of a one-tall stage
    weir_lip = weir_model("weir_lip", 0, WEIR_TOP)   # top layer of a two-tall stage

    # the ports in the shared end walls: the organic overflow high on the front wall, the aqueous drain low on the back
    port_ahead = model("port_ahead", [box((4, 10, 1), (12, 14, 2), {"south": ("#nozzle", [0, 0, 16, 16], False), "up": ("#nozzle", [0, 6, 16, 10], False),
                                                                   "east": ("#nozzle", [6, 4, 10, 12], False), "west": ("#nozzle", [6, 4, 10, 12], False)})])
    port_behind = model("port_behind", [box((4, 2, 14), (12, 6, 15), {"north": ("#nozzle", [0, 0, 16, 16], False), "up": ("#nozzle", [0, 6, 16, 10], False),
                                                                     "east": ("#nozzle", [6, 4, 10, 12], False), "west": ("#nozzle", [6, 4, 10, 12], False)})])
    parts = []
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        rot = {"y": y} if y else {}
        # a wall shared with the next stage carries a port, not a window
        linked = {"front": "link_ahead", "back": "link_behind"}
        for prop, mdl in walls.items():
            plain = {"facing": facing, prop: "false", "window": "false"}
            glazed = {"facing": facing, prop: "false", "window": "true"}
            if prop in linked:
                parts.append({"when": {"facing": facing, prop: "false", "window": "true", linked[prop]: "true"}, "apply": {"model": mdl, **rot}})
                glazed[linked[prop]] = "false"
            parts.append({"when": plain, "apply": {"model": mdl, **rot}})
            parts.append({"when": {**glazed, "above": "false", "below": "false"}, "apply": {"model": windows[(prop, "single")], **rot}})
            parts.append({"when": {**glazed, "above": "true", "below": "false"}, "apply": {"model": windows[(prop, "bottom")], **rot}})
            parts.append({"when": {**glazed, "above": "false", "below": "true"}, "apply": {"model": windows[(prop, "top")], **rot}})
        parts.append({"when": {"facing": facing, "front": "false", "link_ahead": "true", "above": "false"}, "apply": {"model": port_ahead, **rot}})
        parts.append({"when": {"facing": facing, "back": "false", "link_behind": "true", "below": "false"}, "apply": {"model": port_behind, **rot}})
        parts.append({"when": {"facing": facing, "rows": "well", "below": "false", "above": "true"}, "apply": {"model": weir, **rot}})
        parts.append({"when": {"facing": facing, "rows": "well", "below": "false", "above": "false"}, "apply": {"model": weir_low, **rot}})
        parts.append({"when": {"facing": facing, "rows": "well", "below": "true"}, "apply": {"model": weir_lip, **rot}})
    parts.append({"when": {"below": "false"}, "apply": {"model": floor}})
    write(ASSETS / "blockstates/mixer_settler.json", {"multipart": parts})
    write(ASSETS / "models/item/mixer_settler.json", {"ambientocclusion": False, "textures": tex, "elements":
          panel("west") + panel("east") + panel("north") + panel("south")
          + [box((0, 0, 0), (16, F, 16), {"down": ("#top", full, False), "up": ("#top", full, False)}),
             box((0, F, 10), (16, WEIR_TOP, 10 + W), {"north": ("#top", [0, 16 - WEIR_TOP, 16, 16 - F], False), "south": ("#top", [0, 16 - WEIR_TOP, 16, 16 - F], False), "up": ("#top", [0, 10, 16, 10 + W], False)})],
          "display": {"gui": {"rotation": [30, 225, 0], "scale": [0.625, 0.625, 0.625]}, "ground": {"scale": [0.25, 0.25, 0.25]},
                      "fixed": {"scale": [0.5, 0.5, 0.5]}, "thirdperson_righthand": {"rotation": [75, 45, 0], "scale": [0.375, 0.375, 0.375], "translation": [0, 2.5, 0]},
                      "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]}}})
    write(ROOT / "fundamentals_vat.json", VAT)
    write(DATA / "loot_table/blocks/mixer_settler.json", {"type": "minecraft:block", "pools": [drop_self("mixer_settler")]})
    write(RECIPES / "mixer_settler.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P P", "PPP", "PFP"],
        "key": {"P": {"item": "tfmg:plastic_sheet"}, "F": {"item": "create:fluid_pipe"}},
        "result": {"id": "fundamentals:mixer_settler", "count": 6}})
    for name in ("salt", "oxalic_acid", "calcium_ingot"):
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})


def template():
    """A 27x5x5 gametest floor, patched from the 3x3x3 empty one: room for eight stages end to end with mixers over them."""
    empty = gzip.decompress((DATA / "structure/empty.nbt").read_bytes())
    i = empty.index(b"size") + len(b"size") + 1 + 4
    patched = empty[:i] + (27).to_bytes(4, "big") + (5).to_bytes(4, "big") + (5).to_bytes(4, "big") + empty[i + 12:]
    (DATA / "structure/battery.nbt").write_bytes(gzip.compress(patched, mtime=0))


def names():
    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    for id, (name, _, _) in FLUIDS.items():
        lang[f"fluid_type.fundamentals.{id}"] = name
    lang["block.fundamentals.mixer_settler"] = "Mixer-Settler Casing"
    lang["item.fundamentals.salt"] = "Salt"
    lang["item.fundamentals.oxalic_acid"] = "Oxalic Acid"
    lang["item.fundamentals.calcium_ingot"] = "Calcium Ingot"
    lang["goggles.fundamentals.mixer_settler.stages"] = "Battery of %s stages, %s mB a batch"
    lang["goggles.fundamentals.mixer_settler.stage"] = "Stage %s across, %s along, %s tall"
    lang["goggles.fundamentals.mixer_settler.idle"] = "Nothing in the feed"
    lang["goggles.fundamentals.mixer_settler.no_cut"] = "%s does not part"
    lang["goggles.fundamentals.mixer_settler.short"] = "%s parts in %s stages; this battery has %s"
    lang["goggles.fundamentals.mixer_settler.organic"] = "Every stage wants %s on top"
    lang["goggles.fundamentals.mixer_settler.mixer"] = "Every trough wants a Mechanical Mixer turning over it"
    lang["goggles.fundamentals.mixer_settler.lever"] = "Waiting for the lever"
    lang["goggles.fundamentals.mixer_settler.casing"] = "Casing: a stage is three across and three along"
    lang["goggles.fundamentals.mixer_settler.settling"] = "Coming to equilibrium: %s s"
    lang["goggles.fundamentals.mixer_settler.strip"] = "The far end wants %s"
    lang["goggles.fundamentals.mixer_settler.ready"] = "Parting %s into %s and %s"
    lang["goggles.fundamentals.mixer_settler.progress"] = "Organic on %s of %s stages, liquor in %s"
    lang["goggles.fundamentals.mixer_settler.ends"] = "Feed %s, strip %s"
    lang["goggles.fundamentals.mixer_settler.products"] = "Out %s at the head, %s at the tail"
    lang["goggles.fundamentals.mixer_settler.sump"] = "Sump %s"
    lang["goggles.fundamentals.mixer_settler.waste"] = "The sump is full: pump the spent liquor out from below"
    lang["goggles.fundamentals.mixer_settler.nothing"] = "nothing"
    write(path, lang)


def main():
    for folder in ("mixing", "separation", "calcining", "reduction"):
        shutil.rmtree(RECIPES / folder, ignore_errors=True)
    shutil.rmtree(ASSETS / "models/block/mixer_settler", ignore_errors=True)
    for stale in (ASSETS / "models/block").glob("mixer_settler*.json"):
        stale.unlink()
    java_table()
    chemistry()
    cuts()
    oxalates()
    metals()
    mixer_settler()
    template()
    names()
    print(f"{len(FLUIDS)} fluids, {len(CUTS)} cuts")


if __name__ == "__main__":
    main()
