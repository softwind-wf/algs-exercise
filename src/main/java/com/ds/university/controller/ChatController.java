package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.common.Result;
import com.ds.university.service.ChatService;
import com.ds.university.vo.ChatDeptVO;
import com.ds.university.vo.ChatUserVO;
import com.ds.university.vo.LoginUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 在线聊天：学生中心 /student/chat 与教师中心 /instructor/chat 共用同一模板。
 * 实时通信走 WebSocket /ws/chat；
 * 联系人：搜索框（/chat/users 关键字/院系/角色综合查询）+ 智能分组（我的同学/老师/学生/同事）+ 按院系浏览。
 */
@Controller
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public ChatController(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    /** 学生中心入口（AuthInterceptor 已限定 STUDENT 角色） */
    @GetMapping("/student/chat")
    public String studentChat(HttpSession session, Model model) {
        return chatPage(session, model);
    }

    /** 教师中心入口（AuthInterceptor 已限定 INSTRUCTOR 角色） */
    @GetMapping("/instructor/chat")
    public String instructorChat(HttpSession session, Model model) {
        return chatPage(session, model);
    }

    /**
     * 联系人综合查询（需登录）：关键字（姓名/账号）/院系/角色均可选，服务端限流返回。
     * 匿名请求返回 4010。
     */
    @GetMapping("/chat/users")
    @ResponseBody
    public Result<List<ChatUserVO>> searchUsers(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) String dept,
                                                @RequestParam(required = false) String role,
                                                HttpSession session) {
        LoginUser loginUser = (LoginUser) session.getAttribute(AuthController.SESSION_USER);
        if (loginUser == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        return Result.ok(chatService.searchUsers(loginUser.getUserId(), q, dept, role, 20));
    }

    /** 院系列表（含师生人数），供"按院系浏览"（需登录） */
    @GetMapping("/chat/departments")
    @ResponseBody
    public Result<List<ChatDeptVO>> departments(HttpSession session) {
        LoginUser loginUser = (LoginUser) session.getAttribute(AuthController.SESSION_USER);
        if (loginUser == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        return Result.ok(chatService.departments());
    }

    private String chatPage(HttpSession session, Model model) {
        LoginUser loginUser = (LoginUser) session.getAttribute(AuthController.SESSION_USER);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String me = loginUser.getUserId();
        model.addAttribute("myUserId", me);
        model.addAttribute("myName", chatService.displayName(me));
        model.addAttribute("conversationsJson", toJson(chatService.conversations(me)));
        model.addAttribute("groupsJson", toJson(chatService.myContacts(me, loginUser.getUserType(), loginUser.getRefId())));
        model.addAttribute("deptsJson", toJson(chatService.departments()));
        return "chat";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
