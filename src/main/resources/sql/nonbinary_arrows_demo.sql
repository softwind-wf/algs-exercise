-- ============================================================
-- 非二元联系集里的“箭头”与“两种可能的解释” —— 动手验证版
-- 数据库: university (PostgreSQL 17)
-- 教材: 《数据库系统概念》原书第6版·本科教学版(机械工业出版社,
--       Silberschatz / Korth / Sudarshan)
--       第7章 数据库设计与 E-R 模型 · 7.5.5 非二元联系集 · 图 7-13
-- 姊妹脚本: nonbinary_arrows_demo_mysql.sql(MySQL 8.0 等价版)
-- ------------------------------------------------------------
-- 这一节在讲什么：
--   1. 三元(非二元)联系集里，箭头表示“至多一个”，也就是多对一里的“一”。
--      图 7-13 的例子：假设一个 student 在每个项目上最多只能有一位导师，
--      这个约束就画成“从 proj_guide 的边指向 instructor 的箭头”。
--      于是“给定 (student, project) → 至多一个 instructor”，
--      proj_guide 的主码 = (student.ID, project.ID)，也就是去掉箭头端的主码。
--
--   2. 如果一个非二元联系集上画了“两个或更多”箭头，同一张图会有两种读法：
--      读法Ⅰ（整体看）：把箭头端的几个实体集当成一个整体，
--                        其余各实体集的一个组合 → 至多一个箭头端的组合。
--      读法Ⅱ（逐个看）：每个箭头端实体集分别被其余所有实体集唯一决定，
--                        于是“除它以外的所有实体集”各自构成候选码。
--      教材的结论：这两种读法在不同书/系统里都用，为避免混淆，
--                  一个联系集外最多画一个箭头；只有一个箭头时两者等价
--                  (只有一个箭头端，“整体”与“逐个”是同一句话)。
--
-- 本脚本用同一组数据分别往三张表里插，看哪些行被接受、哪些被拒绝：
--   表 nb_pg_single —— 单箭头：UNIQUE(sid, pid)
--   表 nb_pg_read1  —— 双箭头 · 读法Ⅰ(整体)：UNIQUE(iid, pid)
--   表 nb_pg_read2  —— 双箭头 · 读法Ⅱ(逐个)：UNIQUE(sid, pid) + UNIQUE(sid, iid)
-- 三张表都只有 (sid, pid, iid) 三列，区别全在唯一约束上。
--
-- 运行:
--   .\psql.bat -f src\main\resources\sql\nonbinary_arrows_demo.sql
--
-- 实测结论(PostgreSQL 17.11): 见文件末尾。
-- ============================================================


-- ============================================================
-- 步骤 0：清理(可重复运行)
-- ============================================================
DROP TABLE IF EXISTS nb_pg_single;
DROP TABLE IF EXISTS nb_pg_read1;
DROP TABLE IF EXISTS nb_pg_read2;

SELECT '步骤 0 完成：旧表已清理' AS section;


-- ============================================================
-- 步骤 1：建三张表，唯一约束就是“读法”本身
--   proj_guide(student, project, instructor) 的三元事实：
--   一行 = “某学生 在 某项目 上 由 某教师 指导”。
-- ============================================================
CREATE TABLE nb_pg_single (
    sid  VARCHAR(5) NOT NULL,
    pid  VARCHAR(5) NOT NULL,
    iid  VARCHAR(5) NOT NULL,
    CONSTRAINT uk_nb_pg_single UNIQUE (sid, pid)      -- 单箭头：每个(学生,项目)至多一个导师
);

CREATE TABLE nb_pg_read1 (
    sid  VARCHAR(5) NOT NULL,
    pid  VARCHAR(5) NOT NULL,
    iid  VARCHAR(5) NOT NULL,
    CONSTRAINT uk_nb_pg_read1 UNIQUE (iid, pid)      -- 读法Ⅰ(整体)：每个(导师,项目)至多一个学生
);

CREATE TABLE nb_pg_read2 (
    sid  VARCHAR(5) NOT NULL,
    pid  VARCHAR(5) NOT NULL,
    iid  VARCHAR(5) NOT NULL,
    CONSTRAINT uk_nb_pg_read2_a UNIQUE (sid, pid),   -- 读法Ⅱ(逐个)之一：每个(学生,项目)至多一个导师
    CONSTRAINT uk_nb_pg_read2_b UNIQUE (sid, iid)    -- 读法Ⅱ(逐个)之二：每个(学生,导师)至多一个项目
);

SELECT '步骤 1 完成：三张表建好(单箭头 / 读法Ⅰ / 读法Ⅱ)' AS section;


-- ============================================================
-- 步骤 2：A 类数据 —— 三种约束都允许
--   (s1,p1,i1) 与 (s1,p2,i2)：同一个学生，两个项目，两位导师。
--   注意 p1 与 p2 不同、i1 与 i2 不同，所以三种读法都不冲突。
-- ============================================================
INSERT INTO nb_pg_single (sid, pid, iid) VALUES ('s1', 'p1', 'i1');
INSERT INTO nb_pg_single (sid, pid, iid) VALUES ('s1', 'p2', 'i2');

INSERT INTO nb_pg_read1 (sid, pid, iid) VALUES ('s1', 'p1', 'i1');
INSERT INTO nb_pg_read1 (sid, pid, iid) VALUES ('s1', 'p2', 'i2');

INSERT INTO nb_pg_read2 (sid, pid, iid) VALUES ('s1', 'p1', 'i1');
INSERT INTO nb_pg_read2 (sid, pid, iid) VALUES ('s1', 'p2', 'i2');

SELECT '步骤 2 完成：A 类两行已插入三张表(都应成功)' AS section;


-- ============================================================
-- 步骤 3：B 类数据 (s1,p1,i2) —— 一个学生在同一个项目上有两位导师
--   单箭头表：重复 (s1,p1) → 应被拒绝(这正是图 7-13 的约束)
--   读法Ⅰ(整体)：唯一键是 (i2,p1)，与已有 (i1,p1)、(i2,p2) 都不重复 → 允许
--   读法Ⅱ(逐个)：重复 (s1,p1) → 应被拒绝
-- ============================================================
SELECT 'B 类数据 (s1,p1,i2)：学生 s1 在项目 p1 上有第二位导师 i2' AS section;

INSERT INTO nb_pg_single (sid, pid, iid) VALUES ('s1', 'p1', 'i2');   -- 预期失败
INSERT INTO nb_pg_read1  (sid, pid, iid) VALUES ('s1', 'p1', 'i2');   -- 预期成功
INSERT INTO nb_pg_read2  (sid, pid, iid) VALUES ('s1', 'p1', 'i2');   -- 预期失败

SELECT '步骤 3 完成：单箭头拒绝、读法Ⅰ接受、读法Ⅱ拒绝' AS section;


-- ============================================================
-- 步骤 4：C 类数据 (s2,p1,i1) —— 同一个项目上同一位导师带第二个学生
--   单箭头表：唯一键 (s2,p1) 不重复 → 允许
--   读法Ⅰ(整体)：重复 (i1,p1) → 应被拒绝
--   读法Ⅱ(逐个)：唯一键 (s2,p1)、(s2,i1) 都不重复 → 允许
-- ============================================================
SELECT 'C 类数据 (s2,p1,i1)：导师 i1 在项目 p1 上带第二位学生 s2' AS section;

INSERT INTO nb_pg_single (sid, pid, iid) VALUES ('s2', 'p1', 'i1');   -- 预期成功
INSERT INTO nb_pg_read1  (sid, pid, iid) VALUES ('s2', 'p1', 'i1');   -- 预期失败
INSERT INTO nb_pg_read2  (sid, pid, iid) VALUES ('s2', 'p1', 'i1');   -- 预期成功

SELECT '步骤 4 完成：单箭头接受、读法Ⅰ拒绝、读法Ⅱ接受' AS section;


-- ============================================================
-- 步骤 5：看结果 —— 两张双箭头表的行数一样，装的内容却不同
-- ============================================================
SELECT '单箭头表 nb_pg_single(UNIQUE(sid,pid)) 的行' AS section;
SELECT sid, pid, iid FROM nb_pg_single ORDER BY sid, pid, iid;

SELECT '读法Ⅰ表 nb_pg_read1(UNIQUE(iid,pid)) 的行' AS section;
SELECT sid, pid, iid FROM nb_pg_read1 ORDER BY sid, pid, iid;

SELECT '读法Ⅱ表 nb_pg_read2(UNIQUE(sid,pid)+UNIQUE(sid,iid)) 的行' AS section;
SELECT sid, pid, iid FROM nb_pg_read2 ORDER BY sid, pid, iid;

SELECT '读法Ⅰ接受、读法Ⅱ拒绝的那一行(差异证据)' AS section;

SELECT sid, pid, iid FROM nb_pg_read1
EXCEPT
SELECT sid, pid, iid FROM nb_pg_read2;

SELECT '读法Ⅱ接受、读法Ⅰ拒绝的那一行(反向差异证据)' AS section;

SELECT sid, pid, iid FROM nb_pg_read2
EXCEPT
SELECT sid, pid, iid FROM nb_pg_read1;

SELECT '单箭头表与读法Ⅱ表的内容是否相同' AS section,
       (SELECT COUNT(*) FROM (SELECT sid, pid, iid FROM nb_pg_single
                              EXCEPT
                              SELECT sid, pid, iid FROM nb_pg_read2) t) AS 只在单箭头表里的行数;


-- ============================================================
-- 步骤 6：清理(示例表默认保留，想清干净手工删这三张表)
-- ============================================================
SELECT '脚本执行完毕：三张表留库，可反复查看对比' AS section;


-- ============================================================
-- 实测结论(PostgreSQL 17.11, 临时库 nbdemo_check, 无非预期报错 ——
--   3 处“重复键违反唯一约束”都是刻意演示的拒绝)：
--   ============ 单箭头表 nb_pg_single(UNIQUE(sid,pid)) ============
--   A1 (s1,p1,i1)  成功     A2 (s1,p2,i2)  成功
--   B  (s1,p1,i2)  失败(duplicate key uk_nb_pg_single)
--   C  (s2,p1,i1)  成功     → 共 3 行
--   ============ 读法Ⅰ表 nb_pg_read1(UNIQUE(iid,pid)) ============
--   A1 成功  A2 成功  B (s1,p1,i2) 成功  C (s2,p1,i1) 失败(uk_nb_pg_read1)
--   → 共 3 行 = A1, A2, B
--   ============ 读法Ⅱ表 nb_pg_read2(UNIQUE(sid,pid)+UNIQUE(sid,iid)) ============
--   A1 成功  A2 成功  B 失败(uk_nb_pg_read2_a)  C 成功
--   → 共 3 行 = A1, A2, C
--   结论：两张双箭头表行数相同(都是 3 行)，但一行之差 ——
--   读法Ⅰ表里有 B 没有 C，读法Ⅱ表里有 C 没有 B，两者互不包含，
--   所以“两个箭头”的图确实有歧义；而单箭头表与读法Ⅱ表内容完全一致
--   (只在单箭头表里的行数 = 0)，说明一个箭头时两种读法落到同一张表。
-- ============================================================
