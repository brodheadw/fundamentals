# Roadmap

## Multi-version strategy

Target **1.21.1 first**, then extend forward to every later Minecraft version. Thanks to
Stonecutter, adding a version is additive — the shared source tree stays single-copy and
version differences are handled with `//? if >=1.21.3 {` style comments.

### Adding a Minecraft version

1. In `settings.gradle.kts`, add the version to the `mc(...)` calls:
   ```kotlin
   mc("1.21.1", listOf("neoforge"))
   mc("1.21.3", listOf("neoforge"))   // new
   ```
2. Create `versions/1.21.3-neoforge/gradle.properties` with that version's
   `mod.mc_version`, `mod.mc_dep`, `deps.neoforge_version`, `deps.create_version` and
   `deps.parchment_version`. A version can only be added once Create supports it.
3. `./gradlew "Reset active project"` then build/test the new node.

Planned ladder (confirm exact dependency versions per target as they are added):
`1.21.1` → `1.21.3` → `1.21.4` → `1.21.5` → `1.21.6` → … and onward.

## Loaders

**NeoForge only.** Fundamentals is a Create add-on, and Create's 1.21.1 release is NeoForge-only.
Revisit Fabric if Create ships a Fabric build for a version we target.

## Feature direction (content)

1. **Materials as data** — element/material registry with properties (strength, heat resistance, …).
2. **Processing framework** — multi-stage chains (ore → concentrate → element → product), tag-driven.
3. **Tag interop** — expose `c:ingots/tungsten`, `c:rare_earths/*`, etc. so other mods consume outputs
   without knowing the source.
4. **Create integration** (required) — chains are built from Create processing (crushing, mixing,
   pressing, mechanical crafting) plus our own machines where Create has none.
5. **Material-property machines** — e.g. magnet strength read from material stats; leave an API seam
   for a future field simulation.

## Housekeeping

- [x] Add a mod icon (`src/main/resources/assets/fundamentals/icon.png`) and the `logoFile` reference.
- [ ] Decide on and wire up a publishing pipeline (Modrinth/CurseForge) if releasing.
- [x] Mirror this multi-version + multi-loader setup in the **wildspell** repos —
      tracking issues filed (see `NOTE-wildspell.md`).
