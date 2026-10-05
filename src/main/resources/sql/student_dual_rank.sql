-- ============================================================
-- 一个 SELECT 语句中用两个 RANK 表达式, 同时得到"总名次"与"系内名次"
-- 数据源: dept_grades(ID, dept_name, GPA)
--
--   总名次   : RANK() OVER (ORDER BY GPA DESC)                       -- 不分区的全局排名
--   系内名次 : RANK() OVER (PARTITION BY dept_name ORDER BY GPA DESC) -- 按系分区的内部排名
--
-- 说明: 两个 RANK 表达式共享同一个 FROM 结果集, 各自独立计算窗口,
--       所以能在同一行里同时看到两种口径的名次。
-- ============================================================

-- 注: psql.bat / sql.bat 现在分别转发官方的 psql.exe / mysql.exe(元命令、$$、注释里的分号都能用)

SELECT '===== 双 RANK: 总名次(overall) 与 系内名次(dept) 同时输出 =====' AS section;
SELECT ID,
       dept_name,
       GPA                                                  AS ranking_basis,
       RANK() OVER (ORDER BY GPA DESC)                      AS overall_rank,
       RANK() OVER (PARTITION BY dept_name
                    ORDER BY GPA DESC)                      AS dept_rank
FROM dept_grades
ORDER BY overall_rank, dept_rank, ID;

-- ============================================================
-- 对照: 若想并列也"不跳号"的密集名次, 改用 DENSE_RANK()
-- ============================================================
SELECT '===== 对照: 两个 DENSE_RANK 表达式(并列不跳号) =====' AS section;
SELECT ID,
       dept_name,
       GPA                                                  AS ranking_basis,
       DENSE_RANK() OVER (ORDER BY GPA DESC)                AS overall_rank,
       DENSE_RANK() OVER (PARTITION BY dept_name
                          ORDER BY GPA DESC)                AS dept_rank
FROM dept_grades
ORDER BY overall_rank, dept_rank, ID;

-- ============================================================
-- 加: 关联学生姓名, 便于确认结果
-- ============================================================
SELECT '===== 直观版: 姓名 + 总名次 + 系内名次 =====' AS section;
SELECT s.name,
       s.ID,
       d.dept_name,
       d.GPA                                                AS ranking_basis,
       RANK() OVER (ORDER BY d.GPA DESC)                    AS overall_rank,
       RANK() OVER (PARTITION BY d.dept_name
                    ORDER BY d.GPA DESC)                    AS dept_rank
FROM dept_grades d
JOIN student s USING (ID)
ORDER BY overall_rank, dept_rank, s.ID;
