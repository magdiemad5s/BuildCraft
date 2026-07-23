# NeoForge 1.20.1 lane status

Updated: 2026-07-23

| Field | Value |
|---|---|
| Loader | Transitional NeoForge `forge:1.20.1-47.1.106` |
| Java / Gradle | Java 17 / Gradle 8.8 |
| Gameplay slice | Reviewed Factory Tank, `buildcraftfactory:tank` |
| Tests | 27 passed; 0 failures, errors, or skips |
| Package check | PASS - 62 entries, 6 JSON, 8 module IDs, 8 legacy edges |
| Artifact | `buildcraft-neo-better-neoforge-1.20.1-0.1.0-dev+1.20.1.jar` |
| SHA-256 | `636BC36FFF3574922A9DE0832BD804CE043CFB150BAFCE826528B6CDA6FB1DB0` |

The Tank review corrected bounded cascading liquid/gas settling, server-only
direct fluid mutation, menu dimension/block/range validation, and the joined
model's lower face. A bounded sweep covered 2,728 modeled settling states.

No live client, server, GameTest, resource reload, multiplayer, or migration
test was run. The rest of BuildCraft remains unported; see the root status for
the complete limitations and manual release gates.
