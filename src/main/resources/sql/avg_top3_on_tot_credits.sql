-- ============================================================
-- 在视图 tot_credits(year, num_credits) 上实现
--    "指定顺序下前三个元组的均值"
-- 数据库: university (PostgreSQL)
--
-- 需求：
--   * 指定顺序 = 例如按 year 升序 (year ASC)，
--     也可以改成按 num_credits 降序等任意排序。
--   * 取该顺序下的"前 3 个元组"，对这 3 个元组的 num_credits 求均值。
--
-- 说明：
--   * ROW_NUMBER() 不能直接放 WHERE，必须先在内层派生表算出身位 seq，
--     再在外层用 WHERE seq <= 3 过滤，最后外包 AVG 求均值。
--   * 注意：当前 tot_credits 只有 2 个元组(2009=49, 2010=33)，
--     因此"前 3 个"实际只取到全部 2 行，均值=(49+33)/2=41。
--     SQL 对"行数不足"天然不报错，有多少行就取多少行。
-- ============================================================

-- ============================================================
-- 方式一：派生表 + ROW_NUMBER，取前 3 行的 num_credits 均值
-- ============================================================
SELECT AVG(num_credits) AS avg_top3_num_credits
FROM (
    SELECT num_credits,
           ROW_NUMBER() OVER (ORDER BY year ASC) AS seq
    FROM tot_credits
) sorted
WHERE seq <= 3;

-- ============================================================
-- 方式二：ORDER BY ... LIMIT 3 子查询，外包 AVG
-- ============================================================
SELECT AVG(top.num_credits) AS avg_top3_num_credits_limit
FROM (
    SELECT num_credits
    FROM tot_credits
    ORDER BY year ASC
    LIMIT 3
) top;

-- ============================================================
-- 对照：列出实际参与求和的元组本身
-- ============================================================
SELECT year, num_credits
FROM tot_credits
ORDER BY year ASC
LIMIT 3;
