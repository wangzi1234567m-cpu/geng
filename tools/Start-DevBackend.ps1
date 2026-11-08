[CmdletBinding()]
param(
    [ValidateSet('bootstrap','local')][string]$Profile = 'bootstrap',
    [ValidateRange(1,65535)][int]$Port = 8080
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot/Enter-DevEnvironment.ps1"
if ($Profile -eq 'local') {
    . "$PSScriptRoot/Read-LocalSettings.ps1"
    $settings = Read-GengLocalSettings $projectRoot
    $env:GENG_DB_URL = $settings.GENG_DB_URL
    $env:GENG_DB_PASSWORD = $settings.GENG_DB_PASSWORD
}
$jar = Join-Path $projectRoot 'backend\bootstrap\target\geng-backend-0.1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Build the backend first: ./tools/Invoke-Backend.ps1 verify' }
try {
    & java -jar $jar "--spring.profiles.active=$Profile" "--server.port=$Port"
    if ($LASTEXITCODE -ne 0) { throw 'Backend startup failed.' }
} finally {
    Remove-Item Env:GENG_DB_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:GENG_DB_URL -ErrorAction SilentlyContinue
}
