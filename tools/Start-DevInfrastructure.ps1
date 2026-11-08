$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
& "$PSScriptRoot/Initialize-DevEnvironment.ps1"
& docker info --format '{{.ServerVersion}}' 2>$null
if ($LASTEXITCODE -ne 0) { throw 'Docker engine is unavailable. Complete Windows virtualization setup and start Docker Desktop first.' }
& docker compose --env-file "$projectRoot/.dev/local.env" -f "$projectRoot/deploy/compose.dev.yml" up -d --wait postgres
if ($LASTEXITCODE -ne 0) { throw 'Development database failed to start.' }
