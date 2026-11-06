[CmdletBinding()]
param(
    [string]$AndroidSdkRoot = "$env:LOCALAPPDATA\Android\Sdk"
)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$projectRoot = Split-Path $PSScriptRoot -Parent
$toolRoot = Join-Path $projectRoot '.tools'
$downloads = Join-Path $toolRoot 'downloads'
New-Item -ItemType Directory -Force -Path $downloads | Out-Null

function Get-VerifiedArchive([string]$Url, [string]$Destination, [string]$Hash, [string]$Algorithm) {
    if (-not (Test-Path -LiteralPath $Destination)) {
        Write-Host "Downloading $(Split-Path $Destination -Leaf)"
        Invoke-WebRequest -Uri $Url -OutFile "$Destination.partial" -TimeoutSec 600
        Move-Item -LiteralPath "$Destination.partial" -Destination $Destination -Force
    }
    $actual = (Get-FileHash -LiteralPath $Destination -Algorithm $Algorithm).Hash.ToLowerInvariant()
    if ($actual -ne $Hash.ToLowerInvariant()) { throw "Checksum mismatch: $Destination" }
}

$jdkFolder = Join-Path $toolRoot 'jdk-21.0.13+9'
if (-not (Test-Path -LiteralPath "$jdkFolder\bin\java.exe")) {
    $jdkArchive = Join-Path $downloads 'temurin-21.0.13_9-windows-x64.zip'
    Get-VerifiedArchive 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip' $jdkArchive 'a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9' 'SHA256'
    Expand-Archive -LiteralPath $jdkArchive -DestinationPath $toolRoot -Force
}

$mavenVersion = '3.9.16'
$mavenFolder = Join-Path $toolRoot "apache-maven-$mavenVersion"
if (-not (Test-Path -LiteralPath "$mavenFolder\bin\mvn.cmd")) {
    $mavenBase = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip"
    $mavenHash = ((Invoke-WebRequest -Uri "$mavenBase.sha512" -TimeoutSec 60).Content.Trim() -split '\s+')[0]
    $mavenArchive = Join-Path $downloads "apache-maven-$mavenVersion-bin.zip"
    Get-VerifiedArchive $mavenBase $mavenArchive $mavenHash 'SHA512'
    Expand-Archive -LiteralPath $mavenArchive -DestinationPath $toolRoot -Force
}

$sdkManager = Join-Path $AndroidSdkRoot 'cmdline-tools\19.0\bin\sdkmanager.bat'
if (-not (Test-Path -LiteralPath $sdkManager)) {
    $sdkArchive = Join-Path $downloads 'commandlinetools-win-13114758.zip'
    Get-VerifiedArchive 'https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip' $sdkArchive '54a582f3bf73e04253602f2d1c80bd5868aac115' 'SHA1'
    $sdkStaging = Join-Path $toolRoot 'android-commandline-staging-19'
    Expand-Archive -LiteralPath $sdkArchive -DestinationPath $sdkStaging -Force
    New-Item -ItemType Directory -Force -Path "$AndroidSdkRoot\cmdline-tools\19.0" | Out-Null
    Copy-Item -LiteralPath "$sdkStaging\cmdline-tools\bin","$sdkStaging\cmdline-tools\lib","$sdkStaging\cmdline-tools\NOTICE.txt","$sdkStaging\cmdline-tools\source.properties" -Destination "$AndroidSdkRoot\cmdline-tools\19.0" -Recurse -Force
}

$configuration = @{javaHome=$jdkFolder; mavenHome=$mavenFolder; androidSdkRoot=$AndroidSdkRoot; gradleUserHome=(Join-Path $projectRoot '.tools\gradle-drive\home')}
$configuration | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $projectRoot 'dev.local.json') -Encoding utf8
Write-Host 'Toolchain installed. Activate this terminal with: . ./tools/Enter-DevEnvironment.ps1'
Write-Host 'Android SDK licenses and packages are configured separately using sdkmanager.'
