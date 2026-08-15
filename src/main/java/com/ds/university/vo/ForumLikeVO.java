package com.ds.university.vo;

/** 帖子点赞人 */
public class ForumLikeVO {

    private String userId;
    private String userName;
    /** 格式化点赞时间（yyyy-MM-dd HH:mm） */
    private String createdTimeText;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCreatedTimeText() {
        return createdTimeText;
    }

    public void setCreatedTimeText(String createdTimeText) {
        this.createdTimeText = createdTimeText;
    }
}
