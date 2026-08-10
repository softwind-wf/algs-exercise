package com.ds.university.config;

import com.ds.university.vo.LoginUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录鉴权拦截器：/admin/** 需要 ADMIN 角色。
 * 后续可扩展为按 @RequirePermission 注解做细粒度权限校验。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String ADMIN_PREFIX = "/admin";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        LoginUser user = session == null ? null : (LoginUser) session.getAttribute("loginUser");

        String uri = request.getRequestURI();
        if (uri.startsWith(ADMIN_PREFIX)) {
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
            if (!user.getRoles().contains("ADMIN")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }
        return true;
    }
}