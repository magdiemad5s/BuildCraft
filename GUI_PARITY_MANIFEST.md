# BuildCraft Neo GUI parity manifest

Updated: 2026-07-24

This manifest prevents source registration or a successful compile from being
mistaken for a working GUI. The current Forge 1.20.1 static audit reports:

- 28 menu declarations;
- 28 matching client screen registrations;
- 42 literal BuildCraft GUI resource references, all resolved;
- six parsed GUI JSON files;
- 60 GUI PNGs, including all 59 original/released sheets plus the exact-byte
  `buildcraftcore` button compatibility copy;
- zero static GUI parity errors.

These are source/resource facts. Every screen still needs client, interaction,
multiplayer, reload, and visual approval against the latest JAR.

## Registered menu/screen inventory

| Module | Count | Menu -> screen | Source state | Runtime state |
|---|---:|---|---|---|
| Core | 1 | `LIST` -> `GuiList` | Registered and resource-backed | Pending |
| Builders | 6 | `ARCHITECT_TABLE` -> `GuiArchitectTable`; `BUILDER` -> `GuiBuilder`; `ELIBRARY` -> `GuiElectronicLibrary`; `FILLER` -> `GuiFiller`; `FILLER_PLANNER` -> `GuiFillerPlanner`; `REPLACER` -> `ScreenReplacer` | Registered and resource-backed | Pending |
| Energy | 4 | `DYNAMO_MJ` -> `GuiDynamoMJ`; `ENGINE_IRON` -> `GuiEngineIron_BC8`; `ENGINE_RF` -> `GuiEngineRF`; `ENGINE_STONE` -> `GuiEngineStone_BC8` | Registered and resource-backed | Pending |
| Factory | 5 | `AUTO_WORKBENCH_ITEM` -> `GuiAutoCraftItems`; `CHUTE` -> `GuiChute`; `DISTILLER` -> `GuiDistiller`; `HEAT_EXCHANGE` -> `ScreenHeatExchange`; `TANK` -> `GuiTank` | Registered and resource-backed | Pending |
| Silicon | 6 | `ADVANCED_CRAFTING_TABLE` -> `GuiAdvancedCraftingTable`; `ASSEMBLY_TABLE` -> `GuiAssemblyTable`; `CHARGING_TABLE` -> `GuiChargingTable`; `GATE` -> `GuiGate`; `INTEGRATION_TABLE` -> `GuiIntegrationTable`; `PROGRAMMING_TABLE` -> `GuiProgrammingTable` | Registered and resource-backed | Pending |
| Transport | 4 | `FILTERED_BUFFER` -> `GuiFilteredBuffer`; `PIPE_DIAMOND` -> `GuiDiamondPipe`; `PIPE_DIAMOND_WOOD` -> `GuiDiamondWoodPipe`; `PIPE_EMZULI` -> `GuiEmzuliPipe_BC8` | Registered and resource-backed | Pending |
| Robotics | 2 | `REQUESTER` -> `GuiRequester`; `ZONE_PLANNER` -> `GuiZonePlanner` | Registered and resource-backed | Pending |
| **Total** | **28** | **28 declarations -> 28 registrations** | **Static audit PASS** | **Pending** |

The Guide and Guide Note use two modes of `GuideScreen`, opened through client
hooks rather than a `MenuType`; therefore they are additional manual GUI gates
and are not included in the 28 container-menu count.

## Common acceptance checks for every screen

- [ ] Opens only for the correct block/item and on the correct logical side.
- [ ] Server menu validates dimension, distance, block entity, ownership,
  permissions, slot indices, button IDs, and requested quantities.
- [ ] Screen uses the original background/button/gauge artwork with correct
  UVs, scaling, z-order, labels, tooltips, and inventory positions.
- [ ] Controls enable, disable, hide, show, and retain focus at the same states
  as the original behavior.
- [ ] Shift-click, drag, double-click, hotbar swap, recipe placement, and close
  behavior neither lose nor duplicate items/fluids.
- [ ] State changes synchronize to two clients without trusting client-supplied
  machine state.
- [ ] Closing, reopening, walking out of range, changing dimension, breaking the
  block, chunk unloading, and reconnecting are safe.
- [ ] GUI scale 1 through Auto, common window sizes, Unicode locale, and long
  translations do not clip controls or render text outside the window.
- [ ] F3+T keeps the screen usable and produces no missing texture/model/JSON
  errors.

## Issue-specific GUI regressions

- [ ] #4735: guide/document text containing a literal `%` renders without
  `UnknownFormatConversionException`.
- [ ] #4743: literal `\n` in guide content becomes a real line break, then wraps
  within the visible page rather than running off-screen.
- [ ] #4740: List Same Type/Same Material operations do not crash and retain the
  expected match behavior.
- [ ] #4751: swapping Assembly Table input updates the visible recipe choices
  even when the old and new recipe lists have the same length.
- [ ] #4738: when the configured display unit is RF/FE, supported integration
  screens do not label the value as MJ. Optional integration must stay isolated.
- [ ] #4761: the Builder Excavate control is visible only when applicable,
  transmits its state, persists it, and affects the actual build operation.
- [ ] Buttons preserve original visibility and hover/pressed rendering.
- [ ] Ellipsized labels retain a tooltip or other way to read the full text.

## Per-system approval focus

- Core List: add/remove entries, type/material matching, NBT/damage handling,
  scrolling and persistence.
- Builders: blueprint/template selection, mode buttons, excavate toggle,
  planning previews, inventory slots, progress, Library pages and Replacer
  filters.
- Energy: real generated/consumed rates, heat, fuel, coolant, finite buffers,
  configured units and redstone state.
- Factory: tank gauges, fluid names/amounts, recipes, input/output tanks and
  inventories, progress arrows, sided automation and live processing state.
- Silicon: laser power, selectable recipes, recipe refresh, gate statements and
  parameters, chipset/lens/facade results and automation.
- Transport: color/filter matrices, side controls, extraction state and item
  persistence through close/reload.
- Robotics: zone editing, requester filters/amounts, board/task state and
  server-authoritative updates.
- Guide/Guide Note: resource reload, page links, history, search/index, literal
  percent/newline parsing, wrapping, scrolling and note persistence.

A GUI can move from “Pending” only after its checks are recorded against an
exact JAR SHA-256 in [the manual checklist](MANUAL_TEST_CHECKLIST.md).
