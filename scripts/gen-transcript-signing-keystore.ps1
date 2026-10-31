<#
.SYNOPSIS
    生成"成绩单签名"专用证书（PKCS12），与 HTTPS 证书分开，专钥专用。

.DESCRIPTION
    输出 keystore\transcript-signing.p12：
      - RSA 2048 位、有效期 10 年、自签；
      - KeyUsage = digitalSignature,nonRepudiation（签名用途，不能用于 TLS）；
      - 别名 transcript-signer、口令 changeit（生产请改口令并用环境变量注入）。

    该文件含私钥，已加入 .gitignore，禁止提交；生产环境应改用 KMS/HSM。

.EXAMPLE
    .\scripts\gen-transcript-signing-keystore.ps1
    .\scripts\gen-transcript-signing-keystore.ps1 -Force
#>
[CmdletBinding()]
param(
    [string]$StorePass = 'changeit',
    [string]$Alias = 'transcript-signer',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$keystoreDir = Join-Path $root 'keystore'
$keystore = Join-Path $keystoreDir 'transcript-signing.p12'

if ((Test-Path $keystore) -and -not $Force) {
    Write-Host "已存在：$keystore（如需重新生成加 -Force）"
    exit 0
}
if (Test-Path $keystore) { Remove-Item $keystore -Force }
New-Item -ItemType Directory -Force -Path $keystoreDir | Out-Null

$javaHome = (& java -XshowSettings:properties -version 2>&1 |
             Select-String '^\s*java\.home\s*=' |
             Select-Object -First 1).ToString() -replace '.*=\s*', ''
$keytool = Join-Path $javaHome 'bin\keytool.exe'
if (-not (Test-Path $keytool)) { throw "找不到 keytool：$keytool" }

$env:JAVA_TOOL_OPTIONS = '-Duser.language=en -Duser.country=US'
$out = & $keytool -genkeypair -alias $Alias -keyalg RSA -keysize 2048 -validity 3650 `
    -storetype PKCS12 -keystore $keystore -storepass $StorePass `
    -dname "CN=University Demo Transcript Signer, OU=Academic Affairs, O=University Demo, L=Chengdu, ST=Sichuan, C=CN" `
    -ext "ku=digitalSignature,nonRepudiation" 2>&1
Remove-Item Env:JAVA_TOOL_OPTIONS
$out = $out | Where-Object { $_ -notmatch '^Picked up JAVA_TOOL_OPTIONS' }
$out | ForEach-Object { Write-Host "  $_" }
if ($LASTEXITCODE -ne 0) { throw "keytool 生成签名证书失败" }

Write-Host "证书已生成：$keystore"
Write-Host "别名：$Alias，口令：$StorePass"
Write-Host "启动应用后可用 /verify 验证；生产请改口令并把它指到 KMS/HSM 或加密密钥库。"
