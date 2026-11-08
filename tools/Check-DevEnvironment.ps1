[CmdletBinding()]
param([switch]$IncludeDocker)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot/Enter-DevEnvironment.ps1"
$configuration = Get-Content -Raw -LiteralPath "$projectRoot/dev.local.json" | ConvertFrom-Json
$javaVersion = (& java -version 2>&1 | Select-Object -First 1).ToString()
$sdkFolders = @(Get-ChildItem -LiteralPath "$env:ANDROID_HOME/platforms" -Directory -ErrorAction SilentlyContinue | Where-Object { (Test-Path -LiteralPath (Join-Path $_.FullName 'android.jar')) -and (Test-Path -LiteralPath (Join-Path $_.FullName 'source.properties')) } | Select-Object -ExpandProperty Name)
$dockerAvailable = $null
if ($IncludeDocker) {
    & docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
    $dockerAvailable = $LASTEXITCODE -eq 0
}
$checks = [ordered]@{
    java = $javaVersion
    mavenInstalled = (Test-Path -LiteralPath "$($configuration.mavenHome)/bin/mvn.cmd")
    androidPlatforms = $sdkFolders
    androidBuildTools = (Test-Path -LiteralPath "$env:ANDROID_HOME/build-tools/35.0.0/aapt2.exe")
    adbInstalled = (Test-Path -LiteralPath "$env:ANDROID_HOME/platform-tools/adb.exe")
    mavenWrapper = (Test-Path -LiteralPath "$projectRoot/backend/mvnw.cmd")
    gradleWrapper = (Test-Path -LiteralPath "$projectRoot/android/gradlew.bat")
    dockerEngineAvailable = $dockerAvailable
    dockerRequiredForFoundation = $false
    localSecretsPresent = (Test-Path -LiteralPath "$projectRoot/.dev/local.env")
    backendJarPresent = (Test-Path -LiteralPath "$projectRoot/backend/bootstrap/target/geng-backend-0.1.0-SNAPSHOT.jar")
    androidApkPresent = (Test-Path -LiteralPath "$projectRoot/android/app/build/outputs/apk/debug/app-debug.apk")
}
$checks | ConvertTo-Json -Depth 4
if ($IncludeDocker -and -not $dockerAvailable) { Write-Warning 'Docker engine unavailable; PostgreSQL/container verification is pending. Foundation development does not require it.' }
