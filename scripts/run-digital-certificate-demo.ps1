<#
.SYNOPSIS
    离线演示"数字证书"：Java 手搓证书 9 段自检 + keytool 真实证书链 + 真跑 TLS 握手。

.DESCRIPTION
    全程只用 JDK 自带工具（javac / java / keytool），不联网、不引第三方库。

    1) keytool 造一条真实证书链（PKCS12，口令 changeit）：
         root（CA:true，自签） -> intermediate（CA:true, pathLen=0） -> server（SAN=dns:localhost）
       另造 4 张"有问题"的服务器证书，供 TLS 场景对照：
         server-mismatch（SAN 只写 shop.example.com）
         server-expired （2020 年签发、只活 1 天）
         server-otherca （另一家根 CA 签发，客户端不信任）
         server-cnonly  （只有 CN=localhost，没有 SAN）
    2) 编译并运行 com.ds.crypto.CertificateDemo（9 段 32 项自检，手拼 DER 造证书）
       原始输出 -> docs\digital-certificate-raw.txt
    3) 编译并运行 com.ds.crypto.CertChainDemo（读上面生成的真实证书，跑 5 个 TLS 场景）
       原始输出 -> docs\cert-chain-tls-raw.txt

.EXAMPLE
    .\scripts\run-digital-certificate-demo.ps1
    .\scripts\run-digital-certificate-demo.ps1 -SkipTls
#>
[CmdletBinding()]
param(
    [switch]$SkipTls,
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
$javac = Join-Path $javaHome 'bin\javac.exe'
$java = Join-Path $javaHome 'bin\java.exe'
$keytool = Join-Path $javaHome 'bin\keytool.exe'
Write-Host "JDK: $javaHome"

$work = 'target\cert-demo'
$classes = 'target\cert-classes'

function Invoke-Keytool {
    param([string[]]$Arguments, [string]$What)
    $output = & $keytool @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "keytool $What 失败（退出码 $LASTEXITCODE）：`n$($output -join "`n")"
    }
    return ($output | Where-Object { $_ -notmatch '^Picked up JAVA_TOOL_OPTIONS' })
}

if (-not $SkipTls) {
    # ---------- 0) 生成真实证书链 ----------
    Write-Host '=== 0) keytool 生成证书链（root -> intermediate -> server） ==='
    $env:JAVA_TOOL_OPTIONS = '-Duser.language=en -Duser.country=US'

    if (Test-Path $work) { Remove-Item $work -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $work | Out-Null
    $Pass = $StorePass
    $rootStore = Join-Path $work 'root.p12'
    $intStore = Join-Path $work 'intermediate.p12'
    $rootPem = Join-Path $work 'root.pem'
    $intPem = Join-Path $work 'intermediate.pem'

    # 0.1 根 CA（自签，CA:true，可以签证书与吊销列表）
    Write-Host '  0.1 root：自签根 CA'
    $null = Invoke-Keytool @(
        '-genkeypair', '-alias', 'root', '-keyalg', 'RSA', '-keysize', '2048', '-sigalg', 'SHA256withRSA',
        '-validity', '3650', '-dname', 'CN=Alg4 Demo Root CA, O=Alg4 Demo, C=CN',
        '-ext', 'bc:c', '-ext', 'ku:c=keyCertSign,cRLSign',
        '-storetype', 'PKCS12', '-keystore', $rootStore, '-storepass', $Pass, '-keypass', $Pass
    ) '生成根 CA'

    # 0.2 中间 CA 的密钥与 CSR，再由根签发
    Write-Host '  0.2 intermediate：根签发的中间 CA（pathLen=0）'
    $null = Invoke-Keytool @(
        '-genkeypair', '-alias', 'intermediate', '-keyalg', 'RSA', '-keysize', '2048', '-sigalg', 'SHA256withRSA',
        '-dname', 'CN=Alg4 Demo Intermediate CA, O=Alg4 Demo, C=CN',
        '-ext', 'bc=ca:true,pathlen:0', '-ext', 'ku:c=keyCertSign,cRLSign',
        '-storetype', 'PKCS12', '-keystore', $intStore, '-storepass', $Pass, '-keypass', $Pass
    ) '生成中间 CA 密钥'
    $null = Invoke-Keytool @('-certreq', '-alias', 'intermediate', '-keystore', $intStore,
        '-storepass', $Pass, '-file', (Join-Path $work 'intermediate.csr')) '生成中间 CA 的 CSR'
    $null = Invoke-Keytool @('-gencert', '-alias', 'root', '-keystore', $rootStore, '-storepass', $Pass,
        '-infile', (Join-Path $work 'intermediate.csr'), '-outfile', $intPem, '-rfc', '-validity', '1825',
        '-ext', 'bc=ca:true,pathlen:0', '-ext', 'ku:c=keyCertSign,cRLSign') '签发中间 CA'
    $null = Invoke-Keytool @('-exportcert', '-alias', 'root', '-keystore', $rootStore,
        '-storepass', $Pass, '-rfc', '-file', $rootPem) '导出根证书'
    $null = Invoke-Keytool @('-importcert', '-alias', 'root', '-file', $rootPem, '-keystore', $intStore,
        '-storepass', $Pass, '-noprompt') '把根证书装进中间 CA 的信任链'
    $null = Invoke-Keytool @('-importcert', '-alias', 'intermediate', '-file', $intPem, '-keystore', $intStore,
        '-storepass', $Pass, '-noprompt') '把中间 CA 证书装回去（替换自签占位）'

    # 0.3 服务器证书（正确的那张）
    Write-Host '  0.3 server：SAN=dns:localhost,ip:127.0.0.1'
    $serverStore = Join-Path $work 'server.p12'
    $serverPem = Join-Path $work 'server.pem'
    $null = Invoke-Keytool @(
        '-genkeypair', '-alias', 'server', '-keyalg', 'RSA', '-keysize', '2048', '-sigalg', 'SHA256withRSA',
        '-dname', 'CN=localhost, O=Alg4 Demo, OU=Web, C=CN',
        '-ext', 'san=dns:localhost,ip:127.0.0.1', '-ext', 'ku=digitalSignature,keyEncipherment',
        '-ext', 'eku=serverAuth',
        '-storetype', 'PKCS12', '-keystore', $serverStore, '-storepass', $Pass, '-keypass', $Pass
    ) '生成服务器密钥'
    $null = Invoke-Keytool @('-certreq', '-alias', 'server', '-keystore', $serverStore,
        '-storepass', $Pass, '-file', (Join-Path $work 'server.csr')) '生成服务器 CSR'
    $null = Invoke-Keytool @('-gencert', '-alias', 'intermediate', '-keystore', $intStore, '-storepass', $Pass,
        '-infile', (Join-Path $work 'server.csr'), '-outfile', $serverPem, '-rfc', '-validity', '825',
        '-ext', 'san=dns:localhost,ip:127.0.0.1', '-ext', 'ku=digitalSignature,keyEncipherment',
        '-ext', 'eku=serverAuth') '签发服务器证书'

    # 0.4 四个"有问题"的服务器证书，供 TLS 场景对照
    function New-LeafCert {
        param(
            [string]$Alias,
            [string]$Dname,
            [string[]]$Extensions,
            [string]$StartDate,
            [int]$Validity = 825
        )
        Write-Host "  0.4 $Alias"
        $store = Join-Path $work "$Alias.p12"
        $pem = Join-Path $work "$Alias.pem"
        $csr = Join-Path $work "$Alias.csr"
        $null = Invoke-Keytool @(
            '-genkeypair', '-alias', $Alias, '-keyalg', 'RSA', '-keysize', '2048', '-sigalg', 'SHA256withRSA',
            '-dname', $Dname, '-storetype', 'PKCS12', '-keystore', $store, '-storepass', $Pass, '-keypass', $Pass
        ) "生成 $Alias 密钥"
        $null = Invoke-Keytool @('-certreq', '-alias', $Alias, '-keystore', $store,
            '-storepass', $Pass, '-file', $csr) "生成 $Alias 的 CSR"
        $gencertArgs = @('-gencert', '-alias', 'intermediate', '-keystore', $intStore, '-storepass', $Pass,
            '-infile', $csr, '-outfile', $pem, '-rfc', '-validity', "$Validity")
        if ($StartDate) { $gencertArgs += @('-startdate', $StartDate) }
        foreach ($extension in $Extensions) { $gencertArgs += @('-ext', $extension) }
        $null = Invoke-Keytool $gencertArgs "签发 $Alias"
    }

    New-LeafCert -Alias 'server-mismatch' -Dname 'CN=shop.example.com, O=Alg4 Demo, OU=Web, C=CN' `
        -Extensions @('san=dns:shop.example.com', 'ku=digitalSignature,keyEncipherment', 'eku=serverAuth')
    New-LeafCert -Alias 'server-expired' -Dname 'CN=localhost, O=Alg4 Demo, OU=Web, C=CN' `
        -Extensions @('san=dns:localhost', 'ku=digitalSignature,keyEncipherment', 'eku=serverAuth') `
        -StartDate '2020/01/01 00:00:00' -Validity 30
    New-LeafCert -Alias 'server-cnonly' -Dname 'CN=localhost, O=Alg4 Demo, OU=Web, C=CN' `
        -Extensions @('ku=digitalSignature,keyEncipherment', 'eku=serverAuth')

    # 0.5 另一个根 CA + 它签的服务器证书（客户端不信任它）
    Write-Host '  0.5 otherroot + server-otherca：另一家 CA'
    $otherRootStore = Join-Path $work 'otherroot.p12'
    $otherRootPem = Join-Path $work 'otherroot.pem'
    $null = Invoke-Keytool @(
        '-genkeypair', '-alias', 'otherroot', '-keyalg', 'RSA', '-keysize', '2048', '-sigalg', 'SHA256withRSA',
        '-validity', '3650', '-dname', 'CN=Other Demo Root CA, O=Other Org, C=CN',
        '-ext', 'bc:c', '-ext', 'ku:c=keyCertSign,cRLSign',
        '-storetype', 'PKCS12', '-keystore', $otherRootStore, '-storepass', $Pass, '-keypass', $Pass
    ) '生成另一家根 CA'
    $null = Invoke-Keytool @('-exportcert', '-alias', 'otherroot', '-keystore', $otherRootStore,
        '-storepass', $Pass, '-rfc', '-file', $otherRootPem) '导出另一家根证书'
    $otherServerStore = Join-Path $work 'server-otherca.p12'
    $otherServerPem = Join-Path $work 'server-otherca.pem'
    $null = Invoke-Keytool @(
        '-genkeypair', '-alias', 'server', '-keyalg', 'RSA', '-keysize', '2048', '-sigalg', 'SHA256withRSA',
        '-dname', 'CN=localhost, O=Alg4 Demo, OU=Web, C=CN',
        '-ext', 'san=dns:localhost', '-ext', 'ku=digitalSignature,keyEncipherment', '-ext', 'eku=serverAuth',
        '-storetype', 'PKCS12', '-keystore', $otherServerStore, '-storepass', $Pass, '-keypass', $Pass
    ) '生成另一家 CA 的服务器密钥'
    $null = Invoke-Keytool @('-certreq', '-alias', 'server', '-keystore', $otherServerStore,
        '-storepass', $Pass, '-file', (Join-Path $work 'server-otherca.csr')) '生成 server-otherca 的 CSR'
    $null = Invoke-Keytool @('-gencert', '-alias', 'otherroot', '-keystore', $otherRootStore, '-storepass', $Pass,
        '-infile', (Join-Path $work 'server-otherca.csr'), '-outfile', $otherServerPem, '-rfc', '-validity', '825',
        '-ext', 'san=dns:localhost', '-ext', 'ku=digitalSignature,keyEncipherment',
        '-ext', 'eku=serverAuth') '签发 server-otherca'

    # 0.6 把 叶子 + 中间 + 根 拼成链，装回各自的 keystore：
    #     KeyManager 只会发送 keystore 里存的链，不装中间证书客户端就拼不出路径。
    function Install-Chain {
        param([string]$Alias, [string]$LeafPem)
        $store = Join-Path $work "$Alias.p12"
        $chainPem = Join-Path $work "$Alias-chain.pem"
        $text = (Get-Content $LeafPem -Raw) + "`n" + (Get-Content $intPem -Raw) + "`n" + (Get-Content $rootPem -Raw)
        Set-Content -Path $chainPem -Value $text -NoNewline -Encoding ascii
        # 注意：-alias 必须与 keystore 里那把私钥的别名一致，keytool 才会把链装到该私钥条目上；
        # 换成一个新别名导入，只会得到一张 trustedCertEntry（没有私钥，链也丢了）。
        $null = Invoke-Keytool @('-importcert', '-alias', $Alias, '-file', $chainPem, '-keystore', $store,
            '-storepass', $Pass, '-noprompt') "把完整链装进 $Alias.p12"
    }
    Install-Chain -Alias 'server' -LeafPem $serverPem
    Install-Chain -Alias 'server-mismatch' -LeafPem (Join-Path $work 'server-mismatch.pem')
    Install-Chain -Alias 'server-expired' -LeafPem (Join-Path $work 'server-expired.pem')
    Install-Chain -Alias 'server-cnonly' -LeafPem (Join-Path $work 'server-cnonly.pem')

    # server-otherca 认另一家根
    $otherChain = Join-Path $work 'server-otherca-chain.pem'
    Set-Content -Path $otherChain -Encoding ascii -NoNewline `
        -Value ((Get-Content $otherServerPem -Raw) + "`n" + (Get-Content $otherRootPem -Raw))
    $null = Invoke-Keytool @('-importcert', '-alias', 'server', '-file', $otherChain,
        '-keystore', $otherServerStore, '-storepass', $Pass, '-noprompt') '把 server-otherca 的链装好'

    Write-Host '  0.7 证书链自检：keytool 角度看 server.p12'
    $chainView = Invoke-Keytool @('-list', '-v', '-alias', 'server', '-keystore', $serverStore, '-storepass', $Pass) '查看 server 证书链'
    $chainView | Select-String 'Certificate chain length|^Certificate\[|Owner:|Issuer:|DNSName|IPAddress|Subject Alternative' |
        ForEach-Object { Write-Host "      $($_.Line.Trim())" }

    Remove-Item Env:JAVA_TOOL_OPTIONS
    Write-Host "  证书已生成：$work"
}

# ---------- 1) 编译 ----------
Write-Host '=== 1) 编译 com.ds.crypto 下的证书演示 ==='
New-Item -ItemType Directory -Force -Path $classes | Out-Null
& $javac -encoding UTF-8 --release 8 -d $classes `
    src\main\java\com\ds\crypto\Hex.java `
    src\main\java\com\ds\crypto\Der.java `
    src\main\java\com\ds\crypto\CertificateDemo.java `
    src\main\java\com\ds\crypto\CertChainDemo.java
if ($LASTEXITCODE -ne 0) { throw "javac 失败，退出码 $LASTEXITCODE" }

# ---------- 2) 手搓证书演示 ----------
Write-Host '=== 2) 运行 CertificateDemo（9 段，末尾自检） ==='
$demoOut = & $java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
    -cp $classes com.ds.crypto.CertificateDemo 2>&1
$demoExit = $LASTEXITCODE
$demoOut | Out-File -Encoding utf8NoBOM 'docs\digital-certificate-raw.txt'
Write-Host "    退出码: $demoExit；输出: docs\digital-certificate-raw.txt（$(($demoOut | Measure-Object -Line).Lines) 行）"
if ($demoExit -ne 0) {
    $demoOut | Select-Object -Last 40
    throw "CertificateDemo 自检失败，退出码 $demoExit（见 docs\digital-certificate-raw.txt）"
}

if ($SkipTls) {
    Write-Host '=== 已跳过证书链/TLS 段（-SkipTls） ==='
    return
}

# ---------- 3) 真实证书链 + TLS 握手 ----------
Write-Host '=== 3) 运行 CertChainDemo（真实证书链 + 5 个 TLS 场景） ==='
$chainOut = & $java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
    -cp $classes com.ds.crypto.CertChainDemo $work $StorePass 2>&1
$chainExit = $LASTEXITCODE
$chainOut | Out-File -Encoding utf8NoBOM 'docs\cert-chain-tls-raw.txt'
Write-Host "    退出码: $chainExit；输出: docs\cert-chain-tls-raw.txt（$(($chainOut | Measure-Object -Line).Lines) 行）"
if ($chainExit -ne 0) {
    $chainOut | Select-Object -Last 40
    throw "CertChainDemo 自检失败，退出码 $chainExit（见 docs\cert-chain-tls-raw.txt）"
}

Write-Host '=== 完成 ==='
Write-Host '讲解见 docs\数字证书-讲解与演示.md'
