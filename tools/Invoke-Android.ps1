[CmdletBinding()]
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GradleArguments = @('assembleDebug'))
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot/Enter-DevEnvironment.ps1"
$originalGradleHome = $env:GRADLE_USER_HOME
$temporaryDrive = $null
$buildRoot = $projectRoot
try {
    # Windows Java worker argument files can misread non-ASCII classpaths.
    # Alias the workspace for this build; no files move and no partition changes.
    if ($projectRoot -match '[^\x00-\x7F]') {
        foreach ($letter in [char[]](71..90)) {
            $candidate = "${letter}:"
            if (-not (Test-Path -LiteralPath "$candidate/")) {
                & subst.exe $candidate $projectRoot
                if ($LASTEXITCODE -ne 0) { throw 'Could not create temporary build path alias.' }
                $temporaryDrive = $candidate
                $buildRoot = "$candidate/"
                break
            }
        }
        if (-not $temporaryDrive) { throw 'No free drive letter for the temporary build path alias.' }
        $relativeCache = [IO.Path]::GetRelativePath($projectRoot, $originalGradleHome)
        if ($relativeCache -ne '..' -and -not $relativeCache.StartsWith('..\') -and -not [IO.Path]::IsPathRooted($relativeCache)) {
            $env:GRADLE_USER_HOME = Join-Path $buildRoot $relativeCache
        }
    }
    if ($env:GRADLE_USER_HOME -match '[^\x00-\x7F]') { throw 'Gradle cache must be inside the workspace or have an ASCII path.' }
    & "$buildRoot/android/gradlew.bat" -p "$buildRoot/android" --no-daemon @GradleArguments
    if ($LASTEXITCODE -ne 0) { throw "Android build failed with exit code $LASTEXITCODE" }
} finally {
    $env:GRADLE_USER_HOME = $originalGradleHome
    if ($temporaryDrive) { & subst.exe $temporaryDrive /D }
}
