# BuildCraft Neo roadmap

Updated: 2026-07-25

This is the ordered handoff for the active Forge 1.20.1 lane. Do not start the
NeoForge or 1.21.1 ports until this lane reaches its release gates.

## Checkpoint at pause

- Canonical project name: **BuildCraft Neo**.
- Target: Minecraft 1.20.1, Forge 47.4.22, official Mojang mappings, Java 17.
- Compilation succeeds.
- Forced current-source JUnit: **332/332 passed**.
- Clean isolated GameTests: **69/70 passed**.
- Sole GameTest failure: upward Chute persistence/transfer moved **0 of 3**
  expected persisted items after reload.
- Removal-lifecycle GameTests were added, but their discovery and results must
  be explicitly reconfirmed in the next complete run.
- Strict source audits pass: 1,154/1,154 valid PNGs, 28/28 menu/screen pairs,
  40 released locales and 12,205 released keys, 208 recipes, 51/51 migration
  aliases, and all five required `SavedData` names.
- There is no full-restoration release candidate or release JAR yet.

## 1. Fix the upward Chute blocker

Reproduce the failing persistence/transfer scenario in isolation. Verify Chute
orientation, item capability exposure, cooldown/tick scheduling, neighbor cache
invalidation, NBT save/load, and destination insertion. Compare behavior with
the preserved 1.12.2 source/JAR contract. Keep the registry ID and serialized
field meanings stable.

Add or refine focused regression coverage so the test proves all three items
survive reload and arrive exactly once. Do not weaken the assertion or extend
timeouts merely to hide a deterministic transfer failure.

## 2. Prove the complete GameTest suite

Run an isolated `runGameTestServer` after the Chute fix. Inspect the server log,
not only Gradle's exit status. The required result is a fully observed **70/70**
for the currently expected suite. Explicitly search the log for the newly added
removal-lifecycle scenarios and record their names/results; if the source count
changes, document the new discovered total instead of forcing the old number.

## 3. Re-run clean automated and packaging gates

From `ports/forge-1.20.1` with the project-local Gradle cache:

```powershell
$env:GRADLE_USER_HOME = (Resolve-Path '.\.gradle-user-home').Path
.\gradlew.bat clean test --no-daemon --console=plain
.\gradlew.bat runData --no-daemon --console=plain
.\gradlew.bat runGameTestServer --no-daemon --console=plain
.\gradlew.bat build --no-daemon --console=plain
```

Then validate both source resources and the exact built JAR:

```powershell
$sourceResources = (Resolve-Path '.\src\main\resources').Path
$jarPath = (Resolve-Path '.\build\libs\buildcraft-neo-forge-1.20.1-8.0.10+1.20.1+neo1-dev.jar').Path

node tools/verify-gui-parity.mjs
node tools/verify-language-parity.mjs
node tools/verify-recipe-data-parity.mjs
powershell -ExecutionPolicy Bypass -File tools/verify-texture-parity.ps1 -InputPath $sourceResources -WarningsAsErrors
powershell -ExecutionPolicy Bypass -File tools/verify-legacy-identity-parity.ps1 -FailOnMismatch
powershell -ExecutionPolicy Bypass -File tools/verify-packaged-resources.ps1 -JarPath $jarPath
powershell -ExecutionPolicy Bypass -File tools/verify-full-release-jar.ps1 -JarPath $jarPath
```

Parse packaged JSON and retain the validator reports. A compiled or packaged
JAR is not automatically a release candidate.

## 4. Run isolated server and client gates

- Start a dedicated Forge server without optional integrations and confirm all
  eight legacy module IDs load without client-only class errors.
- Start an isolated client and inspect the full model-resolution log for missing
  models, textures, atlas sprites, blockstates, and invalid mappings.
- Open every registered menu/screen and check layout, slots, buttons, tooltips,
  progress bars, text clipping, and synchronization.
- Perform F3+T and confirm a clean resource reload with no texture/model changes
  or crashes.
- Repeat the base startup with required compatibility dependencies only, then
  add optional integrations one at a time.

## 5. Complete gameplay, multiplayer, and migration approval

Use [MANUAL_TEST_CHECKLIST.md](MANUAL_TEST_CHECKLIST.md). Cover creative tabs,
all pipe families, fluids, tanks, engines, Factory machines, Quarry/Builder/
Filler/Replacer workflows, Silicon tables/lasers/gates, robots, oil worldgen,
IC2 fluid interoperability, save/reload, chunk borders, and two-client packet
synchronization.

Test 1.12.2 migration only with a backup copy of a world. Never use the sole
world copy, and never replace a mod JAR while Minecraft is running.

## 6. Produce and install the release candidate

Only after every earlier gate passes:

1. Build the exact candidate JAR in `build/libs`.
2. Record its filename, byte size, SHA-256, source commit, Java/Forge versions,
   automated results, server/client logs, and manual signoff.
3. Update the status and release notes without deleting the historical
   `0.1.x-dev` Tank-only records.
4. Confirm no Minecraft/Java process is using the user's IC2 test instance.
5. Remove only the exact obsolete BuildCraft Neo test JAR, copy the verified
   candidate, and record what was installed.
6. Commit and push the tested checkpoint.

## 7. Later loader/version lanes

After Forge 1.20.1 is stable, port the verified behavior in this order:
NeoForge 1.20.1, Forge 1.21.1, then NeoForge 1.21.1. Fabric and newer Minecraft
versions remain separate abstraction/research work and must receive their own
complete automated and manual validation.
