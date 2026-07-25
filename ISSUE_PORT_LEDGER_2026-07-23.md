# BuildCraft Neo issue-port ledger

Updated: 2026-07-24

This is a screened regression ledger, not a claim that all historical reports
are fixed. The public BuildCraft tracker showed 390 open reports during the
2026-07-23/24 review. The issue numbers below are the reports with a plausible
core-port, compatibility, performance, resource, or GUI requirement for this
restoration. Feature requests, unsupported-version-only reports, duplicates,
and reports without a reproducible BuildCraft failure are not automatic code
changes.

Status vocabulary:

- **Source/test present**: modern code and/or a focused regression exists, but
  the latest full suite and runtime checklist are still pending.
- **Runtime pending**: the implementation is present in the restored source but
  needs GameTest/client/server/manual proof.
- **Optional**: isolated integration work; the base mod must run without it.
- **Excluded direct patch**: evidence does not establish a BuildCraft core bug.

No P0 or P1 row is closed until its automated and manual gate passes against an
exact JAR SHA-256.

## P0: data loss, crash, duplication or unbounded-state gates

| Reports | Required modern regression | Current disposition |
|---|---|---|
| #4734, #4749, #4748 | Bounded MJ/FE ingress, long-safe accounting, exact simulate/execute parity, finite per-side buffers and hostile third-party offers | Energy policy/source tests present; latest full rerun and live pipe/engine proof pending |
| #4752, #4413, #4567 | Collision-free wire network IDs through split/merge, rotation, save/load and reconnect | Persistence/identity tests present; multiplayer and real-world reload pending |
| #4724, #4252, #4644, #4144 | Auto Workbench placement, reload, recipe changes, damaged tools, remainders and shift-click cannot world-lock, lose or duplicate items | Source/unit coverage present; GameTest and GUI/manual workflow pending |
| #4684, #4636, #4667, #4616 | Pipe empty-output, divide-by-zero, reload, reconnect and invalid-destination safety | Pipe math/persistence tests and GameTests exist; latest run plus chunk/reconnect soak pending |
| #4627, #4241, #4450 | Stable registry IDs, nullable owner NBT and neighbor changes cannot remove/corrupt block entities | Static identity gate passes; real save/reload and copied-world migration pending |

## P1: gameplay and GUI correctness

| Area | Reports | Required gate | Current disposition |
|---|---|---|---|
| Lists | #4740 | Same Type/Same Material with ordinary, damaged, NBT and empty entries never crashes and matches correctly | Source regression coverage present; latest rerun and GUI manual check pending |
| Guide text | #4735 | Literal `%` is treated as content rather than format syntax | Parser regression present; latest rerun and client rendering pending |
| Guide layout | #4743 | Literal `\n` becomes a real line break and wrapped text stays on-page | Source repair/regression under final validation; client scale/locale checks pending |
| Assembly Table | #4751, #4295 | Open GUI refreshes recipe choices and facade state when inputs change, including equal-length recipe lists | Source repair under final validation; two-client runtime check pending |
| Energy display | #4738 | Selected RF/FE display never reports the value as MJ | TOP/integration side is optional; base energy GUIs still require unit/rate checks |
| Builder controls | #4761 | Excavate toggle visibility, packet, persistence and real task behavior agree | Control present in source; runtime task proof pending |
| Far-coordinate rendering | #4722, #4689 | Camera-relative builder/quarry/laser rendering at +/-1 to 10 million coordinates | Source contract test present; real client visual check pending |
| Network/tick pressure | #4556, #4475, #4393, #4465, #4620, #4512 | Interest-limited packets, bounded updates and no persistent tick/memory growth | Profiling and multiplayer soak pending |
| Builders/Filler/blueprints | #4695, #4622, #4527, #4375, #4376, #4058, #4076, #4116 | Transactional task accounting, rotation/mirror, placement order, permissions and reload | Source/tests present across builders; complete GameTest/manual matrix pending |
| Quarry power/state | #4670, #4355, #4169, #4383 | Intake caps, completion/reset, chunk-ticket release, lava/snow and reload correctness | Source/tests present; live Quarry cycle pending |
| Pump/Flood Gate fluids | #4630, #4608, #4572, #4550, #4577, #4440 | Exact liquid selection, redstone, back-pressure, chunk-unload and conservation semantics | Source/unit coverage present; live capability chain pending |
| Oil worldgen | #4487, #4002 | Data-driven enable/disable matrix and reachable small/medium/large default deposits | Policy/data tests present; generated-world inspection pending |
| Item transport | #4678, #4163, #4110, #3917, #4037 | One authoritative insert/extract transaction with no loss/duplication on full or changing routes | Flow/persistence tests and GameTests exist; live throughput/reload pending |

## P2: gates, resources and optional integrations

| Area | Reports | Requirement | Current disposition |
|---|---|---|---|
| Gates/controls | #4706, #4264, #4214, #4193, #4036, #4051 | Server-owned state, rotation/persistence, statements/actions and safe parameters | Source/unit coverage present; multiplayer/manual pending |
| Optional integrations | #4683, #4690, #4614, #4311, #4292, #4606, #4098 | Separate adapters and regressions; base startup must remain dependency-free | Optional/deferred until base behavior is stable |
| Resources/rendering | #4763, #4686, #4647, #4044, #4188, #3779, #3762 | Packaged JSON parsing, model/atlas resolution, resource reload and renderer inspection | Static asset audits pass; packaged current JAR and client visual check pending |

## Community-reported acceptance regressions

These comments are not automatically proven GitHub bugs, but the concrete
behaviors are legitimate release tests:

| Reported behavior | Release requirement |
|---|---|
| Combustion engine always shows 0 J/s | Produce a non-zero, correctly labeled live rate while generating |
| Combustion engine does not consume water | Coolant must be finite, visibly consumed and persistent |
| Engine stores energy indefinitely | Every engine/pipe buffer must have enforced capacity and overflow/back-pressure behavior |
| RF pipes do not connect to BuildCraft endpoints | Intended Forge Energy adapters must connect and conserve energy; unsupported endpoints must fail cleanly |
| IC2 Pump visually connects but Tank does not fill | Prove fluid capability transfer by a non-zero Tank amount, conservation and reload—not appearance alone |
| IC2 liquid fuels all receive the same EU value | Optional IC2 fuel mapping requires an explicit per-fluid value/burn-time matrix; base mod cannot depend on IC2 |
| Small oil deposits cannot be found | Small deposits must be reachable under default worldgen and independently configurable |
| Tank/pipe connection has a visible air gap | Compare connection geometry to original models and verify no misleading capability connection |
| Resource pack/OptiFine crash | Normal Forge resource-pack reload is a core gate; OptiFine-specific support is separate and must be declared/tested explicitly |

Requests for newer Minecraft versions, statements that the mod is abandoned,
and nostalgia/balance opinions are important project feedback but are not
reproducible code defects. A generic “crashes with any version” report without a
crash log, exact dependency set, and reproduction remains insufficient for a
direct patch.

## Upstream evidence, not wholesale merge sources

- `fe6f44426`: wire-ID collision pattern behind #4752.
- `7c86626d0`: Assembly Table synchronization pattern behind #4751.
- `6e774142f`: literal-percent and related GUI fixes.
- `663d2c0fa`: RF display behavior.
- #4760, #4762 and #4763 are reference PRs; unmerged code is not accepted
  wholesale without local compatibility and regression review.

The CurativeTree Community Edition repository is also a behavior/API reference,
not a substitute for this ledger. Its known unbounded power, unreachable small
oil default, permissive permission checks and packet concerns must not be
copied.

## Excluded as direct modern-port patches

- #4759: a 1.7.10 Quarry-frame report without a demonstrated 1.12.2/1.20.1
  equivalent.
- #4696: intentional Creative bucket behavior; test it as behavior, not a bug.
- #4721: OptiFine-specific evidence, not a base Forge compatibility promise.
- #4617: another mod's client-only callback; no proven BuildCraft core fault.

Every included issue stays open in this ledger until its named regression and
the relevant section of [MANUAL_TEST_CHECKLIST.md](MANUAL_TEST_CHECKLIST.md)
pass on the exact candidate artifact.
