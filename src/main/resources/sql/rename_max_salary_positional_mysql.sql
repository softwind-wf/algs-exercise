-- ============================================================
-- 更名运算的“位置标记(属性序号)”实现 —— 找出大学里的最高工资  —— MySQL 8.0 版
-- 数据库: university (MySQL 8.0.44)
-- 数据源: instructor(ID, name, dept_name, salary, phone_number)
-- 姊妹脚本: rename_max_salary_mysql.sql(属性名版) / rename_max_salary_positional.sql(PostgreSQL 版)
-- ------------------------------------------------------------
-- 属性名版(上一版)的表达式:
--   Π_salary(instructor)
--     − Π_instructor.salary( σ_instructor.salary < d.salary ( instructor × ρ_d(instructor) ) )
--
-- 本版改用位置标记 $i 表示第 i 个属性:
--   Π_$4(instructor)  −  Π_$4( σ_$4 < $9 ( instructor × instructor ) )
--
--   instructor 的属性顺序: $1=ID  $2=name  $3=dept_name  $4=salary  $5=phone_number
--   自乘后共 10 个属性, 第二个 instructor 的 salary 落在第 9 位, 即 $9。
--
-- 要点: 位置标记天然把两个 salary 区分开(第 4 位与第 9 位), 因此这一版
--   可以省掉更名运算 ρ —— 这正是位置标记的用处, 两个写法语义完全等价。
--
-- SQL 里没有 $i 这样的属性引用语法(只有 ORDER BY / GROUP BY 允许写序号，
-- SELECT / WHERE 必须写列名)，所以用 CTE 的列名清单按位置改名来模拟:
--   WITH r (c1, c2, c3, c4, c5) AS (SELECT ID, name, dept_name, salary, phone_number FROM instructor)
-- 此后 c1..c5 即 $1..$5, c9 即 $9。MySQL 8.0 支持 CTE 与 EXCEPT(8.0.31 起)。
--
-- 运行:
--   .\sql.bat -db university -f src\main\resources\sql\rename_max_salary_positional_mysql.sql
--
-- 实测结论(MySQL 8.0.44, university 库): 12 名教师, 最高工资 95000.00
--   (22222 Einstein / Physics)，与属性名版逐项一致。
-- ============================================================


-- ============================================================
-- 步骤 0：位置化关系 r($1..$5)
--   CTE 的列名清单按“位置”给属性改名，c4 就是原来的 salary。
-- ============================================================
WITH r (c1, c2, c3, c4, c5) AS (
    SELECT ID, name, dept_name, salary, phone_number FROM instructor
)
SELECT c1 AS '$1=ID', c2 AS '$2=name', c3 AS '$3=dept', c4 AS '$4=salary', c5 AS '$5=phone'
FROM r
ORDER BY c4 DESC;


-- ============================================================
-- 步骤 1：笛卡尔积 instructor × instructor —— 位置自动编号到 $10
--   第一个操作数占 $1..$5，第二个操作数占 $6..$10。
--   这正是“位置标记能省掉更名运算”的原因：两个 salary 位次不同，
--   不会像属性名那样撞车。12 × 12 = 144 个元组。
-- ============================================================
WITH r (c1, c2, c3, c4, c5) AS (
    SELECT ID, name, dept_name, salary, phone_number FROM instructor
),
prod (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10) AS (
    SELECT x.c1, x.c2, x.c3, x.c4, x.c5,
           y.c1, y.c2, y.c3, y.c4, y.c5
    FROM r AS x, r AS y
)
SELECT COUNT(*) AS cartesian_tuples FROM prod;


-- ============================================================
-- 步骤 2：选择 σ_$4 < $9 ( 步骤 1 的结果 )
--   读作“第 4 位(我的工资) 小于 第 9 位(对方的工资)”，即我不是最高工资。
-- ============================================================
WITH r (c1, c2, c3, c4, c5) AS (
    SELECT ID, name, dept_name, salary, phone_number FROM instructor
),
prod (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10) AS (
    SELECT x.c1, x.c2, x.c3, x.c4, x.c5,
           y.c1, y.c2, y.c3, y.c4, y.c5
    FROM r AS x, r AS y
)
SELECT c1 AS '$1=i_id', c4 AS '$4=i_salary', c6 AS '$6=d_id', c9 AS '$9=d_salary'
FROM prod
WHERE c4 < c9
ORDER BY c4 DESC, c9
LIMIT 10;


-- ============================================================
-- 步骤 3：投影 Π_$4 ( 步骤 2 的结果 )
--   即“不是最高工资”的全部工资值。
-- ============================================================
WITH r (c1, c2, c3, c4, c5) AS (
    SELECT ID, name, dept_name, salary, phone_number FROM instructor
),
prod (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10) AS (
    SELECT x.c1, x.c2, x.c3, x.c4, x.c5,
           y.c1, y.c2, y.c3, y.c4, y.c5
    FROM r AS x, r AS y
)
SELECT DISTINCT c4 AS '$4=not_max_salary'
FROM prod
WHERE c4 < c9
ORDER BY 1 DESC;


-- ============================================================
-- 步骤 4：集合差 —— 答案
--   Π_$4(instructor) − Π_$4( σ_$4<$9 (instructor × instructor) )
-- ============================================================
WITH r (c1, c2, c3, c4, c5) AS (
    SELECT ID, name, dept_name, salary, phone_number FROM instructor
),
prod (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10) AS (
    SELECT x.c1, x.c2, x.c3, x.c4, x.c5,
           y.c1, y.c2, y.c3, y.c4, y.c5
    FROM r AS x, r AS y
),
not_max (c4) AS (
    SELECT DISTINCT c4 FROM prod WHERE c4 < c9
)
SELECT c4 AS '$4=max_salary'
FROM r
EXCEPT
SELECT c4 FROM not_max;


-- ============================================================
-- 步骤 5：把位置换回“人”
--   Π_{$2,$3,$4}( r ⋈ (上面的差集) )，用 IN 实现，并列最高也能全部列出。
-- ============================================================
WITH r (c1, c2, c3, c4, c5) AS (
    SELECT ID, name, dept_name, salary, phone_number FROM instructor
),
prod (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10) AS (
    SELECT x.c1, x.c2, x.c3, x.c4, x.c5,
           y.c1, y.c2, y.c3, y.c4, y.c5
    FROM r AS x, r AS y
),
not_max (c4) AS (
    SELECT DISTINCT c4 FROM prod WHERE c4 < c9
),
maximal (c4) AS (
    SELECT c4 FROM r
    EXCEPT
    SELECT c4 FROM not_max
)
SELECT r.c1 AS id, r.c2 AS name, r.c3 AS dept_name, r.c4 AS salary
FROM r
WHERE r.c4 IN (SELECT c4 FROM maximal)
ORDER BY r.c1;


-- ============================================================
-- 步骤 6：位置标记在元组关系演算里的对应写法
--   { t | t ∈ instructor ∧ ¬∃ u ∈ instructor ( u[4] > t[4] ) }
--   t[4] / u[4] 就是位置标记（第 4 个分量），SQL 的相关子查询是它的直接翻译。
-- ============================================================
WITH r (c1, c2, c3, c4, c5) AS (
    SELECT ID, name, dept_name, salary, phone_number FROM instructor
)
SELECT t.c1 AS id, t.c2 AS name, t.c3 AS dept_name, t.c4 AS salary
FROM r AS t
WHERE NOT EXISTS (SELECT 1 FROM r AS u WHERE u.c4 > t.c4)
ORDER BY t.c1;


-- ============================================================
-- 步骤 7：对照实现
--   SQL 里真正支持“位置标记”的地方是 ORDER BY / GROUP BY 的序号，
--   SELECT / WHERE 不支持，所以上面只能用 CTE 列名清单来模拟。
-- ============================================================
SELECT MAX(salary) AS max_salary_agg FROM instructor;

SELECT ID, name, dept_name, salary
FROM instructor
ORDER BY 4 DESC, 1
LIMIT 1;


-- ============================================================
-- 一致性核对：位置标记解法 与 聚合解法 必须相等
-- ============================================================
WITH r (c1, c2, c3, c4, c5) AS (
    SELECT ID, name, dept_name, salary, phone_number FROM instructor
),
prod (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10) AS (
    SELECT x.c1, x.c2, x.c3, x.c4, x.c5,
           y.c1, y.c2, y.c3, y.c4, y.c5
    FROM r AS x, r AS y
),
not_max (c4) AS (
    SELECT DISTINCT c4 FROM prod WHERE c4 < c9
)
SELECT (SELECT MAX(c4) FROM r) AS via_agg,
       (SELECT c4 FROM r EXCEPT SELECT c4 FROM not_max) AS via_position;
