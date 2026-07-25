# BuildCraft Neo

Start with [PORT_STATUS_LATEST.md](PORT_STATUS_LATEST.md). The canonical name is
**BuildCraft Neo**, and Forge 1.20.1 is the only active implementation lane.
Compilation succeeds, forced JUnit is 332/332, and the latest clean isolated
GameTest result is 69/70; the remaining failure is upward Chute transfer after
persistence/reload. No full-restoration release candidate or release JAR is
approved yet.

- [Legacy compatibility contract](legacy-1.12.2-contract/LEGACY_IDENTITY_MANIFEST.json)
- [Next-session roadmap](ROADMAP.md)
- [Upstream issue ledger](ISSUE_PORT_LEDGER_2026-07-23.md)
- [Community Edition reference audit](COMMUNITY_EDITION_REFERENCE_AUDIT_2026-07-23.md)
- [GUI parity manifest](GUI_PARITY_MANIFEST.md)
- [Manual gameplay checklist](MANUAL_TEST_CHECKLIST.md)
- [Forge 1.20.1 lane](ports/forge-1.20.1/)
- [Forge 1.20.1 asset audit](ports/forge-1.20.1/TEXTURE_PARITY_REPORT.md)
- [1.21 Tank migration record](TANK_1.21.1_MIGRATION_NOTES.md)
- [Future-target research](FUTURE_TARGET_RESEARCH_2026-07-23.md)

Use a disposable client/server instance and a copied world. The original
`../BuildCraft` source checkout and `../buildcraft-all-8.0.0.jar` are
preservation references and must not be altered.
