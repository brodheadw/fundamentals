# Chemica

[Chemica](https://modrinth.com/mod/chemica) (Cosmads, MIT) is an add-on of The Factory Must Grow: about seventy fluids and
three hundred recipes run on TFMG's vats, distillation tower, hot blast stove and casting, from chlor-alkali and nitric acid to
epoxy, nylon, carbon fibre, high-octane fuels and titanium. It adds no machines of its own. **0.6.0** is the last version for
the original TFMG 1.2.0; 0.7.0 requires TFMG Community Edition 1.3.2.

Fundamentals does not need it. With both installed, `tools/build_chemica_compat.py` makes them one chemical industry:

- **(a) Unified.** Each shared reagent has a `c:` fluid tag holding both mods' fluid. Our recipes take the tag; the
  mixer-settlers, pipes, inert drum and corrosion treat Chemica's fluid as ours (`Separation.reagent`). Chemica's recipes
  that touch a shared reagent are rewritten under `data/chemica/` (behind `neoforge:mod_loaded`) to take the tag and give
  our fluid, so a base running both makes one hydrochloric acid.
- **(b) Taken over or switched off** where Chemica's chemistry contradicts ours.
- **(c) Chemica's machines:** it has none, so nothing to take.

| Chemical | Fundamentals | Chemica | More realistic | Choice |
|---|---|---|---|---|
| Hydrochloric acid | salt + sulfuric acid | hydrogen burnt in chlorine | both real | a: `c:acids/hydrochloric` |
| Nitric acid | Ostwald, ammonia over Pt-Rh gauze; nitrate + sulfuric | ammonia over a Pt carrier to NO2, then water | both; ours is one step | a: `c:acids/nitric` |
| Hydrofluoric acid | fluorspar + sulfuric acid | the same, from its own fluorite | same | a: `c:acids/hydrofluoric`; b: Chemica's off with its fluorite |
| Phosphoric acid | bone and the acid roast's by-product | phosphorus burnt, then water | both | a: `c:acids/phosphoric` |
| Sulfuric acid, hydrogen, kerosene | TFMG's | TFMG's | | already one fluid |
| Aqua regia, P204, P507, naphthenic acid, bromine | ours | none | | |
| Chlorine | chlor-alkali over a dimensionally stable anode; Mg, Ca, Li cells | brine electrolysed on plain electrodes | ours; Chemica's is the pre-1965 graphite cell | a: `c:chlorine` |
| Caustic soda | chlor-alkali; soda ash + lime | brine electrolysis | both | a: `c:caustic_soda` |
| Brine | 2 salt + 1000 mB water to 1000 mB | the same inputs to 250 mB | ours | a: `c:brine`; b: Chemica's off |
| Salt | seawater, halite | boiled out of fresh water | ours | b: Chemica's off; its recipes take `c:dusts/salt` |
| Ammonia | Haber, over magnetite | nitrogen + hydrogen in the hot blast stove, no catalyst | ours | a: `c:ammonia`; b: Chemica's off |
| Argon | air in a centrifuge vat | air distilled (with O2, N2, He) | both | a: `c:argon` |
| Titanium tetrachloride | rutile or titania slag + coke + chlorine | rutile + chlorine + hydrochloric acid, no carbon | ours: the carbon takes the oxygen | a: `c:titanium_tetrachloride`; b: Chemica's off |
| Vinyl chloride | ethylene + chlorine, gives off HCl | the same | same | a: `c:vinyl_chloride` |
| PVC | suspension resin, hot-pressed sheet | monomer over a copper catalyst, cast | ours | b: Chemica's off; its boards take `c:plates/pvc` |
| Polyethylene | TFMG's vats over our Ziegler-Natta catalyst | TiCl4 + ethylene + air or oxygen | ours: oxygen poisons the catalyst | b: Chemica's off; its uses take `c:plates/polyethylene` (TFMG's sheet) |
| PTFE | none | fluorine, persulfate initiator | Chemica's alone | its sheet builds our tanks and cells |
| Soda ash | trona | plant ash in a centrifuge | both (barilla) | its ash gives ours; its glass takes `c:dusts/soda_ash` |
| Sodium | none | salt electrolysed in water | the Downs cell: molten salt | b: the water taken out |

Chemica's vat recipes spell some keys in camelCase (`heatRequirement`, `allowedVatTypes`), which TFMG ignores; the
rewritten ones use TFMG's names, so they now ask the heat and vats Chemica meant. Chemica 0.6.0 also replaces Create's
crushing of copper, gold, iron, redstone, emerald, lead and nickel ore to add a by-product, in 1.20 Forge's format, so all
eleven fail to load and those ores stop crushing; we put back Create's recipe with Chemica's by-product added.

## Its ores and metals

Chemica's thirteen generic ores (tin, silver, platinum, cobalt, chromite, wolframite, molybdenum, rutile, fluorite, graphite,
vanadium, antimony, "phosphorus") and the invented fervorite and zelosite stones are the generic ore Fundamentals exists to
replace, so with Chemica loaded none of them generate: its three biome modifiers are overridden with `neoforge:none`. Every
recipe that takes one of its ores, raw chunks, crushed ores or rutile crystals is switched off, and its raw chunks are struck
off the `c:raw_materials` tags. What its recipes wanted from those ores comes from ours:

| Chemica wanted | Now takes | Why |
|---|---|---|
| tin, silver, platinum, cobalt, chromium, tungsten, molybdenum, titanium, magnesium, iridium ingots | `c:ingots/<metal>`, ours; its metal-producing recipes give ours | the same metal |
| those metals' nuggets, and silver and titanium sheet | `c:nuggets/*`, `c:plates/*`, ours; its packing and pressing of them switched off | ours already does it |
| its metal dusts (platinum, silver, cobalt, iridium, tin...) | its own crushing of our ingot | a ground metal |
| tungsten dust | `c:ingots/tungsten` | our APT-reduced tungsten is the powder |
| chromium dust, for chromic acid | sodium dichromate | chromic acid is made from dichromate and sulfuric acid |
| phosphorus dust, for phosphoric acid | white phosphorus | the thermal process burns P4 |
| fluorite dust, rutile | switched off: our hydrofluoric acid and titanium tetrachloride serve | |
| vanadium | new: 4 magnetite + soda ash + water, heated, to vanadium dust and crushed iron; dust + aluminium powder, superheated, to the ingot | titanomagnetite salt roast (Highveld); aluminothermic reduction |
| tantalum | new: 2 euxenite dust + hydrofluoric acid to tantalum dust (1 in 4); dust + sodium under argon, superheated, to the ingot | HF digestion of the niobate-tantalate; sodium reduction |
| graphite | new: 2 coke in an arc vat (three graphite electrodes) to graphite flakes | Acheson's synthetic graphite |
| antimony | nothing: its only use was a dopant, and ours is phosphorus | |

Chemica's p-type silicon took iridium, which is no dopant; ours (boron, from boric acid) and our n-type (phosphorus) replace
TFMG's recipes at the same path and load after Chemica's. Its copper ore crushing gave arsenic; vanilla copper ore is native
copper here, which carries silver, so it gives a silver nugget. A reachability check over Create, TFMG, Chemica and ours
with Chemica loaded leaves no recipe whose input only Chemica's ores could supply.

Testing with Chemica: `./gradlew -Pchemica runServer`, or `ORG_GRADLE_PROJECT_chemica=1 tools/gametest.sh`.
