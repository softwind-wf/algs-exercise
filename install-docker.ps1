# Docker Desktop 一键安装脚本（必须以管理员身份运行）
# 步骤：启用 WSL2 → winget 安装 Docker Desktop
$ErrorActionPreference = 'Continue'
$log = 'D:\downloads\algs4-master\algs4-master\docker-install.log'
"=== $(Get-Date) 开始安装 ===" | Out-File $log -Encoding utf8

# 1. 检查管理员权限
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
"IsAdmin: $isAdmin" | Out-File $log -Append -Encoding utf8
if (-not $isAdmin) {
    "ERROR: 请右键本脚本选择【使用管理员身份运行】" | Out-File $log -Append -Encoding utf8
    Write-Host "ERROR: 需要管理员权限，请右键以管理员身份运行" -ForegroundColor Red
    exit 1
}

# 2. 安装 WSL2（含 VirtualMachinePlatform 与 WSL 内核，--no-launch 避免立即打开发行版）
"--- wsl --install ---" | Out-File $log -Append -Encoding utf8
wsl --install --no-launch *>&1 | Out-File $log -Append -Encoding utf8
"wsl install exit code: $LASTEXITCODE" | Out-File $log -Append -Encoding utf8

# 3. winget 安装 Docker Desktop（自动接受协议）
"--- winget install Docker.DockerDesktop ---" | Out-File $log -Append -Encoding utf8
winget install --id Docker.DockerDesktop -e --accept-source-agreements --accept-package-agreements --disable-interactivity *>&1 | Out-File $log -Append -Encoding utf8
"winget exit code: $LASTEXITCODE" | Out-File $log -Append -Encoding utf8

# 4. 结果检查
"Docker Desktop exe exists: $(Test-Path 'C:\Program Files\Docker\Docker\Docker Desktop.exe')" | Out-File $log -Append -Encoding utf8
"=== $(Get-Date) 结束 ===" | Out-File $log -Append -Encoding utf8
Write-Host "安装流程结束，日志见 docker-install.log。若 WSL 是新启用，需要重启电脑后才能启动 Docker Desktop。" -ForegroundColor Green
