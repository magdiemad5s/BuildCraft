# Optional integration contract

`buildcraftcompat` is a separate companion module/JAR with resource namespace
and mod ID `buildcraftcompat`. It must never be a base startup requirement.

The legacy source split and reference JAR show these optional scopes:

- Forestry: Propolis pipe/filter GUI and recipes; preserve filter NBT `filter`
  and the `forestry_propolis` pipe definition when the target API is available.
- CraftTweaker: Assembly Table, Combustion Engine, and Refinery script surfaces.
- JEI: recipe categories and transfer handlers for Auto Workbench, Advanced
  Crafting, Assembly Table, heatable/coolable/distiller, and combustion engine.
- IC2: cable matching and `insulation` / `type` NBT behavior only if a viable
  target ecosystem exists.
- The One Probe and Waila/HWYLA: AutoCraft and laser-target data providers.

Each adapter must compile separately against an optional API and must pass a
base-mod-absent test. No optional API type may be reachable from core common or
dedicated-server startup paths.
