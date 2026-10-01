# 用户级全局安装 windows-mcp(与项目无关)
# 用法:pwsh -File setup-user-level.ps1
# 说明:若在受限(沙箱)会话里执行,写工作区外的路径会被拒绝,需更宽权限或改用普通终端
$root = Join-Path $env:USERPROFILE '.windows-mcp-tools'
New-Item -ItemType Directory -Force -Path $root | Out-Null
foreach ($d in 'cache','python','tools','bin') { New-Item -ItemType Directory -Force -Path (Join-Path $root $d) | Out-Null }

$env:UV_CACHE_DIR          = Join-Path $root 'cache'
$env:UV_PYTHON_INSTALL_DIR = Join-Path $root 'python'
$env:UV_TOOL_DIR           = Join-Path $root 'tools'
$env:UV_TOOL_BIN_DIR       = Join-Path $root 'bin'
$env:UV_PYTHON_PREFERENCE  = 'only-managed'
$env:UV_NO_PROGRESS        = '1'
$env:ANONYMIZED_TELEMETRY  = 'false'

Write-Output "=== 1) 安装受管 Python 3.14 ==="
uv python install 3.14 2>&1 | ForEach-Object { Write-Output ("  " + $_) }

Write-Output "=== 2) uv tool install windows-mcp ==="
uv tool install windows-mcp 2>&1 | ForEach-Object { Write-Output ("  " + $_) }
Write-Output ("exit={0}" -f $LASTEXITCODE)

Write-Output "=== 3) uv tool list ==="
uv tool list 2>&1 | ForEach-Object { Write-Output ("  " + $_) }

Write-Output "=== 4) shim ==="
Get-ChildItem (Join-Path $root 'bin') -Force | Select-Object Name, Length | Format-Table -AutoSize

Write-Output "=== 5) 体积 ==="
$s = (Get-ChildItem $root -Recurse -File -Force -ErrorAction SilentlyContinue | Measure-Object Length -Sum).Sum
Write-Output ("  {0:N1} MB" -f ($s/1MB))
