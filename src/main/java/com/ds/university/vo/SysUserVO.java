package com.ds.university.vo;

import java.util.ArrayList;
import java.util.List;

/** 账号管理列表视图 */
public class SysUserVO {

    private String userId;
    private String userType;
    private String refId;
    /** 头像文件名（/uploads/avatars/{文件名} 访问），null 表示未设置 */
    private String avatar;
    private Integer enabled;
    private String createTimeText;
    private List<String> roles = new ArrayList<>();
    private String refName;
    private String refInfo;

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

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public String getCreateTimeText() {
        return createTimeText;
    }

    public void setCreateTimeText(String createTimeText) {
        this.createTimeText = createTimeText;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getRefName() {
        return refName;
    }

    public void setRefName(String refName) {
        this.refName = refName;
    }

    public String getRefInfo() {
        return refInfo;
    }

    public void setRefInfo(String refInfo) {
        this.refInfo = refInfo;
    }
}