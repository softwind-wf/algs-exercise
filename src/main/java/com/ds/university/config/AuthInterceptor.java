/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.config;

import com.ds.university.common.ForbiddenException;
import com.ds.university.vo.LoginUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录鉴权拦截器：/admin/** 需要 ADMIN，/student/** 需要 STUDENT，/instructor/** 需要 INSTRUCTOR；
 * /instructors 等公开列表不受影响。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String ADMIN_PREFIX = "/admin";
    private static final String STUDENT_PREFIX = "/student";
    private static final String INSTRUCTOR_PREFIX = "/instructor";
    private static final String ACCOUNT_PREFIX = "/account";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        LoginUser user = session == null ? null : (LoginUser) session.getAttribute("loginUser");

        String uri = request.getRequestURI();
        if (matches(uri, ADMIN_PREFIX)) {
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
            if (!user.getRoles().contains("ADMIN")) {
                throw new ForbiddenException("当前账号没有权限访问教务管理页面");
            }
        }
        if (matches(uri, STUDENT_PREFIX)) {
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
            if (!user.getRoles().contains("STUDENT")) {
                throw new ForbiddenException("当前账号没有权限访问学生中心页面");
            }
        }
        if (matches(uri, INSTRUCTOR_PREFIX)) {
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
            if (!user.getRoles().contains("INSTRUCTOR")) {
                throw new ForbiddenException("当前账号没有权限访问教师中心页面");
            }
        }
        if (matches(uri, ACCOUNT_PREFIX)) {
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
        }
        return true;
    }
    /** 精确前缀匹配：/xxx 或 /xxx/...，避免误拦 /xxx 的同前缀公开路径（如 /instructors）。 */
    private boolean matches(String uri, String prefix) {
        return uri.equals(prefix) || uri.startsWith(prefix + "/");
    }
}