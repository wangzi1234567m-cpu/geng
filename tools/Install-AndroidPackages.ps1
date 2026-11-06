$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/Enter-DevEnvironment.ps1"
Write-Host 'Install SDK packages before starting Gradle. Accept the Google SDK license if requested.'
& sdkmanager "--sdk_root=$env:ANDROID_HOME" 'platform-tools' 'platforms;android-36' 'build-tools;35.0.0'
if ($LASTEXITCODE -ne 0) { throw 'SDK package installation did not complete.' }
