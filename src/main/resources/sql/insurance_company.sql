-- ============================================================
-- 实践习题 7.1 车辆保险公司 —— E-R 图在 PostgreSQL 里的完整落地
-- 数据库: insurance_demo (PostgreSQL 17.11)
-- 教材: 《数据库系统概念》第7章 数据库设计和 E-R 模型 · 实践习题 7.1
-- 配套图: db-homework/vehicle-insurance-er.html / .svg / .png
-- ------------------------------------------------------------
-- E-R 图(陈氏记号)回顾:
--   实体集: 客户 / 车辆 / 事故 / 保险单 / 支付(弱实体)
--   联系集: 拥有  客户 1 —— N 车辆      (客户全参与, 车辆全参与)
--           涉及  车辆 M —— N 事故      (车辆部分参与, 事故全参与)
--           承保  保险单 M —— N 车辆    (保险单全参与, 车辆部分参与)
--           缴费  保险单 1 —— N 支付    (标识联系, 双方全参与)
--   支付的属性: 缴费时间段(起始日/截止日)、到期日、缴费日、金额
--   支付的部分键: 支付序号;  完整主键 = 保单号 + 支付序号
--
-- 映射规则(教材第7章"把 E-R 图转化为关系模式"):
--   1:N 联系  → 把 1 端主键并入 N 端做外键(客户号→车辆, 保单号→支付)
--   M:N 联系  → 为联系集单独建表, 主键 = 两端主键的组合
--   弱实体    → 主键 = 标识联系另一端的主键 + 部分键
--   复合属性  → 展平成列(缴费时间段 → period_from / period_to)
--   "至少一条"(全参与) → 外键 NOT NULL + 可延迟约束触发器(见步骤 6)
--
-- 运行(先建空库, 再跑脚本):
--   .\psql.bat "DROP DATABASE IF EXISTS insurance_demo"
--   .\psql.bat "CREATE DATABASE insurance_demo"
--   .\psql.bat -d insurance_demo -f src\main\resources\sql\insurance_company.sql
--
-- 表名不加前缀: 这是独立库, 不存在与既有对象撞名的问题
--   (仓库里 er_fig711_*、er_person_specialization 用的是 er_ 前缀,
--    那是为了在 university 库里与旧表区分; 本脚本整套建在 insurance_demo,
--    直接用业务名 customer / vehicle / accident / policy / payment)。
--   若要把它跑进 university 库, 请先确认那里没有同名表。
--
-- 实测结论(PostgreSQL 17.11): 见文件末尾。
-- ============================================================

SET client_encoding = 'UTF8';      -- 脚本本身是 UTF-8, 避免中文入库变乱码

-- 建表/建约束阶段"遇错即止": 任何一处 DDL 失败都立刻停下,
-- 免得函数没建成、触发器静默缺失这类问题被后面的语句盖过去。
-- (步骤 10 的反例测试前会再关掉, 那里本来就要求报错。)
\set ON_ERROR_STOP on


-- ============================================================
-- 步骤 0：清理旧对象(保证脚本可重复运行)
--   先删视图与函数(表被删时其触发器会跟着消失), 再按外键依赖倒序删表。
-- ============================================================
DROP VIEW  IF EXISTS v_payment_status;
DROP VIEW  IF EXISTS v_policy_detail;
DROP VIEW  IF EXISTS v_vehicle_accident_detail;
DROP TABLE IF EXISTS payment          CASCADE;
DROP TABLE IF EXISTS policy_vehicle   CASCADE;
DROP TABLE IF EXISTS vehicle_accident CASCADE;
DROP TABLE IF EXISTS policy           CASCADE;
DROP TABLE IF EXISTS accident         CASCADE;
DROP TABLE IF EXISTS vehicle          CASCADE;
DROP TABLE IF EXISTS customer         CASCADE;
DROP FUNCTION IF EXISTS assert_min_one_child() CASCADE;

SELECT '步骤 0 完成：旧对象已清理' AS section;


-- ============================================================
-- 步骤 1：实体集 客户
--   属性: 客户号(主键)、姓名、地址、联系电话
-- ============================================================
CREATE TABLE customer (
    cust_id     INTEGER      NOT NULL,                 -- 客户号
    cust_name   VARCHAR(40)  NOT NULL,                 -- 姓名
    address     VARCHAR(120)     NULL,                 -- 地址
    phone       VARCHAR(20)      NULL,                 -- 联系电话
    CONSTRAINT pk_customer PRIMARY KEY (cust_id),
    CONSTRAINT ck_customer_phone CHECK (phone IS NULL OR phone ~ '^[0-9+\-]{6,20}$')
);
COMMENT ON TABLE  customer IS '实体集 客户(投保人/车主)';
COMMENT ON COLUMN customer.cust_id IS '主键: 客户号';

SELECT '步骤 1 完成：customer' AS section;


-- ============================================================
-- 步骤 2：实体集 车辆 + 联系集 拥有(客户 1 : N 车辆)
--   · 车辆主键是车牌号；
--   · "拥有"是 1:N, 把 1 端(客户)主键并入 N 端(车辆)当外键 cust_id；
--   · 车辆全参与 → cust_id NOT NULL(每辆车必有车主)；
--   · 客户全参与(每个客户至少一辆车)SQL 表达不了, 交给步骤 6 的触发器。
-- ============================================================
CREATE TABLE vehicle (
    license_no  VARCHAR(16)  NOT NULL,                 -- 车牌号
    model       VARCHAR(40)  NOT NULL,                 -- 车型/品牌型号
    model_year  SMALLINT     NOT NULL,                 -- 出厂年份
    vin         VARCHAR(32)      NULL,                 -- 车架号(业务上的候选键)
    cust_id     INTEGER      NOT NULL,                 -- 车主(外键 → 客户)
    CONSTRAINT pk_vehicle PRIMARY KEY (license_no),
    CONSTRAINT uq_vehicle_vin UNIQUE (vin),
    CONSTRAINT fk_vehicle_owner FOREIGN KEY (cust_id)
        REFERENCES customer (cust_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_vehicle_year CHECK (model_year BETWEEN 1950 AND 2100)
);
COMMENT ON TABLE  vehicle IS '实体集 车辆; 通过 cust_id 实现"拥有(1:N)"联系';
COMMENT ON COLUMN vehicle.vin IS '车架号: 候选键, 允许 NULL(旧车可能没登记)';

SELECT '步骤 2 完成：vehicle(拥有 1:N 已并入外键)' AS section;


-- ============================================================
-- 步骤 3：实体集 事故 + 联系集 涉及(车辆 M : N 事故)
--   · M:N 联系必须单独建表, 主键 = (车牌号, 事故号)；
--   · 车辆部分参与(可以为零次事故) → 车辆这一端没有额外约束；
--   · 事故全参与(每起事故至少涉及一辆车) → 步骤 6 的触发器。
-- ============================================================
CREATE TABLE accident (
    accident_no    INTEGER       NOT NULL,             -- 事故号
    occurred_on    DATE          NOT NULL,             -- 发生日期
    location       VARCHAR(60)   NOT NULL,             -- 事故地点
    description    VARCHAR(200)      NULL,             -- 事故描述
    damage_amount  NUMERIC(12,2) NOT NULL DEFAULT 0,   -- 损失金额
    CONSTRAINT pk_accident PRIMARY KEY (accident_no),
    CONSTRAINT ck_accident_amount CHECK (damage_amount >= 0)
);
COMMENT ON TABLE accident IS '实体集 事故(独立实体, 不是车辆的弱实体: 一起事故可涉及多辆车)';

CREATE TABLE vehicle_accident (
    license_no   VARCHAR(16) NOT NULL,                 -- 车辆(外键)
    accident_no  INTEGER     NOT NULL,                 -- 事故(外键)
    CONSTRAINT pk_vehicle_accident PRIMARY KEY (license_no, accident_no),
    CONSTRAINT fk_va_vehicle  FOREIGN KEY (license_no)
        REFERENCES vehicle (license_no)  ON DELETE RESTRICT,
    CONSTRAINT fk_va_accident FOREIGN KEY (accident_no)
        REFERENCES accident (accident_no) ON DELETE RESTRICT
);
COMMENT ON TABLE vehicle_accident IS '联系集 涉及(M:N)的实现表';

SELECT '步骤 3 完成：accident + vehicle_accident(涉及 M:N)' AS section;


-- ============================================================
-- 步骤 4：实体集 保险单 + 联系集 承保(保险单 M : N 车辆)
--   · "每张保险单为一辆或多辆车保险" → 联系表 policy_vehicle；
--   · 保险单全参与(至少承保一辆车) → 步骤 6 的触发器；
--   · 车辆部分参与(可以没投保) → 车辆端不加约束。
--   联系上的属性: 这张保单为这辆车分摊的保费(可选)。
-- ============================================================
CREATE TABLE policy (
    policy_no        INTEGER       NOT NULL,           -- 保单号
    coverage_type    VARCHAR(20)   NOT NULL,           -- 险种
    coverage_amount  NUMERIC(12,2) NOT NULL,           -- 保额
    valid_from       DATE          NOT NULL,           -- 保险起期
    valid_to         DATE          NOT NULL,           -- 保险止期
    CONSTRAINT pk_policy PRIMARY KEY (policy_no),
    CONSTRAINT ck_policy_type CHECK (coverage_type IN
        ('交强险','商业险','车损险','第三者责任险','盗抢险','综合险')),
    CONSTRAINT ck_policy_amount CHECK (coverage_amount > 0),
    CONSTRAINT ck_policy_period CHECK (valid_from < valid_to)
);
COMMENT ON TABLE policy IS '实体集 保险单(承保合同)';

CREATE TABLE policy_vehicle (
    policy_no      INTEGER       NOT NULL,             -- 保险单(外键)
    license_no     VARCHAR(16)   NOT NULL,             -- 车辆(外键)
    premium_share  NUMERIC(12,2)     NULL,             -- 联系集属性: 本车分摊的保费
    CONSTRAINT pk_policy_vehicle PRIMARY KEY (policy_no, license_no),
    CONSTRAINT fk_pv_policy  FOREIGN KEY (policy_no)
        REFERENCES policy (policy_no)   ON DELETE RESTRICT,
    CONSTRAINT fk_pv_vehicle FOREIGN KEY (license_no)
        REFERENCES vehicle (license_no) ON DELETE RESTRICT,
    CONSTRAINT ck_pv_share CHECK (premium_share IS NULL OR premium_share >= 0)
);
COMMENT ON TABLE policy_vehicle IS '联系集 承保(M:N)的实现表';

SELECT '步骤 4 完成：policy + policy_vehicle(承保 M:N)' AS section;


-- ============================================================
-- 步骤 5：弱实体 支付 + 标识联系 缴费(保险单 1 : N 支付)
--   · 支付靠"保单号 + 支付序号"才能唯一标识 → 弱实体；
--   · 主键 = (policy_no, payment_seq), policy_no 同时是外键；
--   · 复合属性"缴费时间段"展平为 period_from / period_to；
--   · 到期日 due_date 与缴费日 paid_date 是支付自己的属性,
--     其中 paid_date 为 NULL 表示尚未缴纳。
-- ============================================================
CREATE TABLE payment (
    policy_no    INTEGER       NOT NULL,               -- 保单号(外键 + 主键的一部分)
    payment_seq  SMALLINT      NOT NULL,               -- 支付序号(部分键, 从 1 开始)
    period_from  DATE          NOT NULL,               -- 缴费时间段: 起始日
    period_to    DATE          NOT NULL,               -- 缴费时间段: 截止日
    due_date     DATE          NOT NULL,               -- 到期日
    paid_date    DATE              NULL,               -- 缴费日(NULL = 未缴)
    amount       NUMERIC(12,2) NOT NULL,               -- 金额
    CONSTRAINT pk_payment PRIMARY KEY (policy_no, payment_seq),
    CONSTRAINT fk_payment_policy FOREIGN KEY (policy_no)
        REFERENCES policy (policy_no) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_payment_seq    CHECK (payment_seq >= 1),
    CONSTRAINT ck_payment_period CHECK (period_from < period_to),
    CONSTRAINT ck_payment_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_paid   CHECK (paid_date IS NULL OR paid_date >= due_date - 180)
);
COMMENT ON TABLE  payment IS '弱实体 支付; 标识联系 = 缴费, 主键 = 保单号 + 支付序号';
COMMENT ON COLUMN payment.payment_seq IS '部分键: 同一张保单内从 1 开始编号';
COMMENT ON COLUMN payment.paid_date   IS '缴费日; NULL 表示尚未缴纳(到期日已过即逾期)';

SELECT '步骤 5 完成：payment(弱实体 + 缴费 1:N)' AS section;


-- ============================================================
-- 步骤 6：把"全参与(至少一条)"落成可延迟约束触发器
--   外键只能保证"N 端一定指向存在的 1 端"(车辆一定有车主),
--   但"每个客户至少一辆车 / 每张保单至少承保一辆车 / 每张保单至少
--   一笔缴费 / 每起事故至少涉及一辆车"这四条 SQL 的普通约束表达不了。
--   做法: 一个通用函数 + 每条约束两枚 CONSTRAINT TRIGGER(父表插一行时、
--         子表增删改时各一枚), 都声明为 DEFERRABLE INITIALLY DEFERRED,
--         于是检查推迟到事务提交时 —— 只要提交时关系齐备就通过。
--   注意: 这带来的实践约定是 —— 往这些表里插数据要放在一个事务里
--         (见步骤 7 的 BEGIN/COMMIT)。
-- ============================================================
CREATE OR REPLACE FUNCTION assert_min_one_child() RETURNS trigger AS $$
DECLARE
    parent_table text := TG_ARGV[0];   -- 父表名
    parent_key   text := TG_ARGV[1];   -- 父表主键列
    child_table  text := TG_ARGV[2];   -- 子表名
    child_fk     text := TG_ARGV[3];   -- 子表里指向父表的外键列
    keys         text[] := ARRAY[]::text[];
    k            text;
    n            integer;
BEGIN
    IF TG_TABLE_NAME = child_table THEN
        -- 触发器挂在子表上: 被删/被改走的子行, 其父键都要复查
        IF TG_OP IN ('DELETE', 'UPDATE') THEN
            keys := keys || (to_jsonb(OLD) ->> child_fk);
        END IF;
        IF TG_OP IN ('INSERT', 'UPDATE') THEN
            keys := keys || (to_jsonb(NEW) ->> child_fk);
        END IF;
    ELSE
        -- 触发器挂在父表上: 只复查这一行的主键
        keys := keys || (to_jsonb(NEW) ->> parent_key);
    END IF;

    FOREACH k IN ARRAY keys LOOP
        CONTINUE WHEN k IS NULL;
        -- 父键统一转 text 比较: 这样函数对 INTEGER / DATE 等类型的键都通用
        EXECUTE format('SELECT count(*) FROM %I WHERE %I::text = $1', parent_table, parent_key)
           INTO n USING k;
        CONTINUE WHEN n = 0;           -- 父行在同一事务里已被删除, 无需再保证
        EXECUTE format('SELECT count(*) FROM %I WHERE %I::text = $1', child_table, child_fk)
           INTO n USING k;
        IF n = 0 THEN
            RAISE EXCEPTION '全参与约束被破坏: 表 % 的键 % 取值 % 在子表 % 中没有任何对应行',
                parent_table, parent_key, k, child_table
                USING ERRCODE = 'check_violation';
        END IF;
    END LOOP;
    RETURN NULL;                       -- AFTER 触发器, 返回值被忽略
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION assert_min_one_child() IS
'通用"至少一条"检查: TG_ARGV = (父表, 父主键列, 子表, 子表外键列)';

-- 6.1 每个客户至少一辆车
CREATE CONSTRAINT TRIGGER trg_vehicle_owner_needs_car
    AFTER INSERT OR UPDATE OR DELETE ON vehicle
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_min_one_child('customer', 'cust_id', 'vehicle', 'cust_id');
CREATE CONSTRAINT TRIGGER trg_customer_needs_car
    AFTER INSERT ON customer
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_min_one_child('customer', 'cust_id', 'vehicle', 'cust_id');

-- 6.2 每起事故至少涉及一辆车
CREATE CONSTRAINT TRIGGER trg_accident_needs_vehicle
    AFTER INSERT ON accident
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_min_one_child('accident', 'accident_no', 'vehicle_accident', 'accident_no');
CREATE CONSTRAINT TRIGGER trg_va_keeps_accident
    AFTER INSERT OR UPDATE OR DELETE ON vehicle_accident
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_min_one_child('accident', 'accident_no', 'vehicle_accident', 'accident_no');

-- 6.3 每张保险单至少承保一辆车
CREATE CONSTRAINT TRIGGER trg_policy_needs_vehicle
    AFTER INSERT ON policy
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_min_one_child('policy', 'policy_no', 'policy_vehicle', 'policy_no');
CREATE CONSTRAINT TRIGGER trg_pv_keeps_policy
    AFTER INSERT OR UPDATE OR DELETE ON policy_vehicle
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_min_one_child('policy', 'policy_no', 'policy_vehicle', 'policy_no');

-- 6.4 每张保险单至少一笔缴费
CREATE CONSTRAINT TRIGGER trg_policy_needs_payment
    AFTER INSERT ON policy
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_min_one_child('policy', 'policy_no', 'payment', 'policy_no');
CREATE CONSTRAINT TRIGGER trg_payment_keeps_policy
    AFTER INSERT OR UPDATE OR DELETE ON payment
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_min_one_child('policy', 'policy_no', 'payment', 'policy_no');

SELECT '步骤 6 完成：8 枚可延迟约束触发器(4 条全参与约束)' AS section;


-- ============================================================
-- 步骤 7：示例数据
--   ⚠ 因为步骤 6 的检查推迟到提交时, 父子行必须放在同一个事务里插入。
-- ============================================================
BEGIN;

-- 7.1 客户 4 人, 每人至少一辆车
INSERT INTO customer (cust_id, cust_name, address, phone) VALUES
 (1001, '张伟', '北京市海淀区中关村大街1号',   '13800000001'),
 (1002, '李娜', '上海市浦东新区世纪大道100号', '13800000002'),
 (1003, '王强', '广州市天河区体育西路50号',   '13800000003'),
 (1004, '赵敏', '成都市武侯区人民南路4段20号', '13800000004');

-- 7.2 车辆 7 辆(拥有 1:N; 张伟 3 辆, 李娜 2 辆, 王强 1 辆, 赵敏 1 辆)
INSERT INTO vehicle (license_no, model, model_year, vin, cust_id) VALUES
 ('京A12345', '大众 帕萨特',      2018, 'LSVAM4185J2000001', 1001),
 ('京B67890', '丰田 卡罗拉',      2020, 'LFMBB1BB4L3000002', 1001),
 ('京G11223', '奥迪 A4L',         2021, 'LFV3A28K0M4000003', 1001),
 ('沪C24680', '特斯拉 Model 3',   2022, 'LRW3E7FA0NC000004', 1002),
 ('沪D13579', '本田 CR-V',        2019, 'LVHRW1830K5000005', 1002),
 ('粤E86420', '比亚迪 汉',        2023, 'LGXCE4CB0P6000006', 1003),
 ('川F97531', '日产 轩逸',        2017, 'LGBH52E01H7000007', 1004);

-- 7.3 事故 4 起 + 涉及车辆 5 条(涉及 M:N)
INSERT INTO accident (accident_no, occurred_on, location, description, damage_amount) VALUES
 (5001, DATE '2025-03-12', '北京市朝阳区建国路',   '追尾, 后保险杠受损',        8600.00),
 (5002, DATE '2025-07-08', '上海市内环高架',       '变道剐蹭, 右侧车门划伤',     3200.00),
 (5003, DATE '2026-01-20', '成都市三环路',         '雨天打滑撞护栏, 车头受损',  24500.00),
 (5004, DATE '2026-05-05', '北京市西三环停车场',   '倒车剐蹭, 左后翼子板掉漆',   1500.00);

INSERT INTO vehicle_accident (license_no, accident_no) VALUES
 ('京A12345', 5001),          -- 一起事故涉及两辆车
 ('沪C24680', 5001),
 ('沪D13579', 5002),
 ('川F97531', 5003),
 ('京A12345', 5004);          -- 京A12345 出过两次事故

-- 7.4 保险单 4 张 + 承保关系 8 条(承保 M:N; 粤E86420 未投保 → 车辆部分参与)
INSERT INTO policy (policy_no, coverage_type, coverage_amount, valid_from, valid_to) VALUES
 (20001, '综合险', 200000.00, DATE '2025-01-01', DATE '2025-12-31'),
 (20002, '交强险', 120000.00, DATE '2025-06-01', DATE '2026-05-31'),
 (20003, '商业险', 300000.00, DATE '2026-01-01', DATE '2026-12-31'),
 (20004, '综合险', 250000.00, DATE '2026-01-01', DATE '2026-12-31');

INSERT INTO policy_vehicle (policy_no, license_no, premium_share) VALUES
 (20001, '京A12345', 1600.00),      -- 一张保单承保 3 辆车
 (20001, '京B67890', 1300.00),
 (20001, '京G11223', 1900.00),
 (20002, '沪C24680', 950.00),       -- 一张保单承保 2 辆车
 (20002, '沪D13579', 850.00),
 (20003, '川F97531', 3200.00),
 (20004, '京A12345', 2100.00),      -- 京A12345 同时被两张保单承保
 (20004, '京B67890', 2100.00);

-- 7.5 支付 9 笔(缴费 1:N; 每张保单至少一笔; 其中 3 笔逾期未缴)
INSERT INTO payment (policy_no, payment_seq, period_from, period_to, due_date, paid_date, amount) VALUES
 (20001, 1, DATE '2025-01-01', DATE '2025-03-31', DATE '2024-12-25', DATE '2024-12-20', 2500.00),
 (20001, 2, DATE '2025-04-01', DATE '2025-06-30', DATE '2025-03-25', DATE '2025-03-24', 2500.00),
 (20001, 3, DATE '2025-07-01', DATE '2025-09-30', DATE '2025-06-25', DATE '2025-06-30', 2500.00),
 (20001, 4, DATE '2025-10-01', DATE '2025-12-31', DATE '2025-09-25', NULL,              2500.00),  -- 逾期未缴
 (20002, 1, DATE '2025-06-01', DATE '2025-11-30', DATE '2025-05-25', DATE '2025-05-20', 1800.00),
 (20002, 2, DATE '2025-12-01', DATE '2026-05-31', DATE '2025-11-25', NULL,              1800.00),  -- 逾期未缴
 (20003, 1, DATE '2026-01-01', DATE '2026-06-30', DATE '2025-12-25', DATE '2025-12-22', 3200.00),
 (20003, 2, DATE '2026-07-01', DATE '2026-12-31', DATE '2026-06-25', NULL,              3200.00),  -- 逾期未缴
 (20004, 1, DATE '2026-01-01', DATE '2026-12-31', DATE '2026-01-05', DATE '2026-01-03', 4200.00);

COMMIT;

SELECT '步骤 7 完成：示例数据已提交' AS section;


-- ============================================================
-- 步骤 8：视图 —— 把 E-R 图里的"派生信息"现算
-- ============================================================
-- 8.1 缴费状态(已缴 / 逾期未缴 / 未到期)
CREATE OR REPLACE VIEW v_payment_status AS
SELECT pay.policy_no,
       pol.coverage_type,
       pay.payment_seq,
       pay.period_from,
       pay.period_to,
       pay.due_date,
       pay.paid_date,
       pay.amount,
       CASE WHEN pay.paid_date IS NOT NULL                    THEN '已缴'
            WHEN CURRENT_DATE > pay.due_date                  THEN '逾期未缴'
            ELSE '未到期' END                                  AS pay_status,
       CASE WHEN pay.paid_date IS NULL AND CURRENT_DATE > pay.due_date
            THEN CURRENT_DATE - pay.due_date ELSE 0 END        AS overdue_days
FROM payment pay
JOIN policy  pol ON pol.policy_no = pay.policy_no;
COMMENT ON VIEW v_payment_status IS '缴费状态视图: 缴费日为空且已过到期日 → 逾期未缴';

-- 8.2 保单全景(承保车辆数 / 缴费笔数 / 已缴金额 / 未缴金额)
CREATE OR REPLACE VIEW v_policy_detail AS
SELECT pol.policy_no,
       pol.coverage_type,
       pol.coverage_amount,
       pol.valid_from,
       pol.valid_to,
       count(DISTINCT pv.license_no)                                    AS car_count,
       count(DISTINCT pay.payment_seq)                                  AS pay_count,
       COALESCE(sum(pay.amount) FILTER (WHERE pay.paid_date IS NOT NULL), 0) AS paid_total,
       COALESCE(sum(pay.amount) FILTER (WHERE pay.paid_date IS NULL), 0)     AS unpaid_total
FROM policy pol
LEFT JOIN policy_vehicle pv ON pv.policy_no = pol.policy_no
LEFT JOIN payment        pay ON pay.policy_no = pol.policy_no
GROUP BY pol.policy_no, pol.coverage_type, pol.coverage_amount, pol.valid_from, pol.valid_to;
COMMENT ON VIEW v_policy_detail IS '保单全景视图(车辆数、缴费笔数、已缴/未缴金额)';

-- 8.3 事故涉及车辆(M:N 展开)
CREATE OR REPLACE VIEW v_vehicle_accident_detail AS
SELECT a.accident_no,
       a.occurred_on,
       a.location,
       a.damage_amount,
       v.license_no,
       v.model,
       c.cust_id,
       c.cust_name
FROM accident a
JOIN vehicle_accident va ON va.accident_no = a.accident_no
JOIN vehicle  v ON v.license_no = va.license_no
JOIN customer c ON c.cust_id    = v.cust_id;
COMMENT ON VIEW v_vehicle_accident_detail IS '事故-车辆-车主 明细(M:N 联系展开)';

SELECT '步骤 8 完成：3 个视图' AS section;


-- ============================================================
-- 步骤 9：验证查询
-- ============================================================
-- 9.1 各表行数
SELECT '9.1 行数统计' AS section;
SELECT 'customer'          AS 表, count(*) AS 行数 FROM customer
UNION ALL SELECT 'vehicle',          count(*) FROM vehicle
UNION ALL SELECT 'accident',         count(*) FROM accident
UNION ALL SELECT 'vehicle_accident', count(*) FROM vehicle_accident
UNION ALL SELECT 'policy',           count(*) FROM policy
UNION ALL SELECT 'policy_vehicle',   count(*) FROM policy_vehicle
UNION ALL SELECT 'payment',          count(*) FROM payment
ORDER BY 1;

-- 9.2 客户 → 车辆数 → 承保保单数(拥有 1:N + 承保 M:N)
SELECT '9.2 客户-车辆-保单' AS section;
SELECT c.cust_id, c.cust_name,
       count(DISTINCT v.license_no)  AS 车辆数,
       count(DISTINCT pv.policy_no)  AS 承保保单数
FROM customer c
JOIN vehicle        v  ON v.cust_id    = c.cust_id
LEFT JOIN policy_vehicle pv ON pv.license_no = v.license_no
GROUP BY c.cust_id, c.cust_name
ORDER BY c.cust_id;

-- 9.3 事故涉及车辆(涉及 M:N; 每起事故至少一辆车)
SELECT '9.3 事故涉及车辆' AS section;
SELECT a.accident_no, a.occurred_on, a.location, a.damage_amount,
       string_agg(va.license_no, ', ' ORDER BY va.license_no) AS 涉及车辆,
       count(*) AS 车辆数
FROM accident a
JOIN vehicle_accident va ON va.accident_no = a.accident_no
GROUP BY a.accident_no, a.occurred_on, a.location, a.damage_amount
ORDER BY a.accident_no;

-- 9.4 保单全景(含"至少一辆车、至少一笔缴费"的体现)
SELECT '9.4 保单全景' AS section;
SELECT policy_no, coverage_type, car_count AS 承保车辆数,
       pay_count AS 缴费笔数, paid_total AS 已缴金额, unpaid_total AS 未缴金额
FROM v_policy_detail
ORDER BY policy_no;

-- 9.5 缴费状态(逾期未缴的排在前面)
SELECT '9.5 缴费状态' AS section;
SELECT policy_no, payment_seq, period_from, period_to, due_date, paid_date, amount, pay_status, overdue_days
FROM v_payment_status
ORDER BY pay_status DESC, due_date, policy_no, payment_seq;

-- 9.6 全参与自检: 四条查询都应返回 0 行
SELECT '9.6 全参与自检(以下四行都应为 0)' AS section;
SELECT '客户没有车辆'       AS 检查项, count(*) AS 违规数 FROM customer c
  WHERE NOT EXISTS (SELECT 1 FROM vehicle        v  WHERE v.cust_id     = c.cust_id)
UNION ALL
SELECT '事故没有涉及车辆', count(*) FROM accident a
  WHERE NOT EXISTS (SELECT 1 FROM vehicle_accident va WHERE va.accident_no = a.accident_no)
UNION ALL
SELECT '保单没有承保车辆', count(*) FROM policy p
  WHERE NOT EXISTS (SELECT 1 FROM policy_vehicle pv WHERE pv.policy_no   = p.policy_no)
UNION ALL
SELECT '保单没有缴费记录', count(*) FROM policy p
  WHERE NOT EXISTS (SELECT 1 FROM payment        pay WHERE pay.policy_no = p.policy_no);

-- 9.7 车辆部分参与的证据: 未投保车辆 + 未出险车辆
SELECT '9.7 车辆的部分参与' AS section;
SELECT v.license_no, v.model, c.cust_name,
       CASE WHEN EXISTS (SELECT 1 FROM policy_vehicle pv WHERE pv.license_no = v.license_no)
            THEN '已投保' ELSE '未投保' END AS 投保情况,
       (SELECT count(*) FROM vehicle_accident va WHERE va.license_no = v.license_no) AS 事故次数
FROM vehicle v JOIN customer c ON c.cust_id = v.cust_id
ORDER BY v.license_no;

-- 9.8 逾期统计
SELECT '9.8 逾期统计' AS section;
SELECT pay_status, count(*) AS 笔数, sum(amount) AS 金额
FROM v_payment_status GROUP BY pay_status ORDER BY pay_status;


-- ============================================================
-- 步骤 10：反例测试 —— 预期报错(共 10 处)
--   下面开始故意制造错误, 所以把"遇错即止"关掉:
--   psql 会逐条报错并继续, 脚本最后统计仍应是 10 条报错。
-- ============================================================
\set ON_ERROR_STOP off

SELECT '10.1 预期报错：插入没有车辆的客户 9001(提交时被触发器拦下)' AS section;
INSERT INTO customer (cust_id, cust_name, address, phone)
VALUES (9001, '无车客户', '某地', '13000000000');

SELECT '10.2 预期报错：保单 30001 承保了车、但一笔缴费都没有' AS section;
BEGIN;
INSERT INTO policy (policy_no, coverage_type, coverage_amount, valid_from, valid_to)
VALUES (30001, '综合险', 100000.00, DATE '2026-01-01', DATE '2026-12-31');
INSERT INTO policy_vehicle (policy_no, license_no) VALUES (30001, '粤E86420');
COMMIT;

SELECT '10.3 预期报错：保单 30002 有缴费、但没承保任何车' AS section;
BEGIN;
INSERT INTO policy (policy_no, coverage_type, coverage_amount, valid_from, valid_to)
VALUES (30002, '交强险', 120000.00, DATE '2026-01-01', DATE '2026-12-31');
INSERT INTO payment (policy_no, payment_seq, period_from, period_to, due_date, amount)
VALUES (30002, 1, DATE '2026-01-01', DATE '2026-06-30', DATE '2025-12-25', 1000.00);
COMMIT;

SELECT '10.4 预期报错：插入不涉及任何车辆的事故 6001(提交时被拦)' AS section;
BEGIN;
INSERT INTO accident (accident_no, occurred_on, location, damage_amount)
VALUES (6001, DATE '2026-06-01', '某地', 100.00);
COMMIT;

SELECT '10.5 预期报错：重复的"车辆-事故"组合(主键冲突)' AS section;
INSERT INTO vehicle_accident (license_no, accident_no) VALUES ('京A12345', 5001);

SELECT '10.6 预期报错：缴费时间段起止写反(period_from >= period_to)' AS section;
INSERT INTO payment (policy_no, payment_seq, period_from, period_to, due_date, amount)
VALUES (20003, 3, DATE '2026-12-31', DATE '2026-07-01', DATE '2026-06-25', 100.00);

SELECT '10.7 预期报错：金额必须为正' AS section;
INSERT INTO payment (policy_no, payment_seq, period_from, period_to, due_date, amount)
VALUES (20003, 4, DATE '2026-07-01', DATE '2026-12-31', DATE '2026-06-25', -1.00);

SELECT '10.8 预期报错：支付引用不存在的保单 99999(外键)' AS section;
INSERT INTO payment (policy_no, payment_seq, period_from, period_to, due_date, amount)
VALUES (99999, 1, DATE '2026-07-01', DATE '2026-12-31', DATE '2026-06-25', 100.00);

SELECT '10.9 预期报错：删除还有车辆的客户 1003(外键 RESTRICT)' AS section;
DELETE FROM customer WHERE cust_id = 1003;

SELECT '10.10 预期报错：把保单 20002 的承保车辆删光, 提交时报错' AS section;
BEGIN;
DELETE FROM policy_vehicle WHERE policy_no = 20002;
COMMIT;

SELECT '步骤 10 完成：以上 10 处报错都是预期行为' AS section;


-- ============================================================
-- 步骤 11：收尾
-- ============================================================
SELECT '脚本执行完毕：7 张表 + 3 个视图 + 8 枚触发器 + 4 条全参与约束已就绪' AS section;

-- 想清空请手工执行(顺序不能乱):
--   DROP VIEW  v_payment_status, v_policy_detail, v_vehicle_accident_detail;
--   DROP TABLE payment, policy_vehicle, vehicle_accident, policy,
--              accident, vehicle, customer CASCADE;
--   DROP FUNCTION assert_min_one_child() CASCADE;
-- 或者整库删掉: .\psql.bat "DROP DATABASE insurance_demo"


-- ============================================================
-- 实测结论(PostgreSQL 17.11, 库 insurance_demo, 实跑 0 警告 / 10 条预期报错)：
--   · 建 7 张表 + 3 个视图 + 8 枚可延迟约束触发器; 同库连跑两遍结果一致(可重复执行);
--   · 行数: 客户 4 / 车辆 7 / 事故 4 / 车辆-事故 5 / 保单 4 / 承保 8 / 支付 9;
--   · 拥有 1:N: 张伟 3 辆、李娜 2 辆、王强 1 辆、赵敏 1 辆;
--   · 涉及 M:N: 事故 5001 涉及 2 辆车(沪C24680, 京A12345), 5002/5003/5004 各 1 辆;
--     京A12345 出险 2 次, 京B67890 与 京G11223 出险 0 次(车辆部分参与);
--   · 承保 M:N: 20001 承保 3 辆 / 20002 承保 2 辆 / 20003 承保 1 辆 / 20004 承保 2 辆;
--     京A12345 同时被 20001 与 20004 承保; 粤E86420 未投保(车辆部分参与);
--   · 缴费 1:N(弱实体): 支付 9 笔, 主键 (policy_no, payment_seq);
--     视图判定 已缴 6 笔 / 逾期未缴 3 笔(逾期 379 天、318 天、106 天, 以 2026-10-09 为当天);
--   · 全参与自检 4 项(客户无车 / 事故无车 / 保单无承保车 / 保单无缴费)全部 0 违规;
--   · 步骤 10 的 10 处报错一处不多一处不少地被拦下:
--       5 处"至少一条"可延迟触发器(客户无车、保单无缴费、保单无承保车、事故无车、
--       删光承保关系后提交), 1 处主键冲突, 2 处 CHECK(时间段倒置 / 金额非正),
--       1 处外键(支付引用不存在的保单), 1 处外键 RESTRICT(删除还有车辆的客户);
--   · 过程中踩到并在脚本里修掉的两个坑(留作记录):
--       ① RAISE EXCEPTION 格式串里 % 的个数必须与参数个数严格相等 ——
--          '%(%).% ...' 会被算成 5 个占位符, 报"为RAISE子句指定参数过少";
--       ② to_jsonb(NEW) ->> '整数列' 取出来是 text, 与整数列比较要写
--          %I::text = $1, 否则报"操作符不存在: integer = text"。
--   · 提醒: 建表/建约束阶段开了 \set ON_ERROR_STOP on, 反例阶段才关掉 ——
--     所以"函数没建成 → 8 枚触发器静默缺失"这类问题不会再被后面的语句盖过去。
-- ============================================================
