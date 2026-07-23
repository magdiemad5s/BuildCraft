# BuildCraft Neo Better - NeoForge 1.20.1

This is the isolated transitional NeoForge 1.20.1 lane: Java 17,
NeoGradle 7.0.97, and `net.neoforged:forge:1.20.1-47.1.106`. Its use of
`META-INF/mods.toml` and `net.minecraftforge.*` packages is correct for this
transitional release.

It now contains the reviewed `buildcraftfactory:tank` slice plus the bounded
1.20.1 energy foundation. The Tank keeps its registry/NBT identity, vertical
fluid behavior, fluid capability, menu/screen, and package resources.

Verified artifact:

`build/libs/buildcraft-neo-better-neoforge-1.20.1-0.1.0-dev+1.20.1.jar`

- 51,043 bytes
- SHA-256 `636BC36FFF3574922A9DE0832BD804CE043CFB150BAFCE826528B6CDA6FB1DB0`
- Offline build PASS: 27 JUnit tests, static-lane validation, and packaged
  resource/legacy-graph validation all passed.

Use only the lane-local cache and do not launch Minecraft from this lane:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat build --offline --no-daemon
.\tools\verify-static-lane.ps1
.\tools\verify-packaged-resources.ps1 -JarPath .\build\libs\buildcraft-neo-better-neoforge-1.20.1-0.1.0-dev+1.20.1.jar
```

This is a development slice, not complete BuildCraft. See `STATUS.md` and
`../../PORT_STATUS_LATEST.md` before any runtime validation.
