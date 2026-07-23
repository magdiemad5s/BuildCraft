# BuildCraft Neo Better 0.1.0-dev test bundle

These are development test JARs, not a full BuildCraft release. Use exactly
one JAR matching the Minecraft version and loader in your disposable test
profile:

| Minecraft / loader | Test file |
|---|---|
| 1.20.1 Forge | `buildcraft-neo-better-forge-1.20.1-0.1.0-dev+1.20.1.jar` |
| 1.20.1 NeoForge | `buildcraft-neo-better-neoforge-1.20.1-0.1.0-dev+1.20.1.jar` |
| 1.21.1 Forge | `buildcraft-neo-better-forge-1.21.1-0.1.0-dev+1.21.1.jar` |
| 1.21.1 NeoForge | `buildcraft-neo-better-neoforge-1.21.1-0.1.0-dev+1.21.1.jar` |

The implemented gameplay slice is the `buildcraftfactory:tank` block. It has
been compiled, unit-tested, and checked for packaged metadata/resources in all
four lanes. Pipes, engines, machines, world generation, most GUIs, multiplayer,
and legacy-world migration are not complete.

Before testing, verify the selected JAR against `SHA256SUMS.txt`, use a fresh
profile, and keep old worlds backed up. Do not report this as a stable full
BuildCraft release.
