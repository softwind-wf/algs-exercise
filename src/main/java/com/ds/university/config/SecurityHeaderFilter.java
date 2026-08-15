/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.config;

import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 安全响应头过滤器：为所有响应统一注入防点击劫持、防 MIME 嗅探等安全头。
 * <p>
 * 放在 Servlet 过滤器层（先于 MVC 拦截器），保证错误页、静态资源与业务响应全覆盖。
 */
@Component
public class SecurityHeaderFilter implements Filter {

    /**
     * 内容安全策略：
     * - default-src 'self'：默认只允许同源资源，杜绝外部脚本注入后的加载通道
     * - script-src / style-src 保留 'unsafe-inline'：现有模板存在内联脚本块与
     *   onclick 等内联事件处理器（admin 页面），完全禁内联会导致页面功能失效；
     *   来源已锁定为同源，注入的外部脚本仍无法加载远程载荷
     * - img-src / font-src 允许 data: URI（图标、报表内嵌图片场景）
     * - frame-ancestors 'none'：CSP 层面的防嵌套（比 X-Frame-Options 更强，二者并存兼容旧浏览器）
     * - base-uri 'self'：防止注入 &lt;base&gt; 标签劫持相对路径
     * - form-action 'self'：表单只能提交到同源，阻断表单数据外带
     */
    public static final String CONTENT_SECURITY_POLICY =
            "default-src 'self'; "
                    + "script-src 'self' 'unsafe-inline'; "
                    + "style-src 'self' 'unsafe-inline'; "
                    + "img-src 'self' data:; "
                    + "font-src 'self' data:; "
                    + "frame-ancestors 'none'; "
                    + "base-uri 'self'; "
                    + "form-action 'self'";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            // 防点击劫持：禁止页面被任何框架嵌套
            httpResponse.setHeader("X-Frame-Options", "DENY");
            // 防 MIME 嗅探：浏览器不得猜测 Content-Type，阻断伪装脚本上传被当 JS 执行
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            // CSP：见常量注释
            httpResponse.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
            // Referrer 策略：跨站只带来源站点不带完整路径，防敏感 URL 参数泄露
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            // 旧浏览器 XSS 过滤器兜底（现代浏览器已废弃该特性，仅为兼容与安全扫描器要求保留）
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        }
        chain.doFilter(request, response);
    }
}
