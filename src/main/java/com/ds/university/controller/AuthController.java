package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.service.AuthService;
import com.ds.university.vo.LoginUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

/** 登录 / 登出 */
@Controller
public class AuthController {

    public static final String SESSION_USER = "loginUser";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String userId,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        try {
            LoginUser loginUser = authService.login(userId, password);
            session.setAttribute(SESSION_USER, loginUser);
            if (loginUser.getRoles().contains("ADMIN")) {
                return "redirect:/admin";
            }
            if (loginUser.getRoles().contains("STUDENT")) {
                return "redirect:/student";
            }
            if (loginUser.getRoles().contains("INSTRUCTOR")) {
                return "redirect:/instructor";
            }
            return "redirect:/";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("userId", userId);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}