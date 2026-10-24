#!/usr/bin/env python3
import json
import shutil
import zipfile
from pathlib import Path

import common
from common import ASSETS, DATA, write
from build_ore_data import SILK_TOUCH, silk_or_raw

CN = DATA.parent / "createnuclear"
OURS_DIR = DATA / "recipe/createnuclear"
CN_JAR = next(Path.home().glob(".gradle/caches/modules-2/files-2.1/maven.modrinth/createnuclear/*/*/createnuclear-*.jar"))
LOADED = [{"type": "neoforge:mod_loaded", "modid": "createnuclear"}]
OFF = {"neoforge:conditions": [{"type": "neoforge:false"}]}

# Its generic ores, and the mineral each really is.
ORES = {"uranium_ore": "uraninite", "lead_ore": "galena", "thorium_ore": "monazite"}
RAW = {"raw_uranium": "uraninite", "raw_lead": "galena", "raw_thorium": "monazite"}
OWN_ORE_ITEMS = ({f"createnuclear:{host}{ore}" for ore in [*ORES, "nitrate_ore"] for host in ("", "deepslate_")}
                 | {f"createnuclear:{raw}" for raw in RAW} | {f"createnuclear:{raw}_block" for raw in RAW}
                 | {"createnuclear:nitrate", "createnuclear:uranium_powder", "createnuclear:thorium_dust",
                    "create:crushed_raw_uranium", "create:crushed_raw_lead"})
ORE_TAGS = {"c:ores/uranium", "c:ores/lead", "c:ores/thorium", "c:ores/nitrate", "c:raw_materials/uranium", "c:raw_materials/lead",
            "c:raw_materials/thorium", "c:raw_storage_blocks/uranium", "c:raw_storage_blocks/lead", "c:raw_storage_blocks/thorium",
            "c:dusts/uranium", "c:dusts/thorium"}

SWITCHED_OFF = {
    "crushing/granite": "granite crushed to uranium powder half the time; uranium is in uraninite",
    "crushing/nitrate": "limestone crushed to saltpetre and lead; neither is in limestone",
    "crushing/raw_copper": "a lead nugget from native copper, which carries none; Create's own crushing stands",
    "crushing/raw_zinc": "the same input as Create's own zinc crushing",
    "mixing/steel": "steel from iron and coal dust stirred cold; steel is The Factory's, from its blast furnace",
    "mixing/uranium_fluid": "liquid uranium from stirred powder; the metal melts at 1,132 °C, and fuel is ceramic oxide",
    "mixing/thorium_fluid": "the same, for thorium",
    "compacting/uranium_fluid_to_yellowcake": "yellowcake is precipitated from the leach liquor with ammonia",
    "compacting/thorium_fluid_to_thorium_ingot": "liquid thorium is not made",
    "enriched/enriched_yellowcake": "uranium is enriched as hexafluoride in a gas centrifuge, not as yellowcake before a fire",
    "pressing/graphene": "graphene is not pressed out of coal dust, and the control rod no longer takes it",
    "snow_powder/cooled_nitrogen_concentrate": "liquid nitrogen is distilled out of liquefied air",
    "blasting/nitrogen_concentrate_for_nitrogen_concentrate": "the same",
    "smelting/nitrogen_concentrate_for_nitrogen_concentrate": "the same",
}
# The Factory's lead and steel, and the packing it already does, serve.
PACKING = ("crafting/crafting/lead_block_from_compacting", "crafting/crafting/lead_ingot_from_compacting", "crafting/crafting/lead_ingot_from_decompacting",
           "crafting/crafting/lead_nugget_from_decompacting", "crafting/lead_ingot_from_decompacting", "crafting/lead_nugget_from_decompacting",
           "crafting/crafting/steel_block_from_compacting", "crafting/crafting/steel_ingot_from_compacting", "crafting/crafting/steel_ingot_from_decompacting",
           "crafting/crafting/steel_nugget_from_decompacting", "crafting/steel_ingot_from_decompacting", "crafting/steel_nugget_from_decompacting")

ROD = {"type": "create:mechanical_crafting", "accept_mirrored": True, "category": "misc", "show_notification": True}
TAKEN_OVER = {
    "mechanical_crafting/uranium_rod": {**ROD, "pattern": ["P", "P", "T", "P", "P"],
                                        "key": {"P": {"item": "fundamentals:uranium_fuel_pellet"}, "T": {"item": "fundamentals:zircaloy_tube"}},
                                        "result": {"id": "createnuclear:uranium_rod", "count": 1}},
    "mechanical_crafting/thorium_rod": {**ROD, "pattern": ["H", "U", "T", "U", "H"],
                                        "key": {"H": {"item": "fundamentals:thorium_dioxide"}, "U": {"item": "fundamentals:uranium_fuel_pellet"},
                                                "T": {"item": "fundamentals:zircaloy_tube"}},
                                        "result": {"id": "createnuclear:thorium_rod", "count": 1}},
    "mechanical_crafting/graphite_rod": {**ROD, "pattern": ["H", "H", "H", "H"], "key": {"H": {"tag": "c:ingots/hafnium"}},
                                         "result": {"id": "createnuclear:graphite_rod", "count": 1}},
    "mixing/liquid_nitrogen": {"type": "create:compacting", "ingredients": [{"item": "minecraft:packed_ice"},
                                                                           {"type": "neoforge:single", "amount": 1000, "fluid": "tfmg:air"}],
                               "results": [{"id": "createnuclear:nitrogen", "amount": 100}]},
}
OURS = {
    # Thoria reduced with calcium under argon, Marden's route to the metal.
    "thorium_ingot": {"type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"], "machines": ["tfmg:mixing"],
                      "min_size": 1, "processing_time": 100, "heat_requirement": "superheated",
                      "ingredients": [{"item": "fundamentals:thorium_dioxide"}, {"item": "fundamentals:calcium_ingot"}, {"item": "fundamentals:calcium_ingot"},
                                      {"type": "neoforge:tag", "amount": 250, "tag": "c:argon"}],
                      "results": [{"id": "createnuclear:thorium_ingot"}]},
    "control_rod_from_boron_carbide": {**ROD, "pattern": ["SBS", "SBS", "SBS", "SBS"],
                                                           "key": {"B": {"item": "fundamentals:boron_carbide"}, "S": {"tag": "c:plates/stainless_steel"}},
                                                           "result": {"id": "createnuclear:graphite_rod", "count": 2}},
}
# The core's centre takes a beryllium reflector where it took a bucket of liquid uranium.
CORE = ("mechanical_crafting/reactor_core", "B", {"tag": "c:ingots/beryllium"})
NAMES = {"item.createnuclear.graphite_rod": "Control Rod"}


def takes(node):
    if isinstance(node, list):
        return [i for each in node for i in takes(each)]
    if not isinstance(node, dict):
        return []
    own = [node["item"]] if set(node) == {"item"} else [f"#{node['tag']}"] if set(node) == {"tag"} else []
    return own + [i for key, value in node.items() if key not in ("results", "result") for i in takes(value)]


def raw_materials():
    return {metal: [f"createnuclear:raw_{metal}"] for metal in ("uranium", "lead", "thorium")}


def worldgen(jar):
    for name in ("uranium_ore", "lead_ore", "thorium_ore", "nitrate_ore"):
        write(CN / f"neoforge/biome_modifier/{name}.json", {"neoforge:conditions": LOADED, "type": "neoforge:none"})
    feature = json.loads(jar.read("data/createnuclear/worldgen/configured_feature/striated_ores_overworld.json"))
    placement = json.loads(jar.read("data/createnuclear/worldgen/placed_feature/striated_ores_overworld.json"))
    patterns = feature["config"]["layer_patterns"]
    for pattern in patterns:
        pattern[:] = [layer for layer in pattern if not any(t["state"]["Name"].endswith("uranium_ore") for alts in layer["targets"] for t in alts)]
    assert all(patterns), "Create Nuclear's striated layers changed; re-check which carry uranium ore"
    write(CN / "worldgen/configured_feature/striated_ores_overworld.json", {"neoforge:conditions": LOADED, **feature})
    write(CN / "worldgen/placed_feature/striated_ores_overworld.json", {"neoforge:conditions": LOADED, **placement})


def drops():
    for ore, mineral in ORES.items():
        for host in ("", "deepslate_"):
            block = f"{host}{ore}"
            write(CN / f"loot_table/blocks/{block}.json", {"neoforge:conditions": LOADED, "type": "minecraft:block", "pools": [
                {"rolls": 1, "bonus_rolls": 0, "entries": [silk_or_raw(f"createnuclear:{block}", mineral)]}]})
    for host in ("", "deepslate_"):
        write(CN / f"loot_table/blocks/{host}nitrate_ore.json", {"neoforge:conditions": LOADED, "type": "minecraft:block", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"createnuclear:{host}nitrate_ore", "conditions": [SILK_TOUCH]},
                {"type": "minecraft:item", "name": "tfmg:nitrate_dust", "functions": [{"function": "minecraft:set_count", "count": 2},
                                                                                      {"function": "minecraft:explosion_decay"}]}]}]}]})
    for raw, mineral in RAW.items():
        for suffix, count in (("", 1), ("_block", 9)):
            write(OURS_DIR / f"raw_{mineral}_from_{raw}{suffix}.json", {
                "neoforge:conditions": LOADED, "type": "minecraft:crafting_shapeless", "category": "misc",
                "ingredients": [{"item": f"createnuclear:{raw}{suffix}"}], "result": {"id": f"fundamentals:raw_{mineral}", "count": count}})


def main():
    for stale in (CN, OURS_DIR, ASSETS.parent / "createnuclear"):
        shutil.rmtree(stale, ignore_errors=True)
    off = rewritten = 0
    with zipfile.ZipFile(CN_JAR) as jar:
        worldgen(jar)
        for entry in sorted(jar.namelist()):
            if not (entry.startswith("data/createnuclear/recipe/") and entry.endswith(".json")):
                continue
            path = entry[len("data/createnuclear/recipe/"):-len(".json")]
            original = json.loads(jar.read(entry))
            inputs = set(takes(original))
            if path in TAKEN_OVER:
                write(CN / f"recipe/{path}.json", {"neoforge:conditions": LOADED, **TAKEN_OVER[path]})
                rewritten += 1
            elif path == CORE[0]:
                original["key"][CORE[1]] = CORE[2]
                write(CN / f"recipe/{path}.json", {"neoforge:conditions": LOADED, **original})
                rewritten += 1
            elif path in SWITCHED_OFF or path in PACKING or inputs & OWN_ORE_ITEMS or {t[1:] for t in inputs if t.startswith("#")} & ORE_TAGS:
                write(CN / f"recipe/{path}.json", OFF)
                off += 1
        assert set(TAKEN_OVER) | set(SWITCHED_OFF) | set(PACKING) | {CORE[0]} <= {e[len("data/createnuclear/recipe/"):-5] for e in jar.namelist()
                                                                                  if e.startswith("data/createnuclear/recipe/")}, \
            "a recipe this script takes over is gone from Create Nuclear's jar"
    for name, recipe in OURS.items():
        write(OURS_DIR / f"{name}.json", {"neoforge:conditions": LOADED, **recipe})
    drops()
    write(ASSETS.parent / "createnuclear/lang/en_us.json", NAMES)
    # its ores no longer form and our metals and uranium chain stand in for its own
    duplicates = {f"createnuclear:{name}" for name in ("lead_ingot", "lead_nugget", "lead_block", "steel_ingot", "steel_nugget", "steel_block",
                                                       "yellowcake", "enriched_yellowcake", "uranium_powder", "thorium_dust")}
    with zipfile.ZipFile(CN_JAR) as jar:
        hidden = common.hide_dead(jar, "createnuclear", lambda i: i.endswith("_ore") or ":raw_" in i or i in duplicates)
    print(f"Create Nuclear: {rewritten} recipes taken over, {len(OURS)} added, {off} switched off, {hidden} dead items hidden")


if __name__ == "__main__":
    main()
