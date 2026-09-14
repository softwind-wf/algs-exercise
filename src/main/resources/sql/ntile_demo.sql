-- ============================================================
-- NTILE(n) 演示: 把分区内行分成 n 个"尽量相等"的桶, 编号 1..n
--   分桶规则: 按 ORDER BY 排序后依次填入桶1..桶n
--   除不尽时: 靠前的桶多分一行(前面桶行数 >= 后面桶)
--   注意: 桶内数值是"大致等差", 不保证名次/并列与桶边界对齐
-- 数据源: dept_grades(ID, dept_name, GPA)
-- ============================================================

\set ON_ERROR_STOP on

-- ============================================================
-- ① 整体 NTILE(4): 四分位(每桶3行, 12/4=3 恰好整除)
-- ============================================================
\echo '===== ① 整体 NTILE(4): 四分位 ====='
SELECT ID,
       dept_name,
       GPA                                           AS ranking_basis,
       NTILE(4) OVER (ORDER BY GPA DESC)             AS quartile
FROM dept_grades
ORDER BY quartile, GPA DESC, ID;

-- ============================================================
-- ② 整体 NTILE(10): 十分位(12/10 除不尽, 前2桶各2行, 其余各1行)
-- ============================================================
\echo '===== ② 整体 NTILE(10): 十分位 ====='
SELECT ID,
       dept_name,
       GPA                                           AS ranking_basis,
       NTILE(10) OVER (ORDER BY GPA DESC)            AS decile
FROM dept_grades
ORDER BY decile, GPA DESC, ID;

-- ============================================================
-- ③ 按系分区 NTILE(4): 各系内部再分4桶
--   注意: 某个系行数 < 桶数时(如 Physics 2人/4桶), 后面桶为空
-- ============================================================
\echo '===== ③ 按系分区 NTILE(4): 系内四分位 ====='
SELECT ID,
       dept_name,
       GPA                                           AS ranking_basis,
       NTILE(4) OVER (PARTITION BY dept_name
                      ORDER BY GPA DESC)             AS dept_quartile
FROM dept_grades
ORDER BY dept_name, dept_quartile, GPA DESC, ID;

-- ============================================================
-- ④ 统计每个桶的行数, 验证"尽量相等、前面桶多"
-- ============================================================
\echo '===== ④ NTILE(4) 每个桶的行数 ====='
SELECT quartile,
       COUNT(*) AS rows_in_bucket
FROM (
    SELECT NTILE(4) OVER (ORDER BY GPA DESC) AS quartile
    FROM dept_grades
) t
GROUP BY quartile
ORDER BY quartile;
