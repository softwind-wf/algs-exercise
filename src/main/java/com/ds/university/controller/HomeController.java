/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.controller;

import com.ds.university.service.HomeService;
import com.ds.university.service.StatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 首页 */
@Controller
public class HomeController {

    private final StatsService statsService;
    private final HomeService homeService;

    public HomeController(StatsService statsService, HomeService homeService) {
        this.statsService = statsService;
        this.homeService = homeService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("stats", statsService.summary());
        model.addAttribute("hotCourses", homeService.hotCourses());
        model.addAttribute("latestSections", homeService.latestSections());
        model.addAttribute("departments", homeService.departments());
        model.addAttribute("announcements", homeService.announcements());
        return "index";
    }
}