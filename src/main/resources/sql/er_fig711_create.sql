-- ============================================================
-- 图7-11 的 E-R 图怎么变成表 —— 复合属性 / 多值属性 / 派生属性
-- 数据库: university (PostgreSQL 17)
-- 教材: 《数据库系统概念》原书第6版·本科教学版(机械工业出版社,
--       Silberschatz / Korth / Sudarshan) 第7章 数据库设计与 E-R 模型
--       · 复杂属性(复合属性、多值属性、派生属性) · 图 7-11
-- 姊妹脚本: er_fig711_create_mysql.sql(MySQL 8.0 等价版)
-- ------------------------------------------------------------
-- 图 7-11 中实体集 instructor 的属性:
--   ID
--   name ( first_name, middle_initial, last_name )          复合属性
--   address ( street ( street_number, street_name, apt_number ),
--             city, state, zip )                            复合属性(可嵌套)
--   { phone_number }                                        多值属性(花括号)
--   date_of_birth                                           普通属性
--   age ( )                                                 派生属性(括号)
--
-- 转化成关系模式的三条规则(教材第7章 复杂属性 与
-- “把 E-R 图转化为关系模式”):
--   规则1 复合属性 —— 不建新表, 把叶子属性展平成若干列。
--         name    → first_name, middle_initial, last_name
--         address → street_number, street_name, apt_number, city, state, zip
--         中间的 name / address / street 不再单独占一列。
--   规则2 多值属性 —— 必须单独建一张表, 主键 = 宿主实体主键 + 该属性本身,
--         对宿主主键建外键。一行只放一个电话。
--   规则3 派生属性 —— 不存储(不建列), 用的时候现算, 或放进视图/生成列。
--
-- 本脚本用 er_ 前缀建独立示例表, 与库里已有的 instructor 表互不影响:
--   表 er_instructor        —— 对应图中实体集 instructor
--   表 er_instructor_phone  —— 对应它的多值属性 {phone_number}
-- 在新设计里这两张表就叫 instructor 与 instructor_phone。
--
-- 运行:
--   .\psql.bat -f src\main\resources\sql\er_fig711_create.sql
--
-- 实测结论(PostgreSQL 17.11): 见文件末尾。
-- ============================================================


-- ============================================================
-- 步骤 0：清理旧对象(保证脚本可重复运行)
-- ============================================================
DROP VIEW  IF EXISTS v_er_instructor_full;
DROP TABLE IF EXISTS er_instructor_bad;
DROP TABLE IF EXISTS er_instructor_phone;
DROP TABLE IF EXISTS er_instructor;

SELECT '步骤 0 完成：旧对象已清理' AS section;


-- ============================================================
-- 步骤 1：主表 —— 复合属性展平, 派生属性不建列
--   对应图 7-11 中除 {phone_number} 和 age() 之外的全部属性。
--   注意两件事：
--     ① name 和 address 都“消失”了, 只剩它们的叶子属性；
--     ② 表里没有 age 这一列 —— 派生属性不存储。
--   约束: 主键 ID, zip 必须是 5 位数字, 门牌号必须是数字(可选属性允许 NULL)。
-- ============================================================
CREATE TABLE er_instructor (
    ID              CHAR(5)      NOT NULL,
    first_name      VARCHAR(20)  NOT NULL,
    middle_initial  CHAR(1)          NULL,
    last_name       VARCHAR(20)  NOT NULL,
    street_number   VARCHAR(10)      NULL,
    street_name     VARCHAR(30)      NULL,
    apt_number      VARCHAR(10)      NULL,
    city            VARCHAR(20)      NULL,
    state           VARCHAR(20)      NULL,
    zip             CHAR(5)          NULL,
    date_of_birth   DATE             NULL,
    CONSTRAINT pk_er_instructor      PRIMARY KEY (ID),
    CONSTRAINT ck_er_instructor_zip  CHECK (zip IS NULL OR zip ~ '^[0-9]{5}$'),
    CONSTRAINT ck_er_instructor_apt  CHECK (apt_number IS NULL OR apt_number ~ '^[0-9]+$')
);

SELECT '步骤 1 完成：er_instructor 已建立(name/address 展平为 10 列, 无 age 列)' AS section;


-- ============================================================
-- 步骤 2：多值属性单独建表
--   主键 = (ID, phone_number)：同一个电话可以被两位教师共用, 所以
--   phone_number 自己不能当主键(教材里对此有说明)。
--   外键指向 er_instructor(ID)：教师的记录一删, 他的电话一并删除。
--   CHECK 把电话格式约束成 3 位区号-3 位-4 位, 这一列只放一个号码。
-- ============================================================
CREATE TABLE er_instructor_phone (
    ID            CHAR(5)      NOT NULL,
    phone_number  VARCHAR(15)  NOT NULL,
    CONSTRAINT pk_er_instructor_phone PRIMARY KEY (ID, phone_number),
    CONSTRAINT fk_er_instructor_phone FOREIGN KEY (ID)
        REFERENCES er_instructor (ID) ON DELETE CASCADE,
    CONSTRAINT ck_er_instructor_phone
        CHECK (phone_number ~ '^[0-9]{3}-[0-9]{3}-[0-9]{4}$')
);

SELECT '步骤 2 完成：er_instructor_phone 已建立(联合主键 + 外键 + 格式约束)' AS section;


-- ============================================================
-- 步骤 3：插入示例数据
--   10101 两位电话, 22222 与 83821 共用一个号码(所以电话不能作主键),
--   45565 一个电话都没有(多值属性可以取 0 个值),
--   22222 没有中间名与公寓号 → 用 NULL 表示“该属性无值”。
-- ============================================================
INSERT INTO er_instructor
    (ID, first_name, middle_initial, last_name,
     street_number, street_name, apt_number, city, state, zip, date_of_birth)
VALUES
    ('10101', 'Ravi',   'R', 'Srinivasan', '12', 'Elm',    '20', 'Madison', 'WI', '53715', DATE '1958-03-12'),
    ('22222', 'Albert', NULL, 'Einstein',  '45', 'Oak',    NULL, 'Madison', 'WI', '53706', DATE '1962-07-01'),
    ('45565', 'Sara',   'J', 'Katz',       '7',  'Maple',  '3',  'Madison', 'WI', '53711', DATE '1975-11-23'),
    ('83821', 'Lena',   'M', 'Brandt',     '88', 'Cedar',  '12', 'Madison', 'WI', '53719', DATE '1980-01-30');

INSERT INTO er_instructor_phone (ID, phone_number) VALUES
    ('10101', '608-555-1234'),
    ('10101', '608-555-9876'),
    ('22222', '608-555-4321'),
    ('83821', '608-555-4321');

SELECT '步骤 3 完成：4 位教师、4 个电话(其中一位无电话, 两位共用号码)' AS section;


-- ============================================================
-- 步骤 4：把多值属性拼回“一个实体一行”
--   图 7-11 是一个实体的样子, 落到两张表后要用连接 + 聚集拼回来。
--   必须用 LEFT JOIN：没有电话的教师(45565)也要出现在结果里。
-- ============================================================
SELECT i.ID,
       i.first_name,
       COALESCE(i.middle_initial, '') AS middle_initial,
       i.last_name,
       i.street_number,
       i.street_name,
       COALESCE(i.apt_number, '')     AS apt_number,
       i.city,
       i.state,
       i.zip,
       string_agg(p.phone_number, ', ' ORDER BY p.phone_number) AS phone_numbers,
       i.date_of_birth
FROM er_instructor i
LEFT JOIN er_instructor_phone p ON p.ID = i.ID
GROUP BY i.ID, i.first_name, i.middle_initial, i.last_name,
         i.street_number, i.street_name, i.apt_number,
         i.city, i.state, i.zip, i.date_of_birth
ORDER BY i.ID;

SELECT '步骤 4 完成：4 行(每人一行, 电话用逗号拼回；45565 电话为空)' AS section;


-- ============================================================
-- 步骤 5：派生属性 age —— 不建列, 用的时候现算
--   PostgreSQL 的 age(生日) 直接给出“X 年 X 月 X 天”, 取年即 age 属性。
--   也可以把“查询 + 现算”固化成视图, 对外就像图 7-11 那个实体一样。
-- ============================================================
SELECT ID,
       last_name,
       date_of_birth,
       age(date_of_birth)                            AS 精确年龄,
       EXTRACT(YEAR FROM age(date_of_birth))::int    AS age
FROM er_instructor
ORDER BY ID;

CREATE VIEW v_er_instructor_full AS
SELECT i.ID,
       i.first_name,
       i.middle_initial,
       i.last_name,
       i.street_number,
       i.street_name,
       i.apt_number,
       i.city,
       i.state,
       i.zip,
       (SELECT string_agg(p.phone_number, ', ' ORDER BY p.phone_number)
          FROM er_instructor_phone p
         WHERE p.ID = i.ID)                          AS phone_numbers,
       i.date_of_birth,
       EXTRACT(YEAR FROM age(i.date_of_birth))::int  AS age
FROM er_instructor i;

SELECT '步骤 5 完成：视图 v_er_instructor_full 把两张表 + 派生属性合成了一个实体' AS section;

SELECT ID, last_name, phone_numbers, date_of_birth, age
FROM v_er_instructor_full
ORDER BY ID;


-- ============================================================
-- 步骤 6：(可选)把 age 固化成生成列
--   派生属性原则上不存。如果确实要存(例如为了建索引), 只能写“截至某个
--   固定基准日期的年龄”, 因为生成列要求表达式不可变：
--     · age(date_of_birth)        依赖今天         → 被拒绝
--     · age(DATE '2026-01-01', date_of_birth) → 仍然是 stable, 也被拒绝
--       (实测报错: 生成表达式不是不可变的)
--   所以改成“年份相减, 生日还没到再减 1”这种纯算术写法:
--     2026 − 出生年 − (出生月日 > 1月1日 ? 1 : 0)
-- ============================================================
ALTER TABLE er_instructor
    ADD COLUMN age_at_2026_01_01 INT
    GENERATED ALWAYS AS (
        2026 - EXTRACT(YEAR FROM date_of_birth)::int
             - CASE WHEN (EXTRACT(MONTH FROM date_of_birth), EXTRACT(DAY FROM date_of_birth))
                         > (1::numeric, 1::numeric)
                    THEN 1 ELSE 0 END
    ) STORED;

SELECT ID, last_name, date_of_birth, age_at_2026_01_01
FROM er_instructor
ORDER BY ID;

SELECT '步骤 6 完成：生成列 age_at_2026_01_01 已加入(基准日期固定, 不能是 CURRENT_DATE)' AS section;


-- ============================================================
-- 步骤 7：反例 —— 把多值属性塞进一列会怎样
--   建一张坏表: phone_number 一列里用逗号塞好几个号码。
--   演示三件事：
--     ① 精确查找查不到(808-555-1234 这种写法根本不在列里)；
--     ② 只能 LIKE 模糊查, 于是 608-555-12340 被误当成 608-555-1234；
--     ③ 无法为“单个号码”建 UNIQUE / CHECK, 也没法统计“有几个人有两个电话”。
-- ============================================================
CREATE TABLE er_instructor_bad (
    ID            CHAR(5)     NOT NULL PRIMARY KEY,
    last_name     VARCHAR(20) NOT NULL,
    phone_number  VARCHAR(50)              -- 一个列里塞多个电话, 违反第一范式
);

INSERT INTO er_instructor_bad (ID, last_name, phone_number) VALUES
    ('10101', 'Srinivasan', '608-555-1234, 608-555-9876'),
    ('45565', 'Katz',       '608-555-12340');

SELECT '反例 ① 精确查找 608-555-1234 —— 0 行, 号码被埋在一串字符里' AS section;
SELECT * FROM er_instructor_bad WHERE phone_number = '608-555-1234';

SELECT '反例 ② 只能用 LIKE —— 2 行, 其中 45565 的号码其实是 608-555-12340(误命中)' AS section;
SELECT * FROM er_instructor_bad WHERE phone_number LIKE '%608-555-1234%';

DROP TABLE er_instructor_bad;

SELECT '步骤 7 完成：反例表已删除。多值属性必须单独建表' AS section;


-- ============================================================
-- 步骤 8：最终结构确认
-- ============================================================
SELECT table_name, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = current_schema()
  AND table_name IN ('er_instructor', 'er_instructor_phone')
ORDER BY table_name, ordinal_position;

SELECT 'er_instructor 行数' AS section, COUNT(*) AS rows FROM er_instructor
UNION ALL
SELECT 'er_instructor_phone 行数', COUNT(*) FROM er_instructor_phone;


-- ============================================================
-- 步骤 9：清理(示例表默认保留)
--   想清干净请手工执行两条语句 —— 先删视图 v_er_instructor_full,
--   再删表 er_instructor_phone 与 er_instructor(顺序不能反, 有外键依赖)。
-- ============================================================
SELECT '脚本执行完毕：er_instructor + er_instructor_phone 已建好并留库' AS section;


-- ============================================================
-- 实测结论(PostgreSQL 17.11, 临时库 erdemo_check, 实跑 0 报错)：
--   · er_instructor 4 行 / er_instructor_phone 4 行；
--   · 步骤 4 与视图 v_er_instructor_full 均为 4 行, 10101 电话
--     '608-555-1234, 608-555-9876', 45565 电话为 NULL；
--   · 派生属性 age 现算：10101 → 68, 22222 → 64, 45565 → 50, 83821 → 46
--     (以 2026-09-19 为当天计算)；
--   · 生成列 age_at_2026_01_01：1958-03-12 → 67, 1980-01-30 → 45；
--   · 反例表精确查找 0 行、LIKE 查 2 行(含 1 行误命中), 印证多值属性
--     必须单独建表；
--   · CHECK 约束实测拦截：zip='ABCDE' 与 phone_number='abc' 都被拒绝；
--   · 连跑三遍 0 报错, 脚本可重复执行。
-- ============================================================
