# BuildCraft Neo - Forge 1.20.1 asset parity

Audit date: 2026-07-24

This audit compares three independent references:

- Original 1.12.2 resource source at BuildCraft commit
  `7c86626d09c4569fb2e0f458a16aaf8c3148ebaf`.
- Released `buildcraft-all-8.0.0.jar`, SHA-256
  `f617279f8148a9140ab86c0e4aa4c54fe8a94515d2cf487a47cbd928160b88aa`.
- CurativeTree's Forge 1.20.1 resource port at commit
  `8f9924b490c009474d3b2c786bb9ed8504c77358`.

The machine-readable original-art inventory, exact hashes, required GUI list,
and deliberate rename table are in
[`TEXTURE_PARITY_MANIFEST.json`](TEXTURE_PARITY_MANIFEST.json). Current
validator output is in
[`TEXTURE_PARITY_REPORT.json`](TEXTURE_PARITY_REPORT.json).

## Result

The current Forge 1.20.1 resource tree passes the strict source-tree audit with
no errors or warnings:

- 1,154 valid PNG files in the complete resource input, including the mod logo.
- 1,153 PNG files below `assets/`.
- 60 GUI PNGs.
- 86 animation metadata files, each with a valid PNG and valid frame geometry.
- 465 model JSON files and 72 blockstate JSON files.
- 936 explicit block-atlas sources.
- 1,367 resolved internal texture references and 376 resolved internal model
  references.
- 18 texture and 277 model references supplied externally by Minecraft or
  Forge.
- Zero unresolved internal texture, model, blockstate, or atlas references.

The case normalization fixes a Windows-only false success: NTFS resolves
`fillerBlock` and `fillerblock` as the same path, while JAR resource lookup is
case-sensitive. No packaged PNG path now contains uppercase characters.

No artwork was redrawn or generated in this lane.

## Released artwork preservation

The released 8.0.0 JAR contains 1,014 PNGs and 24 animation metadata
files. Every one is present at its case-normalized target path with
byte-identical content. The manifest verifies those 1,038 released files plus
35 deliberate modern aliases, for 1,073 SHA-256 checks in total.

The direct comparison also confirms that the original source contributes no
additional unreleased PNG or animation file that is absent from the port.

## Defect restored

Modern GUI code requests:

`buildcraftcore:textures/gui/buttons.png`

The original raster existed only at:

`buildcraftlib:textures/gui/buttons.png`

An exact-byte compatibility copy was added at the requested namespace. Both
files have SHA-256:

`5c92f6e9773b97a778680ecbdb23e1874285bd38ea108321ce3aec6849832d33`

This prevents a missing button sheet while preserving the original art and
without changing Java.

## Source versus released JAR

The original source contains 985 module PNGs. The released JAR contains those
same 985 paths plus 29 `buildcraftcompat` PNGs for obsolete Forestry/MFR-era
optional integrations.

Of the 985 shared paths:

- 984 are byte-identical.
- `assets/buildcraftfactory/textures/gui/distiller.png` differs.

The port uses the released JAR's Distiller GUI sheet, which is also the exact
version selected by the modern 1.20.1 reference. This follows the rule that the
working JAR is the released visual and behavioral reference.

## Intentional modern mappings

Forty-five original rasters have unambiguous, byte-identical modern paths.
Twenty are semantic modern renames and twenty-five are required lowercase path
normalizations. The mapping families are:

- Gear items moved into `textures/items/gear/`.
- Camel-case resources became snake_case.
- The old `silver` colour name became `light_gray`.
- Base oil sprites dropped the `_heat_0` suffix.
- Current MJ pipe sprites dropped transitional `_old` and `_clear` suffixes.
- Uppercase legacy directories and filenames became lowercase so atlas lookup
  works identically on Windows and from a case-sensitive JAR.

The JSON report records every source path, target path, expected SHA-256, and
reason. The verifier checks the target hashes so a renamed raster cannot be
silently replaced.

## GUI coverage

All 59 original/released GUI sheets required by the manifest are present and
byte-verified. The `buildcraftcore` button compatibility copy raises the
packaged GUI count to 60.

The GUI parity validator additionally confirms:

- 28 menu declarations and 28 matching client screen registrations.
- 42 literal BuildCraft GUI resource references resolve.
- Six GUI JSON files parse.
- Zero GUI parity errors.

All three formerly deferred energy GUI sheets are restored:

- `rf_engine_gui.png`
- `mj_dynamo_gui.png`
- `combustion_engine_gui_old.png`

All 29 released `buildcraftcompat` PNGs are also present. Their presence
preserves released artwork; it does not claim the obsolete integrations are
currently enabled.

Java-side texture references are also covered: all literal BuildCraft GUI and
entity PNG locations resolve, including both robot overlay textures and all 18
robot-board texture variants constructed dynamically by `BCRoboticsBoards`.

## Models and blockstates

The 1.12.2 JAR contains 232 models and 44 blockstates. These JSON files cannot
all remain byte-identical because Minecraft 1.20.1 requires lowercase resource
locations, new state properties, and updated model references. Active files
were ported rather than blindly copied.

The current tree contains 465 models and 72 blockstates. The strict resolver
parses all of them and confirms every internal BuildCraft texture and model
reference resolves with case-sensitive packaged paths. Legacy-only camel-case
or superseded resource names are not shipped as broken duplicate models.

Direct released-JAR path comparison classifies the 232 legacy models as 13
byte-exact, 201 present but intentionally ported, and 18 superseded raw paths.
It classifies the 44 legacy blockstates as 34 intentionally ported and 10
superseded raw paths. The absent raw paths are listed here so they are not
mistaken for an unaudited omission.

Superseded model paths:

- `buildcraftbuilders:models/block/buildToolBlock.json`
- `buildcraftbuilders:models/item/blueprint/{clean,used}.json`
- `buildcraftbuilders:models/item/template/{clean,used}.json`
- `buildcraftcore:models/block/centeredTorch.json`
- `buildcraftcore:models/block/engine/{creative,iron,stone,wood}.json`
- `buildcraftfactory:models/item/water_gel.json`
- `buildcraftsilicon:models/block/advWorkbenchTable.json`
- `buildcraftsilicon:models/block/assemblyTable.json`
- `buildcraftsilicon:models/block/chargingTable.json`
- `buildcraftsilicon:models/block/integrationTable.json`
- `buildcraftsilicon:models/block/laserTable.json`
- `buildcraftsilicon:models/item/redstonecrystal.json`
- `buildcrafttransport:models/item/wire/silver.json`

Their active equivalents use lowercase 1.20.1 paths such as
`centered_torch`, module-specific engine models, snapshot/template selectors,
`water_gel_spawn`, `table/advanced_crafting`, `table/assembly`,
`table/charging`, `table/integration`, `laser`, `redstone_crystal`, and
`wire/light_gray`. `buildToolBlock` was a legacy internal helper rather than a
registered player-facing block model.

Superseded blockstate paths:

- `buildcraftbuilders:blockstates/buildToolBlock.json`
- `buildcraftbuilders:blockstates/frameSurvivalBlock.json`
- `buildcraftcore:blockstates/markerBlock.json`
- `buildcraftcore:blockstates/pathMarkerBlock.json`
- `buildcraftenergy:blockstates/blockFuel.json`
- `buildcraftenergy:blockstates/blockRedPlasma.json`
- `buildcraftfactory:blockstates/heat_exchange_end.json`
- `buildcraftfactory:blockstates/heat_exchange_start.json`
- `buildcraftfactory:blockstates/refineryBlock.json`
- `buildcraftrobotics:blockstates/zonePlan.json`

Registered replacements include `marker_volume`, `marker_path`, modern fluid
blocks, the unified `heat_exchange`, `zone_planner`, and current renderer/model
paths. Legacy registry aliases preserve old world identities where applicable.
No GUI texture, sprite PNG, active model, or active blockstate reference is
missing from the current port.

## Languages and sounds

The released JAR contains 40 legacy locale files with 12,205 translation keys.
They have been converted to valid lowercase 1.20.1 JSON locale files. The
language validator parses 71 current locale JSON files and confirms all 12,205
released keys are available; no legacy `.lang` file remains in the output.

Neither the released 8.0.0 JAR nor the original resource source contains custom
BuildCraft sound files or a `sounds.json`. The current tree contains no
invented replacement sound assets, so the authoritative sound inventory is
complete at zero files.

## Branding

The canonical product name is `BuildCraft Neo`. The Forge 1.20.1 Gradle
artifact name, manifest titles, resource-pack description, and all eight module
display names use that branding. No current metadata or documentation in this
lane uses the accidental `BuildCraft Neo Better` name.

All eight module cards reference `buildcraft_neo_logo.png`. The source logo is
2,139 by 735 pixels, 1,023,228 bytes, with SHA-256
`c45eb983ae0472cf6040269402639a1b0d0c128f399443755c38873c7287ab49`.
The packaged-JAR validator now enforces the logo and canonical display names.

Visual inspection confirms the raster reads `BUILDCRAFT NEO`, uses the purple
Neo treatment, and contains neither `Community Edition` nor `Better` branding.

## Verification

Run against the source resource tree:

```powershell
.\tools\verify-texture-parity.ps1 -InputPath .\src\main\resources
node .\tools\verify-gui-parity.mjs
node .\tools\verify-language-parity.mjs
```

Run against the final packaged JAR:

```powershell
.\tools\verify-texture-parity.ps1 -InputPath .\build\libs\<release-name>.jar
```

Optionally write a JSON run result:

```powershell
.\tools\verify-texture-parity.ps1 `
  -InputPath .\build\libs\<release-name>.jar `
  -JsonReportPath .\build\reports\texture-parity.json
```

The final packaged-JAR audit also verifies that all source resources, the
BuildCraft Neo logo, and all eight canonical module display names are present:

```powershell
.\tools\verify-full-release-jar.ps1 -JarPath .\build\libs\<release-name>.jar
```

The verifier:

- Structurally validates every packaged PNG and obtains its dimensions.
- Uses the platform PNG decoder when available.
- Parses every asset JSON and animation metadata file.
- Checks animation PNG pairs, frame geometry, and explicit frame indexes.
- Resolves BuildCraft texture references from models.
- Resolves BuildCraft model references from models and blockstates.
- Resolves all explicit atlas texture sources.
- Requires every GUI sheet in the machine-readable inventory.
- Enforces per-module PNG and GUI minimums.
- Verifies the SHA-256 of deliberate renamed and compatibility-copy rasters.

Vanilla `minecraft:*` model and texture references are counted as external
dependencies because they are supplied by Minecraft rather than this JAR.

## Licensing

Original raster bytes and attribution are retained. The imported modern
reference is MPL-2.0; CurativeTree/ShipovskijKorp attribution and notices must
remain in the distributed project. This audit does not relicense BuildCraft
artwork.
