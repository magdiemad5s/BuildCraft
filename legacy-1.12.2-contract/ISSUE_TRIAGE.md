# BuildCraft tracker triage

This is the evidence-backed, port-relevant tracker set captured on 2026-07-23.
It deliberately does not claim to be a complete export of every historical issue:
GitHub rate-limited the focused public audit before a full classification could be
completed. Reports are promoted here only when the tracker, local source, or a
repro supplies enough evidence to plan work.

| Issue | Assessment | Modern-port action |
|---|---|---|
| [#4752](https://github.com/BuildCraft/BuildCraft/issues/4752) | Confirmed P0 world-locking wire crash. A gate/wire plus a pipe at a hash-collision coordinate can crash a server and prevent rejoin. Legacy commit `fe6f44426` replaces hash IDs with incrementing network IDs. | Use collision-free runtime IDs; preserve serialized compatibility intentionally; add dedicated-server and two-player collision-coordinate regression tests. |
| [#4751](https://github.com/BuildCraft/BuildCraft/issues/4751) | Confirmed P1 Assembly Table UI synchronization bug. The checked-out source already includes a related recipe-state refresh fix. | Port the complete server-authoritative menu sync and test facade swaps while a different facade is held. |
| [#4749](https://github.com/BuildCraft/BuildCraft/issues/4749) | Confirmed unresolved P1 power-hook API defect: microjoules enter as `long`, return as `int`, and `receivePower` is not invoked. | Define explicit `long` accepted/remaining semantics and prove a third-party hook with a regression test before declaring interoperability. |
| [#4058](https://github.com/BuildCraft/BuildCraft/issues/4058) | Confirmed P2 Filler behavior defect. `TemplateBuilder.canPlace` admits air only, so excavate does not fill water. | Decide documented water/lava semantics before implementation; test both fluids and protection behavior. |
| [#4759](https://github.com/BuildCraft/BuildCraft/issues/4759) | P0 quarry-duplication investigation. The report has a video but lacks reliable version and minimal steps. | Do not guess at a code change. Build deterministic save/reload/multiplayer quarry accounting tests and request a minimal reproduction. |
| [#4600](https://github.com/BuildCraft/BuildCraft/issues/4600) | Historical weak wire-breaking crash report; likely overlaps #4752 but is not independently proven. | Keep as a historical break/save/load regression, not a standalone unverified fix. |
| [#4664](https://github.com/BuildCraft/BuildCraft/issues/4664) | Real Immersive Petroleum guide crash in an optional integration. | Keep it out of the base-mod gate; test it only if that optional integration is restored. |

The newer `8.0.1-pre.2` source is not the sibling `8.0.0` JAR. It already
contains fixes relevant to RF autoconversion, GUI tooltip duplication, an FE
adaptor path, literal-percent localization, and #4751/#4752. Those fixes become
behavioral regression tests; they are not evidence that the old JAR contains
them.
