$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot/Read-LocalSettings.ps1"
. "$PSScriptRoot/Get-PostgresWorkspace.ps1"
$runtimeRoot = Get-GengPostgresWorkspace $projectRoot
$settings = Read-GengLocalSettings $projectRoot
$pgBin = Join-Path $runtimeRoot '.tools/postgresql-17.11/pgsql/bin'
try {
    $env:PGPASSWORD = $settings.GENG_DB_ADMIN_PASSWORD
    $common = @('-X','-h','127.0.0.1','-p','55432','-U','geng_admin','-v','ON_ERROR_STOP=1')
    $exists = & "$pgBin/psql.exe" @common -d postgres -Atc "SELECT count(*) FROM pg_database WHERE datname='geng_foundation_it'"
    if ($LASTEXITCODE -ne 0) { throw 'Local PostgreSQL must be running.' }
    if ($exists -eq '0') {
        & "$pgBin/createdb.exe" -h 127.0.0.1 -p 55432 -U geng_admin geng_foundation_it
        if ($LASTEXITCODE -ne 0) { throw 'Integration database creation failed.' }
        @'
REVOKE ALL ON DATABASE geng_foundation_it FROM PUBLIC;
GRANT CONNECT ON DATABASE geng_foundation_it TO geng_app,geng_migrator;
GRANT CREATE ON DATABASE geng_foundation_it TO geng_migrator;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA ops AUTHORIZATION geng_migrator;
'@ | & "$pgBin/psql.exe" @common -d geng_foundation_it
        if ($LASTEXITCODE -ne 0) { throw 'Integration role setup failed.' }
    }
    $env:GENG_DB_URL = 'jdbc:postgresql://127.0.0.1:55432/geng_foundation_it'
    $env:GENG_DB_PASSWORD = $settings.GENG_DB_PASSWORD
    $env:GENG_MIGRATION_PASSWORD = $settings.GENG_MIGRATION_PASSWORD
    $env:GENG_IT_ADMIN_PASSWORD = $settings.GENG_DB_ADMIN_PASSWORD
    & "$PSScriptRoot/Invoke-Backend.ps1" -MavenArguments @('-N','flyway:migrate')
    & "$PSScriptRoot/Invoke-Backend.ps1" -MavenArguments @('-N','flyway:migrate','flyway:validate')
    & "$PSScriptRoot/Invoke-Backend.ps1" -MavenArguments @('-Ppostgres-it,sbom','verify')
} finally {
    Remove-Item Env:PGPASSWORD,Env:GENG_DB_URL,Env:GENG_DB_PASSWORD,Env:GENG_MIGRATION_PASSWORD,Env:GENG_IT_ADMIN_PASSWORD -ErrorAction SilentlyContinue
}
