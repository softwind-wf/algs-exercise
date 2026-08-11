package com.ds.university.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册登录鉴权拦截器。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final CsrfInterceptor csrfInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor, CsrfInterceptor csrfInterceptor) {
        this.authInterceptor = authInterceptor;
        this.csrfInterceptor = csrfInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // CSRF 校验优先于登录鉴权执行
        registry.addInterceptor(csrfInterceptor)
                .addPathPatterns("/**")
                .order(0);
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login", "/css/**", "/js/**", "/images/**",
                        "/favicon.ico", "/error")
                .order(1);
    }
}