# BuildCraft Neo Better 0.1.1-dev test bundle

These are development test JARs, not a complete or stable BuildCraft release.
The implemented gameplay slice is the legacy `buildcraftfactory:tank`; all other legacy systems remain unfinished.

| Minecraft / loader | Test file |
|---|---|
| 1.20.1 Forge | `buildcraft-neo-better-forge-1.20.1-0.1.1-dev+1.20.1.jar` |
| 1.20.1 NeoForge | `buildcraft-neo-better-neoforge-1.20.1-0.1.1-dev+1.20.1.jar` |
| 1.21.1 Forge | `buildcraft-neo-better-forge-1.21.1-0.1.1-dev+1.21.1.jar` |
| 1.21.1 NeoForge | `buildcraft-neo-better-neoforge-1.21.1-0.1.1-dev+1.21.1.jar` |

## What this fixes

- A placed Factory Tank now renders through its baked block model instead of the invisible `BaseEntityBlock` default.
- The Tank now uses its restored legacy alpha-cutout textures, rather than the placeholder translucent glass model.
- The Tank item has its legacy GUI, ground, fixed, and hand transforms, fixing the oversized clipped in-hand model.
- The Tank GUI now uses its restored legacy background and gauge-overlay artwork.

## Verification

- Forge 1.20.1: build and 31 JUnit tests passed.
- NeoForge 1.20.1: build and 29 JUnit tests passed.
- Forge 1.21.1: build and 6 JUnit tests passed.
- NeoForge 1.21.1: build and 9 JUnit tests passed.
- Every JAR passed its packaged metadata/resource verifier, including the four Tank/GUI PNG assets.

## Installing the Forge 1.20.1 test JAR

1. Fully exit Minecraft before changing the `mods` folder.
2. In the test instance, remove the old `0.1.0-dev` BuildCraft Neo Better JAR so duplicate module IDs are impossible.
3. Copy only the Forge 1.20.1 JAR from this folder into `mods`.
4. Start a fresh disposable world, place a Tank, check the item in hand, open its GUI, and press F3+T once.

Only the Factory Tank vertical slice is implemented. Pipes, engines, machines, world generation, most GUIs, multiplayer validation, and old-world migration are not ready for normal play.
