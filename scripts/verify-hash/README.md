# 散列表验证脚本(哈希练习题)

针对 `cn.exercise.algs4.datastructure.hash` 下的两个类(直接定址法 / 除留余数法),做两类**对抗性验证**,
用来回答"自己写、自己测,凭什么相信测试真的有效"。

## 1. 差分测试(已在测试套件里,随 `mvn test` 一起跑)

`src/test/java/cn/exercise/algs4/datastructure/hash/HashSTDifferentialTest.java`

- 用固定种子生成 2 万次随机操作(put / get / contains / delete / size / isEmpty),同时施加于被测散列表和 JDK 的
  `java.util.HashMap`,逐操作比对;另用一段独立编写的线性探测模拟程序重算 ASL 分子。
- 意义:参照物是**独立实现**(HashMap),不依赖"我认为正确答案是多少",能戳穿"实现和测试共享同一个误解"的自测盲区。

## 2. 变异测试(本目录脚本)

`mutate.ps1` 会把实现**故意改坏**(62 种典型错误,覆盖十二类散列表:去掉 `floorMod`、删除不留墓碑、
插入不复用墓碑、ASL 分母取错、模数不做质数筛选、质数判定边界错、探测次数少 1、忘记减去 `minAddr`、
地址空间少 1、数字分析法拼地址用加代乘、位分布统计记错位、熵计算把空数字算进去、删除不减 size、
平方取中的中位窗口偏移算错、去掉平方溢出检查、位数用 k 而非平方数位数、删除不留墓碑、
折叠法段的提取用除法、分界叠加奇偶段判定反了、忘记对表长取模、put 不识别重复键、反序不补零、
随机数法不屏蔽成 31 位非负、种子不参与混合、hash 忘记取模、探测步长改成 2、delete 不留墓碑、
全域散列公式漏掉 +b、a 可能取到 0、不校验 p 是否素数、hash 去掉上界检查、碰撞统计不校验 k1==k2、
闭合公式条件写错、开放地址探测步长改成 2、扩容阈值放宽一倍、扩容后忘记清零墓碑、delete 不留墓碑、
插入不复用墓碑、不校验散列函数返回值、二次探测 PLUS 步长退化/正负号反了/序号取错/扩容不保持 3 mod 4/
delete 不留墓碑/失败探测不闭合、双重散列步长退化/忘记乘步长/不校验 H2 范围/delete 不留墓碑/
扩容不取素数/默认 H2 可能取 0、链地址法 put 不去重/删除头结点写回自身/扩容阈值放宽/
ASL成功用 l² 代替 l(l+1)/2/失败比较多算判空/ASL失败分母用元素个数、完全散列第二级槽位数用 n_i/第二级不做碰撞检测/第二级地址对第一级槽位数取模/get 不校验关键字/空间统计把 m_i 记成 n_i),
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

**跑全量回归时务必关闭 Maven 增量编译,并且先用 `clean` 重建**:

```powershell
mvn -o clean -Dmaven.compiler.useIncrementalCompilation=false "-Dtest=...*" -DfailIfNoTests=false test
```

两次实测事故:
1. `mutate.ps1` 会临时改写全部实现源码并恢复,maven-compiler-plugin 的增量判断在之后可能误报
   `Nothing to compile - all classes are up to date`,结果是**新类的 class 文件根本没生成**,
   测试报 `ClassNotFoundException` 甚至**静默少跑一批用例而整体仍然 BUILD SUCCESS**
   (上一轮"少了 19 个用例"和"少了 6 个用例 + 1 个 ERROR"都是这个机制)。
2. **更危险的一次**:`Copy-Item` 恢复源码时会保留备份文件的旧 mtime,于是"源码比 class 旧",
   增量构建永远跳过重编译;若变异窗口内有任何构建把变异 class 写进了 `target\classes`
   (实测发生过,失败签名正好是变异体 M9"地址空间少 1"),后续测试就**跑在变异体上**。
   加固:①恢复源码后刷新其 mtime;②harness 结束时清掉 `target\classes` 中本模块的散列 class;
   ③权威回归一律 `mvn clean test`。

### 最近一次运行结果(2026-10-03)

```
BASELINE(原始实现) exit=0  FOUND=231 STARTED=231 SUCCEEDED=231 FAILED=0
变异体总数 = 62,被杀死 = 62,存活 = 0,未应用 = 0
十二个实现逐个校验 SHA256:与原始一致 = True
已清理 maven 输出目录中的散列 class
```

`mvn clean test` 权威回归:**299 个测试全绿**,构成为:实现方 231
+ 仓库既有 `edu.princeton.cs.algs4.SeparateChainingHashSTTest` 21(同名通配符会一并匹配到)
+ 独立测试方 47。

> 注:用通配符 `SeparateChainingHashSTTest*` 会把仓库里 princeton 的同名测试一并选中;
> 只跑本包请用 `scripts/verify-hash/mutate.ps1`(它用全限定类名列表,不含外部类)。

> **等价变异体**:二次探测的 M45 最初写成"`find` 里不再检测轨道走完",它**存活**了 —— 核对语义后确认
> 这是**等价变异体**(去掉 break 只是多绕几圈,返回值与 `lastProbes` 完全不变)。
> 但同一个 break 在 `unsuccessfulProbeSum` 里是可观测的(复合表长 m=8 时失败探测次数会从 13 变成 17),
> 于是把变异改到那里,并补了一个 m=8 的轨道闭合测试,才真正被杀死。
> **教训:变异体存活有两种可能 —— 测试有缺口,或者变异本身就是等价的;必须区分,不能直接当成缺口去补测试。**

> 小坑:跑 `mvn clean` 时**不要把日志写进 `target\`**——clean 插件删不掉正在被占用的日志文件,
> 整个构建会以 `BUILD FAILURE` 结束(实测踩过)。日志写到 `$env:TEMP` 或仓库外即可。

> **防"静默少跑/跑错实现"**:脚本内置基线用例数下限 `$minBaselineTests`(新增测试时同步上调,现值 231)。

## 不能证明什么

变异测试只能说明"现有测试能发现这 9 类缺陷",**不能证明代码没有 bug**:
若实现与测试共享同一个错误理解(例如双方都按错误的 ASL 口径计数),变异测试照样全绿。
差分测试把参照物换成 JDK 实现,能覆盖一部分这类盲区,但仍不是形式化证明。
因此这里的证据应表述为"未发现缺陷",而不是"已证明正确"。
