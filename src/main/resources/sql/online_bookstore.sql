-- ============================================================
-- 实践习题 7.20 网上书店 —— E-R 图(图 7-29)三步扩展的 PostgreSQL 落地
-- 数据库: bookstore_demo (PostgreSQL 17.11)
-- 教材: 《数据库系统概念》第 7 章 数据库设计和 E-R 模型 · 实践习题 7.20
-- 配套图: db-homework/bookstore-er.html(含 a / b / c 三张图与三张表)
-- ------------------------------------------------------------
-- 本脚本落地的是题 c 的最终设计(概化版本):
--   product 是超类, book / blu_ray / downloadable_video 是三个子类;
--   contains(购物篮 M:N)与 stocks(仓库 M:N)挂在超类上, 于是购物篮能装任意组合。
--
-- 概化的两个约束(题 c 的考点, 也是 SQL 里最能体现功夫的地方):
--   全概化 total     —— 每个商品至少属于一种格式; 外键表达不了, 用可延迟约束触发器;
--   重叠 overlapping —— 同一商品可同时是蓝光 + 可下载视频(两个格式、两个价格);
--                       因此只能"超类表 + 各子类表", 不能用"单表 + 判别列 + CHECK"。
--
-- 另外补了一刀(题 c 没要求, 但不补会丢信息): 同一商品有两种格式时,
--   购物篮里必须记住买的是哪种格式, 所以给 contains 加了联系属性 format,
--   并用触发器保证该格式对这个商品确实存在。
--
-- 表名不加前缀: 本脚本建在独立库 bookstore_demo 里(仓库里跑在 university 库的
--   那些 er_ 前缀脚本是为了与旧表区分, 与本脚本无关)。
--
-- 运行:
--   .\psql.bat "DROP DATABASE IF EXISTS bookstore_demo WITH (FORCE)"
--   .\psql.bat "CREATE DATABASE bookstore_demo"
--   .\psql.bat -d bookstore_demo -f src\main\resources\sql\online_bookstore.sql
--
-- 实测结论(PostgreSQL 17.11): 见文件末尾。
-- ============================================================

SET client_encoding = 'UTF8';      -- 脚本是 UTF-8, 避免中文入库乱码

-- 建表/建约束阶段"遇错即止": 任何 DDL 失败立刻停下,
-- 免得函数没建成、触发器静默缺失这类问题被后面的语句盖过去。
\set ON_ERROR_STOP on


-- ============================================================
-- 步骤 0：清理旧对象(保证脚本可重复运行)
-- ============================================================
DROP VIEW IF EXISTS v_stock_overview;
DROP VIEW IF EXISTS v_basket_detail;
DROP VIEW IF EXISTS v_product_full;
DROP TABLE IF EXISTS stocks, contains, written_by CASCADE;
DROP TABLE IF EXISTS downloadable_video, blu_ray, book, product CASCADE;
DROP TABLE IF EXISTS shopping_basket, customer, warehouse, publisher, author CASCADE;
DROP FUNCTION IF EXISTS assert_contains_format_exists() CASCADE;
DROP FUNCTION IF EXISTS assert_product_has_format() CASCADE;

SELECT '步骤 0 完成：旧对象已清理' AS section;


-- ============================================================
-- 步骤 1：独立实体集 author / publisher / warehouse / customer
--   主码按图 7-29 所给属性就近选取(name / code / email)
-- ============================================================
CREATE TABLE author (
    author_name VARCHAR(40)  NOT NULL,                -- 主码: 作者名
    address     VARCHAR(120)     NULL,
    url         VARCHAR(120)     NULL,
    CONSTRAINT pk_author PRIMARY KEY (author_name)
);

CREATE TABLE publisher (
    pub_name VARCHAR(60)  NOT NULL,                   -- 主码: 出版社名
    address  VARCHAR(120)     NULL,
    phone    VARCHAR(20)      NULL,
    url      VARCHAR(120)     NULL,
    CONSTRAINT pk_publisher PRIMARY KEY (pub_name)
);

CREATE TABLE warehouse (
    code    VARCHAR(10)  NOT NULL,                    -- 主码: 仓库编号
    address VARCHAR(120)     NULL,
    phone   VARCHAR(20)      NULL,
    CONSTRAINT pk_warehouse PRIMARY KEY (code)
);

CREATE TABLE customer (
    email   VARCHAR(60)  NOT NULL,                    -- 主码: 电子邮箱
    name    VARCHAR(40)  NOT NULL,
    address VARCHAR(120)     NULL,
    phone   VARCHAR(20)      NULL,
    CONSTRAINT pk_customer PRIMARY KEY (email),
    CONSTRAINT ck_customer_email CHECK (email LIKE '%_@_%')
);

-- basket_of 是 1:N(一个顾客有若干个购物篮, 一个篮只属于一个顾客): 把 1 端主码并入 N 端
CREATE TABLE shopping_basket (
    basket_id INTEGER     NOT NULL,                   -- 主码: 购物篮号
    email     VARCHAR(60) NOT NULL,                   -- 顾客(外键)
    CONSTRAINT pk_basket PRIMARY KEY (basket_id),
    CONSTRAINT fk_basket_customer FOREIGN KEY (email)
        REFERENCES customer (email) ON DELETE RESTRICT ON UPDATE CASCADE
);

COMMENT ON TABLE shopping_basket IS '实体集 购物篮; email 外键实现 basket_of(顾客 1:N 购物篮)';

SELECT '步骤 1 完成：author / publisher / warehouse / customer / shopping_basket' AS section;


-- ============================================================
-- 步骤 2：概化 —— 超类 product + 三个子类
--   题 b 的"同一商品可能只有一种格式、也可能两种格式都有"就是重叠(overlapping):
--   所以 product_id 在每张子类表里是 UNIQUE(每种格式最多一条), 但同一个
--   product_id 可以同时出现在多张子类表里(蓝光 + 可下载视频)。
--   price 只能放在子类上: 同一商品两种格式 = 两个不同价格。
-- ============================================================
CREATE TABLE product (
    product_id INTEGER      NOT NULL,                 -- 主码: 商品号(超类主码)
    title      VARCHAR(120) NOT NULL,                 -- 商品名(各格式共用)
    year       SMALLINT         NULL,                 -- 发行/出版年份
    CONSTRAINT pk_product PRIMARY KEY (product_id),
    CONSTRAINT ck_product_year CHECK (year IS NULL OR year BETWEEN 1400 AND 2100)
);
COMMENT ON TABLE product IS '概化超类: 商品本身(书名/片名同一件东西)';

-- 子类 1: 图书. published_by 是 1:N, 出版社主码并入 book
CREATE TABLE book (
    isbn       VARCHAR(20)  NOT NULL,                 -- 主码: ISBN
    product_id INTEGER      NOT NULL,                 -- 子类 → 超类
    pub_name   VARCHAR(60)  NOT NULL,                 -- 出版社(1:N 并入)
    price      NUMERIC(8,2) NOT NULL,                 -- 图书格式的价格
    CONSTRAINT pk_book PRIMARY KEY (isbn),
    CONSTRAINT uq_book_product UNIQUE (product_id),   -- 一个商品最多一个图书版本
    CONSTRAINT fk_book_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE RESTRICT,
    CONSTRAINT fk_book_publisher FOREIGN KEY (pub_name)
        REFERENCES publisher (pub_name) ON DELETE RESTRICT,
    CONSTRAINT ck_book_price CHECK (price > 0)
);

-- 子类 2: 蓝光光盘
CREATE TABLE blu_ray (
    upc        VARCHAR(20)  NOT NULL,                 -- 主码: 条码
    product_id INTEGER      NOT NULL,
    price      NUMERIC(8,2) NOT NULL,
    CONSTRAINT pk_blu_ray PRIMARY KEY (upc),
    CONSTRAINT uq_blu_ray_product UNIQUE (product_id),
    CONSTRAINT fk_blu_ray_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE RESTRICT,
    CONSTRAINT ck_blu_ray_price CHECK (price > 0)
);

-- 子类 3: 可下载视频
CREATE TABLE downloadable_video (
    video_id     VARCHAR(20)  NOT NULL,               -- 主码: 视频编号
    product_id   INTEGER      NOT NULL,
    price        NUMERIC(8,2) NOT NULL,
    duration_min SMALLINT         NULL,
    CONSTRAINT pk_video PRIMARY KEY (video_id),
    CONSTRAINT uq_video_product UNIQUE (product_id),
    CONSTRAINT fk_video_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE RESTRICT,
    CONSTRAINT ck_video_price CHECK (price > 0),
    CONSTRAINT ck_video_duration CHECK (duration_min IS NULL OR duration_min > 0)
);

COMMENT ON TABLE book IS '子类: 图书版本(ISBN 是子类主码, product_id 指向超类)';
COMMENT ON COLUMN book.price IS '同一商品两种格式时各自定价, 所以 price 只能放在子类';

SELECT '步骤 2 完成：product 超类 + book / blu_ray / downloadable_video 子类' AS section;


-- ============================================================
-- 步骤 3：M:N 联系表 —— written_by / contains / stocks
--   contains 与 stocks 挂在超类 product 上, 这就是题 c 要的效果:
--   购物篮可以装"书 / 蓝光 / 可下载视频"的任意组合。
-- ============================================================
CREATE TABLE written_by (
    author_name VARCHAR(40) NOT NULL,                 -- 作者
    isbn        VARCHAR(20) NOT NULL,                 -- 图书
    CONSTRAINT pk_written_by PRIMARY KEY (author_name, isbn),
    CONSTRAINT fk_wb_author FOREIGN KEY (author_name)
        REFERENCES author (author_name) ON DELETE RESTRICT,
    CONSTRAINT fk_wb_book FOREIGN KEY (isbn) REFERENCES book (isbn) ON DELETE RESTRICT
);

CREATE TABLE contains (
    basket_id  INTEGER     NOT NULL,                  -- 购物篮
    product_id INTEGER     NOT NULL,                  -- 商品(超类)
    format     VARCHAR(20) NOT NULL,                  -- 联系属性: 买的是哪种格式
    number     SMALLINT    NOT NULL,                  -- 联系属性: 数量
    CONSTRAINT pk_contains PRIMARY KEY (basket_id, product_id, format),
    CONSTRAINT fk_ct_basket FOREIGN KEY (basket_id)
        REFERENCES shopping_basket (basket_id) ON DELETE RESTRICT,
    CONSTRAINT fk_ct_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE RESTRICT,
    CONSTRAINT ck_ct_format CHECK (format IN ('book','blu_ray','downloadable_video')),
    CONSTRAINT ck_ct_number CHECK (number > 0)
);
COMMENT ON COLUMN contains.format IS '联系属性: 同一商品有两种格式时必须指明买哪种(题 c 未画, 落地需补)';

CREATE TABLE stocks (
    code       VARCHAR(10) NOT NULL,                  -- 仓库
    product_id INTEGER     NOT NULL,                  -- 商品(超类)
    number     INTEGER     NOT NULL,                  -- 联系属性: 库存量
    CONSTRAINT pk_stocks PRIMARY KEY (code, product_id),
    CONSTRAINT fk_st_warehouse FOREIGN KEY (code)
        REFERENCES warehouse (code) ON DELETE RESTRICT,
    CONSTRAINT fk_st_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE RESTRICT,
    CONSTRAINT ck_st_number CHECK (number >= 0)
);

SELECT '步骤 3 完成：written_by / contains / stocks' AS section;


-- ============================================================
-- 步骤 4：把概化的两个约束落成 SQL
--   4.1 全概化(total): 每个商品至少一种格式 —— 三张子类表合起来算, 外键做不到,
--       用"超类插入时检查 + 子类删除/改挂时检查"的可延迟约束触发器(提交时校验)。
--       注意: 因此示例数据必须写在同一个事务里(见步骤 5 的 BEGIN/COMMIT)。
--   4.2 重叠(overlapping): 不需要额外约束 —— 不禁止一个 product_id 出现在多张子类表,
--       这正是选"超类表+子类表"方案而不是"单表+判别列"的原因(后者只能表达不相交)。
--   4.3 contains.format 必须对该商品真实存在(联系属性的语义完整性)。
-- ============================================================
CREATE OR REPLACE FUNCTION assert_product_has_format() RETURNS trigger AS $$
DECLARE
    keys integer[] := ARRAY[]::integer[];
    k    integer;
    n    integer;
    np   integer;
BEGIN
    IF TG_TABLE_NAME = 'product' THEN
        keys := keys || (to_jsonb(NEW) ->> 'product_id')::integer;
    ELSIF TG_OP IN ('DELETE', 'UPDATE') THEN          -- 三张子类表上的删除/改挂
        keys := keys || (to_jsonb(OLD) ->> 'product_id')::integer;
    END IF;

    FOREACH k IN ARRAY keys LOOP
        CONTINUE WHEN k IS NULL;
        SELECT count(*) INTO np FROM product WHERE product_id = k;
        CONTINUE WHEN np = 0;                          -- 超类行也被删掉了, 无需再保证
        SELECT (SELECT count(*) FROM book               WHERE product_id = k)
             + (SELECT count(*) FROM blu_ray            WHERE product_id = k)
             + (SELECT count(*) FROM downloadable_video WHERE product_id = k)
          INTO n;
        IF n = 0 THEN
            RAISE EXCEPTION '全概化约束被破坏: 商品 % 没有任何格式(book / blu_ray / downloadable_video)', k
                USING ERRCODE = 'check_violation';
        END IF;
    END LOOP;
    RETURN NULL;                                       -- AFTER 触发器, 返回值被忽略
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_product_needs_format
    AFTER INSERT ON product
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_product_has_format();

CREATE CONSTRAINT TRIGGER trg_book_keeps_format
    AFTER DELETE OR UPDATE ON book
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_product_has_format();

CREATE CONSTRAINT TRIGGER trg_blu_ray_keeps_format
    AFTER DELETE OR UPDATE ON blu_ray
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_product_has_format();

CREATE CONSTRAINT TRIGGER trg_video_keeps_format
    AFTER DELETE OR UPDATE ON downloadable_video
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
    EXECUTE FUNCTION assert_product_has_format();

-- 4.3 contains.format 的语义完整性: 商品必须有这种格式
CREATE OR REPLACE FUNCTION assert_contains_format_exists() RETURNS trigger AS $$
DECLARE
    n integer;
BEGIN
    EXECUTE format('SELECT count(*) FROM %I WHERE product_id = $1', NEW.format)
        INTO n USING NEW.product_id;
    IF n = 0 THEN
        RAISE EXCEPTION '格式不匹配: 商品 % 没有 % 这种格式', NEW.product_id, NEW.format
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_contains_format_exists
    BEFORE INSERT OR UPDATE ON contains
    FOR EACH ROW EXECUTE FUNCTION assert_contains_format_exists();

SELECT '步骤 4 完成：全概化触发器 4 枚 + format 完整性触发器 1 枚' AS section;


-- ============================================================
-- 步骤 5：示例数据
--   商品 6 个, 覆盖四种情况: 只有书 / 只有蓝光 / 只有视频 / 两种格式都有。
--   ⚠ 全概化检查推迟到提交时, 所以超类与子类行必须在同一个事务里插入。
-- ============================================================
BEGIN;

INSERT INTO author (author_name, address, url) VALUES
 ('王珊',   '北京市海淀区',   'https://example.org/wangshan'),
 ('萨师煊', '北京市海淀区',   'https://example.org/sashixuan'),
 ('Sedgewick', 'Princeton, NJ', 'https://algs4.cs.princeton.edu'),
 ('刘慈欣', '山西省阳泉市',   NULL);

INSERT INTO publisher (pub_name, address, phone, url) VALUES
 ('高等教育出版社', '北京市西城区', '010-58581000', 'https://www.hep.com.cn'),
 ('人民邮电出版社', '北京市丰台区', '010-81055000', 'https://www.ptpress.com.cn'),
 ('机械工业出版社', '北京市西城区', '010-88379000', 'https://www.cmpbook.com');

INSERT INTO warehouse (code, address, phone) VALUES
 ('WH01', '北京市顺义区物流园1号', '010-11110001'),
 ('WH02', '上海市青浦区物流园8号', '021-22220002');

INSERT INTO customer (email, name, address, phone) VALUES
 ('zhangwei@example.com', '张伟', '北京市海淀区中关村大街1号', '13800000001'),
 ('lina@example.com',     '李娜', '上海市浦东新区世纪大道100号', '13800000002'),
 ('wangqiang@example.com','王强', '广州市天河区体育西路50号',   '13800000003');

INSERT INTO shopping_basket (basket_id, email) VALUES
 (1, 'zhangwei@example.com'),
 (2, 'zhangwei@example.com'),
 (3, 'lina@example.com');

-- 超类: 6 个商品
INSERT INTO product (product_id, title, year) VALUES
 (1001, '数据库系统概论',              2014),
 (1002, '算法(第4版)',                 2011),
 (1003, '流浪地球',                    2019),
 (1004, '三体(蓝光收藏版)',            2023),
 (1005, '深入理解计算机系统(视频课)',  2020),
 (1006, '数据库系统概念(影印版)',      2019);

-- 子类: 图书 3 条(1001/1002/1006)
INSERT INTO book (isbn, product_id, pub_name, price) VALUES
 ('9787040406641', 1001, '高等教育出版社', 45.00),
 ('9787111528975', 1002, '人民邮电出版社', 128.00),
 ('9787111600008', 1006, '机械工业出版社', 99.00);

-- 子类: 蓝光 2 条(1003/1004) —— 1003 同时还有视频版本, 这就是"重叠"
INSERT INTO blu_ray (upc, product_id, price) VALUES
 ('6901234567001', 1003, 68.00),
 ('6901234567002', 1004, 88.00);

-- 子类: 可下载视频 3 条(1003/1005/1006) —— 1003 与蓝光重叠, 1006 与图书重叠
INSERT INTO downloadable_video (video_id, product_id, price, duration_min) VALUES
 ('V1003', 1003, 25.00, 125),
 ('V1005', 1005, 15.00, 1180),
 ('V1006', 1006, 39.00,  900);

-- M:N: 作者 — 图书
INSERT INTO written_by (author_name, isbn) VALUES
 ('王珊',      '9787040406641'),
 ('萨师煊',    '9787040406641'),
 ('Sedgewick', '9787111528975'),
 ('王珊',      '9787111600008');

-- M:N: 购物篮 — 商品(含格式与数量) —— 篮 1 同时装了书和蓝光, 篮 3 装了视频+蓝光
INSERT INTO contains (basket_id, product_id, format, number) VALUES
 (1, 1001, 'book',    1),
 (1, 1002, 'book',    2),
 (1, 1004, 'blu_ray', 1),
 (2, 1003, 'blu_ray', 1),
 (2, 1006, 'downloadable_video', 1),
 (3, 1005, 'downloadable_video', 1),
 (3, 1003, 'downloadable_video', 2);

-- M:N: 仓库 — 商品(库存量)
INSERT INTO stocks (code, product_id, number) VALUES
 ('WH01', 1001, 50), ('WH01', 1002, 30), ('WH01', 1004, 20),
 ('WH02', 1001, 10), ('WH02', 1003,  5), ('WH02', 1006, 15);

COMMIT;

SELECT '步骤 5 完成：示例数据已提交' AS section;


-- ============================================================
-- 步骤 6：视图
-- ============================================================
-- 6.1 商品全景: 一个商品有哪些格式、各自什么价(概化落地后最好用的视图)
CREATE OR REPLACE VIEW v_product_full AS
SELECT p.product_id, p.title, p.year,
       b.isbn,  b.price  AS book_price,  b.pub_name,
       r.upc,   r.price  AS blu_ray_price,
       d.video_id, d.price AS video_price, d.duration_min,
       concat_ws(' + ',
           CASE WHEN b.isbn     IS NOT NULL THEN 'book'               END,
           CASE WHEN r.upc      IS NOT NULL THEN 'blu_ray'            END,
           CASE WHEN d.video_id IS NOT NULL THEN 'downloadable_video' END) AS formats
FROM product p
LEFT JOIN book               b ON b.product_id = p.product_id
LEFT JOIN blu_ray            r ON r.product_id = p.product_id
LEFT JOIN downloadable_video d ON d.product_id = p.product_id;

-- 6.2 购物篮明细: 按联系属性 format 取对应子类的价格
CREATE OR REPLACE VIEW v_basket_detail AS
SELECT sb.basket_id, cu.email, cu.name AS customer_name,
       ct.product_id, p.title, ct.format, ct.number,
       CASE ct.format WHEN 'book'               THEN b.price
                      WHEN 'blu_ray'            THEN r.price
                      WHEN 'downloadable_video' THEN d.price END AS unit_price,
       ct.number * (CASE ct.format WHEN 'book'               THEN b.price
                                   WHEN 'blu_ray'            THEN r.price
                                   WHEN 'downloadable_video' THEN d.price END) AS subtotal
FROM contains ct
JOIN shopping_basket sb ON sb.basket_id = ct.basket_id
JOIN customer        cu ON cu.email     = sb.email
JOIN product         p  ON p.product_id = ct.product_id
LEFT JOIN book               b ON b.product_id = ct.product_id
LEFT JOIN blu_ray            r ON r.product_id = ct.product_id
LEFT JOIN downloadable_video d ON d.product_id = ct.product_id;

-- 6.3 库存总览
CREATE OR REPLACE VIEW v_stock_overview AS
SELECT w.code, w.address, p.product_id, p.title, s.number AS stock_number
FROM stocks s
JOIN warehouse w ON w.code = s.code
JOIN product   p ON p.product_id = s.product_id;

SELECT '步骤 6 完成：3 个视图' AS section;


-- ============================================================
-- 步骤 7：验证查询
-- ============================================================
SELECT '7.1 行数统计' AS section;
SELECT 'author' AS 表, count(*) AS 行数 FROM author
UNION ALL SELECT 'publisher',    count(*) FROM publisher
UNION ALL SELECT 'warehouse',    count(*) FROM warehouse
UNION ALL SELECT 'customer',     count(*) FROM customer
UNION ALL SELECT 'shopping_basket', count(*) FROM shopping_basket
UNION ALL SELECT 'product(超类)', count(*) FROM product
UNION ALL SELECT 'book(子类)',    count(*) FROM book
UNION ALL SELECT 'blu_ray(子类)', count(*) FROM blu_ray
UNION ALL SELECT 'downloadable_video(子类)', count(*) FROM downloadable_video
UNION ALL SELECT 'written_by',   count(*) FROM written_by
UNION ALL SELECT 'contains',     count(*) FROM contains
UNION ALL SELECT 'stocks',       count(*) FROM stocks
ORDER BY 1;

SELECT '7.2 商品全景(看 formats 列: 有的商品两种格式都有)' AS section;
SELECT product_id, title, year, formats, book_price, blu_ray_price, video_price
FROM v_product_full ORDER BY product_id;

SELECT '7.3 购物篮明细(购物篮可装任意组合)' AS section;
SELECT basket_id, customer_name, product_id, title, format, number, unit_price, subtotal
FROM v_basket_detail ORDER BY basket_id, product_id, format;

SELECT '7.4 每个购物篮合计' AS section;
SELECT basket_id, customer_name, count(*) AS 条目数, sum(number) AS 件数, sum(subtotal) AS 金额
FROM v_basket_detail GROUP BY basket_id, customer_name ORDER BY basket_id;

SELECT '7.5 全概化自检(应为 0 行)' AS section;
SELECT p.product_id, p.title
FROM product p
WHERE NOT EXISTS (SELECT 1 FROM book               b WHERE b.product_id = p.product_id)
  AND NOT EXISTS (SELECT 1 FROM blu_ray            r WHERE r.product_id = p.product_id)
  AND NOT EXISTS (SELECT 1 FROM downloadable_video d WHERE d.product_id = p.product_id);

SELECT '7.6 重叠自检(应列出同时有两种以上格式的商品)' AS section;
SELECT product_id, title, formats,
       (CASE WHEN book_price    IS NOT NULL THEN 1 ELSE 0 END
      + CASE WHEN blu_ray_price IS NOT NULL THEN 1 ELSE 0 END
      + CASE WHEN video_price   IS NOT NULL THEN 1 ELSE 0 END) AS 格式数
FROM v_product_full
WHERE (CASE WHEN book_price    IS NOT NULL THEN 1 ELSE 0 END
     + CASE WHEN blu_ray_price IS NOT NULL THEN 1 ELSE 0 END
     + CASE WHEN video_price   IS NOT NULL THEN 1 ELSE 0 END) > 1
ORDER BY product_id;

SELECT '7.7 作者-图书(M:N)与出版社-图书(1:N)' AS section;
SELECT a.author_name, b.isbn, p.title, pb.pub_name
FROM written_by w
JOIN author    a  ON a.author_name = w.author_name
JOIN book      b  ON b.isbn        = w.isbn
JOIN product   p  ON p.product_id  = b.product_id
JOIN publisher pb ON pb.pub_name   = b.pub_name
ORDER BY b.isbn, a.author_name;

SELECT '7.8 库存总览' AS section;
SELECT code, product_id, title, stock_number FROM v_stock_overview ORDER BY code, product_id;


-- ============================================================
-- 步骤 8：反例测试 —— 预期报错(共 10 处)
--   下面开始故意制造错误, 所以把"遇错即止"关掉; 脚本最后应恰好 10 条报错。
-- ============================================================
\set ON_ERROR_STOP off

SELECT '8.1 预期报错：插入没有任何格式的商品 9001(提交时被全概化触发器拦下)' AS section;
INSERT INTO product (product_id, title, year) VALUES (9001, '幽灵商品', 2020);

SELECT '8.2 预期报错：给商品 1001 再插一条图书(违反 uq_book_product: 一种格式最多一条)' AS section;
INSERT INTO book (isbn, product_id, pub_name, price) VALUES ('9787040406642', 1001, '高等教育出版社', 50.00);

SELECT '8.3 预期报错：价格必须为正' AS section;
INSERT INTO blu_ray (upc, product_id, price) VALUES ('6901234567009', 1005, -1.00);

SELECT '8.4 预期报错：购物篮引用不存在的商品 88888(先被 format 完整性触发器拦下)' AS section;
INSERT INTO contains (basket_id, product_id, format, number) VALUES (1, 88888, 'book', 1);

SELECT '8.5 预期报错：format 与商品实际格式不符(商品 1002 只有图书格式)' AS section;
INSERT INTO contains (basket_id, product_id, format, number) VALUES (1, 1002, 'blu_ray', 1);

SELECT '8.6 预期报错：数量必须为正' AS section;
INSERT INTO contains (basket_id, product_id, format, number) VALUES (1, 1001, 'book', 0);

SELECT '8.7 预期报错：同一个篮里重复的同商品同格式(主键冲突)' AS section;
INSERT INTO contains (basket_id, product_id, format, number) VALUES (1, 1001, 'book', 3);

SELECT '8.8 预期报错：删掉商品 1001 唯一的图书行(先清掉作者依赖, 再删), 提交时报全概化错误' AS section;
BEGIN;
DELETE FROM written_by WHERE isbn = '9787040406641';
DELETE FROM book      WHERE isbn = '9787040406641';
COMMIT;

SELECT '8.9 预期报错：删除还有购物篮记录的商品 1001(外键 RESTRICT)' AS section;
DELETE FROM product WHERE product_id = 1001;

SELECT '8.10 预期报错：重复的作者-图书组合(主键冲突)' AS section;
INSERT INTO written_by (author_name, isbn) VALUES ('王珊', '9787040406641');

SELECT '步骤 8 完成：以上 10 处报错都是预期行为' AS section;


-- ============================================================
-- 步骤 9：收尾
-- ============================================================
SELECT '脚本执行完毕：12 张表 + 3 个视图 + 5 枚触发器 + 全概化/重叠约束已就绪' AS section;

-- 想清空请手工执行(顺序不能乱):
--   DROP VIEW  v_stock_overview, v_basket_detail, v_product_full;
--   DROP TABLE stocks, contains, written_by, downloadable_video, blu_ray, book, product,
--              shopping_basket, customer, warehouse, publisher, author CASCADE;
--   DROP FUNCTION assert_product_has_format(), assert_contains_format_exists() CASCADE;
-- 或者整库删掉: .\psql.bat "DROP DATABASE IF EXISTS bookstore_demo WITH (FORCE)"


-- ============================================================
-- 实测结论(PostgreSQL 17.11, 库 bookstore_demo, 实跑 0 警告 / 10 条预期报错)：
--   · 12 张表 + 3 个视图 + 5 枚触发器; 同库连跑两遍结果一致(可重复执行);
--   · 行数: author 4 / publisher 3 / warehouse 2 / customer 3 / shopping_basket 3 /
--           product 6 / book 3 / blu_ray 2 / downloadable_video 3 /
--           written_by 4 / contains 7 / stocks 6;
--   · 概化落地效果(见 7.2 商品全景): 1001、1002 只有 book, 1004 只有 blu_ray,
--     1005 只有 downloadable_video; 而 1003 = blu_ray + downloadable_video
--     (68.00 / 25.00)、1006 = book + downloadable_video(99.00 / 39.00)
--     —— 同一商品两种格式、两个价格, 这正是题 b 说的"两种格式都有且价格不同";
--   · 全概化自检(7.5)返回 0 行; 重叠自检(7.6)列出 1003、1006 两个商品;
--   · 购物篮(7.3 / 7.4): 篮1 = 2 种书 + 1 张蓝光(389.00), 篮2 = 蓝光 + 视频(107.00),
--     篮3 = 2 个可下载视频(65.00) —— 三个篮装的是三种格式的任意组合, 即题 c 的目标;
--   · 10 处报错一处不多一处不少地被拦下:
--       全概化可延迟触发器 2 处(插入没有任何格式的商品 9001; 删掉商品 1001 唯一格式后提交)、
--       UNIQUE 2 处(同一商品的第二条图书、重复的作者-图书)、
--       CHECK 2 处(蓝光价格为负、数量为 0)、
--       format 完整性触发器 2 处(引用不存在的商品 88888、把图书说成蓝光)、
--       外键 RESTRICT 1 处(删除还有图书的商品 1001)、主键冲突 1 处(同篮同商品同格式重复);
--   · 落地时补的一刀: contains 的 format 属性。题 c 的 E-R 图只画到"篮里能装三种商品",
--     但商品 1003 同时有蓝光和视频两个版本, 篮里必须记住买的是哪一种, 否则算不出单价;
--     触发器 assert_contains_format_exists() 负责保证该格式对该商品真实存在。
--   · 提醒: 建表/建约束阶段 \set ON_ERROR_STOP on, 反例阶段才关掉;
--     全概化检查推迟到提交时, 因此示例数据必须包在同一个事务里(步骤 5 的 BEGIN/COMMIT)。
-- ============================================================
