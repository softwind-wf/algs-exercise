/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.config;

import com.ds.university.controller.AuthController;
import com.ds.university.service.ChatService;
import com.ds.university.vo.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpSession;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Map;

/**
 * 聊天 WebSocket 握手鉴权：
 * <ul>
 *   <li>从 HttpSession 读取登录用户（复用登录会话，握手请求自动携带 JSESSIONID），未登录拒绝握手；</li>
 *   <li>同源校验：浏览器握手带 Origin，必须与 Host 一致（防跨站 WebSocket 劫持）；
 *       非浏览器客户端（测试/原生应用）不带 Origin，放行；</li>
 *   <li>用户身份写入会话属性，后续消息处理直接使用，不信任客户端自报身份。</li>
 * </ul>
 */
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "chatUserId";
    public static final String ATTR_USER_NAME = "chatUserName";

    private final ChatService chatService;

    public ChatHandshakeInterceptor(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!sameOrigin(request)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        if (request instanceof ServletServerHttpRequest) {
            HttpSession session = ((ServletServerHttpRequest) request).getServletRequest().getSession(false);
            if (session != null) {
                LoginUser loginUser = (LoginUser) session.getAttribute(AuthController.SESSION_USER);
                if (loginUser != null) {
                    attributes.put(ATTR_USER_ID, loginUser.getUserId());
                    attributes.put(ATTR_USER_NAME, chatService.displayName(loginUser.getUserId()));
                    return true;
                }
            }
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无额外处理
    }

    /** 浏览器握手必须同源（Origin 的 host:port 与 Host 头一致）；无 Origin（非浏览器）放行 */
    private boolean sameOrigin(ServerHttpRequest request) {
        String origin = request.getHeaders().getOrigin();
        if (origin == null || origin.isEmpty()) {
            return true;
        }
        InetSocketAddress host = request.getHeaders().getHost();
        if (host == null) {
            return false;
        }
        try {
            URI uri = URI.create(origin);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!("http".equals(scheme) || "https".equals(scheme))) {
                return false;
            }
            String originHost = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String hostName = host.getHostString() == null ? "" : host.getHostString().toLowerCase(Locale.ROOT);
            if (!originHost.equals(hostName)) {
                return false;
            }
            int originPort = uri.getPort() == -1 ? ("https".equals(scheme) ? 443 : 80) : uri.getPort();
            int actualPort = host.getPort() == -1 ? ("https".equals(scheme) ? 443 : 80) : host.getPort();
            return originPort == actualPort;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
