package com.ds.university.config;

import com.ds.university.common.ForbiddenException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * CSRF 防护拦截器：
 * <ul>
 *   <li>每个会话持有随机 token（存于 session），页面通过 meta / hidden 字段下发；</li>
 *   <li>POST/PUT/DELETE/PATCH 请求必须携带一致的 token（表单参数 _csrf 或请求头 X-CSRF-TOKEN）；</li>
 *   <li>校验失败抛出 ForbiddenException，由全局异常处理渲染 403 友好页。</li>
 * </ul>
 */
@Component
public class CsrfInterceptor implements HandlerInterceptor {

    /** session 中保存 token 的属性名 */
    public static final String SESSION_CSRF = "csrfToken";
    /** 表单提交的参数名 */
    public static final String PARAM_CSRF = "_csrf";
    /** AJAX 请求头名 */
    public static final String HEADER_CSRF = "X-CSRF-TOKEN";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(true);
        String expected = (String) session.getAttribute(SESSION_CSRF);
        if (expected == null) {
            expected = newToken();
            session.setAttribute(SESSION_CSRF, expected);
        }

        if (isUnsafeMethod(request.getMethod())) {
            String actual = request.getHeader(HEADER_CSRF);
            if (actual == null || actual.isEmpty()) {
                actual = request.getParameter(PARAM_CSRF);
            }
            if (!constantTimeEquals(expected, actual)) {
                throw new ForbiddenException("CSRF token 校验失败");
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        // 注入模板变量 ${csrfToken}，供 meta 标签与 hidden 表单字段使用。
        // 跳过重定向视图：redirect:xxx 会把 model 属性序列化为 URL 查询参数（?csrfToken=...），
        // 导致 token 泄露到浏览器历史 / 访问日志 / Referer 头，属已知反模式；
        // 目标页面由新请求渲染，届时会重新注入 token，功能不受影响。
        if (modelAndView == null || modelAndView.getModel().containsKey("csrfToken")
                || isRedirectView(modelAndView)) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SESSION_CSRF) != null) {
            modelAndView.getModel().put("csrfToken", session.getAttribute(SESSION_CSRF));
        }
    }

    /** 是否为重定向视图（viewName 以 redirect: 开头，或显式设置的 RedirectView） */
    private boolean isRedirectView(ModelAndView modelAndView) {
        if (modelAndView.getView() instanceof RedirectView) {
            return true;
        }
        String viewName = modelAndView.getViewName();
        return viewName != null && viewName.startsWith("redirect:");
    }

    /** 登录成功后轮换 token，防止登录前的 token 被攻击者绑定到已认证会话 */
    public static void rotateToken(HttpSession session) {
        session.setAttribute(SESSION_CSRF, newToken());
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean isUnsafeMethod(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }

    /** 常量时间比较，避免时序侧信道 */
    private static boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        byte[] a = expected.getBytes();
        byte[] b = actual.getBytes();
        return MessageDigest.isEqual(a, b);
    }
}
