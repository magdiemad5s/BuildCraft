[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string] $InputPath,

    [string] $ManifestPath = (Join-Path $PSScriptRoot '..\TEXTURE_PARITY_MANIFEST.json'),

    [string] $JsonReportPath,

    [switch] $WarningsAsErrors
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Convert-ToEntryPath {
    param([Parameter(Mandatory = $true)][string] $Path)
    return $Path.Replace([IO.Path]::DirectorySeparatorChar, '/').TrimStart('/')
}

function Add-Issue {
    param(
        [Parameter(Mandatory = $true)][ValidateSet('error', 'warning')][string] $Severity,
        [Parameter(Mandatory = $true)][string] $Code,
        [Parameter(Mandatory = $true)][string] $Path,
        [Parameter(Mandatory = $true)][string] $Message
    )

    $issue = [pscustomobject]@{
        severity = $Severity
        code = $Code
        path = $Path
        message = $Message
    }
    if ($Severity -eq 'error') {
        $script:errors.Add($issue)
    } else {
        $script:warnings.Add($issue)
    }
}

function Get-BigEndianUInt32 {
    param(
        [Parameter(Mandatory = $true)][byte[]] $Bytes,
        [Parameter(Mandatory = $true)][int] $Offset
    )

    return [uint32](
        ([uint64]$Bytes[$Offset] -shl 24) -bor
        ([uint64]$Bytes[$Offset + 1] -shl 16) -bor
        ([uint64]$Bytes[$Offset + 2] -shl 8) -bor
        [uint64]$Bytes[$Offset + 3]
    )
}

function Get-Sha256 {
    param([Parameter(Mandatory = $true)][byte[]] $Bytes)

    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        return [BitConverter]::ToString($sha.ComputeHash($Bytes)).Replace('-', '').ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
}

function Get-PngInfo {
    param(
        [Parameter(Mandatory = $true)][string] $Path,
        [Parameter(Mandatory = $true)][byte[]] $Bytes
    )

    $signature = [byte[]](137, 80, 78, 71, 13, 10, 26, 10)
    if ($Bytes.Length -lt 33) {
        Add-Issue error 'png.too_short' $Path "PNG is only $($Bytes.Length) bytes long."
        return $null
    }
    for ($index = 0; $index -lt $signature.Length; $index++) {
        if ($Bytes[$index] -ne $signature[$index]) {
            Add-Issue error 'png.bad_signature' $Path 'File does not have the PNG signature.'
            return $null
        }
    }

    $offset = 8
    $chunkIndex = 0
    $width = 0
    $height = 0
    $bitDepth = 0
    $colourType = 0
    $foundIend = $false

    while ($offset + 12 -le $Bytes.Length) {
        $length = Get-BigEndianUInt32 $Bytes $offset
        if ($length -gt [int]::MaxValue) {
            Add-Issue error 'png.chunk_too_large' $Path "PNG chunk at byte $offset is too large."
            return $null
        }
        $chunkLength = [int]$length
        $chunkEnd = $offset + 12 + $chunkLength
        if ($chunkEnd -gt $Bytes.Length) {
            Add-Issue error 'png.truncated_chunk' $Path "PNG chunk at byte $offset extends beyond the file."
            return $null
        }

        $chunkType = [Text.Encoding]::ASCII.GetString($Bytes, $offset + 4, 4)
        if ($chunkIndex -eq 0) {
            if ($chunkType -ne 'IHDR' -or $chunkLength -ne 13) {
                Add-Issue error 'png.bad_ihdr' $Path 'The first PNG chunk is not a 13-byte IHDR chunk.'
                return $null
            }
            $width = Get-BigEndianUInt32 $Bytes ($offset + 8)
            $height = Get-BigEndianUInt32 $Bytes ($offset + 12)
            $bitDepth = [int]$Bytes[$offset + 16]
            $colourType = [int]$Bytes[$offset + 17]
            if ($width -eq 0 -or $height -eq 0) {
                Add-Issue error 'png.zero_dimension' $Path "PNG dimensions are ${width}x${height}."
                return $null
            }
            if ($colourType -notin @(0, 2, 3, 4, 6)) {
                Add-Issue error 'png.bad_colour_type' $Path "PNG colour type $colourType is invalid."
            }
        }

        if ($chunkType -eq 'IEND') {
            if ($chunkLength -ne 0) {
                Add-Issue error 'png.bad_iend' $Path 'The PNG IEND chunk is not empty.'
            }
            $foundIend = $true
            $offset = $chunkEnd
            break
        }

        $offset = $chunkEnd
        $chunkIndex++
    }

    if (-not $foundIend) {
        Add-Issue error 'png.missing_iend' $Path 'PNG does not contain a complete IEND chunk.'
        return $null
    }
    if ($offset -ne $Bytes.Length) {
        Add-Issue warning 'png.trailing_data' $Path "PNG has $($Bytes.Length - $offset) byte(s) after IEND."
    }

    if ($script:canDecodePng) {
        $stream = [IO.MemoryStream]::new($Bytes, $false)
        try {
            $image = [Drawing.Image]::FromStream($stream, $false, $true)
            try {
                if ($image.Width -ne $width -or $image.Height -ne $height) {
                    Add-Issue error 'png.decoder_dimension_mismatch' $Path (
                        "IHDR reports ${width}x${height}, but the image decoder reports " +
                        "$($image.Width)x$($image.Height)."
                    )
                }
            } finally {
                $image.Dispose()
            }
        } catch {
            Add-Issue error 'png.decode_failed' $Path "System.Drawing could not decode the PNG: $($_.Exception.Message)"
        } finally {
            $stream.Dispose()
        }
    }

    return [pscustomobject]@{
        width = [uint32]$width
        height = [uint32]$height
        bitDepth = $bitDepth
        colourType = $colourType
        byteLength = $Bytes.Length
    }
}

function Add-RecursivePropertyValues {
    param(
        [AllowNull()] $Node,
        [Parameter(Mandatory = $true)][string] $PropertyName,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][Collections.Generic.List[string]] $Output
    )

    if ($null -eq $Node -or $Node -is [string]) {
        return
    }
    if ($Node -is [Collections.IDictionary]) {
        foreach ($key in $Node.Keys) {
            $value = $Node[$key]
            if ([string]$key -eq $PropertyName -and $value -is [string]) {
                $Output.Add($value)
            }
            Add-RecursivePropertyValues $value $PropertyName $Output
        }
        return
    }
    if ($Node -is [Collections.IEnumerable] -and $Node -isnot [pscustomobject]) {
        foreach ($value in $Node) {
            Add-RecursivePropertyValues $value $PropertyName $Output
        }
        return
    }

    foreach ($property in $Node.PSObject.Properties) {
        if ($property.Name -eq $PropertyName -and $property.Value -is [string]) {
            $Output.Add($property.Value)
        }
        Add-RecursivePropertyValues $property.Value $PropertyName $Output
    }
}

function Split-ResourceLocation {
    param(
        [Parameter(Mandatory = $true)][string] $Reference,
        [Parameter(Mandatory = $true)][string] $DefaultNamespace
    )

    $separator = $Reference.IndexOf(':')
    if ($separator -ge 0) {
        return [pscustomobject]@{
            namespace = $Reference.Substring(0, $separator)
            path = $Reference.Substring($separator + 1)
        }
    }
    return [pscustomobject]@{
        namespace = $DefaultNamespace
        path = $Reference
    }
}

function Test-TextureReference {
    param(
        [Parameter(Mandatory = $true)][string] $Owner,
        [Parameter(Mandatory = $true)][string] $DefaultNamespace,
        [Parameter(Mandatory = $true)][string] $Reference
    )

    if ([string]::IsNullOrWhiteSpace($Reference) -or $Reference.StartsWith('#')) {
        return
    }
    $location = Split-ResourceLocation $Reference $DefaultNamespace
    if ($script:externalNamespaces.Contains([string]$location.namespace)) {
        $script:externalTextureReferences++
        return
    }
    $expected = "assets/$($location.namespace)/textures/$($location.path).png"
    if (-not $script:entries.ContainsKey($expected)) {
        Add-Issue error 'texture_reference.missing' $Owner (
            "Texture '$Reference' does not resolve to packaged entry '$expected'."
        )
    } else {
        $script:resolvedTextureReferences++
    }
}

function Test-ModelReference {
    param(
        [Parameter(Mandatory = $true)][string] $Owner,
        [Parameter(Mandatory = $true)][string] $DefaultNamespace,
        [Parameter(Mandatory = $true)][string] $Reference
    )

    if ([string]::IsNullOrWhiteSpace($Reference) -or $Reference.StartsWith('builtin/')) {
        return
    }
    $location = Split-ResourceLocation $Reference $DefaultNamespace
    if ($script:externalNamespaces.Contains([string]$location.namespace)) {
        $script:externalModelReferences++
        return
    }
    $expected = "assets/$($location.namespace)/models/$($location.path).json"
    if (-not $script:entries.ContainsKey($expected)) {
        Add-Issue error 'model_reference.missing' $Owner (
            "Model '$Reference' does not resolve to packaged entry '$expected'."
        )
    } else {
        $script:resolvedModelReferences++
    }
}

function Test-JsonMember {
    param(
        [AllowNull()] $Node,
        [Parameter(Mandatory = $true)][string] $Name
    )

    if ($null -eq $Node) {
        return $false
    }
    if ($Node -is [Collections.IDictionary]) {
        return $Node.ContainsKey($Name)
    }
    return $null -ne $Node.PSObject.Properties[$Name]
}

function Get-JsonMember {
    param(
        [AllowNull()] $Node,
        [Parameter(Mandatory = $true)][string] $Name
    )

    if (-not (Test-JsonMember $Node $Name)) {
        return $null
    }
    if ($Node -is [Collections.IDictionary]) {
        $dictionary = [Collections.IDictionary]$Node
        return $dictionary[$Name]
    }
    return $Node.PSObject.Properties[$Name].Value
}

function Get-JsonObjectValues {
    param([AllowNull()] $Node)

    if ($null -eq $Node) {
        return @()
    }
    if ($Node -is [Collections.IDictionary]) {
        return @($Node.Values)
    }
    return @($Node.PSObject.Properties | ForEach-Object { $_.Value })
}
$resolvedInput = (Resolve-Path -LiteralPath $InputPath).Path
$resolvedManifest = (Resolve-Path -LiteralPath $ManifestPath).Path
$manifest = Get-Content -LiteralPath $resolvedManifest -Raw | ConvertFrom-Json

$script:externalNamespaces = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach ($externalNamespace in @($manifest.externalResourceNamespaces)) {
    $null = $script:externalNamespaces.Add([string]$externalNamespace)
}

$script:errors = [Collections.Generic.List[object]]::new()
$script:warnings = [Collections.Generic.List[object]]::new()
$script:entries = [Collections.Generic.Dictionary[string, object]]::new([StringComparer]::Ordinal)
$script:jsonCache = [Collections.Generic.Dictionary[string, object]]::new([StringComparer]::Ordinal)
$script:invalidJsonPaths = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$script:pngInfo = [Collections.Generic.Dictionary[string, object]]::new([StringComparer]::Ordinal)
$script:resolvedTextureReferences = 0
$script:externalTextureReferences = 0
$script:resolvedModelReferences = 0
$script:externalModelReferences = 0
$archive = $null
$inputKind = 'directory'

try {
    Add-Type -AssemblyName System.Web.Extensions -ErrorAction Stop
    $script:jsonSerializer = [Web.Script.Serialization.JavaScriptSerializer]::new()
    $script:jsonSerializer.MaxJsonLength = [int]::MaxValue
    $script:jsonSerializer.RecursionLimit = 256
} catch {
    $script:jsonSerializer = $null
}

try {
    Add-Type -AssemblyName System.Drawing -ErrorAction Stop
    $script:canDecodePng = $true
} catch {
    $script:canDecodePng = $false
    Add-Issue warning 'png.decoder_unavailable' '<runtime>' (
        'System.Drawing is unavailable; structural PNG checks still run, but decoder validation is skipped.'
    )
}

try {
    if (Test-Path -LiteralPath $resolvedInput -PathType Container) {
        $root = $resolvedInput.TrimEnd([IO.Path]::DirectorySeparatorChar)
        foreach ($file in Get-ChildItem -LiteralPath $root -Recurse -File) {
            $entryPath = Convert-ToEntryPath $file.FullName.Substring($root.Length + 1)
            if ($script:entries.ContainsKey($entryPath)) {
                Add-Issue error 'entry.duplicate' $entryPath 'Input contains a duplicate resource path.'
            } else {
                $script:entries.Add($entryPath, $file)
            }
        }
    } elseif ([IO.Path]::GetExtension($resolvedInput).Equals('.jar', [StringComparison]::OrdinalIgnoreCase)) {
        $inputKind = 'jar'
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $archive = [IO.Compression.ZipFile]::OpenRead($resolvedInput)
        foreach ($entry in $archive.Entries) {
            if ([string]::IsNullOrEmpty($entry.Name)) {
                continue
            }
            $entryPath = Convert-ToEntryPath $entry.FullName
            if ($script:entries.ContainsKey($entryPath)) {
                Add-Issue error 'entry.duplicate' $entryPath 'JAR contains a duplicate resource path.'
            } else {
                $script:entries.Add($entryPath, $entry)
            }
        }
    } else {
        throw "InputPath must be a resource directory or a .jar file: $resolvedInput"
    }

    function Get-EntryBytes {
        param([Parameter(Mandatory = $true)][string] $EntryPath)

        $entry = $script:entries[$EntryPath]
        if ($script:inputKindForReaders -eq 'directory') {
            return [IO.File]::ReadAllBytes($entry.FullName)
        }
        $stream = $entry.Open()
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

    function Get-EntryJson {
        param([Parameter(Mandatory = $true)][string] $EntryPath)

        if ($script:jsonCache.ContainsKey($EntryPath)) {
            return $script:jsonCache[$EntryPath]
        }
        if ($script:invalidJsonPaths.Contains($EntryPath)) {
            return $null
        }
        try {
            $utf8 = [Text.UTF8Encoding]::new($false, $true)
            $text = $utf8.GetString((Get-EntryBytes $EntryPath))
            if ($null -ne $script:jsonSerializer) {
                $parsed = $script:jsonSerializer.DeserializeObject($text)
            } else {
                $parsed = $text | ConvertFrom-Json
            }
            $script:jsonCache.Add($EntryPath, $parsed)
            return $parsed
        } catch {
            $null = $script:invalidJsonPaths.Add($EntryPath)
            Add-Issue error 'json.invalid' $EntryPath "JSON could not be parsed: $($_.Exception.Message)"
            return $null
        }
    }

    $script:inputKindForReaders = $inputKind

    $pngPaths = @($script:entries.Keys | Where-Object { $_ -match '(?i)\.png$' } | Sort-Object)
    foreach ($pngPath in $pngPaths) {
        $info = Get-PngInfo $pngPath (Get-EntryBytes $pngPath)
        if ($null -ne $info) {
            $script:pngInfo.Add($pngPath, $info)
        }
    }

    $jsonPaths = @(
        $script:entries.Keys |
            Where-Object { $_ -match '^assets/.+\.json$' -or $_ -match '\.png\.mcmeta$' } |
            Sort-Object
    )
    foreach ($jsonPath in $jsonPaths) {
        $null = Get-EntryJson $jsonPath
    }

    $animationPaths = @($script:entries.Keys | Where-Object { $_ -match '\.png\.mcmeta$' } | Sort-Object)
    $animationDefinitionCount = 0
    foreach ($animationPath in $animationPaths) {
        $pngPath = $animationPath.Substring(0, $animationPath.Length - '.mcmeta'.Length)
        if (-not $script:entries.ContainsKey($pngPath)) {
            Add-Issue error 'animation.missing_png' $animationPath "PNG metadata has no matching '$pngPath'."
            continue
        }
        $metadata = Get-EntryJson $animationPath
        if ($null -eq $metadata -or -not (Test-JsonMember $metadata 'animation')) {
            continue
        }
        $animationDefinitionCount++
        if (-not $script:pngInfo.ContainsKey($pngPath)) {
            continue
        }

        $animation = Get-JsonMember $metadata 'animation'
        $image = $script:pngInfo[$pngPath]
        $frameWidth = [int]$image.width
        $frameHeight = [int]$image.width
        if (Test-JsonMember $animation 'width') {
            $frameWidth = [int](Get-JsonMember $animation 'width')
        }
        if (Test-JsonMember $animation 'height') {
            $frameHeight = [int](Get-JsonMember $animation 'height')
        }
        if ($frameWidth -le 0 -or $frameHeight -le 0) {
            Add-Issue error 'animation.bad_frame_size' $animationPath (
                "Animation frame dimensions are ${frameWidth}x${frameHeight}."
            )
            continue
        }
        if (($image.width % $frameWidth) -ne 0 -or ($image.height % $frameHeight) -ne 0) {
            Add-Issue error 'animation.non_integral_frames' $animationPath (
                "Image $($image.width)x$($image.height) is not divisible into " +
                "${frameWidth}x${frameHeight} animation frames."
            )
            continue
        }

        $frameCount = [int](($image.width / $frameWidth) * ($image.height / $frameHeight))
        if (Test-JsonMember $animation 'frames') {
            foreach ($frame in @(Get-JsonMember $animation 'frames')) {
                $frameIndex = $frame
                if ($frame -isnot [ValueType] -and (Test-JsonMember $frame 'index')) {
                    $frameIndex = Get-JsonMember $frame 'index'
                }
                if ($frameIndex -isnot [ValueType] -or [int]$frameIndex -lt 0 -or [int]$frameIndex -ge $frameCount) {
                    Add-Issue error 'animation.frame_out_of_range' $animationPath (
                        "Animation frame index '$frameIndex' is outside 0..$($frameCount - 1)."
                    )
                }
            }
        }
    }

    $modelPaths = @($script:entries.Keys | Where-Object { $_ -match '^assets/[^/]+/models/.+\.json$' })
    foreach ($modelPath in $modelPaths) {
        $model = Get-EntryJson $modelPath
        if ($null -eq $model) {
            continue
        }
        if (Test-JsonMember $model 'textures') {
            foreach ($texture in @(Get-JsonObjectValues (Get-JsonMember $model 'textures'))) {
                if ($texture -is [string]) {
                    Test-TextureReference $modelPath 'minecraft' $texture
                }
            }
        }
        if ((Test-JsonMember $model 'parent') -and (Get-JsonMember $model 'parent') -is [string]) {
            Test-ModelReference $modelPath 'minecraft' (Get-JsonMember $model 'parent')
        }
        if (Test-JsonMember $model 'overrides') {
            foreach ($override in @(Get-JsonMember $model 'overrides')) {
                if ((Test-JsonMember $override 'model') -and (Get-JsonMember $override 'model') -is [string]) {
                    Test-ModelReference $modelPath 'minecraft' (Get-JsonMember $override 'model')
                }
            }
        }
    }

    $blockstatePaths = @(
        $script:entries.Keys | Where-Object { $_ -match '^assets/[^/]+/blockstates/.+\.json$' }
    )
    foreach ($blockstatePath in $blockstatePaths) {
        $parts = $blockstatePath.Split('/')
        $namespace = $parts[1]
        $blockstate = Get-EntryJson $blockstatePath
        if ($null -eq $blockstate) {
            continue
        }
        $modelReferences = [Collections.Generic.List[string]]::new()
        Add-RecursivePropertyValues $blockstate 'model' $modelReferences
        foreach ($modelReference in $modelReferences) {
            Test-ModelReference $blockstatePath 'minecraft' $modelReference
        }
    }

    $atlasPaths = @($script:entries.Keys | Where-Object { $_ -match '^assets/[^/]+/atlases/.+\.json$' })
    $atlasSourceCount = 0
    foreach ($atlasPath in $atlasPaths) {
        $atlas = Get-EntryJson $atlasPath
        if ($null -eq $atlas -or -not (Test-JsonMember $atlas 'sources')) {
            continue
        }
        foreach ($source in @(Get-JsonMember $atlas 'sources')) {
            $atlasSourceCount++
            $sourceType = Get-JsonMember $source 'type'
            if ($sourceType -eq 'minecraft:single' -and
                (Test-JsonMember $source 'resource') -and
                (Get-JsonMember $source 'resource') -is [string]) {
                Test-TextureReference $atlasPath 'minecraft' (Get-JsonMember $source 'resource')
            } elseif ($sourceType -eq 'minecraft:directory') {
                if (-not (Test-JsonMember $source 'source') -or
                    (Get-JsonMember $source 'source') -isnot [string]) {
                    Add-Issue error 'atlas.bad_directory_source' $atlasPath (
                        'Directory atlas source is missing its source path.'
                    )
                }
            } elseif ($sourceType -notin @('minecraft:filter', 'minecraft:paletted_permutations')) {
                Add-Issue warning 'atlas.unchecked_source_type' $atlasPath (
                    "Atlas source type '$sourceType' was not reference-checked."
                )
            }
        }
    }

    $countsByNamespace = [ordered]@{}
    $guiCountsByNamespace = [ordered]@{}
    foreach ($pngPath in $pngPaths) {
        if ($pngPath -notmatch '^assets/([^/]+)/') {
            continue
        }
        $namespace = $matches[1]
        if (-not $countsByNamespace.Contains($namespace)) {
            $countsByNamespace[$namespace] = 0
        }
        $countsByNamespace[$namespace]++
        if ($pngPath -match '^assets/[^/]+/textures/gui/') {
            if (-not $guiCountsByNamespace.Contains($namespace)) {
                $guiCountsByNamespace[$namespace] = 0
            }
            $guiCountsByNamespace[$namespace]++
        }
    }

    foreach ($property in $manifest.expectedPackagedMinimums.pngByNamespace.PSObject.Properties) {
        $actual = 0
        if ($countsByNamespace.Contains($property.Name)) {
            $actual = [int]$countsByNamespace[$property.Name]
        }
        if ($actual -lt [int]$property.Value) {
            Add-Issue error 'parity.namespace_png_count' "assets/$($property.Name)" (
                "Expected at least $($property.Value) PNGs, found $actual."
            )
        }
    }
    foreach ($property in $manifest.expectedPackagedMinimums.guiPngByNamespace.PSObject.Properties) {
        $actual = 0
        if ($guiCountsByNamespace.Contains($property.Name)) {
            $actual = [int]$guiCountsByNamespace[$property.Name]
        }
        if ($actual -lt [int]$property.Value) {
            Add-Issue error 'parity.namespace_gui_count' "assets/$($property.Name)/textures/gui" (
                "Expected at least $($property.Value) GUI PNGs, found $actual."
            )
        }
    }
    if ($animationPaths.Count -lt [int]$manifest.expectedPackagedMinimums.animationMetadata) {
        Add-Issue error 'parity.animation_count' 'assets' (
            "Expected at least $($manifest.expectedPackagedMinimums.animationMetadata) animation metadata files, " +
            "found $($animationPaths.Count)."
        )
    }
    if ($atlasSourceCount -lt [int]$manifest.expectedPackagedMinimums.atlasSources) {
        Add-Issue error 'parity.atlas_source_count' 'assets/minecraft/atlases' (
            "Expected at least $($manifest.expectedPackagedMinimums.atlasSources) atlas sources, " +
            "found $atlasSourceCount."
        )
    }

    foreach ($requiredGui in @($manifest.requiredGuiTextures)) {
        if (-not $script:entries.ContainsKey([string]$requiredGui)) {
            Add-Issue error 'parity.required_gui_missing' ([string]$requiredGui) (
                'Required BuildCraft GUI texture is not packaged.'
            )
        }
    }

    foreach ($mapping in @($manifest.deliberateMappings)) {
        $target = [string]$mapping.target
        if (-not $script:entries.ContainsKey($target)) {
            Add-Issue error 'parity.mapping_target_missing' $target (
                "Target for legacy mapping '$($mapping.legacy)' is not packaged."
            )
            continue
        }
        if ($null -ne $mapping.PSObject.Properties['sha256']) {
            $actualHash = Get-Sha256 (Get-EntryBytes $target)
            if ($actualHash -ne ([string]$mapping.sha256).ToLowerInvariant()) {
                Add-Issue error 'parity.mapping_hash_changed' $target (
                    "Expected original-art SHA-256 $($mapping.sha256), found $actualHash."
                )
            }
        }
        if ($null -ne $mapping.PSObject.Properties['sourcePackaged'] -and
            [bool]$mapping.sourcePackaged) {
            $legacy = [string]$mapping.legacy
            if (-not $script:entries.ContainsKey($legacy)) {
                Add-Issue error 'parity.mapping_source_missing' $legacy (
                    "Mapping requires both source and target copies, but '$legacy' is absent."
                )
            } else {
                $legacyHash = Get-Sha256 (Get-EntryBytes $legacy)
                $targetHash = Get-Sha256 (Get-EntryBytes $target)
                if ($legacyHash -ne $targetHash) {
                    Add-Issue error 'parity.mapping_copies_differ' $target (
                        "Compatibility copy differs from source '$legacy'."
                    )
                }
            }
        }
    }

    $result = 'PASS'
    if ($script:errors.Count -gt 0 -or ($WarningsAsErrors -and $script:warnings.Count -gt 0)) {
        $result = 'FAIL'
    }

    $report = [pscustomobject]@{
        schemaVersion = 1
        result = $result
        input = $resolvedInput
        inputKind = $inputKind
        manifest = $resolvedManifest
        pngDecoderValidation = $script:canDecodePng
        counts = [pscustomobject]@{
            entries = $script:entries.Count
            png = $pngPaths.Count
            validPng = $script:pngInfo.Count
            guiPng = @($pngPaths | Where-Object { $_ -match '^assets/[^/]+/textures/gui/' }).Count
            animationMetadata = $animationPaths.Count
            models = $modelPaths.Count
            blockstates = $blockstatePaths.Count
            atlasSources = $atlasSourceCount
            resolvedTextureReferences = $script:resolvedTextureReferences
            externalTextureReferences = $script:externalTextureReferences
            resolvedModelReferences = $script:resolvedModelReferences
            externalModelReferences = $script:externalModelReferences
            errors = $script:errors.Count
            warnings = $script:warnings.Count
        }
        pngByNamespace = [pscustomobject]$countsByNamespace
        guiPngByNamespace = [pscustomobject]$guiCountsByNamespace
        errors = @($script:errors)
        warnings = @($script:warnings)
    }

    if (-not [string]::IsNullOrWhiteSpace($JsonReportPath)) {
        $parent = Split-Path -Parent $JsonReportPath
        if (-not [string]::IsNullOrWhiteSpace($parent) -and -not (Test-Path -LiteralPath $parent)) {
            throw "JsonReportPath parent directory does not exist: $parent"
        }
        $report | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $JsonReportPath -Encoding UTF8
    }

    $report | Format-List result, input, inputKind, pngDecoderValidation
    $report.counts | Format-List
    if ($script:warnings.Count -gt 0) {
        Write-Host 'Warnings:'
        $script:warnings | Format-Table -AutoSize severity, code, path, message
    }
    if ($script:errors.Count -gt 0) {
        Write-Host 'Errors:'
        $script:errors | Format-Table -AutoSize severity, code, path, message
    }

    if ($result -ne 'PASS') {
        exit 1
    }
} finally {
    if ($null -ne $archive) {
        $archive.Dispose()
    }
}
