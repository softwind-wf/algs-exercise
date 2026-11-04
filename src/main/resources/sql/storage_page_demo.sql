-- ================================================================
-- 数据怎么存到磁盘、又怎么从磁盘读出来 —— PostgreSQL 实机演示
--
-- 配套文档: docs/数据怎么存到磁盘-从记录到页到缓冲池.md
-- 一键运行: scripts/run-storage-page-demo.ps1
--
-- 本文件只在演示库 storage_demo 内建自己的表, 不碰 university 等既有库。
-- 运行时由脚本设置 PGCLIENTENCODING=UTF8, 所以中文注释/别名都能正常显示。
-- ================================================================
\set ON_ERROR_STOP on
\pset pager off
\pset border 2

-- pageinspect: 把一页当成原始字节读出来（page_header / heap_page_items / get_raw_page）
-- pg_buffercache: 看共享缓冲池里此刻缓存了哪些页
CREATE EXTENSION IF NOT EXISTS pageinspect;
CREATE EXTENSION IF NOT EXISTS pg_buffercache;

SET client_min_messages = warning;

\echo ''
\echo '=== 0. 先量清楚单位: 数据库的一"页"有多大, 内存里能放多少页 ==='
SHOW block_size;
SHOW shared_buffers;

\echo ''
\echo '=== 1. 造一张"每行约 244 字节"的表: 32 行正好塞满一个 8KB 页 ==='
DROP TABLE IF EXISTS t_demo;
CREATE TABLE t_demo (
    id   int  PRIMARY KEY,
    name text NOT NULL,
    pad  char(200) NOT NULL DEFAULT ''
);
INSERT INTO t_demo (id, name, pad)
SELECT g, 'student' || g, repeat('x', 190)
FROM generate_series(1, 40) AS g;

SELECT pg_relation_size('t_demo')               AS "字节数",
       pg_relation_size('t_demo') / 8192        AS "占用页数",
       pg_relation_size('t_demo_pkey') / 8192   AS "主键索引页数";

\echo ''
\echo '=== 2. 页头: 每页开头 24 字节的"目录卡片" ==='
\echo '    (lower=空闲空间起点, upper=空闲空间终点, 两者之间才是可插入的空地)'
SELECT * FROM page_header(get_raw_page('t_demo', 0));

\echo ''
\echo '=== 3. 行指针数组 + 元组: 页里到底放了什么 ==='
\echo '    (lp_off = 元组在这一页内的起始偏移, lp_flags: 1=正常 2=重定向 3=死 0=未使用)'
SELECT lp, lp_off, lp_flags, lp_len, t_xmin, t_xmax, t_ctid
FROM heap_page_items(get_raw_page('t_demo', 0))
LIMIT 3;

SELECT count(*) AS "第 0 页上一共几行"
FROM heap_page_items(get_raw_page('t_demo', 0));

\echo ''
\echo '=== 4. 每一行的物理地址: ctid = (块号, 槽号) ==='
SELECT ctid, id, name FROM t_demo ORDER BY ctid LIMIT 5;
SELECT ctid, id, name FROM t_demo ORDER BY ctid DESC LIMIT 3;

\echo ''
\echo '=== 5. 不信 pageinspect? 直接按字节手工解码页头 ==='
SELECT encode(substring(get_raw_page('t_demo', 0) FROM 1 FOR 32), 'hex') AS "页最前面 32 字节";

-- 页头布局(字节都从 0 数, 小端序 = 低位字节在前):
--   0-7 LSN | 8-9 校验和 | 10-11 标志 | 12-13 pd_lower
--  14-15 pd_upper | 16-17 pd_special | 18-19 页大小|版本 | 20-23 prune_xid
WITH p AS (SELECT get_raw_page('t_demo', 0) AS pg)
SELECT get_byte(pg, 12) + get_byte(pg, 13) * 256                     AS "pd_lower 手工解出",
       get_byte(pg, 14) + get_byte(pg, 15) * 256                     AS "pd_upper 手工解出",
       get_byte(pg, 16) + get_byte(pg, 17) * 256                     AS "pd_special 手工解出",
       get_byte(pg, 18) + get_byte(pg, 19) * 256                     AS "页大小|版本"
FROM p;

-- 行指针 1 在页内偏移 24..27(从 0 数), 是一个 32 位小端整数:
-- 低 15 位 = 元组起点, 中间 2 位 = 状态, 高 15 位 = 元组长度
WITH p AS (SELECT get_raw_page('t_demo', 0) AS pg),
     w AS (
         SELECT get_byte(pg, 24)::bigint
              + get_byte(pg, 25)::bigint * 256
              + get_byte(pg, 26)::bigint * 65536
              + get_byte(pg, 27)::bigint * 16777216 AS v
         FROM p
     )
SELECT (v & 32767)     AS "lp_off 手工解出",
       ((v >> 15) & 3) AS "lp_flags 手工解出",
       (v >> 17)       AS "lp_len 手工解出"
FROM w;

\echo ''
\echo '=== 6. 这一页在磁盘上是哪个文件的第几个 8KB ==='
SELECT pg_relation_filepath('t_demo')        AS "数据文件(相对路径)",
       current_setting('data_directory')     AS "数据目录";
\echo '    (脚本会到磁盘上把这个文件的字节读出来, 和上面的数字对一遍)'

\echo ''
\echo '=== 7. 读路径: 索引先给出 ctid, 再按块号把整页搬到内存 ==='
\echo '    (shared hit = 命中的页数; 若页不在内存, 这里会变成 shared read)'
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM t_demo WHERE id = 40;
EXPLAIN (ANALYZE, BUFFERS) SELECT count(*) FROM t_demo WHERE pad LIKE '%zzz%';
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM t_demo WHERE ctid = '(1,1)';

\echo ''
\echo '=== 8. 写路径: 改页之前, 先把"要改什么"顺序写进 WAL ==='
SELECT pg_current_wal_lsn() AS "插入前的 WAL 位置";
INSERT INTO t_demo (id, name, pad) VALUES (41, 'student41', repeat('x', 190));
SELECT pg_current_wal_lsn() AS "插入后的 WAL 位置",
       pg_walfile_name(pg_current_wal_lsn()) AS "WAL 文件名";
SELECT wal_records, wal_fpi, wal_bytes FROM pg_stat_wal;
\echo '    (脚本随后用 pg_waldump 把这一小段 WAL 的原始记录打出来)'

\echo ''
\echo '=== 9. 删一行、改一行: 为什么磁盘上的页不会变小 ==='
DELETE FROM t_demo WHERE id = 10;
UPDATE t_demo SET pad = repeat('y', 190) WHERE id = 11;

SELECT pg_relation_size('t_demo') / 8192 AS "删除+更新后页数(没变)";
SELECT lp, lp_off, lp_flags, lp_len, t_xmin, t_xmax, t_ctid
FROM heap_page_items(get_raw_page('t_demo', 0))
WHERE lp IN (10, 11)
ORDER BY lp;
SELECT ctid, id FROM t_demo WHERE id = 11;
\echo '    (第 10 行只是 t_xmax 被写上"删除事务号"; 第 11 行被写成新版本, 旧版本前转指向新位置)'

\echo ''
\echo '=== 10. VACUUM 之后, 页内的死行空间才会被标成可复用 ==='
VACUUM t_demo;
SELECT lp, lp_off, lp_flags, lp_len
FROM heap_page_items(get_raw_page('t_demo', 0))
WHERE lp IN (10, 11)
ORDER BY lp;
SELECT * FROM page_header(get_raw_page('t_demo', 0));

\echo ''
\echo '=== 11. 缓冲池: 这些页此刻同时存在于内存(shared_buffers)里 ==='
\echo '    (isfork: relforknumber 0=主数据 1=空闲空间映射FSM 2=可见性映射VM)'
SELECT c.relname       AS "表",
       b.relforknumber AS "fork",
       b.relblocknumber AS "页号",
       b.isdirty       AS "是否脏页",
       b.usagecount    AS "使用计数"
FROM pg_buffercache b
JOIN pg_class c ON b.relfilenode = c.relfilenode
WHERE c.relname = 't_demo'
ORDER BY b.relforknumber, b.relblocknumber;
