<#
.SYNOPSIS
    演示"数据库查询优化的全过程": 把教材图 11-1 的流水线在 PostgreSQL 上真跑一遍。

.DESCRIPTION
    场景取《数据库系统概念》(第6版) 11.5.1 的那个查询:

        Pi_{name,title} ( sigma_{dept_name="Music"} (
            instructor 自然连接 ( teaches 自然连接 Pi_{course_id,title}(course) ) ) )

    对应 SQL:

        SELECT name, title
        FROM instructor, teaches, course
        WHERE instructor.id = teaches.id
          AND teaches.course_id = course.course_id
          AND instructor.dept_name = 'Music';

    脚本只建/删自己的 query_opt_demo 演示库，不碰 university 等既有库。
    演示库把三张表放大到 20000 教师 / 4000 课程 / 200000 选课记录，
    否则小表上所有计划一样快，看不出优化的意义。

    分七段对应图 11-1 的各个框:
      1) 统计值      —— 优化器眼里的世界 (pg_class / pg_stats)
      2) 逻辑优化    —— 选择下推、投影下推在计划里的落点
      3) 物理计划    —— 最终选中的计划 + 真实执行数据
      4) 备选计划    —— 禁掉 hash join / merge join 后的代价对比
      5) 索引影响    —— 建了索引也不一定走
      6) 投影不能省  —— 书上"course 上 (course_id,title) 的投影是必需的"
      7) 统计值失真  —— 估算错，计划就错

    输出同时留档到 docs/query-optimizer-demo-raw.txt。

.EXAMPLE
    .\scripts\run-query-optimizer-demo.ps1
#>
[CmdletBinding()]
param(
    [string]$PgHost = '127.0.0.1',
    [int]$PgPort = 0,
    [string]$PgUser = '',
    [string]$PgPassword = '',
    [string]$PgDatabase = 'query_opt_demo'
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
$common = @('-U', $PgUser, '-h', $PgHost, '-p', "$PgPort", '-X', '-q')

Write-Host "== 演示库: $PgDatabase @ $PgHost`:$PgPort（只建/删这一个库）"

# ---------- 重建演示库 ----------
& $psql @common -d postgres -c "DROP DATABASE IF EXISTS $PgDatabase WITH (FORCE);" | Out-Null
& $psql @common -d postgres -c "CREATE DATABASE $PgDatabase;" | Out-Null

# ---------- 跑 SQL 段落 ----------
$sqlFile = Join-Path $root 'src\main\resources\sql\query_optimizer_demo.sql'
Write-Host "== 执行 $sqlFile"
$out = & $psql @common -d $PgDatabase -v ON_ERROR_STOP=1 -f $sqlFile 2>&1 | Out-String
if ($LASTEXITCODE -ne 0) {
    Write-Host $out
    throw "SQL 执行失败（psql 退出码 $LASTEXITCODE），见上方输出"
}
Write-Host $out

# ---------- 留档 ----------
$rawPath = 'docs\query-optimizer-demo-raw.txt'
$header = @(
    "查询优化全过程演示 —— 真实运行输出",
    "生成时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
    "PostgreSQL: $((& $psql @common -d $PgDatabase -Atc 'SHOW server_version').Trim())",
    "演示库: $PgDatabase",
    "规模: instructor 20000 / course 4000 / teaches 200000",
    "复现: .\scripts\run-query-optimizer-demo.ps1"
) -join "`r`n"

Set-Content -Path $rawPath -Value ($header + "`r`n" + $out) -Encoding utf8
Write-Host ''
Write-Host "== 原始输出已留档: $rawPath" -ForegroundColor Green
exit 0
