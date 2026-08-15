/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.service.AuthService;
import com.ds.university.service.AvatarService;
import com.ds.university.vo.LoginUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

/** 账号中心：修改密码、设置头像。 */
@Controller
@RequestMapping("/account")
public class AccountController {

    private final AuthService authService;
    private final AvatarService avatarService;

    public AccountController(AuthService authService, AvatarService avatarService) {
        this.authService = authService;
        this.avatarService = avatarService;
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

    /** 设置头像页 */
    @GetMapping("/avatar")
    public String avatarPage() {
        return "account/avatar";
    }

    /** 上传（覆盖）头像 */
    @PostMapping("/avatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file,
                               HttpSession session, RedirectAttributes ra) {
        try {
            String filename = avatarService.saveAvatar(currentUserId(session), file);
            updateSessionAvatar(session, filename);
            ra.addFlashAttribute("success", "头像上传成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account/avatar";
    }

    /** 移除头像 */
    @PostMapping("/avatar/remove")
    public String removeAvatar(HttpSession session, RedirectAttributes ra) {
        try {
            avatarService.removeAvatar(currentUserId(session));
            updateSessionAvatar(session, null);
            ra.addFlashAttribute("success", "已移除头像");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account/avatar";
    }

    /** 同步会话中的头像字段，导航栏等页面无需重新登录即可生效 */
    private void updateSessionAvatar(HttpSession session, String avatar) {
        LoginUser loginUser = (LoginUser) session.getAttribute(AuthController.SESSION_USER);
        if (loginUser != null) {
            loginUser.setAvatar(avatar);
        }
    }

    private String currentUserId(HttpSession session) {
        LoginUser loginUser = (LoginUser) session.getAttribute(AuthController.SESSION_USER);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser.getUserId();
    }
}