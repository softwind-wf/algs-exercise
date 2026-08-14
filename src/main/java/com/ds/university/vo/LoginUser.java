package com.ds.university.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录会话中的当前用户（含角色与权限集合）。
 */
public class LoginUser {

    private String userId;
    private String userType;
    private String refId;
    /** 头像文件名（null 表示未设置），登录时装载、上传/移除后同步更新会话 */
    private String avatar;
    private List<String> roles = new ArrayList<String>();
    private List<String> permissions = new ArrayList<String>();

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}