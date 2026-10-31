<#
.SYNOPSIS
    运行“数据库中的加密”演示三件套，并把真实输出留档到 docs/。

.DESCRIPTION
    依次执行：
      1) MySQL 内置 AES_ENCRYPT/AES_DECRYPT（确定性、ECB 块重复、CBC+随机 IV、
         密钥进语句历史、TLS 状态）—— src/main/resources/sql/db_encryption_mysql.sql
      2) PostgreSQL pgcrypto（随机 salt/IV、完整性校验、盲索引、bcrypt、SSL 状态）
         —— src/main/resources/sql/db_encryption_pg.sql
      3) Java 应用层 AES-GCM + 信封加密 + 盲索引（JDBC 连 MySQL）
         —— src/main/java/com/ds/db/DbEncryptionDemo.java
      4) 静态文件检查：PostgreSQL 数据文件里，明文表能直接找到手机号，
         加密表的同一列找不到（用来解释 TDE / 静态加密防的是什么）。

    连接参数默认读 src/main/resources/db.properties 与 pg.properties，
    可用参数覆盖。脚本只建独立的 db_encryption_demo 库、只删演示自己的表，
    不会碰 university 等既有库。

.EXAMPLE
    .\scripts\run-db-encryption-demo.ps1
    .\scripts\run-db-encryption-demo.ps1 -SkipMysql
#>
[CmdletBinding()]
param(
    [string]$MysqlHost = '127.0.0.1',
    [int]$MysqlPort = 0,
    [string]$MysqlUser = '',
    [string]$MysqlPassword = '',
    [string]$PgHost = '127.0.0.1',
    [int]$PgPort = 0,
    [string]$PgUser = '',
    [string]$PgPassword = '',
    [string]$PgDatabase = 'db_encryption_demo',
    [switch]$SkipMysql,
    [switch]$SkipPg,
    [switch]$SkipJava,
    [switch]$SkipAtRest
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

$db = Read-Props 'src\main\resources\db.properties'
$pg = Read-Props 'src\main\resources\pg.properties'

if (-not $MysqlPort)     { $MysqlPort = [int]($db['port']     ?? '3306') }
if (-not $MysqlUser)     { $MysqlUser =        ($db['user']     ?? 'root') }
if (-not $MysqlPassword) { $MysqlPassword =    ($db['password'] ?? '') }
if (-not $PgPort)        { $PgPort = [int]($pg['port'] ?? '5432') }
if (-not $PgUser)        { $PgUser =           ($pg['user'] ?? 'postgres') }
if (-not $PgPassword)    { $PgPassword =       ($pg['password'] ?? '') }

# ---------- 找客户端 ----------
function Find-Mysql {
    $c = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($c) { return $c.Source }
    foreach ($v in @('MySQL Server 9.0', 'MySQL Server 8.4', 'MySQL Server 8.0', 'MySQL Server 5.7')) {
        $p = Join-Path $env:ProgramFiles "MySQL\$v\bin\mysql.exe"
        if (Test-Path $p) { return $p }
    }
    throw '找不到 mysql.exe（装了 MySQL 但不在 PATH 的常见目录里）'
}

function Find-Psql {
    $c = Get-Command psql.exe -ErrorAction SilentlyContinue
    if ($c) { return $c.Source }
    foreach ($v in @('17', '16', '15', '14', '13', '12')) {
        $p = Join-Path $env:ProgramFiles "PostgreSQL\$v\bin\psql.exe"
        if (Test-Path $p) { return $p }
    }
    throw '找不到 psql.exe（装了 PostgreSQL 但不在 PATH 的常见目录里）'
}

$mysql = $null
$psql = $null
if (-not $SkipMysql -or -not $SkipJava) { $mysql = Find-Mysql }
if (-not $SkipPg -or -not $SkipAtRest)  { $psql = Find-Psql }

New-Item -ItemType Directory -Force -Path docs | Out-Null

# ---------- 1) MySQL ----------
if (-not $SkipMysql) {
    Write-Host '=== 1) MySQL 内置 AES 演示 ==='
    $env:MYSQL_PWD = $MysqlPassword
    $file = 'src\main\resources\sql\db_encryption_mysql.sql'
    $cmd = '"' + $mysql + '" -h ' + $MysqlHost + ' -P ' + $MysqlPort + ' -u ' + $MysqlUser +
           ' --default-character-set=utf8mb4 -t < "' + $file + '"'
    $out = & cmd /d /c $cmd 2>&1
    $code = $LASTEXITCODE
    $out | Out-File -Encoding utf8NoBOM 'docs\db-encryption-mysql-raw.txt'
    $out | Select-Object -Last 1 | Out-Null
    if ($code -ne 0) { throw "MySQL 演示失败，退出码 $code（见 docs\db-encryption-mysql-raw.txt）" }
    Write-Host "    输出: docs\db-encryption-mysql-raw.txt（$(($out | Measure-Object -Line).Lines) 行）"
}

# ---------- 2) PostgreSQL ----------
if (-not $SkipPg) {
    Write-Host '=== 2) PostgreSQL pgcrypto 演示 ==='
    $env:PGPASSWORD = $PgPassword
    $env:PGCLIENTENCODING = 'UTF8'
    $hasDb = (& $psql -h $PgHost -p $PgPort -U $PgUser -d postgres -At `
        -c "SELECT 1 FROM pg_database WHERE datname = '$PgDatabase'") 2>&1
    if ($LASTEXITCODE -ne 0) { throw "连不上 PostgreSQL：$hasDb" }
    if (-not ($hasDb | Where-Object { $_ -eq '1' })) {
        & $psql -h $PgHost -p $PgPort -U $PgUser -d postgres -c "CREATE DATABASE $PgDatabase" | Out-Null
        if ($LASTEXITCODE -ne 0) { throw '创建 db_encryption_demo 库失败' }
    }

    $file = 'src\main\resources\sql\db_encryption_pg.sql'
    $out = & $psql -h $PgHost -p $PgPort -U $PgUser -d $PgDatabase -f $file 2>&1
    $code = $LASTEXITCODE
    $out | Out-File -Encoding utf8NoBOM 'docs\db-encryption-pg-raw.txt'
    if ($code -ne 0) { throw "PostgreSQL 演示失败，退出码 $code（见 docs\db-encryption-pg-raw.txt）" }
    Write-Host "    输出: docs\db-encryption-pg-raw.txt（$(($out | Measure-Object -Line).Lines) 行）"
}

# ---------- 3) Java 应用层 ----------
if (-not $SkipJava) {
    Write-Host '=== 3) Java 应用层 AES-GCM + 信封加密 + 盲索引 ==='
    $jar = Get-ChildItem 'target\lib' -Filter 'mysql-connector-j-*.jar' -ErrorAction SilentlyContinue |
           Select-Object -First 1
    if (-not $jar) { throw '找不到 target\lib\mysql-connector-j-*.jar' }
    $classes = 'target\db-encryption-classes'
    New-Item -ItemType Directory -Force -Path $classes | Out-Null

    & javac --release 8 -encoding UTF-8 -cp $jar.FullName -sourcepath src/main/java `
        -d $classes src/main/java/com/ds/db/DbEncryptionDemo.java
    if ($LASTEXITCODE -ne 0) { throw 'DbEncryptionDemo 编译失败' }

    $out = & java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
        -cp "$classes;$($jar.FullName)" com.ds.db.DbEncryptionDemo 2>&1
    $code = $LASTEXITCODE
    $out | Out-File -Encoding utf8NoBOM 'docs\db-encryption-app-raw.txt'
    if ($code -ne 0) { throw "Java 演示失败，退出码 $code（见 docs\db-encryption-app-raw.txt）" }
    Write-Host "    输出: docs\db-encryption-app-raw.txt（$(($out | Measure-Object -Line).Lines) 行）"
}

# ---------- 4) 静态文件检查 ----------
if (-not $SkipAtRest) {
    Write-Host '=== 4) 静态文件检查：明文表 vs 加密表的堆文件 ==='
    $env:PGPASSWORD = $PgPassword
    $env:PGCLIENTENCODING = 'UTF8'

    function Pg-Scalar([string]$sql) {
        $v = & $psql -h $PgHost -p $PgPort -U $PgUser -d $PgDatabase -At -c $sql 2>&1
        if ($LASTEXITCODE -ne 0) { throw "SQL 失败: $sql`n$v" }
        return ($v | Select-Object -First 1)
    }

    Pg-Scalar 'CHECKPOINT;' | Out-Null
    $dataDir = (Pg-Scalar 'SHOW data_directory;') -replace '/', '\'
    $plainFile = Join-Path $dataDir ((Pg-Scalar "SELECT pg_relation_filepath('student_plain');") -replace '/', '\')
    $encFile   = Join-Path $dataDir ((Pg-Scalar "SELECT pg_relation_filepath('student_enc');")   -replace '/', '\')

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('=== PostgreSQL 堆文件（data_directory 下）===')
    $lines.Add("student_plain 文件: $plainFile")
    $lines.Add("student_enc   文件: $encFile")

    $plainBytes = [IO.File]::ReadAllBytes($plainFile)
    $encBytes   = [IO.File]::ReadAllBytes($encFile)
    # Latin-1 解码保持字节一一对应，用来做字节级查找
    $plainLatin = [Text.Encoding]::Latin1.GetString($plainBytes)
    $encLatin   = [Text.Encoding]::Latin1.GetString($encBytes)

    $phone = '13900001111'
    $plainPos = $plainLatin.IndexOf($phone)
    $encPos   = $encLatin.IndexOf($phone)
    $lines.Add("在 student_plain 堆文件中搜索明文手机号 '$phone'：offset=$plainPos")
    $lines.Add("在 student_enc   堆文件中搜索明文手机号 '$phone'：offset=$encPos")

    # 用 ciphertext 的字节证明“这一行确实在文件里”，只是没有明文
    $ctHex = Pg-Scalar "SELECT encode(phone_enc, 'hex') FROM student_enc WHERE sid = 'S001';"
    $ct = New-Object byte[] ([int]($ctHex.Length / 2))
    for ($i = 0; $i -lt $ct.Length; $i++) {
        $ct[$i] = [Convert]::ToByte($ctHex.Substring($i * 2, 2), 16)
    }
    $ctPos = -1
    for ($i = 0; $i -le $encBytes.Length - $ct.Length; $i++) {
        $ok = $true
        for ($j = 0; $j -lt $ct.Length; $j++) {
            if ($encBytes[$i + $j] -ne $ct[$j]) { $ok = $false; break }
        }
        if ($ok) { $ctPos = $i; break }
    }
    $lines.Add("在 student_enc 堆文件中搜索 S001 的密文字节（$($ct.Length) 字节）：offset=$ctPos")
    $lines.Add('结论：没有 TDE 时，明文列在磁盘文件里就是明文；字段加密后同一列在文件里找不到明文。')

    $lines | ForEach-Object { Write-Host "    $_" }
    $lines | Out-File -Encoding utf8NoBOM 'docs\db-encryption-atrest-raw.txt'
    Write-Host '    输出: docs\db-encryption-atrest-raw.txt'
}

Write-Host '=== 完成 ==='
Write-Host '演示说明见 docs\数据库中的加密-演示.md'
