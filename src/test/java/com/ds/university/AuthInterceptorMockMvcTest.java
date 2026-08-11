package com.ds.university;

import com.ds.university.config.CsrfInterceptor;
import com.ds.university.controller.AuthController;
import com.ds.university.vo.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpSession;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * AuthInterceptor 鉴权 MockMvc 测试：
 * 未登录访问受保护路径重定向到 /login；登录后角色不匹配返回 403；
 * 精确前缀匹配不误拦公开路径；CSRF token 缺失/错误在鉴权之前被拒。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthInterceptorMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    /** 未登录访问 /student/** → 重定向到登录页 */
    @Test
    void unauthenticatedStudentPathRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/student/courses"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/student"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    /** 未登录访问 /admin/** → 重定向到登录页 */
    @Test
    void unauthenticatedAdminPathRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/admin/departments"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    /** 未登录访问 /account/**（仅需登录，不限角色）→ 重定向到登录页 */
    @Test
    void unauthenticatedAccountPathRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/account/password"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    /** STUDENT 角色访问 /admin/** → 403，且渲染友好提示页 */
    @Test
    void studentAccessingAdminGetsForbidden() throws Exception {
        HttpSession session = sessionWithRole("STUDENT", "00128");
        mockMvc.perform(get("/admin").session((MockHttpSession) session))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("没有权限访问")));
    }

    /** INSTRUCTOR 角色访问 /admin/** → 403 */
    @Test
    void instructorAccessingAdminGetsForbidden() throws Exception {
        HttpSession session = sessionWithRole("INSTRUCTOR", "10101");
        mockMvc.perform(get("/admin/departments").session((MockHttpSession) session))
                .andExpect(status().isForbidden());
    }

    /** STUDENT 角色访问 /instructor/** → 403（角色间互不可越权） */
    @Test
    void studentAccessingInstructorGetsForbidden() throws Exception {
        HttpSession session = sessionWithRole("STUDENT", "00128");
        mockMvc.perform(get("/instructor").session((MockHttpSession) session))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("没有权限访问")));
    }

    /** 公开路径 /instructors 未登录可直接访问（精确前缀匹配不误拦同前缀路径） */
    @Test
    void publicInstructorsListAccessibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/instructors"))
                .andExpect(status().isOk());
    }

    /** ADMIN 角色访问 /admin/** 放行并正常渲染视图 */
    @Test
    void adminAccessingAdminPageSucceeds() throws Exception {
        HttpSession session = sessionWithRole("ADMIN", "admin");
        mockMvc.perform(get("/admin").session((MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/index"));
    }

    /** STUDENT 登录后访问学生中心放行并正常渲染视图 */
    @Test
    void studentAccessingOwnCenterSucceeds() throws Exception {
        HttpSession session = sessionWithRole("STUDENT", "00128");
        mockMvc.perform(get("/student").session((MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(view().name("student/index"));
    }

    /** POST 未带 CSRF token → 被 CSRF 拦截器拒绝（先于业务处理，与登录状态无关） */
    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        HttpSession session = sessionWithRole("STUDENT", "00128");
        mockMvc.perform(post("/student/enroll").session((MockHttpSession) session)
                        .param("courseId", "CS-101")
                        .param("secId", "1")
                        .param("semester", "Spring")
                        .param("year", "2010"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("CSRF token 校验失败")));
    }

    /** POST 带错误 CSRF token → 403 */
    @Test
    void postWithWrongCsrfTokenIsRejected() throws Exception {
        HttpSession session = sessionWithRole("STUDENT", "00128");
        mockMvc.perform(post("/student/enroll").session((MockHttpSession) session)
                        .param(CsrfInterceptor.PARAM_CSRF, "wrong-token")
                        .param("courseId", "CS-101")
                        .param("secId", "1")
                        .param("semester", "Spring")
                        .param("year", "2010"))
                .andExpect(status().isForbidden());
    }

    /** 403 错误页：命中专用模板 error/403，展示友好中文提示而非默认 Whitelabel 页 */
    @Test
    void forbiddenErrorPageRendersFriendlyMessage() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403)
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, ""))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("无权访问")));
    }

    /** 登录页：未登录可直接访问并正常渲染 */
    @Test
    void loginPageRendersForAnonymous() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("用户登录")));
    }

    /**
     * 登录成功后 Session ID 必须变更（防会话固定攻击），
     * 且用户属性与 CSRF token 迁移到新会话不断链。
     */
    @Test
    void loginRegeneratesSessionId() throws Exception {
        // 第一步：访问登录页建立会话并拿到 CSRF token
        MvcResult pageResult = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();
        HttpSession preSession = pageResult.getRequest().getSession(false);
        assertNotNull(preSession, "登录页应创建会话");
        String oldSessionId = preSession.getId();
        String csrfToken = (String) preSession.getAttribute(CsrfInterceptor.SESSION_CSRF);
        assertNotNull(csrfToken, "登录页应下发 CSRF token");

        // 第二步：携带 token 提交登录（演示账号 zhang/password）
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .session((MockHttpSession) preSession)
                        .param(CsrfInterceptor.PARAM_CSRF, csrfToken)
                        .param("userId", "zhang")
                        .param("password", "password"))
                .andExpect(status().isFound())
                .andReturn();
        HttpSession postSession = loginResult.getRequest().getSession(false);
        assertNotNull(postSession, "登录后应存在会话");
        // 会话固定防护：Session ID 已更换
        assertNotEquals(oldSessionId, postSession.getId(), "登录成功后 Session ID 必须变更");
        // 用户属性已写入新会话
        assertNotNull(postSession.getAttribute(AuthController.SESSION_USER));
        // CSRF token 已轮换且随会话保留
        String newToken = (String) postSession.getAttribute(CsrfInterceptor.SESSION_CSRF);
        assertNotNull(newToken);
        assertNotEquals(csrfToken, newToken, "登录后 CSRF token 应轮换");
    }

    private HttpSession sessionWithRole(String role, String refId) {
        MockHttpSession session = new MockHttpSession();
        LoginUser user = new LoginUser();
        user.setUserId("mock-user");
        user.setRefId(refId);
        user.setRoles(Collections.singletonList(role));
        session.setAttribute(AuthController.SESSION_USER, user);
        return session;
    }
}
