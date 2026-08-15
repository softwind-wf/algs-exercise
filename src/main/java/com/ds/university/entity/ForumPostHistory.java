package com.ds.university.entity;

import java.time.LocalDateTime;

/** 帖子编辑历史快照（保存每次编辑前的版本） */
public class ForumPostHistory {

    private Long id;
    private Long postId;
    private String title;
    private String content;
    private Integer categoryId;
    /** 编辑者登录账号 */
    private String editedBy;
    private LocalDateTime createdTime;
    /** 格式化时间（yyyy-MM-dd HH:mm），由查询 SQL 的 DATE_FORMAT 填充 */
    private String createdTimeText;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getEditedBy() {
        return editedBy;
    }

    public void setEditedBy(String editedBy) {
        this.editedBy = editedBy;
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
