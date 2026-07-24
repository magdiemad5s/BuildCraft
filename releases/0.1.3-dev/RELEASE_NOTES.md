# BuildCraft Neo Better 0.1.3-dev Forge 1.20.1 test bundle

This is a development test JAR, not a complete or stable BuildCraft release.
The only implemented gameplay slice is the legacy `buildcraftfactory:tank`.

## Included artifact

| Minecraft / loader | Test file |
|---|---|
| 1.20.1 Forge 47.4.22 | `buildcraft-neo-better-forge-1.20.1-0.1.3-dev+1.20.1.jar` |

## What this fixes

- Recreates the Tank's Forge fluid capability after lifecycle invalidation, so
  external fluid handlers can discover it again after a chunk or block-state
  transition.
- Synchronizes the Tank's `tank` NBT in block-entity update packets, so the
  client GUI receives current server-side fluid contents.
- Restores a visible, tinted in-world fluid level inside the Tank frame for
  vanilla and normal Forge fluid types.
- Makes direct bucket/container interaction use legacy item-to-Tank priority,
  matching the Tank menu's transfer behavior.

## Verification

- Forge 47.4.22 / Java 17 `build --offline --no-daemon`: PASS.
- 31 JUnit tests: PASS in the initial clean build.
- Legacy module/identity contract: PASS.
- Packaged-resource verifier: PASS.
- JAR: 59,307 bytes.
- SHA-256:
  `44B63023B70BB10FF5132E762E65BDCB459EF2A40DC40D27D221481C1111E5E3`.

## Required manual checks

1. Fully exit Minecraft before changing the instance `mods` folder.
2. Use a fresh disposable Forge 1.20.1 world.
3. In Survival, right-click an empty Tank with a vanilla water bucket. Open the
   Tank GUI and hover the gauge: it must report `Water 1000 / 16000 mB`.
   In Creative, Forge deliberately keeps the held bucket even when transfer
   succeeds, so use the gauge or comparator as the source of truth.
4. Confirm the placed Tank displays a visible water level, then drains into an
   empty bucket and visibly updates again.
5. For IC2 Classic, use a water-containing source against the facing side of a
   powered Electric Pipe Pump, then normal IC2 fluid pipe into the Tank. Wait
   at least one second and inspect the Tank GUI. A visible pipe connection
   alone does not configure an emitter or power the pump.
6. Test F3+T and inspect the log for missing model, texture, or packet errors.

The Tank renderer is an initial local-level renderer. It does not yet claim
legacy interpolation or seamless multi-Tank fluid rendering. No dedicated
server, client-startup, GameTest, multiplayer, or old-world-migration run was
performed for this development bundle.

Pipes, engines, machines, world generation, most GUIs, multiplayer validation,
and old-world migration remain unfinished. Do not use this development JAR in
an irreplaceable world.
