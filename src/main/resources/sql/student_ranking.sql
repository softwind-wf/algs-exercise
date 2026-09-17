-- ============================================================
-- 学生排名实现 (university PostgreSQL)
-- 排名依据: student_grades 视图中的 GPA(4.0 制学分加权平均绩点)
-- 排名规则: GPA 降序, 并列(相同 GPA)处理下的三种常见口径 ===
--   ① RANK()       比赛排名: 并列同号, 下一名跳号 -> 1,2,2,4
--   ② DENSE_RANK() 密集排名: 并列同号, 不跳号       -> 1,2,2,3
--   ③ ROW_NUMBER() 连续序号: 并列也强行区分先后     -> 1,2,3,4
-- 说明: student_grades 只含"至少修过一门且已出成绩"的学生,
--       无成绩(成绩全为 NULL)的学生不参与排名。
-- ============================================================

-- 注: psql 客户端专用的 ON_ERROR_STOP 设置已移除(psql.bat / sql.bat 走 JDBC, 不识别 psql 元命令)

-- ============================================================
-- ① RANK() —— 比赛排名(competition ranking, 最常见)
--    相同 GPA 共享同一名次, 下一同分跳号
-- ============================================================
SELECT '===== ① RANK(): 并列同号, 跳号 (1,2,2,4,...) =====' AS section;
SELECT ID,
       GPA                                        AS ranking_basis,
       RANK() OVER (ORDER BY GPA DESC)           AS rank
FROM student_grades
ORDER BY rank, ID;

-- ============================================================
-- ② DENSE_RANK() —— 密集排名, 并列同号但不跳号
-- ============================================================
SELECT '===== ② DENSE_RANK(): 并列同号, 不跳号 (1,2,2,3,...) =====' AS section;
SELECT ID,
       GPA                                        AS ranking_basis,
       DENSE_RANK() OVER (ORDER BY GPA DESC)     AS rank
FROM student_grades
ORDER BY rank, ID;

-- ============================================================
-- ③ ROW_NUMBER() —— 连续序号, 并列时按 (GPA, ID) 二次排序区分
-- ============================================================
SELECT '===== ③ ROW_NUMBER(): 强行区分并列 (1,2,3,4,...) =====' AS section;
SELECT ID,
       GPA                                        AS ranking_basis,
       ROW_NUMBER() OVER (ORDER BY GPA DESC, ID) AS rank
FROM student_grades
ORDER BY rank, ID;

-- ============================================================
-- 带学生姓名与并列标记的直观展示 (基于 DENSE_RANK 口径)
-- ============================================================
SELECT '===== 直观版: ID/姓名/绩点/名次(密集排名) =====' AS section;
SELECT s.ID,
       s.name,
       sg.GPA                                     AS ranking_basis,
       DENSE_RANK() OVER (ORDER BY sg.GPA DESC)  AS rank
FROM student_grades sg
JOIN student s USING (ID)
ORDER BY rank, s.ID;
