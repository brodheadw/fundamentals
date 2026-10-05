#!/usr/bin/env python3
"""Generates every data/asset file the ore and rock blocks need, from the tables below.

Blocks (ORES, ROCKS): blockstate, block + item model, loot table, mining/commodity tags, lang.
World generation (DEPOSITS, PLACERS): configured + placed features, each deposit type's biome
tag and the NeoForge biome modifiers. Re-run after any edit.

    python3 tools/paint_minerals.py && python3 tools/build_ore_data.py

Ore does not generate as scattered blobs. Each row of DEPOSITS is a body in a real shape
(worldgen.DepositFeature): tune a deposit by editing its row.

An ore block takes on the rock it formed in: its `host` blockstate property picks the texture
drawn under the mineral (HOSTS below), so one ore block serves stone, deepslate, granite, the
Create stones and our own rocks. A few ores are whole rocks instead (paint_minerals.WHOLE).
"""
import json
import shutil
from pathlib import Path

from paint_minerals import GRADES, ROCK_BLOCKS, VARIANTS, WHOLE

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

# Rocks an ore can sit in: host name -> (block it stands for, texture drawn under the mineral).
# Must list the same names, in the same order, as registry.OreBlock.Host.
HOSTS = {
    "stone": ("minecraft:stone", "minecraft:block/stone"),
    "deepslate": ("minecraft:deepslate", "minecraft:block/deepslate"),
    "granite": ("minecraft:granite", "minecraft:block/granite"),
    "diorite": ("minecraft:diorite", "minecraft:block/diorite"),
    "andesite": ("minecraft:andesite", "minecraft:block/andesite"),
    "tuff": ("minecraft:tuff", "minecraft:block/tuff"),
    "calcite": ("minecraft:calcite", "minecraft:block/calcite"),
    "dripstone": ("minecraft:dripstone_block", "minecraft:block/dripstone_block"),
    "sand": ("minecraft:sand", "minecraft:block/sand"),
    "carbonatite": ("fundamentals:carbonatite", "fundamentals:block/carbonatite"),
    "gabbro": ("fundamentals:gabbro", "fundamentals:block/gabbro"),
    "syenite": ("fundamentals:syenite", "fundamentals:block/syenite"),
    "laterite": ("fundamentals:laterite", "fundamentals:block/laterite"),
    "asurine": ("create:asurine", "create:block/palettes/stone_types/natural/asurine_0"),
    "crimsite": ("create:crimsite", "create:block/palettes/stone_types/natural/crimsite_0"),
    "ochrum": ("create:ochrum", "create:block/palettes/stone_types/natural/ochrum_0"),
    "veridium": ("create:veridium", "create:block/palettes/stone_types/natural/veridium_0"),
    "limestone": ("create:limestone", "create:block/palettes/stone_types/limestone"),
    "scoria": ("create:scoria", "create:block/palettes/stone_types/scoria"),
    "scorchia": ("create:scorchia", "create:block/palettes/stone_types/scorchia"),
}

# What a deposit may replace. Ore adopts whichever of these it lands in.
ROCK = ["#minecraft:stone_ore_replaceables", "#minecraft:deepslate_ore_replaceables", "minecraft:calcite",
        "minecraft:dripstone_block"] + [block for block, _ in HOSTS.values() if block.startswith("create:")]
REPLACEABLE = {
    "rock": ROCK,
    "ground": ROCK + ["#minecraft:dirt", "minecraft:gravel", "minecraft:clay", "minecraft:mud"],
}

# name: shape, host, [(ore, share of the body, style)], radius, thickness, height,
#       where (BIOMES key), y range of the centre, one per N chunks, what it replaces.
# host   a block the whole body is turned into, or None to leave the existing rock in place so
#        the ore sits directly in whatever is there (stone, deepslate, granite, a Create stone...).
#        A host ending in _ore means the whole body is that ore (bog iron, REE clay).
# styles disseminated (scattered grains) / pockets (masses) / seams (layers) / top (upper part).
DEPOSITS = {
    # Create's stones are themed on metals, and we use them that way: crimsite carries iron,
    # asurine zinc, ochrum the rusty oxidised cap, limestone the lead-zinc and manganese beds.
    # --- iron: beds of banded iron formation, common everywhere ---
    "hematite_bed": ("bed", "create:crimsite", [("magnetite", 0.06, "seams"), ("hematite", 0.42, "pockets")],
                     (9, 14), (3, 6), None, "anywhere", (0, 96), 7, "rock"),
    "magnetite_bed": ("bed", None, [("hematite", 0.06, "seams"), ("magnetite", 0.42, "pockets")],
                      (8, 12), (3, 5), None, "anywhere", (-56, 8), 12, "rock"),
    "bog_iron": ("blanket", "goethite_ore", [], (6, 9), (1, 2), None, "wetland", (60, 64), 3, "ground"),
    # --- stratabound beds in ordinary rock ---
    "lead_zinc_bed": ("bed", "create:limestone", [("sphalerite", 0.20, "pockets"), ("galena", 0.13, "pockets")],
                      (9, 13), (3, 5), None, "anywhere", (-40, 36), 10, "rock"),
    "zinc_oxide_bed": ("bed", "create:asurine", [("smithsonite", 0.24, "pockets")], (7, 10), (2, 4), None,
                       "anywhere", (36, 72), 18, "rock"),
    "manganese_bed": ("bed", "create:limestone", [("pyrolusite", 0.28, "pockets")], (8, 12), (3, 5), None,
                      "anywhere", (0, 60), 14, "rock"),
    "tungsten_skarn": ("bed", "create:limestone", [("scheelite", 0.25, "pockets")], (6, 9), (3, 4), None,
                       "hydrothermal", (-32, 40), 14, "rock"),
    "mercury_lens": ("bed", None, [("cinnabar", 0.25, "pockets")], (5, 8), (2, 4), None,
                     "hydrothermal", (0, 72), 14, "rock"),
    # --- porphyry copper: a big low-grade stock of andesite, enriched near the top ---
    "porphyry_stock": ("plug", "minecraft:andesite",
                       [("chalcopyrite", 0.18, "pockets"), ("molybdenite", 0.04, "pockets"),
                        ("bornite", 0.04, "disseminated"), ("chalcocite", 0.07, "top"), ("covellite", 0.02, "top")],
                       (7, 11), None, (24, 40), "porphyry", (0, 70), 8, "rock"),
    # --- the oxidised cap over copper and zinc, just under the surface in dry country ---
    "oxide_cap": ("blanket", "create:ochrum",
                  [("malachite", 0.20, "pockets"), ("azurite", 0.10, "pockets"), ("cuprite", 0.06, "disseminated"),
                   ("hemimorphite", 0.06, "pockets")],
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
    "tin_vein": ("vein", None, [("cassiterite", 0.36, "pockets"), ("wolframite", 0.16, "pockets")],
                 (10, 15), (2, 3), (12, 24), "pegmatite", (-16, 56), 9, "rock"),
    "silver_vein": ("vein", "minecraft:calcite", [("argentite", 0.38, "pockets"), ("native_silver", 0.16, "pockets")],
                    (10, 15), (1, 2), (14, 26), "hydrothermal", (-32, 64), 12, "rock"),
    "cobalt_vein": ("vein", "minecraft:calcite", [("cobaltite", 0.45, "pockets")], (8, 12), (1, 2), (12, 20),
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

# Blocks from elsewhere in the mod that share the mining-tool tags this script writes.
OTHER_MINEABLE = {"pickaxe": ["fundamentals:bloomery"]}

DISPLAY = {"bastnasite": "Bastnäsite", "ion_adsorption_clay": "Ion-Adsorption Clay"}

def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag(path, values):
    write(path, {"replace": False, "values": sorted(values)})


# What one ore block drops by grade, until raw mineral items exist: (count, chance).
DROPS = {"core": (2, 1.0), "edge": (1, 1.0), "trace": (1, 0.5)}


def drop_self(name, conditions=()):
    return {"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"fundamentals:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}, *conditions]}


def cube(texture):
    return {"parent": "minecraft:block/cube_all", "textures": {"all": texture}}


def overlay(texture):
    """A cube showing only the mineral; the host rock's model is drawn underneath it."""
    faces = {side: {"texture": "#ore", "cullface": side} for side in ("down", "up", "north", "south", "west", "east")}
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"particle": texture, "ore": texture},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]}


def rock_files(name, display, lang):
    write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"fundamentals:block/{name}"}}})
    write(ASSETS / f"models/block/{name}.json", cube(f"fundamentals:block/{name}"))
    write(ASSETS / f"models/item/{name}.json", {"parent": f"fundamentals:block/{name}"})
    lang[f"block.fundamentals.{name}"] = display
    write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [drop_self(name)]})


def ore_files(mineral, display, lang):
    """One ore block. Its blockstate is assembled from parts: the host rock's cube (by `host`),
    then the mineral on top (by `grade`, several variants so a large body does not tile).
    Richer ore drops more."""
    name, whole = f"{mineral}_ore", mineral in WHOLE
    parts, pools = [], []
    if not whole:
        parts += [{"when": {"host": host}, "apply": {"model": f"fundamentals:block/host/{host}"}} for host in HOSTS]
    for grade in GRADES:
        models = []
        for v in range(VARIANTS):
            texture = f"fundamentals:block/{name}_{grade}_{v}"
            write(ASSETS / f"models/block/ore/{name}_{grade}_{v}.json", cube(texture) if whole else overlay(texture))
            models.append({"model": f"fundamentals:block/ore/{name}_{grade}_{v}"})
        parts.append({"when": {"grade": grade}, "apply": models})
        count, chance = DROPS[grade]
        conditions = [{"condition": "minecraft:block_state_property", "block": f"fundamentals:{name}",
                       "properties": {"grade": grade}}]
        if chance < 1:
            conditions.append({"condition": "minecraft:random_chance", "chance": chance})
        pool = drop_self(name, conditions)
        if count > 1:
            pool["entries"][0]["functions"] = [{"function": "minecraft:set_count", "count": count}]
        pools.append(pool)
    write(ASSETS / f"blockstates/{name}.json", {"multipart": parts})
    # In the hand it is shown as it looks in plain stone.
    item = cube(f"fundamentals:block/{name}_edge_0") if whole else {
        "parent": "minecraft:block/block",
        "textures": {"particle": "minecraft:block/stone", "base": "minecraft:block/stone",
                     "ore": f"fundamentals:block/{name}_edge_0"},
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                      "faces": {side: {"texture": tex} for side in ("down", "up", "north", "south", "west", "east")}}
                     for tex in ("#base", "#ore")]}
    write(ASSETS / f"models/item/{name}.json", item)
    lang[f"block.fundamentals.{name}"] = display
    write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": pools})


def state(block, **properties):
    out = {"Name": block if ":" in block else f"fundamentals:{block}"}
    if properties:
        out["Properties"] = properties
    return out


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
    expected = {f"{name}_ore_{g}_{v}" for name in ORES for g in GRADES for v in range(VARIANTS)} | set(ROCKS)
    stale = {name for name in textures if "_ore_" in name} - expected
    assert not (expected - textures) and not stale, \
        f"textures and block tables disagree: missing {sorted(expected - textures)}, stale {sorted(stale)}"
    generated = {ore for _, _, ores, *_ in DEPOSITS.values() for ore, _, _ in ores} | set(PLACERS) \
        | {row[1][:-4] for row in DEPOSITS.values() if row[1] and row[1].endswith("_ore")}
    assert generated == set(ORES), f"ores that never generate, or unknown ores: {sorted(generated ^ set(ORES))}"

    # Only the worldgen folders are wholly ours; everything else is shared and just overwritten.
    for stale in (DATA / "worldgen", DATA / "neoforge", DATA / "tags/worldgen"):
        shutil.rmtree(stale, ignore_errors=True)
    shutil.rmtree(ASSETS / "models/block/ore", ignore_errors=True)

    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))  # other entries are kept as they are
    lang["itemGroup.fundamentals.minerals"] = "Fundamentals"

    for host, (_, texture) in HOSTS.items():
        write(ASSETS / f"models/block/host/{host}.json", cube(texture))

    blocks, by_tool, by_tier, by_commodity = [], {}, {}, {}
    for name, tool in ROCKS.items():
        rock_files(name, name.replace("_", " ").title(), lang)
        blocks.append({"name": name, "soft": tool == "shovel"})
        by_tool.setdefault(tool, []).append(f"fundamentals:{name}")
    for name, (commodity, tool, tier) in ORES.items():
        block = f"fundamentals:{name}_ore"
        ore_files(name, DISPLAY.get(name, name.replace("_", " ").title()), lang)
        blocks.append({"name": f"{name}_ore", "soft": tool == "shovel", "mineral": name})
        by_tool.setdefault(tool, []).append(block)
        if tier:
            by_tier.setdefault(tier, []).append(block)
        by_commodity.setdefault(commodity, []).append(block)
    write(lang_path, dict(sorted(lang.items())))

    for tool, names in by_tool.items():
        tag(MC_TAGS / f"mineable/{tool}.json", names + OTHER_MINEABLE.get(tool, []))
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
        entries = [{"state": state(f"{ore}_ore"), "fraction": share, "style": style} for ore, share, style in ores]
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
                       "targets": [{"state": state(f"{name}_ore", grade="edge", host="sand"),
                                    "target": {"predicate_type": "minecraft:block_match", "block": "minecraft:sand"}}]}})
        write(DATA / f"worldgen/placed_feature/placer_{name}.json",
              placed(f"fundamentals:placer_{name}", per_chunk, y_min, y_max))
        by_biomes.setdefault("placer", []).append(f"fundamentals:placer_{name}")

    for where, features in by_biomes.items():
        tag(DATA / f"tags/worldgen/biome/deposit/{where}.json", BIOMES[where])
        # After underground_ores, so a deposit lands on top of the stone layers Create generates
        # there and its ore takes on whichever Create stone it sits in.
        write(DATA / f"neoforge/biome_modifier/add_{where}_deposits.json", {
            "type": "neoforge:add_features", "biomes": f"#fundamentals:deposit/{where}", "features": features,
            "step": "underground_decoration"})
    write(DATA / "neoforge/biome_modifier/remove_vanilla_iron.json", {
        "type": "neoforge:remove_features", "biomes": "#minecraft:is_overworld", "features": REMOVED,
        "steps": ["underground_ores"]})
    # registry.OreBlocks registers exactly these blocks, so a block exists when its models, loot,
    # tags and spawn rules do.
    write(ROOT / "fundamentals_ores.json", {
        "blocks": blocks, "hosts": {host: block for host, (block, _) in HOSTS.items()}})
    print(f"wrote {len(blocks)} blocks ({len(ORES)} minerals, {len(ROCKS)} rocks), "
          f"{len(DEPOSITS)} deposit types, {len(PLACERS)} placers")


if __name__ == "__main__":
    main()
