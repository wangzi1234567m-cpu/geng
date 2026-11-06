$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$archive = Join-Path $projectRoot '.tools/downloads/postgresql-17.11-3-windows-x64-binaries.zip'
$destination = Join-Path $projectRoot '.tools/postgresql-17.11'
$expected = '4b8db0930c38f6ef845db919551dedda3b6b845aeb0927b3d79a6e8e9e4537cf'
New-Item -ItemType Directory -Force -Path (Split-Path $archive -Parent) | Out-Null
if (-not (Test-Path -LiteralPath $archive)) {
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest 'https://get.enterprisedb.com/postgresql/postgresql-17.11-3-windows-x64-binaries.zip' -OutFile "$archive.partial" -TimeoutSec 1200
    Move-Item -LiteralPath "$archive.partial" -Destination $archive
}
if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'PostgreSQL archive SHA256 mismatch.' }
if (-not (Test-Path -LiteralPath "$destination/pgsql/bin/pg_ctl.exe")) {
    # Exclude pgAdmin and documentation; retain the server runtime and extensions.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($archive)
    try {
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName -notmatch '^pgsql/(bin|lib|share)/' -or $entry.FullName.EndsWith('/')) { continue }
            $target = [IO.Path]::GetFullPath((Join-Path $destination $entry.FullName))
            $safeRoot = [IO.Path]::GetFullPath($destination) + [IO.Path]::DirectorySeparatorChar
            if (-not $target.StartsWith($safeRoot, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe archive entry.' }
            New-Item -ItemType Directory -Force -Path (Split-Path $target -Parent) | Out-Null
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
        }
    } finally { $zip.Dispose() }
}
& "$destination/pgsql/bin/pg_ctl.exe" --version
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL runtime check failed.' }
Write-Host 'Verified PostgreSQL installed inside .tools; no Windows service installed.'
