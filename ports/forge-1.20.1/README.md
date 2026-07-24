# BuildCraft Neo Better - Forge 1.20.1

This isolated Java 17 / Forge 47.4.22 lane contains the first verified
`buildcraftfactory:tank` gameplay slice and the bounded 1.20.1 energy
foundation. It preserves the eight legacy module IDs and `buildcraftfactory`
registry identity for the Tank.

Verified artifact:

`build/libs/buildcraft-neo-better-forge-1.20.1-0.1.3-dev+1.20.1.jar`

- 59,307 bytes
- SHA-256 `44B63023B70BB10FF5132E762E65BDCB459EF2A40DC40D27D221481C1111E5E3`
- `build --offline --no-daemon`: PASS; 31 JUnit tests and packaged-resource
  validation passed.
- Includes Tank fluid-capability revival, update-tag synchronization, legacy
  item-to-Tank transfer priority, and the initial in-world fluid renderer.

Use a lane-local Gradle cache:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat build --offline --no-daemon
.\tools\verify-packaged-resources.ps1 -JarPath .\build\libs\buildcraft-neo-better-forge-1.20.1-0.1.3-dev+1.20.1.jar
```

This is not a complete BuildCraft release. Do not run a client/server/GameTest
or test old worlds without a disposable instance, an explicit EULA decision,
and backup copies. See `../../PORT_STATUS_LATEST.md` for the complete scope and
release gates.
