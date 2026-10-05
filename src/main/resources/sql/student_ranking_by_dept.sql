-- ============================================================
-- 按系分区(dept_name)的学生排名 (university PostgreSQL)
-- 数据源: 上一轮的 dept_grades(ID, dept_name, GPA)
-- 排名: 在每个 dept_name 分区内, 按 GPA 降序排名
-- 并列处理: RANK() / DENSE_RANK() / ROW_NUMBER() 三种口径
-- ============================================================

-- 注: psql.bat / sql.bat 现在分别转发官方的 psql.exe / mysql.exe(元命令、$$、注释里的分号都能用)

-- ============================================================
-- ① RANK() 分区排名 —— 比赛排名(系内并列同号, 跳号)
-- ============================================================
SELECT '===== ① 按系分区, RANK(): 系内并列同号跳号 =====' AS section;
SELECT ID,
       dept_name,
       GPA                                          AS ranking_basis,
       RANK() OVER (PARTITION BY dept_name
                    ORDER BY GPA DESC)              AS rank
FROM dept_grades
ORDER BY dept_name, rank, ID;

-- ============================================================
-- ② DENSE_RANK() 分区排名 —— 密集排名(系内并列同号, 不跳号)
-- ============================================================
SELECT '===== ② 按系分区, DENSE_RANK(): 系内并列同号不跳号 =====' AS section;
SELECT ID,
       dept_name,
       GPA                                          AS ranking_basis,
       DENSE_RANK() OVER (PARTITION BY dept_name
                          ORDER BY GPA DESC)        AS rank
FROM dept_grades
ORDER BY dept_name, rank, ID;

-- ============================================================
-- ③ ROW_NUMBER() 分区排名 —— 系内连续序号(并列按 GPA,ID 二次排序)
-- ============================================================
SELECT '===== ③ 按系分区, ROW_NUMBER(): 系内强行区分并列 =====' AS section;
SELECT ID,
       dept_name,
       GPA                                          AS ranking_basis,
       ROW_NUMBER() OVER (PARTITION BY dept_name
                          ORDER BY GPA DESC, ID)    AS rank
FROM dept_grades
ORDER BY dept_name, rank, ID;

-- ============================================================
-- ④ 汇总: 每个系的学生数 + 系内各自排名(用 RANK)
--    展示哪些系内部出现了真正的"分区排名"
-- ============================================================
SELECT '===== ④ 各系学生数与系内排名(RANK) =====' AS section;
SELECT d.dept_name,
       COUNT(*) AS dept_students,
       array_agg(ID ORDER BY gpa DESC) AS ids
FROM dept_grades d
GROUP BY d.dept_name
ORDER BY d.dept_name;
