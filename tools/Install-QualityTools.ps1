$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$projectRoot = Split-Path $PSScriptRoot -Parent
$destination = Join-Path $projectRoot '.tools/quality'
New-Item -ItemType Directory -Force -Path $destination | Out-Null
function Get-VerifiedTool([string]$Url, [string]$Name, [string]$Sha256) {
    $target = Join-Path $destination $Name
    if (-not (Test-Path -LiteralPath $target)) { Invoke-WebRequest $Url -OutFile $target -TimeoutSec 600 }
    if ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant() -ne $Sha256) { throw "Tool checksum mismatch: $Name" }
    return $target
}
$gitleaks = Get-VerifiedTool 'https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/gitleaks_8.30.1_windows_x64.zip' 'gitleaks-8.30.1.zip' 'd29144deff3a68aa93ced33dddf84b7fdc26070add4aa0f4513094c8332afc4e'
Expand-Archive -LiteralPath $gitleaks -DestinationPath "$destination/gitleaks" -Force
Get-VerifiedTool 'https://github.com/google/osv-scanner/releases/download/v2.7.0/osv-scanner_windows_amd64.exe' 'osv-scanner.exe' '9964ed64ec9a387391eb4cc006ad5c531154821aef29b793e02c448de4686438' | Out-Null
$actionlint = Get-VerifiedTool 'https://github.com/rhysd/actionlint/releases/download/v1.7.12/actionlint_1.7.12_windows_amd64.zip' 'actionlint-1.7.12.zip' '6e7241b51e6817ea6a047693d8e6fed13b31819c9a0dd6c5a726e1592d22f6e9'
Expand-Archive -LiteralPath $actionlint -DestinationPath "$destination/actionlint" -Force
Write-Host 'Verified Gitleaks 8.30.1, OSV Scanner 2.7.0 and actionlint 1.7.12 installed locally.'
