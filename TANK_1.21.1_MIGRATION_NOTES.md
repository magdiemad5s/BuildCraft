# Factory Tank 1.21.1 migration record

The Factory Tank vertical slice is implemented and compiled in both requested
1.21.1 lanes. It preserves `buildcraftfactory:tank`, 16,000 mB capacity, the
`tank` NBT key, compatible reads of `tanks.tank`, vertical liquid/gas ordering,
comparator behavior, and server-authoritative menu interaction.

| Area | Forge 1.21.1 implementation | NeoForge 1.21.1 implementation |
|---|---|---|
| Block interaction | `useItemOn` / `useWithoutItem`, `ServerPlayer.openMenu`, model render shape | Same modern interaction split and menu path |
| Persistence | Holder-aware `saveAdditional` / `loadAdditional` and update tag | Holder-aware persistence with legacy fluid payload retention |
| Fluid exposure | Forge fluid capability with `LazyOptional` | NeoForge registered fluid block capability |
| Client | Tank screen and translucent render layer | Tank screen and NeoForge client registration |
| Data | Singular `loot_table/blocks`, pack formats 34/48 | Singular `loot_table/blocks`, pack formats 34/48 |

Both lanes have isolated Gradle build/package validation and pure Tank policy
tests. That proves compilation, metadata, JSON packaging, identifiers, bounds,
and ordering policies; it does **not** prove a live client, server, multiplayer,
resource reload, or legacy-world migration.

The remaining Tank release gates are bucket/container interaction in a real
game, stacked tank rendering, F3+T reload, menu synchronization in multiplayer,
and migration using a backup copy of an old world.
