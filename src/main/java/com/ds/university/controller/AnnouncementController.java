package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.service.AnnouncementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** 系统公告（公开页）：列表（支持按类型筛选）与详情页。 */
@Controller
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /** 公告列表：展示全部已发布公告，可按类型筛选 */
    @GetMapping("/announcements")
    public String list(@RequestParam(required = false) String category, Model model) {
        model.addAttribute("announcements", announcementService.listPublished(category, null));
        model.addAttribute("categories", announcementService.categoryLabels());
        model.addAttribute("selCategory", category == null ? "" : category);
        return "announcements";
    }

    /** 公告详情：仅已发布且处于有效期内可见 */
    @GetMapping("/announcements/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        if (announcementService.getPublishedById(id) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告不存在或未发布");
        }
        model.addAttribute("announcement", announcementService.getPublishedById(id));
        return "announcement";
    }
}
