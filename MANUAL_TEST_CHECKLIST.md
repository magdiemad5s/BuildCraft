# BuildCraft Neo - Forge 1.20.1 manual test checklist

Use this checklist only after the automated build and packaged-resource gates
pass. Test a copied world in an isolated instance. Never use the only copy of a
world, replace a loaded JAR, or interrupt a Minecraft client already in use.

A source file, unit test, successful compile, visible pipe connection, or GUI
opening is not enough to mark a gameplay system working.

## Test record

- [ ] Candidate JAR filename:
- [ ] Candidate JAR byte size:
- [ ] Candidate JAR SHA-256:
- [ ] Git commit:
- [ ] Minecraft: `1.20.1`.
- [ ] Forge: `47.4.22`.
- [ ] Java: 17.
- [ ] Test date and tester:
- [ ] Fresh profile path recorded.
- [ ] Exact optional-mod filenames/hashes recorded.
- [ ] Automated `test`, `runData`, `runGameTestServer`, `build`, and validator
  reports archived.

Do not start manual approval if the artifact record is incomplete.

## 1. Base startup and registration

- [ ] Start a clean Forge 1.20.1 profile containing Forge and only the candidate
  BuildCraft Neo JAR.
- [ ] Reach the title screen without a crash, missing dependency, duplicate mod,
  client-only class on server, mixin, registry, or datapack error.
- [ ] Confirm all eight legacy module IDs load: `buildcraftlib`,
  `buildcraftcore`, `buildcraftbuilders`, `buildcraftenergy`,
  `buildcraftfactory`, `buildcraftsilicon`, `buildcrafttransport`, and
  `buildcraftrobotics`.
- [ ] Create a fresh Creative world and a fresh Survival world.
- [ ] Start a dedicated server with the same JAR and join it from a matching
  client.
- [ ] Start the base mod without JEI, Jade, IC2, TOP, or any other optional mod.
- [ ] Inspect `latest.log` for registry replacement, missing mapping, recipe,
  tag, model, texture, atlas, shader, packet, or data-fixer errors.

## 2. Creative tabs, items, blocks and resources

- [ ] Every BuildCraft Neo creative tab appears once with its expected icon and
  localized title.
- [ ] All 116 registered modern items exposed by the static audit are visible
  in the appropriate creative tab; compare against the generated identity
  report rather than memory.
- [ ] Search Creative inventory for representative content from every module:
  gears/tools, markers, engines, machines, pipes, wires/plugs, silicon parts,
  builders, robots and zone tools.
- [ ] No creative entry is an air item, invisible stack, duplicate registry ID,
  missing name, or purple/black missing-model cube.
- [ ] Place every registered block once; verify collision, outline, item model,
  world model, breaking particles, sound, hardness and correct dropped item.
- [ ] Check pipe items in inventory and in-hand: center and end sections use the
  correct distinct textures where the original model requires them.
- [ ] Cycle day/night, rain, graphics settings, mipmaps and GUI scale; look for
  transparent-sort, tint, z-fighting or brightness errors.
- [ ] Press F3+T in-world and while a BuildCraft GUI is open; all assets reload
  and the client remains usable.
- [ ] Repeat resource reload with a normal 16x resource pack. Treat OptiFine or
  renderer-mod compatibility as a separate declared support target.
- [ ] Confirm animations play at the intended speed and do not report invalid
  frame indexes.

## 3. Recipes, tags and identity

- [ ] Craft representative recipes from every module in Survival.
- [ ] Verify shaped orientation, ingredients, remainders, output counts, NBT and
  unlock behavior against the original source/JAR.
- [ ] Confirm gear progression, pipe waterproofing, wire/pluggable recipes,
  engines, tanks, machines, builders, silicon tables and robotics recipes.
- [ ] Confirm recipe-book placement neither creates phantom ingredients nor
  consumes the wrong stack.
- [ ] Confirm Forge item/fluid/block tags resolve and interoperate with tagged
  vanilla or third-party equivalents where intended.
- [ ] Check `/give` for preserved legacy IDs and declared migration aliases.
- [ ] Save/reload crafted NBT-bearing items such as lists, blueprints,
  templates, gates, facades, wires, guide notes and robot boards.

## 4. Item pipes and transport

- [ ] Place `pipe_holder` with item pipes in every direction; connections,
  collision boxes, caps and neighbor updates are correct.
- [ ] Test wood extraction plus cobblestone, stone, quartz, iron, sandstone,
  clay, gold, diamond, diamond-wood, daizuli, emzuli, lapis, obsidian, stripes
  and void item behaviors where registered.
- [ ] Verify pipe families that should not connect to each other remain
  disconnected; valid families connect on the correct face.
- [ ] Insert single items, partial stacks, full stacks, damaged tools, NBT items
  and items with container remainders.
- [ ] Check extraction rate, travel speed, routing choice, color filtering,
  directional output, bouncing and overflow against original behavior.
- [ ] Fill every destination. Items must wait, reroute, return or drop according
  to pipe behavior—never disappear or duplicate.
- [ ] Break a pipe while items travel; all items and pipe/pluggable contents are
  accounted for exactly once.
- [ ] Save, quit and reload while items are in motion. Repeat at a chunk border
  and after unloading/reloading both ends.
- [ ] Disconnect and reconnect a destination during travel; no world lock,
  divide-by-zero, empty-output crash, duplication or permanent ghost item.
- [ ] Test Filtered Buffer insertion/extraction, sided rules, GUI filters,
  shift-click and persistence.
- [ ] Test Diamond, Diamond-Wood and Emzuli pipe GUIs for every color/filter
  slot and server-side routing result.
- [ ] Run a high-throughput loop long enough to expose packet/tick growth; item
  count before and after must match exactly.

## 5. Fluid pipes, tanks and Factory fluids

- [ ] Fill a Tank from a vanilla water bucket. Confirm 1,000 mB transfer,
  correct container result, visible level, GUI amount and comparator output.
- [ ] Fill to the Tank's 16,000 mB capacity; additional fill is rejected without
  deleting fluid.
- [ ] Drain through a bucket and through a fluid capability; rendered level,
  menu, comparator and saved NBT update on all clients.
- [ ] Stack Tanks vertically. Liquids settle/fill from the bottom; gases use the
  intended inverse order; incompatible fluids are rejected.
- [ ] Break/re-place, chunk unload/reload and restart with partial/full Tanks;
  contents neither vanish nor duplicate.
- [ ] Test wood extraction plus every registered passive/directional fluid pipe
  family with water, oil, each fuel/heat variant and another Forge fluid.
- [ ] Confirm side connections match actual transfer capability. A rendered
  connection alone is not a pass.
- [ ] Confirm simulate and execute move the same bounded amount, respect source
  and destination capacity, and apply back-pressure.
- [ ] Test mixed-fluid junctions, full destinations, invalid handlers, removal
  mid-transfer, chunk borders and save/reload.
- [ ] Test void fluid behavior only deletes the accepted amount and never drains
  on simulation.
- [ ] Test Pump source selection, finite scan, power/redstone control, output
  back-pressure, reload and nearby multiple fluids.
- [ ] Test Flood Gate placement, redstone behavior, blocked outputs, chunk
  unload and no duplication.
- [ ] Test Mining Well fluid handling and its interaction with pipes/tanks.
- [ ] Test Chute sided item/fluid behavior and inventory persistence.
- [ ] Test Auto Workbench input, output, damaged tools, container items, blocked
  output, recipe changes, chunk reload and shift-click.
- [ ] Test Distiller valid/invalid recipes, both tanks, heat/power, progress,
  output limits, reload and GUI synchronization.
- [ ] Test Heat Exchanger valid/invalid fluid pairs, direction, heat state,
  throughput, blocked outputs, reload and GUI synchronization.

## 6. Engines, MJ and Forge Energy

- [ ] Place and orient Wood, Stone/Redstone, Iron/Combustion, Creative and RF
  engine paths where exposed; piston/model orientation and blockstate update.
- [ ] Verify redstone start/stop, warm-up, color/heat stages, output direction,
  wrench rotation, save/reload and comparator behavior.
- [ ] Wood engine extracts only from valid targets and does not power-loop or
  create energy from nothing.
- [ ] Stone engine consumes its intended fuel, produces the expected MJ rate,
  stops when starved and handles blocked output safely.
- [ ] Combustion engine consumes fuel and coolant, displays a non-zero rate
  while producing, heats/cools correctly, and reaches its intended failure
  behavior without an infinite energy store.
- [ ] One bucket of water is not treated as infinite coolant; water use is
  observable and persisted.
- [ ] Engine chaining transfers only the allowed amount and cannot amplify
  energy, overflow a counter, or bypass per-tick/capacity limits.
- [ ] MJ pipe buffers are finite. Test normal load, full destination, branch,
  reconnect, chunk unload and deliberately excessive third-party offers.
- [ ] RF/FE pipes connect to valid Forge Energy endpoints and to intended
  BuildCraft adapters. Test receive-only, extract-only, both, neither and
  simulation.
- [ ] FE<->MJ conversion conserves energy under rounding; repeat small pulses and
  maximum-sized offers.
- [ ] MJ Dynamo and RF engine GUIs show correct stored energy, rate and selected
  unit. No screen should label an RF/FE value as MJ when RF display is selected.
- [ ] Reload and reconnect never reset to an unbounded buffer or replay a prior
  transfer.

## 7. Quarry, Mining Well and builders

- [ ] Place Quarry using valid and invalid marker rectangles, including minimum
  and large sizes, rotated layouts, height limits and a chunk boundary.
- [ ] Quarry constructs frame/drill correctly, obeys power intake caps, mines
  the intended area, exports drops, handles lava/water/snow and stops cleanly.
- [ ] A completed, broken, moved, saved or reloaded Quarry releases chunk
  tickets and does not resume stale work or leave permanent collision blocks.
- [ ] Mining Well descends, mines/exports items, handles bedrock/fluids, obeys
  power/redstone and cleans its pipe on removal/reload.
- [ ] Architect records an area with correct rotation/mirror, tile NBT and
  excluded-block policy.
- [ ] Builder consumes materials and power transactionally, respects placement
  order and pauses safely when items, power, permissions or output space are
  missing.
- [ ] Filler and Filler Planner apply each supported pattern to the exact
  selection; progress, required materials and power remain correct after
  save/reload.
- [ ] Replacer changes only matching blocks, consumes the correct input, exports
  drops and preserves filters after close/reload.
- [ ] Builder Excavate toggle visibility, packet, persistence and real behavior
  match the selected state.
- [ ] Electronic Library reads/writes blueprint/template data without corrupting
  or duplicating it; page/selection state remains synchronized.
- [ ] Test permissions in spawn protection and, if available, a claim-mod test
  area. Machines must not bypass Forge break/place events or ownership rules.
- [ ] Test cancel, block break, dimension unload, server restart and second
  player interaction during each builder task.
- [ ] At coordinates near +/-1,000,000 and +/-10,000,000, previews, lasers,
  frames and quarry rendering remain camera-relative without jitter/explosion.

## 8. Silicon, gates, plugs, facades and wires

- [ ] Assembly Table receives laser power, discovers correct recipes, consumes
  exact inputs and produces exact outputs.
- [ ] Swap Assembly Table inputs between two recipe sets with the same number of
  choices. The open GUI must refresh names/icons/buttons (#4751).
- [ ] Advanced Crafting Table selects the correct recipe, handles remainders,
  damaged tools, automation, blocked output, shift-click and reload.
- [ ] Integration, Programming and Charging Tables process valid inputs only,
  expose intended sided automation and persist progress/inventory.
- [ ] Lasers connect/orient, distribute bounded power, stop when idle/blocked
  and render correctly at distance and after reload.
- [ ] Create and use chipsets, redstone crystal, gates, gate copier, lenses,
  light sensors, pulsars and timers.
- [ ] Gate statements/actions and parameters synchronize, rotate with the pipe,
  save/reload and remain server-authoritative.
- [ ] Facades copy the intended block/state, rotate/select phases correctly,
  render each side, serialize and swap without item duplication.
- [ ] Plugs block only intended connections; removal returns the exact item.
- [ ] Place every wire color on every side, connect/split/merge networks, rotate
  blocks and save/reload.
- [ ] Wire networks retain collision-free IDs across restart and reconnect;
  signals neither cross unrelated networks nor become permanently stuck.

## 9. Robotics, guides, lists and core tools

- [ ] Spawn/craft every robot and board variant exposed by the port; item and
  entity textures, names and boards resolve.
- [ ] Test station/docking, charging/power, inventory extraction/insertion,
  requester tasks, pathing, interruption, death/drop and restart persistence.
- [ ] Robots do not lose/duplicate carried items when targets fill, disappear,
  unload or move across chunk boundaries.
- [ ] Zone Planner create/edit/delete/select operations match the visible zone
  and synchronize to another client.
- [ ] Requester quantities, filters, cancel/retry and GUI state remain
  server-authoritative and persistent.
- [ ] Open Guide and Guide Note, follow links, navigate history, scroll/search,
  close/reopen and press F3+T.
- [ ] A guide line containing literal `%` renders safely (#4735).
- [ ] Guide text containing literal `\n` becomes a line break and wraps inside
  the page (#4743).
- [ ] Test List add/remove plus Same Type and Same Material with ordinary items,
  tools, damage, NBT and empty entries; no crash (#4740).
- [ ] Test wrench, paintbrush, map location, goggles, volume/path markers,
  springs, power tester and debugger behavior, NBT and permissions.
- [ ] Save/restart marker networks and verify all five legacy `SavedData` names
  retain the correct data without cross-world leakage.

## 10. GUI completeness

Use [GUI_PARITY_MANIFEST.md](GUI_PARITY_MANIFEST.md) and test all 28 registered
container screens plus both Guide modes.

- [ ] Every screen uses the correct original GUI sheet, dimensions, inventory
  position, labels, gauges, buttons, tooltips and progress indicators.
- [ ] Every slot accepts/rejects the intended stack and all shift-click paths
  terminate without loss, duplication or loops.
- [ ] Buttons hide/show, enable/disable and render hover/pressed states correctly.
- [ ] Long and Unicode translations wrap or ellipsize without covering controls;
  the full value remains accessible by tooltip where needed.
- [ ] Test GUI scales 1, 2, 3, 4 and Auto at a small and large window size.
- [ ] Two players can view the same machine and see live inventory, tank,
  progress, energy, mode and button updates.
- [ ] Walk out of range, teleport/dimension-change, break the block and disconnect
  with a menu open; invalid actions are rejected and no crash occurs.

## 11. Oil, fuels and world generation

- [ ] Generate a new world using default settings and locate small, medium and
  large oil deposits. Small deposits must be reachable with default policy.
- [ ] Test each documented enable/disable/configuration combination, including
  biome lists, dimensions, deposit size/rarity and retrogen settings.
- [ ] Confirm disabled worldgen creates no new deposits and enabled settings do
  not ignore biome/dimension restrictions.
- [ ] Inspect deposit shape, surface spout, fluid blocks, chunk edges, caves,
  oceans and neighboring structures.
- [ ] Collect/refine oil through the intended Pump/pipe/Distiller chain and
  verify all fluid/heat variants, recipes, burn values and translations.
- [ ] Restart, explore new chunks and run any supported retrogen on a copied
  world; no duplicate generation or SavedData corruption.

## 12. Dedicated server and multiplayer synchronization

- [ ] Dedicated server reaches “Done” without loading client rendering classes.
- [ ] Join with two clients and repeat representative pipe, engine, machine,
  builder, silicon, robotics and GUI actions.
- [ ] A non-owner/non-op cannot forge menu buttons, slot indices, coordinates,
  quantities, zones, builder actions or machine state through packets.
- [ ] Teleport/dimension change, death/respawn, logout/login and server restart
  resynchronize all visible state.
- [ ] Watch distant chunks: packets are interest-limited and do not broadcast
  every pipe/machine update to every player.
- [ ] Break/replace a synchronized block while packets are in flight; no stale
  packet mutates the replacement.
- [ ] Run a 30-60 minute transport/machine soak with two clients; record tick
  time, memory growth, packet rate and entity/item counts.

## 13. IC2 fluid interoperability

BuildCraft Neo must start without IC2. Treat the user's Forge 1.20.1 IC2 port as
an optional compatibility smoke test, not a required dependency.

- [ ] First confirm the same IC2 Pump and IC2 fluid-pipe setup fills an IC2 or
  other known-good Forge tank.
- [ ] Connect powered IC2 Pump -> IC2 fluid pipe -> BuildCraft Tank. Wait at
  least one second; verify a non-zero amount in the Tank GUI and after reload.
- [ ] Connect IC2 Pump -> BuildCraft fluid pipe -> BuildCraft Tank if both mods
  expose standard Forge fluid capabilities on those faces.
- [ ] Test both input/output faces, full destination, stopped power, pipe removal,
  chunk reload and server restart.
- [ ] Verify exact fluid conservation and no repeated transfer from a one-bucket
  source.
- [ ] Remove IC2 and restart the same base-mod test profile; BuildCraft Neo still
  loads without missing-class or mandatory-dependency errors.

## 14. Persistence and backed-up 1.12.2 migration

- [ ] In a new 1.20.1 world, save/reload every block entity with representative
  inventory, fluid, energy, mode, progress, owner and network state.
- [ ] Repeat after chunk unload, server restart, block rotation and neighboring
  block changes.
- [ ] Verify legacy fluid NBT forms, block-entity NBT and unnamespaced values are
  read without deleting valid content and are rewritten safely when saved.
- [ ] Verify registry aliases resolve old block/item/block-entity/fluid IDs and
  do not map an old ID to the wrong modern object.
- [ ] Make a separate verified backup of the 1.12.2 world and record its source
  JAR hash plus exact dependency set.
- [ ] Load only a copy in a dedicated migration profile; never open the original.
- [ ] Inspect every BuildCraft area before saving: pipes, wires, engines,
  machines, tanks/fluids, builders, markers, blueprints, robots and worldgen.
- [ ] Save, fully restart and inspect again. Compare inventories, amounts,
  networks, ownership, tasks, modes and coordinates against the backup.
- [ ] Treat any missing mapping, removed block entity, empty tank/inventory,
  changed registry ID or crashed chunk as a release blocker.

## 15. Release approval

- [ ] No known P0/P1 regression in the issue ledger remains untested.
- [ ] No unexplained error/warning appears in client, dedicated-server,
  GameTest, data generation or resource-reload logs.
- [ ] All failed checklist items have a reproducible issue with logs, coordinates,
  screenshots and exact JAR hash.
- [ ] Re-run automated tests and validators after the final fix; do not reuse an
  older report.
- [ ] Inspect the final packaged JAR, parse packaged JSON and verify the logo,
  module metadata, resources and licenses/notices.
- [ ] Record final filename, byte size and SHA-256 in `PORT_STATUS_LATEST.md` and
  release notes.
- [ ] Only then copy the candidate to the user's test instance while Minecraft
  is fully closed.

Until every applicable item is checked against one exact artifact, the result
is a development candidate—not a bug-free BuildCraft Neo release.
