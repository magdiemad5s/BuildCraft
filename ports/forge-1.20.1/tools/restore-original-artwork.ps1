[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string] $LegacyJar,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string] $LegacySourceRoot,

    [string] $AdditionalArtworkRoot,

    [string] $DestinationResources = (Join-Path $PSScriptRoot '..\src\main\resources'),

    [string] $JsonReportPath = (Join-Path $PSScriptRoot '..\ORIGINAL_ARTWORK_RESTORE_REPORT.json')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-Sha256 {
    param([Parameter(Mandatory = $true)][byte[]] $Bytes)

    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        return [BitConverter]::ToString($sha.ComputeHash($Bytes)).Replace('-', '').ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
}

function Convert-ToSafeAssetPath {
    param(
        [Parameter(Mandatory = $true)][string] $Path,
        [Parameter(Mandatory = $true)][string] $SourceLabel
    )

    if ($Path.Contains('\')) {
        throw "$SourceLabel contains a backslash instead of an archive separator: $Path"
    }
    if ($Path.StartsWith('/') -or $Path.Contains(':') -or $Path.Contains([char]0)) {
        throw "$SourceLabel is absolute or otherwise unsafe: $Path"
    }

    $segments = @($Path.Split('/'))
    if ($segments.Count -lt 3 -or $segments[0] -ne 'assets') {
        throw "$SourceLabel is not a namespaced asset path: $Path"
    }
    foreach ($segment in $segments) {
        if ([string]::IsNullOrWhiteSpace($segment) -or $segment -eq '.' -or $segment -eq '..' -or
            $segment.EndsWith('.') -or $segment.EndsWith(' ') -or
            $segment.IndexOfAny([IO.Path]::GetInvalidFileNameChars()) -ge 0) {
            throw "$SourceLabel contains an unsafe path segment: $Path"
        }
    }

    return $Path.ToLowerInvariant()
}

function Get-SafeTargetPath {
    param(
        [Parameter(Mandatory = $true)][string] $Root,
        [Parameter(Mandatory = $true)][string] $AssetPath
    )

    $relative = $AssetPath.Replace('/', [IO.Path]::DirectorySeparatorChar)
    $target = [IO.Path]::GetFullPath((Join-Path $Root $relative))
    $rootPrefix = $Root.TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    if (-not $target.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Resolved target escaped the resource root: $AssetPath"
    }
    return $target
}

function Read-ZipEntryBytes {
    param([Parameter(Mandatory = $true)] $Entry)

    $stream = $Entry.Open()
    try {
        $memory = [IO.MemoryStream]::new()
        try {
            $stream.CopyTo($memory)
            return $memory.ToArray()
        } finally {
            $memory.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
}

function Write-OriginalBytes {
    param(
        [Parameter(Mandatory = $true)][string] $Target,
        [Parameter(Mandatory = $true)][byte[]] $Bytes,
        [Parameter(Mandatory = $true)][bool] $Overwrite
    )

    $directory = [IO.Path]::GetDirectoryName($Target)
    if (-not [IO.Directory]::Exists($directory)) {
        $null = [IO.Directory]::CreateDirectory($directory)
    }

    if ([IO.File]::Exists($Target) -and -not $Overwrite) {
        return 'preserved-existing'
    }
    if ([IO.File]::Exists($Target)) {
        $existingHash = Get-Sha256 ([IO.File]::ReadAllBytes($Target))
        $incomingHash = Get-Sha256 $Bytes
        if ($existingHash -eq $incomingHash) {
            return 'unchanged'
        }
        [IO.File]::WriteAllBytes($Target, $Bytes)
        return 'replaced-with-released-original'
    }

    [IO.File]::WriteAllBytes($Target, $Bytes)
    return 'created'
}

$resolvedJar = (Resolve-Path -LiteralPath $LegacyJar).Path
if (-not [IO.Path]::GetExtension($resolvedJar).Equals('.jar', [StringComparison]::OrdinalIgnoreCase)) {
    throw "LegacyJar must be a .jar file: $resolvedJar"
}
$resolvedSourceRoot = (Resolve-Path -LiteralPath $LegacySourceRoot).Path.TrimEnd([IO.Path]::DirectorySeparatorChar)
$resolvedResources = (Resolve-Path -LiteralPath $DestinationResources).Path.TrimEnd([IO.Path]::DirectorySeparatorChar)
$resolvedAdditional = $null
if (-not [string]::IsNullOrWhiteSpace($AdditionalArtworkRoot)) {
    $resolvedAdditional = (Resolve-Path -LiteralPath $AdditionalArtworkRoot).Path.TrimEnd([IO.Path]::DirectorySeparatorChar)
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($resolvedJar)
$jarRecords = [Collections.Generic.List[object]]::new()
$sourceRecords = [Collections.Generic.List[object]]::new()
$additionalRecords = [Collections.Generic.List[object]]::new()
$jarByTarget = [Collections.Generic.Dictionary[string, object]]::new([StringComparer]::Ordinal)

try {
    foreach ($entry in $archive.Entries) {
        if ([string]::IsNullOrEmpty($entry.Name)) {
            continue
        }
        $rawPath = [string]$entry.FullName
        if ($rawPath -notmatch '(?i)^assets/[^/]+/.+\.png(?:\.mcmeta)?$') {
            continue
        }

        $targetPath = Convert-ToSafeAssetPath $rawPath 'Legacy JAR entry'
        $bytes = Read-ZipEntryBytes $entry
        $hash = Get-Sha256 $bytes
        if ($jarByTarget.ContainsKey($targetPath)) {
            $existing = $jarByTarget[$targetPath]
            if ($existing.sha256 -ne $hash) {
                throw "Case-normalized JAR collision has different bytes: $rawPath and $($existing.originalPath)"
            }
            continue
        }

        $record = [pscustomobject]@{
            originalPath = $rawPath
            targetPath = $targetPath
            byteLength = $bytes.Length
            sha256 = $hash
            action = $null
            entry = $entry
        }
        $jarByTarget.Add($targetPath, $record)
        $jarRecords.Add($record)
    }

    foreach ($record in @($jarRecords | Sort-Object targetPath)) {
        $bytes = Read-ZipEntryBytes $record.entry
        $target = Get-SafeTargetPath $resolvedResources $record.targetPath
        $record.action = Write-OriginalBytes $target $bytes $true
    }
} finally {
    $archive.Dispose()
}

$sourceAssets = Join-Path $resolvedSourceRoot 'assets'
if (-not (Test-Path -LiteralPath $sourceAssets -PathType Container)) {
    throw "Legacy source root has no assets directory: $resolvedSourceRoot"
}
foreach ($file in Get-ChildItem -LiteralPath $sourceAssets -Recurse -File | Sort-Object FullName) {
    if ($file.Name -notmatch '(?i)\.png(?:\.mcmeta)?$') {
        continue
    }
    $rawRelative = $file.FullName.Substring($resolvedSourceRoot.Length + 1)
    $archiveStylePath = $rawRelative.Replace([IO.Path]::DirectorySeparatorChar, '/')
    $targetPath = Convert-ToSafeAssetPath $archiveStylePath 'Legacy source asset'
    $bytes = [IO.File]::ReadAllBytes($file.FullName)
    $hash = Get-Sha256 $bytes
    $target = Get-SafeTargetPath $resolvedResources $targetPath

    if ($jarByTarget.ContainsKey($targetPath)) {
        $action = 'released-jar-preferred'
    } else {
        $action = Write-OriginalBytes $target $bytes $false
    }
    $sourceRecords.Add([pscustomobject]@{
        originalPath = $archiveStylePath
        targetPath = $targetPath
        byteLength = $bytes.Length
        sha256 = $hash
        action = $action
    })
}

if ($null -ne $resolvedAdditional) {
    foreach ($file in Get-ChildItem -LiteralPath $resolvedAdditional -Recurse -File | Sort-Object FullName) {
        if ($file.Name -notmatch '(?i)\.png(?:\.mcmeta)?$') {
            continue
        }
        $relative = $file.FullName.Substring($resolvedAdditional.Length + 1)
        $archiveRelative = $relative.Replace([IO.Path]::DirectorySeparatorChar, '/').ToLowerInvariant()
        if ($archiveRelative.StartsWith('/') -or $archiveRelative -match '(^|/)\.\.(/|$)') {
            throw "Additional artwork path is unsafe: $relative"
        }
        $targetPath = Convert-ToSafeAssetPath "assets/buildcraftneo/textures/legacy_unused/$archiveRelative" 'Additional artwork'
        $bytes = [IO.File]::ReadAllBytes($file.FullName)
        $target = Get-SafeTargetPath $resolvedResources $targetPath
        $action = Write-OriginalBytes $target $bytes $false
        $additionalRecords.Add([pscustomobject]@{
            originalPath = $relative.Replace([IO.Path]::DirectorySeparatorChar, '/')
            targetPath = $targetPath
            byteLength = $bytes.Length
            sha256 = Get-Sha256 $bytes
            action = $action
        })
    }
}

$jarHash = (Get-FileHash -LiteralPath $resolvedJar -Algorithm SHA256).Hash.ToLowerInvariant()
$report = [ordered]@{
    schemaVersion = 1
    generatedAtUtc = [DateTime]::UtcNow.ToString('o')
    project = 'BuildCraft Neo'
    target = 'Minecraft 1.20.1 / Forge 47.4.x'
    policy = [ordered]@{
        releasedJarPreferred = $true
        normalizedPaths = 'lowercase invariant'
        existingModernAliasesPreserved = $true
        archiveTraversalRejected = $true
        artworkRedrawn = $false
    }
    sources = [ordered]@{
        legacyJar = $resolvedJar
        legacyJarSha256 = $jarHash
        legacySourceRoot = $resolvedSourceRoot
        additionalArtworkRoot = $resolvedAdditional
    }
    counts = [ordered]@{
        releasedJarEntries = $jarRecords.Count
        releasedJarCreated = @($jarRecords | Where-Object action -eq 'created').Count
        releasedJarReplaced = @($jarRecords | Where-Object action -eq 'replaced-with-released-original').Count
        releasedJarUnchanged = @($jarRecords | Where-Object action -eq 'unchanged').Count
        legacySourceEntries = $sourceRecords.Count
        legacySourceOnlyCreated = @($sourceRecords | Where-Object action -eq 'created').Count
        legacySourceExistingPreserved = @($sourceRecords | Where-Object action -eq 'preserved-existing').Count
        legacySourceReleasedJarPreferred = @($sourceRecords | Where-Object action -eq 'released-jar-preferred').Count
        additionalArtworkEntries = $additionalRecords.Count
        additionalArtworkCreated = @($additionalRecords | Where-Object action -eq 'created').Count
        additionalArtworkExistingPreserved = @($additionalRecords | Where-Object action -eq 'preserved-existing').Count
    }
    releasedJarFiles = @($jarRecords | ForEach-Object {
        [ordered]@{
            originalPath = $_.originalPath
            targetPath = $_.targetPath
            byteLength = $_.byteLength
            sha256 = $_.sha256
            action = $_.action
        }
    })
    legacySourceFiles = @($sourceRecords)
    additionalArtworkFiles = @($additionalRecords)
}

$resolvedReport = [IO.Path]::GetFullPath($JsonReportPath)
$reportDirectory = [IO.Path]::GetDirectoryName($resolvedReport)
if (-not [IO.Directory]::Exists($reportDirectory)) {
    $null = [IO.Directory]::CreateDirectory($reportDirectory)
}
$json = $report | ConvertTo-Json -Depth 8
[IO.File]::WriteAllText($resolvedReport, $json + [Environment]::NewLine, [Text.UTF8Encoding]::new($false))

$report.counts | Format-List
Write-Host "Original artwork restore report: $resolvedReport"
