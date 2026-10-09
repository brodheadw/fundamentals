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
from paint_oxidation import FAMILIES, family_blocks

RECIPES = DATA / "recipe/oxidation"
DAY_FACTOR = {"ingot": 1, "plate": 1, "nugget": 1.5, "block": 0.25}

# metal: (kind, days per stage as an ingot, dry factor, wet factor, the oxide a flaking metal ends as)
METALS = {
    "copper": ("patina", 3, 0.25, 3, None),
    "brass": ("patina", 8, 0.25, 3, None),
    "bronze": ("patina", 6, 0.25, 3, None),
    "silver": ("tarnish", 8, 0.5, 1.5, None),
    "iron": ("rust", 15, 0, 6, None),
    "steel": ("rust", 25, 0, 6, None),
    "lanthanum": ("flaking", 1, 0.25, 4, "fundamentals:lanthanum_oxide"),
    "cerium": ("flaking", 1, 0.25, 4, "fundamentals:cerium_oxide"),
    "praseodymium": ("flaking", 2, 0.25, 4, "fundamentals:praseodymium_oxide"),
    "didymium": ("flaking", 2.5, 0.25, 4, "fundamentals:didymium_oxide"),
    "neodymium": ("flaking", 3, 0.25, 4, "fundamentals:neodymium_oxide"),
    "samarium": ("flaking", 8, 0.25, 4, "fundamentals:samarium_oxide"),
    "gadolinium": ("flaking", 8, 0.25, 4, "fundamentals:gadolinium_oxide"),
    "terbium": ("flaking", 15, 0.25, 4, "fundamentals:terbium_oxide"),
    "dysprosium": ("flaking", 15, 0.25, 4, "fundamentals:dysprosium_oxide"),
    "yttrium": ("flaking", 20, 0.25, 4, "fundamentals:yttrium_oxide"),
    # the alkali and alkaline earth metals crust over white (calcium, magnesium) or black (lithium's nitride); nothing here holds the crust
    "calcium": ("tarnish", 0.5, 0.25, 4, None),
    "lithium": ("tarnish", 1, 0.25, 4, None),
    "magnesium": ("tarnish", 10, 0.25, 3, None),
}

# item: (metal, form)
ITEMS = {
    "minecraft:copper_ingot": ("copper", "ingot"), "create:copper_nugget": ("copper", "nugget"), "create:copper_sheet": ("copper", "plate"),
    "create:brass_ingot": ("brass", "ingot"), "create:brass_nugget": ("brass", "nugget"), "create:brass_sheet": ("brass", "plate"),
    "fundamentals:bronze_ingot": ("bronze", "ingot"), "fundamentals:bronze_nugget": ("bronze", "nugget"), "fundamentals:bronze_plate": ("bronze", "plate"),
    "fundamentals:silver_ingot": ("silver", "ingot"), "fundamentals:silver_nugget": ("silver", "nugget"), "fundamentals:silver_plate": ("silver", "plate"),
    "minecraft:iron_ingot": ("iron", "ingot"), "minecraft:iron_nugget": ("iron", "nugget"), "create:iron_sheet": ("iron", "plate"),
    "tfmg:steel_ingot": ("steel", "ingot"), "tfmg:steel_nugget": ("steel", "nugget"),
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


def storage(lang):
    write(ASSETS / "blockstates/inert_storage_drum.json", {"variants": {"": {"model": "fundamentals:block/inert_storage_drum"}}})
    write(ASSETS / "models/block/inert_storage_drum.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": "fundamentals:block/inert_storage_drum_side", "top": "fundamentals:block/inert_storage_drum_top",
        "bottom": "fundamentals:block/inert_storage_drum_bottom"}})
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
    storage(lang)
    for kind, names in STAGE_NAMES.items():
        for i, name in enumerate(names, 1):
            lang[f"oxidation.fundamentals.{kind}.{i}"] = name
    write(lang_path, dict(sorted(lang.items())))
    print(f"{len(values)} ageing metal items, {sum(len(family_blocks(m)) for m in FAMILIES)} weathering blocks")


if __name__ == "__main__":
    main()
