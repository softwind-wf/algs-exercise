package com.ds.university;

import com.ds.university.config.SecurityHeaderFilter;
import com.ds.university.controller.AuthController;
import com.ds.university.vo.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.HttpSession;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SecurityHeaderFilter 安全响应头 MockMvc 测试：
 * 正常页、403 错误页、未登录重定向三种响应均必须携带
 * X-Frame-Options / X-Content-Type-Options / Content-Security-Policy 等安全头。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityHeaderMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    /** 公开页面（登录页）响应携带全部安全头 */
    @Test
    void publicPageCarriesAllSecurityHeaders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy",
                        SecurityHeaderFilter.CONTENT_SECURITY_POLICY))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"));
    }

    /** CSP 必须锁死框架嵌套与表单/基础地址来源 */
    @Test
    void contentSecurityPolicyLocksFramingAndOrigins() {
        String csp = SecurityHeaderFilter.CONTENT_SECURITY_POLICY;
        assertTrue(csp.contains("frame-ancestors 'none'"),
                "CSP 必须包含 frame-ancestors 'none'");
        assertTrue(csp.contains("form-action 'self'"),
                "CSP 必须包含 form-action 'self'");
        assertTrue(csp.contains("base-uri 'self'"),
                "CSP 必须包含 base-uri 'self'");
    }

    /** 403 错误页同样携带安全头（过滤器先于错误分发，错误响应不被漏掉） */
    @Test
    void forbiddenErrorPageCarriesSecurityHeaders() throws Exception {
        HttpSession session = sessionWithRole("STUDENT", "00128");
        mockMvc.perform(get("/admin").session((MockHttpSession) session))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    /** 未登录重定向响应也携带安全头（防劫持头不能只覆盖 200 响应） */
    @Test
    void redirectResponseCarriesSecurityHeaders() throws Exception {
        mockMvc.perform(get("/student/courses"))
                .andExpect(status().isFound())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    /** 构造带角色的会话（与 AuthInterceptorMockMvcTest 相同方式） */
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
