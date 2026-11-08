[CmdletBinding()]
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$MavenArguments = @('verify'))
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot/Enter-DevEnvironment.ps1"
$wrapper = Join-Path $projectRoot 'backend\mvnw.cmd'
$maven = if (Test-Path -LiteralPath $wrapper) { $wrapper } else { (Get-Command mvn.cmd).Source }
& $maven -B -ntp -f "$projectRoot/backend/pom.xml" "-Dmaven.repo.local=$projectRoot/.tools/maven-repository" @MavenArguments
if ($LASTEXITCODE -ne 0) { throw "Maven failed with exit code $LASTEXITCODE" }
