package com.ds.university;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.mapper.StatsMapper;
import com.ds.university.mapper.StudentMapper;
import com.ds.university.mapper.TakesMapper;
import com.ds.university.service.StatsReportService;
import com.ds.university.service.StudentService;
import com.ds.university.service.TeacherService;
import com.ds.university.vo.DeptBudgetVO;
import com.ds.university.vo.DeptSalaryVO;
import com.ds.university.vo.EnrollmentReportVO;
import com.ds.university.vo.GradeCountVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M6 集成测试：真实 MySQL + MyBatis，验证 UC-09 统计查询与选课/退课/成绩业务流。
 * 所有用例运行在事务中，结束后整体回滚，不污染示例数据。
 */
@SpringBootTest
@Transactional
class UniversityIntegrationTest {

    private static final String SEMESTER = "Spring";
    private static final int YEAR = 2010;
    /** 演示数据中的教师（Srinivasan），用于成绩录入权限校验 */
    private static final String INSTRUCTOR = "10101";

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private StatsMapper statsMapper;
    @Autowired
    private StatsReportService statsReportService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private TeacherService teacherService;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private TakesMapper takesMapper;

    // ========== UC-09 统计报表 ==========

    @Test
    void statsQueriesReturnExpectedRows() {
        List<DeptBudgetVO> budgets = statsMapper.selectDeptBudget();
        assertFalse(budgets.isEmpty());
        assertTrue(budgets.stream().anyMatch(b -> "Comp. Sci.".equals(b.getDeptName())));
        assertTrue(budgets.stream().allMatch(b -> b.getBudget() != null));

        List<DeptSalaryVO> salaries = statsMapper.selectSalaryByDept();
        assertFalse(salaries.isEmpty());
        assertTrue(salaries.stream().allMatch(s -> s.getAvgSalary() != null
                && s.getInstructorCount() != null && s.getInstructorCount() > 0));

        List<EnrollmentReportVO> enrollments = statsMapper.selectEnrollment(SEMESTER, YEAR, null);
        assertFalse(enrollments.isEmpty());
        EnrollmentReportVO cs101 = enrollments.stream()
                .filter(e -> "CS-101".equals(e.getCourseId()) && "1".equals(e.getSecId()))
                .findFirst().orElse(null);
        assertNotNull(cs101, "Spring 2010 应包含 CS-101 开课班");
        assertTrue(cs101.getEnrolled() != null && cs101.getEnrolled() >= 1);

        // 课程筛选
        List<EnrollmentReportVO> filtered = statsMapper.selectEnrollment(SEMESTER, YEAR, "CS-101");
        assertFalse(filtered.isEmpty());
        assertTrue(filtered.stream().allMatch(e -> "CS-101".equals(e.getCourseId())));

        List<GradeCountVO> grades = statsMapper.selectGradeDistribution("CS-101");
        assertFalse(grades.isEmpty());
    }

    @Test
    void statsReportServiceComputesPercentages() {
        List<DeptBudgetVO> budgets = statsReportService.deptBudget();
        assertFalse(budgets.isEmpty());
        assertTrue(budgets.stream().allMatch(b -> b.getBudgetPercent() != null
                && b.getBudgetPercent() >= 0 && b.getBudgetPercent() <= 100));

        List<DeptSalaryVO> salaries = statsReportService.salaryByDept();
        assertFalse(salaries.isEmpty());
        assertTrue(salaries.stream().allMatch(s -> s.getAvgPercent() != null));

        List<EnrollmentReportVO> enrollments = statsReportService.enrollment(SEMESTER, YEAR, null);
        assertFalse(enrollments.isEmpty());
        assertTrue(enrollments.stream()
                .filter(e -> e.getCapacity() != null)
                .allMatch(e -> e.getUtilization() != null
                        && e.getUtilization() >= 0 && e.getUtilization() <= 100));

        List<GradeCountVO> grades = statsReportService.gradeDistribution("CS-101");
        assertFalse(grades.isEmpty());
        // 未出分固定排在最后
        boolean sawUnGraded = false;
        for (GradeCountVO g : grades) {
            if ("未出分".equals(g.getGrade())) {
                sawUnGraded = true;
            } else {
                assertFalse(sawUnGraded, "未出分应排在成绩分布最后");
            }
        }
    }

    // ========== 选课 / 退课 / 成绩业务流 ==========

    @Test
    void enrollmentGradeDropFlowUpdatesTotCred() {
        setupStudent("T0001", "测试学生");
        insertCourse("TEST-1", "测试课程", 3);
        insertSection("TEST-1", "1", "Packard", "101", "A");
        insertTeaches("TEST-1", "1");

        // 选课成功
        studentService.enroll("T0001", "TEST-1", "1", SEMESTER, YEAR);
        assertEquals(1, takesMapper.exists("T0001", "TEST-1", "1", SEMESTER, YEAR));

        // 录入成绩 A（3 学分）→ 已修总学分重算为 3
        teacherService.updateGrade(INSTRUCTOR, "T0001", "TEST-1", "1", SEMESTER, YEAR, "A");
        assertEquals(3, totCred("T0001"));

        // 已出成绩的课程不能退选
        BusinessException cannotDrop = assertThrows(BusinessException.class,
                () -> studentService.drop("T0001", "TEST-1", "1", SEMESTER, YEAR));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), cannotDrop.getCode());

        // 清除成绩 → 已修总学分归零
        teacherService.updateGrade(INSTRUCTOR, "T0001", "TEST-1", "1", SEMESTER, YEAR, "");
        assertEquals(0, totCred("T0001"));

        // 非法成绩被拒
        BusinessException badGrade = assertThrows(BusinessException.class,
                () -> teacherService.updateGrade(INSTRUCTOR, "T0001", "TEST-1", "1", SEMESTER, YEAR, "Z"));
        assertEquals(ErrorCode.INVALID_GRADE.getCode(), badGrade.getCode());

        // 退课成功
        studentService.drop("T0001", "TEST-1", "1", SEMESTER, YEAR);
        assertEquals(0, takesMapper.exists("T0001", "TEST-1", "1", SEMESTER, YEAR));
    }

    @Test
    void enrollRejectsDuplicate() {
        setupStudent("T0001", "测试学生");
        insertCourse("TEST-1", "测试课程", 3);
        insertSection("TEST-1", "1", "Packard", "101", "A");

        studentService.enroll("T0001", "TEST-1", "1", SEMESTER, YEAR);
        BusinessException dup = assertThrows(BusinessException.class,
                () -> studentService.enroll("T0001", "TEST-1", "1", SEMESTER, YEAR));
        assertEquals(ErrorCode.DUPLICATE_ENROLL.getCode(), dup.getCode());
    }

    @Test
    void enrollRejectsWhenPrereqNotMet() {
        setupStudent("T0001", "测试学生");
        insertCourse("TEST-3", "测试课程3", 3);
        insertCourse("TEST-4", "测试课程4", 3);
        jdbcTemplate.update("INSERT INTO prereq (course_id, prereq_id) VALUES ('TEST-3', 'TEST-4')");
        insertSection("TEST-3", "1", "Packard", "101", "A");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> studentService.enroll("T0001", "TEST-3", "1", SEMESTER, YEAR));
        assertEquals(ErrorCode.PREREQ_NOT_DONE.getCode(), ex.getCode());
    }

    @Test
    void enrollRejectsWhenSectionFull() {
        setupStudent("T0001", "测试学生");
        setupStudent("T0002", "测试学生二");
        insertCourse("TEST-2", "测试课程2", 4);
        jdbcTemplate.update("INSERT INTO classroom (building, room_number, capacity) VALUES ('TESTB', '1', 1)");
        jdbcTemplate.update("INSERT INTO section (course_id, sec_id, semester, year, building, room_number, time_slot_id) " +
                        "VALUES ('TEST-2', '1', ?, ?, 'TESTB', '1', 'B')",
                SEMESTER, YEAR);

        studentService.enroll("T0001", "TEST-2", "1", SEMESTER, YEAR);
        BusinessException full = assertThrows(BusinessException.class,
                () -> studentService.enroll("T0002", "TEST-2", "1", SEMESTER, YEAR));
        assertEquals(ErrorCode.SECTION_FULL.getCode(), full.getCode());
    }

    @Test
    void gradeValidationRejectsInvalidValue() {
        setupStudent("T0001", "测试学生");
        insertCourse("TEST-1", "测试课程", 3);
        insertSection("TEST-1", "1", "Packard", "101", "A");
        insertTeaches("TEST-1", "1");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teacherService.updateGrade(INSTRUCTOR, "T0001", "TEST-1", "1", SEMESTER, YEAR, "Z"));
        assertEquals(ErrorCode.INVALID_GRADE.getCode(), ex.getCode());
    }

    // ========== 测试数据准备 ==========

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

    private Integer totCred(String studentId) {
        return jdbcTemplate.queryForObject("SELECT tot_cred FROM student WHERE ID = ?", Integer.class, studentId);
    }
}