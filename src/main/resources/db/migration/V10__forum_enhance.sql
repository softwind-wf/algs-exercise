-- ============================================================
-- V10 论坛增强：分类/置顶/加精/点赞/（回复与正文支持 @提及渲染，无表结构改动）
--   forum_post 新增：category（分类）、pinned（置顶）、featured（加精）、like_count（点赞数）
--   forum_like：点赞去重表（post_id + user_id 联合主键，随帖子级联删除）
--   幂等写法：information_schema 检查列/表是否存在（重放安全）。
-- ============================================================

-- 1. category
SET @fcol := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_post' AND COLUMN_NAME = 'category'
);
SET @ddl := IF(@fcol = 0,
    'ALTER TABLE forum_post ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT ''STUDY'' COMMENT ''分类：STUDY学习交流/COURSE课程答疑/CAMPUS校园生活/SHARE资源共享/SUGGEST意见建议'' AFTER content',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. pinned
SET @fcol := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_post' AND COLUMN_NAME = 'pinned'
);
SET @ddl := IF(@fcol = 0,
    'ALTER TABLE forum_post ADD COLUMN pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''置顶：1 置顶（管理员）'' AFTER category',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. featured
SET @fcol := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_post' AND COLUMN_NAME = 'featured'
);
SET @ddl := IF(@fcol = 0,
    'ALTER TABLE forum_post ADD COLUMN featured TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''加精：1 精华（管理员）'' AFTER pinned',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. like_count
SET @fcol := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_post' AND COLUMN_NAME = 'like_count'
);
SET @ddl := IF(@fcol = 0,
    'ALTER TABLE forum_post ADD COLUMN like_count INT NOT NULL DEFAULT 0 COMMENT ''点赞数（冗余计数）'' AFTER featured',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 点赞表
CREATE TABLE IF NOT EXISTS forum_like (
    post_id      BIGINT      NOT NULL COMMENT '帖子ID（FK→forum_post）',
    user_id      VARCHAR(20) NOT NULL COMMENT '点赞用户登录账号',
    created_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (post_id, user_id),
    CONSTRAINT fk_forum_like_post FOREIGN KEY (post_id) REFERENCES forum_post (id) ON DELETE CASCADE
) ENGINE = InnoDB COMMENT = '帖子点赞';
