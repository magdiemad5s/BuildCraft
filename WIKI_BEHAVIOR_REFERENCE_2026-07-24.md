# BuildCraft behavior reference and Forge 1.20.1 scope

Updated: 2026-07-24

## Authority order

The community [Minecraft BuildCraft Wiki](https://minecraftbuildcraft.fandom.com/wiki/Minecraft_Buildcraft_Wiki)
is useful for player-facing workflows and manual test ideas. It identifies
itself as unsupported by the BuildCraft developers and mixes historical
versions, so it is not authoritative for exact IDs, NBT, rates, recipes or save
formats.

Use evidence in this order:

1. Preserved BuildCraft 1.12.2 source.
2. Released `buildcraft-all-8.0.0.jar` and its resources.
3. [`legacy-1.12.2-contract/LEGACY_IDENTITY_MANIFEST.json`](legacy-1.12.2-contract/LEGACY_IDENTITY_MANIFEST.json).
4. Original issue reports and accepted upstream fixes.
5. Wiki behavior descriptions, checked against the sources above.
6. Community Edition as a modern API/reference implementation, never as an
   unquestioned behavior or compatibility baseline.

Useful behavior entry points include [Tank](https://minecraftbuildcraft.fandom.com/wiki/Tank),
[Pipes](https://minecraftbuildcraft.fandom.com/wiki/Pipes),
[Engines](https://minecraftbuildcraft.fandom.com/wiki/Engines),
[Pump](https://minecraftbuildcraft.fandom.com/wiki/Pump), Quarry, Builder,
Filler, Assembly Table, Distiller, Heat Exchanger and Robotics pages.

## Current source versus old Tank artifacts

The `0.1.x-dev` release directories contain historical Tank-focused test JARs.
They explain why an earlier test instance showed only a Tank, but they do not
represent the current Forge 1.20.1 source.

The active `ports/forge-1.20.1` lane now contains modern source for all eight
legacy modules, including pipes, engines, Factory machines, builders/quarry,
silicon, robotics, GUIs, resources, worldgen and migration helpers. Static
identity, creative-provider, GUI, texture/model and language audits cover that
source surface.

The latest source has not yet passed the complete current build/GameTest/client
and manual matrix. Therefore “restored in source” must not be rewritten as
“fully working” until [the manual checklist](MANUAL_TEST_CHECKLIST.md) passes.

## IC2 compatibility evidence

Earlier testing established that the same IC2 Pump and fluid-pipe setup filled
a different tank, isolating the original empty BuildCraft Tank endpoint. The
Forge Tank fix restored capability revival, update synchronization and direct
container priority. The user subsequently observed the BuildCraft Tank filling
and an IC2 fluid pump transferring into it.

That is useful prior smoke-test evidence, not blanket compatibility approval for
the current full-source candidate. Repeat against the exact new JAR and record:

- IC2 and BuildCraft JAR names/hashes;
- powered IC2 Pump -> IC2 pipe -> BuildCraft Tank;
- a non-zero Tank GUI amount and exact source/destination conservation;
- full-destination, stopped-power, pipe-removal, chunk-reload and restart cases;
- base BuildCraft startup again after IC2 is removed.

A visible pipe connection alone is never transfer proof. IC2 must remain an
optional dependency.

## Wiki-derived behavior contracts

| System | Player-facing contract | Forge 1.20.1 source state | Required proof |
|---|---|---|---|
| Tanks | 16 buckets/16,000 mB, containers and pipes, vertical liquid/gas ordering, visible level and comparator | Restored | Fill/drain/stack/save/two-client/IC2 tests |
| Item pipes | Extraction, transport, filtering, colors, routing, speed, voiding, pickup/placement and overflow remain distinct | Restored | Every family, full routes, break/reload and no-loss/no-duplication |
| Fluid pipes | Real side capabilities, extraction/direction, exact flow, back-pressure and mixed-fluid rejection | Restored | Simulate/execute parity, full destination, chunk/restart and third-party endpoint |
| MJ and RF/FE pipes | Bounded transfer, intended connection rules and conservative conversion | Restored | Finite buffers, hostile offers, chain/branch/reload and live machine endpoints |
| Engines | Fuel/redstone/cooling/heat/output are functional rather than decorative | Restored | Real rates, fuel/water use, finite store, failure state, GUI and persistence |
| Factory | Pump, Flood Gate, Mining Well, Chute, Tank, Auto Workbench, Distiller and Heat Exchanger process real state | Restored | Recipes/rates, inventories/tanks, power, sided automation, reload and multiplayer |
| Builders | Quarry, Architect, Builder, Filler/Planner, Replacer, Library, markers and blueprints place/mine transactionally | Restored | Power/material accounting, permissions, chunk tickets, completion and persistence |
| Silicon | Lasers/tables/chipsets/gates/plugs/facades/wires keep recipes, controls and server authority | Restored | Recipe refresh, automation, serialization, rotation and two-client GUI sync |
| Robotics | Robots, boards, zones, requester/station paths and tasks keep inventory and persistence | Restored | AI/docking/charging, full targets, death/unload/restart and multiplayer |
| Oil/worldgen | Configured small/medium/large deposits and oil/fuel processing | Restored | Default small deposit, full config matrix, new chunks/retrogen and refinery chain |
| GUIs/guides | Original controls, gauges, text, wrapping, inventory behavior and server sync | 28/28 menu/screen registrations plus Guide modes | Visual comparison, interaction, literal `%`/`\n`, F3+T, scaling and multiplayer |

## Cross-check rules

- Preserve exact registry IDs, namespaces, NBT keys, `SavedData` names, packet
  meanings, recipes/tags and block-entity formats even if a wiki uses a newer
  display name.
- When BC7 and BC8 behavior differ, record which preserved source/JAR behavior
  is the chosen baseline; do not silently combine balance values.
- Do not invent missing custom sounds: the original source and released JAR
  contain none.
- Do not interpret a present texture, recipe, class or creative item as proof
  that the corresponding machine works.
- Keep optional integrations isolated and prove base startup without them.
- Test migration only on a verified copy of a world.

Concrete regressions found through the issue tracker and community comments are
recorded in [the issue ledger](ISSUE_PORT_LEDGER_2026-07-23.md). Runtime steps
belong in [the manual checklist](MANUAL_TEST_CHECKLIST.md), not in unsupported
release claims.
