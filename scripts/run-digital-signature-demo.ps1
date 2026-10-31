<#
.SYNOPSIS
    离线编译并运行"数字签名"演示：Java 端 7 段自检 + keytool/jarsigner 真实签名链。

.DESCRIPTION
    本机 .m2 仓库不完整，直接 mvn 会去下载整套 Spring 依赖，因此沿用
    scripts/run-rijndael-tests.ps1 的做法：只用 JDK 自带的 javac / java，全程不联网。

    1) 编译 src/main/java/com/ds/crypto/{Hex,DigitalSignatureDemo}.java（--release 8，与项目一致）
    2) 运行 DigitalSignatureDemo：
         最小闭环（签名/验签/三种篡改）、教科书 RSA 的两种伪造、四种算法实测、
         "私钥加密"与签名的区别、签名 vs HMAC、随机数复用泄漏私钥、公钥替换、重放
       原始输出 -> docs\digital-signature-raw.txt（自检失败时脚本退出码非 0）
    3) 真实工具链（JDK 自带）：
         keytool 生成 RSA 2048 自签证书（PKCS12） -> jar 打包 -> jarsigner 签名
         -> jarsigner -verify 通过 -> 篡改 jar 里的一个 class -> 再验证被拒绝
       原始输出 -> docs\digital-signature-jar-raw.txt

.EXAMPLE
    .\scripts\run-digital-signature-demo.ps1
    .\scripts\run-digital-signature-demo.ps1 -SkipJar
#>
[CmdletBinding()]
param(
    [switch]$SkipJar,
    [string]$StorePass = 'changeit'
)

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

$mainClasses = 'target\sig-classes'
New-Item -ItemType Directory -Force -Path $mainClasses | Out-Null

# ---------- 1) 编译 ----------
Write-Host '=== 1) 编译 com.ds.crypto 下的数字签名演示 ==='
& $javac -encoding UTF-8 --release 8 -d $mainClasses `
    src\main\java\com\ds\crypto\Hex.java `
    src\main\java\com\ds\crypto\DigitalSignatureDemo.java `
    src\main\java\com\ds\crypto\TextbookSignatureDemo.java
if ($LASTEXITCODE -ne 0) { throw "javac 失败，退出码 $LASTEXITCODE" }

# ---------- 2) Java 演示 ----------
Write-Host '=== 2) 运行 Java 演示（7 段，末尾自检） ==='
$demoOut = & $java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
    -cp $mainClasses com.ds.crypto.DigitalSignatureDemo 2>&1
$demoExit = $LASTEXITCODE
$demoOut | Out-File -Encoding utf8NoBOM 'docs\digital-signature-raw.txt'
$demoOut | Select-Object -Last 2 | Format-Table -HideTableHeaders | Out-Null
Write-Host "    退出码: $demoExit；输出: docs\digital-signature-raw.txt（$(($demoOut | Measure-Object -Line).Lines) 行）"
if ($demoExit -ne 0) {
    $demoOut | Select-Object -Last 40
    throw "Java 演示自检失败，退出码 $demoExit（见 docs\digital-signature-raw.txt）"
}

# ---------- 2b) 照教材字面实现的对照演示 ----------
Write-Host '=== 2b) 运行"照教材 9.8.3.1 字面实现"的对照演示 ==='
$textbookOut = & $java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
    -cp $mainClasses com.ds.crypto.TextbookSignatureDemo 2>&1
$textbookExit = $LASTEXITCODE
$textbookOut | Out-File -Encoding utf8NoBOM 'docs\textbook-signature-raw.txt'
Write-Host "    退出码: $textbookExit；输出: docs\textbook-signature-raw.txt（$(($textbookOut | Measure-Object -Line).Lines) 行）"
if ($textbookExit -ne 0) {
    $textbookOut | Select-Object -Last 40
    throw "教材版对照演示自检失败，退出码 $textbookExit（见 docs\textbook-signature-raw.txt）"
}

if ($SkipJar) {
    Write-Host '=== 已跳过 keytool/jarsigner 段（-SkipJar） ==='
    return
}

# ---------- 3) keytool + jarsigner 真实签名链 ----------
Write-Host '=== 3) keytool + jarsigner：真实 JAR 签名链 ==='

$keytool = Join-Path $javaHome 'bin\keytool.exe'
$jarTool = Join-Path $javaHome 'bin\jar.exe'
$jarsigner = Join-Path $javaHome 'bin\jarsigner.exe'
foreach ($tool in @($keytool, $jarTool, $jarsigner)) {
    if (-not (Test-Path $tool)) { throw "JDK 里找不到 $tool" }
}

# JDK 17 在中文 Windows 上按系统代码页输出，混进 UTF-8 文件会变乱码；
# 这几个工具只需要英文输出，用 JAVA_TOOL_OPTIONS 强制 en_US，输出全是 ASCII。
$env:JAVA_TOOL_OPTIONS = '-Duser.language=en -Duser.country=US'
$log = New-Object System.Collections.Generic.List[string]
function Add-Log([string[]]$lines) {
    foreach ($line in $lines) {
        if ($line -notmatch '^Picked up JAVA_TOOL_OPTIONS') { $log.Add($line) }
    }
}
function Add-Title([string]$text) {
    $log.Add('')
    $log.Add('==================================================================')
    $log.Add($text)
    $log.Add('==================================================================')
}

$work = 'target\sig-demo'
if (Test-Path $work) { Remove-Item $work -Recurse -Force }
New-Item -ItemType Directory -Force -Path $work | Out-Null

$keystore = Join-Path $work 'demo-signer.p12'
$alias = 'demo-signer'
$appJar = Join-Path $work 'demo-app.jar'
$tamperedJar = Join-Path $work 'demo-app-tampered.jar'

# 3.1 生成密钥 + 自签证书
Add-Title '1) keytool -genkeypair：RSA 2048 + 自签 X.509 证书（PKCS12）'
$out = & $keytool -genkeypair -alias $alias -keyalg RSA -keysize 2048 -sigalg SHA256withRSA `
    -validity 365 `
    -dname "CN=Demo Signer, OU=Alg4 Demo, O=Local Only, L=Shanghai, ST=Shanghai, C=CN" `
    -storetype PKCS12 -keystore $keystore -storepass $StorePass 2>&1
Add-Log $out
if ($LASTEXITCODE -ne 0) { throw "keytool 生成密钥失败：`n$($out -join "`n")" }

# 3.2 打包并签名
Add-Title '2) jar 打包 + jarsigner -sign'
$out = & $jarTool cf $appJar -C $mainClasses . 2>&1
Add-Log $out
if ($LASTEXITCODE -ne 0) { throw "jar 打包失败：`n$($out -join "`n")" }

$out = & $jarsigner -keystore $keystore -storepass $StorePass `
    -sigalg SHA256withRSA -digestalg SHA-256 $appJar $alias 2>&1
Add-Log $out
if ($LASTEXITCODE -ne 0) { throw "jarsigner 签名失败：`n$($out -join "`n")" }

$log.Add("jar 大小: $((Get-Item $appJar).Length) 字节")

# 3.3 验签
Add-Title '3) jarsigner -verify：签名通过'
$out = & $jarsigner -verify -certs $appJar 2>&1
$verifyExit = $LASTEXITCODE
Add-Log $out
Add-Log @("jarsigner -verify 退出码: $verifyExit")
if ($verifyExit -ne 0) { throw "正常 jar 验签居然失败：`n$($out -join "`n")" }
Write-Host '    正常 jar：验签通过（退出码 0）'

# 3.4 看证书
Add-Title '4) keytool -printcert -jarfile：签名者证书与指纹'
$out = & $keytool -printcert -jarfile $appJar 2>&1
Add-Log $out

# 3.5 看签名后的 jar 内部结构
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Read-JarEntry([string]$jarPath, [string]$entryName) {
    $zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $jarPath))
    try {
        $entry = $zip.Entries | Where-Object { $_.FullName -eq $entryName } | Select-Object -First 1
        if (-not $entry) { return $null }
        $reader = New-Object IO.StreamReader($entry.Open())
        try { return $reader.ReadToEnd() } finally { $reader.Close() }
    } finally { $zip.Dispose() }
}

Add-Title '5) 签名后的 jar 内部结构：MANIFEST 的逐条摘要 + .SF 的总摘要'
$manifest = Read-JarEntry $appJar 'META-INF/MANIFEST.MF'
$log.Add('META-INF/MANIFEST.MF：每个 entry 一行 SHA-256 摘要')
Add-Log (($manifest -split "\r?\n") | Select-Object -First 14)
$sfName = ((& $jarTool tf $appJar 2>$null) | Where-Object { $_ -match '^META-INF/.*\.SF$' } | Select-Object -First 1)
$log.Add('')
$log.Add("$sfName：对 MANIFEST 的总摘要，随后被 ${alias} 的私钥签名成 .RSA")
if ($sfName) {
    $sf = Read-JarEntry $appJar $sfName
    Add-Log (($sf -split "\r?\n") | Select-Object -First 8)
}

# 3.6 篡改一个 class 后重新验签
Add-Title '6) 篡改 jar 里的 com/ds/crypto/Hex.class 一个字节后重新验签'
Copy-Item $appJar $tamperedJar -Force
$stage = Join-Path $work 'tamper-stage'
$stageDir = Join-Path $stage 'com\ds\crypto'
New-Item -ItemType Directory -Force -Path $stageDir | Out-Null
$orig = [IO.File]::ReadAllBytes((Join-Path $mainClasses 'com\ds\crypto\Hex.class'))
$mutated = New-Object byte[] ($orig.Length + 1)
[Array]::Copy($orig, $mutated, $orig.Length)
[IO.File]::WriteAllBytes((Join-Path $stageDir 'Hex.class'), $mutated)

$out = & $jarTool uf $tamperedJar -C $stage com/ds/crypto/Hex.class 2>&1
Add-Log $out
if ($LASTEXITCODE -ne 0) { throw "jar 更新失败：`n$($out -join "`n")" }

$out = & $jarsigner -verify $tamperedJar 2>&1
$tamperedExit = $LASTEXITCODE
Add-Log $out
Add-Log @("jarsigner -verify（篡改后）退出码: $tamperedExit")

if ($tamperedExit -eq 0) {
    throw "篡改后的 jar 居然验签通过，签名链演示失败（见 docs\digital-signature-jar-raw.txt）"
}
Write-Host "    篡改后：验签失败（退出码 $tamperedExit）—— 一个字节都改不得"

$log | Out-File -Encoding utf8NoBOM 'docs\digital-signature-jar-raw.txt'
Remove-Item Env:JAVA_TOOL_OPTIONS
Write-Host '    输出: docs\digital-signature-jar-raw.txt'

Write-Host '=== 完成 ==='
Write-Host '讲解见 docs\数字签名-讲解与演示.md'
