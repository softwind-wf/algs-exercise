package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.service.TeacherService;
import com.ds.university.vo.LoginUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

/** 教师中心：授课列表、班级名单、成绩录入 */
@Controller
@RequestMapping("/instructor")
public class TeacherCenterController {

    private final TeacherService teacherService;

    public TeacherCenterController(TeacherService teacherService) {
        this.teacherService = teacherService;
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
        return "redirect:/instructor/roster?courseId=" + courseId
                + "&secId=" + secId + "&semester=" + semester + "&year=" + year;
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