package com.ds.university.vo;

import java.time.LocalDateTime;

/** 在线用户（由会话监听器追踪） */
public class OnlineUserVO {

    private String userId;
    private String userType;
    private String refId;
    /** 显示名（管理页查询后填充） */
    private String displayName;
    private LocalDateTime loginTime;
    /** 格式化登录时间（yyyy-MM-dd HH:mm:ss） */
    private String loginTimeText;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public String getLoginTimeText() {
        return loginTimeText;
    }

    public void setLoginTimeText(String loginTimeText) {
        this.loginTimeText = loginTimeText;
    }
}
