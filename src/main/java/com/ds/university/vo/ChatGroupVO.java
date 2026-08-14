package com.ds.university.vo;

import java.util.List;

/** 聊天联系人分组（如"我的同学""我的老师"），由服务端按用户角色与选课关系计算 */
public class ChatGroupVO {

    private String title;
    private List<ChatUserVO> users;

    public ChatGroupVO() {
    }

    public ChatGroupVO(String title, List<ChatUserVO> users) {
        this.title = title;
        this.users = users;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ChatUserVO> getUsers() {
        return users;
    }

    public void setUsers(List<ChatUserVO> users) {
        this.users = users;
    }
}
