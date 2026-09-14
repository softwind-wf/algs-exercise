\set ON_ERROR_STOP off
DROP TABLE IF EXISTS section;
DROP TABLE IF EXISTS time_slot;
CREATE TABLE time_slot (time_slot_id varchar(4) PRIMARY KEY, day varchar(1), start_time time);
CREATE TABLE section (course_id varchar(8), sec_id varchar(8), semester varchar(6), year int, time_slot_id varchar(4), PRIMARY KEY(course_id, sec_id, semester, year));

-- 触发器函数：INSERT section 时校验 time_slot_id 必须存在于 time_slot
CREATE FUNCTION timeslot_check1() RETURNS trigger AS $$
BEGIN
    IF NEW.time_slot_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM time_slot WHERE time_slot_id = NEW.time_slot_id) THEN
        RAISE EXCEPTION 'INSERT rejected: time_slot_id "%" not in time_slot', NEW.time_slot_id;
    END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

-- 触发器函数：DELETE time_slot 时校验该 time_slot_id 未被 section 引用
CREATE FUNCTION timeslot_check2() RETURNS trigger AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM section WHERE time_slot_id = OLD.time_slot_id) THEN
        RAISE EXCEPTION 'DELETE rejected: time_slot_id "%" still referenced by section', OLD.time_slot_id;
    END IF;
    RETURN OLD;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_section_ts AFTER INSERT ON section
    FOR EACH ROW EXECUTE FUNCTION timeslot_check1();

CREATE TRIGGER trg_timeslot_del AFTER DELETE ON time_slot
    FOR EACH ROW EXECUTE FUNCTION timeslot_check2();

INSERT INTO time_slot (time_slot_id, day, start_time) VALUES ('A','M','08:00:00'), ('B','W','09:00:00');

\echo '--- 用例1: 合法 INSERT section (time_slot_id=A) 应成功 ---'
INSERT INTO section VALUES ('CS-101','1','Fall',2024,'A');

\echo '--- 用例2: 非法 INSERT section (time_slot_id=ZZ 不存在) 应被拒 ---'
INSERT INTO section VALUES ('CS-102','1','Fall',2024,'ZZ');

\echo '--- 用例3: DELETE 被引用的 time_slot A 应被拒 ---'
DELETE FROM time_slot WHERE time_slot_id='A';

\echo '--- 用例4: DELETE 未被引用的 time_slot B 应成功 ---'
DELETE FROM time_slot WHERE time_slot_id='B';

\echo '--- 最终: section / time_slot 内容 ---'
SELECT * FROM section;
SELECT * FROM time_slot;
