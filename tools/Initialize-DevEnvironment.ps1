$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$privateDirectory = Join-Path $projectRoot '.dev'
New-Item -ItemType Directory -Force -Path $privateDirectory | Out-Null
if ($env:OS -eq 'Windows_NT') {
    $currentSid = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    $workspaceOwner = (Get-Acl -LiteralPath $projectRoot).Owner
    $workspaceSid = ([Security.Principal.NTAccount]::new($workspaceOwner)).Translate([Security.Principal.SecurityIdentifier]).Value
    $directoryInfo = [IO.DirectoryInfo]::new($privateDirectory)
    $privateAcl = [IO.FileSystemAclExtensions]::GetAccessControl($directoryInfo, [Security.AccessControl.AccessControlSections]::Access)
    $privateAcl.SetAccessRuleProtection($true, $false)
    foreach ($sid in @($currentSid, $workspaceSid, 'S-1-5-18', 'S-1-5-32-544') | Select-Object -Unique) {
        $rule = New-Object Security.AccessControl.FileSystemAccessRule([Security.Principal.SecurityIdentifier]::new($sid), 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
        $privateAcl.AddAccessRule($rule)
    }
    [IO.FileSystemAclExtensions]::SetAccessControl($directoryInfo, $privateAcl)
}
$environmentFile = Join-Path $privateDirectory 'local.env'
if (Test-Path -LiteralPath $environmentFile) {
    Write-Host 'Existing local secrets retained; no passwords were changed.'
    return
}
function New-LocalSecret {
    $secretBytes = New-Object byte[] 32
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($secretBytes) } finally { $generator.Dispose() }
    return ([BitConverter]::ToString($secretBytes)).Replace('-', '').ToLowerInvariant()
}
$lines = @(
    "GENG_DB_ADMIN_PASSWORD=$(New-LocalSecret)",
    "GENG_DB_PASSWORD=$(New-LocalSecret)",
    "GENG_MIGRATION_PASSWORD=$(New-LocalSecret)",
    'GENG_DB_URL=jdbc:postgresql://127.0.0.1:55432/geng_dev'
)
[IO.File]::WriteAllLines($environmentFile, $lines, [Text.UTF8Encoding]::new($false))
Write-Host 'Created private local configuration in .dev/local.env. Values were not printed.'
