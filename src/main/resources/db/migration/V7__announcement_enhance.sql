-- ============================================================
-- V7 系统公告增强：类型分类 + 定时发布 + 到期自动下线
--   sys_announcement 新增：
--     category     公告类型（NOTICE 通知 / NEWS 新闻动态 / ACTIVITY 活动 / OTHER 其他）
--     publish_time 定时发布时间（NULL=立即发布；未来时间则到时自动对外可见）
--     expire_time  到期时间（NULL=永不过期；到期自动下线，无需人工操作）
--   对外可见条件：enabled=1 且 publish_time<=NOW() 且 (expire_time IS NULL 或 expire_time>NOW())
--   幂等写法：information_schema 检查列是否存在，存在则跳过（重放安全）。
-- ============================================================

-- 1. category
SET @ann_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_announcement' AND COLUMN_NAME = 'category'
);
SET @ddl := IF(@ann_col = 0,
    'ALTER TABLE sys_announcement ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT ''NOTICE'' COMMENT ''公告类型：NOTICE 通知 / NEWS 新闻动态 / ACTIVITY 活动 / OTHER 其他'' AFTER content',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. publish_time
SET @ann_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_announcement' AND COLUMN_NAME = 'publish_time'
);
SET @ddl := IF(@ann_col = 0,
    'ALTER TABLE sys_announcement ADD COLUMN publish_time DATETIME NULL COMMENT ''定时发布时间（NULL=立即发布，未来时间则到时自动对外可见）'' AFTER enabled',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. expire_time
SET @ann_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_announcement' AND COLUMN_NAME = 'expire_time'
);
SET @ddl := IF(@ann_col = 0,
    'ALTER TABLE sys_announcement ADD COLUMN expire_time DATETIME NULL COMMENT ''到期时间（NULL=永不过期），到期自动下线'' AFTER publish_time',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 新增示例公告（新闻动态类型；IGNORE：已存在则跳过，重放安全）
INSERT IGNORE INTO sys_announcement (id, title, content, category, pinned, enabled) VALUES
    (3, '新学期选课即将开始', '新学期选课将于近期开放，请同学们提前规划课程，详见学生中心"选课 / 退课"。', 'NEWS', 0, 1);
