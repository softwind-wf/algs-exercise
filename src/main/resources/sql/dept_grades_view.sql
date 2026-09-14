-- ============================================================
-- 视图 dept_grades(ID, dept_name, GPA)
-- 教材: Database System Concepts (Silberschatz / Korth / Sudarshan)
-- 数据库: university (PostgreSQL)
--
-- 说明:
--   * GPA 计算逻辑与 student_grades 完全一致:
--       GPA = Σ(grade_point × course.credits) / Σ(course.credits)
--       只计已出成绩(grade IS NOT NULL)。
--   * 在 student_grades(ID, GPA) 基础上, 从 student 表取出该生所属系名 dept_name。
--   * 每个学生一行 (ID, dept_name, GPA)。
--   * GROUP BY 同时带上 ID 与 dept_name(系名按主键 ID 函数依赖, 这样最稳妥,
--     也便于 MySQL 等非函数依赖识别的数据库直接移植)。
-- ============================================================

CREATE OR REPLACE VIEW dept_grades (ID, dept_name, GPA) AS
SELECT t.ID,
       st.dept_name,
       SUM(c.credits * CASE t.grade
            WHEN 'A+' THEN 4.0
            WHEN 'A'  THEN 4.0
            WHEN 'A-' THEN 3.7
            WHEN 'B+' THEN 3.3
            WHEN 'B'  THEN 3.0
            WHEN 'B-' THEN 2.7
            WHEN 'C+' THEN 2.3
            WHEN 'C'  THEN 2.0
            WHEN 'C-' THEN 1.7
            WHEN 'D+' THEN 1.3
            WHEN 'D'  THEN 1.0
            WHEN 'D-' THEN 0.7
            ELSE 0.0
        END) / SUM(c.credits) AS GPA
FROM takes t
JOIN course c USING (course_id)
JOIN student st ON st.ID = t.ID
WHERE t.grade IS NOT NULL
GROUP BY t.ID, st.dept_name;
