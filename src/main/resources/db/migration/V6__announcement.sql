-- ============================================================
-- V6 系统公告
--   sys_announcement：管理员维护，首页与公告页展示已发布（enabled=1）的公告，
--   置顶（pinned=1）优先展示。
--   幂等写法（CREATE TABLE IF NOT EXISTS + INSERT IGNORE），重放安全。
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_announcement (
    id          INT          NOT NULL AUTO_INCREMENT COMMENT '公告ID（自增主键）',
    title       VARCHAR(100) NOT NULL COMMENT '公告标题',
    content     TEXT         NOT NULL COMMENT '公告内容',
    pinned      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '置顶：1 置顶 / 0 普通',
    enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '发布状态：1 已发布（对外可见）/ 0 已下线',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '系统公告';

-- 初始示例公告（IGNORE：已存在则跳过，重放安全）
INSERT IGNORE INTO sys_announcement (id, title, content, pinned, enabled) VALUES
    (1, '演示账号说明', '演示账号：zhang（学生）/ katz（教师）/ admin（管理员），密码均为 password。', 1, 1),
    (2, '访问建议', '建议使用 Chrome / Edge 浏览器访问本站，以获得最佳体验。', 0, 1);
