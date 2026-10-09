# TODO

What's agreed but not built, in the order it's likely to go. The working plan behind all of it is `docs/PLAN.md`.

## Rare earth plant

- **Scrub section.** Real batteries have extraction, scrubbing and stripping sections, with a dilute-acid scrub feed in the
  middle; we fold the scrub into the stage count. Modelling it means a third feed port per battery and a scrub acid.
  Deferred by Will on 2026-10-08.
- **Thorium handling.** Carrying thorium residue should slowly hurt (a radiation effect), and a lead-lined cask should
  carry it safely. Deferred the same day.
- **Oxidation loose ends.** Vanilla's iron block and the Factory's steel block don't rust (Create's weathered iron block
  is the obvious last stage); NdFeB, which really corrodes unless nickel-plated, doesn't age; an aged stack shows its stage
  only in the tooltip, not its texture; items carried by hoppers or sitting in Create's vaults and toolboxes age only
  when a player opens a container they land in; there is no desiccant; and calcium and lithium tarnish but never crust
  through, since nothing here holds the crust.
- **Magnet loose ends.** The electric pump and the voltmeter take any magnet but ignore its grade and the heat, and so does the
  magnetomigration cell; and a motor or generator rebuilt in the grid takes one magnet where the belt took three. Alnico is one cast-and-treated step with no field anneal of its own, sintered NdFeB has no pressing or
  sintering step, and grain-boundary diffusion (terbium on a finished magnet) is folded into the melt.
- **Temperature consumers.** `Heat.at` exists (the `heat` package). Still to hang on it: the tiers Create's recipes use
  as thresholds on the number, tarnish rate, and kerosene and the extractants igniting near heat. The Wildspell Magic
  side is wildspell-magic#7 (Freezing Grasp and Noon push on it, and spells read it back).
- **Thermometer loose ends.** A type K past 1,260 °C pegs where a real one drifts as its chromel oxidises, and a mercury gauge below
  −39 °C pegs where its column would freeze. The cinnabar retort gives off no SO2 (there is no fluid for it), mercury has no use
  beyond the gauge, and the gauges have no Ponder scene.
- **Hard-rock monazite washes like the sand.** Monazite from a carbonatite top or a quartz vein drops the same raw monazite
  as the beach placer and washes without grinding, as placer and vein cassiterite do. Real vein ore is crushed and milled first.
- **Gadolinium**'s real uses are neutron absorption and MRI contrast and the pack has neither; it only stands in for some
  neodymium in NdFeB. Thulium, ytterbium and lutetium oxide have no sink at all.

- **Scandium by-product routes.** Most real scandium comes off nickel laterite acid leach (HPAL) liquor and bauxite residue,
  pulled by P204 and stripped with caustic soda. Only thortveitite is modelled; laterite smelts whole and red mud does not exist.
  Thortveitite's yttrium is not recovered.
- **Magnetic separation loose ends.** The magnetomigration line hands over the battery's exact products, so the few per cent
  of diamagnetic lutetium and weak ytterbium in the late heavies go with them to the magnet, where a real cell would leave
  them behind. Its 50 mB batch is fixed and does not grow with the line, the liquor in the cells is not drawn, and it has no
  Ponder scene and no line in the showcase.

## Other metals

- **Create's zinc ore.** Zinc now reaches Create's zinc ingot (and so its brass), but Create's own zinc ore still generates
  and smelts in a furnace.
- **Tin loose ends.** The niobium, tantalum and tungsten a hard-rock tin concentrate carries are not recovered (wolframite is mined
  on its own), the hardhead liquation leaves goes to slag rather than back to the smelter, and electrolytic tin refining is not
  modelled. Placer cassiterite still wants a pickaxe like the vein ore.
- **Platinum group loose ends.** The reef's minerals join the matte at the leach rather than being floated and smelted with
  it, and the converter matte leach and nickel electrowinning each fold two refinery steps into one. The gold, silver,
  selenium and tellurium a real concentrate carries, sperrylite's arsenic, and copper's own anode slimes (the other
  by-product source) are not modelled. Osmium tetroxide is a pipe-only fluid: it neither fumes nor blinds.
- **Silver loose ends.** Every galena bullion carries the same silver; the Parkes crust is cupelled directly, its zinc burnt
  to oxide, where the works retorted the zinc off first; cupellation is a heated basin, not a reverberatory hearth; and
  Moebius electrorefining, cyanidation and the copper anode slimes are not modelled. Litharge in lead glass waits.
- **Cobalt blue** uses roasted cobaltite directly; a cobalt oxide form would be cleaner.
- **Arsenic.** Roasting cobaltite really gives off arsenic trioxide. It is not an item yet, and a campfire roast has only
  one output. (Chlorine, the other missing by-product, now comes off the molten-chloride electrolyses.)
- **Chromium loose ends.** Ferrochrome smelts in a superheated basin; The Factory Must Grow's arc furnace (a firebrick vat on
  three graphite electrodes) is the truer submerged-arc furnace but its vat needs yttria here, so it waits. The chromate
  leach folds into the roast, the dichromate step makes no sodium sulfate, and electrolytic chromium is not modelled.
  UG2 chromite carries the platinum metals, which should come off the wash into the platinum group concentrate.

- **Titanium loose ends.** Ilmenite smelts in a superheated basin rather than the Factory's arc furnace (the same yttria wait as
  ferrochrome), and the cast iron it gives is not the high-purity pig iron Sorel sells. Chlorination makes no CO, CO2 or ferric
  chloride and the tetrachloride's distillation folds into it; the chloride process burns it in air, not oxygen. The sulfate
  pigment route, the Becher and synthetic-rutile upgrades, and leucoxene are not modelled. Magnesium has no Pidgeon route
  (dolomite and ferrosilicon are not in the mod) and no sink of its own beyond the Kroll loop.
- **Titanium plant tier.** Titanium shrugs off wet chlorine and chloride brines, which is why chlor-alkali and desalination
  plants pipe them in it. A titanium pipe and tank that the liquors and acids (but not hydrofluoric) cannot corrode would be
  the metal's natural sink, but it means new pipe and tank blocks; the plastic tank covers the need for now.

## Reagents

- **Phosphoric acid has no sink** now that the extractants are made from phosphorus trichloride. Its real ones are
  fertiliser and phosphating steel; the old route to phosphorus, phosphoric acid distilled with charcoal, is another.
- **White phosphorus** ignites in air at about 30 °C and is kept under water. It is an inert item for now.
- **Chlorine, phosphorus trichloride and titanium tetrachloride** are pipe-only fluids, so they neither fume nor poison. If they ever stand in
  the world they belong with hydrofluoric and nitric acid in the fume handling.

- **Salt loose ends.** Seawater is told from fresh water by biome (ocean and beach), so a pool dug on a beach is sea and a
  lagoon in a river biome is not; it is no block of its own and pours out as water, so poured seawater still waters crops
  (seawater refuses to hydrate farmland, but never stands in the world to try). Desalination is the salt pan's condensate,
  not a membrane or a flash plant of its own. Salt domes (halite plugs under a gypsum-anhydrite cap) are not generated: halite is
  only in the evaporite beds. Bittern's potash is not drawn off, and bromine comes off it by steaming-out only (the air-blown
  process for seawater is not modelled).
- **Bromine's real sinks.** Flame retardants (half the world's bromine), calcium bromide drilling brines, ethylene dibromide for
  leaded petrol and silver bromide for film are not modelled; the halogen lamp is its one use.

## Plastics

- **Plastics loose ends.** Polyethylene and polypropylene are one molten plastic, as TFMG has it. A dyed tank or pipe breaks
  back to a plain one, a dyed pipe wrenched to glass or encased loses its dye, and tanks of different colours still join (the
  next dye colours the whole tank). The Factory's plastic block takes only the milky colour: it is a full block that hides its
  neighbours' faces, so translucent it would show holes, and making it not do so means a mixin on another mod's block. PVC's stabiliser and plasticiser are not modelled.

## Pack and tooling

- **Fundamentals in the Wildspell pack.** Not in it yet; adding it brings TFMG in. Will's call.
- **Full-pack dev client.** Loading all of the pack's jars into the magic dev client crashed in the Aether's renderer
  registration on a mixin error (2026-10-08). The clean route is the real Modrinth profile with Fundamentals and TFMG
  added, not a dev client pretending to be the pack.
- **Modrinth gallery.** Still shows the old three-battery picture; the fourteen-battery tree deserves a shot.
