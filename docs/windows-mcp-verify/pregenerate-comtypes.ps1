# 在全局 venv 里预生成 comtypes COM 包装模块(需要写权限,跑一次即可)
# 目的:让之后的所有运行只读缓存,不再需要写 site-packages\comtypes\gen
# 用法:pwsh -File pregenerate-comtypes.ps1
$root = Join-Path $env:USERPROFILE '.windows-mcp-tools'
$py   = Join-Path $root 'tools\windows-mcp\Scripts\python.exe'
$gen  = Join-Path $root 'tools\windows-mcp\Lib\site-packages\comtypes\gen'

Write-Output "=== 生成前 ==="
Get-ChildItem $gen -Force -ErrorAction SilentlyContinue | Select-Object Name, Length | Format-Table -AutoSize

$code = @'
import traceback
try:
    from windows_mcp.uia.core import _AutomationClient
    _AutomationClient.instance()
    print("UIA client instance OK")
except Exception:
    traceback.print_exc()
try:
    from windows_mcp.desktop.service import Desktop
    d = Desktop()
    print("Desktop() OK ->", type(d).__name__)
except Exception:
    traceback.print_exc()
import comtypes.client
print("comtypes.client.gen_dir =", comtypes.client.gen_dir)
'@
$codeFile = Join-Path $env:TEMP 'wmcp_pregenerate.py'
Set-Content -Path $codeFile -Value $code -Encoding utf8

Write-Output "=== 执行预生成 ==="
& $py $codeFile 2>&1 | ForEach-Object { Write-Output ("  " + $_) }

Write-Output "=== 生成后 comtypes\gen ==="
Get-ChildItem $gen -Force -ErrorAction SilentlyContinue | Select-Object Name, Length | Format-Table -AutoSize
$sz = (Get-ChildItem $gen -Recurse -File -Force -ErrorAction SilentlyContinue | Measure-Object Length -Sum).Sum
Write-Output ("  gen 体积 = {0:N1} KB" -f ($sz/1KB))
