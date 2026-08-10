package com.ds.university.controller;

import com.ds.university.service.DepartmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/** 系浏览 */
@Controller
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("departments", departmentService.listWithStats());
        return "departments";
    }

    @GetMapping("/{deptName}")
    public String detail(@PathVariable String deptName, Model model) {
        model.addAttribute("detail", departmentService.detail(deptName));
        return "department";
    }
}