# NeoForge 1.20.1 lane status

Updated: 2026-07-23

| Field | Value |
|---|---|
| Loader | Transitional NeoForge `forge:1.20.1-47.1.106` |
| Java / Gradle | Java 17 / Gradle 8.8 |
| Gameplay slice | Reviewed Factory Tank, `buildcraftfactory:tank`, plus the bounded 1.20.1 energy foundation |
| Tests | 29 passed; 0 failures, errors, or skips |
| Package checks | PASS - 70 entries, 6 JSON, 4 Tank PNGs, 8 module IDs, and 8 legacy dependency edges |
| Artifact | `buildcraft-neo-better-neoforge-1.20.1-0.1.1-dev+1.20.1.jar` |
| SHA-256 | `6D3694B705E72582DA0A7AF7149394335561B6441EF7705D8FCCA3CC0C1B8C63` |

The Tank review corrected bounded cascading liquid/gas settling, server-only
direct fluid mutation, menu dimension/block/range validation, and the joined
model's lower face. Visual parity now also restores `RenderShape.MODEL`, the
legacy alpha-cutout Tank models/textures, all legacy item display transforms,
the cutout render layer, and the original `textures/gui/tank.png` base and
gauge overlay.

`build` now executes the Tank visual verifier against the packaged JAR. It
checks source/JAR model contracts, PNG headers and dimensions, and rejects
temporary recovery files from the archive.

No live client, server, GameTest, resource reload, multiplayer, or migration
test was run. The rest of BuildCraft remains unported; see the root status for
the complete limitations and manual release gates.
