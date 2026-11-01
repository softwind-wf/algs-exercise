<#
.SYNOPSIS
    数字证书在真实项目里怎么工作：真实 TLS 握手报文 + mTLS + SNI 三段演示。

.DESCRIPTION
    1) keytool 生成演示证书（PKCS12，口令 changeit）到 target\tls-demo：
         root（自签 CA） -> a.example.com / b.example.com（两张站点证书） + alice（客户端证书）
    2) 用 -Djavax.net.debug=ssl:handshake 跑一次真实握手，原始报文 -> docs\tls-handshake-trace-raw.txt，
       过滤成可读的"消息序列" -> docs\tls-handshake-trace.txt
    3) 跑完整演示（握手 + mTLS + SNI + 小结）-> docs\tls-handshake-demo-raw.txt

.EXAMPLE
    .\scripts\run-tls-handshake-demo.ps1
#>
[CmdletBinding()]
param(
    [string]$StorePass = 'changeit'
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
New-Item -ItemType Directory -Force -Path docs | Out-Null

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

$work = 'target\tls-demo'
$classes = 'target\tls-classes'

function Invoke-Keytool {
    param([string[]]$Arguments, [string]$What)
    $output = & $keytool @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "keytool $What 失败（退出码 $LASTEXITCODE）：`n$($output -join "`n")"
    }
    return ($output | Where-Object { $_ -notmatch '^Picked up JAVA_TOOL_OPTIONS' })
}

# ---------- 0) 生成证书 ----------
Write-Host '=== 0) keytool 生成演示证书 ==='
$env:JAVA_TOOL_OPTIONS = '-Duser.language=en -Duser.country=US'
$Pass = $StorePass
if (Test-Path $work) { Remove-Item $work -Recurse -Force }
New-Item -ItemType Directory -Force -Path $work | Out-Null

$rootStore = Join-Path $work 'root.p12'
$rootPem = Join-Path $work 'root.pem'
Write-Host '  root：自签 CA'
$null = Invoke-Keytool @(
    '-genkeypair', '-alias', 'root', '-keyalg', 'RSA', '-keysize', '2048', '-sigalg', 'SHA256withRSA',
    '-validity', '3650', '-dname', 'CN=TLS Demo Root CA, O=Alg4 Demo, C=CN',
    '-ext', 'bc:c', '-ext', 'ku:c=keyCertSign,cRLSign',
    '-storetype', 'PKCS12', '-keystore', $rootStore, '-storepass', $Pass, '-keypass', $Pass
) '生成根 CA'
$null = Invoke-Keytool @('-exportcert', '-alias', 'root', '-keystore', $rootStore,
    '-storepass', $Pass, '-rfc', '-file', $rootPem) '导出根证书'

# 中间 CA：真实 CA 的典型结构（根离线、中间 CA 日常签发），也是"只发叶子证书必失败"演示的前提
Write-Host '  intermediate：根签发的中间 CA（pathLen=0）'
$intStore = Join-Path $work 'intermediate.p12'
$intPem = Join-Path $work 'intermediate.pem'
$null = Invoke-Keytool @(
    '-genkeypair', '-alias', 'intermediate', '-keyalg', 'RSA', '-keysize', '2048', '-sigalg', 'SHA256withRSA',
    '-dname', 'CN=TLS Demo Intermediate CA, O=Alg4 Demo, C=CN',
    '-ext', 'bc=ca:true,pathlen:0', '-ext', 'ku:c=keyCertSign,cRLSign',
    '-storetype', 'PKCS12', '-keystore', $intStore, '-storepass', $Pass, '-keypass', $Pass
) '生成中间 CA 密钥'
$null = Invoke-Keytool @('-certreq', '-alias', 'intermediate', '-keystore', $intStore,
    '-storepass', $Pass, '-file', (Join-Path $work 'intermediate.csr')) '生成中间 CA 的 CSR'
$null = Invoke-Keytool @('-gencert', '-alias', 'root', '-keystore', $rootStore, '-storepass', $Pass,
    '-infile', (Join-Path $work 'intermediate.csr'), '-outfile', $intPem, '-rfc', '-validity', '1825',
    '-ext', 'bc=ca:true,pathlen:0', '-ext', 'ku:c=keyCertSign,cRLSign') '签发中间 CA'
$null = Invoke-Keytool @('-importcert', '-alias', 'root', '-file', $rootPem, '-keystore', $intStore,
    '-storepass', $Pass, '-noprompt') '把根证书装进中间 CA'
$null = Invoke-Keytool @('-importcert', '-alias', 'intermediate', '-file', $intPem, '-keystore', $intStore,
    '-storepass', $Pass, '-noprompt') '把中间 CA 证书装回去'
$null = Invoke-Keytool @('-exportcert', '-alias', 'intermediate', '-keystore', $intStore,
    '-storepass', $Pass, '-rfc', '-file', $intPem) '导出中间 CA 证书'

# 另一家根：只用来演示"客户端信任的不是这家 CA"时握手失败
Write-Host '  otherroot：另一家自签 CA（反例用）'
$null = Invoke-Keytool @(
    '-genkeypair', '-alias', 'otherroot', '-keyalg', 'RSA', '-keysize', '2048', '-sigalg', 'SHA256withRSA',
    '-validity', '3650', '-dname', 'CN=Other Root CA, O=Other Org, C=CN',
    '-ext', 'bc:c', '-ext', 'ku:c=keyCertSign,cRLSign',
    '-storetype', 'PKCS12', '-keystore', (Join-Path $work 'otherroot.p12'), '-storepass', $Pass, '-keypass', $Pass
) '生成另一家根 CA'

function New-Leaf {
    param([string]$Alias, [string]$Dname, [string[]]$Extensions)
    Write-Host "  $Alias"
    $store = Join-Path $work "$Alias.p12"
    $pem = Join-Path $work "$Alias.pem"
    $csr = Join-Path $work "$Alias.csr"
    $null = Invoke-Keytool @(
        '-genkeypair', '-alias', 'server', '-keyalg', 'RSA', '-keysize', '2048', '-sigalg', 'SHA256withRSA',
        '-dname', $Dname, '-storetype', 'PKCS12', '-keystore', $store, '-storepass', $Pass, '-keypass', $Pass
    ) "生成 $Alias 密钥"
    $null = Invoke-Keytool @('-certreq', '-alias', 'server', '-keystore', $store,
        '-storepass', $Pass, '-file', $csr) "生成 $Alias 的 CSR"
    $gencert = @('-gencert', '-alias', 'intermediate', '-keystore', $intStore, '-storepass', $Pass,
        '-infile', $csr, '-outfile', $pem, '-rfc', '-validity', '825')
    foreach ($extension in $Extensions) { $gencert += @('-ext', $extension) }
    $null = Invoke-Keytool $gencert "签发 $Alias"
    # 叶子 + 中间 + 根 拼成一条链一次导入：keytool 才会把整条链挂在这个私钥条目上
    $chainPem = Join-Path $work "$Alias-chain.pem"
    # keytool -gencert 的输出已经带上签发者链（叶子 + 中间），这里只再补根证书
    $chainText = (Get-Content $pem -Raw) + "`n" + (Get-Content $rootPem -Raw)
    Set-Content -Path $chainPem -Value $chainText -NoNewline -Encoding ascii
    $null = Invoke-Keytool @('-importcert', '-alias', 'server', '-file', $chainPem, '-keystore', $store,
        '-storepass', $Pass, '-noprompt') "把 $Alias 完整链装回去"
}

New-Leaf -Alias 'a' -Dname 'CN=a.example.com, O=Alg4 Demo, C=CN' `
    -Extensions @('san=dns:a.example.com,ip:127.0.0.1', 'ku=digitalSignature,keyEncipherment', 'eku=serverAuth')
New-Leaf -Alias 'b' -Dname 'CN=b.example.com, O=Alg4 Demo, C=CN' `
    -Extensions @('san=dns:b.example.com', 'ku=digitalSignature,keyEncipherment', 'eku=serverAuth')

# 客户端证书：别名 server 统一，方便同一个 KeyManager 代码复用
Write-Host '  client（alice）'
$clientStore = Join-Path $work 'client.p12'
$clientPem = Join-Path $work 'client.pem'
$null = Invoke-Keytool @(
    '-genkeypair', '-alias', 'server', '-keyalg', 'RSA', '-keysize', '2048', '-sigalg', 'SHA256withRSA',
    '-dname', 'CN=alice, OU=Student, O=Alg4 Demo, C=CN',
    '-ext', 'ku=digitalSignature', '-ext', 'eku=clientAuth',
    '-storetype', 'PKCS12', '-keystore', $clientStore, '-storepass', $Pass, '-keypass', $Pass
) '生成客户端密钥'
$null = Invoke-Keytool @('-certreq', '-alias', 'server', '-keystore', $clientStore,
    '-storepass', $Pass, '-file', (Join-Path $work 'client.csr')) '生成客户端 CSR'
$null = Invoke-Keytool @('-gencert', '-alias', 'intermediate', '-keystore', $intStore, '-storepass', $Pass,
    '-infile', (Join-Path $work 'client.csr'), '-outfile', $clientPem, '-rfc', '-validity', '825',
    '-ext', 'ku=digitalSignature', '-ext', 'eku=clientAuth') '签发客户端证书'
$clientChain = Join-Path $work 'client-chain.pem'
$clientChainText = (Get-Content $clientPem -Raw) + "`n" + (Get-Content $rootPem -Raw)
Set-Content -Path $clientChain -Value $clientChainText -NoNewline -Encoding ascii
$null = Invoke-Keytool @('-importcert', '-alias', 'server', '-file', $clientChain, '-keystore', $clientStore,
    '-storepass', $Pass, '-noprompt') '把客户端完整链装回去'

Remove-Item Env:JAVA_TOOL_OPTIONS
Write-Host "  证书已生成：$work"

# ---------- 1) openssl 独立复核 ----------
Write-Host '=== 1) openssl 独立复核：证书内容 + "只发叶子证书"必然失败 ==='
$openssl = $null
foreach ($candidate in @(
        'D:\OpenSSL-Win64\bin\openssl.exe',
        'D:\Git\usr\bin\openssl.exe',
        'D:\Git\mingw64\bin\openssl.exe',
        'C:\Program Files\OpenSSL-Win64\bin\openssl.exe')) {
    if (Test-Path $candidate) { $openssl = $candidate; break }
}
if (-not $openssl) { $openssl = (Get-Command openssl -ErrorAction SilentlyContinue).Source }

$opensslLog = New-Object System.Collections.Generic.List[string]
if (-not $openssl) {
    Write-Host '    未找到 openssl，跳过本段（不影响其他演示）'
} else {
    Write-Host "    $openssl"
    $opensslLog.Add("openssl: $openssl")
    $opensslLog.Add((& $openssl version) -join '')
    $opensslLog.Add('')

    # 1.1 证书内容：SAN / KeyUsage / EKU 是"这张证书能干什么"的全部约束
    $opensslLog.Add('=== a.example.com 的证书约束（openssl x509 -ext） ===')
    $opensslLog.Add((& $openssl x509 -in (Join-Path $work 'a.pem') -noout -subject -issuer -dates `
        -ext subjectAltName,keyUsage,extendedKeyUsage 2>&1) -join "`n")

    # 1.2 整条链有几张
    $chainCount = (Select-String -Path (Join-Path $work 'a-chain.pem') -Pattern 'BEGIN CERTIFICATE').Count
    $opensslLog.Add('')
    $opensslLog.Add("a-chain.pem 里有 $chainCount 张证书（叶子 + 中间 + 根）")

    # 1.3 从 PKCS12 里导出私钥，供 s_server 使用
    $keyPem = Join-Path $work 'a-key.pem'
    & $openssl pkcs12 -in (Join-Path $work 'a.p12') -nocerts -nodes -passin "pass:$Pass" -out $keyPem 2>&1 | Out-Null

    # 1.4 全链 vs 只发叶子：同一张叶子证书，只差"发不发中间证书"
    # 注意：openssl s_server 的 -cert 只读第一张，中间证书要用 -cert_chain 单独给；
    #       Nginx 的 ssl_certificate fullchain.pem 相当于这里"两个都给"。
    $leafPem = Join-Path $work 'a-leaf.pem'
    & $openssl x509 -in (Join-Path $work 'a.pem') -out $leafPem 2>&1 | Out-Null
    $counter = 0
    function Test-Chain {
        param([string]$CertFile, [string]$ChainFile)
        $script:counter++
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
        $listener.Start(); $port = ($listener.LocalEndpoint).Port; $listener.Stop()
        $stdout = Join-Path $work "s_server-$script:counter.out"
        $stderr = Join-Path $work "s_server-$script:counter.err"
        $serverArgs = @('s_server', '-accept', $port, '-cert', $CertFile, '-key', $keyPem, '-www', '-quiet')
        if ($ChainFile) { $serverArgs += @('-cert_chain', $ChainFile) }
        $server = Start-Process -FilePath $openssl -ArgumentList $serverArgs `
            -PassThru -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr
        Start-Sleep -Milliseconds 1000
        try {
            $result = '' | & $openssl s_client -connect "127.0.0.1:$port" -CAfile (Join-Path $work 'root.pem') `
                -verify_return_error -servername a.example.com -brief 2>&1
            return (($result -join "`n") + "`n[exit code: $LASTEXITCODE]")
        } finally {
            Stop-Process -Id $server.Id -Force -ErrorAction SilentlyContinue
        }
    }

    $opensslLog.Add('')
    $opensslLog.Add('=== 场景 A：服务端发【叶子 + 中间证书】（= Nginx 用 fullchain.pem） ===')
    $full = Test-Chain -CertFile $leafPem -ChainFile (Join-Path $work 'intermediate.pem')
    $opensslLog.Add($full)

    $opensslLog.Add('')
    $opensslLog.Add('=== 场景 B：服务端只发【叶子证书】（= 漏配中间证书的线上事故） ===')
    $leafOnly = Test-Chain -CertFile $leafPem
    $opensslLog.Add($leafOnly)
    $opensslLog | Out-File -Encoding utf8NoBOM 'docs\tls-openssl-check.txt'
    Write-Host "    输出 docs\tls-openssl-check.txt（$($opensslLog.Count) 行）"
    $okFull = $full -match 'Verification: OK'
    $failLeaf = $leafOnly -match 'unable to get local issuer certificate|verify error'
    Write-Host "    全链: $(if ($okFull) { 'Verification OK' } else { '异常' })；只发叶子: $(if ($failLeaf) { '按预期验证失败' } else { '未按预期失败' })"
    if (-not $okFull -or -not $failLeaf) {
        throw "openssl 复核结果不符合预期（见 docs\tls-openssl-check.txt）"
    }
}
# ---------- 2) 编译 ----------
Write-Host '=== 2) 编译 TlsHandshakeDemo ==='
New-Item -ItemType Directory -Force -Path $classes | Out-Null
& $javac -encoding UTF-8 --release 8 -d $classes src\main\java\com\ds\crypto\TlsHandshakeDemo.java
if ($LASTEXITCODE -ne 0) { throw "javac 失败，退出码 $LASTEXITCODE" }

# ---------- 3) 抓真实握手报文 ----------
Write-Host '=== 3) 抓一次真实 TLS 握手报文（-Djavax.net.debug=ssl:handshake） ==='
$traceRaw = & $java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Djavax.net.debug=ssl:handshake' `
    -cp $classes com.ds.crypto.TlsHandshakeDemo $work trace 2>&1
$traceExit = $LASTEXITCODE
$traceRaw | Out-File -Encoding utf8NoBOM 'docs\tls-handshake-trace-raw.txt'
Write-Host "    退出码 $traceExit；原始输出 docs\tls-handshake-trace-raw.txt（$(($traceRaw | Measure-Object -Line).Lines) 行）"
if ($traceExit -ne 0) {
    $traceRaw | Select-Object -Last 30
    throw "握手 trace 段失败，退出码 $traceExit"
}

# 把 JDK 的调试日志过滤成"消息序列 + 关键字段"，便于人读
$filtered = New-Object System.Collections.Generic.List[string]
$filtered.Add('# 一次 TLS 1.3 握手在线上实际传的消息（从 javax.net.debug=ssl:handshake 日志抽出，只保留每个报文出现的第一处）')
$filtered.Add('')
$seenFields = New-Object System.Collections.Generic.HashSet[string]
foreach ($line in $traceRaw) {
    $clean = $line -replace '^javax\.net\.ssl\|[^|]*\|[^|]*\|[^|]*\|[^|]*\|', ''
    $clean = $clean -replace '^[A-Za-z]+\.java:\d+\|', ''
    $clean = $clean.TrimEnd()
    # 每个报文只留一条：客户端视角发 ClientHello、服务端视角回 ServerHello 等
    if ($clean -match 'Produced ClientHello|Produced ServerHello|Consuming EncryptedExtensions|Consuming server Certificate|Produced server CertificateVerify|Produced server Finished|Produced client Finished') {
        $filtered.Add('')
        $filtered.Add('>> ' + ($clean -replace '\s*\($', ''))
    }
    elseif ($clean -match 'type=host_name|"cipher suite"\s+:') {
        # 同一个字段两端各打印一次，去重后只留第一次出现的位置
        if ($seenFields.Add($clean.Trim())) {
            $filtered.Add('     ' + $clean.Trim())
        }
    }
}
$filtered | Out-File -Encoding utf8NoBOM 'docs\tls-handshake-trace.txt'
Write-Host "    过滤后可读报文 docs\tls-handshake-trace.txt（$($filtered.Count) 行）"

# ---------- 4) 完整演示 ----------
Write-Host '=== 4) 运行完整演示（握手 + mTLS + SNI） ==='
$demoOut = & $java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -cp $classes com.ds.crypto.TlsHandshakeDemo $work 2>&1
$demoExit = $LASTEXITCODE
$demoOut | Out-File -Encoding utf8NoBOM 'docs\tls-handshake-demo-raw.txt'
Write-Host "    退出码 $demoExit；输出 docs\tls-handshake-demo-raw.txt（$(($demoOut | Measure-Object -Line).Lines) 行）"
if ($demoExit -ne 0) {
    $demoOut | Select-Object -Last 30
    throw "TlsHandshakeDemo 自检失败，退出码 $demoExit"
}

Write-Host '=== 完成 ==='
Write-Host '讲解见 docs\数字证书-在真实项目里怎么工作.md'











