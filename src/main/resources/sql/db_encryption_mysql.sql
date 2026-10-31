-- ============================================================
-- 数据库中的加密（一）：MySQL 侧演示
--
--   ① 明文入库：DBA / 备份 / 应用日志都能直接看到手机号
--   ② 数据库内置函数 AES_ENCRYPT / AES_DECRYPT（默认 aes-128-ecb）
--   ③ ECB 的硬伤：相同 16 字节明文块 → 相同密文块
--   ④ 密钥写成 SQL 字面量：数据库自己会把语句文本记进 performance_schema
--   ⑤ 切到 CBC + 每行随机 IV：同值不同密文，等值查询只能全表解密
--   ⑥ 传输层：这条连接到底有没有加密
--
-- 数据：5 个学生，其中 S001 / S003 手机号相同（故意制造"同值"，
--       用来看泄漏：同值同密文 = 别人能看出"这两个人号码一样"）。
--
-- 前置：本机 MySQL 8.0+ 已启动；连接口令见 src/main/resources/db.properties。
-- 复现：.\scripts\run-db-encryption-demo.ps1
--
-- 注意：只建独立库 db_encryption_demo、只删本演示自己的表，
--       不碰 university 等既有库（DROP DATABASE 是 2026-10-25 事故的元凶）。
-- ============================================================

CREATE DATABASE IF NOT EXISTS db_encryption_demo
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE db_encryption_demo;

DROP TABLE IF EXISTS student_plain;
DROP TABLE IF EXISTS student_det;
DROP TABLE IF EXISTS student_cbc;

-- ============================================================
-- ① 明文入库：数据在库里长什么样
-- ============================================================
SELECT '===== ① 明文入库：DBA 一条 SELECT 就能看到手机号 =====' AS section;

CREATE TABLE student_plain (
  sid   VARCHAR(8)  PRIMARY KEY,
  name  VARCHAR(20) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  KEY idx_phone (phone)
) ENGINE = InnoDB;

INSERT INTO student_plain (sid, name, phone) VALUES
  ('S001', '张伟', '13900001111'),
  ('S002', '李娜', '13800138000'),
  ('S003', '王强', '13900001111'),   -- 与 S001 同号
  ('S004', '刘洋', '13700137000'),
  ('S005', '陈静', '13600136000');

SELECT * FROM student_plain ORDER BY sid;

-- 索引直接建在明文列上：等值、范围、前缀、排序、GROUP BY 全部可用。
EXPLAIN SELECT sid FROM student_plain WHERE phone = '13900001111';

-- ============================================================
-- ② 数据库内置函数加密（默认 aes-128-ecb）
--
--    AES_ENCRYPT(str, key) 不指定模式时走会话变量 block_encryption_mode，
--    MySQL 8.0 默认值是 aes-128-ecb：128 位密钥、ECB 模式、无 IV。
-- ============================================================
SELECT '===== ② 内置函数加密：手机号列变成 16 字节密文 =====' AS section;

SET @k16 = 'demo-key-16byte';   -- 16 字节 = 128 位

CREATE TABLE student_det (
  sid       VARCHAR(8)   PRIMARY KEY,
  name      VARCHAR(20)  NOT NULL,
  phone_enc VARBINARY(32) NOT NULL,
  KEY idx_phone_enc (phone_enc)
) ENGINE = InnoDB;

INSERT INTO student_det (sid, name, phone_enc)
SELECT sid, name, AES_ENCRYPT(phone, @k16)
FROM student_plain;

SELECT sid, name,
       LENGTH(phone_enc) AS ct_bytes,
       HEX(phone_enc)    AS ct_hex
FROM student_det
ORDER BY sid;

-- 解密：有密钥的人在 SQL 里就能还原
SELECT sid, name,
       CONVERT(AES_DECRYPT(phone_enc, @k16) USING utf8mb4) AS phone
FROM student_det
ORDER BY sid;

-- 致命点 1：同一个明文 → 同一个密文（确定性加密）
SELECT HEX(phone_enc) AS ct_hex,
       COUNT(*)       AS rows_cnt,
       GROUP_CONCAT(sid ORDER BY sid) AS sids
FROM student_det
GROUP BY phone_enc
HAVING COUNT(*) > 1;

-- 致命点 2：密文仍然"可索引"，于是等值查询、频次统计照样能做——
-- 攻击者不需要解密，靠统计就能知道哪些人号码相同。
SELECT '-- 等值查询：密文列上仍可走索引（同值同密文）' AS note;

EXPLAIN SELECT sid FROM student_det
WHERE phone_enc = AES_ENCRYPT('13900001111', @k16);

SELECT sid FROM student_det
WHERE phone_enc = AES_ENCRYPT('13900001111', @k16)
ORDER BY sid;

-- ============================================================
-- ③ ECB 的硬伤：块与块之间独立加密
--
--    ECB: C_i = E_K(P_i)，没有链式反馈。明文里出现两个相同的
--    16 字节块，密文里就出现两个相同的 16 字节块。
--    （32 字节明文 = 2 块；PKCS#7 补一整块 → 48 字节密文）
-- ============================================================
SELECT '===== ③ ECB：32 字节明文 = 两块相同，密文两块也相同 =====' AS section;

SET @p32 = CONCAT(REPEAT('A', 16), REPEAT('A', 16));
SET @c32 = AES_ENCRYPT(@p32, @k16);

SELECT LENGTH(@p32) AS pt_bytes,
       LENGTH(@c32) AS ct_bytes,
       SUBSTRING(HEX(@c32), 1,  32) AS block1,
       SUBSTRING(HEX(@c32), 33, 32) AS block2,
       (SUBSTRING(HEX(@c32), 1, 32) = SUBSTRING(HEX(@c32), 33, 32)) AS same_block;

-- 换个更贴近业务的例子：两段相同的 16 字节"证件号前缀+号码"拼接，
-- 只要对齐到 16 字节边界，重复的块在密文里同样一眼可见。
-- 结论：ECB 下密文保留了明文的"块级模式"，能加密但不算安全存储。

-- 补充：默认 ECB 下就算传了 IV 也会被忽略（Warning 1618）
SELECT '-- 默认 ECB 下 IV 参数被忽略：' AS note;
SET @iv0 = RANDOM_BYTES(16);
SELECT HEX(AES_ENCRYPT('13900001111', @k16, @iv0)) AS ct_with_iv;
SHOW WARNINGS;

-- ============================================================
-- ④ 密钥写成 SQL 字面量：数据库自己会记录语句文本
--
--    只要密钥以字面量出现在 SQL 里，它就会出现在：
--      performance_schema.events_statements_history / general_log /
--      slow log（长查询）/ 审计日志 / 某些驱动的日志里。
--    提示：把密钥放应用配置 / KMS，SQL 里只传参数绑定。
-- ============================================================
SELECT '===== ④ 密钥字面量会留在数据库的语句历史里 =====' AS section;

SELECT LENGTH(AES_ENCRYPT('13900001111', 'demo-key-16byte')) AS ct_bytes_with_literal_key;

SET @needle = CONCAT('demo-key', '-16byte');   -- 避免查询自身命中
SELECT SQL_TEXT
FROM performance_schema.events_statements_history
WHERE THREAD_ID = PS_CURRENT_THREAD_ID()
  AND SQL_TEXT LIKE CONCAT('%', @needle, '%')
ORDER BY TIMER_START DESC
LIMIT 3;

-- ============================================================
-- ⑤ 换 CBC + 每行随机 IV：同值不同密文
--
--    CBC: C_i = E_K(P_i XOR C_{i-1})，C_0 = IV。
--    每次插入随机生成 16 字节 IV 并存成一列；解密时必须拿回同一个 IV。
--    MySQL 内置 AES 没有 GCM/AEAD，这里只是"模式对了"，完整性仍无保护
--    （CBC 可被按位翻转 / 填充预言攻击，生产请用应用层 AES-GCM）。
-- ============================================================
SELECT '===== ⑤ CBC + 随机 IV：同一手机号，密文每次不同 =====' AS section;

SET SESSION block_encryption_mode = 'aes-256-cbc';   -- CBC 需要 32 字节密钥
SET @k32 = REPEAT('K', 32);

CREATE TABLE student_cbc (
  sid       VARCHAR(8)   PRIMARY KEY,
  name      VARCHAR(20)  NOT NULL,
  phone_iv  VARBINARY(16) NOT NULL,
  phone_enc VARBINARY(64) NOT NULL
) ENGINE = InnoDB;

-- 每一行：先生成随机 IV，再加密 (明文)，IV 与密文一起入库
SET @iv = RANDOM_BYTES(16);
INSERT INTO student_cbc VALUES ('S001', '张伟', @iv, AES_ENCRYPT('13900001111', @k32, @iv));
SET @iv = RANDOM_BYTES(16);
INSERT INTO student_cbc VALUES ('S002', '李娜', @iv, AES_ENCRYPT('13800138000', @k32, @iv));
SET @iv = RANDOM_BYTES(16);
INSERT INTO student_cbc VALUES ('S003', '王强', @iv, AES_ENCRYPT('13900001111', @k32, @iv));
SET @iv = RANDOM_BYTES(16);
INSERT INTO student_cbc VALUES ('S004', '刘洋', @iv, AES_ENCRYPT('13700137000', @k32, @iv));
SET @iv = RANDOM_BYTES(16);
INSERT INTO student_cbc VALUES ('S005', '陈静', @iv, AES_ENCRYPT('13600136000', @k32, @iv));

SELECT sid, name,
       HEX(phone_iv)  AS iv_hex,
       HEX(phone_enc) AS ct_hex,
       LENGTH(phone_enc) AS ct_bytes
FROM student_cbc
ORDER BY sid;

-- S001 与 S003 号码相同，但两次加密的 IV 不同 → 密文不同
SELECT '-- S001 / S003 同号，密文是否相同：' AS note;
SELECT sid, HEX(phone_enc) AS ct_hex
FROM student_cbc
WHERE sid IN ('S001', 'S003')
ORDER BY sid;

-- 解密：必须把每行的 IV 一起取回来
SELECT sid, name,
       CONVERT(AES_DECRYPT(phone_enc, @k32, phone_iv) USING utf8mb4) AS phone
FROM student_cbc
ORDER BY sid;

-- 代价：等值查询没有可用索引了，只能对每一行做一次解密再比较
SELECT '-- 等值查询：只能全表扫描 + 逐行解密（EXPLAIN type=ALL）' AS note;
EXPLAIN SELECT sid FROM student_cbc
WHERE AES_DECRYPT(phone_enc, @k32, phone_iv) = '13900001111';

SELECT sid FROM student_cbc
WHERE AES_DECRYPT(phone_enc, @k32, phone_iv) = '13900001111'
ORDER BY sid;

-- ============================================================
-- ⑥ 传输层：这条连接有没有加密
--
--    MySQL 8.0 默认启用 TLS（have_ssl=YES），客户端默认 ssl-mode=PREFERRED，
--    走 TCP 时通常会自动协商出 TLS。想看"明文连接"的样子，用客户端关掉：
--      mysql --ssl-mode=DISABLED -e "SHOW SESSION STATUS LIKE 'Ssl_cipher'"
--    此时 Value 为空。
-- ============================================================
SELECT '===== ⑥ 传输层：本机这条 TCP 连接已经是 TLS =====' AS section;

SHOW SESSION STATUS LIKE 'Ssl_cipher';
SHOW GLOBAL VARIABLES LIKE 'have_ssl';

-- ============================================================
-- 小结（MySQL 内置函数这条路）
--   · 能用：AES_ENCRYPT / AES_DECRYPT 加解密、可选 CBC + IV
--   · 不能：没有 AEAD（无完整性校验）、密钥只能拼进 SQL 或变量、
--           没有密钥轮换/信封加密、没有"可搜索加密"原语
--   · 所以字段级加密更适合放在应用层：Java 演示见
--     src/main/java/com/ds/db/DbEncryptionDemo.java（AES-GCM + 信封 + 盲索引）
-- ============================================================
