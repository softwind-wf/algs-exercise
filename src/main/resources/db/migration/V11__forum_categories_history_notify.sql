-- ============================================================
-- V11 论坛扩展：动态板块 / 编辑历史 / 站内通知（@提及）
--   1. forum_category 板块表（管理员维护），forum_post.category → category_id
--   2. forum_post_history 帖子编辑历史快照
--   3. user_notification 站内通知（@提及等）
--   幂等写法（CREATE TABLE IF NOT EXISTS + information_schema 列检查），重放安全。
-- ============================================================

-- 1. 板块表 + 默认 5 个板块
CREATE TABLE IF NOT EXISTS forum_category (
    id          INT          NOT NULL AUTO_INCREMENT COMMENT '板块ID（自增主键）',
    name        VARCHAR(30)  NOT NULL COMMENT '板块名称',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序（小在前）',
    enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '启用：1 启用 / 0 停用（停用后新帖不可选，存量帖保留）',
    created_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_forum_category_name (name)
) ENGINE = InnoDB COMMENT = '论坛板块（管理员维护）';

INSERT IGNORE INTO forum_category (id, name, sort_order) VALUES
    (1, '学习交流', 1),
    (2, '课程答疑', 2),
    (3, '校园生活', 3),
    (4, '资源共享', 4),
    (5, '意见建议', 5);

-- 2. forum_post 增加 category_id 并回填（旧 category 编码 → 板块 ID），再删除旧列
SET @fcol := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_post' AND COLUMN_NAME = 'category_id'
);
SET @ddl := IF(@fcol = 0,
    'ALTER TABLE forum_post ADD COLUMN category_id INT NULL COMMENT ''板块ID（FK→forum_category）'' AFTER category',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE forum_post SET category_id = CASE category
    WHEN 'STUDY'   THEN 1
    WHEN 'COURSE'  THEN 2
    WHEN 'CAMPUS'  THEN 3
    WHEN 'SHARE'   THEN 4
    WHEN 'SUGGEST' THEN 5
    ELSE 1 END;

SET @fcol := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_post' AND COLUMN_NAME = 'category'
);
SET @ddl := IF(@fcol > 0,
    'ALTER TABLE forum_post DROP COLUMN category',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 编辑历史快照表
CREATE TABLE IF NOT EXISTS forum_post_history (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '历史ID（自增主键）',
    post_id     BIGINT       NOT NULL COMMENT '帖子ID（FK→forum_post，级联删除）',
    title       VARCHAR(100) NOT NULL COMMENT '编辑前标题',
    content     TEXT         NOT NULL COMMENT '编辑前正文',
    category_id INT          NULL     COMMENT '编辑前板块',
    edited_by   VARCHAR(20)  NOT NULL COMMENT '编辑者登录账号',
    created_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    PRIMARY KEY (id),
    KEY idx_fph_post (post_id, id),
    CONSTRAINT fk_fph_post FOREIGN KEY (post_id) REFERENCES forum_post (id) ON DELETE CASCADE
) ENGINE = InnoDB COMMENT = '帖子编辑历史';

-- 4. 站内通知（@提及等）
CREATE TABLE IF NOT EXISTS user_notification (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID（自增主键）',
    user_id      VARCHAR(20)  NOT NULL COMMENT '接收人登录账号',
    type         VARCHAR(20)  NOT NULL DEFAULT 'FORUM_MENTION' COMMENT '通知类型：FORUM_MENTION 论坛提及',
    source_url   VARCHAR(200) NULL     COMMENT '跳转地址（如 /forum/123#reply-45）',
    summary      VARCHAR(200) NOT NULL COMMENT '摘要文案',
    read_flag    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已读：1 已读 / 0 未读',
    created_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间',
    PRIMARY KEY (id),
    KEY idx_notice_user (user_id, read_flag, id)
) ENGINE = InnoDB COMMENT = '站内通知';
