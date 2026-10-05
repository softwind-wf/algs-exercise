-- ============================================================
-- 验证: RANK(可带 PARTITION) 与 GROUP BY 同现时的执行顺序
--   顺序: FROM → WHERE → GROUP BY(聚集) → HAVING → SELECT聚集
--         → 窗口函数(RANK/PARTITION) → ORDER BY
--   含义: GROUP BY 先把行聚合成组并算出聚集值, 窗口再在"分组后的结果"
--         上排名, 因此聚集值(如 GPA)可以直接作为 RANK 的依据。
-- 说明: 窗口函数不能用同一 SELECT 里的别名(如 gpa)作为 OVER 的列,
--       必须引用聚集表达式本身(下面 ①), 或先分组再在外层排名(下面 ②)。
-- ============================================================

-- 注: psql.bat / sql.bat 现在分别转发官方的 psql.exe / mysql.exe(元命令、$$、注释里的分号都能用)

-- ============================================================
-- ① 单条 SELECT: GROUP BY 先算 GPA(聚集), 再在同一个 OVER 里按 GPA 排名
--    总名次 + 系内名次, 都引用"分组后算出的 GPA 聚集表达式"
-- ============================================================
SELECT '===== ① 单条 SELECT: GROUP BY 算聚集, RANK 用该聚集值(总+系内) =====' AS section;
SELECT t.ID,
       st.dept_name,
       SUM(c.credits * CASE t.grade
            WHEN 'A+' THEN 4.0 WHEN 'A' THEN 4.0 WHEN 'A-' THEN 3.7
            WHEN 'B+' THEN 3.3 WHEN 'B' THEN 3.0 WHEN 'B-' THEN 2.7
            WHEN 'C+' THEN 2.3 WHEN 'C' THEN 2.0 WHEN 'C-' THEN 1.7
            WHEN 'D+' THEN 1.3 WHEN 'D' THEN 1.0 WHEN 'D-' THEN 0.7
            ELSE 0.0 END) / SUM(c.credits)              AS gpa,
       RANK() OVER (ORDER BY
            SUM(c.credits * CASE t.grade
            WHEN 'A+' THEN 4.0 WHEN 'A' THEN 4.0 WHEN 'A-' THEN 3.7
            WHEN 'B+' THEN 3.3 WHEN 'B' THEN 3.0 WHEN 'B-' THEN 2.7
            WHEN 'C+' THEN 2.3 WHEN 'C' THEN 2.0 WHEN 'C-' THEN 1.7
            WHEN 'D+' THEN 1.3 WHEN 'D' THEN 1.0 WHEN 'D-' THEN 0.7
            ELSE 0.0 END) / SUM(c.credits) DESC)        AS overall_rank,
       RANK() OVER (PARTITION BY st.dept_name ORDER BY
            SUM(c.credits * CASE t.grade
            WHEN 'A+' THEN 4.0 WHEN 'A' THEN 4.0 WHEN 'A-' THEN 3.7
            WHEN 'B+' THEN 3.3 WHEN 'B' THEN 3.0 WHEN 'B-' THEN 2.7
            WHEN 'C+' THEN 2.3 WHEN 'C' THEN 2.0 WHEN 'C-' THEN 1.7
            WHEN 'D+' THEN 1.3 WHEN 'D' THEN 1.0 WHEN 'D-' THEN 0.7
            ELSE 0.0 END) / SUM(c.credits) DESC)        AS dept_rank
FROM takes t
JOIN course c USING (course_id)
JOIN student st USING (ID)
WHERE t.grade IS NOT NULL
GROUP BY t.ID, st.dept_name
ORDER BY overall_rank, dept_rank, t.ID;

-- ============================================================
-- ② 用 CTE 先 GROUP BY 算出聚集值, 再在下一层按它排名(更易读, 顺序一致)
--    CTE 中完成 FROM→WHERE→GROUP BY→聚集； 外层在分组结果上做窗口排名
-- ============================================================
SELECT '===== ② CTE: 先 GROUP BY 算 GPA, 再按 GPA 排名(总+系内) =====' AS section;
WITH per_student AS (
    SELECT t.ID,
           st.dept_name,
           SUM(c.credits * CASE t.grade
                WHEN 'A+' THEN 4.0 WHEN 'A' THEN 4.0 WHEN 'A-' THEN 3.7
                WHEN 'B+' THEN 3.3 WHEN 'B' THEN 3.0 WHEN 'B-' THEN 2.7
                WHEN 'C+' THEN 2.3 WHEN 'C' THEN 2.0 WHEN 'C-' THEN 1.7
                WHEN 'D+' THEN 1.3 WHEN 'D' THEN 1.0 WHEN 'D-' THEN 0.7
                ELSE 0.0 END) / SUM(c.credits) AS gpa
    FROM takes t
    JOIN course c USING (course_id)
    JOIN student st USING (ID)
    WHERE t.grade IS NOT NULL
    GROUP BY t.ID, st.dept_name
)
SELECT ID,
       dept_name,
       gpa                                              AS ranking_basis,
       RANK() OVER (ORDER BY gpa DESC)                  AS overall_rank,
       RANK() OVER (PARTITION BY dept_name
                    ORDER BY gpa DESC)                  AS dept_rank
FROM per_student
ORDER BY overall_rank, dept_rank, ID;
