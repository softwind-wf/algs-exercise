\set ON_ERROR_STOP off
DROP TABLE IF EXISTS takes;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS course;

CREATE TABLE course (course_id varchar(8) PRIMARY KEY, title varchar(50) NOT NULL, credits int NOT NULL);
CREATE TABLE student (id varchar(5) PRIMARY KEY, name varchar(20) NOT NULL, tot_cred int DEFAULT 0);
CREATE TABLE takes (id varchar(5), course_id varchar(8), grade varchar(2));

-- 触发器函数：成绩变为"已通过"时给 student 加学分
CREATE FUNCTION credits_earned() RETURNS trigger AS $$
BEGIN
    UPDATE student
    SET tot_cred = tot_cred + (SELECT credits FROM course WHERE course_id = NEW.course_id)
    WHERE id = NEW.id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 触发器：after update of grade。注意教材用 grade>'F'，但在 PostgreSQL 默认排序里 'A'>'F' 为假，
-- 所以这里改用"成绩非空且非 F"来表达"已通过"（等价于教材意图）。
CREATE TRIGGER credits_earned
    AFTER UPDATE OF grade ON takes
    FOR EACH ROW
    WHEN (NEW.grade IS NOT NULL AND NEW.grade <> 'F'
          AND (OLD.grade = 'F' OR OLD.grade IS NULL))
    EXECUTE FUNCTION credits_earned();

INSERT INTO course VALUES ('CS-101', 'Intro to CS', 4);
INSERT INTO student VALUES ('00128', 'Zhang', 0);
INSERT INTO takes VALUES ('00128', 'CS-101', NULL);   -- 未出分

\echo '--- 用例1: 未出分(null) -> A, 应加 4 ---'
UPDATE takes SET grade='A' WHERE id='00128' AND course_id='CS-101';
SELECT id, tot_cred FROM student;

\echo '--- 用例2: A -> A(重复同分), 不应再加 ---'
UPDATE takes SET grade='A' WHERE id='00128' AND course_id='CS-101';
SELECT id, tot_cred FROM student;

\echo '--- 用例3: A -> F(成绩作废), 不应加 ---'
UPDATE takes SET grade='F' WHERE id='00128' AND course_id='CS-101';
SELECT id, tot_cred FROM student;

\echo '--- 用例4: 重新 F -> B+, 应再加 3(改用另一门 3 学分的课验证) ---'
INSERT INTO course VALUES ('CS-190', 'Game Design', 3);
INSERT INTO takes VALUES ('00128', 'CS-190', 'F');
UPDATE takes SET grade='B+' WHERE id='00128' AND course_id='CS-190';
SELECT id, tot_cred FROM student;
