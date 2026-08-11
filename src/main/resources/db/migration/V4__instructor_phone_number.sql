-- ============================================================
-- V4：收编 instructor.phone_number 列
--
-- 背景：存量开发库曾被手工 ALTER TABLE instructor ADD COLUMN phone_number
-- （全代码零引用的 schema 漂移残留）。本脚本将该列正式纳入版本化管理：
-- 已有该列的库原样保留数据，全新库补建该列，两边 schema 从此一致。
--
-- 幂等写法：MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，
-- 用 information_schema 判断 + PREPARE/EXECUTE 动态 DDL。
-- ============================================================

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'instructor'
      AND column_name = 'phone_number'
);

SET @ddl = IF(
    @col_exists = 0,
    'ALTER TABLE instructor ADD COLUMN phone_number VARCHAR(11) NULL COMMENT ''联系电话''',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
