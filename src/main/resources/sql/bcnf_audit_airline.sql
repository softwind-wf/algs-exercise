-- ============================================================================
-- BCNF 体检 · 实测脚本(对象: airline_demo 里你自己交付的 7.23 表结构)
-- 教材:《数据库系统概念》函数依赖与范式
--
-- 运行:
--   .\psql.bat -d airline_demo -f src\main\resources\sql\bcnf_audit_airline.sql
--
-- 本脚本只做两件事:
--   A. 把"分解后的三层设计"与"拍平的宽表"放在一起对照, 用真实数据量出冗余;
--   B. 把 BCNF 的两个关键判据(无损连接 / 保持依赖)与"候选键边界条件"
--      在真实表上跑出证据。
--
-- 副作用声明:
--   只创建 1 个临时表 + 2 张临时表(TEMP ... ON COMMIT DROP), 不改动任何交付表的数据。
--   负测试里的报错都是**设计上必须出现**的, 详见文件末尾的实测结论。
-- ============================================================================

SET client_encoding = 'UTF8';
\set ON_ERROR_STOP on
\pset pager off

SELECT '=== BCNF 体检开始 (airline_demo) ===' AS phase;


-- ============================================================================
-- 第 1 步: 数据基线
-- ============================================================================
SELECT '第 1 步: 数据基线' AS phase;

SELECT 'aircraft_type' AS tab, count(*) AS rows FROM aircraft_type
UNION ALL SELECT 'airplane',      count(*) FROM airplane
UNION ALL SELECT 'timetable',     count(*) FROM timetable
UNION ALL SELECT 'flight',        count(*) FROM flight
UNION ALL SELECT 'route_leg',     count(*) FROM route_leg
UNION ALL SELECT 'booking_segment', count(*) FROM booking_segment
ORDER BY tab;


-- ============================================================================
-- 第 2 步: 对照物 —— 把三层设计拍平成一张宽表
--   flight_wide = flight ⋈ timetable ⋈ aircraft_type ⋈ airplane
--   只 SELECT, 不建视图、不落表, 保证不改动交付对象。
-- ============================================================================
SELECT '第 2 步: flight_wide(派生对照物)内容' AS phase;

SELECT f.flight_no, f.flight_date, t.route_no, t.type_code,
       ac.seat_capacity, f.dep_time, f.arr_time, f.flight_status
  FROM flight f
  JOIN timetable t       ON t.schedule_id = f.schedule_id
  JOIN aircraft_type ac  ON ac.type_code  = t.type_code
  JOIN airplane ap       ON ap.reg_no     = f.reg_no
 ORDER BY f.flight_no, f.flight_date;

-- 宽表上候选键是否唯一? 同 (flight_no, flight_date) 出现多行就说明宽表把键弄丢了
SELECT '第 2 步: 宽表按候选键 (flight_no, flight_date) 分组检查' AS phase;

SELECT count(*)                                   AS wide_rows,
       count(DISTINCT (f.flight_no, f.flight_date)) AS distinct_keys
  FROM flight f
  JOIN timetable t      ON t.schedule_id = f.schedule_id
  JOIN aircraft_type ac ON ac.type_code  = t.type_code
  JOIN airplane ap      ON ap.reg_no     = f.reg_no;

-- 冗余度量: 非键属性 route_no / type_code / seat_capacity 在宽表里被重复了几次
SELECT '第 2 步: 冗余度量(同一个值在宽表里出现几次)' AS phase;

WITH wide AS (
    SELECT f.flight_no, f.flight_date, t.route_no, t.type_code, ac.seat_capacity
      FROM flight f
      JOIN timetable t      ON t.schedule_id = f.schedule_id
      JOIN aircraft_type ac ON ac.type_code  = t.type_code
)
SELECT route_no, type_code, seat_capacity,
       count(*)                       AS occurrences,
       string_agg(flight_no || ' ' || flight_date::text, ', ' ORDER BY flight_no, flight_date) AS rows_in_wide
  FROM wide
 GROUP BY route_no, type_code, seat_capacity
 ORDER BY route_no, type_code;


-- ============================================================================
-- 第 3 步: FD 的候选键边界条件 —— 空值、条件唯一索引、状态
-- ============================================================================
SELECT '第 3 步: seat_no 的取值分布(NULL 让哪些 FD 约束不上)' AS phase;

SELECT segment_status,
       count(*)                                   AS seg_rows,
       count(seat_no)                             AS with_seat,
       count(*) - count(seat_no)                  AS null_seat
  FROM booking_segment
 GROUP BY segment_status
 ORDER BY segment_status;

SELECT '第 3 步: 座位 -> 舱位 是否成立(反例查询 #1: 只看有座位的行)' AS phase;

SELECT seat_no, count(DISTINCT cabin_class) AS n_class,
       string_agg(DISTINCT cabin_class, '/') AS classes
  FROM booking_segment
 WHERE seat_no IS NOT NULL
 GROUP BY seat_no
HAVING count(DISTINCT cabin_class) > 1;

SELECT '第 3 步: 座位 -> 舱位 是否成立(反例查询 #2: 把 NULL 也算进来)' AS phase;

-- FD 的定义是"左边相同 => 右边必须相同", NULL 行之间也满足"左边相同"。
-- 所以严格判定必须把 NULL 归成一组: 组内舱位不一致, 就说明这条 FD 在整个关系上不成立。
SELECT seat_no, count(*) AS rows_in_group, count(DISTINCT cabin_class) AS n_class,
       string_agg(DISTINCT cabin_class, '/') AS classes
  FROM booking_segment
 GROUP BY seat_no
HAVING count(DISTINCT cabin_class) > 1;

SELECT '第 3 步: 同航班同座位出现多行(部分唯一索引放行了什么)' AS phase;

SELECT flight_no, flight_date, seat_no,
       count(*) AS rows_for_this_seat,
       string_agg(segment_status, ' + ' ORDER BY segment_status) AS statuses
  FROM booking_segment
 WHERE seat_no IS NOT NULL
 GROUP BY flight_no, flight_date, seat_no
HAVING count(*) > 1;

-- 条件唯一索引的判定: 它约束的是"未取消"子集, 不是整个关系
SELECT '第 3 步: 部分唯一索引的定义' AS phase;

SELECT indexname, indexdef
  FROM pg_indexes
 WHERE schemaname = 'public' AND tablename = 'booking_segment';


-- ============================================================================
-- 第 4 步: 无损连接验证 —— 三层设计能否无损重建宽表
-- ============================================================================
SELECT '第 4 步: 无损连接验证' AS phase;

WITH r1 AS (                                  -- timetable 侧: 班号 -> 航线/机型/容量/时列表述
    SELECT f.flight_no, t.route_no, t.type_code, ac.seat_capacity,
           t.dep_time AS sched_dep, t.arr_time AS sched_arr,
           t.valid_from, t.valid_to
      FROM flight f
      JOIN timetable t       ON t.schedule_id = f.schedule_id
      JOIN aircraft_type ac  ON ac.type_code  = t.type_code
     GROUP BY f.flight_no, t.route_no, t.type_code, ac.seat_capacity,
              t.dep_time, t.arr_time, t.valid_from, t.valid_to
),
r2 AS (                                       -- flight 侧: 具体哪天 + 执飞飞机 + 实际时刻
    SELECT flight_no, flight_date, schedule_id, reg_no, dep_time, arr_time, flight_status
      FROM flight
)
SELECT (SELECT count(*) FROM r1 JOIN r2 USING (flight_no))       AS decomposed_rows,
       (SELECT count(*) FROM flight)                             AS original_rows,
       CASE WHEN (SELECT count(*) FROM r1 JOIN r2 USING (flight_no))
               = (SELECT count(*) FROM flight)
            THEN '无损: R1 * R2 的行数 = 原关系行数'
            ELSE '有损: 出现了伪元组'
       END AS verdict;


-- ============================================================================
-- 第 5 步: 更新异常 —— 宽表改一处要动多行 / 三层设计只需改一行
--   用 TEMP 表影子化 aircraft_type, 不动交付表
-- ============================================================================
SELECT '第 5 步: 更新异常(影子表演示, 不动交付表)' AS phase;

BEGIN;

CREATE TEMP TABLE aircraft_type_shadow ON COMMIT DROP AS
    SELECT * FROM aircraft_type;

-- 宽表视角: "把这个机型的座位数从 160 改成 170", 在拍平的表里必须动 3 行
UPDATE aircraft_type_shadow
   SET seat_capacity = 170
 WHERE type_code = 'B738'
RETURNING type_code, seat_capacity AS after_update;

-- 三层设计视角: 同一件事, 只需要改 aircraft_type 里那 1 行
SELECT '三层设计: 同一件事只需 1 行' AS phase;
SELECT count(*) AS rows_touched_in_normalized_design
  FROM aircraft_type
 WHERE type_code = 'B738';

ROLLBACK;


-- ============================================================================
-- 第 6 步: 负测试(故意解除 ON_ERROR_STOP)
--   DDL 阶段保护策略照旧; 但负测试就是要看约束报错, 所以这一段显式放开,
--   最后统一数 ERROR 行数 —— 判成败看数量, 不看退出码。
-- ============================================================================
SELECT '第 6 步: 负测试开始(预期报错)' AS phase;

\set ON_ERROR_STOP off

-- 6.1 主码/唯一列被外码引用时, ON UPDATE 默认是 RESTRICT: 改不动
UPDATE aircraft_type SET type_code = 'B739' WHERE type_code = 'B738';

-- 6.2 座位重复被部分唯一索引拦下(未取消才受约束)
INSERT INTO booking_segment
    (booking_id, seg_no, flight_no, flight_date, cabin_class, seat_no, segment_status)
VALUES (1, 9, 'CA1501', '2024-10-07', '经济舱', '12A', '已确认');

-- 6.3 但已取消的那一行不占用座位: 同一个座位可以再卖给下一位
INSERT INTO booking_segment
    (booking_id, seg_no, flight_no, flight_date, cabin_class, seat_no, segment_status)
VALUES (1, 10, 'MU5101', '2024-10-11', '公务舱', '5A', '已确认');

-- 6.4 主码被引用时删父行, 被 RESTRICT 拦下
DELETE FROM aircraft_type WHERE type_code = 'B738';

-- 6.5 主码重复, 被 pk 拦下(对照组: 与 FD 无关的错误, 说明计数要看具体错误种类)
INSERT INTO aircraft_type (type_code, manufacturer, model, seat_capacity)
VALUES ('B738', '波音', '737-800', 162);

\set ON_ERROR_STOP on

SELECT '第 6 步: 负测试结束 —— 以上应出现 4 条 ERROR(6.1/6.2/6.4/6.5)' AS phase;

-- 负测试全部失败 => 交付表必须一行未变(6.3 那条成功插入是设计意图,见下)
SELECT '第 6 步: 负测试后的行数(应与第 1 步相同)' AS phase;
SELECT (SELECT count(*) FROM aircraft_type)   AS aircraft_type_rows,
       (SELECT count(*) FROM booking_segment) AS booking_segment_rows;


-- ============================================================================
-- 第 7 步: 改进点实测 —— 把 seat_no -> cabin_class 抽成座位字典
--   用 TEMP 表做对照, 不动交付表
-- ============================================================================
SELECT '第 7 步: 座位字典(改进点)对照' AS phase;

BEGIN;

CREATE TEMP TABLE seat_dict (
    flight_no   VARCHAR(8) NOT NULL,
    flight_date DATE       NOT NULL,
    seat_no     VARCHAR(4) NOT NULL,
    cabin_class VARCHAR(6) NOT NULL,
    PRIMARY KEY (flight_no, flight_date, seat_no)
) ON COMMIT DROP;

INSERT INTO seat_dict
SELECT DISTINCT flight_no, flight_date, seat_no, cabin_class
  FROM booking_segment
 WHERE seat_no IS NOT NULL;

SELECT '座位字典行数(与 booking_segment 里有座位的行数对比)' AS phase;
SELECT (SELECT count(*) FROM seat_dict)                           AS seat_dict_rows,
       (SELECT count(*) FROM booking_segment WHERE seat_no IS NOT NULL) AS segment_rows_with_seat;

-- 抽取后: 座位 -> 舱位 这条 FD 的左边成了座位字典的候选键, 满足 BCNF
SELECT '座位字典里 seat_no 是否唯一决定 cabin_class(反例查询, 应为 0 行)' AS phase;
SELECT seat_no, count(DISTINCT cabin_class) AS n_class
  FROM seat_dict
 GROUP BY seat_no
HAVING count(DISTINCT cabin_class) > 1;

ROLLBACK;


-- ============================================================================
-- 第 8 步: 交付表数据未被本脚本改动
-- ============================================================================
SELECT '第 8 步: 交付表未被改动的证据' AS phase;

SELECT (SELECT count(*) FROM aircraft_type)   AS aircraft_type_rows,
       (SELECT count(*) FROM flight)          AS flight_rows,
       (SELECT count(*) FROM booking_segment) AS booking_segment_rows,
       (SELECT count(*) FROM timetable)       AS timetable_rows;

SELECT '=== BCNF 体检结束 ===' AS phase;
