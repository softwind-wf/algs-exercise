# 审计第三方提交的变异体 + 真红跑复核。
#
# 用途:当独立测试方提交 "变异体 + 击杀结论" 时,实现方用它复验,而不是采信文字。
#   Part 0: 把 src/test/.../independent/mut/ 下的每个变异体还原包名/类名后与冻结实现做行级比对,
#           确认"单点注入"属实(差异行内容应与提交方声称的缺陷一致);
#   Part 1: 把每个变异逐个注入**实现副本**(绝不修改 src/main),用它自己的盲测真实运行一次,
#           期望变红(RED)。只有变红才算真正被杀死;"断言变异体偏离期望"不算击杀证据。
#
# 用法:
#   pwsh -File scripts\verify-hash\audit-mutants.ps1
#   pwsh -File scripts\verify-hash\audit-mutants.ps1 -MutantDir <目录> -BlindTests <类名,类名>
param(
    [string]$Jdk8 = "C:\Program Files\Java\jdk-1.8",
    [string]$MavenRepo = "D:\maven-repo",
    [string]$MutantDir,
    [string[]]$BlindTests
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$root    = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$jdk8    = Join-Path $Jdk8 "bin"
$mainDir = Join-Path $root "src\main\java\cn\exercise\algs4\datastructure\hash"
$indDir  = Join-Path $root "src\test\java\cn\exercise\algs4\datastructure\hash\independent"
if (-not $MutantDir) { $MutantDir = Join-Path $indDir "mut" }
if (-not $BlindTests -or $BlindTests.Count -eq 0) {
    $BlindTests = @(
        "cn.exercise.algs4.datastructure.hash.independent.DirectAddressBlindTest",
        "cn.exercise.algs4.datastructure.hash.independent.DivisionHashBlindTest"
    )
}

$work   = Join-Path $root "target\dsh-audit-mutants"
$srcDir = Join-Path $work "src"
$runDir = Join-Path $work "runner"

$junitJars = @(
    "$MavenRepo\org\junit\jupiter\junit-jupiter-api\5.9.2\junit-jupiter-api-5.9.2.jar",
    "$MavenRepo\org\junit\jupiter\junit-jupiter-engine\5.9.2\junit-jupiter-engine-5.9.2.jar",
    "$MavenRepo\org\junit\platform\junit-platform-commons\1.9.2\junit-platform-commons-1.9.2.jar",
    "$MavenRepo\org\junit\platform\junit-platform-engine\1.9.2\junit-platform-engine-1.9.2.jar",
    "$MavenRepo\org\junit\platform\junit-platform-launcher\1.9.2\junit-platform-launcher-1.9.2.jar",
    "$MavenRepo\org\opentest4j\opentest4j\1.2.0\opentest4j-1.2.0.jar",
    "$MavenRepo\org\apiguardian\apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar"
) -join ";"

Remove-Item -Recurse -Force $work -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $srcDir, $runDir | Out-Null

$utf8 = New-Object System.Text.UTF8Encoding($false)
$frozen = @{}
foreach ($f in @("DirectAddressHashST", "DivisionHashST")) {
    $frozen[$f] = ((Get-Content -Raw -Encoding UTF8 (Join-Path $mainDir "$f.java")) -replace "`r`n", "`n")
}

# ---------- Part 0:变异体单点注入审计 ----------
"=== Part 0:变异体与冻结实现的行级差异(应恰好 1 处) ==="
foreach ($file in (Get-ChildItem -Path $MutantDir -Filter *.java | Sort-Object Name)) {
    $raw = (Get-Content -Raw -Encoding UTF8 $file.FullName) -replace "`r`n", "`n"
    if ($raw -match 'class\s+(\w*Mutant\w*)') { $real = $matches[1] } else { $real = "" }
    if (-not $real) { "  {0}: 未识别出变异体类名,跳过" -f $file.Name; continue }
    $base = if ($real -like "Direct*") { "DirectAddressHashST" } else { "DivisionHashST" }
    $norm = $raw.Replace("package cn.exercise.algs4.datastructure.hash.independent.mut;",
                         "package cn.exercise.algs4.datastructure.hash;").Replace($real, $base)
    $a = $frozen[$base] -split "`n"
    $b = $norm -split "`n"
    $count = 0
    for ($i = 0; $i -lt [Math]::Min($a.Count, $b.Count); $i++) {
        if ($a[$i] -ne $b[$i]) {
            $count++
            "  {0} 行 {1}:`n      原始: {2}`n      变异: {3}" -f $file.Name, ($i + 1), $a[$i].Trim(), $b[$i].Trim()
        }
    }
    "{0,-34} 差异 {1} 处" -f $file.Name, $count
}
""

# ---------- Part 1:真红跑 ----------
"=== Part 1:注入实现副本 + 用盲测真跑(期望 RED) ==="
$null = & (Join-Path $jdk8 "javac.exe") -encoding UTF-8 -cp $junitJars -d $runDir (Join-Path $PSScriptRoot "RunTests.java")
if ($LASTEXITCODE -ne 0) { throw "运行器编译失败" }

$mutations = @(
    @{ n = "M1 floorMod -> %";                     f = "DivisionHashST";      find = "Math.floorMod(key, modulus)"; repl = "(key % modulus)" },
    @{ n = "M2 delete 不留墓碑";                    f = "DivisionHashST";      find = "table[i] = TOMBSTONE;"; repl = "table[i] = null;" },
    @{ n = "M3 loadFactor 分母 -> modulus";         f = "DivisionHashST";      find = "return (double) n / tableSize;"; repl = "return (double) n / modulus;" },
    @{ n = "M4 失败探测起点上界 -> modulus";         f = "DivisionHashST";      find = "for (int start = 0; start < tableSize; start++)"; repl = "for (int start = 0; start < modulus; start++)" },
    @{ n = "M5 直接定址去掉 - minAddr";              f = "DirectAddressHashST"; find = "return (int) (a * key + b - minAddr);"; repl = "return (int) (a * key + b);" },
    @{ n = "M6 去掉 null 值校验";                    f = "DirectAddressHashST"; find = "Objects.requireNonNull(value"; repl = "if (false) Objects.requireNonNull(value" },
    @{ n = "M7 地址空间少 1";                        f = "DirectAddressHashST"; find = "long size = hi - lo + 1;"; repl = "long size = hi - lo;" },
    @{ n = "M8 去掉下界检查";                        f = "DirectAddressHashST"; find = "if (key < minKey || key > maxKey) {"; repl = "if (key > maxKey) {" }
)

$verified = 0
foreach ($m in $mutations) {
    $cDir = Join-Path $work ("classes-" + ($m.n -replace "[^\w]", ""))
    New-Item -ItemType Directory -Force $cDir | Out-Null
    foreach ($f in @("DirectAddressHashST", "DivisionHashST")) {
        [System.IO.File]::WriteAllText((Join-Path $srcDir "$f.java"), $frozen[$f], $utf8)
    }
    $target = Join-Path $srcDir "$($m.f).java"
    $text = [System.IO.File]::ReadAllText($target, [System.Text.Encoding]::UTF8)
    if (-not $text.Contains($m.find)) { "MUTATION NOT APPLIED(源码已变动): $($m.n)"; continue }
    [System.IO.File]::WriteAllText($target, $text.Replace($m.find, $m.repl), $utf8)

    $testFiles = $BlindTests | ForEach-Object { Join-Path $indDir (($_.Split('.')[-1]) + ".java") }
    $compileOut = & (Join-Path $jdk8 "javac.exe") -encoding UTF-8 -Xlint:none -cp $junitJars -d $cDir `
        (Join-Path $srcDir "DirectAddressHashST.java") (Join-Path $srcDir "DivisionHashST.java") @testFiles 2>&1
    if ($LASTEXITCODE -ne 0) { "COMPILE FAILED: $($m.n)"; $compileOut | Select-Object -First 5; continue }

    $out = & (Join-Path $jdk8 "java.exe") "-Dfile.encoding=UTF-8" -cp "$cDir;$runDir;$junitJars" RunTests @BlindTests 2>&1
    $code = $LASTEXITCODE
    $summary = ($out | Select-String -Pattern "^FOUND=").Line
    $verdict = if ($code -ne 0) { "RED(真被杀死)" } else { "GREEN(从未变红!)" }
    if ($code -ne 0) { $verified++ }
    "{0,-30} {1}  {2}" -f $m.n, $verdict, $summary
}
""
"真红跑: $verified / $($mutations.Count) 个变异体确实能让盲测变红"
if ($verified -ne $mutations.Count) { exit 1 }
exit 0
