-- ============================================================
-- 更名运算(rename / ρ)演示：找出大学里的最高工资
-- 数据库: university (PostgreSQL 17)
-- 数据源: instructor(ID, name, dept_name, salary, phone_number)
-- 教材对照: 王珊《数据库系统概论》第5版 §2.4 关系代数 —— 更名运算 ρ
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
--   中间那个关系是把自己和自己做笛卡尔积，为了区分两份 instructor，
--   必须用更名运算 ρ 把副本改名成 d，于是同一个 salary 属性可以同时
--   以限定名 instructor.salary 与 d.salary 出现而不冲突。
--
-- SQL 里没有单独的 ρ 语法，它的对应物就是“别名”：
--   ρ_d(instructor)              ==>  FROM instructor AS d
--   ρ_d(A1,...,An)(E)  列更名   ==>  FROM instructor AS d(A1,...,An)   (PostgreSQL 专有)
--                                    或 SELECT e.a AS A1, ... (MySQL 只能这么写)
--
-- 运行:
--   .\psql.bat -f src\main\resources\sql\rename_max_salary.sql
--
-- 实测结论(PostgreSQL 17.11, university 库): 13 名教师, 最高工资 100000.00
--   (25566 Brown / Biology)，步骤 4、5、6、7 与一致性核对全部吻合。
--   并列最高也已实测: 在事务内把 Einstein 的工资临时改成 100000 后，
--   步骤 5 / 步骤 6 均返回 Brown、Einstein 两行，而步骤 4 作为集合运算
--   只返回一个值 100000.00 —— 这正是“集合差求值 + 再连回原表求人”的差别。
-- ============================================================


-- ============================================================
-- 步骤 0a：更名后的副本与原件“同值不同名”
--   ρ_d(instructor) 产生一张结构、数据完全相同、只是名字叫 d 的表。
--   下面用 UNION ALL 把两份并排列出，肉眼可见二者逐行一致。
-- ============================================================
SELECT 'instructor'  AS relation_name, ID, name, dept_name, salary FROM instructor
UNION ALL
SELECT 'd (renamed)' AS relation_name, d.ID, d.name, d.dept_name, d.salary FROM instructor AS d
ORDER BY relation_name, salary DESC;


-- ============================================================
-- 步骤 0b：更名运算的完整形式 ρ_d(A1,...,An)(E) —— 连属性一起改名
--   instructor 的 5 个属性被依次改名为 i_id / i_name / i_dept / i_salary / i_phone，
--   此后必须用新属性名引用它们。
--   注意：表别名后直接跟列别名列表是 PostgreSQL 语法，MySQL 不支持，
--         MySQL 版脚本改用 SELECT d.ID AS i_id, ... 达到同一效果。
-- ============================================================
SELECT d.i_id, d.i_name, d.i_dept, d.i_salary
FROM instructor AS d (i_id, i_name, i_dept, i_salary, i_phone)
ORDER BY d.i_salary DESC;


-- ============================================================
-- 步骤 1：笛卡尔积 instructor × ρ_d(instructor)
--   13 × 13 = 169 个元组。没有 ρ 就没法写这张自乘的表。
-- ============================================================
SELECT COUNT(*) AS cartesian_tuples
FROM instructor, instructor AS d;


-- ============================================================
-- 步骤 2：选择 σ_instructor.salary < d.salary ( 步骤 1 的结果 )
--   每一对 (i, d) 都读作“教师 i 的工资比教师 d 低”，
--   即 i 不是最高工资。只列前 10 对示意。
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
--   SQL 的对应物是 EXCEPT (Oracle 写作 MINUS，标准 SQL 用 EXCEPT)。
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
--   把步骤 4 的结果再与 instructor 做选择，得到完整元组。
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
--   这种写法不需要集合差，且并列最高时会如实返回多行。
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
