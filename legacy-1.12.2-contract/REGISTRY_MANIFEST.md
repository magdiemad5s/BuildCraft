# Registry compatibility manifest — seed inventory

The checked-out `8.0.1-pre.2` source has 182 active registry-backed entries:

| Kind | Count |
|---|---:|
| Items | 116 |
| Blocks | 33 |
| Tile/block-entity types | 33 |

The registry declarations are owned by these module roots:

| Module | Legacy declaration |
|---|---|
| Lib | `common/buildcraft/lib/BCLib.java:154` |
| Core | `common/buildcraft/core/BCCore.java:106` |
| Builders | `common/buildcraft/builders/BCBuilders.java:79` |
| Energy | `common/buildcraft/energy/BCEnergy.java:78` |
| Factory | `common/buildcraft/factory/BCFactory.java:61` |
| Silicon | `common/buildcraft/silicon/BCSilicon.java:100` |
| Transport | `common/buildcraft/transport/BCTransport.java:139` |
| Robotics | `common/buildcraft/robotics/BCRobotics.java:61` |

Rules for every modern lane:

1. Preserve all 49 `.oldReg(...)` aliases as explicit migration records.
2. Preserve Energy aliases `fluid_block_oil` and `fluid_block_fuel`.
3. Keep intentionally inactive/development-only entries explicitly classified,
   rather than silently registering or dropping them: `diamond_shard`,
   `plastic_block`, `pipe_obsidian_fluid`, `pipe_wood_power_2`,
   `pipe_quartz_power_2`, `goggles`, and `plastic_sheet`.
4. `common/buildcraft/transport/BCTransportPipes.java` has 46 live pipe
   definitions, including nine RF pipe definitions. Each requires an ID,
   serialization, rendering, connection, and gameplay test before release.

This is a seed manifest, not a substitute for a generated per-ID JSON manifest.
The generated manifest must become a packaging gate before the first playable
release.
