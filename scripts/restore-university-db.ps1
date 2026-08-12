# ============================================================
# University DB restore (Windows, PowerShell 5.1 compatible)
# NOTE: keep this file pure ASCII (no Chinese characters),
#       PowerShell 5.1 misreads BOM-less UTF-8 scripts as GBK.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts\restore-university-db.ps1
#   ... -DumpFile D:\db-backup\university_20260811_020000.sql
#   ... -Force                     (skip interactive confirmation)
#   ... -NoSafetyBackup            (skip automatic pre-restore backup)
#
# Behavior:
#   1. Picks the newest dump in <BackupDir> unless -DumpFile is given
#   2. Takes a safety backup first (the state you are about to overwrite)
#   3. Requires typing the database name to confirm (unless -Force)
#   4. Restores the dump (drops and recreates all tables in "university")
#   5. Verifies: takes/student/instructor row counts + flyway history
#   6. Appends result summary to <BackupDir>\backup.log
# ============================================================
param(
    [string]$DumpFile = "",
    [string]$BackupDir = "",
    [switch]$Force,
    [switch]$NoSafetyBackup
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
if ($BackupDir -eq "") { $BackupDir = Join-Path $RepoRoot "backups" }
$DbName = "university"
$LogFile = Join-Path $BackupDir "backup.log"

function Write-Log([string]$Msg) {
    $line = (Get-Date -Format "yyyy-MM-dd HH:mm:ss") + "  " + $Msg
    Write-Output $line
    Add-Content -Path $LogFile -Value $line -Encoding ascii
}

function Find-MySqlBin([string]$ExeName) {
    $cmd = Get-Command $ExeName -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $candidates = @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin",
        "C:\Program Files\MySQL\MySQL Server 8.4\bin",
        "C:\Program Files\MySQL\MySQL Server 5.7\bin"
    )
    foreach ($dir in $candidates) {
        $p = Join-Path $dir $ExeName
        if (Test-Path $p) { return $p }
    }
    return $null
}

$MysqlExe = Find-MySqlBin "mysql.exe"
if (-not $MysqlExe) { throw "mysql.exe not found: add MySQL bin to PATH or install MySQL Server" }

# ---- read connection config: db.properties (gitignored) > DB_USER/DB_PASSWORD env > defaults ----
$DbUser = $env:DB_USER; if (-not $DbUser) { $DbUser = "root" }
$DbPassword = $env:DB_PASSWORD; if (-not $DbPassword) { $DbPassword = "root" }
$DbPort = "3306"
$PropsFile = Join-Path $RepoRoot "src\main\resources\db.properties"
if (Test-Path $PropsFile) {
    foreach ($raw in Get-Content $PropsFile) {
        $line = $raw.Trim()
        if ($line.StartsWith("#") -or $line -eq "") { continue }
        $kv = $line -split "=", 2
        if ($kv.Count -ne 2) { continue }
        $key = $kv[0].Trim(); $val = $kv[1].Trim()
        if ($key -eq "user") { $DbUser = $val }
        elseif ($key -eq "password") { $DbPassword = $val }
        elseif ($key -eq "port") { $DbPort = $val }
    }
}

# ---- select dump file ----
if ($DumpFile -eq "") {
    if (-not (Test-Path $BackupDir)) { throw "Backup dir not found: $BackupDir" }
    $Newest = Get-ChildItem -Path $BackupDir -Filter "${DbName}_*.sql" | Sort-Object Name -Descending | Select-Object -First 1
    if (-not $Newest) { throw "No dump file found in $BackupDir" }
    $DumpFile = $Newest.FullName
}
if (-not (Test-Path $DumpFile)) { throw "Dump file not found: $DumpFile" }
$Completed = Select-String -Path $DumpFile -Pattern "Dump completed on" -Quiet
if (-not $Completed) { throw "Dump file looks incomplete (no 'Dump completed on' marker): $DumpFile" }

Write-Output "Dump to restore : $DumpFile"
Write-Output "Target database : $DbName (127.0.0.1:$DbPort)"
Write-Output ""
Write-Output "WARNING: this DROPS and RECREATES all tables in '$DbName'."

# ---- confirmation gate ----
if (-not $Force) {
    $answer = Read-Host "Type the database name to confirm"
    if ($answer.Trim() -ne $DbName) {
        Write-Output "Aborted: confirmation mismatch."
        exit 1
    }
}

# ---- safety backup of current state ----
if (-not $NoSafetyBackup) {
    Write-Output ""
    Write-Output "Taking a safety backup of the current state..."
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "backup-university-db.ps1") -BackupDir $BackupDir
    if ($LASTEXITCODE -ne 0) { throw "Safety backup failed; restore aborted" }
}

# ---- restore ----
$env:MYSQL_PWD = $DbPassword
cmd /c "`"$MysqlExe`" --host=127.0.0.1 --port=$DbPort --user=$DbUser --default-character-set=utf8mb4 < `"$DumpFile`""
$RestoreExit = $LASTEXITCODE
$env:MYSQL_PWD = ""
if ($RestoreExit -ne 0) {
    Write-Log "RESTORE FAILED exit=$RestoreExit dump=$DumpFile"
    throw "Restore failed with exit code $RestoreExit (a safety backup was taken beforehand if enabled)"
}

# ---- post-restore verification ----
$env:MYSQL_PWD = $DbPassword
$CountsSql = "SELECT COUNT(*) FROM ${DbName}.takes; SELECT COUNT(*) FROM ${DbName}.student; SELECT COUNT(*) FROM ${DbName}.instructor; SELECT COUNT(*) FROM ${DbName}.flyway_schema_history;"
# PS 5.1: relax ErrorActionPreference around native calls, rely on $LASTEXITCODE
$OldEAP = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$AfterCounts = (& $MysqlExe -h 127.0.0.1 -P $DbPort -u $DbUser --batch --skip-column-names -e $CountsSql) 2>&1
$VerifyExit = $LASTEXITCODE
$ErrorActionPreference = $OldEAP
$env:MYSQL_PWD = ""
if ($VerifyExit -ne 0) {
    Write-Log "RESTORE DONE but verification query failed: $AfterCounts"
    throw "Post-restore verification failed: $AfterCounts"
}

$CountsLine = ($AfterCounts -join "/")
Write-Log ("RESTORE OK dump=$DumpFile rows(takes/student/instructor/flyway)=$CountsLine")
Write-Output ""
Write-Output "Restore succeeded. Row counts (takes/student/instructor/flyway_history): $CountsLine"
exit 0
