package com.ds.university.controller;

import com.ds.university.service.CourseService;
import com.ds.university.service.SectionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 开课班浏览 */
@Controller
@RequestMapping("/sections")
public class SectionController {

    private final SectionService sectionService;
    private final CourseService courseService;

    public SectionController(SectionService sectionService, CourseService courseService) {
        this.sectionService = sectionService;
        this.courseService = courseService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String semester,
                       @RequestParam(required = false) Integer year,
                       @RequestParam(required = false) String courseId,
                       Model model) {
        model.addAttribute("sections", sectionService.list(semester, year, courseId));
        model.addAttribute("courses", courseService.list(null, null));
        model.addAttribute("semester", semester);
        model.addAttribute("year", year);
        model.addAttribute("courseId", courseId);
        return "sections";
    }
}