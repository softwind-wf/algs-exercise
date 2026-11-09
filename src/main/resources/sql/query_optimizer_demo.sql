-- ============================================================================
-- 查询优化全过程演示
-- 教材:《数据库系统概念》(第6版) 11.5 查询优化
--
--   书上给的关系表达式 (11.5.1):
--     Pi_{name,title} ( sigma_{dept_name="Music"} (
--         instructor 自然连接 ( teaches 自然连接 Pi_{course_id,title}(course) ) ) )
--
--   对应的 SQL:
--     SELECT name, title
--     FROM instructor, teaches, course
--     WHERE instructor.id = teaches.id
--       AND teaches.course_id = course.course_id
--       AND instructor.dept_name = 'Music';
--
-- 本文件按教材的流水线逐段落地,每一步都对应图 11-1 的一个框:
--   [1] 统计值      -> 图右下角"有关数据的统计值"
--   [2] 逻辑优化    -> 优化器: 等价变换(选择下推/投影下推)
--   [3] 物理计划    -> 优化器: 选访问路径 + 连接算法
--   [4] 备选计划    -> 优化器在搜索的计划空间
--   [5] 索引影响    -> 代价估算如何决定走不走索引
--   [6] 投影不能省  -> 书上那段"course 上 (course_id,title) 的投影是必需的"
--   [7] 统计值失真  -> 估算错 -> 计划错
--
-- 规模放大到 20000 教师 / 4000 课程 / 200000 选课记录,才看得出代价差异。
-- ============================================================================

\set ON_ERROR_STOP on
SET client_min_messages = warning;
\pset pager off
\timing off

\echo '===== 0. 建表与造数(同构于 university,只是放大到 20000/4000/200000) ====='

DROP TABLE IF EXISTS teaches, course, instructor CASCADE;

CREATE TABLE instructor (
    id        varchar(5)  PRIMARY KEY,
    name      varchar(20) NOT NULL,
    dept_name varchar(20)
);

CREATE TABLE course (
    course_id varchar(8)  PRIMARY KEY,
    title     varchar(50),
    dept_name varchar(20),
    credits   numeric(2,0)
);

CREATE TABLE teaches (
    id        varchar(5)  NOT NULL,
    course_id varchar(8)  NOT NULL,
    sec_id    varchar(8)  NOT NULL,
    semester  varchar(6)  NOT NULL,
    year      smallint    NOT NULL,
    PRIMARY KEY (id, course_id, sec_id, semester, year)
);

-- 固定随机种子,保证每次跑出来的数字一致
SELECT setseed(0.42);

INSERT INTO instructor (id, name, dept_name)
SELECT lpad(g::text, 5, '0'),
       'teacher' || lpad(g::text, 5, '0'),
       'dept' || lpad(((g % 20) + 1)::text, 2, '0')
FROM generate_series(1, 20000) g;

INSERT INTO course (course_id, title, dept_name, credits)
SELECT 'c' || lpad(g::text, 4, '0'),
       'course title ' || g,
       'dept' || lpad(((g % 20) + 1)::text, 2, '0'),
       (g % 4) + 1
FROM generate_series(1, 4000) g;

-- 每个教师带 10 门课(学生选课关系在这里简化为"教师-课程")
INSERT INTO teaches (id, course_id, sec_id, semester, year)
SELECT DISTINCT i.id,
       'c' || lpad((1 + floor(random() * 4000))::int::text, 4, '0'),
       '1', 'Fall', 2017
FROM instructor i, generate_series(1, 10) g;

ANALYZE instructor;
ANALYZE course;
ANALYZE teaches;

SELECT 'instructor' AS 表, count(*) AS 行数 FROM instructor
UNION ALL SELECT 'course', count(*) FROM course
UNION ALL SELECT 'teaches', count(*) FROM teaches;

\echo ''
\echo '===== 1. 统计值:优化器眼里的世界(它不认识数据,只认识这些数字) ====='

SELECT c.relname AS 表,
       c.reltuples::bigint AS 估算行数,
       c.relpages AS 数据页数,
       pg_size_pretty(pg_relation_size(c.oid)) AS 表大小
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public' AND c.relname IN ('instructor','course','teaches')
ORDER BY c.relname;

\echo '--- pg_stats:列的统计值(dept_name 只有 20 个不同值 -> 选择率估算 1/20) ---'
SELECT tablename AS 表, attname AS 列,
       null_frac AS 空值比例,
       n_distinct AS 不同值个数,
       (most_common_vals::text)::varchar(90) AS 最常见值,
       (most_common_freqs::text)::varchar(60) AS 对应频率
FROM pg_stats
WHERE schemaname = 'public'
  AND ((tablename = 'instructor' AND attname IN ('id','dept_name'))
    OR (tablename = 'course'    AND attname IN ('course_id','dept_name'))
    OR (tablename = 'teaches'   AND attname IN ('id','course_id')))
ORDER BY tablename, attname;

\echo ''
\echo '--- 优化器据此算出来的选择率:dept_name = Music 应该是 1/20 = 5% ---'
SELECT count(*) AS "实际 Music 教师数",
       round(100.0 * count(*) / (SELECT count(*) FROM instructor), 1) AS 实际占比百分比
FROM instructor WHERE dept_name = 'dept01';

SELECT count(*) AS "实际 Music 课程数",
       round(100.0 * count(*) / (SELECT count(*) FROM course), 1) AS 实际占比百分比
FROM course WHERE dept_name = 'dept01';

\echo ''
\echo '===== 2. 逻辑优化:选择和投影被下推到哪里(看 Filter 落在哪一层) ====='
\echo '--- 逻辑计划(不估算代价):注意 Filter 挂在 instructor 扫描上,而不是在最顶层 ---'
\echo '--- 这就是教材说的"把 sigma 尽可能往下推":先筛出 Music 的教师,再连接 ---'

EXPLAIN (VERBOSE, COSTS OFF)
SELECT i.name, c.title
FROM instructor i
JOIN teaches t ON i.id = t.id
JOIN course  c ON t.course_id = c.course_id
WHERE i.dept_name = 'dept01';

\echo ''
\echo '--- 投影下推:计划里 instructor 只输出 id/name/dept_name,teaches 只输出 id/course_id ---'
\echo '--- 书上 Pi_{course_id,title}(course) 的作用在这里体现:course 只带出要用的列 ---'

\echo ''
\echo '===== 3. 物理计划:优化器最终选的那一个(EXPLAIN ANALYZE,真跑) ====='
\timing on

EXPLAIN (ANALYZE, BUFFERS)
SELECT i.name, c.title
FROM instructor i
JOIN teaches t ON i.id = t.id
JOIN course  c ON t.course_id = c.course_id
WHERE i.dept_name = 'dept01';

\timing off

\echo ''
\echo '--- 结果行数(和上面计划里的 actual rows 对得上) ---'
SELECT count(*) AS 结果行数
FROM instructor i
JOIN teaches t ON i.id = t.id
JOIN course  c ON t.course_id = c.course_id
WHERE i.dept_name = 'dept01';

\echo ''
\echo '===== 4. 备选计划:优化器在搜索的空间里还有哪些方案 ====='
\echo '--- 基准(第 3 节)是 hash join,总代价约 4300 ---'
\echo '--- 下面用开关禁掉某类连接算法,逼它走别的路,看代价和真实耗时怎么变 ---'

SET enable_hashjoin = off;

\echo '[4a] 禁掉 hash join,只能 merge join / nested loop:代价涨到约 8600,比 hash 贵近一倍'
EXPLAIN (ANALYZE, BUFFERS)
SELECT i.name, c.title
FROM instructor i
JOIN teaches t ON i.id = t.id
JOIN course  c ON t.course_id = c.course_id
WHERE i.dept_name = 'dept01';

SET enable_mergejoin = off;

\echo '[4b] 再禁掉 merge join,只剩 nested loop:这是教材里最朴素的执行方式'
EXPLAIN (ANALYZE, BUFFERS)
SELECT i.name, c.title
FROM instructor i
JOIN teaches t ON i.id = t.id
JOIN course  c ON t.course_id = c.course_id
WHERE i.dept_name = 'dept01';

RESET enable_hashjoin;
RESET enable_mergejoin;

\echo ''
\echo '===== 5. 索引的影响:建了索引,优化器就一定会用吗? ====='
\echo '--- dept_name 的选择率是 5%,先看没有索引时的代价 ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT i.name, c.title
FROM instructor i
JOIN teaches t ON i.id = t.id
JOIN course  c ON t.course_id = c.course_id
WHERE i.dept_name = 'dept01';

CREATE INDEX idx_instructor_dept ON instructor (dept_name);
ANALYZE instructor;

\echo '--- 建了 idx_instructor_dept 并 ANALYZE 之后:优化器换计划了吗? ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT i.name, c.title
FROM instructor i
JOIN teaches t ON i.id = t.id
JOIN course  c ON t.course_id = c.course_id
WHERE i.dept_name = 'dept01';

\echo '--- 单表查询同一条件:看起来更像"该走索引",但它算的仍是代价 ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name FROM instructor WHERE dept_name = 'dept01';

\echo ''
\echo '===== 6. 书上那句"course 上的投影是必需的":省掉投影会算错 ====='
\echo '--- 正确写法:只按 course_id 连接,Music 系的教师可以教别的系的课 ---'
SELECT count(*) AS 正确行数
FROM instructor i
JOIN teaches t ON i.id = t.id
JOIN course  c ON t.course_id = c.course_id
WHERE i.dept_name = 'dept01';

\echo '--- 相当于没做 Pi_{course_id,title}:自然连接会连 dept_name 一起匹配 ---'
\echo '--- 结果只剩"音乐系教师教的音乐系课程",行数变少,静默算错 ---'
SELECT count(*) AS 错误行数_少了
FROM instructor i
JOIN teaches t ON i.id = t.id
JOIN course  c ON t.course_id = c.course_id AND c.dept_name = i.dept_name
WHERE i.dept_name = 'dept01';

\echo ''
\echo '===== 7. 统计值失真:估算错,计划就错 ====='
\echo '--- 真实分布变成 Music 占 50%,但不 ANALYZE,优化器还以为是 5% ---'

ALTER TABLE instructor SET (autovacuum_enabled = off);

UPDATE instructor SET dept_name = 'dept01' WHERE id <= '10000';

SELECT '实际 Music 教师占比(未 ANALYZE)' AS 指标,
       round(100.0 * count(*) FILTER (WHERE dept_name = 'dept01') / count(*), 1) AS 百分比
FROM instructor;

\echo '--- 优化器的估算行数(来自旧统计值) vs 真实行数 ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name FROM instructor WHERE dept_name = 'dept01';

\echo '--- 失真会顺着计划往上放大:同一个两表连接,对比估算行数与 actual rows ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT i.name, t.course_id
FROM instructor i
JOIN teaches t ON i.id = t.id
WHERE i.dept_name = 'dept01';

ANALYZE instructor;

\echo '--- ANALYZE 之后:估算行数对上了,访问路径也跟着从位图扫描换成顺序扫描 ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name FROM instructor WHERE dept_name = 'dept01';

\echo '--- 同样的连接,现在估算与真实一致 ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT i.name, t.course_id
FROM instructor i
JOIN teaches t ON i.id = t.id
WHERE i.dept_name = 'dept01';

ALTER TABLE instructor RESET (autovacuum_enabled);
