-- ============================================================
-- 教材练习: 窗口函数(分窗特性) 累计均值
-- 原文(SQL 标准):
--   select year, avg(num_credits)
--   over (order by year rows unbounded preceding)
--   as avg_total_credits
--   from tot_credits;
--
-- 数据库: university (PostgreSQL)
-- 数据源: 视图 tot_credits(year, num_credits)
--
-- PostgreSQL 版本说明:
--   * 该语句本就是标准语法, PostgreSQL 可原样执行。
--   * 这里补充:
--       ① 显式写全帧边界 BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
--          (语义完全等价, 可读性更好, 也便于和"定长窗口"对照);
--       ② 用 array_agg 窗口函数逐行展示"窗口实际包含的 num_credits 值",
--          直观看到窗口从第 1 行一直累积到当前行;
--       ③ 对照上一题固定 ROWS 3 PRECEDING(定长窗口)的差异。
-- ============================================================

-- ============================================================
-- ① 忠实还原原文写法(ROWS UNBOUNDED PRECEDING)
--    帧 = 从第 1 行一直到当前行(累积窗口)。
-- ============================================================
\echo '===== ① 原文写法: ROWS UNBOUNDED PRECEDING ====='
SELECT year,
       num_credits,
       AVG(num_credits) OVER (ORDER BY year ROWS UNBOUNDED PRECEDING) AS avg_total_credits
FROM tot_credits
ORDER BY year;

-- ============================================================
-- ② 写成显式边界(语义等价, 更清晰)
-- ============================================================
\echo '===== ② 显式边界: ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW ====='
SELECT year,
       num_credits,
       AVG(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS avg_total_credits
FROM tot_credits
ORDER BY year;

-- ============================================================
-- ③ 用 array_agg 展示每行窗口实际包含的值
--    第 1 行 = {8}; 第 2 行 = {8,10}; ...; 累计到该行为止
-- ============================================================
\echo '===== ③ 逐行窗口内容核对(array_agg) ====='
SELECT year,
       num_credits,
       ROW_NUMBER() OVER (ORDER BY year) AS row_num,
       array_agg(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS window_values,
       AVG(num_credits) OVER (ORDER BY year ROWS UNBOUNDED PRECEDING) AS avg_total_credits
FROM tot_credits
ORDER BY year;

-- ============================================================
-- ④ 对照: 上一题的定长窗口 ROWS 3 PRECEDING
--    帧 = 当前行 + 前 3 行(定长, 最多 4 个值)
-- ============================================================
\echo '===== ④ 对照: 定长窗口 ROWS 3 PRECEDING ====='
SELECT year,
       num_credits,
       array_agg(num_credits) OVER (
           ORDER BY year
           ROWS 3 PRECEDING
       ) AS window_3preceding,
       AVG(num_credits) OVER (ORDER BY year ROWS 3 PRECEDING) AS avg_3preceding,
       AVG(num_credits) OVER (ORDER BY year ROWS UNBOUNDED PRECEDING) AS avg_unbounded
FROM tot_credits
ORDER BY year;
