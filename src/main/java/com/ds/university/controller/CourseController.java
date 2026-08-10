package com.ds.university.controller;

import com.ds.university.service.CourseService;
import com.ds.university.service.DepartmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 课程浏览 */
@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final DepartmentService departmentService;

    public CourseController(CourseService courseService, DepartmentService departmentService) {
        this.courseService = courseService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String deptName,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        model.addAttribute("courses", courseService.page(deptName, keyword, page, size));
        model.addAttribute("departments", departmentService.listAll());
        model.addAttribute("deptName", deptName);
        model.addAttribute("keyword", keyword);
        return "courses";
    }

    @GetMapping("/{courseId}")
    public String detail(@PathVariable String courseId, Model model) {
        model.addAttribute("detail", courseService.detail(courseId));
        return "course";
    }
}