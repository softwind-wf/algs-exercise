-- ============================================================
-- 广义投影运算(Generalized Projection)演示 —— 教师工资普调
-- 数据库: university (PostgreSQL 17)
-- 数据源: instructor(ID, name, dept_name, salary, phone_number)
-- 姊妹脚本: generalized_projection_mysql.sql(MySQL 8.0 等价版)
-- 相关脚本: rename_max_salary.sql(更名运算 ρ) / rename_max_salary_positional.sql(位置标记)
-- ------------------------------------------------------------
-- 定义(《数据库系统概念》原书第6版·本科教学版, Silberschatz / Korth / Sudarshan,
--   机械工业出版社 · 第6章 形式化关系查询语言 · 6.1 关系代数 · 投影运算):
--   普通投影  Π_{A1,...,An}(E)          —— Ai 只能是属性名
--   广义投影  Π_{F1,...,Fn}(E)          —— Fi 可以是“属性名、常量与算术运算符
--                                          ( + - * / ) 组成的表达式”
--   即：投影的同时对每个元组做一次逐行计算，结果关系的属性名由表达式决定，
--       表达式里还能出现常量和新算出来的列。
--
-- 本脚本的具体例子：给每位教师普调工资 10%，输出编号、姓名、原工资、新工资、
--   增额与工龄档位，即
--
--     Π_{ID, name, dept_name, salary, salary*1.1, salary*1.1−salary}(instructor)
--
-- SQL 里没有单独的“广义投影”符号，它的对应物就是 SELECT 列表中的表达式
--   (算术式、常量、CASE、函数)，等价写法是投影后再用 ρ 给结果属性改名。
--
-- 运行:
--   .\psql.bat -f src\main\resources\sql\generalized_projection.sql
--
-- 实测结论(PostgreSQL 17.11, university 库): 13 行输入 → 13 行输出(广义投影
--   不改变元组个数)，最高新工资 110000.00(Brown)，Comp. Sci. 系三人涨后
--   分别为 101200.00 / 82500.00 / 71500.00，与聚合核对一致。
-- ============================================================


-- ============================================================
-- 步骤 0：原始关系(计算前的底表)
-- ============================================================
SELECT ID, name, dept_name, salary
FROM instructor
ORDER BY salary DESC;


-- ============================================================
-- 步骤 1：普通投影 Π_{ID, name, salary}(instructor) —— 只含属性名
--   作为对照：投影列表里全是现成的列，没有任何计算。
-- ============================================================
SELECT ID, name, salary
FROM instructor
ORDER BY salary DESC;


-- ============================================================
-- 步骤 2：广义投影 Π_{ID, name, salary*1.1}(instructor) —— 最小例子
--   投影列表第 3 项是一个算术表达式，输出的是“涨薪后的工资”。
--   注意：关系代数里结果的属性名就是表达式 salary*1.1，所以 SQL 用 AS 起名。
-- ============================================================
SELECT ID,
       name,
       salary * 1.1 AS "salary*1.1"
FROM instructor
ORDER BY 3 DESC;


-- ============================================================
-- 步骤 3：完整的广义投影 —— 多个表达式 + 常量一起出现在投影列表
--     Π_{ID, name, salary, salary*1.1, salary*1.1−salary, 2026}(instructor)
--   第 6 项是常量 2026('2026' 作为年份列)，常量同样是合法的 Fi。
--   四舍五入只是为了好读，算术本身由数据库完成。
-- ============================================================
SELECT ID,
       name,
       salary                              AS old_salary,
       ROUND(salary * 1.1, 2)              AS "salary*1.1",
       ROUND(salary * 1.1 - salary, 2)     AS raise,
       2026                                AS "2026"
FROM instructor
ORDER BY old_salary DESC;


-- ============================================================
-- 步骤 4：表达式里带条件 —— 用 CASE 分档(广义投影的常见扩展写法)
--     Π_{ID, name, salary, 档位}(instructor)
--   分档规则：>= 90000 为 A，>= 70000 为 B，其余为 C。
-- ============================================================
SELECT ID,
       name,
       salary,
       CASE WHEN salary >= 90000 THEN 'A'
            WHEN salary >= 70000 THEN 'B'
            ELSE 'C'
       END AS salary_level
FROM instructor
ORDER BY salary DESC, ID;


-- ============================================================
-- 步骤 5：广义投影 + 选择 + 更名 组合运算
--   只给 Comp. Sci. 系的教师涨薪，并把结果的属性改名：
--     ρ_{result(ID, name, new_salary)}( Π_{ID, name, salary*1.1}( σ_{dept='Comp. Sci.'}(instructor) ) )
--   SQL 里 ρ 的对应物同样是“按位置列出的列名清单”(上一课的写法)。
-- ============================================================
WITH result (ID, name, new_salary) AS (
    SELECT ID,
           name,
           ROUND(salary * 1.1, 2)
    FROM instructor
    WHERE dept_name = 'Comp. Sci.'
)
SELECT ID, name, new_salary
FROM result
ORDER BY new_salary DESC;


-- ============================================================
-- 步骤 6：把广义投影的结果当成一个新关系继续运算
--   先算出“涨薪后的关系”，再对它做投影 + 选择，验证它可以像普通表一样被引用。
-- ============================================================
WITH adjusted (ID, name, new_salary) AS (
    SELECT ID, name, ROUND(salary * 1.1, 2)
    FROM instructor
)
SELECT name, new_salary
FROM adjusted
WHERE new_salary >= 100000
ORDER BY new_salary DESC;


-- ============================================================
-- 步骤 7：集合语义提醒 —— 广义投影同样会自动消除重复元组
--   Π_{salary−salary}(instructor) 每个元组算出来都是 0，关系代数里
--   投影结果是集合，最终只有一个元组 <0>。SQL 默认保留重复行，
--   必须加 DISTINCT 才与关系代数的语义一致。
-- ============================================================
SELECT DISTINCT salary - salary AS "salary-salary"
FROM instructor;

SELECT salary - salary AS "salary-salary_no_distinct"
FROM instructor
ORDER BY 1;


-- ============================================================
-- 步骤 8：对照实现 —— 广义投影 与 先取数再算 / 聚合 的结果应一致
-- ============================================================
SELECT SUM(salary)                    AS total_old,
       ROUND(SUM(salary) * 1.1, 2)    AS total_new_by_agg,
       ROUND(SUM(salary * 1.1), 2)    AS total_new_by_projection
FROM instructor;
