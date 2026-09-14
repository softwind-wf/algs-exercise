-- ============================================================
-- 视图 tot_credits_dept(dept_name, year, num_credits)
-- 教材: Database System Concepts (Silberschatz / Korth / Sudarshan)
--       第5章 高级 SQL / 分窗特性练习
-- 数据库: university (PostgreSQL)
--
-- 需求:
--   tot_credits 是"整个学校每年选课总学分"。
--   本视图把同样的统计按"系"拆分:
--     num_credits = 某年某系开设课程被学生选修的所有学分之和。
--
-- 归属逻辑(关键):
--   * 学分归属"课程所属系" course.dept_name,
--     而不是学生主修系 student.dept_name。
--     因为教材说的是"把每个系的学分数据……记录学生在指定年份中
--     选某个系开设的课程的所有学分数", 即按课程开课系聚合。
--
-- 说明:
--   * 透视: 对每个系、每个年份各产生一行 (dept_name, year, num_credits)。
--   * GROUP BY c.dept_name, t.year 同时带上两个分组键,
--     保证每个 (系, 年份) 组合最多一行。
--   注意: 若某系某年没有选课记录, 该组合不会出现(视图中无该行)。
-- ============================================================

CREATE OR REPLACE VIEW tot_credits_dept (dept_name, year, num_credits) AS
SELECT c.dept_name,
       t.year,
       SUM(c.credits) AS num_credits
FROM takes t
JOIN course c USING (course_id)
GROUP BY c.dept_name, t.year
ORDER BY c.dept_name, t.year;
