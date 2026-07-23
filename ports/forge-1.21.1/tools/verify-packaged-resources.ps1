param(
    [Parameter(Mandatory = $true)]
    [string] $JarPath
)

$ErrorActionPreference = 'Stop'
$resolvedJar = (Resolve-Path -LiteralPath $JarPath).Path
if ([IO.Path]::GetExtension($resolvedJar) -ne '.jar') {
    throw "Expected a JAR file: $resolvedJar"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($resolvedJar)
try {
    $entries = @{}
    foreach ($entry in $archive.Entries) {
        $entries[$entry.FullName] = $entry
    }

    $required = @(
        'META-INF/mods.toml',
        'pack.mcmeta',
        'assets/buildcraftfactory/blockstates/tank.json',
        'assets/buildcraftfactory/models/block/tank.json',
        'assets/buildcraftfactory/models/block/tank_joined_below.json',
        'assets/buildcraftfactory/models/item/tank.json',
        'assets/buildcraftfactory/lang/en_us.json',
        'data/buildcraftfactory/loot_table/blocks/tank.json',
        'assets/buildcraftfactory/textures/block/tank/end.png',
        'assets/buildcraftfactory/textures/block/tank/side.png',
        'assets/buildcraftfactory/textures/block/tank/side_joined_below.png',
        'assets/buildcraftfactory/textures/blocks/tank/end.png',
        'assets/buildcraftfactory/textures/blocks/tank/side.png',
        'assets/buildcraftfactory/textures/blocks/tank/side_joined_below.png',
        'assets/buildcraftfactory/textures/gui/tank.png'
    )
    foreach ($entryName in $required) {
        if (-not $entries.ContainsKey($entryName)) {
            throw "Required packaged resource is missing: $entryName"
        }
    }

    $jsonEntries = $required | Where-Object { $_.EndsWith('.json') -or $_.EndsWith('.mcmeta') }
    $parsedJson = @{}
    foreach ($entryName in $jsonEntries) {
        $reader = [IO.StreamReader]::new($entries[$entryName].Open())
        try {
            $parsedJson[$entryName] = $reader.ReadToEnd() | ConvertFrom-Json
        } finally {
            $reader.Dispose()
        }
    }

    $pack = $parsedJson['pack.mcmeta']
    if ($pack.pack.pack_format -ne 48 -or @($pack.pack.supported_formats).Count -ne 2 -or
        $pack.pack.supported_formats[0] -ne 34 -or $pack.pack.supported_formats[1] -ne 48) {
        throw 'pack.mcmeta must support Minecraft 1.21.1 resource format 34 and data format 48.'
    }

    # Minecraft 1.21.1's blocks atlas scans textures/block and publishes the
    # corresponding block/* sprite IDs. Keeping the legacy textures/blocks
    # copies preserves source parity, but models must use the atlas path.
    $tankBlockModel = $parsedJson['assets/buildcraftfactory/models/block/tank.json']
    if ($tankBlockModel.textures.particle -ne 'buildcraftfactory:block/tank/side' -or
        $tankBlockModel.textures.side -ne 'buildcraftfactory:block/tank/side' -or
        $tankBlockModel.textures.up -ne 'buildcraftfactory:block/tank/end' -or
        $tankBlockModel.textures.down -ne 'buildcraftfactory:block/tank/end') {
        throw 'Tank block model must reference the 1.21.1 block-atlas sprite IDs.'
    }
    $joinedTankModel = $parsedJson['assets/buildcraftfactory/models/block/tank_joined_below.json']
    if ($joinedTankModel.textures.particle -ne 'buildcraftfactory:block/tank/side_joined_below' -or
        $joinedTankModel.textures.side -ne 'buildcraftfactory:block/tank/side_joined_below') {
        throw 'Joined Tank model must reference the 1.21.1 block-atlas sprite ID.'
    }

    $tomlReader = [IO.StreamReader]::new($entries['META-INF/mods.toml'].Open())
    try {
        $toml = $tomlReader.ReadToEnd()
    } finally {
        $tomlReader.Dispose()
    }

    $moduleIds = @(
        'buildcraftlib', 'buildcraftcore', 'buildcraftbuilders', 'buildcraftenergy',
        'buildcraftfactory', 'buildcraftsilicon', 'buildcrafttransport', 'buildcraftrobotics'
    )
    foreach ($moduleId in $moduleIds) {
        if ($toml -notmatch ('modId="' + [regex]::Escape($moduleId) + '"')) {
            throw "mods.toml is missing legacy module identity: $moduleId"
        }
    }

    # Parse dependency blocks rather than only searching for text. This guards
    # the legacy module load graph and Silicon's optional-after Transport rule.
    $dependencyBlocks = [System.Collections.Generic.List[object]]::new()
    $currentSource = $null
    $currentValues = $null
    foreach ($line in ($toml -split "`r?`n")) {
        if ($line -match '^\[\[dependencies\.([a-z0-9_]+)\]\]$') {
            if ($null -ne $currentSource) {
                $dependencyBlocks.Add([pscustomobject]@{
                    Source = $currentSource
                    ModId = $currentValues['modId']
                    Mandatory = $currentValues['mandatory']
                    Ordering = $currentValues['ordering']
                })
            }
            $currentSource = $matches[1]
            $currentValues = @{}
            continue
        }
        if ($null -ne $currentSource -and $line -match '^\s*(modId|mandatory|ordering)\s*=\s*(.+?)\s*$') {
            $currentValues[$matches[1]] = $matches[2].Trim().Trim('"')
        }
    }
    if ($null -ne $currentSource) {
        $dependencyBlocks.Add([pscustomobject]@{
            Source = $currentSource
            ModId = $currentValues['modId']
            Mandatory = $currentValues['mandatory']
            Ordering = $currentValues['ordering']
        })
    }

    $expectedEdges = @(
        @{ Source = 'buildcraftcore'; Target = 'buildcraftlib'; Mandatory = 'true' },
        @{ Source = 'buildcraftbuilders'; Target = 'buildcraftcore'; Mandatory = 'true' },
        @{ Source = 'buildcraftenergy'; Target = 'buildcraftcore'; Mandatory = 'true' },
        @{ Source = 'buildcraftfactory'; Target = 'buildcraftcore'; Mandatory = 'true' },
        @{ Source = 'buildcraftsilicon'; Target = 'buildcraftcore'; Mandatory = 'true' },
        @{ Source = 'buildcraftsilicon'; Target = 'buildcrafttransport'; Mandatory = 'false' },
        @{ Source = 'buildcrafttransport'; Target = 'buildcraftcore'; Mandatory = 'true' },
        @{ Source = 'buildcraftrobotics'; Target = 'buildcraftcore'; Mandatory = 'true' }
    )
    foreach ($edge in $expectedEdges) {
        $match = $dependencyBlocks | Where-Object {
            $_.Source -eq $edge.Source -and $_.ModId -eq $edge.Target -and
            $_.Mandatory -eq $edge.Mandatory -and $_.Ordering -eq 'AFTER'
        }
        if ($null -eq $match) {
            throw "mods.toml is missing the legacy dependency edge $($edge.Source) -> $($edge.Target) (mandatory=$($edge.Mandatory), ordering=AFTER)"
        }
    }

    [pscustomobject]@{
        Jar = $resolvedJar
        EntryCount = $archive.Entries.Count
        JsonResourcesParsed = $jsonEntries.Count
        LegacyModulesValidated = $moduleIds.Count
        LegacyDependencyEdgesValidated = $expectedEdges.Count
        Result = 'PASS'
    } | Format-List
} finally {
    $archive.Dispose()
}
