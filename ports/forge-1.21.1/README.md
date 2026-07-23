# BuildCraft Neo Better - Forge 1.21.1

This independent Java 21 / Forge 52.1.16 lane now includes the verified
`buildcraftfactory:tank` vertical slice. It retains the eight legacy module IDs
and applies the modern 1.21 interaction, persistence, rendering, capability,
menu, and data-path APIs.

Verified artifact:

`build/libs/buildcraft-neo-better-forge-1.21.1-0.1.0-dev+1.21.1.jar`

- 38,841 bytes
- SHA-256 `E1005452D67AB399E081DEE517017A39A8DE9D866F2A2808D2E75CAC9FDE44B8`
- Offline build PASS: 6 JUnit tests and packaged-resource validation passed.

Use the isolated cache only:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat build --offline --no-daemon
.\tools\verify-packaged-resources.ps1 -JarPath .\build\libs\buildcraft-neo-better-forge-1.21.1-0.1.0-dev+1.21.1.jar
```

No live Minecraft process was used. This Tank slice is not a complete BuildCraft
release; see `STATUS.md` and `../../PORT_STATUS_LATEST.md` for remaining work.
