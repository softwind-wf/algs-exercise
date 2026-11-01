# ============================================================
# 把 CA / certbot 交付的 PEM 转成 Spring Boot 能直接读的 PKCS12
#
# 输入（Let's Encrypt / 云厂商下载的 nginx 格式就是这两个文件）：
#   fullchain.pem  —— 叶子证书 + 中间证书（必须带全，缺了中间证书客户端会报
#                     PKIX path building failed）
#   privkey.pem    —— 私钥（PKCS#8 或 PKCS#1 都行）
# 输出：
#   keystore/university.p12（别名 university），配合 application-tls.yml 的
#   KEYSTORE_PATH / KEYSTORE_PASSWORD / KEYSTORE_ALIAS 使用
#
# 优先用 openssl；本机没有 openssl 时退回纯 JDK 实现（src/main/java/com/ds/crypto/PemToPkcs12.java），
# 两者产物等价。
#
# 用法：
#   .\scripts\cert-to-pkcs12.ps1 -FullChain C:\certs\fullchain.pem -PrivateKey C:\certs\privkey.pem
#   .\scripts\cert-to-pkcs12.ps1 -FullChain ... -PrivateKey ... -Out keystore/prod.p12 -Alias university
# ============================================================
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$FullChain,
    [Parameter(Mandatory = $true)][string]$PrivateKey,
    [string]$Out = 'keystore/university.p12',
    [string]$Password,
    [string]$Alias = 'university'
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path $FullChain)) { throw "找不到证书链文件：$FullChain" }
if (-not (Test-Path $PrivateKey)) { throw "找不到私钥文件：$PrivateKey" }
if (-not $Password) {
    $secure = Read-Host -AsSecureString "给 $Out 设置口令（Spring Boot 的 KEYSTORE_PASSWORD）"
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}
if ($Password.Length -lt 6) { throw '口令至少 6 位（PKCS12 与小工具都会拒绝更短的口令）' }

$outFull = [IO.Path]::GetFullPath((Join-Path $root $Out))
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $outFull) | Out-Null

# ---------- 1) 优先 openssl ----------
$openssl = (Get-Command openssl -ErrorAction SilentlyContinue).Source
if (-not $openssl) {
    foreach ($candidate in @(
            'D:\OpenSSL-Win64\bin\openssl.exe',
            'D:\Git\usr\bin\openssl.exe',
            'D:\Git\mingw64\bin\openssl.exe',
            'C:\Program Files\Git\usr\bin\openssl.exe',
            'C:\Program Files\OpenSSL-Win64\bin\openssl.exe',
            'C:\Program Files\Docker\Docker\resources\bin\openssl.exe')) {
        if (Test-Path $candidate) { $openssl = $candidate; break }
    }
}

if ($openssl) {
    Write-Host "[openssl] $openssl"
    # 这个 Windows 版 OpenSSL 编译进去的默认配置路径（C:\Program Files\Common Files\SSL）不存在，
    # 空 OPENSSL_CONF 表示"不加载配置文件"，否则 pkcs12 -export 会直接失败。
    $previousConf = $env:OPENSSL_CONF
    $env:OPENSSL_CONF = ''
    try {
        & $openssl pkcs12 -export -in $FullChain -inkey $PrivateKey `
            -name $Alias -out $outFull -passout "pass:$Password" 2>&1 | ForEach-Object { Write-Host "  $_" }
        if ($LASTEXITCODE -ne 0) { throw "openssl 导出 PKCS12 失败（退出码 $LASTEXITCODE）" }
    } finally {
        if ($previousConf) { $env:OPENSSL_CONF = $previousConf }
        else { Remove-Item Env:OPENSSL_CONF -ErrorAction SilentlyContinue }
    }
} else {
    # ---------- 2) 回退纯 JDK ----------
    Write-Host '[jdk] 本机没有 openssl，改用纯 JDK 实现 PemToPkcs12'
    $javaHome = (& java -XshowSettings:properties -version 2>&1 |
                 Select-String '^\s*java\.home\s*=' |
                 Select-Object -First 1).ToString() -replace '.*=\s*', ''
    $javac = Join-Path $javaHome 'bin\javac.exe'
    $java = Join-Path $javaHome 'bin\java.exe'
    $classes = 'target\cert-classes'
    New-Item -ItemType Directory -Force -Path $classes | Out-Null
    & $javac -encoding UTF-8 --release 8 -d $classes `
        src\main\java\com\ds\crypto\Hex.java `
        src\main\java\com\ds\crypto\Der.java `
        src\main\java\com\ds\crypto\PemToPkcs12.java
    if ($LASTEXITCODE -ne 0) { throw "javac 失败，退出码 $LASTEXITCODE" }
    # 先收进变量再打印，避免原生进程 stdout 缓冲导致输出顺序错乱
    $toolOut = & $java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -cp $classes com.ds.crypto.PemToPkcs12 `
        $FullChain $PrivateKey $outFull $Password $Alias
    $toolExit = $LASTEXITCODE
    $toolOut | ForEach-Object { Write-Host "  $_" }
    if ($toolExit -ne 0) { throw "PemToPkcs12 失败，退出码 $toolExit" }
}

# ---------- 3) 复核：链长、SAN、有效期 ----------
$javaHome = (& java -XshowSettings:properties -version 2>&1 |
             Select-String '^\s*java\.home\s*=' |
             Select-Object -First 1).ToString() -replace '.*=\s*', ''
$keytool = Join-Path $javaHome 'bin\keytool.exe'
if (Test-Path $keytool) {
    Write-Host ''
    Write-Host '=== keytool 复核 ==='
    # JDK 会按系统语言输出；强制英文，输出全是 ASCII，便于在任意控制台/文件里读
    $env:JAVA_TOOL_OPTIONS = '-Duser.language=en -Duser.country=US'
    try {
        $listing = & $keytool -list -v -alias $Alias -keystore $outFull -storepass $Password 2>&1
    } finally {
        Remove-Item Env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
    }
    $listing | Where-Object { $_ -notmatch '^Picked up JAVA_TOOL_OPTIONS' } |
        Select-String 'Entry type|Certificate chain length|^Certificate\[|Owner:|Issuer:|DNSName|IPAddress|Valid from' |
        ForEach-Object { Write-Host "  $($_.Line.Trim())" }
}

Write-Host ''
Write-Host "完成：$outFull（别名 $Alias）"
Write-Host '启动 HTTPS（application-tls.yml 的 tls profile）：'
Write-Host "  `$env:KEYSTORE_PATH='$Out'; `$env:KEYSTORE_PASSWORD='******'; `$env:KEYSTORE_ALIAS='$Alias'"
Write-Host '  java -jar target\algs4-1.0.0.0.jar --spring.profiles.active=prod,tls'
Write-Host '注意：keystore/ 已在 .gitignore 中，私钥不会进 git；但生产证书建议放在 /etc/university/ 并 chmod 600。'


