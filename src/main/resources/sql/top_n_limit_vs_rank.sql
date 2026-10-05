-- ============================================================
-- LIMIT vs 排名+外层过滤: 各自能做什么、做不到什么
-- 数据源: dept_grades(ID, dept_name, GPA)
-- ============================================================

-- 注: psql.bat / sql.bat 现在分别转发官方的 psql.exe / mysql.exe(元命令、$$、注释里的分号都能用)

-- ============================================================
-- ① 全局前 3: LIMIT 最简单, 一行搞定
-- ============================================================
SELECT '===== ① 全局前3: ORDER BY GPA DESC LIMIT 3 =====' AS section;
SELECT ID, dept_name, GPA
FROM dept_grades
ORDER BY GPA DESC
LIMIT 3;

-- ============================================================
-- ② LIMIT 是"恰好 N 行", 不反映并列名次； 加上 RANK 才能看到名次
-- ============================================================
SELECT '===== ② LIMIT 3 同时显示 RANK 名次(恰好3行) =====' AS section;
SELECT ID, dept_name, GPA,
       RANK() OVER (ORDER BY GPA DESC) AS overall_rank
FROM dept_grades
ORDER BY overall_rank
LIMIT 3;

-- ============================================================
-- ③ 每系前 2: LIMIT 做不到 —— 单条 ORDER BY GPA DESC LIMIT 2 只会取全局前2,
--    不是"每个系各取前2"
-- ============================================================
SELECT '===== ③ LIMIT 2 (只会全局前2, 不能按系) =====' AS section;
SELECT ID, dept_name, GPA
FROM dept_grades
ORDER BY GPA DESC
LIMIT 2;

SELECT '===== ③b 每系前2: 必须 RANK 分区 + 外层过滤 =====' AS section;
SELECT ID, dept_name, GPA, dept_rank
FROM (
    SELECT ID, dept_name, GPA,
           RANK() OVER (PARTITION BY dept_name ORDER BY GPA DESC) AS dept_rank
    FROM dept_grades
) ranked
WHERE dept_rank <= 2
ORDER BY dept_name, dept_rank, ID;
