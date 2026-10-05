-- ============================================================
-- 7.8.1 特化(specialization)在 PostgreSQL 里怎么落地
--   实体集 person 特化成 employee 与 student
-- 数据库: university (PostgreSQL 17)
-- 教材: 《数据库系统概念》原书第6版·本科教学版(机械工业出版社,
--       Silberschatz / Korth / Sudarshan) 第7章 7.8 扩展的 E-R 特性
--       7.8.1 特化
-- ------------------------------------------------------------
-- E-R 图(教材图 7-21 的简化版):
--
--                person(ID, name, address)
--                          |
--                       [ISA]
--                 +--------+--------+
--            employee              student
--            (salary)             (tot_cred)
--
-- 本脚本要回答的是"这张图怎么变成 PostgreSQL 里的表和数据",共三种方案:
--   方案 A  标准映射: 超类表 + 每个子类一张表, 子类主键同时是外键
--           —— 教材"把 E-R 图转化为关系模式"的标准做法, 约束最完整
--   方案 B  PostgreSQL 专有: 表继承 INHERITS
--           —— 查询方便(查父表自动带出子表), 但唯一/外键约束不跨子表
--   方案 C  单表 + 判别列 + CHECK
--           —— 全特化/不相交表达得最干脆, 但重叠特化表达不了
--
-- 四种特化约束怎么落 SQL(教材 7.8.1 后半段):
--   部分特化 partial     —— 方案 A 默认就是(有的 person 没有子类行)
--   重叠特化 overlapping —— 方案 A 默认就是(同一 ID 可同时在两张子类表)
--   全特化 total         —— 方案 A 用可延迟约束触发器(提交时检查)
--   不相交 disjoint      —— 方案 A 用触发器； 方案 C 用 CHECK 一条搞定
--
-- 运行(两种等价, psql.bat 已改为转发官方 psql.exe):
--   .\psql.bat -f src\main\resources\sql\er_person_specialization.sql
--   psql -U postgres -d university -f src\main\resources\sql\er_person_specialization.sql
--   (psql 全路径 C:\Program Files\PostgreSQL\17\bin\psql.exe, 已在 PATH;
--    故 $$ 函数体、注释里的分号、\set 等元命令都能正常用)
--
-- 说明: 脚本里刻意演示了 7 处"应当报错"的场景, 所以显式 \set ON_ERROR_STOP off,
--       让 psql 报错后继续往下执行; 判断成败要数错误条数, 不要只看退出码。
--       脚本用 er_/er2_/er3_ 前缀建示例表, 与库里已有的 instructor /
--       student / person 等表互不影响, 可重复执行。
-- 实测结论(PostgreSQL 17.11): 见文件末尾。
-- ============================================================

-- 关掉"遇错即止", 让 7 处预期报错后面的步骤继续执行(psql 默认就是 off, 这里写明意图)
\set ON_ERROR_STOP off


-- ============================================================
-- 步骤 0：清理旧对象(保证脚本可重复运行)
-- ============================================================
-- 先删表(表上的触发器随之消失), 再删函数 —— 否则函数还有依赖会删不掉
DROP TABLE    IF EXISTS er3_person_all;
DROP TABLE    IF EXISTS er2_student;
DROP TABLE    IF EXISTS er2_employee;
DROP TABLE    IF EXISTS er2_person;
DROP TABLE    IF EXISTS er_student;
DROP TABLE    IF EXISTS er_employee;
DROP TABLE    IF EXISTS er_person;
DROP FUNCTION IF EXISTS er_fn_require_subclass();
DROP FUNCTION IF EXISTS er_fn_forbid_disjoint();

SELECT '步骤 0 完成：旧对象已清理' AS section;


-- ============================================================
-- 步骤 1：方案 A —— 标准映射(超类表 + 子类表)
--   person  →  er_person(ID, name, address)         超类: 所有共同属性
--   employee →  er_employee(ID, salary)              子类: 自己的附加属性
--   student  →  er_student (ID, tot_cred)            子类: 自己的附加属性
--
--   关键三点(这就是"特化"在关系模型里的落地):
--     ① 子类表里不再重复 name/address —— 靠 person 的 ID 继承；
--     ② 子类表的主键 = 外键 → 子类实体必然"是一个 person"(ISA 语义)；
--     ③ 是否强制"每个 person 都必须属于某个子类"由额外约束决定(步骤 6)。
-- ============================================================
CREATE TABLE er_person (
    ID       CHAR(5)      NOT NULL,
    name     VARCHAR(30)  NOT NULL,
    address  VARCHAR(40),
    CONSTRAINT pk_er_person PRIMARY KEY (ID)
);

CREATE TABLE er_employee (
    ID      CHAR(5)        NOT NULL,
    salary  NUMERIC(8,2)   NOT NULL,
    CONSTRAINT pk_er_employee     PRIMARY KEY (ID),
    CONSTRAINT fk_er_employee_sub FOREIGN KEY (ID)
        REFERENCES er_person (ID) ON DELETE CASCADE,
    CONSTRAINT ck_er_employee_sal CHECK (salary > 0)
);

CREATE TABLE er_student (
    ID        CHAR(5)   NOT NULL,
    tot_cred  INT       NOT NULL,
    CONSTRAINT pk_er_student     PRIMARY KEY (ID),
    CONSTRAINT fk_er_student_sub FOREIGN KEY (ID)
        REFERENCES er_person (ID) ON DELETE CASCADE,
    CONSTRAINT ck_er_student_cred CHECK (tot_cred >= 0)
);

SELECT '步骤 1 完成：方案 A 三张表已建立(子类主键 = 外键指向 er_person)' AS section;


-- ============================================================
-- 步骤 2：插入数据 —— 一次把四种情况都造出来
--   10101 仅雇员          22222 仅学生
--   45565 既是雇员又是学生(重叠特化的证据)
--   83821 都不是          (部分特化的证据: 它是 person 但没有子类行)
--   76543 仅雇员          98345 仅学生
-- ============================================================
INSERT INTO er_person (ID, name, address) VALUES
    ('10101', 'Ravi Srinivasan', 'Madison'),
    ('22222', 'Albert Einstein', 'Princeton'),
    ('45565', 'Sara Katz',       'Madison'),
    ('83821', 'Lena Brandt',     'Chicago'),
    ('76543', 'Ming Zhao',       'Madison'),
    ('98345', 'Ana Silva',       'Austin');

INSERT INTO er_employee (ID, salary) VALUES
    ('10101', 65000.00),
    ('45565', 72000.00),
    ('76543', 58000.00);

INSERT INTO er_student (ID, tot_cred) VALUES
    ('22222', 90),
    ('45565', 32),
    ('98345', 45);

SELECT '步骤 2 完成：6 个 person、3 个 employee、3 个 student(其中 45565 重叠, 83821 无子类)' AS section;


-- ============================================================
-- 步骤 3：用集合运算验证 ISA 语义(这是"特化"的核心)
--   3.1 子集: employee ∪ student ⊆ person      —— 差集应为 0 行
--   3.2 超集差: person − (employee ∪ student)  —— 就是"未特化的人"(部分特化)
--   3.3 重叠: 两个子集的交集                  —— 就是"既是雇员又是学生"的人
-- ============================================================
SELECT '3.1 employee ∪ student ⊆ person ?(应 0 行)' AS section;
SELECT ID FROM (SELECT ID FROM er_employee UNION SELECT ID FROM er_student) AS sub
EXCEPT
SELECT ID FROM er_person;

SELECT '3.2 person − (employee ∪ student) = 未特化的 person(应为 83821)' AS section;
SELECT ID, name FROM er_person
EXCEPT
SELECT p.ID, p.name FROM er_person p
WHERE EXISTS (SELECT 1 FROM er_employee e WHERE e.ID = p.ID)
   OR EXISTS (SELECT 1 FROM er_student  s WHERE s.ID = p.ID);

SELECT '3.3 employee ∩ student = 重叠特化的人(应为 45565)' AS section;
SELECT ID FROM er_employee
INTERSECT
SELECT ID FROM er_student;


-- ============================================================
-- 步骤 4：把特化拼回 E-R 图里的"一个实体一行"
--   教材图 7-21 中的 person 是一个实体: 它有 salary 或 tot_cred。
--   落到三张表后, 用两次 LEFT JOIN 拼回来(不能用 INNER JOIN,
--   否则 83821 会消失)。
-- ============================================================
SELECT p.ID,
       p.name,
       p.address,
       e.salary,
       s.tot_cred,
       CASE WHEN e.ID IS NOT NULL AND s.ID IS NOT NULL THEN '既是雇员又是学生'
            WHEN e.ID IS NOT NULL                      THEN '仅雇员'
            WHEN s.ID IS NOT NULL                      THEN '仅学生'
            ELSE                                            '都不是' END AS 角色
FROM er_person p
LEFT JOIN er_employee e ON e.ID = p.ID
LEFT JOIN er_student  s ON s.ID = p.ID
ORDER BY p.ID;

SELECT '步骤 4 完成：6 行(每行一个 person, 83821 的 salary/tot_cred 均为 NULL)' AS section;


-- ============================================================
-- 步骤 5：外键带来的两条 ISA 语义
--   5.1 先插 employee 再补 person —— 报错(子类实体必须先是 person)
--   5.2 person 删除 → 子类行级联删除(子类行依附于超类行)
-- ============================================================
SELECT '5.1 预期报错：给一个不存在的 person 插 employee' AS section;
INSERT INTO er_employee (ID, salary) VALUES ('00000', 1000.00);

SELECT '5.2 删除 person 98345(它有一个 student 行)' AS section;
DELETE FROM er_person WHERE ID = '98345';
SELECT '5.2 结果：98345 在两处都消失(级联)' AS section;
SELECT (SELECT COUNT(*) FROM er_person  WHERE ID = '98345') AS person_剩,
       (SELECT COUNT(*) FROM er_student WHERE ID = '98345') AS student_剩;

SELECT '5.2 复原：把 98345 插回去, 保持后续步骤数据一致' AS section;
INSERT INTO er_person  (ID, name, address) VALUES ('98345', 'Ana Silva', 'Austin');
INSERT INTO er_student (ID, tot_cred)      VALUES ('98345', 45);


-- ============================================================
-- 步骤 6：特化的四种约束怎么落 SQL
--   方案 A 默认就是「部分 + 重叠」:
--     · 部分: er_person 里允许存在没有任何子类行的人(如 83821)；
--     · 重叠: 同一个 ID 可以同时在 er_employee 和 er_student(如 45565)。
--   要改成「不相交」或「全特化」, 标准映射本身做不到, 得加约束。
-- ============================================================

-- 6.1 不相交(disjoint): 同一个 ID 不允许既是雇员又是学生
--     —— BEFORE INSERT 触发器, 立刻拒绝
CREATE OR REPLACE FUNCTION er_fn_forbid_disjoint() RETURNS trigger AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM er_employee WHERE ID = NEW.ID) THEN
        RAISE EXCEPTION '违反不相交特化(disjoint): person % 已经是 employee, 不能再是 student', NEW.ID;
    END IF;
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_er3_disjoint_guard ON er_student;
CREATE TRIGGER trg_er3_disjoint_guard
    BEFORE INSERT ON er_student
    FOR EACH ROW EXECUTE FUNCTION er_fn_forbid_disjoint();

SELECT '6.1 预期报错：把已是雇员的 76543 再插进 student' AS section;
INSERT INTO er_student (ID, tot_cred) VALUES ('76543', 10);

SELECT '6.1 结果：er_student 里没有 76543(触发器拦住了)' AS section;
SELECT COUNT(*) AS student_76543行数 FROM er_student WHERE ID = '76543';


-- 6.2 全特化(total): 每个 person 在提交时必须至少属于一个子类
--     —— 可延迟的约束触发器: 语句时不查, 提交(COMMIT)时统一查,
--        这样"先插 person、再插子类行"的正常顺序不会被误杀。
CREATE OR REPLACE FUNCTION er_fn_require_subclass() RETURNS trigger AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM er_employee WHERE ID = NEW.ID)
       AND NOT EXISTS (SELECT 1 FROM er_student WHERE ID = NEW.ID) THEN
        RAISE EXCEPTION '违反全特化(total): person % 既不是 employee 也不是 student', NEW.ID;
    END IF;
    RETURN NULL;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_er_total ON er_person;
CREATE CONSTRAINT TRIGGER trg_er_total
    AFTER INSERT ON er_person
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION er_fn_require_subclass();

SELECT '6.2 预期报错：只插 person 99999, 不给它任何子类行, 提交时报错' AS section;
BEGIN;
INSERT INTO er_person (ID, name, address) VALUES ('99999', 'Test Person', 'Nowhere');
COMMIT;
-- COMMIT 失败时 PostgreSQL 已经把整笔事务回滚掉, 会话回到空闲状态, 无需再 ROLLBACK

SELECT '6.2 结果：整笔事务被回滚, 99999 没进去' AS section;
SELECT COUNT(*) AS person_99999行数 FROM er_person WHERE ID = '99999';

SELECT '6.2 对照：正常顺序(先 person 后 employee)在同一笔事务里提交成功' AS section;
BEGIN;
INSERT INTO er_person   (ID, name, address) VALUES ('77777', 'New Hire', 'Madison');
INSERT INTO er_employee (ID, salary)          VALUES ('77777', 51000.00);
COMMIT;

SELECT ID, name, salary FROM er_employee e JOIN er_person p USING (ID) WHERE e.ID = '77777';

SELECT '6.3 清理：撤掉这两个约束演示(示例表恢复成"部分 + 重叠")' AS section;
DROP TRIGGER  IF EXISTS trg_er3_disjoint_guard ON er_student;
DROP TRIGGER  IF EXISTS trg_er_total ON er_person;
DROP FUNCTION IF EXISTS er_fn_forbid_disjoint();
DROP FUNCTION IF EXISTS er_fn_require_subclass();
DELETE FROM er_person WHERE ID = '77777';


-- ============================================================
-- 步骤 7：方案 B —— PostgreSQL 表继承 INHERITS
--   把 person 当父表, employee/student 用 INHERITS 继承它。
--   好处: 查父表 = 查"所有 person"(子表行自动带出来)。
--   代价: 唯一约束/主键/外键不会继承到子表 —— 见 7.3 的坑。
-- ============================================================
DROP TABLE IF EXISTS er2_student;
DROP TABLE IF EXISTS er2_employee;
DROP TABLE IF EXISTS er2_person;

CREATE TABLE er2_person (
    ID       CHAR(5)      NOT NULL,
    name     VARCHAR(30)  NOT NULL,
    address  VARCHAR(40),
    CONSTRAINT pk_er2_person PRIMARY KEY (ID)
);
CREATE TABLE er2_employee (
    salary NUMERIC(8,2) NOT NULL
) INHERITS (er2_person);
CREATE TABLE er2_student (
    tot_cred INT NOT NULL
) INHERITS (er2_person);

INSERT INTO er2_person   (ID, name, address) VALUES ('83821', 'Lena Brandt', 'Chicago');
INSERT INTO er2_employee (ID, name, address, salary)   VALUES
    ('10101', 'Ravi Srinivasan', 'Madison',   65000.00),
    ('45565', 'Sara Katz',       'Madison',   72000.00),
    ('76543', 'Ming Zhao',       'Madison',   58000.00);
INSERT INTO er2_student  (ID, name, address, tot_cred) VALUES
    ('22222', 'Albert Einstein', 'Princeton', 90),
    ('45565', 'Sara Katz',       'Madison',   32),
    ('98345', 'Ana Silva',       'Austin',    45);

SELECT '7.1 查父表 er2_person: 7 行 —— 6 个人, 但重叠的 45565 在两棵子树里各占一行(tableoid 显示来自哪张表)' AS section;
SELECT tableoid::regclass AS 来自, ID, name FROM er2_person ORDER BY ID;

SELECT '7.2 加 ONLY 才是"只看真正直接插进父表的行": 1 行(83821)' AS section;
SELECT ID, name FROM ONLY er2_person ORDER BY ID;

SELECT '7.3 坑：子表没继承主键 → 同一个 ID 可以在 er2_employee 里插两次' AS section;
INSERT INTO er2_employee (ID, name, address, salary) VALUES ('10101', '重复的人', '重复地址', 1.00);
SELECT ID, COUNT(*) AS 行数 FROM er2_employee GROUP BY ID HAVING COUNT(*) > 1;
SELECT '7.3 清理重复行' AS section;
DELETE FROM ONLY er2_employee WHERE ID = '10101' AND name = '重复的人';

SELECT '7.4 注意：INHERITS 里外键也无法指向"整棵子树", 所以 IS-A 约束比方案 A 弱' AS section;


-- ============================================================
-- 步骤 8：方案 C —— 单表 + 判别列 + CHECK
--   一张表装下 person 的全部属性(共同属性 + 两个子类的附加属性都变成列),
--   用 ptype 判别是雇员还是学生。判别列单值 ⇒ 天然不相交；
--   ptype NOT NULL + CHECK ⇒ 天然全特化。
-- ============================================================
DROP TABLE IF EXISTS er3_person_all;

CREATE TABLE er3_person_all (
    ID       CHAR(5)     NOT NULL,
    name     VARCHAR(30) NOT NULL,
    address  VARCHAR(40),
    ptype    VARCHAR(2)  NOT NULL,
    salary   NUMERIC(8,2),
    tot_cred INT,
    CONSTRAINT pk_er3_person_all PRIMARY KEY (ID),
    CONSTRAINT ck_er3_type     CHECK (ptype IN ('E','S')),
    CONSTRAINT ck_er3_salary   CHECK (ptype <> 'E' OR salary   IS NOT NULL),
    CONSTRAINT ck_er3_totcred  CHECK (ptype <> 'S' OR tot_cred IS NOT NULL),
    CONSTRAINT ck_er3_disjoint CHECK ((ptype = 'E' AND tot_cred IS NULL)
                                   OR (ptype = 'S' AND salary   IS NULL))
);

INSERT INTO er3_person_all (ID, name, address, ptype, salary, tot_cred) VALUES
    ('10101', 'Ravi Srinivasan', 'Madison',   'E', 65000.00, NULL),
    ('76543', 'Ming Zhao',       'Madison',   'E', 58000.00, NULL),
    ('22222', 'Albert Einstein', 'Princeton', 'S', NULL,      90),
    ('98345', 'Ana Silva',       'Austin',    'S', NULL,      45);

SELECT '8.1 单表查询: 一个 SELECT 就能列出"所有 person + 各自角色"' AS section;
SELECT ID, name, ptype, salary, tot_cred FROM er3_person_all ORDER BY ID;

SELECT '8.2 预期报错：ptype = ''X'' 被 CHECK 拒绝(判别列只允许 E/S；实测报出的是 ck_er3_disjoint)' AS section;
INSERT INTO er3_person_all VALUES ('11111', 'Bad Row', 'Nowhere', 'X', NULL, NULL);

SELECT '8.3 预期报错：雇员却带 tot_cred → 被 ck_er3_disjoint 拒绝(不相交)' AS section;
INSERT INTO er3_person_all VALUES ('11111', 'Bad Row', 'Nowhere', 'E', 50000.00, 30);

SELECT '8.4 预期报错：学生却没给 tot_cred → 被 ck_er3_totcred 拒绝' AS section;
INSERT INTO er3_person_all VALUES ('11111', 'Bad Row', 'Nowhere', 'S', NULL, NULL);

SELECT '8.5 代价：ptype 单值装不下"既是雇员又是学生"(45565) 和"都不是"(83821)' AS section;
INSERT INTO er3_person_all VALUES ('45565', 'Sara Katz', 'Madison', 'ES', 72000.00, 32);


-- ============================================================
-- 步骤 9：三种方案对照 + 最终结构确认
-- ============================================================
SELECT * FROM (VALUES
    ('A 标准映射(超类表+子类表)', '外键保证子类⊆超类', '天然支持',  '天然支持',   '触发器(DEFERRABLE)'),
    ('B 表继承 INHERITS',        '无(约束不继承)',    '天然支持',  '天然支持',   '触发器(DEFERRABLE)'),
    ('C 单表+判别列+CHECK',       'CHECK 保证',        '装不下',    'CHECK',     'NOT NULL + CHECK')
) AS t(方案, 子集约束, 重叠特化, 不相交, 全特化);

SELECT table_name, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = current_schema()
  AND table_name IN ('er_person','er_employee','er_student')
ORDER BY table_name, ordinal_position;

SELECT '行数汇总' AS section, 'er_person' AS 表, COUNT(*) AS 行数 FROM er_person
UNION ALL SELECT '行数汇总', 'er_employee',      COUNT(*) FROM er_employee
UNION ALL SELECT '行数汇总', 'er_student',       COUNT(*) FROM er_student
UNION ALL SELECT '行数汇总', 'er2_person(含子表)', COUNT(*) FROM er2_person
UNION ALL SELECT '行数汇总', 'er3_person_all',   COUNT(*) FROM er3_person_all
ORDER BY 表;


-- ============================================================
-- 步骤 10：清理(示例表默认保留)
--   想清干净请手工按顺序执行:
--     DROP TABLE er3_person_all；
--     DROP TABLE er2_student； DROP TABLE er2_employee； DROP TABLE er2_person；
--     DROP TABLE er_student；  DROP TABLE er_employee；  DROP TABLE er_person；
-- ============================================================
SELECT '脚本执行完毕：方案 A/B/C 的示例表已建好并留库' AS section;


-- ============================================================
-- 实测结论(psql 17.11 客户端 + PostgreSQL 17.11 服务端 / university 库, 实跑见脚本输出)：
--   · 步骤 3.1 employee ∪ student ⊆ person: 0 行(外键保证)；
--     3.2 未特化的 person: 83821； 3.3 重叠特化的人: 45565；
--   · 步骤 4 拼回实体视角: 6 行, 83821 的 salary 与 tot_cred 均为 NULL；
--   · 步骤 5.1 给不存在的 person 插 employee 被外键拒绝；
--     5.2 删除 person 98345 后, er_student 中的 98345 一并级联删除；
--   · 步骤 6.1 把已是雇员的 76543 插进 student 被触发器拒绝(不相交)；
--     6.2 只插 person 99999 不提子类行, COMMIT 时报"违反全特化(total)",
--         整笔事务回滚； 先 person 后 employee 的正常顺序提交成功；
--   · 步骤 7.1 查父表 er2_person 得 7 行(6 个人, 重叠的 45565 占两行), 加 ONLY 得 1 行；
--     7.3 子表没继承主键, ID 10101 在 er2_employee 里能插两行；
--   · 步骤 8.2/8.3/8.4 ptype=’X’、雇员带 tot_cred、学生无 tot_cred 三种
--     非法行均被 CHECK 拒绝(8.2 实测报 ck_er3_disjoint)； 8.5 ptype=’ES’ 被拒 —— 单表装不下重叠特化；
--   · 最终行数: er_person 6 / er_employee 3 / er_student 3 /
--     er2_person(含子表) 7 / er3_person_all 4；
--   · 连跑两遍, 每遍恰好 7 条预期报错(5.1、6.1、6.2、8.2、8.3、8.4、8.5), 无其它报错, 可重复执行；
--   · 两条执行通道都实测过(结果一致):
--       .\psql.bat -f <本脚本>              → 官方 psql.exe, 7 报错 / 0 警告
--       PSQL_BAT_FORCE_JDBC=1 + psql.bat    → JDBC 兜底版, 7 报错, 并提示跳过 \set 元命令
-- ============================================================
