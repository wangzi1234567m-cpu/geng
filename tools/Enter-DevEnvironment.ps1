$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$localConfig = Join-Path $projectRoot 'dev.local.json'
if (-not (Test-Path -LiteralPath $localConfig)) { throw 'Run tools/Install-Toolchain.ps1 first.' }
$configuration = Get-Content -Raw -LiteralPath $localConfig | ConvertFrom-Json
$env:JAVA_HOME = $configuration.javaHome
$env:ANDROID_HOME = $configuration.androidSdkRoot
$env:ANDROID_SDK_ROOT = $configuration.androidSdkRoot
$env:GRADLE_USER_HOME = if ($configuration.gradleUserHome) { $configuration.gradleUserHome } else { Join-Path $projectRoot '.tools\gradle-drive\home' }
$env:MAVEN_USER_HOME = Join-Path $projectRoot '.tools\maven-user-home'
$env:ANDROID_USER_HOME = Join-Path $projectRoot '.tools\android-user-home'
$env:MAVEN_OPTS = '-Dfile.encoding=UTF-8'
$toolPaths = @("$env:JAVA_HOME\bin", "$($configuration.mavenHome)\bin", "$env:ANDROID_HOME\platform-tools", "$env:ANDROID_HOME\cmdline-tools\19.0\bin")
$env:Path = ($toolPaths -join ';') + ';' + $env:Path
New-Item -ItemType Directory -Force -Path $env:GRADLE_USER_HOME,$env:ANDROID_USER_HOME,$env:MAVEN_USER_HOME | Out-Null
$androidProject = Join-Path $projectRoot 'android'
if (Test-Path -LiteralPath $androidProject) {
    $sdkProperty = 'sdk.dir=' + $env:ANDROID_HOME.Replace('\','/').Replace(':','\:')
    [System.IO.File]::WriteAllText((Join-Path $androidProject 'local.properties'), $sdkProperty + "`n", [System.Text.UTF8Encoding]::new($false))
}
Write-Host 'Activated project-local JDK, Maven and Android SDK for this terminal.'
