-- ============================================================
-- 更名运算(rename / ρ)演示：找出大学里的最高工资  —— MySQL 8.0 版
-- 数据库: university (MySQL 8.0.44)
-- 数据源: instructor(ID, name, dept_name, salary, phone_number)
-- 教材对照: 《数据库系统概念》(原书第6版·本科教学版, Silberschatz / Korth / Sudarshan,
--           机械工业出版社) 第6章 形式化关系查询语言 · 6.1 关系代数 —— 更名运算 ρ。
--           “找出大学里的最高工资”正是该节的例子, 书中投影符号写作 π(本脚本写作 Π)。
--           注意: 书上 instructor 只有 4 个属性(ID, name, dept_name, salary), 本库多一列
--           phone_number, 不影响本脚本(投影列表按属性名引用)。
-- 姊妹脚本: src/main/resources/sql/rename_max_salary.sql (PostgreSQL 版)
-- ------------------------------------------------------------
-- 关系代数标准解：
--
--     Π_salary(instructor)
--        −
--     Π_instructor.salary(
--         σ_instructor.salary < d.salary ( instructor × ρ_d(instructor) )
--     )
--
-- 思路：最高工资 = “全部工资”减去“不是最高的工资”。
--   中间的关系把自己和自己做笛卡尔积，必须先用更名运算 ρ 把副本改名为 d，
--   同一个 salary 属性才能以 instructor.salary / d.salary 两个限定名并存。
--
-- SQL 里没有单独的 ρ 语法，它的对应物就是“别名”：
--   ρ_d(instructor)             ==>  FROM instructor AS d
--   ρ_d(A1,...,An)(E)  列更名  ==>  MySQL 不支持“表别名(列别名列表)”，
--                                   只能写成 SELECT d.ID AS i_id, ... (见步骤 0b)
--
-- 运行:
--   .\sql.bat -db university -f src\main\resources\sql\rename_max_salary_mysql.sql
--
-- 实测结论(MySQL 8.0.44, university 库): 12 名教师, 最高工资 95000.00
--   (22222 Einstein / Physics)，步骤 4、5、6、7 与一致性核对全部吻合。
--   MySQL 8.0.31 起才支持 EXCEPT，本机 8.0.44 可直接使用。
-- ============================================================


-- ============================================================
-- 步骤 0a：更名后的副本与原件“同值不同名”
--   ρ_d(instructor) 产生一张结构、数据完全相同、只是名字叫 d 的表。
-- ============================================================
SELECT 'instructor'  AS relation_name, ID, name, dept_name, salary FROM instructor
UNION ALL
SELECT 'd (renamed)' AS relation_name, d.ID, d.name, d.dept_name, d.salary FROM instructor AS d
ORDER BY relation_name, salary DESC;


-- ============================================================
-- 步骤 0b：更名运算的完整形式 ρ_d(A1,...,An)(E) —— 连属性一起改名
--   PostgreSQL 可写 FROM instructor AS d(i_id, i_name, ...)，
--   MySQL 没有这个语法，等价写法是把列别名放进 SELECT 列表。
-- ============================================================
SELECT d.ID        AS i_id,
       d.name      AS i_name,
       d.dept_name AS i_dept,
       d.salary    AS i_salary
FROM instructor AS d
ORDER BY d.salary DESC;


-- ============================================================
-- 步骤 1：笛卡尔积 instructor × ρ_d(instructor)
--   12 × 12 = 144 个元组。没有 ρ 就没法写这张自乘的表。
-- ============================================================
SELECT COUNT(*) AS cartesian_tuples
FROM instructor, instructor AS d;


-- ============================================================
-- 步骤 2：选择 σ_instructor.salary < d.salary ( 步骤 1 的结果 )
--   每一对 (i, d) 都读作“教师 i 的工资比教师 d 低”，即 i 不是最高工资。
-- ============================================================
SELECT i.ID AS i_id, i.salary AS i_salary, d.ID AS d_id, d.salary AS d_salary
FROM instructor AS i, instructor AS d
WHERE i.salary < d.salary
ORDER BY i.salary DESC, d.salary
LIMIT 10;


-- ============================================================
-- 步骤 3：投影 Π_instructor.salary ( 步骤 2 的结果 )
--   得到“不是最高工资”的全部工资值(投影后按集合语义去重)。
-- ============================================================
SELECT DISTINCT i.salary AS not_max_salary
FROM instructor AS i, instructor AS d
WHERE i.salary < d.salary
ORDER BY not_max_salary DESC;


-- ============================================================
-- 步骤 4：集合差 —— 答案
--   Π_salary(instructor) 减去步骤 3 的结果，剩下的就是最高工资。
--   MySQL 8.0.31+ 支持 EXCEPT (Oracle 写作 MINUS)。
-- ============================================================
SELECT salary AS max_salary
FROM (
    SELECT salary FROM instructor
    EXCEPT
    SELECT i.salary
    FROM instructor AS i, instructor AS d
    WHERE i.salary < d.salary
) diff
ORDER BY max_salary;


-- ============================================================
-- 步骤 5：把工资还原成“人” —— 找出工资最高的教师
--   用 IN 而不是等值连接，这样即使最高工资并列也能全部列出。
-- ============================================================
SELECT i.ID, i.name, i.dept_name, i.salary
FROM instructor AS i
WHERE i.salary IN (
    SELECT salary FROM instructor
    EXCEPT
    SELECT i2.salary
    FROM instructor AS i2, instructor AS d
    WHERE i2.salary < d.salary
)
ORDER BY i.ID;


-- ============================================================
-- 步骤 6：更名的另一种常见写法 —— 相关子查询 + NOT EXISTS
--   子查询里的别名 d 同样是一次 ρ 更名，语义与步骤 4 完全等价：
--   “不存在工资比我高的人” ⟺ “我的工资是最高的”。
-- ============================================================
SELECT i.ID, i.name, i.dept_name, i.salary
FROM instructor AS i
WHERE NOT EXISTS (
    SELECT 1 FROM instructor AS d WHERE d.salary > i.salary
)
ORDER BY i.ID;


-- ============================================================
-- 步骤 7：对照实现 —— 用聚合/排序直接求最高工资
--   三种写法结果应完全一致，但只有步骤 4/6 体现了更名运算。
-- ============================================================
SELECT MAX(salary) AS max_salary_agg FROM instructor;

SELECT ID, name, dept_name, salary
FROM instructor
ORDER BY salary DESC, ID
LIMIT 1;


-- ============================================================
-- 一致性核对：更名解法 与 聚合解法 必须相等
-- ============================================================
SELECT MAX(salary) AS via_agg,
       (SELECT salary FROM instructor
        EXCEPT
        SELECT i.salary FROM instructor AS i, instructor AS d WHERE i.salary < d.salary) AS via_rename
FROM instructor;
