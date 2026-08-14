package com.ds.university.entity;

import java.time.LocalDateTime;

/** 系统公告 */
public class Announcement {

    /** 公告状态（由服务按 enabled/发布时间/到期时间实时计算） */
    public static final String STATUS_PUBLISHED = "已发布";
    public static final String STATUS_SCHEDULED = "定时发布";
    public static final String STATUS_EXPIRED = "已过期";
    public static final String STATUS_OFFLINE = "已下线";

    private Integer id;
    private String title;
    private String content;
    /** 公告类型：NOTICE 通知 / NEWS 新闻动态 / ACTIVITY 活动 / OTHER 其他 */
    private String category;
    /** 置顶：1 置顶 / 0 普通 */
    private Integer pinned;
    /** 发布状态：1 已发布（对外可见）/ 0 已下线 */
    private Integer enabled;
    /** 定时发布时间（null=立即发布；未来时间则到时自动对外可见） */
    private LocalDateTime publishTime;
    /** 到期时间（null=永不过期），到期自动下线 */
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 格式化时间（yyyy-MM-dd HH:mm），由查询 SQL 的 DATE_FORMAT 填充，供模板直接展示 */
    private String createTimeText;
    private String publishTimeText;
    private String expireTimeText;
    /** 管理端表单 datetime-local 输入框值（yyyy-MM-dd'T'HH:mm），由服务填充 */
    private String publishTimeInput;
    private String expireTimeInput;
    /** 展示状态（已发布/定时发布/已过期/已下线），由服务实时计算 */
    private String statusText;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getPinned() {
        return pinned;
    }

    public void setPinned(Integer pinned) {
        this.pinned = pinned;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateTimeText() {
        return createTimeText;
    }

    public void setCreateTimeText(String createTimeText) {
        this.createTimeText = createTimeText;
    }

    public String getPublishTimeText() {
        return publishTimeText;
    }

    public void setPublishTimeText(String publishTimeText) {
        this.publishTimeText = publishTimeText;
    }

    public String getExpireTimeText() {
        return expireTimeText;
    }

    public void setExpireTimeText(String expireTimeText) {
        this.expireTimeText = expireTimeText;
    }

    public String getPublishTimeInput() {
        return publishTimeInput;
    }

    public void setPublishTimeInput(String publishTimeInput) {
        this.publishTimeInput = publishTimeInput;
    }

    public String getExpireTimeInput() {
        return expireTimeInput;
    }

    public void setExpireTimeInput(String expireTimeInput) {
        this.expireTimeInput = expireTimeInput;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }
}
