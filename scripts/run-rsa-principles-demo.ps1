<#
.SYNOPSIS
    离线编译并运行 RSA 原理演示：从 61×53 的手算，到真实 2048 位密钥。

.DESCRIPTION
    本机 .m2 仓库不完整，直接 mvn 会去下载整套 Spring 依赖，因此沿用
    scripts/run-rijndael-tests.ps1 的做法：只用 JDK 自带的 javac / java，全程不联网。

    演示内容（每一步都打印中间结果）：
      1) 小素数全流程：n、φ(n)、e、d、加密、解密
      2) 欧拉定理：为什么 c^d 能还原 m
      3) m 与 n 不互素时为什么 RSA 仍然正确（中国剩余定理）
      4) 扩展欧几里得求 d
      5) 真实 2048 位：密钥生成耗时、CRT 加速实测
      6) 知道 φ(n) 就能算出 d：安全性落在"分解 n 难"
      7) 加解密往返，以及签名只是把指数换个方向

    原始输出 -> docs\rsa-principles-raw.txt（自检失败时脚本退出码非 0）

.EXAMPLE
    .\scripts\run-rsa-principles-demo.ps1
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
New-Item -ItemType Directory -Force -Path docs | Out-Null

# ---------- 定位 JDK（java 可能在 javapath 垫片里，不能靠 Get-Command 的父目录） ----------
$javaHome = (& java -XshowSettings:properties -version 2>&1 |
             Select-String '^\s*java\.home\s*=' |
             Select-Object -First 1).ToString() -replace '.*=\s*', ''
if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome 'bin\javac.exe'))) {
    throw "定位不到 JDK：java.home=$javaHome"
}
Write-Host "JDK: $javaHome"

$javac = Join-Path $javaHome 'bin\javac.exe'
$java = Join-Path $javaHome 'bin\java.exe'

$classes = 'target\rsa-classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null

Write-Host '=== 1) 编译 RSA 原理演示 ==='
& $javac -encoding UTF-8 --release 8 -d $classes `
    src\main\java\com\ds\crypto\RsaFromScratchDemo.java
if ($LASTEXITCODE -ne 0) { throw "javac 失败，退出码 $LASTEXITCODE" }

Write-Host '=== 2) 运行演示（7 段，末尾自检） ==='
$out = & $java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
    -cp $classes com.ds.crypto.RsaFromScratchDemo 2>&1
$code = $LASTEXITCODE
$out | Out-File -Encoding utf8NoBOM 'docs\rsa-principles-raw.txt'
Write-Host "    退出码: $code；输出: docs\rsa-principles-raw.txt（$(($out | Measure-Object -Line).Lines) 行）"
if ($code -ne 0) {
    $out | Select-Object -Last 40
    throw "RSA 原理演示自检失败，退出码 $code（见 docs\rsa-principles-raw.txt）"
}

Write-Host '=== 完成 ==='
Write-Host '讲解见 docs\RSA-原理-从手算到2048位.md'
