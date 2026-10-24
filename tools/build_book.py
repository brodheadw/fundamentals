#!/usr/bin/env python3
import json
import re
import shutil

from common import ASSETS, DATA, JAVA, raw_item, write
from build_ore_data import DEPOSITS, DISPLAY, PLACERS
from build_separation_data import CURIE, MOMENTS, TITANIUM_PIPEWORK, ZINC_REDUCTION
from build_oxidation_data import METALS as AGEING, PACE
from build_heat_data import REFERENCES, liquids
from build_uses_data import CONVERTER, PIG_IRON_PER_ORE, PUDDLED

BOOK = "first_principles"
BOOK_DATA = DATA / f"patchouli_books/{BOOK}"
BOOK_ASSETS = ASSETS / f"patchouli_books/{BOOK}/en_us"
RECIPES = DATA / "recipe"

PAGE_CHARS = 330
NUMBERS = dict(enumerate(("zero one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen").split()))

WHERE = {
    "anywhere": "anywhere in the overworld", "porphyry": "under mountains and hills, down into the deep dark beneath them", "arid_oxide": "in badlands, savanna and desert",
    "laterite": "under jungle, savanna and mangrove", "pegmatite": "in mountains, hills and badlands and the deep dark beneath them", "carbonatite": "deep under mountains and badlands, in the deep dark",
    "alkaline": "under taiga and snowy country and in the deep dark", "ion_clay": "under jungle", "wetland": "in swamps and bogs", "hydrothermal": "in mountain and hill country and the deep dark beneath it",
    "placer": "in beach and river sand", "temperate": "under temperate forest, plains, hills and swamp", "alpine": "in the mountains",
    "fumarole": "in the Nether, on the basalt deltas and in the soul sand valleys",
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
        "and you have found the metal; this chapter is the prospector's list. The Factory Must Grow's lead, lithium and nickel ores and its "
        "galena and bauxite layers no longer form; any already in the ground mine as raw galena, spodumene, pentlandite and bauxite.", "Deposits, not blobs"), 0)
    for i, (name, spec) in enumerate(DEPOSITS.items()):
        kind, host, minerals, *_rest = spec
        where = WHERE.get(spec[6], spec[6])
        y0, y1 = spec[7]
        host_text = "" if host is None else (f" of {pretty(host)}" if not host.endswith("_ore") else f", the whole body {pretty(host)}")
        parts = [f"{pretty(m)} in {STYLE.get(style, style)}" for m, _grade, style in minerals]
        text = (f"{KIND[kind].capitalize()}{host_text}, {where}, between y {y0} and {y1}. "
                + (f"Carries {', '.join(parts)}." if parts else "") + " Dig at the core: the edges are lean.")
        icon = raw_item(minerals[0][0]) if minerals else f"fundamentals:{host}"
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
        "Copper comes straight out of the bloomery from malachite, azurite and cuprite. Galena has to be roasted first, in a heated basin with air, which "
        "drives off its sulfur as sulfur dioxide; the roasted ore then gives lead bullion, lead still holding its silver, which a furnace remelts to lead.", "Bloom and hammer") + pages_of(
        "A furnace never reduces iron ore, and neither does a fan. Create's crushing wheels grind hematite, magnetite, goethite and roasted siderite to crushed iron ore, "
        "and that goes to The Factory Must Grow's blast furnace, which has the coke and the heat, and gives pig iron: see Iron and steel. Crimsite is the iron stone and crushes to hematite; "
        "washed gravel leaves a little magnetite black sand. The slag every smelt leaves is The Factory Must Grow's own: see Slag.")
        + [crafting("fundamentals:smithing_hammer")], 2)
    entry("ironworking", "slag", "Slag", "tfmg:slag", pages_of(
        "Every smelt leaves slag, the rock of the ore and the flux melted together. A bloomery's is fayalite, iron silicate, and half or more of it is still iron oxide, "
        "because without lime a bloomery cannot pull iron back out of a silicate. The Factory Must Grow's slag is the same item, from its blast furnace and from every smelt here.", "Slag") + pages_of(
        "So old slag was smelted again. A slag in the bloomery with a charcoal, like any ore, gives two iron nuggets. The Romans' cinder heaps in the Forest of Dean and the Weald fed "
        "the blast furnaces of the seventeenth century, and the Etruscan heaps at Populonia were dug up and smelted again in the twentieth.", "Smelting it again") + pages_of(
        "Create's crushing wheels break a slag to gravel, and one time in ten free an iron nugget, as a slag works passes crushed slag under a magnet for its prills of metal "
        "and sells the rest as road stone and ballast. The Factory Must Grow's concrete and asphalt take slag as it is, and ground with clinker it is slag cement (see Cement).", "Aggregate") + pages_of(
        "It is not fertiliser. Thomas meal, the ground slag farmers spread from the 1880s, came from the basic Bessemer converter blowing the phosphoric pig iron of the minette ores with lime, "
        "a slag of 15 to 20 per cent phosphate. No iron here carries phosphorus: a bloomery's slag is an iron silicate, the Factory's blast furnace slag a lime silicate, and neither has any phosphate to give. "
        "Fertiliser comes from sulfuric and phosphoric acid and ammonia (see Fertilisers).", "Not Thomas meal") + pages_of(
        "Sources: Tylecote, A History of Metallurgy (2nd ed., 1992); Cleere and Crossley, The Iron Industry of the Weald (1985); "
        "Proctor and others, \"Physical and chemical characteristics of blast furnace, basic oxygen furnace, and electric arc furnace steel industry slags\", "
        "Environmental Science and Technology 34 (2000); Ullmann's Encyclopedia of Industrial Chemistry, \"Phosphate Fertilizers\".", "Sources"), 3)
    entry("ironworking", "mortar", "Mortar and pestle", "fundamentals:mortar_and_pestle", pages_of(
        "A mortar and pestle grinds by hand what Create's millstone grinds by power: grain to flour, bone to meal, and coloured minerals "
        "to the pigments painters have ground since antiquity: hematite red, goethite yellow, malachite green, azurite blue, pyrolusite black, "
        "cinnabar vermilion. Hold the mortar in one hand and the material in the other.", "Mortar and pestle")
        + [crafting("fundamentals:mortar_and_pestle")], 4)


def steel():
    category("steel", "Iron and steel", "The blast furnace and what comes after it: pig iron cast as it is, puddled to wrought iron, or blown to steel.",
             "tfmg:cast_iron_ingot", 1)
    entry("steel", "pig_iron", "Pig iron", "tfmg:blast_furnace_output", pages_of(
        "A blast furnace does not make steel. Coke burning in a hot blast reduces the ore as it sinks down the stack, and the iron melts, and iron that melts "
        "against coke dissolves carbon: three and a half to four and a half per cent of it, and silicon from the ash. What runs out of the hearth is pig iron, "
        "named for the sand beds it was cast in, a sow feeding a row of pigs. It is hard and brittle and cannot be forged.", "Pig iron") + pages_of(
        f"Crushed iron ore and a limesand in The Factory Must Grow's blast furnace give {PIG_IRON_PER_ORE} mB of molten pig iron, 144 mB of molten slag and furnace gas; "
        "a raw iron lump, twice as much. That is half as much iron again as a bloomery gets from the same ore. A bloomery never melts its iron and loses much of it "
        "to the slag as iron oxide; the blast furnace's lime slag carries almost none, and nearly all the iron goes into the pig (Tylecote, A History of Metallurgy).") + pages_of(
        "Pig iron goes three ways: cast as it is, puddled to wrought iron, or blown to steel. Sources: Tylecote, A History of Metallurgy (2nd ed., 1992); "
        "Gale, Iron and Steel (1969); Ullmann's Encyclopedia of Industrial Chemistry, \"Iron\" and \"Steel\".", "Three ways"), 0)
    entry("steel", "cast_iron", "Cast iron", "tfmg:cast_iron_ingot", pages_of(
        "Run into a mould, pig iron is cast iron, and it casts where steel will not: at four per cent carbon it melts nearly 400 °C lower than pure iron and "
        "runs thin into every corner of the mould. In grey iron the carbon comes out as flakes of graphite, which make it machine cleanly and damp vibration "
        "and make it weak in tension: good for pipes, cylinders, frames, flywheels and machine beds, bad for anything that has to bend.", "Cast iron") + pages_of(
        "The Factory Must Grow's casting basin casts 144 mB of pig iron to a cast iron ingot, and its cast iron pipes, tanks, frames, flywheels and blast furnace "
        "come from that. Before there is a blast furnace a little is made by hand: an iron ingot and a coal compacted in a heated basin, the iron taking up the carbon "
        "as it would in the hearth."), 1)
    entry("steel", "puddling", "Puddling", "fundamentals:puddle_ball", pages_of(
        "Wrought iron in bulk came from pig iron by Henry Cort's puddling furnace, patented in 1784. A reverberatory furnace keeps the coal fire apart from the iron, "
        "so its sulfur stays out. The puddler melts pig on a hearth lined with iron oxide and works it with a rabble through the door; the oxide and the air burn "
        "the carbon out as bubbling carbon monoxide, and as the carbon goes the melting point climbs past the furnace's heat and the iron comes to nature, "
        "thickening to a pasty mass he gathers into balls (Gale, Iron and Steel).", "Puddling") + pages_of(
        "Joseph Hall's wet puddling, pig boiling, fed the bath more oxide, roasted cinder and hammer scale, which the boil reduces back to iron, so it lost far less "
        "than Cort's dry hearth. Here two cast iron ingots, or 288 mB of molten pig iron, and an iron oxide to fettle with, a raw hematite or a rusty iron ingot, "
        "under a mixer over a superheated burner make a puddle ball and a slag, the tap cinder. Rust is iron oxide, so a rusty ingot is just what a puddler wants, and its iron comes back.") + pages_of(
        f"The ball comes out of the furnace still full of liquid slag. Shingling squeezes it out: a press, or the smithing hammer by hand, beats a ball into {NUMBERS[PUDDLED]} "
        "iron ingots of wrought iron, under a tenth of a per cent carbon with threads of slag through it. From the ore that is a third to a half again what the bloomery gives, "
        "and it burns no charcoal.", "Shingling") + [crafting("fundamentals:uses/shingling_by_hand")], 2)
    loss = 1 - CONVERTER["steel"] / CONVERTER["pig_iron"]
    entry("steel", "converter", "The converter", "tfmg:steel_ingot", pages_of(
        "Steel is iron with a few tenths of a per cent of carbon, tougher than cast iron and harder than wrought. Before 1856 it was made by the ton only by cementation "
        "and the crucible. Henry Bessemer blew air up through molten pig iron in a refractory-lined converter and found the bath got hotter, not colder: the silicon, "
        "manganese and carbon burning out of it give heat enough to keep it molten with no fuel at all. A blow of twenty minutes did what a puddler did in a shift.", "The converter") + pages_of(
        "Robert Mushet's spiegeleisen, a manganese pig iron added after the blow, took up the oxygen left in the steel; Thomas and Gilchrist's basic lining of 1878 let lime "
        "take out phosphorus. Since the first at Linz in 1952 the basic oxygen furnace has blown pure oxygen in from above, and it makes most of the world's steel (Ullmann's, \"Steel\").") + pages_of(
        f"Here {CONVERTER['pig_iron']} mB of molten pig iron, {CONVERTER['air']} mB of air and a limesand in any of The Factory Must Grow's vats, with no burner under it and no machine on it, "
        f"blow to {CONVERTER['steel']} mB of molten steel, {CONVERTER['slag']} mB of slag and furnace gas: about {round(loss * 100)} per cent of the metal goes off as burnt carbon "
        "and silicon and as iron oxide in the slag. The casting basin makes the ingots. The Factory's arc furnace makes steel too, as an electric arc furnace does from scrap "
        "and reduced ore, with no pig iron."), 3)


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
                  "Washed under an encased fan, the light grains wash away and what stays is a mixed concentrate: light from monazite and loparite, heavy from xenotime and euxenite. "
                  "Monazite is a clean sand and keeps 85 in 100, as a mineral-sands plant does; the ground pegmatite minerals keep five in eight.", "Concentrate"),
        spotlight("fundamentals:bastnasite_dust", "Bastnäsite will not wash: it is floated, as in every carbonatite mill. Two bastnäsite dust beaten with 250 mB of water and 100 mB of naphthenic acid, the fatty-acid collector, "
                  "in a basin under a mixer, and the rare earth carbonate comes off the top as bastnäsite concentrate, one and a third of the time two: "
                  "Mountain Pass floats 65 to 70 per cent of what it mills.", "Flotation")], 0)
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
        "Argon is spun out of 1,000 mB of air in a centrifuge vat, nine millibuckets at a time with The Factory's one of neon. Calcium is the plant's own calcium chloride, two of it electrolysed molten on electrodes in a heated vat to two ingots. "
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
                  "Its dust is the danger, breathed in: held, or left in an open basin, it hurts and sickens you unless Create's diving helmet on a filled backtank keeps it out. "
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
        "Sulfuric acid is The Factory Must Grow's fluid, made its own way from sulfur and saltpetre in a vat or by the contact process (see Heavy chemicals), and every other acid starts from it. "
        "Two salt and 500 mB of sulfuric acid in a heated basin give 500 mB of hydrochloric acid, the salt-cake process; salt is seawater boiled down, or rock salt ground (see Salt and seawater, under Geology). "
        "Two raw fluorite and 500 mB, heated, give hydrofluoric acid. Two nitrate dust and 500 mB, heated, give nitric acid, which boils off the saltpetre as it did from Glauber's retort. "
        "Two bone meal and 500 mB, cold, give phosphoric acid and a phosphogypsum, the wet process with bone for phosphate rock; cracking monazite frees more.", "Making the acids") + pages_of(
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
        "First water gas: a coal coke and 500 mB of water in a heated vat without a mixer give 1,000 mB of carbon monoxide and hydrogen. Shifted with another 500 mB of water, heated, 1,000 mB of water gas gives 1,000 mB of hydrogen and 500 of carbon dioxide; "
        "that is also the hydrogen cobalt, molybdenum and tungsten are reduced under.", "The extractants") + pages_of(
        "The oxo process: 500 mB each of propylene, from cracked naphtha, water gas and hydrogen on a cobalt ingot, heated, give 250 mB of 2-ethylhexanol. The cobalt is the catalyst and comes back nineteen times in twenty. "
        "Phosphorus: two bone meal, a coal coke and a sand in a vat with two electrodes, superheated, give a white phosphorus and a slag, as the electric furnace does at 1,500 °C. "
        "A white phosphorus and 750 mB of chlorine in a heated basin give 500 mB of phosphorus trichloride.") + pages_of(
        "P204 is D2EHPA: 500 mB of 2-ethylhexanol, 250 of phosphorus trichloride, 500 of air and 250 of water, cold, give 250 mB of it, the air taking the phosphorus to phosphate. "
        "P507 is EHEHPA: the same with 250 mB of caustic soda in place of the air and water, heated, which rearranges the phosphite to a phosphonate, carbon bonded to phosphorus, and takes one ester back off. Both give 500 mB of hydrochloric acid back. "
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
             for metal, (kind, d, dry, wet, _) in ((m, (k, PACE * d, dr, w, o)) for m, (k, d, dr, w, o) in AGEING.items())]
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
    category("metals", "The other metals", "Cobalt, zinc, nickel and Mond's carbonyl, the porphyry chain (copper, molybdenum and the rhenium hiding in it), tungsten, chromium, tin, titanium, the silver in lead, zirconium and hafnium, beryllium, aluminium, lithium, uranium, graphite, antimony, niobium and tantalum, potash and gold, silicon from quartz to wafer, and germanium, indium, gallium and cadmium.", "fundamentals:cobalt_ingot", 3)
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
        "A copper sulfide smelts not to copper but to matte. Chalcopyrite roasted in a heated basin with air, or the calcine bornite, chalcocite and covellite roast to, melts in the bloomery to copper matte and slag. "
        "Two matte and a sand, superheated, are the converter: the air burns off the sulfur and the iron, which the sand fluxes to slag, and leaves two blister copper. The blast furnace fire-refines blister to copper. "
        "Two raw molybdenite roasted in a heated basin with 1,000 mB of air give a molybdenum trioxide, one time in five a second, and 500 mB of sulfur dioxide for the acid plant, and one time in ten a rhenium flue dust. "
        "The missing trioxide is the mill's: molybdenite riding in a copper ore floats at 50 to 60 per cent. "
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
        "the tungsten standing in for the tantalum. A tantalum nugget in the melt, as CMSX-4 has its 6.5 per cent, makes nine ingots where it made seven, "
        "as a hafnium or a ruthenium nugget does.", "Superalloy") + pages_of(
        "The aluminium is what makes it a superalloy. It precipitates as ordered Ni3Al, gamma-prime, which keeps the alloy strong almost to its melting point, and with the chromium it grows "
        "the alumina scale that keeps it from burning in the hot gas; tungsten and rhenium stiffen it against creep.") + pages_of(
        "The Factory Must Grow's turbine blade takes its plates, coated as a real blade is: a yttria-stabilised zirconia thermal barrier over an MCrAlY bond coat. "
        "Four nickel, two cobalt, a chromium, an aluminium and a yttrium nugget, superheated under 100 mB of argon, atomise to eight MCrAlY powder, NiCoCrAlY as Amdry 365 is "
        "(Ni-23Co-17Cr-12.5Al-0.5Y); its yttrium pins the alumina scale to the blade.", "The blade") + pages_of(
        "Molybdenum goes into steel as the roasted trioxide, not as metal. A molybdenum trioxide and six blocks of steel, superheated, make 54 molybdenum steel, about one per cent molybdenum, "
        "the share in the creep-resistant chromium-molybdenum boiler and pressure-vessel steels (ASTM A387 grade 22 is 0.9 to 1.1 per cent, 4140 0.15 to 0.25), their chromium left out. "
        "Its plates make the heavy machinery casing.", "Molybdenum steel"), 2)
    entry("metals", "tungsten", "Tungsten", "fundamentals:tungsten_ingot", pages_of(
        "Both tungsten ores are heavy, and a wash under an encased fan is their gravity table: three raw ore in four come through as concentrate, as gravity and flotation plants recover 65 to 80 per cent. "
        "Wolframite, from the tin veins, will not open to acid. Two wolframite concentrate and 500 mB of caustic soda in a heated chemical vat with a mixer, the pressure digester at 150 to 200 °C, "
        "give 500 mB of sodium tungstate liquor, the iron and manganese left behind as hydroxides; or two concentrate and two soda ash fused over a superheated burner and leached in 500 mB of water give the same. "
        "Scheelite, from the limestone skarns, opens to hot hydrochloric acid: two scheelite concentrate and 500 mB, heated, throw down two yellow tungstic acid.", "Tungsten") + pages_of(
        "Either way the tungsten is taken up as ammonium tungstate and crystallised as ammonium paratungstate, APT, the form tungsten is traded in. "
        "500 mB of the liquor and 250 mB of ammonia, heated, give two APT, the amine extraction and ammonia strip folded into the one step; two tungstic acid dissolved in 250 mB of ammonia give two as well. "
        "A blast furnace calcines APT to the canary-yellow trioxide, and hydrogen in a heated chemical vat reduces two oxide to two ingots (Lassner and Schubert, Tungsten, 1999).", "APT") + pages_of(
        "One ingot draws to four filaments on a stonecutter, and the Factory's light bulb now burns one. In a vacuum the filament boils away and blackens the glass; Langmuir filled the bulb with an inert gas in 1913, "
        "and lamps have been argon-filled since: a filament, a framed glass, four copper and three steel nuggets and 50 mB of argon under a mixer make three bulbs where crafting makes two. "
        "One ingot and two coals, superheated, carburise to two tungsten carbide, "
        "and Create's mechanical drill now bites with it. Ten per cent of the superalloy is tungsten.", "What it is for"), 3)
    entry("metals", "zinc_nickel", "Zinc and nickel", "fundamentals:zinc_oxide", pages_of(
        "Neither melts out of its ore in a furnace. Sphalerite is zinc sulfide, and roasts to zinc oxide only in a heated basin with 500 mB of air, giving off its sulfur as sulfur dioxide; "
        "smithsonite and hemimorphite, the old calamine, calcine to it on a fire. "
        "Zinc boils at 907 °C, below the heat that reduces it, so it was distilled from a sealed retort packed with charcoal: "
        "a zinc oxide and a charcoal in a basin over a blaze burner fed a blaze cake give a zinc ingot. Cadmium boils at 767 °C and comes over first, so one retort in ten "
        "also gives a cadmium sponge: Stromeyer found the element in 1817 in calamine. Asurine, the zinc stone, crushes to smithsonite. "
        "Zinc oxide is also what every sulfur cure of rubber needs to work, so the Factory's rubber takes one.", "Zinc") + pages_of(
        "Since the 1910s most zinc has been won in water instead: roasted, leached in sulfuric acid and electrowon, and now nine tenths of it is (Sinclair, The Extractive Metallurgy of Zinc). "
        "Two zinc oxide, 500 mB of sulfuric acid and 500 mB of water in a heated vat with a mixer give 1,000 mB of zinc leach liquor, and one leach in five leaves a zinc leach residue, "
        "the ferrite and lead sulfate that hold the germanium and indium. The copper, cobalt and cadmium in the liquor would foul the cathode, so it is stirred hot with a zinc nugget, "
        "zinc dust that cements them out: 1,000 mB of zinc sulfate liquor, and one batch in five a cadmium sponge. 500 mB of that on two electrodes in a vat give a zinc ingot "
        "and 250 mB of sulfuric acid back to the leach.", "Electrolytic zinc") + pages_of(
        "The purification comes out in its order: copper first, as a copper cement that a blast furnace fire-refines to a copper ingot; then cobalt, as a cobalt cake that "
        "250 mB of sulfuric acid, heated, turn to cobalt sulfate; then cadmium. Manganese in the liquor plates out on the anodes as anode mud, MnO2, one cell in twenty, "
        "which a coke and 500 mB of sulfuric acid, heated, turn to two manganese sulfate. The roaster gas carries the ore's mercury, scrubbed out as calomel by the "
        "Boliden-Norzink process one roast in twenty; a calomel and a limesand, heated, give mercury back.", "What else is in the liquor") + pages_of(
        "The leach residue is mostly zinc ferrite, which a neutral leach cannot touch. Two of it with 500 mB of sulfuric acid and 100 mB of ammonia in a heated vat give "
        "500 mB more zinc leach liquor, a jarosite and a lead-silver residue: the jarosite process of 1965, which drops the iron as ammonium jarosite (Arregui and others, Lead-Zinc-Tin '80, TMS-AIME, 1980). "
        "The lead-silver residue goes to a lead smelter; in a bloomery it gives lead bullion and a slag. The jarosite, holding lead, arsenic and cadmium, is impounded in lined ponds: "
        "nine pack into a jarosite pond.", "Jarosite") + pages_of(
        "Half the world's zinc goes on steel (International Zinc Association). Dipped in molten zinc at about 450 °C, steel takes a coat that corrodes in its place and, scratched through, "
        "still goes before the steel does. Four heavy plates and a zinc nugget, heated, give four galvanized steel plates, which do anything a steel plate does and never rust; "
        "a bare heavy plate rusts as a steel ingot does.", "Galvanizing") + pages_of(
        "Carbon does not part nickel from iron: it reduces both, and they alloy. Pentlandite roasted in a heated basin with air and smelted with a charcoal, superheated, gives a ferronickel ingot and a slag, "
        "and whatever platinum it carried is lost in it. Nickel laterite, a weathered rock of 1.5 to 2.5 per cent nickel, is smelted whole: four of it with two charcoal, superheated, "
        "give one ferronickel and two slag, as the rotary kilns and electric furnaces of New Caledonia and Indonesia smelt it at about 1,550 °C.", "Ferronickel") + pages_of(
        "Real ferronickel runs 20 to 40 per cent nickel, and nickel pig iron less (Crundwell and others, Extractive Metallurgy of Nickel, Cobalt and Platinum Group Metals, 2011); here an ingot is a third nickel. "
        "None of it is ever nickel metal. It goes where two thirds of the world's nickel goes, into stainless steel, which wants the iron anyway: three ferrochrome, three ferronickel and four steel, superheated, make ten stainless. "
        "Nickel metal comes from sulfide matte: leached and electrowon, under the platinum metals, or by Mond's carbonyl.", "Nickel"), 4)
    entry("metals", "mond", "Mond nickel and its poisons", "fundamentals:nickel_pellets", pages_of(
        "Ludwig Mond found in 1890 that carbon monoxide carries nickel off as a gas at 50 °C and gives it back when heated past 180 °C, and iron, copper and cobalt hardly at all (Mond, Langer and Quincke, Journal of the Chemical Society, 1890). "
        "His refinery at Clydach in Wales has run on it since 1902, and Copper Cliff in Sudbury runs it under pressure.", "The Mond process") + pages_of(
        "Converter matte roasted dead on a campfire or in a smoker burns off its sulfur and leaves nickel oxide. Two nickel oxide and 1,000 mB of water gas in a heated chemical vat give 500 mB of nickel carbonyl, "
        "the water gas's hydrogen reducing the oxide at 400 °C and its monoxide taking the nickel, Ni + 4 CO to Ni(CO)4; one time in ten the residue is a platinum group concentrate, as Clydach's residues fed the Acton refinery.") + pages_of(
        "500 mB of carbonyl in a heated vat decomposes at about 230 °C on hot pellets to two nickel pellets, 99.97 per cent nickel, and 500 mB of the gas goes back round. Neither gas reaction needs an industrial mixer, so the first nickel can make that mixer's steel mechanism. "
        "A heated press over a basin squeezes a pellet into a nickel ingot.") + pages_of(
        "Nickel carbonyl is among the most poisonous gases in industry. NIOSH puts the level immediately dangerous to life at 2 parts per million, and its faint musty smell comes only above that. "
        "It does not eat a pipe or a tank, but where it gets out, from an open pipe end, a broken pipe, tank or vat, or a basin, it reaches three blocks: "
        "headache, nausea and weakness at once, and a hard hit every second.", "Nickel carbonyl") + pages_of(
        "The worst comes later. A day or so after exposure the lungs fill (pulmonary oedema), however well the victim felt; here it comes half a minute later, as withering that lasts four seconds for each second breathed. "
        "Only Create's diving helmet on a filled backtank keeps it out, and it burns two of the tank's air a second doing so. Keep the carbonyl in closed pipes and tanks, and wear the mask to break into them.") + pages_of(
        "The dusts of the nickel smelter cause lung and nasal cancer: the men who roasted and leached matte at Clydach died of them at many times the expected rate (Doll, 1958), "
        "and the International Agency for Research on Cancer puts nickel compounds in its first group. Nickel matte, converter matte, roasted pentlandite and nickel oxide held in the hand or stirred in a basin are breathed by everyone near. "
        "And a sulfide roasting on a lit campfire or in a lit smoker or furnace, cobaltite, stibnite or matte, gives off sulfur dioxide that stings anyone within two blocks. "
        "Roasted in a heated basin with air instead, the gas is caught to make acid (see Sulfuric acid).", "The dusts and the fume"), 4)
    entry("metals", "chromium", "Ferrochrome and chromium", "fundamentals:ferrochrome_ingot", pages_of(
        "Chromite is chromium's only ore, black seams in the gabbro of the deep layered intrusion. A millstone or crushing wheels grind it to a brown powder, and a wash under an encased fan "
        "leaves the heavy chromite behind as concentrate, five eighths of what goes in. Iron in chromite reduces along with the chromium, so smelting gives not chromium but ferrochrome: "
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
        "That weight is how it is concentrated: a raw cassiterite washed under an encased fan leaves a tin concentrate behind seven times in ten; a gravity tin plant loses a quarter to a third of its tin to the tailings. "
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
        "and litharge is the paste on a lead-acid cell's plates (see Batteries, under Power).") + pages_of(
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
        "and leaves 400 mB of spent Bayer liquor, the caustic that goes back to the digester: two bauxite powder and 500 mB of it digest as caustic soda does. "
        "A blast furnace calcines the hydroxide at about 1,100 °C to alumina. The spent liquor carries the bauxite's gallium; see Germanium, indium, gallium and cadmium.", "Bayer") + pages_of(
        "Red mud is the industry's great waste, more than a tonne for every tonne of alumina, piled in ponds; it is also the largest scandium resource there is. "
        "Four red mud and 500 mB of hydrochloric acid, heated, leach out 50 mB of scandium liquor, about a tenth of what two thortveitite give, as Rusal leaches its residue in the Urals. "
        "The rest has no use: nine pack into a block of bauxite residue, the dry stack a modern refinery builds instead of a pond.", "Red mud") + pages_of(
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
    entry("metals", "waste", "Waste and tailings", "fundamentals:red_mud_block", pages_of(
        "Most of what a plant throws off goes somewhere: the separation plant's spent liquor to calcium chloride, the Kroll process's magnesium chloride back to the cell, "
        "the fluorspar of a calcium reduction back to hydrofluoric acid, the tetroxide still's residue to iridium and rhodium. A few things have no use, as in the real industry, "
        "and are packed into blocks and put back in the ground, as a tailings dam or a dry stack holds them: the clarifier's sludge, red mud, and the thorium residue that is not made into mantles. "
        "Anything else burns in lava.", "Waste and tailings") + pages_of(
        "Some are poison. Beryllium compounds, nickel matte and oxide dust, thorium residue and, with Chemica, the arsenic dust gold ore gives up are toxic dusts: "
        "held, or left in an open basin, they hurt and sicken anyone near who is not wearing Create's diving helmet on a filled backtank. "
        "Mercury and nickel carbonyl are vapours and acids fume; see their own entries.", "Toxic dusts") + pages_of(
        "Sources: Evans, \"The History, Challenges, and New Developments in the Management and Use of Bauxite Residue\", Journal of Sustainable Metallurgy 2 (2016); "
        "IAEA, Radiation Protection and NORM Residue Management in the Production of Rare Earths from Thorium Containing Minerals, Safety Reports Series 68 (2011); "
        "ATSDR, Toxicological Profile for Arsenic (2007).", "Sources"), 14)
    entry("metals", "lithium", "Lithium", "tfmg:lithium_ingot", pages_of(
        "Spodumene, LiAlSi2O6, is the pale pyroxene of the lithium pegmatites. A blast furnace calcines it at about 1,050 °C from alpha to beta-spodumene, which opens to acid where the alpha form will not. "
        "Two calcined spodumene, 250 mB of sulfuric acid and 500 mB of water, heated, are the acid roast at about 250 °C and the water leach, and give 500 mB of lithium sulfate liquor. "
        "Two soda ash in the hot liquor throw down two lithium carbonate, the sodium sulfate staying in solution.", "Spodumene") + pages_of(
        "Lithium carbonate is what the trade sells. For the metal it goes to the chloride: two carbonate and 500 mB of hydrochloric acid, heated, give two lithium chloride and 250 mB of carbon dioxide. "
        "Lithium cannot be won from water, so the dry chloride is electrolysed molten. Alone it melts at 610 °C; with potassium chloride, the LiCl-KCl eutectic every lithium cell runs, "
        "it is liquid at about 450: two chloride and a potash on two electrodes in a heated vat give a lithium ingot, half the time three nuggets more, and 250 mB of chlorine, "
        "and the potash comes back nineteen times in twenty, as the bath does.", "Carbonate to metal") + pages_of(
        "About a third of the world's lithium comes not from rock but from the brines under the salt flats of Chile, Argentina and Tibet, evaporated in ponds and precipitated with soda ash; "
        "there is no such brine in the pack yet.") + pages_of(
        "Sources: Ullmann's Encyclopedia of Industrial Chemistry, \"Lithium and Lithium Compounds\"; the Talison and Tianqi spodumene flowsheets; USGS, Mineral Commodity Summaries 2025, Lithium.", "Sources"), 13)
    entry("metals", "uranium", "Uranium", "fundamentals:yellowcake", pages_of(
        "Uraninite, UO2, is pitchblende: black, pitchy and heavy in the hand, about nine grams to the cubic centimetre against galena's 7.6. "
        "It lies in two kinds of ground. Calcite veins in mountain country carry it with native silver and cobaltite, the five-element veins of Joachimsthal (Jáchymov) in the Erzgebirge, "
        "where Klaproth's element was found in Saxon pitchblende in 1789 and the Curies took radium from the mine's residues. "
        "And roll-fronts in desert sandstone: groundwater carrying uranium drops it as uraninite where it meets reduced rock, as in Wyoming and Kazakhstan. Euxenite and monazite carry a little too.", "Uranium") + pages_of(
        "A millstone or crushing wheels grind raw uraninite to dust. Uraninite is uranium(IV) and will not dissolve as it is; an oxidant takes it to uranyl, uranium(VI), which sulfuric acid does take: "
        "two uraninite dust, a raw pyrolusite and 500 mB of sulfuric acid, heated, give 500 mB of uranyl sulfate liquor, the manganese dioxide the oxidant acid-leach mills have long used. "
        "A mill concentrates the uranium by solvent extraction with an amine, or by ion exchange, strips it and throws it down with ammonia. Here that is one step: "
        "500 mB of liquor and 250 mB of ammonia give two yellowcake, ammonium diuranate, bright yellow.") + pages_of(
        "Enrichment wants a gas. A yellowcake, 250 mB of hydrogen and 500 mB of hydrofluoric acid in a heated vat with a mixer reduce it to the dioxide and hydrofluorinate it to green salt, uranium tetrafluoride. "
        "Green salt and 250 mB of fluorine in a heated vat give 500 mB of uranium hexafluoride, a white solid that sublimes at 56.5 °C, the one uranium compound volatile enough to enrich, "
        "as at Honeywell's Metropolis Works.", "Conversion") + pages_of(
        "Fluorine cannot be made in water. Moissan isolated it in 1886 by electrolysing anhydrous hydrogen fluoride with potassium fluoride dissolved in it, and every fluorine plant still does, at about 90 °C on carbon anodes. "
        "Two saltpetre and a charcoal burnt together leave potassium carbonate, which 500 mB of hydrofluoric acid, heated, turns to two potassium bifluoride. "
        "A bifluoride and 500 mB of the acid between two graphite electrodes in a heated vat give 250 mB of fluorine and 250 mB of hydrogen, and the bifluoride comes back nineteen times in twenty.", "Fluorine") + pages_of(
        "Natural uranium is 0.7 per cent uranium-235. A gas centrifuge spins the hexafluoride so the lighter molecules gather towards the axis, and thousands in cascade take it to the 3 to 5 per cent of a light-water reactor's fuel. "
        "1,000 mB in a vat with The Factory's centrifuge give 250 mB enriched and 750 mB depleted. A real cascade puts about nine of feed into one of product at 4.5 per cent; the game is kinder.", "Enrichment") + pages_of(
        "250 mB of enriched hexafluoride, 250 mB of water and 250 mB of hydrogen in a heated vat give two enriched uranium dioxide and 500 mB of hydrofluoric acid back, the dry route of a fuel plant. "
        "A dioxide and 50 mB of hydrogen pressed in a superheated basin sinter to a fuel pellet, the black ceramic fired at about 1,700 °C in hydrogen.", "Fuel") + pages_of(
        "Zirconium takes up neutrons more reluctantly than almost any metal, which is why the nuclear industry wants it free of hafnium. Eight zirconium ingots, a tin nugget and 100 mB of argon, superheated, "
        "give eight Zircaloy, about 1.4 per cent tin as Zircaloy-4 is; pressed to plate, three plates in a column roll two tubes, the cladding a fuel rod's pellets are stacked in.", "Zircaloy") + pages_of(
        "Hafnium is the opposite, a strong absorber, and naval reactors' control rods are solid hafnium. Boron is the cheap absorber: two boric acid and two coke dust in a firebrick-lined vat between three graphite electrodes, "
        "the arc past 2,000 °C, give a boron carbide, after diamond and cubic boron nitride the hardest thing made, and the absorber in a boiling-water reactor's control blades and many pressurised-water reactors' rods.", "Control rods") + pages_of(
        "The tails are depleted uranium, 0.2 to 0.3 per cent uranium-235. 750 mB of depleted hexafluoride and 500 mB of water in a heated vat give a depleted uranium oxide, U3O8, the stable black oxide it is kept as, "
        "and 500 mB of hydrofluoric acid. Its metal goes into counterweights and armour, which the pack does not have.", "Depleted uranium") + pages_of(
        "Thorium nitrate from monazite calcines in a blast furnace to thoria, thorium dioxide. Thorium is fertile, not fissile: it breeds uranium-233 under neutrons but starts nothing alone, so thoria fuel carries a fissile driver. "
        "The nitrate leach of the residue brings out a little of monazite's uranium with the thorium, one yellowcake in twenty. "
        "A yellowcake in eight glass makes eight green-yellow uranium glass, as the Bohemian glassworks made it.", "Thorium") + pages_of(
        "Uranium poisons the kidneys, as lead does, before its radiation tells: uraninite dust, yellowcake, green salt and the oxides are toxic dusts. "
        "With Create: Nuclear installed, its uranium rod is four fuel pellets and a Zircaloy tube, its thorium rod two thoria, two pellets and a tube, and its cooling rod is a control rod: "
        "four hafnium ingots, or four boron carbide in eight stainless steel plates for two. Its reactor core takes a beryllium reflector, and thoria and two calcium under argon reduce to its thorium metal. "
        "Its own ores do not form, and what you hold of ours counts towards its radiation.", "Create: Nuclear") + pages_of(
        "A burnt fuel rod comes back out of the reactor as spent fuel: into a chest or hopper on top of the controller, or loose on top of it. It is hotter than the rod it was: "
        "the fission products, caesium and strontium among them, are what burn. Spent fuel cools only under water, as real fuel spends years in a pool; "
        "a day in a pool makes it cooled spent fuel, and a rod lying in water does not despawn.", "Spent fuel") + pages_of(
        "Then reprocess it, as La Hague and Sellafield do, or bury it, as Finland does. PUREX dissolves cooled fuel in 500 mB of nitric acid, heated: "
        "enriched uranium dioxide comes back for pellets, and 250 mB of high-level waste stays behind. Glass, boric acid and that waste, superheated, "
        "melt to vitrified waste, the borosilicate the waste is locked in. A canister and four vitrified waste, or two cooled spent fuel, seal a waste canister. "
        "It irradiates anyone within eight blocks until it is entombed below y 0 with deepslate, tuff or clay on every face.", "Waste") + pages_of(
        "Sources: World Nuclear Association, Information Library: \"Uranium Mining Overview\", \"Conversion and Deconversion\", \"Uranium Enrichment\" and \"Nuclear Fuel Fabrication\"; "
        "IAEA, Geological Classification of Uranium Deposits and Description of Selected Examples, IAEA-TECDOC-1842 (2018); "
        "Benedict, Pigford and Levi, Nuclear Chemical Engineering, 2nd ed. (McGraw-Hill, 1981); "
        "IAEA, Status and Trends in Spent Fuel and Radioactive Waste Management, NW-T-1.14 (2022).", "Sources"), 15)
    entry("metals", "graphite", "Graphite", "fundamentals:graphite", pages_of(
        "Graphite is carbon in sheets, black, greasy and soft enough to write with. Flake graphite forms when the carbon in old mud and limestone is cooked in the deep crust, "
        "so it lies in schist and gneiss and marble: the flake mines of Heilongjiang, Mozambique's Balama and Madagascar work schist, and Sri Lanka's vein graphite fills cracks in gneiss. "
        "Here it is a band of deepslate deep in any country, the flakes scattered through it.", "Graphite") + pages_of(
        "Flake ore runs ten per cent carbon or so, and graphite floats: it repels water, so a little kerosene, the collector every graphite mill uses, carries the flakes up in the froth. "
        "Two raw graphite, 250 mB of water and 50 mB of kerosene under a mixer give a graphite concentrate, half the time two. "
        "A concentrate is about 95 per cent carbon, the rest silicate. Hydrofluoric acid eats the silicate: two concentrate and 250 mB of it, heated, give two graphite, "
        "the 99.95 per cent the Chinese plants make. Or burn it out: four concentrate under 100 mB of argon between three graphite electrodes in a firebrick-lined vat, "
        "past 2,500 °C, give four.", "Flotation and purification") + pages_of(
        "Synthetic graphite is Acheson's, from 1896: coke baked past 2,500 °C in an electric furnace orders itself into graphite. Two coal coke in the same arc vat give two graphite. "
        "Graphite electrodes are made of it: four graphite and 100 mB of creosote, the pitch binder, pressed hot, give The Factory's graphite electrode, as its coke block cut on a stonecutter still does. "
        "Graphite is also the anode of every lithium-ion cell and the carbon layer of a tantalum capacitor.", "Acheson and electrodes") + pages_of(
        "Sources: USGS, Mineral Commodity Summaries 2025, Graphite (natural); Kogel and others, Industrial Minerals and Rocks, 7th ed. (SME, 2006), \"Graphite\"; "
        "Jara and others, \"Purification, application and current market trend of natural graphite: a review\", International Journal of Mining Science and Technology 29 (2019).", "Sources"), 16)
    entry("metals", "antimony", "Antimony", "fundamentals:antimony_ingot", pages_of(
        "Stibnite, Sb2S3, grows in sprays of lead-grey needles that bend in the hand. It is a mineral of low-temperature hydrothermal veins in quartz, often with a little gold, "
        "and Xikuangshan in Hunan, the Tin Mine Mountain that was never tin, has given the world more antimony than anywhere since 1897. "
        "Here it fills quartz veins in mountain country, and rides in the gold-quartz veins.", "Stibnite") + pages_of(
        "Roasted on a campfire or in a smoker, stibnite burns to antimony trioxide, the white fume a real volatilising roaster collects in its bag house, and gives off sulfur dioxide as every sulfide roast does. "
        "Two oxide, a charcoal and a soda ash for flux, heated, reduce to two antimony ingots and a slag. Antimony is brittle, bluish-white, and expands as it freezes.", "Roasting and reduction") + pages_of(
        "Lead alone is too soft for a battery grid. Eight lead ingots and three antimony nuggets, heated, make eight antimonial lead, about four per cent antimony, "
        "the hard lead of the lead-acid battery since Planté's and Faure's; pressed to plate and sawn, it is the grid of a lead-acid cell (see Batteries, under Power). "
        "Antimony trioxide's great use, as a flame retardant in plastics, has no place in the pack.", "Antimonial lead") + pages_of(
        "Sources: USGS, Mineral Commodity Summaries 2025, Antimony; Anderson, \"The metallurgy of antimony\", Chemie der Erde 72 (2012); "
        "Habashi, Handbook of Extractive Metallurgy (Wiley-VCH, 1997), Antimony.", "Sources"), 17)
    entry("metals", "niobium_tantalum", "Niobium and tantalum", "fundamentals:ferroniobium_ingot", pages_of(
        "Niobium and tantalum are twins, as zirconium and hafnium are, and lie together. Pyrochlore, (Na,Ca)2Nb2O6(OH,F), brown octahedra in carbonatite, is nearly all the niobium there is: "
        "Araxá in Brazil, a weathered carbonatite, gives about nine tenths of it. Columbite-tantalite, (Fe,Mn)(Nb,Ta)2O6, black and heavy, is the tantalum mineral of the lithium pegmatites, "
        "Greenbushes' and Pilgangoora's, and with the Congo's coltan most of the world's tantalum.", "Niobium and tantalum") + pages_of(
        "Washed under a fan, raw pyrochlore leaves half its weight as concentrate. Araxá does not make niobium metal: it makes ferroniobium, about two thirds niobium, by aluminothermic reduction. "
        "Two concentrate, two aluminium powder and a raw hematite, lit over a superheated burner, burn on by themselves to two ferroniobium and a slag.", "Ferroniobium") + pages_of(
        "Nine tenths of niobium goes into steel, a few hundredths of a per cent, as microalloying: it pins the grain small and makes high-strength low-alloy steel, HSLA, for pipelines, bridges and car bodies. "
        "A ferroniobium nugget in two steel blocks, superheated, makes eighteen HSLA steel, about a tenth of a per cent niobium; an ingot of it presses to two heavy plates, being stronger at the same weight.", "HSLA steel") + pages_of(
        "Columbite opens to hydrofluoric acid. Two columbite dust, a potash, 500 mB of hydrofluoric acid and 250 mB of sulfuric acid, heated, are the digest and the MIBK solvent extraction in one step: "
        "the tantalum leaves as potassium fluorotantalate, K2TaF7, crystallised with the potassium chloride, Marignac's salt, and the niobium as its oxide, which goes to ferroniobium as pyrochlore's does.", "Tantalum") + pages_of(
        "Two fluorotantalate, two sodium and 100 mB of argon, superheated, reduce to a tantalum ingot: sodium reduction, the way nearly all tantalum powder is made. "
        "Sodium comes from the Downs cell: two salt and a calcium chloride, which lowers the melt to about 600 °C, on two electrodes in a heated vat give a sodium ingot and 250 mB of chlorine, "
        "and the calcium chloride nine times in ten.", "Sodium reduction") + pages_of(
        "Euxenite carries niobium and tantalum too, and hydrofluoric acid takes them while the rare earths stay behind as fluorides: two euxenite dust digested the same way give a niobium oxide, a fluorotantalate one time in four, "
        "and a heavy rare earth concentrate. Loparite is chlorinated, as Solikamsk does it: two loparite dust, a coal coke and 1,000 mB of chlorine, heated, give a niobium oxide, "
        "250 mB of titanium tetrachloride and 250 mB of crude rare earth liquor.", "From euxenite and loparite") + pages_of(
        "Tantalum's oxide film is the thinnest, toughest dielectric there is, so most tantalum goes into capacitors: a tantalum nugget with a raw pyrolusite, the manganese dioxide cathode, "
        "a graphite and a silver nugget deployed on it in turn assembles into four capacitors. A tantalum nugget in the superalloy melt makes nine ingots where it made seven.", "What it is for") + pages_of(
        "Sources: USGS, Mineral Commodity Summaries 2025, Niobium and Tantalum; CBMM, \"Niobium technical information\"; "
        "Habashi, Handbook of Extractive Metallurgy (Wiley-VCH, 1997), Niobium and Tantalum; Gupta and Suri, Extractive Metallurgy of Niobium (CRC, 1994).", "Sources"), 18)
    entry("metals", "potash", "Potash", "fundamentals:potash", pages_of(
        "Sylvite, KCl, is salt's potassium twin, colourless when pure and red in the Saskatchewan mines, where a dust of hematite stains it. It is the last salt to crystallise as a sea dries up, "
        "so it lies in seams in the evaporite beds with the halite. Mined with its halite, it is sylvinite.", "Sylvite") + pages_of(
        "Potassium chloride dissolves far better hot than cold and salt hardly better at all, so the hot leach parts them, as the Carlsbad and Saskatchewan mills do: two raw sylvite and 500 mB of water, heated, "
        "give two potash, the muriate of potash farmers buy, and a salt. Nearly all potash is fertiliser. Here it is also the bath lithium is electrolysed in and the potassium the tantalum salt is crystallised with.", "Muriate of potash") + pages_of(
        "Sources: USGS, Mineral Commodity Summaries 2025, Potash; Garrett, Potash: Deposits, Processing, Properties and Uses (Chapman & Hall, 1996).", "Sources"), 19)
    entry("metals", "gold", "Gold", "minecraft:gold_ingot", pages_of(
        "Gold is native, the metal itself, alloyed with a tenth or so of silver. It forms in quartz veins, the lode, where hot water dropped it with stibnite and a little galena, "
        "and as the veins weather it washes down into river and beach sand, where its weight keeps it: the placers of the Klondike and of California in 1849. "
        "Vanilla gold ore is native gold too, and mines as raw native gold.", "Gold") + pages_of(
        "Panning is a fan's wash: raw native gold under an encased fan leaves three gold nuggets, the coarse gold, and the fine washes away. "
        "Mercury catches the fine: two raw native gold and a mercury under a mixer give two gold amalgam, as the stamp mills' copper plates caught it. "
        "An amalgam heated in a basin gives off its mercury and leaves an electrum ingot, the mercury coming back about half the time.", "Panning and amalgam") + pages_of(
        "Retorting amalgam in the open is how gold mining poisons people: the vapour is breathed. An amalgam in a basin hurts and poisons everyone within reach who has no diving helmet on a filled backtank, "
        "as small-scale miners from the Amazon to Indonesia are poisoned now. Cyanide replaced mercury in the 1890s, but it starts from hydrogen cyanide, made from methane and ammonia, which the pack does not have.", "Mercury") + pages_of(
        "Electrum is gold and its silver. Miller's process blows chlorine through the melt and the silver leaves as its chloride: an electrum ingot and 250 mB of chlorine, superheated, give a gold ingot and a silver nugget. "
        "Or dissolve it in aqua regia, 500 mB heated, which leaves the silver as chloride and gives the same.", "Parting") + pages_of(
        "Sources: Marsden and House, The Chemistry of Gold Extraction, 2nd ed. (SME, 2006); USGS, Mineral Commodity Summaries 2025, Gold; "
        "UNEP, Global Mercury Assessment 2018, artisanal and small-scale gold mining.", "Sources"), 20)
    entry("metals", "silicon", "Silicon", "tfmg:silicon_ingot", pages_of(
        "Silicon is never found free: it is quartz, and it is won from quartz the way ferrochrome is won from chromite, in a submerged-arc furnace, three graphite electrodes sunk in a charge of quartz and carbon "
        "and the arc at about 2,000 °C between them. Three quartz and a coal coke or a charcoal in a firebrick-lined vat between three graphite electrodes give 144 mB of liquid silicon, an ingot's worth, "
        "and 250 mB of carbon dioxide, the carbon monoxide burning off the top of the charge. The furnace is tapped and the silicon cast: a casting basin makes an ingot of metallurgical silicon, about 98 per cent pure.", "Silicon") + pages_of(
        "Real furnaces run the carbon as a blend of coal, coke, charcoal and wood chips, the wood keeping the charge open for the gas, and take some 12 MWh a tonne (Schei, Tuset and Tveit, Production of High Silicon Alloys). "
        "A blast furnace cannot do it: at its heat silica gives at most a lean ferrosilicon, so The Factory's blast furnace no longer melts silicon out of quartz. "
        "Most metallurgical silicon goes into aluminium casting alloys and silicones, and into electrical steel; the Factory's magnetic alloy, nickel and silicon steel, takes it.") + pages_of(
        "A chip wants silicon to nine nines, and that goes through a liquid. Silicon and dry hydrogen chloride in a fluidised bed at about 300 °C give trichlorosilane, SiHCl3, and a sixth as much silicon tetrachloride. "
        "The hydrogen chloride is burnt dry from hydrogen and chlorine, since water would hydrolyse every chlorosilane: a metallurgical silicon, 750 mB of chlorine and 250 mB of hydrogen, heated, give 500 mB of crude trichlorosilane.", "Trichlorosilane") + pages_of(
        "Trichlorosilane boils at 31.8 °C and the tetrachloride at 57.6, and the boron and phosphorus chlorides, the impurities that matter, are parted from both in tall columns. "
        "In The Factory's distillation tower 200 mB of crude give 170 mB of trichlorosilane and 30 of tetrachloride. Both fume hydrogen chloride in moist air, and trichlorosilane catches fire at 185 °C.") + pages_of(
        "Siemens' process (1950s): trichlorosilane and hydrogen over thin silicon rods heated by current to about 1,100 °C, where silicon deposits and the rods grow thick over days. "
        "1,000 mB of trichlorosilane and 500 mB of hydrogen in a vat between two electrodes give two polysilicon, 250 mB of hydrochloric acid and 250 mB of silicon tetrachloride, "
        "which most of the silicon fed really leaves as. A converter hydrogenates it back over silicon: a metallurgical silicon, 750 mB of tetrachloride and 500 mB of hydrogen, heated, give 1,000 mB of crude trichlorosilane.", "Siemens") + pages_of(
        "A crystal is pulled, as Czochralski found in 1916 and Teal and Little first did with germanium and silicon at Bell Labs: polysilicon is melted in a quartz crucible under argon, "
        "a seed dipped in and drawn up slowly, turning. Four polysilicon, a quartz, the crucible, which serves once, and 100 mB of argon in a superheated basin under a mixer give a silicon boule, "
        "and Create's mechanical saw cuts it to eight wafers. The wafers are what the Factory's N and P semiconductors are doped from.", "Czochralski") + pages_of(
        "Sources: Ullmann's Encyclopedia of Industrial Chemistry, \"Silicon\"; Schei, Tuset and Tveit, Production of High Silicon Alloys (Tapir, 1998); "
        "O'Mara, Herring and Hunt, Handbook of Semiconductor Silicon Technology (Noyes, 1990); USGS, Mineral Commodity Summaries 2025, Silicon.", "Sources"), 21)
    entry("metals", "minor_metals", "Germanium, indium, gallium and cadmium", "fundamentals:germanium_ingot", pages_of(
        "No one mines these four. Germanium, indium and cadmium ride in sphalerite at tens to a few thousand grams a tonne, and gallium in bauxite at about fifty; "
        "each is taken out of a stream a zinc plant or an alumina refinery already runs. Here, as there, they are scarce.", "Byproduct metals") + pages_of(
        "Germanium. Two zinc leach residue, 500 mB of hydrochloric acid and 100 mB of chlorine, heated, give 100 mB of germanium tetrachloride, which boils out of the acid at 83 °C; "
        "the chlorine keeps the arsenic as arsenate, which stays behind. 250 mB of tetrachloride and 500 mB of water give a germanium oxide and 250 mB of the acid back, "
        "and two oxide with 500 mB of hydrogen in a heated vat reduce to two germanium ingots, at about 650 °C, below the metal's 938.", "Germanium") + pages_of(
        "The first transistors were germanium. The alloy-junction transistor (General Electric and RCA, 1951) is an n-type germanium die with a dot of indium alloyed into each face: "
        "a plastic sheet deployed with a germanium nugget, two indium nuggets and a copper wire gives four of the Factory's transistors. "
        "Most germanium now goes to fibre optics and infrared lenses, which the pack does not have.") + pages_of(
        "Indium. Two zinc leach residue and 500 mB of sulfuric acid, heated, are the hot acid leach, 500 mB of indium sulfate liquor. D2EHPA, our P204, takes indium out of a sulfate liquor before anything in it but ferric iron: "
        "1,000 mB of liquor, 250 mB of P204 and 100 mB of hydrochloric acid in a vat with a mixer give 100 mB of indium chloride strip, and the 900 mB of raffinate go back to the zinc leach as zinc leach liquor; "
        "the organic comes back but for 5 mB.", "Indium") + pages_of(
        "100 mB of strip and two zinc nuggets cement an indium sponge, and two sponge electrorefined between two electrodes in a vat give two indium ingots. "
        "Most indium goes into indium tin oxide, In2O3 with a tenth of SnO2, transparent and conducting, the electrode film on every flat screen: an indium ingot, a tin nugget and 250 mB of air, superheated, sinter to two, "
        "and in place of the Factory's silicon a segmented display built on it makes eight.") + pages_of(
        "Gallium. Bauxite's gallium dissolves with the alumina and builds up in the liquor that goes round the refinery, to about 0.2 g/L. Pechiney took it out of the spent liquor on a mercury cathode, as gallium amalgam, "
        "and decomposed that in caustic to sodium gallate; refineries now use ion-exchange resins. 1,000 mB of spent Bayer liquor and a mercury in a vat between two electrodes give 50 mB of sodium gallate liquor "
        "and 950 mB of caustic soda for the digester, and the mercury comes back nineteen times in twenty.", "Gallium") + pages_of(
        "250 mB of gallate liquor on two electrodes give two gallium nuggets and 200 mB of caustic. Gallium melts at 29.8 °C, in a warm hand. "
        "The blue LED is indium gallium nitride on sapphire (Nakamura, Akasaki and Amano, Nobel Prize 2014), grown from gallium and ammonia near 1,000 °C: two gallium nuggets, an indium nugget, "
        "an alumina for the sapphire and 250 mB of ammonia in a heated vat give two LED chips.") + pages_of(
        "A blue chip under a yellow phosphor, yttrium aluminium garnet with cerium, shines white: a chip, a yttrium oxide, a cerium oxide, a framed glass and two copper nuggets make six light bulbs. "
        "Gallium arsenide, the other half of gallium's use, wants arsenic, which the pack does not have.") + pages_of(
        "Cadmium. The zinc dust that cleans the leach liquor brings the cadmium down as a sponge, and the cadmium sponge is a toxic dust: the itai-itai disease of the Jinzu valley was cadmium from a zinc mine. "
        "A sponge pressed in a heated basin melts to a cadmium ingot, at 321 °C. Most cadmium went into nickel-cadmium batteries, which the pack does not have; "
        "the rest into cadmium yellow, the sulfide: an ingot and two sulfur dust, heated, give six yellow dye. Cadmium red wants selenium, which it does not have either.", "Cadmium") + pages_of(
        "Sources: USGS, Mineral Commodity Summaries 2025, Germanium, Indium, Gallium and Cadmium; Ullmann's Encyclopedia of Industrial Chemistry, \"Germanium and Germanium Compounds\", "
        "\"Indium and Indium Compounds\", \"Gallium and Gallium Compounds\" and \"Cadmium and Cadmium Compounds\"; Sinclair, The Extractive Metallurgy of Zinc (AusIMM, 2005); "
        "Alfantazi and Moskalyk, \"Processing of indium: a review\", Minerals Engineering 16 (2003).", "Sources"), 22)
    oxidation()


def platinum():
    category("platinum", "The platinum metals", "Six metals that ride in nickel matte and come out of a refinery one by one, in the order their chemistry allows.",
             "fundamentals:platinum_ingot", 4)
    entry("platinum", "matte", "Matte", "fundamentals:converter_matte_dust", pages_of(
        "The platinum metals are never mined alone. They ride in nickel-copper sulfide, and the smelter's matte is what collects them. "
        "Two roasted pentlandite and a sand, superheated, smelt to two nickel matte and a slag, as the flash furnaces do at Norilsk and Sudbury; no bloomery ever ran hot enough for a nickel matte. "
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
        "and over one 500 mB of naphtha in a heated vat, a fixed bed with no mixer, reforms to 400 mB of gasoline and gives off 100 mB of hydrogen, the catalyst surviving nineteen times in twenty. "
        "Eight platinum nuggets round a rhodium nugget weave the gauze ammonia is burnt over: 250 mB of ammonia and 1,000 mB of air, heated, give 250 mB of nitric acid, Ostwald's process, and the gauze is all but never used up.", "What they are for") + pages_of(
        "The three-way converter: platinum and palladium on ceria burn what an engine leaves, and rhodium breaks down its nitrogen oxides, where four fifths of the world's rhodium goes. "
        "A platinum, a palladium and a rhodium nugget with the cerium oxide make two exhausts instead of one. "
        "Ruthenium lets a superalloy carry more: a ruthenium nugget in the superalloy melt makes nine ingots instead of seven. "
        "An iridium-tipped spark plug outlasts four plain ones, so an iridium nugget, an alumina and a nickel nugget make four, and a platinum nugget three. "
        "And osmium was the first metal filament: an osmium sponge pastes and draws to four filaments, and a light bulb burns one as well as tungsten."), 3)


def power():
    category("power", "Power", "Sunlight into The Factory Must Grow's grid, the cells its accumulators store it in, and the magnets its machines turn on.",
             "fundamentals:photovoltaic_panel", 5)
    entry("power", "solar", "Solar panels", "fundamentals:photovoltaic_panel", pages_of(
        "A solar panel goes on a rack. Build the rack from steel and set it down facing the way you want, then mount a photovoltaic panel on it: a P and an N semiconductor from The Factory Must Grow under glass, in an aluminium frame, with a silver nugget for the front contacts printed on every cell. "
        "Silicon is doped n-type with a donor from group 15 and p-type with an acceptor from group 13 (Sze, Physics of Semiconductor Devices): four silicon wafers and a white phosphorus, heated, give four N semiconductor, "
        "and four wafers and a boric acid four P, where the Factory used sulfur and aluminium. Nearly every solar cell made now is cut from a Czochralski wafer, as these are; see Silicon. "
        "Under open sky it feeds the electrical network, 120 volts while the sun is up and up to 200 watts at noon, nothing at night or in shade. Break it and you get the rack and the panel back.", "Solar panels")
        + [crafting("fundamentals:panel_rack"), crafting("fundamentals:photovoltaic_panel")], 0)
    batteries()
    magnets()


def batteries():
    entry("power", "batteries", "Batteries", "fundamentals:lead_acid_cell", pages_of(
        "The Factory's accumulator is a bank of cells in a casing, and it is built from real ones. A cell is two electrodes that would rather be one compound, kept apart by a separator "
        "and joined only through an electrolyte, so the electrons have to go round by the wire. Four chemistries fill it: lead-acid, nickel-metal hydride, lithium-ion, and the silver-zinc of the "
        "silver entry. The block holds the same charge whichever cells it is built from; what changes is how many cells it takes, because a kilogram of lead-acid stores 30 to 40 watt-hours, "
        "nickel-metal hydride 60 to 120 and lithium-ion 150 to 270.", "Cells") + pages_of(
        "Planté's cell of 1859 is still most of the world's stored energy. The grid is the plate's skeleton, antimonial lead, hard enough to carry the paste: an antimonial lead plate run through a saw. "
        "The paste is leady oxide, litharge blown from molten lead in air (Barton's pot: a lead ingot and 250 mB of air, heated, give a litharge) or "
        "from the cupel, mixed with a little sulfuric acid and water to basic lead sulfate: two litharge, 100 mB of acid and 250 mB of water give two lead paste. A deployer pastes a grid.", "Lead-acid") + pages_of(
        "A pasted plate is not yet an electrode. Formation charges it: two pasted plates and 250 mB of sulfuric acid in a vat between two electrodes give one lead dioxide plate, chocolate brown, "
        "the positive, and one sponge lead plate, grey, the negative. The two plates, a polyethylene separator and 250 mB of acid in a basin make a lead-acid cell of two volts, "
        "and four cells with copper wire round an aluminium casing make an accumulator. Real batteries carry one more negative than positive plate.", "Formation") + pages_of(
        "A lithium-ion cell is a graphite anode on copper foil and a metal oxide cathode on aluminium foil, a polyethylene separator between, wet with lithium hexafluorophosphate in organic carbonates. "
        "It charges by moving lithium ions out of the oxide and in between the graphite's layers. Here the cell is The Factory's lithium charge, which its blade burns and its accumulator stacks: "
        "two charges, copper wire, a casing and a circuit board, the battery management every lithium pack needs, make an accumulator.", "Lithium-ion") + pages_of(
        "NMC, lithium nickel manganese cobalt oxide, is the cathode of most electric cars. Its precursor is co-precipitated from the three sulfates with caustic soda, ammonia holding the nickel "
        "back so the particles grow round and dense. A raw pyrolusite, a coal coke and 500 mB of sulfuric acid, heated, reduce and leach to two manganese sulfate; a cobalt ingot and 250 mB of acid "
        "give two cobalt sulfate and 250 mB of hydrogen. 750 mB of nickel sulfate liquor, a manganese and a cobalt sulfate, 500 mB of caustic soda and 50 mB of ammonia in a vat give two NMC hydroxide, "
        "six parts nickel to two each of manganese and cobalt, NMC 622; the sodium sulfate left in the liquor is not kept.", "NMC") + pages_of(
        "Two hydroxide, a lithium carbonate and 500 mB of air, heated, calcine to two NMC cathode and 250 mB of carbon dioxide, as at 800 to 900 °C in a roller kiln. "
        "The richest nickel grades take lithium hydroxide instead; 622 takes the carbonate. Most of the world's cobalt now goes into cathodes like this one.", "Calcining") + pages_of(
        "LFP, lithium iron phosphate, is the cheap one: no nickel, no cobalt, a lower voltage (3.2 against NMC's 3.7) and a far longer life, the cathode of most Chinese cars and grid batteries. "
        "An iron ingot, 500 mB of phosphoric acid and 250 mB of air, heated, give two iron phosphate, the pale FePO4 the industry precipitates. Two iron phosphate, a lithium carbonate, a sugar and "
        "100 mB of argon, heated, calcine to two LFP cathode, the sugar charring to the carbon coat every LFP particle needs, since the phosphate barely conducts.", "LFP") + pages_of(
        "The anode is graphite, natural flake purified or Acheson's from coke, the two alike to the cell; most anodes now are synthetic, the flake rounded into spheres first.", "Graphite") + pages_of(
        "The electrolyte is the delicate part. A lithium carbonate in 500 mB of hydrofluoric acid gives two lithium fluoride and 250 mB of carbon dioxide. White phosphorus burns in 1,000 mB of fluorine "
        "to 500 mB of phosphorus pentafluoride, a fuming gas, and a lithium fluoride takes up 250 mB of it as lithium hexafluorophosphate, the salt every lithium-ion cell uses. "
        "Ethylene over silver in air gives ethylene oxide, which takes up carbon dioxide: 500 mB of ethylene, 500 mB of air and 250 mB of carbon dioxide on a silver nugget in a heated vat "
        "give two ethylene carbonate, and the silver comes back nineteen times in twenty.", "Electrolyte") + pages_of(
        "A hexafluorophosphate and two ethylene carbonate give 500 mB of lithium-ion electrolyte. Real electrolyte thins the carbonate, which melts at 36 °C, with a linear one, dimethyl or ethyl methyl carbonate; "
        "here that is folded in, and the fluid has the real mixture's figures: it burns, and water turns it to hydrogen fluoride. A cathode, a graphite, an aluminium and a copper sheet for the foils, a plastic "
        "sheet and 250 mB of electrolyte in a basin make two lithium charges, from NMC or LFP alike.", "Assembly") + pages_of(
        "Nickel-metal hydride stores hydrogen in a rare earth alloy. LaNi5 takes up six hydrogen atoms a formula unit, more hydrogen to the litre than liquid hydrogen, and real cells use mischmetal, "
        "the light rare earths unparted, for the lanthanum. Two lanthanum or cerium ingots, four nickel and 100 mB of argon, superheated, give six hydride alloy. Nickel sulfate liquor and caustic soda "
        "with a zinc nugget throw down two nickel hydroxide, a little zinc in it as the cathode makers add.", "Nickel-metal hydride") + pages_of(
        "A hydride alloy, a nickel hydroxide, a nickel sheet, a plastic sheet and 100 mB of caustic soda make two cells of 1.2 volts, and three with copper wire round a casing make an accumulator. "
        "The real electrolyte is mostly potassium hydroxide with a little sodium and lithium hydroxide; caustic soda stands in. This is the battery of the first hybrid cars, and where most of the "
        "lanthanum and cerium the plant parts can go.", "Hydride cells") + pages_of(
        "Sources: Reddy (ed.), Linden's Handbook of Batteries, 4th ed. (McGraw-Hill, 2011); Pavlov, Lead-Acid Batteries: Science and Technology (Elsevier, 2011); "
        "Ullmann's Encyclopedia of Industrial Chemistry, \"Batteries, 2. Secondary Batteries\", \"Lithium and Lithium Compounds\" and \"Carbonic Esters\"; "
        "USGS, Mineral Commodity Summaries 2025, Cobalt, Graphite and Lithium.", "Sources"), 2)


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
        + pages_of("The first magnet is hardened carbon steel. Heat a steel ingot in a furnace and quench the hot steel under a spout with 250 mB of water. "
                   "In an unpowered steel vat, combine copper, zinc, a glass bottle, 50 mB of sulfuric acid and 200 mB of water into a galvanic cell. "
                   "Craft the cell, hardened steel and copper wire together: the cell's current magnetizes the steel. The recipe consumes the cell and winding. "
                   "This is a 25% starter magnet; its game heat limits are 150, 250 and 770 °C. The cell does not drive the factory. Mechanical rotation drives its generator.", "First electricity")
        + pages_of("An ordinary iron ingot can be the unfinished coil's core, and iron plates can be the transformer's laminations. Wind the coils mechanically. "
                   "Silicon steel is useful core material, but is not a prerequisite for induction. Transform the generator's output, never the cell's steady DC. "
                   "Three graphite electrodes make the first silicon. Each needs at least 1,500 volts and 7,500 watts in The Factory's model; keep each on its own small-transformer output "
                   "and supply enough generation upstream. Watch current as well as voltage: raising voltage also raises load.", "To the arc furnace")
        + pages_of("The real figures, from the makers' grade tables: sintered NdFeB runs to about 80 °C, Curie point 310 to 400 °C. "
                   "With dysprosium or terbium (the SH, UH and EH grades) it runs to 150 to 230 °C. SmCo runs to 250 to 350 °C, Curie 720 to 825. "
                   "Alnico runs to 450 to 550 °C, Curie about 860, but stores far less energy: about 5 MGOe, against 35 to 52 for NdFeB and 16 to 32 for SmCo. "
                   "So aircraft and missiles fly SmCo, alnico lives on in sensors, and every other motor is NdFeB with dysprosium for the heat, "
                   "which is most of why the world wants dysprosium (US Department of Energy, Critical Materials Strategy, 2011).", "Why it matters")
        + pages_of("Plain NdFeB is two neodymium, three iron and a ferroboron under argon; three neodymium, a dysprosium or terbium, six iron and two ferroboron make eight Dy-NdFeB. "
                   "SmCo is two samarium, four cobalt, an iron and four copper and two zirconium nuggets, the Sm2Co17 type every high-temperature grade is. "
                   "Alnico is iron with aluminium, nickel, cobalt and a little copper: five iron, an aluminium, two nickel, two cobalt and three copper nuggets, superheated, "
                   "cast and heat-treated to ten ingots. Polarize any of the four ingots, or an NdFeB, Dy-NdFeB or SmCo plate, to its magnet. "
                   "The magnet is the first thing a motor or generator takes on the belt, and it decides the grade of the whole: a motor is begun on a shaft, a generator on a steel plate.", "Making them")
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


def chemicals():
    category("chemicals", "Heavy chemicals", "Sulfuric acid by the contact process, Portland cement, and the fertilisers: the tonnage chemistry the rest stands on.",
             "tfmg:sulfuric_acid_bucket", 7)
    entry("chemicals", "sulfuric_acid", "Sulfuric acid", "tfmg:sulfuric_acid_bucket", pages_of(
        "Sulfuric acid is made in greater tonnage than any other chemical, some 270 million tonnes a year. The Factory Must Grow's way, three sulfur dust and a nitrate dust "
        "with 1,000 mB of water in a vat for 500 mB, is the lead chamber process in small, nitre carrying oxygen to the burning sulfur as in Roebuck's chambers of 1746. "
        "It is hard on sulfur and on nitre. Phillips patented the contact process in 1831, and from about 1900, when Knietsch made it work at BASF, it took over.", "Sulfuric acid") + pages_of(
        "First, sulfur dioxide. A sulfur dust burnt in 1,000 mB of air in a heated basin gives 500 mB. Or catch it off the roasters: two raw sphalerite, chalcopyrite, pentlandite or galena "
        "with 500 mB of air in a heated basin roast to their oxide or calcine and give 250 mB of it, and two molybdenite with 1,000 mB give 500. "
        "Every modern copper, zinc and nickel smelter runs an acid plant on its roaster gas, which pays for the plant and keeps the gas out of the valleys downwind. "
        "Roasted on a campfire, it goes into the air. It is a heavy, choking gas; pipe it away before it boils out of the basin.", "Sulfur dioxide") + pages_of(
        "Sulfur dioxide will not take a second oxygen by itself. Over vanadium pentoxide at about 430 °C it does, and the cooler the bed the further it goes, "
        "so a converter is three or four catalyst beds with coolers between. A heated vat with no machine in it is the bed: a vanadium pentoxide, 500 mB of sulfur dioxide and 500 mB of air give 500 mB of sulfur trioxide, "
        "and the catalyst back 49 times in 50. Platinum was the first catalyst; vanadium, cheaper and not poisoned by arsenic, replaced it from the 1920s.", "The converter") + pages_of(
        "The trioxide is never put into water, which turns it to a fog of acid no scrubber catches. Strong acid absorbs it: 500 mB of trioxide stirred into 500 mB of sulfuric acid give 500 mB of oleum, "
        "fuming sulfuric acid, and oleum let down with 500 mB of water gives 1,000 mB of sulfuric acid. That is 500 mB more for every sulfur dust, three times the lead chamber's, and no nitre. "
        "The trioxide and oleum fume and are poison to breathe.", "Absorption") + pages_of(
        "Vanadium rides in magnetite, a fraction of a per cent of it, and most of the world's comes out of titanomagnetite. Four raw magnetite with a soda ash and 500 mB of water, superheated, "
        "are the salt roast and the water leach: 250 mB of sodium vanadate liquor, and three crushed iron ore still good to smelt. 250 mB of the liquor and an ammonium chloride, heated, "
        "throw down ammonium metavanadate, which calcines to a vanadium pentoxide, orange-brown.", "Vanadium pentoxide") + pages_of(
        "Most of the world's sulfur is not mined. Crude oil and gas carry it, up to a few per cent, and a refinery must take it out or the fuel burns to sulfur dioxide. "
        "The hydrotreater is a heated vat with no machine in it, over a cobalt-molybdenum bed: a cobalt sulfate, a molybdenum oxide and four bauxite powder, heated, make four, "
        "and the bed lasts 19 runs in 20. 500 mB of heavy oil and 250 mB of hydrogen, the reformer's own, give 500 mB of diesel and 250 mB of hydrogen sulfide, the sulfur gone to the gas. "
        "The Claus unit burns a third of that and lets the rest react with it: 1,000 mB of hydrogen sulfide and 500 mB of air, heated, drop two sulfur dust. "
        "Texas, Louisiana and the Persian Gulf make their sulfur this way and ship it to acid plants that have no sulfur of their own.", "Sulfur from oil") + pages_of(
        "Before the oil, pyrite. A pyrite lode is a lens of iron sulfide with a little chalcopyrite, sphalerite and galena through it, a massive sulfide, as at Rio Tinto, "
        "and for a century the acid works of Europe roasted it: two raw pyrite with 1,000 mB of air in a heated basin give 500 mB of sulfur dioxide and two raw hematite, "
        "the cinder, which the blast furnace still takes. Huelva, downstream of Rio Tinto, is a copper-smelting acid hub to this day.", "Pyrite") + pages_of(
        "The Nether has the one ore with a reason to be there. Native sulfur is the yellow crust a fumarole leaves, on the basalt deltas and in the soul sand valleys, "
        "as the vents of Kawah Ijen leave it, where it is still carried down the crater by the basketful. A millstone or crushing wheels grind the raw crust to sulfur dust, "
        "and three sulfur dust with a nitrate in The Factory Must Grow's vat are its own acid.", "Native sulfur") + pages_of(
        "Acid comes back, too. A refinery's alkylation unit makes high-octane gasoline over sulfuric acid: 500 mB of LPG with 100 mB of acid, cold, give 500 mB of gasoline "
        "and 100 mB of spent acid, black with oil and down to about 90%. A regeneration plant cracks it in a furnace: 500 mB of spent acid and 250 mB of air, superheated, "
        "are 500 mB of sulfur dioxide, back to the converter.", "Spent acid") + pages_of(
        "Acid is made where the sulfur is burnt, not where the ore is dug. Nexa's Cajamarquilla refinery outside Lima made 345,000 tonnes of zinc in 2025 and 594,000 tonnes of acid "
        "with it, and sold nearly all the acid, by rail car and chemical tanker, to the copper leach plants of northern Chile at Mejillones and Barquito. "
        "Kokkola in Finland made 290,000 tonnes of zinc and 312,000 of acid; Atlantic Copper's smelter at Huelva makes 1.1 million tonnes a year; "
        "Kamoa-Kakula's new smelter sells 40,000 tonnes a month to the Copperbelt's leach pads. Only about 7% of the world's acid crosses a border: "
        "it is cheap, heavy and eats its tank, so a smelter's acid wants a fertiliser plant or a leach pad nearby, or a port.", "Where it is made") + pages_of(
        "Sources: Ullmann's Encyclopedia of Industrial Chemistry, \"Sulfuric Acid and Sulfur Trioxide\"; King, Davenport and Moats, Sulfuric Acid Manufacture (2nd ed., 2013); "
        "Ullmann's, \"Vanadium and Vanadium Compounds\"; USGS, Mineral Commodity Summaries 2025, \"Sulfur\", \"Vanadium\" and \"Zinc\"; "
        "Nexa Resources, Form 20-F for 2025; Boliden, Annual and Sustainability Report 2025; Atlantic Copper; Ivanhoe Mines, third quarter 2026; "
        "S&P Global Commodity Insights, Sulfuric Acid (2023).", "Sources"), 0)
    entry("chemicals", "cement", "Cement", "fundamentals:cement_clinker", pages_of(
        "Lime and clay stirred together do not make cement; they set only once they have been burnt together. Portland cement is limestone and clay ground to a raw meal and burnt at about 1,450 °C "
        "in a rotary kiln, where the lime takes up the clay's silica and alumina as the calcium silicates that set with water, alite and belite. "
        "Three limesand and a clay ball over a blaze burner fed a blaze cake give three cement clinker, grey nodules, and 250 mB of carbon dioxide. "
        "About half a kiln's carbon dioxide is the limestone's own, which is why cement is some 8 per cent of the world's.", "Clinker") + pages_of(
        "Clinker ground alone sets in minutes. Gypsum ground in with it, about 5 per cent, holds back the aluminate so concrete can be placed: eight clinker and a gypsum in a mixer give eight of The Factory Must Grow's cement, "
        "whose concrete mixture (sand or slag, cement and gravel) and liquid concrete (sand, gravel and cement in a vat with 250 mB of water) go on as before, cement now where raw limesand was. "
        "The gypsum is a scrubber's: a limesand in 250 mB each of sulfur dioxide and water, the sulfite oxidised by air blown through the slurry, is the lime scrubber of a power station, whose gypsum much cement and wallboard now take.", "Cement") + pages_of(
        "Ground granulated blast-furnace slag is a cement too, slow until the clinker's lime wakes it, and may stand for most of the clinker. "
        "Four clinker, four slag and a gypsum give eight cement: half the kiln, and half its carbon dioxide, for the same concrete.", "Slag cement") + pages_of(
        "Sources: Ullmann's Encyclopedia of Industrial Chemistry, \"Cement and Concrete\"; Taylor, Cement Chemistry (2nd ed., 1997); "
        "USGS, Mineral Commodity Summaries 2025, \"Cement\" and \"Iron and Steel Slag\".", "Sources"), 1)
    detonates = re.search(r"DETONATES = (\d+)", (JAVA / "uses/Fertiliser.java").read_text(encoding="utf-8")).group(1)
    entry("chemicals", "fertilisers", "Fertilisers", "fundamentals:npk_fertiliser", pages_of(
        "A crop takes nitrogen, phosphorus and potassium from the soil, and grows as far as the scarcest lets it, Liebig's law of the minimum. "
        "Most of the world's ammonia goes to fertiliser. 250 mB of ammonia and 250 mB of nitric acid neutralise in a basin to two ammonium nitrate, white prills. "
        "500 mB of ammonia and 250 mB of carbon dioxide, heated, give two urea, the most used nitrogen fertiliser of all, made at about 180 °C under pressure from the carbon dioxide the ammonia plant itself gives off.", "Nitrogen") + pages_of(
        "Three bone meal and 250 mB of sulfuric acid give three superphosphate: Lawes patented his \"dissolved bones\" in 1842, and bone stands for phosphate rock here as it does for the acid. "
        "250 mB of phosphoric acid and 250 mB of ammonia give an ammonium phosphate. The wet process that makes phosphoric acid leaves phosphogypsum, about four and a half tonnes to the tonne of phosphate, "
        "holding the rock's radium: no plaster or cement may take it, so it is stacked beside the plant. Nine pack into a phosphogypsum stack.", "Phosphate") + pages_of(
        "Potash is potassium chloride, muriate of potash, dug from evaporite beds; saltpetre is a potash fertiliser too. A nitrogen fertiliser, a phosphate one and a potash or a saltpetre in a mixer "
        "give three NPK compound fertiliser. A straight fertiliser is bone meal twice over on the crop it is put on; NPK, all three nutrients in one granule, is broadcast, "
        "bone meal twice over on every crop in a three by three patch. A dispenser spreads any of them.", "Potash and NPK") + pages_of(
        f"Ammonium nitrate is an oxidiser, and in a mass it detonates. A stack of {detonates} or more that burns, falls in lava or is caught in a blast goes off, harder the bigger the stack; "
        "a few burn away. 2,300 tonnes aboard the Grandcamp levelled the port of Texas City in 1947, and 2,750 tonnes left in a warehouse levelled Beirut's in 2020.", "Ammonium nitrate") + pages_of(
        "Sources: Ullmann's Encyclopedia of Industrial Chemistry, \"Fertilizers\", \"Phosphate Fertilizers\", \"Ammonium Compounds\" and \"Urea\"; "
        "USGS, Mineral Commodity Summaries 2025, \"Nitrogen (Fixed)—Ammonia\", \"Phosphate Rock\" and \"Potash\"; "
        "US EPA, 40 CFR 61 subpart R (phosphogypsum stacks).", "Sources"), 2)


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
    steel()
    rare_earths()
    liquids_entry()
    metals()
    platinum()
    power()
    plastics()
    chemicals()
    check()
    n = len(list(BOOK_ASSETS.glob("entries/*/*.json")))
    print(f"book written: {n} entries")


if __name__ == "__main__":
    main()
