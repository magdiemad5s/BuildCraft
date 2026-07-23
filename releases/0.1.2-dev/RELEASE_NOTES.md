# BuildCraft Neo Better 0.1.2-dev Forge 1.20.1 test bundle

This is a development test JAR, not a complete or stable BuildCraft release.
The only implemented gameplay slice is the legacy `buildcraftfactory:tank`.

## Included artifact

| Minecraft / loader | Test file |
|---|---|
| 1.20.1 Forge 47.4.22 | `buildcraft-neo-better-forge-1.20.1-0.1.2-dev+1.20.1.jar` |

## What this fixes

- Fixes the Factory Tank's magenta/black missing-texture rendering in Forge
  1.20.1.
- Moves Tank block sprites from the legacy `textures/blocks/` convention to
  `textures/block/`, which Minecraft 1.20.1 stitches into `minecraft:blocks`.
- Updates block-model texture IDs to `buildcraftfactory:block/tank/*` and adds
  packaged-resource validation for that atlas requirement.

## Verification

- Offline Forge 47.4.22 / Java 17 `build`: PASS.
- 31 JUnit tests: PASS.
- Legacy module/identity contract: PASS.
- Packaged-resource verifier: PASS; the JAR contains only the singular
  `textures/block/tank/*` Tank sprites.

## Installing this test build

1. Fully exit Minecraft before changing the instance `mods` folder.
2. Keep a backup of the installed `0.1.1-dev` JAR.
3. Install this JAR as the only BuildCraft Neo Better Forge 1.20.1 JAR.
4. Start a fresh disposable world, place a Tank, inspect its item, open its
   GUI, and use F3+T once.

Pipes, engines, machines, world generation, most GUIs, multiplayer validation,
and old-world migration remain unfinished. Do not use this development JAR in
an irreplaceable world.
