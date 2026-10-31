<#
.SYNOPSIS
    离线编译并运行"数字签名在 university 项目中的应用"PoC：成绩单签名 + 审计日志哈希链。

.DESCRIPTION
    不依赖 Spring、不改动现有工程，只用 JDK 自带的 javac / java / keytool：

      1) 用 keytool 生成一张专用的「数字签名用途」证书（KU=digitalSignature,nonRepudiation，
         与 HTTPS 用的 keystore\university.p12 分开）；
      2) 构造一份模拟成绩单（对应 TranscriptVO 的字段），做规范化 JSON；
      3) SHA-256 + RSA-PSS 签名（JDK 8 环境自动回退 SHA256withRSA）；
      4) 验证：原件通过；改 GPA / 改姓名 / 改签名 -> 全拒绝；
      5) 换成 HTTPS 证书的公钥 -> 拒绝（证明"专钥专用"）；
      6) 审计日志哈希链：改任意一条历史记录，重算链对不上链尾签名。

    原始输出 -> docs\university-signature-raw.txt（自检失败时退出码非 0）

.EXAMPLE
    .\scripts\run-university-signature-demo.ps1
    .\scripts\run-university-signature-demo.ps1 -ForceNewKey
#>
[CmdletBinding()]
param(
    [switch]$ForceNewKey,
    [string]$StorePass = 'changeit'
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
New-Item -ItemType Directory -Force -Path docs | Out-Null

# ---------- 定位 JDK ----------
$javaHome = (& java -XshowSettings:properties -version 2>&1 |
             Select-String '^\s*java\.home\s*=' |
             Select-Object -First 1).ToString() -replace '.*=\s*', ''
if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome 'bin\javac.exe'))) {
    throw "定位不到 JDK：java.home=$javaHome"
}
Write-Host "JDK: $javaHome"
$javac = Join-Path $javaHome 'bin\javac.exe'
$java = Join-Path $javaHome 'bin\java.exe'
$keytool = Join-Path $javaHome 'bin\keytool.exe'

$classes = 'target\sig-classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null

# ---------- 1) 生成签名专用证书 ----------
$signingStore = 'target\university-signing.p12'
$alias = 'transcript-signer'
if ($ForceNewKey -and (Test-Path $signingStore)) {
    Remove-Item $signingStore -Force
}
if (-not (Test-Path $signingStore)) {
    Write-Host '=== 1) keytool：生成签名专用证书（KU=digitalSignature,nonRepudiation） ==='
    # JDK 工具在中文 Windows 上按系统代码页输出，混进 UTF-8 日志会乱码；只在这一段强制英文
    $env:JAVA_TOOL_OPTIONS = '-Duser.language=en -Duser.country=US'
    $out = & $keytool -genkeypair -alias $alias -keyalg RSA -keysize 2048 -validity 365 `
        -storetype PKCS12 -keystore $signingStore -storepass $StorePass `
        -dname "CN=University Demo Transcript Signer, OU=Academic Affairs, O=University Demo, L=Chengdu, ST=Sichuan, C=CN" `
        -ext "ku=digitalSignature,nonRepudiation" 2>&1
    Remove-Item Env:JAVA_TOOL_OPTIONS
    $out = $out | Where-Object { $_ -notmatch '^Picked up JAVA_TOOL_OPTIONS' }
    $out | ForEach-Object { Write-Host "    $_" }
    if ($LASTEXITCODE -ne 0) { throw "keytool 生成签名证书失败：`n$($out -join "`n")" }
} else {
    Write-Host '=== 1) 复用已有签名证书 target\university-signing.p12（-ForceNewKey 可重生成） ==='
}

# ---------- 2) 编译 ----------
Write-Host '=== 2) 编译 PoC ==='
& $javac -encoding UTF-8 --release 8 -d $classes `
    src\main\java\com\ds\crypto\Hex.java `
    src\main\java\com\ds\crypto\UniversitySignatureDemo.java
if ($LASTEXITCODE -ne 0) { throw "javac 失败，退出码 $LASTEXITCODE" }

# ---------- 3) 运行 ----------
Write-Host '=== 3) 运行：成绩单签名 + 验证 + 审计哈希链 ==='
$runArgs = @(
    '-Dfile.encoding=UTF-8', '-Dstdout.encoding=UTF-8',
    "-Dkeystore=$signingStore", "-Dstorepass=$StorePass", "-Dalias=$alias",
    '-Dtlsp12=keystore\university.p12',
    '-cp', $classes, 'com.ds.crypto.UniversitySignatureDemo'
)
$out = & $java @runArgs 2>&1
$code = $LASTEXITCODE
$out | Out-File -Encoding utf8NoBOM 'docs\university-signature-raw.txt'
Write-Host "    退出码: $code；输出: docs\university-signature-raw.txt（$(($out | Measure-Object -Line).Lines) 行）"
if ($code -ne 0) {
    $out | Select-Object -Last 40
    throw "PoC 自检失败，退出码 $code（见 docs\university-signature-raw.txt）"
}

Write-Host '=== 完成 ==='
Write-Host '落地建议见 docs\数字签名-在university项目里怎么用.md'
