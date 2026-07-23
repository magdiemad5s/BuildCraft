# Post-1.21.1 target research

Read-only public metadata was checked on 2026-07-23. This is planning input,
not authorization to start a fifth port lane ahead of the four required ones.

| Source | Current result |
|---|---|
| Mojang version manifest | Latest release: `26.2`; latest snapshot: `26.3-snapshot-5` |
| Forge Maven | `26.2-65.0.9` is published; Maven's release field still reports `1.21.8-58.1.20` |
| NeoForge Maven | `26.2.0.32-beta` is published |

Recommended order:

1. Finish and validate the requested Forge/NeoForge `1.20.1` and `1.21.1`
   lanes first.
2. Treat Forge `26.2-65.0.9` and NeoForge `26.2.0.32-beta` as research
   candidates only. Both require a new API/build audit, Java/toolchain check,
   current MDK/ModDevGradle template comparison, and a fresh compatibility
   review.
3. Do not target the `26.3` snapshot for a stable revival build.

The future lane must preserve the same registry, persistence, packet, and
resource contract; it cannot simply reuse a 1.21.1 JAR.
