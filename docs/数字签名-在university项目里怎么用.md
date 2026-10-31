# 数字签名在 university 项目里怎么用：两个值得做的场景 + 一个能跑的 PoC

> PoC：`src/main/java/com/ds/crypto/UniversitySignatureDemo.java`
> 一键复现：`.\scripts\run-university-signature-demo.ps1`
> 原始输出：`docs/university-signature-raw.txt`（67 行，9 项自检全过、退出码 0）
> 前置阅读：[数字签名-讲解与演示.md](<docs/数字签名-讲解与演示.md>)

**结论先说：能，但只值得用在"跨信任边界"的地方。** 项目里最合适的两处是**成绩单/证明文件签名**和
**审计日志防篡改**；其余场景要么没必要，要么应该用别的手段。

---

## 0. 先判断：什么时候值得上签名

签名解决的是"**接收方不完全信任发送方/存储方**"的问题。校内登录、权限控制已经有
session + CSRF + RBAC，不需要签名。判断标准三条，满足任意两条才值得：

1. 文件或数据会**离开系统**（被下载、转发、打印、提交给外部）；
2. 接收方需要**独立验证**，且拿不到你的数据库；
3. 出现纠纷时需要**向第三方证明**"谁发的、有没有被改"。

成绩单、在读证明、毕业证明满足 1+2+3；审计日志满足 2+3（内部取证、监管检查）；
普通公告、论坛帖子一条都不满足。

---

## 1. 场景评估

| 场景 | 价值 | 成本 | 结论 | 落点 |
|---|---|---|---|---|
| **成绩单 / 在读证明 / 毕业证明 PDF 签名** | 高 | 中 | **首选** | `StudentService.transcript()`、新表、`/verify` 页 |
| **审计日志哈希链 + 定期锚点签名** | 中高 | 低 | **强烈推荐** | `AuditService`、`audit_log`、`V13` 迁移 |
| 报表/名单导出签名（评奖、上报教育厅） | 中 | 低 | 可选 | `StatsReportService` / `ScheduleExcelWriter` |
| 与别的系统交换数据（教务↔学工） | 中 | 中 | 看是否真有第二个系统 | 新接口 + 对方公钥 |
| 学生提交作业"数字签名" | 低 | 高 | **不做** | 学生没有可信密钥与证书 |
| 登录/会话/JWT | 无 | — | **不做** | session + CSRF 已覆盖 |
| 服务间调用（内部微服务） | 低 | 中 | **用 HMAC，不必非对称** | 新过滤器 |
| 用签名代替加密 | 无 | — | **不做** | 签名不提供机密性 |

---

## 2. 首选方案：成绩单签名 + 公开验证

### 2.1 流程

```text
学生（已登录）                 学校服务端                       验证方（用人单位/其他学校）
  │  点"下载成绩单"               │                                   │
  │ ───────────────────────────> │  1. StudentService.transcript()    │
  │                              │  2. 生成规范化 JSON（或 PDF）       │
  │                              │  3. SHA-256 + 私钥签名              │
  │                              │  4. 返回 JSON + 签名 + 证书链        │
  │ <─────────────────────────── │                                   │
  │  转发给验证方 ──────────────────────────────────────────────────> │
  │                              │        5. 打开 /verify 上传文件+签名 │
  │                              │ <────────────────────────────────── │
  │                              │  6. 用学校公钥验签 → 通过/不通过      │
```

关键点：**验证页不需要登录、不需要数据库**——它只需要"文件 + 签名 + 学校公钥"。

### 2.2 签什么（比算法更容易出事）

被签内容里必须带上：

| 字段 | 防什么 |
|---|---|
| `type: "TRANSCRIPT"` + `version` | 域分离：防止把"在读证明"的签名挪到"成绩单"上 |
| `studentId` / `studentName` / `deptName` | 张冠李戴（换人） |
| `term` / 每门课的 `courseNo, credits, grade, point` | 改分数、改课程 |
| `serialNo`（流水号，唯一） | 重放、重复下载对不上号 |
| `issuedAt`（时间戳） | 事后补签、时间倒签 |
| 汇总字段 `courseCount, earnedCredits, gpa` | 改汇总 |

序列化必须**确定性**：字段顺序固定、数字格式固定（`4.0` 不写成 `4`）、不塞用户可控的原始字符串。
PoC 里手写了固定顺序的 JSON；生产建议用 JSON Canonicalization（JCS）或 protobuf 的确定性编码。

### 2.3 存储（新增一张表）

```sql
CREATE TABLE IF NOT EXISTS transcript_signature (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    student_id   VARCHAR(20)  NOT NULL,
    term         VARCHAR(20)  NULL,
    serial_no    VARCHAR(40)  NOT NULL,
    payload_hash CHAR(64)     NOT NULL COMMENT 'SHA-256(规范化内容)',
    signature    TEXT         NOT NULL COMMENT 'Base64 签名',
    cert_serial  VARCHAR(64)  NULL     COMMENT '签名证书序列号，便于换钥后追溯',
    issued_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_serial (serial_no),
    KEY idx_student_term (student_id, term)
) ENGINE = InnoDB COMMENT = '成绩单签名（只增不改）';
```

签名本身不必进库（文件里带着），但**哈希和流水号要进库**：换钥、吊销、重复签发时能追溯。

### 2.4 接口与页面

| 接口 | 说明 |
|---|---|
| `GET /student/transcript/signed` | 登录学生下载：返回 `application/json`（内容 + `signature` + `publicKey`/`certChain`） |
| `GET /verify` | 公开验证页（免登录），粘贴 JSON 或上传文件 |
| `POST /verify` | 返回 `通过` / `不通过`，不返回细节（防探测） |

免登录白名单要改 `WebMvcConfig.addInterceptors()` 里 `excludePathPatterns`，
把 `"/verify", "/verify/**"` 加进去（和 `/login` 同级）。验证页还要限流（同一 IP 每分钟 N 次）。

### 2.5 密钥管理（最容易翻车的一环）

- **专钥专用**：不能复用 `keystore/university.p12`。那张是 HTTPS 证书（`CN=localhost`），
  私钥用途是 TLS，签名证书的 KeyUsage 必须是 `digitalSignature`；
- **私钥不进 Git、不进配置、不进日志**：开发用 `target/` 下的临时 PKCS12（本仓库已 gitignore），
  生产放 KMS/HSM，或至少加密的 keystore + 环境变量口令；
- **轮换与吊销**：证书有效期一到，旧签名仍可用旧公钥验证——所以要把 `cert_serial` 存进表，
  并保留历史公钥；吊销走 CRL/OCSP 或"验证页内置吊销列表"；
- **对外可信**：校内自签够用；要发给用人单位，验证页要展示证书指纹并提供下载，或接入学校 CA / 第三方时间戳。

### 2.6 验证页的安全细节

- 只返回"通过/不通过"，**不要**回显成绩内容（否则成了免登录的数据接口）；
- 限流 + 防枚举（`serialNo` 不要可预测自增）；
- 验证结果不要区分"签名错"和"内容被改"（防止 oracle 类探测）；
- 页面本身不需要 CSRF token（只读），但 POST 验证要校验来源。

---

## 3. 次选：审计日志哈希链 + 锚点签名

项目已有 `AuditLog`/`AuditService`（`audit_log` 只增不改）。内部人改日志是真实风险，加两列即可：

```sql
ALTER TABLE audit_log
    ADD COLUMN prev_hash  CHAR(64) NULL COMMENT '上一条的 entry_hash',
    ADD COLUMN entry_hash CHAR(64) NULL COMMENT 'SHA-256(prev_hash || 本记录规范化内容)';

CREATE TABLE IF NOT EXISTS audit_anchor (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    upto_id    BIGINT      NOT NULL COMMENT '锚定到哪条审计记录',
    chain_hash CHAR(64)    NOT NULL,
    signature  TEXT        NOT NULL COMMENT '对 chain_hash 的签名',
    created_at DATETIME    NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;
```

`AuditService.log()` 写入顺序：先算 `prev_hash = 上一条 entry_hash` → `entry_hash = SHA256(prev_hash || 规范化(记录))`
→ 落库。每日定时任务把链尾哈希签名后写入 `audit_anchor`（相当于"给整本账盖章"）。

**能证明什么**：事后改任意一条历史记录（哪怕只改 detail），重算的链和当天锚点对不上。
**不能证明什么**：如果攻击者能同时改日志表 + 所有锚点表，链可以整体重算——所以锚点要
另外存一份（对象存储 WORM、另一台机器、或 RFC 3161 时间戳），这也是 PoC 第 6 段想表达的边界。

---

## 4. 不该用的地方（四条边界）

| 想法 | 为什么不对 | 该用什么 |
|---|---|---|
| 学生给作业签名 | 学生没有可信密钥，也没有证书，签了也无法证明"是他" | 登录态 + 上传时间 + 服务端哈希归档 |
| 服务间接口用非对称签名 | 双方都可信时非对称是浪费（慢、要管密钥） | HMAC + 时间戳 + nonce |
| 用 TLS 证书签成绩单 | KeyUsage 是 TLS，审计上不合规；换证书会牵连 HTTPS | 另发签名证书 |
| 觉得"签名了成绩单就保密了" | 签名不加密，JSON 仍是明文 | 需要保密就另做加密（AES-GCM + 信封） |

---

## 5. 算法与 JDK 兼容（项目现状：pom `java.version=1.8`，运行时 17）

| 算法 | 最低 JDK | 签名长度 | 建议 |
|---|---|---|---|
| `SHA256withRSA`（PKCS#1 v1.5） | 8 | 256 字节 | 保底，兼容性最好 |
| `RSASSA-PSS` | 11 | 256 字节 | 运行 JDK ≥ 11 时首选 |
| `SHA256withECDSA`（P-256） | 7 | 70~72 字节 | 证书体系支持时可选 |
| `Ed25519` | 15 | 64 字节 | 新项目推荐；本项目需确认部署 JDK |

PoC 做了自动回退：先试 `RSASSA-PSS`，JDK 8 抛 `NoSuchAlgorithmException` 就退回 `SHA256withRSA`，
验证端同样处理（实跑输出里打印了实际用的算法）。

---

## 6. 工作量（按本仓库现状估）

| 项 | 人日 |
|---|---|
| PoC（已完成） | 0.5 |
| 成绩单签名：`TranscriptSignatureService` + 表 + 接口 | 1.5 |
| `/verify` 公开页 + 白名单 + 限流 | 1 |
| 密钥管理（KMS/加密 keystore + 轮换记录） | 1 |
| 审计链：迁移 + `AuditService` 改造 + 锚点定时任务 | 1 |
| 测试：篡改/错钥/重放/回归 | 1 |

课程设计只做"成绩单签名 + 验证页"约 **3 天**；两项都做约 **5~6 天**。

---

## 7. PoC 实跑（`docs/university-signature-raw.txt` 节选）

```text
【1】签名密钥：专钥专用，不能复用 TLS 证书
证书主题: CN=University Demo Transcript Signer,...
公钥算法: RSA 2048 位
用途扩展 KeyUsage: [true, true, false, false, false, false, false, false, false]
指纹(SHA-256): 3010efb7cb1a51b71dcbc194580f1dea...

【2】被签的内容：成绩单 + 用途 + 流水号
SHA-256: 4a7f6408ce05b5f740ff22e9ec103f998fb942ccfcfac395a3e9f8beacef7576

【3】学校用私钥签名
算法: RSASSA-PSS + SHA-256
签名长度: 256 字节

【4】谁都能验：文件 + 签名 + 学校公钥
  [通过] 原始成绩单 + 签名 -> 通过
  [通过] 把 GPA 3.37 改成 4.00 -> 拒绝
  [通过] 把姓名换成别人 -> 拒绝
  [通过] 改签名中的 1 个 bit -> 拒绝

【5】换成 HTTPS 证书的公钥：验不过
  [通过] 用 TLS 证书的公钥验证成绩单签名 -> 拒绝

【6】审计日志防篡改：链式哈希 + 链尾签名
  链尾签名（每天/每次批量后签一次，作为「锚点」）: ...
  [通过] 原链的锚点验签通过
  [通过] 从头重算哈希链，与锚点一致
  篡改第 2 条后重算: 90846f727b6f5d64...（与原锚点不一致）
  [通过] 改任意一条历史记录 -> 重算哈希链对不上锚点
  [通过] 篡改后的链过不了锚点验签

自检：通过 9 项，失败 0 项
```

---

## 8. 验收清单

- [ ] 签名证书 KeyUsage 含 `digitalSignature`，且与 HTTPS 证书分开；
- [ ] 私钥不在仓库/配置/日志里，口令走环境变量或 KMS；
- [ ] 被签内容包含 type/version、学生标识、学期、流水号、签发时间；
- [ ] 序列化确定性（同样内容每次签出的哈希一致）；
- [ ] 篡改分数/姓名/签名都验不过（PoC 已验证）；
- [ ] `/verify` 免登录、限流、只返回通过/不通过；
- [ ] 旧证书的公钥保留，能验证历史签名；新证书带 `cert_serial` 可追溯；
- [ ] 审计链每日锚点另存（不只放同一张库）。

---

## 9. 快速跑一遍

```powershell
# 生成签名专用证书 + 编译 + 跑 PoC（-ForceNewKey 可重生成密钥）
pwsh -File scripts\run-university-signature-demo.ps1
pwsh -File scripts\run-university-signature-demo.ps1 -ForceNewKey
```
