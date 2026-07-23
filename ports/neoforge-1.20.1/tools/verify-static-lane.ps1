[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Assert-Condition {
    param(
        [Parameter(Mandatory = $true)][bool]$Condition,
        [Parameter(Mandatory = $true)][string]$Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

function Assert-Contains {
    param(
        [Parameter(Mandatory = $true)][string]$Text,
        [Parameter(Mandatory = $true)][string]$Pattern,
        [Parameter(Mandatory = $true)][string]$Message
    )
    Assert-Condition -Condition ([regex]::IsMatch($Text, $Pattern, [Text.RegularExpressions.RegexOptions]::Multiline)) -Message $Message
}

function Assert-LiteralContains {
    param(
        [Parameter(Mandatory = $true)][string]$Text,
        [Parameter(Mandatory = $true)][string]$Expected,
        [Parameter(Mandatory = $true)][string]$Message
    )
    Assert-Condition -Condition $Text.Contains($Expected) -Message $Message
}

$laneRoot = Split-Path -Parent $PSScriptRoot
$expectedIds = @(
    'buildcraftlib', 'buildcraftcore', 'buildcraftbuilders', 'buildcraftenergy',
    'buildcraftfactory', 'buildcraftsilicon', 'buildcrafttransport', 'buildcraftrobotics'
)

$packPath = Join-Path $laneRoot 'src\main\resources\pack.mcmeta'
$pack = Get-Content -Raw $packPath | ConvertFrom-Json
Assert-Condition -Condition ($pack.pack.pack_format -eq 15) -Message 'pack.mcmeta must use Minecraft 1.20.1 pack format 15.'

$properties = Get-Content -Raw (Join-Path $laneRoot 'gradle.properties')
@{
    'minecraft_version' = '1.20.1'
    'neoforge_version' = '1.20.1-47.1.106'
    'neogradle_version' = '7.0.97'
    'loader_version_range' = '[47,)'
}.GetEnumerator() | ForEach-Object {
    $propertyPattern = '(?m)^' + [regex]::Escape($_.Key) + '=' + [regex]::Escape($_.Value) + '$'
    Assert-Condition -Condition ([regex]::IsMatch($properties, $propertyPattern)) -Message "Missing or incorrect Gradle property $($_.Key)."
}

$build = Get-Content -Raw (Join-Path $laneRoot 'build.gradle')
Assert-LiteralContains -Text $build -Expected "id 'net.neoforged.gradle.userdev' version '7.0.97'" -Message 'build.gradle must pin NeoGradle Userdev 7.0.97.'
Assert-LiteralContains -Text $build -Expected 'implementation "net.neoforged:forge:${neoforge_version}"' -Message 'build.gradle must use the exact transitional NeoForge forge artifact.'
Assert-LiteralContains -Text $build -Expected 'java.toolchain.languageVersion = JavaLanguageVersion.of(17)' -Message 'build.gradle must target Java 17.'
Assert-LiteralContains -Text $build -Expected "'buildcraftfactory'" -Message 'Run/datagen namespaces must use a real retained module ID.'

$toml = Get-Content -Raw (Join-Path $laneRoot 'src\main\resources\META-INF\mods.toml')
Assert-LiteralContains -Text $toml -Expected 'modLoader="javafml"' -Message 'mods.toml must use the transitional javafml loader.'
Assert-LiteralContains -Text $toml -Expected 'loaderVersion="[47,)"' -Message 'mods.toml must declare the transitional loader range.'
$actualIds = [regex]::Matches($toml, '(?m)^\[\[mods\]\]\r?\nmodId="([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
$difference = Compare-Object -ReferenceObject $expectedIds -DifferenceObject $actualIds
Assert-Condition -Condition ($null -eq $difference) -Message "mods.toml module IDs differ from the legacy contract: $($difference | Out-String)"

foreach ($moduleId in $expectedIds) {
    $escapedModule = [regex]::Escape($moduleId)
    $forgeDependencyPattern = '(?ms)^\[\[dependencies\.' + $escapedModule + '\]\]\r?\nmodId="forge"\r?\nmandatory=true\r?\nversionRange="\[47\.1,48\)"'
    $minecraftDependencyPattern = '(?ms)^\[\[dependencies\.' + $escapedModule + '\]\]\r?\nmodId="minecraft"\r?\nmandatory=true\r?\nversionRange="\[1\.20\.1,1\.21\)"'
    Assert-Contains -Text $toml -Pattern $forgeDependencyPattern -Message "$moduleId must depend on the transitional forge loader."
    Assert-Contains -Text $toml -Pattern $minecraftDependencyPattern -Message "$moduleId must declare the Minecraft 1.20.1 range."
}

# Preserve the intended legacy module graph, not a flattened every-module-to-lib graph:
# core -> lib; builders/energy/factory/transport/robotics -> core;
# silicon -> core and an optional, ordered-after transport edge.
$expectedModuleDependencies = [ordered]@{
    buildcraftlib = @()
    buildcraftcore = @(
        @{ modId = 'buildcraftlib'; mandatory = 'true'; versionRange = '[0,)'; ordering = 'AFTER'; side = 'BOTH' }
    )
    buildcraftbuilders = @(
        @{ modId = 'buildcraftcore'; mandatory = 'true'; versionRange = '[0,)'; ordering = 'AFTER'; side = 'BOTH' }
    )
    buildcraftenergy = @(
        @{ modId = 'buildcraftcore'; mandatory = 'true'; versionRange = '[0,)'; ordering = 'AFTER'; side = 'BOTH' }
    )
    buildcraftfactory = @(
        @{ modId = 'buildcraftcore'; mandatory = 'true'; versionRange = '[0,)'; ordering = 'AFTER'; side = 'BOTH' }
    )
    buildcraftsilicon = @(
        @{ modId = 'buildcraftcore'; mandatory = 'true'; versionRange = '[0,)'; ordering = 'AFTER'; side = 'BOTH' },
        @{ modId = 'buildcrafttransport'; mandatory = 'false'; versionRange = '[0,)'; ordering = 'AFTER'; side = 'BOTH' }
    )
    buildcrafttransport = @(
        @{ modId = 'buildcraftcore'; mandatory = 'true'; versionRange = '[0,)'; ordering = 'AFTER'; side = 'BOTH' }
    )
    buildcraftrobotics = @(
        @{ modId = 'buildcraftcore'; mandatory = 'true'; versionRange = '[0,)'; ordering = 'AFTER'; side = 'BOTH' }
    )
}

$dependencyBlocks = [regex]::Matches(
    $toml,
    '(?ms)^\[\[dependencies\.([^\]]+)\]\]\r?\n(.*?)(?=^\[\[dependencies\.|\z)'
)
$actualModuleDependencies = @{}
foreach ($dependencyBlock in $dependencyBlocks) {
    $owner = $dependencyBlock.Groups[1].Value
    $body = $dependencyBlock.Groups[2].Value
    $targetMatch = [regex]::Match($body, '(?m)^modId="([^"]+)"$')
    Assert-Condition -Condition $targetMatch.Success -Message "Dependency block for $owner has no modId."
    $target = $targetMatch.Groups[1].Value
    if ($target -notlike 'buildcraft*') {
        continue
    }
    Assert-Condition -Condition ($expectedIds -contains $owner) -Message "Unexpected BuildCraft dependency owner $owner."
    Assert-Condition -Condition ($expectedIds -contains $target) -Message "Unexpected BuildCraft dependency target $target."
    $dependency = @{
        modId = $target
        mandatory = ([regex]::Match($body, '(?m)^mandatory=(true|false)$')).Groups[1].Value
        versionRange = ([regex]::Match($body, '(?m)^versionRange="([^"]+)"$')).Groups[1].Value
        ordering = ([regex]::Match($body, '(?m)^ordering="([^"]+)"$')).Groups[1].Value
        side = ([regex]::Match($body, '(?m)^side="([^"]+)"$')).Groups[1].Value
    }
    foreach ($field in @('mandatory', 'versionRange', 'ordering', 'side')) {
        Assert-Condition -Condition (-not [string]::IsNullOrWhiteSpace($dependency[$field])) -Message "BuildCraft dependency $owner -> $target is missing $field."
    }
    if (-not $actualModuleDependencies.ContainsKey($owner)) {
        $actualModuleDependencies[$owner] = @()
    }
    $actualModuleDependencies[$owner] += $dependency
}

foreach ($moduleId in $expectedIds) {
    $expectedEdges = @($expectedModuleDependencies[$moduleId])
    $actualEdges = @()
    if ($actualModuleDependencies.ContainsKey($moduleId)) {
        $actualEdges = @($actualModuleDependencies[$moduleId])
    }
    Assert-Condition -Condition ($actualEdges.Count -eq $expectedEdges.Count) -Message "$moduleId has an incorrect number of BuildCraft module dependencies."
    foreach ($expectedEdge in $expectedEdges) {
        $matchingEdges = @($actualEdges | Where-Object {
            $_.modId -eq $expectedEdge.modId -and
            $_.mandatory -eq $expectedEdge.mandatory -and
            $_.versionRange -eq $expectedEdge.versionRange -and
            $_.ordering -eq $expectedEdge.ordering -and
            $_.side -eq $expectedEdge.side
        })
        Assert-Condition -Condition ($matchingEdges.Count -eq 1) -Message "$moduleId must declare exactly the expected BuildCraft dependency on $($expectedEdge.modId)."
    }
}

$sourceRoot = Join-Path $laneRoot 'src\main\java\buildcraft\neo\neoforge1201'
$idsSource = Get-Content -Raw (Join-Path $sourceRoot 'LegacyModuleIds.java')
$constantMap = [ordered]@{
    LIB = 'buildcraftlib'
    CORE = 'buildcraftcore'
    BUILDERS = 'buildcraftbuilders'
    ENERGY = 'buildcraftenergy'
    FACTORY = 'buildcraftfactory'
    SILICON = 'buildcraftsilicon'
    TRANSPORT = 'buildcrafttransport'
    ROBOTICS = 'buildcraftrobotics'
}
$constantMap.GetEnumerator() | ForEach-Object {
    Assert-LiteralContains -Text $idsSource -Expected ('public static final String ' + $_.Key + ' = "' + $_.Value + '";') -Message "LegacyModuleIds must retain $($_.Value)."
}

$bootstrap = Get-Content -Raw (Join-Path $sourceRoot 'BuildCraftNeo.java')
Assert-LiteralContains -Text $bootstrap -Expected 'RESOURCE_NAMESPACE = "buildcraft"' -Message 'The shared resource namespace must remain buildcraft.'
Assert-LiteralContains -Text $bootstrap -Expected 'import net.minecraftforge.eventbus.api.IEventBus;' -Message 'The transitional event-bus package must be explicit.'

$entrypoints = [ordered]@{
    BuildCraftLibEntrypoint = 'LIB'
    BuildCraftCoreEntrypoint = 'CORE'
    BuildCraftBuildersEntrypoint = 'BUILDERS'
    BuildCraftEnergyEntrypoint = 'ENERGY'
    BuildCraftFactoryEntrypoint = 'FACTORY'
    BuildCraftSiliconEntrypoint = 'SILICON'
    BuildCraftTransportEntrypoint = 'TRANSPORT'
    BuildCraftRoboticsEntrypoint = 'ROBOTICS'
}
$entrypoints.GetEnumerator() | ForEach-Object {
    $entrypoint = Get-Content -Raw (Join-Path $sourceRoot ($_.Key + '.java'))
    Assert-LiteralContains -Text $entrypoint -Expected 'import net.minecraftforge.fml.common.Mod;' -Message "$($_.Key) must use the transitional @Mod package."
    Assert-LiteralContains -Text $entrypoint -Expected ('@Mod(LegacyModuleIds.' + $_.Value + ')') -Message "$($_.Key) must retain its module ID annotation."
    Assert-LiteralContains -Text $entrypoint -Expected ('super(LegacyModuleIds.' + $_.Value + ');') -Message "$($_.Key) must initialize the matching module ID."
}

Write-Output 'NeoForge 1.20.1 static lane validation passed: metadata, dependency graph, entrypoints, and pack JSON are consistent.'
