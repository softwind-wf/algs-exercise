# 散列表验证脚本(哈希练习题)

针对 `cn.exercise.algs4.datastructure.hash` 下的两个类(直接定址法 / 除留余数法),做两类**对抗性验证**,
用来回答"自己写、自己测,凭什么相信测试真的有效"。

## 1. 差分测试(已在测试套件里,随 `mvn test` 一起跑)

`src/test/java/cn/exercise/algs4/datastructure/hash/HashSTDifferentialTest.java`

- 用固定种子生成 2 万次随机操作(put / get / contains / delete / size / isEmpty),同时施加于被测散列表和 JDK 的
  `java.util.HashMap`,逐操作比对;另用一段独立编写的线性探测模拟程序重算 ASL 分子。
- 意义:参照物是**独立实现**(HashMap),不依赖"我认为正确答案是多少",能戳穿"实现和测试共享同一个误解"的自测盲区。

## 2. 变异测试(本目录脚本)

`mutate.ps1` 会把实现**故意改坏**(9 种典型错误:去掉 `floorMod`、删除不留墓碑、插入不复用墓碑、
ASL 分母取错、模数不做质数筛选、质数判定边界错、探测次数少 1、忘记减去 `minAddr`、地址空间少 1),
然后运行测试:

- 测试**变红** → 该变异体"被杀死",说明测试确实能发现这类缺陷;
- 测试**仍然全绿** → 变异体"存活",说明测试在这个点上是空的,必须补测试或修实现。

脚本每次变异后立即从备份恢复源码,结束时校验两个源文件的 SHA256 与原始一致。
另外内置"防假通过"检查:若一个测试都没被找到(退出码 2),直接判失败 —— `Tests run: 0 / BUILD SUCCESS`
这种静默通过会被拦下。

### 运行

```powershell
pwsh -File scripts\verify-hash\mutate.ps1
# 可选参数
pwsh -File scripts\verify-hash\mutate.ps1 -Jdk8 "C:\Program Files\Java\jdk-1.8" -MavenRepo "D:\maven-repo"
```

## 3. 独立测试任务书(交给另一个 AI)

`INDEPENDENT-TEST-BRIEF.md`:实现方与测试方分离时,直接把这份文件整份粘贴给另一个 AI(最好换厂商/换模型)。
内含:冻结的接口契约与规格、ASL 计数口径、最低测试要求(含固定种子差分测试与自造变异测试)、
必须提供原始输出与计数的证据格式、分歧归类规则、JDK 8 环境与被测文件 SHA256。

它不能保证什么,也写在文件第 8 节:同源模型可能共享盲区、规格模糊处只会产生"口径分歧"、
测试方自己也可能假通过、以及"未发现缺陷"不等于"证明正确"。

需要本机装有 JDK 8 与本地 Maven 仓库(项目 `pom.xml` 的 `<java.version>1.8</java.version>`,
用 JDK 17 编译不会暴露 Java 9+ API 的问题)。

## 4. 复核第三方提交的变异体与击杀结论

`audit-mutants.ps1`:独立测试方提交"变异体 + 击杀结论"时,实现方用它复验,不采信文字。

- **Part 0**:把 `src/test/.../independent/mut/` 下每个变异体还原包名/类名后与冻结实现做行级比对,
  确认"单点注入"属实(差异行应与声称的缺陷一致);
- **Part 1**:把每个变异逐个注入**实现副本**(绝不修改 `src/main`),用测试方自己的盲测真实运行一次,
  期望变红。**"断言变异体偏离期望"不算击杀证据 —— 只有测试真的变红才算。**

```powershell
pwsh -File scripts\verify-hash\audit-mutants.ps1
```

第 1 轮独立测试(qwen-3.8)的复核结论见 `independent/IMPLEMENTER-REVIEW.md`:
报告计数 47 与变异体单点注入均经复现属实、8/8 真红跑成立;
**1 处真缺陷落在实现方文档**(任务书对 `hash()` 基准的表述歧义,已在第 3 节任务书中修正),
**0 处实现缺陷**;2 处口径分歧(表满异常类型、满表 ASL 兜底)判为合理并写入契约。

### 最近一次运行结果(2026-10-01)

```
BASELINE(原始实现) exit=0  FOUND=39 STARTED=39 SUCCEEDED=39 FAILED=0
变异体总数 = 9,被杀死 = 9,存活 = 0,未应用 = 0
DirectAddressHashST: 与原始一致 = True
DivisionHashST:      与原始一致 = True
```

## 不能证明什么

变异测试只能说明"现有测试能发现这 9 类缺陷",**不能证明代码没有 bug**:
若实现与测试共享同一个错误理解(例如双方都按错误的 ASL 口径计数),变异测试照样全绿。
差分测试把参照物换成 JDK 实现,能覆盖一部分这类盲区,但仍不是形式化证明。
因此这里的证据应表述为"未发现缺陷",而不是"已证明正确"。
