-- ============================================================
-- 视图 student_grades(ID, GPA)：每个学生的平均绩点
-- 教材: Database System Concepts (Silberschatz / Korth / Sudarshan)
-- 数据库: university (PostgreSQL)
--
-- 说明:
--   * 绩点按"学分加权平均"(标准 GPA 算法)：
--       GPA = Σ(grade_point × credits) / Σ(credits)
--   * 只有已出成绩的选课才计入 (grade IS NOT NULL)，NULL 表示在读未评分。
--   * 字母成绩 → 绩点映射为通用的 4.0 制:
--       A+=A=4.0  A-=3.7  B+=3.3  B=3.0  B-=2.7
--       C+=2.3  C=2.0  C-=1.7  D+=1.3  D=1.0  D-=0.7  F=0.0
--   * 视图列名显式命名为 (ID, GPA)，与要求一致。
-- ============================================================

CREATE OR REPLACE VIEW student_grades (ID, GPA) AS
SELECT t.ID,
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
WHERE t.grade IS NOT NULL
GROUP BY t.ID;
