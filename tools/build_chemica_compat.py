#!/usr/bin/env python3
import re
import json
import shutil
import zipfile
from pathlib import Path

from common import DATA, fluid, ns, write
from paint_separation import CREATE_JAR

C_TAGS = DATA.parent / "c/tags"
CHEMICA = DATA.parent / "chemica/recipe"
CRUSHING = DATA.parent / "create/recipe/crushing"
CHEMICA_JAR = next(Path.home().glob(".gradle/caches/modules-2/files-2.1/maven.modrinth/chemica/*/*/chemica-*.jar"))
LOADED = [{"type": "neoforge:mod_loaded", "modid": "chemica"}]

SHARED = {
    "hydrochloric_acid": ("acids/hydrochloric", "hydrochloric_acid"),
    "nitric_acid": ("acids/nitric", "nitric_acid"),
    "hydrofluoric_acid": ("acids/hydrofluoric", "hydrofluoric_acid"),
    "phosphoric_acid": ("acids/phosphoric", "phosphoric_acid"),
    "chlorine": ("chlorine", "chlorine"),
    "ammonia": ("ammonia", "ammonia"),
    "argon": ("argon", "argon"),
    "salt_brine": ("brine", "brine"),
    "caustic_soda": ("caustic_soda", "caustic_soda"),
    "titanium_tetrachloride": ("titanium_tetrachloride", "titanium_tetrachloride"),
    "vinyl_chloride": ("vinyl_chloride", "vinyl_chloride_monomer"),
}
ITEMS = {"salt": ("salt", "dusts/salt"), "soda_ash": ("soda_ash", "dusts/soda_ash")}
SHEETS = {"pvc": "fundamentals:pvc_sheet", "polyethylene": "tfmg:plastic_sheet"}
CHEMICA_SHEETS = ["chemica:polyethylene_sheet", "chemica:polyvinyl_chloride_sheet", "chemica:polytetrafluoroethylene_sheet"]

SWITCHED_OFF = {
    "mixing/salt": "salt boiled out of fresh water; it comes from seawater or halite",
    "mixing/brine": "our brine, from the same salt and water",
    "hot_blast/nitrogen": "ammonia from nitrogen and hydrogen in a hot blast stove, with no catalyst; Haber's wants iron, as ours has",
    "vat_machine_recipe/vat_reaction/polyethylene_air": "oxygen poisons a Ziegler-Natta catalyst; The Factory's plastic vats make polyethylene over ours",
    "vat_machine_recipe/vat_reaction/polyethylene_oxygen": "oxygen poisons a Ziegler-Natta catalyst; The Factory's plastic vats make polyethylene over ours",
    "vat_machine_recipe/vat_reaction/polyvinyl_chloride": "vinyl chloride polymerised over a copper catalyst; it is polymerised in suspension, as ours is",
    "vat_machine_recipe/vat_reaction/hydrofluoric_acid": "ours, from fluorspar and sulfuric acid",
    "vat_machine_recipe/vat_reaction/nitrous_oxide_alt": "coal, fluorite and nitric acid do not make laughing gas; ammonium nitrate does",
    "vat_machine_recipe/vat_reaction/titanium_tetrachloride": "rutile chlorinated without carbon; ours carbochlorinates with coke",
    "vat_machine_recipe/vat_reaction/titanium_tetrachloride_dust": "rutile chlorinated without carbon; ours carbochlorinates with coke",
    "vat_machine_recipe/vat_reaction/titanium_tetrachloride_hg": "rutile chlorinated without carbon; ours carbochlorinates with coke",
    "vat_machine_recipe/vat_reaction/titanium_tetrachloride_mg": "rutile chlorinated without carbon; ours carbochlorinates with coke",
    "casting/polyethylene_sheet": "its polyethylene melt is no longer made; The Factory's plastic sheet serves",
    "casting/polyvinyl_chloride": "its PVC melt is no longer made; our PVC sheet serves",
}
ORE_ITEM = re.compile(r"chemica:((deepslate_)?[a-z]+_ore|raw_[a-z_]+|crushed_raw_[a-z]+|crushed_graphite|([a-z]+_grade_)?rutile_(crystal|dust)"
                      r"|fervorite|zelosite|antimony_(ingot|nugget|dust))")
METALS = ("tin", "silver", "platinum", "cobalt", "chromium", "tungsten", "molybdenum", "titanium", "magnesium", "iridium")
INPUTS = {"chemica:tungsten_dust": {"tag": "c:ingots/tungsten"}, "chemica:phosphorus_dust": {"item": "fundamentals:white_phosphorus"}}
RECIPE_INPUTS = {"vat_machine_recipe/mixing/chromic_acid": {"chemica:chromium_dust": {"item": "fundamentals:sodium_dichromate"}}}
MISNAMED = {"chemica:sulfur_dust": "tfmg:sulfur_dust"}
NATIVE_COPPER = {"chemica:arsenic_dust": "fundamentals:silver_nugget"}
# Chemica's vat recipes spell some of these in camelCase, which The Factory's vat ignores
SNAKE = {"allowedVatTypes": "allowed_vat_types", "heatRequirement": "heat_requirement", "minSize": "min_size", "processingTime": "processing_time"}
OURS = DATA / "recipe/chemica"
ARC = {"type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:firebrick_lined_vat"], "min_size": 1, "processing_time": 200,
       "machines": ["tfmg:graphite_electrode", "tfmg:graphite_electrode", "tfmg:graphite_electrode"]}


def fluid_ingredient(id, amount):
    namespace, name = ns(id).split(":")
    if namespace == "fundamentals" and name in SHARED:
        return {"type": "neoforge:tag", "amount": amount, "tag": f"c:{SHARED[name][0]}"}
    return fluid(id, amount)


def optional(ids):
    return [{"id": id, "required": False} for id in ids]


def rewrite(node, inputs, outputs):
    if isinstance(node, list):
        return [rewrite(each, inputs, outputs) for each in node]
    if not isinstance(node, dict):
        return node
    if node.get("fluid") in inputs and "amount" in node:
        return {"type": "neoforge:tag", "amount": node["amount"], "tag": inputs[node["fluid"]]["tag"]}
    if set(node) == {"item"} and node["item"] in inputs:
        return inputs[node["item"]]
    out = {key: rewrite(value, inputs, outputs) for key, value in node.items()}
    for key in ("id", "fluid"):
        if out.get(key) in outputs:
            out[key] = outputs[out[key]]
    return out


def takes(node):
    if isinstance(node, list):
        return [i for each in node for i in takes(each)]
    if not isinstance(node, dict):
        return []
    return ([node["item"]] if set(node) == {"item"} else []) + [i for key, value in node.items() if key not in ("results", "result") for i in takes(value)]


def take_over(path, recipe):
    if path == "vat_machine_recipe/electrolysis/salt":
        recipe["ingredients"] = [i for i in recipe["ingredients"] if i.get("fluid") != "minecraft:water"]
        recipe["results"] = [r for r in recipe["results"] if r["id"] != "minecraft:water"]
    return recipe


# Chemica's own takeovers of Create's ore crushing are in 1.20 Forge's format and fail to load, leaving copper and gold ore uncrushable.
def crushing(chemica, create):
    written = 0
    for entry in sorted(chemica.namelist()):
        if not (entry.startswith("data/create/recipe/crushing/") and entry.endswith(".json")):
            continue
        original = json.loads(create.read(entry))
        named = MISNAMED | (NATIVE_COPPER if "copper" in entry else {})
        extra = [{**{k: v for k, v in r.items() if k != "item"}, "id": named.get(r["item"], r["item"])}
                 for r in json.loads(chemica.read(entry))["results"] if r["item"].split(":")[0] not in ("minecraft", "create")]
        write(CRUSHING / Path(entry).name, {"neoforge:conditions": LOADED + original.pop("neoforge:conditions", []),
                                            **original, "results": original["results"] + extra})
        written += 1
    return written


def raw_materials():
    with zipfile.ZipFile(CHEMICA_JAR) as jar:
        tags = {Path(e).stem: json.loads(jar.read(e))["values"] for e in jar.namelist()
                if e.startswith("data/c/tags/item/raw_materials/") and e.endswith(".json")}
    return {metal: [v for v in values if ORE_ITEM.fullmatch(v)] for metal, values in tags.items()}


def mixing(name, ingredients, results, heat):
    write(OURS / f"{name}.json", {"neoforge:conditions": LOADED, "type": "create:mixing", "heat_requirement": heat,
                                  "ingredients": ingredients, "results": results})


def missing_metals():
    mixing("vanadium_dust", [{"item": "fundamentals:raw_magnetite"}] * 4 + [{"tag": "c:dusts/soda_ash"}, fluid_ingredient("minecraft:water", 500)],
           [{"id": "chemica:vanadium_dust"}, {"id": "create:crushed_raw_iron", "count": 3}], "heated")
    mixing("vanadium_ingot", [{"item": "chemica:vanadium_dust"}] * 2 + [{"item": "fundamentals:aluminium_powder"}],
           [{"id": "chemica:vanadium_ingot"}, {"id": "fundamentals:alumina", "chance": 0.5}], "superheated")
    mixing("tantalum_dust", [{"tag": "c:dusts/euxenite"}] * 2 + [fluid_ingredient("hydrofluoric_acid", 500)],
           [{"id": "chemica:tantalum_dust", "chance": 0.25}], "heated")
    mixing("tantalum_ingot", [{"item": "chemica:tantalum_dust"}, {"item": "chemica:sodium_ingot"}, fluid_ingredient("argon", 100)],
           [{"id": "chemica:tantalum_ingot"}], "superheated")
    write(OURS / "graphite.json", {"neoforge:conditions": LOADED, **ARC, "ingredients": [{"item": "tfmg:coal_coke"}] * 2,
                                   "results": [{"id": "chemica:purified_graphite_flakes", "count": 2}]})


def main():
    from build_separation_data import ACIDS

    for stale in (CHEMICA.parent, C_TAGS / "fluid", DATA / "tags/fluid", OURS):
        shutil.rmtree(stale, ignore_errors=True)
    for name, (tag, theirs) in SHARED.items():
        ours = [f"fundamentals:{name}"] + ([f"fundamentals:{name}_flowing"] if name in ACIDS else [])
        write(C_TAGS / f"fluid/{tag}.json", {"replace": False, "values": ours + optional([f"chemica:{theirs}", f"chemica:flowing_{theirs}"])})
        write(DATA / f"tags/fluid/{name}.json", {"replace": False, "values": [f"#c:{tag}"]})
    for ours, (_, tag) in ITEMS.items():
        write(C_TAGS / f"item/{tag}.json", {"replace": False, "values": [f"fundamentals:{ours}"]})
    for plastic, sheet in SHEETS.items():
        write(C_TAGS / f"item/plates/{plastic}.json", {"replace": False, "values": [sheet]})
    for modifier in ("add_ores", "add_striated_ores_overworld", "add_striated_ores_nether"):
        write(CHEMICA.parent / f"neoforge/biome_modifier/{modifier}.json", {"neoforge:conditions": LOADED, "type": "neoforge:none"})

    inputs, outputs = dict(INPUTS), {}
    for name, (tag, theirs) in SHARED.items():
        for id in (f"chemica:{theirs}", f"chemica:flowing_{theirs}"):
            inputs[id] = {"tag": f"c:{tag}"}
            outputs[id] = f"fundamentals:{name}"
    for ours, (theirs, tag) in ITEMS.items():
        inputs[f"chemica:{theirs}"] = {"tag": f"c:{tag}"}
        outputs[f"chemica:{theirs}"] = f"fundamentals:{ours}"
    for plastic, theirs in (("pvc", "polyvinyl_chloride"), ("polyethylene", "polyethylene")):
        inputs[f"chemica:{theirs}_sheet"] = {"tag": f"c:plates/{plastic}"}
    ours_too = set()
    for metal in METALS:
        inputs[f"chemica:{metal}_ingot"] = {"tag": f"c:ingots/{metal}"}
        outputs[f"chemica:{metal}_ingot"] = f"fundamentals:{metal}_ingot"
        for form, folder in (("nugget", "nuggets"), ("sheet", "plates")):
            if (C_TAGS / f"item/{folder}/{metal}.json").exists():
                inputs[f"chemica:{metal}_{form}"] = {"tag": f"c:{folder}/{metal}"}
                ours_too.add(f"chemica:{metal}_{form}")
                if form == "nugget":
                    outputs[f"chemica:{metal}_nugget"] = f"fundamentals:{metal}_nugget"

    rewritten, off = 0, len(SWITCHED_OFF)
    with zipfile.ZipFile(CHEMICA_JAR) as jar, zipfile.ZipFile(CREATE_JAR) as create:
        repaired = crushing(jar, create)
        for entry in sorted(jar.namelist()):
            if not (entry.startswith("data/chemica/recipe/") and entry.endswith(".json")):
                continue
            path = entry[len("data/chemica/recipe/"):-len(".json")]
            original = json.loads(jar.read(entry))
            made = {r.get("id", r.get("item")) for r in original.get("results", []) + [original.get("result") or {}]}
            ours_already = (made & ours_too and path.startswith(("crafting/", "pressing/"))) or path.endswith("_ingot_crafting_from_nugget") and \
                f"chemica:{path.split('/')[-1].split('_')[0]}_nugget" in ours_too
            if path in SWITCHED_OFF or ours_already or any(ORE_ITEM.fullmatch(i) for i in takes(original)):
                write(CHEMICA / f"{path}.json", {"neoforge:conditions": [{"type": "neoforge:false"}]})
                off += path not in SWITCHED_OFF
                continue
            recipe = rewrite(take_over(path, json.loads(jar.read(entry))), inputs | RECIPE_INPUTS.get(path, {}), outputs)
            if recipe == original:
                continue
            recipe = {SNAKE.get(key, key): value for key, value in recipe.items()}
            write(CHEMICA / f"{path}.json", {"neoforge:conditions": LOADED, **recipe})
            rewritten += 1
    missing_metals()
    print(f"wrote {len(SHARED)} shared reagents, {rewritten} of Chemica's recipes rewritten, {off} switched off, {repaired} crushing repaired")


if __name__ == "__main__":
    main()
