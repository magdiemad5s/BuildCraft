# BuildCraft Neo Better

<p align="center">
  <img src="assets/branding/buildcraft-neo-logo-v1.png" alt="BuildCraft Neo" width="1200">
</p>

> This repository now carries an **additive, early development port** alongside
> the preserved legacy BuildCraft 8.0.x / Minecraft 1.12.2 source. It is not a
> complete or stable public BuildCraft release.

## Current Forge 1.20.1 test build: 0.1.3-dev

The currently validated test artifact is committed under
[`releases/0.1.3-dev`](releases/0.1.3-dev/):

| Target | File |
|---|---|
| Forge 1.20.1 | `buildcraft-neo-better-forge-1.20.1-0.1.3-dev+1.20.1.jar` |

It fixes the Factory Tank's modern block-atlas path, fluid-capability revival,
server-to-client Tank synchronization, direct container-transfer priority, and
initial in-world fluid rendering.

The older `0.1.1-dev` and `0.1.2-dev` Forge artifacts are **superseded for
Tank testing**. Equivalent source fixes are staged for NeoForge 1.20.1 and both
1.21.1 lanes, but those lanes require fresh builds and validation before
release.

Verify the Forge file with
[`SHA256SUMS.txt`](releases/0.1.3-dev/SHA256SUMS.txt) and read the
[`release notes`](releases/0.1.3-dev/RELEASE_NOTES.md) first. The only
implemented gameplay slice is the Factory Tank (`buildcraftfactory:tank`);
pipes, engines, machines, world generation, most GUIs, multiplayer, and
old-world migration remain unfinished.

## Port sources and status

- [Current port handoff](PORT_STATUS_LATEST.md)
- [Legacy compatibility contract](legacy-1.12.2-contract/LEGACY_IDENTITY_MANIFEST.json)
- [GUI parity manifest](GUI_PARITY_MANIFEST.md)
- [Issue port ledger](ISSUE_PORT_LEDGER_2026-07-23.md)
- [Manual testing checklist](MANUAL_TEST_CHECKLIST.md)
- [Port lanes](ports/)

Each port lane has its own Gradle wrapper and uses a lane-local
`.gradle-user-home` cache. Do not run Minecraft client/server/GameTest tasks
against a normal instance or an irreplaceable world.

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
