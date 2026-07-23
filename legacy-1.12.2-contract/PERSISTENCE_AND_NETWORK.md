# Persistence and network compatibility manifest — seed inventory

## SavedData names

- `buildcraft_wire_systems`
- `buildcraft_volume_boxes`
- `buildcraft_marker_volume`
- `buildcraft_marker_path`
- `buildcraft_world_gen`

## Base block-entity schema

`common/buildcraft/lib/tile/TileBC_Neptune.java` writes `data-version` `2` and
uses `deltas`, `owner`, `items`, and `tanks`; it also migrates legacy `tank`.
Maintain a documented codec/migration for these fields before any old-world
claim.

The active source has 822 NBT call sites and 268 unique literal NBT strings.
Dynamic key patterns include `tank[direction]`, `in[index]`, `slotColors[index]`,
`layer_0` through `layer_15`, `trigger[slot]`, `action[slot]`, and face-name
keys below `plugs`.

## Legacy packet registrations

| Module | Messages / discriminator range |
|---|---|
| `buildcraftlib` | container, debug request/response, marker, object-cache request/response, tile update (`0`–`6`) |
| `buildcraftcore` | volume boxes (`0`) |
| `buildcraftbuilders` | snapshot request/response (`0`–`1`) |
| `buildcraftrobotics` | zone-map request/response (`0`–`1`) |
| `buildcrafttransport` | multi-pipe item, wire systems, wire systems powered (`0`–`2`) |

Modern packet registration may use different transport primitives, but it must
preserve direction, validation, payload bounds, and semantic meaning. Every
state-changing packet must validate the current menu/target/side on the server.
