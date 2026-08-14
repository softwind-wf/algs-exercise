-- ============================================================
-- V8 站内聊天消息
--   chat_message：学生/教师一对一在线聊天，消息落库可追溯，
--   支持历史记录与未读计数。
--   幂等写法（CREATE TABLE IF NOT EXISTS），重放安全。
-- ============================================================

CREATE TABLE IF NOT EXISTS chat_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID（自增主键）',
    from_user   VARCHAR(20)  NOT NULL COMMENT '发送方登录账号',
    from_name   VARCHAR(50)  NOT NULL COMMENT '发送方显示名（冗余，便于历史展示）',
    to_user     VARCHAR(20)  NOT NULL COMMENT '接收方登录账号',
    to_name     VARCHAR(50)  NOT NULL COMMENT '接收方显示名（冗余）',
    content     VARCHAR(500) NOT NULL COMMENT '消息内容',
    read_flag   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已读：1 已读 / 0 未读',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_chat_pair (from_user, to_user, create_time),
    KEY idx_chat_unread (to_user, read_flag)
) ENGINE = InnoDB COMMENT = '站内聊天消息';
