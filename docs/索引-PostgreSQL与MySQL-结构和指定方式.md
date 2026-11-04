# 索引：PostgreSQL 与 MySQL 用什么结构、能不能指定

实测环境：PostgreSQL 17.11（本机 127.0.0.1:5432，库 university）、MySQL 8.0.44（本机 127.0.0.1:3306，库 university）。
两份可复跑脚本 + 原始输出（本文所有引文都来自这两个文件，不是抄文档）：

- `src/main/resources/sql/index_type_demo.sql` → `docs/index-demo-raw.txt`（PostgreSQL，事务内建表、末尾 ROLLBACK，不留数据）
- `src/main/resources/sql/index_type_demo_mysql.sql` → `docs/index-demo-mysql-raw.txt`（MySQL，演示表用完即删）

---

## 1. 一句话结论

| 问题 | PostgreSQL | MySQL（InnoDB） |
| --- | --- | --- |
| 默认/主力索引结构 | **B 树**（B-tree，Lehman-Yao 高并发变种，B-link） | **B+ 树**（聚簇索引 / clustered index） |
| 表与索引的关系 | 堆表（heap）+ 独立索引文件；索引项存 **TID**（页号+行偏移） | 表就是索引：主键 B+ 树叶子页直接存整行；二级索引叶子存 **主键值** |
| 有没有聚簇索引 | **没有**；`CLUSTER` 只是一次性按某索引重排，之后新数据不再保序 | 有，且是内建、永久生效的 |
| 可选的其他结构 | hash、GIN、GiST、SP-GiST、BRIN，共 6 种，可用 `USING` 指定 | 引擎相关：InnoDB 只有 BTREE（+ FULLTEXT、SPATIAL），HASH 只有 MEMORY 引擎有 |
| 建索引时指定“类型” | 可以，`CREATE INDEX ... USING gin` 真正换结构 | 可以写 `USING BTREE|HASH`，但 InnoDB 会把 HASH 静默建成 BTREE（实测） |
| 运行时指定“用哪个索引” | **不支持 hint**；只能用 `enable_*scan` GUC 变通，或装 `pg_hint_plan` 扩展 | **支持**：`USE INDEX`（建议）、`FORCE INDEX`（硬来）、`IGNORE INDEX`、8.0 的 `/*+ INDEX(...) */` |

一句话：两者主力都是“B 树家族”，但 PostgreSQL 是**堆表 + 指向 TID 的 B 树**，MySQL InnoDB 是**表即 B+ 树**；指定索引类型两家都能做，运行时强制某个索引只有 MySQL 原生支持。

---

## 2. PostgreSQL：六种访问方法，默认 btree

`pg_am` 里 `amtype = 'i'` 的六种索引访问方法（实测输出）：

```text
 access_method |                what_for
---------------+----------------------------------------
 brin          | 块级摘要：超大且物理有序的表
 btree         | B 树家族：等值+范围+排序+前缀，默认选项
 gin           | 倒排：数组、JSONB、全文检索（多值字段）
 gist          | 通用搜索树：几何、范围、近邻
 hash          | 哈希表：只支持等值
 spgist        | 空间分区树：点、文本前缀
```

主键自动带 btree 唯一索引，`\d` 直接打出类型：

```text
Indexes:
    "idx_demo_pkey" PRIMARY KEY, btree (id)
```

### 2.1 btree 与 hash 的真实差别（实测）

同一张 2 万行表、同一列上两个索引，等值查询：

```text
-- 只有 btree
Index Scan using idx_demo_name_btree on idx_demo  (cost=0.29..8.30 rows=1)

-- 再加一个 hash 后，同一条 SQL
Index Scan using idx_demo_name_hash on idx_demo   (cost=0.00..8.02 rows=1)
```

hash 更小更便宜（8.02 < 8.30），优化器就换了索引。删掉 btree、改做范围查询，hash 完全用不上，退化为顺序扫描：

```text
Seq Scan on idx_demo  (cost=0.00..497.00 rows=9000)
  Filter: (name > 'user_19999'::text)
```

### 2.2 GIN / GiST / BRIN 各管一段

```text
-- GIN 服务数组包含
Bitmap Index Scan on idx_demo_tags_gin
  Index Cond: (tags @> '{tag3}'::text[])

-- GiST 服务范围重叠
Bitmap Index Scan on idx_demo_period_gist
  Index Cond: (period && '[1000,1010)'::int4range)

-- BRIN 在 2 万行小表上不被选（顺序扫描本来便宜）
Seq Scan on idx_demo  (cost=0.00..547.00 rows=221)
```

关掉 `enable_seqscan` 后 BRIN 被“逼”出来，代价反而更高，而且块级摘要只能定位到页、命中是 lossy 的，要逐行 recheck：

```text
Bitmap Heap Scan on idx_demo  (cost=12.12..559.12 rows=221)
  Rows Removed by Index Recheck: 19835
  Heap Blocks: lossy=247
  ->  Bitmap Index Scan on idx_demo_created_brin
```

### 2.3 PostgreSQL 的“覆盖索引”

PG 的索引项只存键 + TID，默认必须回表。要免回表得用 `INCLUDE` 造 Index Only Scan：

```sql
CREATE INDEX idx_name_inc ON student (name) INCLUDE (dept_name);
```

但 Index Only Scan 依赖可见性映射（visibility map）里的 all-visible 位，表刚大批量写过、没 VACUUM 时依然会去堆里取行。MySQL 的“覆盖索引”没有这个附加条件。

---

## 3. MySQL（InnoDB）：表就是一棵 B+ 树

`SHOW CREATE TABLE` 与 `SHOW INDEX` 的实测输出：

```text
CREATE TABLE `up_idx_demo` (
  `id` int NOT NULL,
  `name` varchar(32) NOT NULL,
  `tag` varchar(16) DEFAULT NULL,
  `created` date NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB

Key_name | Column_name | Index_type
PRIMARY  | id          | BTREE
idx_name | name        | BTREE
```

### 3.1 聚簇索引：主键即数据

InnoDB 没有“堆表”概念：整张表按主键组织成一棵 B+ 树，叶子页就是数据行本身。

- 按主键范围扫描 = 沿着叶子链表顺序读，几乎没有回表成本；
- 没有主键时，InnoDB 选第一个“非空唯一索引”当聚簇键，都没有就加一个隐藏的 6 字节 `DB_ROW_ID`。

### 3.2 二级索引要回表（实测）

二级索引叶子存的是“索引列 + 主键值”，所以：

```text
-- 只查索引里有的列：Using index（覆盖索引，不回表）
EXPLAIN SELECT id, name FROM up_idx_demo WHERE name = 'user_19999';
... type=ref  key=idx_name  Extra=Using index

-- SELECT * 要其它列：没有 Using index，必须拿主键回聚簇索引取整行
EXPLAIN SELECT * FROM up_idx_demo WHERE name = 'user_19999';
... type=ref  key=idx_name  Extra=NULL
```

### 3.3 MySQL 也有别的索引结构，但不在 InnoDB 的 BTREE 上

```text
-- 在 InnoDB 上写 USING HASH：不报错，但被静默建成 BTREE
CREATE INDEX idx_hash_attempt ON up_idx_demo (name) USING HASH;
SHOW INDEX ... -> idx_hash_attempt  Index_type = BTREE

-- 只有 MEMORY 引擎真的给 HASH
CREATE TABLE mem_demo (id INT, KEY idx_id (id) USING HASH) ENGINE=MEMORY;
SHOW INDEX ... -> Index_type = HASH

-- FULLTEXT 是倒排结构，不是 B+ 树
ALTER TABLE up_idx_demo ADD FULLTEXT INDEX ft_name (name);
EXPLAIN ... WHERE MATCH(name) AGAINST('user_19999')  -> type=fulltext
```

另外 InnoDB 支持 `SPATIAL INDEX`（R 树，几何类型专用）。MySQL 8.0 还有函数索引、降序索引、不可见索引、JSON 多值索引。

### 3.4 页数证据（`mysql.innodb_index_stats`，单位 16KB 页）

```text
index_name  stat_name     stat_value
PRIMARY     n_leaf_pages  671
PRIMARY     size          801
idx_name    n_leaf_pages  258
idx_name    size          353
```

20 万行：主键索引（叶子存整行、列更宽）叶子页 671，二级索引（只存 name + 主键）叶子页 258——正好对上“聚簇 + 二级索引叶子存主键”的结构。

---

## 4. “指定索引”其实有两个含义

### 4.1 指定索引的**类型**（结构）

PostgreSQL 可以真正换六种结构：

```sql
CREATE INDEX idx_tags   ON t USING gin  (tags);        -- 数组/JSONB/全文
CREATE INDEX idx_period ON t USING gist (period);      -- 范围/几何
CREATE INDEX idx_name   ON t USING hash (name);        -- 只等值
CREATE INDEX idx_ts     ON t USING brin (ts);          -- 超大表
```

MySQL 语法上也能写，但受引擎限制：

```sql
CREATE INDEX idx_name ON t (name) USING BTREE;   -- InnoDB 实际上只有这一种
CREATE TABLE m (id INT, KEY (id) USING HASH) ENGINE=MEMORY;  -- 真 HASH
ALTER TABLE t ADD FULLTEXT INDEX ft (body);      -- 倒排
ALTER TABLE t ADD SPATIAL INDEX sp (geom);       -- R 树
```

### 4.2 指定**用哪个具体索引**（执行计划）

**PostgreSQL：原生不支持，写 hint 注释等于写注释。**

```sql
-- 实测：这条 hint 被当注释丢掉，计划仍是 Seq Scan
EXPLAIN SELECT /*+ IndexScan(idx_demo idx_demo_created_brin) */
       count(*) FROM idx_demo WHERE created BETWEEN DATE '2024-03-01' AND DATE '2024-03-03';
```

三条可走的路：

1. 官方 GUC 变通（只是提高某类计划的代价，不保证选中）：

   ```sql
   SET enable_seqscan = off;      -- 逼着用索引
   SET enable_bitmapscan = off;   -- 关掉位图扫描
   ```

   本机可用的开关：`enable_seqscan / enable_indexscan / enable_bitmapscan / enable_indexonlyscan / enable_tidscan`。

2. 装扩展 `pg_hint_plan`（本机未安装，`pg_available_extensions` 里没有）：

   ```sql
   /*+ IndexScan(t idx_name) */   -- 该扩展装上后才生效
   ```

3. 结构性手段（长期正解）：更新统计信息（`ANALYZE`、`ALTER COLUMN SET STATISTICS`、`CREATE STATISTICS`）、调整 `random_page_cost / effective_cache_size`、把不需要的索引删掉、把查询改成能走索引的写法。

PG 不给 hint 是设计选择：优化器是纯代价模型，官方立场是“需要 hint 说明统计信息或索引设计有问题”。

**MySQL：原生支持，但力度分三档。**

```sql
EXPLAIN SELECT * FROM up_idx_demo WHERE name LIKE 'user_1%';
--  type=ALL：优化器嫌回表太贵，全表扫

EXPLAIN SELECT * FROM up_idx_demo USE INDEX (idx_name) WHERE name LIKE 'user_1%';
--  type=ALL：USE INDEX 只是“建议”，优化器可以不理

EXPLAIN SELECT * FROM up_idx_demo FORCE INDEX (idx_name) WHERE name LIKE 'user_1%';
--  type=range key=idx_name：强制生效

EXPLAIN SELECT /*+ INDEX(up_idx_demo idx_name) */ * FROM up_idx_demo WHERE name LIKE 'user_1%';
--  8.0 优化器 hint，效果同 FORCE INDEX
```

`IGNORE INDEX (idx)` 则是反向排除；三者都能对 `JOIN` 里的每张表分别写。

### 4.3 强制索引的代价（实测）

同一张 20 万行表，`name LIKE 'user_1%'` 命中约 11.1 万行：

```text
-- 优化器自己选全表扫
-> Filter: (name like 'user_1%')  (cost=20750 rows=99744) (actual time=1.01..181 rows=111111)
   -> Table scan on up_idx_demo  (actual time=1.01..155 rows=200000)

-- FORCE INDEX 后
-> Index range scan on up_idx_demo using idx_name ... with index condition
   (cost=2.21 rows=1) (actual time=0.0584..288 rows=111111)
```

强制后估算行数 1（实际 111111），执行 288 ms，比全表扫的 181 ms 慢约六成：低选择率时走二级索引 = 大量随机回表。行数估算从 99744 崩到 1，也说明强制索引会同时破坏优化器对后续 JOIN/排序的成本判断。

---

## 5. 实践建议

1. 正常业务不要写索引 hint / FORCE INDEX。先查统计信息是否新鲜、索引是否可覆盖、SQL 是否可以改写。
2. 真要强制，优先临时救火并留注释说明原因；MySQL 的 `FORCE INDEX` 在统计信息失真、参数化查询计划漂移时最有用。
3. PG 侧遇到“选错索引”，先 `EXPLAIN (ANALYZE, BUFFERS)` 看估算行数与实际行数差多少；差一个数量级以上先 `ANALYZE` 或补 `CREATE STATISTICS`。
4. 索引不是越多越好：每个索引都要写放大（INSERT/UPDATE 维护 B 树），MySQL 二级索引还多一份主键拷贝。
5. 选型记忆点：
   - 等值 + 范围 + 排序 → B 树/B+ 树（默认）；
   - 数组 / JSONB / 全文 → GIN / FULLTEXT；
   - 几何、范围类型、近邻 → GiST / SPATIAL；
   - 超大 append-only 表按时间范围 → BRIN；
   - 等值且内存表 → HASH（MySQL 仅 MEMORY）。
