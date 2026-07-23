# BuildCraft Neo Better manual test checklist

Use a disposable instance and a copied world. Do not use a live client or the
only copy of an existing world.

## Current Forge 1.20.1 Tank slice

- [ ] Start a fresh Forge 1.20.1 test profile containing only the produced
  BuildCraft Neo Better development JAR and its required loader.
- [ ] Confirm all eight legacy module IDs load without a missing dependency or
  duplicate-mod error.
- [ ] Obtain `buildcraftfactory:tank`; place one Tank and confirm its model,
  item model and English name resolve without a missing-texture/model log.
- [ ] Right-click an empty Tank with a water bucket. The bucket transfer must
  happen before any menu opens.
- [ ] Right-click without a fluid container. Confirm the 176×181 Tank menu,
  title, player inventory, gauge, tooltip, shift-click transfer and gauge-click
  transfer all work.
- [ ] Fill the Tank to 16,000 mB. Confirm no more fluid is accepted and its
  comparator output rises from 0 to 15 as expected.
- [ ] Stack two or more Tanks vertically. Liquid must settle/fill from bottom;
  gas must settle/fill from top; mixed fluids must be rejected.
- [ ] Break a Tank. Confirm it drops the Tank item and does not duplicate fluid
  or items.
- [ ] Leave and re-enter the world; reopen the Tank and confirm contents remain.
- [ ] Open a Tank from two clients. Confirm both see fluid changes and that an
  invalid/out-of-range menu closes or rejects actions.
- [ ] Run F3+T and inspect the log for missing model, texture, JSON, or packet
  errors.

## Not yet available for manual approval

Do not attempt to judge these as working in the current development JAR:

- Engines, MJ transport, RF/FE pipes, pipe plugs, wire networks and conversion.
- Builders, Quarry, Filler, blueprints, world generation, oil/fuels, robots,
  silicon machines and advanced menus.
- Compat integrations (JEI, IC2, or other third-party mod links).
- Any old-world migration claim.

## Before testing migration

- [ ] Make a separate, verified backup of the 1.12.2 world.
- [ ] Record the original JAR SHA-256 and exact dependency set.
- [ ] Test only a copy in a dedicated migration instance.
- [ ] Preserve the original save if the port fails to load or changes data.
