-- index_type_demo_mysql.sql   —— MySQL 8.0 侧实测
-- 目的：① InnoDB 索引用什么结构（B+ 树 + 聚簇索引）；② 能不能“指定用哪个索引/哪种索引”。
-- 用法（Windows，PowerShell 里 `<` 不管用，交给 cmd）：
--   cmd /c ""C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --host=127.0.0.1 --port=3306 --user=root ^
--          --default-character-set=utf8mb4 university --tee=docs/index-demo-mysql-raw.txt < src\main\resources\sql\index_type_demo_mysql.sql"
-- 依赖：MySQL 8.0（EXPLAIN ANALYZE / 优化器 hint / CTE 都是 8.0 特性）。演示表 up_idx_demo 用完即删。

SELECT VERSION() AS mysql_version, @@default_storage_engine AS storage_engine;

SELECT '===== 1. InnoDB 表结构：PRIMARY KEY 就是聚簇索引（B+ 树，叶子页存整行）=====' AS step;
DROP TABLE IF EXISTS up_idx_demo;
CREATE TABLE up_idx_demo (
    id      INT         NOT NULL,
    name    VARCHAR(32) NOT NULL,
    tag     VARCHAR(16),
    created DATE        NOT NULL,
    PRIMARY KEY (id),
    KEY idx_name (name)
) ENGINE=InnoDB;

SET SESSION cte_max_recursion_depth = 300000;
INSERT INTO up_idx_demo (id, name, tag, created)
WITH RECURSIVE seq (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 200000
)
SELECT n, CONCAT('user_', n), CONCAT('tag', n % 10),
       DATE_ADD('2024-01-01', INTERVAL (n % 365) DAY)
  FROM seq;

ANALYZE TABLE up_idx_demo;

SHOW CREATE TABLE up_idx_demo;
SHOW INDEX FROM up_idx_demo;

SELECT '===== 2. 二级索引叶子存“主键 + 索引列”：覆盖索引免回表，SELECT * 要回表 =====' AS step;
EXPLAIN SELECT id, name FROM up_idx_demo WHERE name = 'user_19999';
EXPLAIN SELECT *       FROM up_idx_demo WHERE name = 'user_19999';
EXPLAIN FORMAT=TREE SELECT * FROM up_idx_demo WHERE name = 'user_19999';
EXPLAIN ANALYZE SELECT * FROM up_idx_demo WHERE name = 'user_19999';

SELECT '===== 3. InnoDB 只有 B+ 树：USING HASH 不报错，但被静默建成 BTREE =====' AS step;
CREATE INDEX idx_hash_attempt ON up_idx_demo (name) USING HASH;
SHOW INDEX FROM up_idx_demo;
DROP INDEX idx_hash_attempt ON up_idx_demo;

SELECT '===== 4. 真正支持 HASH 索引的是 MEMORY 引擎 =====' AS step;
DROP TABLE IF EXISTS mem_demo;
CREATE TABLE mem_demo (
    id INT,
    KEY idx_id (id) USING HASH
) ENGINE=MEMORY;
SHOW INDEX FROM mem_demo;
DROP TABLE mem_demo;

SELECT '===== 5. FULLTEXT 索引：倒排结构，不是 B+ 树 =====' AS step;
ALTER TABLE up_idx_demo ADD FULLTEXT INDEX ft_name (name);
SHOW INDEX FROM up_idx_demo;
EXPLAIN SELECT * FROM up_idx_demo WHERE MATCH(name) AGAINST('user_19999');

SELECT '===== 6. 指定“用哪个索引”：USE INDEX 只是建议，FORCE INDEX 才硬来 =====' AS step;
SELECT '--- 6.1 不指定：优化器嫌回表太贵，直接全表扫（type=ALL）---' AS step;
EXPLAIN SELECT * FROM up_idx_demo WHERE name LIKE 'user_1%';

SELECT '--- 6.2 USE INDEX：只是建议，优化器依旧全表扫（type=ALL）---' AS step;
EXPLAIN SELECT * FROM up_idx_demo USE INDEX (idx_name) WHERE name LIKE 'user_1%';

SELECT '--- 6.3 FORCE INDEX：硬来，type=range 走 idx_name（估算 rows=1 明显失真）---' AS step;
EXPLAIN SELECT * FROM up_idx_demo FORCE INDEX (idx_name) WHERE name LIKE 'user_1%';

SELECT '--- 6.4 8.0 优化器 hint /*+ INDEX */：等价于 FORCE INDEX 的选择 ---' AS step;
EXPLAIN SELECT /*+ INDEX(up_idx_demo idx_name) */ * FROM up_idx_demo WHERE name LIKE 'user_1%';

SELECT '--- 6.5 强制未必更快：EXPLAIN ANALYZE 对比（全表扫 vs FORCE INDEX）---' AS step;
EXPLAIN ANALYZE SELECT * FROM up_idx_demo WHERE name LIKE 'user_1%';
EXPLAIN ANALYZE SELECT * FROM up_idx_demo FORCE INDEX (idx_name) WHERE name LIKE 'user_1%';

SELECT '===== 7. B+ 树的页数证据：mysql.innodb_index_stats（size 单位是 16KB 页）=====' AS step;
SELECT index_name, stat_name, stat_value
  FROM mysql.innodb_index_stats
 WHERE database_name = 'university'
   AND table_name = 'up_idx_demo'
   AND stat_name IN ('size', 'n_leaf_pages')
 ORDER BY index_name, stat_name;

SELECT '===== 8. 指定“哪种索引”：CREATE INDEX ... USING BTREE（InnoDB 默认也是它）=====' AS step;
CREATE INDEX idx_created ON up_idx_demo (created) USING BTREE;
EXPLAIN SELECT * FROM up_idx_demo WHERE created BETWEEN '2024-03-01' AND '2024-03-03';

SELECT '===== 9. 收尾：删演示表 =====' AS step;
DROP TABLE up_idx_demo;
