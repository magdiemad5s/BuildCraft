# BuildCraft Neo Better - Forge 1.20.1

This isolated Java 17 / Forge 47.4.22 lane contains the first verified
`buildcraftfactory:tank` gameplay slice and the bounded 1.20.1 energy
foundation. It preserves the eight legacy module IDs and `buildcraftfactory`
registry identity for the Tank.

Verified artifact:

`build/libs/buildcraft-neo-better-forge-1.20.1-0.1.0-dev+1.20.1.jar`

- 52,151 bytes
- SHA-256 `F53680631A152AB0AD616BA2DE472FC3381A33E2625893B2F8C339461578BB6E`
- `build --offline --no-daemon`: PASS; 31 JUnit tests and packaged-resource
  validation passed.

Use a lane-local Gradle cache:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat build --offline --no-daemon
.\tools\verify-packaged-resources.ps1 -JarPath .\build\libs\buildcraft-neo-better-forge-1.20.1-0.1.0-dev+1.20.1.jar
```

This is not a complete BuildCraft release. Do not run a client/server/GameTest
or test old worlds without a disposable instance, an explicit EULA decision,
and backup copies. See `../../PORT_STATUS_LATEST.md` for the complete scope and
release gates.
