# Note: apply the same multi-version / multi-loader setup to wildspell

The **wildspell** repos should be updated to use the same structure as this project:

- **Stonecutter** for multi-version support (one shared source tree, `//? if` preprocessing).
- **Architectury Loom** for multi-loader builds.
- Target **Forge + NeoForge**, not Fabric (Will, 2026-10-08: "All of these mods should be forge/neoforge
  enabled"). Create and TFMG ship NeoForge only on 1.21.1 and Forge only up to 1.20.1, so Forge means
  adding 1.20.1 to the version ladder.

This keeps both projects on an identical build/versioning model, which makes any future
interoperation (shared tags, optional cross-mod integration) much simpler.

> **Status (2026-10-04):** tracking issues have been filed in each repo:
> - `wildspell-mobs` → [#13](https://github.com/brodheadw/wildspell-mobs/issues/13)
> - `wildspell-magic` → [#5](https://github.com/brodheadw/wildspell-magic/issues/5)
> - `fundamental-magic` → [#2](https://github.com/brodheadw/fundamental-magic/issues/2)
> - `wildspell-modpack` → [#3](https://github.com/brodheadw/wildspell-modpack/issues/3) (multi-version only; multi-loader N/A for a modpack)
