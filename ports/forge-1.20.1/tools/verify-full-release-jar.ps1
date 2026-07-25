[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string] $JarPath,

    [string] $SourceResources = (Join-Path $PSScriptRoot '..\src\main\resources'),

    [string] $GeneratedResources = (Join-Path $PSScriptRoot '..\src\generated\resources')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$jar = (Resolve-Path -LiteralPath $JarPath).Path
$sourceRoot = (Resolve-Path -LiteralPath $SourceResources).Path
$generatedRoot = if (Test-Path -LiteralPath $GeneratedResources -PathType Container) {
    (Resolve-Path -LiteralPath $GeneratedResources).Path
} else {
    $null
}

if ([IO.Path]::GetExtension($jar) -ne '.jar') {
    throw "Expected a JAR file: $jar"
}

function Get-EntryText {
    param(
        [Parameter(Mandatory = $true)] $Entry
    )

    $reader = [IO.StreamReader]::new($Entry.Open())
    try {
        return $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
    }
}

function Get-RelativeResourcePath {
    param(
        [Parameter(Mandatory = $true)][string] $Root,
        [Parameter(Mandatory = $true)][string] $Path
    )

    $rootWithSeparator = $Root.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    return $Path.Substring($rootWithSeparator.Length).Replace('\', '/')
}

$moduleIds = @(
    'buildcraftlib',
    'buildcraftcore',
    'buildcraftbuilders',
    'buildcraftenergy',
    'buildcraftfactory',
    'buildcraftsilicon',
    'buildcrafttransport',
    'buildcraftrobotics'
)

$resourceNamespaces = @(
    'buildcraft',
    'buildcraftlib',
    'buildcraftcore',
    'buildcraftbuilders',
    'buildcraftenergy',
    'buildcraftfactory',
    'buildcraftsilicon',
    'buildcrafttransport',
    'buildcraftrobotics'
)

Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.Web.Extensions
$jsonSerializer = [System.Web.Script.Serialization.JavaScriptSerializer]::new()
$jsonSerializer.MaxJsonLength = [int]::MaxValue
$jsonSerializer.RecursionLimit = 512

$archive = [IO.Compression.ZipFile]::OpenRead($jar)
try {
    $entries = [Collections.Generic.Dictionary[string, object]]::new(
        [StringComparer]::Ordinal
    )
    foreach ($entry in $archive.Entries) {
        if ($entries.ContainsKey($entry.FullName)) {
            throw "Duplicate JAR entry: $($entry.FullName)"
        }
        $entries.Add($entry.FullName, $entry)
    }

    foreach ($required in @('META-INF/MANIFEST.MF', 'META-INF/mods.toml', 'pack.mcmeta', 'LICENSE.txt')) {
        if (-not $entries.ContainsKey($required)) {
            throw "Required release entry is missing: $required"
        }
    }

    $expectedResources = [Collections.Generic.HashSet[string]]::new(
        [StringComparer]::Ordinal
    )
    # Restored legacy resources are release-authoritative. runData writes a
    # validation tree whose renamed advancements must not be merged into the JAR.
    foreach ($root in @($sourceRoot)) {
        if ($null -eq $root) {
            continue
        }
        foreach ($file in Get-ChildItem -LiteralPath $root -Recurse -Force -File) {
            $relative = Get-RelativeResourcePath -Root $root -Path $file.FullName
            if ($relative -eq '.cache' -or $relative.StartsWith('.cache/')) {
                continue
            }
            [void] $expectedResources.Add($relative)
        }
    }

    $missingResources = @(
        foreach ($path in $expectedResources) {
            if (-not $entries.ContainsKey($path)) {
                $path
            }
        }
    )
    if ($missingResources.Count -gt 0) {
        throw "The release JAR is missing $($missingResources.Count) source resources. First: $((($missingResources | Select-Object -First 10) -join ', '))"
    }

    $cacheEntries = @(
        $entries.Keys | Where-Object {
            $_ -eq '.cache' -or $_.StartsWith('.cache/') -or $_.Contains('/.cache/')
        }
    )
    if ($cacheEntries.Count -gt 0) {
        throw "Data-generator cache files leaked into the release JAR: $((($cacheEntries | Select-Object -First 10) -join ', '))"
    }

    $jsonEntries = @(
        $entries.Keys | Where-Object {
            $_ -eq 'pack.mcmeta' -or (
                $_.EndsWith('.json', [StringComparison]::OrdinalIgnoreCase) -and
                ($_.StartsWith('assets/') -or $_.StartsWith('data/'))
            )
        }
    )
    foreach ($path in $jsonEntries) {
        try {
            $null = $jsonSerializer.DeserializeObject((Get-EntryText -Entry $entries[$path]))
        } catch {
            throw "Invalid packaged JSON '$path': $($_.Exception.Message)"
        }
    }

    foreach ($namespace in $resourceNamespaces) {
        if (-not ($entries.Keys | Where-Object {
            $_.StartsWith("assets/$namespace/") -or $_.StartsWith("data/$namespace/")
        } | Select-Object -First 1)) {
            throw "Packaged resources are missing namespace '$namespace'"
        }
    }

    $toml = Get-EntryText -Entry $entries['META-INF/mods.toml']
    if ($toml -match 'BuildCraft Neo Better|buildcraft-neo-better') {
        throw 'Accidental "BuildCraft Neo Better" branding remains in packaged metadata.'
    }
    $expectedLogo = 'buildcraft_neo_logo.png'
    if (-not $entries.ContainsKey($expectedLogo)) {
        throw "The BuildCraft Neo logo is missing from the release JAR: $expectedLogo"
    }
    $logoReferenceCount = [regex]::Matches(
        $toml,
        '(?m)^\s*logoFile="buildcraft_neo_logo\.png"\s*$'
    ).Count
    if ($logoReferenceCount -ne $moduleIds.Count) {
        throw "Expected all $($moduleIds.Count) modules to reference the BuildCraft Neo logo; found $logoReferenceCount."
    }
    $displayNameCount = [regex]::Matches(
        $toml,
        '(?m)^\s*displayName="BuildCraft Neo - [^"]+"\s*$'
    ).Count
    if ($displayNameCount -ne $moduleIds.Count) {
        throw "Expected all $($moduleIds.Count) module display names to use the BuildCraft Neo prefix; found $displayNameCount."
    }
    foreach ($moduleId in $moduleIds) {
        if ($toml -notmatch ('modId="' + [regex]::Escape($moduleId) + '"')) {
            throw "mods.toml is missing legacy module identity '$moduleId'"
        }
    }
    if ($toml -notmatch '(?s)\[\[dependencies\.buildcraftsilicon\]\]\s+modId="buildcrafttransport"\s+mandatory=false\s+versionRange="[^"]+"\s+ordering="AFTER"') {
        throw 'mods.toml is missing the optional Silicon-after-Transport dependency edge.'
    }

    $manifest = Get-EntryText -Entry $entries['META-INF/MANIFEST.MF']
    if ($manifest -notmatch '(?m)^Implementation-Title: BuildCraft Neo\r?$') {
        throw 'The release manifest does not identify the project as BuildCraft Neo.'
    }

    $classCount = @($entries.Keys | Where-Object { $_.EndsWith('.class') }).Count
    $pngCount = @($entries.Keys | Where-Object { $_.EndsWith('.png') }).Count
    $guiPngCount = @($entries.Keys | Where-Object {
        $_.EndsWith('.png') -and $_.Contains('/textures/gui/')
    }).Count
    $modelCount = @($entries.Keys | Where-Object {
        $_.StartsWith('assets/') -and $_.Contains('/models/') -and $_.EndsWith('.json')
    }).Count
    $blockstateCount = @($entries.Keys | Where-Object {
        $_.StartsWith('assets/') -and $_.Contains('/blockstates/') -and $_.EndsWith('.json')
    }).Count
    $recipeCount = @($entries.Keys | Where-Object {
        $_ -match '^data/[^/]+/recipes/.+\.json$'
    }).Count

    if ($classCount -lt 1000) {
        throw "The release contains only $classCount classes; the full eight-module port was not packaged."
    }
    if ($pngCount -lt 1153 -or $guiPngCount -lt 60) {
        throw "Artwork packaging is incomplete (PNG=$pngCount, GUI PNG=$guiPngCount)."
    }
    if ($modelCount -lt 454 -or $blockstateCount -lt 72) {
        throw "Model packaging is incomplete (models=$modelCount, blockstates=$blockstateCount)."
    }
    if ($recipeCount -lt 207) {
        throw "Recipe packaging is incomplete (recipes=$recipeCount)."
    }

    [pscustomobject]@{
        Result = 'PASS'
        Jar = $jar
        Entries = $entries.Count
        Classes = $classCount
        JsonParsed = $jsonEntries.Count
        Png = $pngCount
        GuiPng = $guiPngCount
        Models = $modelCount
        Blockstates = $blockstateCount
        Recipes = $recipeCount
        ExpectedResources = $expectedResources.Count
        LegacyModules = $moduleIds.Count
    } | Format-List
} finally {
    $archive.Dispose()
}

& (Join-Path $PSScriptRoot 'verify-texture-parity.ps1') `
    -InputPath $jar `
    -WarningsAsErrors
