-- ============================================================
-- 按系分区(partition by dept_name)的窗口查询
-- 教材: Database System Concepts 第5章 高级 SQL / 分窗特性练习
-- 数据源: 视图 tot_credits_dept(dept_name, year, num_credits)
-- 数据库: university (PostgreSQL)
--
-- 需求:
--   在 tot_credits_dept 上, 按 dept_name 分区(每个系独立处理),
--   在系内按 year 排序, 用分窗特性计算"系内滚动均值"。
--   窗口 = 当前行 + 系内前 1 行 + 系内后 1 行(相邻 3 年)。
--
-- 核心概念(对比):
--   * 上一题不带 PARTITION → 全校一起算(全局窗口)。
--   * 这一题带 PARTITION BY dept_name → 每个系单独算(系内窗口)。
--     窗口绝不会跨系, 每个系从自身第 1 行开始独立累计。
-- ============================================================

-- ============================================================
-- ① 按系分区 + 系内滚动均值(当前+前1+后1年)
-- ============================================================
SELECT '===== ① 按系分区滚动均值: PARTITION BY dept_name =====' AS section;
SELECT dept_name,
       year,
       num_credits,
       AVG(num_credits) OVER (
           PARTITION BY dept_name
           ORDER BY year
           ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
       ) AS avg_dept_credits,
       COUNT(num_credits) OVER (
           PARTITION BY dept_name
           ORDER BY year
           ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
       ) AS win_rows
FROM tot_credits_dept
ORDER BY dept_name, year;

-- ============================================================
-- ② 对照: 不带 PARTITION(全校一起, 全局窗口)
--    与①对比, 看清"分区"把每个系隔开了
-- ============================================================
SELECT '===== ② 对照: 不带 PARTITION(全校全局窗口) =====' AS section;
SELECT dept_name,
       year,
       num_credits,
       AVG(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
       ) AS avg_global
FROM tot_credits_dept
ORDER BY dept_name, year;

-- ============================================================
-- ③ 对照: 按系分区的"累计均值"(UNBOUNDED PRECEDING)
--    每个系从自身第 1 年累积到当前年
-- ============================================================
SELECT '===== ③ 按系分区累计均值: UNBOUNDED PRECEDING =====' AS section;
SELECT dept_name,
       year,
       num_credits,
       array_agg(num_credits) OVER (
           PARTITION BY dept_name
           ORDER BY year
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS win_values,
       AVG(num_credits) OVER (
           PARTITION BY dept_name
           ORDER BY year
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS avg_cumulative
FROM tot_credits_dept
ORDER BY dept_name, year;
