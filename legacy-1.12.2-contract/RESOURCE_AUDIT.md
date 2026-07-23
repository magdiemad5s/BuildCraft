# Resource audit — source and reference artifact

The reference JAR contains 1,014 PNG textures, 418 JSON files, 40 language
files, 24 animation metadata files, 112 Guide Markdown files, 49 advancements,
45 recipes, 44 blockstates, six GUI-definition JSON files, and an OBJ/MTL pair.

The active source roots are `common`, `common_old_license`, `BuildCraftAPI/api`,
`buildcraft_resources`, `BuildCraft-Localization`, and
`BuildCraftGuide/guide_resources`. `src_old_license` is excluded and must not be
mistakenly ported.

Legacy recipes and advancements are located below `assets/`; modern lanes must
move equivalent data into the appropriate `data/<namespace>/` paths while
preserving identifiers and behavior. A byte-for-byte comparison is not valid,
but the package validator must confirm every intended resource is represented.

The reference JAR includes a separate Compat resource set absent from the base
source. It is an optional-add-on contract, not a reason to copy resources into
the base artifact.
