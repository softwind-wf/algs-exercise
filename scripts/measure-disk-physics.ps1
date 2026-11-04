<#
.SYNOPSIS
    实测磁盘 I/O 的物理成本：按"次数"收费，不按字节收费。

.DESCRIPTION
    在本机（NVMe SSD）上测五种读法，单位都是"每次 I/O 多少微秒"和"折合多少 MB/s"：
      1) 页缓存命中，8KB 随机读          —— 只有内存拷贝和系统调用
      2) 设备读，8KB 顺序                —— 绕过页缓存（FILE_FLAG_NO_BUFFERING）
      3) 设备读，8KB 随机
      4) 设备读，4KB 随机                —— 恰好一个物理扇区
      5) 设备读，128KB 顺序              —— 一次读一大块

    结论会直接印出来：8KB 一页能装几十行，所以"把页读进来"比"按行读"便宜几十倍；
    而加大单次 I/O 的块大小，吞吐能翻十几倍，但代价是可能读进用不上的数据。

.PARAMETER SizeMB
    测试文件大小（MB），默认 256。

.EXAMPLE
    .\scripts\measure-disk-physics.ps1
    .\scripts\measure-disk-physics.ps1 -SizeMB 512 -KeepFile
#>
[CmdletBinding()]
param(
    [int]$SizeMB = 256,
    [string]$TestFile = 'out\io-bench.bin',
    [switch]$KeepFile
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public static class RawIO {
    [DllImport("kernel32.dll", SetLastError=true, CharSet=CharSet.Auto, EntryPoint="CreateFileW")]
    public static extern IntPtr CreateFile(string name, uint access, uint share, IntPtr sec, uint disp, uint flags, IntPtr tmpl);
    [DllImport("kernel32.dll", SetLastError=true)]
    public static extern bool ReadFile(IntPtr h, IntPtr buf, uint n, out uint read, IntPtr ov);
    [DllImport("kernel32.dll", SetLastError=true)]
    public static extern bool SetFilePointerEx(IntPtr h, long dist, IntPtr newPos, uint method);
    [DllImport("kernel32.dll")]
    public static extern bool CloseHandle(IntPtr h);
}
"@

$sb = [System.Text.StringBuilder]::new()
function Say([string]$t) { [void]$sb.Append($t + "`r`n"); Write-Host $t }

# ---------- 造测试文件 ----------
$path = Join-Path $root $TestFile
New-Item -ItemType Directory -Force -Path (Split-Path $path) | Out-Null
if (Test-Path $path) { Remove-Item $path -Force }

Say "== 测试盘: $((Split-Path -Qualifier $path)) =="
$fs = [System.IO.File]::Open($path, 'Create', 'ReadWrite')
$chunk = New-Object byte[] (1MB)
for ($i = 0; $i -lt $SizeMB; $i++) { $fs.Write($chunk, 0, $chunk.Length) }
$fs.Flush($true)
$fs.Dispose()
$size = (Get-Item $path).Length
Say ("测试文件 : {0}（{1} MB）" -f $path, $SizeMB)

# ---------- 1. 页缓存命中 ----------
$fs = [System.IO.File]::Open($path, 'Open', 'Read', [System.IO.FileShare]::ReadWrite)
$buf8 = New-Object byte[] 8192
$fs.Seek(0, 'Begin') | Out-Null
$total = 0
while ($total -lt $size) { $n = $fs.Read($buf8, 0, 8192); if ($n -le 0) { break }; $total += $n }   # 预热：整文件进页缓存
$rnd = [System.Random]::new(42)
$count = 2000
$sw = [System.Diagnostics.Stopwatch]::StartNew()
for ($i = 0; $i -lt $count; $i++) {
    $off = [int64]($rnd.Next(0, [int]($size / 8192))) * 8192
    $fs.Seek($off, 'Begin') | Out-Null
    [void]$fs.Read($buf8, 0, 8192)
}
$sw.Stop()
$cacheUs = $sw.Elapsed.TotalMilliseconds * 1000 / $count
$fs.Dispose()
Say ''
Say '--- 1) 页缓存命中（数据已经在内存里）---'
$cacheMbps = (8192 / 1MB) / ($cacheUs / 1e6)
Say ("  8KB 随机读 {0} 次：平均 {1:N2} µs/次，折合 {2:N0} MB/s" -f $count, $cacheUs, $cacheMbps)

# ---------- 2..5 绕过页缓存，真读设备 ----------
$h = [RawIO]::CreateFile($path, [uint32]2147483648, 3, [IntPtr]::Zero, 3, [uint32]0x20000000, [IntPtr]::Zero)  # FILE_FLAG_NO_BUFFERING
if ($h -eq [IntPtr]::Zero -or $h -eq [IntPtr](-1)) { throw "打开失败: $([Runtime.InteropServices.Marshal]::GetLastWin32Error())" }
$buf = [Runtime.InteropServices.Marshal]::AllocHGlobal(262144)
$read = 0

function Measure-Read([int]$bytes, [int]$times, [bool]$random) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    for ($i = 0; $i -lt $times; $i++) {
        if ($random) {
            $off = [int64]($rnd.Next(0, [int](($size - $bytes) / 4096))) * 4096
        } else {
            $off = ([int64]$i * $bytes) % ($size - $bytes)
        }
        [void][RawIO]::SetFilePointerEx($h, $off, [IntPtr]::Zero, 0)
        if (-not [RawIO]::ReadFile($h, $buf, [uint32]$bytes, [ref]$read, [IntPtr]::Zero)) {
            throw "ReadFile 失败: $([Runtime.InteropServices.Marshal]::GetLastWin32Error())"
        }
    }
    $sw.Stop()
    $us = $sw.Elapsed.TotalMilliseconds * 1000 / $times
    return [pscustomobject]@{
        Bytes = $bytes
        Us = $us
        MBps = ($bytes * $times / 1MB) / $sw.Elapsed.TotalSeconds
        Iops = 1e6 / $us
    }
}

$rows = @()
$rows += Measure-Read 8192  800 $false
$rows += Measure-Read 8192  800 $true
$rows += Measure-Read 4096  800 $true
$rows += Measure-Read 131072 300 $false
[Runtime.InteropServices.Marshal]::FreeHGlobal($buf)
[void][RawIO]::CloseHandle($h)

Say ''
Say '--- 2..5) 绕过页缓存，真的读设备（FILE_FLAG_NO_BUFFERING）---'
Say '  读法            每次I/O     折合带宽      折合 IOPS'
foreach ($r in $rows) {
    $kind = switch ($r.Bytes) {
        8192 { '8KB' }
        4096 { '4KB' }
        131072 { '128KB' }
        default { "$($r.Bytes)B" }
    }
    $order = if ($r.Bytes -eq 8192 -and $rows.IndexOf($r) -eq 0) { '顺序' } elseif ($r.Bytes -eq 8192) { '随机' } elseif ($r.Bytes -eq 4096) { '随机' } else { '顺序' }
    Say ("  {0,-6} {1}    {2,7:N1} µs   {3,8:N1} MB/s   {4,9:N0}" -f $kind, $order, $r.Us, $r.MBps, $r.Iops)
}

# ---------- 结论 ----------
Say ''
Say '--- 结论 ---'
$s8 = $rows[1].Us; $s4 = $rows[2].Us; $s128 = $rows[3].Us
$ratio1 = $s8 / $cacheUs
Say ("  同样读 8KB：命中缓存 {0:N1} µs，真读设备 {1:N1} µs —— 差 {2:N0} 倍" -f $cacheUs, $s8, $ratio1)
Say ("  同样一次 I/O：4KB 随机 {0:N1} µs，128KB 顺序 {1:N1} µs —— 单次耗时差不多，但一次搬的数据多 32 倍" -f $s4, $s128)
$ratio2 = $rows[3].MBps / $rows[2].MBps
Say ("  4KB 随机读带宽 {0:N1} MB/s，128KB 顺序读带宽 {1:N1} MB/s —— 差 {2:N0} 倍" -f $rows[2].MBps, $rows[3].MBps, $ratio2)
Say '  也就是说：磁盘的成本主要按"发起多少次 I/O"算，不按"读多少字节"算。'
Say ("  一页 8KB 装 33 行，一次 I/O 就带回 33 行；按行读就要 33 次 I/O —— 这就是数据库按页读的物理原因。")

if (-not $KeepFile) { Remove-Item $path -Force }

$rawPath = 'docs\measure-io-raw.txt'
$header = @(
    '磁盘 I/O 物理成本实测 —— 真实运行输出',
    "生成时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
    "测试文件: $path ($SizeMB MB)",
    '复现: .\scripts\measure-disk-physics.ps1'
) -join "`r`n"
Set-Content -Path $rawPath -Value ($header + "`r`n" + $sb.ToString()) -Encoding utf8
Say ''
Say "原始输出已留档: $rawPath"
exit 0

