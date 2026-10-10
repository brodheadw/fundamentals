#!/usr/bin/env python3
import json
import re
import shutil

from common import ASSETS, DATA, JAVA, write
from build_ore_data import DEPOSITS, DISPLAY, PLACERS
from build_separation_data import CURIE, MOMENTS, TITANIUM_PIPEWORK, ZINC_REDUCTION
from build_oxidation_data import METALS as AGEING
from build_heat_data import REFERENCES, liquids

BOOK = "first_principles"
BOOK_DATA = DATA / f"patchouli_books/{BOOK}"
BOOK_ASSETS = ASSETS / f"patchouli_books/{BOOK}/en_us"
RECIPES = DATA / "recipe"

PAGE_CHARS = 330
NUMBERS = dict(enumerate(("zero one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen").split()))

WHERE = {
    "anywhere": "anywhere in the overworld", "porphyry": "under mountains and hills", "arid_oxide": "in badlands, savanna and desert",
    "laterite": "under jungle, savanna and mangrove", "pegmatite": "in mountains, hills and badlands", "carbonatite": "under mountains and badlands",
    "alkaline": "under taiga and snowy country", "ion_clay": "under jungle", "wetland": "in swamps and bogs", "hydrothermal": "in mountain and hill country",
    "placer": "in beach and river sand", "temperate": "under temperate forest, plains, hills and swamp", "alpine": "in the mountains",
}
STYLE = {"pockets": "masses", "seams": "layers", "disseminated": "scattered grains", "top": "an enriched top"}
KIND = {"bed": "a bed", "plug": "a plug", "blanket": "a blanket just under the surface", "vein": "a vein"}


LANG = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))


def pretty(id):
    key = id.split(":")[-1]
    name = LANG.get(f"fluid_type.fundamentals.{key}") or LANG.get(f"item.fundamentals.{key}") or LANG.get(f"block.fundamentals.{key}")
    if name is None:
        return DISPLAY.get(key, key.replace("_", " ")).lower()
    return name if name.isupper() else name.lower()


def pages_of(text, title=None):
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


def cuts():
    out = {}
    for path in sorted((RECIPES / "separation").glob("*.json")):
        d = json.loads(path.read_text())
        out[d["liquor"]] = d
    return out


def range_of(gauge):
    java = (JAVA / "heat/Thermometer.java").read_text(encoding="utf-8")
    low, high = re.search(rf'"{gauge}", (-?\d+), (-?\d+)', java).groups()
    return f"{int(low):,} to {int(high):,} °C"


def magnetic_cuts():
    return {d["liquor"]: d for d in (json.loads(p.read_text()) for p in sorted((RECIPES / "magnetic").glob("*.json")))}


def susceptibility(element, celsius):
    share = CURIE.get(element, 1.0)
    return round(100 * (share * 293 / (celsius + 273.15) + 1 - share))


def hazard(name):
    java = (JAVA / "separation/Hazards.java").read_text(encoding="utf-8")
    value = re.search(rf"{name} = ([0-9./ ]+);", java).group(1)
    return round(eval(value)) if "/" not in value else round(1 / eval(value) / 20)


def roads(tree):
    roads = {}

    feed, _, *reduced = ZINC_REDUCTION

    def walk(liquor, path):
        if liquor == f"fundamentals:{feed}":
            for product in reduced:
                roads[f"fundamentals:{product}"] = path + [(0, "zinc")]
            return
        if liquor not in tree:
            roads[liquor] = path
            return
        cut = tree[liquor]
        for side in ("light", "heavy"):
            walk(cut[side], path + [(cut["stages"], pretty(cut["organic"]))])

    walk("fundamentals:rare_earth_liquor", [])
    return roads


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
    entry("geology", "salt", "Salt and seawater", "fundamentals:seawater_bucket", pages_of(
        "The sea is salt and the rivers are not. Water drawn from a source in an ocean or off a beach, by bucket, hose pulley or a pump at an open pipe, "
        "comes up as seawater; from a river, a lake, a swamp or a cave it is fresh water as ever. Poured out it is water again. "
        "In a pipe its salt tells, though slowly: copper takes seawater well enough that cupronickel is the sea's standard pipe, and plain copper "
        "goes in an hour or so, so an intake that runs for long wants plastic or titanium.", "Salt and seawater") + pages_of(
        "Fresh water boils away to nothing: salt is made from seawater. 1,000 mB boiled down in a heated basin leaves two salt and 100 mB of bittern, "
        "the bitter mother liquor left once the halite has crystallised, rich in magnesium chloride, and gives back the steam as 900 mB of fresh water: "
        "a salt works' evaporator and a desalination plant are one machine. 500 mB of bittern boiled down gives a magnesium chloride, "
        "the titanium plant's magnesium. Or dig it: halite, rock salt, lies in seams in the desert evaporite beds with the borax and trona, "
        "and a millstone or crushing wheels grind a raw halite to two salt. Bittern is a strong chloride and eats copper pipe like the liquors.") + pages_of(
        "Seawater will not raise steam. A boiler takes fresh water only, as real ones take it demineralised: salt would scale the tubes and eat them. "
        "Nor does it water crops. Seawater is a lixiviant, though: four ion-adsorption clay in 1,000 mB of it under a mixer leach to 250 mB of crude heavy liquor, "
        "twice the water the salt-and-water leach takes, being half as strong.") + pages_of(
        "Bittern holds the sea's bromide. 1,000 mB of it and 20 mB of chlorine in a heated vat with a mixer give 20 mB of bromine, "
        "the chlorine taking the bromide's place, and the bittern's two magnesium chloride. That is still a hundred times what the sea really gives, 65 milligrams a litre. "
        "Bromine is a dark red-brown liquid that boils at 59 °C: it fumes and poisons like hydrofluoric acid, burns what stands in it, and eats copper, iron and aluminium. "
        "Unlike the acids it attacks polyethylene too, so a plastic pipe or tank of it fails as copper does under acid; real plants hold it in glass-lined and lead-lined steel. "
        "Ten millibuckets of it in a quartz envelope make a halogen lamp: a tungsten filament, a quartz, a molybdenum ingot, three steel nuggets "
        "and 10 mB of bromine under a mixer make four light bulbs. The quartz is sealed round molybdenum foil, the one metal whose expansion lets the seal hold, "
        "and the filament stands on molybdenum supports (Ullmann's, Molybdenum).", "Bromine") + [
        spotlight("fundamentals:raw_halite", "Halite is sodium chloride, glassy cubes that are colourless when pure and pink or orange where salt-loving microbes or a trace of iron stained it.", "Halite")], 91)


def ironworking():
    category("ironworking", "By hand", "Iron, copper and lead the way they were won for three thousand years, before any machine.", "fundamentals:smithing_hammer", 1)
    entry("ironworking", "iron_ores", "The iron ores", "fundamentals:raw_siderite", pages_of(
        "Four minerals carry the world's iron. Hematite, Fe2O3, is 70 per cent iron and red however dark it looks; it makes the great banded beds. "
        "Magnetite, Fe3O4, is 72 per cent, black and magnetic, in beds deep down. Goethite, FeO(OH), is the yellow-brown bog iron that collects "
        "under swamps. Siderite, FeCO3, is iron carbonate, 48 per cent iron, pale tan to yellow-brown where fresh and brown where it has weathered.", "The iron ores") + pages_of(
        "Siderite fed Europe's iron for centuries. The Weald of Sussex and Kent smelted its clay ironstone from Roman times through the Middle Ages "
        "and was England's iron country until the eighteenth century (Cleere and Crossley, The Iron Industry of the Weald, 1985). The Coal Measures "
        "carry it as clay ironstone nodules and as blackband, ironstone beds with their own coal in them, which David Mushet found in Scotland in 1801 "
        "and which fed Staffordshire and Lanarkshire. Erzberg in Styria is a mountain of it, mined since the Middle Ages and still worked in terraces.") + pages_of(
        "Here it lies in two kinds of ground: beds in the coal measures under temperate country, layers and nodules of it among seams of coal, "
        "and sparry iron, massive siderite in limestone, in the mountains. It is low in sulfur, its phosphorus varies from bed to bed, and it often "
        "carries manganese, which made Styrian steel prized.") + [
        spotlight("fundamentals:roasted_siderite", "A carbonate must be roasted before it smelts: at about 500 °C FeCO3 gives up its carbon dioxide and leaves FeO, "
                  "which the air takes on to Fe2O3 and Fe3O4. Roast raw siderite on a campfire or in a smoker. The bloomery refuses it raw; "
                  "roasted, it blooms like any other iron ore (Tylecote, A History of Metallurgy).", "Roasting siderite")], 0)
    entry("ironworking", "bloomery", "The bloomery", "fundamentals:bloomery", pages_of(
        "Build a bloomery out of clay, load it with iron ore and charcoal, light it with a torch and wait. Only charcoal: the sulfur in coal "
        "ruins iron. What comes out is not an ingot but a bloom, a spongy lump of iron and slag that never melted. Siderite goes in roasted.", "The bloomery")
        + [crafting("fundamentals:bloomery")], 1)
    entry("ironworking", "bloom", "Bloom and hammer", "fundamentals:iron_bloom", pages_of(
        "A bloom is hammered, not cast. Beat it with the smithing hammer to drive the slag out and weld the iron together into wrought iron. "
        "Copper comes straight out of the bloomery from malachite, azurite and cuprite. Galena has to be roasted on a fire first, which "
        "drives off its sulfur; the roasted ore then gives lead bullion, lead still holding its silver, which a furnace remelts to lead.", "Bloom and hammer") + pages_of(
        "A furnace never reduces iron ore, and neither does a fan. Create's crushing wheels grind hematite, magnetite, goethite and roasted siderite to crushed iron ore, "
        "and that goes to The Factory Must Grow's blast furnace, which has the coke and the heat. Crimsite is the iron stone and crushes to hematite; "
        "washed gravel leaves a little magnetite black sand. The slag every smelt leaves is The Factory Must Grow's own, which its concrete and asphalt take as aggregate, as the world's slag goes to cement and roads.")
        + [crafting("fundamentals:smithing_hammer")], 2)
    entry("ironworking", "mortar", "Mortar and pestle", "fundamentals:mortar_and_pestle", pages_of(
        "A mortar and pestle grinds by hand what Create's millstone grinds by power: grain to flour, bone to meal, and coloured minerals "
        "to the pigments painters have ground since antiquity: hematite red, goethite yellow, malachite green, azurite blue, pyrolusite black, "
        "cinnabar vermilion. Hold the mortar in one hand and the material in the other.", "Mortar and pestle")
        + [crafting("fundamentals:mortar_and_pestle")], 3)


def rare_earths():
    tree = cuts()
    category("rare_earths", "The rare earths", "Sixteen metals that travel together and must be parted one by one. The longest road in the mod.",
             "fundamentals:neodymium_ingot", 2)
    entry("rare_earths", "minerals", "The minerals", "fundamentals:raw_bastnasite", pages_of(
        "The rare earths are not rare, only mixed. Bastnäsite in carbonatite plugs and monazite carry the light ones: monazite in beach sand, in the weathered top of a carbonatite and in rare quartz veins, "
        "lanthanum to samarium. Xenotime and euxenite in pegmatite veins carry the heavy ones and yttrium, and now and then a greenish-black prism of thortveitite, the one scandium mineral. Loparite under cold country "
        "carries the lights too. Ion-adsorption clay under jungle is the odd one: a clay with the heavy rare earths merely stuck to it, "
        "which is why it is leached and never roasted.", "The minerals") + [
        spotlight("fundamentals:light_rare_earth_concentrate", "A millstone or crushing wheels grind bastnäsite, xenotime, loparite and euxenite; monazite is a sand already. "
                  "Washed under an encased fan, the light grains wash away and about half of what you fed it stays as a mixed concentrate: light from monazite and loparite, heavy from xenotime and euxenite.", "Concentrate"),
        spotlight("fundamentals:bastnasite_dust", "Bastnäsite will not wash: it is floated, as in every carbonatite mill. Two bastnäsite dust beaten with 250 mB of water and 100 mB of naphthenic acid, the fatty-acid collector, "
                  "in a basin under a mixer, and the rare earth carbonate comes off the top as bastnäsite concentrate, one and sometimes two.", "Flotation")], 0)
    entry("rare_earths", "liquor", "Liquor", "fundamentals:salt", [
        spotlight("fundamentals:light_rare_earth_sulfate", "Monazite, xenotime and the rest are phosphates hydrochloric acid barely touches, so they are cracked first: a concentrate roasted with 250 mB of sulfuric acid in a heated basin "
                  "gives a rare earth sulfate and frees 125 mB of phosphoric acid. It is the hot roast of Baotou, 500 to 800 °C, and that hot the thorium turns to a pyrophosphate no water dissolves.", "Cracking"),
        spotlight("fundamentals:monazite_residue_dust", "The sulfate leaches in cold water, since rare earth sulfates dissolve worse hot, and the thorium stays behind as a residue; xenotime and euxenite leave one half the time. "
                  "Bake it cooler, at 200 to 300 °C as the old monazite plants did, and the thorium dissolves with the rare earths.", "Leaching"),
        spotlight("fundamentals:light_rare_earth_carbonate", "A sulfate does not turn to a chloride in hydrochloric acid, so the plant goes by way of the carbonate: a sulfate, a soda ash and 500 mB of water under a mixer "
                  "give a rare earth carbonate and the residue. The carbonate fizzes away its carbon dioxide in 500 mB of hydrochloric acid to 500 mB of crude liquor.", "Carbonate"),
        spotlight("fundamentals:cerium_concentrate", "Bastnäsite is a carbonate and needs no acid bake: roast its concentrate on a campfire or in a smoker. Roasting in air takes its cerium to Ce(IV), which hydrochloric acid will not touch, "
                  "so the roasted ore in 250 mB of hot acid gives 250 mB of crude liquor and leaves a cerium concentrate, which Mountain Pass sold as it was. A blast furnace calcines it to cerium oxide.", "Roasting"),
        spotlight("fundamentals:clarifier_sludge", "Crude liquor still carries iron, aluminium and fines, and a battery fed it will run three cuts and then foul: crud at the interface, the organic turned brown and useless. "
                  "So clarify first: 1,000 mB of crude liquor and two limesand under a mixer drop the impurities as a sludge and leave 1,000 mB of liquor a battery wants. "
                  "A fouled organic is not lost: drained and scrubbed with one limesand, 1,000 mB gives back 900.", "Clarifying"),
        spotlight("fundamentals:raw_ion_adsorption_clay", "The clay needs no acid and no heat: four clay, a salt and 500 mB of water in a basin under a mixer leach straight to 250 mB of heavy liquor, and the clay comes back as clay. "
                  "That is the whole reason Chinese clays supply the world's heavy rare earths.", "Leaching")] + pages_of(
        "Salt is the old lixiviant, sodium chloride in heaps in the 1970s. Ammonium sulfate replaced it, and is now pumped into the hillside through boreholes and the liquor caught below: in-situ leaching. "
        "It is cheap and it is ruinous. The ammonium soaks into the groundwater and the streams, the soaked slopes slide, and Ganzhou in Jiangxi has spent billions of yuan cleaning up after it. "
        "Sources: Chi and Tian, Weathered Crust Elution-Deposited Rare Earth Ores (2008); Yang and others, Environmental Development (2013). "
        "The cracking, the carbonate and bastnäsite's ceria are as Gupta and Krishnamurthy give them in Extractive Metallurgy of Rare Earths (2005), and Habashi in his Handbook of Extractive Metallurgy (1997)."), 1)
    entry("rare_earths", "mixer_settler", "Solvent extraction", "fundamentals:mixer_settler", pages_of(
        "Chlorides of neighbouring rare earths are almost identical, so no one step parts them. Instead the liquor is shaken with an "
        "organic extractant (P507, P204 or naphthenic acid, cut with kerosene and saponified with lime as it is made up, which sets the pH the cut works at) that prefers the heavier ions "
        "by a hair, left to settle into two layers, and the two layers sent opposite ways through a long line of identical stages. "
        "Each stage enriches a little; thirty of them compound a hair into a clean split. The stages are mixer-settlers, and a line of them is a battery.",
        "Solvent extraction") + pages_of(
        "Place casings facing the same way and they merge into a vat, three by three up to three across, three along and two tall; "
        "nine casings is a lab vat, eighteen a plant vat. The back row is the mixing box, with Create's Mechanical Mixer standing over its hatch; "
        "the rows ahead are the settling bay, parted from the box by a weir that only the churn tops. "
        "Charge every stage with the extractant from a pipe into its top: it floats and is never used up. Pump the liquor into the back of "
        "the first stage and hydrochloric acid into the front of the last, take the raffinate from the first stage's sides and the loaded strip "
        "from the last stage's, and throw the lever. Goggles on any stage tell you what the battery is waiting for. "
        "The casing is plastic sheet or stainless steel plate, both of which the acid leaves alone.") + pages_of(
        "Two things the plant will not forgive. A mixer over 128 rpm beats the phases into an emulsion that never settles, and the battery stops until it is slowed. "
        "And the organic is not quite immortal: every cut carries a little of it out entrained in the raffinate, two per cent of a batch, so a plant wants a trickle of fresh extractant forever.")
        + [crafting("fundamentals:mixer_settler")], 2)
    cut_pages = []
    for liquor, cut in sorted(tree.items(), key=lambda kv: kv[1]["stages"]):
        cut_pages += pages_of(f"{pretty(liquor).capitalize()} parts into {pretty(cut['light'])} (raffinate, head end) and {pretty(cut['heavy'])} "
                              f"(strip, tail end) in a battery of {cut['stages']} stages charged with {pretty(cut['organic'])}, stripped by {pretty(cut['strip'])}. "
                              f"Each batch comes out {round(cut.get('light_fraction', 0.5) * 100)} per cent raffinate.")
    count = NUMBERS[len(tree)]
    feed, salt, europium, gadolinium = ZINC_REDUCTION
    zinc = json.loads((RECIPES / f"mixing/{salt}.json").read_text())
    batch = next(i["amount"] for i in zinc["ingredients"] if i.get("fluid") == f"fundamentals:{feed}")
    acid = next(i["amount"] for i in zinc["ingredients"] if i.get("fluid") == "tfmg:sulfuric_acid")
    left = next(r["amount"] for r in zinc["results"] if r["id"] == f"fundamentals:{gadolinium}")
    chance = next(r["chance"] for r in zinc["results"] if r["id"] == f"fundamentals:{salt}")
    entry("rare_earths", "cuts", f"The {count} cuts", "fundamentals:neodymium_oxalate", pages_of(
        f"{count.capitalize()} cuts take the mixed liquor down to single elements. A battery too short for its cut does nothing; the goggles say how many stages it wants. "
        "The number is set by how alike the pair is: samarium leaves neodymium in eight, but neodymium from praseodymium takes thirty-two. "
        "A cut does not halve its liquor: a batch comes out light and heavy as the ore carries them. Monazite is nine parts lights to one of the rest, "
        "and the clay's heavy liquor two-thirds yttrium, so europium and terbium come out a trickle.",
        f"The {count} cuts") + pages_of(
        "Europium is not cut from gadolinium at all, though P507 would do it in eighteen stages. It alone of the rare earths goes to a 2+ ion, and zinc takes it there. "
        f"{batch:,} mB of {pretty(feed)}, a zinc nugget and {acid} mB of sulfuric acid under a mixer give {left} mB of {pretty(gadolinium)}, and the europium drops "
        f"as a white europium sulfate one batch in {NUMBERS[round(1 / chance)]}. Heated in 250 mB of nitric acid it goes back to 250 mB of {pretty(europium)}. "
        "That is McCoy's method of 1935, and still how europium is parted.") + cut_pages, 3)
    magnetic = magnetic_cuts()
    moments = ", ".join(f"{e} {m:g}" for e, m in MOMENTS.items())
    magnetic_pages = []
    for liquor, cut in sorted(magnetic.items(), key=lambda kv: kv[1]["passes"]):
        drawn = cut["attracted"]
        rest = cut["heavy"] if drawn == cut["light"] else cut["light"]
        magnetic_pages += pages_of(f"{pretty(liquor).capitalize()}: {cut['passes']} cells, where its battery wants {tree[liquor]['stages']} stages. "
                                   f"The magnets draw {pretty(drawn)}; {pretty(rest)} runs on past them.")
    without = [pretty(liquor) for liquor in tree if liquor not in magnetic]
    entry("rare_earths", "magnetic", "Magnetic separation", "fundamentals:magnetomigration_cell", pages_of(
        "Neighbouring rare earth ions are chemical near-twins, which is why a battery needs dozens of stages. Magnetically they are not: "
        "their partly filled 4f shells give them wildly different moments. A permanent magnet's field gradient pulls the strongly paramagnetic ions "
        "through the liquor toward it and leaves the diamagnetic ones behind, with no organic, no acid and no power. "
        "It is new and lab-stage: PNNL showed it in 2026, KU Leuven measured the migration following the susceptibility, and no plant runs it yet.",
        "Magnetic separation") + pages_of(
        f"Effective moments of the ions, in Bohr magnetons: {moments}. Yttrium, lanthanum and lutetium have no unpaired 4f electron and nothing pulls them. "
        "A cut can go magnetic only where its two products sort by moment, one side carrying a tenth of the other's susceptibility or less. "
        "The passes it needs go as one over the moment the contrast is worth: yttrium from the late heavies is quick, lanthanum from cerium (2.5) slow.") + pages_of(
        "Cells placed end to end facing the same way are a line, one pass each. Pipe the liquor into the back of the first; the last cell parts it, "
        "what the magnets drew out of its right side, where the NdFeB block is, and the rest out of its left. The products and their proportion "
        "are the battery's, so either route feeds the next cut. The cell is plastic or stainless steel, since the chloride liquor eats copper. "
        "Goggles on any cell say how many passes the cut wants and what the line is waiting for.") + pages_of(
        "Cold helps. A paramagnetic ion's susceptibility follows Curie's law, one over the absolute temperature: the colder the liquor, the less "
        "the jostling of the heat undoes the field's lining up of the moments, and the harder the magnets draw. The head cell reads the liquor's "
        f"temperature, and a batch takes as much longer as the cut's contrast falls: at 0 °C the ions are {susceptibility('Nd', 0)} per cent as "
        f"magnetic as at 20, at 80 °C {susceptibility('Nd', 80)}, and the goggles give the figure and the time a batch takes.", "Cold and heat") + pages_of(
        "Samarium and europium are the exceptions. Their first excited levels lie so close above the ground level that the field mixes them in, "
        "a susceptibility Van Vleck explained in 1932 that does not care for temperature; europium's ground level has no moment at all. "
        f"So samarium at 80 °C is still {susceptibility('Sm', 80)} per cent of itself at 20, and europium {susceptibility('Eu', 80)}. "
        "Cotton's Lanthanide and Actinide Chemistry gives the moments and why.") + magnetic_pages + pages_of(
        f"The other {len(without)} cuts have no magnetic route, because their two products do not sort by moment. They are {', '.join(without)}. "
        "Praseodymium and neodymium are both 3.6, dysprosium and holmium both 10.6, terbium 9.7 and erbium 9.6 beside them; "
        "and the broad cuts carry strong and weak ions on both sides, yttrium at nothing among the strongly magnetic heavies, gadolinium at 7.9 beside samarium at 1.5. "
        "Those are solvent extraction's alone.") + [crafting("fundamentals:magnetomigration_cell")], 4)
    road_pages = []
    for liquor, path in sorted(roads(tree).items(), key=lambda kv: sum(s for s, _ in kv[1])):
        total = sum(s for s, _ in path)
        legs = ", then ".join("zinc in a basin" if o == "zinc" else f"{s} stages with {o}" for s, o in path)
        road_pages += pages_of(f"{pretty(liquor).replace(' liquor', '').capitalize()}: {sum(1 for s, _ in path if s)} batteries, {total} stages in all. {legs}.")
    entry("rare_earths", "roads", "The road to each metal", "fundamentals:dysprosium_ingot", pages_of(
        "From the mixed liquor to one element is a chain of batteries, each fed by the one before. These are the roads, shortest first. "
        "The heavy liquor can also be leached straight from the clay, which skips the first eight stages for everything on the heavy side.",
        "The road to each metal") + road_pages, 5)
    entry("rare_earths", "oxide", "Oxalate and oxide", "fundamentals:neodymium_oxide", [
        spotlight("fundamentals:oxalic_acid", "A single-element liquor and oxalic acid in a basin under a mixer, two blocks below it with the whisk between, drop the oxalate: "
                  "250 mB of liquor and one oxalic acid to one oxalate. The oxalates are pale powders with the ion's cast: praseodymium green, neodymium lilac, erbium pink, most of them white.", "Oxalate"),
        spotlight("fundamentals:neodymium_oxide", "A blast furnace or a fan over lava calcines the oxalate to the oxide, the form rare earths trade in; it takes 800 to 1,000 °C, past a plain furnace. The oxides are the colours they really are: "
                  "lanthanum white, cerium pale yellow, praseodymium brown-black, neodymium blue-grey, terbium brown, erbium pink.", "Oxide")], 6)
    entry("rare_earths", "metal", "Oxide to metal", "fundamentals:neodymium_ingot", pages_of(
        "The metal comes out of the oxide three ways, all in The Factory Must Grow's chemical vats. The lights (lanthanum to neodymium, and didymium) and the heavies both go through their fluoride first: "
        "one oxide and 500 mB of hydrofluoric acid in a basin under a mixer. The acid itself is two raw fluorite and 500 mB of sulfuric acid, heated; fluorite rides with the lead and zinc.",
        "Oxide to metal") + pages_of(
        "Electrolysis, for the lights: a steel vat superheated by a blaze burner beneath it, two electrode holders with copper electrodes wired to the grid. One fluoride and two oxide give two ingots in five seconds; the fluoride is the molten salt bath and comes back nine times in ten, the oxide is what is reduced. "
        "Calciothermic reduction, for gadolinium, terbium, dysprosium and yttrium: two fluoride and two calcium ingots under 250 mB of argon in a superheated vat give two ingots and the fluorspar back as slag. "
        "Lanthanothermic distillation, for samarium, which boils: two oxide and two lanthanum ingots under argon give two ingots and lanthanum oxide to go round again. "
        "Europium and the heavies past dysprosium are sold as oxide, and nothing wants them as metal.") + pages_of(
        "Argon is spun out of 1,000 mB of air in a centrifuge vat, nine millibuckets at a time. Calcium is the plant's own calcium chloride, two of it electrolysed molten on electrodes in a heated vat to two ingots. "
        "A kindled blaze burner is 1,000 °C and one fed a blaze cake 1,600. The fluoride bath electrolyses at 1,000 to 1,100 °C, and the two metallothermic reductions run near 1,500, past where the fluorspar slag melts, so all three want the cake."), 7)
    entry("rare_earths", "scandium", "Scandium", "fundamentals:scandium_ingot", pages_of(
        "Scandium rides with the rare earths but never with their liquors: almost none of it is in bastnäsite or monazite. Its own mineral is thortveitite, "
        "a scandium silicate found as a few dark green prisms in a whole pegmatite, as at Iveland in Norway and in Madagascar. Grind it like the others.", "Scandium") + pages_of(
        "A silicate no acid opens, so it is chlorinated: two thortveitite dust, a coal coke, 500 mB of chlorine and 500 mB of water in a heated basin give 500 mB of scandium liquor. "
        "The silica leaves as silicon tetrachloride, which boils at 58 °C. From the liquor it is the heavies' road: oxalic acid to the oxalate, calcined white to scandia, "
        "hydrofluoric acid to the fluoride, and calcium under argon, superheated, to the metal.") + pages_of(
        "Al-Sc is two per cent scandium, and most scandium never becomes metal. A scandium fluoride stirred into four blocks of aluminium, superheated, gives four blocks of Al-Sc straight, the aluminium taking the fluorine "
        "and skimmed off as a slag: the master alloy is made that way. A scandium nugget and eight aluminium give eight ingots."), 7)
    entry("rare_earths", "uses", "What they are for", "fundamentals:neodymium_iron_boron_ingot", pages_of(
        "Nothing in the chain is for its own sake. Two neodymium, three iron and a ferroboron, superheated under 100 mB of argon, melt into four NdFeB, the strongest magnet. Dy-NdFeB, which keeps its field hot, is 8 per cent dysprosium as the EH grades are: three neodymium, a dysprosium, six iron and two ferroboron under 200 mB of argon make eight. "
        "SmCo, hotter still, is Sm2(Co,Fe,Cu,Zr)17: two samarium, four cobalt, an iron, four copper nuggets and two zirconium nuggets under 200 mB of argon make seven. "
        "Praseodymium or didymium serves as well as neodymium, and terbium as well as dysprosium. A gadolinium for a neodymium makes three for every four. "
        "Ferroboron is reduced from boric acid, not borax, whose sodium the melt would keep: a raw borax and 250 mB of sulfuric acid, heated, give two boric acid, and a boric acid, an iron and two charcoal, superheated, a ferroboron. "
        "Polarized, each is the magnet The Factory Must Grow's motors, generators and electric pumps are built from: see Magnets and heat, under Power.", "What they are for") + pages_of(
        "Lanthanum metal reduces samarium, and lanthanum oxide stabilises the zeolite of the catalyst that cracks heavy oil to gasoline and propylene. Cerium with iron is ferrocerium, the lighter flint, a flint and steel that never wears out. "
        "The phosphor every lamp takes is two of the tri-band tube's three: the red is yttria doped with europium, Y2O3:Eu (YOX), and the green lanthanum phosphate doped with cerium and terbium, LaPO4:Ce,Tb (LAP), "
        "the cerium taking up the ultraviolet and handing it to the terbium (Ullmann's Encyclopedia, Luminescent Materials). "
        "The ultraviolet is mercury's, a few milligrams of its vapour in every tube (3 to 5 mg under the EU's RoHS directive): the Factory's aluminium lamp and circular light take two phosphor and a mercury for every pair. Yttria lines the fireproof vat. Didymium glass is the welder's lens the goggles are made of. Erbium turns glass pink. "
        "Scandium in aluminium is the airframe alloy, and makes a panel rack go twice as far.") + pages_of(
        "Cerium oxide stores and releases oxygen, which is what a catalytic converter does: the Factory's exhaust takes two. Neodymium oxide turns glass purple and holmium oxide yellow, "
        "the way erbium turns it pink. A cobalt in the lithium charge is the lithium cobalt oxide cathode the first lithium cells ran on."), 8)
    entry("rare_earths", "waste", "Waste", "fundamentals:monazite_residue_dust", pages_of(
        "A separation plant makes two kinds of waste, and both have to go somewhere. Every cut leaves a fifth of a batch of spent chloride liquor, "
        "acid with everything the organic did not want dissolved in it. It collects in a sump under the head stage; when the sump is full the battery stops, "
        "and the goggles say so. Pump it out through the head stage's underside.", "Waste") + [
        spotlight("fundamentals:calcium_chloride", "Two limesand in 1,000 mB of spent liquor in a basin under a mixer neutralise it to calcium chloride liquor, and 1,000 mB of that boiled in a heated basin leaves three calcium chloride. "
                  "That is what calcium metal is electrolysed from: the plant's waste closes its own loop.", "Lime"),
        spotlight("fundamentals:monazite_residue_dust", "Monazite carries thorium. Roasted hot in sulfuric acid, the thorium turns to a pyrophosphate and stays behind when the sulfate is leached, a residue, mildly radioactive. "
                  "Nine pack into a block; cast it and bury it deep, away from where you live. Bastnäsite and the clay leave none.", "Residue"),
        spotlight("fundamentals:gas_mantle", "Thorium was the rare earth industry's first product. A residue and 250 mB of nitric acid, heated, give a thorium nitrate. Four nitrate, 10 mB of cerium liquor and four string, heated, "
                  "give four gas mantles: Welsbach's thoria with a hundredth part of ceria, glowing white in a gas flame. The Factory's gas lamp burns one.", "The gas mantle"),
        spotlight("fundamentals:clarifier_sludge_block", "The clarifier's sludge is the iron, aluminium and thorium the lime throws down as hydroxides. Nine pack into a block of tailings, for the dam.", "Tailings")], 9)
    entry("rare_earths", "acids", "The acids", "fundamentals:hydrochloric_acid_bucket", pages_of(
        "The plant runs on acid, and acid is not a texture. Each one can be bucketed and poured, and does in the world what it does in the bottle. "
        "Stand in any of them and it burns; hydrofluoric acid also poisons. Pour one against a block it attacks and the block cracks as if being mined, "
        "fizzes for five seconds, and is gone, and the acid that ate it is spent.", "The acids") + pages_of(
        "Hydrochloric acid eats carbonates: calcite, limestone, dripstone, bone. Hydrofluoric acid eats glass and silica: glass, sand, sandstone, quartz, tuff; "
        "it is the one acid glass cannot hold. Nitric acid eats copper and iron, and aqua regia, three of hydrochloric to one of nitric, eats gold as well. Phosphoric acid only stings, which is why it is in your cola. Stone, deepslate and the vats shrug all of them off.") + pages_of(
        "Concentrated hydrochloric acid, hydrofluoric acid, nitric acid and aqua regia fume. Within two blocks of any of them in the open, as a block or in a basin it is being used in, you take a hit a second and the world swims, and hydrofluoric poisons. "
        "The gas mask is Create's: a diving helmet over a filled copper backtank, which breathes its air instead. "
        "And acid eats copper: a Create pipe carrying any acid corrodes and, after a couple of minutes on average, bursts and spills it. The liquors are chlorides in dilute acid, and the spent liquor, the calcium chloride liquor and bittern are chloride too: they eat copper as well, more slowly, eight minutes or so to a pipe; seawater slower still, an hour. Concentrated nitric acid passivates aluminium, which is what it is shipped in, so the Factory's aluminium pipes carry it unharmed. Run the plant in The Factory Must Grow's plastic pipes, pumps and valves, which neither can touch, or in titanium (see Titanium pipework, under Plastics); the Factory's metal ones fare no better than copper, and a glass pipe is a copper pipe with a window. The organic, kerosene, is harmless. "
        "Tanks corrode too, ten times slower for the thicker wall: a copper or metal tank of acid loses a block of its wall in twenty minutes or so, and that block's share of what it held, the acid spilling; under a liquor, in eighty. "
        "Keep the acid and the liquors in the Plastic Fluid Tank, as real plants keep hydrochloric acid in fibreglass and polyethylene; but not bromine, which eats plastic.") + [crafting("fundamentals:plastic_fluid_tank")], 10)
    entry("rare_earths", "making_acids", "Making the acids", "fundamentals:nitric_acid_bucket", pages_of(
        "Sulfuric acid is The Factory Must Grow's, from sulfur and saltpetre in a vat, and every other acid starts from it. "
        "Two salt and 500 mB of sulfuric acid in a heated basin give 500 mB of hydrochloric acid, the salt-cake process; salt is seawater boiled down, or rock salt ground (see Salt and seawater, under Geology). "
        "Two raw fluorite and 500 mB, heated, give hydrofluoric acid. Two nitrate dust and 500 mB, heated, give nitric acid, which boils off the saltpetre as it did from Glauber's retort. "
        "Two bone meal and 500 mB, cold, give phosphoric acid, the wet process with bone for phosphate rock; cracking monazite frees more.", "Making the acids") + pages_of(
        "Oxalic acid is sugar oxidised by nitric acid, Scheele's route: two sugar and 250 mB of nitric acid, heated, give two oxalic acid. "
        "Chlorine is Scheele's too: a raw pyrolusite in 1,000 mB of hot hydrochloric acid gives 250 mB, the manganese staying behind as its chloride. "
        "That is the old way, and the chlor-alkali cell (next) gives twice as much from two salt. "
        "The molten-chloride electrolyses give it off as well, 500 mB with every two calcium and 250 with every lithium."), 11)
    entry("rare_earths", "chlor_alkali", "The chlor-alkali cell", "fundamentals:dimensionally_stable_anode", pages_of(
        "Nearly all the world's chlorine and all its caustic soda come out of one cell. Brine, salt saturated in water, is electrolysed across a membrane that lets only sodium through: "
        "chlorine comes off the anode, hydrogen and caustic soda, sodium hydroxide, off the cathode. Two salt in 1,000 mB of water under a mixer make 1,000 mB of brine. "
        "1,000 mB of brine in a chemical vat with two electrodes and a dimensionally stable anode give 500 mB each of chlorine, caustic soda and hydrogen, with no burner: "
        "the current keeps the cell near 90 °C. The anode comes back ninety-nine times in a hundred.", "The chlor-alkali cell") + pages_of(
        "The anode is what made the membrane cell. Graphite anodes wore away in the wet chlorine and fouled the cell; De Nora's dimensionally stable anode is titanium "
        "coated with ruthenium and iridium oxides, painted on as their chlorides and fired, and lasts for years. "
        "A titanium plate, a ruthenium nugget, an iridium nugget and 100 mB of hydrochloric acid, heated, make one.", "The anode") + pages_of(
        "Before the cell, caustic soda was soda ash boiled with lime, and Bayer's first plants ran on it: a soda ash, a limesand and 500 mB of water, heated, give 250 mB. "
        "Caustic soda leaves copper and steel alone, which is why it is shipped in steel, but it eats aluminium: The Factory Must Grow's aluminium pipes, pumps, valves and tanks "
        "corrode under it and under the aluminate liquor, as copper does under a liquor.", "Caustic soda") + pages_of(
        "Sources: O'Brien, Bommaraju and Hine, Handbook of Chlor-Alkali Technology (Springer, 2005); Ullmann's Encyclopedia of Industrial Chemistry, \"Chlorine\"; "
        "Trasatti, \"Electrocatalysis: understanding the success of DSA\", Electrochimica Acta 45 (2000).", "Sources"), 11)
    entry("rare_earths", "extractants", "The extractants", "fundamentals:white_phosphorus", pages_of(
        "P204 and P507 are both 2-ethylhexyl esters on one phosphorus atom, and the industry makes them from propylene and phosphate rock. So do you, in The Factory Must Grow's chemical vats with an industrial mixer unless a step says otherwise. "
        "First water gas: a coal coke and 500 mB of water, heated, give 1,000 mB of carbon monoxide and hydrogen. Shifted with another 500 mB of water, heated, 1,000 mB of water gas gives 1,000 mB of hydrogen and 500 of carbon dioxide; "
        "that is also the hydrogen cobalt, molybdenum and tungsten are reduced under.", "The extractants") + pages_of(
        "The oxo process: 500 mB each of propylene, from cracked naphtha, water gas and hydrogen on a cobalt ingot, heated, give 250 mB of 2-ethylhexanol. The cobalt is the catalyst and comes back nineteen times in twenty. "
        "Phosphorus: two bone meal, a coal coke and a sand in a vat with two electrodes, superheated, give a white phosphorus and a slag, as the electric furnace does at 1,500 °C. "
        "A white phosphorus and 750 mB of chlorine in a heated basin give 500 mB of phosphorus trichloride.") + pages_of(
        "P204 is D2EHPA: 500 mB of 2-ethylhexanol, 250 of phosphorus trichloride, 500 of air and 250 of water, cold, give 250 mB of it, the air taking the phosphorus to phosphate. "
        "P507 is EHEHPA: the same without the air, heated, which rearranges the phosphite to a phosphonate, carbon bonded to phosphorus. Both give 500 mB of hydrochloric acid back. "
        "Neat, they are too thick to use: 250 mB with 750 mB of kerosene and a limesand under a mixer make 1,000 mB of P204 or P507.") + pages_of(
        "Naphthenic acid is petroleum's own, washed out of the oil with caustic soda as sodium soaps and freed with acid: 1,000 mB of heavy oil, 250 mB of caustic soda and 250 mB of sulfuric acid in a heated vat with a mixer give 500 mB."), 12)
    entry("rare_earths", "temperature", "Temperature", "minecraft:campfire", pages_of(
        "Every block has a temperature. The biome gives the climate: tundra about -5 °C, taiga 1, plains 15, jungle 26, desert 30, cooler with altitude the way vanilla decides where snow lies. "
        "Under open sky the day swings it five degrees either way and rain and thunder take a few off. Then everything hot or cold within reach adds its share with distance: lava at 1,150 °C, fire and a campfire at 800, a lit furnace 750, "
        "a blast furnace 1,500, a bloomery 1,200, a blaze burner at whatever level it burns, and ice and snow the other way. That is inside; walls hold most of it in, so a step from a furnace is hot, not a kiln.", "Temperature") + pages_of(
        "Type /heat to read it where you stand; goggles on a stage read it there. Acid eats twice as fast for every ten degrees warmer. The three heats the recipes ask for, none, a burner (1,000 °C) and a burner fed a blaze cake (1,600 °C), are the coarse version of the same number."), 13)
    entry("rare_earths", "thermometers", "Thermometers", "fundamentals:type_k_thermocouple", pages_of(
        "A thermometer is a dial gauge built like Create's speedometer, what does the sensing standing over an andesite casing. Mount one on the face of a block and it reads that block: a furnace's wall, a vat, a burner. "
        "The needle swings bottom left to bottom right across its scale, goggles give the number, and a comparator reads 0 to 15 across the scale. Each kind reads over the range its material allows.", "Thermometers") + pages_of(
        f"Mercury in glass, {range_of('mercury_thermometer')}: mercury freezes at -38.8 °C and boils at 356.7. Past the top the column boils and bursts its glass, "
        "leaving a little mercury and a breath of its vapour, which poisons anyone near and unmasked. Mercury is roasted out of cinnabar in air, HgS + O2 giving Hg and SO2, the vapour condensed: "
        "a raw cinnabar and 250 mB of air in a heated basin give a flask of mercury.") + pages_of(
        f"Spirit in glass, {range_of('spirit_thermometer')}: the classic red line, kerosene dyed red (others use ethanol, toluene or pentane). "
        "It is what replaced mercury in homes and schools: the EU took mercury out of fever and household thermometers from 2009 (Directive 2007/51/EC), and NIST stopped calibrating mercury thermometers in 2011. "
        "It is safer, but reads a shorter range, the spirit boiling long before mercury would, and it wets the glass, so a falling column leaves some behind and reads low until it drains. "
        "Past the top it boils and bursts its glass with a small pop, and there is nothing in it to poison anyone.") + pages_of(
        f"Bimetallic, {range_of('bimetallic_thermometer')}: a strip of brass on steel curls as it warms, the brass growing half again as fast, and turns the needle itself. Past 500 it pegs. "
        f"Type K, {range_of('type_k_thermocouple')}: chromel (nickel with a tenth of chromium) against alumel (nickel with 2 per cent each of aluminium and manganese and 1 of silicon) gives some 41 microvolts a degree, the everyday industrial thermocouple; past 1,260 the chromel oxidises and the reading drifts, so it pegs. "
        "Nine nickel and a chromium, superheated, give ten chromel; nine nickel and four aluminium nuggets nine alumel, the aluminium standing for the manganese and silicon as well.") + pages_of(
        f"Type S, {range_of('type_s_thermocouple')}: platinum with a tenth of rhodium against pure platinum gives only ten microvolts a degree but holds its calibration to the melting of steel, "
        "for blast furnaces and superheated vats. A rhodium nugget over a platinum nugget make it. The thermocouples wear their IEC colours: type K green, type S orange, the negative leg white.")
        + [{"type": "patchouli:crafting", "recipe": "fundamentals:thermometers/mercury_thermometer", "recipe2": "fundamentals:thermometers/spirit_thermometer"},
           {"type": "patchouli:crafting", "recipe": "fundamentals:thermometers/bimetallic_thermometer", "recipe2": "fundamentals:thermometers/type_k_thermocouple"},
           {"type": "patchouli:crafting", "recipe": "fundamentals:thermometers/type_s_thermocouple"}], 14)


def liquid_rows():
    groups = {}
    for id, (name, *figures) in liquids().items():
        groups.setdefault(json.dumps(figures[:8], sort_keys=True, default=sorted), []).append(name)
    rows = []
    for key, names in groups.items():
        density, viscosity, freezes, boils, flash, _, _, hazards = json.loads(key)
        name = names[0] if len(names) == 1 else f"{names[0]} and {len(names) - 1} like it"
        points = f"{figure(freezes)} to {figure(boils)}" if freezes is not None and boils is not None else \
            f"freezes {figure(freezes)}" if freezes is not None else f"boils {figure(boils)}" if boils is not None else ""
        marks = [f"flash {figure(flash)}"] if flash is not None else []
        marks += [h for h in ("toxic", "fuming", "corrosive") if h in hazards]
        cells = [figure(density), figure(viscosity) if viscosity is not None else "-", points or "-"] + marks
        rows.append(f"$(li)$(l){name}$(): " + ", ".join(cells))
    return rows


def figure(value):
    if abs(value) >= 100:
        return f"{value:,.0f}"
    return f"{float(f'{value:.3g}'):g}"


def liquid_constant(name):
    java = (JAVA / "liquid/Liquids.java").read_text(encoding="utf-8")
    return float(re.search(rf"\b{name} = ([0-9.]+)", java).group(1))


def liquids_entry():
    rows, table_of = liquid_rows(), liquids()
    vent, every, viscous = int(liquid_constant("VENT")), int(liquid_constant("VENT_INTERVAL")) // 20, figure(liquid_constant("VISCOUS"))
    seawater, bayer, carbonyl = table_of["fundamentals:seawater"][3], table_of["fundamentals:sodium_aluminate_liquor"][7], table_of["fundamentals:nickel_carbonyl"][6]
    table = [{"type": "patchouli:text", "text": "".join(rows[i:i + 5]), **({"title": "The table"} if i == 0 else {})} for i in range(0, len(rows), 5)]
    entry("rare_earths", "liquids", "Liquid properties", "minecraft:water_bucket", pages_of(
        f"Every fluid in the plant is a real one, with its real density, viscosity, freezing and boiling point and flash point; goggles on a pipe, "
        "pump, valve, tank or basin give them. Water-based fluids freeze in the pipe: below the freezing point a pipe holds still and a pump moves "
        f"nothing until the heat comes back. Water freezes at 0 °C, seawater at {figure(seawater)}, brines and acids far lower.", "Liquid properties") + pages_of(
        f"A fluid past its boiling point in an open basin, with no mixer or press over it, boils off {vent} mB every {every} seconds, "
        "and whatever it carries is breathed: acid fumes, poison, nickel carbonyl. In a pipe or tank the goggles warn instead. "
        f"Fresh Bayer liquor leaves the digester at {figure(bayer)} °C under pressure, too hot for plastic pipe. A flammable fluid let out by a burst pipe "
        "past its flash point beside a flame, lava or a burner, or anywhere past its autoignition temperature, catches fire; "
        f"nickel carbonyl ignites at {figure(carbonyl)} °C. Spilt oils and fuels in the world burn as wood does, the lower the flash point the readier.") + pages_of(
        f"Thick fluids pump slower, as the Hydraulic Institute's correction has a pump lose flow past about {viscous} mPa·s: "
        "heavy oil at 3,000 mPa·s goes at about half, and a polymer melt or fresh concrete no slower than a quarter. "
        "Each line of the table gives density in kg/m³, viscosity in mPa·s, and freezing to boiling point in °C, then the flash point and hazards. "
        "Gases are at 20 °C and 1 atm and hot melts at their own temperature; an oil's freezing point is its pour point.")
        + table + [{"type": "patchouli:text", "text": "".join(f"$(li){text}" for text in list(REFERENCES.values())[i:i + 3]),
                    **({"title": "Sources"} if i == 0 else {})} for i in range(0, len(REFERENCES), 3)], 15)


def ageing_table():
    def days(d):
        return f"{d:g} day" + ("" if d == 1 else "s")
    lines = [f"$(li){metal.title()}: {kind}, {days(d)} a stage; damp, {days(round(d / wet, 1)) if wet else 'never'}"
             for metal, (kind, d, dry, wet, _) in AGEING.items()]
    return [{"type": "patchouli:text", "text": "".join(lines[i:i + 7]), **({"title": "Ingots, in game days"} if i == 0 else {})}
            for i in range(0, len(lines), 7)]


def oxidation():
    entry("metals", "oxidation", "Oxidation and storage", "fundamentals:inert_storage_drum", pages_of(
        "Metal left in air does not stay as it was made, and a chest does not save it. Aluminium, titanium, chromium, stainless steel, nickel, tin, zinc and lead "
        "grow a skin of oxide a few nanometres thick and stop there, so they keep. Gold and the platinum metals never change. "
        "Copper, brass and bronze take a patina, dull, then brown, then green with verdigris, which stops at the surface: a bronze statue keeps its shape for two thousand years. "
        "Silver blackens with the hydrogen sulfide in air, a film of silver sulfide.", "Oxidation and storage") + pages_of(
        "Iron and steel rust, and only with water: in dry air they keep for years, by the sea they go in weeks, and an ingot rusts through to a rusty one, "
        "its scale flaking off. The rare earth metals are worse. Their oxide is bigger than the metal it eats, so it spalls off and bares fresh metal under it: "
        "lanthanum and cerium go from a bright ingot to a pile of oxide in days of damp air, praseodymium and neodymium in weeks, samarium, gadolinium and the heavies slowly. "
        "Calcium and lithium crust over within hours, magnesium slowly.") + ageing_table() + pages_of(
        "The finer the metal, the faster: a nugget ages half again as fast as an ingot, a block a quarter as fast, and nine nuggets crumbled to oxide make one oxide. "
        "Dry air (a desert, the Nether) slows it, and keeps iron from rusting at all; water touching the chest, a jungle or a swamp speeds it; the salt air of a beach or the sea doubles that again. "
        "An aged stack no longer stacks with fresh metal. Create's sand paper polishes a patina, a tarnish or the first oxide off an ingot, nugget or sheet; "
        "a rusty ingot polishes back to seven nuggets, the rest gone as rust.") + pages_of(
        "Storage is what the trade does. The inert storage drum holds a chest's worth under a blanket of argon piped in, a thousand millibuckets of it, "
        "and nothing inside it ages. Argon seeps out through the bung, ten millibuckets a day, and each time the lid comes off, twenty-five more are lost to the air let in. "
        "Kerosene does the same and does not leak: the alkali metals and the rare earths have always been kept under oil. "
        "A canister filled with 100 mB of argon by a spout seals one stack away: right-click the stack onto it, and the canister empty-handed to open it.") + pages_of(
        "Blocks weather in place as vanilla's copper does, one stage at a time, faster in the rain or by water: bronze to verdigris green, silver to black, "
        "and neodymium, praseodymium, samarium, terbium and dysprosium blocks tarnish, corrode and crumble to their oxide, nine of it when broken. "
        "A honeycomb waxes bronze and silver as lacquer does a statue or a tray, and an axe scrapes a stage back; no wax saves a rare earth, and nothing scrapes back a block that has crumbled."), 9)


def metals():
    category("metals", "The other metals", "Cobalt, zinc, nickel and Mond's carbonyl, the porphyry chain (copper, molybdenum and the rhenium hiding in it), tungsten, chromium, tin, titanium, the silver in lead, zirconium and hafnium, beryllium, aluminium and lithium.", "fundamentals:cobalt_ingot", 3)
    entry("metals", "cobalt", "Cobalt", "fundamentals:cobalt_ingot", pages_of(
        "Cobaltite is a cobalt arsenide-sulfide from the silver-cobalt veins in calcite. Roast it on a campfire or in a smoker to drive off the arsenic and sulfur and leave the oxide. "
        "Cobalt melts at 1,495 °C. The Congo's copper-cobalt ore was once smelted in electric furnaces to a white alloy of cobalt, copper and iron and refined from that; "
        "here the oxide is reduced under hydrogen, as molybdenum is: two roasted cobaltite and 500 mB of hydrogen in a heated chemical vat give two ingots. "
        "Cobalt is half of SmCo and a tenth of the superalloy. "
        "Roasted cobaltite calcined with two bauxite powder is cobalt blue, four blue dye.", "Cobalt") + pages_of(
        "Veins like Cobalt, Ontario, and Bou Azzer, Morocco, are the old sources and a small one now. About three quarters of the world's cobalt is mined in the "
        "Democratic Republic of the Congo, from its copper-cobalt ores, and most of the rest comes out of nickel refineries (USGS, Mineral Commodity Summaries 2025). "
        "The nickel refinery's sulfate liquor carries it: 1,000 mB of it, 250 mB of P507 and 50 mB of hydrochloric acid under a mixer in a chemical vat give 950 mB of nickel sulfate liquor, "
        "50 mB of rose cobalt chloride liquor and 245 mB of the organic back. Electrowon on two electrodes, 500 mB of the cobalt liquor gives a cobalt ingot and 250 mB of chlorine, "
        "as Nikkelverk wins its cobalt.", "From nickel") + pages_of(
        "The extractant takes cobalt over nickel at about pH 5 and the acid strips it again: one step here for the extraction, scrub and strip sections of a real circuit. "
        "Western refineries run Cyanex 272, a phosphinic acid that parts the two some thousands of times over; Chinese ones the phosphonic P507, which does it some hundreds of times "
        "(Flett, Cobalt-nickel separation in hydrometallurgy, 2004). A twentieth of the nickel's weight in cobalt comes out, rather more than a nickel sulfide ore really carries."), 0)
    entry("metals", "porphyry", "Copper, molybdenum, rhenium", "fundamentals:raw_molybdenite", pages_of(
        "A porphyry copper stock carries chalcopyrite with a little molybdenite, and the molybdenite carries rhenium at parts per million. "
        "A copper sulfide smelts not to copper but to matte. Chalcopyrite roasted on a fire, or the calcine bornite, chalcocite and covellite roast to, melts in the bloomery to copper matte and slag. "
        "Two matte and a sand, superheated, are the converter: the air burns off the sulfur and the iron, which the sand fluxes to slag, and leaves two blister copper. The blast furnace fire-refines blister to copper. "
        "Two raw molybdenite roasted in a heated basin give two molybdenum trioxide, and one time in ten a rhenium flue dust: "
        "the roaster's flue is where all mined rhenium comes from, though about a quarter of the rhenium used is now recycled from superalloy scrap "
        "and spent catalyst (USGS, Mineral Commodity Summaries 2025).", "The porphyry chain") + pages_of(
        "The trioxide is reduced under hydrogen in a heated chemical vat with an industrial mixer, as the industry does: two trioxide and 500 mB of hydrogen give two molybdenum ingots. "
        "The flue dust is scrubbed into water and its perrhenate taken up on an ion exchanger and stripped with ammonia: a flue dust, 250 mB of water and 50 mB of ammonia give an ammonium perrhenate, "
        "white crystals, and one with 250 mB of hydrogen in the vat gives two rhenium nuggets. The hydrogen is shifted from water gas; the extractants entry under the rare earths says how.", "Rhenium") + pages_of(
        "Real molybdenite concentrate carries 100 to 3,000 ppm of rhenium. A nugget to every ten molybdenite here is a few times the richest of it, "
        "so that a superalloy melt costs a vein and not a mountain."), 1)
    entry("metals", "superalloy", "Superalloy and molybdenum steel", "fundamentals:superalloy_ingot", pages_of(
        "Six nickel, a cobalt, a chromium and a tungsten ingot, five aluminium and two rhenium nuggets, superheated under 100 mB of argon, make seven ingots of a single-crystal nickel superalloy "
        "after CMSX-4, which is Ni-6.5Cr-9Co-6W-6.5Ta-5.6Al-3Re-1Ti (Reed, The Superalloys, 2006): 61 per cent nickel, ten each of cobalt, chromium and tungsten, 5.7 aluminium and 2.3 rhenium, "
        "the tungsten standing in for the tantalum too, which is not in the pack.", "Superalloy") + pages_of(
        "The aluminium is what makes it a superalloy. It precipitates as ordered Ni3Al, gamma-prime, which keeps the alloy strong almost to its melting point, and with the chromium it grows "
        "the alumina scale that keeps it from burning in the hot gas; tungsten and rhenium stiffen it against creep.") + pages_of(
        "The Factory Must Grow's turbine blade takes its plates, coated as a real blade is: a yttria-stabilised zirconia thermal barrier over an MCrAlY bond coat. "
        "Four nickel, two cobalt, a chromium, an aluminium and a yttrium nugget, superheated under 100 mB of argon, atomise to eight MCrAlY powder, NiCoCrAlY as Amdry 365 is "
        "(Ni-23Co-17Cr-12.5Al-0.5Y); its yttrium pins the alumina scale to the blade.", "The blade") + pages_of(
        "Molybdenum goes into steel as the roasted trioxide, not as metal. A molybdenum trioxide and six blocks of steel, superheated, make 54 molybdenum steel, about one per cent molybdenum, "
        "the share in the creep-resistant chromium-molybdenum boiler and pressure-vessel steels (ASTM A387 grade 22 is 0.9 to 1.1 per cent, 4140 0.15 to 0.25), their chromium left out. "
        "Its plates make the heavy machinery casing.", "Molybdenum steel"), 2)
    entry("metals", "tungsten", "Tungsten", "fundamentals:tungsten_ingot", pages_of(
        "Wolframite, from the tin veins, will not open to acid. Two raw wolframite and 500 mB of caustic soda in a heated chemical vat with a mixer, the pressure digester at 150 to 200 °C, "
        "give 500 mB of sodium tungstate liquor, the iron and manganese left behind as hydroxides; or two wolframite and two soda ash fused over a superheated burner and leached in 500 mB of water give the same. "
        "Scheelite, from the limestone skarns, opens to hot hydrochloric acid: two raw scheelite and 500 mB, heated, throw down two yellow tungstic acid.", "Tungsten") + pages_of(
        "Either way the tungsten is taken up as ammonium tungstate and crystallised as ammonium paratungstate, APT, the form tungsten is traded in. "
        "500 mB of the liquor and 250 mB of ammonia, heated, give two APT, the amine extraction and ammonia strip folded into the one step; two tungstic acid dissolved in 250 mB of ammonia give two as well. "
        "A blast furnace calcines APT to the canary-yellow trioxide, and hydrogen in a heated chemical vat reduces two oxide to two ingots (Lassner and Schubert, Tungsten, 1999).", "APT") + pages_of(
        "One ingot draws to four filaments, and the Factory's light bulb now burns one. In a vacuum the filament boils away and blackens the glass; Langmuir filled the bulb with an inert gas in 1913, "
        "and lamps have been argon-filled since: a filament, a framed glass, four copper and three steel nuggets and 50 mB of argon under a mixer make three bulbs where crafting makes two. "
        "One ingot and two coals, superheated, carburise to two tungsten carbide, "
        "and Create's mechanical drill now bites with it. Ten per cent of the superalloy is tungsten.", "What it is for"), 3)
    entry("metals", "zinc_nickel", "Zinc and nickel", "fundamentals:zinc_oxide", pages_of(
        "Neither melts out of its ore in a furnace. Sphalerite roasts on a fire to zinc oxide, and smithsonite and hemimorphite, the old calamine, calcine to it. "
        "Zinc boils at 907 °C, below the heat that reduces it, so it was distilled from a sealed retort packed with charcoal: "
        "a zinc oxide and a charcoal in a basin over a blaze burner fed a blaze cake give a zinc ingot. Asurine, the zinc stone, crushes to smithsonite. "
        "Zinc oxide is also what every sulfur cure of rubber needs to work, so the Factory's rubber takes one.", "Zinc") + pages_of(
        "Half the world's zinc goes on steel (International Zinc Association). Dipped in molten zinc at about 450 °C, steel takes a coat that corrodes in its place and, scratched through, "
        "still goes before the steel does. Four heavy plates and a zinc nugget, heated, give four galvanized steel plates, which do anything a steel plate does and never rust; "
        "a bare heavy plate rusts as a steel ingot does.", "Galvanizing") + pages_of(
        "Carbon does not part nickel from iron: it reduces both, and they alloy. Pentlandite roasted on a fire and smelted with a charcoal, superheated, gives a ferronickel ingot and a slag, "
        "and whatever platinum it carried is lost in it. Nickel laterite, a weathered rock of 1.5 to 2.5 per cent nickel, is smelted whole: four of it with two charcoal, superheated, "
        "give one ferronickel and two slag, as the rotary kilns and electric furnaces of New Caledonia and Indonesia smelt it at about 1,550 °C.", "Ferronickel") + pages_of(
        "Real ferronickel runs 20 to 40 per cent nickel, and nickel pig iron less (Crundwell and others, Extractive Metallurgy of Nickel, Cobalt and Platinum Group Metals, 2011); here an ingot is a third nickel. "
        "None of it is ever nickel metal. It goes where two thirds of the world's nickel goes, into stainless steel, which wants the iron anyway: three ferrochrome, three ferronickel and four steel, superheated, make ten stainless. "
        "Nickel metal comes from sulfide matte: leached and electrowon, under the platinum metals, or by Mond's carbonyl.", "Nickel"), 4)
    entry("metals", "mond", "Mond nickel and its poisons", "fundamentals:nickel_pellets", pages_of(
        "Ludwig Mond found in 1890 that carbon monoxide carries nickel off as a gas at 50 °C and gives it back when heated past 180 °C, and iron, copper and cobalt hardly at all (Mond, Langer and Quincke, Journal of the Chemical Society, 1890). "
        "His refinery at Clydach in Wales has run on it since 1902, and Copper Cliff in Sudbury runs it under pressure.", "The Mond process") + pages_of(
        "Converter matte roasted dead on a campfire or in a smoker burns off its sulfur and leaves nickel oxide. Two nickel oxide and 1,000 mB of water gas in a heated chemical vat with a mixer give 500 mB of nickel carbonyl, "
        "the water gas's hydrogen reducing the oxide at 400 °C and its monoxide taking the nickel, Ni + 4 CO to Ni(CO)4; one time in ten the residue is a platinum group concentrate, as Clydach's residues fed the Acton refinery.") + pages_of(
        "500 mB of carbonyl in a heated vat decomposes at about 230 °C on hot pellets to two nickel pellets, 99.97 per cent nickel, and 500 mB of the gas goes back round. "
        "A heated press over a basin squeezes a pellet into a nickel ingot.") + pages_of(
        "Nickel carbonyl is among the most poisonous gases in industry. NIOSH puts the level immediately dangerous to life at 2 parts per million, and its faint musty smell comes only above that. "
        "It does not eat a pipe or a tank, but where it gets out, from an open pipe end, a broken pipe, tank or vat, or a basin, it reaches three blocks: "
        "headache, nausea and weakness at once, and a hard hit every second.", "Nickel carbonyl") + pages_of(
        "The worst comes later. A day or so after exposure the lungs fill (pulmonary oedema), however well the victim felt; here it comes half a minute later, as withering that lasts four seconds for each second breathed. "
        "Only Create's diving helmet on a filled backtank keeps it out, and it burns two of the tank's air a second doing so. Keep the carbonyl in closed pipes and tanks, and wear the mask to break into them.") + pages_of(
        "The dusts of the nickel smelter cause lung and nasal cancer: the men who roasted and leached matte at Clydach died of them at many times the expected rate (Doll, 1958), "
        "and the International Agency for Research on Cancer puts nickel compounds in its first group. Nickel matte, converter matte, roasted pentlandite and nickel oxide held in the hand or stirred in a basin are breathed by everyone near. "
        "And a sulfide roasting on a lit campfire or in a lit smoker or furnace, pentlandite, cobaltite or matte, gives off sulfur dioxide that stings anyone within two blocks.", "The dusts and the fume"), 4)
    entry("metals", "chromium", "Ferrochrome and chromium", "fundamentals:ferrochrome_ingot", pages_of(
        "Chromite is chromium's only ore, black seams in the gabbro of the deep layered intrusion. A millstone or crushing wheels grind it to a brown powder, and a wash under an encased fan "
        "leaves the heavy chromite behind as concentrate, half of what goes in. Iron in chromite reduces along with the chromium, so smelting gives not chromium but ferrochrome: "
        "two concentrate, a coal coke and a limesand flux, superheated, as a submerged-arc furnace smelts it at 1,600 to 1,700 °C, give a ferrochrome ingot and slag.", "Ferrochrome") + pages_of(
        "Ferrochrome is what stainless steel is made from: three ferrochrome, a nickel and six steel, superheated, make ten stainless steel, eighteen per cent chromium and ten nickel. "
        "A flare burns in its own flame, so the Factory's flarestack is now built on stainless, and the steel chemical vat is lined with stainless plates where it had nickel.") + pages_of(
        "Chromium metal goes the long way, through its salts. Trona, a soda mineral that lies in the desert evaporite beds beside the borax, calcines in a furnace to soda ash. "
        "A chromite concentrate and two soda ash roasted in a heated basin give two sodium chromate, yellow: the roast oxidises the chromium in air at about 1,100 °C and the iron stays behind as oxide.", "Chromium") + pages_of(
        "Two chromate and 250 mB of sulfuric acid in a basin make the orange sodium dichromate. Heated with a coal it is reduced to the green chromium oxide and gives one soda ash back. "
        "Last, the thermite: a chromium oxide and an aluminium powder (an aluminium ingot milled to two), lit by a burner fed a blaze cake, burn on by themselves past 2,000 °C to a chromium ingot and a slag of alumina. "
        "The superalloy takes the metal, and one oxide makes two green dye, the chrome oxide green of the paint box."), 5)
    entry("metals", "manganese", "Ferromanganese and manganese steel", "fundamentals:ferromanganese_ingot", pages_of(
        "Nine tenths of the world's manganese goes into steel (USGS, Mineral Commodity Summaries, Manganese): a little in every heat takes up its sulfur and oxygen, and it goes in as ferromanganese. "
        "Two raw pyrolusite, a coal coke, an iron nugget and a limesand flux, superheated, smelt as a submerged-arc furnace does at about 1,500 °C to a ferromanganese ingot, four fifths manganese, and a slag.", "Ferromanganese") + pages_of(
        "A lot of it is Hadfield's steel (1882), 12 to 14 per cent manganese and 1 of carbon, which hardens where it is struck: crusher jaws, railway crossings, the Brodie helmet. "
        "A ferromanganese and six steel, superheated, make seven manganese steel. As wear plate a thin plate of it does a thick one's work, so a manganese steel ingot, pressed three times as a steel one is, gives two of the Factory's heavy plates.", "Manganese steel"), 5)
    entry("metals", "tin", "Tin and bronze", "fundamentals:tin_ingot", pages_of(
        "Cassiterite, tin dioxide, comes from the tin veins and from beach and river sand, where it settles because it is seven times as heavy as water. "
        "That weight is how it is concentrated: a raw cassiterite washed under an encased fan leaves a tin concentrate behind. "
        "Pyrite and arsenopyrite ride with it, so the concentrate is roasted on a campfire or in a smoker to drive off their sulfur and arsenic.", "Cassiterite") + pages_of(
        "Charcoal reduces the oxide at 1,200 to 1,300 °C, the bloomery's heat, as the charcoal shaft furnaces of the Cornish blowing houses did: a roasted concentrate smelts to a crude tin ingot and slag. "
        "In a factory, two roasted concentrate and a coal coke in a superheated basin give two crude tin and a slag, as a reverberatory furnace does. "
        "Crude tin carries iron. Tin melts at 232 °C, so on a gentle heat it runs off and leaves the iron-tin hardhead behind (liquation), and a green pole stirred through the melt brings up the last dross: "
        "two crude tin and a stick in a heated basin give two tin ingots, a slag one time in four.", "Smelting and refining") + pages_of(
        "Arsenical copper came first, but tin bronze is the alloy of the age named for it: copper with about an eighth of tin, and bell metal harder still at a fifth to a quarter. "
        "Seven copper and a tin, heated, make eight bronze, still the metal of bells: Create's peculiar bell is cast in it, and five bronze ingots under a stick make a bell. A plain bearing is a bronze bush, so Create's mechanical bearing takes two bronze plates. "
        "A tin and a lead, heated, make eight solder, and every loop of the Factory's circuit board assembly now solders its parts down.", "Bronze and solder"), 6)
    entry("metals", "titanium", "Titanium", "fundamentals:titanium_ingot", pages_of(
        "Ilmenite and rutile are heavy sands, panned from beaches and rivers. Rutile is titanium dioxide already; ilmenite is iron titanate, a third of it iron. "
        "Four ilmenite and a coal coke, superheated, smelt as the electric furnaces of Sorel and Richards Bay do at about 1,650 °C: "
        "the iron runs off as a cast iron ingot and the titanium stays behind in two titania slag, black and some eighty-five per cent titania.", "Titanium") + pages_of(
        "No carbon reduces titanium: it would only make a carbide. It goes through its chloride. Two rutile or two titania slag, a coal coke and 1,000 mB of chlorine, heated, "
        "give 500 mB of titanium tetrachloride, a colourless liquid that boils at 136 °C and fumes to hydrogen chloride in damp air. "
        "In a heated vat with a mixer, 500 mB of it, four magnesium and 100 mB of argon give two titanium sponge and four magnesium chloride: the Kroll process.") + pages_of(
        "Two magnesium chloride electrolysed molten on two electrodes in a heated vat give two magnesium and 500 mB of chlorine, so the magnesium and the chlorine go round again. "
        "The first magnesium comes from the sea. The Dow process: 1,000 mB of seawater, a limesand and 250 mB of hydrochloric acid, heated, give one magnesium chloride. "
        "Or the bittern salt-making leaves: 500 mB boiled down in a heated basin gives one. "
        "Titanium melts at 1,668 °C, past a blaze cake, and burns hot in air, so the sponge is arc-melted: two sponge and 100 mB of argon on two electrodes, superheated, give two ingots.") + pages_of(
        "Most titanium never becomes metal. 250 mB of the tetrachloride burnt in 1,000 mB of air in a heated basin gives a titanium oxide, the purest white there is, and 500 mB of chlorine back; "
        "one oxide makes four white dye. The metal goes where strength for its weight counts: titanium plates in place of steel make four of the Factory's turbine engines instead of two."), 7)
    entry("metals", "silver", "Silver", "fundamentals:silver_ingot", pages_of(
        "Most of the world's silver has always come out of lead. Galena carries a little, so roasted galena smelts in the bloomery not to lead but to lead bullion, which holds it. "
        "A furnace remelts bullion to plain lead and the silver is lost in it. To win it, stir zinc into the molten lead, the Parkes process: zinc and lead do not mix, silver is some three thousand times more soluble in zinc, "
        "and the zinc rises with it as a crust. Four bullion and a zinc ingot in a heated basin give three lead and a silver-zinc crust.", "Silver") + pages_of(
        "Cupellation is how silver has been parted from lead since antiquity: the lead is blown with air on a hearth of bone ash at about 1,000 °C, burns to litharge, the yellow-orange lead oxide, and soaks into the cupel, "
        "and a bead of silver stays bright. A crust, a bone meal and 250 mB of air, heated, give four silver nuggets, a litharge and a zinc oxide for the zinc retort. "
        "A bullion cupelled straight, the old way, gives one nugget and its whole lead as litharge. A litharge and a charcoal, heated, reduce back to lead, "
        "and a lead-acid plate is a lead grid pasted with litharge: the Factory's lead accumulator takes a litharge where it took a block of lead.") + pages_of(
        "The rich silver ores, acanthite and native silver from the calcite veins, were soaked into a lead bath on the cupel and cupelled with it: a raw ore, a lead ingot, a bone meal and 250 mB of air, heated, give a silver ingot and a litharge. "
        "Silver conducts better than any metal, and the oxide an arc burns on it conducts too and gives up its oxygen in the heat, so contacts that arc as they make and break are silver: "
        "The Factory's electrical switch and large switch take silver plates. Its black tarnish, the sulfide, is the trouble: it raises a contact's resistance. "
        "A circuit board can be finished in silver as well as gold, and four silver plates, two zinc, a plastic separator, copper wire and an aluminium casing make a silver-zinc accumulator."), 8)
    entry("metals", "zirconium", "Zirconium and hafnium", "fundamentals:zirconium_ingot", pages_of(
        "Zircon is the third mineral of the heavy sands, after ilmenite and rutile: honey-brown grains panned from beach and river sand, and a few in the syenite massifs. "
        "Washed under an encased fan, the light quartz goes and half stays as zircon concentrate. Most zircon never becomes metal. As sand it faces a mould, standing 2,000 °C unwetted, "
        "so the Factory's casting basin takes a zircon concentrate; milled, it is the white of tile glaze, and one in eight terracotta makes eight white.", "Zirconium") + pages_of(
        "For the metal it takes titanium's road. Two concentrate, a coal coke and 1,000 mB of chlorine, heated, give two crude zirconium tetrachloride, a white solid that sublimes at 331 °C, "
        "the silica leaving as silicon tetrachloride. A crude chloride and 500 mB of water, heated, hydrolyse and calcine to a zirconium oxide, zirconia, and give 250 mB of hydrochloric acid back.") + pages_of(
        "Zircon carries a fiftieth as much hafnium, its chemical twin, and the chloride keeps it. The two are parted by extractive distillation through a molten chloride, hafnium tetrachloride the more volatile: "
        "four crude chloride and a salt, heated, give four zirconium tetrachloride and, one time in ten, a hafnium tetrachloride; the salt comes back nine times in ten.", "Hafnium") + pages_of(
        "Each chloride is reduced as titanium's is: one tetrachloride, two magnesium and 100 mB of argon in a heated vat with a mixer give a sponge and two magnesium chloride, and two sponge and argon "
        "on two electrodes, superheated, arc-melt to two ingots.") + pages_of(
        "Twelve zirconia and a yttrium oxide, superheated, make thirteen yttria-stabilised zirconia, 7.7 per cent yttria as the 7YSZ of a turbine's thermal barrier is, the ceramic coat that lets a turbine blade run in gas hotter than it melts: the Factory's turbine blade takes one. "
        "Zirconium shrugs off hot hydrochloric and sulfuric acid that stainless cannot, so two zirconium plates line four steel chemical vats where stainless lines two. "
        "A hafnium nugget in the superalloy melt, against cracking at the grain boundaries, makes nine ingots where it made seven.", "What they are for"), 10)
    entry("metals", "beryllium", "Beryllium", "fundamentals:beryllium_ingot", pages_of(
        "Beryl is pale green or blue-green, six-sided prisms in the pegmatites; emerald and aquamarine are beryl. Bertrandite is the white mineral of the beryllium tuffs of the dry country, "
        "rhyolite ash with fluorite nodules, as at Spor Mountain, Utah, where most of the world's beryllium is mined.", "Beryllium") + pages_of(
        "Beryl will not open to acid as it is. It is melted in an arc furnace at about 1,650 °C, a little past a blaze cake, and quenched to a glass, the frit (Kjellgren and Sawyer): "
        "two raw beryl and 250 mB of water, superheated, give two beryl frit. "
        "Two frit and 500 mB of sulfuric acid, heated, give 500 mB of beryllium sulfate liquor. Bertrandite leaches as it is, but is poorer: four raw and 500 mB of the acid give 250 mB, a quarter of what beryl gives. "
        "An emerald melts to a frit one time in two.") + pages_of(
        "500 mB of liquor and 250 mB of ammonia give two beryllium hydroxide, the aluminium having crystallised out as alum. Two hydroxide, 500 mB of hydrofluoric acid and 250 mB of ammonia give two ammonium fluoroberyllate, "
        "which a blast furnace takes to beryllium fluoride, the ammonium fluoride passing off; the hydroxide calcined in one is beryllium oxide, beryllia.") + pages_of(
        "Two fluoride, two magnesium and 100 mB of argon in a superheated vat with a mixer give two beryllium pebbles and a slag of magnesium fluoride, and two pebbles melted under argon, superheated, give two ingots. "
        "Beryllium is lighter than aluminium and stiffer than steel, and lets X-rays through as glass lets light.") + pages_of(
        "The dust is the danger: the hydroxide, the oxide, the salts and the metal's pebbles scar the lungs (berylliosis). Held in the hand or stirred in a basin they are breathed by everyone near, "
        "and only Create's diving helmet on a filled backtank keeps it out.", "Berylliosis") + pages_of(
        "Most beryllium goes into copper. A beryllium nugget and five copper ingots, heated, make five beryllium copper, as strong as steel, springy and sparkless when struck. "
        "It is mostly made without the metal: a beryllium oxide, four copper blocks and a coal coke, superheated, give four blocks, as an arc furnace makes the master alloy. "
        "The spring contact in a connector is beryllium copper, so the Factory's cable connector built on it makes three.", "Beryllium copper"), 11)
    entry("metals", "aluminium", "Aluminium", "tfmg:aluminum_ingot", pages_of(
        "Bauxite is not one mineral but a weathered rock under tropical soil: gibbsite and boehmite, the aluminium hydroxides, reddened by iron oxide. A millstone grinds raw bauxite to bauxite powder. "
        "No one electrolyses that straight to metal. The alumina is first got out of it clean, Bayer's way (1888), and then electrolysed dissolved in molten cryolite, Hall's and Héroult's (1886).", "Aluminium") + pages_of(
        "Two bauxite powder and 500 mB of caustic soda in a heated chemical vat with a mixer, the digester at 150 to 250 °C under pressure, give 500 mB of sodium aluminate liquor and a red mud, "
        "what the caustic will not take: iron oxide, titania, silica, and a little scandium and the rare earths. 500 mB of liquor under a mixer, cold, throws down an aluminium hydroxide "
        "and gives 400 mB of caustic soda back to the digester. A blast furnace calcines the hydroxide at about 1,100 °C to alumina.", "Bayer") + pages_of(
        "Red mud is the industry's great waste, more than a tonne for every tonne of alumina, piled in ponds; it is also the largest scandium resource there is.", "Red mud") + pages_of(
        "Cryolite, sodium aluminium fluoride, is the bath. The Greenland mine that gave it is worked out, so it is made: an aluminium hydroxide, 500 mB of hydrofluoric acid "
        "and 250 mB of caustic soda give two, or the hydroxide and 500 mB of the acid with a soda ash for the caustic.", "Cryolite") + pages_of(
        "Two alumina, a cryolite and a coal coke in a heated vat between two graphite electrodes give an aluminium ingot and 250 mB of carbon dioxide. "
        "The alumina dissolves in the cryolite melt at about 960 °C, under a kindled burner's 1,000; the coke is the anode, burning away as the oxygen comes off on it, "
        "some 400 kg of carbon to the tonne of metal; the cryolite comes back nineteen times in twenty. Every aluminium ingot The Factory Must Grow takes comes this way now.", "Hall-Héroult") + pages_of(
        "A pot gives off hydrogen fluoride from its bath: within two blocks of a vat with cryolite in it you take a hit a second and are poisoned, "
        "unless Create's diving helmet on a filled backtank keeps it out.", "Fluoride") + pages_of(
        "Alumina is a spark plug's insulator, ninety-five per cent of it, round a nickel-alloy centre electrode: a nickel nugget, an alumina and a steel nugget make a plug in a mechanical crafter. "
        "A precious tip lasts: an iridium nugget, an alumina and a nickel nugget make four plugs, and a platinum one three.", "Spark plugs") + pages_of(
        "Sources: Grjotheim and Kvande, Introduction to Aluminium Electrolysis (Aluminium-Verlag, 1993); Ullmann's Encyclopedia of Industrial Chemistry, \"Aluminum Oxide\" and \"Aluminum\"; "
        "Evans, \"The History, Challenges, and New Developments in the Management and Use of Bauxite Residue\", Journal of Sustainable Metallurgy 2 (2016).", "Sources"), 12)
    entry("metals", "lithium", "Lithium", "tfmg:lithium_ingot", pages_of(
        "Spodumene, LiAlSi2O6, is the pale pyroxene of the lithium pegmatites. A blast furnace calcines it at about 1,050 °C from alpha to beta-spodumene, which opens to acid where the alpha form will not. "
        "Two calcined spodumene, 250 mB of sulfuric acid and 500 mB of water, heated, are the acid roast at about 250 °C and the water leach, and give 500 mB of lithium sulfate liquor. "
        "Two soda ash in the hot liquor throw down two lithium carbonate, the sodium sulfate staying in solution.", "Spodumene") + pages_of(
        "Lithium carbonate is what the trade sells. For the metal it goes to the chloride: two carbonate and 500 mB of hydrochloric acid, heated, give two lithium chloride and 250 mB of carbon dioxide. "
        "Lithium cannot be won from water, so the dry chloride is electrolysed molten, with potassium chloride in the real bath, at about 450 °C: two chloride on two electrodes in a heated vat "
        "give a lithium ingot, half the time three nuggets more, and 250 mB of chlorine.", "Carbonate to metal") + pages_of(
        "About a third of the world's lithium comes not from rock but from the brines under the salt flats of Chile, Argentina and Tibet, evaporated in ponds and precipitated with soda ash; "
        "there is no such brine in the pack yet.") + pages_of(
        "Sources: Ullmann's Encyclopedia of Industrial Chemistry, \"Lithium and Lithium Compounds\"; the Talison and Tianqi spodumene flowsheets; USGS, Mineral Commodity Summaries 2025, Lithium.", "Sources"), 13)
    oxidation()


def platinum():
    category("platinum", "The platinum metals", "Six metals that ride in nickel matte and come out of a refinery one by one, in the order their chemistry allows.",
             "fundamentals:platinum_ingot", 4)
    entry("platinum", "matte", "Matte", "fundamentals:converter_matte_dust", pages_of(
        "The platinum metals are never mined alone. They ride in nickel-copper sulfide, and the smelter's matte is what collects them. "
        "Raw pentlandite smelts unroasted in the bloomery to nickel matte and slag, as flash furnaces smelt it at Norilsk and Sudbury. "
        "Two nickel matte and a sand, superheated, are the converter: air burns out the iron, which the sand fluxes to slag, and leaves two converter matte. "
        "A nickel matte and a copper matte from the porphyry chain convert together the same way.", "Matte") + pages_of(
        "The base-metal refinery leaches two converter matte in 500 mB of hot sulfuric acid. The nickel and copper dissolve to a green sulfate liquor; "
        "what will not dissolve is the platinum group concentrate, one time in ten from a plain nickel matte. "
        "The deep layered intrusion's platinum minerals, sperrylite, cooperite and braggite, are what make it a reef and not a nickel mine: one in the leach with the matte gives a concentrate every time. "
        "The liquor electrowins on two electrodes in a vat, 500 mB to a nickel ingot, a copper ingot half the time, and 250 mB of its acid back. "
        "It carries the cobalt too, which P507 takes out first, leaving a nickel sulfate liquor that electrowins the same way: the cobalt entry says how. "
        "Or the matte goes the Mond way, by carbonyl."), 0)
    entry("platinum", "reagents", "Aqua regia and ammonia", "fundamentals:aqua_regia_bucket", pages_of(
        "Aqua regia is 375 mB of hydrochloric acid and 125 mB of nitric, mixed cold: the royal water that dissolves gold, and the only common acid that takes platinum. It fumes like nitric acid; wear the mask. "
        "Ammonia is made as Haber and Bosch made it: 750 mB of hydrogen and 250 mB of air over a raw magnetite, the iron catalyst, in a heated basin, give 500 mB and the magnetite back nine times in ten. "
        "250 mB of ammonia and 250 mB of hydrochloric acid meet as white smoke and settle as two ammonium chloride, sal ammoniac, which the refinery precipitates with.", "Aqua regia and ammonia"), 1)
    entry("platinum", "refinery", "The refinery", "fundamentals:ammonium_chloroplatinate", pages_of(
        "A platinum group concentrate in 500 mB of hot aqua regia, or of hydrochloric acid with 250 mB of chlorine bubbled through it as newer refineries leach, "
        "gives 500 mB of orange platinum-palladium liquor, and half the time an insoluble residue: rhodium, iridium, ruthenium and osmium, which no acid takes.", "The refinery") + [
        spotlight("fundamentals:ammonium_chloroplatinate", "Platinum first: two ammonium chloride in the liquor drop the bright yellow ammonium chloroplatinate and leave 250 mB of red-brown palladium liquor. "
                  "A blast furnace or a fan over lava ignites the salt straight to platinum sponge.", "Platinum"),
        spotlight("fundamentals:dichlorodiammine_palladium", "Palladium next: 250 mB of ammonia turns the liquor colourless, the tetrammine, and 250 mB of hydrochloric acid then drops yellow dichlorodiammine palladium "
                  "and leaves spent liquor for the lime. Two salts and 250 mB of hydrogen in a heated vat give two sponge.", "Palladium"),
        spotlight("fundamentals:insoluble_residue", "Osmium and ruthenium boil. The residue with a limesand, 250 mB of chlorine and 500 mB of water in a heated vat is oxidised by the hypochlorite to the tetroxides, "
                  "50 mB of pale yellow osmium tetroxide and 100 mB of orange ruthenium tetroxide, and half the time leaves an iridium-rhodium residue.", "The tetroxides"),
        spotlight("fundamentals:ammonium_chlororuthenate", "Hydrochloric acid catches ruthenium tetroxide: 100 mB, 250 mB of acid and an ammonium chloride give the dark red chlororuthenate, reduced like palladium's salt. "
                  "Osmium tetroxide passes on, and 100 mB with 250 mB of hydrogen in a heated vat is osmium sponge.", "Ruthenium and osmium"),
        spotlight("fundamentals:ammonium_chloroiridate", "The last residue, heated with two salt, 250 mB of chlorine and 250 mB of hydrochloric acid, goes into a dark red-brown iridium-rhodium liquor. "
                  "Two ammonium chloride drop iridium as the near-black chloroiridate and leave a rose rhodium liquor; rhodium comes last, two more ammonium chloride in a heated basin dropping the rose chlororhodate. "
                  "Both are reduced under hydrogen.", "Iridium, then rhodium")] + pages_of(
        "None of the six melts at 1,600 °C but palladium; platinum melts at 1,768, rhodium at 1,964, ruthenium at 2,334, iridium at 2,446 and osmium past 3,000. "
        "So the sponge is not cast but pressed, as Wollaston first made platinum malleable: Create's press over a basin on a burner fed a blaze cake sinters each sponge to an ingot, and nine nuggets pack to one."), 2)
    entry("platinum", "uses", "What they are for", "fundamentals:platinum_rhodium_gauze", pages_of(
        "Platinum is a catalyst. Two platinum and two rhenium nuggets and four bauxite powder, heated, make four platinum-rhenium catalyst, "
        "and over one 500 mB of naphtha in a heated vat reforms to 400 mB of gasoline and gives off 100 mB of hydrogen, the catalyst surviving nineteen times in twenty. "
        "Eight platinum nuggets round a rhodium nugget weave the gauze ammonia is burnt over: 250 mB of ammonia and 1,000 mB of air, heated, give 250 mB of nitric acid, Ostwald's process, and the gauze is all but never used up.", "What they are for") + pages_of(
        "The three-way converter: platinum and palladium on ceria burn what an engine leaves, and rhodium breaks down its nitrogen oxides, where four fifths of the world's rhodium goes. "
        "A platinum, a palladium and a rhodium nugget with the cerium oxide make two exhausts instead of one. "
        "Ruthenium lets a superalloy carry more: a ruthenium nugget in the superalloy melt makes nine ingots instead of seven. "
        "An iridium-tipped spark plug outlasts four plain ones, so an iridium nugget, an alumina and a nickel nugget make four, and a platinum nugget three. "
        "And osmium was the first metal filament: an osmium sponge pastes and draws to four filaments, and a light bulb burns one as well as tungsten."), 3)


def power():
    category("power", "Power", "Sunlight into The Factory Must Grow's grid, and the magnets its machines turn on.", "fundamentals:photovoltaic_panel", 5)
    entry("power", "solar", "Solar panels", "fundamentals:photovoltaic_panel", pages_of(
        "A solar panel goes on a rack. Build the rack from steel and set it down facing the way you want, then mount a photovoltaic panel on it: a P and an N semiconductor from The Factory Must Grow under glass, in an aluminium frame, with a silver nugget for the front contacts printed on every cell. "
        "Silicon is doped n-type with a donor from group 15 and p-type with an acceptor from group 13 (Sze, Physics of Semiconductor Devices): four silicon and a white phosphorus, heated, give four N semiconductor, "
        "and four silicon and a boric acid four P, where the Factory used sulfur and aluminium. "
        "Under open sky it feeds the electrical network, 120 volts while the sun is up and up to 200 watts at noon, nothing at night or in shade. Break it and you get the rack and the panel back.", "Solar panels")
        + [crafting("fundamentals:panel_rack"), crafting("fundamentals:photovoltaic_panel")], 0)
    magnets()


def grades():
    java = (JAVA / "magnet/MagnetGrade.java").read_text(encoding="utf-8")
    return [(m, int(a), int(b), int(c), float(f)) for m, a, b, c, f in re.findall(r'[A-Z_]+\("([a-z_]+)", (\d+), (\d+), (\d+), ([\d.]+)F\)', java)]


def magnets():
    rows = " ".join(f"{LANG[f'item.fundamentals.{m}_magnet']}: rated {a:,} °C, lasting loss past {b:,}, Curie {c:,}, {round(f * 100)}% strength."
                    for m, a, b, c, f in grades())
    entry("power", "magnets", "Magnets and heat", "fundamentals:samarium_cobalt_magnet", pages_of(
        "A permanent magnet holds its field only so hot. Past the temperature it is rated to, the field sags, and comes back as it cools. Further on, part of it is lost for good. "
        "At its Curie point it is gone. The Factory's motors, generators, stators, electric pumps and voltmeters are built round one grade of magnet and carry it: the item says which, "
        "and goggles on the block give the grade, the temperature where it stands (the field the thermometers read) and the share of full output it makes.", "Magnets and heat")
        + pages_of(f"In the game, every few seconds: {rows} Output falls from full at the rating to half at the lasting-loss point and to nothing at Curie; "
                   "a motor turns slower and carries less, a generator gives less, an electric pump pushes less. A demagnetised machine stops. "
                   "A voltmeter's needle swings against its magnet, so it reads low by what the heat has taken of its field.", "The grades")
        + pages_of("The real figures, from the makers' grade tables: sintered NdFeB runs to about 80 °C, Curie point 310 to 400 °C. "
                   "With dysprosium or terbium (the SH, UH and EH grades) it runs to 150 to 230 °C. SmCo runs to 250 to 350 °C, Curie 720 to 825. "
                   "Alnico runs to 450 to 550 °C, Curie about 860, but stores far less energy: about 5 MGOe, against 35 to 52 for NdFeB and 16 to 32 for SmCo. "
                   "So aircraft and missiles fly SmCo, alnico lives on in sensors, and every other motor is NdFeB with dysprosium for the heat, "
                   "which is most of why the world wants dysprosium (US Department of Energy, Critical Materials Strategy, 2011).", "Why it matters")
        + pages_of("Plain NdFeB is two neodymium, three iron and a ferroboron under argon; three neodymium, a dysprosium or terbium, six iron and two ferroboron make eight Dy-NdFeB. "
                   "SmCo is two samarium, four cobalt, an iron and four copper and two zirconium nuggets, the Sm2Co17 type every high-temperature grade is. "
                   "Alnico is iron with aluminium, nickel, cobalt and a little copper: five iron, an aluminium, two nickel, two cobalt and three copper nuggets, superheated, "
                   "cast and heat-treated to ten ingots. Polarize any of the four ingots, or an NdFeB, Dy-NdFeB or SmCo plate, to its magnet. "
                   "The magnet is the first thing a motor or generator takes on the belt, and it decides the grade of the whole.", "Making them")
        + pages_of("A motor, generator, stator, electric pump or voltmeter and any magnet in a crafting grid rebuild it round that magnet, at full field: that is how a cooked or demagnetised machine comes back, "
                   "or a cheap one is upgraded. Machines built before grades, and The Factory Must Grow's own magnets still in a chest, count as Dy-NdFeB, "
                   "which is what every NdFeB the mod made then was. The Factory's magnet is no longer made.", "Rebuilding")
        + [spotlight("fundamentals:alnico_magnet", "The horseshoe: cast alnico, painted red, its two poles bare.", "Alnico")], 1)


def plastics():
    category("plastics", "Plastics", "Polyethylene, polypropylene and PVC: the plant's pipe and tank, natural or dyed; and titanium, where plastic is too hot.",
             "fundamentals:orange_plastic_block", 6)
    entry("plastics", "plastics", "Plastics", "fundamentals:ziegler_natta_catalyst", pages_of(
        "The Factory's olefins do not polymerise by being heated. Polyethylene and polypropylene are made over a Ziegler-Natta catalyst, a titanium chloride: "
        "Natta's first was titanium tetrachloride reduced by aluminium powder to violet TiCl3. 250 mB of titanium tetrachloride, 100 mB of argon and an aluminium powder, heated, "
        "make four catalyst. In the Factory's heated vat 500 mB of ethylene or propylene over a catalyst gives 500 mB of molten plastic, and the catalyst back nine times in ten; "
        "the casting machine and plastic sheets go on as before.", "Plastics") + pages_of(
        "PVC is what chemical plants pipe their acids in. 500 mB of ethylene and 500 mB of chlorine, heated, give 500 mB of vinyl chloride and 500 mB of hydrochloric acid, "
        "the hydrogen chloride the cracking gives off. 250 mB of vinyl chloride stirred hot into 250 mB of water polymerises as droplets to a PVC resin, a white powder, "
        "and a resin pressed on a heated basin is a grey PVC sheet. A PVC sheet does anything a plastic sheet does in a pipe, a tank, a cell or a casing.") + pages_of(
        "Natural plastic is milky: the tank, the cells and the Factory's plastic pipes let a little light and the shape of what is behind them through. "
        "Pigment makes it opaque. Eight plastic blocks round a dye make eight of that colour, and one goes back to nine sheets. "
        "A dye on a plastic tank colours the whole tank, and on a plastic pipe dyes that pipe; a dyed pipe breaks back to a plain one.", "Dyes") + pages_of(
        "Plants colour their lines by what is in them, after ASME A13.1: orange for toxic and corrosive, so the acids and liquors; yellow for flammable, the olefins, "
        "kerosene and the organic; green for water; blue for compressed air. A line you can read from across the plant is a line nobody cuts into by mistake.", "Colour codes")
        + [crafting("fundamentals:plastics/orange_plastic_block")], 0)
    softens = hazard("PLASTIC_SOFTENS")
    entry("plastics", "titanium", "Titanium pipework", "fundamentals:titanium_pipe", pages_of(
        f"Plastic has one weakness: heat. Polyethylene and polypropylene pressure pipe is rated to 80 or 95 °C and is soft by {softens}. "
        f"A plastic pipe, pump or valve in use whose wall is past {softens} °C, from a burner, fire or lava beside it or lava inside, "
        f"sags and bursts in {hazard('softeningChance')} seconds or so, and a plastic tank in ten times that. A steam boiler is no place for one.", "Titanium pipework") + pages_of(
        "Titanium is what a chemical plant pipes hot, corrosive work in. It is a reactive metal, but in air or water it grows an oxide skin a few "
        "nanometres thick that heals as fast as anything scratches it, so long as there is something oxidising about: chlorides, hot brine, seawater, "
        "wet chlorine, nitric acid and even aqua regia leave it alone, and it keeps its strength hot. Two titanium plate and a titanium ingot make four "
        "Titanium Pipe; a cogwheel on one makes a pump, a plate a valve, and two plate round a barrel a tank.") + pages_of(
        "It has its own weaknesses. Hydrofluoric acid dissolves the skin and the metal under it: a titanium pipe of it bursts in "
        f"{hazard('fluorideChance')} seconds or so, quicker than copper goes under any acid, so keep it in plastic. Dry chlorine, with no water to mend "
        f"the skin, burns titanium, and chlorine gas eats it as an acid eats copper. Hydrochloric acid it shrugs off cold, and not past {hazard('TITANIUM_HCL')} °C. "
        "See Schutz and Thomas, Corrosion of Titanium and Titanium Alloys, in the ASM Handbook, vol. 13.")
        + [crafting(f"fundamentals:{name}") for name in TITANIUM_PIPEWORK], 1)


def book():
    write(BOOK_DATA / "book.json", {
        "name": "Fundamentals: First Principles",
        "landing_text": "Real minerals, real deposits, and the real road from rock to metal. $(br2)Everything here is as it is in the ground and in the plant; where the book quotes a number, the game was read for it. $(br2)The advancements screen has a periodic table: each element lights up the first time you hold anything made of it.",
        "version": "1", "creative_tab": "fundamentals:minerals", "use_resource_pack": True, "show_progress": False,
        "book_texture": "patchouli:textures/gui/book_brown.png", "model": "patchouli:book_brown",
    })
    write(RECIPES / "first_principles.json", {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "patchouli"}],
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "minecraft:book"}, {"item": "fundamentals:raw_hematite"}],
        "result": {"id": "patchouli:guide_book", "components": {"patchouli:book": f"fundamentals:{BOOK}"}}})


def check():
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
    liquids_entry()
    metals()
    platinum()
    power()
    plastics()
    check()
    n = len(list(BOOK_ASSETS.glob("entries/*/*.json")))
    print(f"book written: {n} entries")


if __name__ == "__main__":
    main()
