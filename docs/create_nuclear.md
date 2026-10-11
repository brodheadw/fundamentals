# Create: Nuclear

[Create: Nuclear](https://modrinth.com/mod/createnuclear) (the Create Nuclear Team, all rights reserved) adds a fission reactor to
Create: a multiblock of reactor casing, frames, coolers and a core, run by a controller that reads a blueprint of rod slots, fed
fuel and control rods through rod inputs and water or liquid nitrogen through a fluid input, and turning its heat into rotation at
its outputs. Fuel is the uranium rod and the thorium rod, which heat; the graphite rod cools. Overheated, it melts down and leaves
an irradiated zone. Items carry a radiation dose; lead-and-brass anti-radiation armour and reinforced (leaded) glass shield.
Uranium is "enriched" as yellowcake on a fan blowing through enriching fire, from soul soil and a nether star. **2.0.0** is
the NeoForge 1.21.1 build; it needs Create 6.0.8 or newer, and takes JEI, EMI or REI optionally.

Fundamentals does not need it. With both installed, `tools/build_create_nuclear_compat.py` makes its reactor burn our fuel.
Everything below is behind `neoforge:mod_loaded createnuclear`.

| Part | Create: Nuclear | Fundamentals | Choice |
|---|---|---|---|
| Uranium ore | a generic ore, and in a striated layer with autunite | uraninite, in calcite veins with silver and cobalt and in sandstone roll-fronts | its ore off; the layer keeps its autunite, limestone and calcite |
| Lead, thorium, nitrate ores | generic ores | galena; monazite's thorium residue; The Factory's saltpetre | off |
| Uranium processing | crushed to powder, stirred to liquid uranium, pressed to yellowcake | ground, acid-leached with pyrolusite, yellowcake from the liquor with ammonia | its chain off |
| Enrichment | yellowcake through enriching fire on a fan | hexafluoride in The Factory's centrifuge vat: 1,000 mB to 250 enriched and 750 depleted | its fan recipe off |
| Fuel rod | five enriched yellowcake | four uranium dioxide pellets in a Zircaloy tube (Zr with about 1.4% Sn) | ours |
| Thorium rod | five thorium ingots | two thoria and two enriched pellets in a Zircaloy tube: thorium is fertile and needs a driver | ours |
| Cooling ("graphite") rod | steel and graphene pressed out of coal dust | a control rod: four hafnium ingots, as in a naval reactor, or four boron carbide in eight stainless steel plates for two | ours, and named Control Rod |
| Reactor core | a bucket of liquid uranium in the middle | a beryllium ingot: the reflector | ours |
| Steel | iron and coal dust stirred cold | The Factory's, from its blast furnace | its steel off; its steel and lead packing off |
| Thorium metal | its thorium ore | thoria and two calcium under argon, superheated | ours |
| Liquid nitrogen | saltpetre baked, chilled on a fan and stirred with ice | 1,000 mB of air pressed with packed ice to 100 mB | ours |
| Radiation | its own items | uraninite, its dust, yellowcake, green salt, the oxides, pellets, thoria and thorium residue count on its scale (raw 3, yellowcake 4, dust and oxides 2, depleted and thoria 1) | linked through its `RadiationRegistry` |
| Reactor casing, frame, cooler, controller, glass, armour | steel, lead, brass, glass | our and The Factory's steel and lead by `c:` tag | kept |

It has no moderator or reflector block, so beryllium goes into the core and graphite into nothing; graphene, which it pressed out
of coal dust, is no longer made.

## Its ores and raw items

Its four biome modifiers (uranium, lead, thorium, nitrate) are overridden with `neoforge:none`, and its striated layer is rewritten
without the uranium ore. Already in a world, its ores mine as ours: uranium ore as raw uraninite, lead ore as raw galena, thorium ore
as raw monazite, nitrate ore as two saltpetre. Its raw uranium, lead and thorium, and their blocks, craft into one or nine of ours,
and are struck off the `c:raw_materials` tags. Every recipe of its that took its ores, raw items, powder, dust or saltpetre is off
(44 in all, with the ones above), among them granite crushed to uranium powder, limestone crushed to saltpetre and lead, and a lead
nugget from crushing raw copper or zinc. A reachability check over Create, The Factory Must Grow, Create: Nuclear and ours with it
loaded leaves no recipe of its that needs an input only its ores could supply.

Testing with Create: Nuclear: `./gradlew -Pcreatenuclear runServer`, or `ORG_GRADLE_PROJECT_createnuclear=1 tools/gametest.sh`.
