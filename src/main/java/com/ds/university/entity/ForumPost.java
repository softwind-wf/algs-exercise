package com.ds.university.entity;

import java.time.LocalDateTime;

/** 论坛帖子 */
public class ForumPost {

    /** 标题最大长度 */
    public static final int MAX_TITLE_LENGTH = 100;
    /** 正文最大长度 */
    public static final int MAX_CONTENT_LENGTH = 2000;

    private Long id;
    private String title;
    private String content;
    /** 板块ID（FK→forum_category） */
    private Integer categoryId;
    /** 板块名称（查询 LEFT JOIN 填充） */
    private String categoryName;
    /** 置顶：1 置顶（管理员） */
    private Integer pinned;
    /** 加精：1 精华（管理员） */
    private Integer featured;
    /** 点赞数（冗余计数） */
    private Integer likeCount;
    /** 当前用户是否已点赞（按查看者计算，列表/详情填充） */
    private Boolean liked;
    /** @提及渲染后的安全 HTML（服务端计算，模板用 th:utext 输出） */
    private String renderedContent;
    private String authorUser;
    private String authorName;
    /** 回复数（冗余计数） */
    private Integer replyCount;
    private LocalDateTime lastReplyTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    /** 格式化时间（yyyy-MM-dd HH:mm），由查询 SQL 的 DATE_FORMAT 填充 */
    private String createdTimeText;
    private String lastReplyTimeText;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getPinned() {
        return pinned;
    }

    public void setPinned(Integer pinned) {
        this.pinned = pinned;
    }

    public Integer getFeatured() {
        return featured;
    }

    public void setFeatured(Integer featured) {
        this.featured = featured;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Boolean getLiked() {
        return liked;
    }

    public void setLiked(Boolean liked) {
        this.liked = liked;
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

    public Integer getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    public LocalDateTime getLastReplyTime() {
        return lastReplyTime;
    }

    public void setLastReplyTime(LocalDateTime lastReplyTime) {
        this.lastReplyTime = lastReplyTime;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getCreatedTimeText() {
        return createdTimeText;
    }

    public void setCreatedTimeText(String createdTimeText) {
        this.createdTimeText = createdTimeText;
    }

    public String getLastReplyTimeText() {
        return lastReplyTimeText;
    }

    public void setLastReplyTimeText(String lastReplyTimeText) {
        this.lastReplyTimeText = lastReplyTimeText;
    }
}
