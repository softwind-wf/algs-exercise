-- ============================================================
-- V12 全文检索索引（ngram 解析器，支持中文）
--   论坛帖子（标题+正文）、学生姓名、教师姓名：LIKE '%kw%' 无法走索引，
--   改为 MySQL FULLTEXT + ngram 分词，查询用 MATCH...AGAINST（布尔模式），
--   短关键字（1 字符）由应用层回退 LIKE。
--   幂等写法：information_schema.STATISTICS 检查索引是否存在（重放安全）。
-- ============================================================

-- 1. forum_post(title, content)
SET @fidx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_post'
      AND INDEX_NAME = 'ft_forum_post_title_content'
);
SET @ddl := IF(@fidx = 0,
    'ALTER TABLE forum_post ADD FULLTEXT INDEX ft_forum_post_title_content (title, content) WITH PARSER ngram',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. student(name)
SET @fidx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student'
      AND INDEX_NAME = 'ft_student_name'
);
SET @ddl := IF(@fidx = 0,
    'ALTER TABLE student ADD FULLTEXT INDEX ft_student_name (name) WITH PARSER ngram',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. instructor(name)
SET @fidx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'instructor'
      AND INDEX_NAME = 'ft_instructor_name'
);
SET @ddl := IF(@fidx = 0,
    'ALTER TABLE instructor ADD FULLTEXT INDEX ft_instructor_name (name) WITH PARSER ngram',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
