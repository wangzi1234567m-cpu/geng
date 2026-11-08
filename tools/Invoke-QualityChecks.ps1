$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$qualityRoot = Join-Path $projectRoot '.tools/quality'
if (-not (Test-Path -LiteralPath "$qualityRoot/osv-scanner.exe")) { throw 'Run tools/Install-QualityTools.ps1 first.' }
$snapshot = Join-Path $projectRoot ('.dev/review-snapshots/' + [guid]::NewGuid().ToString('N'))
Push-Location $projectRoot
try {
    & "$qualityRoot/actionlint/actionlint.exe"
    if ($LASTEXITCODE -ne 0) { throw 'GitHub Actions validation failed.' }
    & node tools/build-review-snapshot.mjs $snapshot
    if ($LASTEXITCODE -ne 0) { throw 'Review snapshot failed.' }
    & "$qualityRoot/gitleaks/gitleaks.exe" dir $snapshot --redact=100 --no-banner --report-format json --report-path "$projectRoot/.dev/gitleaks-working-tree.json"
    if ($LASTEXITCODE -ne 0) { throw 'Working tree secret scan failed; inspect the redacted private report.' }
    & "$qualityRoot/gitleaks/gitleaks.exe" git $projectRoot --redact=100 --no-banner --report-format json --report-path "$projectRoot/.dev/gitleaks-history.json"
    if ($LASTEXITCODE -ne 0) { throw 'Git history secret scan failed.' }
    & npm run check
    if ($LASTEXITCODE -ne 0) { throw 'Contract/source validation failed.' }
    & npm run format:check
    if ($LASTEXITCODE -ne 0) { throw 'Formatting check failed.' }
    & npm audit --json --cache .tools/npm-cache | Set-Content -LiteralPath "$projectRoot/.dev/npm-audit.json" -Encoding UTF8
    if ($LASTEXITCODE -ne 0) { throw 'npm audit failed; inspect the private report.' }
    & "$qualityRoot/osv-scanner.exe" scan source -L backend/target/classes/META-INF/sbom/application.cdx.json -L android/app/gradle.lockfile -L package-lock.json --all-packages --format json --output-file .dev/osv-report.json
    if ($LASTEXITCODE -ne 0) { throw 'Dependency audit failed; inspect the private report.' }
    Write-Host 'Source, OpenAPI, formatting, working-tree/history secrets and dependency audits passed.'
} finally { Pop-Location }
