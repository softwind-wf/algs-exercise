\set ON_ERROR_STOP off
DROP TABLE IF EXISTS takes;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS course;

CREATE TABLE course (course_id varchar(8) PRIMARY KEY, title varchar(50) NOT NULL, credits int NOT NULL);
CREATE TABLE student (id varchar(5) PRIMARY KEY, name varchar(20) NOT NULL, tot_cred int DEFAULT 0);
CREATE TABLE takes (id varchar(5), course_id varchar(8), grade varchar(2));

-- 触发器1：成绩 由"空/F"变为"已通过" => 加分
CREATE OR REPLACE FUNCTION credits_earned() RETURNS trigger AS $$
BEGIN
    UPDATE student SET tot_cred = tot_cred + (SELECT credits FROM course WHERE course_id = NEW.course_id)
    WHERE id = NEW.id;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER credits_earned AFTER UPDATE OF grade ON takes FOR EACH ROW
    WHEN (NEW.grade IS NOT NULL AND NEW.grade <> 'F' AND (OLD.grade = 'F' OR OLD.grade IS NULL))
    EXECUTE FUNCTION credits_earned();

-- 触发器2（本次需求）：成绩 由"已通过"改为"不及格F" => 减分
CREATE OR REPLACE FUNCTION credits_lost() RETURNS trigger AS $$
BEGIN
    UPDATE student
    SET tot_cred = GREATEST(tot_cred - (SELECT credits FROM course WHERE course_id = NEW.course_id), 0)
    WHERE id = NEW.id;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER credits_lost AFTER UPDATE OF grade ON takes FOR EACH ROW
    WHEN (NEW.grade = 'F' AND OLD.grade IS NOT NULL AND OLD.grade <> 'F')
    EXECUTE FUNCTION credits_lost();

INSERT INTO course VALUES ('CS-101', 'Intro to CS', 4);
INSERT INTO student VALUES ('00128', 'Zhang', 0);
INSERT INTO takes VALUES ('00128', 'CS-101', NULL);

\echo '--- 用例1: null -> A(通过), 应加4 ---'
UPDATE takes SET grade='A' WHERE id='00128' AND course_id='CS-101';
SELECT id, tot_cred FROM student;

\echo '--- 用例2: A -> F(把成功结课改成不及格), 应减4 => 0 ---'
UPDATE takes SET grade='F' WHERE id='00128' AND course_id='CS-101';
SELECT id, tot_cred FROM student;

\echo '--- 用例3: F -> null(清除成绩), 不应减(新值非F) ---'
UPDATE takes SET grade=NULL WHERE id='00128' AND course_id='CS-101';
SELECT id, tot_cred FROM student;

\echo '--- 用例4: null -> B+(再一次通过), 应加4 => 4 ---'
UPDATE takes SET grade='B+' WHERE id='00128' AND course_id='CS-101';
SELECT id, tot_cred FROM student;
