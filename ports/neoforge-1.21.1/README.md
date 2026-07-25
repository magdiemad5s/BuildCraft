# BuildCraft Neo - NeoForge 1.21.1

This is an independent, additive development lane for Minecraft `1.21.1` and
NeoForge `21.1.242`, using Java `21`, ModDevGradle `2.0.142`, and the
lane-local Gradle `9.2.1` wrapper.

It keeps the eight public BuildCraft 1.12.2 module IDs and the shared
`buildcraft` resource/data contract. Content whose Minecraft ID is historically
owned by a module remains in that module namespace; for example the implemented
Tank is `buildcraftfactory:tank` and therefore its model/translation resources
correctly live below `assets/buildcraftfactory`.

`src/main/templates/META-INF/neoforge.mods.toml` is deliberate: ModDevGradle
generates the required NeoForge metadata at build time. Do not add a legacy
`mods.toml` to this lane.

## Current implemented slice

The Factory Tank is the first real gameplay slice:

- Block/item/block entity/menu/screen with the legacy
  `buildcraftfactory:tank` identity.
- 16,000 mB local capacity, vertical tank aggregation, liquid/gas ordering,
  comparator output, bucket/container handling, and safe server-authoritative
  menu transfers.
- Modern NeoForge fluid capability registration.
- Direct `tank` NBT plus compatible reads for legacy `tanks.tank`, bounded
  amounts, and legacy `FluidName`/`Amount` fluid payloads.
- Models, translations, loot table, and a glass recipe.

The rest of BuildCraft is still a staged port, not a release. See
[`STATUS.md`](STATUS.md) for exact validation and limitations.

## Safe validation

The source-level validation command neither downloads dependencies nor starts
Minecraft:

```powershell
.\tools\verify-lane.ps1
```

For Gradle work, keep all caches and output isolated to this lane:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat --no-daemon test
.\gradlew.bat --no-daemon build
```

`build` runs the repeatable packaged-JAR verifier. It checks every packaged
JSON resource, the required Tank resources, the generated NeoForge metadata,
and the full legacy module dependency graph. The configured run directories
remain isolated under `run/`; do not run a client, server, or GameTest server
until a separate disposable test instance and an explicit EULA decision exist.
