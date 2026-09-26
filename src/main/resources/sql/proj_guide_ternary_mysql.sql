-- ============================================================
-- 三元联系集 proj_guide:建立,以及按 7.7.3 节拆分为二元联系 —— 动手验证版
-- 数据库: MySQL 8.0(库名由 -db 指定)
-- 教材: 《数据库系统概念》原书第6版·本科教学版(机械工业出版社,
--       Silberschatz / Korth / Sudarshan)
--       第7章 数据库设计与 E-R 模型 · 7.7.3 二元还是 n 元联系集 · 图 7-19
-- 姊妹脚本: proj_guide_ternary.sql(PostgreSQL 17 等价版)
-- 前置脚本: nonbinary_arrows_demo_mysql.sql(同章 7.5.5 节,图 7-13 的箭头画法)
-- ------------------------------------------------------------
-- 本节在讲什么:
--   1. 图 7-19 a) 是三元联系集 proj_guide,关联 instructor / student / project,
--      一行 = "某教师 带着 某学生 参与 某项目"这一三方联合事实。
--      关系模式 proj_guide(iid, sid, pid),三个键合起来做主码。
--
--   2. 图 7-19 b) 是把它换成二元联系的写法:
--      ① 新建实体集 E,并给它一个"新的标识属性" guide_id
--         (不能拿 (iid, sid, pid) 当 E 的标识属性,否则 E 就是原三元表)；
--      ② 原三元组的每一行,在 E 里生成一个实体 e；
--      ③ 建三个二元联系集 R_A(E, instructor)、R_B(E, student)、R_C(E, project),
--         分别插入 (e, 教师)、(e, 学生)、(e, 项目)。
--      三个二元联系都是"多对一",guide_id 是它们各自的主码。
--
--   3. 逐步验证:拆分无损 -> 拆分后会多出"跨对拼装"的三方事实 ->
--      三元联系上的 (iid, sid) -> pid 多对一约束拆开后表达不出来 ->
--      不建 E 直接拆两个二元联系会因连接造出假元组(有损)。
--
-- 与 PostgreSQL 版的差异:
--   SELECT ... EXCEPT SELECT ...  →  NOT EXISTS 子查询(MySQL 无 EXCEPT)
--   参照完整性:MySQL 的列级 REFERENCES 只是语法占位、不生效,
--               入门演示沿用姊妹脚本的写法,不显式建外键约束
--   建表统一 ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
--
-- 运行:
--   .\sql.bat -db <库名> -f src\main\resources\sql\proj_guide_ternary_mysql.sql
--
-- 实测结论(MySQL 8.0.44): 见文件末尾。
-- ============================================================


-- ============================================================
-- 步骤 0：清理(可重复运行)
-- ============================================================
DROP VIEW  IF EXISTS my_guide_rebuilt;
DROP TABLE IF EXISTS my_guide_r_a;
DROP TABLE IF EXISTS my_guide_r_b;
DROP TABLE IF EXISTS my_guide_r_c;
DROP TABLE IF EXISTS my_guide_event;
DROP TABLE IF EXISTS my_guide_event_v2;
DROP TABLE IF EXISTS my_guide_map;
DROP TABLE IF EXISTS my_guide_ternary;
DROP TABLE IF EXISTS my_guide_m1;
DROP TABLE IF EXISTS my_advises;
DROP TABLE IF EXISTS my_works_on;

SELECT '步骤 0 完成：旧表已清理' AS section;


-- ============================================================
-- 步骤 1：三元联系集 proj_guide(图 7-19 a)
--   一行 = 某教师 带着 某学生 参与 某项目,三个实体缺一不可。
--   沿用姊妹脚本的简化写法:教师/学生/项目只留文本编号,不建三张实体表。
-- ============================================================
CREATE TABLE my_guide_ternary (
    iid  VARCHAR(5) NOT NULL,      -- instructor: i1 Katz / i2 Srinivasan
    sid  VARCHAR(5) NOT NULL,      -- student   : s1 Shankar / s2 Zhang / s3 Umar
    pid  VARCHAR(5) NOT NULL,      -- project   : p1 项目A / p2 项目B / p3 项目C
    CONSTRAINT pk_my_guide_ternary PRIMARY KEY (iid, sid, pid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO my_guide_ternary (iid, sid, pid) VALUES
    ('i1', 's1', 'p1'),            -- Katz 同 Shankar 一起参与项目A
    ('i1', 's2', 'p2'),            -- Katz 同 Zhang   一起参与项目B
    ('i2', 's3', 'p3');            -- Srinivasan 同 Umar 一起参与项目C

SELECT '步骤 1 完成：三元联系集 proj_guide 建立并装入 3 行' AS section;

SELECT '三元联系集 my_guide_ternary(图 7-19 a)' AS section;
SELECT iid, sid, pid FROM my_guide_ternary ORDER BY iid, sid, pid;


-- ============================================================
-- 步骤 2：按 7.7.3 节拆分为二元联系(图 7-19 b)
-- ============================================================
CREATE TABLE my_guide_event (
    guide_id   INT PRIMARY KEY,       -- 课本要求:为 E 新造的标识属性
    guide_date DATE                   -- 原三元联系集上的属性,归到 E
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE my_guide_r_a (
    guide_id INT PRIMARY KEY,         -- E 侧唯一:一个 e 只对应一个教师
    iid      VARCHAR(5) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE my_guide_r_b (
    guide_id INT PRIMARY KEY,
    sid      VARCHAR(5) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE my_guide_r_c (
    guide_id INT PRIMARY KEY,
    pid      VARCHAR(5) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

SELECT '步骤 2 完成：新实体集 E + 三个二元联系集 R_A / R_B / R_C 已建立' AS section;


-- ============================================================
-- 步骤 3：迁移(拆分动作本身)
--   先人工建出"三元组 ↔ guide_id"的对应关系,再往四张表里各插一次。
-- ============================================================
CREATE TABLE my_guide_map (
    guide_id INT PRIMARY KEY,
    iid      VARCHAR(5) NOT NULL,
    sid      VARCHAR(5) NOT NULL,
    pid      VARCHAR(5) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO my_guide_map (guide_id, iid, sid, pid)
SELECT ROW_NUMBER() OVER (ORDER BY iid, sid, pid), iid, sid, pid
FROM my_guide_ternary;

INSERT INTO my_guide_event (guide_id, guide_date)
SELECT guide_id, DATE '2026-09-26' FROM my_guide_map;

INSERT INTO my_guide_r_a (guide_id, iid) SELECT guide_id, iid FROM my_guide_map;
INSERT INTO my_guide_r_b (guide_id, sid) SELECT guide_id, sid FROM my_guide_map;
INSERT INTO my_guide_r_c (guide_id, pid) SELECT guide_id, pid FROM my_guide_map;

SELECT '步骤 3 完成：三元组 -> guide_id 的对应关系与四张表已写入' AS section;

SELECT '迁移对照表 my_guide_map(人工维护的对应关系)' AS section;
SELECT guide_id, iid, sid, pid FROM my_guide_map ORDER BY guide_id;

SELECT '新实体集 E = my_guide_event' AS section;
SELECT guide_id, guide_date FROM my_guide_event ORDER BY guide_id;

SELECT '三个二元联系集 R_A / R_B / R_C' AS section;
SELECT a.guide_id, a.iid, b.sid, c.pid
FROM my_guide_r_a a
JOIN my_guide_r_b b ON b.guide_id = a.guide_id
JOIN my_guide_r_c c ON c.guide_id = a.guide_id
ORDER BY a.guide_id;


-- ============================================================
-- 步骤 4：验证拆分无损 —— 三个二元联系连接回来应与原三元表逐行相同
-- ============================================================
CREATE VIEW my_guide_rebuilt AS
SELECT a.iid, b.sid, c.pid
FROM my_guide_r_a a
JOIN my_guide_r_b b ON b.guide_id = a.guide_id
JOIN my_guide_r_c c ON c.guide_id = a.guide_id;

SELECT '步骤 4 完成：还原视图 my_guide_rebuilt 已建立' AS section;

SELECT '方向一：原三元表有、还原结果没有的行数(期望 0)' AS section,
       (SELECT COUNT(*) FROM my_guide_ternary t
        WHERE NOT EXISTS (SELECT 1 FROM my_guide_rebuilt r
                          WHERE r.iid = t.iid AND r.sid = t.sid AND r.pid = t.pid)) AS 差异行数;

SELECT '方向二：还原结果有、原三元表没有的行数(期望 0)' AS section,
       (SELECT COUNT(*) FROM my_guide_rebuilt r
        WHERE NOT EXISTS (SELECT 1 FROM my_guide_ternary t
                          WHERE t.iid = r.iid AND t.sid = r.sid AND t.pid = r.pid)) AS 差异行数;


-- ============================================================
-- 步骤 5：反例一 —— 跨对拼装
--   原始数据里 Katz 只和 Shankar 一起做过项目A,
--   现在绕过三元表,把"Katz 带 Shankar 做项目B"分别写进三个二元联系。
-- ============================================================
INSERT INTO my_guide_event (guide_id, guide_date) VALUES (999, DATE '2026-09-26');
INSERT INTO my_guide_r_a (guide_id, iid) VALUES (999, 'i1');
INSERT INTO my_guide_r_b (guide_id, sid) VALUES (999, 's1');
INSERT INTO my_guide_r_c (guide_id, pid) VALUES (999, 'p2');

SELECT '步骤 5 完成：跨对拼装的三行已分别写入三个二元联系' AS section;

SELECT '三个二元联系里被违反的多对一约束数(期望 0 —— 谁都没被违反)' AS section,
       (SELECT COUNT(*) FROM (SELECT guide_id FROM my_guide_r_a GROUP BY guide_id HAVING COUNT(*) > 1) t1)
     + (SELECT COUNT(*) FROM (SELECT guide_id FROM my_guide_r_b GROUP BY guide_id HAVING COUNT(*) > 1) t2)
     + (SELECT COUNT(*) FROM (SELECT guide_id FROM my_guide_r_c GROUP BY guide_id HAVING COUNT(*) > 1) t3)
       AS 违反数;

SELECT '但还原出来的三元关系里,Katz(i1)+Shankar(s1) 挂了两个项目' AS section;
SELECT pid FROM my_guide_rebuilt
WHERE iid = 'i1' AND sid = 's1'
ORDER BY pid;


-- ============================================================
-- 步骤 6：反例二 —— 三元联系上的约束无处安放
--   约束:一对 (instructor, student) 最多参与一个项目,即 (iid, sid) 是超键。
-- ============================================================
CREATE TABLE my_guide_m1 (
    iid VARCHAR(5) NOT NULL,
    sid VARCHAR(5) NOT NULL,
    pid VARCHAR(5) NOT NULL,
    CONSTRAINT pk_my_guide_m1 PRIMARY KEY (iid, sid, pid),
    CONSTRAINT uk_my_guide_m1 UNIQUE (iid, sid)        -- 三元模型:约束一句话就能写
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO my_guide_m1 (iid, sid, pid) VALUES
    ('i1', 's1', 'p1'),
    ('i1', 's2', 'p2'),
    ('i2', 's3', 'p3');

SELECT '步骤 6 完成：带 (iid,sid) 唯一约束的三元表 my_guide_m1 已建立' AS section;

-- 下面这条是刻意演示的拒绝:i1+s2 已经参与 p2,再插 p1 违反 uk_my_guide_m1
INSERT INTO my_guide_m1 (iid, sid, pid) VALUES ('i1', 's2', 'p1');

SELECT '三元模型写入 (i1, s2, p1) 后的行数(期望仍是 3 行,违规行被拒绝)' AS section,
       COUNT(*) AS 行数
FROM my_guide_m1;

-- 同样的违规事实,换成拆分模型:三个二元联系谁都没被违反,一路写入成功
INSERT INTO my_guide_event (guide_id, guide_date) VALUES (1000, DATE '2026-09-26');
INSERT INTO my_guide_r_a (guide_id, iid) VALUES (1000, 'i1');
INSERT INTO my_guide_r_b (guide_id, sid) VALUES (1000, 's2');
INSERT INTO my_guide_r_c (guide_id, pid) VALUES (1000, 'p1');

SELECT '拆分模型写入同一条事实:guide_id = 1000 的四行是否都在(期望都在)' AS section;
SELECT (SELECT COUNT(*) FROM my_guide_event WHERE guide_id = 1000) AS e行数,
       (SELECT COUNT(*) FROM my_guide_r_a WHERE guide_id = 1000) AS ra行数,
       (SELECT COUNT(*) FROM my_guide_r_b WHERE guide_id = 1000) AS rb行数,
       (SELECT COUNT(*) FROM my_guide_r_c WHERE guide_id = 1000) AS rc行数;

SELECT '还原后检查:一对师生对应的项目数 > 1(约束被绕过)' AS section;
SELECT iid, sid, COUNT(DISTINCT pid) AS 项目数
FROM my_guide_rebuilt
GROUP BY iid, sid
HAVING COUNT(DISTINCT pid) > 1
ORDER BY iid, sid;

-- 补救写法:三个二元联系都是多对一,可以把外键并回 E,约束就写得回来了
CREATE TABLE my_guide_event_v2 (
    guide_id   INT PRIMARY KEY,
    guide_date DATE,
    iid        VARCHAR(5) NOT NULL,
    sid        VARCHAR(5) NOT NULL,
    pid        VARCHAR(5) NOT NULL,
    CONSTRAINT uk_my_guide_event_v2 UNIQUE (iid, sid)   -- 约束搬回来了
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO my_guide_event_v2 VALUES
    (1, DATE '2026-09-26', 'i1', 's1', 'p1'),
    (2, DATE '2026-09-26', 'i1', 's2', 'p2');

-- 刻意演示的拒绝:同样的违规事实在补救写法下又被挡住了
INSERT INTO my_guide_event_v2 VALUES (3, DATE '2026-09-26', 'i1', 's2', 'p1');

SELECT '补救写法写入 (i1, s2, p1) 后的行数(期望 2 行,又被拒绝)' AS section,
       COUNT(*) AS 行数
FROM my_guide_event_v2;


-- ============================================================
-- 步骤 7：反例三 —— 不建 E,直接拆成两个二元联系(有损)
-- ============================================================
CREATE TABLE my_advises  (iid VARCHAR(5) NOT NULL, sid VARCHAR(5) NOT NULL,
                          CONSTRAINT pk_my_advises  PRIMARY KEY (iid, sid))
                          ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE my_works_on (iid VARCHAR(5) NOT NULL, pid VARCHAR(5) NOT NULL,
                          CONSTRAINT pk_my_works_on PRIMARY KEY (iid, pid))
                          ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO my_advises  (iid, sid) SELECT DISTINCT iid, sid FROM my_guide_ternary;
INSERT INTO my_works_on (iid, pid) SELECT DISTINCT iid, pid FROM my_guide_ternary;

SELECT '步骤 7 完成：两个二元联系已按投影填入' AS section;

SELECT '原三元表行数 / 两个二元联系连接回来的行数(期望 3 / 5)' AS section,
       (SELECT COUNT(*) FROM my_guide_ternary) AS 原三元表行数,
       (SELECT COUNT(*) FROM my_advises a JOIN my_works_on w ON w.iid = a.iid) AS 连接后行数;

SELECT '连接伪造出来的、原三元表里并不存在的三方事实' AS section;
SELECT a.iid, a.sid, w.pid
FROM my_advises a
JOIN my_works_on w ON w.iid = a.iid
WHERE NOT EXISTS (SELECT 1 FROM my_guide_ternary t
                  WHERE t.iid = a.iid AND t.sid = a.sid AND t.pid = w.pid);


-- ============================================================
-- 步骤 8：三种写法对比
-- ============================================================
SELECT '三元模型 proj_guide(iid,sid,pid)'                AS 写法,
       1  AS 联系表数, '三个实体集的键,天然唯一'           AS 标识属性,
       '三元联系上的约束一句话就能写'                     AS 约束可表达性
UNION ALL
SELECT '拆分模型(图 7-19 b):E + R_A + R_B + R_C',
       4, '必须新造 guide_id,并人工维护三元组到 E 的对应关系',
       '跨三表的约束无法表达,只能靠触发器/应用层兜住'
UNION ALL
SELECT '外键并回 E 的补救写法 E(guide_id,iid,sid,pid)',
       1, 'guide_id 是纯代理键', '约束能写,但结构已退回三元表';

SELECT '脚本执行完毕：示例表留库(my_guide_* / my_advises / my_works_on),可反复查看对比' AS section;


-- ============================================================
-- 实测结论(MySQL 8.0.44): 见下方(与 PostgreSQL 版逐项一致)
--   ------------------------------------------------------------------
--   (1) 拆分无损:双向差集均为 0 行。
--   (2) 跨对拼装:三个二元联系被违反的多对一约束数 = 0,
--       但还原出的三元关系里 (i1,s1) 同时挂了 p1、p2 两个项目。
--   (3) 约束丢失:三元模型插 (i1,s2,p1) 报 1062 Duplicate entry 'i1-s2'
--       for key 'my_guide_m1.uk_my_guide_m1',表仍是 3 行；
--       拆分模型同样的事实(guide_id=1000 的四行)全部写入成功,还原后
--       (i1,s1)、(i1,s2) 各对应 2 个项目；补救写法又把违规行挡住了,表仍是 2 行。
--   (4) 直接拆两个二元联系:原三元表 3 行 -> 连接回来 5 行,
--       伪造出 (i1,s1,p2)、(i1,s2,p1) 两条原本不存在的三方事实。
--   结论:与 PostgreSQL 版逐项一致 —— 可以无损拆,但代价是新造标识属性、
--   联系表 1 张变 4 张、三元联系上的约束无法迁移。
-- ============================================================
