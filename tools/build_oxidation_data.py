#!/usr/bin/env python3
"""Metal in air: how fast each metal item ages (the fundamentals:oxidation item data map), the weathering storage blocks'
stages for NeoForge's oxidizables and waxables data maps, their models, loot and names, the inert storage drum, the
argon canister, and the sandpaper polishing that takes the stages back off. Re-run after any edit; build_ore_data.py last
for the tool tags.

    python3 tools/paint_oxidation.py && python3 tools/build_oxidation_data.py && python3 tools/build_ore_data.py

The rates are Minecraft days per stage in ordinary air, rounded from the real behaviour: lanthanum and cerium go from a fresh
ingot to oxide in days of damp air, neodymium and praseodymium in weeks, samarium, gadolinium and the heavies slowly; iron
rusts only with water about; copper and bronze take a patina that stops at the surface; silver blackens with the sulfur in
air; aluminium, titanium, chromium, stainless steel, nickel, tin, zinc and lead put on a skin of oxide and stop, and gold and
the platinum metals never change at all, so none of them has an entry.
"""
import json
import shutil

from build_ore_data import ASSETS, DATA, ROOT, cube, drop_self, write
from paint_oxidation import FAMILIES, STAGED, aged_name, family_blocks

RECIPES = DATA / "recipe/oxidation"
DAY_FACTOR = {"ingot": 1, "plate": 1, "nugget": 1.5, "block": 0.25}

# metal: (kind, days per stage as an ingot, dry factor, wet factor, the oxide a flaking metal ends as)
METALS = {
    "copper": ("patina", 9, 0.25, 3, None),
    "brass": ("patina", 24, 0.25, 3, None),
    "bronze": ("patina", 18, 0.25, 3, None),
    "silver": ("tarnish", 24, 0.5, 1.5, None),
    "iron": ("rust", 45, 0, 6, None),
    "steel": ("rust", 75, 0, 6, None),
    "lanthanum": ("flaking", 3, 0.25, 4, "fundamentals:lanthanum_oxide"),
    "cerium": ("flaking", 3, 0.25, 4, "fundamentals:cerium_oxide"),
    "praseodymium": ("flaking", 6, 0.25, 4, "fundamentals:praseodymium_oxide"),
    "didymium": ("flaking", 7.5, 0.25, 4, "fundamentals:didymium_oxide"),
    "neodymium": ("flaking", 9, 0.25, 4, "fundamentals:neodymium_oxide"),
    "samarium": ("flaking", 24, 0.25, 4, "fundamentals:samarium_oxide"),
    "gadolinium": ("flaking", 24, 0.25, 4, "fundamentals:gadolinium_oxide"),
    "terbium": ("flaking", 45, 0.25, 4, "fundamentals:terbium_oxide"),
    "dysprosium": ("flaking", 45, 0.25, 4, "fundamentals:dysprosium_oxide"),
    "yttrium": ("flaking", 60, 0.25, 4, "fundamentals:yttrium_oxide"),
    # the alkali and alkaline earth metals crust over white (calcium, magnesium) or black (lithium's nitride); nothing here holds the crust
    "calcium": ("tarnish", 1.5, 0.25, 4, None),
    "lithium": ("tarnish", 3, 0.25, 4, None),
    "magnesium": ("tarnish", 30, 0.25, 3, None),
}

# item: (metal, form)
ITEMS = {
    "minecraft:copper_ingot": ("copper", "ingot"), "create:copper_nugget": ("copper", "nugget"), "create:copper_sheet": ("copper", "plate"),
    "create:brass_ingot": ("brass", "ingot"), "create:brass_nugget": ("brass", "nugget"), "create:brass_sheet": ("brass", "plate"),
    "fundamentals:bronze_ingot": ("bronze", "ingot"), "fundamentals:bronze_nugget": ("bronze", "nugget"), "fundamentals:bronze_plate": ("bronze", "plate"),
    "fundamentals:silver_ingot": ("silver", "ingot"), "fundamentals:silver_nugget": ("silver", "nugget"), "fundamentals:silver_plate": ("silver", "plate"),
    "minecraft:iron_ingot": ("iron", "ingot"), "minecraft:iron_nugget": ("iron", "nugget"), "create:iron_sheet": ("iron", "plate"),
    "tfmg:steel_ingot": ("steel", "ingot"), "tfmg:steel_nugget": ("steel", "nugget"), "tfmg:heavy_plate": ("steel", "plate"),
    "fundamentals:calcium_ingot": ("calcium", "ingot"), "fundamentals:magnesium_ingot": ("magnesium", "ingot"),
    "tfmg:lithium_ingot": ("lithium", "ingot"), "tfmg:lithium_nugget": ("lithium", "nugget"),
    "fundamentals:didymium_ingot": ("didymium", "ingot"),
    **{f"fundamentals:{m}_ingot": (m, "ingot") for m in ("lanthanum", "cerium", "praseodymium", "neodymium", "samarium", "gadolinium",
                                                         "terbium", "dysprosium", "yttrium")},
    **{f"fundamentals:{m}_nugget": (m, "nugget") for m in ("praseodymium", "neodymium", "samarium", "terbium", "dysprosium")},
}
# An ingot of iron or steel rusts through to a rusty one, which sandpaper takes back to seven nuggets: the rest flaked off.
RUSTY = {"minecraft:iron_ingot": ("rusty_iron_ingot", "minecraft:iron_nugget"), "tfmg:steel_ingot": ("rusty_steel_ingot", "tfmg:steel_nugget")}
# Vanilla's copper weathers block by block in the world already; held as items, in a chest, it goes the same way.
COPPER_PIECES = ("copper_block", "cut_copper", "chiseled_copper", "cut_copper_slab", "cut_copper_stairs", "copper_door",
                 "copper_trapdoor", "copper_grate", "copper_bulb")
COPPER_STAGES = ("", "exposed_", "weathered_", "oxidized_")

STAGE_NAMES = {
    "patina": ("Dulled", "Patinated", "Green with verdigris"),
    "tarnish": ("Faintly tarnished", "Tarnished", "Black with tarnish"),
    "rust": ("Spotted with rust", "Rusting"),
    "flaking": ("Tarnished", "Flaking with oxide"),
}
STAGE_TITLES = {"exposed": "Exposed", "weathered": "Weathered", "oxidized": "Oxidized", "dulled": "Dulled", "tarnished": "Tarnished",
                "blackened": "Blackened", "corroded": "Corroded", "crumbled": "Crumbled"}


def rate(metal, form, **extra):
    kind, days, dry, wet, _ = METALS[metal]
    entry = {"kind": kind, "days": round(days / DAY_FACTOR[form], 3)}
    if dry != 0.25:
        entry["dry"] = dry
    if wet != 4:
        entry["wet"] = wet
    return {**entry, **extra}


def rates():
    values = {}
    for item, (metal, form) in ITEMS.items():
        kind, _, _, _, oxide = METALS[metal]
        if item in RUSTY:
            values[item] = rate(metal, form, stages=1, product=f"fundamentals:{RUSTY[item][0]}")
        elif kind == "rust":
            values[item] = rate(metal, form, stages=2)
        elif oxide:
            values[item] = rate(metal, form, stages=2, product=oxide, **({"per": 9} if form == "nugget" else {}))
        else:
            values[item] = rate(metal, form)
    for piece in COPPER_PIECES:
        for a, b in zip(COPPER_STAGES, COPPER_STAGES[1:]):
            name = lambda p: "copper_block" if p == "" and piece == "copper_block" else f"{p}{piece.replace('copper_block', 'copper')}"
            values[f"minecraft:{name(a)}"] = rate("copper", "block", stages=0, product=f"minecraft:{name(b)}")
    for metal, (prefixes, _, _, _) in FAMILIES.items():
        for a, b in zip(prefixes, prefixes[1:]):
            values[f"fundamentals:{a}{metal}_block"] = rate(metal, "block", stages=0, product=f"fundamentals:{b}{metal}_block")
    return values


def stage_blocks(lang):
    oxidizables, waxables = {}, {}
    for metal, (prefixes, _, waxable, crumbles) in FAMILIES.items():
        display = metal.title()
        for i, prefix in enumerate(prefixes):
            name = f"{prefix}{metal}_block"
            if i < 3:
                oxidizables[f"fundamentals:{name}"] = {"next_oxidation_stage": f"fundamentals:{prefixes[i + 1]}{metal}_block"}
            if waxable:
                waxables[f"fundamentals:{name}"] = {"waxed": f"fundamentals:waxed_{name}"}
        for name in family_blocks(metal):
            texture = name.removeprefix("waxed_")
            if not name.startswith("waxed_"):
                write(ASSETS / f"models/block/{name}.json", cube(f"fundamentals:block/{name}"))
            write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"fundamentals:block/{texture}"}}})
            write(ASSETS / f"models/item/{name}.json", {"parent": f"fundamentals:block/{texture}"})
            pool = drop_self(name)
            if crumbles and name == f"{prefixes[3]}{metal}_block":
                # a block crumbled to oxide comes apart as the oxide: nine of it
                pool = {**pool, "entries": [{"type": "minecraft:item", "name": f"fundamentals:{metal}_oxide",
                                             "functions": [{"function": "minecraft:set_count", "count": 9}]}]}
            write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [pool]})
            stage = name.removeprefix("waxed_").removesuffix(f"{metal}_block").rstrip("_")
            lang[f"block.fundamentals.{name}"] = ("Waxed " if name.startswith("waxed_") else "") + \
                (f"{STAGE_TITLES[stage]} " if stage else "") + f"Block of {display}"
    neoforge = ROOT / "data/neoforge/data_maps/block"
    write(neoforge / "oxidizables.json", {"replace": False, "values": oxidizables})
    write(neoforge / "waxables.json", {"replace": False, "values": waxables})


def staged_models(values):
    """Each stage of an ageing ingot, nugget or sheet its own look: the item's model picks the stage's model by the
    fundamentals:oxidation_stage item property, which reads the stage off the stack."""
    for item, (_, stages) in STAGED.items():
        entry = values[item]
        shown = entry.get("stages", 3)
        if shown != stages:
            raise SystemExit(f"{item} shows {shown} stages but paint_oxidation.py paints {stages}")
        namespace, name = item.split(":")
        generated = lambda texture: {"parent": "minecraft:item/generated", "textures": {"layer0": texture}}
        for stage in range(1, stages + 1):
            write(ASSETS / f"models/item/aged/{aged_name(item, stage)}.json", generated(f"fundamentals:item/aged/{aged_name(item, stage)}"))
        write(ROOT / f"assets/{namespace}/models/item/{name}.json", {**generated(f"{namespace}:item/{name}"), "overrides": [
            {"predicate": {"fundamentals:oxidation_stage": stage}, "model": f"fundamentals:item/aged/{aged_name(item, stage)}"}
            for stage in range(1, stages + 1)]})


def polishing(values):
    """Sandpaper takes a patina, a tarnish, the first rust or the first oxide off an ingot, nugget or sheet, at no loss; a rusty
    ingot comes back as seven nuggets."""
    for item, entry in values.items():
        if entry.get("stages", 3) == 0:
            continue
        stages = range(1, entry.get("stages", 3) + 1)
        write(RECIPES / f"polishing/{item.split(':')[1]}.json", {
            "type": "create:sandpaper_polishing",
            "ingredients": [{"type": "neoforge:compound", "children": [
                {"type": "neoforge:components", "items": item, "components": {"fundamentals:oxidation_stage": s}} for s in stages]}],
            "results": [{"id": item}]})
    for _, (rusty, nugget) in RUSTY.items():
        write(RECIPES / f"polishing/{rusty}.json", {
            "type": "create:sandpaper_polishing", "ingredients": [{"item": f"fundamentals:{rusty}"}],
            "results": [{"id": nugget, "count": 7}]})


def box(a, b, faces, **extra):
    return {"from": a, "to": b, "faces": faces, **extra}


def drum_elements():
    """A sealed steel drum in Create's manner: an octagon of staves, two rolling hoops, the lid's valve with its red handwheel on
    top, and a pressure gauge on the front (north; the blockstate turns it to face whoever placed it)."""
    side, lid, base, fit = "#side", "#top", "#bottom", "#fittings"
    stave = lambda u0, u1: {"texture": side, "uv": [u0, 2, u1, 16]}
    elements = [
        box([3, 0, 2], [13, 14, 14], {"north": stave(3, 13), "south": stave(3, 13), "east": stave(2, 3), "west": stave(13, 14),
                                      "up": {"texture": lid, "uv": [3, 2, 13, 14]},
                                      "down": {"texture": base, "uv": [3, 2, 13, 14], "cullface": "down"}}),
    ]
    for x0, x1 in ((2, 3), (13, 14)):
        elements.append(box([x0, 0, 3], [x1, 14, 13], {
            "north": stave(13, 14), "south": stave(2, 3), "east" if x0 == 13 else "west": stave(3, 13),
            "up": {"texture": lid, "uv": [x0, 3, x1, 13]}, "down": {"texture": base, "uv": [x0, 3, x1, 13], "cullface": "down"}}))
    hoop = lambda u0, u1: {"texture": fit, "uv": [u0, 0, u1, 1]}
    ledge = lambda u0, u1: {"texture": fit, "uv": [u0, 1, u1, 2]}
    for y in (3, 10):
        elements.append(box([2.5, y, 1.5], [13.5, y + 1, 14.5], {"north": hoop(2.5, 13.5), "south": hoop(2.5, 13.5),
                                                                 "east": hoop(0, 1), "west": hoop(0, 1),
                                                                 "up": ledge(2.5, 13.5), "down": ledge(2.5, 13.5)}))
        for x0, x1 in ((1.5, 2.5), (13.5, 14.5)):
            elements.append(box([x0, y, 2.5], [x1, y + 1, 13.5], {
                "east" if x0 > 8 else "west": hoop(2.5, 13.5), "north": hoop(0, 1), "south": hoop(0, 1),
                "up": ledge(0, 11), "down": ledge(0, 11)}))
    brass = {"texture": fit, "uv": [8, 8, 12, 12]}
    rim = {"texture": fit, "uv": [12, 14, 16, 15]}
    case = {"texture": fit, "uv": [6, 4, 12, 5]}
    elements += [
        box([9, 14, 4], [11, 15, 6], {f: brass for f in ("north", "south", "east", "west")}),
        box([8, 15, 3], [12, 16, 7], {"north": rim, "south": rim, "east": rim, "west": rim,
                                      "up": {"texture": fit, "uv": [12, 10, 16, 14]}, "down": rim}),
        box([5, 4, 1], [11, 10, 2], {"north": {"texture": fit, "uv": [0, 4, 6, 10]}, "east": case, "west": case, "up": case, "down": case}),
    ]
    return elements


def storage(lang):
    write(ASSETS / "blockstates/inert_storage_drum.json", {"variants": {
        f"facing={facing}": {"model": "fundamentals:block/inert_storage_drum", **({"y": y} if y else {})}
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write(ASSETS / "models/block/inert_storage_drum.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "fundamentals:block/inert_storage_drum_side", "side": "fundamentals:block/inert_storage_drum_side",
        "top": "fundamentals:block/inert_storage_drum_top", "bottom": "fundamentals:block/inert_storage_drum_bottom",
        "fittings": "fundamentals:block/inert_storage_drum_fittings"}, "elements": drum_elements()})
    write(ASSETS / "models/item/inert_storage_drum.json", {"parent": "fundamentals:block/inert_storage_drum"})
    write(DATA / "loot_table/blocks/inert_storage_drum.json", {"type": "minecraft:block", "pools": [drop_self("inert_storage_drum")]})
    for name in ("canister", "argon_canister", "rusty_iron_ingot", "rusty_steel_ingot"):
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})
    write(RECIPES / "inert_storage_drum.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SVS", "SCS", "SSS"],
        "key": {"S": {"tag": "c:plates/iron"}, "V": {"item": "create:fluid_valve"}, "C": {"tag": "c:chests/wooden"}},
        "result": {"id": "fundamentals:inert_storage_drum", "count": 1}})
    write(RECIPES / "canister.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["N", "S", "S"],
        "key": {"N": {"tag": "c:nuggets/brass"}, "S": {"tag": "c:plates/iron"}},
        "result": {"id": "fundamentals:canister", "count": 2}})
    write(RECIPES / "argon_canister.json", {
        "type": "create:filling",
        "ingredients": [{"item": "fundamentals:canister"}, {"type": "neoforge:single", "amount": 100, "fluid": "fundamentals:argon"}],
        "results": [{"id": "fundamentals:argon_canister"}]})
    lang["block.fundamentals.inert_storage_drum"] = "Inert Storage Drum"
    lang["block.fundamentals.inert_storage_drum.empty"] = "No argon or kerosene: what is inside is in the air"
    lang["block.fundamentals.inert_storage_drum.charged"] = "Under %s mB of %s"
    lang["goggles.fundamentals.inert_storage_drum.argon"] = "%s mB of %s, leaking %s mB a day"
    lang["goggles.fundamentals.inert_storage_drum.kerosene"] = "%s mB of %s, which does not leak"
    lang["goggles.fundamentals.inert_storage_drum.no_gas"] = "No argon or kerosene in it"
    lang["goggles.fundamentals.inert_storage_drum.holds"] = "%s of %s slots full, %s items"
    lang["goggles.fundamentals.inert_storage_drum.kept"] = "Nothing inside is ageing"
    lang["goggles.fundamentals.inert_storage_drum.ageing"] = "What is inside is ageing in the air"
    lang["item.fundamentals.canister"] = "Canister"
    lang["item.fundamentals.argon_canister"] = "Argon Canister"
    lang["item.fundamentals.argon_canister.empty"] = "Charged with argon. Right-click a stack onto it to seal it in"
    lang["item.fundamentals.argon_canister.holds"] = "Sealed under argon: %s × %s"
    lang["item.fundamentals.rusty_iron_ingot"] = "Rusty Iron Ingot"
    lang["item.fundamentals.rusty_steel_ingot"] = "Rusty Steel Ingot"


def main():
    shutil.rmtree(RECIPES, ignore_errors=True)
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    values = rates()
    write(DATA / "data_maps/item/oxidation.json", {"replace": False, "values": values})
    stage_blocks(lang)
    polishing(values)
    staged_models(values)
    storage(lang)
    for kind, names in STAGE_NAMES.items():
        for i, name in enumerate(names, 1):
            lang[f"oxidation.fundamentals.{kind}.{i}"] = name
    write(lang_path, dict(sorted(lang.items())))
    print(f"{len(values)} ageing metal items, {sum(len(family_blocks(m)) for m in FAMILIES)} weathering blocks")


if __name__ == "__main__":
    main()
