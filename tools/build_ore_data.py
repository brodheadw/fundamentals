#!/usr/bin/env python3
"""Generates every data/asset file an ore block needs, from the one table below.

For each mineral: blockstate, block + item model, loot table, mining/commodity tags, lang entry,
and its world generation (configured + placed feature, the deposit's biome tag, the NeoForge
biome modifier, and the spawn list the Fabric side reads). Re-run after editing the table.

    python3 tools/build_ore_data.py

Spawn tuning lives in ORES: change a row, re-run, commit. PLAN §5 is the spec it follows.
"""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
ASSETS = ROOT / "assets/fundamentals"
DATA = ROOT / "data/fundamentals"
MC_TAGS = ROOT / "data/minecraft/tags/block"
C_TAGS = ROOT / "data/c/tags"

# Deposit type -> the biomes it occurs in (PLAN §5).
DEPOSITS = {
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

ROCK = ["minecraft:stone_ore_replaceables", "minecraft:deepslate_ore_replaceables"]
SOIL = ROCK + ["minecraft:dirt"]
SAND = ["minecraft:sand"]

# mineral: (commodity, deposit, y_min, y_max, spread, per_chunk, vein_size, replaces, tool, tier)
#   spread     "trapezoid" peaks mid-range, "uniform" is flat
#   per_chunk  >= 1: that many veins per chunk; < 1: one vein every 1/x chunks
#   tier       "stone" / "iron" pickaxe needed, or None
ORES = {
    # --- iron: common everywhere, as in reality ---
    "hematite": ("iron", "anywhere", -16, 112, "trapezoid", 14, 12, ROCK, "pickaxe", "stone"),
    "magnetite": ("iron", "anywhere", -64, 16, "uniform", 5, 10, ROCK, "pickaxe", "stone"),
    "goethite": ("iron", "wetland", 50, 70, "uniform", 6, 14, SOIL, "pickaxe", None),
    "pyrolusite": ("manganese", "anywhere", 0, 64, "uniform", 3, 8, ROCK, "pickaxe", "stone"),
    "pentlandite": ("nickel", "anywhere", -64, -8, "uniform", 2, 8, ROCK, "pickaxe", "iron"),
    "nickel_laterite": ("nickel", "laterite", 56, 84, "uniform", 3, 18, SOIL, "shovel", None),
    "chromite": ("chromium", "anywhere", -64, -16, "uniform", 1 / 3, 12, ROCK, "pickaxe", "iron"),
    "wolframite": ("tungsten", "pegmatite", -16, 48, "uniform", 1 / 2, 6, ROCK, "pickaxe", "iron"),
    "scheelite": ("tungsten", "hydrothermal", -32, 40, "uniform", 1 / 3, 6, ROCK, "pickaxe", "iron"),
    "molybdenite": ("molybdenum", "porphyry", -32, 48, "uniform", 2, 6, ROCK, "pickaxe", "iron"),
    "cobaltite": ("cobalt", "hydrothermal", -32, 32, "uniform", 1 / 3, 5, ROCK, "pickaxe", "iron"),
    "ilmenite": ("titanium", "placer", 54, 66, "uniform", 4, 8, SAND, "shovel", None),
    "rutile": ("titanium", "placer", 54, 66, "uniform", 2, 6, SAND, "shovel", None),
    # --- copper: sulfides in mountain porphyries, oxides near the surface in dry country ---
    "chalcopyrite": ("copper", "porphyry", -16, 96, "trapezoid", 10, 12, ROCK, "pickaxe", "stone"),
    "bornite": ("copper", "porphyry", -16, 64, "uniform", 3, 8, ROCK, "pickaxe", "stone"),
    "chalcocite": ("copper", "porphyry", 16, 80, "uniform", 3, 9, ROCK, "pickaxe", "stone"),
    "covellite": ("copper", "porphyry", 16, 64, "uniform", 1 / 3, 5, ROCK, "pickaxe", "stone"),
    "malachite": ("copper", "arid_oxide", 48, 96, "uniform", 5, 9, ROCK, "pickaxe", "stone"),
    "azurite": ("copper", "arid_oxide", 40, 88, "uniform", 3, 7, ROCK, "pickaxe", "stone"),
    "cuprite": ("copper", "arid_oxide", 40, 80, "uniform", 2, 6, ROCK, "pickaxe", "stone"),
    # --- aluminium, lead, zinc, tin ---
    "bauxite": ("aluminum", "laterite", 56, 90, "uniform", 4, 28, SOIL, "pickaxe", None),
    "galena": ("lead", "anywhere", -48, 40, "trapezoid", 5, 9, ROCK, "pickaxe", "stone"),
    "sphalerite": ("zinc", "anywhere", -48, 40, "trapezoid", 6, 10, ROCK, "pickaxe", "stone"),
    "smithsonite": ("zinc", "anywhere", 32, 80, "uniform", 2, 6, ROCK, "pickaxe", "stone"),
    "hemimorphite": ("zinc", "arid_oxide", 40, 88, "uniform", 2, 6, ROCK, "pickaxe", "stone"),
    "cassiterite": ("tin", "pegmatite", -16, 56, "uniform", 4, 7, ROCK, "pickaxe", "stone"),
    # --- rare earths: rare, biome-locked ---
    "bastnasite": ("rare_earth", "carbonatite", -56, 8, "uniform", 1 / 6, 30, ROCK, "pickaxe", "iron"),
    "monazite": ("rare_earth", "placer", 54, 66, "uniform", 4, 7, SAND, "shovel", None),
    "xenotime": ("rare_earth", "pegmatite", -16, 48, "uniform", 1 / 3, 6, ROCK, "pickaxe", "iron"),
    "ion_adsorption_clay": ("rare_earth", "ion_clay", 60, 96, "uniform", 3, 24, SOIL, "shovel", None),
    "loparite": ("rare_earth", "alkaline", -32, 32, "uniform", 1 / 5, 10, ROCK, "pickaxe", "iron"),
    "euxenite": ("rare_earth", "pegmatite", -32, 32, "uniform", 1 / 8, 5, ROCK, "pickaxe", "iron"),
    # --- precious: veins in the mountains; PGMs very rare at the bottom of the world ---
    "native_silver": ("silver", "hydrothermal", -16, 64, "uniform", 1 / 2, 5, ROCK, "pickaxe", "iron"),
    "argentite": ("silver", "hydrothermal", -32, 48, "uniform", 2, 6, ROCK, "pickaxe", "iron"),
    "sperrylite": ("platinum", "anywhere", -64, -32, "uniform", 1 / 12, 4, ROCK, "pickaxe", "iron"),
    "cooperite": ("platinum", "anywhere", -64, -32, "uniform", 1 / 14, 4, ROCK, "pickaxe", "iron"),
    "braggite": ("platinum", "anywhere", -64, -32, "uniform", 1 / 14, 4, ROCK, "pickaxe", "iron"),
    "cinnabar": ("mercury", "hydrothermal", 0, 72, "uniform", 1 / 2, 7, ROCK, "pickaxe", "iron"),
}

# Vanilla ore features switched off: hematite/magnetite replace vanilla iron (PLAN §2.4).
# Vanilla gold and copper ore stay — they are native gold and native copper.
REMOVED = ["minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_iron_small"]

DISPLAY = {"bastnasite": "Bastnäsite", "ion_adsorption_clay": "Ion-Adsorption Clay"}


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag(path, values):
    write(path, {"replace": False, "values": sorted(values)})


def main():
    textures = {p.stem[:-4] for p in (ASSETS / "textures/block").glob("*_ore.png")}
    assert textures == set(ORES), f"textures and ORES disagree: {sorted(textures ^ set(ORES))}"

    for stale in (DATA / "worldgen", DATA / "neoforge", DATA / "loot_table/blocks", DATA / "tags/worldgen"):
        shutil.rmtree(stale, ignore_errors=True)

    lang_path = ASSETS / "lang/en_us.json"
    lang = {k: v for k, v in json.loads(lang_path.read_text(encoding="utf-8")).items()
            if not (k.startswith("block.fundamentals.") and k.endswith("_ore"))}
    lang["itemGroup.fundamentals.minerals"] = "Fundamentals: Minerals"

    by_tool, by_tier, by_commodity, by_deposit = {}, {}, {}, {}
    for name, (commodity, deposit, y_min, y_max, spread, per_chunk, size, replaces, tool, tier) in ORES.items():
        block = f"fundamentals:{name}_ore"
        write(ASSETS / f"blockstates/{name}_ore.json", {"variants": {"": {"model": f"fundamentals:block/{name}_ore"}}})
        write(ASSETS / f"models/block/{name}_ore.json",
              {"parent": "minecraft:block/cube_all", "textures": {"all": f"fundamentals:block/{name}_ore"}})
        write(ASSETS / f"models/item/{name}_ore.json", {"parent": f"fundamentals:block/{name}_ore"})
        lang[f"block.fundamentals.{name}_ore"] = DISPLAY.get(name, name.replace("_", " ").title())
        write(DATA / f"loot_table/blocks/{name}_ore.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0,
                       "entries": [{"type": "minecraft:item", "name": block}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        by_tool.setdefault(tool, []).append(block)
        if tier:
            by_tier.setdefault(tier, []).append(block)
        by_commodity.setdefault(commodity, []).append(block)
        by_deposit.setdefault(deposit, []).append(f"fundamentals:ore_{name}")

        write(DATA / f"worldgen/configured_feature/ore_{name}.json", {
            "type": "minecraft:ore",
            "config": {"discard_chance_on_air_exposure": 0.0, "size": size,
                       "targets": [{"state": {"Name": block},
                                    "target": {"predicate_type": "minecraft:tag_match", "tag": t}}
                                   for t in replaces]}})
        frequency = {"type": "minecraft:count", "count": per_chunk} if per_chunk >= 1 \
            else {"type": "minecraft:rarity_filter", "chance": round(1 / per_chunk)}
        write(DATA / f"worldgen/placed_feature/ore_{name}.json", {
            "feature": f"fundamentals:ore_{name}",
            "placement": [frequency, {"type": "minecraft:in_square"},
                          {"type": "minecraft:height_range",
                           "height": {"type": f"minecraft:{spread}",
                                      "min_inclusive": {"absolute": y_min},
                                      "max_inclusive": {"absolute": y_max}}},
                          {"type": "minecraft:biome"}]})

    write(lang_path, dict(sorted(lang.items())))

    for tool, blocks in by_tool.items():
        tag(MC_TAGS / f"mineable/{tool}.json", blocks)
    for tier, blocks in by_tier.items():
        tag(MC_TAGS / f"needs_{tier}_tool.json", blocks)
    for kind in ("block", "item"):
        tag(C_TAGS / f"{kind}/ores.json", [f"#c:ores/{c}" for c in by_commodity])
        for commodity, blocks in by_commodity.items():
            tag(C_TAGS / f"{kind}/ores/{commodity}.json", blocks)

    spawns = []
    for deposit, features in by_deposit.items():
        tag(DATA / f"tags/worldgen/biome/deposit/{deposit}.json", DEPOSITS[deposit])
        biomes = f"#fundamentals:deposit/{deposit}"
        write(DATA / f"neoforge/biome_modifier/add_{deposit}_ores.json", {
            "type": "neoforge:add_features", "biomes": biomes, "features": features,
            "step": "underground_ores"})
        spawns.append({"biomes": f"fundamentals:deposit/{deposit}", "features": features})
    write(DATA / "neoforge/biome_modifier/remove_vanilla_iron.json", {
        "type": "neoforge:remove_features", "biomes": "#minecraft:is_overworld", "features": REMOVED,
        "steps": ["underground_ores"]})
    # NeoForge reads the biome modifiers above; Fabric has no data-driven equivalent, so
    # worldgen.OreSpawns applies the same add/remove list through the Fabric biome API.
    # The same file is the list of ore blocks registry.OreBlocks registers, so a block exists
    # exactly when its models, loot, tags and spawn rules do.
    write(ROOT / "fundamentals_ores.json", {
        "ores": [{"mineral": name, "soft": row[8] == "shovel"} for name, row in ORES.items()],
        "add": spawns, "remove": REMOVED})
    print(f"wrote data for {len(ORES)} ores across {len(by_deposit)} deposit types")


if __name__ == "__main__":
    main()
