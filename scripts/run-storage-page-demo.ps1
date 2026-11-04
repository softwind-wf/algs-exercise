<#
.SYNOPSIS
    演示"数据怎么存到磁盘、又怎么从磁盘读出来": 8KB 页 / 行指针 / ctid / 缓冲池 / WAL / 检查点。

.DESCRIPTION
    分三段执行，全部真实跑库，输出留档到 docs/storage-page-demo-raw.txt：

      1) SQL 段落 —— src/main/resources/sql/storage_page_demo.sql
         建一张每行约 244 字节的表，看清一页装 32 行；用 pageinspect 打印
         页头、行指针、元组；打印 ctid；用 hex + 位运算手工解码页头与行指针；
         看读路径的 Buffers；删一行/改一行后页大小不变；VACUUM 回收页内空间；
         看缓冲池里这张表占了哪些页。

      2) 磁盘字节复核
         对 t_demo 执行 CHECKPOINT，然后绕过数据库，直接用 .NET 读数据文件
         base/<db>/<relfilenode> 的最前面若干字节，按 PostgreSQL 页格式解出
         pd_lower / pd_upper / 行指针，和 SQL 查出来的数字逐项对照。

      3) 写路径复核
         插入一行（不 CHECKPOINT）：磁盘数据文件里还找不到这行，但 WAL 已经
         写好了；pg_waldump 打出这一小段 WAL 的三条记录（Heap INSERT /
         Btree INSERT_LEAF / COMMIT）。再 CHECKPOINT，数据文件里才出现这行。

    脚本只建/删自己的 storage_demo 演示库，不碰 university 等既有库。
    连接参数默认读 src/main/resources/pg.properties，可用参数覆盖。

.EXAMPLE
    .\scripts\run-storage-page-demo.ps1
    .\scripts\run-storage-page-demo.ps1 -PgPort 5432
#>
[CmdletBinding()]
param(
    [string]$PgHost = '127.0.0.1',
    [int]$PgPort = 0,
    [string]$PgUser = '',
    [string]$PgPassword = '',
    [string]$PgDatabase = 'storage_demo'
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# ---------- 读连接配置 ----------
function Read-Props([string]$path) {
    $map = @{}
    if (Test-Path $path) {
        foreach ($line in Get-Content $path) {
            if ($line -match '^\s*([A-Za-z_]+)\s*=\s*(.*?)\s*$') {
                $map[$matches[1].ToLowerInvariant()] = $matches[2]
            }
        }
    }
    return $map
}

$pg = Read-Props 'src\main\resources\pg.properties'
if (-not $PgPort)     { $PgPort = [int]($pg['port'] ?? '5432') }
if (-not $PgUser)     { $PgUser = ($pg['user'] ?? 'postgres') }
if (-not $PgPassword) { $PgPassword = ($pg['password'] ?? '') }

$env:PGPASSWORD = $PgPassword
$env:PGCLIENTENCODING = 'UTF8'

# ---------- 找客户端 ----------
function Find-PgTool([string]$name) {
    $c = Get-Command $name -ErrorAction SilentlyContinue
    if ($c) { return $c.Source }
    foreach ($v in @('17', '16', '15', '14')) {
        $p = Join-Path $env:ProgramFiles "PostgreSQL\$v\bin\$name"
        if (Test-Path $p) { return $p }
    }
    throw "找不到 $name（装了 PostgreSQL 但不在 PATH 的常见目录里）"
}

$psql = Find-PgTool 'psql.exe'
$pgWaldump = Find-PgTool 'pg_waldump.exe'

$common = @('-U', $PgUser, '-h', $PgHost, '-p', "$PgPort", '-X', '-q')

function Invoke-Psql([string]$db, [string[]]$extra) {
    & $psql @($common + @('-d', $db) + $extra)
}

$sb = [System.Text.StringBuilder]::new()
function Add-Block([string]$title, [string]$body) {
    [void]$sb.Append("`r`n##### $title`r`n$body`r`n")
    Write-Host ''
    Write-Host "##### $title" -ForegroundColor Cyan
    Write-Host $body
}

Write-Host "== 演示库: $PgDatabase @ $PgHost`:$PgPort（只建/删这一个库）"

# ---------- 0. 重建演示库 ----------
Invoke-Psql 'postgres' @('-c', "DROP DATABASE IF EXISTS $PgDatabase WITH (FORCE);") | Out-Null
Invoke-Psql 'postgres' @('-c', "CREATE DATABASE $PgDatabase;") | Out-Null

# ---------- 1. SQL 段落 ----------
$sqlFile = Join-Path $root 'src\main\resources\sql\storage_page_demo.sql'
$sqlOut = Invoke-Psql $PgDatabase @('-v', 'ON_ERROR_STOP=1', '-f', $sqlFile) 2>&1 | Out-String
if ($LASTEXITCODE -ne 0) {
    Write-Host $sqlOut
    throw "SQL 段落执行失败（psql 退出码 $LASTEXITCODE），见上方输出"
}
Add-Block '1. SQL 段落：页头 / 行指针 / ctid / 读路径 / 改写 / 缓冲池' $sqlOut.TrimEnd()

# ---------- 2. 磁盘字节复核 ----------
$datadir = (Invoke-Psql $PgDatabase @('-Atc', 'SHOW data_directory')).Trim()
$relpath = (Invoke-Psql $PgDatabase @('-Atc', "SELECT pg_relation_filepath('t_demo')")).Trim()

# 先把内存里的脏页刷到磁盘，否则数据文件里可能还是空页
Invoke-Psql $PgDatabase @('-c', 'CHECKPOINT;') | Out-Null

$dataFile = Join-Path $datadir ($relpath -replace '/', '\')
$bytes = [System.IO.File]::ReadAllBytes($dataFile)

function Get-U16([byte[]]$b, [int]$o) { [BitConverter]::ToUInt16($b, $o) }
function Get-U32([byte[]]$b, [int]$o) { [BitConverter]::ToUInt32($b, $o) }

$pdLower = Get-U16 $bytes 12
$pdUpper = Get-U16 $bytes 14
$pdSpecial = Get-U16 $bytes 16
$pdVersion = Get-U16 $bytes 18
$lp1 = Get-U32 $bytes 24
$lp1Off = $lp1 -band 0x7FFF
$lp1Flags = ($lp1 -shr 15) -band 0x3
$lp1Len = $lp1 -shr 17
$firstTuple = [System.Text.Encoding]::ASCII.GetString($bytes, $lp1Off, [Math]::Min(60, $lp1Len))
$firstTuple = ($firstTuple -replace '[^\x20-\x7E]', '.')

$mem = (Invoke-Psql $PgDatabase @('-Atc',
    "SELECT lower || ' / ' || upper || ' / ' || special FROM page_header(get_raw_page('t_demo', 0))")).Trim()
$memLp = (Invoke-Psql $PgDatabase @('-Atc',
    "SELECT lp_off || ' / ' || lp_flags || ' / ' || lp_len FROM heap_page_items(get_raw_page('t_demo', 0)) WHERE lp = 1" )).Trim()

$diskReport = @(
    "数据目录 : $datadir",
    "数据文件 : $dataFile",
    "文件大小 : $($bytes.Length) 字节（$([math]::Ceiling($bytes.Length / 8192)) 页）",
    '',
    "字段                    磁盘字节解出    pageinspect 查出",
    "pd_lower                $pdLower                $(($mem -split ' / ')[0])",
    "pd_upper                $pdUpper                $(($mem -split ' / ')[1])",
    "pd_special              $pdSpecial              $(($mem -split ' / ')[2])",
    ("pagesize|version        " + ('0x{0:X4}' -f $pdVersion) + "              （见第 5 段 hex）"),
    "行指针 1 (lp_off)       $lp1Off                $(($memLp -split ' / ')[0])",
    "行指针 1 (lp_flags)     $lp1Flags                 $(($memLp -split ' / ')[1])",
    "行指针 1 (lp_len)       $lp1Len                $(($memLp -split ' / ')[2])",
    '',
    "第 1 个元组开头的可读字节: $firstTuple"
) -join "`r`n"
Add-Block '2. 磁盘字节复核（CHECKPOINT 之后，直接读数据文件）' $diskReport

# ---------- 3. 写路径复核：WAL 先落盘，数据页后落盘 ----------
$lsnBefore = (Invoke-Psql $PgDatabase @('-Atc', 'SELECT pg_current_wal_lsn()')).Trim()
Invoke-Psql $PgDatabase @('-c', "INSERT INTO t_demo (id, name, pad) VALUES (100, 'student100', repeat('x', 190));") | Out-Null
$lsnAfter = (Invoke-Psql $PgDatabase @('-Atc', 'SELECT pg_current_wal_lsn()')).Trim()

$hasBefore = [System.Text.Encoding]::ASCII.GetString([System.IO.File]::ReadAllBytes($dataFile)).Contains('student100')
Invoke-Psql $PgDatabase @('-c', 'CHECKPOINT;') | Out-Null
$hasAfter = [System.Text.Encoding]::ASCII.GetString([System.IO.File]::ReadAllBytes($dataFile)).Contains('student100')

$walDir = Join-Path $datadir 'pg_wal'
$walDump = (& $pgWaldump -p $walDir -s $lsnBefore -e $lsnAfter -n 10 2>&1) |
    ForEach-Object { $_ -replace 'COMMIT\s+.*', 'COMMIT' } |
    Out-String

$writeReport = @(
    "插入前的 WAL 位置 : $lsnBefore",
    "插入后的 WAL 位置 : $lsnAfter",
    '',
    ('{0,-34}{1}' -f '检查点之前，数据文件里有 student100 吗?', $(if ($hasBefore) { '有（页可能已被后台写回）' } else { '没有 —— 数据还只在内存的缓冲页里' })),
    ('{0,-34}{1}' -f '检查点之后，数据文件里有 student100 吗?', $(if ($hasAfter) { '有 —— 脏页被写回磁盘' } else { '仍然没有（异常）' })),
    '',
    '这一小段 WAL 里的原始记录（pg_waldump）：',
    $walDump.TrimEnd()
) -join "`r`n"
Add-Block '3. 写路径复核（WAL 先写，数据页后写）' $writeReport

# ---------- 4. 留档 ----------
$rawPath = 'docs\storage-page-demo-raw.txt'
$header = @(
    "存储与文件结构演示 —— 真实运行输出",
    "生成时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
    "PostgreSQL: $((Invoke-Psql $PgDatabase @('-Atc', 'SHOW server_version')).Trim())",
    "演示库: $PgDatabase",
    "复现: .\scripts\run-storage-page-demo.ps1"
) -join "`r`n"

Set-Content -Path $rawPath -Value ($header + $sb.ToString()) -Encoding utf8
Write-Host ''
Write-Host "== 原始输出已留档: $rawPath" -ForegroundColor Green
exit 0
