# Rijndael（AES 的推广）加密解密过程演示

> 实现：`com.ds.crypto.Rijndael`（分组变换，Nb/Nk 支持 4/6/8）、
> `com.ds.crypto.RijndaelCbc`（CBC 模式 + PKCS#7 填充）、
> `com.ds.crypto.Hex`（十六进制工具）。
> 演示：`com.ds.crypto.RijndaelDemo`；测试：`src/test/java/com/ds/crypto`（29 例）。
> 一键复现：`.\scripts\run-rijndael-tests.ps1`。
> 最近一次实跑：`testsFound=29 succeeded=29 failed=0`，`JUNIT_EXIT=0`。

本文用一个具体例子走完整条链路：**密钥扩展 → 10 轮加密 → 10 轮解密**，中间每一步都给出状态矩阵；
例子取自 FIPS-197 附录 B（AES-128 官方演算），所以下面每个中间值与官方文档逐字节一致。
看懂一个分组，Rijndael 就只剩下“轮数、行移位偏移、密钥扩展细节”这几个参数差异。

---

## 1. Rijndael 与 AES 是什么关系

AES 是 Rijndael 的子集：**分组固定 128 位**，密钥可以是 128/192/256 位。
Rijndael 本身允许分组和密钥各自取 128/192/256 位：

| 参数 | 含义 | 取值范围 |
|---|---|---|
| Nb | 状态矩阵列数（分组 128/192/256 位） | 4 / 6 / 8 |
| Nk | 密钥字数（密钥 128/192/256 位） | 4 / 6 / 8 |
| Nr | 轮数 = max(Nb, Nk) + 6 | 10 / 12 / 14 |

9 种组合都合法（本实现全部支持）；AES 只标准化了其中 Nb=4 的三种：

| 组合 | 分组 | 密钥 | 轮数 | 是否 AES |
|---|---|---|---|---|
| Nb=4, Nk=4 | 128 | 128 | 10 | AES-128 |
| Nb=4, Nk=6 | 128 | 192 | 12 | AES-192 |
| Nb=4, Nk=8 | 128 | 256 | 14 | AES-256 |
| 其余 6 种 | 128/192/256 | 128/192/256 | 12/14 | 非 AES，Rijndael 泛化形式 |

本文主例子用 Nb=4、Nk=4（AES-128），第 7 节再演示 192/256 位分组。

---

## 2. 算法结构（先看骨架，再看细节）

### 2.1 状态怎么装

一个分组按**列优先**装进 4 行 Nb 列的字节矩阵：

```text
state[r][c] = input[4*c + r]        // r = 0..3，c = 0..Nb-1
```

例如明文 `3243f6a8885a308d313198a2e0370734` 的前 4 个字节是第 0 列：

```text
初始状态（列优先：3243f6a8885a308d313198a2e0370734）
  32 88 31 e0
  43 5a 31 37
  f6 30 98 07
  a8 8d a2 34
```

### 2.2 四个轮变换

| 变换 | 作用 | 一句话理解 |
|---|---|---|
| SubBytes | 每个字节过 S 盒 | 非线性代换（S 盒由 GF(2^8) 求逆 + 仿射变换生成） |
| ShiftRows | 第 r 行循环左移 offset 列 | 把列内字节打散到不同列 |
| MixColumns | 每列左乘 MDS 矩阵 | 在 GF(2^8) 上做线性混合，让 1 字节变化扩散到整列 |
| AddRoundKey | 状态与轮密钥异或 | 唯一引入密钥的步骤 |

ShiftRows 的偏移量因 Nb 而异（这是 AES 之外最容易写错的地方）：

| Nb | 第 0/1/2/3 行的左移量 |
|---|---|
| 4 | 0, 1, 2, 3 |
| 6 | 0, 1, 2, 3 |
| 8 | 0, 1, 3, 4 |

### 2.3 轮结构

加密：

```text
AddRoundKey(state, 轮密钥 0)
for round = 1 .. Nr-1:
    SubBytes → ShiftRows → MixColumns → AddRoundKey(round)
最后一轮（round = Nr）:
    SubBytes → ShiftRows → AddRoundKey(Nr)        // 没有 MixColumns
```

解密直接实现 FIPS-197 的“逆密码”结构：

```text
AddRoundKey(state, 轮密钥 Nr)
for round = Nr-1 .. 1:
    InvShiftRows → InvSubBytes → AddRoundKey(round) → InvMixColumns
收尾（round 0）:
    InvShiftRows → InvSubBytes → AddRoundKey(0)
```

### 2.4 密钥扩展

`Nk` 个字（每个字 4 字节）扩成 `Nb*(Nr+1)` 个字，前 `Nk` 个字就是密钥本身，之后：

```text
temp = W[i-1]
if i % Nk == 0:            temp = SubWord(RotWord(temp)) ^ Rcon[i/Nk]
else if Nk > 6 && i % Nk == 4:  temp = SubWord(temp)     // 256 位密钥特有
W[i] = W[i-Nk] ^ temp
```

`Rcon` 由 `xtime` 递推：`01, 02, 04, 08, 10, 20, 40, 80, 1b, 36, ...`。

---

## 3. 一个完整例子：FIPS-197 附录 B（AES-128）

### 3.1 参数与输入

```text
明文 P = 3243f6a8885a308d313198a2e0370734
密钥 K = 2b7e151628aed2a6abf7158809cf4f3c
Nb = 4（分组 16 字节），Nk = 4（密钥 16 字节），Nr = max(4,4)+6 = 10
```

期望密文（FIPS-197 附录 B）：

```text
C = 3925841d02dc09fbdc118597196a0b32
```

### 3.2 密钥扩展：4 个字 → 44 个字

前 4 个字就是密钥；从 W[4] 开始，下标能被 Nk 整除的字要做 RotWord + SubWord + Rcon：

```text
轮密钥  0（W[ 0.. 3]）: 2b7e1516 28aed2a6 abf71588 09cf4f3c
轮密钥  1（W[ 4.. 7]）: a0fafe17 88542cb1 23a33939 2a6c7605   ← W[4] 做 RotWord+SubWord+Rcon
轮密钥  2（W[ 8..11]）: f2c295f2 7a96b943 5935807a 7359f67f   ← W[8] 同上
...
轮密钥 10（W[40..43]）: d014f9a8 c9ee2589 e13f0cc8 b6630ca6
```

W[4] 的来历可以手算：

```text
W[3]        = 09cf4f3c
RotWord     = cf4f3c09
SubWord     = 8a84eb01          （查 S 盒）
Rcon[1]=01  →  8a84eb01 ^ 01000000 = 8b84eb01
W[4] = W[0] ^ 8b84eb01 = 2b7e1516 ^ 8b84eb01 = a0fafe17
```

### 3.3 加密逐步（轮 1 展开）

初始轮密钥加（明文异或轮密钥 0）：

```text
初始状态（列优先：3243f6a8885a308d313198a2e0370734）
  32 88 31 e0
  43 5a 31 37
  f6 30 98 07
  a8 8d a2 34

AddRoundKey(0) 之后（列优先：193de3bea0f4e22b9ac68d2ae9f84808）
  19 a0 9a e9
  3d f4 c6 f8
  e3 e2 8d 48
  be 2b 2a 08
```

第 1 轮四步（每步后的状态，列优先十六进制与 4×4 网格）：

```text
SubBytes（列优先：d42711aee0bf98f1b8b45de51e415230）
  d4 e0 b8 1e
  27 bf b4 41
  11 98 5d 52
  ae f1 e5 30

ShiftRows（列优先：d4bf5d30e0b452aeb84111f11e2798e5）
  d4 e0 b8 1e
  bf b4 41 27
  5d 52 11 98
  30 ae f1 e5

MixColumns（列优先：046681e5e0cb199a48f8d37a2806264c）
  04 e0 48 28
  66 cb f8 06
  81 19 d3 26
  e5 9a 7a 4c

AddRoundKey(1)（列优先：a49c7ff2689f352b6b5bea43026a5049）
  a4 68 6b 02
  9c 9f 5b 6a
  7f 35 ea 50
  f2 2b 43 49
```

这几组值可以和 FIPS-197 附录 B 的中间值逐字节对上（例如 round[1].start = `193de3be...`，
round[1].s_box = `d42711ae...`，round[1].add_round_key = `a49c7ff2...`），
说明实现与标准完全一致，而不是“最后碰巧得到同一个密文”。

第 2～9 轮每轮结束（AddRoundKey 后）的状态：

```text
轮 2: aa8f5f0361dde3ef82d24ad26832469a
轮 3: 486c4eee671d9d0d4de3b138d65f58e7
轮 4: e0927fe8c86363c0d9b1355085b8be01
轮 5: f1006f55c1924cef7cc88b325db5d50c
轮 6: 260e2e173d41b77de86472a9fdd28b25
轮 7: 5a4142b11949dc1fa3e019657a8c040c
轮 8: ea835cf00445332d655d98ad8596b0c5
轮 9: eb40f21e592e38848ba113e71bc342d2
```

第 10 轮（没有 MixColumns）：

```text
SubBytes（列优先：e9098972cb31075f3d327d94af2e2cb5）
  e9 cb 3d af
  09 31 32 2e
  89 07 7d 2c
  72 5f 94 b5

ShiftRows（列优先：e9317db5cb322c723d2e895faf090794）
  e9 cb 3d af
  31 32 2e 09
  7d 2c 89 07
  b5 72 5f 94

AddRoundKey(10)（列优先：3925841d02dc09fbdc118597196a0b32）
  39 02 dc 19
  25 dc 11 6a
  84 09 85 0b
  1d fb 97 32
```

最终密文：

```text
C = 3925841d02dc09fbdc118597196a0b32
```

与 FIPS-197 附录 B 的期望值一致。

### 3.4 解密逐步

解密不是把加密步骤简单倒放，而是 FIPS-197 的“逆密码”结构：
先加第 Nr 轮密钥，再依次做 `InvShiftRows → InvSubBytes → AddRoundKey → InvMixColumns`，
最后一轮不再做 InvMixColumns。下面的轮号沿用实现的标注：第 Nr 轮密钥加 → Nr-1..1 逆轮 → 0 号收尾轮。

起点（密文加第 10 轮密钥）：

```text
解密起点：密文 + 轮密钥 10（列优先：e9317db5cb322c723d2e895faf090794）
  e9 cb 3d af
  31 32 2e 09
  7d 2c 89 07
  b5 72 5f 94
```

第 9 逆轮（每一步）：

```text
InvShiftRows    （列优先：e9098972cb31075f3d327d94af2e2cb5）
InvSubBytes     （列优先：eb40f21e592e38848ba113e71bc342d2）
AddRoundKey(9)  （列优先：473794ed40d4e4a5a3703aa64c9f42bc）
InvMixColumns   （列优先：876e46a6f24ce78c4d904ad897ecc395）
```

中间逆轮结束时（InvMixColumns 后）的状态：

```text
逆轮 8: be3bd4fed4e1f2c80a642cc0da83864d
逆轮 7: f783403f27433df09bb531ff54aba9d3
逆轮 6: a14f3dfe78e803fc10d5a8df4c632923
逆轮 5: e1fb967ce8c8ae9b356cd2ba974ffb53
逆轮 4: 52a4c89485116a28e3cf2fd7f6505e07
逆轮 3: acc1d6b8efb55a7b1323cfdf457311b5
逆轮 2: 49db873b453953897f02d2f177de961a
逆轮 1: d4bf5d30e0b452aeb84111f11e2798e5
```

收尾轮（round 0）：

```text
InvShiftRows  → d42711aee0bf98f1b8b45de51e415230
InvSubBytes   → 193de3bea0f4e22b9ac68d2ae9f84808
AddRoundKey(0)→ 3243f6a8885a308d313198a2e0370734   （恢复出明文）
```

---

## 4. 与 JDK 标准实现对照

演示里把同一组密钥/明文交给 JDK 的 `AES/ECB/NoPadding`，再与本实现比较：

```text
JDK 加密结果   = 3925841d02dc09fbdc118597196a0b32
本实现加密结果 = 3925841d02dc09fbdc118597196a0b32
一致：true
JDK 解密本实现密文 = 3243f6a8885a308d313198a2e0370734，与原始明文一致：true
```

这不是偶然：测试里还对 3 种 AES 密钥长度、每种 60 组随机输入做了双向差分
（本实现加密 = JCE 加密，双方都能解开对方密文）。

---

## 5. 实际消息：CBC + PKCS#7

把“一次一个分组”扩展成任意长度消息，演示使用：

```text
明文        : Rijndael 加密演示：你好，世界！
UTF-8 字节数: 42，填充后 48 字节（3 个分组）
密钥        : RijndaelDemo1234（16 字节）
IV          : 0123456789abcdef
密文        : ded18709d20fd8d9b00f347a16f8c00d
              e5e665832af77a0e8d854687e57e314e
              6573e47199cb26aceeed52bbc482f043
解密        : Rijndael 加密演示：你好，世界！，与原文一致：true
与 JDK AES/CBC/PKCS5Padding 密文一致：true
```

要点：

- CBC 的链式公式是 `C_i = E_K(P_i xor C_{i-1})`，`C_0 = IV`；解密反过来。
- PKCS#7 填充：缺 n 字节就补 n 个值为 n 的字节；明文恰好整块时补一整个块
  （所以 16 字节明文加密后是 32 字节）。
- IV 必须随机且不可预测；CBC 本身不提供完整性保护。

---

## 6. 雪崩效应

只翻转明文第 1 位，看密文有多少位改变：

```text
原始明文密文 : 3925841d02dc09fbdc118597196a0b32
翻转 1 位后  : 8c55eb94dfead5c4fa78df0344bf0cd2
不同的密文位数 = 68 / 128（53.1%）
```

10 轮之后 1 位差异扩散到约一半的密文位，这是 SubBytes + ShiftRows + MixColumns 共同作用的结果。

---

## 7. Rijndael 泛化：192/256 位分组

AES 只固定了 128 位分组；Rijndael 的 9 种组合在本实现中都能往返：

```text
Nb=4 Nk=4（分组 16 字节，密钥 16 字节，Nr=10）：往返一致
Nb=4 Nk=6（分组 16 字节，密钥 24 字节，Nr=12）：往返一致
Nb=4 Nk=8（分组 16 字节，密钥 32 字节，Nr=14）：往返一致
Nb=6 Nk=4（分组 24 字节，密钥 16 字节，Nr=12）：往返一致
Nb=6 Nk=6（分组 24 字节，密钥 24 字节，Nr=12）：往返一致
Nb=6 Nk=8（分组 24 字节，密钥 32 字节，Nr=14）：往返一致
Nb=8 Nk=4（分组 32 字节，密钥 16 字节，Nr=14）：往返一致
Nb=8 Nk=6（分组 32 字节，密钥 24 字节，Nr=14）：往返一致
Nb=8 Nk=8（分组 32 字节，密钥 32 字节，Nr=14）：往返一致
9 种组合通过数：9/9
```

256 位分组 + 256 位密钥的密文示例：

```text
Nb=8, Nk=8 密文 = 448e5ad1a7ad963c1ef5fa997fff15d39a9c0a69d73609f3f425cd7d07627bec
```

两条与 AES 不同的实现细节：

1. Nb=8 时 ShiftRows 的偏移量是 `0,1,3,4`（不是 `0,1,2,3`）；
2. Nk=8 时密钥扩展多一条 `i % Nk == 4 → SubWord` 分支（没有 RotWord / Rcon）。

这两处正是泛化路径最容易出错的地方，测试里用独立实现生成的固定向量把它们锁住。

---

## 8. 测试与验证

### 8.1 测试规模

`.\scripts\run-rijndael-tests.ps1` 实跑结果：

```text
containersFound=3 testsFound=29 succeeded=29 failed=0 skipped=0
JUNIT_EXIT=0
```

两个测试类共 29 例：

| 测试类 | 例数 | 覆盖内容 |
|---|---|---|
| `RijndaelTest` | 19 | S 盒/GF(2^8)、FIPS-197 KAT、NIST ECB、密钥扩展、JCE 差分、9 种参数、异常路径、trace 状态顺序 |
| `RijndaelCbcTest` | 10 | PKCS#7 边界、CBC 往返、JCE 差分、NIST SP 800-38A CBC 固定向量、IV/长度异常 |

### 8.2 已知答案向量（KAT）

| 来源 | 密钥 | 明文 | 密文 |
|---|---|---|---|
| FIPS-197 附录 B | `2b7e1516...4f3c` | `3243f6a8...0734` | `3925841d02dc09fbdc118597196a0b32` |
| FIPS-197 C.1（AES-128） | `00010203...0e0f` | `00112233...eeff` | `69c4e0d86a7b0430d8cdb78070b4c55a` |
| FIPS-197 C.2（AES-192） | `00010203...1617` | `00112233...eeff` | `dda97ca4864cdfe06eaf70a0ec0d7191` |
| FIPS-197 C.3（AES-256） | `00010203...1e1f` | `00112233...eeff` | `8ea2b7ca516745bfeafc49904b496089` |
| NIST SP 800-38A ECB | `2b7e1516...4f3c` | `6bc1bee2...172a` | `3ad77bb40d7a3660a89ecaf32466ef97` |
| NIST SP 800-38A CBC（4 块） | `2b7e1516...4f3c` | `6bc1bee2...3710` | `7649abac...e1a7` |

每个向量都同时验证“加密得到期望值”和“解密还原明文”。

### 8.3 差分测试

- **JCE 对照**：随机密钥/明文，`AES/ECB/NoPadding` 与本实现双向比对（3 种密钥长度 × 60 组）。
- **CBC 对照**：`AES/CBC/PKCS5Padding` 与本实现比对（3 种密钥长度 × 10 种长度 × 2 轮）。
- **独立实现交叉验证**：9 种 Nb/Nk 组合的固定向量与 py3rijndael 0.3.3（phpseclib Rijndael 的 Python 移植）
  逐字节一致；这些向量已固化进测试，避免以后改坏非 AES 路径。

### 8.4 测试抓出的真实问题

初版构造函数只检查了密钥长度“是 4 的倍数且在 16..32 之间”，导致 **20/28 字节密钥
（Nk=5/7）被错误接受**；分组长度同理。测试 `rejectsInvalidKeyAndBlockSizes` 失败后，
校验改为精确的 `16/24/32`（Nb/Nk ∈ {4,6,8}），全部测试转绿。这也是先写测试的价值。

---

## 9. 怎么复现

```powershell
# 编译 -> 跑 29 个 JUnit 用例 -> 跑逐步演示
.\scripts\run-rijndael-tests.ps1

# 只跑演示（先编译）
javac -encoding UTF-8 -d target\rijndael-classes src\main\java\com\ds\crypto\*.java
java -Dfile.encoding=UTF-8 -cp target\rijndael-classes com.ds.crypto.RijndaelDemo
```

代码里最常用的两个入口：

```java
// 单个分组：AES-128
Rijndael aes = new Rijndael(key16);
byte[] ct = aes.encryptBlock(pt16);
byte[] pt = aes.decryptBlock(ct);

// 任意长度消息：CBC + PKCS#7
RijndaelCbc cbc = new RijndaelCbc(key16);
byte[] ciphertext = cbc.encrypt(messageBytes, iv16);
byte[] plaintext = cbc.decrypt(ciphertext, iv16);
```

想看内部过程时传 `Rijndael.Trace`：

```java
aes.encryptBlock(pt16, (round, phase, state) -> {
    // round：轮次；phase：subBytes / shiftRows / mixColumns / addRoundKey ...
    // state：4 行 × Nb 列的状态快照（每次回调都是独立副本）
});
```

---

## 10. 安全边界

这是**教学实现**，目标是“每一步都能看懂、能对上标准”，不是生产级密码库：

- 非恒定时间：S 盒查表、分支都可能泄露侧信道信息；
- 没有认证：CBC 可被篡改（需要 MAC 或改用 GCM 等 AEAD）；
- ECB 不应处理真实数据（相同明文块产生相同密文块）；
- 生产环境请使用 JDK 自带的 JCE（`AES/GCM/NoPadding`）或 BouncyCastle，并正确处理密钥与 IV 管理。

如果只是要“把 Rijndael 学明白”，这份实现和演示可以作为可运行的教材；
如果要“保护真实数据”，请直接用经过审计的库。

---

## 11. “能不能被破解”：分算法、实现、用法三层看

结论先说：**AES/Rijndael 的分组算法本身至今没有被实际破解；但这套实现和演示用法在生产环境里能被攻破**，
攻击点不在“算出密钥”，而在侧信道、模式误用、缺少认证和密钥管理。

### 11.1 算法层：全轮 AES 没有实用攻击

AES 经过 1998–2000 年公开评选、2001 年标准化，之后 20 多年持续分析。目前最好的单密钥攻击是
biclique 分析（Bogdanov、Khovratovich、Rechberger，2011）：

| 版本 | 最好单密钥攻击复杂度 | 说明 |
|---|---|---|
| AES-128（10 轮） | 2^126.1 | 理论结果，相对穷举只快不到 4 倍 |
| AES-192（12 轮） | 2^189.7 | 理论结果，无实用价值 |
| AES-256（14 轮） | 2^254.4 | 理论结果，无实用价值 |

另外还有 related-key 攻击（如 AES-256 的 2^99.5，Biryukov–Khovratovich 2009），但它要求攻击者
能拿到“由同一主密钥派生出的多个相关密钥”，正常协议里这个前提不成立。积分/平方攻击只对
约 6–7 轮的简化版本有效，打不到全轮。

穷举的量级（宇宙年龄按 1.38×10^10 年）：

| 密钥长度 | 密钥空间 | 10^12 次/秒 穷举 | 10^18 次/秒 穷举 |
|---|---|---|---|
| AES-128 | 3.4×10^38 | 约 1.1×10^19 年（约 8×10^8 倍宇宙年龄） | 约 1.1×10^13 年 |
| AES-192 | 6.3×10^57 | 约 2.0×10^38 年 | 约 2.0×10^32 年 |
| AES-256 | 1.2×10^77 | 约 3.7×10^57 年 | 约 3.7×10^51 年 |

量子计算方面：Grover 算法把穷举降到约 2^(n/2) 次量子操作（AES-128 约 2^64、AES-256 约 2^128），
但需要大规模容错量子计算机，目前不存在；Shor 算法破的是 RSA/ECC，不影响 AES。因此长期或
抗量子场景选 AES-256，普通场景 AES-128 已足够。

两点容易误解的地方：

- **Rijndael 的 192/256 位分组不等于更安全。** 它没有被 NIST 标准化，公开分析远少于 AES；
  AES 只定义了 128 位分组。更大的分组主要影响工作模式的生日界，密钥长度才决定暴力破解上限。
- **轮数不是越多越安全那么简单。** 轮数由 Nb/Nk 决定（10/12/14），设计上已经匹配密钥长度；
  自己加轮会破坏标准，也不会带来可证明的收益。

### 11.2 实现与用法层：这三个演示能直接“攻破”

**（1）ECB 泄露明文模式。** 相同明文块得到相同密文块：

```text
[ECB] 明文块        = ATTACK AT DAWN!!
[ECB] 密文块 1      = 6847531d5ec6ebf44a3549a6e2d32075
[ECB] 密文块 2      = 6847531d5ec6ebf44a3549a6e2d32075
[ECB] 两块密文相同  = true
```

真实数据里这会泄露“哪些块相同”，还允许块重排、替换、重放（著名的“ECB 企鹅图”）。

**（2）CBC 没有完整性，可被可控篡改。** 改动第 1 个密文块（C1）第 10 字节的最低位后：

```text
[CBC] 原始第 2 块  = Attack at dawn!!
[CBC] 篡改后第 2块 = Attack at eawn!!
[CBC] 第 1 块变乱码（不影响 2） = true
[CBC] 解密没有报错，说明完整性未被检测
```

原理：CBC 里 `P2 = D(C2) xor C1`，所以翻转 C1 的某一位会精确翻转 P2 的对应位；同时
C1 对应的 P1 变成乱码，但攻击者可以用“丢弃第一块”或“只关心第二块”的方式利用。
换句话说，攻击者不需要密钥也能改明文——只是不能控制改后的全部内容。

**（3）填充错误可区分，构成 padding oracle。** 篡改最后一个密文块后：

```text
[Padding] 篡改末块抛出： PKCS#7 填充非法：末字节 106
[Padding] 错误信息足以区分“填充合法/非法”，服务端若原样返回就构成 padding oracle
```

如果服务端把“填充错误”和“其他错误”区分返回（状态码、错误文案、耗时差异都算），攻击者可以
自适应地逐字节恢复明文（Vaudenay 2002 的经典攻击）。本实现的 `IllegalArgumentException`
信息很详细，直接暴露了这一点；生产代码应统一错误响应，并改用 AEAD。

**（4）侧信道。** 本实现是教学代码，S 盒是 256 字节查表，访问位置依赖秘密数据，存在
cache-timing/缓存侧信道风险；Java 的 JIT、GC、内存分配还会放大时间差异。生产用 AES-NI
（JCE 默认会走硬件指令）可以避免表查找类侧信道，但自己用 Java 写查表 AES 很难做到恒定时间。

**（5）密钥与生命周期。** 演示里的 `"RijndaelDemo1234"`、`"0123456789abcdef"` 是 ASCII 字符串，
不是随机密钥；熵低、可被字典命中，而且 Java `String` 不可清零，密钥会留在堆内存和可能的日志里。
真实系统应使用随机 128/256 位密钥，口令走 Argon2id/scrypt/PBKDF2 派生，密钥放 KMS/HSM，
并避免打印、避免在异常信息里带出密钥材料。

**（6）本实现没有封装认证、重放保护和密钥轮换。** 它只回答“分组怎么变换”，不回答“协议是否安全”。

### 11.3 生产建议

| 场景 | 建议 |
|---|---|
| 通用数据加密 | `AES/GCM/NoPadding`（AEAD），每次加密用 `SecureRandom` 生成 12 字节 nonce，tag 128 位 |
| 必须用 CBC | 随机不可预测 IV + Encrypt-then-MAC（HMAC-SHA256），先验证 MAC 再解密 |
| 口令加密 | 不要直接当密钥；用 Argon2id/scrypt 派生，配合随机 salt |
| 密钥管理 | 随机 128/256 位；KMS/HSM 托管；定期轮换；不要硬编码进源码 |
| 学习/实验 | 这份实现足够；真实数据不要用自己写的密码学代码 |

JDK 里最简的安全用法：

```java
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
byte[] nonce = new byte[12];
new SecureRandom().nextBytes(nonce);
cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
byte[] ciphertext = cipher.doFinal(plaintext);
```

**一句话总结：算法本身“破不了”（没有实用攻击），但这份教学实现 + 演示用法“能被破”**
（ECB 泄模式、CBC 可篡改、填充错误成 oracle、查表侧信道、弱密钥/固定 IV）。学习看算法，
生产用标准库 + AEAD + 正确密钥管理。
