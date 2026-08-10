-- ============================================================
-- 大学数据库完整设计（Database System Concepts 教材经典模型）
-- 数据库: university  字符集: utf8mb4
--
-- 共 9 张表：
--   实体集 5 张：department / course / instructor / student / classroom
--   联系集 4 张：section(开课班) / teaches(授课) / takes(选课) / prereq(先修)
--
-- ⚠️ 脚本开头会 DROP DATABASE IF EXISTS university，如已有数据将被清除
-- ============================================================

DROP DATABASE IF EXISTS university;
CREATE DATABASE university DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE university;

-- ------------------------------------------------------------
-- 1. 系 department
-- ------------------------------------------------------------
CREATE TABLE department (
    dept_name VARCHAR(20) PRIMARY KEY COMMENT '系名称（主键）',
    building  VARCHAR(15) NULL     COMMENT '所在教学楼',
    budget    DECIMAL(12, 2) NULL  COMMENT '经费预算'
) ENGINE = InnoDB COMMENT = '系';

-- ------------------------------------------------------------
-- 2. 教室 classroom（联合主键）
-- ------------------------------------------------------------
CREATE TABLE classroom (
    building    VARCHAR(15) NOT NULL COMMENT '教学楼',
    room_number VARCHAR(7)  NOT NULL COMMENT '房间号',
    capacity    INT         NULL     COMMENT '容量',
    PRIMARY KEY (building, room_number)
) ENGINE = InnoDB COMMENT = '教室';

-- ------------------------------------------------------------
-- 3. 课程 course
-- ------------------------------------------------------------
CREATE TABLE course (
    course_id VARCHAR(8)  PRIMARY KEY COMMENT '课程号（主键）',
    title     VARCHAR(50) NULL     COMMENT '课程名',
    dept_name VARCHAR(20) NULL     COMMENT '所属系（FK→department）',
    credits   DECIMAL(2, 0) NULL    COMMENT '学分',
    CONSTRAINT fk_course_department FOREIGN KEY (dept_name)
        REFERENCES department (dept_name)
) ENGINE = InnoDB COMMENT = '课程';

-- ------------------------------------------------------------
-- 4. 教师 instructor
-- ------------------------------------------------------------
CREATE TABLE instructor (
    ID        VARCHAR(5)   PRIMARY KEY COMMENT '教师编号（主键）',
    name      VARCHAR(20)  NOT NULL   COMMENT '姓名',
    dept_name VARCHAR(20)  NULL       COMMENT '所属系（FK→department）',
    salary    DECIMAL(8, 2) NULL       COMMENT '工资',
    CONSTRAINT fk_instructor_department FOREIGN KEY (dept_name)
        REFERENCES department (dept_name)
) ENGINE = InnoDB COMMENT = '教师';

-- ------------------------------------------------------------
-- 5. 学生 student
-- ------------------------------------------------------------
CREATE TABLE student (
    ID        VARCHAR(5)  PRIMARY KEY COMMENT '学生编号（主键）',
    name      VARCHAR(20) NOT NULL    COMMENT '姓名',
    dept_name VARCHAR(20) NULL        COMMENT '主修系（FK→department）',
    tot_cred  INT         NULL        COMMENT '已修总学分',
    CONSTRAINT fk_student_department FOREIGN KEY (dept_name)
        REFERENCES department (dept_name)
) ENGINE = InnoDB COMMENT = '学生';

-- ------------------------------------------------------------
-- 6. 开课班 section
--    (course_id, sec_id, semester, year) 联合主键
-- ------------------------------------------------------------
CREATE TABLE section (
    course_id    VARCHAR(8)  NOT NULL COMMENT '课程号（FK→course）',
    sec_id       VARCHAR(8)  NOT NULL COMMENT '开课号',
    semester     VARCHAR(6)  NOT NULL COMMENT '学期（Fall/Spring/Summer）',
    year         SMALLINT    NOT NULL COMMENT '年份',
    building     VARCHAR(15) NULL     COMMENT '教学楼（FK→classroom）',
    room_number  VARCHAR(7)  NULL     COMMENT '房间号（FK→classroom）',
    time_slot_id VARCHAR(4)  NULL     COMMENT '时间段标识',
    PRIMARY KEY (course_id, sec_id, semester, year),
    CONSTRAINT fk_section_course FOREIGN KEY (course_id)
        REFERENCES course (course_id),
    -- 复合外键：引用 classroom 的联合主键 (building, room_number)
    CONSTRAINT fk_section_classroom FOREIGN KEY (building, room_number)
        REFERENCES classroom (building, room_number)
) ENGINE = InnoDB COMMENT = '开课班';

-- ------------------------------------------------------------
-- 7. 授课 teaches：Instructor ⇋ Section（多对多）
-- ------------------------------------------------------------
CREATE TABLE teaches (
    ID        VARCHAR(5) NOT NULL COMMENT '教师编号（FK→instructor）',
    course_id VARCHAR(8) NOT NULL COMMENT '课程号',
    sec_id    VARCHAR(8) NOT NULL COMMENT '开课号',
    semester  VARCHAR(6) NOT NULL COMMENT '学期',
    year      SMALLINT   NOT NULL COMMENT '年份',
    PRIMARY KEY (ID, course_id, sec_id, semester, year),
    CONSTRAINT fk_teaches_instructor FOREIGN KEY (ID)
        REFERENCES instructor (ID),
    -- 复合外键：引用 section 的联合主键
    CONSTRAINT fk_teaches_section FOREIGN KEY (course_id, sec_id, semester, year)
        REFERENCES section (course_id, sec_id, semester, year)
) ENGINE = InnoDB COMMENT = '授课';

-- ------------------------------------------------------------
-- 8. 选课 takes：Student ⇋ Section，带属性 grade 成绩
-- ------------------------------------------------------------
CREATE TABLE takes (
    ID        VARCHAR(5) NOT NULL COMMENT '学生编号（FK→student）',
    course_id VARCHAR(8) NOT NULL COMMENT '课程号',
    sec_id    VARCHAR(8) NOT NULL COMMENT '开课号',
    semester  VARCHAR(6) NOT NULL COMMENT '学期',
    year      SMALLINT   NOT NULL COMMENT '年份',
    grade     VARCHAR(2) NULL     COMMENT '成绩（A+/A-/B/...）',
    PRIMARY KEY (ID, course_id, sec_id, semester, year),
    CONSTRAINT fk_takes_student FOREIGN KEY (ID)
        REFERENCES student (ID),
    -- 复合外键：引用 section 的联合主键
    CONSTRAINT fk_takes_section FOREIGN KEY (course_id, sec_id, semester, year)
        REFERENCES section (course_id, sec_id, semester, year)
) ENGINE = InnoDB COMMENT = '选课';

-- ------------------------------------------------------------
-- 9. 先修课 prereq：Course ⇋ Course（自引用）
-- ------------------------------------------------------------
CREATE TABLE prereq (
    course_id VARCHAR(8) NOT NULL COMMENT '课程号（FK→course）',
    prereq_id VARCHAR(8) NOT NULL COMMENT '先修课程号（FK→course）',
    PRIMARY KEY (course_id, prereq_id),
    CONSTRAINT fk_prereq_course FOREIGN KEY (course_id)
        REFERENCES course (course_id),
    CONSTRAINT fk_prereq_course_prereq FOREIGN KEY (prereq_id)
        REFERENCES course (course_id)
) ENGINE = InnoDB COMMENT = '先修课';

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
INSERT INTO instructor VALUES
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

-- ------------------------------------------------------------
-- 10. 指导关系 advisor：Student ⇋ Instructor（一个学生一个导师）
-- ------------------------------------------------------------
CREATE TABLE advisor (
    s_id VARCHAR(5) PRIMARY KEY COMMENT '学生编号（FK→student.ID）',
    i_id VARCHAR(5) NULL     COMMENT '指导教师编号（FK→instructor.ID）',
    CONSTRAINT fk_advisor_student FOREIGN KEY (s_id)
        REFERENCES student (ID),
    CONSTRAINT fk_advisor_instructor FOREIGN KEY (i_id)
        REFERENCES instructor (ID)
) ENGINE = InnoDB COMMENT = '指导关系';

-- ------------------------------------------------------------
-- 11. 时间段 time_slot（(time_slot_id, day, start_time) 联合主键）
-- ------------------------------------------------------------
CREATE TABLE time_slot (
    time_slot_id VARCHAR(4) NOT NULL COMMENT '时间段标识',
    day          VARCHAR(1) NOT NULL COMMENT '星期（M/W/F）',
    start_time   TIME       NOT NULL COMMENT '开始时间',
    end_time     TIME       NULL     COMMENT '结束时间',
    PRIMARY KEY (time_slot_id, day, start_time)
) ENGINE = InnoDB COMMENT = '时间段';

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
