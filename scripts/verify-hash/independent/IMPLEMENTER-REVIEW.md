# 实现方复核意见 —— 针对 qwen-3.8 独立测试报告

- 复核方角色:实现方(按任务书第 5 节"实现方保留反驳权"执行)
- 被复核对象:`scripts/verify-hash/independent/REPORT.md`
- 复核日期:2026-10-01
- 复核方式:不采信报告文字,逐项**自行复现**——版本校验 → 计数复现 → 变异体逐行审计 → **真红跑**(把变异注入实现副本,用测试方的盲测去跑)

---

## 1. 版本一致性(通过)

我本地实测的完整 SHA256 与报告 §0 逐字节一致:

```
9DDD74855662BEAD77472A751A429C4F194E484B18FFDA0AC14DBF9CF1AC342A  DirectAddressHashST.java
D88854591EA465D522DF01C14CAE9E05EC4EE0CD4B85304ACF0CC0DE7B22C9F0  DivisionHashST.java
```

结论:报告测的确实是冻结版本,且测试期间未改动受保护实现。

## 2. 计数复现(通过)

我按报告 §5 的命令原样重跑,原始输出与报告完全一致:

```
DifferentialBlindTest          Tests run: 2
DirectAddressBlindTest         Tests run: 15
DivisionHashBlindTest          Tests run: 13
MutationKillingTest            Tests run: 8
SupplementaryAfterReadingTest  Tests run: 9
合计                            Tests run: 47, Failures: 0, Errors: 0, BUILD SUCCESS
```

没有出现"计数为 0 却算通过"的情况,分布与报告算术一致。

## 3. 变异体单点注入审计(通过)

我把 `independent/mut/` 下 8 个变异体逐一还原包名/类名后与冻结源码做**行级比对**,结果:

**8/8 个变异体与冻结源码恰好只差 1 行**,且差异行内容与报告表格声称的缺陷一致
(floorMod→%、TOMBSTONE→null、loadFactor 分母、失败探测起点上界、去掉 -minAddr、
去掉 requireNonNull、hi-lo+1→hi-lo、去掉下界检查)。

结论:不存在"顺手多改几处让测试更容易失败"的情况,变异体来源可信。

## 4. 真红跑复核(报告结论成立,但报告的证据形式比声称的弱)

报告 §4 的做法是"在变异体上断言它偏离期望值"(断言为**真**、套件为**绿**),并称这
"等价于证明该盲测在变异体上会变红"。这是**推断**,不是观察——报告本身没有让任何测试变红过。

我做了它没做的实验:把每个变异注入实现副本(不碰受保护实现),**直接运行它自己的盲测**
(`DirectAddressBlindTest` + `DivisionHashBlindTest`),看是否真的变红。结果:

| 变异体 | 真红跑结果 | 变红的盲测 |
| --- | --- | --- |
| M1 floorMod→% | RED | `hash_isFloorMod_nonNegative`(expected 12, was -1) |
| M2 delete 不留墓碑 | RED | `deleteLeavesTombstone_probeChainNotBroken` |
| M3 loadFactor 分母→modulus | RED | `loadFactor_usesTableSize`(0.5 vs 0.615) |
| M4 失败探测起点上界→modulus | RED | `classicExample_addressesInsertProbesAndASL`(94 vs 91) |
| M5 去掉 -minAddr | RED | `distinctKeys_mapToDistinctAddresses_...`(hash(10)=130 越界) |
| M6 去掉 null 校验 | RED | `nullValue_throwsNullPointerException` |
| M7 地址空间少 1 | RED(7 处失败) | `hash_matchesLinearIndex_...`、`singleValueRange_works` 等 |
| M8 去掉下界检查 | RED | `outOfRangeKey_throwsIllegalArgumentException`(IAE→AIOOBE) |

**8/8 结论正确**,即报告的核心主张经得起复验;但证据强度需要按事实纠正:
"断言变异体偏离期望"只能证明"变异体可被观测到偏差",**不能**替代"测试真的变红"这一观察。
本次复核补上了后者。

## 5. 对测试方法本身的两点批评

1. **绿的击杀测试不算击杀证据**(见上)。若某条击杀断言写成与盲测不同的性质,就会出现"假击杀"而不被察觉。
2. `MutationKillingTest` 第 110 行 `assertTrue(threwAnything || true)` 是**恒真断言**,永远通过、不证明任何事
   (该行自称是"记录实际行为"),属于应删除的填充式断言。

## 6. 逐条回应报告 §7 的"口径分歧"(这是本轮唯一的真实发现,且缺陷在实现方的文档,不在代码)

| # | 测试方提出 | 我的裁定 | 动作 |
| --- | --- | --- | --- |
| 7.1 | 表满时 `put` 抛 `IllegalStateException`,契约未规定 | **口径分歧,判为合理**。参数本身合法,失败原因是容器状态,`IllegalStateException` 是 Java 的正确惯用法;换取 `IllegalArgumentException` 会误导 | 已在任务书 §1 中把该异常类型写入契约 |
| 7.2 | 通用构造 `hash()` 的取值基准:契约同时写 `H(key)=a·key+b` 与"地址落在 [0,capacity)",二者冲突 | **确认是实现方文档缺陷**。测试方选择"相对地址"解读是正确的;任务书文字有歧义,代码行为无误 | 已修正任务书:明确 `hash` 返回**相对地址** `H(key) − H(minKey)`,`a·key+b` 是地址的线性函数而非数组下标本身 |
| 7.3 | 满表时 `unsuccessfulProbeSum` 以 `probes == tableSize` 兜底,m=2 满表时 ASL失败 = 2.0 | **口径分歧,判为合理**。满表下失败查找理论上永不终止,以 m 为上限是唯一有限且可解释的约定 | 已在任务书中显式写明该兜底约定 |

我实测确认 7.3 描述准确:`DivisionHashST(2)` 装满两个槽后,`unsuccessfulProbeSum()` = 4、
`averageUnsuccessfulProbes()` = 2.0。

**其余 §2 报告结论(未发现真缺陷)与我的复核一致。**

## 7. 对报告 §8"未覆盖风险"的补充说明

- `DivisionHashST.toString()`:任务书未列该方法,属**任务书漏项**;实现方自己的套件
  (`DivisionHashSTTest.keysInSlotOrder`)已对格式下了断言,故该项不是实现缺陷。
- 并发/线程安全:两个类**不是线程安全的**(无同步、无 volatile),任务书未声明,现已补入契约。
- 2^26 上限的**真实内存分配**未压测:接受为已知风险(实测将需要 GB 级内存),上限拒绝逻辑已被覆盖。
- 泛型 `Value` 的 equals 语义、近满表下的逐操作差分:属于测试覆盖缺口,非实现缺陷;如需可后续补。

## 8. 最终裁定

| 分类 | 数量 | 内容 |
| --- | --- | --- |
| 真缺陷(实现) | **0** | 冻结版本上未发现 |
| 真缺陷(实现方文档) | **1** | 任务书 §1 关于 `hash()` 基准的表述歧义(7.2) |
| 口径分歧 | **2** | 表满异常类型(7.1)、满表 ASL 兜底(7.3)——均判为合理,已写入契约 |
| 覆盖缺口 | 4 | 见 §7(其中 toString 已由实现方套件覆盖) |
| 无法判定 | 0 | — |

按任务书第 5 节,以上均由最小可复现证据支撑;本次复核未改动任何受保护实现文件,
冻结版本与 SHA256 保持不变,报告结论继续有效。
