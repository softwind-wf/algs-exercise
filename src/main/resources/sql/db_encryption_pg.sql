-- ============================================================
-- 数据库中的加密（二）：PostgreSQL + pgcrypto 侧演示
--
--   ① 明文入库（对照组）
--   ② pgp_sym_encrypt：随机 salt/IV + 完整性校验（同值不同密文）
--   ③ 口令错 / 密文被改：pgcrypto 直接报 "Wrong key or corrupt data"
--   ④ 盲索引（HMAC-SHA256）：不存明文也能做等值查询，且能走索引
--   ⑤ 密码不要"加密"，要"哈希"：crypt() + bcrypt（gen_salt('bf')）
--   ⑥ 传输层：这条连接有没有 SSL
--
-- 数据与 MySQL 演示一致：5 个学生，S001 / S003 手机号相同。
-- 前置：本机 PostgreSQL 17 已启动；连接口令见 src/main/resources/pg.properties。
-- 复现：.\scripts\run-db-encryption-demo.ps1
-- ============================================================

\set ON_ERROR_STOP on
\pset pager off

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP TABLE IF EXISTS student_plain;
DROP TABLE IF EXISTS student_enc;
DROP TABLE IF EXISTS app_user;

-- ============================================================
-- ① 明文入库：对照组
-- ============================================================
SELECT '===== ① 明文入库：psql 一条 SELECT 就能看到手机号 =====' AS section;

CREATE TABLE student_plain (
  sid   varchar(8)  PRIMARY KEY,
  name  varchar(20) NOT NULL,
  phone varchar(20) NOT NULL
);
CREATE INDEX idx_student_plain_phone ON student_plain (phone);

INSERT INTO student_plain (sid, name, phone) VALUES
  ('S001', '张伟', '13900001111'),
  ('S002', '李娜', '13800138000'),
  ('S003', '王强', '13900001111'),   -- 与 S001 同号
  ('S004', '刘洋', '13700137000'),
  ('S005', '陈静', '13600136000');

SELECT * FROM student_plain ORDER BY sid;
EXPLAIN (COSTS OFF) SELECT sid FROM student_plain WHERE phone = '13900001111';

-- ============================================================
-- ② pgp_sym_encrypt：OpenPGP 对称加密，随机 salt + 随机 IV
--
--    每次调用都会生成新的随机 salt / session key / IV，所以
--    "同一明文加密两次" 得到的密文不同；输出里还带 MDC 完整性校验。
--    密文长度 = 11 字节明文 → 77 字节（salt、算法参数、IV、
--    session key、MDC 都算进去了）。
-- ============================================================
SELECT '===== ② pgp_sym_encrypt：同值不同密文（随机 salt/IV） =====' AS section;

CREATE TABLE student_enc (
  sid        varchar(8)  PRIMARY KEY,
  name       varchar(20) NOT NULL,
  phone_enc  bytea       NOT NULL,
  phone_bidx bytea       NOT NULL    -- 盲索引：HMAC-SHA256(手机号)
);

INSERT INTO student_enc (sid, name, phone_enc, phone_bidx)
SELECT sid,
       name,
       pgp_sym_encrypt(phone, 'demo-passphrase'),
       hmac(phone, 'blind-index-key', 'sha256')
FROM student_plain;

SELECT sid, name,
       length(phone_enc) AS ct_bytes,
       encode(phone_enc, 'hex')  AS ct_hex,
       encode(phone_bidx, 'hex') AS bidx_hex
FROM student_enc
ORDER BY sid;

-- 同一明文加密两次：密文不同（随机 salt/IV 的直接结果）
SELECT (pgp_sym_encrypt('13900001111', 'demo-passphrase') =
        pgp_sym_encrypt('13900001111', 'demo-passphrase')) AS same_ciphertext,
       length(pgp_sym_encrypt('13900001111', 'demo-passphrase')) AS ct_bytes;

-- 加密是可逆的：有口令就能还原
SELECT sid, name,
       pgp_sym_decrypt(phone_enc, 'demo-passphrase') AS phone
FROM student_enc
ORDER BY sid;

-- ============================================================
-- ③ 口令错 / 密文被改：完整性校验会失败
--
--    pgp_sym_decrypt 解出来先校验 MDC 摘要，不对就报错，而不是
--    悄悄返回一段垃圾明文（这一点比裸 CBC/ECB 强）。
-- ============================================================
SELECT '===== ③ 错误口令 / 篡改密文：pgcrypto 报错而不是返回垃圾 =====' AS section;

DO $$
BEGIN
  PERFORM pgp_sym_decrypt(
            pgp_sym_encrypt('13900001111', 'demo-passphrase'),
            'wrong-passphrase');
  RAISE NOTICE '不应执行到这里（错误口令居然解开了）';
EXCEPTION WHEN others THEN
  RAISE NOTICE '错误口令 -> %', SQLERRM;
END $$;

DO $$
DECLARE
  ct bytea := pgp_sym_encrypt('13900001111', 'demo-passphrase');
BEGIN
  -- 把密文中间某个字节改掉（偏移 50 落在数据区内）
  PERFORM pgp_sym_decrypt(set_byte(ct, 50, 0), 'demo-passphrase');
  RAISE NOTICE '不应执行到这里（篡改后居然解开了）';
EXCEPTION WHEN others THEN
  RAISE NOTICE '密文被篡改 -> %', SQLERRM;
END $$;

-- ============================================================
-- ④ 盲索引：加密了但还能按等值查询
--
--    思路：存一列 HMAC(手机号) 的摘要，查询时算同一个 HMAC 去匹配。
--    · 只能做"等值"，做不了范围/前缀/排序（摘要没有顺序语义）
--    · 同值摘要相同 → 仍然泄漏"哪些行同值"（频率分析），只是不泄漏明文
--    · 摘要列可以建普通 B-Tree 索引，性能与明文索引接近
-- ============================================================
SELECT '===== ④ 盲索引：WHERE phone_bidx = hmac(...) 走索引 =====' AS section;

CREATE INDEX idx_student_enc_bidx ON student_enc (phone_bidx);

-- 5 行小表默认会走顺序扫描；关掉 seqscan 看索引路径
SET enable_seqscan = off;

EXPLAIN (COSTS OFF)
SELECT sid FROM student_enc
WHERE phone_bidx = hmac('13900001111', 'blind-index-key', 'sha256');

SELECT sid FROM student_enc
WHERE phone_bidx = hmac('13900001111', 'blind-index-key', 'sha256')
ORDER BY sid;

SET enable_seqscan = on;

-- 对照：没有盲索引，只能把每一行解密出来再比 → 全表扫描 + 解密
EXPLAIN (COSTS OFF)
SELECT sid FROM student_enc
WHERE pgp_sym_decrypt(phone_enc, 'demo-passphrase') = '13900001111';

SELECT sid FROM student_enc
WHERE pgp_sym_decrypt(phone_enc, 'demo-passphrase') = '13900001111'
ORDER BY sid;

-- 盲索引的泄漏面：同值的行摘要相同
SELECT encode(phone_bidx, 'hex') AS bidx,
       count(*)                  AS rows_cnt,
       string_agg(sid, ',' ORDER BY sid) AS sids
FROM student_enc
GROUP BY phone_bidx
HAVING count(*) > 1;

-- ============================================================
-- ⑤ 密码不是"加密"，是"哈希"
--
--    bcrypt 每次用随机 salt 生成不同摘要，且故意慢（cost=10 约几十毫秒），
--    只能"验算"不能"解密"；数据库/备份泄露也拿不回原密码。
--    （加密是双向的：有密钥就能还原，绝不该用在口令上。）
-- ============================================================
SELECT '===== ⑤ 口令哈希：bcrypt(crypt + gen_salt(''bf'')) =====' AS section;

CREATE TABLE app_user (
  username varchar(32) PRIMARY KEY,
  pwd_hash text        NOT NULL
);

INSERT INTO app_user (username, pwd_hash) VALUES
  ('alice', crypt('alice123', gen_salt('bf', 10))),
  ('bob',   crypt('alice123', gen_salt('bf', 10)));   -- 同一个口令

SELECT username, pwd_hash FROM app_user ORDER BY username;

SELECT username,
       crypt('alice123', pwd_hash) = pwd_hash AS password_ok
FROM app_user
ORDER BY username;

SELECT username,
       crypt('wrong-pass', pwd_hash) = pwd_hash AS wrong_password_ok
FROM app_user
ORDER BY username;

-- ============================================================
-- ⑥ 传输层：本机连接有没有 SSL
--
--    PostgreSQL 默认 ssl=off，本机 TCP 连接就是明文的；
--    远程部署应在 postgresql.conf 配 ssl=on + 证书，客户端 sslmode=require。
-- ============================================================
SELECT '===== ⑥ 传输层：本机这条连接是否 SSL =====' AS section;

SHOW ssl;

SELECT ssl, version, cipher
FROM pg_stat_ssl
WHERE pid = pg_backend_pid();

-- ============================================================
-- 小结（pgcrypto 这条路）
--   · 能用：对称加密（随机 salt/IV + 完整性校验）、HMAC 盲索引、bcrypt
--   · 不能：范围/前缀/排序查询、密钥轮换、真正 AEAD 的字段级方案
--   · 密钥/口令仍必须由应用管理；数据库只负责"存字节"
-- ============================================================
