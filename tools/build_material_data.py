#!/usr/bin/env python3
"""Writes what the material items painted by paint_materials.py need besides their textures:
models, names, tags, the storage blocks' blockstates and loot, the recipes that pack nuggets
into ingots into blocks and press plates, and the rare earth minerals' first steps on Create's
machines, and chromite's. Re-run after any edit.

    python3 tools/paint_materials.py && python3 tools/build_material_data.py && python3 tools/build_ore_data.py

build_ore_data.py comes last because it owns the mining-tool tags the storage blocks go into.
"""
import json
import shutil

from build_ore_data import ASSETS, C_TAGS, DATA, cube, drop_self, tag, write
from paint_materials import DISPLAY, MATERIALS, item_name

RECIPES = DATA / "recipe"

# form: (common tag folder, name of the item for a material called X)
FORMS = {
    "concentrate": (DATA / "tags/item/concentrates", "{}"),
    "oxalate": (DATA / "tags/item/oxalates", "{} Oxalate"),
    "fluoride": (DATA / "tags/item/fluorides", "{} Fluoride"),
    "oxide": (DATA / "tags/item/oxides", "{} Oxide"),
    "dust": (C_TAGS / "item/dusts", "{} Dust"),
    "sponge": (DATA / "tags/item/sponges", "{} Sponge"),
    "ingot": (C_TAGS / "item/ingots", "{} Ingot"),
    "nugget": (C_TAGS / "item/nuggets", "{} Nugget"),
    "plate": (C_TAGS / "item/plates", "{} Plate"),
    "block": (C_TAGS / "item/storage_blocks", "Block of {}"),
}

# Minerals ground in a millstone or crushing wheels. Monazite is a sand already.
GROUND = ("bastnasite", "xenotime", "loparite", "euxenite", "thortveitite", "chromite")

# Gravity concentration, done as a wash under an encased fan: what goes in, and which
# concentrate the heavy grains left behind are. Bastnäsite needs flotation and the clay a leach;
# neither has a machine yet.
WASHED = {
    "raw_monazite": "light_rare_earth_concentrate",
    "loparite_dust": "light_rare_earth_concentrate",
    "xenotime_dust": "heavy_rare_earth_concentrate",
    "euxenite_dust": "heavy_rare_earth_concentrate",
    "chromite_dust": "chromite_concentrate",
    "raw_zircon": "zircon_concentrate",
}


def packed(small, big, name):
    """Nine of `small` make one `big`, and back."""
    write(RECIPES / f"packing/{name}.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["###", "###", "###"],
        "key": {"#": {"tag": small["tag"]}}, "result": {"id": big["id"], "count": 1}})
    write(RECIPES / f"packing/{name}_unpacked.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"tag": big["tag"]}], "result": {"id": small["id"], "count": 9}})


def main():
    for folder, _ in FORMS.values():
        shutil.rmtree(folder, ignore_errors=True)
    for stale in (C_TAGS / "block/storage_blocks", RECIPES / "packing", RECIPES / "pressing", RECIPES / "milling",
                  RECIPES / "crushing", RECIPES / "washing"):
        shutil.rmtree(stale, ignore_errors=True)

    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    lang["itemGroup.fundamentals.materials"] = "Fundamentals Materials"

    tagged = {form: [] for form in FORMS}
    for material, forms in MATERIALS.items():
        display = DISPLAY.get(material, material.replace("_", " ").title())
        made = {}
        for form in forms:
            name = item_name(material, form)
            folder, title = FORMS[form]
            if form == "concentrate" and name != material:
                title = "{} Concentrate"
            made[form] = {"id": f"fundamentals:{name}", "tag": f"{'c' if folder.is_relative_to(C_TAGS) else 'fundamentals'}:{folder.name}/{material}"}
            tag(folder / f"{material}.json", [made[form]["id"]])
            tagged[form].append("#" + made[form]["tag"])
            if form == "block":
                write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"fundamentals:block/{name}"}}})
                write(ASSETS / f"models/block/{name}.json", cube(f"fundamentals:block/{name}"))
                write(ASSETS / f"models/item/{name}.json", {"parent": f"fundamentals:block/{name}"})
                write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [drop_self(name)]})
                tag(C_TAGS / f"block/storage_blocks/{material}.json", [made[form]["id"]])
                lang[f"block.fundamentals.{name}"] = title.format(display)
            else:
                write(ASSETS / f"models/item/{name}.json",
                      {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})
                lang[f"item.fundamentals.{name}"] = title.format(display)
        if "nugget" in made:
            packed(made["nugget"], made["ingot"], f"{material}_ingot")
        if "block" in made:
            # a metal's block packs from its ingot; a residue's from its dust
            packed(made.get("ingot") or made["dust"], made["block"], f"{material}_block")
        if "plate" in made:
            write(RECIPES / f"pressing/{material}_plate.json", {
                "type": "create:pressing", "ingredients": [{"tag": made["ingot"]["tag"]}],
                "results": [{"id": made["plate"]["id"]}]})

    for mineral in GROUND:
        raw, dust = [{"item": f"fundamentals:raw_{mineral}"}], {"id": f"fundamentals:{mineral}_dust"}
        write(RECIPES / f"milling/{mineral}_dust.json", {
            "type": "create:milling", "ingredients": raw, "processing_time": 250, "results": [dust]})
        write(RECIPES / f"crushing/{mineral}_dust.json", {
            "type": "create:crushing", "ingredients": raw, "processing_time": 400,
            "results": [dust, {"chance": 0.25, **dust}]})
    for feed, concentrate in WASHED.items():
        write(RECIPES / f"washing/{feed}.json", {
            "type": "create:splashing", "ingredients": [{"item": f"fundamentals:{feed}"}],
            "results": [{"chance": 0.5, "id": f"fundamentals:{concentrate}"}]})

    for form, (folder, _) in FORMS.items():
        tag(folder.with_suffix(".json"), tagged[form])
    tag(C_TAGS / "block/storage_blocks.json", tagged["block"])
    write(lang_path, dict(sorted(lang.items())))
    print(f"wrote {sum(len(forms) for forms in MATERIALS.values())} material items")


if __name__ == "__main__":
    main()
