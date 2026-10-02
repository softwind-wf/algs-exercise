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
