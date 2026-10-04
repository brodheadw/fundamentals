# Fundamentals

A chemistry-driven **materials and processing framework** for Minecraft. Built as a
standalone, multi-loader, multi-version project.

> Separate from the **wildspell** project. The two may interoperate later (shared tags,
> optional integration), but they are developed and versioned independently.

> 📋 **Working on this repo? Read [`docs/PLAN.md`](docs/PLAN.md) first** — it's the shared
> source of truth for architecture, who owns which ores, the conventions that keep parallel
> work compatible, and the biome/worldgen scheme. Ore data lives in
> [`data/ores.json`](data/ores.json) / [`docs/ore-catalog.md`](docs/ore-catalog.md).

## Design direction

Fundamentals treats materials as data, not hardcoded items:

- **Processing chains**, not one-step magic — e.g. ore → concentrate → element → product
  (bastnäsite → flotation → mixed rare-earth concentrate → solvent extraction → oxides → metal;
  wolframite → APT → WO₃ → tungsten metal).
- **Tag-driven interop** — other mods ask for `tungsten` and don't care what item provided it.
- **Material properties as stats** — e.g. magnet strength is a material property (NdFeB vs SmCo),
  so recipes output magnets with different stats and machines can read them. Leaves room for a
  real field simulation later without blocking on it.

Start small (materials + processing), layer the richer simulation on top.

## Supported targets

| Minecraft | Fabric | NeoForge |
|-----------|:------:|:--------:|
| 1.21.1    | ✅     | ✅       |

More versions are planned — see [ROADMAP.md](ROADMAP.md).

## Project layout

```
build.gradle.kts            # Shared build logic, applied to every version×loader node
settings.gradle.kts         # Stonecutter target registration + plugin repos
stonecutter.gradle.kts      # Stonecutter controller (active version, chiseledBuild)
gradle.properties           # Mod identity + shared dependency versions
gradle/libs.versions.toml   # Plugin version catalog
versions/<mc>-<loader>/     # Per-target dependency versions (Fabric API, NeoForge, Parchment…)
src/main/java/              # Shared source; loader differences via Stonecutter //? comments
src/main/resources/         # fabric.mod.json, META-INF/neoforge.mods.toml, mixins, assets
```

This is a **single shared source set**. Loader-specific code is selected at build time with
Stonecutter preprocessor comments:

```java
//? if fabric {
import net.fabricmc.api.ModInitializer;
//?}
//? if neoforge {
/*import net.neoforged.fml.common.Mod;
*///?}
```

## Building

Requires JDK 21 (auto-provisioned by Gradle's toolchain resolver).

```bash
# Build the currently active target (see stonecutter.gradle.kts)
./gradlew build

# Switch the active target
./gradlew "Set active project to 1.21.1-neoforge"

# Build every registered target
./gradlew chiseledBuild
```

Output jars land in each node's `versions/<target>/build/libs/`.

## Stack

- [Architectury Loom](https://github.com/architectury/architectury-loom) — multi-loader build, Mojang mappings
- [Stonecutter](https://stonecutter.kikugie.dev) — multi-version preprocessing from one source tree
- [Parchment](https://parchmentmc.org) — parameter names + javadoc on top of Mojmap

## License

MIT — see [LICENSE](LICENSE).
