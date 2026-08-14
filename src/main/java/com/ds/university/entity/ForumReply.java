package com.ds.university.entity;

import java.time.LocalDateTime;

/** 论坛回复 */
public class ForumReply {

    /** 回复内容最大长度 */
    public static final int MAX_CONTENT_LENGTH = 1000;

    private Long id;
    private Long postId;
    private String content;
    /** @提及渲染后的安全 HTML（服务端计算，模板用 th:utext 输出） */
    private String renderedContent;
    private String authorUser;
    private String authorName;
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

    public String getRenderedContent() {
        return renderedContent;
    }

    public void setRenderedContent(String renderedContent) {
        this.renderedContent = renderedContent;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthorUser() {
        return authorUser;
    }

    public void setAuthorUser(String authorUser) {
        this.authorUser = authorUser;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
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
