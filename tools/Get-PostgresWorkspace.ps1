function Get-GengPostgresWorkspace([string]$ProjectRoot) {
    if ($ProjectRoot -notmatch '[^\x00-\x7F]') { return $ProjectRoot }
    $marker = Join-Path $ProjectRoot '.dev/postgres/.workspace-path'
    New-Item -ItemType Directory -Force -Path (Split-Path $marker -Parent) | Out-Null
    if (-not (Test-Path -LiteralPath $marker)) {
        [IO.File]::WriteAllText($marker, $ProjectRoot, [Text.UTF8Encoding]::new($false))
    }
    foreach ($letter in [char[]](80..90)) {
        $drive = "${letter}:"
        $mappedMarker = "$drive/.dev/postgres/.workspace-path"
        if (Test-Path -LiteralPath "$drive/") {
            if ((Test-Path -LiteralPath $mappedMarker) -and [IO.File]::ReadAllText($mappedMarker) -eq $ProjectRoot) { return "$drive/" }
            continue
        }
        & subst.exe $drive $ProjectRoot
        if ($LASTEXITCODE -ne 0) { throw 'Cannot create PostgreSQL workspace alias.' }
        Write-Host "Using $drive as a workspace alias while local PostgreSQL runs; Stop-LocalDatabase removes it."
        return "$drive/"
    }
    throw 'No free ASCII drive alias available for PostgreSQL.'
}
