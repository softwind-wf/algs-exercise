-- ============================================================
-- CUME_DIST() 演示 (累积分布, cumulative distribution)
--   公式: 排序方向下 "<=当前值的行数"  /  分区总行数
--   语义: 该行处于分区内累积比例的位置；  区间 (0,1], 最后一名恒为 1
--   与 PERCENT_RANK 的区别:
--       - 首名: PERCENT_RANK=0,  CUME_DIST=(并列条数/总数)>0
--       - 末名: 两者都=1
--       - 并列: 都取同值, 但 CUME_DIST 取"并列最大累计比例"
-- 数据源: dept_grades(ID, dept_name, GPA)
-- ============================================================

-- 注: psql.bat / sql.bat 现在分别转发官方的 psql.exe / mysql.exe(元命令、$$、注释里的分号都能用)

-- ============================================================
-- ① 全校(整体) CUME_DIST, 按 GPA 降序
-- ============================================================
SELECT '===== ① 整体 CUME_DIST(按GPA降序) =====' AS section;
SELECT ID,
       dept_name,
       GPA                                           AS ranking_basis,
       CUME_DIST()  OVER (ORDER BY GPA DESC)         AS cume_dist
FROM dept_grades
ORDER BY GPA DESC, ID;

-- ============================================================
-- ② 按系分区 CUME_DIST(每个系内各自累积)
-- ============================================================
SELECT '===== ② 按系分区 CUME_DIST =====' AS section;
SELECT ID,
       dept_name,
       GPA                                           AS ranking_basis,
       CUME_DIST()  OVER (PARTITION BY dept_name
                          ORDER BY GPA DESC)         AS dept_cume_dist
FROM dept_grades
ORDER BY dept_name, GPA DESC, ID;

-- ============================================================
-- ③ 单行分区: 只有1人的系, CUME_DIST=1/1=1 (对比 PERCENT_RANK=0)
-- ============================================================
SELECT '===== ③ 单生系(Biology/Finance/History/Music) CUME_DIST 均为 1 =====' AS section;
SELECT ID,
       dept_name,
       GPA                                           AS ranking_basis,
       CUME_DIST()  OVER (PARTITION BY dept_name
                          ORDER BY GPA DESC)         AS dept_cume_dist
FROM dept_grades
WHERE dept_name IN ('Biology','Finance','History','Music')
ORDER BY dept_name;

-- ============================================================
-- ④ 对照 CUME_DIST vs PERCENT_RANK (整体): 首名差异最直观
-- ============================================================
SELECT '===== ④ 对照: CUME_DIST vs PERCENT_RANK(整体) =====' AS section;
SELECT ID,
       GPA                                           AS ranking_basis,
       CUME_DIST()   OVER (ORDER BY GPA DESC)        AS cume_dist,
       PERCENT_RANK() OVER (ORDER BY GPA DESC)       AS percent_rank
FROM dept_grades
ORDER BY GPA DESC, ID;
