<#
.SYNOPSIS
    从磁盘最底层往上走：物理扇区 → 文件系统簇 → 文件字节 → 数据库页 → 元组字段。

.DESCRIPTION
    全程不依赖数据库的"抽象"，而是：
      1) 用 Win32 API / 注册表读出这块盘的型号与扇区大小（物理 4KB、逻辑 512B）；
      2) 用 IOCTL 读出 NTFS 的簇大小（4KB），并读出数据文件在磁盘上实际占的物理簇位置；
      3) 按 8192 字节把数据文件切成页，用 MD5 证明"数据库里的页 == 磁盘上的这 8192 字节"；
      4) 把页头的每个字段、行指针的每个位段、元组的每个字节逐条解码成人能看懂的字段；
      5) 实测从文件读一页要多久。

    输出同时写进 docs/inspect-datafile-bytes-raw.txt。

.PARAMETER Table
    要解剖的表名，默认 t_demo（由 run-storage-page-demo.ps1 创建的演示表）。

.EXAMPLE
    .\scripts\inspect-datafile-bytes.ps1
    .\scripts\inspect-datafile-bytes.ps1 -Table t_demo -TupleSlot 1
#>
[CmdletBinding()]
param(
    [string]$PgHost = '127.0.0.1',
    [int]$PgPort = 0,
    [string]$PgUser = '',
    [string]$PgPassword = '',
    [string]$PgDatabase = 'storage_demo',
    [string]$Table = 't_demo',
    [int]$TupleSlot = 1
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# ============ Win32 interop ============
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

public static class NativeFs {
    [DllImport("kernel32.dll", SetLastError=true, CharSet=CharSet.Auto)]
    public static extern bool GetDiskFreeSpace(string root, out uint sectorsPerCluster, out uint bytesPerSector, out uint freeClusters, out uint totalClusters);

    [DllImport("kernel32.dll", SetLastError=true, CharSet=CharSet.Auto, EntryPoint="CreateFileW")]
    public static extern IntPtr CreateFile(string name, uint access, uint share, IntPtr sec, uint disp, uint flags, IntPtr tmpl);

    [DllImport("kernel32.dll", SetLastError=true)]
    public static extern bool DeviceIoControl(IntPtr h, uint code, byte[] inBuf, uint inSize, byte[] outBuf, uint outSize, out uint returned, IntPtr ov);

    [DllImport("kernel32.dll")]
    public static extern bool CloseHandle(IntPtr h);
}
"@

function Get-VolumeGeometry([string]$root) {
    $spc = 0; $bps = 0; $fc = 0; $tc = 0
    if (-not [NativeFs]::GetDiskFreeSpace($root, [ref]$spc, [ref]$bps, [ref]$fc, [ref]$tc)) {
        throw "GetDiskFreeSpace 失败: $root"
    }
    return [pscustomobject]@{ SectorsPerCluster = $spc; BytesPerSector = $bps; ClusterSize = $spc * $bps; TotalClusters = $tc }
}

function Get-SectorAlignment([string]$volume) {
    # IOCTL_STORAGE_QUERY_PROPERTY + StorageAccessAlignmentProperty
    $name = '\\.\' + $volume.TrimEnd(':') + ':'
    $h = [NativeFs]::CreateFile($name, 0, 3, [IntPtr]::Zero, 3, 0, [IntPtr]::Zero)
    if ($h -eq [IntPtr]::Zero -or $h -eq [IntPtr](-1)) { return $null }
    try {
        $inBuf = New-Object byte[] 12
        [BitConverter]::GetBytes([int]6).CopyTo($inBuf, 0)   # StorageAccessAlignmentProperty
        [BitConverter]::GetBytes([int]0).CopyTo($inBuf, 4)   # PropertyStandardQuery
        $outBuf = New-Object byte[] 256
        $ret = 0
        if (-not [NativeFs]::DeviceIoControl($h, [uint32]0x2D1400, $inBuf, 12, $outBuf, 256, [ref]$ret, [IntPtr]::Zero)) {
            return $null
        }
        return [pscustomobject]@{
            BytesPerLogicalSector  = [BitConverter]::ToUInt32($outBuf, 16)
            BytesPerPhysicalSector = [BitConverter]::ToUInt32($outBuf, 20)
        }
    } finally {
        [void][NativeFs]::CloseHandle($h)
    }
}

function Get-FileExtents([string]$path) {
    # FSCTL_GET_RETRIEVAL_POINTERS
    $h = [NativeFs]::CreateFile($path, [uint32]2147483648, 3, [IntPtr]::Zero, 3, 0, [IntPtr]::Zero)
    if ($h -eq [IntPtr]::Zero -or $h -eq [IntPtr](-1)) { return $null }
    try {
        $inBuf = New-Object byte[] 8      # StartingVcn = 0
        $outBuf = New-Object byte[] 1024
        $ret = 0
        if (-not [NativeFs]::DeviceIoControl($h, [uint32]0x00090073, $inBuf, 8, $outBuf, 1024, [ref]$ret, [IntPtr]::Zero)) {
            return $null
        }
        $count = [BitConverter]::ToUInt32($outBuf, 0)
        $vcn = [BitConverter]::ToInt64($outBuf, 8)
        $list = @()
        for ($i = 0; $i -lt $count; $i++) {
            $o = 16 + $i * 16
            $next = [BitConverter]::ToInt64($outBuf, $o)
            $lcn = [BitConverter]::ToInt64($outBuf, $o + 8)
            $list += [pscustomobject]@{ StartVcn = $vcn; NextVcn = $next; Lcn = $lcn }
            $vcn = $next
        }
        return $list
    } finally {
        [void][NativeFs]::CloseHandle($h)
    }
}

# ============ psql ============
function Read-Props([string]$path) {
    $map = @{}
    if (Test-Path $path) {
        foreach ($line in Get-Content $path) {
            if ($line -match '^\s*([A-Za-z_]+)\s*=\s*(.*?)\s*$') { $map[$matches[1].ToLowerInvariant()] = $matches[2] }
        }
    }
    return $map
}

$pg = Read-Props 'src\main\resources\pg.properties'
if (-not $PgPort) { $PgPort = [int]($pg['port'] ?? '5432') }
if (-not $PgUser) { $PgUser = ($pg['user'] ?? 'postgres') }
if (-not $PgPassword) { $PgPassword = ($pg['password'] ?? '') }
$env:PGPASSWORD = $PgPassword
$env:PGCLIENTENCODING = 'UTF8'

function Find-Psql {
    $c = Get-Command psql.exe -ErrorAction SilentlyContinue
    if ($c) { return $c.Source }
    foreach ($v in @('17', '16', '15', '14')) {
        $p = Join-Path $env:ProgramFiles "PostgreSQL\$v\bin\psql.exe"
        if (Test-Path $p) { return $p }
    }
    throw '找不到 psql.exe'
}
$psql = Find-Psql
function Q([string]$sql) {
    (& $psql -U $PgUser -h $PgHost -p "$PgPort" -X -q -d $PgDatabase -Atc $sql)
}

# ============ helpers ============
function Get-U16([byte[]]$b, [int]$o) { [BitConverter]::ToUInt16($b, $o) }
function Get-U32([byte[]]$b, [int]$o) { [BitConverter]::ToUInt32($b, $o) }

$sb = [System.Text.StringBuilder]::new()
function Say([string]$text) {
    [void]$sb.Append($text + "`r`n")
    Write-Host $text
}
function Section([string]$title) {
    Say ''
    Say ('=' * 78)
    Say $title
}

function Format-HexDump([byte[]]$b, [int]$start, [int]$length) {
    $lines = @()
    for ($i = 0; $i -lt $length; $i += 16) {
        $s = $start + $i
        $n = [Math]::Min(16, $length - $i)
        $chunk = $b[$s..($s + $n - 1)]
        $hex = ($chunk | ForEach-Object { $_.ToString('x2') }) -join ' '
        $ascii = ($chunk | ForEach-Object { if ($_ -ge 32 -and $_ -le 126) { [char]$_ } else { '.' } }) -join ''
        $lines += ('  {0,6}  {1,-47}  |{2}|' -f $s, $hex, $ascii)
    }
    return ($lines -join "`r`n")
}

function Read-Varlena([byte[]]$b, [int]$offset) {
    # 1 字节头: 头 = (len << 1) | 1；4 字节头: 低 30 位 = len << 2
    $b0 = $b[$offset]
    if (($b0 -band 0x01) -eq 0x01) {
        $len = ($b0 -shr 1) -band 0x7F
        return [pscustomobject]@{ Kind = '1B'; HeaderBytes = 1; Length = $len; DataOffset = $offset + 1; DataLength = $len - 1 }
    }
    elseif (($b0 -band 0x03) -eq 0x00) {
        $raw = Get-U32 $b $offset
        $len = $raw -shr 2
        return [pscustomobject]@{ Kind = '4B'; HeaderBytes = 4; Length = $len; DataOffset = $offset + 4; DataLength = $len - 4 }
    }
    else {
        return [pscustomobject]@{ Kind = 'external'; HeaderBytes = 1; Length = 0; DataOffset = $offset + 1; DataLength = 0 }
    }
}

# ============ 0. 硬件 + 文件系统 ============
$relPath = (Q "SELECT pg_relation_filepath('$Table')").Trim()
$dataDir = (Q "SELECT current_setting('data_directory')").Trim()
$file = ($dataDir + '/' + $relPath).Replace('/', '\')
if (-not (Test-Path $file)) { throw "找不到数据文件: $file — 先跑 scripts\run-storage-page-demo.ps1 建演示数据" }

Q 'CHECKPOINT;' | Out-Null      # 保证磁盘上的内容和数据库里一致

$bytes = [System.IO.File]::ReadAllBytes($file)
$blockSize = [int](Q 'SHOW block_size;')
$pageCount = [int][Math]::Ceiling($bytes.Length / $blockSize)
$drive = (Split-Path -Qualifier $file)
$geo = Get-VolumeGeometry ($drive + '\')
$align = Get-SectorAlignment $drive

$diskModel = ''
try { $diskModel = (Get-ItemProperty 'HKLM:\SYSTEM\CurrentControlSet\Services\disk\Enum').'0' } catch { }

Section '0. 最底层: 磁盘与文件系统'
Say "物理磁盘          : $diskModel"
Say "数据文件          : $file"
Say "文件大小          : $($bytes.Length) 字节 = $pageCount 页"
Say "文件系统          : $drive 簇大小 $($geo.ClusterSize) 字节（$($geo.SectorsPerCluster) 个扇区 × $($geo.BytesPerSector) 字节）"
if ($align) {
    Say "逻辑扇区(系统报告): $($align.BytesPerLogicalSector) 字节"
    Say "物理扇区(设备上报): $($align.BytesPerPhysicalSector) 字节"
    Say ''
    Say ("换算: 1 页 = {0} 字节 = {1} 个文件系统簇 = {2} 个物理扇区" -f $blockSize, ($blockSize / $geo.ClusterSize), ($blockSize / $align.BytesPerPhysicalSector))
} else {
    Say '（拿不到扇区对齐信息）'
}

# ============ 1. 文件在磁盘上的物理位置 ============
Section '1. 这个文件在磁盘上到底占哪几块'
$extents = Get-FileExtents $file
if ($extents) {
    foreach ($e in $extents) {
        $clusters = $e.NextVcn - $e.StartVcn
        Say ("  VCN {0}..{1}  ->  LCN {2}   共 {3} 个簇 / {4} 字节，卷内字节偏移 {5}" -f `
                $e.StartVcn, $e.NextVcn, $e.Lcn, $clusters, ($clusters * $geo.ClusterSize), ($e.Lcn * $geo.ClusterSize))
    }
    Say ''
    Say '  VCN = 文件内的簇序号（从 0 数），LCN = 卷内的簇序号；文件系统维护 "VCN -> LCN" 的映射（extents 表）。'
    Say '  文件在用户态看起来是一整串连续字节，物理上可以是几段不连续的簇。'
} else {
    Say '  （当前权限拿不到 extent 信息，跳过）'
}

# ============ 2. 文件切页 ============
Section '2. 文件就是字节数组，按 block_size 切页'
for ($p = 0; $p -lt $pageCount; $p++) {
    $start = $p * $blockSize
    Say ("  第 {0} 页 : 文件偏移 {1,6} ~ {2,6}   (VCN {3}..{4})" -f $p, $start, ($start + $blockSize - 1), ($start / $geo.ClusterSize), (($start + $blockSize) / $geo.ClusterSize))
}

# ============ 3. 数据库页 == 磁盘字节 ============
Section '3. 证明: 数据库里的页 == 文件里的这 8192 字节'
for ($p = 0; $p -lt $pageCount; $p++) {
    $md5 = [System.Security.Cryptography.MD5]::Create()
    $h = ($md5.ComputeHash($bytes, $p * $blockSize, $blockSize) | ForEach-Object { $_.ToString('x2') }) -join ''
    $dbMd5 = (Q "SELECT md5(get_raw_page('$Table', $p))").Trim()
    $same = if ($dbMd5 -eq $h) { '一致' } else { '不一致' }
    Say ("  第 {0} 页  DB 算出的 md5 = {1}  文件算出的 md5 = {2}  [{3}]" -f $p, $dbMd5, $h, $same)
}

# ============ 4. 页头字节 ============
$page0 = New-Object byte[] $blockSize
[Array]::Copy($bytes, 0, $page0, 0, $blockSize)

Section '4. 第 0 页最前面 64 字节（十六进制 + ASCII）'
Say (Format-HexDump $page0 0 64)

Section '5. 页头 24 字节，逐字段解码'
$pdLsnHigh = Get-U32 $page0 0
$pdLsnLow  = Get-U32 $page0 4
$pdLower = Get-U16 $page0 12
$pdUpper = Get-U16 $page0 14
$pdSpecial = Get-U16 $page0 16
$pdVersion = Get-U16 $page0 18
$pdPrune = Get-U32 $page0 20
Say ("  偏移  0..7   pd_lsn            = {0:X}/{1:X}  (本页最后一次修改的 WAL 位置, 段号/段内字节偏移)" -f $pdLsnHigh, $pdLsnLow)
Say ("  偏移  8..9   pd_checksum       = 0x{0:X4}  (0 = 本机未开启页校验)" -f (Get-U16 $page0 8))
Say ("  偏移 10..11  pd_flags          = 0x{0:X4}" -f (Get-U16 $page0 10))
Say ("  偏移 12..13  pd_lower          = {0}   (已用部分末尾 / 空闲空间起点)" -f $pdLower)
Say ("  偏移 14..15  pd_upper          = {0}   (空闲空间末尾 / 元组区起点)" -f $pdUpper)
Say ("  偏移 16..17  pd_special        = {0}   (特殊区起点)" -f $pdSpecial)
Say ("  偏移 18..19  pd_pagesize_ver   = 0x{0:X4}  (高 13 位 {1} = 页大小, 低 3 位 {2} = 格式版本)" -f $pdVersion, ($pdVersion -band 0xFFF8), ($pdVersion -band 0x0007))
Say ("  偏移 20..23  pd_prune_xid      = {0}" -f $pdPrune)
Say ''
Say ("  空闲空间 = pd_upper - pd_lower = {0} - {1} = {2} 字节" -f $pdUpper, $pdLower, ($pdUpper - $pdLower))

# ============ 6. 行指针数组 ============
Section '6. 行指针数组（每项 4 字节，从偏移 24 开始）'
$lpCount = [int][math]::Floor(($pdLower - 24) / 4)
Say "  本页共 $lpCount 个行指针"
Say '  位段: 高 15 位 = 元组长度, 中间 2 位 = 状态(0 未用/1 正常/2 重定向/3 死), 低 15 位 = 元组页内偏移'
Say ''
Say '  lp   原始4字节      lp_off   flags   lp_len'
for ($i = 0; $i -lt [Math]::Min($lpCount, 5); $i++) {
    $o = 24 + $i * 4
    $raw = Get-U32 $page0 $o
    $hexRaw = (($page0[$o..($o + 3)] | ForEach-Object { $_.ToString('x2') }) -join ' ')
    Say ("  {0,2}   {1}   {2,7}   {3,5}   {4,6}" -f ($i + 1), $hexRaw, ($raw -band 0x7FFF), (($raw -shr 15) -band 0x3), ($raw -shr 17))
}
if ($lpCount -gt 5) { Say '  ...' }

# ============ 7. 元组逐字节 ============
$slot = $TupleSlot
$lpRaw = Get-U32 $page0 (24 + ($slot - 1) * 4)
$tupOff = $lpRaw -band 0x7FFF
$tupLen = $lpRaw -shr 17

Section "7. 第 $slot 个元组的 $tupLen 字节，逐字段解码（页内偏移 $tupOff）"
Say (Format-HexDump $page0 $tupOff ([Math]::Min(64, $tupLen)))
Say ''
$t = New-Object byte[] $tupLen
[Array]::Copy($page0, $tupOff, $t, 0, $tupLen)

$tXmin = Get-U32 $t 0
$tXmax = Get-U32 $t 4
$tField3 = Get-U32 $t 8
$tCtidBlk = Get-U32 $t 12
$tCtidPos = Get-U16 $t 16
$tInfomask2 = Get-U16 $t 18
$tInfomask = Get-U16 $t 20
$tHoff = $t[22]
$natts = $tInfomask2 -band 0x07FF

Say '  --- 元组头 (23 字节) ---'
Say ("  +0   .. +3     t_xmin      = {0}      (插入这一行的事务号)" -f $tXmin)
Say ("  +4   .. +7     t_xmax      = {0}      (0 = 没被删/没被更新；非 0 = 删除事务号)" -f $tXmax)
Say ("  +8   .. +11    t_field3    = {0}      (cmin/cmax: 同一事务内第几次改动)" -f $tField3)
Say ("  +12  .. +15    t_ctid.block= {0}" -f $tCtidBlk)
Say ("  +16  .. +17    t_ctid.posid= {0}      (合起来就是这一行的物理地址 ctid)" -f $tCtidPos)
Say ("  +18  .. +19    t_infomask2 = 0x{0:X4}  (低 11 位 = 属性个数 = {1})" -f $tInfomask2, $natts)
Say ("  +20  .. +21    t_infomask  = 0x{0:X4}" -f $tInfomask)
Say ("  +22           t_hoff      = {0}      (数据从元组第几个字节开始)" -f $tHoff)

# 属性类型（从 catalog 查，用来决定怎么解字段）
$cols = (Q "SELECT string_agg(attname || ':' || format_type(atttypid, atttypmod), ',' ORDER BY attnum) FROM pg_attribute WHERE attrelid = '$Table'::regclass AND attnum > 0 AND NOT attisdropped;").Trim()
$colDefs = @()
foreach ($c in $cols.Split(',')) {
    $pair = $c.Split(':')
    $colDefs += [pscustomobject]@{ Name = $pair[0]; Type = $pair[1] }
}

$alignMap = @{ 'int2' = 2; 'int4' = 4; 'int8' = 8; 'text' = 4; 'character varying' = 4; 'character' = 4; 'boolean' = 1; 'numeric' = 4 }
function Get-TypeAlign([string]$t) {
    $base = ($t -replace '\(.*$', '').Trim()
    if ($alignMap.ContainsKey($base)) { return $alignMap[$base] }
    return 4
}

Say ''
Say '  --- 属性区（每个字段从哪开始、占几个字节） ---'
Say '  字段            类型            元组内偏移   长度   值'
$off = $tHoff
$values = @{}
for ($a = 0; $a -lt $colDefs.Count; $a++) {
    $base = ($colDefs[$a].Type -replace '\(.*$', '').Trim()
    $al = Get-TypeAlign $colDefs[$a].Type
    $off += (($al - ($off % $al)) % $al)
    if ($base -eq 'text' -or $base -eq 'character varying' -or $base -eq 'character') {
        $v = Read-Varlena $t $off
        $txt = [Text.Encoding]::UTF8.GetString($t, $v.DataOffset, $v.DataLength)
        if ($base -eq 'character') { $txt = $txt.TrimEnd() }
        $values[$colDefs[$a].Name] = $txt
        $show = $txt
        if ($show.Length -gt 40) { $show = $show.Substring(0, 40) + '...' }
        Say ("  {0,-14} {1,-15} {2,10}   {3,4}   '{4}'  (变长头 {5} 字节, {6} 格式)" -f $colDefs[$a].Name, $colDefs[$a].Type, $off, $v.Length, $show, $v.HeaderBytes, $v.Kind)
        $off += $v.Length
    }
    elseif ($base -eq 'int2') {
        $val = [BitConverter]::ToInt16($t, $off)
        $values[$colDefs[$a].Name] = $val
        Say ("  {0,-14} {1,-15} {2,10}   {3,4}   {4}" -f $colDefs[$a].Name, $colDefs[$a].Type, $off, 2, $val)
        $off += 2
    }
    elseif ($base -eq 'int8') {
        $val = [BitConverter]::ToInt64($t, $off)
        $values[$colDefs[$a].Name] = $val
        Say ("  {0,-14} {1,-15} {2,10}   {3,4}   {4}" -f $colDefs[$a].Name, $colDefs[$a].Type, $off, 8, $val)
        $off += 8
    }
    else {
        $val = [BitConverter]::ToInt32($t, $off)
        $values[$colDefs[$a].Name] = $val
        Say ("  {0,-14} {1,-15} {2,10}   {3,4}   {4}" -f $colDefs[$a].Name, $colDefs[$a].Type, $off, 4, $val)
        $off += 4
    }
}
Say ''
Say "  解完占用到元组偏移 $off，元组总长 $tupLen（多出来的是对齐填充）"
Say ''
$vals = @()
foreach ($cd in $colDefs) { $vals += [string]$values[$cd.Name] }
Say ('  从磁盘字节直接还原出的这一行: ' + ($vals -join ' | '))

# ============ 8. 读一页要多久 ============
Section '8. 从文件读一页要多久（page cache 命中，所以是微秒级）'
$fs = [System.IO.File]::Open($file, 'Open', 'Read', [System.IO.FileShare]::ReadWrite)
try {
    $buf = New-Object byte[] $blockSize
    for ($i = 0; $i -lt 50; $i++) { $fs.Position = 0; [void]$fs.Read($buf, 0, $blockSize) }
    $n = 500
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    for ($i = 0; $i -lt $n; $i++) { $fs.Position = 0; [void]$fs.Read($buf, 0, $blockSize) }
    $sw.Stop()
    $us = $sw.Elapsed.TotalMilliseconds * 1000 / $n
    Say ("  顺序读同一页 {0} 次，平均 {1:N2} µs/次" -f $n, $us)
    Say '  这还只是"数据已经在文件系统缓存里"的成本；真落到 NVMe，一次 8KB 读大约几十微秒。'
} finally {
    $fs.Dispose()
}

# ============ 9. 收尾 ============
Section '9. 往上就是数据库自己的解释'
Say '  同样的 8192 字节：'
Say '    - 操作系统看到的是文件的一段字节；'
Say '    - PostgreSQL 看到的是"页头 + 行指针数组 + 元组区"；'
Say '    - 再往上，元组被 schema 解释成 id / name / pad 三列。'
Say '  层数不变，变的只是"谁按什么规则解释这些字节"。'

$rawPath = 'docs\inspect-datafile-bytes-raw.txt'
$header = @(
    '数据文件字节解剖 —— 真实运行输出',
    "生成时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
    "数据文件: $file",
    '复现: .\scripts\inspect-datafile-bytes.ps1'
) -join "`r`n"
Set-Content -Path $rawPath -Value ($header + "`r`n" + $sb.ToString()) -Encoding utf8
Say ''
Say "原始输出已留档: $rawPath"
exit 0

