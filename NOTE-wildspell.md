# Note: apply the same multi-version / multi-loader setup to wildspell

The **wildspell** repos should be updated to use the same structure as this project:

- **Stonecutter** for multi-version support (one shared source tree, `//? if` preprocessing).
- **Architectury Loom** for multi-loader builds.
- Target **Fabric + NeoForge**, starting at **Minecraft 1.21.1**, then forward to every
  later version — the same ladder Fundamentals follows.

This keeps both projects on an identical build/versioning model, which makes any future
interoperation (shared tags, optional cross-mod integration) much simpler.

> Action item carried in `ROADMAP.md`. The wildspell repos were not found on this machine
> at project-creation time, so this note lives here until it can be copied over.
