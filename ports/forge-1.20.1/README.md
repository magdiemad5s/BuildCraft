# BuildCraft Neo Better - Forge 1.20.1

This isolated Java 17 / Forge 47.4.22 lane contains the first verified
`buildcraftfactory:tank` gameplay slice and the bounded 1.20.1 energy
foundation. It preserves the eight legacy module IDs and `buildcraftfactory`
registry identity for the Tank.

Verified artifact:

`build/libs/buildcraft-neo-better-forge-1.20.1-0.1.2-dev+1.20.1.jar`

- 55,137 bytes
- SHA-256 `A5A8256A623DACE6BED66D308B1569DDB83AAAD93278418FAE93FA91AB9F4C07`
- `build --offline --no-daemon`: PASS; 31 JUnit tests and packaged-resource
  validation passed. Tank block sprites use the 1.20.1 block-atlas path
  `textures/block/tank/`.

Use a lane-local Gradle cache:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat build --offline --no-daemon
.\tools\verify-packaged-resources.ps1 -JarPath .\build\libs\buildcraft-neo-better-forge-1.20.1-0.1.2-dev+1.20.1.jar
```

This is not a complete BuildCraft release. Do not run a client/server/GameTest
or test old worlds without a disposable instance, an explicit EULA decision,
and backup copies. See `../../PORT_STATUS_LATEST.md` for the complete scope and
release gates.
