# Fundamentals

Real ore minerals, real deposits, and the real road from rock to metal. A Create add-on for NeoForge 1.21.1.

![Every ore in the mod](docs/images/all-ores.png)

Vanilla gives you "iron ore". The ground doesn't. Iron is in hematite and magnetite, lead is in galena, zinc is in sphalerite, the rare earths are in bastnäsite and monazite and a clay that doesn't even have a mineral name. Fundamentals replaces the generic ores with the minerals that actually carry each metal, puts them in the ground the way geology does, and makes you win the metal out of them the way smiths and smelters did, by hand at first and with Create's machines later.

Requires [Create](https://modrinth.com/mod/create) and [Create: The Factory Must Grow](https://modrinth.com/mod/create-tfmg). Early alpha: the geology is in, the processing is just beginning.

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

The first step toward them works on Create's own machines. A millstone or crushing wheels grind bastnäsite, xenotime, loparite and euxenite. Monazite is a beach sand and needs no grinding. A wash under an encased fan then does what a miner's shaking table does: the light grains wash away and about half of what you feed it stays behind as mixed concentrate, light from monazite and loparite, heavy from xenotime and euxenite. Bastnäsite has to be floated and the clay leached, and those machines are not built yet.

The rare earths are parted the way the industry parts them, by solvent extraction in **mixer-settler batteries**. Casings facing the same way merge as you place them, the way Create's tanks do, into any vat up to three across, three along and two tall, their tanks and batches growing with the volume: a single casing is a lab box, eighteen are a plant vat. The back row is the mixing trough under its motor, the rows ahead the settling bay, with a weir between and a window strip along the bay. Stages of one size standing end to end are one battery, fed and drained by Create's pipes, and a cut needs the same number of stages at any size, so a line of small vats proves a cut before the plant is built.

Charge every stage with an organic extractant (P507, P204 or naphthenic acid, made from phosphoric acid and kerosene) and it floats on top and is never used up. Pump the chloride liquor in at the back of the first stage and hydrochloric acid in at the front of the last: the raffinate comes out of the sides of the first stage, the loaded strip out of the last. How many stages a cut needs depends on how alike the pair is: samarium leaves neodymium in eight stages, but neodymium from praseodymium takes a battery thirty-two stages long. A battery too short for its cut just stalls, and Create's goggles tell you why. Fourteen cuts take the mixed liquor down to single elements; each of those precipitates with oxalic acid and calcines to the oxide.

![A mixer-settler battery](docs/images/mixer-settlers.jpg)

## Power

A solar panel goes on a rack. Build the rack from steel and set it down facing the way you want, then mount a photovoltaic panel on it: a P and an N semiconductor from The Factory Must Grow under glass, in an aluminium frame. Under open sky it feeds TFMG's electrical network, 120 volts while the sun is up and up to 200 watts at noon, nothing at night or in shade. Break it and you get the rack and the panel back.
Where The Factory Must Grow has its own ore for a metal, ours takes its place and ends in the same ingot. Lead comes from galena, nickel from pentlandite and nickel laterite, and lithium from spodumene: roast the spodumene white in a blast furnace, then run it through an electrode vat with sulfuric acid. Bauxite grinds to the powder TFMG's vat turns into aluminium. Silicon still comes from Nether quartz.

## Status

Alpha. Expect ore amounts and recipes to change. Not yet in any pack. Issues and ideas welcome.

## Building

JDK 21 is provisioned by Gradle. `./gradlew buildAll` builds every target; jars land in `versions/<target>/build/libs/`. Textures and ore data are generated by the scripts in `tools/`, not edited by hand. `./gradlew :1.21.1-neoforge:runShowcase` opens the dev client into `run/saves/showcase`, a staged world (local only) for looking at the blocks. The working plan lives in `docs/PLAN.md`.

MIT licensed.
