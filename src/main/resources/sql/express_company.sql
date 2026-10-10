-- ============================================================
-- 实践习题 7.22 全球快递公司数据库 —— E-R 图落成 PostgreSQL 数据库 + 一组约束
-- 数据库: express_demo (PostgreSQL 17.11)
-- 教材: 《数据库系统概念》第 7 章 数据库设计和 E-R 模型 · 实践习题 7.22
-- 配套图: db-homework/express-er.html (E-R 图 + 关系模式 + 约束清单)
-- ------------------------------------------------------------
-- 业务: 全球快递公司要跟踪"谁寄的、谁收的"以及每个包裹的位置历史。
--   · 寄件客户与收件客户共用一套 customer, 有些客户两者都是;
--   · 每个包裹由运单号唯一标识, 可追踪 —— 位置历史是一条条事件(弱实体),
--     每小时刻记下"在哪个位置(卡车/飞机/机场/仓库)";
--   · 位置用概化: 超类 location + 四个子类 truck / plane / airport / warehouse,
--     本题是"全概化 total + 不相交 disjoint"(每个位置恰属于其中一类);
--   · "当前位置"不存字段, 用视图取历史里 event_time 最大的那条(派生数据)。
--
-- 表名不加前缀: 本脚本建在独立库 express_demo 里。
--
-- 运行:
--   .\psql.bat "DROP DATABASE IF EXISTS express_demo WITH (FORCE)"
--   .\psql.bat "CREATE DATABASE express_demo"
--   .\psql.bat -d express_demo -f src\main\resources\sql\express_company.sql
--
-- 实测结论(PostgreSQL 17.11): 见文件末尾。
-- ============================================================

SET client_encoding = 'UTF8';

-- 建表/建约束阶段"遇错即止": 任何 DDL 失败立刻停下,
-- 免得函数没建成、触发器静默缺失这类问题被后面的语句盖过去。
\set ON_ERROR_STOP on


-- ============================================================
-- 步骤 0：清理旧对象
-- ============================================================
DROP VIEW IF EXISTS v_customer_both_roles;
DROP VIEW IF EXISTS v_package_current;
DROP VIEW IF EXISTS v_package_track;
DROP VIEW IF EXISTS v_package_full;
DROP VIEW IF EXISTS v_location_kind;
DROP TABLE IF EXISTS package_event, truck, plane, airport, warehouse, location CASCADE;
DROP TABLE IF EXISTS package, service, customer CASCADE;
DROP FUNCTION IF EXISTS assert_location_isa() CASCADE;
DROP FUNCTION IF EXISTS assert_event_time_valid() CASCADE;

SELECT '步骤 0 完成：旧对象已清理' AS section;


-- ============================================================
-- 步骤 1：客户 / 服务类型
--   只有一个 customer 实体集: 同一客户既可以寄件(sender)也可以收件(receiver),
--   题目"有些客户可能两者都是"因此天然成立, 不需要建两张客户表。
-- ============================================================
CREATE TABLE customer (
    customer_id INTEGER     NOT NULL,                 -- 主码: 客户号
    name        VARCHAR(40) NOT NULL,
    phone       VARCHAR(20)     NULL,
    email       VARCHAR(60)     NULL,
    address     VARCHAR(160)    NULL,
    CONSTRAINT pk_customer PRIMARY KEY (customer_id),
    CONSTRAINT uq_customer_email UNIQUE (email)
);

CREATE TABLE service (
    service_code VARCHAR(6)  NOT NULL,                -- 主码: 服务代码
    service_name VARCHAR(30) NOT NULL,
    sla_days     SMALLINT    NOT NULL,                -- 承诺时限(天)
    CONSTRAINT pk_service PRIMARY KEY (service_code),
    CONSTRAINT uq_service_name UNIQUE (service_name),
    CONSTRAINT ck_service_sla CHECK (sla_days > 0)
);

SELECT '步骤 1 完成：customer / service' AS section;


-- ============================================================
-- 步骤 2：包裹（运单号是主码; 两个外码分别指向寄件人与收件人）
--   sends    : customer 1:N package  → sender_id
--   receives : customer 1:N package  → receiver_id
--   uses_service: service 1:N package → service_code
-- ============================================================
CREATE TABLE package (
    tracking_no   VARCHAR(20)   NOT NULL,             -- 主码: 运单号
    sender_id     INTEGER       NOT NULL,             -- 寄件客户(外码)
    receiver_id   INTEGER       NOT NULL,             -- 收件客户(外码)
    service_code  VARCHAR(6)    NOT NULL,             -- 服务类型(外码)
    weight_kg     NUMERIC(8,3)  NOT NULL,
    length_cm     NUMERIC(6,1)  NOT NULL,
    width_cm      NUMERIC(6,1)  NOT NULL,
    height_cm     NUMERIC(6,1)  NOT NULL,
    declared_value NUMERIC(12,2) NOT NULL DEFAULT 0,
    created_at    TIMESTAMP     NOT NULL,             -- 收寄时间
    status        VARCHAR(10)   NOT NULL,             -- 包裹当前状态
    CONSTRAINT pk_package PRIMARY KEY (tracking_no),
    CONSTRAINT fk_package_sender FOREIGN KEY (sender_id)
        REFERENCES customer (customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_package_receiver FOREIGN KEY (receiver_id)
        REFERENCES customer (customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_package_service FOREIGN KEY (service_code)
        REFERENCES service (service_code) ON DELETE RESTRICT,
    CONSTRAINT ck_package_weight CHECK (weight_kg > 0),
    CONSTRAINT ck_package_size CHECK (length_cm > 0 AND width_cm > 0 AND height_cm > 0),
    CONSTRAINT ck_package_value CHECK (declared_value >= 0),
    CONSTRAINT ck_package_status CHECK (status IN ('已收寄','运输中','派送中','已签收','异常'))
);

SELECT '步骤 2 完成：package（寄件人/收件人两个外码 + 一组 CHECK）' AS section;


-- ============================================================
-- 步骤 3：位置概化 —— 超类 location + 四个子类
--   卡车/飞机/机场/仓库属性差别很大(车牌/机尾号/三字码/仓号), 但都要能被事件引用,
--   所以抽超类。子类主码同时是外码 → 指向超类。
--   注: 本题 ISA 是"全概化 + 不相交", 与上一题(网上书店, 重叠概化)相反 ——
--       重叠只能用超类表+子类表; 不相交还可以用"单表+判别列+CHECK"。
--       这里仍按最规范的"超类表 + 子类表"落地, 约束由步骤 4 的触发器保证。
-- ============================================================
CREATE TABLE location (
    location_id   INTEGER     NOT NULL,               -- 主码: 位置号(超类主码)
    location_name VARCHAR(60) NOT NULL,               -- 位置显示名
    CONSTRAINT pk_location PRIMARY KEY (location_id)
);

CREATE TABLE truck (
    location_id INTEGER     NOT NULL,                 -- 主码兼外码
    plate_no    VARCHAR(12) NOT NULL,                 -- 车牌(候选码)
    capacity_kg NUMERIC(8,1)    NULL,
    CONSTRAINT pk_truck PRIMARY KEY (location_id),
    CONSTRAINT uq_truck_plate UNIQUE (plate_no),
    CONSTRAINT fk_truck_location FOREIGN KEY (location_id)
        REFERENCES location (location_id) ON DELETE CASCADE,
    CONSTRAINT ck_truck_capacity CHECK (capacity_kg IS NULL OR capacity_kg > 0)
);

CREATE TABLE plane (
    location_id INTEGER     NOT NULL,
    tail_no     VARCHAR(12) NOT NULL,                 -- 机尾号(候选码)
    model       VARCHAR(30) NOT NULL,
    capacity_kg NUMERIC(9,1)    NULL,
    CONSTRAINT pk_plane PRIMARY KEY (location_id),
    CONSTRAINT uq_plane_tail UNIQUE (tail_no),
    CONSTRAINT fk_plane_location FOREIGN KEY (location_id)
        REFERENCES location (location_id) ON DELETE CASCADE,
    CONSTRAINT ck_plane_capacity CHECK (capacity_kg IS NULL OR capacity_kg > 0)
);

CREATE TABLE airport (
    location_id INTEGER     NOT NULL,
    iata_code   VARCHAR(3)  NOT NULL,                 -- 三字码(候选码), 如 PEK
    city        VARCHAR(40) NOT NULL,
    CONSTRAINT pk_airport PRIMARY KEY (location_id),
    CONSTRAINT uq_airport_iata UNIQUE (iata_code),
    CONSTRAINT fk_airport_location FOREIGN KEY (location_id)
        REFERENCES location (location_id) ON DELETE CASCADE,
    CONSTRAINT ck_airport_iata CHECK (iata_code ~ '^[A-Z]{3}$')
);

CREATE TABLE warehouse (
    location_id INTEGER     NOT NULL,
    wh_code     VARCHAR(10) NOT NULL,                 -- 仓库编号(候选码)
    city        VARCHAR(40) NOT NULL,
    capacity_m3 NUMERIC(10,1)   NULL,
    CONSTRAINT pk_warehouse PRIMARY KEY (location_id),
    CONSTRAINT uq_warehouse_code UNIQUE (wh_code),
    CONSTRAINT fk_warehouse_location FOREIGN KEY (location_id)
        REFERENCES location (location_id) ON DELETE CASCADE,
    CONSTRAINT ck_warehouse_capacity CHECK (capacity_m3 IS NULL OR capacity_m3 > 0)
);

COMMENT ON TABLE location IS '概化超类: 位置(卡车/飞机/机场/仓库四选一, 全概化+不相交)';

SELECT '步骤 3 完成：location + truck / plane / airport / warehouse' AS section;


-- ============================================================
-- 步骤 4：位置历史（弱实体）+ 两条跨表约束触发器
--   package_event 的主码 = (tracking_no, event_seq):
--   tracking_no 来自标识联系 tracks(包裹 1:N 事件), event_seq 是部分键。
--   每条事件还带 location_id(在哪个位置)、event_time、event_type。
-- ============================================================
CREATE TABLE package_event (
    tracking_no VARCHAR(20) NOT NULL,                 -- 属于哪个包裹(外码 + 主码一部分)
    event_seq   SMALLINT    NOT NULL,                 -- 部分键: 同一包裹内从 1 递增
    event_time  TIMESTAMP   NOT NULL,
    location_id INTEGER     NOT NULL,                 -- 事件发生在哪个位置(外码)
    event_type  VARCHAR(10) NOT NULL,
    note        VARCHAR(120)    NULL,
    CONSTRAINT pk_package_event PRIMARY KEY (tracking_no, event_seq),
    CONSTRAINT fk_event_package  FOREIGN KEY (tracking_no)
        REFERENCES package (tracking_no) ON DELETE RESTRICT,
    CONSTRAINT fk_event_location FOREIGN KEY (location_id)
        REFERENCES location (location_id) ON DELETE RESTRICT,
    CONSTRAINT ck_event_seq CHECK (event_seq >= 1),
    CONSTRAINT ck_event_type CHECK (event_type IN
        ('收寄','入库','出库','装机','卸机','装车','派送','签收','异常'))
);
COMMENT ON TABLE package_event IS '弱实体: 位置历史; 主码 = 运单号 + 事件序号';

-- 4.1 位置概化: 每个 location 恰好一张子类行(0 张 = 违反全概化, ≥2 张 = 违反不相交)
CREATE OR REPLACE FUNCTION assert_location_isa() RETURNS trigger AS $$
DECLARE
    keys integer[] := ARRAY[]::integer[];
    k    integer;
    n    integer;
    nloc integer;
BEGIN
    IF TG_TABLE_NAME = 'location' THEN
        keys := keys || (to_jsonb(NEW) ->> 'location_id')::integer;
    ELSE
        -- 四个子类表上的增删改都要复查: 插入第二类会破坏不相交, 删除会破坏全概化
        IF TG_OP IN ('DELETE', 'UPDATE') THEN
            keys := keys || (to_jsonb(OLD) ->> 'location_id')::integer;
        END IF;
        IF TG_OP IN ('INSERT', 'UPDATE') THEN
            keys := keys || (to_jsonb(NEW) ->> 'location_id')::integer;
        END IF;
    END IF;

    FOREACH k IN ARRAY keys LOOP
        CONTINUE WHEN k IS NULL;
        SELECT count(*) INTO nloc FROM location WHERE location_id = k;
        CONTINUE WHEN nloc = 0;                         -- 超类行也删掉了, 无需再保证
        SELECT (SELECT count(*) FROM truck     WHERE location_id = k)
             + (SELECT count(*) FROM plane     WHERE location_id = k)
             + (SELECT count(*) FROM airport   WHERE location_id = k)
             + (SELECT count(*) FROM warehouse WHERE location_id = k)
          INTO n;
        IF n = 0 THEN
            RAISE EXCEPTION '全概化约束被破坏: 位置 % 不属于任何一类(卡车/飞机/机场/仓库)', k
                USING ERRCODE = 'check_violation';
        ELSIF n > 1 THEN
            RAISE EXCEPTION '不相交约束被破坏: 位置 % 同时属于 % 个子类(应为恰好 1 个)', k, n
                USING ERRCODE = 'check_violation';
        END IF;
    END LOOP;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_location_needs_subclass
    AFTER INSERT ON location
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_location_isa();
CREATE CONSTRAINT TRIGGER trg_truck_keeps_isa
    AFTER INSERT OR DELETE OR UPDATE ON truck
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_location_isa();
CREATE CONSTRAINT TRIGGER trg_plane_keeps_isa
    AFTER INSERT OR DELETE OR UPDATE ON plane
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_location_isa();
CREATE CONSTRAINT TRIGGER trg_airport_keeps_isa
    AFTER INSERT OR DELETE OR UPDATE ON airport
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_location_isa();
CREATE CONSTRAINT TRIGGER trg_warehouse_keeps_isa
    AFTER INSERT OR DELETE OR UPDATE ON warehouse
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_location_isa();

-- 4.2 历史事件的两条业务规则(跨表, CHECK 写不出来):
--     ① 事件时间不能早于包裹收寄时间;
--     ② 同一包裹内事件时间要随序号递增, 防止把轨迹写成乱序。
CREATE OR REPLACE FUNCTION assert_event_time_valid() RETURNS trigger AS $$
DECLARE
    v_created timestamp;
    v_prev    timestamp;
BEGIN
    SELECT created_at INTO v_created FROM package WHERE tracking_no = NEW.tracking_no;
    IF v_created IS NOT NULL AND NEW.event_time < v_created THEN
        RAISE EXCEPTION '事件时间早于收寄时间: 包裹 % 收寄于 %, 事件却写在 %',
            NEW.tracking_no, v_created, NEW.event_time USING ERRCODE = 'check_violation';
    END IF;
    SELECT max(event_time) INTO v_prev
    FROM package_event
    WHERE tracking_no = NEW.tracking_no AND event_seq < NEW.event_seq;
    IF v_prev IS NOT NULL AND NEW.event_time < v_prev THEN
        RAISE EXCEPTION '轨迹时间倒序: 包裹 % 序号 % 的时间 % 早于前一事件 %',
            NEW.tracking_no, NEW.event_seq, NEW.event_time, v_prev USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_event_time_valid
    BEFORE INSERT OR UPDATE ON package_event
    FOR EACH ROW EXECUTE FUNCTION assert_event_time_valid();

SELECT '步骤 4 完成：package_event(弱实体) + 位置 ISA 触发器 5 枚 + 事件时间触发器 1 枚' AS section;


-- ============================================================
-- 步骤 5：示例数据
--   ⚠ 位置 ISA 检查推迟到提交时, 所以超类行与子类行要放在同一个事务里。
-- ============================================================
BEGIN;

INSERT INTO customer (customer_id, name, phone, email, address) VALUES
 (1, '张伟', '13800000001', 'zhangwei@example.com', '北京市海淀区中关村大街1号'),
 (2, '李娜', '13800000002', 'lina@example.com',     '上海市浦东新区世纪大道100号'),
 (3, '王强', '13800000003', 'wangqiang@example.com','广州市天河区体育西路50号'),
 (4, '赵敏', '13800000004', 'zhaomin@example.com',  '成都市武侯区人民南路4段20号'),
 (5, '陈明', '13800000005', 'chenming@example.com', '杭州市西湖区文三路100号');

INSERT INTO service (service_code, service_name, sla_days) VALUES
 ('STD', '标准快递', 3),
 ('EXP', '加急快递', 2),
 ('OND', '次日达',   1);

-- 位置: 超类 8 行 + 四类子类各 2 行
INSERT INTO location (location_id, location_name) VALUES
 (101, '京A·12345'), (102, '沪B·67890'),                       -- 卡车
 (201, 'B-2025'),    (202, 'B-2030'),                          -- 飞机
 (301, '北京首都机场'), (302, '上海浦东机场'),                 -- 机场
 (401, '北京仓'),    (402, '上海仓');                          -- 仓库

INSERT INTO truck (location_id, plate_no, capacity_kg) VALUES
 (101, '京A12345', 8000.0), (102, '沪B67890', 12000.0);
INSERT INTO plane (location_id, tail_no, model, capacity_kg) VALUES
 (201, 'B-2025', 'Boeing 777F', 102000.0), (202, 'B-2030', 'Airbus A330F', 65000.0);
INSERT INTO airport (location_id, iata_code, city) VALUES
 (301, 'PEK', '北京'), (302, 'PVG', '上海');
INSERT INTO warehouse (location_id, wh_code, city, capacity_m3) VALUES
 (401, 'BJWH01', '北京', 20000.0), (402, 'SHWH01', '上海', 15000.0);

INSERT INTO package (tracking_no, sender_id, receiver_id, service_code,
                     weight_kg, length_cm, width_cm, height_cm, declared_value, created_at, status) VALUES
 ('PKG2024001', 1, 2, 'STD',  2.500, 30.0, 20.0, 15.0,  800.00, TIMESTAMP '2024-09-01 09:00:00', '已签收'),
 ('PKG2024002', 2, 1, 'EXP',  1.200, 25.0, 18.0, 10.0, 1200.00, TIMESTAMP '2024-09-03 14:30:00', '已签收'),
 ('PKG2024003', 4, 4, 'STD',  0.800, 20.0, 15.0,  8.0,  200.00, TIMESTAMP '2024-09-05 10:15:00', '运输中'),
 ('PKG2024004', 3, 5, 'OND',  5.000, 40.0, 30.0, 25.0, 3000.00, TIMESTAMP '2024-09-08 08:00:00', '派送中'),
 ('PKG2024005', 5, 3, 'STD',  0.300, 18.0, 12.0,  6.0,  100.00, TIMESTAMP '2024-09-10 19:40:00', '已收寄'),
 ('PKG2024006', 1, 3, 'EXP', 12.000, 60.0, 40.0, 35.0, 9000.00, TIMESTAMP '2024-09-02 11:20:00', '已签收');

-- 位置历史: 6 个包裹共 21 条事件(时间必须不早于收寄时间、且随序号递增)
INSERT INTO package_event (tracking_no, event_seq, event_time, location_id, event_type, note) VALUES
 ('PKG2024001', 1, TIMESTAMP '2024-09-01 09:10:00', 401, '收寄', '北京仓揽收'),
 ('PKG2024001', 2, TIMESTAMP '2024-09-01 18:00:00', 101, '装车', '装车发往机场'),
 ('PKG2024001', 3, TIMESTAMP '2024-09-01 22:30:00', 301, '入库', '到达北京首都机场'),
 ('PKG2024001', 4, TIMESTAMP '2024-09-02 02:00:00', 201, '装机', '装机 B-2025 飞上海'),
 ('PKG2024001', 5, TIMESTAMP '2024-09-02 04:30:00', 302, '卸机', '抵达上海浦东机场'),
 ('PKG2024001', 6, TIMESTAMP '2024-09-02 09:00:00', 102, '装车', '装车派送'),
 ('PKG2024001', 7, TIMESTAMP '2024-09-02 15:20:00', 402, '签收', '李娜签收'),
 ('PKG2024002', 1, TIMESTAMP '2024-09-03 14:40:00', 402, '收寄', '上海仓揽收'),
 ('PKG2024002', 2, TIMESTAMP '2024-09-03 20:10:00', 302, '出库', '发往北京'),
 ('PKG2024002', 3, TIMESTAMP '2024-09-04 03:00:00', 202, '装机', '装机 B-2030'),
 ('PKG2024002', 4, TIMESTAMP '2024-09-04 06:40:00', 301, '卸机', '抵达北京首都机场'),
 ('PKG2024002', 5, TIMESTAMP '2024-09-04 13:00:00', 401, '签收', '张伟签收'),
 ('PKG2024003', 1, TIMESTAMP '2024-09-05 10:20:00', 401, '收寄', '北京仓揽收'),
 ('PKG2024003', 2, TIMESTAMP '2024-09-05 21:00:00', 101, '装车', '干线运输'),
 ('PKG2024003', 3, TIMESTAMP '2024-09-06 08:30:00', 402, '入库', '到达上海仓'),
 ('PKG2024004', 1, TIMESTAMP '2024-09-08 08:10:00', 402, '收寄', '上海仓揽收'),
 ('PKG2024004', 2, TIMESTAMP '2024-09-08 19:00:00', 302, '出库', '连夜发运'),
 ('PKG2024004', 3, TIMESTAMP '2024-09-09 05:30:00', 202, '装机', '装机 B-2030 飞广州'),
 ('PKG2024004', 4, TIMESTAMP '2024-09-09 12:00:00', 102, '装车', '广州落地派送'),
 ('PKG2024005', 1, TIMESTAMP '2024-09-10 19:50:00', 402, '收寄', '上海仓揽收'),
 ('PKG2024006', 1, TIMESTAMP '2024-09-02 11:30:00', 401, '收寄', '北京仓揽收'),
 ('PKG2024006', 2, TIMESTAMP '2024-09-02 23:00:00', 101, '装车', '发往北京首都机场'),
 ('PKG2024006', 3, TIMESTAMP '2024-09-03 06:00:00', 301, '入库', '机场暂存'),
 ('PKG2024006', 4, TIMESTAMP '2024-09-03 15:30:00', 401, '签收', '王强到仓自提');

COMMIT;

SELECT '步骤 5 完成：示例数据已提交' AS section;


-- ============================================================
-- 步骤 6：视图
-- ============================================================
-- 6.1 位置与类型(概化落地后的样子)
CREATE OR REPLACE VIEW v_location_kind AS
SELECT l.location_id, l.location_name,
       CASE WHEN t.location_id IS NOT NULL THEN '卡车'
            WHEN p.location_id IS NOT NULL THEN '飞机'
            WHEN a.location_id IS NOT NULL THEN '机场'
            WHEN w.location_id IS NOT NULL THEN '仓库' END AS location_kind,
       COALESCE(t.plate_no, p.tail_no, a.iata_code, w.wh_code) AS biz_code,
       COALESCE(a.city, w.city) AS city
FROM location l
LEFT JOIN truck     t ON t.location_id = l.location_id
LEFT JOIN plane     p ON p.location_id = l.location_id
LEFT JOIN airport   a ON a.location_id = l.location_id
LEFT JOIN warehouse w ON w.location_id = l.location_id;

-- 6.2 包裹全景: 寄件人 + 收件人 + 服务(注意 sender/receiver 来自同一张 customer)
CREATE OR REPLACE VIEW v_package_full AS
SELECT p.tracking_no, s.customer_id AS sender_id, s.name AS sender_name,
       r.customer_id AS receiver_id, r.name AS receiver_name,
       sv.service_name, sv.sla_days, p.weight_kg, p.declared_value, p.created_at, p.status
FROM package p
JOIN customer s ON s.customer_id = p.sender_id
JOIN customer r ON r.customer_id = p.receiver_id
JOIN service  sv ON sv.service_code = p.service_code;

-- 6.3 完整轨迹(位置历史展开)
CREATE OR REPLACE VIEW v_package_track AS
SELECT e.tracking_no, e.event_seq, e.event_time, e.event_type,
       l.location_name, k.location_kind, e.note
FROM package_event e
JOIN location l ON l.location_id = e.location_id
JOIN v_location_kind k ON k.location_id = e.location_id;

-- 6.4 当前位置(派生: 取每条轨迹里 event_time 最大的一条)
CREATE OR REPLACE VIEW v_package_current AS
SELECT DISTINCT ON (e.tracking_no)
       e.tracking_no, e.event_time AS last_event_time, e.event_type,
       l.location_name AS current_location, k.location_kind AS current_kind
FROM package_event e
JOIN location l ON l.location_id = e.location_id
JOIN v_location_kind k ON k.location_id = e.location_id
ORDER BY e.tracking_no, e.event_time DESC, e.event_seq DESC;

-- 6.5 既是寄件人又是收件人的客户(题目那句话的查询)
CREATE OR REPLACE VIEW v_customer_both_roles AS
SELECT c.customer_id, c.name,
       count(DISTINCT p1.tracking_no) AS 寄件数,
       count(DISTINCT p2.tracking_no) AS 收件数
FROM customer c
LEFT JOIN package p1 ON p1.sender_id   = c.customer_id
LEFT JOIN package p2 ON p2.receiver_id = c.customer_id
GROUP BY c.customer_id, c.name
HAVING count(DISTINCT p1.tracking_no) > 0 AND count(DISTINCT p2.tracking_no) > 0;

SELECT '步骤 6 完成：5 个视图' AS section;


-- ============================================================
-- 步骤 7：验证查询
-- ============================================================
SELECT '7.1 行数统计' AS section;
SELECT 'customer' AS 表, count(*) AS 行数 FROM customer
UNION ALL SELECT 'service',       count(*) FROM service
UNION ALL SELECT 'package',       count(*) FROM package
UNION ALL SELECT 'location',      count(*) FROM location
UNION ALL SELECT 'truck',         count(*) FROM truck
UNION ALL SELECT 'plane',         count(*) FROM plane
UNION ALL SELECT 'airport',       count(*) FROM airport
UNION ALL SELECT 'warehouse',     count(*) FROM warehouse
UNION ALL SELECT 'package_event', count(*) FROM package_event
ORDER BY 1;

SELECT '7.2 位置与类型(概化)' AS section;
SELECT location_id, location_name, location_kind, biz_code, city FROM v_location_kind ORDER BY location_id;

SELECT '7.3 包裹全景(寄件人/收件人来自同一张 customer 表)' AS section;
SELECT tracking_no, sender_name, receiver_name, service_name, weight_kg, status FROM v_package_full ORDER BY tracking_no;

SELECT '7.4 完整轨迹(PKG2024001)' AS section;
SELECT event_seq, event_time, event_type, location_name, location_kind, note
FROM v_package_track WHERE tracking_no = 'PKG2024001' ORDER BY event_seq;

SELECT '7.5 所有包裹的当前位置(派生视图)' AS section;
SELECT * FROM v_package_current ORDER BY tracking_no;

SELECT '7.6 既是寄件人又是收件人的客户' AS section;
SELECT * FROM v_customer_both_roles ORDER BY customer_id;

SELECT '7.7 ISA 自检(应为 0 行: 有没有位置不是恰好一类)' AS section;
SELECT l.location_id, l.location_name,
       (SELECT count(*) FROM truck     WHERE location_id = l.location_id)
     + (SELECT count(*) FROM plane     WHERE location_id = l.location_id)
     + (SELECT count(*) FROM airport   WHERE location_id = l.location_id)
     + (SELECT count(*) FROM warehouse WHERE location_id = l.location_id) AS 子类行数
FROM location l
WHERE (SELECT count(*) FROM truck     WHERE location_id = l.location_id)
    + (SELECT count(*) FROM plane     WHERE location_id = l.location_id)
    + (SELECT count(*) FROM airport   WHERE location_id = l.location_id)
    + (SELECT count(*) FROM warehouse WHERE location_id = l.location_id) <> 1;


-- ============================================================
-- 步骤 8：反例测试 —— 预期报错(共 10 处)
-- ============================================================
\set ON_ERROR_STOP off

SELECT '8.1 预期报错：位置 101 已经是一辆卡车, 又想把它同时登记成机场(违反不相交)' AS section;
BEGIN;
INSERT INTO airport (location_id, iata_code, city) VALUES (101, 'XXX', '某地');
COMMIT;

SELECT '8.2 预期报错：插入一个位置行却没有任何子类行(违反全概化)' AS section;
INSERT INTO location (location_id, location_name) VALUES (999, '幽灵位置');

SELECT '8.3 预期报错：删掉机场 302 的子类行, 它就一类都不属于了(提交时报错)' AS section;
BEGIN;
DELETE FROM airport WHERE location_id = 302;
COMMIT;

SELECT '8.4 预期报错：事件时间早于包裹收寄时间' AS section;
INSERT INTO package_event (tracking_no, event_seq, event_time, location_id, event_type)
VALUES ('PKG2024005', 2, TIMESTAMP '2024-09-01 00:00:00', 402, '入库');

SELECT '8.5 预期报错：同一包裹的事件序号重复(主键冲突)' AS section;
INSERT INTO package_event (tracking_no, event_seq, event_time, location_id, event_type)
VALUES ('PKG2024001', 1, TIMESTAMP '2024-09-01 23:59:00', 101, '装车');

SELECT '8.6 预期报错：轨迹时间倒序(序号 8 的时间早于序号 7)' AS section;
INSERT INTO package_event (tracking_no, event_seq, event_time, location_id, event_type)
VALUES ('PKG2024001', 8, TIMESTAMP '2024-09-01 12:00:00', 101, '装车');

SELECT '8.7 预期报错：重量必须为正' AS section;
INSERT INTO package (tracking_no, sender_id, receiver_id, service_code, weight_kg,
                     length_cm, width_cm, height_cm, declared_value, created_at, status)
VALUES ('PKG2024099', 1, 2, 'STD', 0, 10, 10, 10, 0, TIMESTAMP '2024-09-11 10:00:00', '已收寄');

SELECT '8.8 预期报错：寄件人引用不存在的客户(外码)' AS section;
INSERT INTO package (tracking_no, sender_id, receiver_id, service_code, weight_kg,
                     length_cm, width_cm, height_cm, declared_value, created_at, status)
VALUES ('PKG2024098', 777, 2, 'STD', 1, 10, 10, 10, 0, TIMESTAMP '2024-09-11 10:00:00', '已收寄');

SELECT '8.9 预期报错：事件类型不在允许列表内' AS section;
INSERT INTO package_event (tracking_no, event_seq, event_time, location_id, event_type)
VALUES ('PKG2024005', 3, TIMESTAMP '2024-09-12 10:00:00', 402, '随便写');

SELECT '8.10 预期报错：删除还有历史事件的包裹(外码 RESTRICT)' AS section;
DELETE FROM package WHERE tracking_no = 'PKG2024001';

SELECT '步骤 8 完成：以上 10 处报错都是预期行为' AS section;


-- ============================================================
-- 步骤 9：收尾
-- ============================================================
SELECT '脚本执行完毕：9 张表 + 5 个视图 + 6 枚触发器 + 主码/外码/CHECK 已就绪' AS section;

-- 清空(顺序不能乱):
--   DROP VIEW  v_customer_both_roles, v_package_current, v_package_track, v_package_full, v_location_kind;
--   DROP TABLE package_event, truck, plane, airport, warehouse, location,
--              package, service, customer CASCADE;
--   DROP FUNCTION assert_location_isa(), assert_event_time_valid() CASCADE;
-- 或整库删掉: .\psql.bat "DROP DATABASE IF EXISTS express_demo WITH (FORCE)"


-- ============================================================
-- 实测结论(PostgreSQL 17.11, 库 express_demo, 实跑 0 警告 / 10 条预期报错)：
--   · 9 张表 + 5 个视图 + 6 枚触发器; 同库连跑两遍结果一致(可重复执行);
--   · 行数: customer 5 / service 3 / package 6 / location 8 /
--           truck 2 / plane 2 / airport 2 / warehouse 2 / package_event 24;
--   · 位置概化(7.2): 8 个位置被正确归类为 卡车×2 / 飞机×2 / 机场×2 / 仓库×2,
--     业务码分别取车牌、机尾号、三字码、仓号 —— 一个视图把四类属性统一出来;
--   · "客户可能两者都是"(7.3、7.6): 6 个包裹里 PKG2024003 是赵敏寄给自己的,
--     PKG2024001/2024002 是张伟与李娜互为寄收件人; 7.6 把"既寄过又收过"的客户列出来;
--   · 位置历史(7.4): PKG2024001 的 7 条轨迹把 仓库→卡车→机场→飞机→机场→卡车→仓库
--     串成一条完整链路; 7.5 的"当前位置"按 event_time 最大的一条现算(派生, 不存字段);
--   · 7.7 的 ISA 自检(每个位置是否恰好一类)返回 0 行;
--   · 10 处报错一处不多一处不少地被拦下:
--       位置 ISA 触发器 3 处(把卡车同时登记成机场=违反不相交; 位置 999 没有子类行
--       =违反全概化; 删掉机场 302 的子类行后提交=违反全概化)、
--       事件时间触发器 2 处(事件早于收寄时间; 轨迹时间倒序)、
--       主键冲突 1 处(同一包裹事件序号重复)、CHECK 2 处(重量为 0、事件类型非法)、
--       外键 1 处(寄件人不存在)、外键 RESTRICT 1 处(删除还有轨迹的包裹);
--   · 踩到并修掉的坑: 位置子类的 ISA 约束触发器一开始只挂了 DELETE/UPDATE,
--     于是"给同一位置再插第二类子类行"这种破坏不相交的写法没被拦住 ——
--     负测试对数(只有 9 条)把它暴露出来, 改成 INSERT OR DELETE OR UPDATE 后正常。
--   · 提醒: 建表阶段 \set ON_ERROR_STOP on, 反例阶段才关掉;
--     位置 ISA 检查推迟到提交时, 所以超类行与子类行必须写在同一个事务里(步骤 5)。
-- ============================================================
