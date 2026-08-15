/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.entity.Instructor;
import com.ds.university.service.AdminService;
import com.ds.university.service.TeacherService;
import com.ds.university.service.TermDefaults;
import com.ds.university.util.ScheduleExcelWriter;
import com.ds.university.vo.LoginUser;
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

/** 教师中心：授课列表、班级名单、成绩录入 */
@Controller
@RequestMapping("/instructor")
public class TeacherCenterController {

    private final TeacherService teacherService;
    private final AdminService adminService;
    private final TermDefaults termDefaults;

    public TeacherCenterController(TeacherService teacherService, AdminService adminService,
                                   TermDefaults termDefaults) {
        this.teacherService = teacherService;
        this.adminService = adminService;
        this.termDefaults = termDefaults;
    }

    /** 教师中心首页：授课列表 */
    @GetMapping
    public String index(HttpSession session, Model model) {
        model.addAttribute("dashboard", teacherService.dashboard(currentInstructorId(session)));
        return "instructor/index";
    }
    /** 授课列表（独立页面） */
    @GetMapping("/sections")
    public String sections(HttpSession session, Model model) {
        model.addAttribute("dashboard", teacherService.dashboard(currentInstructorId(session)));
        return "instructor/sections";
    }

    /** 授课表：按 星期 x 时间段 展示本学期所授课程 */
    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) String semester,
                           @RequestParam(required = false) Integer year,
                           HttpSession session, Model model) {
        String safeSemester = semester == null || semester.isEmpty() ? termDefaults.semester() : semester;
        Integer safeYear = year == null ? termDefaults.year() : year;
        model.addAttribute("week", adminService.weeklySchedule(
                safeSemester, safeYear, "instructor", currentInstructorId(session)));
        model.addAttribute("years", adminService.sectionYears());
        model.addAttribute("semester", safeSemester);
        model.addAttribute("year", safeYear);
        return "instructor/schedule";
    }

    /** 下载授课表（Excel 周课表网格，与页面一致） */
    @GetMapping("/schedule/download")
    public ResponseEntity<byte[]> downloadSchedule(@RequestParam(required = false) String semester,
                                                   @RequestParam(required = false) Integer year,
                                                   HttpSession session) throws IOException {
        String safeSemester = semester == null || semester.isEmpty() ? termDefaults.semester() : semester;
        Integer safeYear = year == null ? termDefaults.year() : year;
        String instructorId = currentInstructorId(session);
        Instructor instructor = teacherService.dashboard(instructorId).getInstructor();
        WeeklyScheduleVO week = adminService.weeklySchedule(
                safeSemester, safeYear, "instructor", instructorId);
        String title = "授课表（" + safeSemester + " " + safeYear + "）　教师："
                + instructor.getName() + "（" + instructorId + "）　院系：" + instructor.getDeptName();
        byte[] bytes = ScheduleExcelWriter.toXlsx(week, "授课表", title);

        String filename = "授课表_" + instructorId + "_" + safeSemester + "_" + safeYear + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /** 班级名单（含成绩录入） */
    @GetMapping("/roster")
    public String roster(@RequestParam String courseId,
                         @RequestParam String secId,
                         @RequestParam String semester,
                         @RequestParam Integer year,
                         HttpSession session, Model model) {
        model.addAttribute("roster", teacherService.roster(
                currentInstructorId(session), courseId, secId, semester, year));
        return "instructor/roster";
    }

    /** 录入/修改成绩 */
    @PostMapping("/grade")
    public String grade(@RequestParam String studentId,
                        @RequestParam String courseId,
                        @RequestParam String secId,
                        @RequestParam String semester,
                        @RequestParam Integer year,
                        @RequestParam(required = false) String grade,
                        HttpSession session, RedirectAttributes ra) {
        try {
            teacherService.updateGrade(currentInstructorId(session), studentId,
                    courseId, secId, semester, year, grade);
            ra.addFlashAttribute("success", "成绩已保存");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + UriComponentsBuilder.fromPath("/instructor/roster")
                .queryParam("courseId", courseId)
                .queryParam("secId", secId)
                .queryParam("semester", semester)
                .queryParam("year", year)
                .build().encode().toUriString();
    }

    private String currentInstructorId(HttpSession session) {
        LoginUser loginUser = (LoginUser) session.getAttribute(AuthController.SESSION_USER);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (loginUser.getRefId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前账号未关联教师信息");
        }
        return loginUser.getRefId();
    }
}