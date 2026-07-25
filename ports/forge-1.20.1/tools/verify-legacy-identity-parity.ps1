[CmdletBinding()]
param(
    [string] $ManifestPath,
    [string] $SourceRoot,
    [string] $ReportPath,
    [switch] $FailOnMismatch
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$LaneRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ([string]::IsNullOrWhiteSpace($ManifestPath)) {
    $ManifestPath = Join-Path $LaneRoot '..\..\legacy-1.12.2-contract\LEGACY_IDENTITY_MANIFEST.json'
}
if ([string]::IsNullOrWhiteSpace($SourceRoot)) {
    $SourceRoot = Join-Path $LaneRoot 'src\main\java\buildcraft'
}
$ManifestPath = [System.IO.Path]::GetFullPath($ManifestPath)
$SourceRoot = [System.IO.Path]::GetFullPath($SourceRoot)
if (-not [System.IO.File]::Exists($ManifestPath)) { throw "Manifest not found: $ManifestPath" }
if (-not [System.IO.Directory]::Exists($SourceRoot)) { throw "Source root not found: $SourceRoot" }

function Remove-JavaComments([string] $Text) {
    $blocks = [regex]::Replace($Text, '(?s)/\*.*?\*/', {
        param($m)
        [regex]::Replace($m.Value, '[^\r\n]', ' ')
    })
    [regex]::Replace($blocks, '(?m)//[^\r\n]*', {
        param($m)
        ' ' * $m.Value.Length
    })
}

function Get-LineNumber([string] $Text, [int] $Index) {
    if ($Index -le 0) { return 1 }
    1 + [regex]::Matches($Text.Substring(0, $Index), '\r\n|\r|\n').Count
}

function Get-RelativePath([string] $Base, [string] $Path) {
    $prefix = $Base.TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
    if (-not $Path.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Path '$Path' is outside '$Base'"
    }
    $Path.Substring($prefix.Length).Replace('\', '/')
}
function Get-LaneRelativePath([string] $Path) {
    $baseUri = New-Object System.Uri($LaneRoot.TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar)
    $pathUri = New-Object System.Uri([System.IO.Path]::GetFullPath($Path))
    [System.Uri]::UnescapeDataString($baseUri.MakeRelativeUri($pathUri).ToString())
}

function Get-Namespace([string] $File) {
    $segment = (Get-RelativePath $SourceRoot $File).Split('/')[0]
    $map = @{
        lib='buildcraftlib'; core='buildcraftcore'; builders='buildcraftbuilders'
        energy='buildcraftenergy'; factory='buildcraftfactory'; silicon='buildcraftsilicon'
        transport='buildcrafttransport'; robotics='buildcraftrobotics'
    }
    if ($map.ContainsKey($segment)) { return $map[$segment] }
    $null
}

function Get-Kind([string] $Receiver) {
    if ($Receiver -match '(?:^|\.)ITEMS$') { return 'item' }
    if ($Receiver -match '(?:^|\.)(?:BLOCK_ENTITYS|BET)$') { return 'blockEntity' }
    if ($Receiver -match '(?:^|\.)BLOCKS$') { return 'block' }
    throw "Unknown registry receiver: $Receiver"
}

function Flatten-Ids($ByNamespace) {
    @($ByNamespace.PSObject.Properties | ForEach-Object { @($_.Value) } | Sort-Object -Unique)
}

function Get-MethodBody([string] $Source, [string] $Method) {
    $name = [regex]::Escape($Method)
    $match = [regex]::Match($Source, "(?ms)\b(?:public|protected|private)\s+static\s+[^;{}=]+?\b$name\s*\([^)]*\)\s*\{")
    if (-not $match.Success) { return $null }
    $open = $Source.IndexOf('{', $match.Index)
    $depth = 0
    $inString = $false
    $inChar = $false
    $escaped = $false
    for ($i = $open; $i -lt $Source.Length; $i++) {
        $c = $Source[$i]
        if ($escaped) { $escaped = $false; continue }
        if (($inString -or $inChar) -and $c -eq '\') { $escaped = $true; continue }
        if (-not $inChar -and $c -eq '"') { $inString = -not $inString; continue }
        if (-not $inString -and $c -eq "'") { $inChar = -not $inChar; continue }
        if ($inString -or $inChar) { continue }
        if ($c -eq '{') { $depth++ }
        elseif ($c -eq '}') {
            $depth--
            if ($depth -eq 0) { return $Source.Substring($open + 1, $i - $open - 1) }
        }
    }
    throw "Unbalanced method: $Method"
}

$manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json
if ($manifest.schema -ne 'buildcraft-neo/legacy-identity-contract/v1') {
    throw "Unsupported manifest schema: $($manifest.schema)"
}
$expected = [ordered]@{
    item = Flatten-Ids $manifest.registryContract.itemsByNamespace
    block = Flatten-Ids $manifest.registryContract.blocksByNamespace
    blockEntity = Flatten-Ids $manifest.registryContract.blockEntitiesByNamespace
}
$manifestErrors = @()
foreach ($kind in @('item','block','blockEntity')) {
    $declared = [int]$manifest.registryContract.countsByType.$kind
    if ($declared -ne @($expected[$kind]).Count) {
        $manifestErrors += "$kind count declares $declared but contains $(@($expected[$kind]).Count) unique IDs"
    }
}
$total = @($expected.item).Count + @($expected.block).Count + @($expected.blockEntity).Count
if ([int]$manifest.registryContract.declarationCount -ne $total) {
    $manifestErrors += "total declares $($manifest.registryContract.declarationCount) but contains $total unique IDs"
}

$sources = @()
$classIndex = @{}
Get-ChildItem -LiteralPath $SourceRoot -Recurse -File -Filter '*.java' | Sort-Object FullName | ForEach-Object {
    $namespace = Get-Namespace $_.FullName
    if ($null -ne $namespace) {
        $raw = [System.IO.File]::ReadAllText($_.FullName)
        $source = [pscustomobject]@{
            File=$_.FullName
            Relative=('src/main/java/buildcraft/' + (Get-RelativePath $SourceRoot $_.FullName))
            Namespace=$namespace
            Owner=$_.BaseName
            Clean=(Remove-JavaComments $raw)
        }
        $sources += $source
        if (-not $classIndex.ContainsKey($source.Owner)) { $classIndex[$source.Owner] = @() }
        $classIndex[$source.Owner] += $source
    }
}

$registrations = @()
$registrationKeys = @{}
function Add-Registration($Kind, $Namespace, $Path, $Field, $CreativeKey, $Source, $Index, $Discovery) {
    if ([string]::IsNullOrWhiteSpace($CreativeKey)) { $CreativeKey = $Field }
    $id = ($Namespace + ':' + $Path).ToLowerInvariant()
    $key = "$Kind|$id|$($Source.Relative)|$Field"
    if ($registrationKeys.ContainsKey($key)) { return }
    $registrationKeys[$key] = $true
    $script:registrations += [pscustomobject]@{
        kind=$Kind; id=$id; field=$Field; creativeKey=$CreativeKey; owner=$Source.Owner
        file=$Source.Relative; line=(Get-LineNumber $Source.Clean $Index); discovery=$Discovery
    }
}

$fieldPattern = '(?ms)\b(?:public|protected|private)\s+static\s+(?:final\s+)?RegistryObject\s*<[^;=]+>\s+(?<field>[A-Za-z_]\w*)\s*=\s*(?<receiver>(?:[A-Za-z_]\w*\.)?(?:ITEMS|BLOCKS|BLOCK_ENTITYS|BET))\s*\.register\s*\(\s*"(?<path>[^"]+)"'
$assignmentPattern = '(?m)^\s*(?<field>[A-Za-z_]\w*)\s*=\s*(?<receiver>(?:[A-Za-z_]\w*\.)?(?:ITEMS|BLOCKS|BLOCK_ENTITYS|BET))\s*\.register\s*\(\s*"(?<path>[^"]+)"'
foreach ($source in $sources) {
    foreach ($pattern in @($fieldPattern,$assignmentPattern)) {
        foreach ($m in [regex]::Matches($source.Clean, $pattern)) {
            Add-Registration (Get-Kind $m.Groups['receiver'].Value) $source.Namespace $m.Groups['path'].Value $m.Groups['field'].Value $null $source $m.Index 'literal'
        }
    }
}

# Resolve compatibility-sensitive registry paths held in public static final
# String constants without loading Forge or executing mod code.
$stringConstants = @{}
$stringConstantPattern = '(?m)\bpublic\s+static\s+final\s+String\s+(?<field>[A-Za-z_]\w*)\s*=\s*"(?<value>[^"]*)"\s*;'
foreach ($source in $sources) {
    foreach ($m in [regex]::Matches($source.Clean, $stringConstantPattern)) {
        $stringConstants["$($source.Owner).$($m.Groups['field'].Value)"] = $m.Groups['value'].Value
    }
}
$constantFieldPattern = '(?ms)\b(?:public|protected|private)\s+static\s+(?:final\s+)?RegistryObject\s*<[^;=]+>\s+(?<field>[A-Za-z_]\w*)\s*=\s*(?<receiver>(?:[A-Za-z_]\w*\.)?(?:ITEMS|BLOCKS|BLOCK_ENTITYS|BET))\s*\.register\s*\(\s*(?<constant>[A-Za-z_]\w*\.[A-Za-z_]\w*)\s*,'
foreach ($source in $sources) {
    foreach ($m in [regex]::Matches($source.Clean, $constantFieldPattern)) {
        $reference = $m.Groups['constant'].Value
        if ($stringConstants.ContainsKey($reference)) {
            Add-Registration (Get-Kind $m.Groups['receiver'].Value) $source.Namespace $stringConstants[$reference] $m.Groups['field'].Value $null $source $m.Index ('constant:' + $reference)
        }
    }
}
# Expand the port's generic enum registration helper without loading Forge.
$enumValues = @{
    DyeColor=@('white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black')
    EnumRedstoneChipset=@('red','iron','gold','quartz','diamond')
}
$enumPattern = '(?ms)\b(?<field>[A-Za-z_]\w*)\s*=\s*ItemByEnum\.creatItems\s*\(.*?(?<enum>DyeColor|EnumRedstoneChipset)\.values\s*\(\s*\)\s*,\s*\k<enum>\.class\s*,\s*"(?<prefix>[^"]+)"\s*,\s*(?:[A-Za-z_]\w*\.)?ITEMS\s*\)'
foreach ($source in $sources) {
    foreach ($m in [regex]::Matches($source.Clean, $enumPattern)) {
        foreach ($value in $enumValues[$m.Groups['enum'].Value]) {
            Add-Registration 'item' $source.Namespace ($m.Groups['prefix'].Value + '/' + $value) $m.Groups['field'].Value $m.Groups['field'].Value $source $m.Index ('ItemByEnum<' + $m.Groups['enum'].Value + '>')
        }
    }
}

# Resolve only pipe definitions that BCTransportItems actually instantiates.
$pipeDefs = @($sources | Where-Object { $_.Owner -eq 'BCTransportPipes' } | Select-Object -First 1)
$pipeItems = @($sources | Where-Object { $_.Owner -eq 'BCTransportItems' } | Select-Object -First 1)
if ($pipeDefs.Count -eq 1 -and $pipeItems.Count -eq 1) {
    $paths = @{}
    $defPattern = '(?ms)\b(?<field>[A-Za-z_]\w*)\s*=\s*builder\.idTex(?:Prefix)?\s*\(\s*"(?<path>[^"]+)"\s*\)[^;]*?\.define\s*\(\s*\)\s*;'
    foreach ($m in [regex]::Matches($pipeDefs[0].Clean, $defPattern)) { $paths[$m.Groups['field'].Value] = $m.Groups['path'].Value }
    $itemPattern = '(?m)\b(?<field>[A-Za-z_]\w*)\s*=\s*makePipeItem\s*\(\s*BCTransportPipes\.(?<definition>[A-Za-z_]\w*)\s*\)'
    foreach ($m in [regex]::Matches($pipeItems[0].Clean, $itemPattern)) {
        $definition = $m.Groups['definition'].Value
        if (-not $paths.ContainsKey($definition)) { throw "Unresolved pipe definition: $definition" }
        $definitionPath = $paths[$definition]
        # Pipe definitions are save-data identities, while their historical
        # item registry IDs use a pipe_ prefix and the cobble abbreviation.
        $itemPath = if ($definitionPath.StartsWith('cobblestone_')) {
            'pipe_cobble_' + $definitionPath.Substring('cobblestone_'.Length)
        } else {
            'pipe_' + $definitionPath
        }
        Add-Registration 'item' 'buildcrafttransport' $itemPath $m.Groups['field'].Value 'PIPE_MAP' $pipeItems[0] $m.Index ('PipeDefinition.' + $definition)
    }
}

$duplicates = @($registrations | Group-Object kind,id | Where-Object Count -gt 1 | ForEach-Object {
    [ordered]@{ kind=$_.Group[0].kind; id=$_.Group[0].id; evidence=@($_.Group | ForEach-Object { "$($_.file):$($_.line) [$($_.field)]" }) }
})
$actual = [ordered]@{}
$parity = [ordered]@{}
foreach ($kind in @('item','block','blockEntity')) {
    $actual[$kind] = @($registrations | Where-Object kind -eq $kind | ForEach-Object id | Sort-Object -Unique)
    $matching = @($expected[$kind] | Where-Object { $actual[$kind] -ccontains $_ } | Sort-Object)
    $missing = @($expected[$kind] | Where-Object { $actual[$kind] -cnotcontains $_ } | Sort-Object)
    $extra = @($actual[$kind] | Where-Object { $expected[$kind] -cnotcontains $_ } | Sort-Object)
    $parity[$kind] = [ordered]@{
        expectedCount=@($expected[$kind]).Count; actualCount=@($actual[$kind]).Count
        matchingCount=$matching.Count; missingCount=$missing.Count; extraCount=$extra.Count
        matching=$matching; missing=$missing; extra=$extra
    }
}

# Only methods wired through addItemProvider are accepted as creative exposure.
$providerRefs = @()
$providerPattern = '\.addItemProvider\s*\(\s*(?<owner>[A-Za-z_]\w*)::(?<method>[A-Za-z_]\w*)\s*\)'
foreach ($source in $sources) {
    foreach ($m in [regex]::Matches($source.Clean, $providerPattern)) {
        $providerRefs += [pscustomobject]@{ owner=$m.Groups['owner'].Value; method=$m.Groups['method'].Value; wiredFrom="$($source.Relative):$(Get-LineNumber $source.Clean $m.Index)" }
    }
}
$providers = @()
$unresolvedProviders = @()
foreach ($ref in $providerRefs) {
    $resolved = $false
    if ($classIndex.ContainsKey($ref.owner)) {
        foreach ($ownerSource in @($classIndex[$ref.owner])) {
            $body = Get-MethodBody $ownerSource.Clean $ref.method
            if ($null -ne $body) {
                $providers += [pscustomobject]@{ owner=$ref.owner; method=$ref.method; file=$ownerSource.Relative; wiredFrom=$ref.wiredFrom; body=$body }
                $resolved = $true
                break
            }
        }
    }
    if (-not $resolved) { $unresolvedProviders += $ref }
}
$creativeIds = @()
foreach ($reg in $registrations | Where-Object kind -eq 'item') {
    $key = '\b' + [regex]::Escape($reg.creativeKey) + '\b'
    $hits = @($providers | Where-Object { $_.owner -eq $reg.owner -and $_.body -match $key })
    if ($hits.Count -gt 0) { $creativeIds += $reg.id }
}
$creativeIds = @($creativeIds | Sort-Object -Unique)
$registeredLegacyItems = @($expected.item | Where-Object { $actual.item -ccontains $_ })
$registeredLegacyMissingCreative = @($registeredLegacyItems | Where-Object { $creativeIds -cnotcontains $_ } | Sort-Object)
$allModernMissingCreative = @($actual.item | Where-Object { $creativeIds -cnotcontains $_ } | Sort-Object)

$conditionalKeys = @($manifest.conditionalOrNonPublicDeclarations.declaredButNotNormalReleaseRegistrations | ForEach-Object { "$($_.registryType)|$($_.id)" })
$requiredMissing = [ordered]@{}
$conditionalMissing = [ordered]@{}
foreach ($kind in @('item','block','blockEntity')) {
    $requiredMissing[$kind] = @($parity[$kind].missing | Where-Object { $conditionalKeys -cnotcontains "$kind|$_" })
    $conditionalMissing[$kind] = @($parity[$kind].missing | Where-Object { $conditionalKeys -ccontains "$kind|$_" })
}

# Compare oldReg metadata triples and separately require a live Forge remap hook.
$aliasEvidence = @()
$tagPattern = '(?ms)registerTag\s*\(\s*"(?<tag>[^"]+)"\s*\)(?<chain>[^;]*?)\s*;'
foreach ($source in $sources) {
    foreach ($tag in [regex]::Matches($source.Clean, $tagPattern)) {
        $reg = [regex]::Match($tag.Groups['chain'].Value, '\.reg\s*\(\s*"(?<path>[^"]+)"\s*\)')
        if (-not $reg.Success) { continue }
        $type = if ($tag.Groups['tag'].Value.StartsWith('item.')) {'item'} elseif ($tag.Groups['tag'].Value.StartsWith('block.')) {'block'} elseif ($tag.Groups['tag'].Value.StartsWith('tile.')) {'blockEntity'} else {$null}
        if ($null -eq $type) { continue }
        foreach ($old in [regex]::Matches($tag.Groups['chain'].Value, '\.oldReg\s*\(\s*"(?<path>[^"]+)"\s*\)')) {
            $aliasEvidence += [pscustomobject]@{ registryType=$type; target=($source.Namespace + ':' + $reg.Groups['path'].Value); legacyPath=$old.Groups['path'].Value; file=$source.Relative }
        }
    }
}
# The modern Forge handler stores the same triples in typed alias tables rather than legacy registerTag chains.
$migrationTableTypes = [ordered]@{
    ITEM_ALIASES = 'item'
    BLOCK_ALIASES = 'block'
    BLOCK_ENTITY_ALIASES = 'blockEntity'
}
foreach ($source in $sources) {
    foreach ($tableName in $migrationTableTypes.Keys) {
        $tablePattern = '(?ms)\b' + [regex]::Escape($tableName) + '\s*=\s*aliases\s*\(\s*new\s+String\[\]\[\]\s*\{(?<body>.*?)\}\s*\)'
        $table = [regex]::Match($source.Clean, $tablePattern)
        if (-not $table.Success) { continue }
        foreach ($row in [regex]::Matches($table.Groups['body'].Value, '\{\s*"(?<legacy>[^"]+)"\s*,\s*"(?<target>[^"]+)"\s*\}')) {
            $aliasEvidence += [pscustomobject]@{
                registryType=$migrationTableTypes[$tableName]
                target=$row.Groups['target'].Value
                legacyPath=$row.Groups['legacy'].Value
                file=$source.Relative
            }
        }
    }
}
function Get-AliasMatchTarget($Alias) {
    $modernTarget = $Alias.PSObject.Properties['modernTarget']
    if ($null -ne $modernTarget -and -not [string]::IsNullOrWhiteSpace([string]$modernTarget.Value)) {
        return [string]$modernTarget.Value
    }
    [string]$Alias.target
}
function Get-AliasKey($Alias) { ("$($Alias.registryType)|$(Get-AliasMatchTarget $Alias)|$($Alias.legacyPath)").ToLowerInvariant() }
$expectedAliases = @(@($manifest.oldRegistryAliases.aliases) + @($manifest.additionalMigrationAliases))
$expectedAliasResolution = @($expectedAliases | ForEach-Object {
    $currentTarget = Get-AliasMatchTarget $_
    [ordered]@{
        registryType=[string]$_.registryType
        legacyPath=[string]$_.legacyPath
        historicalTarget=[string]$_.target
        currentTargetUsedForMatching=$currentTarget
        modernTargetOverrideApplied=($currentTarget -cne [string]$_.target)
    }
})
$expectedAliasKeys = @($expectedAliases | ForEach-Object { Get-AliasKey $_ } | Sort-Object -Unique)
$actualAliasKeys = @($aliasEvidence | ForEach-Object { Get-AliasKey $_ } | Sort-Object -Unique)
$missingAliases = @($expectedAliases | Where-Object { $actualAliasKeys -cnotcontains (Get-AliasKey $_) } | ForEach-Object {
    $currentTarget = Get-AliasMatchTarget $_
    [ordered]@{
        registryType=$_.registryType
        legacyPath=$_.legacyPath
        historicalTarget=$_.target
        currentTargetUsedForMatching=$currentTarget
        modernTargetOverrideApplied=($currentTarget -cne [string]$_.target)
    }
})
$allSource = ($sources | ForEach-Object Clean) -join "`n"
$missingMappingHandler = $allSource -notmatch '\bMissingMappingsEvent\b|\bMissingMapping\b'

$savedDataEvidence = @()
foreach ($entry in $manifest.persistence.savedData) {
    $hits = @()
    $needle = [regex]::Escape('"' + [string]$entry.name + '"')
    foreach ($source in $sources) {
        foreach ($m in [regex]::Matches($source.Clean, $needle)) { $hits += "$($source.Relative):$(Get-LineNumber $source.Clean $m.Index)" }
    }
    $savedDataEvidence += [pscustomobject]@{ name=[string]$entry.name; found=($hits.Count -gt 0); evidence=$hits }
}
$missingSavedData = @($savedDataEvidence | Where-Object found -eq $false | ForEach-Object name)

# Do not pretend dynamic calls are counted when their IDs cannot be resolved statically.
$unresolvedDynamic = @()
$dynamicPattern = '(?<receiver>(?:[A-Za-z_]\w*\.)?(?:ITEMS|BLOCKS|BLOCK_ENTITYS|BET))\s*\.register\s*\(\s*(?<argument>[^,\r\n)]+)'
foreach ($source in $sources) {
    foreach ($m in [regex]::Matches($source.Clean, $dynamicPattern)) {
        $arg = $m.Groups['argument'].Value.Trim()
        if ($arg.StartsWith('"') -or $arg -match '^(?:bus|b|m|modEventBus)$') { continue }
        if ($stringConstants.ContainsKey($arg)) { continue }
        if ($source.Owner -eq 'ItemByEnum') { continue }
        if ($source.Owner -eq 'BCTransportItems' -and ($arg -match '^def\.identifier' -or $arg -eq 'itemPath')) { continue }
        $unresolvedDynamic += [pscustomobject]@{ registry=(Get-Kind $m.Groups['receiver'].Value); argument=$arg; file=$source.Relative; line=(Get-LineNumber $source.Clean $m.Index) }
    }
}

$requiredMissingCount = @($requiredMissing.item).Count + @($requiredMissing.block).Count + @($requiredMissing.blockEntity).Count
$failureCount = $manifestErrors.Count + $duplicates.Count + $requiredMissingCount + $registeredLegacyMissingCreative.Count + $missingAliases.Count + $missingSavedData.Count + $unresolvedProviders.Count + $(if ($missingMappingHandler) {1} else {0})
$report = [ordered]@{
    schema='buildcraft-neo/legacy-identity-parity-report/v1'
    generatedAtUtc=[DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ')
    mode='static-source-read-only'
    inputs=[ordered]@{manifest=(Get-LaneRelativePath $ManifestPath); sourceRoot=(Get-LaneRelativePath $SourceRoot); javaFileCount=$sources.Count}
    manifestIntegrity=[ordered]@{
        declarationCount=[int]$manifest.registryContract.declarationCount
        itemCount=@($expected.item).Count; blockCount=@($expected.block).Count; blockEntityCount=@($expected.blockEntity).Count
        legacyAliasCount=@($manifest.oldRegistryAliases.aliases).Count; additionalAliasCount=@($manifest.additionalMigrationAliases).Count
        savedDataCount=@($manifest.persistence.savedData).Count; errors=$manifestErrors
    }
    modernSourceDiscovery=[ordered]@{
        declarationCount=@($actual.item).Count+@($actual.block).Count+@($actual.blockEntity).Count
        itemCount=@($actual.item).Count; blockCount=@($actual.block).Count; blockEntityCount=@($actual.blockEntity).Count
        registrationEvidenceCount=$registrations.Count; duplicateRegistrations=$duplicates
        unresolvedDynamicSites=$unresolvedDynamic
        countingRule='Literal RegistryObject fields plus ItemByEnum and instantiated PipeDefinition expansions; unresolved dynamic calls are listed, never guessed.'
    }
    registryParity=$parity
    releaseRequiredMissing=$requiredMissing
    legacyConditionalMissing=$conditionalMissing
    creativeTabParity=[ordered]@{
        wiredProviderCount=$providers.Count; unresolvedProviders=$unresolvedProviders
        registeredModernItemCount=@($actual.item).Count; exposedModernItemCount=$creativeIds.Count
        registeredLegacyItemCount=$registeredLegacyItems.Count
        registeredLegacyItemsMissingCreativeCount=$registeredLegacyMissingCreative.Count
        registeredLegacyItemsMissingCreative=$registeredLegacyMissingCreative
        allRegisteredModernItemsMissingCreativeCount=$allModernMissingCreative.Count
        allRegisteredModernItemsMissingCreative=$allModernMissingCreative
        wiredProviders=@($providers | ForEach-Object { [ordered]@{provider="$($_.owner)::$($_.method)"; source=$_.file; wiredFrom=$_.wiredFrom} })
    }
    migrationAliasParity=[ordered]@{
        expectedAliasCount=$expectedAliasKeys.Count; declaredAliasEvidenceCount=$actualAliasKeys.Count
        matchingAliasEvidenceCount=@($expectedAliasKeys | Where-Object { $actualAliasKeys -ccontains $_ }).Count
        missingAliasCount=$missingAliases.Count; missingAliases=$missingAliases
        modernTargetOverrideCount=@($expectedAliasResolution | Where-Object modernTargetOverrideApplied).Count
        expectedAliasResolution=$expectedAliasResolution
        missingMappingHandlerDetected=(-not $missingMappingHandler)
        qualification='Manifest target retains the audited 1.12.2 destination. When modernTarget is present, parity matching uses that current registered destination; expectedAliasResolution reports both. oldReg remains metadata evidence, and Forge MissingMappingsEvent handling is independently required.'
    }
    savedDataParity=[ordered]@{
        expectedCount=@($manifest.persistence.savedData).Count
        foundCount=@($savedDataEvidence | Where-Object found).Count
        missingCount=$missingSavedData.Count; missing=$missingSavedData; evidence=$savedDataEvidence
    }
    gate=[ordered]@{
        complete=($failureCount -eq 0); strictFailureCount=$failureCount
        strictFailureRules=@('manifest counts valid','no duplicate modern IDs','all non-conditional legacy IDs exact','registered legacy items exposed by wired creative providers','all aliases declared','Forge missing-mapping handler present','all SavedData names retained','all creative provider references resolve')
    }
}

$json = $report | ConvertTo-Json -Depth 20
if (-not [string]::IsNullOrWhiteSpace($ReportPath)) {
    $resolved = if ([System.IO.Path]::IsPathRooted($ReportPath)) {[System.IO.Path]::GetFullPath($ReportPath)} else {[System.IO.Path]::GetFullPath((Join-Path $LaneRoot $ReportPath))}
    $parent = [System.IO.Path]::GetDirectoryName($resolved)
    if (-not [System.IO.Directory]::Exists($parent)) { [void][System.IO.Directory]::CreateDirectory($parent) }
    [System.IO.File]::WriteAllText($resolved, $json + [Environment]::NewLine, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "Wrote report: $resolved"
}

Write-Host ''
Write-Host 'BuildCraft Neo legacy identity parity'
Write-Host "  Manifest: item=$(@($expected.item).Count), block=$(@($expected.block).Count), blockEntity=$(@($expected.blockEntity).Count)"
Write-Host "  Modern:   item=$(@($actual.item).Count), block=$(@($actual.block).Count), blockEntity=$(@($actual.blockEntity).Count)"
foreach ($kind in @('item','block','blockEntity')) {
    Write-Host "  $kind parity: matching=$($parity[$kind].matchingCount), missing=$($parity[$kind].missingCount), extra=$($parity[$kind].extraCount)"
}
Write-Host "  Required legacy IDs missing: $requiredMissingCount"
Write-Host "  Registered legacy items missing creative exposure: $($registeredLegacyMissingCreative.Count)"
Write-Host "  Alias evidence: $($actualAliasKeys.Count)/$($expectedAliasKeys.Count); missing-mapping handler=$(-not $missingMappingHandler)"
Write-Host "  SavedData names: $(@($savedDataEvidence | Where-Object found).Count)/$(@($manifest.persistence.savedData).Count)"
Write-Host "  Unresolved dynamic registry sites: $($unresolvedDynamic.Count)"
Write-Host "  Strict gate complete: $($report.gate.complete)"

if ($FailOnMismatch -and -not $report.gate.complete) {
    Write-Error "Legacy identity parity gate failed with $failureCount release-blocking findings."
    exit 1
}
$report