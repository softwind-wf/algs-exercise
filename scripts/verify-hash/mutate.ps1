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

$mainFiles   = @("DirectAddressHashST", "DivisionHashST", "DigitAnalysisHashST", "MidSquareHashST", "FoldingHashST", "RandomHashST", "UniversalHashST")
$testFiles   = @("DirectAddressHashSTTest", "DivisionHashSTTest", "HashSTDifferentialTest",
                 "HashSTExhaustiveTest", "DigitAnalysisHashSTTest", "MidSquareHashSTTest",
                 "FoldingHashSTTest", "RandomHashSTTest", "UniversalHashSTTest")
$testClasses = $testFiles | ForEach-Object { "cn.exercise.algs4.datastructure.hash.$_" }

# 基线用例数下限(新增测试类/用例时同步上调):用于挡住"测试类没被编译或没被选中"的静默少跑
$minBaselineTests = 132

Remove-Item -Recurse -Force $classes, $backup -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $classes, $backup | Out-Null

foreach ($f in $mainFiles) { Copy-Item (Join-Path $mainDir "$f.java") (Join-Path $backup "$f.java") -Force }
$origHash = @{}
foreach ($f in $mainFiles) { $origHash[$f] = (Get-FileHash (Join-Path $mainDir "$f.java") -Algorithm SHA256).Hash }

$cp = "$classes;$junitCp"
$mainSources = $mainFiles | ForEach-Object { Join-Path $mainDir "$_.java" }
$testSources = $testFiles | ForEach-Object { Join-Path $testDir "$_.java" }
$null = & (Join-Path $jdk8 "javac.exe") -encoding UTF-8 -cp $junitCp -d $classes (Join-Path $PSScriptRoot "RunTests.java")
if ($LASTEXITCODE -ne 0) { throw "运行器编译失败" }
$null = & (Join-Path $jdk8 "javac.exe") -encoding UTF-8 -Xlint:none -cp $cp -d $classes @mainSources
if ($LASTEXITCODE -ne 0) { throw "主类编译失败" }
$null = & (Join-Path $jdk8 "javac.exe") -encoding UTF-8 -Xlint:none -cp $cp -d $classes @testSources
if ($LASTEXITCODE -ne 0) { throw "测试类编译失败" }

function Invoke-Suite {
    $out = & (Join-Path $jdk8 "java.exe") "-Dfile.encoding=UTF-8" -cp $cp RunTests @testClasses 2>&1
    return @{ out = (($out | Out-String).Trim()); code = $LASTEXITCODE }
}

$base = Invoke-Suite
"BASELINE(原始实现) exit=$($base.code)  $($base.out)"
# 防"静默少跑":RunTests 只在"一个测试都没找到"时报错,若某个测试类缺失(例如增量编译后
# class 未生成),总数会悄悄变小而结果依然全绿。这里用基线用例数下限把它挡住。
if ($base.out -notmatch 'FOUND=(\d+)') { throw "无法解析基线用例数:$($base.out)" }
$foundBaseline = [int]$Matches[1]
if ($foundBaseline -lt $minBaselineTests) {
    throw "基线只跑到 $foundBaseline 个用例(期望不少于 $minBaselineTests 个),疑似有测试类未被编译/执行,先排查再谈变异测试"
}
if ($base.code -ne 0) {
    foreach ($f in $mainFiles) {
        Copy-Item (Join-Path $backup "$f.java") (Join-Path $mainDir "$f.java") -Force
    }
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
       find = "long size = hi - lo + 1;"; repl = "long size = hi - lo;" },
    @{ n = "M10 数字分析法:拼地址用加代替乘 radix"; f = "DigitAnalysisHashST";
       find = "        checkKey(key);`n        int address = 0;`n        for (int i = positions.length - 1; i >= 0; i--) {`n            address = address * radix + digitOf(key, positions[i]);";
       repl = "        checkKey(key);`n        int address = 0;`n        for (int i = positions.length - 1; i >= 0; i--) {`n            address = address + digitOf(key, positions[i]);" },
    @{ n = "M11 数字分析法:位分布统计恒记到数字 0"; f = "DigitAnalysisHashST";
       find = "                counts[pos][digitOf(key, pos)]++;"; repl = "                counts[pos][0]++;" },
    @{ n = "M12 数字分析法:熵计算把空数字也算进去(log 0)"; f = "DigitAnalysisHashST";
       find = "            if (count > 0) {"; repl = "            if (count >= 0) {" },
    @{ n = "M13 数字分析法:delete 忘记减少 size"; f = "DigitAnalysisHashST";
       find = "                n--;`n                return node.value;"; repl = "                return node.value;" },
    @{ n = "M14 平方取中:中位窗口偏移算错"; f = "MidSquareHashST";
       find = "        int offset = (length - addressDigits) / 2;"; repl = "        int offset = (length - addressDigits + 1) / 2;" },
    @{ n = "M15 平方取中:去掉平方溢出检查"; f = "MidSquareHashST";
       find = "            return Math.multiplyExact(key, key);"; repl = "            return key * key;" },
    @{ n = "M16 平方取中:位数直接用 k 而非平方数的位数"; f = "MidSquareHashST";
       find = "        int length = decimalLength(square);"; repl = "        int length = addressDigits;" },
    @{ n = "M17 平方取中:delete 不留墓碑"; f = "MidSquareHashST";
       find = "        state[i] = TOMBSTONE;"; repl = "        state[i] = EMPTY;" },
    @{ n = "M18 折叠法:段的提取用除法代替取余"; f = "FoldingHashST";
       find = "            result[i] = (int) (rest % unit);"; repl = "            result[i] = (int) (rest / unit);" },
    @{ n = "M19 折叠法:分界叠加的奇偶段判定反了"; f = "FoldingHashST";
       find = "            if (mode == Mode.BOUNDARY && (i + 1) % 2 == 0) {"; repl = "            if (mode == Mode.BOUNDARY && (i + 1) % 2 == 1) {" },
    @{ n = "M20 折叠法:hash 忘记对表长取模"; f = "FoldingHashST";
       find = "        return (int) (foldedSum(key) % tableSize);"; repl = "        return (int) foldedSum(key);" },
    @{ n = "M21 折叠法:put 不再识别重复键"; f = "FoldingHashST";
       find = "            if (node.key == key) {`n                node.value = value;"; repl = "            if (false) {`n                node.value = value;" },
    @{ n = "M22 折叠法:反序时不按固定宽度补零"; f = "FoldingHashST";
       find = "        for (int i = 0; i < width; i++) {"; repl = "        while (rest > 0) {" },
    @{ n = "M23 随机数法:返回值不再屏蔽成 31 位非负"; f = "RandomHashST";
       find = "        return (int) (x & 0x7fffffffL);"; repl = "        return (int) x;" },
    @{ n = "M24 随机数法:种子不参与混合"; f = "RandomHashST";
       find = "        long x = key ^ seed;"; repl = "        long x = key;" },
    @{ n = "M25 随机数法:hash 忘记对表长取模"; f = "RandomHashST";
       find = "        return randomBits(key) % tableSize;"; repl = "        return randomBits(key);" },
    @{ n = "M26 随机数法:find 的线性探测步长改成 2"; f = "RandomHashST";
       find = "            if (state[i] == OCCUPIED && slotKeys[i] == key) {`n                lastProbes = probes;`n                return i;`n            }`n            i = (i + 1) % tableSize;";
       repl = "            if (state[i] == OCCUPIED && slotKeys[i] == key) {`n                lastProbes = probes;`n                return i;`n            }`n            i = (i + 2) % tableSize;" },
    @{ n = "M27 随机数法:delete 不留墓碑"; f = "RandomHashST";
       find = "        state[i] = TOMBSTONE;"; repl = "        state[i] = EMPTY;" },
    @{ n = "M28 全域散列:h 公式漏掉 +b"; f = "UniversalHashST";
       find = "        return (int) (((a * key + b) % primeP) % tableSize);"; repl = "        return (int) (((a * key) % primeP) % tableSize);" },
    @{ n = "M29 全域散列:a 可能取到 0(函数不在族内)"; f = "UniversalHashST";
       find = "        return 1 + Math.floorMod(random.nextLong(), primeP - 1);"; repl = "        return Math.floorMod(random.nextLong(), primeP - 1);" },
    @{ n = "M30 全域散列:不再校验 p 是否素数"; f = "UniversalHashST";
       find = "        if (!isPrime(primeP)) {"; repl = "        if (false) {" },
    @{ n = "M31 全域散列:hash 去掉上界检查"; f = "UniversalHashST";
       find = "        if (key < 0 || key >= primeP) {"; repl = "        if (key < 0) {" },
    @{ n = "M32 全域散列:碰撞统计不校验 k1==k2"; f = "UniversalHashST";
       find = "        if (k1 == k2) {"; repl = "        if (false) {" },
    @{ n = "M33 全域散列:闭合公式条件写错"; f = "UniversalHashST";
       find = "            if (d % tableSize == 0) {"; repl = "            if (d % tableSize == 1) {" }
)

$killed = 0
$survived = @()
$notApplied = @()

foreach ($m in $mutations) {
    $src = Join-Path $mainDir "$($m.f).java"
    # 先把所有实现恢复成原始版本,再注入当前变异:
    # 否则上一个变异体编译出的 class 会残留在 $classes 里,污染本轮失败归因。
    foreach ($f in $mainFiles) {
        Copy-Item (Join-Path $backup "$f.java") (Join-Path $mainDir "$f.java") -Force
        # 关键:刷新 mtime。Copy-Item 会保留备份文件的旧时间戳,若源码时间戳比 class 还旧,
        # 任何增量构建(IDE/maven)都会认为"已是最新"而跳过重编译,变异 class 就会永久留在
        # 构建输出目录里 —— 之后跑测试等于在测变异体,这是最危险的假信号。
        (Get-Item (Join-Path $mainDir "$f.java")).LastWriteTime = Get-Date
    }
    $text = (Get-Content -Raw -Encoding UTF8 $src) -replace "`r`n", "`n"
    if (-not $text.Contains($m.find)) { $notApplied += $m.n; "NOT APPLIED(源码已变动,请更新脚本): $($m.n)"; continue }
    $mutated = $text.Replace($m.find, $m.repl)
    [System.IO.File]::WriteAllText($src, $mutated, (New-Object System.Text.UTF8Encoding($false)))

    # 编译全部实现(不只当前文件),确保 $classes 中只有当前这一个变异
    $null = & (Join-Path $jdk8 "javac.exe") -encoding UTF-8 -Xlint:none -cp $cp -d $classes @mainSources 2>&1
    if ($LASTEXITCODE -ne 0) {
        "MUTANT COMPILE FAILED: $($m.n)"
        foreach ($f in $mainFiles) {
            Copy-Item (Join-Path $backup "$f.java") (Join-Path $mainDir "$f.java") -Force
        }
        continue
    }

    $r = Invoke-Suite
    $verdict = if ($r.code -ne 0) { "KILLED" } else { "SURVIVED" }
    if ($r.code -ne 0) { $killed++ } else { $survived += $m.n }
    "{0,-42} {1,-9} {2}" -f $m.n, $verdict, $r.out
    foreach ($f in $mainFiles) {
        Copy-Item (Join-Path $backup "$f.java") (Join-Path $mainDir "$f.java") -Force
        (Get-Item (Join-Path $mainDir "$f.java")).LastWriteTime = Get-Date
    }
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

# 清掉 maven 构建输出目录里本模块的散列 class:万一某个增量构建在变异窗口内把它们写进了
# target\classes(实测发生过),这里一次清干净,避免后续测试跑在变异 class 上。
$mavenHashDir = Join-Path $root "target\classes\cn\exercise\algs4\datastructure\hash"
if (Test-Path $mavenHashDir) {
    Remove-Item -Force (Join-Path $mavenHashDir "*.class") -ErrorAction SilentlyContinue
    "已清理 maven 输出目录中的散列 class:$mavenHashDir(请用 mvn clean test 做权威回归)"
}
""
"变异体总数 = $($mutations.Count),被杀死 = $killed,存活 = $($survived.Count),未应用 = $($notApplied.Count)"
if ($survived.Count -gt 0) { "存活变异体: " + ($survived -join "; ") }
if ($notApplied.Count -gt 0) { "未应用: " + ($notApplied -join "; ") }

if (-not $restoreOk) { throw "源码恢复校验失败!请用 git diff 检查" }
if ($survived.Count -gt 0 -or $notApplied.Count -gt 0 -or $killed -ne $mutations.Count) {
    exit 1
}
"全部变异体被杀死,测试对上述全部变异体都是有效的。"
exit 0
