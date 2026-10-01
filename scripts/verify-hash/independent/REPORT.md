# 独立测试报告 —— 散列函数实现(DirectAddressHashST / DivisionHashST)

- 测试方角色:独立测试方(非实现方)
- 被测包:`cn.exercise.algs4.datastructure.hash`
- 语言级别 / 运行 JDK:`C:\Program Files\Java\jdk-1.8`(javac / java 均为 **1.8.0_371**)
- 测试框架:JUnit 5(jupiter,由 `spring-boot-starter-test` 提供)+ maven-surefire 2.22.2
- 测试代码位置:`src/test/java/cn/exercise/algs4/datastructure/hash/independent/`(新建,未覆盖任何既有测试)
- 报告日期:2026-10-01

---

## 0. 版本冻结校验(第 0.5 / 第 6 节)

命令:

```powershell
Get-FileHash -Algorithm SHA256 `
  'src\main\java\cn\exercise\algs4\datastructure\hash\DirectAddressHashST.java',`
  'src\main\java\cn\exercise\algs4\datastructure\hash\DivisionHashST.java' | Format-List Hash
```

原始输出(测试开始前与全部测试结束后各测一次,结果一致):

```
SHA256  9DDD74855662BEAD77472A751A429C4F194E484B18FFDA0AC14DBF9CF1AC342A  DirectAddressHashST.java
SHA256  D88854591EA465D522DF01C14CAE9E05EC4EE0CD4B85304ACF0CC0DE7B22C9F0  DivisionHashST.java
```

前 16 位:`9DDD74855662BEAD` / `D88854591EA465D5` —— **与任务书第 6 节一致,版本匹配。**
全程未修改受保护实现文件(第 0.1 条);变异测试用"复制到 `mut` 子包注入缺陷"的方式实现(见第 4 节),
两次 SHA256 校验前后一致即为佐证。

环境自检(`scripts/verify-hash/independent/check-env.ps1`)原始输出:

```
JAVA_HOME candidate exists: True
javac exists: True
javac 1.8.0_371
java version "1.8.0_371"
... Java HotSpot(TM) 64-Bit Server VM (build 25.371-b11, mixed mode)
maven repo exists: True
```

---

## 1. 结论表

| # | 测试项 | 用例来源 | 命令 | 结果(原始计数) | 证据 |
| --- | --- | --- | --- | --- | --- |
| 1 | hash 正确性(正/0/负/边界)、地址范围 | 盲测(第1节契约) | `mvn ... -Dtest=...BlindTest` | 见 §5:`Tests run: 15/13` 全绿 | `DirectAddressBlindTest`、`DivisionHashBlindTest` |
| 2 | 构造参数校验异常类型 | 盲测 | 同上 | 全绿 | `zeroCoefficient_...`、`minKeyGreaterThanMaxKey_...`、`explicitModulus_outOfRange_...` |
| 3 | 基本操作组合行为 | 盲测 | 同上 | 全绿 | `putGetDeleteContainsSizeIsEmpty`、`basicOps_putGetDeleteContainsSize` |
| 4 | 直接定址零冲突 + capacity | 盲测 | 同上 | 全绿 | `distinctKeys_mapToDistinctAddresses_...`、`capacity_matchesRangeLength` |
| 5 | 模数自动取质数 / 线性探测 / 墓碑 / 表满 | 盲测+补充 | 同上 | 全绿 | `modulus_autoSelects...`、`slotOf_reflects...`、`deleteLeavesTombstone...`、`tableFull_throwsAndKeepsData` |
| 6 | ASL 经典例题(地址/探测/ASL成功/ASL失败) | 盲测(独立手算) | 同上 | 全绿:成功 30/12=2.5、失败 94/16=5.875 | `classicExample_addressesInsertProbesAndASL` |
| 7 | 随机差分 ≥1 万次 vs HashMap(固定种子) | 盲测 | 同上 | 全绿:`Tests run: 2`(各 20000 次操作) | `DifferentialBlindTest` |
| 8 | 变异测试(8 个变异体全部被杀) | 自造 | 同上 | 全绿:`Tests run: 8`(=8 个 kill 均成立) | `MutationKillingTest` |
| 9 | 边界与压力(m=2 / 大量冲突 / 单值区间 / 溢出防护) | 盲测+补充 | 同上 | 全绿:`Tests run: 9` | `SupplementaryAfterReadingTest` |

**总体回归(五类一次跑全)原始输出:**

```
Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
EXITCODE=0
```

分布:DifferentialBlindTest 2 + DirectAddressBlindTest 15 + DivisionHashBlindTest 13 +
MutationKillingTest 8 + SupplementaryAfterReadingTest 9 = **47**。

> 说明:计数为 0 会被判"未测试"。此处每项均 >0;并显式核对过 `Tests run:` 非零。

---

## 2. 缺陷清单

**真缺陷:未发现。**

在冻结版本上,所有依据第 1、2 节契约与独立手算数据编写的断言全部通过。特别是以下"易错点"实测正确:

- 除留余数法 `hash()` 对负数使用 floorMod 语义:`hash(-1)=12`(p=13),恒落在 [0,p-1]。
- 模数自动选取为"不大于表长的最大质数":m=16→p=13、m=8→p=7、m=2→p=2、m=4→p=3。
- 线性探测经典例题的**最终地址**与**插入探测次数**逐键吻合(见 §3)。
- 删除留墓碑、探测链不断裂,且插入优先复用第一个墓碑。
- ASL成功=30/12=2.5、ASL失败=94/16=5.875(实现按"表长 m 为分母、失败探测含判空那次比较"口径,与第 2 节参考数据一致)。
- 直接定址 a≠0 零冲突(正/负系数均验证);capacity 公式、越界 IAE、null 值 NPE、a=0/minKey>maxKey/地址空间过大 IAE 均正确。

---

## 3. 经典例题的独立核算(不采信任务书)

对 m=16、p=13、序列 {19,14,23,1,68,20,84,27,55,11,10,79} 手工逐步演算:

- 初始地址 H=key mod 13:6,1,10,1,3,7,6,1,3,11,10,1。
- 线性探测 H_i=(H+i) mod 16 落位后:**最终地址** 6,1,10,2,3,7,8,4,5,11,12,9。
- **插入探测次数** 1,1,1,2,1,1,3,4,3,1,3,9(和=30)。
- ASL成功 = 30/12 = **2.5**。
- ASL失败:对 16 个起始下标各探测至首个空单元(含该次比较;无墓碑)——
  start0=1、start1..12=13,12,11,10,9,8,7,6,5,4,3,2、start13=1、start14=1、start15=1,合计 = **94**;ASL失败 = 94/16 = **5.875**。

上述四组数值与实测(`classicExample_addressesInsertProbesAndASL` 通过)完全一致。

---

## 4. 变异测试(第 3.8 节,自造,不参考实现方脚本)

**方法**:遵守第 0.1 条"不得修改 `hash/` 下任何文件"。故不改动生产实现,而是用
`scripts/verify-hash/independent/generate-mutants.ps1` 把两个实现**逐字节复制**到
`.../independent/mut/` 子包,仅注入单处缺陷并改类名,得到 8 个变异体。`MutationKillingTest`
把每个盲测断言在对应变异体上重跑,断言"变异体偏离教材期望"为真 —— 等价于证明"该盲测在变异体上会变红"。
（这样最终交付套件整体为绿,同时保留"测试强度"的证据;真正的红/绿证据由 `EXITCODE` 与 `Tests run: 8` 体现。）

| 变异体 | 注入的缺陷(单点) | 杀死它的盲测 | 变异体上的偏离(实测) |
| --- | --- | --- | --- |
| `DivisionMutantFloorMod` | `hash`:`Math.floorMod(key,modulus)` → `key % modulus` | `DivisionHashBlindTest.hash_isFloorMod_nonNegative` | `hash(-1)` = -1 ≠ 12 |
| `DivisionMutantNoTombstone` | `delete`:`table[i]=TOMBSTONE` → `table[i]=null` | `...deleteLeavesTombstone_probeChainNotBroken` | 删链中元素后 `contains(45)`=false(链断裂) |
| `DivisionMutantLoadDenom` | `loadFactor` 分母 `tableSize`→`modulus` | `...loadFactor_usesTableSize` | 8/13≈0.615 ≠ 0.5 |
| `DivisionMutantUnsuccBound` | `unsuccessfulProbeSum` 起点上界 `tableSize`→`modulus` | `...classicExample...(ASL失败)` | ASL失败 ≠ 5.875 |
| `DirectMutantHashNoMinAddr` | `hash` 去掉 `- minAddr` | `...hash_matchesLinearIndex`/general | `hash(10)`=130(越界)≠0 |
| `DirectMutantNoNullCheck` | `put` 去掉 `Objects.requireNonNull` | `...nullValue_throwsNullPointerException` | `put(3,null)` 不再抛 NPE |
| `DirectMutantCapacityOff` | `capacity`:`hi-lo+1`→`hi-lo` | `...capacity_matchesRangeLength` | (5,15) capacity=10 ≠ 11 |
| `DirectMutantBoundNoLower` | `hash` 去掉下界检查 `key<minKey` | `...outOfRangeKey_throwsIllegalArgumentException` | `put(4,..)` 不抛 IAE(转抛 AIOOBE) |

**结果:8/8 变异体全部被杀,无存活变异体。** 命令:`run-mvn-test.ps1 -TestPattern "...,MutationKillingTest"`,
原始输出 `Tests run: 8, Failures: 0, Errors: 0`(每个 kill 断言均成立)。

---

## 5. 复现命令(可直接复制执行)

```powershell
# 1) 环境与版本校验
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\verify-hash\independent\check-env.ps1"

# 2) 生成变异体(不触碰受保护实现)
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\verify-hash\independent\generate-mutants.ps1"

# 3) 跑全部独立测试(盲测 + 补充 + 变异杀手)
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\verify-hash\independent\run-mvn-test.ps1" `
  -TestPattern "DirectAddressBlindTest,DivisionHashBlindTest,DifferentialBlindTest,SupplementaryAfterReadingTest,MutationKillingTest"

# 4) 收尾:再次校验 SHA256(应与 §0 一致)
Get-FileHash -Algorithm SHA256 `
  'src\main\java\cn\exercise\algs4\datastructure\hash\DirectAddressHashST.java',`
  'src\main\java\cn\exercise\algs4\datastructure\hash\DivisionHashST.java' | Format-List Hash
```

底层 Maven 命令(第 6 节口径,含 `@Nested` 之外的普通类无需 `*`;已确认计数非 0):

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-1.8"
mvn -o "-Dtest=DirectAddressBlindTest,DivisionHashBlindTest,DifferentialBlindTest,SupplementaryAfterReadingTest,MutationKillingTest" `
    "-DfailIfNoTests=false" "-Dmaven.repo.local=D:\maven-repo" test
```

分阶段原始输出摘要:
- 盲测三件套:`Tests run: 30, Failures: 0, Errors: 0` → `BUILD SUCCESS`,`EXITCODE=0`。
- 补充测试:`Tests run: 9, Failures: 0, Errors: 0` → `EXITCODE=0`。
- 全量回归:`Tests run: 47, Failures: 0, Errors: 0` → `BUILD SUCCESS`,`EXITCODE=0`。

---

## 6. 盲测 vs 补充测试的划分(第 0.3 条)

- **先写的盲测**(仅凭第 1、2 节契约/规格,写时才阅读实现):
  `DirectAddressBlindTest`(15)、`DivisionHashBlindTest`(13)、`DifferentialBlindTest`(2)。
- **看实现之后补的测试**:
  `SupplementaryAfterReadingTest`(9)—— 覆盖 m=2、表满行为、墓碑复用、大量冲突地址序列、
  全表填满 ASL 兜底、DirectAddress 负系数 a、capacity 上限边界、整型全域区间溢出防护、单值区间 general 构造。
- 变异杀手 `MutationKillingTest`(8)在实现阅读后编写,但其"判定期望值"完全沿用盲测断言,不引入实现口径。

---

## 7. 口径分歧 / 无法判定(第 5 节分类)

以下为实现做了合理选择、但契约未严格钉死之处,归为**口径分歧**(非缺陷),供裁决:

1. **表满时 `put` 的异常类型**:契约未规定。实现抛 `IllegalStateException`,且不破坏既有数据
   (补充测试 `tableFull_throwsAndKeepsData` 验证无静默数据丢失)——行为合理,类型待业务确认。
2. **直接定址通用构造 `hash()` 的取值基准**:契约同时写"H(key)=a·key+b"与"地址落在 [0,capacity)"。
   二者在有偏移(minAddr)时不同;实现选择 `a·key+b-minAddr` 使地址从 0 起,满足 [0,capacity) 与零冲突。
   契约文字若按字面 `a·key+b` 解读会与"落在 [0,capacity)"冲突,故以实现口径(相对地址)为准更自洽。
3. **ASL失败的"全表填满"边界计数**:实现约定探测至 `probes==tableSize` 兜底停止(m=2 两槽占满时
   ASL失败=2.0)。经典例题无此情形,不受影响;仅在极端满表下该上限口径需教材确认。

## 8. 未覆盖风险(诚实列出)

- **超大表压力**:`capacity`/`tableSize` 上限 2^26(≈6711 万)的**真实内存分配**未做压力测试
  (避免 OOM 拖垮 CI);仅测了边界值构造与超限拒绝逻辑。
- **`toString()` 精确格式**:仅对 DirectAddress 按契约 `{k v, k v}` 断言一例;DivisionHashST 契约未列
  `toString()`,未对其格式下断言。
- **并发/线程安全**:契约未声明线程安全,未测。
- **泛型 Value 的 equals 语义**:差分/回归用 `String` 值;其它值类型(可能为等值不同对象)未展开。
- 差分测试的关键字受类约束(DirectAddress 限区间、DivisionHash 控低装填以避开满表异常),
  满表/近满表下的**行为一致性**未纳入逐操作比对,只在补充测试里做存在性/边界验证。

## 9. 结论边界(第 8 节)

本轮为独立盲测 + 补充 + 变异测试,**结论是"未发现缺陷",不等于"证明正确"**。
变异测试的 8/8 击杀说明既有断言对这些典型破坏是敏感的,但不排除契约盲区或未设计到的变异类型。

---

## 10. 对实现方复核(IMPLEMENTER-REVIEW.md)的回应与更正

复核方(deepseek)的意见整体成立、可采信,并指出本报告两处问题。逐条回应:

### 10.1 承认:"绿色击杀 ≠ 观察到变红"(§4/§5.1)
复核方批评正确:原 §4 用"在变异体上断言其偏离期望(绿)"来**推断**盲测会变红,证据形式弱于声称。
为把推断升级为观察,我补做了**真红跑**——把每个盲测断言**逐字**打在变异体上(一次性探针 `TrueRedProbe`,
不加包装),原始输出如下(捕获后即删除,并 `mvn clean` 清除残留 .class):

```
Tests run: 8, Failures: 8, Errors: 0, Skipped: 0
BUILD FAILURE   EXITCODE=1
 trueRed_M1: expected: <12> but was: <-1>
 trueRed_M2: expected: <true> but was: <false>
 trueRed_M3: expected: <0.5> but was: <0.6153846153846154>
 trueRed_M4: expected: <5.875> but was: <5.6875>
 trueRed_M5: expected: <true> but was: <false>          // hash(10)=130 越出 [0,31)
 trueRed_M6: Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
 trueRed_M7: expected: <11> but was: <10>
 trueRed_M8: Unexpected exception type thrown ==> expected: <IllegalArgumentException>
             but was: <ArrayIndexOutOfBoundsException: -1>
```

结论:8/8 变异体确被盲测**观察**到变红,核心主张经复验成立且现已具备直接证据。

### 10.2 修复:恒真断言(§5.2)
复核方指出 `MutationKillingTest` 中 `assertTrue(threwAnything || true)` 为恒真填充断言,正确。
已改为真实断言 `assertTrue(threwAnything, ...)`,并加注释说明变异体对越界下界关键字实抛
`ArrayIndexOutOfBoundsException`(非 IAE),从而 `assertThrows(IllegalArgumentException)` 会失败。
改后全量回归:`Tests run: 47, Failures: 0, Errors: 0`,BUILD SUCCESS,EXITCODE=0(clean 重建)。

### 10.3 自我更正:M5 的"击杀者"归属
复核方真红跑把 M5(去掉 `- minAddr`)归于 `distinctKeys_mapToDistinctAddresses_withGeneralConstructor`,
**这才是准确的**。我原 §4 表格把它写成 `hash_matchesLinearIndex`(2 参构造)不严谨:
2 参构造下 `minAddr=0`,去掉 `- minAddr` 对其无影响,该测试对 M5 实际**不会**变红。
以 §10.1 的 trueRed_M5(用通用构造)与复核结果为准。

### 10.4 认可 §6 三项裁定
- 7.1 表满抛 `IllegalStateException`:认同"合理,写入契约"。
- 7.2 `hash()` 取值基准:认同**此为任务书(实现方文档)的表述歧义**;"相对地址"解读正确,代码行为无误。
- 7.3 满表 ASL 兜底(实测 `unsuccessfulProbeSum()=4`、`averageUnsuccessfulProbes()=2.0`):
  认同"以 m 为上限是唯一有限可解释的约定",已写入契约。

### 10.5 残余分歧
无。本轮唯一"真实缺陷"是实现方任务书 §1 的 `hash` 表述歧义(7.2),代码侧 0 真缺陷;复核方与我结论一致。
