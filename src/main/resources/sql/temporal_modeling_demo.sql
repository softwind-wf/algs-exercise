-- ============================================================
-- 双时态(bitemporal)建模：三版对比 —— 可实跑、可重复运行的演示
-- 数据库: university (PostgreSQL 17)
-- 主题: 有效时间(valid time，业务世界何时为真)
--       × 事务时间(transaction time，数据库何时开始相信这条事实)
-- 对比:
--   版 A  emp_current        —— 只存当前值，没有历史
--   版 B  salary_valid_only  —— 只有有效期(单条时间线)，有历史但没有"认知史"
--   版 C  temporal_salary    —— 双时态：valid range × tx range，排除约束保证不变量
-- ------------------------------------------------------------
-- 这一节在讲什么：
--   1. 工资只是"当前值"时，问"2022-06-01 的工资是多少"只能拿到现在的数，
--      问题本身都无从回答。
--   2. 加上 valid time(有效期)后，可以问"2022-06-01 的工资是多少"，
--      但只有一条时间线：无法回答"2023-07-15 那天，系统认为 2022-06-01 的工资是多少"。
--      而且一旦追溯更正，旧值被覆盖，不可恢复。
--   3. 双时态给每一行再加一条 tx 时间线：同一段 valid 可以有多个"认知版本"，
--      追溯更正是"关闭旧版本 + 插入新版本"，历史认知全部保留。
--      核心约束 no_overlap_per_tx：
--        EXCLUDE USING gist (emp_id WITH =, valid WITH &&, tx WITH &&)
--      它的语义是"所有列对同时冲突才拒绝"：
--        同一 emp_id 且 valid 重叠 且 tx 重叠 -> 拒绝(同一时刻不可能有两个工资)；
--        同一 valid 的不同 tx 版本 -> 不冲突(这正是版本历史)。
--   4. SQL:2011 的 UPDATE/DELETE ... FOR PORTION OF ... TIME 在 PostgreSQL 17
--      没有实现，所以这里用四个 PL/pgSQL 函数手工实现应用时间语义：
--        temporal_salary_append          插入新事实
--        temporal_salary_update_portion  按有效区间改值(切开旧行，插入新值)
--        temporal_salary_delete_portion  按有效区间删除(只保留左右残余)
--        temporal_salary_set_from        从某日起改值(便捷操作)
--      事务时间 p_tx 一律由调用方显式传入，便于复现、也便于演示"事务时钟单调"。
--   5. 脚本按 7 个业务事件写入，每步之后打印"当前信念"和两条不变量自检；
--      最后给出当前值/as-of valid/as-of tx/双时态矩阵/版本史/冲突拒绝。
--
-- 运行(在仓库根目录):
--   .\psql.bat -f src\main\resources\sql\temporal_modeling_demo.sql
-- 注意: 脚本开头 DROP、结尾也 DROP，跑完不留表，可以反复运行。
--       只有"故意演示约束拒绝"的一小段临时关闭 ON_ERROR_STOP，
--       演示完立刻恢复，所以整体退出码仍为 0。
--
-- 实测结论(PostgreSQL 17.11): 见文件末尾注释。
-- ============================================================


\set ON_ERROR_STOP on

-- 让可重复运行时 DROP IF EXISTS 的 NOTICE 不出现在输出里，保持 raw 干净。
SET client_min_messages = warning;
-- 服务器消息统一用英文(lc_messages=C): 输出文件全 ASCII，报错原文跨环境可复现。
SET lc_messages = 'C';


-- ============================================================
-- 步骤 0：清理旧对象(保证脚本可重复运行)
-- ============================================================
DROP TABLE IF EXISTS temporal_salary;
DROP TABLE IF EXISTS salary_valid_only;
DROP TABLE IF EXISTS emp_current;
DROP FUNCTION IF EXISTS temporal_salary_set_from(timestamp, text, int, date);
DROP FUNCTION IF EXISTS temporal_salary_delete_portion(timestamp, text, date, date);
DROP FUNCTION IF EXISTS temporal_salary_update_portion(timestamp, text, int, date, date);
DROP FUNCTION IF EXISTS temporal_salary_append(timestamp, text, int, date, date);

\echo ''
\echo '=================================================================='
\echo '==== [VERSION A] only current values: emp_current              ===='
\echo '=================================================================='


-- ============================================================
-- 版 A：只存当前值
--   表里只有 (emp_id, salary)，没有时间维度。
--   问题：想查"2022-06-01 的工资"，表里没有能回答这个问题的列。
-- ============================================================
CREATE TABLE emp_current (
    emp_id text PRIMARY KEY,
    salary int
);

INSERT INTO emp_current (emp_id, salary) VALUES
    ('E1001', 18000),
    ('E1002', 10000);

-- 这一条回答什么问题：表里现在有什么？(只有当前值，历史已丢失)
SELECT emp_id, salary FROM emp_current ORDER BY emp_id;

-- 这一条回答什么问题：想查"2022-06-01 的工资"。
--   没有 valid/tx 列，只能返回"现在"的 18000 —— 问题无法被正确回答。
SELECT 'version A: asking for salary on 2022-06-01 can only return the current row' AS note;
SELECT emp_id, salary FROM emp_current WHERE emp_id = 'E1001';


\echo ''
\echo '=================================================================='
\echo '==== [VERSION B] valid time only: salary_valid_only             ===='
\echo '=================================================================='


-- ============================================================
-- 版 B：只有有效期(valid time)，单条时间线
--   排除约束保证同一 emp 的有效期两两不重叠：
--     同一 emp_id 且 valid 重叠 -> 拒绝。
--   这里按"张伟涨薪史"的最终形式写入(张伟 = E1001)：
--     12000 [2021-01-01, 2022-04-01)
--     14000 [2022-04-01, 2024-01-01)
--     18000 [2024-01-01, ∞)
--   btree_gist 让 text 的 = 也能进 GiST 索引(范围类型自带的 && 不需要扩展)。
-- ============================================================
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE salary_valid_only (
    emp_id text NOT NULL,
    salary int NOT NULL,
    valid  daterange NOT NULL,
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- 同一员工的有效期不允许重叠
    CONSTRAINT salary_valid_only_no_overlap
        EXCLUDE USING gist (emp_id WITH =, valid WITH &&)
);

INSERT INTO salary_valid_only (emp_id, salary, valid) VALUES
    ('E1001', 12000, daterange('2021-01-01', '2022-04-01')),
    ('E1001', 14000, daterange('2022-04-01', '2024-01-01')),
    ('E1001', 18000, daterange('2024-01-01', NULL)),
    ('E1002', 10000, daterange('2021-07-01', NULL));

-- 这一条回答什么问题：版 B 里 2022-06-01 的工资是多少？(能回答：14000)
SELECT 'version B: salary on 2022-06-01 is answerable' AS note;
SELECT emp_id, salary, valid FROM salary_valid_only
WHERE emp_id = 'E1001' AND valid @> date '2022-06-01';

-- 这一条回答什么问题：2023-07-15 那天，系统认为 2022-06-01 的工资是多少？
--   版 B 只有一条 valid 时间线，没有 tx 列 —— 这个问题根本没有可比较的维度，
--   任何时刻查到的都是同一个 14000，无法区分"当时以为"和"后来更正"。
SELECT 'version B: cannot answer what the system believed on 2023-07-15 (no tx timeline)' AS note;
SELECT emp_id, salary, valid FROM salary_valid_only
WHERE emp_id = 'E1001' AND valid @> date '2022-06-01';

-- 追溯更正演示：HR 后来发现 2022 年那次涨薪录错了，应改成 14500。
--   版 B 只能原地 UPDATE —— 旧的 14000 被覆盖，没有地方保留"曾经认为是 14000"。
UPDATE salary_valid_only
SET salary = 14500
WHERE emp_id = 'E1001' AND valid @> date '2022-06-01';

-- 这一条回答什么问题：更正之后，还能查到更正前的旧值 14000 吗？(不能)
SELECT 'version B: retroactive correction overwrites the old value, 14000 is unrecoverable' AS note;
SELECT emp_id, salary, valid FROM salary_valid_only
WHERE emp_id = 'E1001' ORDER BY lower(valid);


\echo ''
\echo '=================================================================='
\echo '==== [VERSION C] bitemporal table: temporal_salary              ===='
\echo '=================================================================='


-- ============================================================
-- 版 C：双时态表
--   valid = 有效时间(业务上何时为真)，daterange
--   tx    = 事务时间(数据库何时开始相信)，tsrange
--   一行 = "某员工 在 valid 这段时间内 工资为 salary，
--           这条认知从 tx 下界开始被数据库相信，直到 tx 上界为止"。
--   tx 上界 = ∞ 表示"当前信念"(current belief)。
--   tx 上界有限 = 已被后续更正关闭的历史认知。
--
--   校验约束：
--     valid 非空，且下界 < 上界；无上界时 upper(valid) 为 NULL，
--       用 upper_inf(valid) 作为例外(此时只要求下界存在)。
--     tx 同理。
--
--   核心约束 no_overlap_per_tx：
--     EXCLUDE USING gist (emp_id WITH =, valid WITH &&, tx WITH &&)
--   PostgreSQL 的排除约束是"所有列对的条件同时成立才禁止"，即
--     同一 emp_id 且 valid 重叠 且 tx 重叠 -> 拒绝；
--   所以"同一 valid 的不同 tx 版本"不会冲突 —— 那正是我们要保留的版本历史。
--   (约束靠 GiST 索引实现；text 的 = 需要 btree_gist，范围类型的 && 内建。)
-- ============================================================
CREATE TABLE temporal_salary (
    emp_id text NOT NULL,
    salary int NOT NULL,
    valid  daterange NOT NULL,
    tx     tsrange NOT NULL,
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    CONSTRAINT temporal_salary_valid_not_empty
        CHECK (NOT isempty(valid)),
    CONSTRAINT temporal_salary_valid_bounds
        CHECK (lower(valid) IS NOT NULL
               AND (upper_inf(valid) OR lower(valid) < upper(valid))),
    CONSTRAINT temporal_salary_tx_not_empty
        CHECK (NOT isempty(tx)),
    CONSTRAINT temporal_salary_tx_bounds
        CHECK (lower(tx) IS NOT NULL
               AND (upper_inf(tx) OR lower(tx) < upper(tx))),
    -- 同一员工、同一段 valid、同一段 tx 不允许有两个工资；
    -- 同一 valid 的不同 tx 版本是合法的(它们 tx 不重叠)。
    CONSTRAINT no_overlap_per_tx
        EXCLUDE USING gist (emp_id WITH =, valid WITH &&, tx WITH &&)
);

-- 查询建议索引：按员工查有效期(排除约束的 GiST 索引不能直接用于这种查询)
CREATE INDEX temporal_salary_emp_valid_idx ON temporal_salary (emp_id, valid);


-- ------------------------------------------------------------
-- 函数 1：temporal_salary_append
--   追加一条新事实：valid = [p_from, p_until 或 ∞)，tx = [p_tx, ∞)。
--   p_until 给 NULL 表示有效期无上界。
--   如果与当前信念(同一 emp 且 upper_inf(tx))的 valid 重叠，
--   不在这里预查，直接让 no_overlap_per_tx 报错 —— 让数据库当裁判。
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION temporal_salary_append(
    p_tx     timestamp,
    p_emp_id text,
    p_salary int,
    p_from   date,
    p_until  date DEFAULT NULL
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_last_tx timestamp;
BEGIN
    -- 事务时钟单调：新事务时间必须严格大于表里已有的最大事务时间，
    -- 否则等于"回到过去写库"，会篡改历史认知。
    SELECT max(lower(tx)) INTO v_last_tx FROM temporal_salary;
    IF v_last_tx IS NOT NULL AND p_tx <= v_last_tx THEN
        RAISE EXCEPTION 'tx clock must move forward: p_tx=% must be > latest tx=%',
            p_tx, v_last_tx;
    END IF;
    IF p_from IS NULL THEN
        RAISE EXCEPTION 'p_from must not be null';
    END IF;
    IF p_until IS NOT NULL AND p_until <= p_from THEN
        RAISE EXCEPTION 'bad valid range: lower=% must be < upper=%', p_from, p_until;
    END IF;

    INSERT INTO temporal_salary (emp_id, salary, valid, tx)
    VALUES (p_emp_id, p_salary, daterange(p_from, p_until), tsrange(p_tx, NULL));
END;
$$;


-- ------------------------------------------------------------
-- 函数 2：temporal_salary_update_portion
--   SQL:2011 的 UPDATE ... FOR PORTION OF valid TIME FROM p_from TO p_until
--   语义：只改 [p_from, p_until) 这段有效时间上的值，区间外保持旧值。
--   做法(对每条当前信念中与区间重叠的行 r)：
--     1) 关闭旧行：tx = [lower(tx), p_tx)
--     2) 左残余 valid = [r.lower, p_from)   用旧值，tx = [p_tx, ∞)
--     3) 右残余 valid = [p_until, r.upper) 用旧值，tx = [p_tx, ∞)
--        (p_until 为 NULL 或 r.upper 无界时没有右残余)
--     4) 重叠部分 valid = [greatest(r.lower,p_from), least(r.upper,p_until))
--        用新值，tx = [p_tx, ∞)
--   PostgreSQL 17 没有 FOR PORTION OF，这段全部手工实现。
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION temporal_salary_update_portion(
    p_tx     timestamp,
    p_emp_id text,
    p_salary int,
    p_from   date,
    p_until  date
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_last_tx timestamp;
    v_rows    temporal_salary[];   -- 先把要处理的行抓成数组，避免边遍历边插入
    r         temporal_salary;
    v_lo      date;
    v_hi      date;
    v_ov_lo   date;
    v_ov_hi   date;
BEGIN
    SELECT max(lower(tx)) INTO v_last_tx FROM temporal_salary;
    IF v_last_tx IS NOT NULL AND p_tx <= v_last_tx THEN
        RAISE EXCEPTION 'tx clock must move forward: p_tx=% must be > latest tx=%',
            p_tx, v_last_tx;
    END IF;
    IF p_from IS NULL THEN
        RAISE EXCEPTION 'p_from must not be null';
    END IF;
    IF p_until IS NOT NULL AND p_until <= p_from THEN
        RAISE EXCEPTION 'bad valid range: lower=% must be < upper=%', p_from, p_until;
    END IF;

    -- 只要"当前信念"(upper_inf(tx))里与目标区间重叠的行
    v_rows := ARRAY(
        SELECT t FROM temporal_salary t
        WHERE t.emp_id = p_emp_id
          AND upper_inf(t.tx)
          AND t.valid && daterange(p_from, p_until)
        ORDER BY lower(t.valid)
    );
    IF cardinality(v_rows) = 0 THEN
        RAISE NOTICE 'update_portion: no current belief row of % covers [%,%) - nothing changed',
            p_emp_id, p_from, COALESCE(p_until::text, 'infinity');
        RETURN;
    END IF;

    FOREACH r IN ARRAY v_rows LOOP
        -- 1) 关闭旧认知：上界改成 p_tx(半开区间，p_tx 本身不包含)
        UPDATE temporal_salary SET tx = tsrange(lower(tx), p_tx) WHERE id = r.id;

        v_lo := lower(r.valid);
        v_hi := upper(r.valid);   -- NULL 表示无上界

        -- 2) 左残余：老值
        IF v_lo < p_from THEN
            INSERT INTO temporal_salary (emp_id, salary, valid, tx)
            VALUES (p_emp_id, r.salary, daterange(v_lo, p_from), tsrange(p_tx, NULL));
        END IF;

        -- 3) 右残余：老值(p_until 为 NULL 时没有右残余)
        IF p_until IS NOT NULL AND (v_hi IS NULL OR v_hi > p_until) THEN
            INSERT INTO temporal_salary (emp_id, salary, valid, tx)
            VALUES (p_emp_id, r.salary, daterange(p_until, v_hi), tsrange(p_tx, NULL));
        END IF;

        -- 4) 重叠部分：新值
        --    greatest 处理 r 起点在 p_from 之后的情况；
        --    least 会自动忽略 NULL(p_until 为 NULL 或 v_hi 无界时得到有界的另一端)。
        v_ov_lo := greatest(v_lo, p_from);
        v_ov_hi := least(v_hi, p_until);
        INSERT INTO temporal_salary (emp_id, salary, valid, tx)
        VALUES (p_emp_id, p_salary, daterange(v_ov_lo, v_ov_hi), tsrange(p_tx, NULL));
    END LOOP;
END;
$$;


-- ------------------------------------------------------------
-- 函数 3：temporal_salary_delete_portion
--   SQL:2011 的 DELETE ... FOR PORTION OF valid TIME FROM p_from TO p_until
--   语义：把 [p_from, p_until) 这段有效时间从历史里删掉，
--         区间外的左右残余仍然作为当前信念保留。
--   做法与 update_portion 相同，只是不插入"重叠部分"。
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION temporal_salary_delete_portion(
    p_tx     timestamp,
    p_emp_id text,
    p_from   date,
    p_until  date
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_last_tx timestamp;
    v_rows    temporal_salary[];
    r         temporal_salary;
    v_lo      date;
    v_hi      date;
BEGIN
    SELECT max(lower(tx)) INTO v_last_tx FROM temporal_salary;
    IF v_last_tx IS NOT NULL AND p_tx <= v_last_tx THEN
        RAISE EXCEPTION 'tx clock must move forward: p_tx=% must be > latest tx=%',
            p_tx, v_last_tx;
    END IF;
    IF p_from IS NULL THEN
        RAISE EXCEPTION 'p_from must not be null';
    END IF;
    IF p_until IS NOT NULL AND p_until <= p_from THEN
        RAISE EXCEPTION 'bad valid range: lower=% must be < upper=%', p_from, p_until;
    END IF;

    v_rows := ARRAY(
        SELECT t FROM temporal_salary t
        WHERE t.emp_id = p_emp_id
          AND upper_inf(t.tx)
          AND t.valid && daterange(p_from, p_until)
        ORDER BY lower(t.valid)
    );
    IF cardinality(v_rows) = 0 THEN
        RAISE NOTICE 'delete_portion: no current belief row of % covers [%,%) - nothing changed',
            p_emp_id, p_from, COALESCE(p_until::text, 'infinity');
        RETURN;
    END IF;

    FOREACH r IN ARRAY v_rows LOOP
        -- 1) 关闭旧认知
        UPDATE temporal_salary SET tx = tsrange(lower(tx), p_tx) WHERE id = r.id;

        v_lo := lower(r.valid);
        v_hi := upper(r.valid);

        -- 2) 左残余：老值
        IF v_lo < p_from THEN
            INSERT INTO temporal_salary (emp_id, salary, valid, tx)
            VALUES (p_emp_id, r.salary, daterange(v_lo, p_from), tsrange(p_tx, NULL));
        END IF;

        -- 3) 右残余：老值
        IF p_until IS NOT NULL AND (v_hi IS NULL OR v_hi > p_until) THEN
            INSERT INTO temporal_salary (emp_id, salary, valid, tx)
            VALUES (p_emp_id, r.salary, daterange(p_until, v_hi), tsrange(p_tx, NULL));
        END IF;

        -- 4) 重叠部分不插入任何行 —— 这段有效时间在"当前信念"里消失了
    END LOOP;
END;
$$;


-- ------------------------------------------------------------
-- 函数 4：temporal_salary_set_from
--   便捷操作："从 p_from 起工资改成 p_salary"。
--   在当前信念里找覆盖 p_from 的那一行，用它自己的上界作为截止，
--   然后调用 update_portion([p_from, 该行上界))。
--   所以它只能改到"已知有效期的尽头"：如果补录一笔更早的涨薪，
--   它会被夹在覆盖 p_from 的那行的上界处，不会影响之后的区间。
--   找不到覆盖行则报错。
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION temporal_salary_set_from(
    p_tx     timestamp,
    p_emp_id text,
    p_salary int,
    p_from   date
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_last_tx timestamp;
    v_valid   daterange;
BEGIN
    SELECT max(lower(tx)) INTO v_last_tx FROM temporal_salary;
    IF v_last_tx IS NOT NULL AND p_tx <= v_last_tx THEN
        RAISE EXCEPTION 'tx clock must move forward: p_tx=% must be > latest tx=%',
            p_tx, v_last_tx;
    END IF;
    IF p_from IS NULL THEN
        RAISE EXCEPTION 'p_from must not be null';
    END IF;

    -- 找覆盖 p_from 的当前信念行
    SELECT t.valid INTO v_valid
    FROM temporal_salary t
    WHERE t.emp_id = p_emp_id
      AND upper_inf(t.tx)
      AND t.valid @> p_from
    ORDER BY lower(t.valid) DESC
    LIMIT 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'set_from: no current belief row of % covers %', p_emp_id, p_from;
    END IF;

    -- upper(v_valid) 为 NULL(无上界)时，update_portion 会改到无穷
    PERFORM temporal_salary_update_portion(
        p_tx, p_emp_id, p_salary, p_from, upper(v_valid));
END;
$$;


-- ============================================================
-- 7 个业务事件
--   每一步之后打印两样东西：
--     1) 当前信念(所有 upper_inf(tx) 的行)
--     2) 不变量自检(两个计数都应为 0)
--   自检 1：当前信念里同一 emp 的 valid 两两不重叠
--   自检 2：全表同一 emp、valid 重叠且 tx 重叠的行对(由排除约束保证)
-- ============================================================

\echo ''
\echo '=================================================================='
\echo '==== [EVENT 1] append E1001 12000 valid=[2021-01-01,inf) tx=2021-01-01'
\echo '==== Zhang Wei (E1001) joins the company ===='
\echo '=================================================================='
SELECT temporal_salary_append(timestamp '2021-01-01', 'E1001', 12000, date '2021-01-01');

SELECT 'current belief after event 1' AS section;
SELECT id, emp_id, salary, valid, tx FROM temporal_salary
WHERE upper_inf(tx) ORDER BY emp_id, lower(valid);

-- 不变量自检(两个计数都应为 0)
SELECT
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND upper_inf(a.tx) AND upper_inf(b.tx)
        AND a.valid && b.valid) AS current_belief_valid_overlaps,
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND a.valid && b.valid
        AND a.tx && b.tx) AS any_version_overlaps;


\echo ''
\echo '=================================================================='
\echo '==== [EVENT 2] append E1002 10000 valid=[2021-07-01,inf) tx=2021-07-01'
\echo '==== Li Na (E1002) joins the company ===='
\echo '=================================================================='
SELECT temporal_salary_append(timestamp '2021-07-01', 'E1002', 10000, date '2021-07-01');

SELECT 'current belief after event 2' AS section;
SELECT id, emp_id, salary, valid, tx FROM temporal_salary
WHERE upper_inf(tx) ORDER BY emp_id, lower(valid);

SELECT
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND upper_inf(a.tx) AND upper_inf(b.tx)
        AND a.valid && b.valid) AS current_belief_valid_overlaps,
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND a.valid && b.valid
        AND a.tx && b.tx) AS any_version_overlaps;


\echo ''
\echo '=================================================================='
\echo '==== [EVENT 3] set_from E1001 14000 from 2022-04-01 tx=2022-04-01'
\echo '==== salary raise ===='
\echo '=================================================================='
SELECT temporal_salary_set_from(timestamp '2022-04-01', 'E1001', 14000, date '2022-04-01');

SELECT 'current belief after event 3' AS section;
SELECT id, emp_id, salary, valid, tx FROM temporal_salary
WHERE upper_inf(tx) ORDER BY emp_id, lower(valid);

SELECT
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND upper_inf(a.tx) AND upper_inf(b.tx)
        AND a.valid && b.valid) AS current_belief_valid_overlaps,
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND a.valid && b.valid
        AND a.tx && b.tx) AS any_version_overlaps;


\echo ''
\echo '=================================================================='
\echo '==== [EVENT 4] set_from E1001 18000 from 2024-01-01 tx=2024-01-05'
\echo '==== salary raise ===='
\echo '=================================================================='
SELECT temporal_salary_set_from(timestamp '2024-01-05', 'E1001', 18000, date '2024-01-01');

SELECT 'current belief after event 4' AS section;
SELECT id, emp_id, salary, valid, tx FROM temporal_salary
WHERE upper_inf(tx) ORDER BY emp_id, lower(valid);

SELECT
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND upper_inf(a.tx) AND upper_inf(b.tx)
        AND a.valid && b.valid) AS current_belief_valid_overlaps,
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND a.valid && b.valid
        AND a.tx && b.tx) AS any_version_overlaps;


\echo ''
\echo '=================================================================='
\echo '==== [EVENT 5] set_from E1001 16000 from 2023-03-01 tx=2024-05-10'
\echo '==== HR back-dated raise: set_from clips at the covering row upper bound 2024-01-01 ===='
\echo '=================================================================='
SELECT temporal_salary_set_from(timestamp '2024-05-10', 'E1001', 16000, date '2023-03-01');

SELECT 'current belief after event 5' AS section;
SELECT id, emp_id, salary, valid, tx FROM temporal_salary
WHERE upper_inf(tx) ORDER BY emp_id, lower(valid);

SELECT
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND upper_inf(a.tx) AND upper_inf(b.tx)
        AND a.valid && b.valid) AS current_belief_valid_overlaps,
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND a.valid && b.valid
        AND a.tx && b.tx) AS any_version_overlaps;


\echo ''
\echo '=================================================================='
\echo '==== [EVENT 6] update_portion E1001 14500 [2022-04-01,2023-03-01) tx=2024-06-01'
\echo '==== correct the wrong entry of the 2022 raise ===='
\echo '=================================================================='
SELECT temporal_salary_update_portion(
    timestamp '2024-06-01', 'E1001', 14500, date '2022-04-01', date '2023-03-01');

SELECT 'current belief after event 6' AS section;
SELECT id, emp_id, salary, valid, tx FROM temporal_salary
WHERE upper_inf(tx) ORDER BY emp_id, lower(valid);

SELECT
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND upper_inf(a.tx) AND upper_inf(b.tx)
        AND a.valid && b.valid) AS current_belief_valid_overlaps,
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND a.valid && b.valid
        AND a.tx && b.tx) AS any_version_overlaps;


\echo ''
\echo '=================================================================='
\echo '==== [EVENT 7] delete_portion E1002 [2021-07-01,2021-08-01) tx=2024-07-01'
\echo '==== correct Li Na hire date: delete the wrongly entered first segment ===='
\echo '=================================================================='
SELECT temporal_salary_delete_portion(
    timestamp '2024-07-01', 'E1002', date '2021-07-01', date '2021-08-01');

SELECT 'current belief after event 7' AS section;
SELECT id, emp_id, salary, valid, tx FROM temporal_salary
WHERE upper_inf(tx) ORDER BY emp_id, lower(valid);

SELECT
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND upper_inf(a.tx) AND upper_inf(b.tx)
        AND a.valid && b.valid) AS current_belief_valid_overlaps,
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND a.valid && b.valid
        AND a.tx && b.tx) AS any_version_overlaps;


-- ============================================================
-- 查询展示：双时态到底能回答哪些单时态回答不了的问题
-- ============================================================

\echo ''
\echo '=================================================================='
\echo '==== [Q1] current belief (all rows with tx upper = infinity)    ===='
\echo '=================================================================='
-- 这一条回答什么问题：现在(最新信念)每个员工的工资和有效期是什么？
SELECT id, emp_id, salary, valid, tx FROM temporal_salary
WHERE upper_inf(tx) ORDER BY emp_id, lower(valid);

-- 全表版本数(含已关闭的历史认知)：应为 E1001=8, E1002=2, 总计 10
SELECT emp_id, count(*) AS versions FROM temporal_salary
GROUP BY emp_id ORDER BY emp_id;
SELECT count(*) AS total_versions FROM temporal_salary;


\echo ''
\echo '=================================================================='
\echo '==== [Q2] single-temporal as-of VALID date (current belief)     ===='
\echo '=================================================================='
-- 这一条回答什么问题：按现在的认知，某有效日期 E1001 的工资是多少？
--   期望: 2022-06-01 -> 14500, 2023-06-01 -> 16000, 2024-06-01 -> 18000
SELECT probe.valid_on, s.salary
FROM (VALUES (date '2022-06-01'), (date '2023-06-01'), (date '2024-06-01'))
     AS probe(valid_on)
LEFT JOIN temporal_salary s
       ON s.emp_id = 'E1001'
      AND upper_inf(s.tx)
      AND s.valid @> probe.valid_on
ORDER BY probe.valid_on;


\echo ''
\echo '=================================================================='
\echo '==== [Q3] single-temporal as-of TX timestamp                    ===='
\echo '=================================================================='
-- 这一条回答什么问题：站在某个事务时刻，系统当时相信的全体工资是什么？
--   "相信" = tx @> 该时刻；期望 2023-07-15 -> 3 行, 2024-05-10 -> 5 行, 2024-06-05 -> 5 行
SELECT asof.tx_at, s.emp_id, s.salary, s.valid
FROM (VALUES (timestamp '2023-07-15'), (timestamp '2024-05-10'), (timestamp '2024-06-05'))
     AS asof(tx_at)
JOIN temporal_salary s ON s.tx @> asof.tx_at
ORDER BY asof.tx_at, s.emp_id, lower(s.valid);


\echo ''
\echo '=================================================================='
\echo '==== [Q4] bitemporal matrix: valid date x tx timestamp          ===='
\echo '=================================================================='
-- 这一条回答什么问题："某有效日期 × 某事务时刻"的系统认知是什么？
--   期望: 2022-06-01 -> 14000 / 14000 / 14500
--         2023-06-01 -> 14000 / 16000 / 16000
--         2024-06-01 -> 14000 / 18000 / 18000
WITH probe(valid_on) AS (
    VALUES (date '2022-06-01'), (date '2023-06-01'), (date '2024-06-01')
), as_of(tx_at) AS (
    VALUES (timestamp '2023-07-15'), (timestamp '2024-05-10'), (timestamp '2024-06-05')
)
SELECT p.valid_on,
       max(s.salary) FILTER (WHERE a.tx_at = timestamp '2023-07-15') AS tx_2023_07_15,
       max(s.salary) FILTER (WHERE a.tx_at = timestamp '2024-05-10') AS tx_2024_05_10,
       max(s.salary) FILTER (WHERE a.tx_at = timestamp '2024-06-05') AS tx_2024_06_05
FROM probe p
CROSS JOIN as_of a
LEFT JOIN temporal_salary s
       ON s.emp_id = 'E1001'
      AND s.valid @> p.valid_on
      AND s.tx @> a.tx_at
GROUP BY p.valid_on
ORDER BY p.valid_on;


\echo ''
\echo '=================================================================='
\echo '==== [Q5] E1001 full version history (ordered by tx then valid) ===='
\echo '=================================================================='
-- 这一条回答什么问题：张伟的工资认知在数据库里留下了哪些版本？
--   (每次追溯更正都会关闭旧版本，所以历史版本一行都不会丢)
SELECT id, salary, valid, tx FROM temporal_salary
WHERE emp_id = 'E1001'
ORDER BY lower(tx), lower(valid), id;


\echo ''
\echo '=================================================================='
\echo '==== [Q6] E1002 hire-date bitemporal comparison                 ===='
\echo '=================================================================='
-- 这一条回答什么问题：李娜 2021-07-15 这一天是否有工资记录？
--   事务时刻 2024-06-05(更正前): 10000，入职日 2021-07-01
--   事务时刻 2024-07-05(更正后): 查不到 —— 入职日已改为 2021-08-01
SELECT asof.tx_at, s.id IS NOT NULL AS found, s.salary, s.valid
FROM (VALUES (timestamp '2024-06-05'), (timestamp '2024-07-05')) AS asof(tx_at)
LEFT JOIN temporal_salary s
       ON s.emp_id = 'E1002'
      AND s.valid @> date '2021-07-15'
      AND s.tx @> asof.tx_at
ORDER BY asof.tx_at;


\echo ''
\echo '=================================================================='
\echo '==== [Q7] invariants after all events (both counts must be 0)   ===='
\echo '=================================================================='
-- 自检 1：当前信念同一 emp 的 valid 两两不重叠
-- 自检 2：全表同一 emp、valid 重叠且 tx 重叠的行对(排除约束保证)
SELECT
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND upper_inf(a.tx) AND upper_inf(b.tx)
        AND a.valid && b.valid) AS current_belief_valid_overlaps,
    (SELECT count(*)
       FROM temporal_salary a JOIN temporal_salary b
         ON a.id < b.id AND a.emp_id = b.emp_id
        AND a.valid && b.valid
        AND a.tx && b.tx) AS any_version_overlaps;


\echo ''
\echo '=================================================================='
\echo '==== [Q8] exclusion constraint rejects a conflicting insert     ===='
\echo '==== (ON_ERROR_STOP is turned off only for this demo)           ===='
\echo '=================================================================='
-- 这一条演示什么：绕过函数直接插入一条"同一员工、valid 与当前信念重叠、
--   tx 也重叠"的行，必须被 no_overlap_per_tx 拒绝。
--   报错原文里应出现 conflicting key value violates exclusion constraint "no_overlap_per_tx"。
\set ON_ERROR_STOP off
INSERT INTO temporal_salary (emp_id, salary, valid, tx)
VALUES ('E1001', 15000, daterange('2023-01-01', '2023-06-01'), tsrange('2024-08-01', NULL));
\set ON_ERROR_STOP on

-- 这一条回答什么问题：被拒绝之后表有没有被改脏？(行数仍是 10，当前信念不变)
SELECT 'after the rejected insert the table is unchanged' AS note;
SELECT count(*) AS total_versions FROM temporal_salary;
SELECT id, emp_id, salary, valid, tx FROM temporal_salary
WHERE upper_inf(tx) ORDER BY emp_id, lower(valid);


-- ============================================================
-- 清理：跑完不留表，保证下次运行从零开始
-- ============================================================
DROP FUNCTION IF EXISTS temporal_salary_set_from(timestamp, text, int, date);
DROP FUNCTION IF EXISTS temporal_salary_delete_portion(timestamp, text, date, date);
DROP FUNCTION IF EXISTS temporal_salary_update_portion(timestamp, text, int, date, date);
DROP FUNCTION IF EXISTS temporal_salary_append(timestamp, text, int, date, date);
DROP TABLE IF EXISTS temporal_salary;
DROP TABLE IF EXISTS salary_valid_only;
DROP TABLE IF EXISTS emp_current;

\echo ''
\echo '==== [DONE] all demo objects dropped; script is repeatable ===='


-- ============================================================
-- 实测结论(PostgreSQL 17.11，Windows 11，psql 17.11)
-- ------------------------------------------------------------
-- 【运行】在仓库根目录执行:
--     $env:PGCLIENTENCODING='UTF8'
--     & .\psql.bat -f src\main\resources\sql\temporal_modeling_demo.sql *> docs\temporal-sql-raw.txt
--   docs\temporal-sql-raw.txt 为完整实跑输出。
--   脚本里 SET client_min_messages = warning 把可重复运行时 DROP IF EXISTS 的
--   "does not exist, skipping" NOTICE 屏蔽掉；SET lc_messages = 'C' 让服务器
--   消息(含报错原文)统一英文。所有打印标签/数据均为 ASCII，中文只出现在 -- 注释里，
--   所以 raw 文件是纯 ASCII(合法 UTF-8 的子集，无 BOM)，任何 IDE 打开都不会乱码。
--
-- 【退出码】连续运行两次，第一次 EXIT=0，第二次 EXIT=0(脚本可重复运行)。
--   故意演示约束拒绝的那一段临时 \set ON_ERROR_STOP off，报错只打印不终止，
--   演示完立刻恢复 on，故整体退出码仍为 0。
--   每次运行结束都 DROP 掉 3 张表 + 4 个函数，库里不留对象，可直接再跑。
--
-- 【逐项核对(与规格期望值一致)】
--   1) 当前信念 E1001 共 4 行:
--        [2021-01-01,2022-04-01) 12000
--        [2022-04-01,2023-03-01) 14500
--        [2023-03-01,2024-01-01) 16000
--        [2024-01-01,)            18000
--      E1002 共 1 行: [2021-08-01,) 10000
--   2) 全表版本数: E1001=8, E1002=2, 总计 10 行。
--   3) 当前信念按有效日期查询: 2022-06-01 -> 14500, 2023-06-01 -> 16000,
--      2024-06-01 -> 18000。
--   4) as-of 事务时刻 2023-07-15: 3 行(E1001 12000/[2021-01-01,2022-04-01)、
--      14000/[2022-04-01,∞); E1002 10000/[2021-07-01,∞))。
--   5) as-of 事务时刻 2024-05-10: 5 行(E1001 12000/14000/16000/18000 + E1002)。
--   6) as-of 事务时刻 2024-06-05: 5 行(E1001 12000/14500/16000/18000;
--      E1002 仍是旧入职日 [2021-07-01,∞) 10000)。
--   7) 双时态矩阵(valid x tx):
--        valid 2022-06-01: 14000 | 14000 | 14500
--        valid 2023-06-01: 14000 | 16000 | 16000
--        valid 2024-06-01: 14000 | 18000 | 18000
--      (tx 依次为 2023-07-15 / 2024-05-10 / 2024-06-05)
--   8) 李娜入职日双时态对照: valid 2021-07-15 在 tx 2024-06-05 查得 10000
--      (入职日 2021-07-01，found=t)，在 tx 2024-07-05 查不到(found=f，
--      该段已删，入职日 2021-08-01)。
--   9) 冲突示范: 直接插入被 no_overlap_per_tx 拒绝，报错原文为
--        ERROR:  conflicting key value violates exclusion constraint "no_overlap_per_tx"
--        DETAIL:  Key (emp_id, valid, tx)=... conflicts with existing key ...
--      被拒后全表仍是 10 行，当前信念不变。
--  10) 不变量自检(每步与结尾): current_belief_valid_overlaps=0,
--      any_version_overlaps=0。
--
-- 【版 B 追溯更正覆盖旧值的演示】
--   版 B 只有 valid 一条时间线，追溯更正只能原地 UPDATE:
--     UPDATE salary_valid_only SET salary = 14500
--      WHERE emp_id = 'E1001' AND valid @> date '2022-06-01';
--   执行后查 [2022-04-01,2024-01-01) 这一段工资变成 14500，
--   原来的 14000 被覆盖，表里没有任何地方还能查到"曾经认为是 14000"，
--   也无法回答"2023-07-15 那天系统认为 2022-06-01 的工资是多少"。
--   版 C 用同样的更正(事件 6)则把旧版本 tx 关闭、新版本另起一行，
--   于是 as-of 2023-07-15 仍能查到 14000，而当前信念是 14500 —— 两个答案同时存在。
--
-- 【实现要点】
--   · PostgreSQL 17 没有 SQL:2011 的 FOR PORTION OF，四个函数手工实现应用时间语义。
--   · 排除约束是"所有列对同时冲突才拒绝"，因此同一 valid 的不同 tx 版本合法，
--     而同一 emp 的 valid 重叠 + tx 重叠会被拒绝。
--   · 函数入口统一校验 p_tx 严格大于 max(lower(tx))(事务时钟单调)，
--     以及 p_from < p_until，避免调用方写坏历史。
--   · 每步之后跑两条不变量 SQL，全部为 0。
-- ============================================================
