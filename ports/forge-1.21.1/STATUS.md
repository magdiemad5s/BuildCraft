# Forge 1.21.1 lane status

Updated: 2026-07-23

| Field | Value |
|---|---|
| Loader | Forge `52.1.16` |
| Java / Gradle | Java 21 / Gradle 8.8 |
| Gameplay slice | Factory Tank, `buildcraftfactory:tank` |
| Tests | 6 passed; 0 failures, errors, or skips |
| Package check | PASS - 50 entries, 7 JSON, 8 module IDs, 8 legacy edges |
| Artifact | `buildcraft-neo-better-forge-1.21.1-0.1.0-dev+1.21.1.jar` |
| SHA-256 | `E1005452D67AB399E081DEE517017A39A8DE9D866F2A2808D2E75CAC9FDE44B8` |

The port includes the 1.21 split interaction methods, `RenderShape.MODEL`,
holder-aware NBT/update tags, fluid capability, client screen/render layer,
and singular 1.21 data paths. Its JUnit tests cover identity/capacity, vertical
ordering, comparator bounds, and malformed amount clamping.

Live client, dedicated-server, GameTest, reload, multiplayer, and old-world
migration validation are still release gates. The rest of BuildCraft remains
unported.
