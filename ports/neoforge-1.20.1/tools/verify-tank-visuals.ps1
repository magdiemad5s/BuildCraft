[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$JarPath
)

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

function Assert-LiteralContains {
    param(
        [Parameter(Mandatory = $true)][string]$Text,
        [Parameter(Mandatory = $true)][string]$Expected,
        [Parameter(Mandatory = $true)][string]$Message
    )
    Assert-Condition -Condition $Text.Contains($Expected) -Message $Message
}

function Assert-Sequence {
    param(
        [Parameter(Mandatory = $true)][object[]]$Actual,
        [Parameter(Mandatory = $true)][object[]]$Expected,
        [Parameter(Mandatory = $true)][string]$Message
    )
    $actualText = @($Actual) -join ','
    $expectedText = @($Expected) -join ','
    Assert-Condition -Condition ($actualText -eq $expectedText) -Message "$Message Expected '$expectedText', found '$actualText'."
}

function Assert-PngDimensions {
    param(
        [Parameter(Mandatory = $true)][byte[]]$Bytes,
        [Parameter(Mandatory = $true)][int]$ExpectedWidth,
        [Parameter(Mandatory = $true)][int]$ExpectedHeight,
        [Parameter(Mandatory = $true)][string]$Origin
    )
    $signature = @(137, 80, 78, 71, 13, 10, 26, 10)
    Assert-Condition -Condition ($Bytes.Length -ge 24) -Message "$Origin is too short to be a PNG."
    for ($index = 0; $index -lt $signature.Length; $index++) {
        Assert-Condition -Condition ([int]$Bytes[$index] -eq $signature[$index]) -Message "$Origin does not have a PNG signature."
    }
    Assert-Condition -Condition ([System.Text.Encoding]::ASCII.GetString($Bytes, 12, 4) -eq 'IHDR') -Message "$Origin does not have a PNG IHDR header."
    $width = ([int]$Bytes[16] -shl 24) -bor ([int]$Bytes[17] -shl 16) -bor ([int]$Bytes[18] -shl 8) -bor [int]$Bytes[19]
    $height = ([int]$Bytes[20] -shl 24) -bor ([int]$Bytes[21] -shl 16) -bor ([int]$Bytes[22] -shl 8) -bor [int]$Bytes[23]
    Assert-Condition -Condition ($width -eq $ExpectedWidth -and $height -eq $ExpectedHeight) -Message "$Origin has ${width}x${height}; expected ${ExpectedWidth}x${ExpectedHeight}."
}

function Get-ZipEntryBytes {
    param(
        [Parameter(Mandatory = $true)]$Archive,
        [Parameter(Mandatory = $true)][string]$Name
    )
    $entry = $Archive.GetEntry($Name)
    if ($null -eq $entry) {
        throw "Packaged JAR is missing $Name."
    }
    $stream = $entry.Open()
    $memory = [System.IO.MemoryStream]::new()
    try {
        $stream.CopyTo($memory)
        return $memory.ToArray()
    }
    finally {
        $memory.Dispose()
        $stream.Dispose()
    }
}

function Get-ZipEntryText {
    param(
        [Parameter(Mandatory = $true)]$Archive,
        [Parameter(Mandatory = $true)][string]$Name
    )
    return [System.Text.Encoding]::UTF8.GetString((Get-ZipEntryBytes -Archive $Archive -Name $Name))
}

$laneRoot = Split-Path -Parent $PSScriptRoot
$resourcesRoot = Join-Path $laneRoot 'src\main\resources'
$blockSourcePath = Join-Path $laneRoot 'src\main\java\buildcraft\neo\neoforge1201\factory\FactoryTankBlock.java'
$clientEventsPath = Join-Path $laneRoot 'src\main\java\buildcraft\neo\neoforge1201\factory\client\FactoryTankClientEvents.java'
$screenPath = Join-Path $laneRoot 'src\main\java\buildcraft\neo\neoforge1201\factory\client\TankScreen.java'
$blockModelPath = Join-Path $resourcesRoot 'assets\buildcraftfactory\models\block\tank.json'
$joinedModelPath = Join-Path $resourcesRoot 'assets\buildcraftfactory\models\block\tank_joined_below.json'
$itemModelPath = Join-Path $resourcesRoot 'assets\buildcraftfactory\models\item\tank.json'
$textureDimensions = [ordered]@{
    'assets/buildcraftfactory/textures/blocks/tank/end.png' = @(16, 16)
    'assets/buildcraftfactory/textures/blocks/tank/side.png' = @(16, 16)
    'assets/buildcraftfactory/textures/blocks/tank/side_joined_below.png' = @(16, 16)
    'assets/buildcraftfactory/textures/gui/tank.png' = @(256, 256)
}

foreach ($path in @($blockSourcePath, $clientEventsPath, $screenPath, $blockModelPath, $joinedModelPath, $itemModelPath)) {
    Assert-Condition -Condition (Test-Path -LiteralPath $path -PathType Leaf) -Message "Required Tank visual source is missing: $path"
}

$blockSource = Get-Content -LiteralPath $blockSourcePath -Raw
Assert-LiteralContains -Text $blockSource -Expected 'return RenderShape.MODEL;' -Message 'Factory Tank must render a baked model rather than BaseEntityBlock invisibly.'
$clientEvents = Get-Content -LiteralPath $clientEventsPath -Raw
Assert-LiteralContains -Text $clientEvents -Expected 'RenderType.cutout()' -Message 'Factory Tank must use the cutout render layer for its legacy alpha-cutout textures.'
Assert-Condition -Condition (-not $clientEvents.Contains('RenderType.translucent()')) -Message 'Factory Tank must not retain the translucent placeholder render layer.'
$screen = Get-Content -LiteralPath $screenPath -Raw
Assert-LiteralContains -Text $screen -Expected '"textures/gui/tank.png"' -Message 'Factory Tank screen must reference the restored GUI texture.'
Assert-LiteralContains -Text $screen -Expected 'private static final int OVERLAY_U = 176;' -Message 'Factory Tank screen must retain the legacy gauge-overlay sprite origin.'
Assert-LiteralContains -Text $screen -Expected 'graphics.blit(TEXTURE' -Message 'Factory Tank screen must blit its background and gauge overlay.'

$blockModel = Get-Content -LiteralPath $blockModelPath -Raw | ConvertFrom-Json
Assert-Condition -Condition ($blockModel.textures.side -eq 'buildcraftfactory:blocks/tank/side') -Message 'Tank model must use the legacy side texture.'
Assert-Condition -Condition ($blockModel.textures.up -eq 'buildcraftfactory:blocks/tank/end' -and $blockModel.textures.down -eq 'buildcraftfactory:blocks/tank/end') -Message 'Tank model must use the legacy end texture.'
$joinedModel = Get-Content -LiteralPath $joinedModelPath -Raw | ConvertFrom-Json
Assert-Condition -Condition ($joinedModel.parent -eq 'buildcraftfactory:block/tank') -Message 'Joined Tank model must inherit the base Tank model.'
Assert-Condition -Condition ($joinedModel.textures.side -eq 'buildcraftfactory:blocks/tank/side_joined_below') -Message 'Joined Tank model must use the legacy joined-side texture.'
$itemModel = Get-Content -LiteralPath $itemModelPath -Raw | ConvertFrom-Json
$expectedTransforms = [ordered]@{
    gui = @{ rotation = @(30, 225, 0); translation = @(0, 0, 0); scale = @(0.625, 0.625, 0.625) }
    ground = @{ rotation = @(0, 0, 0); translation = @(0, 3, 0); scale = @(0.25, 0.25, 0.25) }
    fixed = @{ rotation = @(0, 0, 0); translation = @(0, 0, 0); scale = @(0.5, 0.5, 0.5) }
    thirdperson_righthand = @{ rotation = @(75, 45, 0); translation = @(0, 2.5, 0); scale = @(0.375, 0.375, 0.375) }
    firstperson_righthand = @{ rotation = @(0, 45, 0); translation = @(0, 0, 0); scale = @(0.4, 0.4, 0.4) }
    firstperson_lefthand = @{ rotation = @(0, 225, 0); translation = @(0, 0, 0); scale = @(0.4, 0.4, 0.4) }
}
foreach ($transformName in $expectedTransforms.Keys) {
    $transform = $itemModel.display.PSObject.Properties[$transformName].Value
    Assert-Condition -Condition ($null -ne $transform) -Message "Tank item model is missing the legacy $transformName transform."
    foreach ($field in @('rotation', 'translation', 'scale')) {
        $actual = $transform.PSObject.Properties[$field].Value
        Assert-Sequence -Actual $actual -Expected $expectedTransforms[$transformName][$field] -Message "Tank item model $transformName.$field drifted."
    }
}

foreach ($relativePath in $textureDimensions.Keys) {
    $sourcePath = Join-Path $resourcesRoot $relativePath
    Assert-Condition -Condition (Test-Path -LiteralPath $sourcePath -PathType Leaf) -Message "Tank texture is missing: $sourcePath"
    $dimensions = $textureDimensions[$relativePath]
    Assert-PngDimensions -Bytes ([System.IO.File]::ReadAllBytes($sourcePath)) -ExpectedWidth $dimensions[0] -ExpectedHeight $dimensions[1] -Origin $sourcePath
}

$resolvedJar = (Resolve-Path -LiteralPath $JarPath).Path
Assert-Condition -Condition ([IO.Path]::GetExtension($resolvedJar) -eq '.jar') -Message "Expected a JAR file: $resolvedJar"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($resolvedJar)
try {
    $entryNames = @($archive.Entries | ForEach-Object FullName)
    $requiredEntries = @(
        'META-INF/mods.toml',
        'assets/buildcraftfactory/blockstates/tank.json',
        'assets/buildcraftfactory/models/block/tank.json',
        'assets/buildcraftfactory/models/block/tank_joined_below.json',
        'assets/buildcraftfactory/models/item/tank.json',
        'assets/buildcraftfactory/lang/en_us.json',
        'data/buildcraftfactory/loot_tables/blocks/tank.json'
    ) + @($textureDimensions.Keys)
    foreach ($entryName in $requiredEntries) {
        Assert-Condition -Condition ($entryNames -contains $entryName) -Message "Required packaged Tank visual resource is missing: $entryName"
    }
    $backupEntries = @($entryNames | Where-Object { $_ -match '\.codex-.*-backup$' })
    Assert-Condition -Condition ($backupEntries.Count -eq 0) -Message "Packaged JAR contains temporary recovery files: $($backupEntries -join ', ')"
    $jsonEntries = @($entryNames | Where-Object { $_.EndsWith('.json') })
    foreach ($entryName in $jsonEntries) {
        Get-ZipEntryText -Archive $archive -Name $entryName | ConvertFrom-Json -ErrorAction Stop | Out-Null
    }
    foreach ($relativePath in $textureDimensions.Keys) {
        $dimensions = $textureDimensions[$relativePath]
        Assert-PngDimensions -Bytes (Get-ZipEntryBytes -Archive $archive -Name $relativePath) -ExpectedWidth $dimensions[0] -ExpectedHeight $dimensions[1] -Origin "Packaged JAR entry $relativePath"
    }
    Write-Output "NeoForge 1.20.1 Tank visual validation passed: $($archive.Entries.Count) JAR entries, $($jsonEntries.Count) JSON files parsed, $($textureDimensions.Count) Tank PNG files verified."
}
finally {
    $archive.Dispose()
}
