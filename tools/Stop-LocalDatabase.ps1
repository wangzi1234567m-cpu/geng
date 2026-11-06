$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot/Get-PostgresWorkspace.ps1"
$runtimeRoot = Get-GengPostgresWorkspace $projectRoot
$clusterRoot = Join-Path $runtimeRoot '.dev/postgres'
if (-not (Test-Path -LiteralPath "$clusterRoot/.geng-cluster")) { throw 'Refusing to manage an unmarked cluster.' }
& "$runtimeRoot/.tools/postgresql-17.11/pgsql/bin/pg_ctl.exe" -D "$clusterRoot/data" -w -m fast stop
if ($LASTEXITCODE -ne 0) { throw 'Local PostgreSQL shutdown failed.' }
if ($runtimeRoot -match '^([P-Z]):/$') { & subst.exe ($Matches[1] + ':') /D }
