<#
.SYNOPSIS
    离线编译并运行 com.ds.fd 下的 JUnit 5 测试 + FD 闭包演示。

.DESCRIPTION
    本机 .m2 仓库不存在（工程由 IDE 构建），直接 mvn 会去下载整套 Spring 依赖。
    本脚本改为从已有位置解析 jar：
      1) 优先 target\lib（IDE/Maven 已导出的依赖）
      2) 兜底 D:\maven-repo（本机 Maven 本地仓库）
    只用 javac + java 即可完成"编译 → 跑测试 → 跑演示"，全程不联网。

.EXAMPLE
    .\scripts\run-fd-tests.ps1

.EXAMPLE
    # Temporarily enlarge the randomized differential rounds of the canonical-cover
    # property test (default 300 rounds per attribute pool).
    .\scripts\run-fd-tests.ps1 -CoverRounds 2000
#>
[CmdletBinding()]
param(
    [string]$AltRepo = "D:\maven-repo",

    # Rounds for the canonical-cover random differential; 0 = use the test class default
    # (which reads -Dfd.cover.rounds). Passed to java as -Dfd.cover.rounds=<n>.
    [int]$CoverRounds = 0
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

$mainClasses = 'target\fd-classes'
$testClasses = 'target\fd-test-classes'
New-Item -ItemType Directory -Force -Path $mainClasses, $testClasses | Out-Null

Write-Host '=== 1) 编译主源码 (com.ds.fd) ==='
& javac -encoding UTF-8 -cp $cp -d $mainClasses src\main\java\com\ds\fd\*.java
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host '=== 2) 编译测试源码 ==='
& javac -encoding UTF-8 -cp "$cp;$mainClasses" -d $testClasses src\test\java\com\ds\fd\*.java
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host '=== 3) 跑 JUnit 5 ==='
$runner = 'com.ds.fd.FdTestRunner'
$runnerFile = Join-Path $testClasses 'com\ds\fd\FdTestRunner.class'
if (-not (Test-Path $runnerFile)) {
    Write-Host "  (未编译出 $runner, 跳过测试执行 —— 请检查 src\test\java\com\ds\fd\FdTestRunner.java)"
} else {
    $extra = @()
    if ($CoverRounds -gt 0) { $extra += "-Dfd.cover.rounds=$CoverRounds" }
    & java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' @extra `
        -cp "$cp;$mainClasses;$testClasses" $runner | Out-Host
    $testExit = $LASTEXITCODE
    Write-Host "JUNIT_EXIT=$testExit"
    if ($testExit -ne 0) { exit $testExit }
}

Write-Host '=== 4) 跑 FD 闭包演示 ==='
& java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
    -cp "$cp;$mainClasses" com.ds.fd.FdClosureDemo
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host '=== 5) 批量用例体检（两种算法对账） ==='
$report = 'target\fd-report\fd-report.txt'
& java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
    -cp $mainClasses com.ds.fd.FdCrossCheck 'src\main\resources\fd\fd-cases.txt' $report | Out-Host
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Get-Content $report -Encoding UTF8 | ForEach-Object { Write-Host ("  " + $_) }

Write-Host '=== 完成 ==='
