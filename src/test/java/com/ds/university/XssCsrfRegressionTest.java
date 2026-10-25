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
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * XSS / CSRF 回归测试（2026-10 安全审计后补）：
 * <ul>
 *   <li>聊天页曾把 ObjectMapper 序列化的 JSON 用 Thymeleaf 非转义表达式
 *       {@code [(${conversationsJson})]} 直接内联进 &lt;script&gt;：聊天消息内容里的
 *       {@code </script>} 会提前闭合脚本块，形成存储型 XSS。修复后 JSON 改由
 *       data-* 属性下发（HTML 属性转义）再由 chat.js 的 JSON.parse 读取，
 *       本用例断言页面不再出现可闭合脚本的原始载荷。</li>
 *   <li>退出登录曾是可被跨站顶层导航触发的 GET /logout，现改为 POST + CSRF token，
 *       本用例断言 GET 不再受理、缺少/错误 token 被 403 拒绝、正确 token 才登出。</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class XssCsrfRegressionTest {

    /** 攻击载荷 + 唯一标记：既模拟真实消息内容，也便于用例结束后精确清理。 */
    private static final String MARKER = "xss-regression-20261025";
    private static final String XSS_PAYLOAD =
            "</script><img src=x onerror=alert(document.domain)> " + MARKER;

    private static final String TEST_TOKEN = "test-csrf-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM chat_message WHERE content LIKE ?", "%" + MARKER + "%");
    }

    /**
     * 聊天页必须把消息内容里的 {@code </script><img ...>} 转义成不可执行的文本：
     * - 页面不得出现原始的 {@code </script><img}（否则脚本块被提前闭合）；
     * - 会话数据改由 data-conversations 属性下发；
     * - 不得再出现内联的 {@code conversations: [...]} JS 数组字面量写法。
     */
    @Test
    void chatPageNeutralizesScriptBreakoutPayload() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO chat_message (from_user, from_name, to_user, to_name, content, read_flag) "
                        + "VALUES (?, ?, ?, ?, ?, 0)",
                "zhang", "Zhang", "katz", "Katz", XSS_PAYLOAD);

        MvcResult result = mockMvc.perform(get("/instructor/chat")
                        .session(session("katz", "INSTRUCTOR", "45565")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("</script><img"))))
                .andExpect(content().string(containsString("data-conversations")))
                .andExpect(content().string(containsString("&lt;/script&gt;")))
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("data-conversations"),
                "会话数据应通过 data-* 属性下发");
        assertFalse(html.contains("conversations: ["),
                "不得再把 JSON 直接内联成 JS 数组字面量（回归为 [(${...})] 写法）");
    }

    /** 退出登录只接受 POST，且必须携带会话内一致的 CSRF token；成功登出后会话失效。 */
    @Test
    void logoutRequiresPostWithCsrfToken() throws Exception {
        // 1) GET /logout 不再受理（旧行为可被跨站顶层导航触发 CSRF 登出）
        mockMvc.perform(get("/logout").session(session("zhang", "STUDENT", "00128")))
                .andExpect(status().isMethodNotAllowed());

        // 2) POST 但缺少 token → 403
        MockHttpSession noToken = session("zhang", "STUDENT", "00128");
        mockMvc.perform(post("/logout").session(noToken))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("CSRF token 校验失败")));

        // 3) POST 但 token 错误 → 403
        mockMvc.perform(post("/logout")
                        .session(session("zhang", "STUDENT", "00128"))
                        .param(CsrfInterceptor.PARAM_CSRF, "wrong-token"))
                .andExpect(status().isForbidden());

        // 4) POST + 正确 token → 302 回登录页，会话被销毁
        MockHttpSession valid = session("zhang", "STUDENT", "00128");
        mockMvc.perform(post("/logout")
                        .session(valid)
                        .param(CsrfInterceptor.PARAM_CSRF, TEST_TOKEN))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
        assertTrue(valid.isInvalid(), "登出成功后会话必须失效");
    }

    /** 页面上的退出入口必须是 POST 表单并带 _csrf 隐藏域，不能再是 GET 链接。 */
    @Test
    void logoutEntryRendersAsPostFormWithToken() throws Exception {
        String html = mockMvc.perform(get("/student").session(session("zhang", "STUDENT", "00128")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(containsString("method=\"post\"")))
                .andExpect(content().string(containsString(TEST_TOKEN)))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertFalse(html.contains("href=\"/logout\""), "退出入口不应再有 GET 链接");
    }

    /** 构造已登录会话：写入会话用户与 CSRF token（与 CsrfInterceptor 的校验口径一致）。 */
    private MockHttpSession session(String userId, String role, String refId) {
        MockHttpSession session = new MockHttpSession();
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setUserType(role);
        user.setRefId(refId);
        user.setRoles(Collections.singletonList(role));
        session.setAttribute(AuthController.SESSION_USER, user);
        session.setAttribute(CsrfInterceptor.SESSION_CSRF, TEST_TOKEN);
        return session;
    }
}
