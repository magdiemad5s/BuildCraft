# Forge 1.21.1 lane status

Updated: 2026-07-23

| Field | Value |
|---|---|
| Loader | Forge `52.1.16` |
| Java / Gradle | Java 21 / Gradle 8.8 |
| Gameplay slice | Factory Tank, `buildcraftfactory:tank` |
| Tests | 6 passed; 0 failures, errors, or skips |
| Package check | PASS - 50 entries, 7 JSON, 8 module IDs, 8 legacy edges |
| Artifact | `buildcraft-neo-better-forge-1.21.1-0.1.1-dev+1.21.1.jar` |
| SHA-256 | `72A4C38E1EC3076CB21C33CC3BBD60A673706D23F75B0B59727EE57CE2566EDE` |

The port includes the 1.21 split interaction methods, `RenderShape.MODEL`,
holder-aware NBT/update tags, fluid capability, client screen/render layer,
singular 1.21 data paths, and restored legacy Tank alpha-cutout models,
textures, item transforms, and GUI artwork. Its JUnit tests cover
identity/capacity, vertical ordering, comparator bounds, and malformed amount
clamping.

Live client, dedicated-server, GameTest, reload, multiplayer, and old-world
migration validation are still release gates. The rest of BuildCraft remains
unported.
