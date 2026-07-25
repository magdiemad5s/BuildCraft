# Current status

The authoritative handoff is [PORT_STATUS_LATEST.md](PORT_STATUS_LATEST.md).
The canonical project name is **BuildCraft Neo**. The active lane is Minecraft
1.20.1 on Forge 47.4.22 and Java 17.

Current checkpoint:

- compilation succeeds;
- a forced current-source JUnit run passed **332/332** tests;
- a clean isolated GameTest run passed **69/70** tests;
- the only failing GameTest is the upward Chute persistence/transfer scenario:
  **0 of 3** persisted items reached the expected destination;
- removal-lifecycle GameTests were added after that work, but their discovery
  and results must be explicitly reconfirmed in the next full GameTest run;
- strict source audits pass for 1,154 PNGs, 28/28 menu/screen pairs, all 40
  released locales and 12,205 released translation keys, 208 recipes, 51/51
  migration aliases, and all five required `SavedData` names.

There is **no full-restoration release candidate or release JAR yet**. The
remaining work is ordered in [ROADMAP.md](ROADMAP.md): fix the Chute, obtain a
fully observed 70/70 GameTest run including removal lifecycle coverage, then
complete clean build/data/JAR, isolated server/client, resource reload,
multiplayer, gameplay, and copied-world migration gates.

The old `0.1.x-dev` Tank-only artifacts are historical test bundles. They do
not represent the current source and must not be used to decide whether the
full restoration is complete.
