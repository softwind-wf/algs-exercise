# ============================================================
# 生成自签名 HTTPS 证书（开发/演示用，生产环境请替换为正式证书）
# 用法：powershell -ExecutionPolicy Bypass -File scripts\gen-keystore.ps1
# 输出：keystore\university.p12（PKCS12，密码 changeit，有效期 10 年）
# 备注：SAN 包含 localhost 与 127.0.0.1，浏览器自签名提示可接受；
#       正式部署请用 -dname/-ext 参数生成或购买正式证书。
# ============================================================

$ErrorActionPreference = 'Stop'
$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
if (-not (Test-Path $keytool)) {
    $keytool = 'keytool'   # 回退到 PATH
}

$root = Split-Path -Parent $PSScriptRoot
$keystoreDir = Join-Path $root 'keystore'
$keystore = Join-Path $keystoreDir 'university.p12'
$password = 'changeit'
$alias = 'university'

if (-not (Test-Path $keystoreDir)) {
    New-Item -ItemType Directory -Path $keystoreDir | Out-Null
}

if (Test-Path $keystore) {
    Write-Host "已存在：$keystore（如需重新生成请先删除）"
    exit 0
}

& $keytool -genkeypair `
    -alias $alias `
    -keyalg RSA -keysize 2048 -validity 3650 `
    -storetype PKCS12 -keystore $keystore -storepass $password `
    -dname "CN=localhost, OU=University Demo, O=University, L=Chengdu, ST=Sichuan, C=CN" `
    -ext "SAN=dns:localhost,ip:127.0.0.1"

if ($LASTEXITCODE -eq 0) {
    Write-Host "证书已生成：$keystore"
    Write-Host "密码：$password（application-tls.yml 的 KEYSTORE_PASSWORD 默认值）"
    Write-Host "启动 HTTPS：java -jar target/algs4-1.0.0.0.jar --spring.profiles.active=dev,tls"
} else {
    Write-Host '证书生成失败，请检查 JAVA_HOME 或 keytool 可用性。'
    exit 1
}
