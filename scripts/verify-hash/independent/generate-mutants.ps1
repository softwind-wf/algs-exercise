$ErrorActionPreference = 'Stop'
$root = 'D:\downloads\algs4-master\algs4-master\src'
$divSrc = Join-Path $root 'main\java\cn\exercise\algs4\datastructure\hash\DivisionHashST.java'
$dirSrc = Join-Path $root 'main\java\cn\exercise\algs4\datastructure\hash\DirectAddressHashST.java'
$outDir = Join-Path $root 'test\java\cn\exercise\algs4\datastructure\hash\independent\mut'
if (-not (Test-Path -LiteralPath $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$div = [System.IO.File]::ReadAllText($divSrc, [System.Text.Encoding]::UTF8)
$dir = [System.IO.File]::ReadAllText($dirSrc, [System.Text.Encoding]::UTF8)

function Emit($content, $oldClass, $newClass, $file) {
    $c = $content.Replace('package cn.exercise.algs4.datastructure.hash;', `
                          'package cn.exercise.algs4.datastructure.hash.independent.mut;')
    $c = $c.Replace($oldClass, $newClass)
    [System.IO.File]::WriteAllText($file, $c, $utf8NoBom)
}

$outBase = $outDir

# ---- Division mutants ----
# M1 floorMod -> %
$one = $div.Replace('Math.floorMod(key, modulus)', '(key % modulus)')
Emit $one 'DivisionHashST' 'DivisionMutantFloorMod' (Join-Path $outBase 'DivisionMutantFloorMod.java')

# M2 delete: no tombstone
$two = $div.Replace('table[i] = TOMBSTONE;', 'table[i] = null;')
Emit $two 'DivisionHashST' 'DivisionMutantNoTombstone' (Join-Path $outBase 'DivisionMutantNoTombstone.java')

# M3 loadFactor denominator tableSize -> modulus
$three = $div.Replace('return (double) n / tableSize;', 'return (double) n / modulus;')
Emit $three 'DivisionHashST' 'DivisionMutantLoadDenom' (Join-Path $outBase 'DivisionMutantLoadDenom.java')

# M4 unsuccessfulProbeSum start bound tableSize -> modulus
$four = $div.Replace('for (int start = 0; start < tableSize; start++)', 'for (int start = 0; start < modulus; start++)')
Emit $four 'DivisionHashST' 'DivisionMutantUnsuccBound' (Join-Path $outBase 'DivisionMutantUnsuccBound.java')

# ---- DirectAddress mutants ----
# M5 hash: drop - minAddr
$five = $dir.Replace('return (int) (a * key + b - minAddr);', 'return (int) (a * key + b);')
Emit $five 'DirectAddressHashST' 'DirectMutantHashNoMinAddr' (Join-Path $outBase 'DirectMutantHashNoMinAddr.java')

# M6 put: disable null check (ASCII-anchored)
$six = $dir.Replace('Objects.requireNonNull(value', 'if (false) Objects.requireNonNull(value')
Emit $six 'DirectAddressHashST' 'DirectMutantNoNullCheck' (Join-Path $outBase 'DirectMutantNoNullCheck.java')

# M7 capacity off-by-one (hi-lo+1 -> hi-lo)
$seven = $dir.Replace('long size = hi - lo + 1;', 'long size = hi - lo;')
Emit $seven 'DirectAddressHashST' 'DirectMutantCapacityOff' (Join-Path $outBase 'DirectMutantCapacityOff.java')

# M8 hash lower bound removed
$eight = $dir.Replace('if (key < minKey || key > maxKey) {', 'if (key > maxKey) {')
Emit $eight 'DirectAddressHashST' 'DirectMutantBoundNoLower' (Join-Path $outBase 'DirectMutantBoundNoLower.java')

Write-Output 'MUTANTS-GENERATED'
Get-ChildItem -LiteralPath $outBase -Filter *.java | ForEach-Object { $_.Name }
