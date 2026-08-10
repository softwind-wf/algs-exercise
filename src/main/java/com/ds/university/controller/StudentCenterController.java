package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.common.PageResult;
import com.ds.university.service.CourseService;
import com.ds.university.service.StudentService;
import com.ds.university.vo.LoginUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;

/** 学生中心：选课/退课、成绩单、导师 */
@Controller
@RequestMapping("/student")
public class StudentCenterController {

    private static final String DEFAULT_SEMESTER = "Spring";
    private static final int DEFAULT_YEAR = 2010;

    private final StudentService studentService;
    private final CourseService courseService;

    public StudentCenterController(StudentService studentService, CourseService courseService) {
        this.studentService = studentService;
        this.courseService = courseService;
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
        String safeSemester = semester == null || semester.isEmpty() ? DEFAULT_SEMESTER : semester;
        Integer safeYear = year == null ? DEFAULT_YEAR : year;
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

    /** 导师 */
    @GetMapping("/advisor")
    public String advisor(HttpSession session, Model model) {
        model.addAttribute("advisor", studentService.advisor(currentStudentId(session)));
        return "student/advisor";
    }

    private String redirectToCourses(String semester, Integer year, String courseIdFilter,
                                     Integer page, Integer size) {
        StringBuilder sb = new StringBuilder("redirect:/student/courses?semester=").append(semester)
                .append("&year=").append(year);
        if (courseIdFilter != null && !courseIdFilter.isEmpty()) {
            sb.append("&courseId=").append(courseIdFilter);
        }
        if (page != null && page > 1) {
            sb.append("&page=").append(page);
        }
        if (size != null && size != PageResult.normalizeSize(0)) {
            sb.append("&size=").append(size);
        }
        return sb.toString();
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