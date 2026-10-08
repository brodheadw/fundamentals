# Fundamentals

Real ore minerals, real deposits, and the real road from rock to metal. A Create add-on for NeoForge 1.21.1.

![Every ore in the mod](docs/images/all-ores.png)

Vanilla gives you "iron ore". The ground doesn't. Iron is in hematite and magnetite, lead is in galena, zinc is in sphalerite, the rare earths are in bastnäsite and monazite and a clay that doesn't even have a mineral name. Fundamentals replaces the generic ores with the minerals that actually carry each metal, puts them in the ground the way geology does, and makes you win the metal out of them the way smiths and smelters did, by hand at first and with Create's machines later.

Requires [Create](https://modrinth.com/mod/create) and [Create: The Factory Must Grow](https://modrinth.com/mod/create-tfmg). Alpha: the geology is in, and the rare earth chain runs from ore to magnet.

## Deposits, not blobs

Ore generates as the kind of body it really forms: banded iron beds, lead-zinc beds in limestone, silver and cobalt veins in calcite, porphyry copper stocks, bauxite and nickel laterite under tropical soil, carbonatite plugs for the rare earths, a dark layered intrusion at the bottom of the world for chromite and the platinum metals. Each body is rich at its core and peters out at the edges, and the grade of an ore block decides what it drops.

![A banded iron bed in crimsite](docs/images/banded-iron-in-crimsite.jpg)
![A silver vein in calcite](docs/images/silver-vein-in-calcite.jpg)
![A lead-zinc bed in limestone](docs/images/lead-zinc-bed-in-limestone.jpg)

An ore takes on whatever rock it formed in. The same hematite sits in stone, deepslate, granite, tuff, calcite, Create's crimsite, limestone, asurine and ochrum, or our own carbonatite and gabbro, and looks like it belongs there.

![Ores in Create's stones](docs/images/ores-in-create-stones.jpg)

## The minerals

Thirty-nine of them, each dropping a raw chunk of itself:

- **Iron and ferroalloys:** Hematite, Magnetite, Goethite, Pyrolusite, Pentlandite, Nickel Laterite, Chromite, Wolframite, Scheelite, Molybdenite, Cobaltite, Ilmenite, Rutile
- **Copper:** Chalcopyrite, Bornite, Chalcocite, Covellite, Malachite, Azurite, Cuprite
- **Aluminium, lead, zinc, tin:** Bauxite, Galena, Sphalerite, Smithsonite, Hemimorphite, Cassiterite
- **Lithium:** Spodumene
- **Rare earths:** Bastnäsite, Monazite, Xenotime, Ion-Adsorption Clay, Loparite, Euxenite
- **Precious:** Native Silver, Argentite, Sperrylite, Cooperite, Braggite, Cinnabar

Vanilla gold and copper ore stay: they are native gold and native copper, which are real minerals. Vanilla iron ore is gone, including the big deep veins, which are magnetite now.

## From rock to metal

Iron is made the way it was made for three thousand years before the blast furnace. Build a bloomery out of clay, load it with iron ore and charcoal (and only charcoal; the sulfur in coal ruins iron), light it with a torch, and wait. What comes out is not an ingot but a bloom, a spongy lump of iron and slag, which you hammer into wrought iron. Copper comes straight out of the bloomery from malachite, azurite and cuprite. Galena has to be roasted on a fire first, then gives lead.

![Bloomeries and a campfire](docs/images/bloomery.jpg)
![Raw chunks, a bloom, slag, the hammer, the mortar and pestle](docs/images/items.jpg)

A mortar and pestle grinds by hand what Create's millstone grinds by power: grain to flour, and coloured minerals to the pigments painters have ground since antiquity. Hold the mortar in one hand and the material in the other.

Everything else is still just ore. The sulfides, zinc, aluminium, nickel, tungsten, the rare earths and the platinum metals wait for Create machinery and the multiblock plant that comes after it. That is the plan, not a gap: the ore is the reason to build the machines.

What that plant will make from the rare earths already exists as items: the ground minerals, the light and heavy mixed concentrates, each of the sixteen elements as oxide, powder and ingot, and the magnet alloys NdFeB and SmCo. The oxides are the colours they really are, and each metal leans toward the colour its salts are known by.

![The rare earth materials](docs/images/rare-earth-materials.png)

The first step toward them works on Create's own machines. A millstone or crushing wheels grind bastnäsite, xenotime, loparite and euxenite. Monazite is a beach sand and needs no grinding. A wash under an encased fan then does what a miner's shaking table does: the light grains wash away and about half of what you feed it stays behind as mixed concentrate, light from monazite and loparite, heavy from xenotime and euxenite. Bastnäsite is floated instead, as it is in every carbonatite mill: its dust beaten with water and naphthenic acid, the fatty-acid collector, in a basin under a mixer, and the rare earth carbonate comes off the top as concentrate. The clay is leached, not milled at all.

The rare earths are parted the way the industry parts them, by solvent extraction in **mixer-settler batteries**. Casings facing the same way merge as you place them, the way Create's tanks do, into any vat from three by three up to three across, three along and two tall, their tanks and batches growing with the volume: nine casings are a lab vat, eighteen a plant vat. The back row is the mixing box, stirred by one of Create's Mechanical Mixers standing over it, the rows ahead the settling bay, with a weir between that only the churn tops and a window in each outside wall to watch the phases part. The vats are open: you can fall in, and neither liquid is good for you. Stages of one size standing end to end are one battery, fed and drained by Create's pipes, and a cut needs the same number of stages at any size, so a line of small vats proves a cut before the plant is built.

What comes out of the dissolver is crude: iron, aluminium, thorium and fines still in it, and each ore leaves its own residue behind (monazite its thorium and phosphate, the clay its aluminium as bauxite). Lime drops the impurities out as a sludge and clarifies the liquor; feed a battery crude liquor instead and it runs three cuts and then fouls, crud at the interface and the organic turned brown, until you drain it and scrub it with lime. Charge every stage with an organic extractant (P507, P204 or naphthenic acid, made from phosphoric acid and kerosene and saponified with lime as it is made up, which is what sets the pH the cuts work at) and it floats on top and is all but never used up: two per cent of a batch leaves entrained in the raffinate each cut. Keep the mixers under 128 rpm, or the phases emulsify and the battery stops. Pump the chloride liquor in at the back of the first stage and hydrochloric acid in at the front of the last: the raffinate comes out of the sides of the first stage, the loaded strip out of the last. How many stages a cut needs depends on how alike the pair is: samarium leaves neodymium in eight stages, but neodymium from praseodymium takes a battery thirty-two stages long. A battery too short for its cut just stalls, and Create's goggles tell you why. Fourteen cuts take the mixed liquor down to single elements; each of those precipitates with oxalic acid and calcines to the oxide. The oxide is the stable form, and the metal comes out of it three ways, all on The Factory Must Grow's chemical vats: the lights (lanthanum to neodymium, and didymium) go through their fluoride, made with hydrofluoric acid from fluorspar, and are electrolysed from the molten salt on electrodes; the heavies (gadolinium to lutetium, and yttrium) are reduced from the fluoride by calcium metal under argon, which gives the fluorspar back as slag; and the four that boil, samarium, europium, thulium and ytterbium, are reduced straight from the oxide by lanthanum metal under argon and distil off, leaving lanthanum oxide to go round again. Electrolysis runs at a blaze burner's heat; the two metallothermic reductions run near 1,500 °C, which is a burner fed a blaze cake. Argon is spun out of air in a centrifuge vat, and calcium is electrolysed from lime and hydrochloric acid.

![A mixer-settler battery](docs/images/mixer-settlers.jpg)

Goggles on any casing tell you what the battery is waiting for, how far the organic and the liquor have got down the line, and what the ends and the product tanks hold; hold W over a casing for the Ponder scene of a working stage.

### What they are for

Nothing in the chain is for its own sake. Neodymium (or didymium, as the industry sinters it) with iron, a borax from the desert evaporites and a little dysprosium, superheated, makes NdFeB, the strongest magnet; samarium with cobaltite makes SmCo, which keeps its field hot. Polarized, either is the magnet The Factory Must Grow's motors, generators and electric pumps are built from, so a rare earth plant is what a motor needs. Lanthanum metal reduces the four that boil and lanthanum oxide is the catalyst its naphtha cracking spends. Cerium with iron is ferrocerium: a flint and steel that never wears out. Europium's red and terbium's green on a yttria host are the phosphor its lamps take; yttria lines its fireproof vat. Didymium glass is the welder's lens Create's goggles are made of, erbium turns glass pink, and scandium in aluminium is the airframe alloy that makes a panel rack go twice as far. Gadolinium waits for a reactor.

### Waste

A separation plant makes waste. Every cut leaves a fifth of a batch of spent chloride liquor in a sump under the head stage; when the sump is full the battery stops, and the goggles say so. Pump it out from below: limesand neutralises it to brine, and brine boiled down leaves salt for the clay leach, so the plant's water closes its own loop. Monazite carries thorium, and when the light concentrate dissolves the thorium stays behind as a mildly radioactive residue that packs into blocks to bury.

### Acids

The acids exist outside pipes. Hydrochloric, hydrofluoric, nitric and phosphoric acid can be bucketed and poured, burn whatever stands in them (hydrofluoric also poisons), and eat what they really eat: pour one against such a block and it cracks as if being mined, fizzes for five seconds and is gone, and the acid that ate it is spent. Hydrochloric acid takes carbonates, calcite, limestone, dripstone and bone; hydrofluoric takes glass, sand and quartz; nitric takes copper and iron. Stone shrugs them all off. Hydrofluoric and nitric acid fume: within two blocks of either in the open, as a block or in a basin, you take a hit a second unless you wear Create's diving helmet on a filled backtank, the gas mask. And acid eats copper: a Create pipe carrying any acid corrodes and bursts within a couple of minutes, spilling it; run acid in The Factory Must Grow's plastic or glass pipes.

### The other metals

Cobaltite from the silver-cobalt veins roasts on a fire and blasts to cobalt; four of it with a samarium make SmCo, two with four nickel and a rhenium make the nickel superalloy that The Factory Must Grow's turbine blades are now cast from, and one goes into its lithium charge as the cathode. Chalcopyrite from a porphyry stock roasts to an oxide the bloomery smelts to copper. Molybdenite roasted in a heated basin gives molybdenum trioxide and, up the flue, the rhenium dust that is the only source of rhenium there is; hydrogen in a heated vat reduces both to metal, and molybdenum in steel makes the plate the heavy machinery casing now takes. Scheelite and wolframite decompose in hot hydrochloric acid to tungsten oxide, hydrogen reduces it, and the metal draws to the filament every light bulb burns and carburises to the carbide every mechanical drill bites with.

### The book

With Patchouli installed, a book and a raw hematite craft *Fundamentals: First Principles*: every deposit and where to find it, the hand-working, the whole rare earth chain with the stage count of every cut and the road to every metal, the uses, the waste and the acids. It is generated from the same data the game runs on, so its numbers are the game's.

## Power

A solar panel goes on a rack. Build the rack from steel and set it down facing the way you want, then mount a photovoltaic panel on it: a P and an N semiconductor from The Factory Must Grow under glass, in an aluminium frame. Under open sky it feeds TFMG's electrical network, 120 volts while the sun is up and up to 200 watts at noon, nothing at night or in shade. Break it and you get the rack and the panel back.
Where The Factory Must Grow has its own ore for a metal, ours takes its place and ends in the same ingot. Lead comes from galena, nickel from pentlandite and nickel laterite, and lithium from spodumene: roast the spodumene white in a blast furnace, then run it through an electrode vat with sulfuric acid. Bauxite grinds to the powder TFMG's vat turns into aluminium. Silicon still comes from Nether quartz.

## Status

Alpha. Expect ore amounts and recipes to change. Not yet in any pack. Issues and ideas welcome.

## Building

JDK 21 is provisioned by Gradle. `./gradlew buildAll` builds every target; jars land in `versions/<target>/build/libs/`. `tools/gametest.sh` runs the gametests headless and prints only the failures ("No tests found" after the run means all passed). Textures and ore data are generated by the scripts in `tools/`, not edited by hand. `tools/showcase/build_showcase.py` writes `tools/showcase/datapack` from the cut recipes: the whole separation tree, fourteen batteries from the mixed liquor down to every single element, each with its mixers, lever, organic and acid feeds and sump, piped into one another, with an oxalate-to-oxide station on every element. `tools/showcase/fresh.sh` builds a flat world, stages that in it and opens the dev client; `tools/showcase/reopen.sh` reopens that world once the client has quit. Never run either while someone is playing: they close the client. The working plan lives in `docs/PLAN.md`; what's agreed but not built yet is in `TODO.md`.

MIT licensed.
