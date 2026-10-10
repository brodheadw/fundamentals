#!/usr/bin/env python3
"""Chemica, The Factory Must Grow's chemistry add-on, makes a dozen of our reagents its own way. Fundamentals does not need it, but
with both installed they are one chemical industry, not two: either mod's hydrochloric acid, chlorine or argon goes into the other's
recipes, the mixer-settlers, the inert drum and the pipes that corrode. docs/chemica.md has the overlap and the reasons. Re-run
after any edit; it reads Chemica's recipes out of its jar in the Gradle cache, so build once first.

    python3 tools/build_chemica_compat.py

Each shared reagent gets a c: fluid tag holding both mods' fluid, which our recipes take, and a fundamentals:<reagent> tag pointing
at it, which is how separation.Separation.reagent knows another mod's fluid for ours. Chemica's own recipes that touch a shared
reagent are rewritten under data/chemica/, behind a mod_loaded condition: they take the tag, and they give our fluid, so a base
running both makes one hydrochloric acid. The few of its recipes that contradict ours are taken over or switched off.

Chemica's ores, and the generic metals it wins from them, are the generic ore this mod exists to replace. With Chemica loaded its
ores no longer generate; its recipes take our ingots, nuggets and plates by c: tag and give ours; every recipe that still needs one
of its ores, raw chunks or crushed ores is switched off; and the four metals we do not make (vanadium, tantalum, graphite and
antimony, which only Chemica's dopant took) get a real road from our minerals, under data/fundamentals/recipe/chemica/.
"""
import re
import json
import shutil
import zipfile
from pathlib import Path

from build_ore_data import DATA, write
from paint_separation import CREATE_JAR

C_TAGS = DATA.parent / "c/tags"
CHEMICA = DATA.parent / "chemica/recipe"
CRUSHING = DATA.parent / "create/recipe/crushing"
CHEMICA_JAR = next(Path.home().glob(".gradle/caches/modules-2/files-2.1/maven.modrinth/chemica/*/*/chemica-*.jar"))
LOADED = [{"type": "neoforge:mod_loaded", "modid": "chemica"}]

# our reagent -> (its c: fluid tag, Chemica's fluid); Chemica's own acid tags already have these names
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
# Chemica's items that are ours under another name, and the c: tag each is known by
ITEMS = {"salt": ("salt", "dusts/salt"), "soda_ash": ("soda_ash", "dusts/soda_ash")}
# the plastic sheets of ours and The Factory's that stand for Chemica's
SHEETS = {"pvc": "fundamentals:pvc_sheet", "polyethylene": "tfmg:plastic_sheet"}
CHEMICA_SHEETS = ["chemica:polyethylene_sheet", "chemica:polyvinyl_chloride_sheet", "chemica:polytetrafluoroethylene_sheet"]

# Chemica's recipes that contradict ours, switched off: path -> why
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
# Chemica's items that only its ores give: a recipe that takes one is switched off
ORE_ITEM = re.compile(r"chemica:((deepslate_)?[a-z]+_ore|raw_[a-z_]+|crushed_raw_[a-z]+|crushed_graphite|([a-z]+_grade_)?rutile_(crystal|dust)"
                      r"|fervorite|zelosite|antimony_(ingot|nugget|dust))")
# the metals both mods make: Chemica's ingots, nuggets and sheets stand for ours by c: tag
METALS = ("tin", "silver", "platinum", "cobalt", "chromium", "tungsten", "molybdenum", "titanium", "magnesium", "iridium")
# Chemica's items that are one of ours under another name, as a recipe takes them
INPUTS = {"chemica:tungsten_dust": {"tag": "c:ingots/tungsten"}, "chemica:phosphorus_dust": {"item": "fundamentals:white_phosphorus"}}
# where one recipe wants the real compound rather than the metal: chromic acid is made from sodium dichromate
RECIPE_INPUTS = {"vat_machine_recipe/mixing/chromic_acid": {"chemica:chromium_dust": {"item": "fundamentals:sodium_dichromate"}}}
# a byproduct Chemica names that does not exist, and native copper's real one (it carries silver, not arsenic)
MISNAMED = {"chemica:sulfur_dust": "tfmg:sulfur_dust"}
NATIVE_COPPER = {"chemica:arsenic_dust": "fundamentals:silver_nugget"}
# Chemica's vat recipes spell some of these in camelCase, which The Factory's vat ignores
SNAKE = {"allowedVatTypes": "allowed_vat_types", "heatRequirement": "heat_requirement", "minSize": "min_size", "processingTime": "processing_time"}
OURS = DATA / "recipe/chemica"
ARC = {"type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:firebrick_lined_vat"], "min_size": 1, "processing_time": 200,
       "machines": ["tfmg:graphite_electrode", "tfmg:graphite_electrode", "tfmg:graphite_electrode"]}


def fluid_ingredient(id, amount):
    """A fluid a recipe of ours takes: a shared reagent by its c: tag, so Chemica's serves, anything else as itself."""
    id = id if ":" in id else f"fundamentals:{id}"
    namespace, name = id.split(":")
    if namespace == "fundamentals" and name in SHARED:
        return {"type": "neoforge:tag", "amount": amount, "tag": f"c:{SHARED[name][0]}"}
    return {"type": "neoforge:single", "amount": amount, "fluid": id}


def optional(ids):
    return [{"id": id, "required": False} for id in ids]


def rewrite(node, inputs, outputs):
    """Chemica's recipe with every shared fluid or item taken by tag and given as ours."""
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
    """Every item a recipe names on its own, not by tag."""
    if isinstance(node, list):
        return [i for each in node for i in takes(each)]
    if not isinstance(node, dict):
        return []
    return ([node["item"]] if set(node) == {"item"} else []) + [i for key, value in node.items() if key not in ("results", "result") for i in takes(value)]


def take_over(path, recipe):
    """Sodium is won from molten salt, not brine, which gives up hydrogen first (the Downs cell)."""
    if path == "vat_machine_recipe/electrolysis/salt":
        recipe["ingredients"] = [i for i in recipe["ingredients"] if i.get("fluid") != "minecraft:water"]
        recipe["results"] = [r for r in recipe["results"] if r["id"] != "minecraft:water"]
    return recipe


def crushing(chemica, create):
    """Chemica's takeovers of Create's ore crushing, which add a byproduct (arsenic with gold, cobalt with nickel, vanadium with
    iron), are still in 1.20 Forge's format and fail to load, which leaves copper and gold ore with no crushing recipe at all.
    Create's own recipe with Chemica's byproduct added is what Chemica meant."""
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
    """Chemica's raw ore chunks in the c:raw_materials tags, which Create's compat crushing takes: with its ores gone they are struck
    off. build_ore_data.py writes the tags, as it owns that folder."""
    with zipfile.ZipFile(CHEMICA_JAR) as jar:
        tags = {Path(e).stem: json.loads(jar.read(e))["values"] for e in jar.namelist()
                if e.startswith("data/c/tags/item/raw_materials/") and e.endswith(".json")}
    return {metal: [v for v in values if ORE_ITEM.fullmatch(v)] for metal, values in tags.items()}


def mixing(name, ingredients, results, heat):
    write(OURS / f"{name}.json", {"neoforge:conditions": LOADED, "type": "create:mixing", "heat_requirement": heat,
                                  "ingredients": ingredients, "results": results})


def missing_metals():
    """The metals Chemica wants that we do not otherwise make, won the way they are. Vanadium rides in titanomagnetite: roasted with
    soda ash and leached, it comes off as sodium vanadate, and the calcine goes on to the blast furnace (Highveld); its oxide is
    reduced by aluminium. Tantalum is digested out of its niobate-tantalate in hydrofluoric acid and its fluoride reduced by sodium
    under argon (Ullmann's, Niobium and Tantalum); euxenite is the one of ours that carries it. Graphite is made, not dug, by baking
    coke to 2,500 C in an electric furnace (Acheson)."""
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
            # Chemica packing or pressing its own form of a metal we make: ours does that already
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
