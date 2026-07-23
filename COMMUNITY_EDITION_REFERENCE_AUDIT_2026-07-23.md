# Community Edition reference audit

Audited read-only reference:
`references/CurativeTree-BuildCraft-1.20.1` at commit
`8f9924b490c009474d3b2c786bb9ed8504c77358` (`beta3`), branch
`shipovskijkorp-1.20.1-main`.

This repository is a useful Forge 1.20.1 **reference**, not an adopted
implementation or a production baseline for BuildCraft Neo Better. It contains
one commit, no JUnit/GameTest source tree, and no runtime validation performed
by this project.

## Useful reference coverage

It has modern source for all major legacy areas: API/library, core, Factory
(Tank/Pump/Mining Well/Flood Gate/Chute/Distiller/Heat Exchanger/Auto
Workbench), Transport (pipes/gates/wires/facades), Energy (engines/fuels/oil
worldgen), Builders (Quarry/Filler/Builder), Silicon (tables/lasers/gates), and
Robotics (entity/AI/boards/zones). It also demonstrates module-scoped
`DeferredRegister` aggregation, menu registration, client-only renderer
registration, `ModelEvent.ModifyBakingResult`, atlas-source JSON, and
data-driven 1.20.1 worldgen.

JEI and Jade are isolated optional integrations. IC2 Classic is explicitly
disabled for 1.20.1. No Create, Mekanism, Curios, or TOP integration was found.

## Do not copy these defects

1. **Power is not a valid port template.**
   `PipeFlowPower.tryExtractPower()` is a TODO returning zero; per-side power
   buffers are unbounded; Forge ENERGY is not exposed; `MjBattery` does not
   enforce capacity or safe NBT bounds; and the sole `BCEnergyStorage` is both
   unused and contains a `canRecive` spelling defect. BuildCraft Neo Better's
   bounded energy foundation is retained instead.

2. **Small oil deposits are unreachable by default.**
   The generator chooses the small/Lake variant only for `oilBiome`, while the
   default configured feature has an empty `surfaceDepositBiomes` list. This
   explains the reported missing-small-spout complaint and must be corrected
   when worldgen is ported.

3. **Permission handling is unsafe.**
   `PermissionUtil` returns true for every overload and does not apply Forge
   break/place events, spawn protection, or claim-mod logic. Quarry, Builder,
   Filler, and Replacer need explicit protection event and claim-mod tests.

4. **Client packet dispatch is suspicious.**
   Its clientbound message path rejects a null packet sender before invoking
   the client handler; normal clientbound packets commonly have no sender.
   Treat this as a synchronization-risk candidate, not reusable protocol code.

5. **Other explicit gaps:** Obsidian fluid extraction is a TODO, and the typed
   recipe-book constructor throws `AbstractMethodError`.

## Build evidence boundary

The isolated reference compilation reached classes, but Windows reobfuscation
failed because the long workspace path exceeded the operating-system command
line limit. No `reobfJar/output.jar` exists, so its generated JAR is not a
runtime-proven distributable artifact.

## Safe reuse rules

- Preserve BuildCraft Neo Better's legacy ID/NBT/SavedData/packet manifest;
  Community Edition IDs differ in places, notably some block-entity IDs.
- Recreate behavior with the current loader APIs rather than copy source or
  assets blindly. Any copied MPL-2.0 material requires file-level attribution
  and license compliance.
- Validate client/server startup, resource reload, multiplayer, protection,
  worldgen, and backed-up-world migration before making compatibility claims.
