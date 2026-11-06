$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot/Read-LocalSettings.ps1"
. "$PSScriptRoot/Get-PostgresWorkspace.ps1"
$settings = Read-GengLocalSettings $projectRoot
$runtimeRoot = Get-GengPostgresWorkspace $projectRoot
$table = 'foundation_probe_' + [guid]::NewGuid().ToString('N')
function Invoke-ProbeSql([string]$Sql) {
    $env:PGPASSWORD = $settings.GENG_DB_ADMIN_PASSWORD
    & "$runtimeRoot/.tools/postgresql-17.11/pgsql/bin/psql.exe" -X -h 127.0.0.1 -p 55432 -U geng_admin -d geng_dev -v ON_ERROR_STOP=1 -Atc $Sql
    if ($LASTEXITCODE -ne 0) { throw 'Local restart probe SQL failed.' }
}
try {
    Invoke-ProbeSql "CREATE TABLE ops.$table(id integer PRIMARY KEY); INSERT INTO ops.$table VALUES (1);" | Out-Null
    & "$PSScriptRoot/Stop-LocalDatabase.ps1"
    & "$PSScriptRoot/Start-LocalDatabase.ps1"
    $runtimeRoot = Get-GengPostgresWorkspace $projectRoot
    $count = Invoke-ProbeSql "SELECT count(*) FROM ops.$table WHERE id=1"
    if ($count -ne '1') { throw 'Committed data did not survive database restart.' }
    $version = Invoke-ProbeSql 'SELECT version FROM ops.flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1'
    if ($version -ne '2') { throw 'Unexpected migration state after restart.' }
    Invoke-ProbeSql "DROP TABLE ops.$table" | Out-Null
    Write-Host 'PostgreSQL restart retained committed probe data, roles and migration v2; probe removed.'
} finally { Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue }
