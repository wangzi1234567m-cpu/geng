function Read-GengLocalSettings {
    param([string]$ProjectRoot)
    $environmentFile = Join-Path $ProjectRoot '.dev\local.env'
    if (-not (Test-Path -LiteralPath $environmentFile)) { throw 'Run tools/Initialize-DevEnvironment.ps1 first.' }
    $settings = @{}
    foreach ($line in Get-Content -LiteralPath $environmentFile) {
        if ($line -match '^([A-Z][A-Z0-9_]*)=(.*)$') { $settings[$Matches[1]] = $Matches[2] }
    }
    return $settings
}
