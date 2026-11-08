$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
& "$PSScriptRoot/Initialize-DevEnvironment.ps1"
. "$PSScriptRoot/Read-LocalSettings.ps1"
$settings = Read-GengLocalSettings $projectRoot
. "$PSScriptRoot/Get-PostgresWorkspace.ps1"
$runtimeRoot = Get-GengPostgresWorkspace $projectRoot
$pgBin = Join-Path $runtimeRoot '.tools/postgresql-17.11/pgsql/bin'
if (-not (Test-Path -LiteralPath "$pgBin/pg_ctl.exe")) { throw 'Run tools/Install-Postgres.ps1 first.' }
$clusterRoot = Join-Path $runtimeRoot '.dev/postgres'
$data = Join-Path $clusterRoot 'data'
$marker = Join-Path $clusterRoot '.geng-cluster'
New-Item -ItemType Directory -Force -Path $clusterRoot | Out-Null
if ((Test-Path -LiteralPath "$data/PG_VERSION") -and -not (Test-Path -LiteralPath $marker)) {
    throw 'Refusing to manage an unmarked PostgreSQL cluster.'
}
if ($settings.GENG_DB_URL -ne 'jdbc:postgresql://127.0.0.1:55432/geng_dev') {
    throw 'This local-cluster helper requires the documented loopback development URL.'
}
if (-not (Test-Path -LiteralPath "$data/PG_VERSION")) {
    $passwordFile = Join-Path $clusterRoot 'init-password.tmp'
    try {
        [IO.File]::WriteAllText($passwordFile, $settings.GENG_DB_ADMIN_PASSWORD, [Text.UTF8Encoding]::new($false))
        & "$pgBin/initdb.exe" -D $data -U geng_admin --encoding=UTF8 --locale=C --auth=scram-sha-256 "--pwfile=$passwordFile"
        if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL cluster initialization failed.' }
    } finally { Remove-Item -LiteralPath $passwordFile -Force -ErrorAction SilentlyContinue }
    @"
listen_addresses = '127.0.0.1'
port = 55432
password_encryption = 'scram-sha-256'
log_statement = 'none'
log_min_error_statement = 'panic'
log_parameter_max_length_on_error = 0
"@ | Add-Content -LiteralPath "$data/postgresql.conf" -Encoding UTF8
    [IO.File]::WriteAllText($marker, 'geng project development cluster', [Text.UTF8Encoding]::new($false))
}
& "$pgBin/pg_ctl.exe" -D $data status *> $null
if ($LASTEXITCODE -ne 0) {
    $process = Start-Process -FilePath "$pgBin/pg_ctl.exe" -ArgumentList @('-D', ('"' + $data + '"'), '-l', ('"' + "$clusterRoot/server.log" + '"'), '-w', 'start') -WindowStyle Hidden -PassThru -RedirectStandardOutput "$clusterRoot/start.out" -RedirectStandardError "$clusterRoot/start.err"
    # Wait for pg_ctl itself; Start-Process -Wait also waits for the server child.
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw 'PostgreSQL startup failed; see private .dev/postgres logs.' }
}
try {
    $env:PGPASSWORD = $settings.GENG_DB_ADMIN_PASSWORD
    $env:PGCLIENTENCODING = 'UTF8'
    $common = @('-X', '-h', '127.0.0.1', '-p', '55432', '-U', 'geng_admin', '-v', 'ON_ERROR_STOP=1')
    $databaseExists = & "$pgBin/psql.exe" @common -d postgres -Atc "SELECT count(*) FROM pg_database WHERE datname='geng_dev'"
    if ($LASTEXITCODE -ne 0) { throw 'Database administrator authentication failed.' }
    if ($databaseExists -eq '0') {
        & "$pgBin/createdb.exe" -h 127.0.0.1 -p 55432 -U geng_admin -O geng_admin geng_dev
        if ($LASTEXITCODE -ne 0) { throw 'Development database creation failed.' }
    }
    $roleCount = & "$pgBin/psql.exe" @common -d geng_dev -Atc "SELECT count(*) FROM pg_roles WHERE rolname IN ('geng_app','geng_migrator')"
    if ($LASTEXITCODE -ne 0) { throw 'Role inspection failed.' }
    if ($roleCount -eq '0') {
        $env:GENG_DB_PASSWORD = $settings.GENG_DB_PASSWORD
        $env:GENG_MIGRATION_PASSWORD = $settings.GENG_MIGRATION_PASSWORD
        $bootstrap = (Join-Path $runtimeRoot 'deploy/postgres/bootstrap.sql').Replace('\', '/')
        @"
\getenv app_password GENG_DB_PASSWORD
\getenv migration_password GENG_MIGRATION_PASSWORD
\i '$bootstrap'
"@ | & "$pgBin/psql.exe" @common -d geng_dev
        if ($LASTEXITCODE -ne 0) { throw 'Database role bootstrap failed.' }
    } elseif ($roleCount -ne '2') { throw 'Partial role setup found; refusing to overwrite credentials.' }
} finally {
    Remove-Item Env:PGPASSWORD,Env:PGCLIENTENCODING,Env:GENG_DB_PASSWORD,Env:GENG_MIGRATION_PASSWORD -ErrorAction SilentlyContinue
}
Write-Host 'Project PostgreSQL is ready on 127.0.0.1:55432. Existing data and passwords retained.'
