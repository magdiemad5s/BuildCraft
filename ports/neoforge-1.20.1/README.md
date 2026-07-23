# BuildCraft Neo Better - NeoForge 1.20.1

This is the isolated transitional NeoForge 1.20.1 lane: Java 17,
NeoGradle 7.0.97, and `net.neoforged:forge:1.20.1-47.1.106`. Its use of
`META-INF/mods.toml` and `net.minecraftforge.*` packages is correct for this
transitional release.

It contains the reviewed `buildcraftfactory:tank` slice plus the bounded
1.20.1 energy foundation. The Tank keeps its registry/NBT identity, vertical
fluid behavior, fluid capability, menu/screen, and package resources. Its
legacy visual identity is restored with the original cutout Tank models,
textures, item transforms, and GUI base/gauge artwork.

Verified `0.1.1-dev` development artifact:

`build/libs/buildcraft-neo-better-neoforge-1.20.1-0.1.1-dev+1.20.1.jar`

- 53,893 bytes
- SHA-256 `6D3694B705E72582DA0A7AF7149394335561B6441EF7705D8FCCA3CC0C1B8C63`
- Offline build PASS: 29 JUnit tests, static-lane validation, packaged
  resource/legacy-graph validation, and the build-wired Tank visual verifier.

Use only the lane-local cache and do not launch Minecraft from this lane:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat build --offline --no-daemon
.\tools\verify-static-lane.ps1
.\tools\verify-packaged-resources.ps1 -JarPath .\build\libs\buildcraft-neo-better-neoforge-1.20.1-0.1.1-dev+1.20.1.jar
```

This is a development slice, not complete BuildCraft. See `STATUS.md` and
`../../PORT_STATUS_LATEST.md` before any runtime validation.
