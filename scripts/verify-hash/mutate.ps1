# 变异测试(mutation testing):故意改坏实现,验证测试是否真的会红。
# 关键保证:
#   1) 被测源码只做临时替换,每个变异体跑完立即从 backup 恢复,最后校验 SHA256 与原始一致;
#   2) 若一个测试都没被找到(RunTests 退出码 2),判为失败,拦下"Tests run: 0 也算通过"的假通过;
#   3) 有变异体存活时脚本以退出码 1 结束,便于在 CI 中当作质量门。
# 用法见同目录 README.md。
param(
    [string]$Jdk8 = "C:\Program Files\Java\jdk-1.8",
    [string]$MavenRepo = "D:\maven-repo"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$root    = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$work    = Join-Path $root "target\dsh-mutation"
$classes = Join-Path $work "classes"
$backup  = Join-Path $work "backup"
$jdk8    = Join-Path $Jdk8 "bin"
$mainDir = Join-Path $root "src\main\java\cn\exercise\algs4\datastructure\hash"
$testDir = Join-Path $root "src\test\java\cn\exercise\algs4\datastructure\hash"

if (-not (Test-Path (Join-Path $jdk8 "javac.exe"))) { throw "找不到 JDK 8:$jdk8" }

$M2 = $MavenRepo
$junitJars = @(
    "$M2\org\junit\jupiter\junit-jupiter-api\5.9.2\junit-jupiter-api-5.9.2.jar",
    "$M2\org\junit\jupiter\junit-jupiter-engine\5.9.2\junit-jupiter-engine-5.9.2.jar",
    "$M2\org\junit\platform\junit-platform-commons\1.9.2\junit-platform-commons-1.9.2.jar",
    "$M2\org\junit\platform\junit-platform-engine\1.9.2\junit-platform-engine-1.9.2.jar",
    "$M2\org\junit\platform\junit-platform-launcher\1.9.2\junit-platform-launcher-1.9.2.jar",
    "$M2\org\opentest4j\opentest4j\1.2.0\opentest4j-1.2.0.jar",
    "$M2\org\apiguardian\apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar"
)
foreach ($jar in $junitJars) {
    if (-not (Test-Path $jar)) { throw "缺少依赖 jar:$jar(可用 -MavenRepo 指定本地仓库)" }
}
$junitCp = $junitJars -join ";"

$mainFiles   = @("DirectAddressHashST", "DivisionHashST")
$testFiles   = @("DirectAddressHashSTTest", "DivisionHashSTTest", "HashSTDifferentialTest")
$testClasses = $testFiles | ForEach-Object { "cn.exercise.algs4.datastructure.hash.$_" }

Remove-Item -Recurse -Force $classes, $backup -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $classes, $backup | Out-Null

foreach ($f in $mainFiles) { Copy-Item (Join-Path $mainDir "$f.java") (Join-Path $backup "$f.java") -Force }
$origHash = @{}
foreach ($f in $mainFiles) { $origHash[$f] = (Get-FileHash (Join-Path $mainDir "$f.java") -Algorithm SHA256).Hash }

$cp = "$classes;$junitCp"
$null = & (Join-Path $jdk8 "javac.exe") -encoding UTF-8 -cp $junitCp -d $classes (Join-Path $PSScriptRoot "RunTests.java")
if ($LASTEXITCODE -ne 0) { throw "运行器编译失败" }
$null = & (Join-Path $jdk8 "javac.exe") -encoding UTF-8 -Xlint:none -cp $cp -d $classes (Join-Path $mainDir "DirectAddressHashST.java") (Join-Path $mainDir "DivisionHashST.java")
if ($LASTEXITCODE -ne 0) { throw "主类编译失败" }
$null = & (Join-Path $jdk8 "javac.exe") -encoding UTF-8 -Xlint:none -cp $cp -d $classes (Join-Path $testDir "DirectAddressHashSTTest.java") (Join-Path $testDir "DivisionHashSTTest.java") (Join-Path $testDir "HashSTDifferentialTest.java")
if ($LASTEXITCODE -ne 0) { throw "测试类编译失败" }

function Invoke-Suite {
    $out = & (Join-Path $jdk8 "java.exe") "-Dfile.encoding=UTF-8" -cp $cp RunTests @testClasses 2>&1
    return @{ out = (($out | Out-String).Trim()); code = $LASTEXITCODE }
}

$base = Invoke-Suite
"BASELINE(原始实现) exit=$($base.code)  $($base.out)"
if ($base.code -ne 0) {
    Copy-Item (Join-Path $backup "DirectAddressHashST.java") (Join-Path $mainDir "DirectAddressHashST.java") -Force
    Copy-Item (Join-Path $backup "DivisionHashST.java") (Join-Path $mainDir "DivisionHashST.java") -Force
    throw "基线测试未通过,先修实现/测试再谈变异测试"
}
""

$mutations = @(
    @{ n = "M1 hash 去掉 floorMod(负数取模错误)"; f = "DivisionHashST";
       find = "return Math.floorMod(key, modulus);"; repl = "return key % modulus;" },
    @{ n = "M2 delete 不留墓碑"; f = "DivisionHashST";
       find = "table[i] = TOMBSTONE;"; repl = "table[i] = null;" },
    @{ n = "M3 put 不复用墓碑"; f = "DivisionHashST";
       find = "                    table[firstTombstone] = new Entry(key, value);"; repl = "                    table[i] = new Entry(key, value);" },
    @{ n = "M4 ASL成功分母用表长而非元素个数"; f = "DivisionHashST";
       find = "return n == 0 ? 0.0 : (double) successfulProbeSum() / n;"; repl = "return n == 0 ? 0.0 : (double) successfulProbeSum() / tableSize;" },
    @{ n = "M5 模数不做质数筛选"; f = "DivisionHashST";
       find = "return largestPrimeNotGreaterThan(tableSize);"; repl = "return tableSize;" },
    @{ n = "M6 质数判定边界改错(d*d<=n -> d*d<n)"; f = "DivisionHashST";
       find = "for (int d = 2; (long) d * d <= n; d++) {"; repl = "for (int d = 2; d * d < n; d++) {" },
    @{ n = "M7 失败查找探测次数计数少 1"; f = "DivisionHashST";
       find = "            int probes = 1;`n            int i = start;"; repl = "            int probes = 0;`n            int i = start;" },
    @{ n = "M8 直接定址忘记减去 minAddr"; f = "DirectAddressHashST";
       find = "return (int) (a * key + b - minAddr);"; repl = "return (int) (a * key + b);" },
    @{ n = "M9 地址空间少 1(hi-lo+1 -> hi-lo)"; f = "DirectAddressHashST";
       find = "long size = hi - lo + 1;"; repl = "long size = hi - lo;" }
)

$killed = 0
$survived = @()
$notApplied = @()

foreach ($m in $mutations) {
    $src = Join-Path $mainDir "$($m.f).java"
    Copy-Item (Join-Path $backup "$($m.f).java") $src -Force
    $text = (Get-Content -Raw -Encoding UTF8 $src) -replace "`r`n", "`n"
    if (-not $text.Contains($m.find)) { $notApplied += $m.n; "NOT APPLIED(源码已变动,请更新脚本): $($m.n)"; continue }
    $mutated = $text.Replace($m.find, $m.repl)
    [System.IO.File]::WriteAllText($src, $mutated, (New-Object System.Text.UTF8Encoding($false)))

    $null = & (Join-Path $jdk8 "javac.exe") -encoding UTF-8 -Xlint:none -cp $cp -d $classes $src 2>&1
    if ($LASTEXITCODE -ne 0) {
        "MUTANT COMPILE FAILED: $($m.n)"
        Copy-Item (Join-Path $backup "$($m.f).java") $src -Force
        continue
    }

    $r = Invoke-Suite
    $verdict = if ($r.code -ne 0) { "KILLED" } else { "SURVIVED" }
    if ($r.code -ne 0) { $killed++ } else { $survived += $m.n }
    "{0,-42} {1,-9} {2}" -f $m.n, $verdict, $r.out
    Copy-Item (Join-Path $backup "$($m.f).java") $src -Force
}

""
"=== 恢复校验 ==="
$restoreOk = $true
foreach ($f in $mainFiles) {
    $now = (Get-FileHash (Join-Path $mainDir "$f.java") -Algorithm SHA256).Hash
    $same = ($now -eq $origHash[$f])
    if (-not $same) { $restoreOk = $false }
    "{0}: 与原始一致 = {1}" -f $f, $same
}
""
"变异体总数 = $($mutations.Count),被杀死 = $killed,存活 = $($survived.Count),未应用 = $($notApplied.Count)"
if ($survived.Count -gt 0) { "存活变异体: " + ($survived -join "; ") }
if ($notApplied.Count -gt 0) { "未应用: " + ($notApplied -join "; ") }

if (-not $restoreOk) { throw "源码恢复校验失败!请用 git diff 检查" }
if ($survived.Count -gt 0 -or $notApplied.Count -gt 0 -or $killed -ne $mutations.Count) {
    exit 1
}
"全部变异体被杀死,测试对上述 9 类缺陷是有效的。"
exit 0
