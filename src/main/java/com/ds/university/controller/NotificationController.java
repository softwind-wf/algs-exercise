package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.common.Result;
import com.ds.university.service.NotificationService;
import com.ds.university.vo.LoginUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

/**
 * 站内通知（@提及等）：通知列表、未读数（导航栏角标轮询）、已读管理。
 * 需登录访问。
 */
@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 通知列表（需登录） */
    @GetMapping("/notifications")
    public String list(HttpSession session, Model model) {
        LoginUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("notifications", notificationService.list(user.getUserId()));
        return "notifications";
    }

    /** 未读数（导航栏角标轮询；未登录返回 0） */
    @GetMapping("/notifications/unread-count")
    @ResponseBody
    public Result<Integer> unreadCount(HttpSession session) {
        LoginUser user = currentUser(session);
        return Result.ok(user == null ? 0 : notificationService.unreadCount(user.getUserId()));
    }

    /** 单条已读并跳转到来源（需登录，仅本人） */
    @PostMapping("/notifications/{id}/read")
    public String readAndGo(@PathVariable Long id, HttpSession session) {
        LoginUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        notificationService.markRead(id, user.getUserId());
        return "redirect:/notifications";
    }

    /** 全部标记已读（需登录） */
    @PostMapping("/notifications/read-all")
    public String readAll(HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        notificationService.markAllRead(user.getUserId());
        ra.addFlashAttribute("success", "已全部标记为已读");
        return "redirect:/notifications";
    }

    private LoginUser currentUser(HttpSession session) {
        return session == null ? null : (LoginUser) session.getAttribute(AuthController.SESSION_USER);
    }
}
