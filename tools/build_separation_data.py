#!/usr/bin/env python3
"""The rare earth separation line: the reagent fluids, the solvent-extraction cuts, and the chemistry
around them. Writes the Java reagent table, the cut recipes, the Create mixing recipes that make the
reagents and liquors, the oxalate route out, the magnetic route for the cuts whose ions differ in moment (at MOMENTS
below), the mixer-settler's, the magnetomigration cell's and the plastic tank's blockstates, models,
loot and recipes, the names, and the gametest template. Each cut parts a batch in the proportion its feed carries
light and heavy: Australian monazite and Longnan ion-adsorption clay, per Gupta and Krishnamurthy, Extractive Metallurgy
of Rare Earths (2005), at MONAZITE below. Re-run after any edit; build_ore_data.py last for the tool tags.

    python3 tools/paint_separation.py && python3 tools/build_separation_data.py && python3 tools/build_ore_data.py
"""
import gzip
import json
import math
import shutil
from pathlib import Path

from build_ore_data import ASSETS, DATA, ROOT, drop_self, write
from paint_materials import MATERIALS

JAVA = Path(__file__).resolve().parent.parent / "src/main/java/ai/gsmc/fundamentals/separation/Reagents.java"
RECIPES = DATA / "recipe"

# The vat's proportions in sixteenths of a block. Written to fundamentals_vat.json for VatGeometry.java, so the
# models here, the fluid renderer and the buoyancy agree on where the floor, the walls, the weir and the window are.
VAT = {
    "wall": 1,             # thickness of every wall and the weir
    "floor": 1,            # thickness of the floor under the bottom layer
    "rim_clearance": 5,    # the settled phases stop this far under the rim
    "weir_below_rim": 3,   # the weir's lip, between the trough and the bay, stands this far under the rim
    "window": [4, 12],     # the glass spans these pixels of a window casing's wall, between two posts
}

# id: (name, tint, kind). Tints are what the chloride solutions look like: Nd lilac, Pr green, Er pink,
# Sm and Dy straw, Ho and Tm faintly yellow and green, the rest colourless, so a mixed liquor takes the
# colour of whatever in it is coloured.
CLEAR = 0xDCE6EC
LIQUORS = {
    "rare_earth_liquor": ("Rare Earth Liquor", 0xB9A8C8),
    "light_rare_earth_liquor": ("Light Rare Earth Liquor", 0xB4A6CF),
    "heavy_rare_earth_liquor": ("Heavy Rare Earth Liquor", 0xE2D6B0),
    "lanthanum_cerium_liquor": ("Lanthanum-Cerium Liquor", CLEAR),
    "praseodymium_neodymium_liquor": ("Didymium Liquor", 0x9A8CC4),
    "samarium_europium_gadolinium_liquor": ("Samarium-Europium-Gadolinium Liquor", 0xE8DFB4),
    "europium_gadolinium_liquor": ("Europium-Gadolinium Liquor", 0xE4DEDA),
    "terbium_to_lutetium_liquor": ("Terbium-to-Lutetium Liquor", 0xE6D4C0),
    "terbium_dysprosium_liquor": ("Terbium-Dysprosium Liquor", 0xE6DFC2),
    "yttrium_heavies_liquor": ("Yttrium and Late Heavies Liquor", 0xE4CFC6),
    "holmium_to_lutetium_liquor": ("Holmium-to-Lutetium Liquor", 0xE5C8C0),
    "holmium_erbium_liquor": ("Holmium-Erbium Liquor", 0xE6C2C0),
    "thulium_ytterbium_lutetium_liquor": ("Thulium-Ytterbium-Lutetium Liquor", 0xDCE4D4),
    "ytterbium_lutetium_liquor": ("Ytterbium-Lutetium Liquor", CLEAR),
    "lanthanum_liquor": ("Lanthanum Liquor", CLEAR),
    "cerium_liquor": ("Cerium Liquor", CLEAR),
    "praseodymium_liquor": ("Praseodymium Liquor", 0xA8D6A0),
    "neodymium_liquor": ("Neodymium Liquor", 0x9C86CC),
    "samarium_liquor": ("Samarium Liquor", 0xEFE6A8),
    "europium_liquor": ("Europium Liquor", 0xE8D8D8),
    "gadolinium_liquor": ("Gadolinium Liquor", CLEAR),
    "terbium_liquor": ("Terbium Liquor", CLEAR),
    "dysprosium_liquor": ("Dysprosium Liquor", 0xEEE8B8),
    "holmium_liquor": ("Holmium Liquor", 0xECE4B0),
    "erbium_liquor": ("Erbium Liquor", 0xECB4BC),
    "thulium_liquor": ("Thulium Liquor", 0xD4E8C8),
    "ytterbium_liquor": ("Ytterbium Liquor", CLEAR),
    "lutetium_liquor": ("Lutetium Liquor", CLEAR),
    "yttrium_liquor": ("Yttrium Liquor", CLEAR),
    "scandium_liquor": ("Scandium Liquor", CLEAR),
}
# P507 and P204 are colourless to pale yellow and ride in kerosene, so they are straw; naphthenic acid is the dark one.
ORGANICS = {
    "p204": ("P204", 0xEAD88C),
    "p507": ("P507", 0xECE0A8),
    "naphthenic_acid": ("Naphthenic Acid", 0xA8843C),
}
ACIDS = {
    "hydrochloric_acid": ("Hydrochloric Acid", 0xE4EEF2),
    "nitric_acid": ("Nitric Acid", 0xF0EDC8),
    "phosphoric_acid": ("Phosphoric Acid", 0xE8ECE4),
    "hydrofluoric_acid": ("Hydrofluoric Acid", 0xE6F0EA),
    # three of hydrochloric to one of nitric, fuming orange-red with the nitrosyl chloride and chlorine it gives off
    "aqua_regia": ("Aqua Regia", 0xE0662A),
    # No acid, but it lives with them: a dense, dark red-brown liquid that boils at 59 C, fumes, burns the skin and eats metal.
    "bromine": ("Bromine", 0x7A1E0E),
}
# What the plant cannot use: the spent chloride liquor every cut leaves behind, and the calcium chloride liquor it
# becomes once lime has neutralised it. That boils down to calcium chloride, which is where calcium metal comes from.
# Once called brine; the old id is aliased to this one so tanks in older worlds keep it.
WASTES = {
    "spent_liquor": ("Spent Liquor", 0x8E9A86),
    "calcium_chloride_liquor": ("Calcium Chloride Liquor", 0xDCE6E4),
}
# Seawater is water in every way but its salt: it looks, pours and flows as water does, and its tint is vanilla water's.
SEA = {"seawater": ("Seawater", 0x3F76E4)}
# Bittern, the bitter, faintly yellow mother liquor left once the halite has crystallised out of seawater, rich in magnesium
# chloride; a strong chloride, it eats copper like the liquors.
SALINES = {"bittern": ("Bittern", 0xE6DEB8), "salt_brine": ("Salt Brine", 0xE2ECEE)}
# What comes out of the dissolver before anyone has cleaned it: iron, aluminium, thorium and fines still in it. A
# battery will take it, and it will foul the organic. Lime drops the impurities and clarifies it.
CRUDES = {
    "crude_rare_earth_liquor": ("Crude Rare Earth Liquor", 0x8E7F86),
    "crude_heavy_rare_earth_liquor": ("Crude Heavy Rare Earth Liquor", 0x9E9A80),
}
# An organic with crud at its interface: fines and hydroxides from a dirty feed. Lime scrubs it back.
FOULED = {
    "fouled_p204": ("Fouled P204", 0x6E5A38),
    "fouled_p507": ("Fouled P507", 0x72603E),
    "fouled_naphthenic_acid": ("Fouled Naphthenic Acid", 0x5A4424),
}
# The extractants' road from propylene and bone: the alcohol both are esters of, the phosphorus chloride both are built on,
# and the two neat extractants before kerosene cuts them. All four are colourless to pale yellow.
PRECURSORS = {
    "ethylhexanol": ("2-Ethylhexanol", 0xEEEEE6),
    "phosphorus_trichloride": ("Phosphorus Trichloride", 0xECF0EC),
    "d2ehpa": ("D2EHPA", 0xF0E4B0),
    "ehehpa": ("EHEHPA", 0xF2EAC4),
    # colourless like phosphorus trichloride, and like it fuming to hydrogen chloride in moist air but harmless to dry steel
    "titanium_tetrachloride": ("Titanium Tetrachloride", 0xEEF0EA),
    # the monomer of PVC, a colourless gas liquefied under pressure
    "vinyl_chloride": ("Vinyl Chloride", 0xEEF0EE),
}
GASES = {
    "argon": ("Argon", 0xC8D8F0),
    "chlorine": ("Chlorine", 0xD2E496),
    "water_gas": ("Water Gas", 0xD8DCE0),
    "ammonia": ("Ammonia", 0xE4ECF0),
    # the two volatile tetroxides of the platinum refinery: osmium's pale yellow, ruthenium's yellow-orange
    "osmium_tetroxide": ("Osmium Tetroxide", 0xF2E8A0),
    "ruthenium_tetroxide": ("Ruthenium Tetroxide", 0xF0A830),
}
# The base-metal refinery's sulfate leach, green with nickel, and the precious-metal refinery's chloride liquors, the colours of
# their complexes: chloroplatinic acid orange, tetrachloropalladate red-brown, palladium's tetrammine colourless, hexachloroiridate
# dark red-brown, rhodium's chloro complexes rose.
PLATINUM_LIQUORS = {
    "nickel_copper_sulfate": ("Nickel-Copper Sulfate Liquor", 0x58A890),
    "platinum_palladium_liquor": ("Platinum-Palladium Liquor", 0xC8701E),
    "palladium_liquor": ("Palladium Liquor", 0xA8542A),
    "palladium_tetrammine_liquor": ("Palladium Tetrammine Liquor", 0xE6E8E0),
    "iridium_rhodium_liquor": ("Iridium-Rhodium Liquor", 0x6A2A1E),
    "rhodium_liquor": ("Rhodium Liquor", 0xC85A6A),
}
# beryllium sulfate, with the aluminium the beryl carried, in the sulfuric acid that opened the frit or the tuff: colourless
SULFATE_LIQUORS = {"beryllium_sulfate_liquor": ("Beryllium Sulfate Liquor", CLEAR)}
CAUSTICS = {"caustic_soda": ("Caustic Soda", 0xEEF2F0), "sodium_aluminate_liquor": ("Sodium Aluminate Liquor", 0xB8864A)}
FLUIDS = {**{k: (*v, "LIQUOR") for k, v in {**LIQUORS, **PLATINUM_LIQUORS, **SULFATE_LIQUORS}.items()}, **{k: (*v, "ORGANIC") for k, v in ORGANICS.items()},
          **{k: (*v, "ACID") for k, v in ACIDS.items()}, **{k: (*v, "GAS") for k, v in GASES.items()},
          **{k: (*v, "WASTE") for k, v in WASTES.items()}, **{k: (*v, "SALINE") for k, v in SALINES.items()}, **{k: (*v, "WATER") for k, v in SEA.items()}, **{k: (*v, "CRUDE") for k, v in CRUDES.items()},
          **{k: (*v, "FOULED") for k, v in FOULED.items()}, **{k: (*v, "PRECURSOR") for k, v in PRECURSORS.items()},
          **{k: (*v, "CAUSTIC") for k, v in CAUSTICS.items()}}

# Oxide to metal. The lights and the heavies go through their fluoride: the lights by molten-salt
# electrolysis on TFMG's electrodes, the heavies by calciothermic reduction under argon, which gives the
# fluorite back as slag. Samarium, which boils, is reduced straight from the oxide by lanthanum metal under
# argon and distils off, leaving lanthanum oxide to go round again. Europium and the heavies past dysprosium
# are sold as oxide and never reduced.
ELECTROLYSIS = ("lanthanum", "cerium", "praseodymium", "neodymium", "didymium")
CALCIOTHERMIC = ("gadolinium", "terbium", "dysprosium", "yttrium", "scandium")
LANTHANOTHERMIC = ("samarium",)
# which liquor precipitates each element's oxalate; everything else is its own name
OXALATE_FROM = {"didymium": "praseodymium_neodymium_liquor"}

# One cut per mixer-settler battery: (liquor in, organic, stages, light out, heavy out). The stage count
# follows the separation factor of the pair being parted: Sm/Nd is about 10 and parts in a few stages,
# Nd/Pr is about 1.4 and takes a battery thirty-two long. Everything strips with hydrochloric acid.
CUTS = [
    ("rare_earth_liquor", "p507", 8, "light_rare_earth_liquor", "heavy_rare_earth_liquor"),
    ("light_rare_earth_liquor", "p507", 14, "lanthanum_cerium_liquor", "praseodymium_neodymium_liquor"),
    ("lanthanum_cerium_liquor", "p507", 12, "lanthanum_liquor", "cerium_liquor"),
    ("praseodymium_neodymium_liquor", "p507", 32, "praseodymium_liquor", "neodymium_liquor"),
    ("heavy_rare_earth_liquor", "p204", 10, "samarium_europium_gadolinium_liquor", "terbium_to_lutetium_liquor"),
    ("samarium_europium_gadolinium_liquor", "p507", 16, "samarium_liquor", "europium_gadolinium_liquor"),
    ("terbium_to_lutetium_liquor", "p204", 12, "terbium_dysprosium_liquor", "yttrium_heavies_liquor"),
    ("terbium_dysprosium_liquor", "p507", 20, "terbium_liquor", "dysprosium_liquor"),
    ("yttrium_heavies_liquor", "naphthenic_acid", 14, "yttrium_liquor", "holmium_to_lutetium_liquor"),
    ("holmium_to_lutetium_liquor", "p204", 10, "holmium_erbium_liquor", "thulium_ytterbium_lutetium_liquor"),
    ("holmium_erbium_liquor", "p507", 20, "holmium_liquor", "erbium_liquor"),
    ("thulium_ytterbium_lutetium_liquor", "p204", 12, "thulium_liquor", "ytterbium_lutetium_liquor"),
    ("ytterbium_lutetium_liquor", "p507", 20, "ytterbium_liquor", "lutetium_liquor"),
]
# A cut does not part its liquor in half: each batch comes out light and heavy in the proportion the feed carries
# them. Per cent of the rare earth oxide in the two feeds, from Gupta and Krishnamurthy, Extractive Metallurgy of Rare
# Earths (CRC, 2005), chapter 1, the analyses Castor and Hedrick also give in Industrial Minerals and Rocks (SME, 2006).
# The mixed liquor is the lights' ore: Australian east-coast monazite, which bastnäsite (Mountain Pass: La 33, Ce 49,
# Pr 4, Nd 12, everything after neodymium about 1 per cent) only makes lighter still. The heavy liquor is above all
# the clay's: the high-yttrium ion-adsorption clay of Longnan, Jiangxi, which Malaysian xenotime (Y 61, Dy 8, Er 6,
# Yb 7, Gd 4) closely resembles. The clay's own few per cent of lanthanum to neodymium ride with the samarium.
MONAZITE = {"La": 23.9, "Ce": 46.0, "Pr": 5.0, "Nd": 17.4, "Sm": 2.53, "Eu": 0.05, "Gd": 1.49, "Tb": 0.04, "Dy": 0.69,
            "Ho": 0.05, "Er": 0.21, "Tm": 0.01, "Yb": 0.12, "Lu": 0.04, "Y": 2.41}
ION_CLAY = {"Sm": 2.8, "Eu": 0.1, "Gd": 6.9, "Tb": 1.3, "Dy": 6.7, "Ho": 1.6, "Er": 4.9, "Tm": 0.7, "Yb": 2.5, "Lu": 0.4, "Y": 64.9}
CARRIES = {
    "rare_earth_liquor": tuple(MONAZITE),
    "light_rare_earth_liquor": ("La", "Ce", "Pr", "Nd"),
    "heavy_rare_earth_liquor": tuple(ION_CLAY),
    "lanthanum_cerium_liquor": ("La", "Ce"),
    "praseodymium_neodymium_liquor": ("Pr", "Nd"),
    "samarium_europium_gadolinium_liquor": ("Sm", "Eu", "Gd"),
    "europium_gadolinium_liquor": ("Eu", "Gd"),
    "terbium_to_lutetium_liquor": ("Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu", "Y"),
    "terbium_dysprosium_liquor": ("Tb", "Dy"),
    "yttrium_heavies_liquor": ("Y", "Ho", "Er", "Tm", "Yb", "Lu"),
    "holmium_to_lutetium_liquor": ("Ho", "Er", "Tm", "Yb", "Lu"),
    "holmium_erbium_liquor": ("Ho", "Er"),
    "thulium_ytterbium_lutetium_liquor": ("Tm", "Yb", "Lu"),
    "ytterbium_lutetium_liquor": ("Yb", "Lu"),
    **{f"{e}_liquor": (s,) for e, s in (("lanthanum", "La"), ("cerium", "Ce"), ("praseodymium", "Pr"), ("neodymium", "Nd"),
                                        ("samarium", "Sm"), ("europium", "Eu"), ("gadolinium", "Gd"), ("terbium", "Tb"),
                                        ("dysprosium", "Dy"), ("holmium", "Ho"), ("erbium", "Er"), ("thulium", "Tm"),
                                        ("ytterbium", "Yb"), ("lutetium", "Lu"), ("yttrium", "Y"))},
}
# Europium is not parted from gadolinium by a cut. Zinc reduces it, alone of the rare earths, to Eu2+, which sulfate drops
# as europium(II) sulfate the way it drops barium (McCoy, 1935; the reduction route every Chinese europium line runs, per
# Gupta and Krishnamurthy), and the gadolinium stays in the liquor. A batch gives europium in the proportion the 18-stage P507
# cut it replaced gave, and the sulfate goes back into nitric acid, oxidised, as the europium liquor the oxalate wants.
ZINC_REDUCTION = ("europium_gadolinium_liquor", "europium_sulfate", "europium_liquor", "gadolinium_liquor")
LIGHT_BRANCH = ("rare_earth_liquor", "light_rare_earth_liquor", "lanthanum_cerium_liquor", "praseodymium_neodymium_liquor")
# Fractions are rounded to twentieths and kept between one and nineteen of them, so a pilot battery's 90 mB batch
# still gives 5 mB of the scarce side: europium is 1.4 per cent of what feeds its cut and comes out at 5.
LEAST = 0.05


def light_fraction(liquor, light, heavy):
    feed = MONAZITE if liquor in LIGHT_BRANCH else ION_CLAY
    assert set(CARRIES[light]) | set(CARRIES[heavy]) == set(CARRIES[liquor]), liquor
    share = sum(feed[e] for e in CARRIES[light]) / sum(feed[e] for e in CARRIES[liquor])
    return min(1 - LEAST, max(LEAST, round(share * 20) / 20))


# Magnetomigration. The trivalent ions are chemically near-twins, which is why solvent extraction takes dozens of stages,
# but their 4f shells give them very different paramagnetism. Effective moments of the Ln3+ ions in Bohr magnetons (Y3+,
# La3+ and Lu3+ have no unpaired f electron and are diamagnetic). A permanent magnet's field gradient pulls the paramagnetic
# ions through the solution toward it and leaves the diamagnetic ones: PNNL, "Local magnetic field gradients enable critical
# material separations" (2026), https://www.pnnl.gov/publications/local-magnetic-field-gradients-enable-critical-material-separations;
# PCCP (2025), Dy3+ and Gd3+ moving toward the strongest gradient and Y3+ away, https://pubs.rsc.org/en/content/articlepdf/2025/cp/d5cp02703a;
# KU Leuven, migration that follows susceptibility, https://lirias.kuleuven.be/bitstream/123456789/642674/2/MagSuscGradients_postprint.pdf.
# It is a laboratory result: no plant runs it.
MOMENTS = {"La": 0, "Ce": 2.5, "Pr": 3.6, "Nd": 3.6, "Sm": 1.5, "Eu": 3.4, "Gd": 7.9, "Tb": 9.7, "Dy": 10.6, "Ho": 10.6,
           "Er": 9.6, "Tm": 7.6, "Yb": 4.5, "Lu": 0, "Y": 0}
# A cut is magnetic only when its two products sort by moment: the weaker side carries at most a tenth of the stronger
# side's susceptibility, and at most a twentieth of the feed sits on the wrong side (an ion whose own susceptibility is
# nearer the other product's). Susceptibility goes as the moment squared (Curie), and a cell drifts ions as far as their
# susceptibility contrast carries them, so the passes a cut needs go as PASS_MOMENT over the moment that contrast is worth.
WEAK = 0.1
STRAY = 0.05
PASS_MOMENT = 16


def magnetic_cut(liquor, stages, light, heavy):
    """(the product drawn to the magnet, passes) for a cut whose products sort by moment, or None."""
    feed = MONAZITE if liquor in LIGHT_BRANCH else ION_CLAY

    def chi(side):
        return sum(feed[e] * MOMENTS[e] ** 2 for e in CARRIES[side]) / sum(feed[e] for e in CARRIES[side])

    (drawn, strong), (left, weak) = sorted(((light, chi(light)), (heavy, chi(heavy))), key=lambda kv: -kv[1])
    if strong == 0 or weak > WEAK * strong:
        return None
    middle = (strong + weak) / 2
    stray = sum(feed[e] for e in CARRIES[drawn] if MOMENTS[e] ** 2 < middle) + sum(feed[e] for e in CARRIES[left] if MOMENTS[e] ** 2 > middle)
    if stray > STRAY * sum(feed[e] for e in CARRIES[liquor]):
        return None
    passes = math.ceil(PASS_MOMENT / math.sqrt(strong - weak))
    assert passes <= 0.6 * stages, liquor
    return drawn, passes


MAGNETIC = {liquor: cut for liquor, _, stages, light, heavy in CUTS if (cut := magnetic_cut(liquor, stages, light, heavy))}


STRIP = "hydrochloric_acid"
# the plant's items that are not a form of a material
PLANT_ITEMS = {"salt": "Salt", "oxalic_acid": "Oxalic Acid", "roasted_bastnasite": "Roasted Bastnäsite", "light_rare_earth_sulfate": "Light Rare Earth Sulfate",
               "heavy_rare_earth_sulfate": "Heavy Rare Earth Sulfate", "calcium_chloride": "Calcium Chloride", "calcium_ingot": "Calcium Ingot",
               "white_phosphorus": "White Phosphorus", "light_rare_earth_carbonate": "Light Rare Earth Carbonate",
               "heavy_rare_earth_carbonate": "Heavy Rare Earth Carbonate", "cerium_concentrate": "Cerium Concentrate", "europium_sulfate": "Europium Sulfate"}


def fluid(id, amount):
    return {"type": "neoforge:single", "amount": amount, "fluid": id if ":" in id else f"fundamentals:{id}"}


def item(id, count=1):
    out = {"item": id if ":" in id else f"fundamentals:{id}"}
    return out if count == 1 else [out] * count


def mixing(name, ingredients, results, heated=False):
    recipe = {"type": "create:mixing", "ingredients": ingredients, "results": results}
    if heated:
        recipe["heat_requirement"] = "heated"
    write(RECIPES / f"mixing/{name}.json", recipe)


def result_fluid(id, amount):
    return {"id": f"fundamentals:{id}", "amount": amount}


def result_item(id, count=1):
    out = {"id": f"fundamentals:{id}"}
    return out if count == 1 else {**out, "count": count}


def java_table():
    lines = ["package ai.gsmc.fundamentals.separation;", "", "import java.util.List;", "",
             "// Written by tools/build_separation_data.py; edit the table there.",
             "public final class Reagents {", "",
             "    public enum Kind { LIQUOR, ORGANIC, ACID }", "",
             "    public record Reagent(String id, int tint, Kind kind) {}", "",
             "    public static final List<Reagent> ALL = List.of("]
    entries = [f'            new Reagent("{id}", 0x{tint:06X}, Kind.{kind})' for id, (_, tint, kind) in FLUIDS.items()]
    lines += [",\n".join(entries) + ");", "", "    private Reagents() {}", "}", ""]
    kinds = sorted({kind for _, _, kind in FLUIDS.values()}, key=["LIQUOR", "ORGANIC", "ACID", "GAS", "WASTE", "SALINE", "WATER", "CRUDE", "FOULED", "PRECURSOR", "CAUSTIC"].index)
    lines[7] = "    public enum Kind { " + ", ".join(kinds) + " }"
    JAVA.write_text("\n".join(lines), encoding="utf-8")


# What each acid eats when it stands against it in the world; hashed entries are tags, and the modded
# blocks are optional so the tags load without their mods.
DISSOLVES = {
    "hydrochloric_acid": ["minecraft:calcite", "minecraft:dripstone_block", "minecraft:pointed_dripstone", "minecraft:bone_block", "create:limestone"],
    "hydrofluoric_acid": ["#c:glass_blocks", "#c:glass_panes", "#minecraft:sand", "minecraft:sandstone", "minecraft:red_sandstone", "minecraft:quartz_block", "minecraft:smooth_quartz",
                          "minecraft:tuff"],
    "nitric_acid": ["#c:storage_blocks/copper", "#c:storage_blocks/iron", "minecraft:copper_block", "minecraft:iron_block", "minecraft:cut_copper"],
    "phosphoric_acid": [],
    "aqua_regia": ["#c:storage_blocks/gold", "minecraft:gold_block", "minecraft:raw_gold_block", "#c:storage_blocks/copper", "#c:storage_blocks/iron",
                   "minecraft:copper_block", "minecraft:iron_block", "minecraft:cut_copper"],
    "bromine": ["#c:storage_blocks/aluminum", "#c:storage_blocks/copper", "#c:storage_blocks/iron", "minecraft:copper_block", "minecraft:iron_block",
                "minecraft:cut_copper"],
}


# What the plant's vessels are built of: plastic sheet, or stainless steel plate as the mixer-settlers of a plant without a
# polymer works are, so neither the separation plant nor the cracker that gives the olefins waits on the other.
ACID_PROOF = [{"tag": "fundamentals:plastic_sheets"}, {"tag": "c:plates/stainless_steel"}]


def acids():
    """The acids' blocks, buckets and appetites, and the seawater bucket."""
    for acid, eats in DISSOLVES.items():
        write(ASSETS / f"blockstates/{acid}.json", {"variants": {"": {"model": f"fundamentals:block/{acid}"}}})
        write(ASSETS / f"models/block/{acid}.json", {"textures": {"particle": "fundamentals:block/fluid/liquor_still"}})
        write(ASSETS / f"models/item/{acid}_bucket.json", {"parent": "neoforge:item/bucket", "loader": "neoforge:fluid_container", "fluid": f"fundamentals:{acid}"})
        values = [{"id": v, "required": False} if ":" in v and not v.startswith("#") and not v.startswith("minecraft:") else v for v in eats]
        write(DATA / f"tags/block/dissolves/{acid}.json", {"replace": False, "values": values})
    write(ASSETS / "models/item/seawater_bucket.json", {"parent": "neoforge:item/bucket", "loader": "neoforge:fluid_container", "fluid": "fundamentals:seawater"})


def chemistry():
    # Sea salt: seawater boiled down in a heated pan leaves the halite and, once it has crystallised, the bittern. Fresh water
    # carries next to no salt. The steam is the rest of it, condensed: a vacuum-pan salt works and a thermal desalination
    # plant are the same evaporator, so the pan gives back the 900 mB the halite and bittern do not keep, as fresh water.
    # Then the Mannheim process for the acid.
    mixing("salt", [fluid("seawater", 1000)], [result_item("salt", 2), result_fluid("bittern", 100), {"id": "minecraft:water", "amount": 900}], heated=True)
    mixing("hydrochloric_acid", item("salt", 2) + [fluid("tfmg:sulfuric_acid", 500)], [result_fluid("hydrochloric_acid", 500)], heated=True)
    # Saltpetre heated in sulfuric acid gives up nitric acid, which boils off at 83 C into the receiver: Glauber's retort.
    mixing("nitric_acid", item("tfmg:nitrate_dust", 2) + [fluid("tfmg:sulfuric_acid", 500)], [result_fluid("nitric_acid", 500)], heated=True)
    # Carbohydrate oxidised by nitric acid: the classical oxalic acid route.
    mixing("oxalic_acid", item("minecraft:sugar", 2) + [fluid("nitric_acid", 250)], [result_item("oxalic_acid", 2)], heated=True)
    # Wet-process phosphoric acid from a phosphate rock, which bone meal stands in for.
    mixing("phosphoric_acid", item("minecraft:bone_meal", 2) + [fluid("tfmg:sulfuric_acid", 500)], [result_fluid("phosphoric_acid", 500)])
    # The organophosphorus extractants are made neat (solvents() below) and cut with kerosene, a quarter extractant, only
    # as the organic is made up. Each is saponified then too, with lime rather than ammonia (calcium saponification, which
    # Chinese plants moved to so that the raffinate carries no ammonia): that is what sets the pH the cuts work at.
    # Naphthenic acid is petroleum's own: washed out of the oil as soluble sodium soaps with soda ash and freed again with acid.
    mixing("p204", [fluid("d2ehpa", 250), fluid("tfmg:kerosene", 750), item("tfmg:limesand")], [result_fluid("p204", 1000)])
    mixing("p507", [fluid("ehehpa", 250), fluid("tfmg:kerosene", 750), item("tfmg:limesand")], [result_fluid("p507", 1000)])
    mixing("naphthenic_acid", [fluid("tfmg:heavy_oil", 1000), fluid("tfmg:sulfuric_acid", 250), item("soda_ash")], [result_fluid("naphthenic_acid", 500)], heated=True)
    # Froth flotation for bastnasite: the ground mineral beaten with water and a fatty-acid collector, naphthenic acid,
    # floats the rare earth carbonate off the gangue. About half of what goes in comes out as concentrate.
    mixing("bastnasite_concentrate", item("bastnasite_dust", 2) + [fluid("minecraft:water", 250), fluid("naphthenic_acid", 100)],
           [result_item("bastnasite_concentrate"), {"id": "fundamentals:bastnasite_concentrate", "chance": 0.5}])
    # Bastnäsite is roasted in air at about 600 C, which drives off the carbon dioxide and takes its cerium to Ce(IV). Hot
    # hydrochloric acid leaches the trivalent lanthanum, praseodymium and neodymium and leaves the ceria behind, which
    # Molycorp sold as cerium concentrate and which calcines clean to ceria. Cerium is half of what bastnäsite carries, so a
    # roasted concentrate gives half the liquor it would whole.
    for kind, time in (("campfire_cooking", 400), ("smoking", 200)):
        write(RECIPES / f"roasting/roasted_bastnasite_{kind}.json", {"type": f"minecraft:{kind}", "category": "misc", "ingredient": item("bastnasite_concentrate"),
                                                                    "result": {"id": "fundamentals:roasted_bastnasite"}, "experience": 0.1, "cookingtime": time})
    mixing("rare_earth_liquor_from_bastnasite", [item("roasted_bastnasite"), fluid(STRIP, 250)],
           [result_fluid("crude_rare_earth_liquor", 250), result_item("cerium_concentrate")], heated=True)
    write(RECIPES / "calcining/cerium_oxide_from_concentrate.json", {
        "type": "minecraft:blasting", "category": "misc", "ingredient": item("cerium_concentrate"),
        "result": {"id": "fundamentals:cerium_oxide"}, "experience": 0.3, "cookingtime": 100})
    # Monazite, xenotime and the rest are phosphates and niobates hydrochloric acid barely touches. They are roasted in
    # concentrated sulfuric acid at 500 to 800 C, as at Baotou, which turns the rare earths to sulfates and frees the
    # phosphate as phosphoric acid; that hot, the thorium goes to its pyrophosphate, which water does not take up. (Baked
    # at 200 to 300 C instead, the thorium dissolves with the rare earths.) The cake is leached in cold water, since rare
    # earth sulfates dissolve worse hot, and the thorium stays behind as a residue, as does the uranium and thorium of
    # xenotime and euxenite. A sulfate does not turn to a chloride in hydrochloric acid, so soda ash drops the rare earths
    # out of the leach as their carbonate, which hydrochloric acid dissolves, the carbon dioxide fizzing off. Lime then
    # drops the iron, aluminium and the rest as a sludge, and the clarified liquor is what a battery wants.
    for grade in ("light", "heavy"):
        mixing(f"{grade}_rare_earth_sulfate", [item(f"{grade}_rare_earth_concentrate"), fluid("tfmg:sulfuric_acid", 250)],
               [result_item(f"{grade}_rare_earth_sulfate"), result_fluid("phosphoric_acid", 125)], heated=True)
    residue = {"light": result_item("monazite_residue_dust"), "heavy": {"id": "fundamentals:monazite_residue_dust", "chance": 0.5}}
    for grade, crude, name in (("light", "crude_rare_earth_liquor", "rare_earth_liquor"), ("heavy", "crude_heavy_rare_earth_liquor", "heavy_rare_earth_liquor")):
        mixing(f"{grade}_rare_earth_carbonate", [item(f"{grade}_rare_earth_sulfate"), item("soda_ash"), fluid("minecraft:water", 500)],
               [result_item(f"{grade}_rare_earth_carbonate"), residue[grade]])
        mixing(name, [item(f"{grade}_rare_earth_carbonate"), fluid(STRIP, 500)], [result_fluid(crude, 500)])
    for crude, clean in (("crude_rare_earth_liquor", "rare_earth_liquor"), ("crude_heavy_rare_earth_liquor", "heavy_rare_earth_liquor")):
        mixing(f"clarify_{clean}", item("tfmg:limesand", 2) + [fluid(crude, 1000)], [result_fluid(clean, 1000), result_item("clarifier_sludge")])
    # a fouled organic scrubbed clean with lime, a tenth lost with the crud
    for organic in ORGANICS:
        mixing(f"scrub_{organic}", [item("tfmg:limesand"), fluid(f"fouled_{organic}", 1000)], [result_fluid(organic, 900)])
    # Waste: lime neutralises the spent chloride liquor to calcium chloride liquor, which boils down to the dry salt.
    mixing("calcium_chloride_liquor", item("tfmg:limesand", 2) + [fluid("spent_liquor", 1000)], [result_fluid("calcium_chloride_liquor", 1000)])
    mixing("calcium_chloride", [fluid("calcium_chloride_liquor", 1000)], [result_item("calcium_chloride", 3)], heated=True)
    # The clay is not ground or roasted: its rare earths sit on the clay as ions and a salt solution lifts them off. Salt,
    # sodium chloride, was the first lixiviant, in heaps, from the 1970s; ammonium sulfate replaced it in the 1980s, now
    # pumped into the hillside in place. Salt is what the plant has. What is left is the clay, kaolinite, as before.
    # Thortveitite is a silicate no acid opens. Chlorinated with coke at about 900 C, its scandium goes over as the chloride and
    # the silica as silicon tetrachloride, which boils at 58 C and passes on; the chloride is taken up in water.
    mixing("scandium_liquor", item("thortveitite_dust", 2) + [item("tfmg:coal_coke"), fluid("chlorine", 500), fluid("minecraft:water", 500)],
           [result_fluid("scandium_liquor", 500)], heated=True)
    mixing("heavy_rare_earth_liquor_from_clay", item("raw_ion_adsorption_clay", 4) + [item("salt"), fluid("minecraft:water", 500)],
           [result_fluid("crude_heavy_rare_earth_liquor", 250), {"id": "minecraft:clay_ball", "count": 4}])
    # Seawater is a lixiviant too, at about half the strength of the 6 to 8 per cent salt the heaps were leached with (and its
    # magnesium exchanges as well as sodium does): twice as much of it lifts the same rare earths.
    mixing("heavy_rare_earth_liquor_from_clay_with_seawater", item("raw_ion_adsorption_clay", 4) + [fluid("seawater", 1000)],
           [result_fluid("crude_heavy_rare_earth_liquor", 250), {"id": "minecraft:clay_ball", "count": 4}])
    # A zinc nugget is far more zinc than a batch's europium wants; the sulfuric acid is the sulfate that drops it.
    feed, salt, liquor, rest = ZINC_REDUCTION
    europium = light_fraction(feed, liquor, rest)
    mixing(salt, [fluid(feed, 1000), {"tag": "c:nuggets/zinc"}, fluid("tfmg:sulfuric_acid", 100)],
           [result_fluid(rest, round(1000 * (1 - europium))), {"id": f"fundamentals:{salt}", "chance": round(1000 * europium / 250, 2)}])
    mixing(liquor, [item(salt), fluid("nitric_acid", 250)], [result_fluid(liquor, 250)], heated=True)
    # Bromine from bittern, as it was first made from the Stassfurt potash bitterns: chlorine oxidises the bromide,
    # Cl2 + 2 Br- -> Br2 + 2 Cl-, one bromine for each chlorine, and steam blown through the hot liquor carries the bromine
    # out. What is left is bittern still, its magnesium chloride boiling down as it would have: two from 1,000 mB.
    vat("bromine", [fluid("bittern", 1000), fluid("chlorine", 100)], [result_fluid("bromine", 100), result_item("magnesium_chloride", 2)],
        ["tfmg:mixing"], folder="salt")


def vat(name, ingredients, results, machines, heated=True, time=100, folder="reduction"):
    """heated: False, True (a blaze burner) or "superheated" (a blaze burner fed a blaze cake)."""
    recipe = {"type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"],
              "ingredients": ingredients, "machines": machines, "min_size": 1, "processing_time": time, "results": results}
    if heated:
        recipe["heat_requirement"] = "superheated" if heated == "superheated" else "heated"
    write(RECIPES / f"{folder}/{name}.json", recipe)


def metals():
    # Hydrofluoric acid from fluorspar and sulfuric acid; argon spun out of air as TFMG spins out neon;
    # calcium by electrolysing the plant's own calcium chloride, molten at about 800 C.
    mixing("hydrofluoric_acid", item("raw_fluorite", 2) + [fluid("tfmg:sulfuric_acid", 500)], [result_fluid("hydrofluoric_acid", 500)], heated=True)
    vat("argon", [fluid("tfmg:air", 1000)], [{"id": "fundamentals:argon", "amount": 9}], ["tfmg:centrifuge"], heated=False, time=10)
    vat("calcium_ingot", item("calcium_chloride", 2), [result_item("calcium_ingot", 2), result_fluid("chlorine", 500)], ["tfmg:electrode", "tfmg:electrode"])
    for element in ELECTROLYSIS + CALCIOTHERMIC:
        mixing(f"{element}_fluoride", [item(f"{element}_oxide"), fluid("hydrofluoric_acid", 500)], [result_item(f"{element}_fluoride")])
    # the fluoride is the bath the oxide dissolves in, not the feed: it comes back but for what the tapping loses
    for element in ELECTROLYSIS:
        vat(f"{element}_ingot", [item(f"{element}_fluoride")] + item(f"{element}_oxide", 2),
            [result_item(f"{element}_ingot", 2), {"id": f"fundamentals:{element}_fluoride", "chance": 0.9}],
            ["tfmg:electrode", "tfmg:electrode"], heated="superheated")
    # Electrolysis runs in the fluoride melt at 1,000 to 1,100 C, past a kindled burner's 1,000; the two
    # metallothermic reductions run near 1,500 C, past calcium fluoride's 1,418 melt. Both want the burner fed a
    # blaze cake, 1,600. TFMG vats take four item inputs at most.
    for element in CALCIOTHERMIC:
        vat(f"{element}_ingot", item(f"{element}_fluoride", 2) + item("calcium_ingot", 2) + [fluid("argon", 250)],
            [result_item(f"{element}_ingot", 2), result_item("raw_fluorite", 2)], ["tfmg:mixing"], heated="superheated")
    for element in LANTHANOTHERMIC:
        vat(f"{element}_ingot", item(f"{element}_oxide", 2) + item("lanthanum_ingot", 2) + [fluid("argon", 250)],
            [result_item(f"{element}_ingot", 2), result_item("lanthanum_oxide", 2)], ["tfmg:mixing"], heated="superheated")
    write(DATA.parent / "c/tags/item/ingots/calcium.json", {"replace": False, "values": ["fundamentals:calcium_ingot"]})


def solvents():
    """P204 and P507 from propylene and bone, as they are made: both are 2-ethylhexyl esters on one phosphorus, so they share
    the alcohol and the phosphorus trichloride and part only at the last step."""
    def chem(name, ingredients, results, machines=("tfmg:mixing",), heated=True):
        vat(name, ingredients, results, list(machines), heated=heated, folder="solvents")

    # Water gas: steam over white-hot coke gives carbon monoxide and hydrogen, half and half. Shifted with more steam, the
    # monoxide takes the water's oxygen and leaves its hydrogen: how hydrogen was made before natural gas.
    chem("water_gas", [item("tfmg:coal_coke"), fluid("minecraft:water", 500)], [result_fluid("water_gas", 1000)])
    chem("hydrogen", [fluid("water_gas", 1000), fluid("minecraft:water", 500)], [{"id": "tfmg:hydrogen", "amount": 1000}, {"id": "tfmg:carbon_dioxide", "amount": 500}])
    # The oxo process on a cobalt catalyst, which comes back but for what is lost: propylene with the water gas's monoxide
    # and hydrogen gives butyraldehyde, two of which condense to the C8 aldehyde, and more hydrogen saturates that to
    # 2-ethylhexanol.
    chem("ethylhexanol", [item("cobalt_ingot"), fluid("tfmg:propylene", 500), fluid("water_gas", 500), fluid("tfmg:hydrogen", 500)],
         [result_fluid("ethylhexanol", 250), {"id": "fundamentals:cobalt_ingot", "chance": 0.95}])
    # White phosphorus from the electric furnace: phosphate (bone, as for the acid), coke and silica at 1,500 C on electrodes,
    # the phosphorus distilling off and the lime running out as slag.
    chem("white_phosphorus", item("minecraft:bone_meal", 2) + [item("tfmg:coal_coke"), {"tag": "c:sands/colorless"}],
         [result_item("white_phosphorus"), {"id": "tfmg:slag"}], machines=("tfmg:electrode", "tfmg:electrode"), heated="superheated")
    # Chlorine as Scheele found it, hydrochloric acid on pyrolusite, the manganese staying behind as its chloride. The
    # molten-chloride electrolyses give it off too.
    mixing("chlorine", [item("raw_pyrolusite"), fluid(STRIP, 1000)], [result_fluid("chlorine", 250)], heated=True)
    # Chlorine over melted phosphorus, P4 + 6 Cl2 -> 4 PCl3, which boils off at 76 C.
    mixing("phosphorus_trichloride", [item("white_phosphorus"), fluid("chlorine", 750)], [result_fluid("phosphorus_trichloride", 500)], heated=True)
    # P204, di(2-ethylhexyl) phosphoric acid: the trichloride oxidised by air to the oxychloride, two of the alcohol on it
    # and water for the last chlorine, cold. Every chlorine leaves as hydrogen chloride, taken up as hydrochloric acid.
    chem("d2ehpa", [fluid("ethylhexanol", 500), fluid("phosphorus_trichloride", 250), fluid("tfmg:air", 500), fluid("minecraft:water", 250)],
         [result_fluid("d2ehpa", 250), result_fluid(STRIP, 500)], heated=False)
    # P507, 2-ethylhexyl phosphonic acid mono-2-ethylhexyl ester: the alcohol on the trichloride gives the phosphite, which
    # heat rearranges (Arbuzov) to the phosphonate, carbon on phosphorus; water then takes one ester off.
    chem("ehehpa", [fluid("ethylhexanol", 500), fluid("phosphorus_trichloride", 250), fluid("minecraft:water", 250)],
         [result_fluid("ehehpa", 250), result_fluid(STRIP, 500)])


def cuts():
    for liquor, organic, stages, light, heavy in CUTS:
        write(RECIPES / f"separation/{liquor}.json", {
            "type": "fundamentals:separation", "liquor": f"fundamentals:{liquor}", "organic": f"fundamentals:{organic}",
            "strip": f"fundamentals:{STRIP}", "stages": stages,
            "light": f"fundamentals:{light}", "heavy": f"fundamentals:{heavy}", "light_fraction": light_fraction(liquor, light, heavy)})


def magnetic():
    """The magnetic route for the cuts that have one: the same products in the same proportion as the battery, so either
    feeds the next cut."""
    for liquor, _, _, light, heavy in CUTS:
        if liquor in MAGNETIC:
            drawn, passes = MAGNETIC[liquor]
            write(RECIPES / f"magnetic/{liquor}.json", {
                "type": "fundamentals:magnetic", "liquor": f"fundamentals:{liquor}", "light": f"fundamentals:{light}",
                "heavy": f"fundamentals:{heavy}", "light_fraction": light_fraction(liquor, light, heavy),
                "attracted": f"fundamentals:{drawn}", "passes": passes})


def oxalates():
    """A single-element liquor precipitates with oxalic acid, and the oxalate calcines to the oxide at 800 to
    1,000 °C, past what a plain furnace reaches: a blast furnace, or Create's fan over lava."""
    for element, forms in MATERIALS.items():
        if "oxalate" not in forms:
            continue
        liquor = OXALATE_FROM.get(element, f"{element}_liquor")
        assert liquor in LIQUORS, element
        mixing(f"{element}_oxalate", [item("oxalic_acid"), fluid(liquor, 250)], [result_item(f"{element}_oxalate")])
        write(RECIPES / f"calcining/{element}_oxide.json", {
            "type": "minecraft:blasting", "category": "misc", "ingredient": item(f"{element}_oxalate"),
            "result": {"id": f"fundamentals:{element}_oxide"}, "experience": 0.3, "cookingtime": 100})


def mixer_settler():
    """The casing: a cell of an open-topped welded tank, as a Chinese separation hall is built. A floor under
    the bottom layer, a full panel on every face not shared with the rest of its stage, no lid, a window in
    the middle casing of each outside wall, and a weir on the seam between the mixing trough (the back row)
    and the settling bay, full height on the bottom layer of a two-tall stage and a lip on the one above.
    Every size comes from VAT. Create's connected textures put the frame ribs on the exterior edges; the
    fluids inside are drawn by the renderer."""
    tex = {"side": "fundamentals:block/mixer_settler_side", "top": "fundamentals:block/mixer_settler_top",
           "window": "fundamentals:block/mixer_settler_window", "nozzle": "fundamentals:block/mixer_settler_nozzle",
           "particle": "fundamentals:block/mixer_settler_side"}
    full = [0, 0, 16, 16]

    def box(f, t, faces):
        return {"from": list(f), "to": list(t), "faces": {d: {"texture": tx, "uv": uv, **({"cullface": d} if cull else {})}
                                                           for d, (tx, uv, cull) in faces.items()}}

    def model(name, elements):
        write(ASSETS / f"models/block/mixer_settler/{name}.json",
              {"ambientocclusion": False, "render_type": "minecraft:cutout", "textures": tex, "elements": elements})
        return f"fundamentals:block/mixer_settler/{name}"

    W, F = VAT["wall"], VAT["floor"]
    WEIR_TOP = 16 - VAT["weir_below_rim"]
    WIN0, WIN1 = VAT["window"]
    SLAB = {"west": ((0, 0, 0), (W, 16, 16)), "east": ((16 - W, 0, 0), (16, 16, 16)),
            "north": ((0, 0, 0), (16, 16, W)), "south": ((0, 0, 16 - W), (16, 16, 16))}
    INNER = {"west": "east", "east": "west", "north": "south", "south": "north"}

    def panel(side):
        """A plain wall: one panel, our dark sheet outside, the lid colour inside, capped."""
        lo, hi = SLAB[side]
        along_x = side in ("north", "south")
        cap = ("#top", [0, 0, 16, W] if along_x else [0, 0, W, 16], False)
        return [box(lo, hi, {side: ("#side", full, True), INNER[side]: ("#top", full, False), "up": cap})]

    def window_wall(side, part):
        """Create's tank window: two posts and, between them, glass filling the wall's thickness so the rim
        stays whole from above. Create cuts its window strip so the bolts sit only at the window's outer ends;
        `part` is single, bottom or top of a two-tall window."""
        lo, hi = SLAB[side]
        along_x = side in ("north", "south")
        axis = 0 if along_x else 2
        cap = ("#top", [0, 0, 16, W] if along_x else [0, 0, W, 16], False)
        out = []
        # each post also closes its end toward the glass, or the fluid shows through it from an angle
        ends = ("east", "west") if along_x else ("south", "north")
        for (p0, p1, uv), end in zip(((0, WIN0, [0, 0, WIN0, 16]), (WIN1, 16, [WIN1, 0, 16, 16])), ends):
            f, t = list(lo), list(hi)
            f[axis], t[axis] = p0, p1
            out.append(box(f, t, {side: ("#side", uv, True), INNER[side]: ("#top", uv, False), "up": cap, end: ("#top", [0, 0, W, 16], False)}))
        uv = {"single": [0, 0, 8, 16], "bottom": [0, 2, 8, 16], "top": [0, 0, 8, 14]}[part]
        f, t = list(lo), list(hi)
        f[axis], t[axis] = WIN0, WIN1
        faces = {side: ("#window", uv, True), INNER[side]: ("#window", uv, False)}
        if part != "bottom":
            faces["up"] = cap
        out.append(box(f, t, faces))
        return out

    def weir_model(name, y0, y1):
        """The weir on the trough's front seam, from y0 to y1 of the casing, both faces the lid colour."""
        return model(name, [box((0, y0, 0), (16, y1, W), {"north": ("#top", [0, 16 - y1, 16, 16 - y0], False),
                                                          "south": ("#top", [0, 16 - y1, 16, 16 - y0], False),
                                                          "up": ("#top", [0, 0, 16, W], False)})])

    sides = (("west", "left"), ("east", "right"), ("north", "front"), ("south", "back"))
    walls = {prop: model(f"wall_{prop}", panel(side)) for side, prop in sides}
    windows = {(prop, part): model(f"wall_{prop}_{part}", window_wall(side, part)) for side, prop in sides for part in ("single", "bottom", "top")}
    floor = model("floor", [box((0, 0, 0), (16, F, 16), {"down": ("#top", full, True), "up": ("#top", full, False)})])
    weir = weir_model("weir", F, 16)                 # bottom layer of a two-tall stage: runs on into the lip above
    weir_low = weir_model("weir_low", F, WEIR_TOP)   # the only layer of a one-tall stage
    weir_lip = weir_model("weir_lip", 0, WEIR_TOP)   # top layer of a two-tall stage

    # the ports in the shared end walls: the organic overflow high on the front wall, the aqueous drain low on the back
    port_ahead = model("port_ahead", [box((4, 10, 1), (12, 14, 2), {"south": ("#nozzle", [0, 0, 16, 16], False), "up": ("#nozzle", [0, 6, 16, 10], False),
                                                                   "east": ("#nozzle", [6, 4, 10, 12], False), "west": ("#nozzle", [6, 4, 10, 12], False)})])
    port_behind = model("port_behind", [box((4, 2, 14), (12, 6, 15), {"north": ("#nozzle", [0, 0, 16, 16], False), "up": ("#nozzle", [0, 6, 16, 10], False),
                                                                     "east": ("#nozzle", [6, 4, 10, 12], False), "west": ("#nozzle", [6, 4, 10, 12], False)})])
    parts = []
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        rot = {"y": y} if y else {}
        # a wall shared with the next stage carries a port, not a window
        linked = {"front": "link_ahead", "back": "link_behind"}
        for prop, mdl in walls.items():
            plain = {"facing": facing, prop: "false", "window": "false"}
            glazed = {"facing": facing, prop: "false", "window": "true"}
            if prop in linked:
                parts.append({"when": {"facing": facing, prop: "false", "window": "true", linked[prop]: "true"}, "apply": {"model": mdl, **rot}})
                glazed[linked[prop]] = "false"
            parts.append({"when": plain, "apply": {"model": mdl, **rot}})
            parts.append({"when": {**glazed, "above": "false", "below": "false"}, "apply": {"model": windows[(prop, "single")], **rot}})
            parts.append({"when": {**glazed, "above": "true", "below": "false"}, "apply": {"model": windows[(prop, "bottom")], **rot}})
            parts.append({"when": {**glazed, "above": "false", "below": "true"}, "apply": {"model": windows[(prop, "top")], **rot}})
        parts.append({"when": {"facing": facing, "front": "false", "link_ahead": "true", "above": "false"}, "apply": {"model": port_ahead, **rot}})
        parts.append({"when": {"facing": facing, "back": "false", "link_behind": "true", "below": "false"}, "apply": {"model": port_behind, **rot}})
        parts.append({"when": {"facing": facing, "rows": "well", "below": "false", "above": "true"}, "apply": {"model": weir, **rot}})
        parts.append({"when": {"facing": facing, "rows": "well", "below": "false", "above": "false"}, "apply": {"model": weir_low, **rot}})
        parts.append({"when": {"facing": facing, "rows": "well", "below": "true"}, "apply": {"model": weir_lip, **rot}})
    parts.append({"when": {"below": "false"}, "apply": {"model": floor}})
    write(ASSETS / "blockstates/mixer_settler.json", {"multipart": parts})
    write(ASSETS / "models/item/mixer_settler.json", {"ambientocclusion": False, "textures": tex, "elements":
          panel("west") + panel("east") + panel("north") + panel("south")
          + [box((0, 0, 0), (16, F, 16), {"down": ("#top", full, False), "up": ("#top", full, False)}),
             box((0, F, 10), (16, WEIR_TOP, 10 + W), {"north": ("#top", [0, 16 - WEIR_TOP, 16, 16 - F], False), "south": ("#top", [0, 16 - WEIR_TOP, 16, 16 - F], False), "up": ("#top", [0, 10, 16, 10 + W], False)})],
          "display": {"gui": {"rotation": [30, 225, 0], "scale": [0.625, 0.625, 0.625]}, "ground": {"scale": [0.25, 0.25, 0.25]},
                      "fixed": {"scale": [0.5, 0.5, 0.5]}, "thirdperson_righthand": {"rotation": [75, 45, 0], "scale": [0.375, 0.375, 0.375], "translation": [0, 2.5, 0]},
                      "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]}}})
    write(ROOT / "fundamentals_vat.json", VAT)
    write(DATA / "loot_table/blocks/mixer_settler.json", {"type": "minecraft:block", "pools": [drop_self("mixer_settler")]})
    write(RECIPES / "mixer_settler.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P P", "PPP", "PFP"],
        "key": {"P": ACID_PROOF, "F": {"item": "create:fluid_pipe"}},
        "result": {"id": "fundamentals:mixer_settler", "count": 6}})
    for name in PLANT_ITEMS:
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"fundamentals:item/{name}"}})


def magnetomigration_cell():
    """A plastic channel with an NdFeB block set in its right wall. The liquor runs along the channel, cell to cell, and the
    paramagnetic ions drift into the stream along the magnet; the last cell splits the stream, magnet side out of its right
    face, the rest out of its left. Plastic because the chloride liquor eats copper, and no power: the magnets are permanent."""
    faces = {"north": "end", "south": "end", "east": "magnet", "west": "side", "up": "top", "down": "side"}
    tex = {face: f"fundamentals:block/magnetomigration_cell_{sheet}" for face, sheet in faces.items()}
    write(ASSETS / "models/block/magnetomigration_cell.json",
          {"parent": "minecraft:block/cube", "render_type": "minecraft:translucent",
           "textures": {**tex, "particle": "fundamentals:block/magnetomigration_cell_side"}})
    write(ASSETS / "blockstates/magnetomigration_cell.json", {"variants": {
        f"facing={facing}": {"model": "fundamentals:block/magnetomigration_cell", **({"y": y} if y else {})}
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write(ASSETS / "models/item/magnetomigration_cell.json", {"parent": "fundamentals:block/magnetomigration_cell"})
    write(DATA / "loot_table/blocks/magnetomigration_cell.json", {"type": "minecraft:block", "pools": [drop_self("magnetomigration_cell")]})
    write(RECIPES / "magnetomigration_cell.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PPP", "PTM", "PPP"],
        "key": {"P": ACID_PROOF, "T": [{"item": "fundamentals:plastic_fluid_tank"}, {"tag": "c:plates/stainless_steel"}], "M": {"tag": "fundamentals:magnets"}},
        "result": {"id": "fundamentals:magnetomigration_cell", "count": 1}})


def plastic_tank():
    """Create's fluid tank in plastic: its own blockstate on Create's tank models, which take our sheets in place of the copper
    ones, and Create's recipe with plastic sheets for the copper. The acids and liquors are kept in fibreglass and polyethylene
    tanks for the reason they run in plastic pipe. The models draw it translucent, natural plastic; a dyed tank's colour is a
    blockstate property the models ignore, and client.PlasticTankModel tints it and draws it opaque."""
    sheets = {"0": "top", "1": "", "3": "window", "4": "inner", "5": "window_single", "particle": ""}
    tex = {k: "fundamentals:block/plastic_fluid_tank" + (f"_{v}" if v else "") for k, v in sheets.items()}
    variants = {}
    for top in (False, True):
        for bottom in (False, True):
            part = "single" if top and bottom else "top" if top else "bottom" if bottom else "middle"
            for shape in ("plain", "window", "window_ne", "window_nw", "window_se", "window_sw"):
                name = f"block_{part}" + ("" if shape == "plain" else f"_{shape}")
                write(ASSETS / f"models/block/plastic_fluid_tank/{name}.json",
                      {"parent": f"create:block/fluid_tank/{name}", "render_type": "minecraft:translucent", "textures": tex})
                variants[f"bottom={str(bottom).lower()},shape={shape},top={str(top).lower()}"] = {"model": f"fundamentals:block/plastic_fluid_tank/{name}"}
    write(ASSETS / "blockstates/plastic_fluid_tank.json", {"variants": variants})
    write(ASSETS / "models/item/plastic_fluid_tank.json", {"parent": "fundamentals:block/plastic_fluid_tank/block_single_window"})
    write(DATA / "loot_table/blocks/plastic_fluid_tank.json", {"type": "minecraft:block", "pools": [drop_self("plastic_fluid_tank")]})
    write(RECIPES / "plastic_fluid_tank.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P", "B", "P"],
        "key": {"P": {"tag": "fundamentals:plastic_sheets"}, "B": {"tag": "c:barrels/wooden"}},
        "result": {"id": "fundamentals:plastic_fluid_tank", "count": 1}})


def template():
    """A 27x5x5 gametest floor, patched from the 3x3x3 empty one: room for eight stages end to end with mixers over them."""
    empty = gzip.decompress((DATA / "structure/empty.nbt").read_bytes())
    i = empty.index(b"size") + len(b"size") + 1 + 4
    patched = empty[:i] + (27).to_bytes(4, "big") + (5).to_bytes(4, "big") + (5).to_bytes(4, "big") + empty[i + 12:]
    (DATA / "structure/battery.nbt").write_bytes(gzip.compress(patched, mtime=0))


def names():
    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    for id, (name, _, _) in FLUIDS.items():
        lang[f"fluid_type.fundamentals.{id}"] = name
    lang["block.fundamentals.mixer_settler"] = "Mixer-Settler Casing"
    lang["block.fundamentals.plastic_fluid_tank"] = "Plastic Fluid Tank"
    for acid in DISSOLVES:
        lang[f"block.fundamentals.{acid}"] = FLUIDS[acid][0]
        lang[f"item.fundamentals.{acid}_bucket"] = f"{FLUIDS[acid][0]} Bucket"
    lang["item.fundamentals.seawater_bucket"] = "Seawater Bucket"
    lang["goggles.fundamentals.boiler.fresh_water"] = "No water coming in: a boiler takes fresh water, never seawater"
    for name, display in PLANT_ITEMS.items():
        lang[f"item.fundamentals.{name}"] = display
    lang["goggles.fundamentals.mixer_settler.stages"] = "Battery of %s stages, %s mB a batch"
    lang["goggles.fundamentals.mixer_settler.stage"] = "Stage %s across, %s along, %s tall"
    lang["goggles.fundamentals.mixer_settler.idle"] = "Nothing in the feed"
    lang["goggles.fundamentals.mixer_settler.no_cut"] = "%s does not part"
    lang["goggles.fundamentals.mixer_settler.short"] = "%s parts in %s stages; this battery has %s"
    lang["goggles.fundamentals.mixer_settler.organic"] = "Every stage wants %s on top"
    lang["goggles.fundamentals.mixer_settler.mixer"] = "Every trough wants a Mechanical Mixer turning over it"
    lang["goggles.fundamentals.mixer_settler.lever"] = "Waiting for the lever"
    lang["goggles.fundamentals.mixer_settler.casing"] = "Casing: a stage is three across and three along"
    lang["goggles.fundamentals.mixer_settler.settling"] = "Coming to equilibrium: %s s"
    lang["goggles.fundamentals.mixer_settler.strip"] = "The far end wants %s"
    lang["goggles.fundamentals.mixer_settler.ready"] = "Parting %s into %s mB %s and %s mB %s a batch"
    lang["goggles.fundamentals.mixer_settler.progress"] = "Organic on %s of %s stages, liquor in %s"
    lang["goggles.fundamentals.mixer_settler.ends"] = "Feed %s, strip %s"
    lang["goggles.fundamentals.mixer_settler.products"] = "Out %s at the head, %s at the tail"
    lang["goggles.fundamentals.mixer_settler.sump"] = "Sump %s"
    lang["goggles.fundamentals.mixer_settler.waste"] = "The sump is full: pump the spent liquor out from below"
    lang["goggles.fundamentals.mixer_settler.crud"] = "Crud at the interface: a dirty feed has fouled the organic; drain it and scrub it with lime"
    lang["goggles.fundamentals.mixer_settler.emulsion"] = "The mixer is too fast: the phases emulsify and will not settle"
    lang["goggles.fundamentals.mixer_settler.nothing"] = "nothing"
    lang["block.fundamentals.magnetomigration_cell"] = "Magnetomigration Cell"
    cell = "goggles.fundamentals.magnetomigration_cell"
    lang[f"{cell}.line"] = "Line of %s cells, %s mB a batch"
    lang[f"{cell}.idle"] = "Nothing in the feed"
    lang[f"{cell}.no_cut"] = "%s has no magnetic cut: its products do not sort by moment"
    lang[f"{cell}.short"] = "%s parts in %s passes; this line has %s cells"
    lang[f"{cell}.full"] = "The outlets are full: pipe the products away from the last cell's sides"
    lang[f"{cell}.ready"] = "Parting %s: %s mB %s to the magnets, %s mB %s away a batch"
    lang[f"{cell}.passes"] = "%s passes wanted, %s cells in line"
    lang[f"{cell}.feed"] = "Feed %s"
    lang[f"{cell}.outlets"] = "Magnet side %s, far side %s"
    write(path, lang)


def main():
    for folder in ("mixing", "separation", "magnetic", "calcining", "reduction", "solvents"):
        shutil.rmtree(RECIPES / folder, ignore_errors=True)
    shutil.rmtree(ASSETS / "models/block/mixer_settler", ignore_errors=True)
    shutil.rmtree(ASSETS / "models/block/plastic_fluid_tank", ignore_errors=True)
    for stale in (ASSETS / "models/block").glob("mixer_settler*.json"):
        stale.unlink()
    java_table()
    chemistry()
    cuts()
    magnetic()
    oxalates()
    acids()
    metals()
    solvents()
    mixer_settler()
    magnetomigration_cell()
    plastic_tank()
    template()
    names()
    print(f"{len(FLUIDS)} fluids, {len(CUTS)} cuts, {len(MAGNETIC)} with a magnetic route")


if __name__ == "__main__":
    main()
