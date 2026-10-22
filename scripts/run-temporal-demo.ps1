<#
.SYNOPSIS
    离线编译并运行 com.ds.temporal 的双时态演示 + JUnit 5 测试。

.DESCRIPTION
    与 scripts\run-fd-tests.ps1 同一套路：本机 .m2 仓库不存在（工程由 IDE 构建），直接 mvn 会去下载
    整套 Spring 依赖。本脚本从已有位置解析 JUnit jar：
      1) 优先 target\lib（IDE/Maven 已导出的依赖）
      2) 兜底 D:\maven-repo（本机 Maven 本地仓库）
    只用 javac + java 即可完成"编译 → 跑测试 → 跑演示"，全程不联网。

    演示对应文档 docs\时态数据建模-一个具体例子.md；SQL 版见
    src\main\resources\sql\temporal_modeling_demo.sql（用 .\psql.bat -f 跑）。

.EXAMPLE
    .\scripts\run-temporal-demo.ps1
#>
[CmdletBinding()]
param(
    [string]$AltRepo = "D:\maven-repo"
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Resolve-Jar([string]$name) {
    $dir = Join-Path $root 'target\lib'
    if (Test-Path $dir) {
        $hit = Get-ChildItem -Path $dir -Filter "$name-*.jar" -ErrorAction SilentlyContinue |
               Where-Object { $_.Name -notmatch 'sources|javadoc' } | Select-Object -First 1
        if ($hit) { return $hit.FullName }
    }
    if (Test-Path $AltRepo) {
        $hit = Get-ChildItem -Path $AltRepo -Recurse -Filter "$name-*.jar" -ErrorAction SilentlyContinue |
               Where-Object { $_.Name -notmatch 'sources|javadoc' } |
               Sort-Object FullName | Select-Object -First 1
        if ($hit) { return $hit.FullName }
    }
    throw "找不到依赖 jar: $name（target\lib 与 $AltRepo 都没有）"
}

$jars = @(
    'junit-jupiter-api', 'junit-jupiter-engine', 'junit-platform-commons',
    'junit-platform-engine', 'junit-platform-launcher', 'opentest4j', 'apiguardian-api'
) | ForEach-Object { Resolve-Jar $_ }

$cp = ($jars -join ';')
Write-Host ("依赖 jar 数: " + $jars.Count)

$mainClasses = 'target\temporal-classes'
$testClasses = 'target\temporal-test-classes'
New-Item -ItemType Directory -Force -Path $mainClasses, $testClasses | Out-Null

Write-Host '=== 1) 编译主源码 (com.ds.temporal) ==='
& javac -encoding UTF-8 -cp $cp -d $mainClasses src\main\java\com\ds\temporal\*.java
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host '=== 2) 编译测试源码 ==='
& javac -encoding UTF-8 -cp "$cp;$mainClasses" -d $testClasses src\test\java\com\ds\temporal\*.java
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host '=== 3) 跑 JUnit 5 ==='
& java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
    -cp "$cp;$mainClasses;$testClasses" com.ds.temporal.TemporalTestRunner | Out-Host
$testExit = $LASTEXITCODE
Write-Host "JUNIT_EXIT=$testExit"
if ($testExit -ne 0) { exit $testExit }

Write-Host '=== 4) 跑双时态演示 ==='
& java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
    -cp $mainClasses com.ds.temporal.TemporalDemo
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host '=== 完成 ==='
