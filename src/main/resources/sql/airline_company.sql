-- ============================================================
-- 实践习题 7.23 航空公司数据库 —— E-R 图落成 PostgreSQL 数据库 + 一组约束
-- 数据库: airline_demo (PostgreSQL 17.11)
-- 教材: 《数据库系统概念》第 7 章 数据库设计和 E-R 模型 · 实践习题 7.23
-- 配套图: db-homework/airline-er.html (E-R 图 + 关系模式 + 约束清单)
-- ------------------------------------------------------------
-- 要追踪的东西(题目原话):
--   客户及其预订 · 航班 · 旅客在航班上的状态 · 座位分配 · 未来航班的时刻表与飞行路线。
--
-- 机票分三层(这是本题设计的关键):
--   timetable 时刻表  : schedule_id + flight_no + 班期(星期几) + 起降时刻 + 有效期
--   flight    具体航班 : (flight_no, flight_date) —— 某班号在某一天的那次飞行, 来自某条时刻表
--   booking_segment    : (booking_id, seg_no) —— 某位旅客在某个航班上的座位与状态
-- 座位分配与乘机状态(已确认/候补/已登机/已取消)都在 booking_segment 上;
-- 联程票 = 同一张 booking 下的多条 segment。
--
-- 两个弱实体:
--   route_leg        : (route_no, leg_no)      —— 一条飞行路线由若干航段组成, 每段起降两个机场
--   booking_segment  : (booking_id, seg_no)    —— 订单下的一段行程
--
-- 表名不加前缀: 本脚本建在独立库 airline_demo 里。
--
-- 运行:
--   .\psql.bat "DROP DATABASE IF EXISTS airline_demo WITH (FORCE)"
--   .\psql.bat "CREATE DATABASE airline_demo"
--   .\psql.bat -d airline_demo -f src\main\resources\sql\airline_company.sql
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
DROP VIEW IF EXISTS v_flight_load;
DROP VIEW IF EXISTS v_flight_seat_map;
DROP VIEW IF EXISTS v_booking_detail;
DROP VIEW IF EXISTS v_timetable_future;
DROP VIEW IF EXISTS v_flight_detail;
DROP VIEW IF EXISTS v_route_chain;
DROP TABLE IF EXISTS booking_segment, booking, flight, timetable, route_leg, route,
                     airplane, aircraft_type, airport, customer CASCADE;
DROP FUNCTION IF EXISTS assert_flight_matches_timetable() CASCADE;
DROP FUNCTION IF EXISTS assert_seat_within_capacity() CASCADE;
DROP FUNCTION IF EXISTS assert_segment_seat_status() CASCADE;
DROP FUNCTION IF EXISTS assert_route_legs_continuous() CASCADE;

SELECT '步骤 0 完成：旧对象已清理' AS section;


-- ============================================================
-- 步骤 1：客户 / 机场 / 机型 / 飞机（基础字典表）
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

CREATE TABLE airport (
    airport_code VARCHAR(3)  NOT NULL,                -- 主码: IATA 三字码
    airport_name VARCHAR(60) NOT NULL,
    city         VARCHAR(40) NOT NULL,
    country      VARCHAR(40) NOT NULL,
    CONSTRAINT pk_airport PRIMARY KEY (airport_code),
    CONSTRAINT ck_airport_code CHECK (airport_code ~ '^[A-Z]{3}$')
);

CREATE TABLE aircraft_type (
    type_code     VARCHAR(6)  NOT NULL,               -- 主码: 机型代码
    manufacturer  VARCHAR(30) NOT NULL,
    model         VARCHAR(40) NOT NULL,
    seat_capacity SMALLINT    NOT NULL,               -- 座位数
    CONSTRAINT pk_aircraft_type PRIMARY KEY (type_code),
    CONSTRAINT ck_aircraft_capacity CHECK (seat_capacity > 0)
);

CREATE TABLE airplane (
    reg_no        VARCHAR(10) NOT NULL,               -- 主码: 机尾号
    type_code     VARCHAR(6)  NOT NULL,               -- 机型(外码)
    delivery_date DATE            NULL,
    status        VARCHAR(10) NOT NULL,               -- 在役/检修/退役
    CONSTRAINT pk_airplane PRIMARY KEY (reg_no),
    CONSTRAINT fk_airplane_type FOREIGN KEY (type_code)
        REFERENCES aircraft_type (type_code) ON DELETE RESTRICT,
    CONSTRAINT ck_airplane_status CHECK (status IN ('在役','检修','退役'))
);

SELECT '步骤 1 完成：customer / airport / aircraft_type / airplane' AS section;


-- ============================================================
-- 步骤 2：飞行路线 route + 弱实体 route_leg（航段）
--   route_leg 的主码 = (route_no, leg_no): route_no 来自标识联系 legs,
--   leg_no 是部分键; 每段起降两个机场(departs_from / arrives_at)。
-- ============================================================
CREATE TABLE route (
    route_no   VARCHAR(8)  NOT NULL,                  -- 主码: 航线号
    route_name VARCHAR(60) NOT NULL,                  -- 航线名, 如 京沪快线
    CONSTRAINT pk_route PRIMARY KEY (route_no)
);

CREATE TABLE route_leg (
    route_no    VARCHAR(8)  NOT NULL,                 -- 所属航线(外码 + 主码一部分)
    leg_no      SMALLINT    NOT NULL,                 -- 部分键: 第几段, 从 1 开始
    from_code   VARCHAR(3)  NOT NULL,                 -- 出发机场(外码)
    to_code     VARCHAR(3)  NOT NULL,                 -- 到达机场(外码)
    distance_km NUMERIC(7,1) NOT NULL,                -- 航段距离
    CONSTRAINT pk_route_leg PRIMARY KEY (route_no, leg_no),
    CONSTRAINT fk_leg_route FOREIGN KEY (route_no)
        REFERENCES route (route_no) ON DELETE CASCADE,
    CONSTRAINT fk_leg_from FOREIGN KEY (from_code)
        REFERENCES airport (airport_code) ON DELETE RESTRICT,
    CONSTRAINT fk_leg_to FOREIGN KEY (to_code)
        REFERENCES airport (airport_code) ON DELETE RESTRICT,
    CONSTRAINT ck_leg_seq CHECK (leg_no >= 1),
    CONSTRAINT ck_leg_distance CHECK (distance_km > 0),
    CONSTRAINT ck_leg_diff_airport CHECK (from_code <> to_code)
);
COMMENT ON TABLE route_leg IS '弱实体: 飞行路线的航段; 主码 = 航线号 + 航段号';

SELECT '步骤 2 完成：route + route_leg(弱实体)' AS section;


-- ============================================================
-- 步骤 3：时刻表 timetable + 具体航班 flight
--   timetable: 未来航班怎么排(班号 + 班期 + 起降时刻 + 有效期)
--   flight   : 具体哪一天的那一班(flight_no, flight_date), 引用它来自的时刻表与执飞飞机
-- ============================================================
CREATE TABLE timetable (
    schedule_id  INTEGER     NOT NULL,                -- 主码: 时刻表行号
    flight_no    VARCHAR(8)  NOT NULL,                -- 航班号, 如 CA1501
    route_no     VARCHAR(8)  NOT NULL,                -- 飞哪条航线(外码)
    type_code    VARCHAR(6)  NOT NULL,                -- 计划机型(外码)
    day_of_week  SMALLINT    NOT NULL,                -- 班期: 1=周一 … 7=周日
    dep_time     TIME        NOT NULL,                -- 计划起飞时刻
    arr_time     TIME        NOT NULL,                -- 计划到达时刻
    valid_from   DATE        NOT NULL,                -- 有效期起
    valid_to     DATE        NOT NULL,                -- 有效期止
    CONSTRAINT pk_timetable PRIMARY KEY (schedule_id),
    CONSTRAINT uq_timetable UNIQUE (flight_no, day_of_week, valid_from),
    CONSTRAINT fk_timetable_route FOREIGN KEY (route_no)
        REFERENCES route (route_no) ON DELETE RESTRICT,
    CONSTRAINT fk_timetable_type FOREIGN KEY (type_code)
        REFERENCES aircraft_type (type_code) ON DELETE RESTRICT,
    CONSTRAINT ck_tt_dow CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_tt_period CHECK (valid_from < valid_to),
    CONSTRAINT ck_tt_times CHECK (dep_time < arr_time)
);
COMMENT ON TABLE timetable IS '未来航班的时刻表: 班号 + 班期 + 起降时刻 + 有效期';

CREATE TABLE flight (
    flight_no     VARCHAR(8) NOT NULL,                -- 航班号(与 flight_date 一起构成主码)
    flight_date   DATE       NOT NULL,                -- 航班日期
    schedule_id   INTEGER    NOT NULL,                -- 来自哪条时刻表(外码)
    reg_no        VARCHAR(10) NOT NULL,               -- 执飞飞机(外码)
    dep_time      TIME       NOT NULL,                -- 实际(计划)起飞时刻
    arr_time      TIME       NOT NULL,                -- 实际(计划)到达时刻
    flight_status VARCHAR(8) NOT NULL,                -- 计划/延误/已起飞/已到达/取消
    CONSTRAINT pk_flight PRIMARY KEY (flight_no, flight_date),
    CONSTRAINT fk_flight_schedule FOREIGN KEY (schedule_id)
        REFERENCES timetable (schedule_id) ON DELETE RESTRICT,
    CONSTRAINT fk_flight_airplane FOREIGN KEY (reg_no)
        REFERENCES airplane (reg_no) ON DELETE RESTRICT,
    CONSTRAINT ck_flight_times CHECK (dep_time < arr_time),
    CONSTRAINT ck_flight_status CHECK (flight_status IN ('计划','延误','已起飞','已到达','取消'))
);

SELECT '步骤 3 完成：timetable + flight' AS section;


-- ============================================================
-- 步骤 4：预订 booking + 弱实体 booking_segment（订座航段）
--   booking_segment 装着题目要的三件事:
--     旅客在航班上的状态 segment_status、座位分配 seat_no、舱位 cabin_class。
--   复合外码 (flight_no, flight_date) 就是"订座 ↔ 航班"这个 M:N 联系的落地形式。
--   座位冲突用"部分唯一索引"解决: 只约束未取消的订座。
-- ============================================================
CREATE TABLE booking (
    booking_id     INTEGER       NOT NULL,            -- 主码: 订单号
    customer_id    INTEGER       NOT NULL,            -- 下单客户(外码)
    booking_date   DATE          NOT NULL,
    total_amount   NUMERIC(10,2) NOT NULL,
    booking_status VARCHAR(8)    NOT NULL,            -- 已确认/候补/已取消
    CONSTRAINT pk_booking PRIMARY KEY (booking_id),
    CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id)
        REFERENCES customer (customer_id) ON DELETE RESTRICT,
    CONSTRAINT ck_booking_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_booking_status CHECK (booking_status IN ('已确认','候补','已取消'))
);

CREATE TABLE booking_segment (
    booking_id     INTEGER     NOT NULL,              -- 所属订单(外码 + 主码一部分)
    seg_no         SMALLINT    NOT NULL,              -- 部分键: 第几段行程
    flight_no      VARCHAR(8)  NOT NULL,              -- 航班号(复合外码)
    flight_date    DATE        NOT NULL,              -- 航班日期(复合外码)
    cabin_class    VARCHAR(6)  NOT NULL,              -- 经济舱/公务舱/头等舱
    seat_no        VARCHAR(4)      NULL,              -- 座位号, 如 12A; 候补可空
    segment_status VARCHAR(6)  NOT NULL,              -- 已确认/候补/已登机/已取消
    CONSTRAINT pk_booking_segment PRIMARY KEY (booking_id, seg_no),
    CONSTRAINT fk_seg_booking FOREIGN KEY (booking_id)
        REFERENCES booking (booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_seg_flight FOREIGN KEY (flight_no, flight_date)
        REFERENCES flight (flight_no, flight_date) ON DELETE RESTRICT,
    CONSTRAINT ck_seg_seq CHECK (seg_no >= 1),
    CONSTRAINT ck_seg_cabin CHECK (cabin_class IN ('经济舱','公务舱','头等舱')),
    CONSTRAINT ck_seg_seat CHECK (seat_no IS NULL OR seat_no ~ '^[0-9]{1,2}[A-K]$'),
    CONSTRAINT ck_seg_status CHECK (segment_status IN ('已确认','候补','已登机','已取消'))
);
COMMENT ON TABLE booking_segment IS '弱实体: 订座航段(座位分配 + 乘机状态); 主码 = 订单号 + 航段号';

-- 座位不被重复占用: 只对"未取消"的订座生效(取消的订座自动让出座位)
CREATE UNIQUE INDEX uq_seat_per_flight
    ON booking_segment (flight_no, flight_date, seat_no)
    WHERE seat_no IS NOT NULL AND segment_status <> '已取消';

SELECT '步骤 4 完成：booking + booking_segment(弱实体 + 座位部分唯一索引)' AS section;


-- ============================================================
-- 步骤 5：跨表语义约束(触发器)
--   5.1 具体航班必须与它引用的时刻表对齐: 班号一致、日期的星期匹配班期、日期在有效期内
--   5.2 座位不能超出机型容量(座位排号 × 6 ≤ 机型座位数, 跨四张表)
--   5.3 状态与座位自洽: 已确认/已登机 必须有座位; 候补可以没有
--   5.4 航线航段必须首尾相接且编号连续(可延迟约束触发器)
-- ============================================================
CREATE OR REPLACE FUNCTION assert_flight_matches_timetable() RETURNS trigger AS $$
DECLARE
    tt timetable%ROWTYPE;
BEGIN
    SELECT * INTO tt FROM timetable WHERE schedule_id = NEW.schedule_id;
    IF tt.schedule_id IS NULL THEN
        RETURN NEW;                                   -- 时刻表不存在交给外码报错
    END IF;
    IF tt.flight_no <> NEW.flight_no THEN
        RAISE EXCEPTION '航班与时刻表不符: 时刻表 % 的班号是 %, 航班却写成 %',
            tt.schedule_id, tt.flight_no, NEW.flight_no USING ERRCODE = 'check_violation';
    END IF;
    IF EXTRACT(ISODOW FROM NEW.flight_date)::int <> tt.day_of_week THEN
        RAISE EXCEPTION '航班日期与班期不符: % 是星期 %, 而时刻表 % 的班期是星期 %',
            NEW.flight_date, EXTRACT(ISODOW FROM NEW.flight_date)::int,
            tt.schedule_id, tt.day_of_week USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.flight_date < tt.valid_from OR NEW.flight_date > tt.valid_to THEN
        RAISE EXCEPTION '航班日期超出时刻表有效期: % 不在 % ~ % 之内',
            NEW.flight_date, tt.valid_from, tt.valid_to USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_flight_matches_timetable
    BEFORE INSERT OR UPDATE ON flight
    FOR EACH ROW EXECUTE FUNCTION assert_flight_matches_timetable();

CREATE OR REPLACE FUNCTION assert_seat_within_capacity() RETURNS trigger AS $$
DECLARE
    v_row      integer;
    v_capacity integer;
BEGIN
    IF NEW.seat_no IS NULL THEN
        RETURN NEW;
    END IF;
    v_row := substring(NEW.seat_no from '^[0-9]+')::integer;
    SELECT at.seat_capacity INTO v_capacity
    FROM flight f
    JOIN airplane a        ON a.reg_no = f.reg_no
    JOIN aircraft_type at  ON at.type_code = a.type_code
    WHERE f.flight_no = NEW.flight_no AND f.flight_date = NEW.flight_date;
    IF v_capacity IS NOT NULL AND v_row * 6 > v_capacity THEN
        RAISE EXCEPTION '座位超出机型容量: 航班 %(%) 的机型只有 % 个座位(按每排 6 座, 座位 % 排号过大)',
            NEW.flight_no, NEW.flight_date, v_capacity, NEW.seat_no USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_seat_within_capacity
    BEFORE INSERT OR UPDATE ON booking_segment
    FOR EACH ROW EXECUTE FUNCTION assert_seat_within_capacity();

CREATE OR REPLACE FUNCTION assert_segment_seat_status() RETURNS trigger AS $$
BEGIN
    IF NEW.segment_status IN ('已确认', '已登机') AND NEW.seat_no IS NULL THEN
        RAISE EXCEPTION '状态与座位不自洽: 订座 %-% 是 %, 必须分配座位号',
            NEW.booking_id, NEW.seg_no, NEW.segment_status USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_segment_seat_status
    BEFORE INSERT OR UPDATE ON booking_segment
    FOR EACH ROW EXECUTE FUNCTION assert_segment_seat_status();

CREATE OR REPLACE FUNCTION assert_route_legs_continuous() RETURNS trigger AS $$
DECLARE
    routes varchar(8)[] := ARRAY[]::varchar(8)[];
    r      varchar(8);
    rec    record;
    prev   record := NULL;
BEGIN
    -- UPDATE 时新旧航线都要复查(航段可能被挪到别的航线)
    IF TG_OP <> 'INSERT' THEN routes := routes || OLD.route_no; END IF;
    IF TG_OP <> 'DELETE' THEN routes := routes || NEW.route_no; END IF;

    FOR r IN SELECT DISTINCT unnest(routes) LOOP
        prev := NULL;
        FOR rec IN
            SELECT leg_no, from_code, to_code FROM route_leg WHERE route_no = r ORDER BY leg_no
        LOOP
            IF prev IS NOT NULL THEN
                IF rec.leg_no <> prev.leg_no + 1 THEN
                    RAISE EXCEPTION '航段编号不连续: 航线 % 在 % 段之后直接跳到 % 段',
                        r, prev.leg_no, rec.leg_no USING ERRCODE = 'check_violation';
                END IF;
                IF rec.from_code <> prev.to_code THEN
                    RAISE EXCEPTION '航段不衔接: 航线 % 第 % 段到达 %, 第 % 段却从 % 出发',
                        r, prev.leg_no, prev.to_code, rec.leg_no, rec.from_code USING ERRCODE = 'check_violation';
                END IF;
            ELSIF rec.leg_no <> 1 THEN
                RAISE EXCEPTION '航段编号必须从 1 开始: 航线 % 的第一段是 %', r, rec.leg_no
                    USING ERRCODE = 'check_violation';
            END IF;
            prev := rec;
        END LOOP;
    END LOOP;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_route_legs_continuous
    AFTER INSERT OR UPDATE OR DELETE ON route_leg
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_route_legs_continuous();

SELECT '步骤 5 完成：4 枚业务约束触发器' AS section;


-- ============================================================
-- 步骤 6：示例数据
--   航段不衔接的检查推迟到提交, 所以航段数据要放在同一事务里。
-- ============================================================
BEGIN;

INSERT INTO customer (customer_id, name, phone, email, address) VALUES
 (1, '张伟', '13800000001', 'zhangwei@example.com',  '北京市海淀区中关村大街1号'),
 (2, '李娜', '13800000002', 'lina@example.com',      '上海市浦东新区世纪大道100号'),
 (3, '王强', '13800000003', 'wangqiang@example.com', '广州市天河区体育西路50号'),
 (4, '赵敏', '13800000004', 'zhaomin@example.com',   '成都市武侯区人民南路4段20号');

INSERT INTO airport (airport_code, airport_name, city, country) VALUES
 ('PEK', '北京首都国际机场', '北京', '中国'),
 ('PVG', '上海浦东国际机场', '上海', '中国'),
 ('CAN', '广州白云国际机场', '广州', '中国'),
 ('CTU', '成都天府国际机场', '成都', '中国');

INSERT INTO aircraft_type (type_code, manufacturer, model, seat_capacity) VALUES
 ('B738', 'Boeing', '737-800', 162),
 ('A333', 'Airbus', 'A330-300', 262),
 ('B789', 'Boeing', '787-9',   293);

INSERT INTO airplane (reg_no, type_code, delivery_date, status) VALUES
 ('B-5678', 'B738', DATE '2018-05-20', '在役'),
 ('B-1234', 'A333', DATE '2019-09-12', '在役'),
 ('B-9876', 'B789', DATE '2021-03-08', '在役'),
 ('B-2468', 'B738', DATE '2016-11-01', '检修');

-- 飞行路线: R1 京沪单段; R2 京-蓉-穗 两段; R3 沪-穗 单段
INSERT INTO route (route_no, route_name) VALUES
 ('R1', '京沪快线'),
 ('R2', '京蓉穗联程'),
 ('R3', '沪穗线');

INSERT INTO route_leg (route_no, leg_no, from_code, to_code, distance_km) VALUES
 ('R1', 1, 'PEK', 'PVG', 1080.0),
 ('R2', 1, 'PEK', 'CTU', 1550.0),
 ('R2', 2, 'CTU', 'CAN', 1230.0),
 ('R3', 1, 'PVG', 'CAN', 1200.0);

-- 时刻表: 未来航班怎么排(2024-10-01 ~ 2025-03-31 冬春航季)
INSERT INTO timetable (schedule_id, flight_no, route_no, type_code, day_of_week, dep_time, arr_time, valid_from, valid_to) VALUES
 (1, 'CA1501', 'R1', 'B738', 1, TIME '08:00', TIME '10:15', DATE '2024-10-01', DATE '2025-03-31'),
 (2, 'CA1501', 'R1', 'B738', 4, TIME '08:00', TIME '10:15', DATE '2024-10-01', DATE '2025-03-31'),
 (3, 'CA4301', 'R2', 'B789', 2, TIME '09:30', TIME '14:20', DATE '2024-10-01', DATE '2025-03-31'),
 (4, 'MU5101', 'R3', 'A333', 5, TIME '14:00', TIME '16:40', DATE '2024-10-01', DATE '2025-03-31'),
 (5, 'MU5102', 'R3', 'A333', 7, TIME '18:30', TIME '21:10', DATE '2024-10-01', DATE '2025-03-31');

-- 具体航班(日期必须落在对应班期的星期上)
INSERT INTO flight (flight_no, flight_date, schedule_id, reg_no, dep_time, arr_time, flight_status) VALUES
 ('CA1501', DATE '2024-10-07', 1, 'B-5678', TIME '08:05', TIME '10:20', '已到达'),  -- 周一
 ('CA1501', DATE '2024-10-10', 2, 'B-5678', TIME '08:00', TIME '10:15', '计划'),    -- 周四
 ('CA4301', DATE '2024-10-08', 3, 'B-9876', TIME '09:40', TIME '14:25', '已到达'),  -- 周二
 ('MU5101', DATE '2024-10-11', 4, 'B-1234', TIME '14:20', TIME '17:00', '计划'),    -- 周五
 ('MU5102', DATE '2024-10-13', 5, 'B-1234', TIME '18:30', TIME '21:10', '计划');    -- 周日

INSERT INTO booking (booking_id, customer_id, booking_date, total_amount, booking_status) VALUES
 (1, 1, DATE '2024-10-01', 1280.00, '已确认'),
 (2, 2, DATE '2024-10-02', 4560.00, '已确认'),
 (3, 3, DATE '2024-10-03',  980.00, '候补'),
 (4, 4, DATE '2024-10-04', 2350.00, '已取消'),
 (5, 1, DATE '2024-10-05', 1120.00, '已确认'),
 (6, 3, DATE '2024-10-06', 1400.00, '已确认');

-- 订座航段: 含"候补无座位"(订座3)、"已取消让出座位"(订座4 的 5A 被订座6 重新买走)、联程(订座2 两段)
INSERT INTO booking_segment (booking_id, seg_no, flight_no, flight_date, cabin_class, seat_no, segment_status) VALUES
 (1, 1, 'CA1501', DATE '2024-10-07', '经济舱', '12A', '已登机'),
 (2, 1, 'CA4301', DATE '2024-10-08', '经济舱', '25F', '已登机'),
 (2, 2, 'MU5101', DATE '2024-10-11', '经济舱', '18D', '已确认'),
 (3, 1, 'CA1501', DATE '2024-10-07', '经济舱', NULL,  '候补'),
 (4, 1, 'MU5101', DATE '2024-10-11', '公务舱', '5A',  '已取消'),
 (5, 1, 'MU5102', DATE '2024-10-13', '经济舱', '12A', '已确认'),
 (6, 1, 'MU5101', DATE '2024-10-11', '公务舱', '5A',  '已确认');   -- 座位被取消后重新卖出

COMMIT;

SELECT '步骤 6 完成：示例数据已提交' AS section;


-- ============================================================
-- 步骤 7：视图
-- ============================================================
-- 7.1 航线航段链(按 leg_no 串起来, 用窗口函数标出上一段到达机场)
CREATE OR REPLACE VIEW v_route_chain AS
SELECT r.route_no, r.route_name, l.leg_no, l.from_code, fa.city AS from_city,
       l.to_code, ta.city AS to_city, l.distance_km,
       lag(l.to_code) OVER (PARTITION BY l.route_no ORDER BY l.leg_no) AS prev_to_code
FROM route r
JOIN route_leg l ON l.route_no = r.route_no
JOIN airport fa ON fa.airport_code = l.from_code
JOIN airport ta ON ta.airport_code = l.to_code;

-- 7.2 航班全景(时刻表 + 航线 + 机型 + 飞机)
CREATE OR REPLACE VIEW v_flight_detail AS
SELECT f.flight_no, f.flight_date, EXTRACT(ISODOW FROM f.flight_date)::int AS dow,
       r.route_no, r.route_name, tt.type_code, a.reg_no, ac.model,
       ac.seat_capacity, f.dep_time, f.arr_time, f.flight_status
FROM flight f
JOIN timetable tt     ON tt.schedule_id = f.schedule_id
JOIN route r          ON r.route_no = tt.route_no
JOIN airplane a       ON a.reg_no = f.reg_no
JOIN aircraft_type ac ON ac.type_code = a.type_code;

-- 7.3 未来时刻表
CREATE OR REPLACE VIEW v_timetable_future AS
SELECT tt.schedule_id, tt.flight_no, r.route_name, tt.day_of_week,
       CASE tt.day_of_week WHEN 1 THEN '周一' WHEN 2 THEN '周二' WHEN 3 THEN '周三'
                           WHEN 4 THEN '周四' WHEN 5 THEN '周五' WHEN 6 THEN '周六' ELSE '周日' END AS 班期,
       tt.dep_time, tt.arr_time, tt.type_code, tt.valid_from, tt.valid_to
FROM timetable tt JOIN route r ON r.route_no = tt.route_no;

-- 7.4 预订明细(客户 / 订单 / 航段 / 航班 / 座位 / 状态)
CREATE OR REPLACE VIEW v_booking_detail AS
SELECT b.booking_id, c.customer_id, c.name AS customer_name, b.booking_status,
       s.seg_no, s.flight_no, s.flight_date, f.flight_status,
       s.cabin_class, s.seat_no, s.segment_status
FROM booking b
JOIN customer c        ON c.customer_id = b.customer_id
JOIN booking_segment s ON s.booking_id = b.booking_id
JOIN flight f          ON f.flight_no = s.flight_no AND f.flight_date = s.flight_date;

-- 7.5 航班座位图(每个航班已占座位清单)
CREATE OR REPLACE VIEW v_flight_seat_map AS
SELECT f.flight_no, f.flight_date, ac.seat_capacity,
       count(s.seat_no) AS 已占座位数,
       ac.seat_capacity - count(s.seat_no) AS 剩余座位,
       string_agg(s.seat_no, ', ' ORDER BY s.seat_no) AS 已占座位
FROM flight f
JOIN airplane a       ON a.reg_no = f.reg_no
JOIN aircraft_type ac ON ac.type_code = a.type_code
LEFT JOIN booking_segment s
       ON s.flight_no = f.flight_no AND s.flight_date = f.flight_date
      AND s.seat_no IS NOT NULL AND s.segment_status <> '已取消'
GROUP BY f.flight_no, f.flight_date, ac.seat_capacity;

-- 7.6 客座率
CREATE OR REPLACE VIEW v_flight_load AS
SELECT m.flight_no, m.flight_date, m.seat_capacity, m.已占座位数, m.剩余座位,
       round(100.0 * m.已占座位数 / m.seat_capacity, 1) AS 客座率百分比
FROM v_flight_seat_map m;

SELECT '步骤 7 完成：6 个视图' AS section;


-- ============================================================
-- 步骤 8：验证查询
-- ============================================================
SELECT '8.1 行数统计' AS section;
SELECT 'customer' AS 表, count(*) AS 行数 FROM customer
UNION ALL SELECT 'airport',         count(*) FROM airport
UNION ALL SELECT 'aircraft_type',   count(*) FROM aircraft_type
UNION ALL SELECT 'airplane',        count(*) FROM airplane
UNION ALL SELECT 'route',           count(*) FROM route
UNION ALL SELECT 'route_leg',       count(*) FROM route_leg
UNION ALL SELECT 'timetable',       count(*) FROM timetable
UNION ALL SELECT 'flight',          count(*) FROM flight
UNION ALL SELECT 'booking',         count(*) FROM booking
UNION ALL SELECT 'booking_segment', count(*) FROM booking_segment
ORDER BY 1;

SELECT '8.2 飞行路线(航段链)' AS section;
SELECT route_no, route_name, leg_no, from_city, to_city, distance_km, prev_to_code
FROM v_route_chain ORDER BY route_no, leg_no;

SELECT '8.3 未来航班时刻表' AS section;
SELECT flight_no, route_name, 班期, dep_time, arr_time, type_code, valid_from, valid_to
FROM v_timetable_future ORDER BY flight_no, day_of_week;

SELECT '8.4 航班全景' AS section;
SELECT flight_no, flight_date, dow, route_name, reg_no, model, seat_capacity, flight_status
FROM v_flight_detail ORDER BY flight_date, flight_no;

SELECT '8.5 预订明细(座位分配 + 乘机状态)' AS section;
SELECT booking_id, customer_name, booking_status, seg_no, flight_no, flight_date,
       cabin_class, seat_no, segment_status
FROM v_booking_detail ORDER BY booking_id, seg_no;

SELECT '8.6 航班座位图与客座率' AS section;
SELECT * FROM v_flight_load ORDER BY flight_date, flight_no;

SELECT '8.7 航段衔接自检(应为 0 行: 有断链的航线)' AS section;
SELECT route_no, leg_no, from_code, prev_to_code
FROM v_route_chain
WHERE prev_to_code IS NOT NULL AND prev_to_code <> from_code;

SELECT '8.8 座位冲突自检(应为 0 行)' AS section;
SELECT flight_no, flight_date, seat_no, count(*) AS 占用次数
FROM booking_segment
WHERE seat_no IS NOT NULL AND segment_status <> '已取消'
GROUP BY flight_no, flight_date, seat_no
HAVING count(*) > 1;


-- ============================================================
-- 步骤 9：反例测试 —— 预期报错(共 10 处)
-- ============================================================
\set ON_ERROR_STOP off

SELECT '9.1 预期报错：航班日期与时刻表班期不符(CA1501 只有周一/周四, 却排周三)' AS section;
INSERT INTO flight (flight_no, flight_date, schedule_id, reg_no, dep_time, arr_time, flight_status)
VALUES ('CA1501', DATE '2024-10-09', 1, 'B-5678', TIME '08:00', TIME '10:15', '计划');

SELECT '9.2 预期报错：航班日期超出时刻表有效期' AS section;
INSERT INTO flight (flight_no, flight_date, schedule_id, reg_no, dep_time, arr_time, flight_status)
VALUES ('CA1501', DATE '2025-04-07', 1, 'B-5678', TIME '08:00', TIME '10:15', '计划');

SELECT '9.3 预期报错：航班班号与它引用的时刻表不一致' AS section;
INSERT INTO flight (flight_no, flight_date, schedule_id, reg_no, dep_time, arr_time, flight_status)
VALUES ('CA1502', DATE '2024-10-14', 1, 'B-5678', TIME '08:00', TIME '10:15', '计划');

SELECT '9.4 预期报错：座位排号超出机型容量(B738 只有 162 座, 却给 60A)' AS section;
INSERT INTO booking_segment (booking_id, seg_no, flight_no, flight_date, cabin_class, seat_no, segment_status)
VALUES (1, 2, 'CA1501', DATE '2024-10-10', '经济舱', '60A', '已确认');

SELECT '9.5 预期报错：座位号格式非法(12Z 不是合法座位号)' AS section;
INSERT INTO booking_segment (booking_id, seg_no, flight_no, flight_date, cabin_class, seat_no, segment_status)
VALUES (1, 3, 'CA1501', DATE '2024-10-10', '经济舱', '12Z', '已确认');

SELECT '9.6 预期报错：同一航班同一座位被两个未取消订座占用(部分唯一索引)' AS section;
INSERT INTO booking_segment (booking_id, seg_no, flight_no, flight_date, cabin_class, seat_no, segment_status)
VALUES (5, 2, 'MU5102', DATE '2024-10-13', '经济舱', '12A', '已确认');

SELECT '9.7 预期报错：已确认的订座没有座位号(状态与座位不自洽)' AS section;
INSERT INTO booking_segment (booking_id, seg_no, flight_no, flight_date, cabin_class, seat_no, segment_status)
VALUES (6, 2, 'MU5102', DATE '2024-10-13', '经济舱', NULL, '已确认');

SELECT '9.8 预期报错：航段同场起降(from = to)' AS section;
INSERT INTO route_leg (route_no, leg_no, from_code, to_code, distance_km)
VALUES ('R3', 2, 'CAN', 'CAN', 100.0);

SELECT '9.9 预期报错：航段不衔接(第 2 段从 PEK 出发, 而第 1 段到达 CTU)' AS section;
BEGIN;
INSERT INTO route_leg (route_no, leg_no, from_code, to_code, distance_km)
VALUES ('R2', 3, 'PEK', 'PVG', 1080.0);
COMMIT;

SELECT '9.10 预期报错：删除还有订座的航班(外码 RESTRICT)' AS section;
DELETE FROM flight WHERE flight_no = 'CA1501' AND flight_date = DATE '2024-10-07';

SELECT '步骤 9 完成：以上 10 处报错都是预期行为' AS section;


-- ============================================================
-- 步骤 10：收尾
-- ============================================================
SELECT '脚本执行完毕：10 张表 + 6 个视图 + 4 枚约束触发器 + 1 个部分唯一索引已就绪' AS section;

-- 清空(顺序不能乱):
--   DROP VIEW v_flight_load, v_flight_seat_map, v_booking_detail, v_timetable_future, v_flight_detail, v_route_chain;
--   DROP TABLE booking_segment, booking, flight, timetable, route_leg, route,
--              airplane, aircraft_type, airport, customer CASCADE;
--   DROP FUNCTION assert_flight_matches_timetable(), assert_seat_within_capacity(),
--                assert_segment_seat_status(), assert_route_legs_continuous() CASCADE;
-- 或整库删掉: .\psql.bat "DROP DATABASE IF EXISTS airline_demo WITH (FORCE)"


-- ============================================================
-- 实测结论(PostgreSQL 17.11, 库 airline_demo, 实跑 0 警告 / 10 条预期报错)：
--   · 10 张表 + 6 个视图 + 4 枚业务约束触发器 + 1 个部分唯一索引;
--     同库连跑两遍结果一致(可重复执行);
--   · 行数: customer 4 / airport 4 / aircraft_type 3 / airplane 4 / route 3 /
--           route_leg 4 / timetable 5 / flight 5 / booking 6 / booking_segment 7;
--   · 飞行路线(8.2): R2"京蓉穗联程"两段 北京→成都→广州, 第 2 段的 prev_to_code
--     正好等于第 1 段的 to_code, 链式关系成立; R1、R3 各 1 段;
--   · 时刻表(8.3): 5 条班期(CA1501 周一/周四、CA4301 周二、MU5101 周五、MU5102 周日),
--     带起降时刻与有效期 2024-10-01 ~ 2025-03-31;
--   · 具体航班(8.4): 5 个航班分别落在 10-07(周一)、10-08(周二)、10-10(周四)、
--     10-11(周五)、10-13(周日) —— 与各自时刻表的班期一致, 并带出执飞机型与座位数;
--   · 预订与座位(8.5)三种情形都在: 联程票(订座 2 两段)、候补无座位(订座 3)、
--     已取消让出座位(订座 4 的公务舱 5A 被订座 6 重新买走);
--   · 8.7 航段衔接自检、8.8 座位冲突自检均为 0 行;
--   · 10 处报错一处不多一处不少地被拦下:
--       航班↔时刻表触发器 3 处(日期的星期与班期不符、日期超出有效期、班号与时刻表不一致)、
--       座位容量触发器 1 处(60A 超出 B738 的 162 座)、状态座位自洽触发器 1 处(已确认却无座位)、
--       航段衔接触发器 1 处(第 3 段与第 2 段不衔接)、
--       CHECK 2 处(座位号格式 12Z、航段同场起降)、
--       部分唯一索引 1 处(同一航班同一座位被两个未取消订座占用)、
--       外键 RESTRICT 1 处(删除还有订座的航班);
--   · 座位冲突用"部分唯一索引"(WHERE segment_status <> '已取消')而不是普通唯一约束,
--     这样已取消的订座不会永久占住座位 —— 示例里 5A 正是取消后又被卖出去的。
--   · 提醒: 建表阶段 \set ON_ERROR_STOP on, 反例阶段才关掉;
--     航段衔接检查推迟到提交时, 所以航段数据写在同一个事务里(步骤 6)。
-- ============================================================
