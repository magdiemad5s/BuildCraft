# BuildCraft Neo - Forge 1.21.1

This independent Java 21 / Forge 52.1.16 lane now includes the verified
`buildcraftfactory:tank` vertical slice. It retains the eight legacy module IDs
and applies the modern 1.21 interaction, persistence, rendering, capability,
menu, and data-path APIs.

Verified artifact:

`build/libs/buildcraft-neo-better-forge-1.21.1-0.1.1-dev+1.21.1.jar`

- 41,784 bytes
- SHA-256 `72A4C38E1EC3076CB21C33CC3BBD60A673706D23F75B0B59727EE57CE2566EDE`
- Offline build PASS: 6 JUnit tests and packaged-resource validation passed.

Use the isolated cache only:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat build --offline --no-daemon
.\tools\verify-packaged-resources.ps1 -JarPath .\build\libs\buildcraft-neo-better-forge-1.21.1-0.1.1-dev+1.21.1.jar
```

No live Minecraft process was used. This Tank slice is not a complete BuildCraft
release; see `STATUS.md` and `../../PORT_STATUS_LATEST.md` for remaining work.
