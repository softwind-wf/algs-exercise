-- index_type_demo.sql   —— PostgreSQL 侧实测
-- 目的：① 索引用什么结构（pg_am + 真实索引的元数据）；② 能不能“指定/强制”用某个索引。
-- 用法：psql -h 127.0.0.1 -U postgres -d university -f src/main/resources/sql/index_type_demo.sql -o docs/index-demo-raw.txt
-- 全部对象建在事务里，末尾 ROLLBACK，不留垃圾表，可反复跑。

\set ON_ERROR_STOP on
\pset pager off
\pset format aligned

\qecho '===== 0. 实例提供的索引访问方法（同一个“索引”名词，六种不同实现）====='
SELECT amname AS access_method,
       CASE amname
           WHEN 'btree'  THEN 'B 树家族：等值+范围+排序+前缀，默认选项'
           WHEN 'hash'   THEN '哈希表：只支持等值'
           WHEN 'gin'    THEN '倒排：数组、JSONB、全文检索（多值字段）'
           WHEN 'gist'   THEN '通用搜索树：几何、范围、近邻'
           WHEN 'spgist' THEN '空间分区树：点、文本前缀'
           WHEN 'brin'   THEN '块级摘要：超大且物理有序的表'
       END AS what_for
  FROM pg_am
 WHERE amtype = 'i'
 ORDER BY amname;

BEGIN;

\qecho '===== 1. 建 2 万行演示表；PRIMARY KEY 自动带一个 btree 唯一索引 ====='
CREATE TABLE idx_demo (
    id      int       PRIMARY KEY,
    name    text      NOT NULL,
    tags    text[],
    created date      NOT NULL,
    period  int4range
);

INSERT INTO idx_demo
SELECT g,
       'user_' || g,
       ARRAY['tag' || (g % 10), 'grp' || (g % 5)],
       DATE '2024-01-01' + (g % 365),
       int4range(g * 2, g * 2 + 10)
  FROM generate_series(1, 20000) AS g;

ANALYZE idx_demo;

\d idx_demo

\qecho '===== 2. 同一张表上并存 6 种结构的索引（describe 会打出每个索引的类型）====='
\qecho '--- 2.1 btree：不写 USING 就是它 ---'
CREATE INDEX idx_demo_name_btree ON idx_demo (name);
\d idx_demo_name_btree

\qecho '--- 2.2 hash ---'
CREATE INDEX idx_demo_name_hash ON idx_demo USING hash (name);
\d idx_demo_name_hash

\qecho '--- 2.3 gin：数组列，服务 @> 包含查询 ---'
CREATE INDEX idx_demo_tags_gin ON idx_demo USING gin (tags);
\d idx_demo_tags_gin

\qecho '--- 2.4 gist：范围类型，服务 && 重叠查询 ---'
CREATE INDEX idx_demo_period_gist ON idx_demo USING gist (period);
\d idx_demo_period_gist

\qecho '--- 2.5 spgist：文本前缀 ---'
CREATE INDEX idx_demo_name_spgist ON idx_demo USING spgist (name text_ops);
\d idx_demo_name_spgist

\qecho '--- 2.6 brin：块级摘要，适合 append-only 大表 ---'
CREATE INDEX idx_demo_created_brin ON idx_demo USING brin (created);
\d idx_demo_created_brin

\qecho '--- 2.7 六种索引同时可见 ---'
\d idx_demo

\qecho '===== 3. 优化器怎么选：同一条查询，索引不同计划就不同 ====='

\qecho '--- 3.1 只留 btree：等值查询走 btree Index Scan（cost 8.30）---'
DROP INDEX idx_demo_name_hash;
DROP INDEX idx_demo_name_spgist;
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM idx_demo WHERE name = 'user_19999';

\qecho '--- 3.2 再加一个 hash：同一条查询，优化器改挑 hash（cost 8.02 < 8.30）---'
CREATE INDEX idx_demo_name_hash ON idx_demo USING hash (name);
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM idx_demo WHERE name = 'user_19999';

\qecho '--- 3.3 删掉 btree 后做范围查询：hash 只认等值，退化为 Seq Scan ---'
DROP INDEX idx_demo_name_btree;
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM idx_demo WHERE name > 'user_19999';

\qecho '--- 3.4 GIN：数组包含 ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM idx_demo WHERE tags @> ARRAY['tag3']::text[];

\qecho '--- 3.5 GiST：范围重叠 ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM idx_demo WHERE period && int4range(1000, 1010);

\qecho '--- 3.6 BRIN：日期范围。2 万行小表上不被选（顺序扫描本来就便宜）---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM idx_demo WHERE created BETWEEN DATE '2024-03-01' AND DATE '2024-03-03';

\qecho '--- 3.7 关掉 seqscan 后 BRIN 被“逼”出来，但代价反而更高，且按块命中是 lossy 的 ---'
SET LOCAL enable_seqscan = off;
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM idx_demo WHERE created BETWEEN DATE '2024-03-01' AND DATE '2024-03-03';
RESET enable_seqscan;

\qecho '===== 4. 能不能“指定用哪个索引”？ ====='

\qecho '--- 4.1 写 hint 注释：PostgreSQL 当注释忽略，不报错也不生效（仍是 Seq Scan）---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT /*+ IndexScan(idx_demo idx_demo_created_brin) */
       count(*) FROM idx_demo WHERE created BETWEEN DATE '2024-03-01' AND DATE '2024-03-03';

\qecho '--- 4.2 能写 hint 的扩展：pg_hint_plan（本机未安装，故为 NULL）---'
SELECT name, default_version, installed_version
  FROM pg_available_extensions
 WHERE name IN ('pg_hint_plan', 'pg_trgm', 'btree_gist')
 ORDER BY name;

\qecho '--- 4.3 官方批准的“变通”：enable_*scan 系列 GUC 开关计划形态 ---'
SELECT name, setting, short_desc
  FROM pg_settings
 WHERE name LIKE 'enable_%scan'
 ORDER BY name;

\qecho '===== 5. 收尾：回滚，表与索引都不留 ====='
ROLLBACK;

\qecho '===== done ====='
