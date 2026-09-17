-- ============================================================
-- 用聚合函数(而非窗口函数)实现学生排名 (university PostgreSQL)
-- 排名依据: student_grades 视图的 GPA(4.0 制学分加权平均), GPA 降序
-- 核心思想: 名次 = "GPA 严格高于自己的学生数" + 1
--   并列处理: 用 COUNT(*)      -> 比赛排名(并列同号, 跳号) 1,2,2,4
--             用 COUNT(DISTINCT)-> 密集排名(并列同号, 不跳号) 1,2,2,3
-- ============================================================

-- 注: psql 客户端专用的 ON_ERROR_STOP 设置已移除(psql.bat / sql.bat 走 JDBC, 不识别 psql 元命令)

-- ============================================================
-- ① 相关子查询 + COUNT(*) —— 教材经典写法(比赛排名, 并列同号跳号)
-- ============================================================
SELECT '===== ① 相关子查询 + COUNT(*): 比赛排名 (1,2,2,4,...) =====' AS section;
SELECT s1.ID,
       s1.GPA                                     AS ranking_basis,
       (SELECT COUNT(*) FROM student_grades s2
         WHERE s2.GPA > s1.GPA) + 1               AS rank
FROM student_grades s1
ORDER BY rank, s1.ID;

-- ============================================================
-- ② 自连接 + GROUP BY + COUNT —— 等价做法, 单条语句完成
-- ============================================================
SELECT '===== ② 自连接 + GROUP BY + COUNT(s2.ID): 比赛排名 (与①一致) =====' AS section;
SELECT s1.ID,
       s1.GPA                                     AS ranking_basis,
       COUNT(s2.ID) + 1                           AS rank
FROM student_grades s1
LEFT JOIN student_grades s2 ON s2.GPA > s1.GPA
GROUP BY s1.ID, s1.GPA
ORDER BY rank, s1.ID;

-- ============================================================
-- ③ 相关子查询 + COUNT(DISTINCT GPA) —— 密集排名(并列同号不跳号)
-- ============================================================
SELECT '===== ③ COUNT(DISTINCT GPA): 密集排名 (1,2,2,3,...) =====' AS section;
SELECT s1.ID,
       s1.GPA                                     AS ranking_basis,
       (SELECT COUNT(DISTINCT s2.GPA) FROM student_grades s2
         WHERE s2.GPA > s1.GPA) + 1               AS rank
FROM student_grades s1
ORDER BY rank, s1.ID;

-- ============================================================
-- ④ 汇总验证: 用聚合函数排名, 关联学生姓名
-- ============================================================
SELECT '===== ④ 直观版: ID/姓名/绩点/名次(比赛排名) =====' AS section;
SELECT s.ID,
       s.name,
       sg.GPA                                     AS ranking_basis,
       (SELECT COUNT(*) FROM student_grades s2
         WHERE s2.GPA > sg.GPA) + 1               AS rank
FROM student_grades sg
JOIN student s USING (ID)
ORDER BY rank, s.ID;
