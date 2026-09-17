-- ============================================================
-- OLAP 演示（MySQL 8.0 版本）：教材图 5-16「关系 sales 的例子」
--
-- 与 PostgreSQL 版（olap_sales.sql）数据完全一致：
--   sales(item_name, color, clothes_size, quantity)
--   4 种服装 × 3 种颜色 × 3 种尺码 = 36 行，quantity 合计 155
--
-- 方言差异（MySQL 没有 CUBE / GROUPING SETS）：
--   · 单维/层次汇总用  GROUP BY ... WITH ROLLUP + GROUPING()
--   · 多维任意组合汇总用  UNION ALL 手工拼接（即教材图 5-17 的写法）
--
-- 执行：
--   mysqlsh --sql --uri root@localhost:3306 --schema=university --file=src\main\resources\sql\olap_sales_mysql.sql
--   mysql -u root -p university < src\main\resources\sql\olap_sales_mysql.sql
--
-- 本机验证结论（MySQL 8.0.44，经 mysqlsh 实跑）：
--   · 重复执行 exit=0、无 ERROR，脚本可重复执行（首句即 DROP TABLE IF EXISTS sales）
--   · 录入 36 行 / 合计 155 / 36 个唯一 (服装,颜色,尺码) 组合
--   · CHECK 域约束确实生效（插入非法值报 ERROR 3819 Check constraint ... is violated）
--   · 各查询结果与 PostgreSQL 版（olap_sales.sql）逐项一致：
--     按服装 44/35/49/27、按颜色 62/45/48、按尺码 64/36/55、总计 155
-- ============================================================

DROP TABLE IF EXISTS sales;

CREATE TABLE sales (
    item_name    VARCHAR(10) NOT NULL COMMENT '维属性：服装名',
    color        VARCHAR(10) NOT NULL COMMENT '维属性：颜色',
    clothes_size VARCHAR(10) NOT NULL COMMENT '维属性：尺码',
    quantity     INTEGER     NOT NULL COMMENT '度量属性：销售数量',
    CONSTRAINT sales_item_name_domain    CHECK (item_name    IN ('skirt', 'dress', 'shirt', 'pants')),
    CONSTRAINT sales_color_domain        CHECK (color        IN ('dark', 'pastel', 'white')),
    CONSTRAINT sales_clothes_size_domain CHECK (clothes_size IN ('small', 'medium', 'large')),
    CONSTRAINT sales_quantity_nonneg     CHECK (quantity >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '图5-16 关系 sales 的例子（OLAP 示例事实表）';

-- ========== 图 5-16 的 36 个元组 ==========
INSERT INTO sales (item_name, color, clothes_size, quantity) VALUES
    -- skirt
    ('skirt', 'dark',   'small',   2),
    ('skirt', 'dark',   'medium',  5),
    ('skirt', 'dark',   'large',   1),
    ('skirt', 'pastel', 'small',  11),
    ('skirt', 'pastel', 'medium',  0),
    ('skirt', 'pastel', 'large',  15),
    ('skirt', 'white',  'small',   2),
    ('skirt', 'white',  'medium',  5),
    ('skirt', 'white',  'large',   3),
    -- dress
    ('dress', 'dark',   'small',   2),
    ('dress', 'dark',   'medium',  6),
    ('dress', 'dark',   'large',  12),
    ('dress', 'pastel', 'small',   4),
    ('dress', 'pastel', 'medium',  3),
    ('dress', 'pastel', 'large',   3),
    ('dress', 'white',  'small',   2),
    ('dress', 'white',  'medium',  3),
    ('dress', 'white',  'large',   0),
    -- shirt
    ('shirt', 'dark',   'small',   2),
    ('shirt', 'dark',   'medium',  6),
    ('shirt', 'dark',   'large',   6),
    ('shirt', 'pastel', 'small',   4),
    ('shirt', 'pastel', 'medium',  1),
    ('shirt', 'pastel', 'large',   2),
    ('shirt', 'white',  'small',  17),
    ('shirt', 'white',  'medium',  1),
    ('shirt', 'white',  'large',  10),
    -- pants
    ('pants', 'dark',   'small',  14),
    ('pants', 'dark',   'medium',  6),
    ('pants', 'dark',   'large',   0),
    ('pants', 'pastel', 'small',   1),
    ('pants', 'pastel', 'medium',  0),
    ('pants', 'pastel', 'large',   1),
    ('pants', 'white',  'small',   3),
    ('pants', 'white',  'medium',  0),
    ('pants', 'white',  'large',   2);

-- ========== ① 录入校验：应为 36 行 / 155 件 ==========
SELECT COUNT(*) AS rows_cnt, SUM(quantity) AS total_quantity FROM sales;

-- ========== ② 原始表 ==========
SELECT item_name, color, clothes_size, quantity
FROM sales
ORDER BY item_name, color, clothes_size;

-- ========== ③ WITH ROLLUP：服装 → 颜色 → 尺码 逐级小计 + 总计 ==========
-- ROLLUP 产生的汇总行在对应列上为 NULL，用 GROUPING() 区分"真 NULL"与"汇总 NULL"
SELECT IF(GROUPING(item_name) = 1, 'all', item_name)       AS item_name,
       IF(GROUPING(color) = 1, 'all', color)               AS color,
       IF(GROUPING(clothes_size) = 1, 'all', clothes_size) AS clothes_size,
       SUM(quantity)                                       AS quantity
FROM sales
GROUP BY item_name, color, clothes_size WITH ROLLUP
ORDER BY item_name, color, clothes_size;

-- ========== ④ 行转列：item_name × clothes_size（含行合计） ==========
SELECT item_name,
       SUM(CASE WHEN clothes_size = 'small'  THEN quantity ELSE 0 END) AS small,
       SUM(CASE WHEN clothes_size = 'medium' THEN quantity ELSE 0 END) AS medium,
       SUM(CASE WHEN clothes_size = 'large'  THEN quantity ELSE 0 END) AS large,
       SUM(quantity)                                                   AS total
FROM sales
GROUP BY item_name
ORDER BY item_name;

-- ========== ⑤ 行转列 + 列合计（WITH ROLLUP 与条件聚集结合） ==========
SELECT IF(GROUPING(color) = 1, 'all', color) AS color,
       SUM(CASE WHEN clothes_size = 'small'  THEN quantity ELSE 0 END) AS small,
       SUM(CASE WHEN clothes_size = 'medium' THEN quantity ELSE 0 END) AS medium,
       SUM(CASE WHEN clothes_size = 'large'  THEN quantity ELSE 0 END) AS large,
       SUM(quantity)                                                   AS total
FROM sales
GROUP BY color WITH ROLLUP
ORDER BY color;

-- ========== ⑥ 教材图 5-17 风格交叉表：UNION ALL 拼 all 小计 ==========
-- MySQL 无 GROUPING SETS，用 UNION ALL 组合四种分组集（只扫 4 遍表）
SELECT item_name, clothes_size, SUM(quantity) AS quantity FROM sales GROUP BY item_name, clothes_size
UNION ALL
SELECT item_name, 'all',        SUM(quantity)          FROM sales GROUP BY item_name
UNION ALL
SELECT 'all',     clothes_size, SUM(quantity)          FROM sales GROUP BY clothes_size
UNION ALL
SELECT 'all',     'all',        SUM(quantity)          FROM sales
ORDER BY item_name, clothes_size;

-- ========== ⑦ 清理（演示后可执行） ==========
-- DROP TABLE IF EXISTS sales；
