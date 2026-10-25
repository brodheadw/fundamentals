#!/usr/bin/env python3
"""The fab and CC: Tweaked's computers rebuilt from it: recipes, models, blockstate, loot and lang."""
import shutil

from common import ASSETS, DATA, read_lang, write, write_lang

MOD = "fundamentals"
CC_LOADED = [{"type": "neoforge:mod_loaded", "modid": "computercraft"}]
CHEMICA_LOADED = [{"type": "neoforge:mod_loaded", "modid": "chemica"}]


def ing(x):
    if isinstance(x, list):
        return [ing(i) for i in x]
    return {"tag": x[1:]} if x.startswith("#") else {"item": x}


def fluid(fid, amount):
    return {"type": "neoforge:single", "amount": amount, "fluid": fid}


def out(x, count=1, chance=None):
    r = {"id": x}
    if count != 1:
        r["count"] = count
    if chance is not None:
        r["chance"] = chance
    return r


RECIPES = {}


def recipe(name, obj):
    RECIPES[name] = obj


def m(x):
    return f"{MOD}:{x}"


# --- materials -------------------------------------------------------------
# Fused silica: quartz melted past 1,700 C, the only glass that passes deep UV and holds still under a projection lens.
recipe("compute/fused_quartz", {"type": "create:mixing", "heat_requirement": "superheated",
                            "ingredients": [ing("minecraft:quartz_block")], "results": [out(m("fused_quartz"), 2)]})
# Novolac: phenol condensed under an acid catalyst; the resin every positive photoresist is built on.
recipe("compute/novolac_resin", {"neoforge:conditions": CHEMICA_LOADED, "type": "create:mixing", "heat_requirement": "heated",
                             "ingredients": [fluid("chemica:phenol", 250), fluid("tfmg:sulfuric_acid", 25)],
                             "results": [out(m("novolac_resin"), 2)]})
# Positive resist: novolac with a light-sensitive inhibitor, thinned in solvent. Light makes it soluble in alkali.
recipe("compute/positive_photoresist", {"type": "create:mixing",
                                    "ingredients": [ing(m("novolac_resin")), ing("#c:dusts/redstone"), fluid("tfmg:naphtha", 250)],
                                    "results": [{"id": m("positive_photoresist"), "amount": 250}]})
# Negative resist (the 1960s KTFR kind): cyclised rubber in solvent. Light cross-links it; the rest washes off in solvent.
recipe("compute/negative_photoresist", {"type": "create:mixing",
                                    "ingredients": [ing("tfmg:rubber_sheet"), fluid("tfmg:naphtha", 250)],
                                    "results": [{"id": m("negative_photoresist"), "amount": 250}]})
# Ceric ammonium nitrate: the etch that eats a mask's chromium and leaves its glass alone.
recipe("compute/chrome_etchant", {"type": "create:mixing",
                              "ingredients": [ing("fundamentals:cerium_oxide"), fluid("fundamentals:nitric_acid", 250), fluid("fundamentals:ammonia", 100)],
                              "results": [{"id": m("chrome_etchant"), "amount": 250}]})
# Epitaxy: a fresh crystal layer grown from trichlorosilane in hydrogen on a doped wafer, clean enough for CMOS.
recipe("compute/epitaxial_wafer", {"type": "create:sequenced_assembly", "ingredient": ing("tfmg:p_semiconductor"), "loops": 1,
                               "results": [out(m("epitaxial_wafer"))], "transitional_item": out(m("processing_wafer")),
                               "sequence": [
                                   {"type": "create:filling", "ingredients": [ing(m("processing_wafer")), fluid("fundamentals:trichlorosilane", 100)], "results": [out(m("processing_wafer"))]},
                                   {"type": "create:filling", "ingredients": [ing(m("processing_wafer")), fluid("tfmg:hydrogen", 250)], "results": [out(m("processing_wafer"))]},
                               ]})

# --- masks -----------------------------------------------------------------
recipe("compute/chrome_mask_blank", {"type": "create:deploying", "ingredients": [ing("#c:glass_panes"), ing("fundamentals:chromium_ingot")],
                                 "results": [out(m("chrome_mask_blank"))]})


def step(kind, extra, keep=False):
    s = {"type": kind, "ingredients": [ing(m("processing_wafer"))] + extra, "results": [out(m("processing_wafer"))]}
    if keep:
        s["keep_held_item"] = True
    return s


def mstep(kind, extra, keep=False):
    s = {"type": kind, "ingredients": [ing(m("processing_mask"))] + extra, "results": [out(m("processing_mask"))]}
    if keep:
        s["keep_held_item"] = True
    return s


# A reticle's quartz is lapped flat before it is chromed, with garnet or emery, as optical shops lap glass.
recipe("compute/quartz_mask_blank", {"type": "create:sequenced_assembly", "ingredient": ing(m("fused_quartz")), "loops": 1,
                                 "results": [out(m("quartz_mask_blank"))], "transitional_item": out(m("processing_mask")),
                                 "sequence": [mstep("create:deploying", [ing("#fundamentals:lapping_abrasives")], True),
                                              mstep("create:deploying", [ing("fundamentals:chromium_ingot")])]})


# The pattern goes in first: a hand-cut film drawing photo-reduced for the 10 um mask; an optical pattern generator run
# by a microcontroller for the memory reticle, and one with the bigger design held in memory for the microprocessor's;
# a computer-driven laser writer for 90 nm. (Create tells sequences apart by their first step.)
MASKS = {
    "microcontroller": ("chrome_mask_blank", "negative_photoresist", [ing("minecraft:paper")], False),
    "memory_chip": ("quartz_mask_blank", "positive_photoresist", [ing(m("microcontroller"))], False),
    "microprocessor": ("quartz_mask_blank", "positive_photoresist", [ing(m("memory_chip"))], False),
    "advanced_processor": ("quartz_mask_blank", "positive_photoresist", [ing("computercraft:computer_normal")], True),
}
for chip, (blank, resist, writer, keep) in MASKS.items():
    design = chip.replace("_chip", "")
    recipe(f"compute/{design}_mask", {**({"neoforge:conditions": CC_LOADED} if keep else {}), "type": "create:sequenced_assembly", "ingredient": ing(m(blank)), "loops": 1,
                                  "results": [out(m(design + "_mask"))], "transitional_item": out(m("processing_mask")),
                                  "sequence": [
                                      mstep("create:deploying", writer, keep),
                                      mstep("create:filling", [fluid(m(resist), 25)]),
                                      mstep("create:filling", [fluid("fundamentals:caustic_soda" if resist.startswith("positive") else "tfmg:naphtha", 25)]),
                                      mstep("create:filling", [fluid(m("chrome_etchant"), 25)]),
                                  ]})

# --- wafers ----------------------------------------------------------------
# One loop is one mask layer: expose through the mask, develop, etch the oxide in HF, dope or metallise what the etch
# opened, then grow fresh oxide in steam and coat resist for the next layer. Finer processes need more layers. The mask
# comes first so Create can tell the designs apart.
OXIDE = step("create:filling", [fluid("minecraft:water", 25)])
HF = step("create:filling", [fluid("fundamentals:hydrofluoric_acid", 25)])


def layer(chip, resist, developer, finish):
    design = chip.replace("_chip", "")
    return [step("create:deploying", [ing(m(design + "_mask"))], keep=True),
            step("create:filling", [fluid(developer, 25)]), HF] + finish + [OXIDE, step("create:filling", [fluid(m(resist), 25)])]


WAFERS = {
    # PMOS on an n-type wafer, 10 um (1971): boron diffused into the openings.
    "microcontroller": ("tfmg:n_semiconductor", 3, layer("microcontroller", "negative_photoresist", "tfmg:naphtha",
                                                          [step("create:deploying", [ing("fundamentals:boric_acid")])])),
    # NMOS DRAM on a p-type wafer, 1 um: phosphorus from its chloride.
    "memory_chip": ("tfmg:p_semiconductor", 5, layer("memory_chip", "positive_photoresist", "fundamentals:caustic_soda",
                                                      [step("create:filling", [fluid("fundamentals:phosphorus_trichloride", 25)])])),
    # CMOS on epitaxial silicon, 1 um (1985): both dopants and an aluminium interconnect each layer.
    "microprocessor": (m("epitaxial_wafer"), 6, layer("microprocessor", "positive_photoresist", "fundamentals:caustic_soda",
                                                     [step("create:filling", [fluid("fundamentals:phosphorus_trichloride", 25)]),
                                                      step("create:deploying", [ing("#c:plates/aluminum")])])),
    # 90 nm (2004): copper damascene interconnect, polished flat after every layer.
    "advanced_processor": (m("epitaxial_wafer"), 8, layer("advanced_processor", "positive_photoresist", "fundamentals:caustic_soda",
                                                         [step("create:filling", [fluid("fundamentals:phosphorus_trichloride", 25)]),
                                                          step("create:deploying", [ing("#c:plates/copper")]),
                                                          step("create:pressing", [])])),
}
# Dies per wafer, then the extra the yield sometimes gives.
DICING = {"microcontroller": (6, 2), "memory_chip": (4, 2), "microprocessor": (3, 1), "advanced_processor": (1, 1)}
for chip, (source, loops, sequence) in WAFERS.items():
    design = chip.replace("_chip", "")
    recipe(f"compute/{design}_wafer", {"type": "create:sequenced_assembly", "ingredient": ing(source), "loops": loops,
                                   "results": [out(m(design + "_wafer"))], "transitional_item": out(m("processing_wafer")),
                                   "sequence": sequence})
    dies, extra = DICING[chip]
    recipe(f"compute/{design}_die", {"type": "create:cutting", "ingredients": [ing(m(design + "_wafer"))], "processing_time": 200,
                                 "results": [out(m(design + "_die"), dies), out(m(design + "_die"), extra, 0.5)]})
    # Packaging: die bonded to a copper lead frame, gold wires to its pins, sealed in plastic.
    recipe(f"compute/{chip}", {"type": "create:sequenced_assembly", "ingredient": ing(m(design + "_die")), "loops": 1,
                           "results": [out(m(chip))], "transitional_item": out(m("packaging_chip")),
                           "sequence": [
                               {"type": "create:deploying", "ingredients": [ing(m("packaging_chip")), ing("#c:wires/copper")], "results": [out(m("packaging_chip"))]},
                               {"type": "create:deploying", "ingredients": [ing(m("packaging_chip")), ing("#c:nuggets/gold")], "results": [out(m("packaging_chip"))]},
                               {"type": "create:filling", "ingredients": [ing(m("packaging_chip")), fluid("tfmg:molten_plastic", 50)], "results": [out(m("packaging_chip"))]},
                           ]})

# Displays: liquid crystal between two panes with a transparent indium tin oxide electrode; colour adds an LED backlight.
recipe("compute/lcd_panel", {"type": "create:deploying", "ingredients": [ing("#c:glass_panes"), ing("fundamentals:indium_tin_oxide")], "results": [out(m("lcd_panel"))]})
recipe("compute/colour_lcd_panel", {"type": "create:deploying", "ingredients": [ing(m("lcd_panel")), ing("fundamentals:led_chip")], "results": [out(m("colour_lcd_panel"))]})


def shaped(result, pattern, key, count=1, kind="minecraft:crafting_shaped"):
    r = {"type": kind, "category": "redstone", "pattern": pattern, "key": {k: ing(v) for k, v in key.items()},
         "result": {"count": count, "id": result}}
    return r


recipe("compute/power_supply", shaped(m("power_supply"), ["PWP", "CTD", "PWP"],
                              {"P": "#c:plates/iron", "W": "#c:wires/copper", "C": "tfmg:capacitor_item", "T": "tfmg:electromagnetic_coil", "D": "tfmg:electric_diode"}))

# --- CC: Tweaked, rebuilt from real parts ---------------------------------
CC = {}
CC["computer_normal"] = shaped("computercraft:computer_normal", ["PPP", "CBM", "PLP"],
                               {"P": "#c:plates/iron", "C": m("microprocessor"), "B": "tfmg:circuit_board", "M": m("memory_chip"), "L": m("lcd_panel")})
CC["computer_advanced"] = shaped("computercraft:computer_advanced", ["PPP", "CBM", "PLP"],
                                 {"P": "#c:plates/aluminum", "C": m("advanced_processor"), "B": "tfmg:circuit_board", "M": m("memory_chip"), "L": m("colour_lcd_panel")})
CC["computer_advanced_upgrade"] = {"type": "computercraft:transform_shaped", "category": "redstone",
                                   "function": [{"type": "computercraft:copy_components", "from": {"item": "computercraft:computer_normal"}}],
                                   "pattern": ["PAP", "MCM", "PLP"],
                                   "key": {"P": ing("#c:plates/aluminum"), "A": ing(m("advanced_processor")), "M": ing(m("memory_chip")),
                                           "C": ing("computercraft:computer_normal"), "L": ing(m("colour_lcd_panel"))},
                                   "result": {"count": 1, "id": "computercraft:computer_advanced"}}
CC["pocket_computer_normal"] = shaped("computercraft:pocket_computer_normal", ["SLS", "CBM", "SNS"],
                                      {"S": "tfmg:plastic_sheet", "L": m("lcd_panel"), "C": m("microprocessor"), "B": "tfmg:circuit_board",
                                       "M": m("memory_chip"), "N": "fundamentals:nickel_metal_hydride_cell"})
CC["pocket_computer_advanced"] = shaped("computercraft:pocket_computer_advanced", ["SLS", "CBM", "SNS"],
                                        {"S": "tfmg:plastic_sheet", "L": m("colour_lcd_panel"), "C": m("advanced_processor"), "B": "tfmg:circuit_board",
                                         "M": m("memory_chip"), "N": "fundamentals:nickel_metal_hydride_cell"})
CC["monitor_normal"] = shaped("computercraft:monitor_normal", ["PPP", "PLP", "PCP"], {"P": "#c:plates/iron", "L": m("lcd_panel"), "C": m("microcontroller")})
CC["monitor_advanced"] = shaped("computercraft:monitor_advanced", ["PPP", "LCL", "PLP"],
                                {"P": "#c:plates/aluminum", "L": m("colour_lcd_panel"), "C": m("microcontroller")}, count=4)
CC["cable"] = shaped("computercraft:cable", [" S ", "WWW", " S "], {"S": "tfmg:plastic_sheet", "W": "#c:wires/copper"}, count=6)
CC["wired_modem"] = shaped("computercraft:wired_modem", ["PPP", "WCW", "PPP"], {"P": "#c:plates/iron", "W": "#c:wires/copper", "C": m("microcontroller")})
CC["wireless_modem_normal"] = shaped("computercraft:wireless_modem_normal", ["PAP", "TCT", "PPP"],
                                     {"P": "#c:plates/iron", "A": "#c:rods/iron", "T": "tfmg:electromagnetic_coil", "C": m("microcontroller")})
CC["wireless_modem_advanced"] = shaped("computercraft:wireless_modem_advanced", ["PEP", "TCT", "PPP"],
                                       {"P": "#c:plates/aluminum", "E": "minecraft:ender_eye", "T": "tfmg:electromagnetic_coil", "C": m("advanced_processor")})
CC["disk_drive"] = shaped("computercraft:disk_drive", ["PPP", "TCT", "PWP"],
                          {"P": "#c:plates/iron", "T": "tfmg:electromagnetic_coil", "C": m("microcontroller"), "W": "#c:wires/copper"})
CC["disk"] = {"type": "minecraft:crafting_shapeless", "category": "redstone",
              "ingredients": [ing("tfmg:plastic_sheet"), ing("tfmg:magnetic_alloy_sheet")], "result": {"count": 1, "id": "computercraft:disk"}}
CC["printer"] = shaped("computercraft:printer", ["PPP", "TCT", "PDP"],
                       {"P": "#c:plates/iron", "T": "tfmg:electromagnetic_coil", "C": m("microcontroller"), "D": "#c:dyes"})
CC["speaker"] = shaped("computercraft:speaker", ["PPP", "TCT", "PRP"],
                       {"P": "#c:plates/iron", "T": "tfmg:electromagnetic_coil", "C": m("microcontroller"), "R": "minecraft:paper"})
CC["redstone_relay"] = shaped("computercraft:redstone_relay", ["PRP", "RCR", "PRP"],
                              {"P": "#c:plates/iron", "R": "#c:dusts/redstone", "C": m("microcontroller")})
DISABLED = ["disk_%d" % i for i in range(1, 17)]


def main():
    shutil.rmtree(DATA / "recipe" / "compute", ignore_errors=True)
    for name, obj in RECIPES.items():
        write(DATA / "recipe" / f"{name}.json", obj)
    cc = DATA.parent / "computercraft" / "recipe"
    shutil.rmtree(cc, ignore_errors=True)
    for name, obj in CC.items():
        write(cc / f"{name}.json", {"neoforge:conditions": CC_LOADED, **obj})
    for name in DISABLED:
        write(cc / f"{name}.json", {"neoforge:conditions": [{"type": "neoforge:false"}], "type": "minecraft:crafting_shapeless",
                                    "ingredients": [], "result": {"id": "minecraft:air"}})

    items = ["fused_quartz", "novolac_resin", "epitaxial_wafer", "processing_wafer", "chrome_mask_blank", "quartz_mask_blank",
             "processing_mask", "packaging_chip", "lcd_panel", "colour_lcd_panel"]
    for chip in ["microcontroller", "memory_chip", "microprocessor", "advanced_processor"]:
        design = chip.replace("_chip", "")
        items += [design + "_mask", design + "_wafer", design + "_die", chip]
    for item in items:
        write(ASSETS / "models" / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/compute/{item}"}})
    write(ASSETS / "models" / "item" / "power_supply.json", {"parent": f"{MOD}:block/power_supply"})
    for name, front in [("power_supply", "power_supply_front"), ("power_supply_on", "power_supply_front_on")]:
        write(ASSETS / "models" / "block" / f"{name}.json", {"parent": "minecraft:block/orientable", "textures": {
            "top": f"{MOD}:block/compute/power_supply_top", "front": f"{MOD}:block/compute/{front}", "side": f"{MOD}:block/compute/power_supply_side",
            "particle": f"{MOD}:block/compute/power_supply_side"}})
    write(ASSETS / "blockstates" / "power_supply.json", {"variants": {
        f"facing={f},powered={p}": {"model": f"{MOD}:block/power_supply" + ("_on" if p == "true" else ""), **({"y": y} if y else {})}
        for f, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)] for p in ["false", "true"]}})
    write(DATA / "loot_table" / "blocks" / "power_supply.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": m("power_supply")}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    lang = read_lang()
    lang.update(LANG)
    write_lang(lang)


LANG = {
    "block.fundamentals.power_supply": "Power Supply",
    "message.fundamentals.no_power": "No power: the computer needs a live Power Supply beside it",
    "message.fundamentals.overheated": "Thermal cutout: the computer is too hot to run",
    "fluid_type.fundamentals.negative_photoresist": "Negative Photoresist",
    "fluid_type.fundamentals.positive_photoresist": "Positive Photoresist",
    "fluid_type.fundamentals.chrome_etchant": "Chrome Etchant",
    "item.fundamentals.fused_quartz": "Fused Quartz",
    "item.fundamentals.novolac_resin": "Novolac Resin",
    "item.fundamentals.epitaxial_wafer": "Epitaxial Wafer",
    "item.fundamentals.epitaxial_wafer.tooltip": "A fresh, flawless crystal layer grown on a doped wafer, ready for CMOS",
    "item.fundamentals.processing_wafer": "Wafer in Process",
    "item.fundamentals.chrome_mask_blank": "Chrome-on-Glass Mask Blank",
    "item.fundamentals.quartz_mask_blank": "Chrome-on-Quartz Reticle Blank",
    "item.fundamentals.processing_mask": "Mask in Process",
    "item.fundamentals.packaging_chip": "Chip in Packaging",
    "item.fundamentals.lcd_panel": "LCD Panel",
    "item.fundamentals.colour_lcd_panel": "Colour LCD Panel",
    "item.fundamentals.microcontroller_mask": "Microcontroller Photomask",
    "item.fundamentals.microcontroller_mask.tooltip": "10 µm contact mask. A deployer keeps it",
    "item.fundamentals.memory_mask": "Memory Reticle",
    "item.fundamentals.memory_mask.tooltip": "1 µm projection reticle. A deployer keeps it",
    "item.fundamentals.microprocessor_mask": "Microprocessor Reticle",
    "item.fundamentals.microprocessor_mask.tooltip": "1 µm projection reticle. A deployer keeps it",
    "item.fundamentals.advanced_processor_mask": "Advanced Processor Reticle",
    "item.fundamentals.advanced_processor_mask.tooltip": "90 nm reticle, laser-written by a computer. A deployer keeps it",
    "item.fundamentals.microcontroller_wafer": "Patterned Microcontroller Wafer",
    "item.fundamentals.memory_wafer": "Patterned Memory Wafer",
    "item.fundamentals.microprocessor_wafer": "Patterned Microprocessor Wafer",
    "item.fundamentals.advanced_processor_wafer": "Patterned Advanced Processor Wafer",
    "item.fundamentals.microcontroller_die": "Microcontroller Die",
    "item.fundamentals.memory_die": "Memory Die",
    "item.fundamentals.microprocessor_die": "Microprocessor Die",
    "item.fundamentals.advanced_processor_die": "Advanced Processor Die",
    "item.fundamentals.microcontroller": "Microcontroller",
    "item.fundamentals.microcontroller.tooltip": "10 µm PMOS, 1971. Runs peripherals",
    "item.fundamentals.memory_chip": "Memory Chip",
    "item.fundamentals.memory_chip.tooltip": "1 µm NMOS DRAM",
    "item.fundamentals.microprocessor": "Microprocessor",
    "item.fundamentals.microprocessor.tooltip": "1 µm CMOS, 1985. Runs a computer",
    "item.fundamentals.advanced_processor": "Advanced Processor",
    "item.fundamentals.advanced_processor.tooltip": "90 nm, copper interconnect, 2004. Runs an advanced computer",
}


if __name__ == "__main__":
    main()
