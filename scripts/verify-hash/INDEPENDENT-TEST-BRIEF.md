# 独立测试任务书:散列函数实现(交给另一个 AI 的完整指令)

> 使用方式:把本文件**整份**粘贴给另一个 AI(最好是不同厂商的模型),并让它能访问本仓库。
> 实施者(实现方)不参与你的测试结论的形成,只在你交付报告后负责复现与修缺陷。

---

## 0. 你的角色与硬性约束

1. 你是**独立测试方**。不得修改 `src/main/java/cn/exercise/algs4/datastructure/hash/` 下的任何文件
   ——改了实现就会掩盖缺陷、也让"测试通过"失去意义。
2. 你的测试写在 `src/test/java/cn/exercise/algs4/datastructure/hash/independent/` 下(新目录),不要覆盖已有测试文件。
3. **盲测纪律**:先只按第 1、2 节的接口契约与规格写测试;**写完之后**才允许阅读实现代码与已有测试。
   读完实现后如要补充测试,必须在报告里标明"哪些测试是先写的、哪些是看了实现后补的"。
4. 所有结论必须附**原始命令与原始输出**(含测试计数与退出码)。不接受"我测试通过了""结果正确"这类概述性结论。
   若输出显示 0 个测试被执行,一律视为"未测试",不得算通过。
5. 被测版本已冻结(见第 6 节)。开始前先记录你实际测的文件 SHA256;若哈希与第 6 节不符,停止并报告"版本不一致"。

---

## 1. 接口契约(冻结,不以后来的实现为准)

包 `cn.exercise.algs4.datastructure.hash`,语言级别 **Java 8**(禁止使用 Java 9+ API)。

### DirectAddressHashST&lt;Value&gt;(直接定址法)

| 成员 | 语义 |
| --- | --- |
| `DirectAddressHashST(int minKey, int maxKey)` | H(key) = key - minKey |
| `DirectAddressHashST(int minKey, int maxKey, long a, long b)` | H(key) = a\*key + b,a ≠ 0 |
| `int hash(int key)` | **相对地址** = a·key + b − H(minKey),落在 [0, capacity)。注意 a·key+b 是散列函数本身,数组下标是它减去偏移后的结果;put/get 等内部一律使用相对地址 |
| `void put(int key, Value value)` | 插入/覆盖;值不得为 null |
| `Value get(int key)` / `Value delete(int key)` | 不存在返回 null;delete 返回被删值 |
| `boolean contains(int key)` | |
| `int size()` / `boolean isEmpty()` | |
| `int capacity()` | = H(maxKey) - H(minKey) + 1 |
| `double loadFactor()` | = size / capacity |
| `long coefficientA()` / `long constantB()` | 返回 a、b |
| `Iterable<Integer> keys()` | 按地址升序 |
| `String toString()` | `{k v, k v}` |

约束:关键字必须落在 [minKey, maxKey],越界抛 `IllegalArgumentException`;
值为 null 抛 `NullPointerException`;`a == 0`、`minKey > maxKey`、地址空间过大(上限 2^26)均抛 `IllegalArgumentException`。
**性质**:a ≠ 0 时不同关键字的地址必然互不相同 —— 零冲突,无需任何冲突处理。**非线程安全**。

### DivisionHashST&lt;Value&gt;(除留余数法)

| 成员 | 语义 |
| --- | --- |
| `DivisionHashST(int tableSize)` | 表长 m = tableSize,模数 p = **不大于 m 的最大质数** |
| `DivisionHashST(int tableSize, int modulus)` | 1 ≤ p ≤ m |
| `int hash(int key)` | **key mod p**,负数也必须得到非负地址(floorMod 语义) |
| `void put(int key, Value value)` | 冲突时**线性探测再散列** H_i = (H(key)+i) mod m |
| `Value get(int key)` / `Value delete(int key)` / `boolean contains(int key)` | 同上有 null 语义 |
| `int size()` / `boolean isEmpty()` | |
| `int tableSize()` / `int modulus()` | m、p |
| `int slotOf(int key)` | 关键字实际所在下标;不存在返回 -1 |
| `int tombstones()` | 墓碑个数 |
| `double loadFactor()` | = size / tableSize |
| `int lastProbes()` | 最近一次查找/插入/删除的探测次数 |
| `long successfulProbeSum()` / `long unsuccessfulProbeSum()` | ASL 分子的原始和 |
| `double averageSuccessfulProbes()` | ASL成功 = 成功探测次数总和 / n(n = 元素个数) |
| `double averageUnsuccessfulProbes()` | ASL失败 = 失败探测次数总和 / m(以**表长**为分母) |
| `Iterable<Integer> keys()` | 按数组下标升序 |
| `String toString()` | `{k v, k v}`(按数组下标升序) |
| `static int largestPrimeNotGreaterThan(int n)` | 不大于 n 的最大质数;n < 2 抛 IllegalArgumentException |

**ASL 口径(判定缺陷时必须遵守,否则算误报)**:
- 失败查找的探测次数**包含**最后判定为"空单元"的那一次比较;墓碑视为**非空**,继续向后探测。
- **表长 m 与模数 p 是两个不同的量**(例:m = 16、p = 13);`averageUnsuccessfulProbes()` 的分母是**表长 m**。

**边界约定(2026-10-01 依据独立测试复核意见补入,原先只有口头上的一致)**:
- 表满(无空单元且无可复用墓碑)时 `put` 抛 `IllegalStateException`:入参合法、失败原因是容器状态,
  且抛异常前不修改任何既有数据。
- 满表(全表无空单元)时失败查找以 `tableSize` 次探测为上限兜底,不能再向后找空单元;
  例:m = 2 且两槽占满时 `averageUnsuccessfulProbes()` = 2.0。
- 表长上限 2^26(67108864),超过抛 `IllegalArgumentException`(避免分配超大数组导致 OOM)。
- **两个类都不是线程安全的**:没有同步、没有 volatile;并发读写需调用方自行加锁。

### DigitAnalysisHashST&lt;Value&gt;(数字分析法)

适用前提:关键字集合**已知且静态**,关键字是同基数的多位数字编码(学号、工号、电话等)。
散列函数不是公式,而是**从样本中挑位**:统计每一位上各数字的出现次数 → 用归一化熵给每位打分 →
选出最均匀的若干位、按原有位序拼成地址。

| 成员 | 语义 |
| --- | --- |
| `DigitAnalysisHashST(long[] sample, int digitCount, int maxCapacity)` | 十进制基数 |
| `DigitAnalysisHashST(long[] sample, int radix, int digitCount, int maxCapacity)` | 自定义基数 |
| `int hash(long key)` | 按选中位从高位到低位拼地址,落在 [0, addressSpace) |
| `void put(long, Value)` / `Value get(long)` / `Value delete(long)` / `boolean contains(long)` | **拉链法**;不存在返回 null |
| `int size()` / `boolean isEmpty()` | |
| `int radix()` / `int digitCount()` | 基数与参与分析的位数 |
| `int[] chosenPositions()` | 选中的位号(升序,0 = 个位) |
| `int addressSpace()` | = radix^选位数 |
| `int[] distribution(int position)` | 该位上各数字出现次数(长度 = radix) |
| `double uniformity(int position)` | 归一化熵,恒定位 = 0、等频 = 1 |
| `int sampleSize()` / `int sampleCollisionCount()` / `int distinctAddressCount()` | 样本统计 |
| `double loadFactor()` | = size / addressSpace |
| `int maxChainLength()` | 最长链长度(最坏查找长度) |
| `Iterable<Long> keys()` / `String toString()` | 按地址升序 |

**选位规则(判定缺陷时必须遵守)**:按归一化熵降序贪心,至少选 1 位;若再选一位会使
radix^选位数 超过 `maxCapacity` 就停止;熵相同时**位号小者优先**(结果可复现)。
位序约定:第 0 位是最低位(个位),位号越大越靠左。

**约束**:`sample` 不能为 null/空;`radix ≥ 2`;`digitCount ≥ 1`;`maxCapacity ≥ radix`;
关键字必须满足 `0 ≤ key < radix^digitCount`,否则抛 `IllegalArgumentException`;
值为 null 抛 `NullPointerException`;地址空间上限 2^26。**非线程安全**。
与除留余数法的差别:冲突用**拉链法**(无墓碑、无表满拒插)。

### MidSquareHashST&lt;Value&gt;(平方取中法)

散列函数:`H(key) = key² 的中间 k 位十进制数字`。平方让关键字的每一位都影响结果的高低位,
因此中间几位由关键字所有位共同决定,比"取末位"更均匀。

| 成员 | 语义 |
| --- | --- |
| `MidSquareHashST(int addressDigits)` | 地址位数 k ∈ [1, 7];表长 m = 10^k |
| `long square(long key)` | 返回 key²;溢出 long 抛 IllegalArgumentException |
| `int hash(long key)` | 取平方数中间 k 位,地址落在 [0, 10^k) |
| `void put(long, Value)` / `Value get(long)` / `Value delete(long)` / `boolean contains(long)` | 线性探测 + 墓碑 |
| `int slotOf(long)` / `int tombstones()` / `int lastProbes()` | 探测与墓碑观察 |
| `int tableSize()` / `int addressDigits()` / `double loadFactor()` | |
| `long successfulProbeSum()` / `long unsuccessfulProbeSum()` / `double averageSuccessfulProbes()` / `double averageUnsuccessfulProbes()` | ASL 口径与 DivisionHashST 完全一致 |
| `Iterable<Long> keys()` / `String toString()` | 按数组下标升序 |

**取位约定(判定缺陷时必须遵守)**:设平方数共 L 位十进制数字:
- 若 `L ≤ k`,不足 k 位,地址 = **平方值本身**(等价于左侧补零);
- 否则 `offset = (L - k) / 2`(向下取整,窗口略偏右),地址 = `(square / 10^offset) mod 10^k`。

例:k=2 时 1234 → 1234² = 1522756 → offset 2 → 15227 → **27**;5678 → 32239684 → offset 3 → 32239 → **39**;
99 → 9801 → offset 1 → 980 → **80**;3 → 9(L ≤ k)→ **9**。

**约束**:`k ∈ [1, 7]`;key² 必须在 long 范围内,否则抛 `IllegalArgumentException`;
**负数与其相反数地址相同**(平方的性质),但作为两个不同的键分别保存(靠线性探测放在不同槽);
值为 null 抛 `NullPointerException`;表满(无空槽且无墓碑)时 `put` 抛 `IllegalStateException`。
冲突策略与 DivisionHashST 相同(线性探测 + 墓碑 + 复用墓碑)。**非线程安全**。

### FoldingHashST&lt;Value&gt;(折叠法)

散列函数:把关键字按段长切成若干段,叠加后取模。

| 成员 | 语义 |
| --- | --- |
| `FoldingHashST(int segmentDigits, int tableSize)` | 默认**移位叠加** |
| `FoldingHashST(int segmentDigits, int tableSize, Mode mode)` | `Mode.SHIFT` 移位叠加 / `Mode.BOUNDARY` 分界叠加 |
| `int[] segments(long key)` | 各段数值,低位→高位(段号从 1 开始,最高段可短) |
| `long foldedSum(long key)` | 各段叠加之和(未取模) |
| `int hash(long key)` | `foldedSum mod tableSize` |
| `void put/get/delete/contains` | **拉链法**(无表满拒插) |
| `int size()` / `boolean isEmpty()` / `int segmentDigits()` / `int tableSize()` / `Mode mode()` | |
| `double loadFactor()` / `int maxChainLength()` | |
| `Iterable<Long> keys()` / `String toString()` | 按地址升序 |

**分段与叠加约定(判定缺陷时必须遵守)**:
- 关键字按十进制数字处理,**从低位向高位**每 `segmentDigits` 位切一段,段号从 1 开始,最高段可以不足段长;
- `SHIFT`:各段全部正序相加;
- `BOUNDARY`:**奇数段正序、偶数段反序**;反序按**固定宽度**处理(缺位补零),
  故段 `"001"` 反序为 `"100"`、段 `"040"` 反序仍为 `"040"`;
- `H(key) = foldedSum mod tableSize`;表长取 10^k 时即"取叠加和的后 k 位、舍去进位"。

示例(段长 3):`123456789` → 各段 789 | 456 | 123 → 移位 1368、分界 1566;表长 1000 时地址 **368** / **566**。
`87654321` → 321 | 654 | 87 → 移位 1062(→62)、分界 321+456+87 = 864。`1000` → 0 | 1 → 移位 1、分界 100。

**约束**:关键字必须非负;`segmentDigits >= 1`;`tableSize ∈ [1, 2^26]`;`mode` 不能为 null;
值为 null 抛 `NullPointerException`。**非线程安全**。

### RandomHashST&lt;Value&gt;(随机数法)

散列函数:`H(key) = random(key) mod m`,其中 `random` 是**确定性**伪随机函数
(64 位 xor-shift-multiply 三轮混合,seed 混入关键字,取 31 位非负值)。

| 成员 | 语义 |
| --- | --- |
| `RandomHashST(int tableSize)` | 默认固定种子 |
| `RandomHashST(int tableSize, long seed)` | 指定种子 |
| `int randomBits(long key)` | 伪随机值,[0, 2^31-1] |
| `int hash(long key)` | `randomBits % tableSize`,落在 [0, tableSize) |
| `void put/get/delete/contains` | 线性探测 + 墓碑 |
| `int slotOf(long)` / `int tombstones()` / `int lastProbes()` | 探测与墓碑观察 |
| `int tableSize()` / `long seed()` / `double loadFactor()` | |
| `long successfulProbeSum()` / `long unsuccessfulProbeSum()` / `double averageSuccessfulProbes()` / `double averageUnsuccessfulProbes()` | ASL 口径与 DivisionHashST 一致 |
| `Iterable<Long> keys()` / `String toString()` | 按数组下标升序 |

**必须遵守的性质**:同一关键字 + 同一种子必须得到同一地址(**确定性**);`randomBits` 恒非负、地址恒在
[0, tableSize);换种子后地址一般不同。关键字可以是**任意 long(含负数与 Long.MIN_VALUE)**,
不要求十进制编码 —— 这正是随机数法相对数字分析法/平方取中法/折叠法的优势场景。

**重点反例(测试必须覆盖)**:若用 `Math.random()` 或未固定种子的 `new Random()` 当 random,
同一关键字两次地址不同,散列表立刻失效 —— 这是随机数法最典型的错误实现。

**约束**:`tableSize ∈ [1, 2^26]`;值为 null 抛 `NullPointerException`;表满(无空槽且无墓碑)时
`put` 抛 `IllegalStateException`。**非线程安全**。

### UniversalHashST&lt;Value&gt;(全域散列法)

不是固定一个函数,而是**建表时从一族函数里随机选一个**:
`H = { h(a,b) | h(a,b)(key) = ((a*key + b) mod p) mod m },a ∈ [1,p-1],b ∈ [0,p-1],p 为素数`。

| 成员 | 语义 |
| --- | --- |
| `UniversalHashST(int m, long p)` | 默认固定种子选 (a,b);结果可复现 |
| `UniversalHashST(int m, long p, long seed)` | 由 seed 决定 (a,b),同 seed 必得同一函数 |
| `UniversalHashST(int m, long p, long a, long b)` | 直接指定函数(验证/复现用) |
| `int hash(long key)` | `((a*key+b) mod p) mod m`,key 必须 ∈ [0, p-1] |
| `long primeP()` / `int tableSize()` / `long a()` / `long b()` / `long seed()` | |
| `void put/get/delete/contains` | **拉链法** |
| `int size()` / `boolean isEmpty()` / `double loadFactor()` / `int maxChainLength()` | |
| `Iterable<Long> keys()` / `String toString()` | 按地址升序 |
| `static long familySize(long p)` | 族大小 (p−1)·p |
| `static long collisionCountInFamily(long k1, long k2, int m, long p)` | **遍历全族**统计碰撞函数个数 |
| `static double collisionProbabilityInFamily(...)` | 碰撞概率 |
| `static long predictedCollisionCount(int m, long p)` | 用**闭合公式**独立计算同一计数 |

**必须遵守的性质**:
- **全域性**:`Pr[h(k1) = h(k2)] ≤ 1/m`(k1 ≠ k2,a、b 均匀随机)。可用两条独立路径互相印证:
  遍历全族 `collisionCountInFamily` 与闭合公式 `predictedCollisionCount`
  `= Σ_{d=1}^{p-1} [ (p-d)·[m|d] + d·[m|(d-p)] ]`。该计数**与 k1、k2 的具体取值无关**,只要求两者不同。
  例:p=97、m=16 时两者都等于 **492**(概率 0.052835 ≤ 1/16);p=17、m=4 时等于 **56**
  (概率 = (p+1-m)/(p·m) = 0.2059 < 1/4,**仅 m=1 时取等**)。
- 随机性只在**建表时用一次**:函数选定后必须保持固定,否则查不回来。
- **全域性只保证概率**:某个随机选中的函数仍可能把对手的集合全部聚到一起
  (例:p=1009、m=64,a=1、b=47 时,集合 {64,128,…,960} 全部落进桶 47,最长链 15)。

**约束**:p 必须是素数且 2 ≤ p ≤ 2^31-1(否则抛 `IllegalArgumentException`);
关键字必须满足 0 ≤ key ≤ p-1;a ∈ [1,p-1]、b ∈ [0,p-1];值为 null 抛 `NullPointerException`。
默认固定种子**不提供真实对手防护**,工程上应传入不可预测的种子。**非线程安全**。

---

## 2. 规格(教材定义,与实现无关)

1. **直接定址法** H(key) = a·key + b:取关键字的线性函数作地址,数组下标即地址。
   适合关键字连续或接近连续的集合;关键字稀疏时地址空间浪费极大。
2. **除留余数法** H(key) = key mod p:p 通常取不大于表长的最大质数;若 p 取 2 的幂,
   关键字的奇偶性会直接决定地址奇偶性,分布变差。
3. **冲突处理**:线性探测再散列;删除必须留墓碑,否则会截断后续关键字的探测链;
   插入优先复用探测路径上遇到的第一个墓碑。
4. 装填因子 α = n / m;α 越大,线性探测的聚集越严重,ASL 越大。

**可独立核算的参考数据(经典例题)**:m = 16、p = 13,
关键字序列 {19, 14, 23, 1, 68, 20, 84, 27, 55, 11, 10, 79},依次插入:

- 最终地址依次为:6, 1, 10, 2, 3, 7, 8, 4, 5, 11, 12, 9
- 插入时探测次数依次为:1, 1, 1, 2, 1, 1, 3, 4, 3, 1, 3, 9
- ASL成功 = 30 / 12 = 2.5;ASL失败 = 94 / 16 = 5.875

请**独立手算或另写程序核算**这组数据,不要直接采信本文件 —— 本文件由实现方编写,可能与实现共享同一误解。

---

## 3. 最低测试要求

1. `hash` 正确性:正数、0、负数、边界 minKey/maxKey;除留余数法地址必须落在 [0, p-1]。
2. 构造参数校验:各非法输入是否抛**正确类型**的异常。
3. 基本操作:put / get / delete / contains / size / isEmpty / loadFactor 的组合行为,
   包括重复插入覆盖、删除不存在的键、空表查询。
4. 直接定址法:不同关键字地址互不相同(零冲突);capacity 与 H 的映射区间一致。
5. 除留余数法:模数自动取质数(含 m 本身是质数、m 是 2 的幂、m = 4、m = 2 等边界);
   线性探测的地址序列;墓碑删除后探测链不断裂;表满时的行为。
6. ASL:复算第 2 节经典例题的地址、探测次数、ASL成功、ASL失败。
7. **随机差分测试**:≥ 1 万次随机操作(put/get/contains/delete/size),同时施加于被测类与
   `java.util.HashMap`,逐操作比对;随机数用固定种子,保证可复现。
8. **变异测试**:自造 ≥ 5 个变异体(**你自己设计**,不要参考实现方的脚本),
   逐个改坏实现,验证你的测试能把它变红;记录每个变异体是被哪个测试杀死的。
9. 边界与压力:极小的表(m = 2)、大量冲突、关键字区间恰好只有一个值。
10. 数字分析法:位分布统计与归一化熵(恒定/等频/偏斜三种)、选位与 `maxCapacity` 约束、按选位拼地址、
    选位造成的同桶冲突必须由拉链法正确处理(`hash(370) == hash(371)` 但两者都能取回)。
11. 平方取中法:取位规则(含平方位数不足 k 位)、平方溢出 long 的边界(3037000499² 可用、
    3037000500² 抛异常)、负数与相反数同址、末两位相同的关键字被分散、
    ASL 统计与独立编写的线性探测模拟一致。
12. 折叠法:分段(低位起、最高段可短)、移位叠加与分界叠加的差值、**偶数段按固定宽度补零反序**
    (段 "001"→"100")、`foldedSum mod tableSize` 的地址、末几位相同的关键字被折叠打散。
13. 随机数法:**确定性**(同 key 同 seed 恒同址,不同实例也一致)、31 位非负与地址范围、
    雪崩效应(相邻关键字平均约 16/32 位不同)、分布均匀性、种子参与、负数与 `Long.MIN_VALUE` 可用、
    `Math.random()` 式实现的反例说明。
14. 全域散列法:族成员公式、同 seed 的确定性选取(a ∈ [1,p-1]、b ∈ [0,p-1])、
    **全域性 Pr ≤ 1/m 的穷举验证**(并与闭合公式互相印证)、计数与关键字取值无关、
    m=1 时全碰撞、"全域性只保证概率"的反例、p 非素数与关键字越界的异常类型。

---

## 4. 证据格式(每条结论都必须满足)

- 完整命令(可直接复制执行)
- **原始输出片段**:必须包含测试计数(如 `Tests run: N` 或 `FOUND=N`)与退出码
- 若判为缺陷:给出**最小可复现片段** —— 能独立编译运行的最小代码 + 期望值 + 实际值
- 不允许出现"已验证""符合预期"而无上述证据的结论

---

## 5. 分歧处理规则

1. 判定基准是**教材定义与本任务书第 1 节的契约**,以及第 2 节可独立核算的数据;不以实现行为为准。
2. 你与实现不一致时,先给出最小反例。给不出反例的差异,按"口径分歧"上报,不判缺陷。
3. 报告必须把每个发现归入四类之一:**真缺陷 / 口径分歧 / 覆盖缺口 / 无法判定**。
4. 实现方保留反驳权:实现方需按同样的证据格式复现你的反例;复现不成立则归为"无法判定"并记录双方证据。

---

## 6. 环境与被测版本

**运行的 Java 版本**:`C:\Program Files\Java\jdk-1.8`(项目 `pom.xml` 的 `<java.version>1.8</java.version>`)。
注意:用 JDK 17 编译带 `-source/-target 1.8` **不会**拦截 Java 9+ API,必须用真实 JDK 8 或 `javac --release 8`。

**跑测试(离线)**:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-1.8"
mvn -o "-Dtest=你的测试类名*" "-DfailIfNoTests=false" "-Dmaven.repo.local=D:\maven-repo" test
```

**已知坑**:surefire 2.22.2 下 `-Dtest=类名` 对**只含 `@Nested` 内部类**的测试类会输出
`Tests run: 0 ... BUILD SUCCESS` —— 测试根本没跑却"成功"。必须写成 `-Dtest=类名*`。
凡是计数为 0 的输出,一律按"未测试"处理。

**冻结的被测版本**(文件 SHA256 前 16 位):

| 文件 | SHA256(前 16 位) |
| --- | --- |
| `src/main/java/cn/exercise/algs4/datastructure/hash/DirectAddressHashST.java` | `9DDD74855662BEAD` |
| `src/main/java/cn/exercise/algs4/datastructure/hash/DivisionHashST.java` | `D88854591EA465D5` |
| `src/main/java/cn/exercise/algs4/datastructure/hash/DigitAnalysisHashST.java` | `8FFB4A4853598431` |
| `src/main/java/cn/exercise/algs4/datastructure/hash/MidSquareHashST.java` | `3178F8030330FE17` |
| `src/main/java/cn/exercise/algs4/datastructure/hash/FoldingHashST.java` | `490BF5DF85C65A65` |
| `src/main/java/cn/exercise/algs4/datastructure/hash/RandomHashST.java` | `53EF8B4719783256` |
| `src/main/java/cn/exercise/algs4/datastructure/hash/UniversalHashST.java` | `6B00DC32E38C5AF5` |

---

## 7. 交付物

一份报告,包含:

1. **结论表**:测试项 / 方法 / 命令 / 结果 / 证据引用
2. **缺陷清单**:每条含最小反例与期望/实际
3. **未覆盖风险**:你没能测到的部分(诚实列出,包括时间与环境限制)
4. **无法判定的问题**:需要实现方或人工裁决的口径分歧
5. 你实际执行的命令与完整输出附件

---

## 8. 这套安排仍然不能保证什么(写给使用者,不是写给测试方)

- 两个 AI 若同源同模型,可能共享同一类盲区;换厂商/换模型比换会话更有效。
- 规格本身模糊时(如 ASL 计数口径),独立测试只会产生"口径分歧",不会自动产生正确答案 —— 需要教材/老师/业务方裁决。
- 测试方同样可能假通过(没真跑、只做代码审查、只测 happy path),所以第 0 节强制了原始输出与计数。
- 通过独立测试只能得到"**未发现缺陷**",不是"**证明正确**"。
