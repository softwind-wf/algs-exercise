-- ============================================================
-- 大学网站审计日志表
-- 数据库: university  字符集: utf8mb4
--
-- 敏感操作留痕：改成绩、删数据、建账号/重置密码/启停账号等，
-- 由 AuditService 在业务成功后写入，出问题可追溯。
--
-- 依赖：需先执行 university.sql 与 university_auth.sql
--
-- ⚠️ 脚本会 DROP audit_log 后重建（历史审计记录将清空）
-- ============================================================

USE university;

DROP TABLE IF EXISTS audit_log;

CREATE TABLE audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '审计记录ID（自增主键）',
    user_id     VARCHAR(20)  NULL     COMMENT '操作者登录账号；无请求上下文（如定时任务/测试）为 NULL',
    action      VARCHAR(30)  NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/GRADE_UPDATE/ACCOUNT_CREATE/PASSWORD_RESET/ACCOUNT_TOGGLE/ACCOUNT_DELETE/ACCOUNT_BATCH_CREATE',
    target_type VARCHAR(30)  NOT NULL COMMENT '对象类型：DEPARTMENT/COURSE/INSTRUCTOR/STUDENT/CLASSROOM/SECTION/PREREQ/ACCOUNT/GRADE',
    target_id   VARCHAR(100) NULL     COMMENT '对象标识（业务主键或组合键）',
    detail      VARCHAR(500) NULL     COMMENT '操作详情（含变更前后值，供追溯）',
    client_ip   VARCHAR(64)  NULL     COMMENT '操作来源 IP（TCP 对端地址，不读可伪造的 X-Forwarded-For）',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_audit_created (created_at),
    KEY idx_audit_action (action),
    KEY idx_audit_target (target_type, target_id)
) ENGINE = InnoDB COMMENT = '审计日志（敏感操作留痕，只增不改）';
