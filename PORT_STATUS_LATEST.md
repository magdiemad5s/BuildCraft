# BuildCraft Neo Better - port handoff

Updated: 2026-07-23

## Release state

This is a verified four-lane development foundation, **not a complete or
bug-free public BuildCraft release**. Each requested loader/version now has a
real Factory Tank vertical slice and a packaged development JAR. The original
1.12.2 checkout, reference JAR, Minecraft clients, EULA state, and worlds were
not changed.

The original preservation references remain unchanged:

- Source commit: `7c86626d09c4569fb2e0f458a16aaf8c3148ebaf`
- Reference JAR SHA-256: `F617279F8148A9140AB86C0E4AA4C54FE8A94515D2CF487A47CBD928160B88AA`

## Verified loader/version matrix

| Lane | Loader/toolchain | Build and tests | Packaged-resource check |
|---|---|---:|---|
| Forge 1.20.1 | Forge `47.4.22`, Java 17, Gradle 8.8 | PASS - 31 JUnit tests | PASS - 62 entries, 6 JSON, 8 module IDs, 8 edges |
| NeoForge 1.20.1 | transitional `net.neoforged:forge:1.20.1-47.1.106`, Java 17, NeoGradle 7.0.97 | PASS - 27 JUnit tests | PASS - 62 entries, 6 JSON, 8 module IDs, 8 edges |
| Forge 1.21.1 | Forge `52.1.16`, Java 21, Gradle 8.8 | PASS - 6 JUnit tests | PASS - 50 entries, 7 JSON, 8 module IDs, 8 edges |
| NeoForge 1.21.1 | NeoForge `21.1.242`, Java 21, ModDevGradle 2.0.142 | PASS - 7 JUnit tests | PASS - 54 entries, 8 JSON, 8-module graph |

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
| Forge 1.20.1 | `ports/forge-1.20.1/build/libs/buildcraft-neo-better-forge-1.20.1-0.1.0-dev+1.20.1.jar` | 52,151 bytes | `F53680631A152AB0AD616BA2DE472FC3381A33E2625893B2F8C339461578BB6E` |
| NeoForge 1.20.1 | `ports/neoforge-1.20.1/build/libs/buildcraft-neo-better-neoforge-1.20.1-0.1.0-dev+1.20.1.jar` | 51,043 bytes | `636BC36FFF3574922A9DE0832BD804CE043CFB150BAFCE826528B6CDA6FB1DB0` |
| Forge 1.21.1 | `ports/forge-1.21.1/build/libs/buildcraft-neo-better-forge-1.21.1-0.1.0-dev+1.21.1.jar` | 38,841 bytes | `E1005452D67AB399E081DEE517017A39A8DE9D866F2A2808D2E75CAC9FDE44B8` |
| NeoForge 1.21.1 | `ports/neoforge-1.21.1/build/libs/buildcraft-neo-better-neoforge-1.21.1-0.1.0-dev+1.21.1.jar` | 39,920 bytes | `F40E822AD9BA65FC46083E5FAFD6A90854CB0AD103372F148C4C8A6230922177` |

Use only the explicitly listed `buildcraft-neo-better-*` artifacts. Older
same-size pre-rename JARs were retained rather than deleted; they are not the
hand-off artifacts above.

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
