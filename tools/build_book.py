#!/usr/bin/env python3
"""Fundamentals: First Principles, the in-game book (Patchouli). The chapters are written here; whatever
they say about the data (which minerals form which deposits, how many stages each cut takes, how long
the road from liquor to each metal is) is read from the generators and the recipe files, so the book
cannot drift from the game. Re-run after any edit to the chain.

    python3 tools/build_book.py

Patchouli is optional: without it the book's files are ignored and its recipe is conditioned away.
"""
import json
import re
import shutil

from build_ore_data import ASSETS, BIOMES, DATA, DEPOSITS, PLACERS, write

BOOK = "first_principles"
BOOK_DATA = DATA / f"patchouli_books/{BOOK}"
BOOK_ASSETS = ASSETS / f"patchouli_books/{BOOK}/en_us"
RECIPES = DATA / "recipe"

PAGE_CHARS = 330

WHERE = {
    "anywhere": "anywhere in the overworld", "porphyry": "under mountains and hills", "arid_oxide": "in badlands, savanna and desert",
    "laterite": "under jungle, savanna and mangrove", "pegmatite": "in mountains, hills and badlands", "carbonatite": "under mountains and badlands",
    "alkaline": "under taiga and snowy country", "ion_clay": "under jungle", "wetland": "in swamps and bogs", "hydrothermal": "in mountain and hill country",
    "placer": "in beach and river sand",
}
STYLE = {"pockets": "masses", "seams": "layers", "disseminated": "scattered grains", "top": "an enriched top"}
KIND = {"bed": "a bed", "plug": "a plug", "blanket": "a blanket just under the surface", "vein": "a vein"}


LANG = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))


def pretty(id):
    """A fluid or item by the name the game shows, lower-cased for the middle of a sentence."""
    key = id.split(":")[-1]
    name = LANG.get(f"fluid_type.fundamentals.{key}") or LANG.get(f"item.fundamentals.{key}") or LANG.get(f"block.fundamentals.{key}")
    if name is None:
        return key.replace("_", " ")
    return name if name[:1].isupper() and name[1:2].isdigit() else name.lower()


def pages_of(text, title=None):
    """Patchouli truncates a page; split a long text at sentence ends into pages that fit."""
    sentences = re.split(r"(?<=[.!?])\s+", text.strip())
    pages, current = [], ""
    for s in sentences:
        if current and len(current) + 1 + len(s) > PAGE_CHARS:
            pages.append(current)
            current = s
        else:
            current = f"{current} {s}".strip()
    if current:
        pages.append(current)
    out = [{"type": "patchouli:text", "text": p} for p in pages]
    if title and out:
        out[0]["title"] = title
    return out


def spotlight(item, text, title=None):
    page = {"type": "patchouli:spotlight", "item": item, "text": text}
    if title:
        page["title"] = title
    return page


def crafting(recipe, text=None):
    page = {"type": "patchouli:crafting", "recipe": recipe}
    if text:
        page["text"] = text
    return page


def category(id, name, description, icon, sortnum):
    write(BOOK_ASSETS / f"categories/{id}.json", {"name": name, "description": description, "icon": icon, "sortnum": sortnum})


def entry(cat, id, name, icon, pages, sortnum=0):
    write(BOOK_ASSETS / f"entries/{cat}/{id}.json", {"name": name, "category": f"fundamentals:{cat}", "icon": icon, "sortnum": sortnum, "pages": pages})


# ---- the data the chapters quote ----

def cuts():
    out = {}
    for path in sorted((RECIPES / "separation").glob("*.json")):
        d = json.loads(path.read_text())
        out[d["liquor"]] = d
    return out


def roads(tree):
    """Every single-element liquor and the batteries it takes to reach it from the mixed liquor."""
    roads = {}

    def walk(liquor, path):
        if liquor not in tree:
            roads[liquor] = path
            return
        cut = tree[liquor]
        for side in ("light", "heavy"):
            walk(cut[side], path + [(cut["stages"], pretty(cut["organic"]))])

    walk("fundamentals:rare_earth_liquor", [])
    return roads


# ---- chapters ----

def geology():
    category("geology", "Geology", "Where the minerals are, and why they are there.", "fundamentals:raw_hematite", 0)
    entry("geology", "deposits", "Deposits, not blobs", "fundamentals:raw_hematite", pages_of(
        "Vanilla scatters ore in blobs. The ground doesn't. Every ore body here is a kind of deposit that really forms: a bed laid down in "
        "water, a plug of odd rock pushed up from below, a blanket of weathered soil, a vein filling a crack. Each body is rich at its "
        "core and peters out at the edges, and the grade of an ore block decides what it drops. Find the right rock in the right country "
        "and you have found the metal; this chapter is the prospector's list.", "Deposits, not blobs"), 0)
    for i, (name, spec) in enumerate(DEPOSITS.items()):
        kind, host, minerals, *_rest = spec
        where = WHERE.get(spec[6], spec[6])
        y0, y1 = spec[7]
        host_text = "" if host is None else (f" of {pretty(host)}" if not host.endswith("_ore") else f", the whole body {pretty(host)}")
        parts = [f"{pretty(m)} in {STYLE.get(style, style)}" for m, _grade, style in minerals]
        text = (f"{KIND[kind].capitalize()}{host_text}, {where}, between y {y0} and {y1}. "
                + (f"Carries {', '.join(parts)}." if parts else "") + " Dig at the core: the edges are lean.")
        icon = f"fundamentals:raw_{minerals[0][0]}" if minerals else f"fundamentals:{host}"
        entry("geology", name, pretty(name).title(), icon, pages_of(text, pretty(name).title()), 10 + i)
    placer_text = " ".join(f"{pretty(m).capitalize()} washes into beach and river sand between y {y0} and {y1}." for m, (y0, y1, _n, _s) in PLACERS.items())
    entry("geology", "placers", "Placer sands", "fundamentals:raw_monazite", pages_of(
        "Heavy minerals weather out of the rock and settle where water slows. " + placer_text + " No digging: pan the sand.", "Placer sands"), 90)


def ironworking():
    category("ironworking", "By hand", "Iron, copper and lead the way they were won for three thousand years, before any machine.", "fundamentals:smithing_hammer", 1)
    entry("ironworking", "bloomery", "The bloomery", "fundamentals:bloomery", pages_of(
        "Build a bloomery out of clay, load it with iron ore and charcoal, light it with a torch and wait. Only charcoal: the sulfur in coal "
        "ruins iron. What comes out is not an ingot but a bloom, a spongy lump of iron and slag that never melted.", "The bloomery")
        + [crafting("fundamentals:bloomery")], 0)
    entry("ironworking", "bloom", "Bloom and hammer", "fundamentals:iron_bloom", pages_of(
        "A bloom is hammered, not cast. Beat it with the smithing hammer to drive the slag out and weld the iron together into wrought iron. "
        "Copper comes straight out of the bloomery from malachite, azurite and cuprite. Galena has to be roasted on a fire first, which "
        "drives off its sulfur; the roasted ore then gives lead.", "Bloom and hammer") + [crafting("fundamentals:smithing_hammer")], 1)
    entry("ironworking", "mortar", "Mortar and pestle", "fundamentals:mortar_and_pestle", pages_of(
        "A mortar and pestle grinds by hand what Create's millstone grinds by power: grain to flour, bone to meal, and coloured minerals "
        "to the pigments painters have ground since antiquity: hematite red, goethite yellow, malachite green, azurite blue, pyrolusite black, "
        "cinnabar vermilion. Hold the mortar in one hand and the material in the other.", "Mortar and pestle")
        + [crafting("fundamentals:mortar_and_pestle")], 2)


def rare_earths():
    tree = cuts()
    category("rare_earths", "The rare earths", "Sixteen metals that travel together and must be parted one by one. The longest road in the mod.",
             "fundamentals:neodymium_ingot", 2)
    entry("rare_earths", "minerals", "The minerals", "fundamentals:raw_bastnasite", pages_of(
        "The rare earths are not rare, only mixed. Bastnäsite in carbonatite plugs and monazite in beach sand carry the light ones, "
        "lanthanum to samarium. Xenotime and euxenite in pegmatite veins carry the heavy ones and yttrium. Loparite under cold country "
        "carries the lights too. Ion-adsorption clay under jungle is the odd one: a clay with the heavy rare earths merely stuck to it, "
        "which is why it is leached and never roasted.", "The minerals") + [
        spotlight("fundamentals:light_rare_earth_concentrate", "A millstone or crushing wheels grind bastnäsite, xenotime, loparite and euxenite; monazite is a sand already. "
                  "Washed under an encased fan, the light grains wash away and about half of what you fed it stays as a mixed concentrate: light from monazite and loparite, heavy from xenotime and euxenite.", "Concentrate")], 0)
    entry("rare_earths", "liquor", "Liquor", "fundamentals:salt", [
        spotlight("fundamentals:light_rare_earth_concentrate", "A concentrate dissolved in 500 mB of hydrochloric acid in a heated basin under a mixer gives 500 mB of chloride liquor: "
                  "every rare earth in it together, lilac from the neodymium. The heavy concentrate gives the heavy liquor.", "Dissolving"),
        spotlight("fundamentals:raw_ion_adsorption_clay", "The clay needs no acid and no heat: four clay, a salt and 500 mB of water in a basin under a mixer leach straight to 250 mB of heavy liquor. "
                  "That is the whole reason Chinese clays supply the world's heavy rare earths.", "Leaching")], 1)
    entry("rare_earths", "mixer_settler", "Solvent extraction", "fundamentals:mixer_settler", pages_of(
        "Chlorides of neighbouring rare earths are almost identical, so no one step parts them. Instead the liquor is shaken with an "
        "organic extractant (P507, P204 or naphthenic acid, each made from phosphoric acid and kerosene) that prefers the heavier ions "
        "by a hair, left to settle into two layers, and the two layers sent opposite ways through a long line of identical stages. "
        "Each stage enriches a little; thirty of them compound a hair into a clean split. The stages are mixer-settlers, and a line of them is a battery.",
        "Solvent extraction") + pages_of(
        "Place casings facing the same way and they merge into a vat, three by three up to three across, three along and two tall; "
        "nine casings is a lab vat, eighteen a plant vat. The back row is the mixing box, with Create's Mechanical Mixer standing over its hatch; "
        "the rows ahead are the settling bay, parted from the box by a weir that only the churn tops. "
        "Charge every stage with the extractant from a pipe into its top: it floats and is never used up. Pump the liquor into the back of "
        "the first stage and hydrochloric acid into the front of the last, take the raffinate from the first stage's sides and the loaded strip "
        "from the last stage's, and throw the lever. Goggles on any stage tell you what the battery is waiting for.") + [crafting("fundamentals:mixer_settler")], 2)
    # the cuts, as the data has them
    cut_pages = []
    for liquor, cut in sorted(tree.items(), key=lambda kv: kv[1]["stages"]):
        cut_pages += pages_of(f"{pretty(liquor).capitalize()} parts into {pretty(cut['light'])} (raffinate, head end) and {pretty(cut['heavy'])} "
                              f"(strip, tail end) in a battery of {cut['stages']} stages charged with {pretty(cut['organic'])}, stripped by {pretty(cut['strip'])}.")
    entry("rare_earths", "cuts", "The fourteen cuts", "fundamentals:neodymium_oxalate", pages_of(
        f"Fourteen cuts take the mixed liquor down to single elements. A battery too short for its cut does nothing; the goggles say how many stages it wants. "
        "The number is set by how alike the pair is: samarium leaves neodymium in eight, but neodymium from praseodymium takes thirty-two.",
        "The fourteen cuts") + cut_pages, 3)
    # the road to each metal
    road_pages = []
    for liquor, path in sorted(roads(tree).items(), key=lambda kv: sum(s for s, _ in kv[1])):
        total = sum(s for s, _ in path)
        legs = ", then ".join(f"{s} stages with {o}" for s, o in path)
        road_pages += pages_of(f"{pretty(liquor).replace(' liquor', '').capitalize()}: {len(path)} batteries, {total} stages in all. {legs}.")
    entry("rare_earths", "roads", "The road to each metal", "fundamentals:dysprosium_ingot", pages_of(
        "From the mixed liquor to one element is a chain of batteries, each fed by the one before. These are the roads, shortest first. "
        "The heavy liquor can also be leached straight from the clay, which skips the first eight stages for everything on the heavy side.",
        "The road to each metal") + road_pages, 4)
    entry("rare_earths", "oxide", "Oxalate and oxide", "fundamentals:neodymium_oxide", [
        spotlight("fundamentals:oxalic_acid", "A single-element liquor and oxalic acid in a basin under a mixer, two blocks below it with the whisk between, drop the oxalate: "
                  "250 mB of liquor and one oxalic acid to one oxalate. The oxalates are pale powders with the ion's cast: praseodymium green, neodymium lilac, erbium pink, most of them white.", "Oxalate"),
        spotlight("fundamentals:neodymium_oxide", "Any furnace calcines the oxalate to the oxide, the stable form every rare earth is traded in. The oxides are the colours they really are: "
                  "lanthanum white, cerium pale yellow, praseodymium brown-black, neodymium blue-grey, terbium brown, erbium pink.", "Oxide")], 5)
    entry("rare_earths", "metal", "Oxide to metal", "fundamentals:neodymium_ingot", pages_of(
        "The metal comes out of the oxide three ways, all in The Factory Must Grow's chemical vats. The lights (lanthanum to neodymium, and didymium) and the heavies both go through their fluoride first: "
        "one oxide and 500 mB of hydrofluoric acid in a basin under a mixer. The acid itself is two raw fluorite and 500 mB of sulfuric acid, heated; fluorite rides with the lead and zinc.",
        "Oxide to metal") + pages_of(
        "Electrolysis, for the lights: a steel vat heated by a blaze burner beneath it, two electrode holders with copper electrodes wired to the grid. One fluoride and two oxide give two ingots in five seconds; the fluoride is the molten salt bath, the oxide is what is reduced. "
        "Calciothermic reduction, for the heavies and yttrium: two fluoride and two calcium ingots under 250 mB of argon in a heated vat give two ingots and the fluorspar back as slag. "
        "Lanthanothermic distillation, for samarium, europium, thulium and ytterbium, which boil: two oxide and two lanthanum ingots under argon give two ingots and lanthanum oxide to go round again.") + pages_of(
        "Argon is spun out of 1,000 mB of air in a centrifuge vat, nine millibuckets at a time. Calcium is two limesand and 500 mB of hydrochloric acid on electrodes."), 6)
    entry("rare_earths", "uses", "What they are for", "fundamentals:neodymium_iron_boron_ingot", pages_of(
        "Nothing in the chain is for its own sake. Neodymium (or didymium) with iron, borax and a little dysprosium, superheated, sinters into NdFeB, the strongest magnet; samarium with cobaltite into SmCo, which keeps its field hot. "
        "Polarized, either is the magnet The Factory Must Grow's motors, generators and electric pumps are built from.", "What they are for") + pages_of(
        "Lanthanum metal reduces the four that boil, and lanthanum oxide is the catalyst that cracks naphtha. Cerium with iron is ferrocerium, the lighter flint, a flint and steel that never wears out. "
        "Europium's red and terbium's green on a yttria host are the phosphor every lamp takes. Yttria lines the fireproof vat. Didymium glass is the welder's lens the goggles are made of. Erbium turns glass pink. "
        "Scandium in aluminium is the airframe alloy, and makes a panel rack go twice as far. Gadolinium waits for a reactor."), 7)
    entry("rare_earths", "waste", "Waste", "fundamentals:monazite_residue_dust", pages_of(
        "A separation plant makes two kinds of waste, and both have to go somewhere. Every cut leaves a fifth of a batch of spent chloride liquor, "
        "acid with everything the organic did not want dissolved in it. It collects in a sump under the head stage; when the sump is full the battery stops, "
        "and the goggles say so. Pump it out through the head stage's underside.", "Waste") + [
        spotlight("fundamentals:salt", "Two limesand in 1,000 mB of spent liquor in a basin under a mixer neutralise it to brine, which is harmless, and 1,000 mB of brine boiled in a heated basin leaves three salt. "
                  "The salt goes back into the clay leach: the plant's waste water closes its own loop, as the real ones are made to.", "Lime and brine"),
        spotlight("fundamentals:monazite_residue_dust", "Monazite carries thorium. When the light concentrate dissolves, the thorium stays behind as a residue, mildly radioactive and good for nothing here. "
                  "Nine pack into a block; cast it and bury it deep, away from where you live. Bastnäsite and the clay leave none.", "Residue")], 8)
    entry("rare_earths", "acids", "The acids", "fundamentals:hydrochloric_acid_bucket", pages_of(
        "The plant runs on acid, and acid is not a texture. Each one can be bucketed and poured, and does in the world what it does in the bottle. "
        "Stand in any of them and it burns; hydrofluoric acid also poisons. Pour one against a block it attacks and the block cracks as if being mined, "
        "fizzes for five seconds, and is gone, and the acid that ate it is spent.", "The acids") + pages_of(
        "Hydrochloric acid eats carbonates: calcite, limestone, dripstone, bone, tuff. Hydrofluoric acid eats glass and silica: glass, sand, sandstone, quartz; "
        "it is the one acid glass cannot hold. Nitric acid eats copper and iron. Phosphoric acid only stings, which is why it is in your cola. Stone, deepslate and the vats shrug all of them off."), 9)


def metals():
    category("metals", "The other metals", "Cobalt, and the porphyry chain: copper, molybdenum and the rhenium hiding in it.", "fundamentals:cobalt_ingot", 3)
    entry("metals", "cobalt", "Cobalt", "fundamentals:cobalt_ingot", pages_of(
        "Cobaltite is a cobalt arsenide-sulfide from the silver-cobalt veins in calcite. Roast it on a campfire or in a smoker to drive off the arsenic and sulfur, "
        "then blast the roasted ore to the metal. Four cobalt and a samarium make SmCo; two cobalt, four nickel and a rhenium make the superalloy. "
        "Roasted cobaltite calcined with two bauxite powder is cobalt blue, four blue dye.", "Cobalt"), 0)
    entry("metals", "porphyry", "Copper, molybdenum, rhenium", "fundamentals:raw_molybdenite", pages_of(
        "A porphyry copper stock carries chalcopyrite with a little molybdenite, and the molybdenite carries rhenium at parts per million. "
        "Chalcopyrite roasted on a fire becomes a copper oxide the bloomery smelts to copper, the iron going to slag. "
        "Two raw molybdenite roasted in a heated basin give two molybdenum trioxide, and half the time a rhenium flue dust: "
        "the roaster's flue is where every gram of the world's rhenium comes from.", "The porphyry chain") + pages_of(
        "Both oxides are reduced under hydrogen in a heated chemical vat with an industrial mixer, as the industry does: two trioxide and 500 mB of hydrogen give two molybdenum ingots, "
        "two flue dust and 250 mB give one rhenium ingot. Hydrogen is The Factory Must Grow's."), 1)
    entry("metals", "superalloy", "Superalloy and molybdenum steel", "fundamentals:superalloy_ingot", pages_of(
        "Four nickel, two cobalt and one rhenium, superheated, make four ingots of the nickel superalloy that turbine blades are cast from; "
        "The Factory Must Grow's turbine blade now takes its plates. One molybdenum in four steel makes molybdenum steel, and its plates now make the heavy machinery casing.",
        "Superalloy and molybdenum steel"), 2)


def power():
    category("power", "Power", "Sunlight into The Factory Must Grow's grid.", "fundamentals:photovoltaic_panel", 4)
    entry("power", "solar", "Solar panels", "fundamentals:photovoltaic_panel", pages_of(
        "A solar panel goes on a rack. Build the rack from steel and set it down facing the way you want, then mount a photovoltaic panel on it: a P and an N semiconductor from The Factory Must Grow under glass, in an aluminium frame. "
        "Under open sky it feeds the electrical network, 120 volts while the sun is up and up to 200 watts at noon, nothing at night or in shade. Break it and you get the rack and the panel back.", "Solar panels")
        + [crafting("fundamentals:panel_rack"), crafting("fundamentals:photovoltaic_panel")], 0)


def book():
    write(BOOK_DATA / "book.json", {
        "name": "Fundamentals: First Principles",
        "landing_text": "Real minerals, real deposits, and the real road from rock to metal. $(br2)Everything here is as it is in the ground and in the plant; where the book quotes a number, the game was read for it.",
        "version": "1", "creative_tab": "fundamentals:minerals", "use_resource_pack": True, "show_progress": False,
        "book_texture": "patchouli:textures/gui/book_brown.png", "model": "patchouli:book_brown",
    })
    write(RECIPES / "first_principles.json", {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "patchouli"}],
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "minecraft:book"}, {"item": "fundamentals:raw_hematite"}],
        "result": {"id": "patchouli:guide_book", "components": {"patchouli:book": f"fundamentals:{BOOK}"}}})


def check():
    """Every recipe a page shows must exist."""
    missing = []
    for path in BOOK_ASSETS.glob("entries/*/*.json"):
        for page in json.loads(path.read_text())["pages"]:
            if page["type"] == "patchouli:crafting":
                rid = page["recipe"].split(":")[1]
                if not (RECIPES / f"{rid}.json").exists():
                    missing.append(page["recipe"])
    if missing:
        raise SystemExit(f"book shows recipes that do not exist: {missing}")


def main():
    shutil.rmtree(BOOK_DATA, ignore_errors=True)
    shutil.rmtree(BOOK_ASSETS.parent, ignore_errors=True)
    book()
    geology()
    ironworking()
    rare_earths()
    metals()
    power()
    check()
    n = len(list(BOOK_ASSETS.glob("entries/*/*.json")))
    print(f"book written: {n} entries")


if __name__ == "__main__":
    main()
