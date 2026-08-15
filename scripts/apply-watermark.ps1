# ============================================================
# 版权水印署名替换脚本
# 用途：把源码中水印占位符 YOUR_NAME 批量替换为你的实际署名
#       （网名 / 店铺名 / 真实姓名），防止忘替换就发货。
# 用法：
#   交互式（推荐，会提示输入署名，防呆）：
#     powershell -ExecutionPolicy Bypass -File scripts\apply-watermark.ps1
#   非交互（指定署名）：
#     powershell -ExecutionPolicy Bypass -File scripts\apply-watermark.ps1 -Name 你的网名
#   非交互 + 跳过确认（脚本化/CI 用）：
#     powershell -ExecutionPolicy Bypass -File scripts\apply-watermark.ps1 -Name 你的网名 -Force
#   仅检查不替换（验证当前水印状态）：
#     powershell -ExecutionPolicy Bypass -File scripts\apply-watermark.ps1 -Check
# 说明：只处理 src\main\java\com\ds\university 下的 Java 文件，
#       已包含版权水印且含占位符的文件才会被改写；UTF-8 无 BOM 写回，
#       兼容 Java 8 编译。
# ============================================================

param(
    [string]$Name,          # 署名；不传则交互输入
    [string]$Placeholder = 'YOUR_NAME',   # 水印占位符
    [switch]$Check,         # 只检查不替换
    [switch]$Force          # 跳过确认提示（配合 -Name 用于非交互场景）
)

$ErrorActionPreference = 'Stop'

# ---------- 定位项目根目录（脚本位于 <根>/scripts/ 下） ----------
$root = Split-Path -Parent $PSScriptRoot
$javaRoot = Join-Path $root 'src\main\java\com\ds\university'

if (-not (Test-Path $javaRoot)) {
    Write-Host "[错误] 未找到源码目录：$javaRoot" -ForegroundColor Red
    Write-Host "请确认脚本位于项目根目录的 scripts\ 下。" -ForegroundColor Red
    exit 1
}

# ---------- 收集目标文件 ----------
$files = Get-ChildItem -Path $javaRoot -Recurse -Filter *.java -File
$targets = $files | Where-Object {
    $_.Name -notmatch '^package-info\.java$'
}

Write-Host "源码目录：$javaRoot"
Write-Host "扫描到 Java 文件：$($targets.Count) 个"

# ---------- 检查当前水印状态 ----------
$withPlaceholder = @()
foreach ($f in $targets) {
    $content = [System.IO.File]::ReadAllText($f.FullName)
    if ($content -match [regex]::Escape($Placeholder)) {
        $withPlaceholder += $f
    }
}

if ($withPlaceholder.Count -eq 0) {
    Write-Host ""
    Write-Host "[OK] 所有文件均不含占位符 $Placeholder，水印已完成署名（或本就没有水印）。" -ForegroundColor Green
    exit 0
}

Write-Host "含占位符 $Placeholder 的文件：$($withPlaceholder.Count) 个"

if ($Check) {
    Write-Host ""
    Write-Host "[检查模式] 以下文件仍含占位符，发货前必须替换：$Placeholder" -ForegroundColor Yellow
    $withPlaceholder | ForEach-Object { Write-Host "  - $($_.FullName.Substring($root.Length + 1))" -ForegroundColor Yellow }
    exit 0
}

# ---------- 获取署名（防呆：不允许为空） ----------
if ([string]::IsNullOrWhiteSpace($Name)) {
    Write-Host ""
    Write-Host "尚未指定署名，现在输入（将写入全部水印，替换 $Placeholder）：" -ForegroundColor Cyan
    $Name = Read-Host "你的署名（网名/店铺名/真实姓名）"
    if ([string]::IsNullOrWhiteSpace($Name)) {
        Write-Host ""
        Write-Host "[错误] 署名不能为空，已取消，防止带着占位符发货。" -ForegroundColor Red
        exit 1
    }
}

if ($Name -eq $Placeholder) {
    Write-Host "[错误] 署名不能仍是占位符 $Placeholder，请重新指定。" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "即将把 $($withPlaceholder.Count) 个文件中的 $Placeholder 替换为：$Name" -ForegroundColor Cyan
if (-not $Force) {
    $confirm = Read-Host "确认执行？输入 y 继续，其他任意键取消"
    if ($confirm -ne 'y' -and $confirm -ne 'Y') {
        Write-Host "已取消，未做任何修改。" -ForegroundColor Yellow
        exit 0
    }
}

# ---------- 执行替换（UTF-8 无 BOM 写回） ----------
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$changed = 0
foreach ($f in $withPlaceholder) {
    $content = [System.IO.File]::ReadAllText($f.FullName)
    $newContent = $content.Replace($Placeholder, $Name)
    [System.IO.File]::WriteAllText($f.FullName, $newContent, $utf8NoBom)
    $changed++
}
Write-Host ""
Write-Host "[OK] 已替换 $changed 个文件。" -ForegroundColor Green

# ---------- 替换后校验：确保无残留 ----------
$remaining = @()
foreach ($f in $targets) {
    $content = [System.IO.File]::ReadAllText($f.FullName)
    if ($content -match [regex]::Escape($Placeholder)) {
        $remaining += $f
    }
}
if ($remaining.Count -gt 0) {
    Write-Host "[警告] 仍有 $($remaining.Count) 个文件含占位符，请检查：" -ForegroundColor Red
    $remaining | ForEach-Object { Write-Host "  - $($_.FullName.Substring($root.Length + 1))" -ForegroundColor Red }
    exit 2
}

Write-Host "[OK] 校验通过：全项目无 $Placeholder 残留。" -ForegroundColor Green
Write-Host ""
Write-Host "提示：换好署名后记得重新构建镜像（docker compose up -d --build），让容器内的代码也带上水印。" -ForegroundColor Cyan
exit 0
