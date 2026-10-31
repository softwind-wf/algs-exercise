/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册登录鉴权/CSRF 拦截器，映射头像上传目录为静态资源。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final CsrfInterceptor csrfInterceptor;
    private final UploadProperties uploadProperties;

    public WebMvcConfig(AuthInterceptor authInterceptor, CsrfInterceptor csrfInterceptor,
                        UploadProperties uploadProperties) {
        this.authInterceptor = authInterceptor;
        this.csrfInterceptor = csrfInterceptor;
        this.uploadProperties = uploadProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // CSRF 校验优先于登录鉴权执行。
        // /verify 保留 CSRF 校验：GET 会下发 token，表单带 _csrf 提交；这样公开页也不需要豁免。
        registry.addInterceptor(csrfInterceptor)
                .addPathPatterns("/**")
                .order(0);
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login", "/css/**", "/js/**", "/images/**", "/uploads/**",
                        "/favicon.ico", "/error",
                        // 成绩单签名验证页：用人单位/其他学校无需登录即可验签
                        "/verify")
                .order(1);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 头像静态资源：/uploads/avatars/{文件名} -> 本地磁盘 app.upload.avatar-dir
        // 注意：URL 前缀 /uploads/avatars/ 与目录层级一一对应，避免路径重复（如 avatars/avatars/x.png）
        // PathResourceResolver 自带路径穿越防护，文件名一律服务端 UUID 生成，双重保险
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(uploadProperties.avatarDirResourceUri());
    }
}