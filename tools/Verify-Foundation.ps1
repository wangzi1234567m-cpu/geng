[CmdletBinding()]
param([switch]$Full)
$ErrorActionPreference = 'Stop'
if ($Full) {
    & "$PSScriptRoot/Start-LocalDatabase.ps1"
    & "$PSScriptRoot/Invoke-DatabaseMigration.ps1"
    & "$PSScriptRoot/Invoke-PostgresChecks.ps1"
} else {
    & "$PSScriptRoot/Invoke-Backend.ps1" -MavenArguments @('verify')
}
& "$PSScriptRoot/Invoke-Android.ps1" -GradleArguments @('testDebugUnitTest', 'assembleDebug', 'lintDebug')
if ($Full) { & "$PSScriptRoot/Invoke-QualityChecks.ps1" }
Write-Host "Foundation checks passed. Full local database/quality checks requested: $Full. Docker and remote CI were not implied."
