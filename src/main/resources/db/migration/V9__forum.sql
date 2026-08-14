-- ============================================================
-- V9 学习论坛
--   forum_post  帖子（标题/内容/作者/回复数/最后回复时间）
--   forum_reply 回复（按帖子分组，级联删除）
--   所有学生/教师（及管理员）可发帖与回复；作者/管理员可删除。
--   幂等写法（CREATE TABLE IF NOT EXISTS），重放安全。
-- ============================================================

CREATE TABLE IF NOT EXISTS forum_post (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '帖子ID（自增主键）',
    title           VARCHAR(100) NOT NULL COMMENT '标题',
    content         TEXT         NOT NULL COMMENT '正文',
    author_user     VARCHAR(20)  NOT NULL COMMENT '作者登录账号',
    author_name     VARCHAR(50)  NOT NULL COMMENT '作者显示名（冗余）',
    reply_count     INT          NOT NULL DEFAULT 0 COMMENT '回复数（冗余计数）',
    last_reply_time DATETIME     NULL     COMMENT '最后回复时间（列表按活跃度排序）',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_forum_post_active (last_reply_time, id)
) ENGINE = InnoDB COMMENT = '论坛帖子';

CREATE TABLE IF NOT EXISTS forum_reply (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '回复ID（自增主键）',
    post_id      BIGINT      NOT NULL COMMENT '所属帖子（FK→forum_post）',
    content      TEXT        NOT NULL COMMENT '回复内容',
    author_user  VARCHAR(20) NOT NULL COMMENT '作者登录账号',
    author_name  VARCHAR(50) NOT NULL COMMENT '作者显示名（冗余）',
    created_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回复时间',
    PRIMARY KEY (id),
    KEY idx_forum_reply_post (post_id, id),
    CONSTRAINT fk_forum_reply_post FOREIGN KEY (post_id) REFERENCES forum_post (id) ON DELETE CASCADE
) ENGINE = InnoDB COMMENT = '论坛回复';
