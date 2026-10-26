<#
.SYNOPSIS
    离线编译并运行 com.ds.crypto 下 Rijndael（AES 的推广形式）的 JUnit 5 测试与逐步演示。

.DESCRIPTION
    本机 .m2 仓库不完整时直接 mvn 会去下载整套 Spring 依赖，因此沿用
    scripts/run-fd-tests.ps1 的做法：只用 javac + java，先完成
    “编译 → 跑测试 → 跑演示” 三步，全程不联网。
    依赖 jar 的解析顺序：
      1) target\lib（IDE/Maven 已导出的依赖）
      2) 兜底 D:\maven-repo（本机 Maven 本地仓库）

.EXAMPLE
    .\scripts\run-rijndael-tests.ps1
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

$mainClasses = 'target\rijndael-classes'
$testClasses = 'target\rijndael-test-classes'
New-Item -ItemType Directory -Force -Path $mainClasses, $testClasses | Out-Null

Write-Host '=== 1) 编译主源码 (com.ds.crypto) ==='
& javac -encoding UTF-8 -cp $cp -d $mainClasses src\main\java\com\ds\crypto\*.java
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host '=== 2) 编译测试源码 ==='
& javac -encoding UTF-8 -cp "$cp;$mainClasses" -d $testClasses src\test\java\com\ds\crypto\*.java
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host '=== 3) 跑 JUnit 5 ==='
& java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
    -cp "$cp;$mainClasses;$testClasses" com.ds.crypto.RijndaelTestRunner | Out-Host
$testExit = $LASTEXITCODE
Write-Host "JUNIT_EXIT=$testExit"
if ($testExit -ne 0) { exit $testExit }

Write-Host '=== 4) 跑 Rijndael 加密/解密逐步演示 ==='
& java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
    -cp "$mainClasses" com.ds.crypto.RijndaelDemo | Out-Host
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host '=== 完成 ==='
