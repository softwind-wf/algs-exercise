-- ============================================================
-- PL/Java 演示 —— 安装 jar、注册外部函数、调用、验证
-- 用法：psql -U postgres -d <db> -f install.sql
-- 注意：univ.jar 需位于下面的 file:// 路径（Docker 流运行时在 /tmp/pljava-demo/univ.jar）
-- ============================================================
\set ON_ERROR_STOP on

-- 1) 启用 PL/Java（语言 java）
CREATE EXTENSION IF NOT EXISTS java;

-- 2) 把 jar 安装进数据库（第三个参数 true = 信任）
SELECT sqlj.install_jar('file:///tmp/pljava-demo/univ.jar',
                        'univ', true);
SELECT sqlj.set_classpath('public', 'univ');

-- 3) 注册外部函数（AS '包.类.方法' LANGUAGE java）
CREATE OR REPLACE FUNCTION greet(name text, title text) RETURNS text
  AS 'dsh.demo.UniversityFuncs.greet' LANGUAGE java;

CREATE OR REPLACE FUNCTION avg_of(xs int[]) RETURNS float8
  AS 'dsh.demo.UniversityFuncs.avgOf' LANGUAGE java;

CREATE OR REPLACE FUNCTION most_frequent_word(t text) RETURNS text
  AS 'dsh.demo.UniversityFuncs.mostFrequentWord' LANGUAGE java;

-- 演示：回查数据库。建一张 student 表再调用。
CREATE TABLE IF NOT EXISTS student (ID int PRIMARY KEY, name text NOT NULL);
TRUNCATE student;
INSERT INTO student VALUES (1,'Zhang'),(2,'Shankar'),(3,'Williams');

CREATE OR REPLACE FUNCTION count_students() RETURNS int
  AS 'dsh.demo.UniversityFuncs.countStudents' LANGUAGE java;

-- 4) 调用 & 展示结果
SELECT greet('Zhang','Dr.')                      AS salutation;
SELECT avg_of(ARRAY[80,90,75])                   AS avg_grade;
SELECT most_frequent_word('the cat and the dog and the bird') AS top_word;
SELECT count_students()                          AS student_count;
