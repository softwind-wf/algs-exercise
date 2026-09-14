-- ============================================================
-- 计算"指定顺序下前三个元组的均值"
-- 数据库: university (PostgreSQL)
-- 数据源: dept_grades(ID, dept_name, GPA)
--
-- 需求：
--   * 指定顺序 = 按 GPA 降序 (GPA DESC)。
--   * 取该顺序下的前 3 个元组(前 3 名学生)。
--   * 对这 3 个元组求均值(AVG)。
--
-- 实现要点：
--   窗口函数(如 ROW_NUMBER)不能直接放在 WHERE 里，必须先在
--   内层算出身位，再在外层过滤 `seq <= 3`，最后外层聚合求 AVG。
--   ROW_NUMBER 会按 (GPA DESC, ID) 强行排出一个完全确定的顺序，
--   取到的恰好是 3 个元组(即使 GPA 并列也能区分先后)。
-- ============================================================

-- ============================================================
-- 方式一：派生表 + ROW_NUMBER，得"恰好 3 行"的均值
-- ============================================================
SELECT AVG(GPA) AS avg_top3_gpa
FROM (
    SELECT GPA,
           ROW_NUMBER() OVER (ORDER BY GPA DESC, ID) AS seq
    FROM dept_grades
) sorted
WHERE seq <= 3;

-- ============================================================
-- 方式二：用 LIMIT / FETCH FIRST 取前 3 行，再包一层求均值
--         (LIMIT 属于"取行"，不能直接和 AVG 同层生效)
-- ============================================================
SELECT AVG(top.GPA) AS avg_top3_gpa_limit
FROM (
    SELECT GPA
    FROM dept_grades
    ORDER BY GPA DESC, ID
    LIMIT 3
) top;

-- ============================================================
-- 对照：列出这 3 个元组本身，便于肉眼核对均值
-- ============================================================
SELECT ID, dept_name, GPA
FROM dept_grades
ORDER BY GPA DESC, ID
LIMIT 3;
