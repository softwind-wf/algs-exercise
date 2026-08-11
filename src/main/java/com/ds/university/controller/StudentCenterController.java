package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.common.PageResult;
import com.ds.university.service.CourseService;
import com.ds.university.service.StudentService;
import com.ds.university.service.TermDefaults;
import com.ds.university.util.ScheduleExcelWriter;
import com.ds.university.vo.LoginUser;
import com.ds.university.vo.StudentProfileVO;
import com.ds.university.vo.WeeklyScheduleVO;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 学生中心：选课/退课、成绩单、导师 */
@Controller
@RequestMapping("/student")
public class StudentCenterController {

    private final StudentService studentService;
    private final CourseService courseService;
    private final TermDefaults termDefaults;

    public StudentCenterController(StudentService studentService, CourseService courseService,
                                   TermDefaults termDefaults) {
        this.studentService = studentService;
        this.courseService = courseService;
        this.termDefaults = termDefaults;
    }

    /** 学生中心首页 */
    @GetMapping
    public String index(HttpSession session, Model model) {
        model.addAttribute("dashboard", studentService.dashboard(currentStudentId(session)));
        return "student/index";
    }

    /** 选课 / 退课页 */
    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) String semester,
                          @RequestParam(required = false) Integer year,
                          @RequestParam(required = false) String courseId,
                          @RequestParam(required = false) Integer page,
                          @RequestParam(required = false) Integer size,
                          HttpSession session, Model model) {
        String studentId = currentStudentId(session);
        String safeSemester = semester == null || semester.isEmpty() ? termDefaults.semester() : semester;
        Integer safeYear = year == null ? termDefaults.year() : year;
        int safeSize = PageResult.normalizeSize(size == null ? 0 : size);

        model.addAttribute("catalogPage", studentService.catalogPage(studentId, safeSemester, safeYear,
                courseId, page == null ? 1 : page, safeSize));
        model.addAttribute("enrollments", studentService.enrollments(studentId));
        model.addAttribute("courses", courseService.list(null, null));
        model.addAttribute("years", studentService.years());
        model.addAttribute("semester", safeSemester);
        model.addAttribute("year", safeYear);
        model.addAttribute("courseId", courseId);
        model.addAttribute("page", page);
        model.addAttribute("size", safeSize);
        return "student/courses";
    }

    /** 执行选课 */
    @PostMapping("/enroll")
    public String enroll(@RequestParam String courseId,
                         @RequestParam String secId,
                         @RequestParam String semester,
                         @RequestParam Integer year,
                         @RequestParam(required = false) String courseIdFilter,
                         @RequestParam(required = false) Integer page,
                         @RequestParam(required = false) Integer size,
                         HttpSession session, RedirectAttributes ra) {
        try {
            studentService.enroll(currentStudentId(session), courseId, secId, semester, year);
            ra.addFlashAttribute("success", "选课成功：" + titleOf(courseId) + "（开课班 " + secId + "，" + semester + " " + year + "）");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectToCourses(semester, year, courseIdFilter, page, size);
    }

    /** 执行退课 */
    @PostMapping("/drop")
    public String drop(@RequestParam String courseId,
                       @RequestParam String secId,
                       @RequestParam String semester,
                       @RequestParam Integer year,
                       @RequestParam(required = false) String courseIdFilter,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       HttpSession session, RedirectAttributes ra) {
        try {
            studentService.drop(currentStudentId(session), courseId, secId, semester, year);
            ra.addFlashAttribute("success", "退课成功：" + titleOf(courseId) + "（开课班 " + secId + "）");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectToCourses(semester, year, courseIdFilter, page, size);
    }

    /** 成绩单 */
    @GetMapping("/transcript")
    public String transcript(@RequestParam(required = false) Integer page,
                             @RequestParam(required = false) Integer size,
                             HttpSession session, Model model) {
        model.addAttribute("transcript", studentService.transcript(currentStudentId(session),
                page == null ? 1 : page, size == null ? 0 : size));
        return "student/transcript";
    }

    /** 我的课程表 */
    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) String semester,
                           @RequestParam(required = false) Integer year,
                           HttpSession session, Model model) {
        String safeSemester = semester == null || semester.isEmpty() ? termDefaults.semester() : semester;
        Integer safeYear = year == null ? termDefaults.year() : year;
        model.addAttribute("week", studentService.weeklySchedule(
                currentStudentId(session), safeSemester, safeYear));
        model.addAttribute("years", studentService.years());
        model.addAttribute("semester", safeSemester);
        model.addAttribute("year", safeYear);
        return "student/schedule";
    }

    /** 下载课程表（Excel 周课表网格，与页面一致） */
    @GetMapping("/schedule/download")
    public ResponseEntity<byte[]> downloadSchedule(@RequestParam(required = false) String semester,
                                                   @RequestParam(required = false) Integer year,
                                                   HttpSession session) throws IOException {
        String safeSemester = semester == null || semester.isEmpty() ? termDefaults.semester() : semester;
        Integer safeYear = year == null ? termDefaults.year() : year;
        String studentId = currentStudentId(session);
        StudentProfileVO profile = studentService.profile(studentId);
        WeeklyScheduleVO week = studentService.weeklySchedule(studentId, safeSemester, safeYear);
        String title = "课程表（" + safeSemester + " " + safeYear + "）　学生："
                + profile.getStudent().getName() + "（" + studentId + "）　专业："
                + profile.getStudent().getDeptName();
        byte[] bytes = ScheduleExcelWriter.toXlsx(week, "课程表", title);

        String filename = "课程表_" + studentId + "_" + safeSemester + "_" + safeYear + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /** 导师 */
    @GetMapping("/advisor")
    public String advisor(HttpSession session, Model model) {
        model.addAttribute("advisor", studentService.advisor(currentStudentId(session)));
        return "student/advisor";
    }

    /** 选课/退课后重定向回选课页；用 UriComponentsBuilder 统一做 URL 编码，避免拼接注入 */
    private String redirectToCourses(String semester, Integer year, String courseIdFilter,
                                     Integer page, Integer size) {
        UriComponentsBuilder ub = UriComponentsBuilder.fromPath("/student/courses")
                .queryParam("semester", semester)
                .queryParam("year", year);
        if (courseIdFilter != null && !courseIdFilter.isEmpty()) {
            ub.queryParam("courseId", courseIdFilter);
        }
        if (page != null && page > 1) {
            ub.queryParam("page", page);
        }
        if (size != null && size != PageResult.normalizeSize(0)) {
            ub.queryParam("size", size);
        }
        return "redirect:" + ub.build().encode().toUriString();
    }

    private String currentStudentId(HttpSession session) {
        LoginUser loginUser = (LoginUser) session.getAttribute(AuthController.SESSION_USER);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (loginUser.getRefId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前账号未关联学生信息");
        }
        return loginUser.getRefId();
    }

    private String titleOf(String courseId) {
        String title = courseService.title(courseId);
        return title == null ? courseId : title;
    }
}