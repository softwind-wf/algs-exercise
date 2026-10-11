-- ============================================================================
-- BCNF 违反与拆分的**最小演示**:两张表,每一步都能看到实际数据
-- 教材:《数据库系统概念》函数依赖与范式
--
-- 运行(建独立演示库,不动任何交付库):
--   .\psql.bat "DROP DATABASE IF EXISTS bcnf_demo WITH (FORCE)"
--   .\psql.bat "CREATE DATABASE bcnf_demo"
--   .\psql.bat -d bcnf_demo -f src\main\resources\sql\bcnf_demo_walkthrough.sql
--
-- 例 1(FD 的来源**看得见**):同一门课,同一个教师,却存了多行
--   R1(SNO 学号, SNAME 姓名, CNO 课程号, CNAME 课程名, TNO 教师号, TNAME 教师名)
--     约束: 学号->姓名, 课程号->课程名, 教师号->教师名  (这三条 DDL 能写, 演示时用外码表达)
--     语义: 一个教师只教一门课   =>  TNO -> CNO
--           一个学生选一门课只有一位教师 => (SNO, CNO) -> TNO
--   候选键: (SNO,CNO) 与 (SNO,TNO)
--   违反项: TNO -> CNO   (TNO 不是超键, CNO 是非主属性 => 连 3NF 都违反)
--
-- 例 2(FD 的来源**藏起来**):整个关系只有一个键, 违反项却是非平凡 FD
--   R2(ENO 员工号, DNO 部门号, PNO 项目号), 一个员工只属于一个部门、一个部门只做一个项目
--     ENO -> DNO, DNO -> PNO     候选键: (ENO, PNO) 是唯一候选键
--     所有属性都是主属性 => **满足 3NF**, 但 DNO 不是超键 => **违反 BCNF**
--
-- 副作用: 本脚本自建 6 张演示表(前缀 demo_*), 结尾全部 DROP, 可重复运行。
-- ============================================================================

SET client_encoding = 'UTF8';
\set ON_ERROR_STOP on
\pset pager off

SELECT '=== 演示开始: bcnf_demo ===' AS phase;


-- ############################################################################
-- 例 1 —— 第 0 步: 建表看数据
-- ############################################################################
SELECT '【例 1 · 第 0 步】建 5 张原始表(未做 BCNF 分解)' AS phase;

DROP TABLE IF EXISTS stu_teacher CASCADE;
DROP TABLE IF EXISTS stu_course  CASCADE;
DROP TABLE IF EXISTS stu         CASCADE;
DROP TABLE IF EXISTS course      CASCADE;
DROP TABLE IF EXISTS teacher     CASCADE;

CREATE TABLE teacher (
    tno   VARCHAR(4)  NOT NULL,
    tname VARCHAR(20) NOT NULL,
    CONSTRAINT pk_teacher PRIMARY KEY (tno)
);
CREATE TABLE course (
    cno   VARCHAR(4)  NOT NULL,
    cname VARCHAR(20) NOT NULL,
    CONSTRAINT pk_course PRIMARY KEY (cno)
);
CREATE TABLE stu (
    sno   VARCHAR(4)  NOT NULL,
    sname VARCHAR(20) NOT NULL,
    CONSTRAINT pk_stu PRIMARY KEY (sno)
);
CREATE TABLE stu_course (
    sno  VARCHAR(4) NOT NULL,
    cno  VARCHAR(4) NOT NULL,
    CONSTRAINT pk_stu_course PRIMARY KEY (sno, cno),
    CONSTRAINT fk_sc_s FOREIGN KEY (sno) REFERENCES stu (sno),
    CONSTRAINT fk_sc_c FOREIGN KEY (cno) REFERENCES course (cno)
);
CREATE TABLE stu_teacher (
    sno  VARCHAR(4) NOT NULL,
    cno  VARCHAR(4) NOT NULL,
    tno  VARCHAR(4) NOT NULL,
    CONSTRAINT pk_stu_teacher PRIMARY KEY (sno, cno),
    CONSTRAINT fk_st_s FOREIGN KEY (sno) REFERENCES stu (sno),
    CONSTRAINT fk_st_c FOREIGN KEY (cno) REFERENCES course (cno),
    CONSTRAINT fk_st_t FOREIGN KEY (tno) REFERENCES teacher (tno)
);

INSERT INTO teacher (tno, tname) VALUES ('T1', '李教授'), ('T2', '王教授');
INSERT INTO course  (cno, cname) VALUES ('C1', '数据库'), ('C2', '算法');
INSERT INTO stu     (sno, sname) VALUES ('S1', '张三'), ('S2', '李四'), ('S3', '王五');

-- 事实: 张三/李四 选数据库(李教授教), 王五 选算法(王教授教)
INSERT INTO stu_course (sno, cno) VALUES ('S1','C1'), ('S2','C1'), ('S3','C2');
INSERT INTO stu_teacher (sno, cno, tno) VALUES
    ('S1','C1','T1'),
    ('S2','C1','T1'),
    ('S3','C2','T2');

SELECT '【例 1】把 5 张表自然连接成一张大表(这就是"违反 BCNF 的关系")' AS phase;

DROP TABLE IF EXISTS demo_wide;
CREATE TABLE demo_wide AS
SELECT st.sno, s.sname, st.cno, c.cname, st.tno, t.tname
  FROM stu_teacher st
  JOIN stu     s ON s.sno = st.sno
  JOIN course  c ON c.cno = st.cno
  JOIN teacher t ON t.tno = st.tno;

SELECT * FROM demo_wide ORDER BY sno;

SELECT '【例 1】冗余度量: (TNO, tname, CNO, cname) 四元组重复了几次' AS phase;

SELECT tno, tname, cno, cname,
       count(*)                        AS 出现次数,
       string_agg(sno, ', ' ORDER BY sno) AS 涉及学生
  FROM demo_wide
 GROUP BY tno, tname, cno, cname
HAVING count(*) > 1
 ORDER BY tno;

SELECT '【例 1】判定 TNO -> CNO 是否成立(反例查询, 应为 0 行)' AS phase;

SELECT tno, count(DISTINCT cno) AS 教了几门课
  FROM demo_wide
 GROUP BY tno
HAVING count(DISTINCT cno) > 1;

-- ############################################################################
-- 例 1 —— 第 1 步: 三种异常都看得见
-- ############################################################################
SELECT '【例 1 · 第 1 步】更新异常: 李教授改个名字要动几行?' AS phase;

-- 用影子表, 不动原表
BEGIN;
CREATE TEMP TABLE tw ON COMMIT DROP AS SELECT * FROM demo_wide;
SELECT count(*) AS 改名需要改动的行数 FROM tw WHERE tno = 'T1';
UPDATE tw SET tname = '李明教授' WHERE tno = 'T1';
ROLLBACK;

SELECT '★ 上面这个数字就是"冗余"的代价: 同一份事实存了几行, 改名就要改几次' AS conclusion;

SELECT '【例 1 · 第 1 步】删除异常: 删掉王五选算法这一行, 会连带丢掉什么?' AS phase;

BEGIN;
CREATE TEMP TABLE dw ON COMMIT DROP AS SELECT * FROM demo_wide;
DELETE FROM dw WHERE sno = 'S3';
SELECT '删除后, 还能查出 T2/王教授 教哪门课吗?' AS question;
SELECT * FROM dw WHERE tno = 'T2';
SELECT count(*) AS T2的痕迹还剩几行 FROM dw WHERE tno = 'T2';
ROLLBACK;

SELECT '★ 0 行 = 王教授教算法这个事实被误删了(它本来和"谁选课"无关)' AS conclusion;

-- ############################################################################
-- 例 1 —— 第 2 步: 数据库能不能拦住"同课两师"?
--   两条 INSERT 都会失败, 但**失败原因是同一个主码** —— 这恰好暴露了边界:
--   主码 (SNO,CNO) 顶多能保证"一个学生在一门课上不会挂两个教师",
--   它管不了"同一门课的不同学生挂了不同教师"这条路。
-- ############################################################################
SELECT '【例 1 · 第 2 步】试着往表里塞"同一门课、第二个教师"' AS phase;

\set ON_ERROR_STOP off

-- 张三在 C1 上再挂一个教师 T2
INSERT INTO stu_teacher (sno, cno, tno) VALUES ('S1','C1','T2');

-- 换个学生也一样: (S2, C1) 这个主码值已存在, 所以只能"改教师", 不能"加一个教师"
INSERT INTO stu_teacher (sno, cno, tno) VALUES ('S2','C1','T2');

\set ON_ERROR_STOP on

SELECT '两条都被主码 pk_stu_teacher (sno, cno) 拦下, 但注意拦住的是"这个主码值已存在"' AS note;

SELECT '【例 1 · 第 2 步】关键的一问: 用什么约束能保证"一门课只有一个教师"? 答案是主码做不到' AS phase;

SELECT '理由: 主码是 (SNO,CNO), 它只能保证同一个学生的同一门课只有一行(因此只有一个教师);' AS 理由1,
       '而"不同学生选同一门课"本来就该是多行, 主码不会阻止它们各写一个教师。' AS 理由2;

SELECT '要不要验证一下当前数据有没有已经出现"同课两师"?' AS question;
SELECT cno, count(DISTINCT tno) AS 教师个数,
       string_agg(DISTINCT tno, '/') AS 教师
  FROM stu_teacher
 GROUP BY cno
HAVING count(DISTINCT tno) > 1;

SELECT '★ 0 行不是"数据库保证了它", 而是这份示例数据恰好没写坏。' AS conclusion,
       '要跨多个学生行去保证 TNO->CNO, 只能靠触发器/可延迟约束, 或者干脆按 BCNF 拆表(见第 3 步)。' AS 说明;

SELECT '【例 1 · 第 2 步】把主码拿掉(影子表), 看看"同课两师"其实长什么样' AS phase;

BEGIN;
CREATE TEMP TABLE shadow_stu_teacher ON COMMIT DROP AS
    SELECT * FROM stu_teacher;

-- 同一个学生 S1 在 C1 上挂两个教师: 只有主码拦得住
INSERT INTO shadow_stu_teacher VALUES ('S1','C1','T1');
INSERT INTO shadow_stu_teacher VALUES ('S1','C1','T2');
-- 更隐蔽的一种: 不同学生、同一门课、挂着不同教师 —— 这行数据在外码层面完全合法
INSERT INTO shadow_stu_teacher VALUES ('S2','C1','T2');

SELECT * FROM shadow_stu_teacher ORDER BY cno, sno, tno;

SELECT cno, count(DISTINCT tno) AS 教师个数,
       string_agg(DISTINCT tno, '/') AS 教师
  FROM shadow_stu_teacher
 GROUP BY cno
HAVING count(DISTINCT tno) > 1;

SELECT '★ 这就是 FD 的违反现场: 课程 C1 同时对应 T1 与 T2, 而 TNO->CNO 要求它只能对应一个。' AS conclusion,
       '外码查了每一行的值是否合法, 却看不见"跨行"的这层约束 —— 所以这类 FD 只能靠拆表或触发器。' AS 说明;
ROLLBACK;

-- ############################################################################
-- 例 1 —— 第 3 步: 拆分(BCNF 分解算法, 逐步)
-- ############################################################################
SELECT '【例 1 · 第 3 步】候选键与违反项' AS phase;

WITH "属性闭包" AS (
    SELECT 'TNO+' AS 左边, '{TNO, CNO}' AS 闭包结果, '缺 SNO/姓名 => TNO 不是超键' AS 说明
    UNION ALL SELECT 'SNO+' , '{SNO, SNAME}',              '缺课程/教师 => SNO 不是超键'
    UNION ALL SELECT 'CNO+' , '{CNO, CNAME}',              '缺人 => CNO 不是超键'
    UNION ALL SELECT '(SNO,CNO)+', '{全部 6 个属性}',      '是候选键'
    UNION ALL SELECT '(SNO,TNO)+', '{全部 6 个属性}',      '是候选键'
)
SELECT * FROM "属性闭包";

SELECT '违反项: TNO -> CNO(左边不是超键, 右边 CNO 是非主属性 => 连 3NF 都违反)' AS 判定;

SELECT '【例 1 · 第 3 步】按 TNO -> CNO 拆成两张表' AS phase;

DROP TABLE IF EXISTS demo_tc;
DROP TABLE IF EXISTS demo_st;
CREATE TABLE demo_tc AS SELECT DISTINCT tno, cno FROM demo_wide;
ALTER TABLE demo_tc ADD PRIMARY KEY (tno);
CREATE TABLE demo_st AS SELECT DISTINCT sno, tno FROM demo_wide;
ALTER TABLE demo_st ADD PRIMARY KEY (sno, tno);

SELECT 'R1(TNO, CNO) —— 教师教哪门课' AS "拆分结果";
SELECT * FROM demo_tc ORDER BY tno;
SELECT 'R2(SNO, TNO) —— 学生跟哪位教师' AS "拆分结果";
SELECT * FROM demo_st ORDER BY sno;

SELECT '【例 1 · 第 3 步】拆完还剩几个关系? 原始 6 个属性哪去了?' AS phase;

SELECT '拆出的表' AS 项, 'demo_tc(TNO,CNO)' AS 内容
UNION ALL SELECT '拆出的表', 'demo_st(SNO,TNO)'
UNION ALL SELECT '不再需要的表', 'stu / course / teacher / stu_course 变成纯字典或重复'
UNION ALL SELECT '★ 关键', 'SNAME/CNAME/TNAME 退回各自的字典表, 大表里不再重复';

-- ############################################################################
-- 例 1 —— 第 4 步: 验证(无损连接 + 保持依赖 + 异常消失)
-- ############################################################################
SELECT '【例 1 · 第 4 步】无损连接: R1 * R2 能不能还原原关系?' AS phase;

SELECT (SELECT count(*) FROM demo_tc JOIN demo_st USING (tno)) AS 连接后行数,
       (SELECT count(*) FROM demo_wide)                        AS 原关系行数,
       CASE WHEN (SELECT count(*) FROM demo_tc JOIN demo_st USING (tno))
               = (SELECT count(*) FROM demo_wide)
            THEN '无损: 行数相同, 没有伪元组'
            ELSE '有损: 出现了伪元组' END AS 结论;

SELECT '【例 1 · 第 4 步】保持依赖: 原 FD 能不能只用这两张表检查?' AS phase;

WITH "逐条检查" AS (
    SELECT 'TNO -> CNO' AS FD, '在 demo_tc 上检查(TNO 是它的候选键)' AS 在哪检查,
           '保持' AS 结论
    UNION ALL SELECT '(SNO,CNO) -> TNO', '跨 demo_st 与 demo_tc, 需连接', '跨表, 但可用触发器/断言检查'
    UNION ALL SELECT 'SNO -> SNAME', '已退回字典表 stu', '由主码保持'
    UNION ALL SELECT 'CNO -> CNAME', '已退回字典表 course', '由主码保持'
)
SELECT * FROM "逐条检查";

SELECT '【例 1 · 第 4 步】异常消失验证: 同一个教师还是存 2 行吗?' AS phase;

SELECT tno, cno, count(*) AS 出现次数 FROM demo_tc GROUP BY tno, cno ORDER BY tno;

SELECT '★ 出现次数恒为 1 => 改名只需改 1 行, 冗余消除' AS conclusion;

-- ############################################################################
-- 例 2 —— 3NF 通过但 BCNF 不通过(更隐蔽的一类)
-- ############################################################################
SELECT '【例 2 · 第 0 步】R2(ENO 员工号, DNO 部门号, PNO 项目号)' AS phase;

DROP TABLE IF EXISTS demo_emp_wide;
CREATE TABLE demo_emp_wide (eno VARCHAR(4), dno VARCHAR(4), pno VARCHAR(4));

-- 事实: 一个员工只属于一个部门, 一个部门只做一个项目
--   张工/李工 同属 D1(做 P1), 王工 属 D2(做 P2)
INSERT INTO demo_emp_wide VALUES
    ('E1','D1','P1'),
    ('E2','D1','P1'),
    ('E3','D2','P2');

SELECT * FROM demo_emp_wide ORDER BY eno;

SELECT '【例 2】冗余: 部门 -> 项目 的关系被存了几遍?' AS phase;

SELECT dno, pno, count(*) AS 出现次数,
       string_agg(eno, ', ' ORDER BY eno) AS 涉及员工
  FROM demo_emp_wide
 GROUP BY dno, pno
 ORDER BY dno;

SELECT 'D1/P1 存了 2 遍: E2 调走后, D1 做 P1 这个事实就会被误删' AS note;

SELECT '【例 2】候选键(唯一): (ENO, PNO)' AS phase;
SELECT 'ENO+ = {ENO, DNO} 不是超键; DNO+ = {DNO, PNO} 不是超键; (ENO,PNO)+ = 全部 3 个属性 => 唯一候选键' AS 推导;

SELECT '【例 2】为什么 3NF 放行、BCNF 不放行?' AS phase;

SELECT 'DNO -> PNO 的左边 DNO 不是超键' AS a, '右边 PNO **是主属性**(属于候选键 (ENO,PNO))' AS b,
       '=> 3NF 放行, BCNF 不放行' AS 结论;

SELECT '【例 2】按 DNO -> PNO 拆: (DNO, PNO) 与 (ENO, DNO)' AS phase;

DROP TABLE IF EXISTS demo_dp;
DROP TABLE IF EXISTS demo_ed;
CREATE TABLE demo_dp AS SELECT DISTINCT dno, pno FROM demo_emp_wide;
ALTER TABLE demo_dp ADD PRIMARY KEY (dno);
CREATE TABLE demo_ed AS SELECT DISTINCT eno, dno FROM demo_emp_wide;
ALTER TABLE demo_ed ADD PRIMARY KEY (eno);

SELECT 'R1(DNO, PNO) —— 哪个部门做哪个项目' AS "拆分结果";
SELECT * FROM demo_dp ORDER BY dno;
SELECT 'R2(ENO, DNO) —— 谁在哪个部门' AS "拆分结果";
SELECT * FROM demo_ed ORDER BY eno;

SELECT '【例 2】无损连接验证' AS phase;
SELECT (SELECT count(*) FROM demo_dp JOIN demo_ed USING (dno)) AS 连接后行数,
       (SELECT count(*) FROM demo_emp_wide)                    AS 原关系行数,
       CASE WHEN (SELECT count(*) FROM demo_dp JOIN demo_ed USING (dno))
               = (SELECT count(*) FROM demo_emp_wide)
            THEN '无损' ELSE '有损' END AS 结论;

SELECT '【例 2】冗余消失验证: D1/P1 现在存几行?' AS phase;
SELECT * FROM demo_dp ORDER BY dno;

-- ############################################################################
-- 收尾
-- ############################################################################
SELECT '【收尾】清理演示表(库保留, 便于你自己反复跑)' AS phase;

DROP TABLE IF EXISTS demo_wide, demo_tc, demo_st,
                     demo_emp_wide, demo_dp, demo_ed;

DROP TABLE IF EXISTS stu_teacher, stu_course, stu, course, teacher;

SELECT '=== 演示结束: 表已全部清理 ===' AS phase;
