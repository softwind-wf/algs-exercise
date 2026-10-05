-- ============================================================
-- PERCENT_RANK() 演示
--   公式: (RANK() - 1) / (分区行数 - 1)   -> 结果在 [0, 1]
--   语义: 该行名次在分区内的"百分位"位置； 第一名=0, 最后一名=1
--   与 RANK() 同源(名次跳号), 但给的是 0~1 的相对比例而非绝对名次
-- 数据源: dept_grades(ID, dept_name, GPA)
-- ============================================================

-- 注: psql.bat / sql.bat 现在分别转发官方的 psql.exe / mysql.exe(元命令、$$、注释里的分号都能用)

-- ============================================================
-- ① 全校(整体) PERCENT_RANK, 按 GPA 降序
-- ============================================================
SELECT '===== ① 整体 PERCENT_RANK(按GPA降序) =====' AS section;
SELECT ID,
       dept_name,
       GPA                                           AS ranking_basis,
       RANK()      OVER (ORDER BY GPA DESC)          AS rank_,
       PERCENT_RANK() OVER (ORDER BY GPA DESC)       AS percent_rank
FROM dept_grades
ORDER BY rank_, ID;

-- ============================================================
-- ② 按系分区 PERCENT_RANK(每个系内各自 0~1)
-- ============================================================
SELECT '===== ② 按系分区 PERCENT_RANK =====' AS section;
SELECT ID,
       dept_name,
       GPA                                           AS ranking_basis,
       RANK()      OVER (PARTITION BY dept_name
                         ORDER BY GPA DESC)          AS dept_rank,
       PERCENT_RANK() OVER (PARTITION BY dept_name
                            ORDER BY GPA DESC)       AS dept_percent_rank
FROM dept_grades
ORDER BY dept_name, dept_rank, ID;

-- ============================================================
-- ③ 对照区: 单行分区(如 Biology 只有1人)时 PERCENT_RANK 行为
--    分母=行数-1=0, PostgreSQL 返回 0(避免除零)
-- ============================================================
SELECT '===== ③ 单生系(Biology/Finance/History/Music) PERCENT_RANK 均为 0 =====' AS section;
SELECT ID,
       dept_name,
       GPA                                           AS ranking_basis,
       PERCENT_RANK() OVER (PARTITION BY dept_name
                            ORDER BY GPA DESC)       AS dept_percent_rank
FROM dept_grades
WHERE dept_name IN ('Biology','Finance','History','Music')
ORDER BY dept_name;

-- ============================================================
-- ④ 顺带展示 CUME_DIST(累积分布) 作对比: 该值= "<=当前值的行数/总行数"
--   与 PERCENT_RANK 不同, CUME_DIST 有并列时同值, 且首名不为 0
-- ============================================================
SELECT '===== ④ 对照: CUME_DIST vs PERCENT_RANK(整体) =====' AS section;
SELECT ID,
       GPA                                           AS ranking_basis,
       PERCENT_RANK() OVER (ORDER BY GPA DESC)       AS percent_rank,
       CUME_DIST()   OVER (ORDER BY GPA DESC)        AS cume_dist
FROM dept_grades
ORDER BY GPA DESC, ID;
