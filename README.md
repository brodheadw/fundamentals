# Fundamentals

A chemistry-driven **materials and processing** add-on for [Create](https://github.com/Creators-of-Create/Create),
on NeoForge.

> Separate from the **wildspell** project. The two may interoperate later (shared tags,
> optional integration), but they are developed and versioned independently.

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

| Minecraft | Loader | Requires |
|-----------|--------|----------|
| 1.21.1    | NeoForge 21.1.219+ | Create 6.0.10+ |

Fabric is not supported: Create has no Fabric release for 1.21.1.

## Project layout

```
build.gradle.kts            # Shared build logic, applied to every version node
settings.gradle.kts         # Stonecutter target registration + plugin repos
stonecutter.gradle.kts      # Stonecutter controller (active version)
gradle.properties           # Mod identity
versions/<mc>-neoforge/     # Per-version dependency versions (NeoForge, Create, Parchment)
src/main/java/              # Source; version differences via Stonecutter //? comments
src/main/resources/         # META-INF/neoforge.mods.toml, mixins, assets, data
tools/                      # Generators for ore textures and ore data (see PLAN §6)
```

## Building

Requires JDK 21 (auto-provisioned by Gradle's toolchain resolver).

```bash
# Build every registered target
./gradlew buildAll
```

Output jars land in each node's `versions/<target>/build/libs/`.

## Stack

- [Architectury Loom](https://github.com/architectury/architectury-loom) — NeoForge build, Mojang mappings
- [Stonecutter](https://stonecutter.kikugie.dev) — multi-version preprocessing from one source tree
- [Parchment](https://parchmentmc.org) — parameter names + javadoc on top of Mojmap

## License

MIT — see [LICENSE](LICENSE).
