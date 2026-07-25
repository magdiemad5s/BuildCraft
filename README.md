# BuildCraft Neo

<p align="center">
  <img src="assets/branding/buildcraft-neo-logo-v1.png" alt="BuildCraft Neo" width="1200">
</p>

> BuildCraft Neo is an authorized restoration of BuildCraft for modern
> Minecraft. The active target is Forge 1.20.1. This repository preserves the
> original 1.12.2 source and released JAR as compatibility references; the
> latest restored source is still undergoing release validation.

## Active target

| Component | Version |
|---|---|
| Minecraft | `1.20.1` |
| Loader | Forge `47.4.22` |
| Java | 17 |
| Mappings | Official Mojang `1.20.1` |
| Source lane | [`ports/forge-1.20.1`](ports/forge-1.20.1/) |
| Source version | `8.0.10+1.20.1+neo1-dev` |

Forge 1.20.1 is the only active completion target. NeoForge 1.20.1 and both
Forge/NeoForge 1.21.1 lanes follow after the 1.20.1 behavior is stable; Fabric
and later Minecraft versions are separate follow-up work.

## Current state

The modern lane now contains the full eight-module source surface: Core, Lib,
Transport, Energy, Factory, Builders, Silicon, and Robotics. Registries,
creative providers, pipes, engines, machines, builders, worldgen, networking,
rendering, menus/screens, guides, migrations, and optional-integration hooks are
present in source.

That is a source-restoration statement, not a bug-free or release-ready claim.
The last recorded full unit checkpoint passed **314/314 JUnit tests across 98
suites**. The working tree changed afterward. It now declares **69 GameTests**,
but the latest source still needs a clean test/build/data/GameTest pass,
packaged-JAR validation, dedicated-server and client startup, F3+T resource
reload, multiplayer checks, and the complete manual gameplay checklist.

The historical `0.1.x-dev` files under `releases/` were Tank-focused test
bundles. They are preserved for traceability but are superseded as a
representation of the current full source. No full-restoration release
candidate is approved until a new JAR name, size, SHA-256, commit, and test
record are published.

## Preserved identities and assets

- The eight legacy module/registry namespaces remain unchanged.
- The static identity gate accounts for the 182-entry legacy registry contract,
  creative providers, migration aliases, and five legacy `SavedData` names.
- All 116 currently registered modern items are wired into creative providers;
  in-game creative-tab confirmation is still required.
- The resource tree contains 1,154 PNGs, 60 GUI sheets, 86 animations, 465
  models, 72 blockstates, and 936 atlas sources.
- All 1,014 PNGs and 24 animation metadata files from the released 8.0.0 JAR
  are present byte-for-byte at normalized modern paths.
- The GUI static audit reports 28 menu declarations and 28 matching screen
  registrations.
- All 40 released locales and 12,205 released translation keys are retained.
- The original source/JAR contains no authoritative custom sound assets.

## Status and testing

- [Current Forge 1.20.1 handoff](PORT_STATUS_LATEST.md)
- [Forge lane instructions](ports/forge-1.20.1/README.md)
- [Legacy compatibility contract](legacy-1.12.2-contract/LEGACY_IDENTITY_MANIFEST.json)
- [Asset parity report](ports/forge-1.20.1/TEXTURE_PARITY_REPORT.md)
- [GUI parity manifest](GUI_PARITY_MANIFEST.md)
- [Issue port ledger](ISSUE_PORT_LEDGER_2026-07-23.md)
- [Manual testing checklist](MANUAL_TEST_CHECKLIST.md)
- [Community Edition reference audit](COMMUNITY_EDITION_REFERENCE_AUDIT_2026-07-23.md)

Use only a disposable test instance and a copied world. Never test migration on
the only world copy, overwrite an actively loaded mod JAR, or interrupt a
Minecraft client the user is testing.
## Legacy BuildCraft 8.0.x documentation

The original project README follows unchanged.

## Welcome to BuildCraft on GitHub

### Reporting an issue

Please open an issue for a bug report only if:

* you are sure the bug is caused by BuildCraft and not by any other mod,
* you have at least one of the following:
  * a crash report, 
  * means of reproducing the bug in question,
  * screenshots/videos/etc. to demonstrate the bug.

**If you are not sure if a bug report is valid, please use the "Ask Help!" subforum.**

Please only use **official BuildCraft releases** for any kind of bug reports unless otherwise told to do by the BuildCraft team. Custom builds (for instance from Jenkins) are unsupported, often buggy and will **not** get any support from the developers.

Please check if the bug has been reported beforehand. Also, provide the version of BuildCraft used - if it's a version compiled from source, link to the commit/tree you complied from.

Please mention if you are using MCPC+, Cauldron, OptiFine, FastCraft or any other mods which optimize or otherwise severely modify the functioning of the Minecraft engine. That is very helpful when trying to reproduce a bug.

Please do not open issues for features unless you are a member of the BuildCraft team. For that, use the "Feature Requests" subforum.

BuildCraft, being an open-source project, gives you the right to submit a pull request if a particular fix or feature is important to you. However, if the change in question is major, please contact the team beforehand - we wish to prevent wasted effort.

### Contributing

If you wish to submit a pull request to fix bugs or broken behaviour feel free to do so. If you would like to add 
features or change existing behaviour or balance, please discuss it on discord before submitting a PR (https://discord.gg/v4geqgA).

Do not submit pull requests which solely "fix" formatting. As these kinds of changes are usually very intrusive in commit history and everyone has their own idea what "proper formatting" is, they should be done by one of the main contributors. 
Please only submit "code cleanup", if the changes actually have a substantial impact on readability.

PR implementing new features or changing large portions of code are helpful. But if you're doing such a change and if it gets accepted, please don't "fire and forget". Complex changes are introducing bugs, and as thorough as testing and peer review may be, there will be bugs. Please carry on playing your changes after initial commit and fix residual issues. It is extremely frustrating for others to spend days fixing regressions introduced by unmaintained submissions.

#### Frequently reported

* java.lang.AbstractMethodError, java.lang.NoSuchMethodException
  * A mod has not updated to the current BuildCraft API
  * You are not using the correct version of BuildCraft for your Forge/Minecraft versions
  * You are using the dev version on a normal game instance (or vice versa)
* Render issue (Quarry causes flickering) - Try without OptiFine first! This is a known issue with some versions of OptiFine.

### Compiling and packaging BuildCraft
1. Ensure that `Java` (found [here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)), `Git` (found [here](http://git-scm.com/)) are installed correctly on your system.
 * Optional: Install `Gradle` (found [here](http://www.gradle.org/downloads)). You probably want to install version 4.3.1.
2. Create a base directory for the build
3. Clone the BuildCraft repository into 'baseDir/BuildCraft/'
4. Clone (and update) the submodules into 'baseDir/BuildCraft with 'git submodule init' and 'git submodule update'
5. Navigate to basedir/BuildCraft in a shell and run one of two commands:
    * `./gradlew setupCIWorkspace build` to just build a current jar (this may take a while).
    * `./gradlew setupDecompWorkspace` to setup a complete development environment.
    * With `Gradle` installed: use `gradle` instead of `./gradlew`
    * On Windows: use `gradlew.bat` instead of `./gradlew`
6. The compiles and obfuscated module jars will be in 'baseDir/BuildCraft/build/libs/&lt;build number&gt;/modules'

Your directory structure should look like this before running gradle:
***

    baseDir
    \- BuildCraft
     |- buildcraft_resources
     |- common
     |- ...
     \- BuildCraftAPI
      |- api
      |- ...
     \- BuildCraft-Localization
      |- lang
      |- ...

***

And like this after running gradle:
***

    basedir
    \- BuildCraft
     |- .gradle
     |- build
     |- buildcraft_resources
     |- common
     |- ...
     \- BuildCraftAPI
      |- api
      |- ...
     \- BuildCraft-Localization
      |- lang
      |- ...

***

### Localizations

Localizations can be submitted [here](https://github.com/BuildCraft/BuildCraft-Localization). Localization PRs against
this repository will have to be rejected.

### Depending on BuildCraft

Instructions for depending on BC 7.1.x can be found [here](https://github.com/BuildCraft/BuildCraft/blob/7.1.x/README.md) (for 1.7.10).

8.0.x hasn't been finished yet, so there are no instructions for depending on it :(

The following instructions are for BC 7.99.12 (1.12.2):

Add the following to your build.gradle file:
```
repositories {
    maven {
        name "BuildCraft"
        url = "https://mod-buildcraft.com/maven"
    }
}
````

If you want to depend on JUST the API then do this:
````
dependencies {
    deobfCompile "com.mod-buildcraft:buildcraft-api:7.99.12"
}
````

If you want to depend on JUST the lib then do this:
````
dependencies {
    deobfCompile "com.mod-buildcraft:buildcraft-lib:7.99.12"
}
````

If you want to depend on the whole of buildcraft do this:
```
dependencies {
    deobfCompile "com.mod-buildcraft:buildcraft:7.99.12"
}
```
Where `7.99.12` is the desired version of BuildCraft.
