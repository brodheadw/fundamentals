#!/usr/bin/env python3
"""Paints the items the rare earths are spent on: the phosphor, the didymium glass and the magnets. Edit and
re-run; don't hand-edit the PNGs.

    python3 tools/paint_uses.py
"""
import random

from PIL import Image

from paint_materials import paint_block, paint_item
from paint_minerals import paint_raw
from paint_separation import TEXTURES, heap


def lens():
    """A square of didymium glass: the grey-violet pane of a welder's goggle, with a highlight."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    body, edge, light = (124, 110, 150, 210), (74, 62, 96, 255), (214, 204, 232, 230)
    for x in range(2, 14):
        for y in range(2, 14):
            on_edge = x in (2, 13) or y in (2, 13)
            img.putpixel((x, y), edge if on_edge else body)
    for x, y in ((4, 4), (5, 4), (4, 5), (6, 4), (4, 6)):
        img.putpixel((x, y), light)
    return img


def filament(wire=(150, 154, 162, 255), light=(214, 218, 226, 255)):
    """A coil of wire: a zigzag of bright metal across the sprite."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(2, 14):
        y = 8 + (2 if (x // 2) % 2 == 0 else -2)
        img.putpixel((x, y), light if x % 4 == 0 else wire)
        img.putpixel((x, y + 1), wire)
    return img


def gauze():
    """A square of woven platinum-rhodium wire, the catalyst pad of an ammonia burner: a fine silver mesh."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wire, light, rim = (200, 202, 208, 255), (246, 247, 250, 255), (120, 122, 130, 255)
    for x in range(2, 14):
        for y in range(2, 14):
            if x in (2, 13) or y in (2, 13):
                img.putpixel((x, y), rim)
            elif x % 2 == 0 or y % 2 == 0:
                img.putpixel((x, y), light if (x + y) % 4 == 0 else wire)
    return img


def spangled():
    """A steel plate hot-dipped in zinc: the plate's own silhouette in zinc's bluish white, its face broken into the flat grains
    the zinc freezes in, each a shade off its neighbours."""
    img = paint_item("plate", ((92, 98, 106), (164, 170, 178), (208, 214, 220), (240, 244, 248)))
    rng = random.Random("spangle")
    seeds = [((rng.randrange(16), rng.randrange(16)), rng.choice((-14, -6, 6, 12))) for _ in range(7)]
    for y in range(16):
        for x in range(16):
            r, g, b, a = img.getpixel((x, y))
            if a and r > 120:
                shift = min(seeds, key=lambda s: (s[0][0] - x) ** 2 + (s[0][1] - y) ** 2)[1]
                img.putpixel((x, y), tuple(max(0, min(255, c + shift)) for c in (r, g, b)) + (a,))
    return img


def mantle():
    """A Welsbach mantle: a little stocking of knitted thoria, white and loose-meshed, gathered at the neck onto its ring."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    mesh, light, shade, ring = (232, 230, 222, 255), (255, 255, 250, 255), (176, 172, 160, 255), (120, 116, 108, 255)
    for x in range(6, 10):
        img.putpixel((x, 2), ring)
        img.putpixel((x, 3), ring)
    for y in range(4, 14):
        half = min(4, 1 + (y - 4) // 2) if y < 12 else 4 - (y - 11)
        for x in range(8 - half - 1, 8 + half + 1):
            edge = x in (8 - half - 1, 8 + half)
            colour = shade if edge else light if (x + y) % 3 == 0 and x < 8 else mesh if (x + y) % 2 else shade
            img.putpixel((x, y), colour)
    return img


def anode():
    """A dimensionally stable anode: an expanded titanium mesh blackened by its ruthenium-iridium oxide coat, hung from a bright titanium
    current bar."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    bar, bar_light = (150, 150, 156, 255), (204, 204, 210, 255)
    strand, glint, rim = (66, 68, 80, 255), (122, 126, 142, 255), (36, 36, 44, 255)
    for x in range(2, 14):
        img.putpixel((x, 2), bar_light)
        img.putpixel((x, 3), bar)
    for x in (4, 11):
        img.putpixel((x, 4), bar)
    for x in range(3, 13):
        for y in range(5, 14):
            if x in (3, 12) or y in (5, 13):
                img.putpixel((x, y), rim)
            elif (x + y) % 4 == 0 or (x - y) % 4 == 0:
                img.putpixel((x, y), glint if (x + 2 * y) % 7 == 0 else strand)
    return img


def flask():
    """Mercury in a stoppered glass flask, the way it was sold: a bright silver pool with a mirror highlight under clear glass."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    glass, rim, cork = (200, 222, 230, 110), (150, 176, 188, 230), (138, 96, 58, 255)
    mercury = [(92, 96, 106, 255), (156, 162, 174, 255), (212, 218, 228, 255), (250, 252, 255, 255)]
    for y in range(1, 4):
        for x in range(7, 10):
            img.putpixel((x, y), cork)
    for y in range(4, 7):
        for x in (6, 9):
            img.putpixel((x, y), rim)
        for x in (7, 8):
            img.putpixel((x, y), glass)
    for y in range(7, 15):
        half = min(5, 2 + (y - 6))
        for x in range(8 - half, 8 + half):
            edge = x in (8 - half, 8 + half - 1) or y == 14
            if edge:
                img.putpixel((x, y), rim)
            elif y >= 10:
                shade = 1 if x > 9 or y == 13 else 3 if (x, y) in ((4, 10), (5, 10), (4, 11)) else 2
                img.putpixel((x, y), mercury[0] if y == 13 and x > 9 else mercury[shade])
            else:
                img.putpixel((x, y), glass)
    return img


def bar(tones, band=None):
    """A sintered block magnet seen from above the front edge: a lit top, a front face and a shaded end, with a stripe of
    `band` round its middle where a grade is marked."""
    dark, mid, light, shine = tones
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(6, 12):
        for x in range(2, 12):
            img.putpixel((x, y), dark if y == 11 or x == 2 else mid)
    for y in (4, 5):
        for x in range(2 + 6 - y, 12 + 6 - y):
            img.putpixel((x, y), shine if (x, y) in ((5, 4), (6, 4), (4, 5)) else light)
    for x in (12, 13):
        for y in range(6 - (x - 11), 12 - (x - 11)):
            img.putpixel((x, y), dark)
    if band:
        for y in range(6, 12):
            img.putpixel((6, y), band[0])
            img.putpixel((7, y), band[0] if y == 11 else band[1])
        for x, y in ((7, 5), (8, 5), (8, 4), (9, 4)):
            img.putpixel((x, y), band[1])
    return img


def horseshoe(paint, pole):
    """The alnico horseshoe, painted red as they always were, its two pole faces bare metal."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(2, 14):
        for x in range(2, 14):
            dx, dy = x - 7.5, y - 7.5
            r = (dx * dx + dy * dy) ** 0.5
            arc = y <= 7.5 and 2.6 <= r <= 5.9
            leg = y > 7.5 and (2 <= x <= 4 or 11 <= x <= 13)
            if not (arc or leg):
                continue
            tones = pole if y >= 11 else paint
            edge = (arc and (r > 5.2 or r < 3.2)) or (leg and x in (2, 4, 11, 13))
            lit = x < 7 and not edge
            img.putpixel((x, y), tones[0] if edge else tones[2] if lit else tones[1])
    return img


def magnets():
    """NdFeB rusts unless plated, so a sintered magnet is sold nickel bright; the dysprosium grade the same with its band;
    SmCo needs no plating and stays matte; alnico is cast and painted red."""
    nickel = ((92, 96, 104, 255), (170, 174, 182, 255), (214, 218, 226, 255), (250, 252, 255, 255))
    bar(nickel).save(TEXTURES / "item/neodymium_iron_boron_magnet.png")
    bar(nickel, ((98, 104, 46, 255), (156, 166, 78, 255))).save(TEXTURES / "item/dysprosium_neodymium_iron_boron_magnet.png")
    bar(((44, 40, 38, 255), (84, 78, 74, 255), (120, 114, 108, 255), (156, 150, 142, 255))).save(TEXTURES / "item/samarium_cobalt_magnet.png")
    horseshoe(((112, 20, 18, 255), (184, 40, 34, 255), (226, 84, 70, 255)),
              ((96, 98, 104, 255), (176, 180, 188, 255), (226, 230, 236, 255))).save(TEXTURES / "item/alnico_magnet.png")


def main():
    heap("phosphor", (255, 250, 252), (240, 226, 236), (196, 170, 190)).save(TEXTURES / "item/phosphor.png")
    lens().save(TEXTURES / "item/didymium_glass.png")
    paint_raw("roasted_cobaltite", ((70, 60, 66), (120, 108, 112), (166, 154, 156), (214, 206, 206))).save(TEXTURES / "item/roasted_cobaltite.png")
    paint_raw("roasted_chalcopyrite", ((80, 50, 36), (138, 90, 62), (184, 132, 96), (230, 190, 150))).save(TEXTURES / "item/roasted_chalcopyrite.png")
    heap("rhenium_flue_dust", (250, 250, 246), (214, 214, 208), (150, 150, 146)).save(TEXTURES / "item/rhenium_flue_dust.png")
    heap("tungsten_carbide", (120, 122, 128), (74, 76, 82), (40, 42, 46)).save(TEXTURES / "item/tungsten_carbide.png")
    filament().save(TEXTURES / "item/tungsten_filament.png")
    heap("clarifier_sludge", (150, 128, 96), (112, 92, 64), (70, 56, 38)).save(TEXTURES / "item/clarifier_sludge.png")
    # the sludge pressed to a cake and packed for the tailings dam: the rust-brown of the iron hydroxide in it
    paint_block("clarifier_sludge_block", ((62, 48, 32), (104, 84, 58), (140, 118, 86), (176, 152, 116))).save(TEXTURES / "block/clarifier_sludge_block.png")
    # thorium nitrate is white crystals; the mantle soaked in it and burnt out is the white of thoria
    heap("thorium_nitrate", (255, 255, 255), (238, 238, 234), (186, 186, 180)).save(TEXTURES / "item/thorium_nitrate.png")
    mantle().save(TEXTURES / "item/gas_mantle.png")
    # tenorite, CuO, is black; zinc oxide is white; nickel oxide is green, dulled here by the pentlandite's iron oxide
    paint_raw("copper_calcine", ((22, 20, 20), (48, 44, 42), (78, 72, 68), (122, 114, 108))).save(TEXTURES / "item/copper_calcine.png")
    heap("zinc_oxide", (252, 252, 248), (222, 222, 214), (160, 160, 152)).save(TEXTURES / "item/zinc_oxide.png")
    paint_raw("roasted_pentlandite", ((40, 46, 34), (72, 84, 60), (108, 122, 90), (156, 168, 132))).save(TEXTURES / "item/roasted_pentlandite.png")
    # lithium chloride is white; ferroboron a grey metal lump
    heap("lithium_chloride", (255, 255, 255), (236, 238, 238), (182, 186, 188)).save(TEXTURES / "item/lithium_chloride.png")
    paint_raw("ferroboron", ((58, 60, 64), (104, 108, 114), (150, 154, 160), (200, 204, 210))).save(TEXTURES / "item/ferroboron.png")
    # soda ash is white; sodium chromate lemon yellow and the dichromate orange-red, as chromate salts are; aluminium powder dull silver
    heap("soda_ash", (254, 254, 252), (232, 232, 228), (178, 178, 172)).save(TEXTURES / "item/soda_ash.png")
    heap("sodium_chromate", (252, 240, 110), (234, 206, 34), (168, 138, 18)).save(TEXTURES / "item/sodium_chromate.png")
    heap("sodium_dichromate", (255, 160, 80), (226, 98, 28), (150, 52, 16)).save(TEXTURES / "item/sodium_dichromate.png")
    heap("aluminium_powder", (224, 226, 230), (172, 176, 184), (110, 114, 122)).save(TEXTURES / "item/aluminium_powder.png")
    # titania slag is a black glassy lump off the arc furnace, browned by the iron left in it; magnesium chloride is white
    paint_raw("titania_slag", ((18, 16, 16), (42, 38, 36), (72, 64, 60), (118, 106, 98))).save(TEXTURES / "item/titania_slag.png")
    heap("magnesium_chloride", (255, 255, 255), (234, 236, 238), (180, 184, 188)).save(TEXTURES / "item/magnesium_chloride.png")
    # The platinum refinery: sal ammoniac white; the insolubles black; each metal's salt the colour chemists know it by, the
    # chloroplatinate bright yellow, dichlorodiammine palladium a duller yellow, the chlororuthenate red-brown, the chloroiridate
    # near black, the chlororhodate rose. The reforming catalyst is grey-white alumina beads.
    heap("ammonium_chloride", (255, 255, 255), (238, 240, 240), (186, 190, 192)).save(TEXTURES / "item/ammonium_chloride.png")
    heap("insoluble_residue", (88, 88, 92), (52, 52, 56), (24, 24, 28)).save(TEXTURES / "item/insoluble_residue.png")
    heap("iridium_rhodium_residue", (104, 92, 90), (66, 56, 54), (34, 28, 28)).save(TEXTURES / "item/iridium_rhodium_residue.png")
    heap("ammonium_chloroplatinate", (255, 244, 120), (246, 214, 40), (190, 150, 16)).save(TEXTURES / "item/ammonium_chloroplatinate.png")
    heap("dichlorodiammine_palladium", (250, 230, 140), (226, 192, 78), (162, 128, 40)).save(TEXTURES / "item/dichlorodiammine_palladium.png")
    heap("ammonium_chlororuthenate", (156, 72, 52), (112, 40, 28), (64, 20, 14)).save(TEXTURES / "item/ammonium_chlororuthenate.png")
    heap("ammonium_chloroiridate", (96, 40, 32), (58, 24, 20), (28, 12, 10)).save(TEXTURES / "item/ammonium_chloroiridate.png")
    heap("ammonium_chlororhodate", (236, 132, 150), (204, 80, 104), (140, 42, 64)).save(TEXTURES / "item/ammonium_chlororhodate.png")
    heap("reforming_catalyst", (246, 246, 242), (206, 206, 204), (136, 138, 142)).save(TEXTURES / "item/reforming_catalyst.png")
    gauze().save(TEXTURES / "item/platinum_rhodium_gauze.png")
    filament((112, 126, 150, 255), (178, 192, 216, 255)).save(TEXTURES / "item/osmium_filament.png")
    # roasting burns the sulfides' iron to red-brown oxide among the black cassiterite; solder is a coil of dull tin-lead wire
    heap("roasted_tin_concentrate", (130, 84, 60), (82, 50, 34), (38, 24, 18)).save(TEXTURES / "item/roasted_tin_concentrate.png")
    filament((150, 152, 156, 255), (206, 208, 212, 255)).save(TEXTURES / "item/solder.png")
    # Parkes crust: zinc-silver alloy skimmed off the lead, a pale grey dross; litharge, PbO off the cupel, orange-yellow
    paint_raw("silver_zinc_crust", ((70, 72, 76), (118, 120, 124), (164, 166, 170), (212, 214, 216))).save(TEXTURES / "item/silver_zinc_crust.png")
    heap("litharge", (252, 206, 100), (230, 150, 46), (164, 88, 22)).save(TEXTURES / "item/litharge.png")
    flask().save(TEXTURES / "item/mercury.png")
    # the zirconium chlorides are white; the crude one yellowed by the ferric chloride in it. Yttria-stabilised zirconia is an off-white ceramic powder
    heap("crude_zirconium_tetrachloride", (252, 248, 224), (228, 222, 186), (170, 162, 122)).save(TEXTURES / "item/crude_zirconium_tetrachloride.png")
    heap("zirconium_tetrachloride", (255, 255, 255), (236, 238, 238), (182, 186, 188)).save(TEXTURES / "item/zirconium_tetrachloride.png")
    heap("hafnium_tetrachloride", (255, 255, 255), (230, 232, 236), (172, 176, 184)).save(TEXTURES / "item/hafnium_tetrachloride.png")
    heap("yttria_stabilised_zirconia", (254, 252, 244), (232, 228, 214), (176, 170, 152)).save(TEXTURES / "item/yttria_stabilised_zirconia.png")
    # beryl frit is a pale green glass, quenched to grit; the hydroxide and the fluoroberyllate are white; the pebbles dull grey metal
    paint_raw("beryl_frit", ((58, 94, 82), (108, 148, 130), (162, 198, 180), (226, 244, 236))).save(TEXTURES / "item/beryl_frit.png")
    heap("beryllium_hydroxide", (255, 255, 255), (240, 240, 238), (190, 190, 186)).save(TEXTURES / "item/beryllium_hydroxide.png")
    heap("ammonium_fluoroberyllate", (255, 255, 255), (236, 240, 240), (184, 190, 192)).save(TEXTURES / "item/ammonium_fluoroberyllate.png")
    paint_raw("beryllium_pebbles", ((70, 76, 84), (124, 132, 142), (170, 178, 188), (216, 222, 230))).save(TEXTURES / "item/beryllium_pebbles.png")
    anode().save(TEXTURES / "item/dimensionally_stable_anode.png")
    heap("red_mud", (178, 84, 58), (140, 58, 38), (92, 36, 24)).save(TEXTURES / "item/red_mud.png")
    heap("aluminium_hydroxide", (255, 255, 255), (240, 240, 236), (192, 192, 186)).save(TEXTURES / "item/aluminium_hydroxide.png")
    heap("alumina", (255, 255, 255), (244, 244, 244), (200, 202, 206)).save(TEXTURES / "item/alumina.png")
    heap("cryolite", (255, 255, 255), (232, 236, 238), (178, 186, 190)).save(TEXTURES / "item/cryolite.png")
    # nickel oxide roasted from matte is the green of bunsenite, greyed by what the roast leaves; carbonyl pellets are bright, nearly pure nickel
    paint_raw("nickel_oxide", ((50, 72, 44), (90, 120, 72), (130, 160, 102), (178, 204, 148))).save(TEXTURES / "item/nickel_oxide.png")
    paint_raw("nickel_pellets", ((96, 96, 90), (160, 160, 152), (206, 206, 198), (244, 244, 238))).save(TEXTURES / "item/nickel_pellets.png")
    # tungstic acid is yellow; APT, ammonium perrhenate and lithium carbonate are white crystals; MCrAlY a grey gas-atomised metal powder
    heap("tungstic_acid", (250, 238, 120), (226, 204, 60), (160, 140, 30)).save(TEXTURES / "item/tungstic_acid.png")
    heap("ammonium_paratungstate", (255, 255, 255), (240, 242, 244), (192, 196, 202)).save(TEXTURES / "item/ammonium_paratungstate.png")
    heap("ammonium_perrhenate", (255, 255, 255), (236, 238, 242), (184, 188, 196)).save(TEXTURES / "item/ammonium_perrhenate.png")
    heap("lithium_carbonate", (255, 255, 255), (242, 242, 240), (196, 196, 192)).save(TEXTURES / "item/lithium_carbonate.png")
    heap("mcraly_powder", (196, 198, 204), (140, 144, 152), (84, 88, 96)).save(TEXTURES / "item/mcraly_powder.png")
    # boric acid is white, pearly flakes; a hot-dipped plate is bright zinc, crystallised in spangles
    heap("boric_acid", (255, 255, 255), (242, 242, 238), (196, 196, 190)).save(TEXTURES / "item/boric_acid.png")
    spangled().save(TEXTURES / "item/galvanized_steel_plate.png")
    magnets()
    print("uses textures written")


if __name__ == "__main__":
    main()
