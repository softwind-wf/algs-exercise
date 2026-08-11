-- ============================================================
-- V2 账号与权限表（RBAC 模型，5 张 sys_* 表）+ 初始数据
--
-- 幂等写法（CREATE TABLE IF NOT EXISTS + INSERT IGNORE）：
-- 存量库重放不会破坏已有账号密码与角色分配。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 账号表 sys_user
--    user_type + ref_id 关联业务主键：
--      STUDENT    -> student.ID（如 '00128'）
--      INSTRUCTOR -> instructor.ID（如 '45565'）
--      ADMIN      -> ref_id 为 NULL
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    user_id     VARCHAR(20)  NOT NULL COMMENT '登录账号（主键）',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 哈希，不存明文）',
    user_type   VARCHAR(20)  NOT NULL COMMENT '关联类型：STUDENT / INSTRUCTOR / ADMIN',
    ref_id      VARCHAR(5)   NULL     COMMENT '关联业务主键：student.ID / instructor.ID；ADMIN 为 NULL',
    enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：1 启用 / 0 禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (user_id)
) ENGINE = InnoDB COMMENT = '系统账号';

-- ------------------------------------------------------------
-- 2. 角色表 sys_role
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    role_id     VARCHAR(20)  NOT NULL COMMENT '角色标识（主键）：STUDENT / INSTRUCTOR / ADMIN',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    description VARCHAR(200) NULL     COMMENT '角色说明',
    PRIMARY KEY (role_id)
) ENGINE = InnoDB COMMENT = '角色';

-- ------------------------------------------------------------
-- 3. 权限表 sys_permission
--    权限编码格式：资源:操作，如 course:view / course:manage
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_permission (
    permission_id INT          NOT NULL AUTO_INCREMENT COMMENT '权限ID（自增主键）',
    perm_code     VARCHAR(50)  NOT NULL COMMENT '权限编码（如 course:view）',
    perm_name     VARCHAR(50)  NOT NULL COMMENT '权限名称',
    description   VARCHAR(200) NULL     COMMENT '权限说明',
    PRIMARY KEY (permission_id),
    UNIQUE KEY uk_permission_code (perm_code)
) ENGINE = InnoDB COMMENT = '权限';

-- ------------------------------------------------------------
-- 4. 用户-角色 关联 sys_user_role（一个用户可有多个角色）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id VARCHAR(20) NOT NULL COMMENT '账号（FK->sys_user）',
    role_id VARCHAR(20) NOT NULL COMMENT '角色（FK->sys_role）',
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (user_id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (role_id)
) ENGINE = InnoDB COMMENT = '用户-角色关联';

-- ------------------------------------------------------------
-- 5. 角色-权限 关联 sys_role_permission
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id       VARCHAR(20) NOT NULL COMMENT '角色（FK->sys_role）',
    permission_id INT         NOT NULL COMMENT '权限（FK->sys_permission）',
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES sys_role (role_id),
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES sys_permission (permission_id)
) ENGINE = InnoDB COMMENT = '角色-权限关联';

-- ============================================================
-- 初始数据（INSERT IGNORE，重放安全）
-- ============================================================

-- 角色
INSERT IGNORE INTO sys_role (role_id, role_name, description) VALUES
    ('STUDENT',    '学生',   '浏览课程、选课/退课、查看成绩单与导师'),
    ('INSTRUCTOR', '教师',   '查看授课任务、班级名单、录入/修改成绩'),
    ('ADMIN',      '管理员', '维护基础数据、分配账号与权限、查看统计报表');

-- 权限点（资源:操作）
INSERT IGNORE INTO sys_permission (perm_code, perm_name, description) VALUES
    ('department:view',     '查看系',     '浏览系列表与详情'),
    ('department:manage',   '管理系',     '系的新增/修改/删除'),
    ('course:view',         '查看课程',   '浏览课程目录与详情'),
    ('course:manage',       '管理课程',   '课程的新增/修改/删除'),
    ('instructor:view',     '查看教师',   '浏览教师列表与详情'),
    ('instructor:manage',   '管理教师',   '教师的新增/修改/删除'),
    ('student:view',        '查看学生',   '浏览学生信息'),
    ('student:manage',      '管理学生',   '学生的新增/修改/删除'),
    ('section:view',        '查看开课班', '浏览开课班信息'),
    ('section:manage',      '排课管理',   '开课班的新增/修改/删除'),
    ('classroom:manage',    '管理教室',   '教室的新增/修改/删除'),
    ('prereq:manage',       '管理先修',   '先修关系的新增/删除'),
    ('take:enroll',         '选课/退课',  '学生选课与退课'),
    ('take:grade',          '成绩录入',   '教师录入/修改成绩'),
    ('take:transcript',     '成绩单',     '查看本人成绩单'),
    ('advisor:view',        '导师查询',   '查看导师信息'),
    ('stats:view',          '统计报表',   '查看统计报表'),
    ('user:manage',         '用户管理',   '账号与权限管理');

-- 角色-权限 映射（IGNORE：已有映射跳过）
-- 学生：可浏览 + 选课/成绩单/导师
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 'STUDENT', permission_id FROM sys_permission
WHERE perm_code IN (
    'department:view', 'course:view', 'instructor:view', 'section:view',
    'student:view', 'take:enroll', 'take:transcript', 'advisor:view'
);

-- 教师：可浏览 + 班级名单 + 成绩录入
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 'INSTRUCTOR', permission_id FROM sys_permission
WHERE perm_code IN (
    'department:view', 'course:view', 'instructor:view', 'section:view',
    'student:view', 'take:grade'
);

-- 管理员：全部权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 'ADMIN', permission_id FROM sys_permission;

-- 演示账号（密码为 BCrypt 哈希，默认密码均为 password）
--   对应哈希：$2a$10$/Gu.uRug7LYoOu0PzCdVKOqo4Ayxt3fM2utBEet4jNQ5nouNqojKO
--   ⚠️ 仅用于开发/演示，上线前必须重置。
--   IGNORE：账号已存在时不覆盖（避免冲掉线上已改的密码）。
INSERT IGNORE INTO sys_user (user_id, password, user_type, ref_id, enabled) VALUES
    ('zhang', '$2a$10$/Gu.uRug7LYoOu0PzCdVKOqo4Ayxt3fM2utBEet4jNQ5nouNqojKO', 'STUDENT',    '00128', 1),
    ('katz',  '$2a$10$/Gu.uRug7LYoOu0PzCdVKOqo4Ayxt3fM2utBEet4jNQ5nouNqojKO', 'INSTRUCTOR', '45565', 1),
    ('admin', '$2a$10$/Gu.uRug7LYoOu0PzCdVKOqo4Ayxt3fM2utBEet4jNQ5nouNqojKO', 'ADMIN',      NULL,    1);

-- 用户-角色 映射
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
    ('zhang', 'STUDENT'),
    ('katz',  'INSTRUCTOR'),
    ('admin', 'ADMIN');
