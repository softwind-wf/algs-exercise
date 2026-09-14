-- ============================================================
-- 用排名函数找"排名最高的 n 个元组"
--   原理: 窗口排名不能直接放进 WHERE, 必须先在内层算出名次,
--         再在外层(派生表/CTE)用 WHERE 名次 <= n 过滤。
-- 数据源: dept_grades(ID, dept_name, GPA)
-- ============================================================

\set ON_ERROR_STOP on

-- ============================================================
-- ① 总名次最高的 3 个学生(按 GPA 全局排名, rank<=3)
-- ============================================================
\echo '===== ① 全局排名最高的 3 个学生 (overall_rank <= 3) ====='
SELECT ID, dept_name, GPA, overall_rank
FROM (
    SELECT ID,
           dept_name,
           GPA,
           RANK() OVER (ORDER BY GPA DESC) AS overall_rank
    FROM dept_grades
) ranked
WHERE overall_rank <= 3
ORDER BY overall_rank, ID;

-- ============================================================
-- ② 每个系内排名最高的 2 个学生 (dept_rank <= 2, 按系分区)
-- ============================================================
\echo '===== ② 每个系内排名最高的 2 个学生 (dept_rank <= 2) ====='
SELECT ID, dept_name, GPA, dept_rank
FROM (
    SELECT ID,
           dept_name,
           GPA,
           RANK() OVER (PARTITION BY dept_name
                        ORDER BY GPA DESC) AS dept_rank
    FROM dept_grades
) ranked
WHERE dept_rank <= 2
ORDER BY dept_name, dept_rank, ID;

-- ============================================================
-- ③ 并列说明: RANK 的"前 n 名"可能多于 n 行(因为并列同号, 同分都算前 n)
--    若要恰好 n 行, 改用 ROW_NUMBER()
-- ============================================================
\echo '===== ③ 对照: ROW_NUMBER 的"恰好 3 行" (并列也被强行区分) ====='
SELECT ID, dept_name, GPA, seq
FROM (
    SELECT ID,
           dept_name,
           GPA,
           ROW_NUMBER() OVER (ORDER BY GPA DESC, ID) AS seq
    FROM dept_grades
) ranked
WHERE seq <= 3
ORDER BY seq;

\echo '===== ④ 对照: RANK 的"前 3 名"总行数(含并列) ====='
SELECT COUNT(*) AS rows_returned
FROM (
    SELECT ID,
           dept_name,
           GPA,
           RANK() OVER (ORDER BY GPA DESC) AS overall_rank
    FROM dept_grades
) ranked
WHERE overall_rank <= 3;
