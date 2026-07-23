# BuildCraft Neo Better GUI parity manifest

This inventory prevents a successful compile from being mistaken for GUI
parity. The legacy source has 25 machine/pipe menu identifiers plus Library
Guide and Guide Note screens.

| Module | Legacy menus/screens | Current port state |
|---|---|---|
| Core | `LIST` | Not ported |
| Builders | `ARCHITECT`, `BUILDER`, `FILLER`, `LIBRARY`, `REPLACER`, `FILLER_PLANNER` | Not ported |
| Energy | `ENGINE_STONE`, `ENGINE_IRON`, `ENGINE_RF`, `DYNAMO_MJ` | Not ported |
| Factory | `AUTO_WORKBENCH_ITEMS`, `AUTO_WORKBENCH_FLUIDS`, `CHUTE`, `TANK`, `DISTILLER` | `TANK` is functionally implemented in Forge/NeoForge 1.20.1 and 1.21.1; all other Factory menus are not ported |
| Silicon | `ASSEMBLY_TABLE`, `ADVANCED_CRAFTING_TABLE`, `INTEGRATION_TABLE`, `GATE` | Not ported |
| Transport | `FILTERED_BUFFER`, `PIPE_DIAMOND`, `PIPE_DIAMOND_WOOD`, `PIPE_EMZULI` | Not ported |
| Robotics | `ZONE_PLANTER` | Not ported |
| Library | Guide and Guide Note screens | Not ported |

The Tank screen has a real menu and server-authoritative interaction path, but
it has not yet passed visual-parity, client-reload, multiplayer, or old-world
migration signoff. It is the only GUI that can be called implemented.

Every remaining menu/screen is a release gate. Its port must include:

1. A stable server menu with target, distance, ownership, slot, and button
   validation.
2. A client screen preserving controls, visibility, tooltips, literal-percent
   safety, truncation, and state-dependent rendering.
3. Server-authoritative network behavior with a multiplayer synchronization
   regression.
4. Client startup/model inspection, F3+T reload, and manual close/reopen and
   shift-click checks.
