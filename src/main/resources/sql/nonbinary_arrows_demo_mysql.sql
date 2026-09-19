-- ============================================================
-- 非二元联系集里的“箭头”与“两种可能的解释” —— 动手验证版
-- 数据库: MySQL 8.0(默认库由 -db 指定)
-- 教材: 《数据库系统概念》原书第6版·本科教学版(机械工业出版社,
--       Silberschatz / Korth / Sudarshan)
--       第7章 数据库设计与 E-R 模型 · 7.5.5 非二元联系集 · 图 7-13
-- 姊妹脚本: nonbinary_arrows_demo.sql(PostgreSQL 17 等价版)
-- ------------------------------------------------------------
-- 本脚本用同一组数据分别往三张表里插，看哪些行被接受、哪些被拒绝：
--   表 nb_my_single —— 单箭头(图 7-13 的画法)：UNIQUE(sid, pid)
--   表 nb_my_read1  —— 双箭头 · 读法Ⅰ(把箭头端当成一个整体)：UNIQUE(iid, pid)
--   表 nb_my_read2  —— 双箭头 · 读法Ⅱ(每个箭头端逐个看)：
--                      UNIQUE(sid, pid) + UNIQUE(sid, iid)
-- 三张表都只有 (sid, pid, iid) 三列，区别全在唯一约束上。
--
-- 与 PostgreSQL 版的差异：
--   SELECT ... EXCEPT SELECT ...  →  NOT EXISTS 子查询
--
-- 运行:
--   .\sql.bat -db <库名> -f src\main\resources\sql\nonbinary_arrows_demo_mysql.sql
--
-- 实测结论(MySQL 8.0.44): 见文件末尾。
-- ============================================================


-- ============================================================
-- 步骤 0：清理(可重复运行)
-- ============================================================
DROP TABLE IF EXISTS nb_my_single;
DROP TABLE IF EXISTS nb_my_read1;
DROP TABLE IF EXISTS nb_my_read2;

SELECT '步骤 0 完成：旧表已清理' AS section;


-- ============================================================
-- 步骤 1：建三张表，唯一约束就是“读法”本身
--   一行 = “某学生 在 某项目 上 由 某教师 指导”。
-- ============================================================
CREATE TABLE nb_my_single (
    sid  VARCHAR(5) NOT NULL,
    pid  VARCHAR(5) NOT NULL,
    iid  VARCHAR(5) NOT NULL,
    CONSTRAINT uk_nb_my_single UNIQUE (sid, pid)      -- 单箭头：每个(学生,项目)至多一个导师
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE nb_my_read1 (
    sid  VARCHAR(5) NOT NULL,
    pid  VARCHAR(5) NOT NULL,
    iid  VARCHAR(5) NOT NULL,
    CONSTRAINT uk_nb_my_read1 UNIQUE (iid, pid)       -- 读法Ⅰ(整体)：每个(导师,项目)至多一个学生
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE nb_my_read2 (
    sid  VARCHAR(5) NOT NULL,
    pid  VARCHAR(5) NOT NULL,
    iid  VARCHAR(5) NOT NULL,
    CONSTRAINT uk_nb_my_read2_a UNIQUE (sid, pid),    -- 读法Ⅱ(逐个)之一
    CONSTRAINT uk_nb_my_read2_b UNIQUE (sid, iid)     -- 读法Ⅱ(逐个)之二
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

SELECT '步骤 1 完成：三张表建好(单箭头 / 读法Ⅰ / 读法Ⅱ)' AS section;


-- ============================================================
-- 步骤 2：A 类数据 —— 三种约束都允许
-- ============================================================
INSERT INTO nb_my_single (sid, pid, iid) VALUES ('s1', 'p1', 'i1');
INSERT INTO nb_my_single (sid, pid, iid) VALUES ('s1', 'p2', 'i2');

INSERT INTO nb_my_read1 (sid, pid, iid) VALUES ('s1', 'p1', 'i1');
INSERT INTO nb_my_read1 (sid, pid, iid) VALUES ('s1', 'p2', 'i2');

INSERT INTO nb_my_read2 (sid, pid, iid) VALUES ('s1', 'p1', 'i1');
INSERT INTO nb_my_read2 (sid, pid, iid) VALUES ('s1', 'p2', 'i2');

SELECT '步骤 2 完成：A 类两行已插入三张表(都应成功)' AS section;


-- ============================================================
-- 步骤 3：B 类数据 (s1,p1,i2) —— 一个学生在同一个项目上有两位导师
--   单箭头表：应被拒绝 / 读法Ⅰ：允许 / 读法Ⅱ：应被拒绝
-- ============================================================
SELECT 'B 类数据 (s1,p1,i2)：学生 s1 在项目 p1 上有第二位导师 i2' AS section;

INSERT INTO nb_my_single (sid, pid, iid) VALUES ('s1', 'p1', 'i2');   -- 预期失败
INSERT INTO nb_my_read1  (sid, pid, iid) VALUES ('s1', 'p1', 'i2');   -- 预期成功
INSERT INTO nb_my_read2  (sid, pid, iid) VALUES ('s1', 'p1', 'i2');   -- 预期失败

SELECT '步骤 3 完成：单箭头拒绝、读法Ⅰ接受、读法Ⅱ拒绝' AS section;


-- ============================================================
-- 步骤 4：C 类数据 (s2,p1,i1) —— 同一个项目上同一位导师带第二个学生
--   单箭头表：允许 / 读法Ⅰ：应被拒绝 / 读法Ⅱ：允许
-- ============================================================
SELECT 'C 类数据 (s2,p1,i1)：导师 i1 在项目 p1 上带第二位学生 s2' AS section;

INSERT INTO nb_my_single (sid, pid, iid) VALUES ('s2', 'p1', 'i1');   -- 预期成功
INSERT INTO nb_my_read1  (sid, pid, iid) VALUES ('s2', 'p1', 'i1');   -- 预期失败
INSERT INTO nb_my_read2  (sid, pid, iid) VALUES ('s2', 'p1', 'i1');   -- 预期成功

SELECT '步骤 4 完成：单箭头接受、读法Ⅰ拒绝、读法Ⅱ接受' AS section;


-- ============================================================
-- 步骤 5：看结果 —— 两张双箭头表的行数一样，装的内容却不同
-- ============================================================
SELECT '单箭头表 nb_my_single(UNIQUE(sid,pid)) 的行' AS section;
SELECT sid, pid, iid FROM nb_my_single ORDER BY sid, pid, iid;

SELECT '读法Ⅰ表 nb_my_read1(UNIQUE(iid,pid)) 的行' AS section;
SELECT sid, pid, iid FROM nb_my_read1 ORDER BY sid, pid, iid;

SELECT '读法Ⅱ表 nb_my_read2(UNIQUE(sid,pid)+UNIQUE(sid,iid)) 的行' AS section;
SELECT sid, pid, iid FROM nb_my_read2 ORDER BY sid, pid, iid;

SELECT '读法Ⅰ接受、读法Ⅱ拒绝的那一行(差异证据)' AS section;

SELECT r1.sid, r1.pid, r1.iid
FROM nb_my_read1 r1
WHERE NOT EXISTS (SELECT 1 FROM nb_my_read2 r2
                   WHERE r2.sid = r1.sid AND r2.pid = r1.pid AND r2.iid = r1.iid);

SELECT '读法Ⅱ接受、读法Ⅰ拒绝的那一行(反向差异证据)' AS section;

SELECT r2.sid, r2.pid, r2.iid
FROM nb_my_read2 r2
WHERE NOT EXISTS (SELECT 1 FROM nb_my_read1 r1
                   WHERE r1.sid = r2.sid AND r1.pid = r2.pid AND r1.iid = r2.iid);

SELECT '单箭头表与读法Ⅱ表的内容是否相同' AS section,
       (SELECT COUNT(*) FROM nb_my_single s
         WHERE NOT EXISTS (SELECT 1 FROM nb_my_read2 r
                            WHERE r.sid = s.sid AND r.pid = s.pid AND r.iid = s.iid)) AS 只在单箭头表里的行数;


-- ============================================================
-- 步骤 6：清理(示例表默认保留，想清干净手工删这三张表)
-- ============================================================
SELECT '脚本执行完毕：三张表留库，可反复查看对比' AS section;


-- ============================================================
-- 实测结论(MySQL 8.0.44, 临时库 nbdemo_check, 无非预期报错 ——
--   3 处 Duplicate entry 都是刻意演示的拒绝)：
--   ============ 单箭头表 nb_my_single(UNIQUE(sid,pid)) ============
--   A1 成功  A2 成功  B 失败(Duplicate entry 's1-p1')  C 成功 → 共 3 行
--   ============ 读法Ⅰ表 nb_my_read1(UNIQUE(iid,pid)) ============
--   A1 成功  A2 成功  B 成功  C 失败(Duplicate entry 'i1-p1') → 共 3 行
--   ============ 读法Ⅱ表 nb_my_read2 ============
--   A1 成功  A2 成功  B 失败  C 成功 → 共 3 行
--   结论：读法Ⅰ表 = A1,A2,B；读法Ⅱ表 = A1,A2,C；单箭头表 = A1,A2,C
--   (与读法Ⅱ表一致)。两种读法各行数相同但内容互不包含，
--   说明“两个箭头”的图确实有歧义；一个箭头时两种读法落到同一张表。
-- ============================================================
