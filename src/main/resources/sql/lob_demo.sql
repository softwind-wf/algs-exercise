-- ============================================================
-- PostgreSQL 大对象类型演示（对照教材 BLOB / CLOB 概念）
--
-- 建库:  psql.bat -d postgres "CREATE DATABASE lob_demo"
-- 执行:  psql.bat -d lob_demo -f src\main\resources\sql\lob_demo.sql
--
-- 三种大对象形态:
--   1. BYTEA  —— 二进制大对象（等价教材 BLOB）
--   2. TEXT   —— 长文本（等价教材 CLOB）
--   3. OID/lo —— PostgreSQL 内置 Large Object 机制（内容存系统表，不占行内空间）
-- ============================================================

-- ========== 1. BYTEA：二进制大对象（BLOB） ==========
DROP TABLE IF EXISTS binary_docs;
CREATE TABLE binary_docs (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    data BYTEA
);

-- convert_to：文本→二进制字节；decode：十六进制→二进制
INSERT INTO binary_docs (name, data) VALUES
    ('hello.txt', convert_to('Hello, 大对象世界!', 'UTF8')),
    ('pdf_head',  decode('255044462d312e37', 'hex')),   -- "%PDF-1.7" 文件头
    ('gif_head',  decode('474946383961', 'hex'));       -- "GIF89a" 文件头

SELECT id, name,
       octet_length(data) AS bytes,
       encode(data, 'hex') AS hex_dump
FROM binary_docs
ORDER BY id;

-- ========== 2. TEXT：长文本（CLOB） ==========
DROP TABLE IF EXISTS text_docs;
CREATE TABLE text_docs (
    id    SERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    body  TEXT
);

-- repeat 快速造长文本；chr(10) 是换行
INSERT INTO text_docs (title, body) VALUES
    ('算法随笔',   repeat('排序是算法的基础。', 50)),
    ('数据库笔记', 'ACID：原子性、一致性、隔离性、持久性。' || chr(10)
                   || '大对象类型分为 BLOB（二进制）与 CLOB（字符）。');

SELECT id, title,
       length(body)       AS chars,
       octet_length(body) AS bytes,
       substring(body FROM 1 FOR 12) AS preview
FROM text_docs
ORDER BY id;

-- ========== 3. PostgreSQL 内置 Large Object（lo 机制） ==========
-- 内容实际存 pg_largeobject 系统表，行内只存一个 OID 引用，适合超大对象
DROP TABLE IF EXISTS lo_docs;
CREATE TABLE lo_docs (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    data OID
);

INSERT INTO lo_docs (name, data) VALUES
    ('lob_content', lo_from_bytea(0, convert_to(
        '这是 PostgreSQL 的大对象(LO)机制,大内容不占行内空间,而是存到系统表。', 'UTF8')));

SELECT id, name,
       lo_get(data)               AS content,
       octet_length(lo_get(data)) AS bytes
FROM lo_docs;

-- 清理：删除演示用大对象（避免 pg_largeobject 残留孤儿条目）
SELECT lo_unlink(data) FROM lo_docs;
