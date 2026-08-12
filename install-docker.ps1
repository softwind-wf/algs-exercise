# Docker Desktop one-click installer (MUST run as Administrator)
# Steps: enable WSL2 (no distro) -> install Docker Desktop from LOCAL package (env DOCKER_PKG)
# NOTE: keep this file ASCII-only, PowerShell 5.1 mis-reads UTF-8 without BOM.
# Usage: $env:DOCKER_PKG='D:\path\Docker Desktop Installer.exe' then run this script.
$ErrorActionPreference = 'Continue'
$log = 'D:\downloads\algs4-master\algs4-master\docker-install.log'
"=== $(Get-Date) start ===" | Out-File $log -Encoding utf8

# 1. admin check
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
"IsAdmin: $isAdmin" | Out-File $log -Append -Encoding utf8
if (-not $isAdmin) {
    "ERROR: need admin, right-click PowerShell and Run as Administrator" | Out-File $log -Append -Encoding utf8
    Write-Host "ERROR: need admin privileges. Run this script in an elevated PowerShell." -ForegroundColor Red
    exit 1
}

# 2. install WSL2 only (features + kernel). No Ubuntu distro: Docker Desktop ships its own.
"--- wsl --install --no-distribution ---" | Out-File $log -Append -Encoding utf8
wsl --install --no-distribution *>&1 | Out-File $log -Append -Encoding utf8
"wsl install exit code: $LASTEXITCODE" | Out-File $log -Append -Encoding utf8

# 3. install Docker Desktop from user's local installer package
$pkg = $env:DOCKER_PKG
"--- install Docker Desktop from local package ---" | Out-File $log -Append -Encoding utf8
if ($pkg -and (Test-Path $pkg)) {
    "Using package: $pkg" | Out-File $log -Append -Encoding utf8
    $p = Start-Process $pkg -ArgumentList 'install','--accept-license','--quiet' -Wait -PassThru
    "installer exit code: $($p.ExitCode)" | Out-File $log -Append -Encoding utf8
} else {
    "DOCKER_PKG not set or not found, fallback to winget" | Out-File $log -Append -Encoding utf8
    winget install --id Docker.DockerDesktop -e --accept-source-agreements --accept-package-agreements --disable-interactivity *>&1 | Out-File $log -Append -Encoding utf8
    "winget exit code: $LASTEXITCODE" | Out-File $log -Append -Encoding utf8
}

# 4. result check
"Docker Desktop exe exists: $(Test-Path 'C:\Program Files\Docker\Docker\Docker Desktop.exe')" | Out-File $log -Append -Encoding utf8
"=== $(Get-Date) end ===" | Out-File $log -Append -Encoding utf8
Write-Host "DONE. Log: docker-install.log. If WSL was newly enabled, REBOOT first, then start Docker Desktop." -ForegroundColor Green
