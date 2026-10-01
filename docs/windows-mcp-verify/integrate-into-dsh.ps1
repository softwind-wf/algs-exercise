# 把 windows-mcp 接进 DSH 的 MCP 配置(~\.dsh\profiles\<档>\cordis.patch.yml)
# 幂等:已存在则跳过;写入前备份。对 desktop 与 web 两个档都处理。
$root = Join-Path $env:USERPROFILE '.dsh\profiles'
$exe  = Join-Path $env:USERPROFILE '.windows-mcp-tools\bin\windows-mcp.exe'
$marker = 'mcp-windows'

$block = @'
# ── Windows-MCP:Windows 桌面自动化(用户级全局安装) ────────────────────────
# 启动器:C:\Users\wf\.windows-mcp-tools\windows-mcp.cmd serve
- insert:
    - id: mcp-windows
      name: '@deepseek-ai/dsh-mcp-client'
      config:
        serverName: windows
        transport: stdio
        command: C:/Users/wf/.windows-mcp-tools/bin/windows-mcp.exe
        args:
          - serve
        env:
          UV_CACHE_DIR: C:/Users/wf/.windows-mcp-tools/cache
          UV_PYTHON_INSTALL_DIR: C:/Users/wf/.windows-mcp-tools/python
          UV_TOOL_DIR: C:/Users/wf/.windows-mcp-tools/tools
          UV_TOOL_BIN_DIR: C:/Users/wf/.windows-mcp-tools/bin
          UV_PYTHON_PREFERENCE: only-managed
          ANONYMIZED_TELEMETRY: 'false'
'@

Write-Output ("windows-mcp.exe exists = {0}" -f (Test-Path $exe))

foreach ($prof in 'desktop','web') {
    $dir  = Join-Path $root $prof
    $file = Join-Path $dir 'cordis.patch.yml'
    Write-Output ""
    Write-Output ("=== profile: {0} ===" -f $prof)
    if (-not (Test-Path $file)) { Write-Output "  跳过:文件不存在"; continue }

    $raw = Get-Content $file -Raw
    if ($raw -match $marker) { Write-Output "  跳过:已包含 $marker"; continue }

    $bak = "$file.bak-windows-mcp"
    Copy-Item $file $bak -Force
    Write-Output ("  备份 -> {0}" -f $bak)

    $sep = if ($raw.EndsWith("`n")) { '' } else { "`r`n" }
    $new = $raw + $sep + $block
    # 统一为 CRLF,YAML 对换行不敏感但保持一致
    $new = $new -replace "`r`n", "`n" -replace "`n", "`r`n"
    [System.IO.File]::WriteAllText($file, $new, (New-Object System.Text.UTF8Encoding($false)))
    Write-Output ("  追加完成,新大小 = {0} 字节" -f (Get-Item $file).Length)
}

Write-Output ""
Write-Output "=== 结果检查 ==="
foreach ($prof in 'desktop','web') {
    $file = Join-Path $root "$prof\cordis.patch.yml"
    if (Test-Path $file) {
        $c = Get-Content $file -Raw
        Write-Output ("{0,-8} 含 mcp-windows = {1}  行数 = {2}" -f $prof, ($c -match $marker), (($c -split "`n").Count))
    }
}
