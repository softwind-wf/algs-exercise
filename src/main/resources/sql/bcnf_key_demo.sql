-- ============================================================================
-- 超键/候选键最小演示 + 外键与包含依赖的本质区别
-- 库: bcnf_demo
-- 运行: .\psql.bat -d bcnf_demo -f src\main\resources\sql\bcnf_key_demo.sql
--
-- 演示三件事:
--   ① 超键: 能唯一确定一行 —— {学号} 是超键, {学号,姓名} 也是(但"过大")
--   ② 候选键: 极小超键 —— {学号};  {姓名} 不是(重名)
--   ③ 外键查得住"每一行", 查不住"跨行聚合"; 而包含依赖是集合级约束
-- ============================================================================

SET client_encoding = 'UTF8';
\set ON_ERROR_STOP on
\pset pager off

SELECT '=== 演示开始 ===' AS phase;

DROP TABLE IF EXISTS demo_diameter CASCADE;
DROP TABLE IF EXISTS demo_circle   CASCADE;
DROP TABLE IF EXISTS demo_child    CASCADE;
DROP TABLE IF EXISTS demo_parent   CASCADE;
DROP TABLE IF EXISTS demo_student  CASCADE;
DROP TABLE IF EXISTS demo_club     CASCADE;

-- ---------------------------------------------------------------------------
-- 第 1 部分: 超键 vs 候选键
-- ---------------------------------------------------------------------------
SELECT '【第 1 部分】超键与候选键(用真实数据看差别)' AS phase;

CREATE TABLE demo_student (
    sno   VARCHAR(4)  NOT NULL,
    sname VARCHAR(20) NOT NULL,
    major VARCHAR(20) NOT NULL,
    CONSTRAINT pk_demo_student PRIMARY KEY (sno)
);

INSERT INTO demo_student VALUES
    ('S1', '张三', '计算机'),
    ('S2', '李四', '计算机'),
    ('S3', '张三', '数学');        -- 重名, 这是关键

SELECT * FROM demo_student ORDER BY sno;

SELECT '【问题 1】姓名能唯一确定一行吗?(找重名 => 能查出 1 行就说明不能)' AS phase;
SELECT sname, count(*) AS 同名人数, string_agg(sno, ', ' ORDER BY sno) AS 学号
  FROM demo_student
 GROUP BY sname
HAVING count(*) > 1;

SELECT '【问题 2】学号能唯一确定一行吗?(应为 0 行)' AS phase;
SELECT sno, count(*) AS 出现次数
  FROM demo_student
 GROUP BY sno
HAVING count(*) > 1;

SELECT '【问题 3】学号 + 姓名 也能唯一确定, 但有 2 列, 不算"极小"' AS phase;
SELECT sno, sname, count(*) AS 出现次数
  FROM demo_student
 GROUP BY sno, sname
HAVING count(*) > 1;

SELECT '{学号}'         AS 属性集, '是超键(能定位到唯一一行)' AS 是不是超键, '是候选键(不能再删属性)' AS 是不是候选键
UNION ALL SELECT '{学号, 姓名}', '是超键(能定位)',                 '不是候选键(学号单独就够了 => 太大)'
UNION ALL SELECT '{姓名}',      '不是超键(张三有两人)',             '不是候选键';

-- ---------------------------------------------------------------------------
-- 第 2 部分: 外键查得住每一行, 查不住跨行聚合
-- ---------------------------------------------------------------------------
SELECT '【第 2 部分】同样的数据, 外键能拦一句、拦不住另一句' AS phase;

CREATE TABLE demo_parent (
    id INTEGER NOT NULL,
    CONSTRAINT pk_demo_parent PRIMARY KEY (id)
);
CREATE TABLE demo_child (
    id INTEGER NOT NULL,
    CONSTRAINT pk_demo_child PRIMARY KEY (id),
    CONSTRAINT fk_demo_child FOREIGN KEY (id) REFERENCES demo_parent (id)
);

INSERT INTO demo_parent VALUES (1);

-- 这两句来自同一个函数, 在同一事务里执行, 但结果完全不同
DROP FUNCTION IF EXISTS demo_insert_and_count();
CREATE FUNCTION demo_insert_and_count() RETURNS void AS $$
DECLARE
    n_parent INTEGER;
    n_child  INTEGER;
BEGIN
    INSERT INTO demo_parent VALUES (2);            -- ① 单行插入: 本语句内部先落父行,
                                                   --    再同一语句立即插子行 -> 外键校验通过
    INSERT INTO demo_child  VALUES (2);

    -- ② 父表里没有 id=3 -> 外键拒绝。用嵌套 BEGIN...EXCEPTION 当场抓住,
    --    免得异常冒泡出去、被 ON_ERROR_STOP 把整个脚本掐断(每个 EXCEPTION 块
    --    是一个子事务, 报错只回滚它自己那一条语句)。
    BEGIN
        INSERT INTO demo_child VALUES (3);
    EXCEPTION WHEN foreign_key_violation THEN
        RAISE NOTICE '② 单行插入 id=3 被外键拒绝: 父表里没有这一行';
    END;

    SELECT count(*) INTO n_parent FROM demo_parent;
    SELECT count(*) INTO n_child  FROM demo_child;
    RAISE NOTICE '父表行数=%  子表行数=%', n_parent, n_child;
END;
$$ LANGUAGE plpgsql;

SELECT demo_insert_and_count();

SELECT '父表内容' AS 表, id::text AS 值 FROM demo_parent
UNION ALL
SELECT '子表内容', id::text FROM demo_child
ORDER BY 1, 2;

SELECT '★ ① 插进去了 ② 被拒绝: 外键真的在查, 但每个 INSERT 只看自己那一行。' AS conclusion,
       '包含依赖 π_id(子集) ⊆ π_id(父集) 是"整个集合"的约束 —— 单行校验看不见集合, 所以 SQL 里它只能靠触发器或延迟约束。' AS 说明;

-- ---------------------------------------------------------------------------
-- 第 3 部分: 具体表 → 集合约束 → 落到 SQL
-- ---------------------------------------------------------------------------
SELECT '【第 3 部分】圆 / 球体: 具体表 → 抽象约束 → 具体实现' AS phase;

CREATE TABLE demo_circle (
    cid    INTEGER     NOT NULL,
    cname  VARCHAR(20) NOT NULL,
    radius NUMERIC(6,2) NOT NULL,
    CONSTRAINT pk_demo_circle PRIMARY KEY (cid)
);
CREATE TABLE demo_diameter (
    did    INTEGER     NOT NULL,
    cid    INTEGER     NOT NULL,
    length NUMERIC(6,2) NOT NULL,
    CONSTRAINT pk_demo_diameter PRIMARY KEY (did),
    CONSTRAINT fk_demo_diameter FOREIGN KEY (cid) REFERENCES demo_circle (cid)
);

INSERT INTO demo_circle   VALUES (1, '圆A', 5.0), (2, '圆B', 3.0);
INSERT INTO demo_diameter VALUES (1, 1, 10.0), (2, 2, 6.0);

SELECT '具体三张表(圆 / 直径 / 球体)' AS phase;
SELECT 'demo_circle' AS 表名, count(*) AS 行数 FROM demo_circle
UNION ALL SELECT 'demo_diameter', count(*) FROM demo_diameter;

SELECT '抽象成集合约束' AS phase;
SELECT '每张圆都有至少一条直径' AS 约束1,
       'π_cid(demo_diameter) ⊇ π_cid(demo_circle)' AS 写法1,
       '这是**包含依赖**, 不是函数依赖' AS 说明1
UNION ALL SELECT '每条直径都指向一张存在的圆',
       'π_cid(demo_diameter) ⊆ π_cid(demo_circle)',
       '这是外码约束, DDL 里写得出';

SELECT '落到 SQL: 前一半(子⊆父)有外键管, 后一半(父⊆子)谁也管不了' AS phase;

\set ON_ERROR_STOP off

-- 外键真的在拦: 插一条指向 C99 的直径
INSERT INTO demo_diameter VALUES (9, 99, 4.0);

-- 但"圆 C2 现在没有直径了"这件事, 没有任何约束会报错
DELETE FROM demo_diameter WHERE cid = 2;

\set ON_ERROR_STOP on

SELECT '删掉 C2 的直径后, 还有几条"没有直径的圆"?' AS question;
SELECT c.cid, c.cname
  FROM demo_circle c
 WHERE NOT EXISTS (SELECT 1 FROM demo_diameter d WHERE d.cid = c.cid);

SELECT '★ 外键一句 ERROR 拦下了越界插入; 而"孤儿圆"静静地留下来了。' AS conclusion,
       '要拦住它必须写触发器或可延迟约束检查(每张圆至少一条直径)。' AS 说明;

-- ---------------------------------------------------------------------------
-- 收尾
-- ---------------------------------------------------------------------------
DROP FUNCTION IF EXISTS demo_insert_and_count();
DROP TABLE IF EXISTS demo_diameter, demo_circle, demo_child, demo_parent, demo_student;

SELECT '=== 演示结束, 演示表已清理 ===' AS phase;
