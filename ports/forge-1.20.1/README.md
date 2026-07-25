# BuildCraft Neo - Forge 1.20.1

This is the active BuildCraft Neo restoration lane for Minecraft 1.20.1. It
uses Forge 47.4.22, official Mojang mappings, Java 17, Gradle 8.8, and
ForgeGradle 6.

## Validation state

The eight legacy modules and the full gameplay/resource source surface are
present. Current source compiles, and a forced JUnit execution passed
**332/332** tests. A clean isolated GameTest run passed **69/70** tests. Its
only failure is upward Chute persistence/transfer: 0 of 3 expected persisted
items reached the destination after reload.

Removal-lifecycle GameTests have also been added, but the next run must
explicitly confirm that they were discovered and passed. Remaining gates:

- fix the upward Chute behavior and obtain a fully observed 70/70 GameTest run;
- clean `test`, `runData`, and `build`, followed by packaged JAR validators;
- isolated dedicated-server and client startup;
- model-log inspection and F3+T resource reload;
- multiplayer, complete gameplay, IC2 fluid compatibility, and copied-world
  migration testing;
- record the candidate filename, size, SHA-256, commit, and test timestamps.

No current full-restoration JAR is approved as release-ready or bug-free. The
old `0.1.x-dev` Tank-only JARs at repository level are historical and do not
represent this source tree.

## Source coverage

The lane contains modern Forge source for:

- the Lib/Core foundations, guides, lists, capabilities, networking, creative
  tabs, tools, markers, volume/path data, recipes and migration helpers;
- item, fluid, MJ/power and RF/FE pipes, pipe holders, routing, facades, plugs,
  gates, wires and saved networks;
- engines, fuels, oil fluids/worldgen, MJ/FE conversion and bounded energy
  state;
- Tank, Pump, Flood Gate, Mining Well, Chute, Auto Workbench, Distiller and
  Heat Exchanger;
- Architect, Builder, Quarry, Filler/Planner, Replacer, Library,
  blueprints/templates/snapshots and construction markers;
- Assembly, advanced crafting, integration, programming and charging tables,
  lasers, chipsets, gates and pluggables;
- robots, boards, requester/station paths and the Zone Planner;
- 28 registered container screens plus Guide/Guide Note screen paths.

Presence in source is not a substitute for the runtime checklist.

## Asset and identity evidence

The source-tree validators report:

- 1,154/1,154 valid PNGs, including 60 GUI sheets, with zero errors/warnings;
- 86 animation metadata files;
- 465 models, 72 blockstates, and 936 atlas sources;
- every one of the released JAR's 1,014 PNGs and 24 animation metadata files
  byte-identical at normalized paths;
- 28/28 menu/screen pairs with zero reported errors;
- all 40 released locales and all 12,205 released translation keys, with 71/71 JSON files parsed;
- 208 current recipes with zero reported parity errors;
- all 116 registered modern items exposed by wired creative providers;
- 51/51 migration aliases and all five `SavedData` names present, with zero strict identity failures.

The original BuildCraft source/JAR contains no authoritative custom sound
assets. See [the asset report](TEXTURE_PARITY_REPORT.md) and the repository
[port status](../../PORT_STATUS_LATEST.md).

## Required build sequence

Use the lane-local Gradle cache. These commands are instructions, not a claim
that the latest working tree has already passed them:

```powershell
$env:GRADLE_USER_HOME = (Resolve-Path '.\.gradle-user-home').Path
.\gradlew.bat test --no-daemon --console=plain
.\gradlew.bat runData --no-daemon --console=plain
.\gradlew.bat runGameTestServer --no-daemon --console=plain
.\gradlew.bat build --no-daemon --console=plain
```

Run the validators after the build and supply the exact JAR path where a script
requires it:

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

Inspect GameTest and client/server logs directly. A zero Gradle exit code does
not prove every model resolved or every GameTest ran.

## Safety and handoff

- Test in an isolated instance and never against the only copy of a world.
- Do not replace a mod JAR while Minecraft is active.
- Keep optional integrations absent for the first base-mod startup, then add
  them one at a time.
- Record the candidate JAR filename, byte size, SHA-256, commit and test times
  only after all automated gates pass.
- Use [the manual checklist](../../MANUAL_TEST_CHECKLIST.md) for gameplay and
  migration approval.
- Resume with the repository [roadmap](../../ROADMAP.md), starting at the Chute
  blocker rather than packaging.

## Credits

BuildCraft Neo preserves the original BuildCraft work and attribution. The
CurativeTree/ShipovskijKorp Community Edition port is an implementation
reference; imported MPL-2.0 material retains its required notices. Registry
IDs, NBT keys, `SavedData` names, packet meanings, recipes, tags, and world
identities must remain compatible regardless of release branding.
