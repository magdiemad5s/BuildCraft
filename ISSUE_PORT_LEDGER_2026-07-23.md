# BuildCraft Neo issue-port ledger

This is a triage ledger, not a claim that every historical report is fixed. The
public BuildCraft tracker had 390 open reports when it was screened on 2026-07-23.
Feature requests, reports tied to unsupported Minecraft versions, and reports
without a reproducible core-mod failure are deliberately not treated as direct
code-change requirements.

## P0: must have a regression before release

| Reports | Modern-port requirement | Current disposition |
|---|---|---|
| #4734, #4749, #4748 | Bounded MJ/FE pipe ingress, exact simulate/execute parity, and long-safe third-party power hooks. | Loader-neutral regression layer in progress; no pipe gameplay claim yet. |
| #4752, #4413, #4567 | Collision-free wire network IDs, save/load and reconnect safety. | Preserve the upstream `fe6f44426` invariant; add modern tests when wires port. |
| #4724, #4252, #4644, #4144 | Auto Workbench placement, reload, recipes, damaged tools and shift-click must not world-lock. | Scheduled before Factory workbench release. |
| #4684, #4636, #4667, #4616 | Pipe empty-output, divide-by-zero, reload and reconnect safety. | Scheduled with real Transport implementation. |
| #4627, #4241, #4450 | Stable registries, nullable owner NBT, and neighbour changes must not remove block entities. | Identity manifest freezes IDs; persistence tests remain required. |

## P1: gameplay, GUI, or performance release gates

| Area | Reports | Required gate |
|---|---|---|
| Assembly Table state/UI | #4751, #4295 | Server-authoritative facade/recipe synchronization while the menu is open. |
| GUI/text | #4735, #4743, #4738, #4761 | Literal-percent safety, newline rendering, RF display and Builder control parity. |
| Far-coordinate rendering | #4722, #4689 | Camera-relative rendering at ±1–10 million coordinates. |
| Network/tick pressure | #4556, #4475, #4393, #4465, #4620, #4512 | Packet budgets, interest-limited sync, soak and profile tests. |
| Builders/Filler/blueprints | #4695, #4622, #4527, #4375, #4376, #4058, #4076, #4116 | Transactional task accounting and placement GameTests. |
| Quarry power/state | #4670, #4355, #4169, #4383 | Intake caps, completion resets, lava/snow/reload tests. |
| Pump/Floodgate fluids | #4630, #4608, #4572, #4550, #4577, #4440 | Exact liquid selection, redstone and chunk-unload semantics. |
| Oil worldgen | #4487, #4002 | Data-driven settings matrix with each enable/disable state tested. |
| Item transport | #4678, #4163, #4110, #3917, #4037 | One authoritative insert/extract transaction; no loss/duplication. |

## P2: later core and optional-integration work

| Area | Reports | Requirement |
|---|---|---|
| Gates/controls | #4706, #4264, #4214, #4193, #4036, #4051 | Server state synchronization without client authority. |
| Optional integrations | #4683, #4690, #4614, #4311, #4292, #4606, #4098 | Separate companion work; base startup must stay dependency-free. |
| Resources/rendering | #4763, #4686, #4647, #4044, #4188, #3779, #3762 | Packaged JSON parsing and client model-resolution inspection. |

## Upstream fixes and PRs used only as evidence

- `fe6f44426`: fixes the wire-ID collision pattern behind #4752.
- `7c86626d0`: fixes the Assembly Table synchronization pattern behind #4751.
- `6e774142f`: safe literal-percent formatting and GUI fixes.
- `663d2c0fa`: RF display behavior.
- #4760, #4762, and #4763 remain unmerged references, not wholesale merge sources.

## Excluded as direct modern-port patches

- #4759 is a 1.7.10 quarry-frame report, not a demonstrated 1.12/1.20.1 defect.
- #4696 is intentional creative-bucket behavior.
- #4721 is OptiFine-specific and is not a core Forge/NeoForge compatibility promise.
- #4617 is caused by another mod's client-only callback, not proven BuildCraft core behavior.

Every issue above stays open in this ledger until its named automated and manual
release gate passes in each supported loader/version lane.
