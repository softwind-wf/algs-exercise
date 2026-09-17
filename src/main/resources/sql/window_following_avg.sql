-- ============================================================
-- 教材练习: 窗口函数(分窗特性) —— 用 FOLLOWING 定义当前元组之后的窗口
-- 原文(SQL 标准/高级 SQL 章):
--   select year, avg(num_credits)
--   over (order by year rows between 3 preceding and 2 following)
--   as avg_total_credits
--   from tot_credits；
--
-- 数据库: university (PostgreSQL)
-- 数据源: 视图 tot_credits(year, num_credits)
--
-- PostgreSQL 版本说明:
--   * 该语句本就是标准语法, PostgreSQL 可原样执行。
--   * 这个帧是"双向窗口": 下界 = 当前行往前 3 行, 上界 = 当前行往后 2 行。
--     window = [当前-3 , 当前+2] 共最多 6 行。
--   * 用 array_agg 打印每行窗口实际包含的值, 直观看到 FOLLOWING 的作用。
-- ============================================================

-- ============================================================
-- ① 忠实还原原文写法(ROWS BETWEEN 3 PRECEDING AND 2 FOLLOWING)
-- ============================================================
SELECT '===== ① 原文写法: ROWS BETWEEN 3 PRECEDING AND 2 FOLLOWING =====' AS section;
SELECT year,
       num_credits,
       AVG(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN 3 PRECEDING AND 2 FOLLOWING
       ) AS avg_total_credits
FROM tot_credits
ORDER BY year;

-- ============================================================
-- ② 用 array_agg 展示每行窗口实际包含的值
--    window = 当前行往前 3 行 到 当前行往后 2 行
-- ============================================================
SELECT '===== ② 逐行窗口内容核对(array_agg) =====' AS section;
SELECT year,
       num_credits,
       ROW_NUMBER() OVER (ORDER BY year) AS row_num,
       array_agg(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN 3 PRECEDING AND 2 FOLLOWING
       ) AS window_values,
       AVG(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN 3 PRECEDING AND 2 FOLLOWING
       ) AS avg_total_credits
FROM tot_credits
ORDER BY year;

-- ============================================================
-- ③ 对照: 只往前(上界 CURRENT ROW)与"往后 2 行"的差别
-- ============================================================
SELECT '===== ③ 对照: 仅 PRECEDING (上界 CURRENT ROW) =====' AS section;
SELECT year,
       num_credits,
       array_agg(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN 3 PRECEDING AND CURRENT ROW
       ) AS window_before,
       AVG(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN 3 PRECEDING AND CURRENT ROW
       ) AS avg_before,
       AVG(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN 3 PRECEDING AND 2 FOLLOWING
       ) AS avg_both
FROM tot_credits
ORDER BY year;
