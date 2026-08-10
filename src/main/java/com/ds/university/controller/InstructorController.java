package com.ds.university.controller;

import com.ds.university.service.DepartmentService;
import com.ds.university.service.InstructorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 教师浏览 */
@Controller
@RequestMapping("/instructors")
public class InstructorController {

    private final InstructorService instructorService;
    private final DepartmentService departmentService;

    public InstructorController(InstructorService instructorService, DepartmentService departmentService) {
        this.instructorService = instructorService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String deptName, Model model) {
        model.addAttribute("instructors", instructorService.list(deptName));
        model.addAttribute("departments", departmentService.listAll());
        model.addAttribute("deptName", deptName);
        return "instructors";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        model.addAttribute("detail", instructorService.detail(id));
        return "instructor";
    }
}