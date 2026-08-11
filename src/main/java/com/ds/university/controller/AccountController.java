package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.service.AuthService;
import com.ds.university.vo.LoginUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

/** 账号中心：修改密码。 */
@Controller
@RequestMapping("/account")
public class AccountController {

    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    /** 修改密码页 */
    @GetMapping("/password")
    public String passwordPage() {
        return "account/password";
    }

    /** 提交修改密码 */
    @PostMapping("/password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session, RedirectAttributes ra) {
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 32) {
            ra.addFlashAttribute("error", "新密码长度需为 6~32 位");
            return "redirect:/account/password";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "两次输入的新密码不一致");
            return "redirect:/account/password";
        }
        try {
            authService.changePassword(currentUserId(session), oldPassword, newPassword);
            ra.addFlashAttribute("success", "密码修改成功，请牢记新密码");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account/password";
    }

    private String currentUserId(HttpSession session) {
        LoginUser loginUser = (LoginUser) session.getAttribute(AuthController.SESSION_USER);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser.getUserId();
    }
}