#!/usr/bin/env python3
import json
import re

from common import REPO, RESOURCES, read_lang

DATA = RESOURCES / "data"
OUT = REPO / "docs/byproducts.md"
OUTPUT_KEYS = ("result", "results", "byproduct")
NOT_BYPRODUCTS = {"create:experience_nugget", "tfmg:lithium_nugget"}
OPTIONAL = ("chemica", "createnuclear")
TAILINGS = {"fundamentals:clarifier_sludge_block", "fundamentals:red_mud_block", "fundamentals:monazite_residue_block"}

# Made by Java, not by a recipe file.
MADE_IN_CODE = {
    "fundamentals:spent_liquor": "every solvent-extraction cut, into the head stage's sump",
    "fundamentals:fouled_p204": "a battery run on crude liquor",
    "fundamentals:fouled_p507": "a battery run on crude liquor",
    "fundamentals:fouled_naphthenic_acid": "a battery run on crude liquor",
}

# Where a byproduct goes outside this mod's own recipes.
ELSEWHERE = {
    "tfmg:slag": "The Factory Must Grow's concrete and asphalt mixtures",
    "tfmg:carbon_dioxide": "The Factory Must Grow's carbon dioxide tank and fire extinguisher",
    "tfmg:cast_iron_ingot": "The Factory Must Grow's cast iron parts",
    "tfmg:coal_coke_dust": "The Factory Must Grow's coke uses",
    "tfmg:furnace_gas": "The Factory Must Grow's blast stove, which burns it to heat the blast",
    "tfmg:molten_slag": "The Factory Must Grow's casting basin, which casts it to slag blocks",
    "tfmg:propylene": "The Factory Must Grow's plastics",
    "tfmg:sulfuric_acid": "everything that takes sulfuric acid",
    "minecraft:copper_ingot": "everything that takes copper",
    "minecraft:clay_ball": "everything that takes clay",
    "minecraft:iron_nugget": "nine craft to an iron ingot",
}

# Finished goods no recipe takes, which are not blocks or equipment.
END_PRODUCTS = {
    "fundamentals:thulium_oxide": "Thulium is sold as oxide for portable X-ray sources and fibre lasers, neither in the pack.",
    "fundamentals:ytterbium_oxide": "Ytterbium is sold as oxide for fibre lasers and atomic clocks, neither in the pack.",
    "fundamentals:lutetium_oxide": "Lutetium is sold as oxide for the scintillator crystals of PET scanners, not in the pack.",
    "fundamentals:depleted_uranium_oxide": "Depleted uranium is kept as U3O8, the stable oxide, as the American deconversion plants keep theirs; "
                                           "its metal's uses, counterweights and armour, are not in the pack. Burn it in lava.",
    "fundamentals:uranium_fuel_pellet": "Fuel for Create: Nuclear's uranium and thorium rods; without it the pack has no reactor.",
    "fundamentals:zircaloy_tube": "Fuel cladding for Create: Nuclear's rods; without it the pack has no reactor.",
    "fundamentals:thorium_dioxide": "Thoria for Create: Nuclear's thorium rod; without it the pack has no reactor.",
    "fundamentals:boron_carbide": "The absorber of Create: Nuclear's control rod; without it the pack has no reactor.",
}
EQUIPMENT = {"fundamentals:argon_canister", "fundamentals:photovoltaic_panel"}

# Deliberate dead ends: what the waste is, and how a player is rid of it.
DEAD = {
    "fundamentals:clarifier_sludge": "Iron, aluminium and thorium hydroxides: a neutralisation sludge, impounded as a low-level radioactive residue (Lynas stores its own at Gebeng). "
                                     "Nine pack into clarifier tailings to wall into a dam or bury.",
}

WHY = {
    "fundamentals:argon": "Argon comes off the same cryogenic air separation that gives neon, as every industrial air plant co-produces both.",
    "fundamentals:bittern": "Bittern, the mother liquor of salt pans, is the old source of bromine and magnesium chloride.",
    "fundamentals:caustic_soda": "The Bayer precipitator gives most of its caustic back to the digester, as a real refinery recycles it.",
    "fundamentals:cerium_concentrate": "Mountain Pass sold its leached-out ceria concentrate for polishing powder.",
    "fundamentals:chlorine": "Anode chlorine from a molten-chloride cell goes back to chlorination, as at every magnesium and titanium plant.",
    "fundamentals:clarifier_sludge": "The lime-neutralised hydroxide sludge has no market.",
    "fundamentals:cobalt_chloride_liquor": "The cobalt strip of a nickel refinery's P507 circuit is electrowon, as at Niihama and Harjavalta.",
    "fundamentals:europium_sulfate": "Europium sulfate is redissolved to europium liquor in McCoy's method.",
    "fundamentals:depleted_uranium_hexafluoride": "Enrichment leaves most of its feed as tails, down to 0.2 or 0.3 per cent uranium-235, kept as hexafluoride in steel cylinders until it is deconverted to oxide.",
    "fundamentals:hydrofluoric_acid": "Deconverting uranium hexafluoride gives its fluorine back as hydrofluoric acid, which the deconversion plants at Paducah and Portsmouth sell.",
    "fundamentals:yellowcake": "Monazite carries a little uranium beside its thorium, and India's monazite plants recover it.",
    "fundamentals:hafnium_tetrachloride": "Hafnium is the by-product of making nuclear-grade zirconium, and all of it comes this way.",
    "fundamentals:hydrochloric_acid": "Hydrolysing a chloride gives the acid back, which a chloride plant regenerates and re-uses.",
    "fundamentals:insoluble_residue": "The aqua regia residue carries the osmium, ruthenium, iridium and rhodium, which every PGM refinery works up next.",
    "fundamentals:iridium_rhodium_residue": "What the tetroxide distillation leaves still holds the iridium and rhodium.",
    "fundamentals:lanthanum_oxide": "Lanthanum oxide left by the samarium reduction goes round again; spent FCC catalyst lanthanum is re-used.",
    "fundamentals:litharge": "Cupellation litharge was always reduced back to lead, and PbO is a lead-acid battery's paste.",
    "fundamentals:magnesium_chloride": "The Kroll process's magnesium chloride is electrolysed back to magnesium and chlorine.",
    "fundamentals:monazite_residue_dust": "Thorium from monazite made Welsbach mantles; the rest of the residue is stored as radioactive waste.",
    "fundamentals:palladium_liquor": "The filtrate from the platinum salt carries the palladium on.",
    "fundamentals:phosphoric_acid": "The phosphate freed by cracking monazite is the base of phosphate fertiliser.",
    "fundamentals:platinum_group_concentrate": "The nickel refinery's leach residue is where the platinum metals come from.",
    "fundamentals:raw_fluorite": "Calciothermic reduction leaves calcium fluoride, which is fluorspar.",
    "fundamentals:red_mud": "Bauxite residue is the alumina industry's great waste, mostly ponded or dry-stacked; it carries about 100 g a tonne of scandium, which Rusal leaches out at its Urals refineries.",
    "fundamentals:rhenium_flue_dust": "All mined rhenium comes from molybdenite roaster flue dust.",
    "fundamentals:rhodium_liquor": "The filtrate from the iridium salt carries the rhodium on.",
    "fundamentals:ruthenium_tetroxide": "The tetroxide distillation is how ruthenium is parted from the rest.",
    "fundamentals:silver_zinc_crust": "The Parkes crust is retorted and cupelled for its silver.",
    "fundamentals:soda_ash": "The dichromate reduction gives the sodium back as carbonate.",
    "fundamentals:spent_liquor": "Spent chloride raffinate is neutralised with lime; a plant recovers its calcium chloride. It corrodes metal pipe.",
    "fundamentals:fouled_p204": "Fouled organic is scrubbed and returned; extractant is too dear to throw away.",
    "fundamentals:fouled_p507": "Fouled organic is scrubbed and returned; extractant is too dear to throw away.",
    "fundamentals:fouled_naphthenic_acid": "Fouled organic is scrubbed and returned; extractant is too dear to throw away.",
    "fundamentals:water_gas": "Carbon monoxide freed by decomposing nickel carbonyl goes back to make more, as at Clydach.",
    "fundamentals:zinc_oxide": "Zinc oxide from the Parkes crust goes back to the zinc works.",
    "tfmg:carbon_dioxide": "Carbon dioxide from the anode and the soda reactions; real plants mostly vent it.",
    "tfmg:cast_iron_ingot": "The Sorel slag furnace's iron is sold as high-purity pig iron.",
    "tfmg:coal_coke_dust": "Coke burnt onto a cracking catalyst is burnt off in the regenerator.",
    "tfmg:furnace_gas": "Blast furnace and converter gas is a fifth carbon monoxide; works burn it in the Cowper stoves that heat the blast.",
    "tfmg:molten_slag": "Lime-silicate slag runs off the iron and is granulated for slag cement or cast and crushed for road stone.",
    "tfmg:hydrogen": "Chlor-alkali hydrogen is burnt or used for hydrogenation and reduction.",
    "tfmg:propylene": "A fluid catalytic cracker is the world's second source of propylene.",
    "tfmg:slag": "Silicate slag is crushed for aggregate and cement (slag cement), and old bloomery slag, near two-thirds iron oxide, was re-smelted for its iron.",
    "tfmg:sulfuric_acid": "Electrowinning regenerates the sulfuric acid at the anode.",
    "minecraft:clay_ball": "Ion-adsorption clay leached of its rare earths is just clay again.",
    "minecraft:water": "Brine boiled for salt gives its water off as condensate.",
    "fundamentals:raw_magnetite": "Washing gravel leaves the heavy black sand, magnetite, behind.",
    "minecraft:copper_ingot": "Copper co-deposited with nickel at the cathode is a sellable metal.",
    "minecraft:iron_nugget": "Crushed slag is passed under a magnet to pull out its metallic iron prills.",
    "fundamentals:salt": "Sylvinite is sylvite and halite together; the hot leach takes the potassium chloride and leaves the salt, which Saskatchewan's mills stack or pump back underground.",
    "fundamentals:niobium_oxide": "Columbite-tantalite carries more niobium than tantalum, and the same extraction strips it as niobium oxide, as at Ulba and Ningxia.",
    "fundamentals:potassium_fluorotantalate": "Euxenite carries a little tantalum beside its niobium, and its fluoride digest yields it the same way.",
    "fundamentals:heavy_rare_earth_concentrate": "Hydrofluoric acid leaves euxenite's yttrium and rare earths behind as insoluble fluorides, which are cracked like any other rare earth concentrate.",
    "fundamentals:titanium_tetrachloride": "Solikamsk chlorinates loparite and condenses titanium tetrachloride with the niobium and tantalum chlorides; the titanium goes to sponge.",
    "fundamentals:crude_rare_earth_liquor": "The chloride melt a loparite chlorinator leaves holds the rare earths, which Solikamsk leaches and sends to separation.",
    "fundamentals:mercury": "An amalgam retort condenses its mercury and the mill uses it again, though old mills lost a good part of it to the air and the river.",
    "fundamentals:silver_nugget": "Native gold always carries silver; Miller chlorination and aqua regia part it as silver chloride, which is reduced to silver.",
}


def recipes():
    for path in sorted(DATA.glob("*/recipe/**/*.json")):
        recipe = json.loads(path.read_text(encoding="utf-8"))
        conditions = recipe.get("neoforge:conditions", [])
        if any(c.get("type") == "neoforge:false" or c.get("type") == "neoforge:mod_loaded" and c.get("modid") in OPTIONAL for c in conditions):
            continue
        if path.parts[path.parts.index("data") + 1] in OPTIONAL:
            continue
        yield "/".join(path.relative_to(DATA).with_suffix("").parts[2:]), recipe


def tags():
    found = {}
    for kind in ("item", "fluid"):
        for path in DATA.glob(f"*/tags/{kind}/**/*.json"):
            ns = path.parts[path.parts.index("data") + 1]
            name = "/".join(path.relative_to(DATA / ns / "tags" / kind).with_suffix("").parts)
            values = json.loads(path.read_text(encoding="utf-8"))["values"]
            found.setdefault(f"{ns}:{name}", []).extend(v["id"] if isinstance(v, dict) else v for v in values)
    return found


def resolve(table, tag, seen=frozenset()):
    out = set()
    for value in table.get(tag, []):
        if value.startswith("#"):
            if value[1:] not in seen:
                out |= resolve(table, value[1:], seen | {value[1:]})
        else:
            out.add(value)
    return out


def ids_in(node, table):
    out = set()
    if isinstance(node, dict):
        if "tag" in node:
            out |= resolve(table, node["tag"])
        out |= {node[k] for k in ("item", "id", "fluid") if isinstance(node.get(k), str)}
        for key, value in node.items():
            if key not in OUTPUT_KEYS:
                out |= ids_in(value, table)
    elif isinstance(node, list):
        for value in node:
            out |= ids_in(value, table)
    elif isinstance(node, str) and re.fullmatch(r"[a-z0-9_]+:[a-z0-9_/]+", node):
        out.add(node)
    return out


def outputs(recipe):
    out = []
    if recipe.get("type") == "create:sequenced_assembly":
        return [recipe["results"][0]["id"]]
    for key in OUTPUT_KEYS:
        value = recipe.get(key)
        for stack in value if isinstance(value, list) else [value] if isinstance(value, dict) else []:
            ident = stack.get("id") or stack.get("item") or stack.get("fluid")
            if ident not in out:
                out.append(ident)
    return out


def inputs(recipe, table):
    return ids_in({k: v for k, v in recipe.items() if k not in OUTPUT_KEYS + ("type", "neoforge:conditions", "light", "heavy", "attracted")}, table)


def main():
    table, lang = tags(), read_lang()
    toxic = resolve(table, "fundamentals:toxic_dusts")
    made, used, packs, finished = {}, {}, {}, set()
    for name, recipe in recipes():
        taken = inputs(recipe, table)
        finished.update(i for i in outputs(recipe) if i and i.startswith("fundamentals:"))
        if outputs(recipe)[:1] and outputs(recipe)[0] in TAILINGS:
            packs[name] = outputs(recipe)[0]
        for ident in taken:
            used.setdefault(ident, []).append(name)
        for ident in outputs(recipe)[1:]:
            if ident not in taken and ident not in NOT_BYPRODUCTS:
                made.setdefault(ident, []).append(name)
    for ident, where in MADE_IN_CODE.items():
        made.setdefault(ident, []).append(where)

    missing = sorted(set(made) - set(WHY))
    assert not missing, f"byproducts with no real-world line in WHY: {missing}"
    stale = sorted(set(WHY) - set(made))
    assert not stale, f"WHY lists things no recipe makes as a byproduct: {stale}"
    unhandled = sorted(i for i in made if not set(used.get(i, [])) - set(packs) and i not in ELSEWHERE and i not in DEAD)
    assert not unhandled, f"byproducts nothing takes, neither sent elsewhere nor declared dead ends: {unhandled}"

    dead_used = sorted(i for i in DEAD if set(used.get(i, [])) - set(packs))
    assert not dead_used, f"declared dead ends that a recipe now takes: {dead_used}"
    blocks = {f"fundamentals:{p.stem}" for p in (RESOURCES / "assets/fundamentals/blockstates").glob("*.json")}
    idle = {i for i in finished if i not in used and i not in made and i not in blocks and i not in EQUIPMENT}
    assert idle == set(END_PRODUCTS), f"finished goods nothing takes, against END_PRODUCTS: {sorted(idle ^ set(END_PRODUCTS))}"

    def display(ident):
        ns, key = ident.split(":")
        name = lang.get(f"item.{ns}.{key}") or lang.get(f"block.{ns}.{key}") or lang.get(f"fluid_type.{ns}.{key}")
        return f"{name} (`{ident}`)" if name else f"`{ident}`"

    rows, counts = [], {"use": 0, "elsewhere": 0, "dead": 0, "toxic": 0}
    for ident in sorted(made, key=lambda i: (i.split(":")[0] != "fundamentals", i)):
        sinks = sorted(set(used.get(ident, [])))
        uses = [s for s in sinks if s not in packs]
        goes = ", ".join(f"`{s}`" for s in uses[:4]) + (f" and {len(uses) - 4} more" if len(uses) > 4 else "")
        if ident in ELSEWHERE:
            goes = f"{goes}; {ELSEWHERE[ident]}" if goes else ELSEWHERE[ident]
        tailings = sorted({packs[s] for s in sinks if s in packs})
        if tailings and ident not in DEAD:
            goes += "; the rest packs into " + " and ".join(display(t) for t in tailings) + " to bury"
        if ident in DEAD:
            verdict = f"**Dead end.** {DEAD[ident]}"
            counts["dead"] += 1
        else:
            verdict = goes
            counts["use" if uses else "elsewhere"] += 1
        if ident in toxic:
            verdict += " **Toxic dust:** held, or left in an open basin, it hurts and sickens anyone without a diving helmet on a filled backtank."
            counts["toxic"] += 1
        sources = sorted(set(made[ident]))
        source = ", ".join(s if " " in s else f"`{s}`" for s in sources[:3]) + (f" and {len(sources) - 3} more" if len(sources) > 3 else "")
        rows.append(f"| {display(ident)} | {source} | {verdict} | {WHY[ident]} |")

    md = ["# Byproducts", "",
          "> Generated by `tools/build_byproducts.py` from the recipes in `src/main/resources/data`. Do not edit by hand.", "",
          "Every second output of a recipe (and the waste the separation plant makes in code), where it goes, and why that is what the real one does. "
          "A catalyst or bath a recipe hands back is not counted. Recipes that need Chemica or Create: Nuclear are left out; Chemica's own arsenic dust from gold ore is "
          "in the toxic dust tag all the same.", "",
          f"**{len(made)} byproducts.** Going to a use in this mod: {counts['use']}. Going only to The Factory Must Grow or the vanilla game: {counts['elsewhere']}. "
          f"Deliberate dead ends: {counts['dead']}. Toxic to handle: {counts['toxic']}.", "",
          "| Byproduct | Made by | Goes to | Why |", "|---|---|---|---|", *rows, "",
          "## Tailings", "",
          "Waste that has no use is packed into a block and put back in the ground, as a tailings dam or a dry stack holds it: "
          + ", ".join(display(t) for t in sorted(TAILINGS)) + ". "
          "Anything else can be burnt in lava or thrown in the void, as in the vanilla game.", "",
          "## Finished, and taken by nothing", "",
          "Not byproducts, but the only other items the recipes make that no recipe takes:", "",
          "| Item | Why |", "|---|---|", *(f"| {display(i)} | {END_PRODUCTS[i]} |" for i in sorted(END_PRODUCTS)), ""]
    OUT.write_text("\n".join(md), encoding="utf-8")
    print(f"byproducts: {len(made)} ({counts})")


if __name__ == "__main__":
    main()
