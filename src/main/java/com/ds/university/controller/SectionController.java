/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.controller;

import com.ds.university.service.CourseService;
import com.ds.university.service.SectionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        model.addAttribute("sections", sectionService.page(semester, year, courseId, page, size));
        model.addAttribute("courses", courseService.list(null, null));
        model.addAttribute("semester", semester);
        model.addAttribute("year", year);
        model.addAttribute("courseId", courseId);
        return "sections";
    }

    @GetMapping("/{courseId}/{secId}/{semester}/{year}")
    public String detail(@PathVariable String courseId,
                         @PathVariable String secId,
                         @PathVariable String semester,
                         @PathVariable int year,
                         Model model) {
        model.addAttribute("detail", sectionService.detail(courseId, secId, semester, year));
        return "section";
    }
}