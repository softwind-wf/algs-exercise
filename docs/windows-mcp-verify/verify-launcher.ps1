# 最终验证:清空当前进程所有 UV_* 环境变量,只靠启动器自身设置,握手 initialize + tools/list
# 用法:pwsh -File verify-launcher.ps1
$root = Join-Path $env:USERPROFILE '.windows-mcp-tools'
$cmd  = Join-Path $root 'windows-mcp.cmd'
Write-Output ("launcher exists = {0}  ({1})" -f (Test-Path $cmd), $cmd)

foreach ($n in 'UV_CACHE_DIR','UV_PYTHON_INSTALL_DIR','UV_TOOL_DIR','UV_TOOL_BIN_DIR','UV_PYTHON_PREFERENCE','ANONYMIZED_TELEMETRY') {
    Remove-Item ("Env:" + $n) -ErrorAction SilentlyContinue
}
Write-Output ("剩余 UV_* 环境变量数 = {0}" -f (Get-ChildItem env: | Where-Object Name -match '^UV_' | Measure-Object).Count)

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $cmd
$psi.Arguments = 'serve'
$psi.UseShellExecute = $false
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.CreateNoWindow = $true
$psi.WorkingDirectory = $env:TEMP

$p = New-Object System.Diagnostics.Process
$p.StartInfo = $psi
$null = $p.Start()
Write-Output ("pid={0} cwd={1}" -f $p.Id, $psi.WorkingDirectory)

function RL([int]$s) { $t = $p.StandardOutput.ReadLineAsync(); if ($t.Wait([TimeSpan]::FromSeconds($s))) { return $t.Result } return '<TIMEOUT>' }
function RE([int]$s) { $t = $p.StandardError.ReadLineAsync(); if ($t.Wait([TimeSpan]::FromSeconds($s))) { return $t.Result } return '<TIMEOUT>' }

Write-Output ("stderr -> {0}" -f (RE 40))
$p.StandardInput.WriteLine('{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"dsh-final","version":"1"}}}'); $p.StandardInput.Flush()
$l1 = RL 60
if ($l1 -eq '<TIMEOUT>') { Write-Output "initialize -> TIMEOUT" } else { $m1 = $l1|ConvertFrom-Json; Write-Output ("initialize -> OK {0} v{1} protocol={2}" -f $m1.result.serverInfo.name, $m1.result.serverInfo.version, $m1.result.protocolVersion) }
$p.StandardInput.WriteLine('{"jsonrpc":"2.0","method":"notifications/initialized","params":{}}'); $p.StandardInput.Flush()
$p.StandardInput.WriteLine('{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'); $p.StandardInput.Flush()
$l2 = RL 60
if ($l2 -eq '<TIMEOUT>') { Write-Output "tools/list -> TIMEOUT" } else { $m2 = $l2|ConvertFrom-Json; Write-Output ("tools/list -> OK {0} 个工具" -f @($m2.result.tools).Count) }
Write-Output ("alive={0}" -f (-not $p.HasExited))

Get-CimInstance Win32_Process -Filter ("ParentProcessId={0}" -f $p.Id) -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1
Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.ProcessName -match '^python|^uv|windows-mcp' } | ForEach-Object { Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue }
Write-Output ("残留进程={0}" -f ((Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.ProcessName -match '^python|^uv|windows-mcp' } | Measure-Object).Count))
