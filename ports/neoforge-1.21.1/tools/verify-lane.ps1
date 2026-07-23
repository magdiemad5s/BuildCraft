[CmdletBinding()]
param(
    [string]$JarPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$laneRoot = Split-Path -Parent $PSScriptRoot
$expectedModules = @(
    'buildcraftlib',
    'buildcraftcore',
    'buildcraftbuilders',
    'buildcraftenergy',
    'buildcraftfactory',
    'buildcraftsilicon',
    'buildcrafttransport',
    'buildcraftrobotics'
)
$entrypoints = [ordered]@{
    buildcraftlib       = @('LIB', 'BuildCraftLibEntrypoint')
    buildcraftcore      = @('CORE', 'BuildCraftCoreEntrypoint')
    buildcraftbuilders  = @('BUILDERS', 'BuildCraftBuildersEntrypoint')
    buildcraftenergy    = @('ENERGY', 'BuildCraftEnergyEntrypoint')
    buildcraftfactory   = @('FACTORY', 'BuildCraftFactoryEntrypoint')
    buildcraftsilicon   = @('SILICON', 'BuildCraftSiliconEntrypoint')
    buildcrafttransport = @('TRANSPORT', 'BuildCraftTransportEntrypoint')
    buildcraftrobotics  = @('ROBOTICS', 'BuildCraftRoboticsEntrypoint')
}

function Assert-Equal([object]$Actual, [object]$Expected, [string]$Message) {
    $actualText = @($Actual) -join ','
    $expectedText = @($Expected) -join ','
    if ($actualText -ne $expectedText) {
        throw "$Message Expected '$expectedText', found '$actualText'."
    }
}

function Get-TomlStringField([string]$Body, [string]$Field, [string]$Origin) {
    $pattern = '(?m)^' + [regex]::Escape($Field) + '\s*=\s*"([^"]*)"\s*$'
    $matches = [regex]::Matches($Body, $pattern)
    if ($matches.Count -ne 1) {
        throw "$Origin must declare exactly one $Field string."
    }
    return $matches[0].Groups[1].Value
}

function Get-ModuleIds([string]$Toml, [string]$Origin) {
    $moduleIds = @(
        [regex]::Matches($Toml, '(?m)^\[\[mods\]\]\s*\r?\nmodId="([^"]+)"$') |
            ForEach-Object { $_.Groups[1].Value }
    )
    Assert-Equal $moduleIds $expectedModules "$Origin module IDs drifted."
}

function Get-DependencyRecords([string]$Toml, [string]$Origin) {
    $sectionPattern = '(?ms)^\[\[dependencies\.([a-z0-9_]+)\]\][^\S\r\n]*\r?\n(.*?)(?=^\[\[|\z)'
    $sections = [regex]::Matches($Toml, $sectionPattern)
    if ($sections.Count -eq 0) {
        throw "$Origin declares no dependency sections."
    }

    foreach ($section in $sections) {
        $owner = $section.Groups[1].Value
        $body = $section.Groups[2].Value
        $sectionOrigin = "$Origin dependency section for $owner"
        [pscustomobject]@{
            Owner        = $owner
            ModId        = Get-TomlStringField $body 'modId' $sectionOrigin
            Type         = Get-TomlStringField $body 'type' $sectionOrigin
            VersionRange = Get-TomlStringField $body 'versionRange' $sectionOrigin
            Ordering     = Get-TomlStringField $body 'ordering' $sectionOrigin
            Side         = Get-TomlStringField $body 'side' $sectionOrigin
        }
    }
}

function New-ExpectedDependency(
    [string]$ModId,
    [string]$Type,
    [string]$VersionRange,
    [string]$Ordering,
    [string]$Side
) {
    [pscustomobject]@{
        ModId        = $ModId
        Type         = $Type
        VersionRange = $VersionRange
        Ordering     = $Ordering
        Side         = $Side
    }
}

$expectedDependencies = [ordered]@{
    buildcraftlib = @(
        (New-ExpectedDependency 'neoforge' 'required' '[${neo_version},)' 'NONE' 'BOTH'),
        (New-ExpectedDependency 'minecraft' 'required' '${minecraft_version_range}' 'NONE' 'BOTH')
    )
    buildcraftcore = @(
        (New-ExpectedDependency 'buildcraftlib' 'required' '[0,)' 'AFTER' 'BOTH'),
        (New-ExpectedDependency 'neoforge' 'required' '[${neo_version},)' 'NONE' 'BOTH'),
        (New-ExpectedDependency 'minecraft' 'required' '${minecraft_version_range}' 'NONE' 'BOTH')
    )
    buildcraftbuilders = @(
        (New-ExpectedDependency 'buildcraftcore' 'required' '[0,)' 'AFTER' 'BOTH'),
        (New-ExpectedDependency 'neoforge' 'required' '[${neo_version},)' 'NONE' 'BOTH'),
        (New-ExpectedDependency 'minecraft' 'required' '${minecraft_version_range}' 'NONE' 'BOTH')
    )
    buildcraftenergy = @(
        (New-ExpectedDependency 'buildcraftcore' 'required' '[0,)' 'AFTER' 'BOTH'),
        (New-ExpectedDependency 'neoforge' 'required' '[${neo_version},)' 'NONE' 'BOTH'),
        (New-ExpectedDependency 'minecraft' 'required' '${minecraft_version_range}' 'NONE' 'BOTH')
    )
    buildcraftfactory = @(
        (New-ExpectedDependency 'buildcraftcore' 'required' '[0,)' 'AFTER' 'BOTH'),
        (New-ExpectedDependency 'neoforge' 'required' '[${neo_version},)' 'NONE' 'BOTH'),
        (New-ExpectedDependency 'minecraft' 'required' '${minecraft_version_range}' 'NONE' 'BOTH')
    )
    buildcraftsilicon = @(
        (New-ExpectedDependency 'buildcraftcore' 'required' '[0,)' 'AFTER' 'BOTH'),
        (New-ExpectedDependency 'buildcrafttransport' 'optional' '[0,)' 'AFTER' 'BOTH'),
        (New-ExpectedDependency 'neoforge' 'required' '[${neo_version},)' 'NONE' 'BOTH'),
        (New-ExpectedDependency 'minecraft' 'required' '${minecraft_version_range}' 'NONE' 'BOTH')
    )
    buildcrafttransport = @(
        (New-ExpectedDependency 'buildcraftcore' 'required' '[0,)' 'AFTER' 'BOTH'),
        (New-ExpectedDependency 'neoforge' 'required' '[${neo_version},)' 'NONE' 'BOTH'),
        (New-ExpectedDependency 'minecraft' 'required' '${minecraft_version_range}' 'NONE' 'BOTH')
    )
    buildcraftrobotics = @(
        (New-ExpectedDependency 'buildcraftcore' 'required' '[0,)' 'AFTER' 'BOTH'),
        (New-ExpectedDependency 'neoforge' 'required' '[${neo_version},)' 'NONE' 'BOTH'),
        (New-ExpectedDependency 'minecraft' 'required' '${minecraft_version_range}' 'NONE' 'BOTH')
    )
}

function Assert-DependencyGraph(
    [string]$Toml,
    [string]$Origin,
    [switch]$RequireTemplateVersionRanges
) {
    $records = @(Get-DependencyRecords $Toml $Origin)
    $actualOwners = @($records | ForEach-Object { $_.Owner } | Select-Object -Unique)
    Assert-Equal $actualOwners $expectedModules "$Origin dependency owners drifted."

    foreach ($moduleId in $expectedModules) {
        $actualForModule = @($records | Where-Object { $_.Owner -eq $moduleId })
        $expectedForModule = @($expectedDependencies[$moduleId])
        if ($actualForModule.Count -ne $expectedForModule.Count) {
            throw "$Origin has $($actualForModule.Count) dependency declarations for $moduleId; expected $($expectedForModule.Count)."
        }

        for ($index = 0; $index -lt $expectedForModule.Count; $index++) {
            $actual = $actualForModule[$index]
            $expected = $expectedForModule[$index]
            foreach ($field in @('ModId', 'Type', 'Ordering', 'Side')) {
                if ($actual.$field -ne $expected.$field) {
                    throw "$Origin dependency $moduleId -> $($expected.ModId) has $field '$($actual.$field)'; expected '$($expected.$field)'."
                }
            }

            if ($RequireTemplateVersionRanges -and $actual.VersionRange -ne $expected.VersionRange) {
                throw "$Origin dependency $moduleId -> $($expected.ModId) has versionRange '$($actual.VersionRange)'; expected '$($expected.VersionRange)'."
            }
            if ([string]::IsNullOrWhiteSpace($actual.VersionRange)) {
                throw "$Origin dependency $moduleId -> $($expected.ModId) must declare a non-empty versionRange."
            }
        }
    }
}

function Get-ZipEntryText($Archive, [string]$Name) {
    $entry = $Archive.GetEntry($Name)
    if ($null -eq $entry) {
        throw "Packaged JAR is missing $Name."
    }
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try {
        return $reader.ReadToEnd()
    }
    finally {
        $reader.Dispose()
    }
}

$metadataPath = Join-Path $laneRoot 'src\main\templates\META-INF\neoforge.mods.toml'
$legacyModsPath = Join-Path $laneRoot 'src\main\resources\META-INF\mods.toml'
$packPath = Join-Path $laneRoot 'src\main\resources\pack.mcmeta'
$resourceNamespacePath = Join-Path $laneRoot 'src\main\resources\assets\buildcraft\lang\en_us.json'
$tankContractPath = Join-Path $laneRoot 'src\main\java\buildcraft\neo\neoforge1211\factory\FactoryTankContract.java'
$tankResourcePaths = @(
    'assets/buildcraftfactory/blockstates/tank.json',
    'assets/buildcraftfactory/models/block/tank.json',
    'assets/buildcraftfactory/models/block/tank_joined_below.json',
    'assets/buildcraftfactory/models/item/tank.json',
    'assets/buildcraftfactory/lang/en_us.json',
    'data/buildcraftfactory/loot_table/blocks/tank.json',
    'data/buildcraftfactory/recipe/tank.json'
)

if (-not (Test-Path -LiteralPath $metadataPath -PathType Leaf)) {
    throw "Missing NeoForge metadata template: $metadataPath"
}
if (Test-Path -LiteralPath $legacyModsPath) {
    throw 'NeoForge 1.21.1 must use META-INF/neoforge.mods.toml, not META-INF/mods.toml.'
}
if (-not (Test-Path -LiteralPath $resourceNamespacePath -PathType Leaf)) {
    throw 'The preserved buildcraft resource namespace is missing.'
}
if (-not (Test-Path -LiteralPath $tankContractPath -PathType Leaf)) {
    throw 'The Factory Tank compatibility contract is missing.'
}

$metadata = Get-Content -LiteralPath $metadataPath -Raw
Get-ModuleIds $metadata 'NeoForge metadata template'
Assert-DependencyGraph $metadata 'NeoForge metadata template' -RequireTemplateVersionRanges

foreach ($moduleId in $entrypoints.Keys) {
    $details = $entrypoints[$moduleId]
    $javaPath = Join-Path $laneRoot ("src\main\java\buildcraft\neo\neoforge1211\{0}.java" -f $details[1])
    if (-not (Test-Path -LiteralPath $javaPath -PathType Leaf)) {
        throw "Missing Java entrypoint for ${moduleId}: $javaPath"
    }
    $source = Get-Content -LiteralPath $javaPath -Raw
    if ($source -notmatch [regex]::Escape("@Mod(LegacyModuleIds.$($details[0]))")) {
        throw "Entrypoint $($details[1]) does not map to legacy module $moduleId."
    }
}

$identitySource = Get-Content -LiteralPath (Join-Path $laneRoot 'src\main\java\buildcraft\neo\neoforge1211\BuildCraftNeo.java') -Raw
if ($identitySource -notmatch 'RESOURCE_NAMESPACE\s*=\s*"buildcraft"') {
    throw 'The shared buildcraft resource/data namespace constant drifted.'
}

$tankContract = Get-Content -LiteralPath $tankContractPath -Raw
if ($tankContract -notmatch 'REGISTRY_PATH\s*=\s*"tank"' -or $tankContract -notmatch 'CAPACITY_MILLIBUCKETS\s*=\s*16_000') {
    throw 'Factory Tank registry ID or capacity drifted.'
}

$pack = Get-Content -LiteralPath $packPath -Raw | ConvertFrom-Json
if ([int]$pack.pack.pack_format -ne 48) {
    throw 'Minecraft 1.21.1 server-data pack format must be 48.'
}
Assert-Equal @($pack.pack.supported_formats) @(34, 48) 'pack.mcmeta supported formats drifted.'

foreach ($resourcePath in $tankResourcePaths) {
    $sourcePath = Join-Path $laneRoot ('src\main\resources\' + $resourcePath)
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Factory Tank resource is missing: $sourcePath"
    }
    Get-Content -LiteralPath $sourcePath -Raw | ConvertFrom-Json | Out-Null
}

if ($JarPath) {
    $resolvedJar = (Resolve-Path -LiteralPath $JarPath).Path
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($resolvedJar)
    try {
        $names = @($archive.Entries | ForEach-Object FullName)
        foreach ($requiredEntry in @('META-INF/neoforge.mods.toml', 'pack.mcmeta', 'assets/buildcraft/lang/en_us.json') + $tankResourcePaths) {
            if ($names -notcontains $requiredEntry) {
                throw "Packaged JAR is missing $requiredEntry."
            }
        }

        $packagedMetadata = Get-ZipEntryText $archive 'META-INF/neoforge.mods.toml'
        Get-ModuleIds $packagedMetadata 'Packaged NeoForge metadata'
        Assert-DependencyGraph $packagedMetadata 'Packaged NeoForge metadata'

        $jsonEntries = @($archive.Entries | Where-Object { $_.FullName.EndsWith('.json') })
        foreach ($jsonEntry in $jsonEntries) {
            Get-ZipEntryText $archive $jsonEntry.FullName | ConvertFrom-Json -ErrorAction Stop | Out-Null
        }
        Get-ZipEntryText $archive 'pack.mcmeta' | ConvertFrom-Json -ErrorAction Stop | Out-Null
        Write-Output "Packaged JAR validation passed: $($names.Count) entries; $($jsonEntries.Count) JSON files parsed."
    }
    finally {
        $archive.Dispose()
    }
}

Write-Output 'NeoForge 1.21.1 lane static validation passed.'
