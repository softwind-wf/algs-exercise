-- ============================================================
-- V5 用户头像
--   sys_user 增加 avatar 列：仅存文件名（存储在 app.upload.avatar-dir 目录，
--   通过 /uploads/avatars/{文件名} 访问），不落库二进制内容。
--   幂等写法：information_schema 检查列是否存在，存在则跳过（重放安全，
--   与 V1-V4 的 IF NOT EXISTS / INSERT IGNORE 约定一致）。
-- ============================================================

SET @avatar_column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'avatar'
);

SET @ddl := IF(@avatar_column_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN avatar VARCHAR(255) NULL COMMENT ''头像文件名（存储在 uploads 目录，仅存文件名）'' AFTER ref_id',
    'SELECT 1');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
