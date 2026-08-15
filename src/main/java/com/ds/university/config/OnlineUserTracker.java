package com.ds.university.config;

import com.ds.university.vo.LoginUser;
import com.ds.university.vo.OnlineUserVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 在线用户追踪器（配合 {@link SessionListener} 使用）：
 * <ul>
 *   <li>登录成功后由 AuthController 注册（sessionId → 用户）；</li>
 *   <li>会话销毁（退出登录 / 超时 / 容器回收）由 SessionListener 注销；</li>
 *   <li>存储由 {@link OnlineUserStore} 提供：多实例模式走 Redis（跨实例共享 + TTL 兜底清理），
 *       否则单机内存实现；</li>
 *   <li>管理端可查看当前在线用户数与明细。</li>
 * </ul>
 * 注：总会话数（含匿名访客）为实例本地统计，仅作参考；在线用户数为跨实例共享。
 */
@Component
public class OnlineUserTracker {

    private static final Logger log = LoggerFactory.getLogger(OnlineUserTracker.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 在线会话 TTL：会话超时（30 分钟）+ 5 分钟宽限；实例宕机时由 Redis 自动清理 */
    private static final long SESSION_TTL_SECONDS = 35 * 60L;

    private final OnlineUserStore store;
    private final ObjectMapper objectMapper;
    /** 全部会话数（含未登录访客，仅实例本地参考） */
    private final AtomicInteger totalSessions = new AtomicInteger();

    public OnlineUserTracker(OnlineUserStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    /** 会话创建（含匿名访客） */
    public void sessionCreated() {
        totalSessions.incrementAndGet();
    }

    /** 登录成功后注册为在线用户 */
    public void login(HttpSession session, LoginUser user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getUserId());
        data.put("userType", user.getUserType());
        data.put("refId", user.getRefId());
        data.put("loginTime", LocalDateTime.now().toString());
        try {
            store.put(session.getId(), objectMapper.writeValueAsString(data), SESSION_TTL_SECONDS);
        } catch (Exception e) {
            log.warn("在线用户写入失败 sessionId={}: {}", session.getId(), e.toString());
        }
    }

    /** 会话销毁：退出 / 超时 / 容器回收 */
    public void sessionDestroyed(HttpSession session) {
        if (session != null) {
            store.remove(session.getId());
        }
        totalSessions.updateAndGet(n -> n > 0 ? n - 1 : 0);
    }

    /** 当前在线（已登录）人数 */
    public int onlineCount() {
        return store.count();
    }

    /** 全部会话数（含匿名访客，仅本实例） */
    public int totalSessionCount() {
        return totalSessions.get();
    }

    /** 在线用户明细（按登录时间倒序），并填充格式化登录时间 */
    public List<OnlineUserVO> onlineUsers() {
        List<OnlineUserVO> list = new ArrayList<>();
        for (String json : store.allValues()) {
            try {
                Map<String, Object> data = objectMapper.readValue(json,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { });
                OnlineUserVO vo = new OnlineUserVO();
                vo.setUserId(String.valueOf(data.get("userId")));
                vo.setUserType(data.get("userType") == null ? null : String.valueOf(data.get("userType")));
                vo.setRefId(data.get("refId") == null ? null : String.valueOf(data.get("refId")));
                String loginTime = data.get("loginTime") == null ? null : String.valueOf(data.get("loginTime"));
                if (loginTime != null && !loginTime.isEmpty()) {
                    vo.setLoginTime(LocalDateTime.parse(loginTime));
                    vo.setLoginTimeText(FMT.format(vo.getLoginTime()));
                }
                list.add(vo);
            } catch (Exception e) {
                log.warn("在线用户数据解析失败: {}", e.toString());
            }
        }
        list.sort((a, b) -> {
            LocalDateTime ta = a.getLoginTime() == null ? LocalDateTime.MIN : a.getLoginTime();
            LocalDateTime tb = b.getLoginTime() == null ? LocalDateTime.MIN : b.getLoginTime();
            return tb.compareTo(ta);
        });
        return list;
    }
}
