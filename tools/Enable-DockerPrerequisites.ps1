#Requires -RunAsAdministrator
[CmdletBinding()]
param([string]$ReportPath)
$ErrorActionPreference = 'Stop'
$features = @('VirtualMachinePlatform', 'Microsoft-Windows-Subsystem-Linux')
$requiresRestart = $false
try {
    foreach ($name in $features) {
        $current = Get-WindowsOptionalFeature -Online -FeatureName $name
        if ($current.State -eq 'Enabled') {
            Write-Host "$name already enabled."
            continue
        }
        if ($current.State -eq 'EnablePending') {
            $requiresRestart = $true
            continue
        }
        $result = Enable-WindowsOptionalFeature -Online -FeatureName $name -All -NoRestart
        if ($result.RestartNeeded) { $requiresRestart = $true }
    }
    $states = @($features | ForEach-Object {
        $feature = Get-WindowsOptionalFeature -Online -FeatureName $_
        if ($feature.State -eq 'EnablePending') { $requiresRestart = $true }
        [pscustomobject]@{name=$feature.FeatureName; state=$feature.State.ToString()}
    })
    if ($ReportPath) {
        @{status='Completed';restartRequired=$requiresRestart;features=$states;completedUtc=[DateTime]::UtcNow.ToString('o')} |
            ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $ReportPath -Encoding utf8
    }
} catch {
    if ($ReportPath) {
        @{status='Failed';error=$_.Exception.Message;completedUtc=[DateTime]::UtcNow.ToString('o')} |
            ConvertTo-Json | Set-Content -LiteralPath $ReportPath -Encoding utf8
    }
    throw
}
if ($requiresRestart) {
    Write-Warning 'Windows restart required. No restart has been performed.'
} else {
    Write-Host 'Feature configuration completed. Start Docker Desktop and check the engine.'
}
