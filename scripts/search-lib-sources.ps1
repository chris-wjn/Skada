# Skada: Search Workspace and Library Sources
param(
    [string]$Pattern,
    [string]$FileName,
    [string]$WorkspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")),
    [switch]$CaseSensitive,
    [int]$MaxJars = 500,
    [string]$GradleCache = (Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1")
)

function Write-Match {
    param(
        [string]$SourceType, # Workspace | Jar
        [string]$Container,  # Folder path or JAR path
        [string]$EntryPath,  # File path or entry inside jar
        [int]$LineNumber,
        [string]$LineText
    )
    $prefix = "[$SourceType]"
    if ($LineNumber -gt 0) {
        Write-Output ("{0} {1} :: {2} :: {3}: {4}" -f $prefix, $Container, $EntryPath, $LineNumber, $LineText)
    } else {
        Write-Output ("{0} {1} :: {2}" -f $prefix, $Container, $EntryPath)
    }
}

function Get-IsTextMatch {
    param(
        [string]$Text,
        [string]$Pattern,
        [switch]$CaseSensitive
    )
    if (-not $Pattern) { return $false }
    $options = [System.Text.RegularExpressions.RegexOptions]::Multiline
    if (-not $CaseSensitive) { $options = $options -bor [System.Text.RegularExpressions.RegexOptions]::IgnoreCase }
    return [System.Text.RegularExpressions.Regex]::IsMatch($Text, $Pattern, $options)
}

function Get-LinesMatches {
    param(
        [string]$Text,
        [string]$Pattern,
        [switch]$CaseSensitive
    )
    if (-not $Pattern) { return @() }
    $options = [System.Text.RegularExpressions.RegexOptions]::Multiline
    if (-not $CaseSensitive) { $options = $options -bor [System.Text.RegularExpressions.RegexOptions]::IgnoreCase }
    $lines = $Text -split "`r?`n"
    $matches = @()
    for ($i = 0; $i -lt $lines.Length; $i++) {
        if ([System.Text.RegularExpressions.Regex]::IsMatch($lines[$i], $Pattern, $options)) {
            $matches += @{ line = $i + 1; text = $lines[$i] }
        }
    }
    return $matches
}

function Search-Workspace {
    param(
        [string]$Root,
        [string]$Pattern,
        [string]$FileName,
        [switch]$CaseSensitive
    )
    $includeExt = @('*.java','*.kt','*.groovy','*.gradle','*.xml','*.json','*.md','*.properties','*.yml','*.yaml','*.txt')
    $files = Get-ChildItem -Path $Root -Recurse -File -Include $includeExt -ErrorAction SilentlyContinue
    foreach ($f in $files) {
        if ($FileName) {
            $nameMatch = if ($CaseSensitive) { $f.Name.Contains($FileName) } else { $f.Name.ToLower().Contains($FileName.ToLower()) }
            if ($nameMatch) { Write-Match -SourceType "Workspace" -Container $f.DirectoryName -EntryPath $f.Name -LineNumber 0 -LineText "" }
        }
        if ($Pattern) {
            try {
                $content = Get-Content -Path $f.FullName -Raw -ErrorAction Stop
            } catch {
                continue
            }
            $lineMatches = Get-LinesMatches -Text $content -Pattern $Pattern -CaseSensitive:$CaseSensitive
            foreach ($m in $lineMatches) {
                Write-Match -SourceType "Workspace" -Container $f.DirectoryName -EntryPath $f.Name -LineNumber $m.line -LineText $m.text
            }
        }
    }
}

function Search-JarSources {
    param(
        [string]$CacheRoot,
        [string]$Pattern,
        [string]$FileName,
        [switch]$CaseSensitive,
        [int]$MaxJars
    )
    if (-not (Test-Path $CacheRoot)) { return }
    $jarPaths = Get-ChildItem -Path $CacheRoot -Recurse -File -Filter *sources.jar -ErrorAction SilentlyContinue | Select-Object -First $MaxJars
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    foreach ($jar in $jarPaths) {
        try {
            $zip = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
        } catch {
            continue
        }
        foreach ($entry in $zip.Entries) {
            $ext = [System.IO.Path]::GetExtension($entry.FullName).ToLower()
            if (@('.java','.kt','.scala','.groovy','.xml','.json','.md','.properties','.yml','.yaml','.txt') -notcontains $ext) { continue }
            if ($FileName) {
                $nameOnly = [System.IO.Path]::GetFileName($entry.FullName)
                $nameMatch = if ($CaseSensitive) { $nameOnly.Contains($FileName) } else { $nameOnly.ToLower().Contains($FileName.ToLower()) }
                if ($nameMatch) { Write-Match -SourceType "Jar" -Container $jar.FullName -EntryPath $entry.FullName -LineNumber 0 -LineText "" }
            }
            if ($Pattern) {
                try {
                    $stream = $entry.Open()
                    $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8, $true)
                    $text = $reader.ReadToEnd()
                    $reader.Dispose(); $stream.Dispose()
                } catch {
                    continue
                }
                $lineMatches = Get-LinesMatches -Text $text -Pattern $Pattern -CaseSensitive:$CaseSensitive
                foreach ($m in $lineMatches) {
                    Write-Match -SourceType "Jar" -Container $jar.FullName -EntryPath $entry.FullName -LineNumber $m.line -LineText $m.text
                }
            }
        }
        $zip.Dispose()
    }
}

Write-Output ("Searching workspace: {0}" -f $WorkspaceRoot)
Search-Workspace -Root $WorkspaceRoot -Pattern $Pattern -FileName $FileName -CaseSensitive:$CaseSensitive

Write-Output ("Searching Gradle sources jars under: {0} (max {1})" -f $GradleCache, $MaxJars)
Search-JarSources -CacheRoot $GradleCache -Pattern $Pattern -FileName $FileName -CaseSensitive:$CaseSensitive -MaxJars $MaxJars

Write-Output "Done."
