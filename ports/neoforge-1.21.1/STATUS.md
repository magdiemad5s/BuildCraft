# NeoForge 1.21.1 lane status

Updated: 2026-07-23

| Field | Value |
|---|---|
| Project identity | BuildCraft Neo Better |
| Loader | NeoForge 21.1.242 |
| Minecraft | 1.21.1 |
| Java | 21 |
| Gradle | lane-local wrapper pinned to 9.2.1 |
| Build plugin | ModDevGradle 2.0.142 |
| Metadata file | generated `META-INF/neoforge.mods.toml` |
| Resource/data namespace | preserved `buildcraft`; Factory content uses its legacy `buildcraftfactory` registry/resource namespace where Minecraft requires the registry namespace |
| Legacy module IDs | all eight preserved with matching `@Mod` bootstraps |
| Module graph | Core requires Lib; Builders/Energy/Factory/Transport/Robotics require Core; Silicon requires Core and is optional-after Transport |
| Configuration cache | deliberately disabled until `doLast` verification tasks are cache safe |
| Gameplay port | Factory Tank vertical slice implemented; all other BuildCraft systems remain unported |
| Game/client/server execution | not run |

## Implemented Factory Tank slice

- Preserved registry ID: `buildcraftfactory:tank`.
- Block, block item, block entity, menu, client screen, creative-tab entry,
  blockstate/models, loot table, and a glass recipe.
- A 16,000 mB local tank with a NeoForge 1.21.1 fluid block capability.
- Connected vertical tanks expose a single aggregate handler. Liquids fill from
  the bottom and drain from the top; gases do the inverse.
- Comparator output retains the legacy per-block 0--15 behaviour.
- Bucket/container interaction is attempted before opening the menu. Menu slot
  transfers and the gauge action stay server-authoritative.
- Saved fluid data uses the preserved `tank` key, reads the legacy nested
  `tanks.tank` shape, clamps bad/oversized amounts, understands legacy
  `FluidName`/`Amount` payloads, and retains an unresolved legacy payload
  instead of silently deleting it.
- Visual compatibility restores `RenderShape.MODEL`, the original alpha-cutout
  Tank textures/models, legacy item display transforms, the cutout render layer,
  and the original `textures/gui/tank.png` base and gauge overlay.

## Validation completed

All Gradle state and downloaded dependencies were kept under this lane's
`.gradle-user-home`; no Minecraft client, dedicated server, GameTest server,
EULA, or world data was touched.

- `./tools/verify-lane.ps1` passed, checking the source visual contract and
  its packaged equivalent, including Tank PNG headers and dimensions.
- `./gradlew.bat test --no-daemon --console=plain` passed: 9 JUnit tests,
  including the Tank visual-resource tests.
- `./gradlew.bat build --no-daemon --console=plain` passed. Its packaged-JAR
  verifier parsed 8 JSON resources and verified 4 Tank PNG entries.

Produced artifact:

`build/libs/buildcraft-neo-better-neoforge-1.21.1-0.1.1-dev+1.21.1.jar`

| Size | SHA-256 |
|---:|---|
| 42,928 bytes | `2C29EC854E23DE333835FFDDF81F5D477F4737BA420535170A99CB471F5C3C27` |

## Remaining release gates

- This is not a complete BuildCraft port and must not be described as
  bug-free or published as a full gameplay release.
- No live client, dedicated-server, multiplayer, resource-reload, or GameTest
  run has occurred. Those require a separate disposable test instance and an
  explicit EULA decision.
- No backed-up legacy world has been migrated, saved, and reloaded. The Tank's
  legacy NBT reader is implemented, but old-world compatibility remains
  unproven until that process succeeds.
- Pipes, engines, power conversion/gameplay, machines, builders, robots,
  silicon, world generation, packets beyond vanilla menu traffic, optional
  integrations, and the rest of the original GUIs remain unported.

For future safe Gradle invocations, use the local cache and wrapper from this
lane:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat --no-daemon check
```
