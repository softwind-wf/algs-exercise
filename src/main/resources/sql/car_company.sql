-- ============================================================
-- 实践习题 7.21 汽车公司数据库 —— E-R 图落成 PostgreSQL 数据库 + 一组约束
-- 数据库: car_company_demo (PostgreSQL 17.11)
-- 教材: 《数据库系统概念》第 7 章 数据库设计和 E-R 模型 · 实践习题 7.21
-- 配套图: db-homework/car-company-er.html (E-R 图 + 关系模式 + 约束清单)
-- ------------------------------------------------------------
-- 业务: 公司管品牌/车型/选装件, 经销商管库存与客户档案, 销售员接单卖车。
--   · 每辆车由 VIN 唯一标识, 且是"某品牌下某车型"的一辆具体车;
--   · 每个车型可以有一组可选装件, 一辆车可能只装了其中一些或一件都没装;
--   · 订单有两类: 现货订单(指名一辆现车 VIN) 与 订制订单(按车型下单并挑选装件,
--     出厂前还没有 VIN) —— 题目"协助销售人员订购车辆"两种情形都要能表达。
--
-- 表名不加前缀: 本脚本建在独立库 car_company_demo 里。
--
-- 运行:
--   .\psql.bat "DROP DATABASE IF EXISTS car_company_demo WITH (FORCE)"
--   .\psql.bat "CREATE DATABASE car_company_demo"
--   .\psql.bat -d car_company_demo -f src\main\resources\sql\car_company.sql
--
-- 实测结论(PostgreSQL 17.11): 见文件末尾。
-- ============================================================

SET client_encoding = 'UTF8';      -- 脚本是 UTF-8, 避免中文入库乱码

-- 建表/建约束阶段"遇错即止": 任何 DDL 失败立刻停下, 免得函数没建成、
-- 触发器静默缺失这类问题被后面的语句盖过去。
\set ON_ERROR_STOP on


-- ============================================================
-- 步骤 0：清理旧对象
-- ============================================================
DROP VIEW IF EXISTS v_dealer_stock;
DROP VIEW IF EXISTS v_order_full;
DROP VIEW IF EXISTS v_car_full;
DROP VIEW IF EXISTS v_model_option;
DROP TABLE IF EXISTS order_option, car_option, model_option CASCADE;
DROP TABLE IF EXISTS orders, car, salesperson, customer, dealer, option, model, brand CASCADE;
DROP FUNCTION IF EXISTS assert_car_option_allowed() CASCADE;
DROP FUNCTION IF EXISTS assert_order_option_allowed() CASCADE;
DROP FUNCTION IF EXISTS assert_order_car_model_match() CASCADE;

SELECT '步骤 0 完成：旧对象已清理' AS section;


-- ============================================================
-- 步骤 1：品牌 / 车型 / 选装件（产品目录三张表）
-- ============================================================
CREATE TABLE brand (
    brand_name VARCHAR(40) NOT NULL,                  -- 主码: 品牌名
    country    VARCHAR(40)     NULL,
    CONSTRAINT pk_brand PRIMARY KEY (brand_name)
);

CREATE TABLE model (
    model_id   INTEGER      NOT NULL,                 -- 主码: 车型号
    brand_name VARCHAR(40)  NOT NULL,                 -- 所属品牌(外码)
    model_name VARCHAR(60)  NOT NULL,                 -- 车型名, 如 XF / F-PACE
    model_year SMALLINT     NOT NULL,                 -- 年款
    msrp       NUMERIC(10,2) NOT NULL,                -- 厂商建议零售价
    CONSTRAINT pk_model PRIMARY KEY (model_id),
    CONSTRAINT uq_model_name UNIQUE (brand_name, model_name, model_year),
    CONSTRAINT fk_model_brand FOREIGN KEY (brand_name)
        REFERENCES brand (brand_name) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_model_year CHECK (model_year BETWEEN 1950 AND 2100),
    CONSTRAINT ck_model_msrp CHECK (msrp > 0)
);

CREATE TABLE option (                                 -- 选装件(option 在 PG 里非保留字)
    option_id    INTEGER      NOT NULL,               -- 主码: 选装件号
    option_name  VARCHAR(60)  NOT NULL,               -- 名称, 如 全景天窗
    option_group VARCHAR(20)  NOT NULL,               -- 类别: 内饰/外观/科技
    price        NUMERIC(8,2) NOT NULL,               -- 选装件价格
    CONSTRAINT pk_option PRIMARY KEY (option_id),
    CONSTRAINT uq_option_name UNIQUE (option_name),
    CONSTRAINT ck_option_group CHECK (option_group IN ('内饰','外观','科技')),
    CONSTRAINT ck_option_price CHECK (price >= 0)
);

SELECT '步骤 1 完成：brand / model / option' AS section;


-- ============================================================
-- 步骤 2：经销商 / 销售员 / 客户
--   works_for:   经销商 1:N 销售员  → salesperson.dealer_id
--   customer_of: 经销商 1:N 客户    → customer.dealer_id(客户档案由经销商维护)
-- ============================================================
CREATE TABLE dealer (
    dealer_id   VARCHAR(10)  NOT NULL,                -- 主码: 经销商编号
    dealer_name VARCHAR(80)  NOT NULL,
    address     VARCHAR(120)     NULL,
    phone       VARCHAR(20)      NULL,
    CONSTRAINT pk_dealer PRIMARY KEY (dealer_id)
);

CREATE TABLE salesperson (
    emp_id    VARCHAR(10) NOT NULL,                   -- 主码: 工号
    name      VARCHAR(40) NOT NULL,
    phone     VARCHAR(20)     NULL,
    dealer_id VARCHAR(10) NOT NULL,                   -- 所属经销商(外码)
    CONSTRAINT pk_salesperson PRIMARY KEY (emp_id),
    CONSTRAINT fk_salesperson_dealer FOREIGN KEY (dealer_id)
        REFERENCES dealer (dealer_id) ON DELETE RESTRICT
);

CREATE TABLE customer (
    customer_id INTEGER      NOT NULL,                -- 主码: 客户号
    name        VARCHAR(40)  NOT NULL,
    address     VARCHAR(120)     NULL,
    phone       VARCHAR(20)      NULL,
    email       VARCHAR(60)      NULL,
    dealer_id   VARCHAR(10)  NOT NULL,                -- 客户档案归属经销商(外码)
    CONSTRAINT pk_customer PRIMARY KEY (customer_id),
    CONSTRAINT uq_customer_email UNIQUE (email),
    CONSTRAINT fk_customer_dealer FOREIGN KEY (dealer_id)
        REFERENCES dealer (dealer_id) ON DELETE RESTRICT
);

SELECT '步骤 2 完成：dealer / salesperson / customer' AS section;


-- ============================================================
-- 步骤 3：车辆（VIN 是主码; 归属车型与经销商）
--   VIN 规则: 17 位, 不含字母 I、O、Q(容易被误读) —— 用正则 CHECK 钉死
-- ============================================================
CREATE TABLE car (
    vin              VARCHAR(17) NOT NULL,            -- 主码: 车辆识别代号
    model_id         INTEGER     NOT NULL,            -- 车型(外码)
    dealer_id        VARCHAR(10) NOT NULL,            -- 所在经销商(外码)
    color            VARCHAR(20)     NULL,
    manufacture_date DATE        NOT NULL,            -- 出厂日期
    status           VARCHAR(10) NOT NULL,            -- 在库/在途/已售
    CONSTRAINT pk_car PRIMARY KEY (vin),
    CONSTRAINT fk_car_model FOREIGN KEY (model_id)
        REFERENCES model (model_id) ON DELETE RESTRICT,
    CONSTRAINT fk_car_dealer FOREIGN KEY (dealer_id)
        REFERENCES dealer (dealer_id) ON DELETE RESTRICT,
    CONSTRAINT ck_car_vin CHECK (vin ~ '^[A-HJ-NPR-Z0-9]{17}$'),
    CONSTRAINT ck_car_status CHECK (status IN ('在库','在途','已售'))
);

SELECT '步骤 3 完成：car（VIN 主码 + 格式 CHECK）' AS section;


-- ============================================================
-- 步骤 4：订单（两种类型）+ 三张 M:N 联系表
--   现货订单: order_type='现货', vin 非空, 车是现成的
--   订制订单: order_type='订制', vin 为空, 按 model_id 下单, order_option 选装件
--   CHECK 把"两种订单各自必须完整"钉死; 跨表的一致性(车型是否对得上、
--   选项是否属于该车型)留给步骤 5 的触发器。
-- ============================================================
CREATE TABLE orders (
    order_no    VARCHAR(12)   NOT NULL,               -- 主码: 订单号
    order_date  DATE          NOT NULL,
    order_type  VARCHAR(6)    NOT NULL,               -- 现货 / 订制
    customer_id INTEGER       NOT NULL,               -- 下单顾客(外码)
    emp_id      VARCHAR(10)   NOT NULL,               -- 接单销售员(外码)
    vin         VARCHAR(17)       NULL,               -- 现货订单的车(外码, 可空)
    model_id    INTEGER       NOT NULL,               -- 车型(外码)
    total_price NUMERIC(12,2) NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (order_no),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id)
        REFERENCES customer (customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_salesperson FOREIGN KEY (emp_id)
        REFERENCES salesperson (emp_id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_car FOREIGN KEY (vin)
        REFERENCES car (vin) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_model FOREIGN KEY (model_id)
        REFERENCES model (model_id) ON DELETE RESTRICT,
    CONSTRAINT ck_orders_type CHECK (order_type IN ('现货','订制')),
    CONSTRAINT ck_orders_complete CHECK (
        (order_type = '现货' AND vin IS NOT NULL)
     OR (order_type = '订制' AND vin IS NULL)),
    CONSTRAINT ck_orders_price CHECK (total_price >= 0)
);
COMMENT ON COLUMN orders.vin IS '只有现货订单才有 VIN; 订制订单出厂前还没有车';

-- M:N 联系表 1: 车型 ↔ 选装件(该车型可以装哪些)
CREATE TABLE model_option (
    model_id  INTEGER NOT NULL,
    option_id INTEGER NOT NULL,
    CONSTRAINT pk_model_option PRIMARY KEY (model_id, option_id),
    CONSTRAINT fk_mo_model FOREIGN KEY (model_id)
        REFERENCES model (model_id) ON DELETE RESTRICT,
    CONSTRAINT fk_mo_option FOREIGN KEY (option_id)
        REFERENCES option (option_id) ON DELETE RESTRICT
);

-- M:N 联系表 2: 车辆 ↔ 选装件(这辆车实际装了哪些) —— 可以是 0 个
CREATE TABLE car_option (
    vin       VARCHAR(17) NOT NULL,
    option_id INTEGER     NOT NULL,
    CONSTRAINT pk_car_option PRIMARY KEY (vin, option_id),
    CONSTRAINT fk_co_car FOREIGN KEY (vin)
        REFERENCES car (vin) ON DELETE RESTRICT,
    CONSTRAINT fk_co_option FOREIGN KEY (option_id)
        REFERENCES option (option_id) ON DELETE RESTRICT
);

-- M:N 联系表 3: 订制订单 ↔ 选装件(客户挑了哪些) —— 同样可以为 0 个
CREATE TABLE order_option (
    order_no  VARCHAR(12) NOT NULL,
    option_id INTEGER     NOT NULL,
    CONSTRAINT pk_order_option PRIMARY KEY (order_no, option_id),
    CONSTRAINT fk_oo_order FOREIGN KEY (order_no)
        REFERENCES orders (order_no) ON DELETE RESTRICT,
    CONSTRAINT fk_oo_option FOREIGN KEY (option_id)
        REFERENCES option (option_id) ON DELETE RESTRICT
);

SELECT '步骤 4 完成：orders + model_option / car_option / order_option' AS section;


-- ============================================================
-- 步骤 5：跨表语义约束(触发器) —— 题目的"一组约束"里最见功夫的部分
--   5.1 car_option ⊆ model_option     车装的必须是该车型的可选项
--   5.2 order_option ⊆ model_option   订制订单选的必须是该车型的可选项
--   5.3 order_option 只属于订制订单   现货订单的车是现成的, 选装件看 car_option
--   5.4 现货订单车型一致              订单上的 model_id 必须等于该 VIN 的车型
-- ============================================================
CREATE OR REPLACE FUNCTION assert_car_option_allowed() RETURNS trigger AS $$
DECLARE
    n integer;
BEGIN
    SELECT count(*) INTO n
    FROM car c JOIN model_option mo ON mo.model_id = c.model_id
    WHERE c.vin = NEW.vin AND mo.option_id = NEW.option_id;
    IF n = 0 THEN
        RAISE EXCEPTION '选装件不属于该车型的可选项: VIN % 装的选项 % 不在其车型的可选清单里',
            NEW.vin, NEW.option_id USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_car_option_allowed
    BEFORE INSERT OR UPDATE ON car_option
    FOR EACH ROW EXECUTE FUNCTION assert_car_option_allowed();

CREATE OR REPLACE FUNCTION assert_order_option_allowed() RETURNS trigger AS $$
DECLARE
    v_type  varchar(6);
    v_model integer;
    n       integer;
BEGIN
    SELECT order_type, model_id INTO v_type, v_model FROM orders WHERE order_no = NEW.order_no;
    IF v_type IS NULL THEN
        RETURN NEW;                                   -- 订单不存在的报错交给外键
    END IF;
    IF v_type <> '订制' THEN
        RAISE EXCEPTION '现货订单不能带选装件: 订单 % 是现货订单(选装件以 car_option 为准)', NEW.order_no
            USING ERRCODE = 'check_violation';
    END IF;
    SELECT count(*) INTO n FROM model_option
    WHERE model_id = v_model AND option_id = NEW.option_id;
    IF n = 0 THEN
        RAISE EXCEPTION '选装件不属于订单车型的可选项: 订单 %(车型 %) 没有选项 %',
            NEW.order_no, v_model, NEW.option_id USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_order_option_allowed
    BEFORE INSERT OR UPDATE ON order_option
    FOR EACH ROW EXECUTE FUNCTION assert_order_option_allowed();

CREATE OR REPLACE FUNCTION assert_order_car_model_match() RETURNS trigger AS $$
DECLARE
    v_model integer;
BEGIN
    IF NEW.vin IS NULL THEN
        RETURN NEW;
    END IF;
    SELECT model_id INTO v_model FROM car WHERE vin = NEW.vin;
    IF v_model IS NOT NULL AND v_model <> NEW.model_id THEN
        RAISE EXCEPTION '现货订单车型不一致: VIN % 属于车型 %, 订单却写了车型 %',
            NEW.vin, v_model, NEW.model_id USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_order_car_model_match
    BEFORE INSERT OR UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION assert_order_car_model_match();

SELECT '步骤 5 完成：4 条跨表约束触发器' AS section;


-- ============================================================
-- 步骤 6：示例数据
-- ============================================================
BEGIN;

INSERT INTO brand (brand_name, country) VALUES
 ('捷豹', '英国'), ('路虎', '英国'), ('大众', '德国');

INSERT INTO model (model_id, brand_name, model_name, model_year, msrp) VALUES
 (1, '捷豹', 'XF',       2024, 458000.00),
 (2, '捷豹', 'F-PACE',   2024, 628000.00),
 (3, '路虎', '揽胜极光', 2024, 428000.00),
 (4, '大众', '帕萨特',   2024, 189000.00);

INSERT INTO option (option_id, option_name, option_group, price) VALUES
 (1, '真皮座椅',     '内饰', 12000.00),
 (2, '全景天窗',     '外观',  8000.00),
 (3, '自适应巡航',   '科技',  6500.00),
 (4, '哈曼卡顿音响', '科技',  9000.00),
 (5, '20英寸轮毂',   '外观', 15000.00),
 (6, '座椅加热',     '内饰',  4000.00);

INSERT INTO dealer (dealer_id, dealer_name, address, phone) VALUES
 ('D001', '北京朝阳捷豹路虎中心', '北京市朝阳区', '010-66660001'),
 ('D002', '上海浦东捷豹路虎中心', '上海市浦东新区', '021-66660002'),
 ('D003', '广州天河大众4S店',     '广州市天河区', '020-66660003');

INSERT INTO salesperson (emp_id, name, phone, dealer_id) VALUES
 ('S001', '陈明', '13900000001', 'D001'),
 ('S002', '刘洋', '13900000002', 'D002'),
 ('S003', '周杰', '13900000003', 'D003');

INSERT INTO customer (customer_id, name, address, phone, email, dealer_id) VALUES
 (1, '张伟', '北京市海淀区中关村大街1号', '13800000001', 'zhangwei@example.com', 'D001'),
 (2, '李娜', '上海市浦东新区世纪大道100号', '13800000002', 'lina@example.com', 'D002'),
 (3, '王强', '广州市天河区体育西路50号',   '13800000003', 'wangqiang@example.com', 'D003'),
 (4, '赵敏', '北京市朝阳区建国路88号',     '13800000004', 'zhaomin@example.com', 'D001');

INSERT INTO car (vin, model_id, dealer_id, color, manufacture_date, status) VALUES
 ('LSVAM4185J2000001', 4, 'D003', '白色', DATE '2024-03-10', '在库'),
 ('LSVAM4185J2000002', 4, 'D003', '黑色', DATE '2024-05-02', '在库'),
 ('SAJAA5BXK10000003', 1, 'D001', '红色', DATE '2024-04-15', '在库'),
 ('SAJAA5BXK10000004', 1, 'D001', '白色', DATE '2024-06-20', '在库'),
 ('SADCA2BXK20000005', 2, 'D001', '黑色', DATE '2024-07-01', '在库'),
 ('SALVA2BXK30000006', 3, 'D002', '灰色', DATE '2024-02-18', '在库'),
 ('SALVA2BXK30000007', 3, 'D002', '蓝色', DATE '2024-08-09', '已售'),
 ('SAJAA5BXK10000008', 1, 'D002', '银色', DATE '2024-09-12', '在途');

-- 该车型可以装哪些选装件
INSERT INTO model_option (model_id, option_id) VALUES
 (1,1),(1,2),(1,3),(1,5),            -- XF
 (2,1),(2,2),(2,3),(2,4),(2,5),      -- F-PACE
 (3,1),(3,2),(3,6),                  -- 揽胜极光
 (4,1),(4,3),(4,6);                  -- 帕萨特(注意: 没有全景天窗)

-- 这辆车实际装了哪些(第一辆、第四辆、第八辆一件都没装 —— 题目说的"没有可用选项")
INSERT INTO car_option (vin, option_id) VALUES
 ('LSVAM4185J2000002', 1), ('LSVAM4185J2000002', 6),
 ('SAJAA5BXK10000003', 1), ('SAJAA5BXK10000003', 2), ('SAJAA5BXK10000003', 5),
 ('SADCA2BXK20000005', 1), ('SADCA2BXK20000005', 2), ('SADCA2BXK20000005', 4),
 ('SALVA2BXK30000006', 1), ('SALVA2BXK30000006', 6),
 ('SALVA2BXK30000007', 2);

INSERT INTO orders (order_no, order_date, order_type, customer_id, emp_id, vin, model_id, total_price) VALUES
 ('O2024001', DATE '2024-07-20', '现货', 1, 'S001', 'SAJAA5BXK10000003', 1, 493000.00),
 ('O2024002', DATE '2024-08-15', '现货', 2, 'S002', 'SALVA2BXK30000007', 3, 436000.00),
 ('O2024003', DATE '2024-09-01', '订制', 3, 'S003', NULL, 4, 205000.00),
 ('O2024004', DATE '2024-09-05', '订制', 4, 'S001', NULL, 2, 628000.00),
 ('O2024005', DATE '2024-09-18', '订制', 1, 'S001', NULL, 1, 464500.00),
 ('O2024006', DATE '2024-09-25', '现货', 4, 'S001', 'LSVAM4185J2000001', 4, 189000.00);

-- 订制订单挑了哪些(订单 4 一件都没挑)
INSERT INTO order_option (order_no, option_id) VALUES
 ('O2024003', 1), ('O2024003', 6),
 ('O2024005', 3);

COMMIT;

SELECT '步骤 6 完成：示例数据已提交' AS section;


-- ============================================================
-- 步骤 7：视图
-- ============================================================
CREATE OR REPLACE VIEW v_model_option AS
SELECT m.model_id, b.brand_name, m.model_name, m.model_year, m.msrp,
       count(mo.option_id) AS option_count,
       string_agg(o.option_name, ', ' ORDER BY o.option_id) AS option_list
FROM model m
JOIN brand b ON b.brand_name = m.brand_name
LEFT JOIN model_option mo ON mo.model_id = m.model_id
LEFT JOIN option o ON o.option_id = mo.option_id
GROUP BY m.model_id, b.brand_name, m.model_name, m.model_year, m.msrp;

CREATE OR REPLACE VIEW v_car_full AS
SELECT c.vin, b.brand_name, m.model_name, c.color, c.status, d.dealer_name,
       count(co.option_id) AS installed_count,
       COALESCE(string_agg(o.option_name, ', ' ORDER BY o.option_id), '(无选装件)') AS installed_list,
       m.msrp + COALESCE(sum(o.price), 0) AS car_price
FROM car c
JOIN model m ON m.model_id = c.model_id
JOIN brand b ON b.brand_name = m.brand_name
JOIN dealer d ON d.dealer_id = c.dealer_id
LEFT JOIN car_option co ON co.vin = c.vin
LEFT JOIN option o ON o.option_id = co.option_id
GROUP BY c.vin, b.brand_name, m.model_name, c.color, c.status, d.dealer_name, m.msrp;

CREATE OR REPLACE VIEW v_order_full AS
SELECT o.order_no, o.order_date, o.order_type, cu.name AS customer_name,
       sp.name AS salesperson_name, d.dealer_name,
       o.vin, b.brand_name, m.model_name, o.total_price,
       COALESCE(string_agg(oo.option_id::text, ', ' ORDER BY oo.option_id), '(未选)') AS order_options
FROM orders o
JOIN customer cu ON cu.customer_id = o.customer_id
JOIN salesperson sp ON sp.emp_id = o.emp_id
JOIN dealer d ON d.dealer_id = sp.dealer_id
JOIN model m ON m.model_id = o.model_id
JOIN brand b ON b.brand_name = m.brand_name
LEFT JOIN order_option oo ON oo.order_no = o.order_no
GROUP BY o.order_no, o.order_date, o.order_type, cu.name, sp.name, d.dealer_name,
         o.vin, b.brand_name, m.model_name, o.total_price;

CREATE OR REPLACE VIEW v_dealer_stock AS
SELECT d.dealer_id, d.dealer_name,
       count(*) FILTER (WHERE c.status = '在库') AS 在库车辆数,
       COALESCE(sum(m.msrp) FILTER (WHERE c.status = '在库'), 0) AS 在库车辆金额
FROM dealer d
LEFT JOIN car c ON c.dealer_id = d.dealer_id
LEFT JOIN model m ON m.model_id = c.model_id
GROUP BY d.dealer_id, d.dealer_name;

SELECT '步骤 7 完成：4 个视图' AS section;


-- ============================================================
-- 步骤 8：验证查询
-- ============================================================
SELECT '8.1 行数统计' AS section;
SELECT 'brand' AS 表, count(*) AS 行数 FROM brand
UNION ALL SELECT 'model',        count(*) FROM model
UNION ALL SELECT 'option',       count(*) FROM option
UNION ALL SELECT 'dealer',       count(*) FROM dealer
UNION ALL SELECT 'salesperson',  count(*) FROM salesperson
UNION ALL SELECT 'customer',     count(*) FROM customer
UNION ALL SELECT 'car',          count(*) FROM car
UNION ALL SELECT 'orders',       count(*) FROM orders
UNION ALL SELECT 'model_option', count(*) FROM model_option
UNION ALL SELECT 'car_option',   count(*) FROM car_option
UNION ALL SELECT 'order_option', count(*) FROM order_option
ORDER BY 1;

SELECT '8.2 车型与可选装件' AS section;
SELECT model_id, brand_name, model_name, msrp, option_count, option_list
FROM v_model_option ORDER BY model_id;

SELECT '8.3 车辆全景(含"一件选装件都没装"的车)' AS section;
SELECT vin, brand_name, model_name, color, status, dealer_name, installed_count, installed_list, car_price
FROM v_car_full ORDER BY vin;

SELECT '8.4 订单全景(现货 vs 订制)' AS section;
SELECT order_no, order_date, order_type, customer_name, salesperson_name, vin, brand_name, model_name, order_options, total_price
FROM v_order_full ORDER BY order_no;

SELECT '8.5 经销商库存' AS section;
SELECT * FROM v_dealer_stock ORDER BY dealer_id;

SELECT '8.6 子集约束自检(应为 0 行: 有没有车装了本车型没有的件)' AS section;
SELECT co.vin, co.option_id
FROM car_option co
JOIN car c ON c.vin = co.vin
WHERE NOT EXISTS (SELECT 1 FROM model_option mo
                  WHERE mo.model_id = c.model_id AND mo.option_id = co.option_id);

SELECT '8.7 订单选装件自检(应为 0 行)' AS section;
SELECT oo.order_no, oo.option_id
FROM order_option oo
JOIN orders o ON o.order_no = oo.order_no
WHERE o.order_type <> '订制'
   OR NOT EXISTS (SELECT 1 FROM model_option mo
                  WHERE mo.model_id = o.model_id AND mo.option_id = oo.option_id);


-- ============================================================
-- 步骤 9：反例测试 —— 预期报错(共 10 处)
-- ============================================================
\set ON_ERROR_STOP off

SELECT '9.1 预期报错：帕萨特(车型4)没有全景天窗, 却给它装一个' AS section;
INSERT INTO car_option (vin, option_id) VALUES ('LSVAM4185J2000001', 2);

SELECT '9.2 预期报错：现货订单不能带选装件' AS section;
INSERT INTO order_option (order_no, option_id) VALUES ('O2024001', 3);

SELECT '9.3 预期报错：订制订单选了该车型没有的选装件(车型4没有全景天窗)' AS section;
INSERT INTO order_option (order_no, option_id) VALUES ('O2024003', 2);

SELECT '9.4 预期报错：现货订单的车型与 VIN 实际车型不符' AS section;
INSERT INTO orders (order_no, order_date, order_type, customer_id, emp_id, vin, model_id, total_price)
VALUES ('O2024099', DATE '2024-10-01', '现货', 1, 'S001', 'LSVAM4185J2000001', 1, 189000.00);

SELECT '9.5 预期报错：VIN 含字母 I(且长度不足), 被正则 CHECK 拒绝' AS section;
INSERT INTO car (vin, model_id, dealer_id, color, manufacture_date, status)
VALUES ('LSVAMI185J2000001', 4, 'D003', '灰色', DATE '2024-10-02', '在库');

SELECT '9.6 预期报错：订单类型填了 ''二手''(实测先被 ck_orders_complete 拦下)' AS section;
INSERT INTO orders (order_no, order_date, order_type, customer_id, emp_id, vin, model_id, total_price)
VALUES ('O2024098', DATE '2024-10-03', '二手', 1, 'S001', NULL, 1, 100000.00);

SELECT '9.7 预期报错：订制订单却填了 VIN(完整性 CHECK)' AS section;
INSERT INTO orders (order_no, order_date, order_type, customer_id, emp_id, vin, model_id, total_price)
VALUES ('O2024097', DATE '2024-10-04', '订制', 1, 'S001', 'SAJAA5BXK10000004', 1, 458000.00);

SELECT '9.8 预期报错：同品牌同车型同年款重复(UNIQUE)' AS section;
INSERT INTO model (model_id, brand_name, model_name, model_year, msrp)
VALUES (9, '捷豹', 'XF', 2024, 460000.00);

SELECT '9.9 预期报错：删除还有销售员与车辆的经销商(实测先报 fk_salesperson_dealer)' AS section;
DELETE FROM dealer WHERE dealer_id = 'D001';

SELECT '9.10 预期报错：订制订单金额为负' AS section;
INSERT INTO orders (order_no, order_date, order_type, customer_id, emp_id, vin, model_id, total_price)
VALUES ('O2024096', DATE '2024-10-05', '订制', 1, 'S001', NULL, 2, -1.00);

SELECT '步骤 9 完成：以上 10 处报错都是预期行为' AS section;


-- ============================================================
-- 步骤 10：收尾
-- ============================================================
SELECT '脚本执行完毕：11 张表 + 4 个视图 + 4 枚约束触发器 + 主码/外码/CHECK 已就绪' AS section;

-- 清空(顺序不能乱):
--   DROP VIEW  v_dealer_stock, v_order_full, v_car_full, v_model_option;
--   DROP TABLE order_option, car_option, model_option, orders, car,
--              salesperson, customer, dealer, option, model, brand CASCADE;
--   DROP FUNCTION assert_car_option_allowed(), assert_order_option_allowed(),
--                assert_order_car_model_match() CASCADE;
-- 或整库删掉: .\psql.bat "DROP DATABASE IF EXISTS car_company_demo WITH (FORCE)"


-- ============================================================
-- 实测结论(PostgreSQL 17.11, 库 car_company_demo, 实跑 0 警告 / 10 条预期报错)：
--   · 11 张表 + 4 个视图 + 4 枚约束触发器; 同库连跑两遍结果一致(可重复执行);
--   · 行数: brand 3 / model 4 / option 6 / dealer 3 / salesperson 3 / customer 4 /
--           car 8 / orders 6 / model_option 15 / car_option 11 / order_option 3;
--   · "一辆车可能只有一些(或没有)可用选项"落到实处(见 8.3): 8 辆车中
--     LSVAM4185J2000001、SAJAA5BXK10000004、SAJAA5BXK10000008 三辆 installed_count = 0,
--     其余 1~3 件; 车价 = 车型 msrp + 该车选装件价(如 F-PACE 628000 + 29000 = 657000);
--   · 两种订单一目了然(8.4): 现货订单带 VIN(O2024001/O2024002/O2024006, 与车、经销商、
--     库存状态能对上), 订制订单 VIN 为空、按车型下单(O2024003 帕萨特 + 选装 1,6;
--     O2024005 XF + 选装 3; O2024004 一件都没选);
--   · 经销商库存(8.5): D001 在库 3 辆 1544000.00 / D002 1 辆 428000.00 / D003 2 辆 378000.00;
--   · 8.6、8.7 两条子集自检(车装的件是否都在车型可选清单里、订单选装件是否合法)均为 0 行;
--   · 10 处报错一处不多一处不少地被拦下:
--       选装件子集触发器 3 处(车装了车型没有的件、现货订单带选装件、订制订单选了车型没有的件)、
--       订单车型一致性触发器 1 处、VIN 正则 CHECK 1 处、订单完整性 CHECK 2 处
--       (类型填了"二手"、订制却填了 VIN)、UNIQUE 1 处(同品牌同车型同年款)、
--       外键 RESTRICT 1 处(删除还有销售员与车辆的经销商)、金额 CHECK 1 处(总价为负);
--   · 提醒: 建表阶段 \set ON_ERROR_STOP on, 反例阶段才关掉;
--     选装件类约束跨三张表(model_option / car_option / order_option), CHECK 表达不了,
--     只能用触发器(步骤 5)—— 这是本题"一组约束"里最见功夫的部分。
-- ============================================================
