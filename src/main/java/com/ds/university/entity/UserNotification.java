package com.ds.university.entity;

import java.time.LocalDateTime;

/** 站内通知（@提及等） */
public class UserNotification {

    /** 通知类型：论坛提及 */
    public static final String TYPE_FORUM_MENTION = "FORUM_MENTION";

    private Long id;
    /** 接收人登录账号 */
    private String userId;
    private String type;
    /** 跳转地址（如 /forum/123#reply-45） */
    private String sourceUrl;
    private String summary;
    /** 是否已读：1 已读 / 0 未读 */
    private Integer readFlag;
    private LocalDateTime createdTime;
    /** 格式化时间（yyyy-MM-dd HH:mm），由查询 SQL 的 DATE_FORMAT 填充 */
    private String createdTimeText;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getReadFlag() {
        return readFlag;
    }

    public void setReadFlag(Integer readFlag) {
        this.readFlag = readFlag;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getCreatedTimeText() {
        return createdTimeText;
    }

    public void setCreatedTimeText(String createdTimeText) {
        this.createdTimeText = createdTimeText;
    }
}
