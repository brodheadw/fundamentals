#!/usr/bin/env python3
"""Writes the showcase datapack's functions: the whole separation tree, read from the cut recipes, laid out as
batteries on a flat world. The root battery parts the mixed liquor; each product is either the feed of the next
battery (piped north to it) or a single element, which gets a station that precipitates the oxalate, smelts it and
sets the oxide on a depot; neodymium and dysprosium go on to metal in a vat north of their stations. The root's liquor starts
as monazite: washed, baked in sulfuric acid, leached in hydrochloric and clarified, west of the root. Every battery has
its mixers, lever, organic feed, acid feed, sump drain and side tanks, every end tank is filled and refilled, and the whole plant is force-loaded so it runs while you walk it.
South of spawn is a gallery of the rest, each exhibit signed and facing north: every ore, the zirconium, hafnium and beryllium
routes in frames, magnet grades among blast furnaces, oxidation, thermometers, seawater and corrosion, cracking and plastics.

    python3 tools/showcase/build_showcase.py

Layout: a battery is 3 blocks per stage along +x, 3 wide in z, 2 tall from y -60; rows are 12 blocks apart going
north; the light product's battery sits 3 blocks east of its parent's head, the heavy product's battery east of the
light subtree, fed by a pipe that jogs along a lane 6 blocks north of the parent.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
RECIPES = ROOT / "src/main/resources/data/fundamentals/recipe/separation"
FUNCTIONS = ROOT / "tools/showcase/datapack/data/showcase/function"

Y = -60               # the floor the plant stands on is y -61; vats are y -60 and -59
ROW = 12              # z between a battery and its children
LANE = 6              # z north of a battery where the heavy pipe runs east
MOTOR = "{ScrollValue:64}"
LIQUOR_PIPE = "tfmg:plastic_pipe"   # the liquors are chlorides in dilute acid and eat copper; the organic does not
LIQUOR_PUMP = "tfmg:plastic_mechanical_pump"
LIQUOR_TANK = "fundamentals:plastic_fluid_tank"   # and so do the acid, the crude liquor and the spent liquor; copper is for the organic

lines, fills, refills = [], [], []
bounds = [0, 0, 0, 0]   # min x, min z, max x, max z, grown as blocks are placed


def put(x, y, z, block):
    lines.append(f"setblock {x} {y} {z} {block}")
    bounds[0] = min(bounds[0], x); bounds[1] = min(bounds[1], z); bounds[2] = max(bounds[2], x); bounds[3] = max(bounds[3], z)


def tank(x, y, z, fluid=None, amount=8000, block=LIQUOR_TANK):
    put(x, y, z, block)
    if fluid:
        fluid = fluid if ":" in fluid else f"fundamentals:{fluid}"
        merge = f'data merge block {x} {y} {z} {{TankContent:{{Fluid:{{id:"{fluid}",amount:{amount}}}}}}}'
        fills.append(merge)
        refills.append(f"execute unless data block {x} {y} {z} TankContent.Fluid run {merge}")


def pump(x, y, z, facing, axis, motor_at, motor_facing, block=LIQUOR_PUMP):
    """A pump with the cogwheel above it on its axis and a motor beside the cog."""
    put(x, y, z, f"{block}[facing={facing}]")
    put(x, y + 1, z, f"create:cogwheel[axis={axis}]")
    put(*motor_at, f"create:creative_motor[facing={motor_facing}]{MOTOR}")


def cuts():
    out = {}
    for path in sorted(RECIPES.glob("*.json")):
        d = json.loads(path.read_text())
        out[d["liquor"].split(":")[1]] = {"stages": d["stages"], "organic": d["organic"].split(":")[1],
                                          "light": d["light"].split(":")[1], "heavy": d["heavy"].split(":")[1]}
    return out


CUTS = cuts()
# mixed liquors precipitated as they are, before their own cut parts them (didymium is praseodymium and neodymium together)
TAPS = {i["fluid"].split(":")[1] for p in (ROOT / "src/main/resources/data/fundamentals/recipe/mixing").glob("*_oxalate.json")
        for i in json.loads(p.read_text())["ingredients"] if "fluid" in i and i["fluid"].split(":")[1] in CUTS}


def width(liquor):
    """Blocks of x a battery and everything it feeds take up, from its feed tank eastward."""
    if liquor not in CUTS:
        return 0
    cut = CUTS[liquor]
    own = 3 * cut["stages"] + 4
    light = width(cut["light"])
    if cut["heavy"] in CUTS:
        return max(own, heavy_x(liquor, 0) + width(cut["heavy"]))
    return max(own, 3 + light + 2)


def heavy_x(liquor, x0):
    """Where the heavy product's battery starts: past this battery's tail, and past the light subtree."""
    cut = CUTS[liquor]
    return x0 + max(3 * cut["stages"], 3 + width(cut["light"]) + 3)


def battery(liquor, x0, z0):
    cut = CUTS[liquor]
    n = cut["stages"]
    for i in range(n):
        for dx in range(3):
            for dz in range(3):
                for dy in range(2):
                    put(x0 + 3 * i + dx, Y + dy, z0 + dz, "fundamentals:mixer_settler[facing=east]")
    for i in range(n):
        # the mixer over the hatch, its cogwheel beside it in the left row, the motor over the cog
        put(x0 + 3 * i, Y + 2, z0 + 1, "create:mechanical_mixer")
        put(x0 + 3 * i, Y + 2, z0, "create:cogwheel[axis=y]")
        put(x0 + 3 * i, Y + 3, z0, f"create:creative_motor[facing=down]{MOTOR}")
    # the organic: a tank and pump at the head, a pipe along the top of the right row with a drop into every stage
    tank(x0 - 2, Y + 3, z0 + 2, cut["organic"], block="create:fluid_tank")
    pump(x0 - 1, Y + 3, z0 + 2, "east", "x", (x0 - 2, Y + 4, z0 + 2), "east", "create:mechanical_pump")
    for x in range(x0, x0 + 3 * n):
        drop = (x - x0) % 3 == 2
        east = x < x0 + 3 * n - 1
        put(x, Y + 3, z0 + 2, f"create:fluid_pipe[east={str(east).lower()},west=true,down={str(drop).lower()}]")
        if drop:
            put(x, Y + 2, z0 + 2, "create:fluid_pipe[up=true,down=true]")
    # feed at the back, acid at the front
    tank(x0 - 2, Y, z0 + 1, None if liquor == "rare_earth_liquor" else None)
    pump(x0 - 1, Y, z0 + 1, "east", "x", (x0 - 2, Y + 1, z0 + 1), "east")
    tank(x0 + 3 * n + 1, Y, z0 + 1, "hydrochloric_acid")
    pump(x0 + 3 * n, Y, z0 + 1, "west", "x", (x0 + 3 * n + 1, Y + 1, z0 + 1), "west")
    # the products pumped straight out of the sides, northward; the lever on the head's back wall
    for x in (x0 + 1, x0 + 3 * n - 2):
        pump(x, Y, z0 - 1, "north", "z", (x, Y + 1, z0 - 2), "south")
    put(x0, Y + 1, z0 - 1, "minecraft:lever[face=wall,facing=north,powered=false]")
    # the sump under the head, drained into a tank below the floor
    put(x0, Y - 1, z0, f"{LIQUOR_PUMP}[facing=down]")
    put(x0 + 1, Y - 1, z0, "create:cogwheel[axis=y]")
    put(x0 + 1, Y - 2, z0, f"create:creative_motor[facing=up]{MOTOR}")
    put(x0, Y - 2, z0, LIQUOR_TANK)
    return x0 + 1, x0 + 3 * n - 2   # x of the light and heavy side pumps, at z0 - 1


def pipe_north(x, z_from, z_to):
    """Pipe from z_from north to z_to inclusive, pushed by the side pump behind it."""
    for z in range(z_from, z_to - 1, -1):
        put(x, Y, z, f"{LIQUOR_PIPE}[north=true,south=true]")


def pipe_jog(x_from, x_to, z_pipe, z_lane, z_to):
    """From the side pump's outlet at (x_from, z_pipe) north to the lane, east along it to x_to with a relay pump
    every twelve blocks, then north to the feed tank just past z_to."""
    if x_from == x_to:
        pipe_north(x_from, z_pipe, z_to + 1)
        return
    for z in range(z_pipe, z_lane, -1):
        put(x_from, Y, z, f"{LIQUOR_PIPE}[north=true,south=true]")
    put(x_from, Y, z_lane, f"{LIQUOR_PIPE}[south=true,east=true]")
    for x in range(x_from + 1, x_to):
        if (x - x_from) % 12 == 6 and x < x_to - 1:
            pump(x, Y, z_lane, "east", "x", (x - 1, Y + 1, z_lane), "east")
        else:
            put(x, Y, z_lane, f"{LIQUOR_PIPE}[east=true,west=true]")
    put(x_to, Y, z_lane, f"{LIQUOR_PIPE}[west=true,north=true]")
    for z in range(z_lane - 1, z_to, -1):
        put(x_to, Y, z, f"{LIQUOR_PIPE}[north=true,south=true]")


def station(x, z):
    """From the side pump's outlet at (x, z): pipe north and up into a raised basin under a mixer, oxalic acid in it and a chest
    of it beside. A basin pours out only onto something a belt could feed (a chute, a depot, another basin) with clear air
    beside it, so the oxalate goes into a chute that drops it into the top of a blast furnace; a furnace gives up what it
    made only from below, so a hopper under it sets the oxide on a depot."""
    put(x, Y, z, f"{LIQUOR_PIPE}[north=true,south=true]")
    put(x, Y, z - 1, f"{LIQUOR_PIPE}[south=true,up=true]")
    put(x, Y + 1, z - 1, f"{LIQUOR_PIPE}[down=true,up=true]")
    put(x, Y + 2, z - 1, f"{LIQUOR_PIPE}[down=true,up=true]")
    put(x, Y + 3, z - 1, f"{LIQUOR_PIPE}[down=true,north=true]")
    put(x, Y + 3, z - 2, 'create:basin[facing=north]{InputItems:{Size:9,Items:[{Slot:0b,id:"fundamentals:oxalic_acid",count:64}]}}')
    put(x, Y + 5, z - 2, "create:mechanical_mixer")
    put(x + 1, Y + 5, z - 2, "create:cogwheel[axis=y]")
    put(x + 1, Y + 6, z - 2, f"create:creative_motor[facing=down]{MOTOR}")
    put(x - 1, Y, z - 2, 'minecraft:chest[facing=west]{Items:[{Slot:0b,id:"fundamentals:oxalic_acid",count:64}]}')
    put(x, Y + 2, z - 3, "create:chute")
    put(x, Y + 1, z - 3, 'minecraft:blast_furnace[facing=south]{Items:[{Slot:1b,id:"minecraft:coal",count:64}]}')
    put(x, Y, z - 3, "minecraft:hopper[facing=north]")
    put(x, Y, z - 4, "create:depot")


def stocked(x, y, z, item):
    """A chest of 27 stacks of one thing, restocked by refill once it is empty."""
    items = "{Items:[" + ",".join(f'{{Slot:{i}b,id:"fundamentals:{item}",count:64}}' for i in range(27)) + "]}"
    put(x, y, z, "minecraft:chest[facing=north]" + items)
    refills.append(f"execute unless data block {x} {y} {z} Items[0] run data merge block {x} {y} {z} {items}")


def feed(x, y, z, item):
    """A hopper pouring one thing down into the vat under it from a stocked chest on top. One thing per hopper: a hopper
    full of one item never lets a second through."""
    put(x, y, z, "minecraft:hopper[facing=down]")
    stocked(x, y + 1, z, item)


def vat(x, z, outputs):
    """A 2x2 firebrick-lined TFMG vat at y Y+2, its north-west corner at (x, z), and what it stands on. A vat forms its
    multiblock only when its block entity is marked Uninitialized (setblock places the block before its entity exists, so
    the placement hook finds nothing), and only the north-west corner may be marked: mark all four and one is left out as a
    lone vat of size 0 that takes no fluid. That corner is the controller. It reads heat from every block under it and adds
    them up: a seething blaze burner gives 2, so two creative ones make the 4 that superheated needs, which no 1x1 vat can
    reach. It runs a recipe only when its attachments (electrode holders, an industrial mixer, anything from one below to
    one above it) are exactly the recipe's machines, and stops once a product would pass 64 in its output slots. A hopper
    fills those output slots too once an input stack is full. Its items come out from any side through any extractor,
    inputs first, so a smart chute filtered to one product pulls it down into a chest (a depot takes one insert and then
    refuses the next)."""
    for dx in range(2):
        for dz in range(2):
            put(x + dx, Y + 2, z + dz, "tfmg:fireproof_chemical_vat" + ("{Uninitialized:1b}" if dx == dz == 0 else ""))
    for dx, dz in ((0, 0), (1, 1)):
        put(x + dx, Y, z + dz, "tfmg:fireproof_bricks")
        put(x + dx, Y + 1, z + dz, "create:blaze_burner[blaze=seething]{isCreative:1b}")
    for (dx, dz), item in outputs:
        put(x + dx, Y + 1, z + dz, f'create:smart_chute{{Filter:{{id:"{item if ":" in item else "fundamentals:" + item}",count:1}}}}')
        put(x + dx, Y, z + dz, "minecraft:chest[facing=west]")


def electrolysis_cell(x, z, metal):
    """Fluoride-melt electrolysis north of the oxide station at (x, z): two electrode holders on the vat, each holding a
    copper electrode and powered from a creative generator straight above it (a holder takes power only from above, and the
    generators sit on the diagonal so they make two networks); 500 V across an electrode's 10 ohms is 50 A, past the
    5 A an electrolyser needs. The oxide is hoppered in; the fluoride comes back 9 times in 10 and is set in the vat once,
    since a hopper would top its output slot past 64 and stop the vat."""
    vat(x, z - 8, [((1, 0), f"{metal}_ingot")])
    for dx, dz in ((0, 0), (1, 1)):
        put(x + dx, Y + 3, z - 8 + dz, 'tfmg:electrode_holder{Electrode:"tfmg:copper"}')
        put(x + dx, Y + 4, z - 8 + dz, "tfmg:creative_generator")
    feed(x + 1, Y + 3, z - 8, f"{metal}_oxide")
    fills.append(f'data merge block {x} {Y + 2} {z - 8} {{InputItems:{{Size:4,Items:[{{Slot:0b,id:"fundamentals:{metal}_fluoride",count:64}}]}}}}')


def reduction_cell(x, z, metal):
    """Calciothermic reduction north of the oxide station at (x, z): an industrial mixer with a mixer blade on the vat
    (driven from above at 64 rpm; it needs 30), the fluoride and the calcium each hoppered in, argon pumped in from the
    east; the metal and the fluorite each into a chest. The vat joins its corners into one without telling its neighbours,
    so a pump placed with it keeps pushing into the corner as it was, takes a sip and stops: fill places the pump again
    once the vat has formed."""
    vat(x, z - 8, [((1, 0), f"{metal}_ingot"), ((0, 1), "raw_fluorite")])
    put(x, Y + 3, z - 8, 'tfmg:industrial_mixer{MixerMode:"mixing"}')
    put(x, Y + 4, z - 8, f"create:creative_motor[facing=down]{MOTOR}")
    feed(x + 1, Y + 3, z - 8, f"{metal}_fluoride")
    feed(x, Y + 3, z - 7, "calcium_ingot")
    tank(x + 3, Y + 2, z - 7, "argon", block="create:fluid_tank")
    pump(x + 2, Y + 2, z - 7, "west", "x", (x + 3, Y + 3, z - 7), "west", "create:mechanical_pump")
    fills.extend([f"setblock {x + 2} {Y + 2} {z - 7} minecraft:air", f"setblock {x + 2} {Y + 2} {z - 7} create:mechanical_pump[facing=west]"])


CELLS = {"neodymium_liquor": electrolysis_cell, "dysprosium_liquor": reduction_cell}


def place(liquor, x0, z0):
    light_x, heavy_px = battery(liquor, x0, z0)
    if liquor in TAPS:
        pump(x0 - 2, Y, z0, "north", "z", (x0 - 2, Y + 1, z0 - 1), "south")
        station(x0 - 2, z0 - 1)
    cut = CUTS[liquor]
    for product, px in ((cut["light"], light_x), (cut["heavy"], heavy_px)):
        if product in CUTS:
            cx = x0 + 3 if product == cut["light"] else heavy_x(liquor, x0)
            pipe_jog(px, cx - 2, z0 - 2, z0 - LANE, z0 - ROW + 1)
            place(product, cx, z0 - ROW)
        else:
            station(px, z0 - 2)
            if product in CELLS:
                CELLS[product](px, z0 - 2, product.removesuffix("_liquor"))


def ore():
    """Monazite to crude liquor, west of the clarifier, falling one block a step so items only ever go down. The ore is
    hoppered onto a depot in front of a fan blowing through water (held in a glass trough) and washed to concentrate; a fan
    washes whatever sits on a depot, and a hopper under it would pull the raw ore straight off it (a depot gives up the
    stack it is still processing), so a smart chute filtered to the concentrate takes it down into a hopper that feeds the
    acid bake. The bake is a basin on a kindled creative blaze burner, sulfuric acid pumped in from the south; it spouts the
    sulfate and the phosphoric acid into a collector, whose phosphoric acid is pumped north into a tank and whose sulfate a
    hopper under it hands into the leach basin. The leach (hydrochloric acid pumped in from the south) spouts the crude
    liquor and the residue into a second collector: the liquor is pumped round into the clarifier's crude tank, the
    residue hoppered into a chest. Two pumps (a pump is a small cogwheel) side by side on one axis mesh, so no two share
    a row; and a pipe joins whatever holds fluid beside it, whatever its placed state says, so a feed pipe run under the
    leach basin also filled the collector next to it with acid. An acid pump placed before its tank is filled takes a sip
    and stops, as at the vats, so fill places both again."""
    stocked(-15, Y + 7, 1, "raw_monazite")
    put(-15, Y + 6, 1, "minecraft:hopper[facing=down]")
    put(-15, Y + 5, 1, "create:depot")
    put(-18, Y + 5, 1, f"create:creative_motor[facing=east]{MOTOR}")
    put(-17, Y + 5, 1, "create:encased_fan[facing=east]")
    for x, y, z in ((-16, Y + 4, 1), (-16, Y + 5, 0), (-16, Y + 5, 2)):
        put(x, y, z, "minecraft:glass")
    put(-16, Y + 5, 1, "minecraft:water")
    put(-15, Y + 4, 1, 'create:smart_chute{Filter:{id:"fundamentals:light_rare_earth_concentrate",count:1}}')
    put(-15, Y + 3, 1, "minecraft:hopper[facing=east]")
    # the bake
    put(-14, Y + 2, 1, "create:blaze_burner[blaze=kindled]{isCreative:1b}")
    put(-14, Y + 3, 1, "create:basin[facing=east]")
    put(-14, Y + 5, 1, "create:mechanical_mixer")
    put(-14, Y + 5, 0, "create:cogwheel[axis=y]")
    put(-14, Y + 6, 0, f"create:creative_motor[facing=down]{MOTOR}")
    tank(-14, Y + 3, 3, "tfmg:sulfuric_acid")
    pump(-14, Y + 3, 2, "north", "z", (-14, Y + 4, 3), "north")
    put(-13, Y + 2, 1, "create:basin[facing=down]")
    pump(-13, Y + 2, 0, "north", "z", (-13, Y + 3, -1), "south")
    tank(-13, Y + 2, -1)
    put(-13, Y + 1, 1, "minecraft:hopper[facing=east]")
    # the leach
    put(-12, Y + 1, 1, "create:basin[facing=east]")
    put(-12, Y + 3, 1, "create:mechanical_mixer")
    put(-11, Y + 3, 1, "create:cogwheel[axis=y]")
    put(-11, Y + 4, 1, f"create:creative_motor[facing=down]{MOTOR}")
    tank(-12, Y + 1, 3, "hydrochloric_acid")
    pump(-12, Y + 1, 2, "north", "z", (-12, Y + 2, 3), "north")
    for x, y in ((-14, Y + 3), (-12, Y + 1)):
        fills.extend([f"setblock {x} {y} 2 minecraft:air", f"setblock {x} {y} 2 {LIQUOR_PUMP}[facing=north]"])
    put(-11, Y, 1, "create:basin[facing=down]")
    put(-11, Y - 1, 1, "minecraft:hopper[facing=down]")
    put(-11, Y - 2, 1, "minecraft:chest")
    pump(-11, Y, 0, "north", "z", (-11, Y + 1, -1), "south")
    put(-11, Y, -1, f"{LIQUOR_PIPE}[south=true,east=true]")
    put(-10, Y, -1, f"{LIQUOR_PIPE}[west=true,east=true]")
    put(-9, Y, -1, f"{LIQUOR_PIPE}[west=true,south=true]")
    put(-9, Y, 0, f"{LIQUOR_PIPE}[north=true,south=true]")
    put(-9, Y, 1, f"{LIQUOR_PIPE}[north=true,up=true]")


LANG = json.loads((ROOT / "src/main/resources/assets/fundamentals/lang/en_us.json").read_text())
WALL = "minecraft:polished_andesite"


def ns(id):
    return id if ":" in id else f"fundamentals:{id}"


def name(id):
    path = ns(id).split(":")[1]
    for kind in ("item", "block", "fluid_type"):
        if ns(id).startswith("fundamentals:") and f"{kind}.fundamentals.{path}" in LANG:
            return LANG[f"{kind}.fundamentals.{path}"]
    return path.replace("_", " ").title()


def text(words):
    """A waxed sign's front: words wrapped to four lines of 15 characters, | forcing a break."""
    rows = []
    for part in words.split("|"):
        row = ""
        for word in part.split():
            if row and len(row) + 1 + len(word) > 15:
                rows.append(row)
                row = word
            else:
                row = f"{row} {word}".strip()
        rows.append(row)
    assert len(rows) <= 4, words
    rows += [""] * (4 - len(rows))
    return "{front_text:{messages:[" + ",".join("'" + json.dumps(r, ensure_ascii=False).replace("'", "\\'") + "'" for r in rows) + "]},is_waxed:1b}"


def sign(x, y, z, words):
    put(x, y, z, "minecraft:birch_sign[rotation=8]" + text(words))


def label(x, y, z, words):
    """On the north face of the block at z + 1."""
    put(x, y, z, "minecraft:birch_wall_sign[facing=north]" + text(words))


def post(x, z, words):
    put(x, Y, z, WALL)
    put(x, Y + 1, z, WALL)
    label(x, Y + 1, z - 1, words)


def stack(item):
    id, count, *components = item
    return f'id:"{ns(id)}",count:{count}' + (f",components:{components[0]}" if components else "")


def chest(x, z, items, y=Y):
    put(x, y, z, "minecraft:chest[facing=north]{Items:[" + ",".join(f"{{Slot:{i}b,{stack(it)}}}" for i, it in enumerate(items)) + "]}")


def frame(x, y, z, item):
    """An item frame hung on the north face of the block at z + 1."""
    lines.append(f'summon minecraft:item_frame {x + 0.5} {y + 0.5} {z + 0.5} {{Facing:2b,Fixed:1b,Invulnerable:1b,Item:{{id:"{ns(item)}",count:1}}}}')


def ore_wall(x, z):
    ores = sorted(k.split(".")[2] for k in LANG if k.startswith("block.fundamentals.") and k.count(".") == 2 and k.endswith("_ore"))
    rocks = ["carbonatite", "gabbro", "laterite", "syenite"]
    for title, blocks in (("Every ore", ores), ("Host rocks", rocks)):
        post(x, z, title)
        x += 1
        for block in blocks:
            put(x, Y, z, WALL)
            put(x, Y + 1, z, ns(block))
            label(x, Y + 1, z - 1, name(block))
            x += 1
        x += 1


ROUTES = [
    ("Zirconium: zircon to ingot", ["raw_zircon", "zircon_concentrate", "crude_zirconium_tetrachloride", "zirconium_tetrachloride",
                                    "zirconium_sponge", "zirconium_ingot", "zirconium_plate"]),
    ("Hafnium: parted off the chloride", ["crude_zirconium_tetrachloride", "hafnium_tetrachloride", "hafnium_sponge", "hafnium_ingot"]),
    ("Zirconia, YSZ and the Factory", ["crude_zirconium_tetrachloride", "zirconium_oxide", "yttrium_oxide", "yttria_stabilised_zirconia",
                                       "tfmg:turbine_blade", "zircon_concentrate", ("block", "tfmg:casting_basin")]),
    ("Beryllium: beryl and bertrandite", ["raw_beryl", "beryl_frit", "raw_bertrandite", ("fluid", "beryllium_sulfate_liquor"),
                                          "beryllium_hydroxide", "ammonium_fluoroberyllate", "beryllium_fluoride", "beryllium_pebbles",
                                          "beryllium_ingot", "beryllium_oxide", "beryllium_copper_ingot", ("block", "beryllium_copper_block")]),
]


def routes(x, z):
    """Each route a wall, its stages in frames left to right with their names above; a chest of them all under the title."""
    for title, steps in ROUTES:
        for y in range(3):
            put(x, Y + y, z + 1, WALL)
        label(x, Y + 2, z, title)
        chest(x, z, [(s, 64) for s in steps if isinstance(s, str)])
        x += 1
        for step in steps:
            kind, id = step if isinstance(step, tuple) else ("item", step)
            for y in range(3):
                put(x, Y + y, z + 1, WALL)
            label(x, Y + 2, z, name(id))
            if kind == "item":
                frame(x, Y + 1, z, id)
            else:
                put(x, Y, z, WALL)
                if kind == "fluid":
                    tank(x, Y + 1, z, id)
                else:
                    put(x, Y + 1, z, ns(id))
            x += 1
        x += 1


GRADES = [("neodymium_iron_boron", "NdFeB"), ("dysprosium_neodymium_iron_boron", "Dy-NdFeB"), ("samarium_cobalt", "SmCo"), ("alnico", "Alnico")]


def magnets(x0, z0):
    """A generator of each grade turned by a creative motor, as MagnetTests builds them: three lit blast furnaces round each
    in the hot row (a furnace with no fuel and nothing to smelt stays lit), none in the cold row seven blocks south."""
    for z, hot in ((z0, True), (z0 + 7, False)):
        post(x0 - 3, z + 1, "Generators among blast furnaces" if hot else "The same, cold")
        for i, (grade, short) in enumerate(GRADES):
            x = x0 + 5 * i
            put(x, Y, z, "create:creative_motor[facing=south]{ScrollValue:256}")
            put(x, Y, z + 1, f'tfmg:generator[facing=north]{{components:{{"fundamentals:magnet":{{grade:"{grade}"}}}}}}')
            if hot:
                for dx, dy in ((-1, 0), (1, 0), (0, 1)):
                    put(x + dx, Y + dy, z + 1, "minecraft:blast_furnace[lit=true]")
            sign(x, Y, z - 1, f"{short}|{'hot' if hot else 'cold'}")


AGEING = ["calcium_ingot", "lanthanum_ingot", "cerium_ingot", "neodymium_ingot", "neodymium_nugget", "minecraft:copper_ingot",
          "bronze_ingot", "silver_ingot", "minecraft:iron_ingot"]
WEATHERING = [["bronze_block", "exposed_bronze_block", "weathered_bronze_block", "oxidized_bronze_block", "waxed_bronze_block"],
              ["silver_block", "tarnished_silver_block", "dulled_silver_block", "blackened_silver_block", "waxed_silver_block"]] + [
    [f"{metal}_block", f"tarnished_{metal}_block", f"corroded_{metal}_block", f"crumbled_{metal}_block"]
    for metal in ("neodymium", "praseodymium", "samarium", "dysprosium", "terbium")]


def oxidation(x0, z0):
    """The same metals in the open air, under argon, and over water; sealed canisters; blocks at every stage, and fresh ones
    left out to weather, three of them over water. Damp is a water block beside or under, so the water sits in the floor."""
    metals = [(m, 16) for m in AGEING]
    chest(x0, z0, metals)
    sign(x0, Y, z0 - 1, "In the air")
    put(x0 + 2, Y, z0, "fundamentals:inert_storage_drum{Items:[" + ",".join(f"{{Slot:{i}b,{stack(m)}}}" for i, m in enumerate(metals))
        + '],Tank:{Fluid:{id:"fundamentals:argon",amount:1000}}}')
    sign(x0 + 2, Y, z0 - 1, "Under argon")
    put(x0 + 4, Y - 1, z0, "minecraft:water")
    chest(x0 + 4, z0, metals)
    sign(x0 + 4, Y, z0 - 1, "Over water: damp")
    sealed = lambda m: ("argon_canister", 1, f'{{"minecraft:container":[{{slot:0,item:{{id:"fundamentals:{m}",count:64}}}}]}}')
    chest(x0 + 6, z0, [sealed("lanthanum_ingot"), sealed("cerium_ingot"), sealed("neodymium_ingot"), ("argon_canister", 1), ("argon_canister", 1), ("canister", 16)])
    sign(x0 + 6, Y, z0 - 1, "Argon canisters, three sealed")
    for row, blocks in enumerate(WEATHERING):
        z = z0 + 4 + 2 * row
        for i, block in enumerate(blocks):
            put(x0 + i, Y, z, ns(block))
            label(x0 + i, Y, z - 1, name(block))
    z = z0 + 5 + 2 * len(WEATHERING)
    for i, block in enumerate(["bronze_block", "silver_block", "neodymium_block"]):
        put(x0 + i, Y - 1, z, "minecraft:water")
        put(x0 + i, Y, z, ns(block))
        label(x0 + i, Y, z - 1, f"{name(block)} over water")


THERMOMETERS = ["mercury_thermometer", "bimetallic_thermometer", "type_k_thermocouple", "type_s_thermocouple"]
FURNACE = "minecraft:blast_furnace[lit=true]"


def thermometers(x0, z0):
    """Each kind on five plinths, reading the block it is mounted on. Stations are five blocks apart and thermometers five
    along a station, past the reach of each other's heat (a blast furnace reaches 3, a blaze burner 4)."""
    stations = [("Cold: blue ice", "minecraft:blue_ice", [((0, 1), "minecraft:blue_ice")]),
                ("Ambient", WALL, []),
                ("Beside three lit blast furnaces", WALL, [((-1, 0), FURNACE), ((1, 0), FURNACE), ((0, 1), FURNACE)]),
                ("On a lit blast furnace", FURNACE, []),
                ("On a superheated blaze burner", "create:blaze_burner[blaze=seething]{isCreative:1b}", [])]
    for s, (title, plinth, around) in enumerate(stations):
        z = z0 + 5 * s
        post(x0 - 3, z, title)
        for i, kind in enumerate(THERMOMETERS):
            x = x0 + 5 * i
            put(x, Y, z, plinth)
            for (dx, dz), block in around:
                put(x + dx, Y, z + dz, block)
            if kind == "mercury_thermometer" and s >= 3:
                sign(x, Y, z - 1, "Mercury boils at 357 °C: it would burst here")
                continue
            put(x, Y + 1, z, f"fundamentals:{kind}[facing=up]")
            sign(x, Y, z - 1, name(kind))


def corrosion_loop(x0, z0, fluid, copper):
    """A tank pumped round a loop of pipe into a second tank and back: east along z0, west along z0 + 2. A pipe joins every
    pipe beside it whatever its placed state says, so the two runs keep a row of air between them."""
    pipe, pump_block, tank_block = ("create:fluid_pipe", "create:mechanical_pump", "create:fluid_tank") if copper else (LIQUOR_PIPE, LIQUOR_PUMP, LIQUOR_TANK)
    put(x0, Y, z0, tank_block)
    fills.append(f'data merge block {x0} {Y} {z0} {{TankContent:{{Fluid:{{id:"{ns(fluid)}",amount:8000}}}}}}')
    pump(x0 + 1, Y, z0, "east", "x", (x0, Y + 1, z0), "east", pump_block)
    for x in range(x0 + 2, x0 + 5):
        put(x, Y, z0, f"{pipe}[east=true,west=true]")
    put(x0 + 5, Y, z0, tank_block)
    for x in (x0, x0 + 5):
        put(x, Y, z0 + 1, f"{pipe}[north=true,south=true]")
    put(x0 + 5, Y, z0 + 2, f"{pipe}[north=true,west=true]")
    put(x0 + 4, Y, z0 + 2, f"{pipe}[east=true,west=true]")
    pump(x0 + 3, Y, z0 + 2, "west", "x", (x0 + 4, Y + 1, z0 + 2), "west", pump_block)
    for x in (x0 + 2, x0 + 1):
        put(x, Y, z0 + 2, f"{pipe}[east=true,west=true]")
    put(x0, Y, z0 + 2, f"{pipe}[east=true,north=true]")
    for x, z, facing in ((x0 + 1, z0, "east"), (x0 + 3, z0 + 2, "west")):
        fills.extend([f"setblock {x} {Y} {z} minecraft:air", f"setblock {x} {Y} {z} {pump_block}[facing={facing}]"])
    sign(x0 - 1, Y, z0, f"{name(fluid)} in {'copper' if copper else 'plastic'}")


def seawater(x0, z0):
    """Seawater is no block of its own, so it stands in tanks. A creative-heated basin under a mixer boils seawater pumped
    into it down to salt and bittern; it keeps both, so it stops once its output is full."""
    for i, fluid in enumerate(["seawater", "bittern", "bromine"]):
        tank(x0 + 2 * i, Y, z0, fluid)
        sign(x0 + 2 * i, Y, z0 - 1, name(fluid))
    chest(x0 + 6, z0, [("seawater_bucket", 1), ("bromine_bucket", 1), ("salt", 64), ("raw_halite", 64), ("halite_ore", 16),
                       ("magnesium_chloride", 16), ("raw_ion_adsorption_clay", 16)])
    sign(x0 + 6, Y, z0 - 1, "Salt, halite, bittern products")
    z = z0 + 4
    put(x0, Y, z, WALL)
    tank(x0, Y + 1, z, "seawater")
    put(x0 + 1, Y, z, WALL)
    pump(x0 + 1, Y + 1, z, "east", "x", (x0, Y + 2, z), "east")
    put(x0 + 2, Y, z, "create:blaze_burner[blaze=kindled]{isCreative:1b}")
    put(x0 + 2, Y + 1, z, "create:basin[facing=down]")
    put(x0 + 2, Y + 3, z, "create:mechanical_mixer")
    put(x0 + 2, Y + 3, z + 1, "create:cogwheel[axis=y]")
    put(x0 + 2, Y + 4, z + 1, f"create:creative_motor[facing=down]{MOTOR}")
    fills.extend([f"setblock {x0 + 1} {Y + 1} {z} minecraft:air", f"setblock {x0 + 1} {Y + 1} {z} {LIQUOR_PUMP}[facing=east]"])
    sign(x0 + 3, Y, z - 1, "Seawater boiled to salt and bittern")
    for row, fluid in enumerate(["seawater", "hydrochloric_acid"]):
        for col, copper in enumerate((True, False)):
            corrosion_loop(x0 + 1 + 9 * col, z0 + 9 + 5 * row, fluid, copper)


def cracking(x, z):
    """Heavy oil cracked over lanthanum oxide in a firebrick vat with an industrial mixer, as the plant's reduction cell: the oil
    pumped in from the east, the catalyst hoppered in, the coke dust taken out into a chest. Gasoline and propylene stay in
    the vat, and the catalyst it gives back fills its output, so it runs until either is full."""
    vat(x, z, [((1, 0), "tfmg:coal_coke_dust")])
    put(x, Y + 3, z, 'tfmg:industrial_mixer{MixerMode:"mixing"}')
    put(x, Y + 4, z, f"create:creative_motor[facing=down]{MOTOR}")
    feed(x + 1, Y + 3, z, "lanthanum_oxide")
    tank(x + 3, Y + 2, z + 1, "tfmg:heavy_oil", block="create:fluid_tank")
    pump(x + 2, Y + 2, z + 1, "west", "x", (x + 3, Y + 3, z + 1), "west", "create:mechanical_pump")
    fills.extend([f"setblock {x + 2} {Y + 2} {z + 1} minecraft:air", f"setblock {x + 2} {Y + 2} {z + 1} create:mechanical_pump[facing=west]"])
    sign(x, Y, z - 1, "Fluid catalytic cracking")
    sign(x + 1, Y, z - 1, "Heavy oil over lanthanum oxide")


PIGMENTS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
            "brown", "green", "red", "black"]


def plastics(x0, z0):
    post(x0 - 2, z0, "Plastic blocks: natural, then dyed")
    put(x0, Y, z0, "tfmg:plastic_block")
    for i, colour in enumerate(PIGMENTS):
        put(x0 + 1 + i, Y, z0, f"fundamentals:{colour}_plastic_block")
    post(x0 - 2, z0 + 3, "Dyed plastic pipe")
    for i, colour in enumerate(PIGMENTS):
        put(x0 + 1 + i, Y, z0 + 3, f"fundamentals:dyed_plastic_pipe[color={colour},north=true,south=true]")
    post(x0 - 2, z0 + 6, "Plastic tanks")
    for i, pigment in enumerate(["none", "white", "red", "yellow", "green", "blue", "black"]):
        put(x0 + 2 * i, Y, z0 + 6, f"fundamentals:plastic_fluid_tank[color={pigment}]")
    post(x0 - 2, z0 + 9, "Plastic pipe, pump, valve")
    put(x0, Y, z0 + 9, LIQUOR_TANK)
    pump(x0 + 1, Y, z0 + 9, "east", "x", (x0, Y + 1, z0 + 9), "east")
    put(x0 + 2, Y, z0 + 9, f"{LIQUOR_PIPE}[east=true,west=true]")
    put(x0 + 3, Y, z0 + 9, "tfmg:glass_plastic_pipe[axis=x]")
    put(x0 + 4, Y, z0 + 9, "tfmg:plastic_fluid_valve[facing=east]")
    put(x0 + 5, Y, z0 + 9, "tfmg:plastic_smart_fluid_pipe[face=floor,facing=east]")
    put(x0 + 6, Y, z0 + 9, LIQUOR_TANK)
    chest(x0 + 8, z0 + 9, [("mixer_settler", 6), ("stainless_steel_plate", 64), ("tfmg:plastic_sheet", 64), ("pvc_sheet", 64), ("create:fluid_pipe", 16),
                           ("ziegler_natta_catalyst", 16), ("pvc_resin", 16)])
    sign(x0 + 8, Y, z0 + 8, "Mixer-settler: plastic or stainless plate")


def gallery():
    ore_wall(-20, 16)
    routes(-20, 25)
    magnets(26, 24)
    oxidation(-20, 36)
    thermometers(4, 37)
    seawater(26, 37)
    cracking(-18, 66)
    plastics(-6, 66)


def main():
    lines.extend(["scoreboard players set #world showcase 1", "gamemode creative @s", "time set 6000", "weather clear",
                  "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "kill @e[type=item]"])
    head = len(lines)
    place("rare_earth_liquor", 0, 0)
    # the clarifier before the root: crude liquor pumped into a raised basin of lime under a mixer. A basin hands what it makes to
    # the block below and beside it on its facing side (the block beside it must stay clear), all at once or not at all, so it spouts into a second basin, which takes
    # both the liquor and the sludge; that one is emptied by a pump into the root's feed tank and by a hopper into a chest
    ore()
    tank(-9, Y + 1, 1)
    pump(-8, Y + 1, 1, "east", "x", (-9, Y + 2, 1), "east")
    put(-7, Y + 1, 1, f"{LIQUOR_PIPE}[east=true,west=true]")
    put(-6, Y + 1, 1, 'create:basin[facing=east]{InputItems:{Size:9,Items:[{Slot:0b,id:"tfmg:limesand",count:64}]}}')
    put(-6, Y + 3, 1, "create:mechanical_mixer")
    put(-6, Y + 3, 2, "create:cogwheel[axis=y]")
    put(-6, Y + 4, 2, f"create:creative_motor[facing=down]{MOTOR}")
    put(-6, Y, 2, 'minecraft:chest[facing=south]{Items:[{Slot:0b,id:"tfmg:limesand",count:64}]}')
    put(-5, Y, 1, "create:basin[facing=down]")
    put(-5, Y - 1, 1, "minecraft:hopper[facing=down]")
    put(-5, Y - 2, 1, "minecraft:chest")
    pump(-4, Y, 1, "east", "x", (-3, Y + 1, 1), "west")
    put(-3, Y, 1, f"{LIQUOR_PIPE}[east=true,west=true]")
    # the chests at spawn: the components to build a stage, the metals to build with, and the gallery's loose things
    chest(6, 6, [("fundamentals:mixer_settler", 64), ("create:mechanical_mixer", 16), ("create:cogwheel", 32), ("create:creative_motor", 16), ("tfmg:plastic_mechanical_pump", 16), ("create:mechanical_pump", 8),
                 ("create:fluid_pipe", 64), ("tfmg:plastic_pipe", 64), ("create:fluid_tank", 16), (LIQUOR_TANK, 16), ("create:wrench", 1), ("fundamentals:oxalic_acid", 64), ("create:basin", 4), ("create:chute", 4),
                 ("minecraft:blast_furnace", 4), ("create:depot", 4), ("minecraft:coal", 64), ("minecraft:lever", 4)])
    chest(6, 7, [("fundamentals:cobalt_ingot", 64), ("fundamentals:molybdenum_ingot", 64), ("fundamentals:rhenium_ingot", 32), ("fundamentals:tungsten_ingot", 64),
                 ("fundamentals:superalloy_plate", 32), ("fundamentals:molybdenum_steel_plate", 32), ("fundamentals:tungsten_carbide", 32), ("fundamentals:tungsten_filament", 32),
                 ("fundamentals:neodymium_iron_boron_ingot", 32), ("fundamentals:samarium_cobalt_ingot", 32), ("fundamentals:phosphor", 32), ("fundamentals:didymium_glass", 32),
                 ("fundamentals:hydrochloric_acid_bucket", 1), ("fundamentals:hydrofluoric_acid_bucket", 1), ("fundamentals:nitric_acid_bucket", 1), ("fundamentals:raw_borax", 32),
                 ("fundamentals:cerium_oxide", 32), ("fundamentals:neodymium_oxide", 32), ("fundamentals:lanthanum_ingot", 32), ("fundamentals:neodymium_ingot", 32),
                 ("fundamentals:dysprosium_ingot", 32), ("fundamentals:aluminium_scandium_plate", 32)])
    machine = lambda id, grade: (id, 1, f'{{"fundamentals:magnet":{{grade:"{grade}"}}}}')
    chest(6, 8, [("create:goggles", 1)] + [(t, 4) for t in THERMOMETERS] + [(f"{g}_magnet", 16) for g, _ in GRADES]
          + [machine("tfmg:generator", g) for g, _ in GRADES] + [machine("tfmg:electric_motor", g) for g, _ in GRADES]
          + [("argon_canister", 1), ("argon_canister", 1), ("canister", 16), ("inert_storage_drum", 4), ("seawater_bucket", 1), ("salt", 64)])
    gallery()
    # the floor, the air over it, and the chunks kept loaded, in pieces small enough for the commands
    x1, z1, x2, z2 = bounds[0] - 6, bounds[1] - 6, bounds[2] + 6, bounds[3] + 6
    prelude = []
    for z in range(z1, z2 + 1, 8):
        prelude.append(f"fill {x1} -61 {z} {x2} -61 {min(z + 7, z2)} minecraft:smooth_stone")
        for y in range(-60, -50, 2):
            prelude.append(f"fill {x1} {y} {z} {x2} {y + 1} {min(z + 7, z2)} minecraft:air")
    for x in range(x1, x2 + 1, 128):
        prelude.append(f"forceload add {x} {z1} {min(x + 127, x2)} {z2}")
    lines[head:head] = prelude
    lines.extend(["schedule function showcase:fill 10t", "schedule function showcase:refill 60t"])
    (FUNCTIONS / "stage.mcfunction").write_text("\n".join(lines) + "\n")
    (FUNCTIONS / "fill.mcfunction").write_text("\n".join(fills) + "\nscoreboard players set #world showcase 3\n")
    (FUNCTIONS / "refill.mcfunction").write_text("\n".join(refills) + "\nschedule function showcase:refill 200t\n")
    batteries = len(CUTS)
    stages = sum(c["stages"] for c in CUTS.values())
    print(f"{batteries} batteries, {stages} stages, {len(lines)} commands, plant {x1}..{x2} x {z1}..{z2}")


if __name__ == "__main__":
    main()
