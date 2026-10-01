# 修复 comtypes 代码缓存导致的 windows-mcp 启动崩溃(留档)
# 背景:comtypes 优先用 site-packages\comtypes\gen,不可写才回落 %APPDATA%\Python\...\comtypes_cache
#       两条路若都不可写 -> PermissionError: [WinError 5] 拒绝访问: 'C:\Users\wf\AppData\Roaming\Python'
# 修法:① 确保 comtypes\gen 存在;② 写 .pth 让 comtypes.gen 提前导入,使 gen 目录被直接采用
# 用法:pwsh -File fix-comtypes.ps1   (需要对该 venv 的写权限)
$root   = Join-Path $env:USERPROFILE '.windows-mcp-tools'
$venvSp = Join-Path $root 'tools\windows-mcp\Lib\site-packages'
$ctGen  = Join-Path $venvSp 'comtypes\gen'
$pth    = Join-Path $venvSp '_dsh_comtypes_gen_fix.pth'

Write-Output "=== 确保 comtypes\gen 存在 ==="
New-Item -ItemType Directory -Force -Path $ctGen | Out-Null
try { Set-Content (Join-Path $ctGen '.probe') 'ok' -Encoding ascii -ErrorAction Stop; Write-Output ("  gen 可写: {0}" -f $ctGen); Remove-Item (Join-Path $ctGen '.probe') -Force } catch { Write-Output "  gen 不可写: $($_.Exception.Message)" }

Write-Output "=== 写 .pth ==="
$line = 'import comtypes.gen as _dsh_comtypes_gen'
Set-Content -Path $pth -Value $line -Encoding ascii
Write-Output ("  {0} -> {1}" -f $pth, (Get-Content $pth -Raw).Trim())

Write-Output "=== 验证 ==="
$py = Join-Path $root 'tools\windows-mcp\Scripts\python.exe'
& $py -c 'import comtypes.client, comtypes.gen; print("gen.__path__ =", list(comtypes.gen.__path__)); print("import ok")' 2>&1 | ForEach-Object { Write-Output ("  " + $_) }
