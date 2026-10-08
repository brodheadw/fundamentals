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
- **Arsenic and chlorine.** Roasting cobaltite really gives off arsenic trioxide, and the molten-chloride electrolyses
  (calcium, lithium) chlorine. Neither is an item or a fluid yet, and a campfire roast has only one output.

## Pack and tooling

- **Fundamentals in the Wildspell pack.** Not in it yet; adding it brings TFMG in. Will's call.
- **Full-pack dev client.** Loading all of the pack's jars into the magic dev client crashed in the Aether's renderer
  registration on a mixin error (2026-10-08). The clean route is the real Modrinth profile with Fundamentals and TFMG
  added, not a dev client pretending to be the pack.
- **Modrinth gallery.** Still shows the old three-battery picture; the fourteen-battery tree deserves a shot.
