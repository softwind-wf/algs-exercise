-- ============================================================
-- 大学数据库完整设计（Database System Concepts 教材经典模型）
-- 数据库: university  编码: UTF8
--
-- PostgreSQL 版（从 MySQL 版 university.sql 转换，语法已适配）
-- 共 11 张表：
--   实体集 5 张：department / course / instructor / student / classroom
--   联系集 4 张：section(开课班) / teaches(授课) / takes(选课) / prereq(先修)
--   扩展 2 张：advisor(指导) / time_slot(时间段)
--
-- ⚠️ 不含建库语句：请先执行 `CREATE DATABASE university;`
--    然后 `psql -d university -f university_pg.sql`
-- ============================================================

-- ------------------------------------------------------------
-- 1. 系 department
-- ------------------------------------------------------------
CREATE TABLE department (
    dept_name VARCHAR(20) PRIMARY KEY,
    building  VARCHAR(15) NULL,
    budget    DECIMAL(12, 2) NULL
);

-- ------------------------------------------------------------
-- 2. 教室 classroom（联合主键）
-- ------------------------------------------------------------
CREATE TABLE classroom (
    building    VARCHAR(15) NOT NULL,
    room_number VARCHAR(7)  NOT NULL,
    capacity    INT         NULL,
    PRIMARY KEY (building, room_number)
);

-- ------------------------------------------------------------
-- 3. 课程 course
-- ------------------------------------------------------------
CREATE TABLE course (
    course_id VARCHAR(8)  PRIMARY KEY,
    title     VARCHAR(50) NULL,
    dept_name VARCHAR(20) NULL,
    credits   DECIMAL(2, 0) NULL,
    CONSTRAINT fk_course_department FOREIGN KEY (dept_name)
        REFERENCES department (dept_name)
);

-- ------------------------------------------------------------
-- 4. 教师 instructor
-- ------------------------------------------------------------
CREATE TABLE instructor (
    ID        VARCHAR(5)   PRIMARY KEY,
    name      VARCHAR(20)  NOT NULL,
    dept_name VARCHAR(20)  NULL,
    salary    DECIMAL(8, 2) NULL,
    phone_number VARCHAR(11) NULL,
    CONSTRAINT fk_instructor_department FOREIGN KEY (dept_name)
        REFERENCES department (dept_name)
);

-- ------------------------------------------------------------
-- 5. 学生 student
-- ------------------------------------------------------------
CREATE TABLE student (
    ID        VARCHAR(5)  PRIMARY KEY,
    name      VARCHAR(20) NOT NULL,
    dept_name VARCHAR(20) NULL,
    tot_cred  INT         NULL,
    CONSTRAINT fk_student_department FOREIGN KEY (dept_name)
        REFERENCES department (dept_name)
);

-- ------------------------------------------------------------
-- 6. 开课班 section
--    (course_id, sec_id, semester, year) 联合主键
-- ------------------------------------------------------------
CREATE TABLE section (
    course_id    VARCHAR(8)  NOT NULL,
    sec_id       VARCHAR(8)  NOT NULL,
    semester     VARCHAR(6)  NOT NULL,
    year         SMALLINT    NOT NULL,
    building     VARCHAR(15) NULL,
    room_number  VARCHAR(7)  NULL,
    time_slot_id VARCHAR(4)  NULL,
    PRIMARY KEY (course_id, sec_id, semester, year),
    CONSTRAINT fk_section_course FOREIGN KEY (course_id)
        REFERENCES course (course_id),
    CONSTRAINT fk_section_classroom FOREIGN KEY (building, room_number)
        REFERENCES classroom (building, room_number)
);

-- ------------------------------------------------------------
-- 7. 授课 teaches：Instructor ⇋ Section（多对多）
-- ------------------------------------------------------------
CREATE TABLE teaches (
    ID        VARCHAR(5) NOT NULL,
    course_id VARCHAR(8) NOT NULL,
    sec_id    VARCHAR(8) NOT NULL,
    semester  VARCHAR(6) NOT NULL,
    year      SMALLINT   NOT NULL,
    PRIMARY KEY (ID, course_id, sec_id, semester, year),
    CONSTRAINT fk_teaches_instructor FOREIGN KEY (ID)
        REFERENCES instructor (ID),
    CONSTRAINT fk_teaches_section FOREIGN KEY (course_id, sec_id, semester, year)
        REFERENCES section (course_id, sec_id, semester, year)
);

-- ------------------------------------------------------------
-- 8. 选课 takes：Student ⇋ Section，带属性 grade 成绩
-- ------------------------------------------------------------
CREATE TABLE takes (
    ID        VARCHAR(5) NOT NULL,
    course_id VARCHAR(8) NOT NULL,
    sec_id    VARCHAR(8) NOT NULL,
    semester  VARCHAR(6) NOT NULL,
    year      SMALLINT   NOT NULL,
    grade     VARCHAR(2) NULL,
    PRIMARY KEY (ID, course_id, sec_id, semester, year),
    CONSTRAINT fk_takes_student FOREIGN KEY (ID)
        REFERENCES student (ID),
    CONSTRAINT fk_takes_section FOREIGN KEY (course_id, sec_id, semester, year)
        REFERENCES section (course_id, sec_id, semester, year)
);

-- ------------------------------------------------------------
-- 9. 先修课 prereq：Course ⇋ Course（自引用）
-- ------------------------------------------------------------
CREATE TABLE prereq (
    course_id VARCHAR(8) NOT NULL,
    prereq_id VARCHAR(8) NOT NULL,
    PRIMARY KEY (course_id, prereq_id),
    CONSTRAINT fk_prereq_course FOREIGN KEY (course_id)
        REFERENCES course (course_id),
    CONSTRAINT fk_prereq_course_prereq FOREIGN KEY (prereq_id)
        REFERENCES course (course_id)
);

-- ------------------------------------------------------------
-- 10. 指导关系 advisor：Student ⇋ Instructor（一个学生一个导师）
-- ------------------------------------------------------------
CREATE TABLE advisor (
    s_id VARCHAR(5) PRIMARY KEY,
    i_id VARCHAR(5) NULL,
    CONSTRAINT fk_advisor_student FOREIGN KEY (s_id)
        REFERENCES student (ID),
    CONSTRAINT fk_advisor_instructor FOREIGN KEY (i_id)
        REFERENCES instructor (ID)
);

-- ------------------------------------------------------------
-- 11. 时间段 time_slot（(time_slot_id, day, start_time) 联合主键）
-- ------------------------------------------------------------
CREATE TABLE time_slot (
    time_slot_id VARCHAR(4) NOT NULL,
    day          VARCHAR(1) NOT NULL,
    start_time   TIME       NOT NULL,
    end_time     TIME       NULL,
    PRIMARY KEY (time_slot_id, day, start_time)
);

-- ------------------------------------------------------------
-- 表 / 列注释（PostgreSQL 用 COMMENT ON 保留语义）
-- ------------------------------------------------------------
COMMENT ON TABLE department IS '系';
COMMENT ON COLUMN department.dept_name IS '系名称（主键）';
COMMENT ON COLUMN department.building  IS '所在教学楼';
COMMENT ON COLUMN department.budget    IS '经费预算';

COMMENT ON TABLE classroom IS '教室';
COMMENT ON COLUMN classroom.building    IS '教学楼';
COMMENT ON COLUMN classroom.room_number IS '房间号';
COMMENT ON COLUMN classroom.capacity    IS '容量';

COMMENT ON TABLE course IS '课程';
COMMENT ON COLUMN course.course_id IS '课程号（主键）';
COMMENT ON COLUMN course.title     IS '课程名';
COMMENT ON COLUMN course.dept_name IS '所属系（FK→department）';
COMMENT ON COLUMN course.credits   IS '学分';

COMMENT ON TABLE instructor IS '教师';
COMMENT ON COLUMN instructor.ID           IS '教师编号（主键）';
COMMENT ON COLUMN instructor.name         IS '姓名';
COMMENT ON COLUMN instructor.dept_name    IS '所属系（FK→department）';
COMMENT ON COLUMN instructor.salary       IS '工资';
COMMENT ON COLUMN instructor.phone_number IS '联系电话';

COMMENT ON TABLE student IS '学生';
COMMENT ON COLUMN student.ID        IS '学生编号（主键）';
COMMENT ON COLUMN student.name      IS '姓名';
COMMENT ON COLUMN student.dept_name IS '主修系（FK→department）';
COMMENT ON COLUMN student.tot_cred  IS '已修总学分';

COMMENT ON TABLE section IS '开课班';
COMMENT ON COLUMN section.course_id    IS '课程号（FK→course）';
COMMENT ON COLUMN section.sec_id       IS '开课号';
COMMENT ON COLUMN section.semester     IS '学期（Fall/Spring/Summer）';
COMMENT ON COLUMN section.year         IS '年份';
COMMENT ON COLUMN section.building     IS '教学楼（FK→classroom）';
COMMENT ON COLUMN section.room_number  IS '房间号（FK→classroom）';
COMMENT ON COLUMN section.time_slot_id IS '时间段标识';

COMMENT ON TABLE teaches IS '授课';
COMMENT ON COLUMN teaches.ID        IS '教师编号（FK→instructor）';
COMMENT ON COLUMN teaches.course_id IS '课程号';
COMMENT ON COLUMN teaches.sec_id    IS '开课号';
COMMENT ON COLUMN teaches.semester  IS '学期';
COMMENT ON COLUMN teaches.year      IS '年份';

COMMENT ON TABLE takes IS '选课';
COMMENT ON COLUMN takes.ID        IS '学生编号（FK→student）';
COMMENT ON COLUMN takes.course_id IS '课程号';
COMMENT ON COLUMN takes.sec_id    IS '开课号';
COMMENT ON COLUMN takes.semester  IS '学期';
COMMENT ON COLUMN takes.year      IS '年份';
COMMENT ON COLUMN takes.grade     IS '成绩（A+/A-/B/...）';

COMMENT ON TABLE prereq IS '先修课';
COMMENT ON COLUMN prereq.course_id IS '课程号（FK→course）';
COMMENT ON COLUMN prereq.prereq_id IS '先修课程号（FK→course）';

COMMENT ON TABLE advisor IS '指导关系';
COMMENT ON COLUMN advisor.s_id IS '学生编号（FK→student.ID）';
COMMENT ON COLUMN advisor.i_id IS '指导教师编号（FK→instructor.ID）';

COMMENT ON TABLE time_slot IS '时间段';
COMMENT ON COLUMN time_slot.time_slot_id IS '时间段标识';
COMMENT ON COLUMN time_slot.day          IS '星期（M/W/F）';
COMMENT ON COLUMN time_slot.start_time   IS '开始时间';
COMMENT ON COLUMN time_slot.end_time     IS '结束时间';

-- ============================================================
-- 示例数据（Database System Concepts 附录 A 官方样例）
-- 插入顺序遵循外键依赖：父表先插，子表后插
-- ============================================================

-- 1. department
INSERT INTO department VALUES
    ('Biology',    'Watson',  90000),
    ('Comp. Sci.', 'Taylor', 100000),
    ('Elec. Eng.', 'Taylor',  85000),
    ('Finance',    'Painter', 120000),
    ('History',    'Painter', 50000),
    ('Music',      'Packard', 80000),
    ('Physics',    'Watson',  70000);

-- 2. classroom
INSERT INTO classroom VALUES
    ('Packard', '101',  500),
    ('Painter', '514',  10),
    ('Taylor',  '3128', 70),
    ('Watson',  '100',  30),
    ('Watson',  '120',  50);

-- 3. course
INSERT INTO course VALUES
    ('BIO-101', 'Intro. to Biology',        'Biology',     4),
    ('BIO-301', 'Genetics',                 'Biology',     4),
    ('BIO-399', 'Computational Biology',    'Biology',     3),
    ('CS-101',  'Intro. to Computer Science', 'Comp. Sci.', 4),
    ('CS-190',  'Game Design',              'Comp. Sci.',  4),
    ('CS-315',  'Robotics',                 'Comp. Sci.',  3),
    ('CS-319',  'Image Processing',         'Comp. Sci.',  3),
    ('CS-347',  'Database System Concepts', 'Comp. Sci.',  3),
    ('EE-181',  'Intro. to Digital Systems', 'Elec. Eng.', 3),
    ('FIN-201', 'Investment Banking',       'Finance',     3),
    ('HIS-351', 'World History',            'History',     3),
    ('MU-199',  'Music Video Production',   'Music',       3),
    ('PHY-101', 'Physical Principles',      'Physics',     4);

-- 4. instructor
INSERT INTO instructor (ID, name, dept_name, salary) VALUES
    ('10101', 'Srinivasan', 'Comp. Sci.', 65000),
    ('12121', 'Wu',         'Finance',    90000),
    ('15151', 'Mozart',     'Music',      40000),
    ('22222', 'Einstein',   'Physics',    95000),
    ('32343', 'El Said',    'History',    60000),
    ('33456', 'Gold',       'Physics',    87000),
    ('45565', 'Katz',       'Comp. Sci.', 75000),
    ('58583', 'Califieri',  'History',    62000),
    ('76543', 'Singh',      'Finance',    80000),
    ('76766', 'Crick',      'Biology',    72000),
    ('83821', 'Brandt',     'Comp. Sci.', 92000),
    ('98345', 'Kim',        'Elec. Eng.', 80000);

-- 5. student
INSERT INTO student VALUES
    ('00128', 'Zhang',    'Comp. Sci.', 102),
    ('12345', 'Shankar',  'Comp. Sci.', 32),
    ('19991', 'Brandt',   'History',    80),
    ('23121', 'Chavez',   'Finance',    110),
    ('44553', 'Peltier',  'Physics',    56),
    ('45678', 'Levy',     'Physics',    46),
    ('54321', 'Williams', 'Comp. Sci.', 54),
    ('55739', 'Sanchez',  'Music',      38),
    ('70557', 'Snow',     'Physics',    0),
    ('76543', 'Brown',    'Comp. Sci.', 58),
    ('76653', 'Aoi',      'Elec. Eng.', 60),
    ('98765', 'Bourikas', 'Elec. Eng.', 98),
    ('98988', 'Tanaka',   'Biology',    120);

-- 6. section
INSERT INTO section (course_id, sec_id, semester, year, building, room_number, time_slot_id) VALUES
    ('BIO-101', '1', 'Summer', 2009, 'Painter', '514',  'B'),
    ('BIO-301', '1', 'Summer', 2010, 'Painter', '514',  'A'),
    ('CS-101',  '1', 'Fall',   2009, 'Packard', '101',  'H'),
    ('CS-101',  '1', 'Spring', 2010, 'Packard', '101',  'F'),
    ('CS-190',  '1', 'Spring', 2009, 'Taylor',  '3128', 'E'),
    ('CS-190',  '2', 'Spring', 2009, 'Taylor',  '3128', 'A'),
    ('CS-315',  '1', 'Spring', 2010, 'Watson',  '120',  'D'),
    ('CS-319',  '1', 'Spring', 2010, 'Watson',  '100',  'B'),
    ('CS-319',  '2', 'Spring', 2010, 'Taylor',  '3128', 'C'),
    ('CS-347',  '1', 'Fall',   2009, 'Taylor',  '3128', 'A'),
    ('EE-181',  '1', 'Spring', 2009, 'Taylor',  '3128', 'C'),
    ('FIN-201', '1', 'Spring', 2010, 'Packard', '101',  'B'),
    ('HIS-351', '1', 'Spring', 2010, 'Painter', '514',  'C'),
    ('MU-199',  '1', 'Spring', 2010, 'Packard', '101',  'D'),
    ('PHY-101', '1', 'Fall',   2009, 'Watson',  '100',  'A');

-- 7. teaches
INSERT INTO teaches VALUES
    ('10101', 'CS-101',  '1', 'Fall',   2009),
    ('10101', 'CS-315',  '1', 'Spring', 2010),
    ('10101', 'CS-347',  '1', 'Fall',   2009),
    ('12121', 'FIN-201', '1', 'Spring', 2010),
    ('15151', 'MU-199',  '1', 'Spring', 2010),
    ('22222', 'PHY-101', '1', 'Fall',   2009),
    ('32343', 'HIS-351', '1', 'Spring', 2010),
    ('45565', 'CS-101',  '1', 'Spring', 2010),
    ('45565', 'CS-319',  '1', 'Spring', 2010),
    ('76766', 'BIO-101', '1', 'Summer', 2009),
    ('76766', 'BIO-301', '1', 'Summer', 2010),
    ('83821', 'CS-190',  '1', 'Spring', 2009),
    ('83821', 'CS-190',  '2', 'Spring', 2009),
    ('83821', 'CS-319',  '2', 'Spring', 2010),
    ('98345', 'EE-181',  '1', 'Spring', 2009);

-- 8. takes
INSERT INTO takes VALUES
    ('00128', 'CS-101',  '1', 'Fall',   2009, 'A'),
    ('00128', 'CS-347',  '1', 'Fall',   2009, 'A-'),
    ('12345', 'CS-101',  '1', 'Fall',   2009, 'C'),
    ('12345', 'CS-190',  '2', 'Spring', 2009, 'A'),
    ('12345', 'CS-315',  '1', 'Spring', 2010, 'A'),
    ('12345', 'CS-347',  '1', 'Fall',   2009, 'A'),
    ('19991', 'HIS-351', '1', 'Spring', 2010, 'B'),
    ('23121', 'FIN-201', '1', 'Spring', 2010, 'C+'),
    ('44553', 'PHY-101', '1', 'Fall',   2009, 'B-'),
    ('45678', 'CS-101',  '1', 'Fall',   2009, 'F'),
    ('45678', 'CS-101',  '1', 'Spring', 2010, 'B+'),
    ('45678', 'CS-319',  '1', 'Spring', 2010, 'B'),
    ('54321', 'CS-101',  '1', 'Fall',   2009, 'A-'),
    ('54321', 'CS-190',  '2', 'Spring', 2009, 'B+'),
    ('55739', 'MU-199',  '1', 'Spring', 2010, 'A-'),
    ('70557', 'PHY-101', '1', 'Fall',   2009, 'B'),
    ('76543', 'CS-101',  '1', 'Fall',   2009, 'A+'),
    ('76543', 'CS-319',  '2', 'Spring', 2010, 'A'),
    ('76653', 'EE-181',  '1', 'Spring', 2009, 'C'),
    ('98765', 'CS-101',  '1', 'Fall',   2009, 'C-'),
    ('98765', 'CS-315',  '1', 'Spring', 2010, 'B'),
    ('98988', 'BIO-101', '1', 'Summer', 2009, 'A'),
    ('98988', 'BIO-301', '1', 'Summer', 2010, NULL);

-- 9. prereq
INSERT INTO prereq VALUES
    ('BIO-301', 'BIO-101'),
    ('CS-190',  'CS-101'),
    ('CS-315',  'CS-101'),
    ('CS-319',  'CS-101'),
    ('CS-347',  'CS-101'),
    ('EE-181',  'PHY-101');

-- 10. advisor 示例数据
INSERT INTO advisor VALUES
    ('00128', '45565'),
    ('12345', '10101'),
    ('23121', '76543'),
    ('44553', '22222'),
    ('45678', '22222'),
    ('54321', '45565'),
    ('76543', '45565'),
    ('76653', '98345'),
    ('98765', '98345'),
    ('98988', '76766');

-- 11. time_slot 示例数据
INSERT INTO time_slot VALUES
    ('A', 'M', '08:00:00', '08:50:00'),
    ('A', 'W', '08:00:00', '08:50:00'),
    ('A', 'F', '09:00:00', '09:50:00'),
    ('B', 'M', '09:00:00', '09:50:00'),
    ('B', 'W', '09:00:00', '09:50:00'),
    ('B', 'F', '09:00:00', '09:50:00'),
    ('C', 'M', '11:00:00', '11:50:00'),
    ('C', 'W', '11:00:00', '11:50:00'),
    ('C', 'F', '11:00:00', '11:50:00'),
    ('D', 'M', '13:00:00', '13:50:00'),
    ('D', 'W', '13:00:00', '13:50:00'),
    ('F', 'M', '10:00:00', '10:50:00'),
    ('F', 'W', '10:00:00', '10:50:00'),
    ('F', 'F', '11:00:00', '11:50:00'),
    ('G', 'M', '16:00:00', '16:50:00'),
    ('G', 'W', '16:00:00', '16:50:00'),
    ('G', 'F', '16:00:00', '16:50:00'),
    ('H', 'W', '10:00:00', '10:50:00');
