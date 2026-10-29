#!/usr/bin/env python3
import re
import json
import shutil
import zipfile
from pathlib import Path

import common
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
    "oxygen": ("oxygen", "oxygen"),
    "salt_brine": ("brine", "brine"),
    "caustic_soda": ("caustic_soda", "caustic_soda"),
    "titanium_tetrachloride": ("titanium_tetrachloride", "titanium_tetrachloride"),
    "vinyl_chloride": ("vinyl_chloride", "vinyl_chloride_monomer"),
    "fluorine": ("fluorine", "fluorine"),
}
ITEMS = {"salt": ("salt", "dusts/salt"), "soda_ash": ("soda_ash", "dusts/soda_ash")}
# Chemica items whose recipes we turned off or take over: our salt, graphite and sheets serve in their place.
STOOD_IN = {"chemica:salt", "chemica:purified_graphite_flakes", "chemica:silver_sheet", "chemica:titanium_sheet",
            "chemica:polyethylene_sheet", "chemica:polyvinyl_chloride_sheet"}
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
    "vat_machine_recipe/electrolysis/hydrofluoric_acid": "aqueous hydrofluoric acid gives oxygen at the anode; Moissan's cell runs it anhydrous, in molten potassium bifluoride",
    "vat_machine_recipe/vat_reaction/nitrous_oxide_alt": "coal, fluorite and nitric acid do not make laughing gas; ammonium nitrate does",
    "vat_machine_recipe/vat_reaction/titanium_tetrachloride": "rutile chlorinated without carbon; ours carbochlorinates with coke",
    "vat_machine_recipe/vat_reaction/titanium_tetrachloride_dust": "rutile chlorinated without carbon; ours carbochlorinates with coke",
    "vat_machine_recipe/vat_reaction/titanium_tetrachloride_hg": "rutile chlorinated without carbon; ours carbochlorinates with coke",
    "vat_machine_recipe/vat_reaction/titanium_tetrachloride_mg": "rutile chlorinated without carbon; ours carbochlorinates with coke",
    "casting/polyethylene_sheet": "its polyethylene melt is no longer made; The Factory's plastic sheet serves",
    "casting/polyvinyl_chloride": "its PVC melt is no longer made; our PVC sheet serves",
    "vat_machine_recipe/electrolysis/powdered_obsidian": "obsidian is volcanic glass; electrolysed, it does not give iron, silicon, nitrate and sulfur",
    "vat_machine_recipe/electrolysis/salt": "salt electrolysed in water gives caustic soda and hydrogen, not sodium; ours is the Downs cell, molten salt with calcium chloride",
    "vat_machine_recipe/vat_reaction/titanium_dust": "its magnesium comes back after reducing titanium; Kroll's spends it, as ours does",
    "vat_machine_recipe/arc_furnace_titanium": "titanium melted down with coke would be titanium carbide; ours is remelted under argon",
    "smelting/chromium_dust": "chromium melts at 1907 °C, past any furnace; ours is reduced from its oxide with aluminium",
    "blasting/chromium_dust": "chromium melts at 1907 °C, past any furnace; ours is reduced from its oxide with aluminium",
    "compacting/chromium_dust": "chromium melts at 1907 °C and is not pressed out of a dust; ours is reduced with aluminium",
    "mixing/nitrogen_dioxide": "ammonia burnt with no oxygen and its catalyst used up; the vat's, with oxygen over platinum, stands in",
    "vat_machine_recipe/electrolysis/charcoal_dust": "charcoal does not electrolyse into coal",
    "vat_machine_recipe/vat_reaction/reformate": "benzene reformed back into aromatics; reformate is what reforming naphtha gives",
}
ORE_ITEM = re.compile(r"chemica:((deepslate_)?[a-z]+_ore|raw_[a-z_]+|crushed_raw_[a-z]+|crushed_graphite|([a-z]+_grade_)?rutile_(crystal|dust)"
                      r"|fervorite|zelosite|antimony_(ingot|nugget|dust))")
METALS = ("tin", "silver", "platinum", "cobalt", "chromium", "tungsten", "molybdenum", "titanium", "magnesium", "iridium", "tantalum", "sodium")
# Trace metals crushed out of ore are not free dust: vanadium is caught as its pentoxide, which is the catalyst itself, cobalt
# is won from the nickel refinery and silver from lead bullion.
INPUTS = {"chemica:vanadium_dust": {"item": "fundamentals:vanadium_pentoxide"}, "chemica:cobalt_dust": {"tag": "c:ingots/cobalt"},
          "chemica:silver_dust": {"tag": "c:nuggets/silver"}, "chemica:tungsten_dust": {"tag": "c:ingots/tungsten"}, "chemica:phosphorus_dust": {"item": "fundamentals:white_phosphorus"},
          "chemica:purified_graphite_flakes": {"tag": "c:dusts/graphite"}}
RECIPE_INPUTS = {"vat_machine_recipe/mixing/chromic_acid": {"chemica:chromium_dust": {"item": "fundamentals:sodium_dichromate"}}}
MISNAMED = {"chemica:sulfur_dust": "tfmg:sulfur_dust"}
NATIVE_COPPER = {"chemica:arsenic_dust": "fundamentals:silver_nugget"}
# metals that sit inside an ore's mineral, which crushing does not set free: emerald's trace of chromium, iron ore's vanadium,
# pentlandite's cobalt and galena's silver come out in slag, leach liquor and bullion further on
TRACE = {"chemica:chromium_dust", "chemica:vanadium_dust", "chemica:cobalt_dust", "chemica:silver_dust"}
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


def single(id, amount):
    return {"type": "neoforge:single", "amount": amount, "fluid": id}


# Chemistry Chemica gets wrong, put right: each its real reactants, conditions and byproducts.
REPAIRED = {
    # Ostwald's first step: 4 NH3 + 7 O2 over platinum, which is a catalyst and comes back
    "vat_machine_recipe/mixing/nitrogen_dioxide": {"ingredients": [single("chemica:ammonia", 1000), single("chemica:oxygen", 1750), {"item": "chemica:platinum_catalyst"}],
                                                   "results": [{"amount": 750, "id": "chemica:nitrogen_dioxide"}, {"id": "chemica:platinum_catalyst"}]},
    # PTFE's carbon is methane's: chlorinated to chloroform, fluorinated with HF to R-22, pyrolysed to TFE, then polymerised with persulfate
    "vat_machine_recipe/vat_reaction/polytetrafluoroethylene": {
        "ingredients": [single("chemica:methane", 250), single("chemica:chlorine", 750), single("chemica:hydrofluoric_acid", 500), {"item": "chemica:ammonium_persulfate"}],
        "results": [{"id": "chemica:polytetrafluoroethylene_sheet"}, {"amount": 1000, "id": "chemica:hydrochloric_acid"}]},
    # ethylene and chlorine to EDC, cracked to vinyl chloride and hydrogen chloride, one for one
    "vat_machine_recipe/mixing/vinyl_chloride_monomer": {"ingredients": [single("tfmg:ethylene", 500), single("chemica:chlorine", 500)],
                                                         "results": [{"amount": 500, "id": "chemica:vinyl_chloride_monomer"}, {"amount": 500, "id": "chemica:hydrochloric_acid"}]},
    # SOHIO: propylene, ammonia and oxygen
    "vat_machine_recipe/mixing/acrylonitrile": {"ingredients": [single("chemica:propene", 1000), single("chemica:ammonia", 500), single("chemica:oxygen", 750), {"item": "chemica:vanadium_catalyst"}]},
    # Monsanto: acrylonitrile dimerised to adiponitrile and hydrogenated over nickel; a six-carbon diamine from two three-carbon nitriles
    "vat_machine_recipe/mixing/hexamethylenediamine_solution": {
        "ingredients": [single("tfmg:hydrogen", 1000), single("chemica:acrylonitrile", 1000), {"item": "chemica:nickel_catalyst"}],
        "results": [{"amount": 500, "id": "chemica:hexamethylenediamine_solution"}, {"id": "chemica:nickel_catalyst"}]},
    # persulfate is made at the anode, from ammonium bisulfate; oxygen cannot raise sulfate to it
    "vat_machine_recipe/vat_reaction/ammonium_persulfate": {"machines": ["tfmg:graphite_electrode", "tfmg:graphite_electrode"],
                                                            "ingredients": [single("chemica:ammonia", 500), single("tfmg:sulfuric_acid", 500)],
                                                            "results": [{"id": "chemica:ammonium_persulfate"}, {"amount": 250, "id": "tfmg:hydrogen"}]},
    # sand and soda alone is water glass, which dissolves; lime makes it soda-lime glass
    "mixing/molten_glass": {"ingredients": [{"tag": "minecraft:sand"}, {"tag": "c:dusts/soda_ash"}, {"item": "tfmg:limesand"}]},
    # yeast ferments sugar in water to a weak beer and carbon dioxide, distilled to a quarter of its volume in ethanol
    "vat_machine_recipe/vat_reaction/ethanol": {"ingredients": [{"item": "minecraft:sugar"}] * 3 + [{"item": "chemica:yeast"}, single("minecraft:water", 1000)],
                                                "results": [{"amount": 250, "id": "chemica:ethanol"}, {"amount": 250, "id": "tfmg:carbon_dioxide"}]},
    # benzene takes three hydrogen and keeps its volume
    "vat_machine_recipe/vat_reaction/cyclohexane": {"ingredients": [single("chemica:benzene", 250), single("tfmg:hydrogen", 750), {"item": "chemica:nickel_catalyst"}],
                                                    "results": [{"amount": 250, "id": "chemica:cyclohexane"}, {"id": "chemica:nickel_catalyst"}]},
    # hydrogen is liquefied by compressing it, carrying the heat off in cooling water, and expanding it; never by heating
    "compacting/liquid_hydrogen": {"ingredients": [single("tfmg:hydrogen", 1000), single("minecraft:water", 250)],
                                   "results": [{"amount": 100, "id": "chemica:liquid_hydrogen"}, {"amount": 250, "id": "minecraft:water"}]},
    # wood ash leaches to potash, not soda
    "vat_machine_recipe/centrifuge/ash": {"results": [{"chance": 0.2, "id": "fundamentals:potash"}]},
    # air is 78 % nitrogen, 21 % oxygen and 0.9 % argon; its helium, five parts in a million, is not worth taking, and comes from natural gas
    "distillation/air": {"results": [{"amount": 780, "id": "chemica:nitrogen"}, {"amount": 9, "id": "chemica:argon"}, {"amount": 210, "id": "chemica:oxygen"}]},
    # copper is etched by an oxidising acid: cupric chloride in hydrochloric acid, kept up with air
    "vat_machine_recipe/vat_reaction/etched_epoxy_circuit_board": {"ingredients": [{"item": "chemica:coated_epoxy_circuit_board"}, single("chemica:hydrochloric_acid", 250), single("tfmg:air", 250)]},
    "vat_machine_recipe/vat_reaction/etched_pvc_circuit_board": {"ingredients": [{"item": "chemica:coated_pvc_circuit_board"}, single("chemica:hydrochloric_acid", 250), single("tfmg:air", 250)]},
}


def take_over(path, recipe):
    recipe.update(REPAIRED.get(path, {}))
    if path == "compacting/liquid_hydrogen":
        recipe.pop("heat_requirement", None)
    # isobutylene adds to ethanol: ETBE gives off no water
    if path == "vat_machine_recipe/vat_reaction/ethyl_tert_butyl_ether":
        recipe["results"] = [r for r in recipe["results"] if "water" not in r.get("id", "")]
    # the cell splits brine to hydrogen and chlorine one for one
    if path == "vat_machine_recipe/electrolysis/brine":
        for r in recipe["results"]:
            if r.get("id") == "tfmg:hydrogen":
                r["amount"] = next(c["amount"] for c in recipe["results"] if c.get("id") == "chemica:chlorine")
    # Our chlor-alkali cell runs the same brine on the same two electrodes over a dimensionally stable anode, and Chemica's
    # steam is water alone in a vat with no machine: on graphite, and under a mixer, neither can be taken for the other.
    if path == "vat_machine_recipe/electrolysis/brine":
        recipe["machines"] = ["tfmg:graphite_electrode", "tfmg:graphite_electrode"]
    if path == "vat_machine_recipe/vat_reaction/epichlorohydrin":
        recipe["machines"] = ["tfmg:mixing"]
    return recipe


def liberated(results):
    """Crushing wheels liberate and do not multiply: an ore gives one crushed ore, whatever Create's count."""
    crushed = next((r["id"] for r in results if "crushed_raw_" in r["id"]), None)
    return [{"id": crushed}] + [r for r in results if r["id"] != crushed] if crushed else results


# Chemica's own takeovers of Create's ore crushing are in 1.20 Forge's format and fail to load, leaving copper and gold ore uncrushable.
def crushing(chemica, create):
    written = 0
    theirs = {e for e in chemica.namelist() if e.startswith("data/create/recipe/crushing/") and e.endswith(".json")}
    for entry in sorted(create.namelist()):
        if not (entry.startswith("data/create/recipe/crushing/") and entry.endswith(".json")):
            continue
        original = json.loads(create.read(entry))
        ores = json.dumps(original["ingredients"])
        if entry not in theirs and not (("_ore" in ores or "c:ores/" in ores) and any("crushed_raw_" in r.get("id", "") for r in original["results"])):
            continue
        if entry.endswith("/platinum_ore.json"):
            # Create Propulsion's own recipes crush the only platinum ores, one for one.
            write(CRUSHING / "platinum_ore.json", {"neoforge:conditions": [{"type": "neoforge:false"}]})
            continue
        conditions = original.pop("neoforge:conditions", [])
        extra = []
        if entry in theirs:
            named = MISNAMED | (NATIVE_COPPER if "copper" in entry else {})
            extra = [{**{k: v for k, v in r.items() if k != "item"}, "id": named.get(r["item"], r["item"])}
                     for r in json.loads(chemica.read(entry))["results"] if r["item"].split(":")[0] not in ("minecraft", "create")
                     and r["item"] not in TRACE]
            conditions = LOADED + conditions
        write(CRUSHING / Path(entry).name, {**({"neoforge:conditions": conditions} if conditions else {}),
                                            **original, "results": liberated(original["results"]) + extra})
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
    mixing("vanadium_ingot", [{"item": "fundamentals:vanadium_pentoxide"}] * 2 + [{"item": "fundamentals:aluminium_powder"}] * 2,
           [{"id": "chemica:vanadium_ingot"}, {"id": "fundamentals:alumina"}], "superheated")


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
    metals = {f"chemica:{metal}_{form}" for metal in METALS for form in ("ingot", "nugget", "block")}
    with zipfile.ZipFile(CHEMICA_JAR) as jar:
        # its ores no longer generate and our recipes stand in for its metals
        hidden = common.hide_dead(jar, "chemica", lambda i: bool(ORE_ITEM.fullmatch(i)) or i in metals or i in STOOD_IN)
    print(f"wrote {len(SHARED)} shared reagents, {rewritten} of Chemica's recipes rewritten, {off} switched off, {repaired} crushing repaired, {hidden} dead items hidden")


if __name__ == "__main__":
    main()
