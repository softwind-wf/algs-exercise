-- ============================================================
-- 关系代数「怎么做」—— 小例子逐步演算（每一步都打印中间结果）
-- 数据库: university（PostgreSQL 17 与 MySQL 8.0 都能直接跑，只用临时表 + EXCEPT，无方言语法）
-- 运行:
--   .\psql.bat -f src\main\resources\sql\ra_walkthrough.sql
--   .\sql.bat -db university -f src\main\resources\sql\ra_walkthrough.sql
--
-- 为什么另写这一份:
--   《数据库系统概念》第6章只给「表达式 + 一句结论」，不展示中间关系，所以看完
--   还是不知道手该怎么动。这里故意把表做得很小（ins 只有 5 行），于是
--   笛卡尔积只有 25 行、选择只剩 2 行……每一步都能当场数出来、对上号。
--
-- 全脚本只用四张 toy_* 小表，开头结尾各 DROP 一次，不会在 university 库里留下东西。
-- （不能用 MySQL 的 TEMPORARY 表：MySQL 不允许在同一查询里两次引用同一张临时表，
--   而自连接正是本演示的关键动作，所以改用普通表 + 收尾清理。）
-- 每个小节打印一行 section 当标题，照着顺序读即可。
-- ============================================================


-- ============================================================
-- 0. 准备四张小表
--    ins : 教师    dept: 系    course: 课程    takes: 选课
-- ============================================================
DROP TABLE IF EXISTS toy_ins;

CREATE TABLE toy_ins (
    ID        VARCHAR(5),
    name      VARCHAR(20),
    dept_name VARCHAR(20),
    salary    DECIMAL(8, 2)
);

INSERT INTO toy_ins (ID, name, dept_name, salary) VALUES
    ('10101', 'Alice', 'Comp. Sci.', 60000),
    ('22222', 'Bob',   'Physics',    90000),
    ('33456', 'Carol', 'Physics',    70000),
    ('45565', 'Dave',  'Comp. Sci.', 95000),
    ('58583', 'Erin',  'History',    95000);

DROP TABLE IF EXISTS toy_dept;

CREATE TABLE toy_dept (
    dept_name VARCHAR(20),
    building  VARCHAR(20),
    budget    DECIMAL(12, 2)
);

INSERT INTO toy_dept (dept_name, building, budget) VALUES
    ('Comp. Sci.', 'Turing', 1000000),
    ('Physics',    'Watson',  800000),
    ('History',    'Painter', 500000);

DROP TABLE IF EXISTS toy_course;

CREATE TABLE toy_course (
    course_id VARCHAR(8),
    title     VARCHAR(30),
    dept_name VARCHAR(20)
);

INSERT INTO toy_course (course_id, title, dept_name) VALUES
    ('BIO-101', 'Intro to Biology',   'Biology'),
    ('BIO-301', 'Genetics',           'Biology'),
    ('CS-101',  'Intro to CS',        'Comp. Sci.');

DROP TABLE IF EXISTS toy_takes;

CREATE TABLE toy_takes (
    ID        VARCHAR(5),
    course_id VARCHAR(8)
);

INSERT INTO toy_takes (ID, course_id) VALUES
    ('10101', 'BIO-101'),
    ('10101', 'BIO-301'),
    ('10101', 'CS-101'),
    ('22222', 'BIO-101'),
    ('33456', 'BIO-101'),
    ('33456', 'BIO-301');

SELECT '0. 原始关系 ins(5 行)、dept(3 行) —— 表小到可以手算' AS section;

SELECT * FROM toy_ins ORDER BY ID;

SELECT * FROM toy_dept ORDER BY dept_name;


-- ============================================================
-- 1. 选择 σ —— 挑「行」：只留下满足条件的元组
--    σ_dept_name = Physics (ins)
-- ============================================================
SELECT '1. 选择: σ_dept_name=Physics(ins) —— 5 行里挑出 2 行' AS section;

SELECT *
FROM toy_ins
WHERE dept_name = 'Physics'
ORDER BY ID;


-- ============================================================
-- 2. 投影 π —— 挑「列」，并自动去掉重复元组
--    π_name,salary(ins)      正常挑列
--    π_salary(ins)           两个 95000 只留一个（关系是集合，不允许重复元组）
-- ============================================================
SELECT '2a. 投影: π_name,salary(ins) —— 只挑列, 行数不变' AS section;

SELECT name, salary
FROM toy_ins
ORDER BY salary DESC, name;

SELECT '2b. 投影去重: π_salary(ins) 只有 4 行, 两个 95000 合成一个' AS section;

SELECT DISTINCT salary
FROM toy_ins
ORDER BY salary DESC;

SELECT '2c. 对照: SQL 默认保留重复行, 必须写 DISTINCT 才等于关系代数的投影' AS section;

SELECT salary
FROM toy_ins
ORDER BY salary DESC;


-- ============================================================
-- 3. 笛卡尔积 × —— 把两张表的行两两拼起来（5 × 5 = 25 行）
--    列名前缀 ins. / d. 就是「限定名」，这是下一节更名运算的动机
-- ============================================================
SELECT '3. 笛卡尔积: ins × ins = 25 行(5×5), 两个 salary 靠 ins./d. 区分' AS section;

SELECT i.ID AS i_id, i.name AS i_name, i.salary AS i_salary,
       d.ID AS d_id, d.name AS d_name, d.salary AS d_salary
FROM toy_ins AS i, toy_ins AS d
ORDER BY i.ID, d.ID;


-- ============================================================
-- 4. 更名 ρ + 选择 —— 教材里「找出大学里的最高工资」的第一步
--    ρ_d(ins) 把副本改名 d，于是能写 σ_ins.salary < d.salary
--    含义: 保留「工资比我低的那个我」，即「我不是最高工资」
-- ============================================================
SELECT '4. 更名后比较: σ_ins.salary<d.salary(ins × ρ_d(ins)) —— 25 行里剩 9 行' AS section;

SELECT i.ID AS i_id, i.name AS i_name, i.salary AS i_salary,
       d.ID AS d_id, d.name AS d_name, d.salary AS d_salary
FROM toy_ins AS i, toy_ins AS d
WHERE i.salary < d.salary
ORDER BY i.salary DESC, d.salary, i.ID;


-- ============================================================
-- 5. 集合差 − —— 第二步：全部工资 减去「不是最高的工资」= 最高工资
-- ============================================================
SELECT '5a. 集合差: π_salary(ins) − π_salary(上一步) = 95000' AS section;

SELECT salary AS max_salary
FROM toy_ins
EXCEPT
SELECT i.salary
FROM toy_ins AS i, toy_ins AS d
WHERE i.salary < d.salary;

SELECT '5b. 换回「人」: 工资最高的是谁 —— 并列最高的 Dave 与 Erin 都要列出来' AS section;

SELECT t.ID, t.name, t.dept_name, t.salary
FROM toy_ins AS t
WHERE NOT EXISTS (SELECT 1 FROM toy_ins AS u WHERE u.salary > t.salary)
ORDER BY t.ID;


-- ============================================================
-- 6. 连接 ⋈ —— 两张表按公共属性对接，再选择 + 投影
--    π_name( σ_building=Watson ( ins ⋈ dept ) )
-- ============================================================
SELECT '6a. 连接: ins ⋈ dept(按 dept_name 对接) —— 5 行各配上自己系的楼' AS section;

SELECT i.ID, i.name, i.dept_name, d.building, d.budget
FROM toy_ins AS i
JOIN toy_dept AS d ON i.dept_name = d.dept_name
ORDER BY i.ID;

SELECT '6b. 再选择 + 投影: 在 Watson 楼办公的教师姓名' AS section;

SELECT i.name
FROM toy_ins AS i
JOIN toy_dept AS d ON i.dept_name = d.dept_name
WHERE d.building = 'Watson'
ORDER BY i.name;


-- ============================================================
-- 7. 广义投影 —— 投影列表里放「表达式」，投影的同时逐行算
--    π_{ID, name, salary*1.1}(ins)
-- ============================================================
SELECT '7. 广义投影: π_{ID,name,salary*1.1}(ins) —— 多出一列算出来的新工资' AS section;

SELECT ID,
       name,
       salary                        AS old_salary,
       ROUND(salary * 1.1, 2)        AS new_salary,
       ROUND(salary * 1.1 - salary, 2) AS raise
FROM toy_ins
ORDER BY old_salary DESC, ID;


-- ============================================================
-- 8. 位置标记 $i —— 用属性「第几位」代替属性名
--    本表次序: $1=ID  $2=name  $3=dept_name  $4=salary
--    自乘后第二个 ins 的 salary 变成 $8，所以最高工资写成:
--      Π_$4(ins) − Π_$4( σ_$4 < $8 ( ins × ins ) )
--    SQL 没有 $i 写法，用 CTE 的列名清单按位置改名来模拟
-- ============================================================
SELECT '8a. 位置标记: CTE 列名清单把属性改名为 c1..c4, 与 $1..$4 一一对应' AS section;

WITH r (c1, c2, c3, c4) AS (
    SELECT ID, name, dept_name, salary FROM toy_ins
)
SELECT c1 AS "$1=ID", c2 AS "$2=name", c3 AS "$3=dept", c4 AS "$4=salary"
FROM r
ORDER BY c4 DESC, c1;

SELECT '8b. 位置标记写最高工资: Π_$4(r) − Π_$4(σ_$4<$8(r × r))' AS section;

WITH r (c1, c2, c3, c4) AS (
    SELECT ID, name, dept_name, salary FROM toy_ins
),
prod (c1, c2, c3, c4, c5, c6, c7, c8) AS (
    SELECT x.c1, x.c2, x.c3, x.c4,
           y.c1, y.c2, y.c3, y.c4
    FROM r AS x, r AS y
),
lower (c4) AS (
    SELECT DISTINCT c4 FROM prod WHERE c4 < c8
)
SELECT c4 AS max_salary
FROM r
EXCEPT
SELECT c4 FROM lower;


-- ============================================================
-- 9. 除运算 ÷ —— 教材里最容易卡住的一个：「选修了 Biology 全部课程的学生」
--    手算思路（双重否定）: 这个学生「不存在」一门 Biology 课是他「没选」的
-- ============================================================
SELECT '9a. 选课关系: 谁选了什么(以及 Biology 有哪些课)' AS section;

SELECT t.ID, t.course_id, c.dept_name
FROM toy_takes AS t
JOIN toy_course AS c ON t.course_id = c.course_id
ORDER BY t.ID, t.course_id;

SELECT '9b. 除运算: 选修了 Biology 全部课程的学生 —— 双重否定写法' AS section;

SELECT DISTINCT t.ID
FROM toy_takes AS t
WHERE NOT EXISTS (
    SELECT 1
    FROM toy_course AS c
    WHERE c.dept_name = 'Biology'
      AND NOT EXISTS (
          SELECT 1 FROM toy_takes AS t2
          WHERE t2.ID = t.ID AND t2.course_id = c.course_id
      )
)
ORDER BY t.ID;

SELECT '9c. 同一件事的第二种写法: 数一数, 选够门数才算数' AS section;

SELECT t.ID, COUNT(DISTINCT t.course_id) AS bio_taken
FROM toy_takes AS t
JOIN toy_course AS c ON t.course_id = c.course_id
WHERE c.dept_name = 'Biology'
GROUP BY t.ID
HAVING COUNT(DISTINCT t.course_id) = (
    SELECT COUNT(*) FROM toy_course WHERE dept_name = 'Biology'
)
ORDER BY t.ID;


-- ============================================================
-- 10. 综合例题: 「找出工资比 Physics 系每一位教师都高的教师」
--     见到「比……都 / 所有 / 全部」→ 立刻想到 NOT EXISTS 的双重否定
-- ============================================================
SELECT '10. 综合例题: 工资比 Physics 系每位教师都高的人' AS section;

SELECT t.ID, t.name, t.dept_name, t.salary
FROM toy_ins AS t
WHERE NOT EXISTS (
    SELECT 1 FROM toy_ins AS p
    WHERE p.dept_name = 'Physics' AND p.salary >= t.salary
)
ORDER BY t.ID;

SELECT '10b. 同一题的「差集」写法: 全校教师 − 工资没超过某个 Physics 教师的人' AS section;

SELECT name
FROM toy_ins
EXCEPT
SELECT i.name
FROM toy_ins AS i, toy_ins AS p
WHERE p.dept_name = 'Physics' AND i.salary <= p.salary
ORDER BY 1;


-- ============================================================
-- 11. 自测题 3 道（先自己想，再往下看答案）
--     题1: 找出 Physics 系工资最高的教师姓名
--     题2: 找出没有选 CS-101 这门课的学生
--     题3: 找出与 Carol 同系但工资比她高的教师姓名
-- ============================================================
SELECT '11-1. 题1 答案: π_name(σ_dept=Physics ∧ 不存在同系工资更高的人)' AS section;

SELECT t.ID, t.name, t.salary
FROM toy_ins AS t
WHERE t.dept_name = 'Physics'
  AND NOT EXISTS (
      SELECT 1 FROM toy_ins AS p
      WHERE p.dept_name = 'Physics' AND p.salary > t.salary
  )
ORDER BY t.ID;

SELECT '11-2. 题2 答案: π_ID(takes) − π_ID(σ_course=CS-101(takes)) —— 差集' AS section;

SELECT ID FROM toy_takes
EXCEPT
SELECT ID FROM toy_takes WHERE course_id = 'CS-101'
ORDER BY 1;

SELECT '11-3. 题3 答案: 先取 Carol 所在系与工资, 再拿同系的人去比' AS section;

SELECT t.ID, t.name, t.salary
FROM toy_ins AS t
WHERE t.dept_name = (SELECT dept_name FROM toy_ins WHERE name = 'Carol')
  AND t.salary > (SELECT salary FROM toy_ins WHERE name = 'Carol')
ORDER BY t.ID;


-- ============================================================
-- 12. 思考题: 「找出工资高于本系平均工资的教师」
--     基本关系代数(σ π × − ρ ∪ ∩ ⋈)写不出来 —— 因为要算「平均值」，
--     得用教材 6.1 后面的聚集运算。SQL 里用相关子查询或窗口函数即可。
-- ============================================================
SELECT '12. 思考题: 高于本系平均工资的教师(需要聚集, 超出基本运算)' AS section;

SELECT t.ID, t.name, t.dept_name, t.salary,
       (SELECT ROUND(AVG(x.salary), 2) FROM toy_ins AS x WHERE x.dept_name = t.dept_name) AS dept_avg
FROM toy_ins AS t
WHERE t.salary > (
    SELECT AVG(x.salary) FROM toy_ins AS x WHERE x.dept_name = t.dept_name
)
ORDER BY t.dept_name, t.ID;


-- ============================================================
-- 13. 收尾: 删掉四张 toy_* 表（脚本开头也会 DROP 一次，因此本脚本可反复运行）
-- ============================================================
SELECT '13. 收尾: 删除 toy_* 演示表, university 库不留痕迹' AS section;

DROP TABLE IF EXISTS toy_takes;

DROP TABLE IF EXISTS toy_course;

DROP TABLE IF EXISTS toy_dept;

DROP TABLE IF EXISTS toy_ins;
