-- ============================================================================
-- BCNF 体检 · 边界条件探测(NULL 与 UNIQUE 的真实交互)
-- 全部在事务内进行, 末尾 ROLLBACK, 不留痕。
-- 运行: .\psql.bat -d airline_demo -f src\main\resources\sql\bcnf_audit_boundary.sql
-- ============================================================================
SET client_encoding = 'UTF8';
\set ON_ERROR_STOP off
\pset pager off

SELECT '=== 边界探测开始(全部回滚) ===' AS phase;

BEGIN;

-- ---------------------------------------------------------------------------
-- 探测 1: customer.email 可空 + UNIQUE(email)
--   DDL 声明了 email 唯一 => 教科书会把 email 当成一条候选键;
--   但在 SQL 的三值逻辑下, 多个 NULL 互不相等, 唯一约束**放行**
-- ---------------------------------------------------------------------------
SELECT '探测 1: 插入两个 email 为 NULL 的客户' AS phase;

INSERT INTO customer (customer_id, name, phone, email, address)
VALUES (9001, '甲', '13000000000', NULL, '某地');

INSERT INTO customer (customer_id, name, phone, email, address)
VALUES (9002, '乙', '13000000001', NULL, '某地');

SELECT '探测 1 结果: NULL email 并存的客户数(能查出行说明 UNIQUE 放行了多行 NULL)' AS phase;
SELECT count(*) AS customers_with_null_email FROM customer WHERE email IS NULL;

-- ---------------------------------------------------------------------------
-- 探测 2: 座位号可空 => booking_segment 里同航班可以让多个"无座位"的候补并存
--   这正是 seat_no -> cabin_class 这条 FD 在"整个关系"上不成立的原因
-- ---------------------------------------------------------------------------
SELECT '探测 2: 同一航班插入两个候选(seat_no 为 NULL)' AS phase;

INSERT INTO booking_segment
    (booking_id, seg_no, flight_no, flight_date, cabin_class, seat_no, segment_status)
VALUES (1, 11, 'CA1501', '2024-10-07', '经济舱', NULL, '候补');

INSERT INTO booking_segment
    (booking_id, seg_no, flight_no, flight_date, cabin_class, seat_no, segment_status)
VALUES (2, 11, 'CA1501', '2024-10-07', '公务舱', NULL, '候补');

SELECT '探测 2 结果: 同航班 seat_no 为 NULL 的行(两行并存 => FD 左边相同右边不同)' AS phase;
SELECT flight_no, flight_date, seat_no, cabin_class, segment_status
  FROM booking_segment
 WHERE flight_no = 'CA1501' AND flight_date = '2024-10-07'
 ORDER BY cabin_class;

SELECT '★ 这条结果就是 seat_no -> cabin_class 不是"整个关系上"的 FD 的活证据' AS conclusion;

ROLLBACK;

SELECT '=== 边界探测结束, 已全部回滚 ===' AS phase;
SELECT (SELECT count(*) FROM customer)         AS customer_rows,
       (SELECT count(*) FROM booking_segment)  AS booking_segment_rows;
