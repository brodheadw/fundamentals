#!/usr/bin/env python3
import shutil

from common import ASSETS, DATA, RESOURCES, cube, drop_self, ns, read_lang, tag, write, write_lang
from paint_materials import items
from paint_minerals import GRADES, ROCK_BLOCKS, VARIANTS, WHOLE
from paint_oxidation import BLOCKS as OXIDATION_BLOCKS

MC_TAGS = RESOURCES / "data/minecraft/tags/block"
C_TAGS = RESOURCES / "data/c/tags"

BIOMES = {
    "anywhere": ["#minecraft:is_overworld"],
    "porphyry": ["#minecraft:is_mountain", "#minecraft:is_hill"],
    "arid_oxide": ["#minecraft:is_badlands", "#minecraft:is_savanna", "minecraft:desert"],
    "laterite": ["#minecraft:is_jungle", "#minecraft:is_savanna", "minecraft:mangrove_swamp"],
    "pegmatite": ["#minecraft:is_mountain", "#minecraft:is_hill", "#minecraft:is_badlands", "#minecraft:is_taiga",
                  "minecraft:savanna_plateau", "minecraft:windswept_savanna"],
    "carbonatite": ["#minecraft:is_mountain", "#minecraft:is_badlands"],
    "alkaline": ["#minecraft:is_taiga", "minecraft:snowy_plains", "minecraft:grove", "minecraft:snowy_slopes"],
    "ion_clay": ["#minecraft:is_jungle"],
    "placer": ["#minecraft:is_beach", "#minecraft:is_river"],
    "wetland": ["minecraft:swamp", "minecraft:mangrove_swamp"],
    "hydrothermal": ["#minecraft:is_mountain", "#minecraft:is_hill", "#minecraft:is_badlands"],
    "temperate": ["#minecraft:is_forest", "#minecraft:is_hill", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow",
                  "minecraft:swamp"],
    "alpine": ["#minecraft:is_mountain"],
}

ORES = {
    "hematite": ("iron", "pickaxe", "stone"),
    "magnetite": ("iron", "pickaxe", "stone"),
    "goethite": ("iron", "pickaxe", None),
    "siderite": ("iron", "pickaxe", "stone"),
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
    "monazite": ("rare_earth", ("shovel", "pickaxe"), None),
    "xenotime": ("rare_earth", "pickaxe", "iron"),
    "ion_adsorption_clay": ("rare_earth", "shovel", None),
    "loparite": ("rare_earth", "pickaxe", "iron"),
    "euxenite": ("rare_earth", "pickaxe", "iron"),
    "thortveitite": ("rare_earth", "pickaxe", "iron"),
    "native_silver": ("silver", "pickaxe", "iron"),
    "argentite": ("silver", "pickaxe", "iron"),
    "sperrylite": ("platinum", "pickaxe", "iron"),
    "cooperite": ("platinum", "pickaxe", "iron"),
    "braggite": ("platinum", "pickaxe", "iron"),
    "cinnabar": ("mercury", "pickaxe", "iron"),
    "spodumene": ("lithium", "pickaxe", "iron"),
    "fluorite": ("fluorspar", "pickaxe", "stone"),
    "borax": ("boron", "pickaxe", "stone"),
    "trona": ("soda_ash", "pickaxe", "stone"),
    "halite": ("salt", "pickaxe", None),
    "zircon": ("zirconium", ("shovel", "pickaxe"), None),
    "beryl": ("beryllium", "pickaxe", "iron"),
    "bertrandite": ("beryllium", "pickaxe", "stone"),
}

ROCKS = {name: "shovel" if name == "laterite" else "pickaxe" for name in ROCK_BLOCKS}

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
    "quartz": ("minecraft:quartz_block", "minecraft:block/quartz_block_side"),
}

ROCK = ["#minecraft:stone_ore_replaceables", "#minecraft:deepslate_ore_replaceables", "minecraft:calcite",
        "minecraft:dripstone_block"] + [block for block, _ in HOSTS.values() if block.startswith("create:")]
REPLACEABLE = {
    "rock": ROCK,
    "ground": ROCK + ["#minecraft:dirt", "minecraft:gravel", "minecraft:clay", "minecraft:mud"],
}

DEPOSITS = {
    "hematite_bed": ("bed", "create:crimsite", [("magnetite", 0.06, "seams"), ("hematite", 0.42, "pockets")],
                     (9, 14), (3, 6), None, "anywhere", (0, 96), 5, "rock"),
    "magnetite_bed": ("bed", None, [("hematite", 0.06, "seams"), ("magnetite", 0.42, "pockets")],
                      (8, 12), (3, 5), None, "anywhere", (-56, 8), 12, "rock"),
    "bog_iron": ("blanket", "goethite_ore", [], (6, 9), (1, 2), None, "wetland", (60, 64), 3, "ground"),
    "coal_measures": ("bed", None, [("siderite", 0.18, "seams"), ("minecraft:coal_ore", 0.22, "seams"), ("siderite", 0.14, "pockets")],
                      (9, 13), (4, 6), None, "temperate", (16, 72), 10, "rock"),
    "sparry_iron": ("bed", "create:limestone", [("siderite", 0.40, "pockets")], (8, 12), (4, 7), None,
                    "alpine", (48, 120), 18, "rock"),
    "lead_zinc_bed": ("bed", "create:limestone", [("sphalerite", 0.20, "pockets"), ("galena", 0.13, "pockets"), ("fluorite", 0.10, "pockets")],
                      (9, 13), (3, 5), None, "anywhere", (-40, 36), 10, "rock"),
    "evaporite_bed": ("bed", "minecraft:sandstone", [("halite", 0.20, "seams"), ("borax", 0.18, "seams"), ("trona", 0.14, "seams")], (8, 12), (2, 4), None,
                      "arid_oxide", (52, 68), 9, "rock"),
    "zinc_oxide_bed": ("bed", "create:asurine", [("smithsonite", 0.24, "pockets")], (7, 10), (2, 4), None,
                       "anywhere", (36, 72), 18, "rock"),
    "manganese_bed": ("bed", "create:limestone", [("pyrolusite", 0.28, "pockets")], (8, 12), (3, 5), None,
                      "anywhere", (0, 60), 20, "rock"),
    "tungsten_skarn": ("bed", "create:limestone", [("scheelite", 0.25, "pockets")], (6, 9), (3, 4), None,
                       "hydrothermal", (-32, 40), 14, "rock"),
    "mercury_lens": ("bed", None, [("cinnabar", 0.25, "pockets")], (5, 8), (2, 4), None,
                     "hydrothermal", (0, 72), 14, "rock"),
    "porphyry_stock": ("plug", "minecraft:andesite",
                       [("chalcopyrite", 0.18, "pockets"), ("molybdenite", 0.01, "pockets"),
                        ("bornite", 0.04, "disseminated"), ("chalcocite", 0.07, "top"), ("covellite", 0.02, "top")],
                       (7, 11), None, (24, 40), "porphyry", (0, 70), 5, "rock"),
    "oxide_cap": ("blanket", "create:ochrum",
                  [("malachite", 0.20, "pockets"), ("azurite", 0.10, "pockets"), ("cuprite", 0.06, "disseminated"),
                   ("hemimorphite", 0.06, "pockets")],
                  (9, 14), (5, 8), None, "arid_oxide", (60, 64), 4, "rock"),
    "bauxite_blanket": ("blanket", "laterite", [("bauxite", 0.50, "pockets")], (10, 15), (4, 7), None,
                        "laterite", (60, 64), 5, "ground"),
    "nickel_laterite_blanket": ("blanket", "laterite", [("nickel_laterite", 0.40, "pockets")], (9, 13), (4, 6),
                                None, "laterite", (60, 64), 9, "ground"),
    "ion_clay_blanket": ("blanket", "ion_adsorption_clay_ore", [], (8, 12), (3, 5), None,
                         "ion_clay", (60, 64), 6, "ground"),
    "carbonatite_plug": ("plug", "carbonatite", [("bastnasite", 0.16, "pockets"), ("monazite", 0.05, "top")], (6, 9), None, (20, 34),
                         "carbonatite", (-56, 0), 36, "rock"),
    "syenite_massif": ("plug", "syenite", [("loparite", 0.10, "seams"), ("zircon", 0.02, "disseminated")], (8, 12), None, (16, 26),
                       "alkaline", (-32, 32), 24, "rock"),
    "layered_intrusion": ("bed", "gabbro",
                          [("chromite", 0.12, "seams"), ("pentlandite", 0.05, "pockets"),
                           ("sperrylite", 0.006, "disseminated"), ("cooperite", 0.005, "disseminated"),
                           ("braggite", 0.005, "disseminated")],
                          (12, 15), (8, 12), None, "anywhere", (-60, -28), 80, "rock"),
    "nickel_sulfide": ("bed", "gabbro", [("pentlandite", 0.14, "pockets")], (9, 12), (5, 7), None,
                       "anywhere", (-60, -8), 45, "rock"),
    "pegmatite_dyke": ("vein", "minecraft:granite", [("xenotime", 0.08, "pockets"), ("euxenite", 0.04, "pockets"),
                                                         ("thortveitite", 0.01, "pockets"), ("beryl", 0.04, "pockets")],
                       (11, 15), (3, 5), (14, 24), "pegmatite", (-16, 48), 16, "rock"),
    "monazite_vein": ("vein", "minecraft:quartz_block", [("monazite", 0.25, "pockets")], (9, 13), (2, 3), (12, 18),
                      "pegmatite", (0, 40), 48, "rock"),
    "spodumene_pegmatite": ("vein", "minecraft:granite", [("spodumene", 0.30, "pockets"), ("beryl", 0.02, "pockets")], (10, 14), (3, 5), (12, 20),
                            "pegmatite", (0, 64), 14, "rock"),
    "beryllium_tuff": ("bed", "minecraft:tuff", [("bertrandite", 0.10, "disseminated"), ("fluorite", 0.05, "pockets")], (8, 12), (3, 5), None,
                       "arid_oxide", (40, 70), 30, "rock"),
    "tin_vein": ("vein", None, [("cassiterite", 0.36, "pockets"), ("wolframite", 0.16, "pockets")],
                 (10, 15), (2, 3), (12, 24), "pegmatite", (-16, 56), 6, "rock"),
    "silver_vein": ("vein", "minecraft:calcite", [("argentite", 0.38, "pockets"), ("native_silver", 0.16, "pockets")],
                    (10, 15), (1, 2), (14, 26), "hydrothermal", (-32, 64), 9, "rock"),
    "cobalt_vein": ("vein", "minecraft:calcite", [("cobaltite", 0.45, "pockets")], (8, 12), (1, 2), (12, 20),
                    "hydrothermal", (-32, 32), 20, "rock"),
}

PLACERS = {
    "monazite": (54, 66, 4, 9),
    "ilmenite": (54, 66, 5, 12),
    "rutile": (54, 66, 3, 9),
    "cassiterite": (54, 66, 3, 9),
    "zircon": (54, 66, 4, 9),
}

REMOVED = {
    "vanilla_iron": ["minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_iron_small"],
    "tfmg_lead_ore": ["tfmg:lead_ore"],
    "tfmg_lithium_ore": ["tfmg:lithium_ore"],
    "tfmg_nickel_ore": ["tfmg:nickel_ore"],
}

STORAGE_BLOCKS = [f"fundamentals:{name}" for _, form, name in items() if form == "block"]
PLASTIC_BLOCKS = [f"fundamentals:{dye}_plastic_block" for dye in ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                                                                  "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")]
OTHER_MINEABLE = {"pickaxe": ["fundamentals:bloomery", "fundamentals:panel_rack", "fundamentals:solar_panel", "fundamentals:mixer_settler", "fundamentals:plastic_fluid_tank",
                                "fundamentals:magnetomigration_cell", "fundamentals:dyed_plastic_pipe", "fundamentals:titanium_pipe",
                                "fundamentals:titanium_mechanical_pump", "fundamentals:titanium_fluid_valve", "fundamentals:titanium_fluid_tank",
                                "fundamentals:mercury_thermometer", "fundamentals:spirit_thermometer", "fundamentals:bimetallic_thermometer", "fundamentals:type_k_thermocouple", "fundamentals:type_s_thermocouple"] + STORAGE_BLOCKS + PLASTIC_BLOCKS
                + [f"fundamentals:{name}" for name in OXIDATION_BLOCKS]}
OTHER_TIERED = {"stone": STORAGE_BLOCKS + [f"fundamentals:{name}" for name in OXIDATION_BLOCKS if name != "inert_storage_drum"]}

SHARED = ("aluminum",)

DISPLAY = {"bastnasite": "Bastnäsite", "ion_adsorption_clay": "Ion-Adsorption Clay", "argentite": "Acanthite"}

DROPS = {"core": (2, 3, 1.0), "edge": (1, 1, 1.0), "trace": (1, 1, 0.5)}

SILK_TOUCH = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}


def overlay(texture):
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
        low, high, chance = DROPS[grade]
        count = low if low == high else {"type": "minecraft:uniform", "min": low, "max": high}
        raw = {"type": "minecraft:item", "name": f"fundamentals:raw_{mineral}",
               "functions": [{"function": "minecraft:set_count", "count": count},
                             {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                              "formula": "minecraft:ore_drops"},
                             {"function": "minecraft:explosion_decay"}]}
        if chance < 1:
            raw["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
        pools.append({"rolls": 1, "bonus_rolls": 0,
                      "conditions": [{"condition": "minecraft:block_state_property", "block": f"fundamentals:{name}",
                                      "properties": {"grade": grade}}],
                      "entries": [{"type": "minecraft:alternatives", "children": [
                          {"type": "minecraft:item", "name": f"fundamentals:{name}", "conditions": [SILK_TOUCH]}, raw]}]})
    write(ASSETS / f"blockstates/{name}.json", {"multipart": parts})
    item = cube(f"fundamentals:block/{name}_edge_0") if whole else {
        "parent": "minecraft:block/block",
        "textures": {"particle": "minecraft:block/stone", "base": "minecraft:block/stone",
                     "ore": f"fundamentals:block/{name}_edge_0"},
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                      "faces": {side: {"texture": tex} for side in ("down", "up", "north", "south", "west", "east")}}
                     for tex in ("#base", "#ore")]}
    write(ASSETS / f"models/item/{name}.json", item)
    write(ASSETS / f"models/item/raw_{mineral}.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/raw_{mineral}"}})
    lang[f"block.fundamentals.{name}"] = display if whole else f"{display} Ore"
    lang[f"item.fundamentals.raw_{mineral}"] = f"Raw {display}"
    write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": pools})


def state(block, **properties):
    out = {"Name": ns(block)}
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
    raw_textures = {p.stem for p in (ASSETS / "textures/item").glob("raw_*.png")}
    assert raw_textures == {f"raw_{name}" for name in ORES}, \
        f"raw item textures and ORES disagree: {sorted(raw_textures ^ {f'raw_{name}' for name in ORES})}"
    stale = {name for name in textures if "_ore_" in name} - expected
    assert not (expected - textures) and not stale, \
        f"textures and block tables disagree: missing {sorted(expected - textures)}, stale {sorted(stale)}"
    generated = {ore for _, _, ores, *_ in DEPOSITS.values() for ore, _, _ in ores if ":" not in ore} | set(PLACERS) \
        | {row[1][:-4] for row in DEPOSITS.values() if row[1] and row[1].endswith("_ore")}
    assert generated == set(ORES), f"ores that never generate, or unknown ores: {sorted(generated ^ set(ORES))}"

    for stale in (DATA / "worldgen", DATA / "neoforge", DATA / "tags/worldgen", DATA / "tags/block/ores", DATA / "tags/item/ores",
                  DATA / "tags/item/raw_materials", C_TAGS / "block/ores", C_TAGS / "item/ores", C_TAGS / "item/raw_materials"):
        shutil.rmtree(stale, ignore_errors=True)
    shutil.rmtree(ASSETS / "models/block/ore", ignore_errors=True)

    lang = read_lang()
    lang["itemGroup.fundamentals.minerals"] = "Fundamentals"

    for host, (_, texture) in HOSTS.items():
        write(ASSETS / f"models/block/host/{host}.json", cube(texture))

    blocks, by_tool, by_tier, by_commodity, raw_by_commodity = [], {}, {}, {}, {}
    for name, tool in ROCKS.items():
        rock_files(name, name.replace("_", " ").title(), lang)
        blocks.append({"name": name, "soft": tool == "shovel"})
        by_tool.setdefault(tool, []).append(f"fundamentals:{name}")
    for name, (commodity, tool, tier) in ORES.items():
        block = f"fundamentals:{name}_ore"
        ore_files(name, DISPLAY.get(name, name.replace("_", " ").title()), lang)
        tools = (tool,) if isinstance(tool, str) else tool
        blocks.append({"name": f"{name}_ore", "soft": tools[0] == "shovel", "mineral": name})
        for each in tools:
            by_tool.setdefault(each, []).append(block)
        if tier:
            by_tier.setdefault(tier, []).append(block)
        by_commodity.setdefault(commodity, []).append(block)
        raw_by_commodity.setdefault(commodity, []).append(f"fundamentals:raw_{name}")
    write_lang(lang)

    for tool, names in by_tool.items():
        tag(MC_TAGS / f"mineable/{tool}.json", names + OTHER_MINEABLE.get(tool, []))
    for tier, names in by_tier.items():
        tag(MC_TAGS / f"needs_{tier}_tool.json", names + OTHER_TIERED.get(tier, []))
    for kind in ("block", "item"):
        tag(C_TAGS / f"{kind}/ores.json", [f"#fundamentals:ores/{c}" for c in by_commodity])
        for commodity, names in by_commodity.items():
            tag(DATA / f"tags/{kind}/ores/{commodity}.json", names)
        for commodity in SHARED:
            tag(C_TAGS / f"{kind}/ores/{commodity}.json", [f"#fundamentals:ores/{commodity}"])
    tag(C_TAGS / "item/raw_materials.json", [f"#fundamentals:raw_materials/{c}" for c in raw_by_commodity])
    for commodity, names in raw_by_commodity.items():
        tag(DATA / f"tags/item/raw_materials/{commodity}.json", names)
    for commodity in SHARED:
        tag(C_TAGS / f"item/raw_materials/{commodity}.json", [f"#fundamentals:raw_materials/{commodity}"])
    from build_chemica_compat import optional, raw_materials
    for metal, theirs in raw_materials().items():
        write(C_TAGS / f"item/raw_materials/{metal}.json", {"replace": False, "values": [], "remove": optional(theirs)})
    for key, values in REPLACEABLE.items():
        tag(DATA / f"tags/block/deposit_replaceable/{key}.json", values)

    by_biomes = {}
    for name, (shape, host, ores, radius, thickness, height, where, y, chunks, replaces) in DEPOSITS.items():
        entries = [{"state": state(ore if ":" in ore else f"{ore}_ore"), "fraction": share, "style": style} for ore, share, style in ores]
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
        write(DATA / f"neoforge/biome_modifier/add_{where}_deposits.json", {
            "type": "neoforge:add_features", "biomes": f"#fundamentals:deposit/{where}", "features": features,
            "step": "underground_decoration"})
    for name, features in REMOVED.items():
        write(DATA / f"neoforge/biome_modifier/remove_{name}.json", {
            "type": "neoforge:remove_features", "biomes": "#minecraft:is_overworld", "features": features,
            "steps": ["underground_ores"]})
    write(RESOURCES / "fundamentals_ores.json", {
        "blocks": blocks, "hosts": {host: block for host, (block, _) in HOSTS.items()}})
    print(f"wrote {len(blocks)} blocks ({len(ORES)} minerals, {len(ROCKS)} rocks), "
          f"{len(DEPOSITS)} deposit types, {len(PLACERS)} placers")


if __name__ == "__main__":
    main()
