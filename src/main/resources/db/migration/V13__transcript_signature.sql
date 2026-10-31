-- ============================================================
-- V13 成绩单签名记录 transcript_signature
--
-- 学生中心「下载签名版成绩单」时写入：
--   payload_hash = SHA-256(规范化 JSON)，signature = 私钥签名（Base64）。
-- 只增不改：换证书、吊销、重复签发都能按 serial_no / issued_at 追溯。
--
-- 幂等写法（CREATE TABLE IF NOT EXISTS）：存量库重放不会清空记录。
-- ============================================================

CREATE TABLE IF NOT EXISTS transcript_signature (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '签名记录ID',
    student_id   VARCHAR(20)  NOT NULL COMMENT '学号',
    term         VARCHAR(20)  NULL     COMMENT '学期（如 2026-FALL）；多学期成绩单记 ALL',
    serial_no    VARCHAR(40)  NOT NULL COMMENT '签名流水号（被签内容里也带着，防重放）',
    payload_hash CHAR(64)     NOT NULL COMMENT 'SHA-256(规范化成绩单 JSON)',
    signature    TEXT         NOT NULL COMMENT 'Base64 签名（内容在文件里，库里存哈希+签名便于追溯）',
    cert_serial  VARCHAR(64)  NULL     COMMENT '签名证书序列号（换钥后仍能定位历史公钥）',
    issued_at    DATETIME     NOT NULL COMMENT '签发时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transcript_signature_serial (serial_no),
    KEY idx_transcript_signature_student (student_id, issued_at)
) ENGINE = InnoDB COMMENT = '成绩单签名记录（只增不改）';
