package com.ds.university.controller;

import com.ds.university.service.StatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 首页 */
@Controller
public class HomeController {

    private final StatsService statsService;

    public HomeController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("stats", statsService.summary());
        return "index";
    }
}