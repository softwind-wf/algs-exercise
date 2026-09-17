-- ============================================================
-- 教材练习: 窗口函数(分窗特性) 计算滚动均值
-- 原文(SQL 标准/MySQL):
--   select year, avg(num_credits)
--   over (order by year rows 3 preceding)
--   as avg_total_credits
--   from tot_credits；
--
-- 数据库: university (PostgreSQL)
-- 数据源: 视图 tot_credits(year, num_credits)
--
-- PostgreSQL 版本说明:
--   * 该语句本身已是标准语法, PostgreSQL 可原样执行, 无需改语法。
--   * 这里补充:
--       ① 显式写全帧边界 BETWEEN ... AND CURRENT ROW(可读性更好)；
--       ② ORDER BY year 指定顺序(分窗特性必须带 ORDER BY)；
--       ③ 末尾再 ORDER BY year 让输出有序。
-- ============================================================

-- ============================================================
-- ① 忠实还原原文写法(ROWS 3 PRECEDING)
--    帧 = 当前行 + 之前最多 3 行, 即最多 4 行。
-- ============================================================
SELECT '===== ① 原文写法: ROWS 3 PRECEDING =====' AS section;
SELECT year,
       num_credits,
       AVG(num_credits) OVER (ORDER BY year ROWS 3 PRECEDING) AS avg_total_credits
FROM tot_credits
ORDER BY year;

-- ============================================================
-- ② 写成显式边界(语义等价, 更清晰)
-- ============================================================
SELECT '===== ② 显式边界: ROWS BETWEEN 3 PRECEDING AND CURRENT ROW =====' AS section;
SELECT year,
       num_credits,
       AVG(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN 3 PRECEDING AND CURRENT ROW
       ) AS avg_total_credits
FROM tot_credits
ORDER BY year;

-- ============================================================
-- ③ 对照: 若想严格"当前行 + 前 2 行"(恰好 3 行), 用 2 PRECEDING
-- ============================================================
SELECT '===== ③ 严格 3 行(当前+前2): ROWS BETWEEN 2 PRECEDING AND CURRENT ROW =====' AS section;
SELECT year,
       num_credits,
       AVG(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
       ) AS avg_3window
FROM tot_credits
ORDER BY year;

-- ============================================================
-- ④ 用 array_agg 窗口函数逐行展示"窗口实际包含的 num_credits 值"
--    预期: 2009 只含 [49]； 2010 含 [49, 33](该顺序下前 2 行)
--    (当前库仅 2009/2010 两年, 凑不满 3 个前序元组)
-- ============================================================
SELECT '===== ④ 逐行窗口内容核对(array_agg) =====' AS section;
SELECT year,
       num_credits,
       ROW_NUMBER() OVER (ORDER BY year) AS row_num,
       array_agg(num_credits) OVER (
           ORDER BY year
           ROWS BETWEEN 3 PRECEDING AND CURRENT ROW
       ) AS window_values,
       AVG(num_credits) OVER (ORDER BY year ROWS 3 PRECEDING) AS avg_total_credits
FROM tot_credits
ORDER BY year;
