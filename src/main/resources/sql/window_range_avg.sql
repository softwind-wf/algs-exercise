-- ============================================================
-- 教材练习: 窗口函数(分窗特性) —— 用 RANGE 按"属性值的范围"而不是行数定窗口
-- 原文(SQL 标准/高级 SQL 章):
--   select year, avg(num_credits)
--   over (order by year range between 1 preceding and 1 following)
--   as avg_total_credits
--   from tot_credits;
--
-- 数据库: university (PostgreSQL)
-- 数据源: 视图 tot_credits(year, num_credits)
--
-- 关键概念(教材第5章强调):
--   * ROWS 按"行的数目"定窗口; RANGE 按"排序属性的值范围"定窗口。
--   * 这里 ORDER BY year, RANGE BETWEEN 1 PRECEDING AND 1 FOLLOWING
--     表示窗口 = year 落在 [当前year-1 , 当前year+1] 的所有行。
--   * 注意: 它不要求年份连续! 只要 year 的差值 <=1 就纳入窗口。
--     因此范围里"有多少行由数据决定", 而不是固定行数。
--   * 若数据里有 year 相同(并列)的多个元组, 它们会被一并算进同一窗口,
--     这正是 RANGE 与 ROWS 的本质区别(教材后续会专门讲并列情况)。
-- ============================================================

-- ============================================================
-- ① 忠实还原原文写法(RANGE BETWEEN 1 PRECEDING AND 1 FOLLOWING)
-- ============================================================
\echo '===== ① 原文写法: RANGE BETWEEN 1 PRECEDING AND 1 FOLLOWING ====='
SELECT year,
       num_credits,
       AVG(num_credits) OVER (
           ORDER BY year
           RANGE BETWEEN 1 PRECEDING AND 1 FOLLOWING
       ) AS avg_total_credits
FROM tot_credits
ORDER BY year;

-- ============================================================
-- ② 用 array_agg 展示每行窗口实际包含的值
--    RANGE: 窗口 = year 落在 [当前year-1, 当前year+1] 的所有行
-- ============================================================
\echo '===== ② 逐行窗口内容核对(array_agg, RANGE) ====='
SELECT year,
       num_credits,
       array_agg(num_credits) OVER (
           ORDER BY year
           RANGE BETWEEN 1 PRECEDING AND 1 FOLLOWING
       ) AS window_values,
       AVG(num_credits) OVER (
           ORDER BY year
           RANGE BETWEEN 1 PRECEDING AND 1 FOLLOWING
       ) AS avg_total_credits
FROM tot_credits
ORDER BY year;

-- ============================================================
-- ③ 对照: 同样的 1 PRECEDING AND 1 FOLLOWING, 用 ROWS 得到的是"3行"
--         用 RANGE 得到的是"差值<=1的所有行"——展示两者的差别
-- ============================================================
\echo '===== ③ 对照: ROWS vs RANGE (1 PRECEDING AND 1 FOLLOWING) ====='
SELECT year,
       num_credits,
       array_agg(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
       ) AS window_rows,
       array_agg(num_credits) OVER (
           ORDER BY year
           RANGE BETWEEN 1 PRECEDING AND 1 FOLLOWING
       ) AS window_range,
       AVG(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
       ) AS avg_rows,
       AVG(num_credits) OVER (
           ORDER BY year
           RANGE BETWEEN 1 PRECEDING AND 1 FOLLOWING
       ) AS avg_range
FROM tot_credits
ORDER BY year;
