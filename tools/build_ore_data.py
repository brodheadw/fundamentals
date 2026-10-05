#!/usr/bin/env python3
"""Generates every data/asset file the ore and rock blocks need, from the tables below.

Blocks (ORES, ROCKS): blockstate, block + item model, loot table, mining/commodity tags, lang.
World generation (DEPOSITS, PLACERS): configured + placed features, each deposit type's biome
tag, the NeoForge biome modifiers, and the list the Fabric side reads. Re-run after any edit.

    python3 tools/paint_minerals.py && python3 tools/build_ore_data.py

Ore does not generate as scattered blobs. Each row of DEPOSITS is a body in a real shape
(worldgen.DepositFeature): tune a deposit by editing its row.

Which ores are drawn over vanilla stone/deepslate/granite/sand and which are a full rock of
their own is decided in paint_minerals.OVERLAY; this script follows it.
"""
import json
import shutil
from pathlib import Path

from paint_minerals import OVERLAY, ROCK_BLOCKS

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
ASSETS = ROOT / "assets/fundamentals"
DATA = ROOT / "data/fundamentals"
MC_TAGS = ROOT / "data/minecraft/tags/block"
C_TAGS = ROOT / "data/c/tags"

# Where a deposit type occurs (PLAN §5).
BIOMES = {
    "anywhere": ["#minecraft:is_overworld"],
    "porphyry": ["#minecraft:is_mountain", "#minecraft:is_hill"],
    "arid_oxide": ["#minecraft:is_badlands", "#minecraft:is_savanna", "minecraft:desert"],
    "laterite": ["#minecraft:is_jungle", "#minecraft:is_savanna", "minecraft:mangrove_swamp"],
    "pegmatite": ["#minecraft:is_mountain", "#minecraft:is_hill", "#minecraft:is_badlands"],
    "carbonatite": ["#minecraft:is_mountain", "#minecraft:is_badlands"],
    "alkaline": ["#minecraft:is_taiga", "minecraft:snowy_plains", "minecraft:grove", "minecraft:snowy_slopes"],
    "ion_clay": ["#minecraft:is_jungle"],
    "placer": ["#minecraft:is_beach", "#minecraft:is_river"],
    "wetland": ["minecraft:swamp", "minecraft:mangrove_swamp"],
    "hydrothermal": ["#minecraft:is_mountain", "#minecraft:is_hill", "#minecraft:is_badlands"],
}

# mineral: (commodity, tool, tier). tier = "stone" / "iron" pickaxe needed, or None.
ORES = {
    "hematite": ("iron", "pickaxe", "stone"),
    "magnetite": ("iron", "pickaxe", "stone"),
    "goethite": ("iron", "pickaxe", None),
    "pyrolusite": ("manganese", "pickaxe", "stone"),
    "pentlandite": ("nickel", "pickaxe", "iron"),
    "nickel_laterite": ("nickel", "shovel", None),
    "chromite": ("chromium", "pickaxe", "iron"),
    "wolframite": ("tungsten", "pickaxe", "iron"),
    "scheelite": ("tungsten", "pickaxe", "iron"),
    "molybdenite": ("molybdenum", "pickaxe", "iron"),
    "cobaltite": ("cobalt", "pickaxe", "iron"),
    "ilmenite": ("titanium", "shovel", None),
    "rutile": ("titanium", "shovel", None),
    "chalcopyrite": ("copper", "pickaxe", "stone"),
    "bornite": ("copper", "pickaxe", "stone"),
    "chalcocite": ("copper", "pickaxe", "stone"),
    "covellite": ("copper", "pickaxe", "stone"),
    "malachite": ("copper", "pickaxe", "stone"),
    "azurite": ("copper", "pickaxe", "stone"),
    "cuprite": ("copper", "pickaxe", "stone"),
    "bauxite": ("aluminum", "pickaxe", None),
    "galena": ("lead", "pickaxe", "stone"),
    "sphalerite": ("zinc", "pickaxe", "stone"),
    "smithsonite": ("zinc", "pickaxe", "stone"),
    "hemimorphite": ("zinc", "pickaxe", "stone"),
    "cassiterite": ("tin", "pickaxe", "stone"),
    "bastnasite": ("rare_earth", "pickaxe", "iron"),
    "monazite": ("rare_earth", "shovel", None),
    "xenotime": ("rare_earth", "pickaxe", "iron"),
    "ion_adsorption_clay": ("rare_earth", "shovel", None),
    "loparite": ("rare_earth", "pickaxe", "iron"),
    "euxenite": ("rare_earth", "pickaxe", "iron"),
    "native_silver": ("silver", "pickaxe", "iron"),
    "argentite": ("silver", "pickaxe", "iron"),
    "sperrylite": ("platinum", "pickaxe", "iron"),
    "cooperite": ("platinum", "pickaxe", "iron"),
    "braggite": ("platinum", "pickaxe", "iron"),
    "cinnabar": ("mercury", "pickaxe", "iron"),
}

# Host rocks that are blocks of their own -> tool.
ROCKS = {name: "shovel" if name == "laterite" else "pickaxe" for name in ROCK_BLOCKS}

# What a deposit may replace.
REPLACEABLE = {
    "rock": ["#minecraft:stone_ore_replaceables", "#minecraft:deepslate_ore_replaceables"],
    "ground": ["#minecraft:stone_ore_replaceables", "#minecraft:deepslate_ore_replaceables", "#minecraft:dirt",
               "minecraft:gravel", "minecraft:clay", "minecraft:mud"],
}

# name: shape, host, [(ore, share of the body, style)], radius, thickness, height,
#       where (BIOMES key), y range of the centre, one per N chunks, what it replaces.
# host   a block the whole body is turned into, or None to leave the existing rock in place so
#        the ore sits directly in stone / deepslate (using each ore's deepslate version there).
#        A host ending in _ore means the whole body is that ore (bog iron, REE clay).
# styles disseminated (scattered grains) / pockets (masses) / seams (layers) / top (upper part).
DEPOSITS = {
    # --- iron: beds of banded iron formation, common everywhere ---
    "hematite_bed": ("bed", None, [("magnetite", 0.08, "seams"), ("hematite", 0.85, "pockets")],
                     (9, 14), (3, 6), None, "anywhere", (0, 96), 12, "rock"),
    "magnetite_bed": ("bed", None, [("hematite", 0.10, "seams"), ("magnetite", 0.85, "pockets")],
                      (8, 12), (3, 5), None, "anywhere", (-56, 8), 20, "rock"),
    "bog_iron": ("blanket", "goethite_ore", [], (6, 9), (1, 2), None, "wetland", (60, 64), 3, "ground"),
    # --- stratabound beds in ordinary rock ---
    "lead_zinc_bed": ("bed", None, [("sphalerite", 0.24, "pockets"), ("galena", 0.15, "pockets")],
                      (9, 13), (3, 5), None, "anywhere", (-40, 36), 10, "rock"),
    "zinc_oxide_bed": ("bed", None, [("smithsonite", 0.30, "pockets")], (7, 10), (2, 4), None,
                       "anywhere", (36, 72), 18, "rock"),
    "manganese_bed": ("bed", None, [("pyrolusite", 0.35, "seams")], (8, 12), (3, 5), None,
                      "anywhere", (0, 60), 14, "rock"),
    "tungsten_skarn": ("bed", None, [("scheelite", 0.25, "pockets")], (6, 9), (3, 4), None,
                       "hydrothermal", (-32, 40), 14, "rock"),
    "mercury_lens": ("bed", None, [("cinnabar", 0.25, "pockets")], (5, 8), (2, 4), None,
                     "hydrothermal", (0, 72), 14, "rock"),
    # --- porphyry copper: a big low-grade stock, enriched near the top ---
    "porphyry_stock": ("plug", None,
                       [("chalcopyrite", 0.22, "pockets"), ("molybdenite", 0.05, "pockets"),
                        ("bornite", 0.05, "disseminated"), ("chalcocite", 0.08, "top"), ("covellite", 0.02, "top")],
                       (7, 11), None, (24, 40), "porphyry", (0, 70), 8, "rock"),
    # --- the oxidised cap over copper and zinc, just under the surface in dry country ---
    "oxide_cap": ("blanket", None,
                  [("malachite", 0.25, "pockets"), ("azurite", 0.12, "pockets"), ("cuprite", 0.08, "disseminated"),
                   ("hemimorphite", 0.07, "pockets")],
                  (9, 14), (5, 8), None, "arid_oxide", (60, 64), 6, "rock"),
    # --- tropical weathering blankets: these are rocks of their own ---
    "bauxite_blanket": ("blanket", "laterite", [("bauxite", 0.50, "pockets")], (10, 15), (4, 7), None,
                        "laterite", (60, 64), 5, "ground"),
    "nickel_laterite_blanket": ("blanket", "laterite", [("nickel_laterite", 0.40, "pockets")], (9, 13), (4, 6),
                                None, "laterite", (60, 64), 9, "ground"),
    "ion_clay_blanket": ("blanket", "ion_adsorption_clay_ore", [], (8, 12), (3, 5), None,
                         "ion_clay", (60, 64), 6, "ground"),
    # --- rare intrusions: bodies of their own rock, the big finds ---
    "carbonatite_plug": ("plug", "carbonatite", [("bastnasite", 0.16, "pockets")], (6, 9), None, (20, 34),
                         "carbonatite", (-56, 0), 36, "rock"),
    "syenite_massif": ("plug", "syenite", [("loparite", 0.10, "seams")], (8, 12), None, (16, 26),
                       "alkaline", (-32, 32), 24, "rock"),
    "layered_intrusion": ("bed", "gabbro",
                          [("chromite", 0.12, "seams"), ("pentlandite", 0.05, "pockets"),
                           ("sperrylite", 0.006, "disseminated"), ("cooperite", 0.005, "disseminated"),
                           ("braggite", 0.005, "disseminated")],
                          (12, 15), (8, 12), None, "anywhere", (-60, -28), 60, "rock"),
    "nickel_sulfide": ("bed", "gabbro", [("pentlandite", 0.14, "pockets")], (9, 12), (5, 7), None,
                       "anywhere", (-60, -8), 45, "rock"),
    # --- veins and dykes in mountain country ---
    "pegmatite_dyke": ("vein", "minecraft:granite", [("xenotime", 0.08, "pockets"), ("euxenite", 0.04, "pockets")],
                       (11, 15), (3, 5), (14, 24), "pegmatite", (-16, 48), 16, "rock"),
    "tin_vein": ("vein", None, [("cassiterite", 0.50, "pockets"), ("wolframite", 0.22, "pockets")],
                 (10, 15), (2, 3), (12, 24), "pegmatite", (-16, 56), 9, "rock"),
    "silver_vein": ("vein", None, [("argentite", 0.50, "pockets"), ("native_silver", 0.22, "pockets")],
                    (10, 15), (1, 2), (14, 26), "hydrothermal", (-32, 64), 12, "rock"),
    "cobalt_vein": ("vein", None, [("cobaltite", 0.60, "pockets")], (8, 12), (1, 2), (12, 20),
                    "hydrothermal", (-32, 32), 20, "rock"),
}

# Heavy minerals washed into beach and river sand: (y_min, y_max, veins per chunk, vein size).
PLACERS = {
    "monazite": (54, 66, 4, 9),
    "ilmenite": (54, 66, 5, 12),
    "rutile": (54, 66, 3, 9),
}

# Vanilla ore features switched off: hematite/magnetite replace vanilla iron (PLAN §2.4).
# Vanilla gold and copper ore stay — they are native gold and native copper.
REMOVED = ["minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_iron_small"]

DISPLAY = {"bastnasite": "Bastnäsite", "ion_adsorption_clay": "Ion-Adsorption Clay"}

# Rock blocks earlier versions generated; their files are removed if still lying around.
RETIRED = ["limestone", "gossan", "porphyry", "greisen", "pegmatite", "vein_quartz"] + [
    f"deepslate_{name}_ore" for name in ("smithsonite", "chalcocite", "covellite", "malachite", "azurite",
                                         "cuprite", "hemimorphite")]


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag(path, values):
    write(path, {"replace": False, "values": sorted(values)})


def ore_blocks(name):
    """[(block name, vanilla block it is drawn over or None)] for one mineral."""
    if name not in OVERLAY:
        return [(f"{name}_ore", None)]
    return [(("deepslate_" if base == "deepslate" else "") + f"{name}_ore", base) for base in OVERLAY[name][0]]


def block_files(name, display, lang, texture=None, base=None):
    texture = texture or name
    write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"fundamentals:block/{name}"}}})
    if base:
        # The ore is a transparent layer over the game's own texture for the rock it sits in.
        def faces(tex):
            return {side: {"texture": tex, "cullface": side}
                    for side in ("down", "up", "north", "south", "west", "east")}
        model = {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                 "textures": {"particle": f"minecraft:block/{base}", "base": f"minecraft:block/{base}",
                              "overlay": f"fundamentals:block/{texture}"},
                 "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces("#base")},
                              {"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces("#overlay")}]}
    else:
        model = {"parent": "minecraft:block/cube_all", "textures": {"all": f"fundamentals:block/{texture}"}}
    write(ASSETS / f"models/block/{name}.json", model)
    write(ASSETS / f"models/item/{name}.json", {"parent": f"fundamentals:block/{name}"})
    lang[f"block.fundamentals.{name}"] = display
    write(DATA / f"loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": f"fundamentals:{name}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def state(block):
    return {"Name": block if ":" in block else f"fundamentals:{block}"}


def placed(feature, per_chunk, y_min, y_max):
    frequency = {"type": "minecraft:count", "count": per_chunk} if per_chunk >= 1 \
        else {"type": "minecraft:rarity_filter", "chance": round(1 / per_chunk)}
    return {"feature": feature,
            "placement": [frequency, {"type": "minecraft:in_square"},
                          {"type": "minecraft:height_range",
                           "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": y_min},
                                      "max_inclusive": {"absolute": y_max}}},
                          {"type": "minecraft:biome"}]}


def main():
    textures = {p.stem for p in (ASSETS / "textures/block").glob("*.png")}
    expected = {f"{name}_ore" for name in ORES} | set(ROCKS)
    assert textures == expected, f"textures and block tables disagree: {sorted(textures ^ expected)}"
    generated = {ore for _, _, ores, *_ in DEPOSITS.values() for ore, _, _ in ores} | set(PLACERS) \
        | {row[1][:-4] for row in DEPOSITS.values() if row[1] and row[1].endswith("_ore")}
    assert generated == set(ORES), f"ores that never generate, or unknown ores: {sorted(generated ^ set(ORES))}"

    # Only the worldgen folders are wholly ours; everything else is shared and just overwritten.
    for stale in (DATA / "worldgen", DATA / "neoforge", DATA / "tags/worldgen"):
        shutil.rmtree(stale, ignore_errors=True)
    for folder in (ASSETS / "blockstates", ASSETS / "models/block", ASSETS / "models/item", DATA / "loot_table/blocks"):
        for name in RETIRED:
            (folder / f"{name}.json").unlink(missing_ok=True)

    lang_path = ASSETS / "lang/en_us.json"
    lang = {k: v for k, v in json.loads(lang_path.read_text(encoding="utf-8")).items()
            if not k.startswith("block.fundamentals.")}
    lang["itemGroup.fundamentals.minerals"] = "Fundamentals: Minerals"

    blocks, by_tool, by_tier, by_commodity = [], {}, {}, {}
    for name, tool in ROCKS.items():
        block_files(name, name.replace("_", " ").title(), lang)
        blocks.append({"name": name, "soft": tool == "shovel", "overlay": False})
        by_tool.setdefault(tool, []).append(f"fundamentals:{name}")
    for name, (commodity, tool, tier) in ORES.items():
        display = DISPLAY.get(name, name.replace("_", " ").title())
        for block, base in ore_blocks(name):
            block_files(block, ("Deepslate " if base == "deepslate" else "") + display, lang, f"{name}_ore", base)
            blocks.append({"name": block, "soft": tool == "shovel", "overlay": base is not None, "mineral": name})
            by_tool.setdefault(tool, []).append(f"fundamentals:{block}")
            if tier:
                by_tier.setdefault(tier, []).append(f"fundamentals:{block}")
            by_commodity.setdefault(commodity, []).append(f"fundamentals:{block}")
    write(lang_path, dict(sorted(lang.items())))

    for tool, names in by_tool.items():
        tag(MC_TAGS / f"mineable/{tool}.json", names)
    for tier, names in by_tier.items():
        tag(MC_TAGS / f"needs_{tier}_tool.json", names)
    for kind in ("block", "item"):
        tag(C_TAGS / f"{kind}/ores.json", [f"#c:ores/{c}" for c in by_commodity])
        for commodity, names in by_commodity.items():
            tag(C_TAGS / f"{kind}/ores/{commodity}.json", names)
    for key, values in REPLACEABLE.items():
        tag(DATA / f"tags/block/deposit_replaceable/{key}.json", values)

    by_biomes = {}
    for name, (shape, host, ores, radius, thickness, height, where, y, chunks, replaces) in DEPOSITS.items():
        entries = []
        for ore, share, style in ores:
            entry = {"state": state(f"{ore}_ore"), "fraction": share, "style": style}
            if "deepslate" in OVERLAY.get(ore, ((),))[0]:
                entry["deepslate_state"] = state(f"deepslate_{ore}_ore")
            entries.append(entry)
        config = {"shape": shape, "ores": entries,
                  "radius": {"min": radius[0], "max": radius[1]},
                  "thickness": {"min": (thickness or (1, 1))[0], "max": (thickness or (1, 1))[1]},
                  "replaceable": f"fundamentals:deposit_replaceable/{replaces}"}
        if host:
            config["host"] = state(host)
        if height:
            config["height"] = {"min": height[0], "max": height[1]}
        write(DATA / f"worldgen/configured_feature/{name}.json", {"type": "fundamentals:deposit", "config": config})
        write(DATA / f"worldgen/placed_feature/{name}.json", placed(f"fundamentals:{name}", 1 / chunks, *y))
        by_biomes.setdefault(where, []).append(f"fundamentals:{name}")
    for name, (y_min, y_max, per_chunk, size) in PLACERS.items():
        write(DATA / f"worldgen/configured_feature/placer_{name}.json", {
            "type": "minecraft:ore",
            "config": {"discard_chance_on_air_exposure": 0.0, "size": size,
                       "targets": [{"state": state(f"{name}_ore"),
                                    "target": {"predicate_type": "minecraft:block_match", "block": "minecraft:sand"}}]}})
        write(DATA / f"worldgen/placed_feature/placer_{name}.json",
              placed(f"fundamentals:placer_{name}", per_chunk, y_min, y_max))
        by_biomes.setdefault("placer", []).append(f"fundamentals:placer_{name}")

    spawns = []
    for where, features in by_biomes.items():
        tag(DATA / f"tags/worldgen/biome/deposit/{where}.json", BIOMES[where])
        write(DATA / f"neoforge/biome_modifier/add_{where}_deposits.json", {
            "type": "neoforge:add_features", "biomes": f"#fundamentals:deposit/{where}", "features": features,
            "step": "underground_ores"})
        spawns.append({"biomes": f"fundamentals:deposit/{where}", "features": features})
    write(DATA / "neoforge/biome_modifier/remove_vanilla_iron.json", {
        "type": "neoforge:remove_features", "biomes": "#minecraft:is_overworld", "features": REMOVED,
        "steps": ["underground_ores"]})
    # NeoForge reads the biome modifiers above; Fabric has no data-driven equivalent, so
    # worldgen.OreSpawns applies the same add/remove list through the Fabric biome API.
    # The same file lists the blocks registry.OreBlocks registers, so a block exists exactly
    # when its models, loot, tags and spawn rules do.
    write(ROOT / "fundamentals_ores.json", {"blocks": blocks, "add": spawns, "remove": REMOVED})
    print(f"wrote {len(blocks)} blocks ({len(ORES)} minerals, {len(ROCKS)} rocks), "
          f"{len(DEPOSITS)} deposit types, {len(PLACERS)} placers")


if __name__ == "__main__":
    main()
