-- ============================================================
-- OLAP 演示：教材图 5-16「关系 sales 的例子」
--
-- 关系模式:  sales(item_name, color, clothes_size, quantity)
--   维属性 (dimension attribute)  : item_name, color, clothes_size
--        item_name    ∈ {skirt, dress, shirt, pants}
--        color        ∈ {dark, pastel, white}
--        clothes_size ∈ {small, medium, large}
--   度量属性 (measure attribute)  : quantity（整数值，表示一次销售的件数）
--
-- 数据规模：4 种服装 × 3 种颜色 × 3 种尺码 = 36 个元组
-- 合计 quantity = 155（按服装 44 / 35 / 49 / 27；按颜色 62 / 45 / 48；按尺码 64 / 36 / 55）
--
-- 执行：
--   psql.bat -f src\main\resources\sql\olap_sales.sql
--   psql.bat -d university -f src\main\resources\sql\olap_sales.sql
-- ============================================================

-- 注: psql.bat / sql.bat 现在分别转发官方的 psql.exe / mysql.exe(元命令、$$、注释里的分号都能用)

-- ============================================================
-- 1. 建表：维属性 + 度量属性，用 CHECK 约束落实教材给出的取值域
-- ============================================================
-- 先删依赖 sales 的物化视图，否则重跑时 DROP TABLE 会因依赖而失败
DROP MATERIALIZED VIEW IF EXISTS sales_cube;
DROP TABLE IF EXISTS sales;

CREATE TABLE sales (
    item_name    VARCHAR(10) NOT NULL,      -- 维属性：服装名
    color        VARCHAR(10) NOT NULL,      -- 维属性：颜色
    clothes_size VARCHAR(10) NOT NULL,      -- 维属性：尺码
    quantity     INTEGER     NOT NULL,      -- 度量属性：销售数量
    CONSTRAINT sales_item_name_domain    CHECK (item_name    IN ('skirt', 'dress', 'shirt', 'pants')),
    CONSTRAINT sales_color_domain        CHECK (color        IN ('dark', 'pastel', 'white')),
    CONSTRAINT sales_clothes_size_domain CHECK (clothes_size IN ('small', 'medium', 'large')),
    CONSTRAINT sales_quantity_nonneg     CHECK (quantity >= 0)
);

COMMENT ON TABLE  sales              IS '图5-16 关系 sales 的例子（OLAP 示例事实表）';
COMMENT ON COLUMN sales.item_name    IS '维属性：服装名';
COMMENT ON COLUMN sales.color        IS '维属性：颜色';
COMMENT ON COLUMN sales.clothes_size IS '维属性：尺码';
COMMENT ON COLUMN sales.quantity     IS '度量属性：销售数量';

-- ============================================================
-- 2. 图 5-16 的 36 个元组（逐行录入教材原表数值）
-- ============================================================
INSERT INTO sales (item_name, color, clothes_size, quantity) VALUES
    -- skirt（9 行）
    ('skirt', 'dark',   'small',   2),
    ('skirt', 'dark',   'medium',  5),
    ('skirt', 'dark',   'large',   1),
    ('skirt', 'pastel', 'small',  11),
    ('skirt', 'pastel', 'medium',  0),
    ('skirt', 'pastel', 'large',  15),
    ('skirt', 'white',  'small',   2),
    ('skirt', 'white',  'medium',  5),
    ('skirt', 'white',  'large',   3),
    -- dress（9 行）
    ('dress', 'dark',   'small',   2),
    ('dress', 'dark',   'medium',  6),
    ('dress', 'dark',   'large',  12),
    ('dress', 'pastel', 'small',   4),
    ('dress', 'pastel', 'medium',  3),
    ('dress', 'pastel', 'large',   3),
    ('dress', 'white',  'small',   2),
    ('dress', 'white',  'medium',  3),
    ('dress', 'white',  'large',   0),
    -- shirt（9 行）
    ('shirt', 'dark',   'small',   2),
    ('shirt', 'dark',   'medium',  6),
    ('shirt', 'dark',   'large',   6),
    ('shirt', 'pastel', 'small',   4),
    ('shirt', 'pastel', 'medium',  1),
    ('shirt', 'pastel', 'large',   2),
    ('shirt', 'white',  'small',  17),
    ('shirt', 'white',  'medium',  1),
    ('shirt', 'white',  'large',  10),
    -- pants（9 行）
    ('pants', 'dark',   'small',  14),
    ('pants', 'dark',   'medium',  6),
    ('pants', 'dark',   'large',   0),
    ('pants', 'pastel', 'small',   1),
    ('pants', 'pastel', 'medium',  0),
    ('pants', 'pastel', 'large',   1),
    ('pants', 'white',  'small',   3),
    ('pants', 'white',  'medium',  0),
    ('pants', 'white',  'large',   2);

-- ============================================================
-- 3. 录入校验：行数与合计应分别是 36 和 155
-- ============================================================
SELECT '===== ① 原表 sales（36 行） =====' AS section;
SELECT item_name, color, clothes_size, quantity
FROM sales
ORDER BY item_name, color, clothes_size;

SELECT '===== ② 录入校验：行数 = 36、总销量 = 155、无重复 (服装,颜色,尺码) =====' AS section;
SELECT count(*)                        AS rows,
       sum(quantity)                   AS total_quantity,
       count(DISTINCT (item_name, color, clothes_size)) AS distinct_combos
FROM sales;

-- ============================================================
-- 4. 单维汇总：按每个维属性分别汇总（OLAP 的「上卷 roll-up」最简形态）
-- ============================================================
SELECT '===== ③ 按服装汇总 =====' AS section;
SELECT item_name, sum(quantity) AS quantity
FROM sales
GROUP BY item_name
ORDER BY item_name;

SELECT '===== ④ 按颜色汇总 =====' AS section;
SELECT color, sum(quantity) AS quantity
FROM sales
GROUP BY color
ORDER BY color;

SELECT '===== ⑤ 按尺码汇总 =====' AS section;
SELECT clothes_size, sum(quantity) AS quantity
FROM sales
GROUP BY clothes_size
ORDER BY clothes_size;

-- ============================================================
-- 5. 图 5-17 式交叉表：行 = item_name、列 = clothes_size，
--    行列均带 'all' 小计（教材用 UNION 把各层汇总拼起来）
--    注意：'all' 不属任何维的取值域，因此可安全作为汇总标记
-- ============================================================
SELECT '===== ⑥ 交叉表（教材图5-17 风格：UNION 拼接 all 小计） =====' AS section;
SELECT item_name, clothes_size, sum(quantity) AS quantity FROM sales GROUP BY item_name, clothes_size
UNION ALL
SELECT item_name, 'all',        sum(quantity) AS quantity FROM sales GROUP BY item_name
UNION ALL
SELECT 'all',     clothes_size, sum(quantity) AS quantity FROM sales GROUP BY clothes_size
UNION ALL
SELECT 'all',     'all',        sum(quantity) AS quantity FROM sales
ORDER BY item_name, clothes_size;

-- ============================================================
-- 6. GROUPING SETS：一次扫描算出「指定分组集」的汇总，
--    'all' 由 COALESCE 生成；等价于上面 UNION ALL 的写法但只扫一遍表
-- ============================================================
SELECT '===== ⑦ GROUPING SETS：等价交叉表，一次扫描 =====' AS section;
SELECT coalesce(item_name,    'all') AS item_name,
       coalesce(clothes_size, 'all') AS clothes_size,
       sum(quantity)                 AS quantity
FROM sales
GROUP BY GROUPING SETS (
    (item_name, clothes_size),      -- 明细小计
    (item_name),                    -- 行小计
    (clothes_size),                 -- 列小计
    ()                              -- 总计
)
ORDER BY item_name, clothes_size;

-- ============================================================
-- 7. CUBE：对 3 个维属性做全组合汇总（0/1/2/3 维共 2^3 = 8 种分组集）
--    GROUPING() = 1 表示该列是本次汇总产生的占位 NULL（真 NULL 时为 0）
-- ============================================================
SELECT '===== ⑧ CUBE：三维全组合数据立方体（8 种分组集） =====' AS section;
SELECT CASE WHEN GROUPING(item_name)    = 1 THEN 'all' ELSE item_name    END AS item_name,
       CASE WHEN GROUPING(color)        = 1 THEN 'all' ELSE color        END AS color,
       CASE WHEN GROUPING(clothes_size) = 1 THEN 'all' ELSE clothes_size END AS clothes_size,
       sum(quantity)  AS quantity,
       GROUPING(item_name, color, clothes_size) AS grouping_bits
FROM sales
GROUP BY CUBE (item_name, color, clothes_size)
ORDER BY grouping_bits, item_name, color, clothes_size;

-- ============================================================
-- 8. ROLLUP：按层次 (item_name → color → clothes_size) 逐级上卷，
--    共 4 种分组集，适合"服装 → 颜色 → 尺码"的钻取路径
-- ============================================================
SELECT '===== ⑨ ROLLUP：层次上卷（4 种分组集） =====' AS section;
SELECT coalesce(item_name,    'all') AS item_name,
       coalesce(color,        'all') AS color,
       coalesce(clothes_size, 'all') AS clothes_size,
       sum(quantity)                 AS quantity
FROM sales
GROUP BY ROLLUP (item_name, color, clothes_size)
ORDER BY item_name, color, clothes_size;

-- ============================================================
-- 9. 行转列（PIVOT）：把 clothes_size 维度旋转成列，
--    PostgreSQL 用 sum(...) FILTER (WHERE ...) 实现条件聚集
-- ============================================================
SELECT '===== ⑩ 行转列：item_name × clothes_size（含行合计） =====' AS section;
SELECT item_name,
       sum(quantity) FILTER (WHERE clothes_size = 'small')  AS small,
       sum(quantity) FILTER (WHERE clothes_size = 'medium') AS medium,
       sum(quantity) FILTER (WHERE clothes_size = 'large')  AS large,
       sum(quantity)                                        AS total
FROM sales
GROUP BY item_name
ORDER BY item_name;

SELECT '===== ⑪ 行转列：color × clothes_size（含行/列合计） =====' AS section;
SELECT coalesce(color, 'all') AS color,
       sum(quantity) FILTER (WHERE clothes_size = 'small')  AS small,
       sum(quantity) FILTER (WHERE clothes_size = 'medium') AS medium,
       sum(quantity) FILTER (WHERE clothes_size = 'large')  AS large,
       sum(quantity)                                        AS total
FROM sales
GROUP BY ROLLUP (color)
ORDER BY color;

-- ============================================================
-- 10. 物化视图：把 CUBE 结果固化下来，模拟"预计算的数据立方体"，
--     OLAP 查询直接读它，避免每次重算
-- ============================================================
SELECT '===== ⑫ 物化视图 sales_cube（预计算立方体） =====' AS section;
CREATE MATERIALIZED VIEW sales_cube AS
SELECT item_name,
       color,
       clothes_size,
       sum(quantity) AS quantity,
       count(*)      AS row_count
FROM sales
GROUP BY CUBE (item_name, color, clothes_size);

-- 查询立方体中"一维聚合"的汇总行（color/clothes_size 为汇总 NULL，item_name 有值）
SELECT item_name, sum(quantity) AS quantity
FROM sales_cube
WHERE color IS NULL AND clothes_size IS NULL
  AND item_name IS NOT NULL
GROUP BY item_name
ORDER BY item_name;

-- 立方体行数 = 8 种分组集的行数之和：
--   36 (item_name,color,clothes_size) + 12 (item_name,color) + 12 (item_name,clothes_size)
-- +  9 (color,clothes_size) + 4 (item_name) + 3 (color) + 3 (clothes_size) + 1 ()  = 80
SELECT count(*) AS cube_rows FROM sales_cube;

-- 数据变化后刷新（演练用：删掉一行再刷新会看到立方体变化，随后回滚）
BEGIN;
DELETE FROM sales WHERE item_name = 'skirt' AND color = 'pastel' AND clothes_size = 'medium';
REFRESH MATERIALIZED VIEW sales_cube;
SELECT item_name, color, clothes_size, quantity
FROM sales_cube
WHERE color IS NULL AND clothes_size IS NULL;
ROLLBACK;

-- ============================================================
-- 清理（演示后可执行；注释掉以免影响后续查询）
-- ============================================================
-- DROP MATERIALIZED VIEW IF EXISTS sales_cube；
-- DROP TABLE IF EXISTS sales；
