-- ============================================================
-- 习题 4.20（Database System Concepts 7th ed. / Intermediate SQL）
--
-- 定义一个视图 tot_credits(year, num_credits)，
-- 记录每年学生选课的总学分（每门被选课程的学分求和）。
--
-- 说明：
--   * takes 与 course 做自然连接（共同列 course_id），
--     这样每条选课记录都能带出对应课程的 credits。
--   * 以 year 作为分组键：SQL 保证每个分组只产生一行，
--     从而满足题目要求的『每个年份最多有一个元组』。
--   * num_credits = 该年所有选课记录的学分之和。
--   * 若只统计『已获学分』（排除 F/在读），可在 WHERE 里过滤 grade，
--     教材原解未加过滤，此处按原解实现（统计全部选课学分）。
-- ============================================================

CREATE OR REPLACE VIEW tot_credits(year, num_credits) AS
SELECT year, SUM(credits)
FROM takes NATURAL JOIN course
GROUP BY year
ORDER BY year ASC;
