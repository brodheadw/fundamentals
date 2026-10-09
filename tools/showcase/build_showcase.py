#!/usr/bin/env python3
"""Writes the showcase datapack's functions: the whole separation tree, read from the cut recipes, laid out as
batteries on a flat world. The root battery parts the mixed liquor; each product is either the feed of the next
battery (piped north to it) or a single element, which gets a station that precipitates the oxalate, smelts it and
sets the oxide on a depot. Every battery has its mixers, lever, organic feed, acid feed, sump drain and side tanks,
every end tank is filled and refilled, and the whole plant is force-loaded so it runs while you walk it.

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

lines, fills, refills = [], [], []
bounds = [0, 0, 0, 0]   # min x, min z, max x, max z, grown as blocks are placed


def put(x, y, z, block):
    lines.append(f"setblock {x} {y} {z} {block}")
    bounds[0] = min(bounds[0], x); bounds[1] = min(bounds[1], z); bounds[2] = max(bounds[2], x); bounds[3] = max(bounds[3], z)


def tank(x, y, z, fluid=None, amount=8000):
    put(x, y, z, "create:fluid_tank")
    if fluid:
        merge = f'data merge block {x} {y} {z} {{TankContent:{{Fluid:{{id:"fundamentals:{fluid}",amount:{amount}}}}}}}'
        fills.append(merge)
        refills.append(f"execute unless data block {x} {y} {z} TankContent.Fluid run {merge}")


def pump(x, y, z, facing, axis, motor_at, motor_facing):
    """A pump with the cogwheel above it on its axis and a motor beside the cog."""
    put(x, y, z, f"create:mechanical_pump[facing={facing}]")
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
    tank(x0 - 2, Y + 3, z0 + 2, cut["organic"])
    pump(x0 - 1, Y + 3, z0 + 2, "east", "x", (x0 - 2, Y + 4, z0 + 2), "east")
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
    put(x0, Y - 1, z0, "create:mechanical_pump[facing=down]")
    put(x0 + 1, Y - 1, z0, "create:cogwheel[axis=y]")
    put(x0 + 1, Y - 2, z0, f"create:creative_motor[facing=up]{MOTOR}")
    put(x0, Y - 2, z0, "create:fluid_tank")
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


def main():
    lines.extend(["scoreboard players set #world showcase 1", "gamemode creative @s", "time set 6000", "weather clear",
                  "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "kill @e[type=item]"])
    head = len(lines)
    place("rare_earth_liquor", 0, 0)
    # the clarifier before the root: crude liquor pumped into a raised basin of lime under a mixer. A basin hands what it makes to
    # the block below and beside it on its facing side (the block beside it must stay clear), all at once or not at all, so it spouts into a second basin, which takes
    # both the liquor and the sludge; that one is emptied by a pump into the root's feed tank and by a hopper into a chest
    tank(-9, Y + 1, 1, "crude_rare_earth_liquor")
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
    # the chests at spawn: the components to build a stage, and the metals to build with
    chest = lambda x, z, items: put(x, Y, z, "minecraft:chest[facing=north]{Items:[" + ",".join(f'{{Slot:{i}b,id:"{it}",count:{n}}}' for i, (it, n) in enumerate(items)) + "]}")
    chest(6, 6, [("fundamentals:mixer_settler", 64), ("create:mechanical_mixer", 16), ("create:cogwheel", 32), ("create:creative_motor", 16), ("create:mechanical_pump", 16),
                 ("create:fluid_pipe", 64), ("tfmg:plastic_pipe", 64), ("create:fluid_tank", 16), ("create:wrench", 1), ("fundamentals:oxalic_acid", 64), ("create:basin", 4), ("create:chute", 4),
                 ("minecraft:blast_furnace", 4), ("create:depot", 4), ("minecraft:coal", 64), ("minecraft:lever", 4)])
    chest(6, 7, [("fundamentals:cobalt_ingot", 64), ("fundamentals:molybdenum_ingot", 64), ("fundamentals:rhenium_ingot", 32), ("fundamentals:tungsten_ingot", 64),
                 ("fundamentals:superalloy_plate", 32), ("fundamentals:molybdenum_steel_plate", 32), ("fundamentals:tungsten_carbide", 32), ("fundamentals:tungsten_filament", 32),
                 ("fundamentals:neodymium_iron_boron_ingot", 32), ("fundamentals:samarium_cobalt_ingot", 32), ("fundamentals:phosphor", 32), ("fundamentals:didymium_glass", 32),
                 ("fundamentals:hydrochloric_acid_bucket", 1), ("fundamentals:hydrofluoric_acid_bucket", 1), ("fundamentals:nitric_acid_bucket", 1), ("fundamentals:raw_borax", 32),
                 ("fundamentals:cerium_oxide", 32), ("fundamentals:neodymium_oxide", 32), ("fundamentals:lanthanum_ingot", 32), ("fundamentals:neodymium_ingot", 32),
                 ("fundamentals:dysprosium_ingot", 32), ("fundamentals:aluminium_scandium_plate", 32)])
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
