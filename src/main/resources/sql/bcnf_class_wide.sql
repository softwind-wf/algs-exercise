-- ============================================================================
-- BCNF 实战演练：一张真实的"教务宽表" class_wide —— 判定 → 分解 → 迁移 → 验证
-- 教材《数据库系统概念》8.5 节（中文版第 198 页起：BCNF 判定与 BCNF 分解算法）
--
-- 数据库：bcnf_class_demo（独立演示库，不动任何交付库）
-- 运行：
--   .\psql.bat "DROP DATABASE IF EXISTS bcnf_class_demo WITH (FORCE)"
--   .\psql.bat "CREATE DATABASE bcnf_class_demo"
--   .\psql.bat -d bcnf_class_demo -f src\main\resources\sql\bcnf_class_wide.sql
--
-- 场景：教务系统里一张手工维护的"课程 + 开课班 + 教室"宽表（报表导出表的典型形态）。
--       11 个属性混在一起，课程名/系/学分随每个开课班重复，教室容量随每节课重复。
--
-- 语义 FD（业务规则，不是从数据统计出来的）：
--   F1  course_id -> title, dept_name, credits
--       一门课只有一个课程名、一个开课系、一个学分数。
--   F2  (building, room_number) -> capacity
--       一个教室只有一个容量。
--   F3  (course_id, sec_id, semester, year) -> building, room_number, time_slot_id
--       一个开课班只在一个教室、一个时段上课。
--
-- 结论（脚本逐步验证）：
--   候选键 = (course_id, sec_id, semester, year)（这也就是宽表的主键）
--   F1 左部 course_id 不是超键          ⇒ 违反 BCNF
--   F2 左部 (building, room_number) 不是超键 ⇒ 违反 BCNF
--   F3 左部就是候选键（超键）           ⇒ 不违反
--
-- BCNF 分解（图 8-11 算法，与 Java 工具 com.ds.fd.BcnfDecomposition 输出一致）：
--   course   (course_id, title, dept_name, credits)
--   section  (course_id, sec_id, semester, year, building, room_number, time_slot_id)
--   classroom(building, room_number, capacity)
--   三张表都满足 BCNF；分解无损；三条 FD 各自落在某张表里（保持依赖）。
--
-- 本脚本可重复运行：开头先 DROP 再重建，结尾保留演示对象，方便随时复查。
-- ============================================================================

SET client_encoding = 'UTF8';
\set ON_ERROR_STOP on
\pset pager off

SELECT '=== BCNF 实战：bcnf_class_demo.class_wide ===' AS phase;


-- ############################################################################
-- 第 0 步：建宽表 + 灌 11 行真实感数据
-- ############################################################################
\echo '【第 0 步】建宽表 class_wide（课程 × 开课班 × 教室 混在一起）'

DROP VIEW  IF EXISTS class_wide_v;
DROP TABLE IF EXISTS section, course, classroom CASCADE;
DROP TABLE IF EXISTS class_wide CASCADE;

CREATE TABLE class_wide (
    course_id    VARCHAR(8)   NOT NULL,
    title        VARCHAR(64)  NOT NULL,
    dept_name    VARCHAR(32)  NOT NULL,
    credits      NUMERIC(3,1) NOT NULL,
    sec_id       VARCHAR(8)   NOT NULL,
    semester     VARCHAR(16)  NOT NULL,
    year         INT          NOT NULL,
    building     VARCHAR(32)  NOT NULL,
    room_number  VARCHAR(16)  NOT NULL,
    capacity     INT          NOT NULL,
    time_slot_id VARCHAR(8)   NOT NULL,
    CONSTRAINT pk_class_wide PRIMARY KEY (course_id, sec_id, semester, year),
    CONSTRAINT ck_class_wide_credits  CHECK (credits > 0),
    CONSTRAINT ck_class_wide_capacity CHECK (capacity > 0)
);

COMMENT ON TABLE  class_wide IS '教务宽表（反例）：课程信息与开课/教室信息混存';
COMMENT ON COLUMN class_wide.course_id IS '课程号；F1 的左部，不是超键';
COMMENT ON COLUMN class_wide.capacity  IS '教室容量；由 (building, room_number) 决定，不是键';

INSERT INTO class_wide (course_id, title, dept_name, credits,
                        sec_id, semester, year,
                        building, room_number, capacity, time_slot_id) VALUES
('CS-101', 'Intro to Comp. Sci.', 'Comp. Sci.', 4.0, '1', 'Fall',   2017, 'Packard', '101',  500, 'A'),
('CS-101', 'Intro to Comp. Sci.', 'Comp. Sci.', 4.0, '2', 'Fall',   2017, 'Packard', '101',  500, 'B'),
('CS-101', 'Intro to Comp. Sci.', 'Comp. Sci.', 4.0, '1', 'Spring', 2018, 'Packard', '101',  500, 'A'),
('CS-315', 'Robotics',            'Comp. Sci.', 3.0, '1', 'Fall',   2017, 'Taylor',  '3128',  70, 'C'),
('CS-315', 'Robotics',            'Comp. Sci.', 3.0, '2', 'Fall',   2017, 'Taylor',  '3128',  70, 'D'),
('CS-319', 'Image Processing',    'Comp. Sci.', 3.0, '1', 'Spring', 2018, 'Taylor',  '3128',  70, 'B'),
('BIO-101','Intro to Biology',    'Biology',    4.0, '1', 'Fall',   2017, 'Painter', '514',   100, 'E'),
('BIO-101','Intro to Biology',    'Biology',    4.0, '2', 'Fall',   2017, 'Painter', '514',   100, 'F'),
('BIO-301','Genetics',            'Biology',    4.0, '1', 'Spring', 2018, 'Painter', '514',   100, 'G'),
('CS-190', 'Game Design',         'Comp. Sci.', 4.0, '1', 'Spring', 2018, 'Watson',  '120',    30, 'H'),
('CS-190', 'Game Design',         'Comp. Sci.', 4.0, '2', 'Spring', 2018, 'Watson',  '120',    30, 'I');

\echo '【第 0 步】表结构（11 个属性；主键 = course_id + sec_id + semester + year）'
\d class_wide

\echo '【第 0 步】全部数据（11 行；同一门课 / 同一间教室的信息在重复出现）'
SELECT * FROM class_wide ORDER BY course_id, semester, year, sec_id;


-- ############################################################################
-- 第 1 步：先看冗余到底长什么样
-- ############################################################################
\echo '【第 1 步】冗余现场：同一门课存几行？同一间教室存几行？'

SELECT course_id, title, dept_name, credits, count(*) AS 重复行数
FROM class_wide
GROUP BY course_id, title, dept_name, credits
ORDER BY course_id;

SELECT building, room_number, capacity, count(*) AS 重复行数
FROM class_wide
GROUP BY building, room_number, capacity
ORDER BY building, room_number;

SELECT count(*)                                  AS 总行数,
       count(DISTINCT course_id)                 AS 课程数,
       count(DISTINCT (building, room_number))   AS 教室数
FROM class_wide;


-- ############################################################################
-- 第 2 步：数据侧验证三条 FD 成立（0 行反例 ≠ 证明了 FD，只是没被数据打脸）
-- ############################################################################
\echo '【第 2 步】数据侧验证：三条 FD 在现有数据上没有反例（0 行）'
\echo '  注意：FD 是业务规则，不是数据统计结论；数据只能"证伪"，不能"证明"。'

\echo '  F1: course_id -> {title, dept_name, credits}'
SELECT course_id, count(DISTINCT (title, dept_name, credits)) AS 不同组合数
FROM class_wide
GROUP BY course_id
HAVING count(DISTINCT (title, dept_name, credits)) > 1;

\echo '  F2: {building, room_number} -> capacity'
SELECT building, room_number, count(DISTINCT capacity) AS 不同容量数
FROM class_wide
GROUP BY building, room_number
HAVING count(DISTINCT capacity) > 1;

\echo '  F3: {course_id, sec_id, semester, year} -> {building, room_number, time_slot_id}'
SELECT course_id, sec_id, semester, year,
       count(DISTINCT (building, room_number, time_slot_id)) AS 不同教室时段组合数
FROM class_wide
GROUP BY course_id, sec_id, semester, year
HAVING count(DISTINCT (building, room_number, time_slot_id)) > 1;


-- ############################################################################
-- 第 3 步：候选键，以及"为什么不是超键"
-- ############################################################################
\echo '【第 3 步】候选键与"不是超键"的证据'

\echo '  3.1 主键 (course_id, sec_id, semester, year) 唯一标识一行 => 是候选键'
SELECT count(*) AS 总行数,
       count(DISTINCT (course_id, sec_id, semester, year)) AS 主键唯一组合数
FROM class_wide;

\echo '  3.2 course_id 不是超键：同一门课有多个开课班（决定不了 sec_id）'
SELECT course_id, count(*) AS 行数, count(DISTINCT sec_id) AS 不同开课班
FROM class_wide
GROUP BY course_id
HAVING count(DISTINCT sec_id) > 1
ORDER BY course_id;

\echo '  3.3 (building, room_number) 不是超键：同一间教室开了多门课'
SELECT building, room_number, count(DISTINCT course_id) AS 不同课程数
FROM class_wide
GROUP BY building, room_number
HAVING count(DISTINCT course_id) > 1
ORDER BY building, room_number;


-- ############################################################################
-- 第 4 步：BCNF 判定表（对照教材判据）
-- ############################################################################
\echo '【第 4 步】BCNF 判定：F 中每条非平凡 FD 的左部都必须是超键'
SELECT *
FROM (VALUES
    ('course_id -> {title, dept_name, credits}',
     'course_id',
     '否（有多个 sec_id）',
     '违反 BCNF'),
    ('{building, room_number} -> {capacity}',
     '{building, room_number}',
     '否（同一教室多门课）',
     '违反 BCNF'),
    ('{course_id, sec_id, semester, year} -> {building, room_number, time_slot_id}',
     '{course_id, sec_id, semester, year}',
     '是（候选键 / 超键）',
     '不违反')
) AS t(非平凡函数依赖, 左部, 左部是超键吗, 判定);


-- ############################################################################
-- 第 5 步：异常现场（都在事务里做，最后 ROLLBACK，不改数据）
-- ############################################################################
\echo '【第 5 步】宽表的异常现场（事务内演示，结束后 ROLLBACK）'

BEGIN;
\echo '  5.1 更新异常：改一门课的名字，要改 3 行；漏改一行就自相矛盾'
UPDATE class_wide SET title = 'Intro to Computer Science' WHERE course_id = 'CS-101';
ROLLBACK;

BEGIN;
\echo '  5.2 删除异常：删掉 CS-319 唯一的开课班，课程本身的信息也跟着消失'
DELETE FROM class_wide WHERE course_id = 'CS-319';
SELECT count(*) AS 删后剩余行数 FROM class_wide;
ROLLBACK;


-- ############################################################################
-- 第 6 步：按 BCNF 分解算法建三张表并迁移数据
-- ############################################################################
\echo '【第 6 步】建分解后的三张表（分解结果 = Java 工具 BcnfDecomposition 的输出）'

CREATE TABLE course (
    course_id VARCHAR(8)   NOT NULL,
    title     VARCHAR(64)  NOT NULL,
    dept_name VARCHAR(32)  NOT NULL,
    credits   NUMERIC(3,1) NOT NULL,
    CONSTRAINT pk_course PRIMARY KEY (course_id),
    CONSTRAINT ck_course_credits CHECK (credits > 0)
);
COMMENT ON TABLE course IS 'BCNF 表 1：课程（F1 的落点，左部 course_id 是主键）';

CREATE TABLE classroom (
    building    VARCHAR(32) NOT NULL,
    room_number VARCHAR(16) NOT NULL,
    capacity    INT         NOT NULL,
    CONSTRAINT pk_classroom PRIMARY KEY (building, room_number),
    CONSTRAINT ck_classroom_capacity CHECK (capacity > 0)
);
COMMENT ON TABLE classroom IS 'BCNF 表 2：教室（F2 的落点，左部 (building, room_number) 是主键）';

CREATE TABLE section (
    course_id    VARCHAR(8)  NOT NULL,
    sec_id       VARCHAR(8)  NOT NULL,
    semester     VARCHAR(16) NOT NULL,
    year         INT         NOT NULL,
    building     VARCHAR(32) NOT NULL,
    room_number  VARCHAR(16) NOT NULL,
    time_slot_id VARCHAR(8)  NOT NULL,
    CONSTRAINT pk_section PRIMARY KEY (course_id, sec_id, semester, year),
    CONSTRAINT fk_section_course
        FOREIGN KEY (course_id) REFERENCES course (course_id),
    CONSTRAINT fk_section_classroom
        FOREIGN KEY (building, room_number) REFERENCES classroom (building, room_number)
);
COMMENT ON TABLE section IS 'BCNF 表 3：开课班（F3 的落点，左部是主键）';

\echo '  迁移：同一门课只留一行（DISTINCT），同一间教室只留一行'
INSERT INTO course (course_id, title, dept_name, credits)
SELECT DISTINCT course_id, title, dept_name, credits FROM class_wide;

INSERT INTO classroom (building, room_number, capacity)
SELECT DISTINCT building, room_number, capacity FROM class_wide;

INSERT INTO section (course_id, sec_id, semester, year, building, room_number, time_slot_id)
SELECT course_id, sec_id, semester, year, building, room_number, time_slot_id
FROM class_wide;

\echo '  迁移后行数：course / classroom 去重，section 与宽表一一对应'
SELECT 'class_wide' AS 表, count(*) AS 行数 FROM class_wide
UNION ALL
SELECT 'course',    count(*) FROM course
UNION ALL
SELECT 'classroom', count(*) FROM classroom
UNION ALL
SELECT 'section',   count(*) FROM section
ORDER BY 表;


-- ############################################################################
-- 第 7 步：无损验证 —— 三张表连接回来必须等于原宽表
-- ############################################################################
\echo '【第 7 步】无损验证：自然连接回来 = 原来的 11 行（双向 EXCEPT 都为空）'

SELECT count(*) AS 连接后行数
FROM course c
JOIN section s USING (course_id)
JOIN classroom r USING (building, room_number);

\echo '  7.1 原宽表 − 连接结果（应为 0 行）'
SELECT course_id, title, dept_name, credits, sec_id, semester, year,
       building, room_number, capacity, time_slot_id
FROM class_wide
EXCEPT
SELECT c.course_id, c.title, c.dept_name, c.credits, s.sec_id, s.semester, s.year,
       r.building, r.room_number, r.capacity, s.time_slot_id
FROM course c
JOIN section s USING (course_id)
JOIN classroom r USING (building, room_number);

\echo '  7.2 连接结果 − 原宽表（应为 0 行）'
SELECT c.course_id, c.title, c.dept_name, c.credits, s.sec_id, s.semester, s.year,
       r.building, r.room_number, r.capacity, s.time_slot_id
FROM course c
JOIN section s USING (course_id)
JOIN classroom r USING (building, room_number)
EXCEPT
SELECT course_id, title, dept_name, credits, sec_id, semester, year,
       building, room_number, capacity, time_slot_id
FROM class_wide;

\echo '  7.3 展开看一眼：连接结果与原宽表逐行一致'
SELECT c.course_id, c.title, s.sec_id, s.semester, s.year,
       r.building, r.room_number, r.capacity, s.time_slot_id
FROM course c
JOIN section s USING (course_id)
JOIN classroom r USING (building, room_number)
ORDER BY c.course_id, s.semester, s.year, s.sec_id;


-- ############################################################################
-- 第 8 步：三张表的 BCNF 证据（每条非平凡 FD 的左部都是该表的键）
-- ############################################################################
\echo '【第 8 步】三张表的 BCNF 证据：非平凡 FD 的左部都是该表的键'
SELECT *
FROM (VALUES
    ('course',
     'course_id -> {title, dept_name, credits}',
     'course_id = 主键',
     'BCNF'),
    ('classroom',
     '{building, room_number} -> {capacity}',
     '(building, room_number) = 主键',
     'BCNF'),
    ('section',
     '{course_id, sec_id, semester, year} -> {building, room_number, time_slot_id}',
     '{course_id, sec_id, semester, year} = 主键',
     'BCNF')
) AS t(表, 非平凡函数依赖, 左部是键吗, 结论);

\echo '  8.1 数据库层面已经把三条 FD 变成约束：三个主键 + 两个外键'
SELECT conrelid::regclass AS 表, conname AS 约束, pg_get_constraintdef(oid) AS 定义
FROM pg_constraint
WHERE conrelid IN ('course'::regclass, 'classroom'::regclass, 'section'::regclass)
ORDER BY 表, conname;


-- ############################################################################
-- 第 9 步：同样的更新，现在只改 1 行
-- ############################################################################
\echo '【第 9 步】更新异常消失：改课程名只动 1 行（事务内演示后 ROLLBACK）'
BEGIN;
UPDATE course SET title = 'Intro to Computer Science' WHERE course_id = 'CS-101';
SELECT course_id, title FROM course WHERE course_id = 'CS-101';
ROLLBACK;


-- ############################################################################
-- 第 10 步：建兼容视图 + 结论
-- ############################################################################
\echo '【第 10 步】建兼容视图 class_wide_v：应用查询不必改写'
CREATE OR REPLACE VIEW class_wide_v AS
SELECT c.course_id, c.title, c.dept_name, c.credits,
       s.sec_id, s.semester, s.year,
       r.building, r.room_number, r.capacity, s.time_slot_id
FROM course c
JOIN section s USING (course_id)
JOIN classroom r USING (building, room_number);

SELECT * FROM class_wide_v ORDER BY course_id, semester, year, sec_id;

\echo '【结论】'
\echo '  1) class_wide 不满足 BCNF：F1 左部 course_id、F2 左部 (building, room_number) 都不是超键。'
\echo '  2) 按图 8-11 分解为 course / section / classroom 三张表，每张都是 BCNF。'
\echo '  3) 分解无损：连接回来仍是 11 行，双向 EXCEPT 为空。'
\echo '  4) 分解保持依赖：F1 在 course、F2 在 classroom、F3 在 section，都由主键直接约束。'
\echo '  5) 冗余消失：课程信息 6 行、教室信息 4 行，更新课程名从 3 行变 1 行。'
\echo '=== 演示结束：对象保留在 bcnf_class_demo 中，可直接查询复查 ==='
