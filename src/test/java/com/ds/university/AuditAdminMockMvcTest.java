package com.ds.university;

import com.ds.university.config.CsrfInterceptor;
import com.ds.university.controller.AuthController;
import com.ds.university.vo.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 审计日志页面与 Web 请求留痕 MockMvc 测试（非事务，用例自行清理）：
 * /admin/audit 仅 ADMIN 可访问；通过 HTTP 删除课程的审计记录
 * 必须包含会话中的操作者账号与请求来源 IP。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditAdminMockMvcTest {

    private static final String CSRF_TOKEN = "audit-test-csrf-token";
    /** MockMvc 默认远端地址 */
    private static final String MOCK_IP = "127.0.0.1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 清理本类创建的测试课程与审计行，避免污染共享数据库 */
    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM audit_log WHERE target_id LIKE 'AUD-%' OR detail LIKE '%AUD-%'");
        jdbcTemplate.update("DELETE FROM course WHERE course_id LIKE 'AUD-%'");
    }

    /** 未登录访问 /admin/audit → 重定向登录页 */
    @Test
    void auditPageRequiresLogin() throws Exception {
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    /** STUDENT 角色访问 /admin/audit → 403 */
    @Test
    void auditPageForbidsNonAdmin() throws Exception {
        HttpSession session = sessionWithRole("STUDENT", "00128");
        mockMvc.perform(get("/admin/audit").session((MockHttpSession) session))
                .andExpect(status().isForbidden());
    }

    /** ADMIN 访问 /admin/audit → 200 并渲染审计日志页面 */
    @Test
    void auditPageRendersForAdmin() throws Exception {
        HttpSession session = sessionWithRole("ADMIN", "admin");
        mockMvc.perform(get("/admin/audit").session((MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit"))
                .andExpect(content().string(containsString("审计日志")));
    }

    /** 通过 HTTP 删除课程：审计记录必须带上会话操作者与来源 IP */
    @Test
    void httpDeleteCourseRecordsOperatorAndIp() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO course (course_id, title, dept_name, credits) VALUES ('AUD-WEB', '待删课程', 'Comp. Sci.', 3)");

        HttpSession session = sessionWithRole("ADMIN", "admin");
        session.setAttribute(CsrfInterceptor.SESSION_CSRF, CSRF_TOKEN);
        mockMvc.perform(post("/admin/courses/delete").session((MockHttpSession) session)
                        .param(CsrfInterceptor.PARAM_CSRF, CSRF_TOKEN)
                        .param("courseId", "AUD-WEB"))
                .andExpect(status().isFound())
                // 重定向会附带 csrfToken 查询参数（CsrfInterceptor 注入 Model），用 pattern 放宽
                .andExpect(redirectedUrlPattern("/admin/courses*"));

        Map<String, Object> audit = jdbcTemplate.queryForMap(
                "SELECT action, target_type, user_id, client_ip, detail FROM audit_log " +
                        "WHERE target_type = 'COURSE' AND target_id = 'AUD-WEB' ORDER BY id DESC LIMIT 1");
        assertEquals("DELETE", audit.get("action"));
        assertEquals("mock-user", audit.get("user_id"), "审计记录必须包含会话中的操作者账号");
        assertEquals(MOCK_IP, audit.get("client_ip"), "审计记录必须包含请求来源 IP");
        assertTrue(((String) audit.get("detail")).contains("删除课程：AUD-WEB"));

        // 追溯页面（ADMIN）能看到刚写入的记录
        HttpSession pageSession = sessionWithRole("ADMIN", "admin");
        mockMvc.perform(get("/admin/audit").param("keyword", "AUD-WEB").session((MockHttpSession) pageSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AUD-WEB")));
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
