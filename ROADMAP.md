# Roadmap

## Multi-version strategy

Target **1.21.1 first**, then extend forward to every later Minecraft version. Thanks to
Stonecutter, adding a version is additive — the shared source tree stays single-copy and
version differences are handled with `//? if >=1.21.3 {` style comments.

### Adding a Minecraft version

1. In `settings.gradle.kts`, add the version to the `mc(...)` calls:
   ```kotlin
   mc("1.21.1", listOf("fabric", "neoforge"))
   mc("1.21.3", listOf("fabric", "neoforge"))   // new
   ```
2. Create `versions/1.21.3-fabric/gradle.properties` and
   `versions/1.21.3-neoforge/gradle.properties` with that version's
   `mod.mc_version`, `mod.mc_dep`, `deps.fabric_api_version` / `deps.neoforge_version`,
   and `deps.parchment_version`.
3. `./gradlew "Reset active project"` then build/test the new node.

Planned ladder (confirm exact dependency versions per target as they are added):
`1.21.1` → `1.21.3` → `1.21.4` → `1.21.5` → `1.21.6` → … and onward.

## Loaders

- **Fabric** and **NeoForge** are first-class.
- **Forge** is intentionally *not* targeted for 1.21+: it was effectively superseded by
  NeoForge after 1.20.1. Revisit only if a concrete need appears.

## Feature direction (content)

1. **Materials as data** — element/material registry with properties (strength, heat resistance, …).
2. **Processing framework** — multi-stage chains (ore → concentrate → element → product), tag-driven.
3. **Tag interop** — expose `c:ingots/tungsten`, `c:rare_earths/*`, etc. so other mods consume outputs
   without knowing the source.
4. **Create integration** (optional dependency) — express chains as Create processing
   (crushing, mixing, pressing, mechanical crafting) when Create is present.
5. **Material-property machines** — e.g. magnet strength read from material stats; leave an API seam
   for a future field simulation.

## Housekeeping

- [ ] Add a mod icon (`src/main/resources/assets/fundamentals/icon.png`) and re-add the
      `icon` / `logoFile` references in `fabric.mod.json` / `neoforge.mods.toml`.
- [ ] Decide on and wire up a publishing pipeline (Modrinth/CurseForge) if releasing.
- [ ] Mirror this multi-version + multi-loader setup in the **wildspell** repos
      (see `NOTE-wildspell.md`).
