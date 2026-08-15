/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.config;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * 会话监听器：配合 {@link OnlineUserTracker} 统计在线人数。
 * 会话创建（含匿名访客）计数；会话销毁（退出登录 invalidate / 超时 / 容器回收）时
 * 注销在线记录并扣减总会话数。
 * Spring Boot 会自动把实现 HttpSessionListener 的 Bean 注册到内嵌容器。
 */
@Component
public class SessionListener implements HttpSessionListener {

    private final OnlineUserTracker tracker;

    public SessionListener(OnlineUserTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        tracker.sessionCreated();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        tracker.sessionDestroyed(se.getSession());
    }
}
