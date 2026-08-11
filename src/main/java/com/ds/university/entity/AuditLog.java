package com.ds.university.entity;

import java.util.Date;

/** 审计日志（敏感操作留痕，只增不改） */
public class AuditLog {

    private Long id;
    /** 操作者登录账号；无请求上下文（如定时任务/测试）时为 null */
    private String userId;
    /** 操作类型，见 AuditService 的 ACTION_* 常量 */
    private String action;
    /** 对象类型，见 AuditService 的 TARGET_* 常量 */
    private String targetType;
    /** 对象标识（业务主键或组合键） */
    private String targetId;
    /** 操作详情（含变更前后值，供追溯） */
    private String detail;
    /** 操作来源 IP（TCP 对端地址） */
    private String clientIp;
    private Date createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
