# ============================================================
# University DB backup (Windows, PowerShell 5.1 compatible)
# NOTE: keep this file pure ASCII (no Chinese characters),
#       PowerShell 5.1 misreads BOM-less UTF-8 scripts as GBK.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts\backup-university-db.ps1
#   ... -BackupDir D:\db-backup -Keep 30
#
# Behavior:
#   1. Reads user/password/port from src\main\resources\db.properties
#   2. mysqldump full "university" database (single-transaction,
#      consistent snapshot without locking InnoDB)
#   3. Verifies dump: exit code + "Dump completed on" marker + size
#   4. Rotates: keeps the newest $Keep dumps (default 14)
#   5. Appends result summary to <BackupDir>\backup.log
# ============================================================
param(
    [string]$BackupDir = "",
    [int]$Keep = 14
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

# ---- locate mysql client binaries ----
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

$DumpExe = Find-MySqlBin "mysqldump.exe"
$MysqlExe = Find-MySqlBin "mysql.exe"
if (-not $DumpExe) { throw "mysqldump.exe not found: add MySQL bin to PATH or install MySQL Server" }
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

if (-not (Test-Path $BackupDir)) { New-Item -ItemType Directory -Path $BackupDir | Out-Null }

# password via env var, never on the command line
$env:MYSQL_PWD = $DbPassword

# ---- pre-backup row counts (used for post-restore comparison) ----
$CountsSql = "SELECT COUNT(*) FROM ${DbName}.takes; SELECT COUNT(*) FROM ${DbName}.student; SELECT COUNT(*) FROM ${DbName}.instructor;"
# PS 5.1: native command stderr + ErrorActionPreference=Stop aborts the script,
# so relax to Continue around native calls and rely on $LASTEXITCODE instead
$OldEAP = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$BeforeCounts = (& $MysqlExe -h 127.0.0.1 -P $DbPort -u $DbUser --batch --skip-column-names -e $CountsSql) 2>&1
$CountExit = $LASTEXITCODE
$ErrorActionPreference = $OldEAP
if ($CountExit -ne 0) {
    $env:MYSQL_PWD = ""
    throw "Cannot connect to MySQL (port $DbPort): $BeforeCounts"
}

# ---- dump ----
$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$DumpFile = Join-Path $BackupDir ("${DbName}_${Stamp}.sql")
# run through cmd to keep the dump byte-exact (avoid PS output re-encoding)
$DumpArgs = "--host=127.0.0.1 --port=$DbPort --user=$DbUser --single-transaction --routines --triggers --default-character-set=utf8mb4 --databases $DbName"
cmd /c "`"$DumpExe`" $DumpArgs > `"$DumpFile`""
$DumpExit = $LASTEXITCODE
$env:MYSQL_PWD = ""

if ($DumpExit -ne 0) {
    if (Test-Path $DumpFile) { Remove-Item $DumpFile -Force }
    Write-Log "BACKUP FAILED exit=$DumpExit"
    throw "mysqldump failed with exit code $DumpExit"
}

# ---- verify dump integrity ----
$Size = (Get-Item $DumpFile).Length
$Completed = Select-String -Path $DumpFile -Pattern "Dump completed on" -Quiet
if (-not $Completed) {
    Remove-Item $DumpFile -Force
    Write-Log "BACKUP FAILED: 'Dump completed on' marker missing, file removed"
    throw "Dump verification failed: incomplete dump file removed"
}
if ($Size -lt 10240) {
    Remove-Item $DumpFile -Force
    Write-Log "BACKUP FAILED: dump too small ($Size bytes), file removed"
    throw "Dump verification failed: file too small, removed"
}

# ---- rotation: keep newest $Keep dumps ----
$All = Get-ChildItem -Path $BackupDir -Filter "${DbName}_*.sql" | Sort-Object Name -Descending
$Removed = 0
if ($All.Count -gt $Keep) {
    $All | Select-Object -Skip $Keep | ForEach-Object {
        Remove-Item $PSItem.FullName -Force
        $Removed++
    }
}

$CountsLine = ($BeforeCounts -join "/")
Write-Log ("BACKUP OK file=$DumpFile size=$Size bytes rows(takes/student/instructor)=$CountsLine rotated_out=$Removed keep=$Keep")
Write-Output ""
Write-Output "Backup succeeded: $DumpFile"
exit 0
