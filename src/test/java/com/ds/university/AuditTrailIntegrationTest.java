package com.ds.university;

import com.ds.university.service.AccountService;
import com.ds.university.service.AdminService;
import com.ds.university.service.TeacherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审计留痕集成测试：真实 MySQL + MyBatis，验证改成绩、删数据、建账号等
 * 敏感操作都会写入 audit_log（含变更前后值），且无会话时操作者优雅降级
 * 为 NULL。所有用例运行在事务中，结束后整体回滚。
 */
@SpringBootTest
@Transactional
class AuditTrailIntegrationTest {

    private static final String SEMESTER = "Spring";
    private static final int YEAR = 2010;
    /** 演示数据中的教师（Srinivasan），用于成绩录入权限校验 */
    private static final String INSTRUCTOR = "10101";

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TeacherService teacherService;
    @Autowired
    private AdminService adminService;
    @Autowired
    private AccountService accountService;

    /** 改成绩留痕：detail 含旧成绩 -> 新成绩 */
    @Test
    void gradeUpdateLeavesAuditTrail() {
        setupStudent("T0001", "测试学生");
        insertCourse("TEST-1", "测试课程", 3);
        insertSection("TEST-1", "1", "Packard", "101", "A");
        insertTeaches("TEST-1", "1");
        insertTakes("T0001", "TEST-1", null);

        teacherService.updateGrade(INSTRUCTOR, "T0001", "TEST-1", "1", SEMESTER, YEAR, "A");

        Map<String, Object> audit = latestAudit("GRADE", "T0001/TEST-1/1/Spring/2010");
        assertEquals("GRADE_UPDATE", audit.get("action"));
        String detail = (String) audit.get("detail");
        assertTrue(detail.contains("教师 " + INSTRUCTOR), "detail 应含操作教师：" + detail);
        assertTrue(detail.contains("无 -> A"), "detail 应含旧成绩到新成绩：" + detail);
        // 无会话（未登录调用）：操作者记为 NULL，不阻断业务。
        // 注：@SpringBootTest 会预置 mock 请求（remoteAddr=127.0.0.1），故 IP 非 NULL。
        assertNull(audit.get("user_id"), "无会话时操作者应为 NULL");
        assertEquals("127.0.0.1", audit.get("client_ip"), "mock 请求上下文的来源 IP 应为 127.0.0.1");

        // 再改一次成绩：detail 记录 B（旧值）而不是最初值
        teacherService.updateGrade(INSTRUCTOR, "T0001", "TEST-1", "1", SEMESTER, YEAR, "B");
        Map<String, Object> second = latestAudit("GRADE", "T0001/TEST-1/1/Spring/2010");
        assertTrue(((String) second.get("detail")).contains("A -> B"), "第二次修改应以 A 为旧值");
    }

    /** 删数据留痕：删除课程后 audit_log 有 DELETE COURSE 记录 */
    @Test
    void deleteCourseLeavesAuditTrail() {
        insertCourse("AUD-DEL", "待删课程", 3);

        adminService.deleteCourse("AUD-DEL");

        Map<String, Object> audit = latestAudit("COURSE", "AUD-DEL");
        assertEquals("DELETE", audit.get("action"));
        assertTrue(((String) audit.get("detail")).contains("删除课程：AUD-DEL"));
    }

    /** 建学生留痕：业务行 CREATE STUDENT + 自动开户 ACCOUNT_CREATE 各一条 */
    @Test
    void createStudentLeavesBusinessAndAccountTrails() {
        adminService.createStudent("T9001", "审计学生", "Comp. Sci.", 0);

        Map<String, Object> studentAudit = latestAudit("STUDENT", "T9001");
        assertEquals("CREATE", studentAudit.get("action"));
        assertTrue(((String) studentAudit.get("detail")).contains("新建学生：T9001"));

        Map<String, Object> accountAudit = latestAudit("ACCOUNT", "T9001");
        assertEquals("ACCOUNT_CREATE", accountAudit.get("action"));
        assertTrue(((String) accountAudit.get("detail")).contains("开户：T9001"));
    }

    /** 账号操作留痕：重置密码与启用/禁用均写入 audit_log */
    @Test
    void accountOpsLeaveAuditTrail() {
        setupStudent("T9002", "审计学生二");
        accountService.createAccount("audit9002", "STUDENT", "T9002", null);

        accountService.resetPassword("audit9002", null);
        Map<String, Object> reset = latestAuditByAction("PASSWORD_RESET", "ACCOUNT", "audit9002");
        assertTrue(((String) reset.get("detail")).contains("重置密码：audit9002"));

        boolean enabled = accountService.toggleEnabled("audit9002");
        Map<String, Object> toggle = latestAuditByAction("ACCOUNT_TOGGLE", "ACCOUNT", "audit9002");
        assertTrue(((String) toggle.get("detail")).contains(enabled ? "启用账号：audit9002" : "禁用账号：audit9002"));
    }

    // ========== 测试数据准备与断言辅助 ==========

    private Map<String, Object> latestAudit(String targetType, String targetId) {
        return jdbcTemplate.queryForMap(
                "SELECT action, target_type, target_id, detail, user_id, client_ip FROM audit_log " +
                        "WHERE target_type = ? AND target_id = ? ORDER BY id DESC LIMIT 1",
                targetType, targetId);
    }

    private Map<String, Object> latestAuditByAction(String action, String targetType, String targetId) {
        return jdbcTemplate.queryForMap(
                "SELECT action, target_type, target_id, detail, user_id, client_ip FROM audit_log " +
                        "WHERE action = ? AND target_type = ? AND target_id = ? ORDER BY id DESC LIMIT 1",
                action, targetType, targetId);
    }

    private void setupStudent(String id, String name) {
        jdbcTemplate.update("INSERT INTO student (ID, name, dept_name, tot_cred) VALUES (?, ?, 'Comp. Sci.', 0)",
                id, name);
    }

    private void insertCourse(String courseId, String title, int credits) {
        jdbcTemplate.update("INSERT INTO course (course_id, title, dept_name, credits) VALUES (?, ?, 'Comp. Sci.', ?)",
                courseId, title, credits);
    }

    private void insertSection(String courseId, String secId, String building, String room, String timeSlotId) {
        jdbcTemplate.update("INSERT INTO section (course_id, sec_id, semester, year, building, room_number, time_slot_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                courseId, secId, SEMESTER, YEAR, building, room, timeSlotId);
    }

    private void insertTeaches(String courseId, String secId) {
        jdbcTemplate.update("INSERT INTO teaches (ID, course_id, sec_id, semester, year) VALUES (?, ?, ?, ?, ?)",
                INSTRUCTOR, courseId, secId, SEMESTER, YEAR);
    }

    private void insertTakes(String studentId, String courseId, String grade) {
        jdbcTemplate.update("INSERT INTO takes (ID, course_id, sec_id, semester, year, grade) " +
                        "VALUES (?, ?, '1', ?, ?, ?)",
                studentId, courseId, SEMESTER, YEAR, grade);
    }
}
