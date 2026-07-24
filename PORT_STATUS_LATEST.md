# BuildCraft Neo Better - port handoff

Updated: 2026-07-24

## Release state

This is a development foundation, **not a complete or bug-free public
BuildCraft release**. The Factory Tank source slice exists in all requested
lanes, but only the Forge 1.20.1 `0.1.3-dev` artifact has passed a fresh
capacity/client-sync/renderer build and packaged-resource validation. The
original 1.12.2 checkout, reference JAR, Minecraft clients, EULA state, and
worlds were not changed.

The original preservation references remain unchanged:

- Source commit: `7c86626d09c4569fb2e0f458a16aaf8c3148ebaf`
- Reference JAR SHA-256: `F617279F8148A9140AB86C0E4AA4C54FE8A94515D2CF487A47CBD928160B88AA`

## Verified loader/version matrix

| Lane | Loader/toolchain | Build and tests | Packaged-resource check |
|---|---|---:|---|
| Forge 1.20.1 | Forge `47.4.22`, Java 17, Gradle 8.8 | PASS - 31 JUnit tests (`0.1.3-dev`) | PASS - packaged resource and singular block-atlas Tank verifier |
| NeoForge 1.20.1 | transitional `net.neoforged:forge:1.20.1-47.1.106`, Java 17, NeoGradle 7.0.97 | Source atlas fix staged; rebuild pending | `0.1.1-dev` visual JAR superseded |
| Forge 1.21.1 | Forge `52.1.16`, Java 21, Gradle 8.8 | Source atlas fix staged; rebuild pending | `0.1.1-dev` visual JAR superseded |
| NeoForge 1.21.1 | NeoForge `21.1.242`, Java 21, ModDevGradle 2.0.142 | Source atlas fix staged; rebuild pending | `0.1.1-dev` visual JAR superseded |

All lanes preserve the public module graph:

```text
buildcraftcore      -> buildcraftlib
buildcraftbuilders  -> buildcraftcore
buildcraftenergy    -> buildcraftcore
buildcraftfactory   -> buildcraftcore
buildcrafttransport -> buildcraftcore
buildcraftrobotics  -> buildcraftcore
buildcraftsilicon   -> buildcraftcore, optional AFTER buildcrafttransport
```

## Produced development artifacts

| Lane | JAR | Size | SHA-256 |
|---|---|---:|---|
| Forge 1.20.1 | `releases/0.1.3-dev/buildcraft-neo-better-forge-1.20.1-0.1.3-dev+1.20.1.jar` | 59,307 bytes | `44B63023B70BB10FF5132E762E65BDCB459EF2A40DC40D27D221481C1111E5E3` |
| NeoForge 1.20.1 | `releases/0.1.1-dev/buildcraft-neo-better-neoforge-1.20.1-0.1.1-dev+1.20.1.jar` (superseded) | 53,893 bytes | `6D3694B705E72582DA0A7AF7149394335561B6441EF7705D8FCCA3CC0C1B8C63` |
| Forge 1.21.1 | `releases/0.1.1-dev/buildcraft-neo-better-forge-1.21.1-0.1.1-dev+1.21.1.jar` (superseded) | 41,784 bytes | `72A4C38E1EC3076CB21C33CC3BBD60A673706D23F75B0B59727EE57CE2566EDE` |
| NeoForge 1.21.1 | `releases/0.1.1-dev/buildcraft-neo-better-neoforge-1.21.1-0.1.1-dev+1.21.1.jar` (superseded) | 42,928 bytes | `2C29EC854E23DE333835FFDDF81F5D477F4737BA420535170A99CB471F5C3C27` |

Do not use the superseded `0.1.1-dev` or `0.1.2-dev` JARs to test the
Tank. Use only the Forge 1.20.1 `0.1.3-dev` artifact above until the other
source fixes have fresh loader-specific builds, packaged verification, and
real-client validation.

## Implemented functionality

The first gameplay slice is the legacy `buildcraftfactory:tank` in all four
lanes:

- block, item, block entity, fluid capability, comparator, vertical topology,
  menu, screen, models, language data, loot table, and recipe where supported;
- 16,000 mB local capacity; liquids settle/fill at the bottom and drain from
  the top, with gas ordering inverted;
- direct `tank` NBT plus compatible reads for legacy `tanks.tank`; malformed
  and over-capacity stored amounts are clamped;
- server-authoritative container/menu transfers and distance/dimension/block
  entity revalidation;
- 1.20.1 Tank review fixes: bounded cascading settling, server-only direct
  fluid mutation, model-face correction, and transfer authority checks.
- Forge 1.20.1 `0.1.3-dev`: recreated fluid capability lifecycle, synchronized
  `tank` update tags, legacy item-to-Tank transfer priority, and a first
  in-world fluid renderer for the Tank.

Forge and transitional NeoForge 1.20.1 additionally contain a bounded,
unit-tested microjoule/FE foundation. It is intentionally **not** represented
as a working energy-pipe system yet.

## Compatibility and issue controls

- Registry IDs, module IDs, aliases, NBT/SavedData names, packet inventory,
  recipes, and tags are captured in
  `legacy-1.12.2-contract/LEGACY_IDENTITY_MANIFEST.json`.
- `ISSUE_PORT_LEDGER_2026-07-23.md` records the screened upstream issues and
  their required regressions. It is a release gate, not a claim that every
  historical issue is fixed.
- `COMMUNITY_EDITION_REFERENCE_AUDIT_2026-07-23.md` documents which Community
  Edition patterns were useful and why its unsafe power, oil, permission, and
  packet behavior was not copied.
- `GUI_PARITY_MANIFEST.md` enumerates every legacy GUI. Only the Tank GUI is
  implemented; every other legacy GUI remains a required port item.

## Deliberately not claimed

Do not call these JARs full BuildCraft, pipe-compatible, multiplayer-proven,
old-world-compatible, visually complete, or bug-free. Real pipes, engines,
machines, builders/quarry, robotics, silicon systems, oil/world generation,
rendering, packets, optional integrations, and the remaining GUIs still need
porting and runtime validation.

No client, dedicated server, GameTest server, F3+T reload, multiplayer test,
or legacy-world migration test was run. Those must use a disposable test
instance and a backed-up world, with an explicit EULA decision.

## Safe repeatable checks

All lane caches are isolated under `.gradle-user-home`. From any lane:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat build --offline --no-daemon
```

Run the lane's packaged verifier after the build. Do not run `runClient`,
`runServer`, `runGameTestServer`, or `runData` against a live Minecraft setup.
