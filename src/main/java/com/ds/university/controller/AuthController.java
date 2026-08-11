package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.config.CsrfInterceptor;
import com.ds.university.service.AuthService;
import com.ds.university.vo.LoginUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
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
                        HttpServletRequest request,
                        Model model) {
        try {
            LoginUser loginUser = authService.login(userId, password);
            // 登录成功后更换 Session ID（Servlet 3.1），防止会话固定攻击；
            // changeSessionId 保留原会话属性，CSRF token 等不断链
            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute(SESSION_USER, loginUser);
            // 登录成功后轮换 CSRF token，避免登录前 token 被绑定到已认证会话
            CsrfInterceptor.rotateToken(session);
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