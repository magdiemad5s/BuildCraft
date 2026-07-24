# BuildCraft behavior reference and port scope

Updated: 2026-07-24

## How this reference is used

The community [Minecraft BuildCraft Wiki](https://minecraftbuildcraft.fandom.com/wiki/Minecraft_Buildcraft_Wiki)
is a player-facing behavior and test reference. The preserved 1.12.2 source,
the reference JAR, and
[`legacy-1.12.2-contract/LEGACY_IDENTITY_MANIFEST.json`](legacy-1.12.2-contract/LEGACY_IDENTITY_MANIFEST.json)
remain authoritative for registry IDs, NBT, packets, recipes, rates, and save
compatibility. Wiki pages can describe historical versions, so every modern
implementation must be cross-checked against that legacy source.

Relevant behavior pages: [Tank](https://minecraftbuildcraft.fandom.com/wiki/Tank),
[Pipes](https://minecraftbuildcraft.fandom.com/wiki/Pipes),
[Engines](https://minecraftbuildcraft.fandom.com/wiki/Engines), and
[Pump](https://minecraftbuildcraft.fandom.com/wiki/Pump).

## What the IC2 test established

The same IC2 pump and IC2 fluid-pipe setup fills a different tank. IC2 is
therefore not the primary cause of the observed empty BuildCraft Tank; the old
BuildCraft Tank endpoint is at fault.

Forge `0.1.3-dev` targets that exact endpoint: it recreates an invalidated
`LazyOptional<IFluidHandler>` in `FactoryTankBlockEntity.reviveCaps()`, sends
Tank update-tag state to clients, and restores container-to-Tank priority.
This is a fix candidate, not a runtime-proven claim. Test only this JAR after
fully closing Minecraft:

`releases/0.1.3-dev/buildcraft-neo-better-forge-1.20.1-0.1.3-dev+1.20.1.jar`

The acceptance check is a powered IC2 pump -> IC2 fluid pipe -> BuildCraft
Tank, followed by a non-zero fluid amount in the Tank GUI. A visible connection
is not evidence of a working transfer.

## Why only the Tank appears

The current Forge 1.20.1 artifact is deliberately a Tank-only vertical slice,
not a complete port with missing creative-tab wiring. It compiles only
`ports/forge-1.20.1/src/main`; preserved legacy code under `common/` is not
registered as modern gameplay. Compatibility module IDs are scaffolding.

| Legacy module | Legacy items / blocks / block entities | Current Forge 1.20.1 |
|---|---:|---|
| Lib | 3 / 0 / 0 | Bootstrap only |
| Core | 20 / 6 / 5 | Bootstrap only |
| Builders | 10 / 7 / 6 | Bootstrap only |
| Energy | 2 / 1 / 5 | Bootstrap plus non-gameplay MJ/FE helpers |
| Factory | 12 / 10 / 8 | `buildcraftfactory:tank` only: 1 / 1 / 1 |
| Silicon | 14 / 6 / 6 | Bootstrap only |
| Transport | 54 / 2 / 2 | No pipes, `pipe_holder`, or filtered buffer |
| Robotics | 1 / 1 / 1 | Bootstrap only |

Only three registry entries are implemented out of 182 preserved entries
(1.65%). The rest of the legacy source remains in
`common/buildcraft/{transport,energy,factory,builders,silicon,robotics}`.
Do not add placeholder entries merely to make the creative inventory look full.

The present Tank is also Creative/command-only: the modern artifact has no
recipe JSON yet. That is a small baseline gap, not an excuse to skip real
transport behavior.

## Wiki-derived behavioral contracts

| System | Contract | Current state |
|---|---|---|
| Tank | 16 buckets / 16,000 mB; vertical stacks, container and fluid-pipe interaction, predictable liquid/gas order. | First slice; IC2 runtime test and recipe remain open. |
| Fluid pipes | Waterproof pipes must move fluids with real side connections and back-pressure. | Not implemented. |
| Item pipes | Extraction, transport, filtering, routing, voiding, pickup, and placement behavior remain distinct. | Not implemented. |
| Engines | Fuel/redstone/cooling and energy behavior must be real, not decorative. | No in-world implementation. |
| Pump | Gather fluid and obey output back-pressure. | Not implemented. |
| Builders/Gates | Quarry, Builder, Filler, markers, blueprints, wires, and gates require persistence and server authority. | Not implemented. |

## Delivery order and release gates

1. Finish the Tank baseline: recipe plus live Forge capability/GameTest.
2. Port a genuine passive fluid transport alpha: `buildcrafttransport:pipe_holder`
   and `buildcrafttransport:pipe_stone_fluid`, including legacy throughput,
   topology, NBT, model/rendering, client sync, drops, and recipe.
3. Prove `IC2 pump -> BC fluid pipe -> BC Tank -> BC fluid pipe -> BC Tank` in
   a disposable 1.20.1 Forge world without adding IC2 as a compile dependency.
4. Add the passive fluid family, then powered extraction and Factory Pump only
   after the MJ/FE gameplay contract exists.
5. Port Factory processing/Energy, item transport, Silicon, Builders, and
   Robotics in dependency order.

Every transport release needs simulate/execute parity, no-loss/no-duplication
tests, full-destination and mixed-fluid checks, save/reload and capability
revival coverage, dedicated-server/client/multiplayer synchronization, F3+T
model reload, and a real IC2 interoperability smoke test.
