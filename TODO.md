# TODO

What's agreed but not built, in the order it's likely to go. The working plan behind all of it is `docs/PLAN.md`.

## Rare earth plant

- **Scrub section.** Real batteries have extraction, scrubbing and stripping sections, with a dilute-acid scrub feed in the
  middle; we fold the scrub into the stage count. Modelling it means a third feed port per battery and a scrub acid.
  Deferred by Will on 2026-10-08.
- **Thorium handling.** Carrying thorium residue should slowly hurt (a radiation effect), and a lead-lined cask should
  carry it safely. Deferred the same day.
- **Storage under argon / tarnish.** Lanthanum, cerium, europium and neodymium oxidise in air. Chests don't tick their
  contents, so this only works as a sealed canister item (argon-flushed) plus tarnish in the player's inventory and on
  the ground. Design open since the oxide-to-metal work.
- **Temperature consumers.** `Heat.at` exists (the `heat` package). Still to hang on it: the tiers Create's recipes use
  as thresholds on the number, tarnish rate, and kerosene and the extractants igniting near heat. The Wildspell Magic
  side is wildspell-magic#7 (Freezing Grasp and Noon push on it, and spells read it back).
- **Gadolinium** has no sink: its real uses are neutron absorption and MRI contrast and the pack has neither. Holmium,
  thulium, ytterbium and lutetium likewise beyond the glass colours.

## Other metals

- **Tin.** Cassiterite generates but ends as ore; bronze is the natural sink. Zinc now reaches Create's zinc ingot (and so
  its brass), but Create's own zinc ore still generates and smelts in a furnace.
- **Platinum group.** The layered intrusion carries sperrylite, cooperite and braggite; the materials are declared
  (`PreciousMaterials`) but not items. Catalysts (reforming, the catalytic converter alongside ceria) are the sinks.
- **Cobalt blue** uses roasted cobaltite directly; a cobalt oxide form would be cleaner.
- **Arsenic.** Roasting cobaltite really gives off arsenic trioxide. It is not an item yet, and a campfire roast has only
  one output. (Chlorine, the other missing by-product, now comes off the molten-chloride electrolyses.)
- **Chromium loose ends.** Ferrochrome smelts in a superheated basin; The Factory Must Grow's arc furnace (a firebrick vat on
  three graphite electrodes) is the truer submerged-arc furnace but its vat needs yttria here, so it waits. The chromate
  leach folds into the roast, the dichromate step makes no sodium sulfate, and electrolytic chromium is not modelled.
  UG2 chromite carries the platinum metals, which should come off the wash once they are items.

## Reagents

- **Nitric acid by Ostwald.** Ammonia burnt over platinum gauze is how nitric acid has been made since 1910; it waits on
  the platinum group being items. The saltpetre retort stays as the early route.
- **Phosphoric acid has no sink** now that the extractants are made from phosphorus trichloride. Its real ones are
  fertiliser and phosphating steel; the old route to phosphorus, phosphoric acid distilled with charcoal, is another.
- **White phosphorus** ignites in air at about 30 °C and is kept under water. It is an inert item for now.
- **Chlorine and phosphorus trichloride** are pipe-only fluids, so they neither fume nor poison. If they ever stand in
  the world they belong with hydrofluoric and nitric acid in the fume handling.

## Pack and tooling

- **Fundamentals in the Wildspell pack.** Not in it yet; adding it brings TFMG in. Will's call.
- **Full-pack dev client.** Loading all of the pack's jars into the magic dev client crashed in the Aether's renderer
  registration on a mixin error (2026-10-08). The clean route is the real Modrinth profile with Fundamentals and TFMG
  added, not a dev client pretending to be the pack.
- **Modrinth gallery.** Still shows the old three-battery picture; the fourteen-battery tree deserves a shot.
