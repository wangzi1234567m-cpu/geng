$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot/Read-LocalSettings.ps1"
$settings = Read-GengLocalSettings $projectRoot
try {
    $env:GENG_DB_URL = $settings.GENG_DB_URL
    $env:GENG_MIGRATION_PASSWORD = $settings.GENG_MIGRATION_PASSWORD
    & "$PSScriptRoot/Invoke-Backend.ps1" -MavenArguments @('-N', 'flyway:migrate')
} finally {
    Remove-Item Env:GENG_MIGRATION_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:GENG_DB_URL -ErrorAction SilentlyContinue
}
