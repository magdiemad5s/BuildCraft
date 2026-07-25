# BuildCraft Neo - Forge 1.20.1 port status

Updated: 2026-07-25

## Status in one sentence

The full eight-module BuildCraft source restoration is present in the Forge
1.20.1 lane. Compilation succeeds, a forced JUnit run passed **332/332**, and a
clean isolated GameTest run passed **69/70**. The only failing scenario is the
upward Chute persistence/transfer test, which observed **0 of 3** expected item
transfers. A clean build, packaged-JAR validation, server/client and reload
checks, multiplayer, gameplay, and copied-world migration are still required;
there is no full-restoration release candidate or release JAR yet.

Do not describe this working tree as bug-free, release-ready, or fully
world-compatible until those gates pass.

## Exact baseline

| Property | Value |
|---|---|
| Project | **BuildCraft Neo** |
| Active game target | Minecraft `1.20.1` |
| Loader | Forge `47.4.22` |
| Mappings | Official Mojang mappings for `1.20.1` |
| Java | 17 |
| Gradle | 8.8 / ForgeGradle 6 lane |
| Source lane | `ports/forge-1.20.1` |
| Current source version | `8.0.10+1.20.1+neo1-dev` |
| Current archive prefix | `buildcraft-neo-forge-1.20.1` |

Preservation references:

- Original source commit: `7c86626d09c4569fb2e0f458a16aaf8c3148ebaf`.
- Released reference JAR: `buildcraft-all-8.0.0.jar`.
- Reference JAR SHA-256:
  `F617279F8148A9140AB86C0E4AA4C54FE8A94515D2CF487A47CBD928160B88AA`.
- Legacy registry/persistence contract:
  [`legacy-1.12.2-contract/LEGACY_IDENTITY_MANIFEST.json`](legacy-1.12.2-contract/LEGACY_IDENTITY_MANIFEST.json).

The public identities remain the eight legacy module IDs:
`buildcraftlib`, `buildcraftcore`, `buildcraftbuilders`, `buildcraftenergy`,
`buildcraftfactory`, `buildcraftsilicon`, `buildcrafttransport`, and
`buildcraftrobotics`.

## Target order

| Lane | State |
|---|---|
| Forge 1.20.1 | **Active restoration and validation target** |
| NeoForge 1.20.1 | Deferred until the Forge 1.20.1 release gates pass |
| Forge 1.21.1 | Deferred until the 1.20.1 behavior is stable |
| NeoForge 1.21.1 | Deferred until the 1.20.1 behavior is stable |
| Fabric / later Minecraft | Research only; separate abstraction and validation work |

The old `0.1.x-dev` JARs in `releases/` are historical Tank-focused test
bundles. Their names, sizes, hashes, and release notes remain valid historical
records, but they are superseded as a representation of the current source.
There is no approved full-restoration release candidate yet.

## Restored source inventory

“Present in source” means the modern Forge lane contains registrations,
implementation, data, rendering, and/or tests for the system. It does **not**
mean every behavior has passed runtime and manual approval.

| Module | Source-restored surface | Remaining proof |
|---|---|---|
| Lib | Capabilities, fluid/energy helpers, recipes, networking, guides, lists, chunk loading, permissions, migration helpers, GUI foundations | Dedicated-server safety, resource reload, packet and multiplayer behavior |
| Core | Creative tabs, gears/tools, markers, volume/path data, springs, engines shared by modules, guide/list interaction | In-world marker topology, GUI interaction, persistence, far-coordinate rendering |
| Transport | Pipe holder; item, fluid, MJ/power and RF/FE pipe families; routing, plugs, facades, gates, wires and persistence | All transfer families, back-pressure, no-loss/no-duplication, chunk reload, models and multiplayer |
| Energy | Wood/stone/iron/RF engine paths, MJ dynamo, oil/fuel fluids, combustion state, FE/MJ boundaries and oil worldgen data | Live generation/consumption/cooling, finite buffers, displayed rates, explosions, worldgen matrix |
| Factory | Tank, Pump, Flood Gate, Mining Well, Chute, Auto Workbench, Distiller and Heat Exchanger | Processing rates, inventories/tanks, redstone, reload, fluid capabilities and client synchronization |
| Builders | Architect, Builder, Quarry, Filler/Planner, Replacer, Library, construction markers, blueprints/templates/snapshots | Placement accuracy, power accounting, permissions, chunk tickets, completion and reload |
| Silicon | Assembly, advanced crafting, integration, programming and charging tables; lasers, chipsets, gates and pluggables | Recipe refresh, automation, laser power, menus, gate state and multiplayer synchronization |
| Robotics | Robot entities/items, boards, stations/requester paths, zone planner and network codecs | AI tasks, docking, charging, inventory transfer, zone editing, persistence and multiplayer |

## Verification evidence already recorded

| Check | Recorded result | Qualification |
|---|---|---|
| Compilation | PASS | Current source compiled successfully; this is not yet the final clean build gate |
| JUnit | **332/332 PASS** | Forced current-source execution, rather than an up-to-date result |
| GameTests | **69/70 PASS** | Clean isolated run; sole failure is upward Chute persistence/transfer, with 0/3 expected items transferred |
| Removal-lifecycle GameTests | Added; confirmation pending | Explicitly confirm discovery and results in the next full run |
| Legacy identity static audit | PASS | Strict gate: 51/51 migration aliases, all five `SavedData` names, missing-mapping handler, and zero strict failures |
| Creative inventory static audit | PASS | All 116 currently registered modern items are exposed by wired providers; client confirmation pending |
| GUI static audit | PASS | 28/28 menu/screen pairs, 60 GUI PNGs, and zero reported errors |
| Texture/model source-tree audit | PASS | 1,154/1,154 valid PNGs, 86 animations, 465 models, 72 blockstates, 936 atlas sources, zero errors/warnings |
| Language source-tree audit | PASS | 40 released locales and all 12,205 released keys retained; 71/71 JSON files parsed, zero errors |
| Recipe/data static audit | PASS | 208 current recipes validated with zero reported parity errors |
| Full current build/JAR | Pending | No full-restoration candidate or release JAR has been produced |
| Dedicated server / client | Pending | No current-source runtime signoff |
| Multiplayer / old-world migration | Pending | Must use an isolated instance and a copied world |

Static reports prove inventory and reference resolution; they cannot prove
machines tick, packets synchronize, recipes behave, models look correct, or
world data migrates safely.

## Original asset restoration

The current Forge resource input contains:

- 1,154 PNGs, including 60 GUI sheets;
- 86 animation metadata files;
- 465 model JSON files and 72 blockstate JSON files;
- 936 explicit block-atlas sources;
- 28/28 registered menu/screen pairs;
- 40 released locales with all 12,205 released translation keys.

Direct normalized-path comparison confirms all 1,014 PNGs and all 24 animation
metadata files from the released 8.0.0 JAR are present byte-for-byte. The extra
files are modern aliases, compatibility copies, branding, or resources from the
preserved source/reference port. No legacy artwork was redrawn. Neither the
released JAR nor original resource source contains authoritative custom sound
assets, so the correct original custom-sound count is zero.

See [`ports/forge-1.20.1/TEXTURE_PARITY_REPORT.md`](ports/forge-1.20.1/TEXTURE_PARITY_REPORT.md)
for path mappings and hash evidence.

## Compatibility contract

Do not casually rename or reinterpret:

- block, item, entity, menu, recipe, block-entity, fluid, tag, or damage IDs;
- module and resource namespaces;
- NBT field names, `SavedData` names, packet identifiers or packet meanings;
- blueprint/snapshot serialization and block-entity persistence;
- worldgen data IDs or recipe/tag paths.

The strict identity verifier reports 51/51 required migration aliases and all
five legacy `SavedData` names present. Runtime migration remains pending because
static presence cannot prove a real 1.12.2 world will load correctly.

## Issue work and known risk

[`ISSUE_PORT_LEDGER_2026-07-23.md`](ISSUE_PORT_LEDGER_2026-07-23.md) records the
screened upstream reports and their release regressions. In particular, the
latest source must be rechecked for bounded MJ/FE transfer, wire IDs, list
matching, literal percent/newline guide text, Assembly Table recipe refresh,
pipe persistence, builders/quarry accounting, oil deposit variants, resource
reload, and optional integration isolation.

A source fix or a passing unit test is not enough by itself. Each issue remains
a release gate until its relevant GameTest and manual/runtime scenario passes.

The current runtime blocker is narrower: the upward Chute test persisted its
three source items but transferred none after reload. Fix that behavior first,
then rerun the complete GameTest suite and explicitly verify that the newly
added removal-lifecycle scenarios were discovered and passed.

## Required automated gates

Run from `ports/forge-1.20.1` with the lane-local cache. These commands are the
required sequence, not a record that this documentation update ran them:

```powershell
$env:GRADLE_USER_HOME = (Resolve-Path '.\.gradle-user-home').Path
.\gradlew.bat test --no-daemon --console=plain
.\gradlew.bat runData --no-daemon --console=plain
.\gradlew.bat runGameTestServer --no-daemon --console=plain
.\gradlew.bat build --no-daemon --console=plain
```

Then run every parity/package verifier and inspect the actual reports/logs:

```powershell
$sourceResources = (Resolve-Path '.\src\main\resources').Path
$jarPath = (Resolve-Path '.\build\libs\buildcraft-neo-forge-1.20.1-8.0.10+1.20.1+neo1-dev.jar').Path

node tools/verify-gui-parity.mjs
node tools/verify-language-parity.mjs
node tools/verify-recipe-data-parity.mjs
powershell -ExecutionPolicy Bypass -File tools/verify-texture-parity.ps1 -InputPath $sourceResources -WarningsAsErrors
powershell -ExecutionPolicy Bypass -File tools/verify-legacy-identity-parity.ps1 -FailOnMismatch
powershell -ExecutionPolicy Bypass -File tools/verify-packaged-resources.ps1 -JarPath $jarPath
powershell -ExecutionPolicy Bypass -File tools/verify-full-release-jar.ps1 -JarPath $jarPath
```

GameTest success must be confirmed in the server log; Gradle exit status alone
is not sufficient. After a successful build, record the exact JAR filename,
size, SHA-256, commit, and test timestamps here before calling it a candidate.

## Next-session roadmap

Follow [ROADMAP.md](ROADMAP.md) in order. Do not skip from the Chute failure to
release packaging, and do not promote the historical Tank-only artifacts.

## Manual release gates

The full checklist is in
[`MANUAL_TEST_CHECKLIST.md`](MANUAL_TEST_CHECKLIST.md). It covers:

- creative tabs and all registered items;
- item/fluid/MJ/RF pipes, facades, plugs, wires and gates;
- tanks, fluids, engines, Factory processing and IC2 fluid interoperability;
- Quarry, Mining Well, Builder, Filler, Replacer and blueprint workflows;
- all 28 container screens plus Guide/Guide Note;
- oil worldgen including small deposits;
- dedicated server, two-client synchronization and reconnects;
- save/reload, chunk boundaries, backed-up 1.12.2 migration and performance.

## Safety boundary

- Build and test inside this lane first; never overwrite a user instance with an
  unverified JAR.
- Do not close or interrupt a Minecraft client the user is actively testing.
- Never run migration against the only copy of a world.
- Keep downloaded references and Gradle caches isolated to the project.
- Before copying a candidate into a test profile, confirm no Minecraft/Java
  process is using the instance and remove only the exact obsolete BuildCraft
  Neo test JAR.
