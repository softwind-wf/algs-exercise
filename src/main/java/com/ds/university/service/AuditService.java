package com.ds.university.service;

import com.ds.university.controller.AuthController;
import com.ds.university.entity.AuditLog;
import com.ds.university.mapper.AuditLogMapper;
import com.ds.university.vo.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;

/**
 * 审计日志服务：敏感操作（改成绩、删数据、建账号等）留痕。
 *
 * <p>设计要点：
 * <ul>
 *   <li>操作者/来源 IP 自动从当前 Web 请求上下文解析，调用方无需传参；
 *       无请求上下文（集成测试/定时任务）时操作者与 IP 记为 null。</li>
 *   <li>审计写入失败只记 warn 日志，绝不阻断业务（留痕是辅助能力，不能反过来影响主流程）。</li>
 *   <li>audit_log 只增不改，追溯时按 detail 中的变更前后值还原现场。</li>
 * </ul>
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    /** 页面查询默认返回条数上限 */
    public static final int QUERY_LIMIT = 200;

    // ---- 操作类型 ----
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_GRADE_UPDATE = "GRADE_UPDATE";
    public static final String ACTION_ACCOUNT_CREATE = "ACCOUNT_CREATE";
    public static final String ACTION_ACCOUNT_DELETE = "ACCOUNT_DELETE";
    public static final String ACTION_ACCOUNT_TOGGLE = "ACCOUNT_TOGGLE";
    public static final String ACTION_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String ACTION_ACCOUNT_BATCH_CREATE = "ACCOUNT_BATCH_CREATE";
    public static final String ACTION_AVATAR_UPDATE = "AVATAR_UPDATE";
    public static final String ACTION_AVATAR_REMOVE = "AVATAR_REMOVE";

    /** 全部操作类型（页面筛选下拉用，顺序即展示顺序） */
    public static final List<String> ALL_ACTIONS = Arrays.asList(
            ACTION_GRADE_UPDATE, ACTION_DELETE, ACTION_CREATE, ACTION_UPDATE,
            ACTION_ACCOUNT_CREATE, ACTION_ACCOUNT_BATCH_CREATE, ACTION_PASSWORD_RESET,
            ACTION_ACCOUNT_TOGGLE, ACTION_ACCOUNT_DELETE, ACTION_AVATAR_UPDATE, ACTION_AVATAR_REMOVE);

    // ---- 对象类型 ----
    public static final String TARGET_DEPARTMENT = "DEPARTMENT";
    public static final String TARGET_COURSE = "COURSE";
    public static final String TARGET_INSTRUCTOR = "INSTRUCTOR";
    public static final String TARGET_STUDENT = "STUDENT";
    public static final String TARGET_CLASSROOM = "CLASSROOM";
    public static final String TARGET_SECTION = "SECTION";
    public static final String TARGET_PREREQ = "PREREQ";
    public static final String TARGET_ACCOUNT = "ACCOUNT";
    public static final String TARGET_GRADE = "GRADE";
    public static final String TARGET_ANNOUNCEMENT = "ANNOUNCEMENT";

    private final AuditLogMapper auditLogMapper;

    public AuditService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 记录一条审计日志（操作者与来源 IP 自动解析）。
     * 任何异常都被吞掉，只打 warn——审计失败不阻断业务。
     */
    public void record(String action, String targetType, String targetId, String detail) {
        try {
            auditLogMapper.insert(resolveUserId(), action, targetType, targetId,
                    truncate(detail, 500), resolveClientIp());
        } catch (Exception e) {
            log.warn("审计日志写入失败 action={}, targetType={}, targetId={}: {}",
                    action, targetType, targetId, e.toString());
        }
    }

    /** 按操作类型/关键字查询最近 limit 条（倒序），供管理端追溯页面使用 */
    public List<AuditLog> query(String action, String keyword, int limit) {
        return auditLogMapper.query(emptyToNull(action), emptyToNull(keyword),
                limit <= 0 || limit > QUERY_LIMIT ? QUERY_LIMIT : limit);
    }

    /** 从会话取当前登录账号；无请求上下文或未登录返回 null */
    private String resolveUserId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object user = session.getAttribute(AuthController.SESSION_USER);
        return user instanceof LoginUser ? ((LoginUser) user).getUserId() : null;
    }

    /** 只取 TCP 对端地址（与登录限流口径一致，不读可伪造的 X-Forwarded-For） */
    private String resolveClientIp() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getRemoteAddr() : null;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static String emptyToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
