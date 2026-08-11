package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.common.Result;
import com.ds.university.entity.Section;
import com.ds.university.service.AdminService;
import com.ds.university.service.AccountService;
import com.ds.university.service.AuditService;
import com.ds.university.service.StatsReportService;
import com.ds.university.service.TermDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Arrays;

/** 教务管理（M5）：基础数据维护、排课、先修管理（UC-07/08）。 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final AccountService accountService;
    private final StatsReportService statsReportService;
    private final TermDefaults termDefaults;
    private final AuditService auditService;

    public AdminController(AdminService adminService, StatsReportService statsReportService,
                           AccountService accountService, TermDefaults termDefaults,
                           AuditService auditService) {
        this.adminService = adminService;
        this.statsReportService = statsReportService;
        this.accountService = accountService;
        this.termDefaults = termDefaults;
        this.auditService = auditService;
    }

    /** 教务管理首页：统计概览 */
    @GetMapping
    public String index(Model model) {
        model.addAttribute("departmentCount", adminService.countDepartments());
        model.addAttribute("courseCount", adminService.countCourses());
        model.addAttribute("instructorCount", adminService.countInstructors());
        model.addAttribute("studentCount", adminService.countStudents());
        model.addAttribute("sectionCount", adminService.countSections());
        model.addAttribute("classroomCount", adminService.countClassrooms());
        model.addAttribute("prereqCount", adminService.countPrereqs());
        return "admin/index";
    }

    // ========== 审计日志 ==========

    /** 审计日志：改成绩、删数据、建账号等敏感操作的追溯页面（只读，GET 筛选） */
    @GetMapping("/audit")
    public String audit(@RequestParam(required = false) String action,
                        @RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("logs", auditService.query(action, keyword, AuditService.QUERY_LIMIT));
        model.addAttribute("actions", AuditService.ALL_ACTIONS);
        model.addAttribute("selAction", action == null ? "" : action);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "admin/audit";
    }

    // ========== 院系 ==========

    @GetMapping("/departments")
    public String departments(@RequestParam(required = false) String edit, Model model) {
        model.addAttribute("departments", adminService.departments());
        if (edit != null && !edit.isEmpty()) {
            model.addAttribute("editDept", adminService.departmentById(edit));
        }
        return "admin/departments";
    }

    @PostMapping("/departments")
    public String createDepartment(@RequestParam String deptName,
                                   @RequestParam(required = false) String building,
                                   @RequestParam(required = false) BigDecimal budget,
                                   RedirectAttributes ra) {
        try {
            adminService.createDepartment(deptName, building, budget);
            ra.addFlashAttribute("success", "院系已创建");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/update")
    public String updateDepartment(@RequestParam String deptName,
                                   @RequestParam(required = false) String building,
                                   @RequestParam(required = false) BigDecimal budget,
                                   RedirectAttributes ra) {
        try {
            adminService.updateDepartment(deptName, building, budget);
            ra.addFlashAttribute("success", "院系已更新");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/delete")
    public String deleteDepartment(@RequestParam String deptName, RedirectAttributes ra) {
        try {
            adminService.deleteDepartment(deptName);
            ra.addFlashAttribute("success", "院系已删除");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    // ========== 课程 ==========

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) String edit, Model model) {
        model.addAttribute("courses", adminService.courses());
        model.addAttribute("departments", adminService.departments());
        if (edit != null && !edit.isEmpty()) {
            model.addAttribute("editCourse", adminService.courseById(edit));
        }
        return "admin/courses";
    }

    @PostMapping("/courses")
    public String createCourse(@RequestParam String courseId,
                               @RequestParam String title,
                               @RequestParam String deptName,
                               @RequestParam BigDecimal credits,
                               RedirectAttributes ra) {
        try {
            adminService.createCourse(courseId, title, deptName, credits);
            ra.addFlashAttribute("success", "课程已创建");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    @PostMapping("/courses/update")
    public String updateCourse(@RequestParam String courseId,
                               @RequestParam String title,
                               @RequestParam String deptName,
                               @RequestParam BigDecimal credits,
                               RedirectAttributes ra) {
        try {
            adminService.updateCourse(courseId, title, deptName, credits);
            ra.addFlashAttribute("success", "课程已更新");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    @PostMapping("/courses/delete")
    public String deleteCourse(@RequestParam String courseId, RedirectAttributes ra) {
        try {
            adminService.deleteCourse(courseId);
            ra.addFlashAttribute("success", "课程已删除");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    // ========== 教师 ==========

    @GetMapping("/instructors")
    public String instructors(@RequestParam(required = false) String edit, Model model) {
        model.addAttribute("instructors", adminService.instructors());
        model.addAttribute("departments", adminService.departments());
        if (edit != null && !edit.isEmpty()) {
            model.addAttribute("editInstructor", adminService.instructorById(edit));
        }
        return "admin/instructors";
    }

    @PostMapping("/instructors")
    public String createInstructor(@RequestParam String id,
                                   @RequestParam String name,
                                   @RequestParam String deptName,
                                   @RequestParam(required = false) BigDecimal salary,
                                   RedirectAttributes ra) {
        try {
            adminService.createInstructor(id, name, deptName, salary);
            ra.addFlashAttribute("success", "教师已创建，登录账号 = " + id + "，初始密码 = " + AccountService.DEFAULT_PASSWORD);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/instructors";
    }

    @PostMapping("/instructors/update")
    public String updateInstructor(@RequestParam String id,
                                   @RequestParam String name,
                                   @RequestParam String deptName,
                                   @RequestParam(required = false) BigDecimal salary,
                                   RedirectAttributes ra) {
        try {
            adminService.updateInstructor(id, name, deptName, salary);
            ra.addFlashAttribute("success", "教师已更新");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/instructors";
    }

    @PostMapping("/instructors/delete")
    public String deleteInstructor(@RequestParam String id, RedirectAttributes ra) {
        try {
            adminService.deleteInstructor(id);
            ra.addFlashAttribute("success", "教师已删除");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/instructors";
    }

    // ========== 学生 ==========

    @GetMapping("/students")
    public String students(@RequestParam(required = false) String edit, Model model) {
        model.addAttribute("students", adminService.students());
        model.addAttribute("departments", adminService.departments());
        if (edit != null && !edit.isEmpty()) {
            model.addAttribute("editStudent", adminService.studentById(edit));
        }
        return "admin/students";
    }

    @PostMapping("/students")
    public String createStudent(@RequestParam String id,
                                @RequestParam String name,
                                @RequestParam String deptName,
                                @RequestParam(required = false) Integer totCred,
                                RedirectAttributes ra) {
        try {
            adminService.createStudent(id, name, deptName, totCred);
            ra.addFlashAttribute("success", "学生已创建，登录账号 = " + id + "，初始密码 = " + AccountService.DEFAULT_PASSWORD);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/students/update")
    public String updateStudent(@RequestParam String id,
                                @RequestParam String name,
                                @RequestParam String deptName,
                                @RequestParam(required = false) Integer totCred,
                                RedirectAttributes ra) {
        try {
            adminService.updateStudent(id, name, deptName, totCred);
            ra.addFlashAttribute("success", "学生已更新");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/students/delete")
    public String deleteStudent(@RequestParam String id, RedirectAttributes ra) {
        try {
            adminService.deleteStudent(id);
            ra.addFlashAttribute("success", "学生已删除");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/students";
    }

    // ========== 教室 ==========

    @GetMapping("/classrooms")
    public String classrooms(@RequestParam(required = false) String building,
                             @RequestParam(required = false) String roomNumber,
                             Model model) {
        model.addAttribute("classrooms", adminService.classrooms());
        if (building != null && !building.isEmpty()) {
            model.addAttribute("editClassroom", adminService.classroomById(building, roomNumber));
        }
        return "admin/classrooms";
    }

    @PostMapping("/classrooms")
    public String createClassroom(@RequestParam String building,
                                  @RequestParam String roomNumber,
                                  @RequestParam Integer capacity,
                                  RedirectAttributes ra) {
        try {
            adminService.createClassroom(building, roomNumber, capacity);
            ra.addFlashAttribute("success", "教室已创建");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/classrooms";
    }

    @PostMapping("/classrooms/update")
    public String updateClassroom(@RequestParam String building,
                                  @RequestParam String roomNumber,
                                  @RequestParam Integer capacity,
                                  RedirectAttributes ra) {
        try {
            adminService.updateClassroom(building, roomNumber, capacity);
            ra.addFlashAttribute("success", "教室已更新");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/classrooms";
    }

    @PostMapping("/classrooms/delete")
    public String deleteClassroom(@RequestParam String building,
                                  @RequestParam String roomNumber,
                                  RedirectAttributes ra) {
        try {
            adminService.deleteClassroom(building, roomNumber);
            ra.addFlashAttribute("success", "教室已删除");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/classrooms";
    }

    // ========== 排课：开课班 ==========

    @GetMapping("/sections")
    public String sections(@RequestParam(required = false) String editCourseId,
                           @RequestParam(required = false) String editSecId,
                           @RequestParam(required = false) String editSemester,
                           @RequestParam(required = false) Integer editYear,
                           Model model) {
        model.addAttribute("sections", adminService.sections());
        model.addAttribute("courses", adminService.allCourses());
        model.addAttribute("instructors", adminService.allInstructors());
        model.addAttribute("timeSlots", adminService.timeSlotOptions());
        model.addAttribute("buildingNames", adminService.buildingNames());
        model.addAttribute("classrooms", adminService.classrooms());
        if (editCourseId != null && !editCourseId.isEmpty()) {
            model.addAttribute("editSection",
                    adminService.sectionById(editCourseId, editSecId, editSemester, editYear));
            model.addAttribute("editInstructorId", adminService
                    .instructorIdsOfSection(editCourseId, editSecId, editSemester, editYear)
                    .stream().findFirst().orElse(""));
        }
        return "admin/sections";
    }

    @PostMapping("/sections")
    public String createSection(@RequestParam String courseId,
                                @RequestParam String secId,
                                @RequestParam String semester,
                                @RequestParam Integer year,
                                @RequestParam String building,
                                @RequestParam String roomNumber,
                                @RequestParam String timeSlotId,
                                @RequestParam(required = false) String instructorId,
                                RedirectAttributes ra) {
        try {
            adminService.createSection(buildSection(courseId, secId, semester, year,
                    building, roomNumber, timeSlotId), instructorId);
            ra.addFlashAttribute("success", "开课班已创建");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sections";
    }

    @PostMapping("/sections/update")
    public String updateSection(@RequestParam String courseId,
                                @RequestParam String secId,
                                @RequestParam String semester,
                                @RequestParam Integer year,
                                @RequestParam String building,
                                @RequestParam String roomNumber,
                                @RequestParam String timeSlotId,
                                @RequestParam(required = false) String instructorId,
                                RedirectAttributes ra) {
        try {
            adminService.updateSection(buildSection(courseId, secId, semester, year,
                    building, roomNumber, timeSlotId), instructorId);
            ra.addFlashAttribute("success", "开课班已更新");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sections";
    }

    @PostMapping("/sections/delete")
    public String deleteSection(@RequestParam String courseId,
                                @RequestParam String secId,
                                @RequestParam String semester,
                                @RequestParam Integer year,
                                RedirectAttributes ra) {
        try {
            adminService.deleteSection(courseId, secId, semester, year);
            ra.addFlashAttribute("success", "开课班已删除");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sections";
    }

    private Section buildSection(String courseId, String secId, String semester, Integer year,
                                 String building, String roomNumber, String timeSlotId) {
        Section section = new Section();
        section.setCourseId(courseId);
        section.setSecId(secId);
        section.setSemester(semester);
        section.setYear(year);
        section.setBuilding(building);
        section.setRoomNumber(roomNumber);
        section.setTimeSlotId(timeSlotId);
        return section;
    }

    // ========== 排课看板（拖拽排课） ==========

    /** 默认学期/年份：年份取数据库中开课班最大年份，学期可配置 */
    private static final String DEFAULT_COURSE = "CS-101";

    @GetMapping("/scheduling")
    public String scheduling(@RequestParam(required = false) String semester,
                             @RequestParam(required = false) Integer year,
                             Model model) {
        String sem = (semester == null || semester.isEmpty()) ? termDefaults.semester() : semester;
        int yr = (year == null) ? termDefaults.year() : year;
        fillSchedulingModel(model, sem, yr);
        model.addAttribute("courses", adminService.allCourses());
        model.addAttribute("instructors", adminService.allInstructors());
        return "admin/scheduling";
    }

    /** 排课看板局部刷新（拖拽成功后由前端 AJAX 拉取） */
    @GetMapping("/scheduling/board")
    public String schedulingBoard(@RequestParam String semester,
                                  @RequestParam Integer year,
                                  Model model) {
        fillSchedulingModel(model, semester, year);
        return "admin/scheduling :: board";
    }

    private void fillSchedulingModel(Model model, String semester, Integer year) {
        model.addAttribute("board", adminService.schedulingBoard(semester, year));
    }

    /** 新建待排课班（教室/时间段留空，等待拖拽） */
    @PostMapping("/scheduling/create")
    @ResponseBody
    public Result<Void> createPendingSection(@RequestParam String courseId,
                                             @RequestParam String secId,
                                             @RequestParam String semester,
                                             @RequestParam Integer year,
                                             @RequestParam(required = false) String instructorId) {
        try {
            adminService.createUnassignedSection(
                    buildSection(courseId, secId, semester, year, null, null, null), instructorId);
            return Result.ok();
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    /** 拖拽排课：将开课班放入指定教室+时间段 */
    @PostMapping("/scheduling/assign")
    @ResponseBody
    public Result<Void> assignSchedule(@RequestParam String courseId,
                                       @RequestParam String secId,
                                       @RequestParam String semester,
                                       @RequestParam Integer year,
                                       @RequestParam String building,
                                       @RequestParam String roomNumber,
                                       @RequestParam String timeSlotId,
                                       @RequestParam(required = false) String instructorId) {
        try {
            adminService.assignSchedule(
                    buildSection(courseId, secId, semester, year, building, roomNumber, timeSlotId),
                    instructorId);
            return Result.ok();
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    /** 周课表视图（按 星期 x 时段，可筛选教室/教师） */
    @GetMapping("/scheduling/week")
    public String schedulingWeek(@RequestParam String semester,
                                 @RequestParam Integer year,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) String key,
                                 Model model) {
        model.addAttribute("week", adminService.weeklySchedule(semester, year, type, key));
        return "admin/scheduling-week :: week";
    }

    /** 取消排课：将开课班移回待排课区 */
    @PostMapping("/scheduling/unassign")
    @ResponseBody
    public Result<Void> unassignSchedule(@RequestParam String courseId,
                                         @RequestParam String secId,
                                         @RequestParam String semester,
                                         @RequestParam Integer year) {
        try {
            adminService.unassignSchedule(courseId, secId, semester, year);
            return Result.ok();
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }
    // ========== 统计报表（UC-09） ==========

    // ========== 账号管理 ==========

    @GetMapping("/accounts")
    public String accounts(Model model) {
        model.addAttribute("accounts", accountService.listAccounts());
        model.addAttribute("students", accountService.studentsWithoutAccount());
        model.addAttribute("instructors", accountService.instructorsWithoutAccount());
        model.addAttribute("defaultPassword", AccountService.DEFAULT_PASSWORD);
        return "admin/accounts";
    }

    @PostMapping("/accounts")
    public String createAccount(@RequestParam String userId,
                                @RequestParam String userType,
                                @RequestParam String refId,
                                @RequestParam(required = false) String password,
                                RedirectAttributes ra) {
        try {
            accountService.createAccount(userId, userType, refId, password);
            String pwd = (password == null || password.isEmpty()) ? AccountService.DEFAULT_PASSWORD : password;
            ra.addFlashAttribute("success", "账号已创建，初始密码：" + pwd);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/accounts/reset")
    public String resetPassword(@RequestParam String userId,
                                @RequestParam(required = false) String password,
                                RedirectAttributes ra) {
        try {
            accountService.resetPassword(userId, password);
            String pwd = (password == null || password.isEmpty()) ? AccountService.DEFAULT_PASSWORD : password;
            ra.addFlashAttribute("success", "账号 " + userId + " 密码已重置为：" + pwd);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/accounts/toggle")
    public String toggleAccount(@RequestParam String userId, RedirectAttributes ra) {
        try {
            boolean enabled = accountService.toggleEnabled(userId);
            ra.addFlashAttribute("success", enabled ? "账号已启用" : "账号已禁用");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/accounts";
    }
    @PostMapping("/accounts/open-all")
    public String openAllAccounts(RedirectAttributes ra) {
        try {
            int[] counts = accountService.openAllPendingAccounts();
            ra.addFlashAttribute("success", "已为 " + counts[0] + " 名学生、" + counts[1] + " 名教师开户，初始密码：" + AccountService.DEFAULT_PASSWORD);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/accounts";
    }
    @GetMapping("/stats")
    public String stats(@RequestParam(required = false) String semester,
                        @RequestParam(required = false) Integer year,
                        @RequestParam(required = false) String courseId,
                        Model model) {
        String sem = (semester == null || semester.isEmpty()) ? termDefaults.semester() : semester;
        int yr = (year == null) ? termDefaults.year() : year;
        String cid = (courseId == null || courseId.isEmpty()) ? DEFAULT_COURSE : courseId;
        model.addAttribute("semester", sem);
        model.addAttribute("year", yr);
        model.addAttribute("gradeCourseId", cid);
        model.addAttribute("semesters", Arrays.asList("Fall", "Spring", "Summer"));
        model.addAttribute("years", adminService.sectionYears());
        model.addAttribute("courses", adminService.allCourses());
        model.addAttribute("deptBudgets", statsReportService.deptBudget());
        model.addAttribute("deptSalaries", statsReportService.salaryByDept());
        model.addAttribute("enrollments", statsReportService.enrollment(sem, yr, null));
        model.addAttribute("gradeDistribution", statsReportService.gradeDistribution(cid));
        model.addAttribute("departmentCount", adminService.countDepartments());
        model.addAttribute("courseCount", adminService.countCourses());
        model.addAttribute("instructorCount", adminService.countInstructors());
        model.addAttribute("studentCount", adminService.countStudents());
        model.addAttribute("sectionCount", adminService.countSections());
        return "admin/stats";
    }
    // ========== 先修管理 ==========

    @GetMapping("/prereqs")
    public String prereqs(Model model) {
        model.addAttribute("prereqs", adminService.prereqs());
        model.addAttribute("courses", adminService.allCourses());
        return "admin/prereqs";
    }

    @PostMapping("/prereqs")
    public String addPrereq(@RequestParam String courseId,
                            @RequestParam String prereqId,
                            RedirectAttributes ra) {
        try {
            adminService.addPrereq(courseId, prereqId);
            ra.addFlashAttribute("success", "先修关系已添加");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/prereqs";
    }

    @PostMapping("/prereqs/delete")
    public String deletePrereq(@RequestParam String courseId,
                               @RequestParam String prereqId,
                               RedirectAttributes ra) {
        try {
            adminService.deletePrereq(courseId, prereqId);
            ra.addFlashAttribute("success", "先修关系已删除");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/prereqs";
    }
}