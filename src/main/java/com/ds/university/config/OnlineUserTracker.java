package com.ds.university.config;

import com.ds.university.vo.LoginUser;
import com.ds.university.vo.OnlineUserVO;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 在线用户追踪器（配合 {@link SessionListener} 使用）：
 * <ul>
 *   <li>登录成功后由 AuthController 注册（sessionId → 用户）；</li>
 *   <li>会话销毁（退出登录 / 超时 / 容器回收）由 SessionListener 注销；</li>
 *   <li>管理端可查看当前在线用户数与明细。</li>
 * </ul>
 * 线程安全：ConcurrentHashMap + AtomicInteger，多实例部署需改为 Redis 等共享存储。
 */
@Component
public class OnlineUserTracker {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** sessionId → 在线用户（已登录会话） */
    private final ConcurrentHashMap<String, OnlineUserVO> onlineSessions = new ConcurrentHashMap<>();
    /** 全部会话数（含未登录访客，仅供参考） */
    private final AtomicInteger totalSessions = new AtomicInteger();

    /** 会话创建（含匿名访客） */
    public void sessionCreated() {
        totalSessions.incrementAndGet();
    }

    /** 登录成功后注册为在线用户 */
    public void login(HttpSession session, LoginUser user) {
        OnlineUserVO vo = new OnlineUserVO();
        vo.setUserId(user.getUserId());
        vo.setUserType(user.getUserType());
        vo.setRefId(user.getRefId());
        vo.setLoginTime(LocalDateTime.now());
        onlineSessions.put(session.getId(), vo);
    }

    /** 会话销毁：退出 / 超时 / 容器回收 */
    public void sessionDestroyed(HttpSession session) {
        if (session != null) {
            onlineSessions.remove(session.getId());
        }
        totalSessions.updateAndGet(n -> n > 0 ? n - 1 : 0);
    }

    /** 当前在线（已登录）人数 */
    public int onlineCount() {
        return onlineSessions.size();
    }

    /** 全部会话数（含匿名访客） */
    public int totalSessionCount() {
        return totalSessions.get();
    }

    /** 在线用户明细（按登录时间倒序），并填充格式化登录时间 */
    public List<OnlineUserVO> onlineUsers() {
        List<OnlineUserVO> list = new ArrayList<>(onlineSessions.values());
        list.sort((a, b) -> b.getLoginTime().compareTo(a.getLoginTime()));
        for (OnlineUserVO vo : list) {
            vo.setLoginTimeText(vo.getLoginTime() == null ? "" : FMT.format(vo.getLoginTime()));
        }
        return list;
    }
}
