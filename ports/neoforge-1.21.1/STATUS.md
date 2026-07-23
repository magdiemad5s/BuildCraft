# NeoForge 1.21.1 lane status

Updated: 2026-07-23

| Field | Value |
|---|---|
| Loader | NeoForge `21.1.242` with ModDevGradle 2.0.142 |
| Java / Gradle | Java 21 / Gradle 9.2.1 |
| Gameplay slice | Factory Tank, `buildcraftfactory:tank` |
| Tests | 7 passed; 0 failures, errors, or skips |
| Package check | PASS - 54 entries, 8 JSON, exact 8-module graph |
| Artifact | `buildcraft-neo-better-neoforge-1.21.1-0.1.0-dev+1.21.1.jar` |
| SHA-256 | `F40E822AD9BA65FC46083E5FAFD6A90854CB0AD103372F148C4C8A6230922177` |

The Tank retains `buildcraftfactory:tank`, capacity/NBT compatibility, legacy
fluid payload retention, vertical aggregation, modern NeoForge fluid capability
registration, menu/screen behavior, model/data resources, and the eight-module
metadata graph. `build --offline --no-daemon` and the built-in packaged-JAR
verifier passed using the lane-local cache.

No Minecraft client, server, GameTest, resource reload, multiplayer, or old
world was run. The Tank is the only implemented gameplay slice; the rest of
BuildCraft is still a staged port.
